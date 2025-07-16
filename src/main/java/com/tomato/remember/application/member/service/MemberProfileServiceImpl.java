package com.tomato.remember.application.member.service;

import com.tomato.remember.application.member.dto.ProfileImageDTO;
import com.tomato.remember.application.member.dto.ProfileSettingsDTO;
import com.tomato.remember.application.member.dto.ProfileUpdateRequest;
import com.tomato.remember.application.member.dto.ProfileUpdateResponse;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.entity.MemberAiProfileImage;
import com.tomato.remember.application.member.repository.MemberAiProfileImageRepository;
import com.tomato.remember.application.member.repository.MemberRepository;
import com.tomato.remember.common.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 통합 프로필 관리 서비스 (수정된 버전)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberAiProfileImageRepository profileImageRepository;
    private final FileStorageService fileStorageService;
    private final FaceDetectionService faceDetectionService;

    // 지원하는 이미지 타입
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 통합 프로필 업데이트 (Request DTO 사용)
     */
    @Override
    public ProfileUpdateResponse updateProfileUnified(Long memberId, ProfileUpdateRequest request) {
        log.info("통합 프로필 업데이트 시작 - 회원 ID: {}", memberId);

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 1. 기본 정보 업데이트
        updateBasicInfo(member, request);

        // 2. 이미지 처리
        ProfileImageResult imageResult = processImagesWithSortOrder(member, request);

        // 3. 저장
        Member updatedMember = memberRepository.save(member);

        // 4. 🔥 프로필 이미지가 5장 완성되면 얼굴 인식 처리 (간단 버전)
        if (imageResult.getFinalImageCount() == 5) {
            log.info("프로필 이미지 5장 완성 - 얼굴 인식 처리 시작");
            faceDetectionService.processProfileImages(updatedMember);
        }

        // 5. 최종 이미지 정보 다시 조회 (얼굴 인식 결과 포함)
        List<MemberAiProfileImage> finalImages = profileImageRepository.findByMemberOrderBySortOrderAsc(updatedMember);

        List<ProfileImageDTO> finalImageDTOs = finalImages.stream()
        .map(img -> ProfileImageDTO.builder()
            .sortOrder(img.getSortOrder())
            .imageUrl(img.getImageUrl())
            .originalFilename(img.getOriginalFilename())
            .aiProcessed(img.getAiProcessed()) //얼굴 인식 결과 포함
            .build())
        .collect(Collectors.toList());

        // 6.재인증 필요한 이미지 번호 찾기
        List<Integer> invalidImageNumbers = finalImages.stream()
            .filter(img -> !img.getAiProcessed())
            .map(MemberAiProfileImage::getSortOrder)
            .collect(Collectors.toList());

        log.info("통합 프로필 업데이트 완료 - 회원 ID: {}, 최종 이미지 수: {}",
            memberId, imageResult.getFinalImageCount());

        return ProfileUpdateResponse.builder()
            .memberId(updatedMember.getId())
            .name(updatedMember.getName())
            .email(updatedMember.getEmail())
            .phoneNumber(updatedMember.getPhoneNumber())
            .birthDate(updatedMember.getBirthDate())
            .preferredLanguage(updatedMember.getPreferredLanguage())
            .totalImages(imageResult.getFinalImageCount())
            .imageUrls(finalImageDTOs)
            .invalidImageNumbers(invalidImageNumbers)
            .updatedAt(updatedMember.getUpdatedAt())
            .build();
    }

    /**
     * 이미지 처리 메인 로직 (sortOrder 포함)
     */
    private ProfileImageResult processImagesWithSortOrder(Member member, ProfileUpdateRequest request) {
        List<MemberAiProfileImage> currentImages =
            profileImageRepository.findByMemberOrderBySortOrderAsc(member);

        log.debug("현재 이미지 개수: {}", currentImages.size());

        MultipartFile[] images = request.getImages();
        Integer[] imagesToDelete = request.getImagesToDelete();

        if (images == null || images.length == 0) {
            // 시나리오 1: 기본 정보만 수정
            return handleBasicInfoOnlyWithSortOrder(currentImages);
        } else if (currentImages.isEmpty()) {
            // 시나리오 2: 처음 사진 업로드
            return handleFirstTimeUploadWithSortOrder(member, images);
        } else {
            // 시나리오 3: 기존 사진 수정
            return handleExistingImageUpdateWithSortOrder(member, currentImages, images, imagesToDelete);
        }
    }

    /**
     * 시나리오 1: 기본 정보만 수정 (sortOrder 포함)
     */
    private ProfileImageResult handleBasicInfoOnlyWithSortOrder(List<MemberAiProfileImage> currentImages) {
        log.debug("시나리오 1: 기본 정보만 수정 - sortOrder 포함");

        List<ProfileImageDTO> images = currentImages.stream()
            .map(img -> ProfileImageDTO.builder()
                .sortOrder(img.getSortOrder())
                .imageUrl(img.getImageUrl())
                .originalFilename(img.getOriginalFilename())
                .build())
            .collect(Collectors.toList());

        return new ProfileImageResult(currentImages.size(), images);
    }

    /**
     * 시나리오 2: 처음 사진 업로드 (sortOrder 포함)
     */
    private ProfileImageResult handleFirstTimeUploadWithSortOrder(Member member, MultipartFile[] images) {
        log.debug("시나리오 2: 처음 사진 업로드 - sortOrder 포함");

        if (images.length != 5) {
            throw new IllegalArgumentException("프로필 사진은 반드시 5장을 모두 업로드해야 합니다.");
        }

        List<ProfileImageDTO> uploadedImages = new ArrayList<>();

        for (int i = 0; i < images.length; i++) {
            MultipartFile image = images[i];
            validateImageFile(image);

            try {
                int sortOrder = i + 1;
                String imageUrl = fileStorageService.saveProfileImage(image, member.getId(), sortOrder);

                MemberAiProfileImage profileImage = MemberAiProfileImage.create(
                    member, imageUrl, sortOrder, image.getOriginalFilename(),
                    image.getSize(), image.getContentType()
                );

                profileImageRepository.save(profileImage);
                member.addProfileImage(profileImage);

                // 🔧 개선: sortOrder 포함 DTO 생성
                ProfileImageDTO imageDTO = ProfileImageDTO.builder()
                    .sortOrder(sortOrder)
                    .imageUrl(imageUrl)
                    .originalFilename(image.getOriginalFilename())
                    .build();

                uploadedImages.add(imageDTO);

                log.debug("이미지 업로드 완료 - 순서: {}, URL: {}", sortOrder, imageUrl);

            } catch (Exception e) {
                log.error("이미지 업로드 실패 - 순서: {}", i + 1, e);
                cleanupUploadedFiles(uploadedImages.stream()
                    .map(ProfileImageDTO::getImageUrl)
                    .collect(Collectors.toList()));
                throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
            }
        }

        return new ProfileImageResult(5, uploadedImages);
    }

    /**
     * 시나리오 3: 기존 사진 수정 (sortOrder 포함)
     */
    private ProfileImageResult handleExistingImageUpdateWithSortOrder(Member member,
        List<MemberAiProfileImage> currentImages, MultipartFile[] newImages,
        Integer[] imagesToDelete) {

        log.debug("시나리오 3: 기존 사진 수정 - sortOrder 포함");

        Set<Integer> deleteSet = imagesToDelete != null ?
            new HashSet<>(Arrays.asList(imagesToDelete)) : new HashSet<>();

        // 삭제 후 남을 이미지 계산
        int remainingCount = currentImages.size() - deleteSet.size();
        int newImageCount = newImages != null ? newImages.length : 0;
        int finalCount = remainingCount + newImageCount;

        // 최종 5장 검증
        if (finalCount != 5) {
            throw new IllegalArgumentException(
                String.format("최종 이미지는 반드시 5장이어야 합니다. (현재: %d장 - 삭제: %d장 + 신규: %d장 = %d장)",
                    currentImages.size(), deleteSet.size(), newImageCount, finalCount));
        }

        // 삭제 처리
        for (MemberAiProfileImage image : currentImages) {
            if (deleteSet.contains(image.getSortOrder())) {
                deleteExistingImage(image);
                log.debug("이미지 삭제 완료 - 순서: {}", image.getSortOrder());
            }
        }

        // 새 이미지 업로드 (개선된 중복 방지)
        List<ProfileImageDTO> newUploadedImages = new ArrayList<>();
        if (newImages != null && newImages.length > 0) {
            newUploadedImages = uploadNewImagesWithSortOrder(member, newImages);
        }

        // 최종 이미지 목록 생성
        List<MemberAiProfileImage> finalImages =
            profileImageRepository.findByMemberOrderBySortOrderAsc(member);

        List<ProfileImageDTO> finalImageDTOs = finalImages.stream()
            .map(img -> ProfileImageDTO.builder()
                .sortOrder(img.getSortOrder())
                .imageUrl(img.getImageUrl())
                .originalFilename(img.getOriginalFilename())
                .build())
            .collect(Collectors.toList());

        return new ProfileImageResult(5, finalImageDTOs);
    }

    /**
     * 새 이미지 업로드 (sortOrder 포함, 중복 방지)
     */
    private List<ProfileImageDTO> uploadNewImagesWithSortOrder(Member member, MultipartFile[] newImages) {
        List<ProfileImageDTO> uploadedImages = new ArrayList<>();

        // 기존 sortOrder 목록 확보
        List<MemberAiProfileImage> existingImages =
            profileImageRepository.findByMemberOrderBySortOrderAsc(member);

        Set<Integer> existingSortOrders = existingImages.stream()
            .map(MemberAiProfileImage::getSortOrder)
            .collect(Collectors.toSet());

        int currentSortOrder = 1;

        for (MultipartFile image : newImages) {
            validateImageFile(image);

            try {
                // 사용 가능한 다음 sortOrder 찾기
                while (existingSortOrders.contains(currentSortOrder)) {
                    currentSortOrder++;
                }

                int sortOrder = currentSortOrder;
                existingSortOrders.add(sortOrder); // 사용한 번호 추가
                currentSortOrder++; // 다음 번호로 증가

                String imageUrl = fileStorageService.saveProfileImage(image, member.getId(), sortOrder);

                MemberAiProfileImage profileImage = MemberAiProfileImage.create(
                    member, imageUrl, sortOrder, image.getOriginalFilename(),
                    image.getSize(), image.getContentType()
                );

                profileImageRepository.save(profileImage);
                member.addProfileImage(profileImage);

                // 🔧 개선: sortOrder 포함 DTO 생성
                ProfileImageDTO imageDTO = ProfileImageDTO.builder()
                    .sortOrder(sortOrder)
                    .imageUrl(imageUrl)
                    .originalFilename(image.getOriginalFilename())
                    .build();

                uploadedImages.add(imageDTO);

                log.debug("신규 이미지 업로드 완료 - 순서: {}, URL: {}", sortOrder, imageUrl);

            } catch (Exception e) {
                log.error("신규 이미지 업로드 실패 - sortOrder: {}", currentSortOrder, e);
                cleanupUploadedFiles(uploadedImages.stream()
                    .map(ProfileImageDTO::getImageUrl)
                    .collect(Collectors.toList()));
                throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
            }
        }

        return uploadedImages;
    }

    /**
     * 기본 정보 업데이트 (생년월일 포함)
     */
    private void updateBasicInfo(Member member, ProfileUpdateRequest request) {
        log.debug("기본 정보 업데이트 시작");

        // 이름 업데이트
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            member.setName(request.getName().trim());
            log.debug("이름 업데이트: {}", request.getName());
        }

        // 이메일 업데이트
        if (request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().isEmpty() ?
                null : request.getEmail().toLowerCase().trim();
            member.setEmail(normalizedEmail);
            log.debug("이메일 업데이트: {}", normalizedEmail);
        }

        // 전화번호 업데이트
        if (request.getPhoneNumber() != null) {
            String normalizedPhone = request.getPhoneNumber().trim().isEmpty() ?
                null : request.getPhoneNumber().replaceAll("[-\\s]", "");
            member.setPhoneNumber(normalizedPhone);
            log.debug("전화번호 업데이트: {}", normalizedPhone);
        }

        // 생년월일 업데이트 (새로 추가)
        if (request.getBirthDate() != null) {
            member.setBirthDate(request.getBirthDate());
            log.debug("생년월일 업데이트: {}", request.getBirthDate());
        }

        // 선호 언어 업데이트
        if (request.getPreferredLanguage() != null) {
            member.setPreferredLanguage(request.getPreferredLanguage());
            log.debug("선호 언어 업데이트: {}", request.getPreferredLanguage());
        }

        // 마케팅 동의 업데이트
        if (request.getMarketingAgreed() != null) {
            member.setMarketingAgreed(request.getMarketingAgreed());
            log.debug("마케팅 동의 업데이트: {}", request.getMarketingAgreed());
        }

        log.debug("기본 정보 업데이트 완료");
    }

    /**
     * 이미지 처리 결과 클래스 (수정된 버전)
     */
    private static class ProfileImageResult {
        private final int finalImageCount;
        private final List<ProfileImageDTO> images;

        public ProfileImageResult(int finalImageCount, List<ProfileImageDTO> images) {
            this.finalImageCount = finalImageCount;
            this.images = images;
        }

        public int getFinalImageCount() { return finalImageCount; }
        public List<ProfileImageDTO> getImages() { return images; }
    }

//    /**
//     * 시나리오 1: 기본 정보만 수정
//     */
//    private ProfileImageResult handleBasicInfoOnly(List<MemberAiProfileImage> currentImages) {
//        log.debug("시나리오 1: 기본 정보만 수정");
//
//        List<String> imageUrls = currentImages.stream()
//            .map(MemberAiProfileImage::getImageUrl)
//            .collect(Collectors.toList());
//
//        return new ProfileImageResult(currentImages.size(), imageUrls);
//    }

//    /**
//     * 시나리오 2: 처음 사진 업로드 (5장 필수)
//     */
//    private ProfileImageResult handleFirstTimeUpload(Member member, MultipartFile[] images) {
//        log.debug("시나리오 2: 처음 사진 업로드");
//
//        if (images.length != 5) {
//            throw new IllegalArgumentException("프로필 사진은 반드시 5장을 모두 업로드해야 합니다.");
//        }
//
//        List<String> uploadedUrls = new ArrayList<>();
//
//        for (int i = 0; i < images.length; i++) {
//            MultipartFile image = images[i];
//            validateImageFile(image);
//
//            try {
//                // 파일 업로드
//                String imageUrl = fileStorageService.saveProfileImage(image, member.getId(), i + 1);
//
//                // DB 저장
//                MemberAiProfileImage profileImage = MemberAiProfileImage.create(
//                    member, imageUrl, i + 1, image.getOriginalFilename(),
//                    image.getSize(), image.getContentType()
//                );
//
//                profileImageRepository.save(profileImage);
//                member.addProfileImage(profileImage);
//                uploadedUrls.add(imageUrl);
//
//                log.debug("이미지 업로드 완료 - 순서: {}, URL: {}", i + 1, imageUrl);
//
//            } catch (Exception e) {
//                log.error("이미지 업로드 실패 - 순서: {}", i + 1, e);
//                // 이미 업로드된 파일들 정리
//                cleanupUploadedFiles(uploadedUrls);
//                throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
//            }
//        }
//
//        return new ProfileImageResult(5, uploadedUrls);
//    }

//    /**
//     * 시나리오 3: 기존 사진 수정 (최종 5장 보장)
//     */
//    private ProfileImageResult handleExistingImageUpdate(Member member,
//        List<MemberAiProfileImage> currentImages, MultipartFile[] newImages,
//        Integer[] imagesToDelete) {
//
//        log.debug("시나리오 3: 기존 사진 수정");
//
//        // 삭제할 이미지 처리
//        Set<Integer> deleteSet = imagesToDelete != null ?
//            new HashSet<>(Arrays.asList(imagesToDelete)) : new HashSet<>();
//
//        // 삭제 후 남을 이미지 계산
//        int remainingCount = currentImages.size() - deleteSet.size();
//        int newImageCount = newImages != null ? newImages.length : 0;
//        int finalCount = remainingCount + newImageCount;
//
//        // 최종 5장 검증
//        if (finalCount != 5) {
//            throw new IllegalArgumentException(
//                String.format("최종 이미지는 반드시 5장이어야 합니다. (현재: %d장 - 삭제: %d장 + 신규: %d장 = %d장)",
//                    currentImages.size(), deleteSet.size(), newImageCount, finalCount));
//        }
//
//        // 삭제 처리
//        for (MemberAiProfileImage image : currentImages) {
//            if (deleteSet.contains(image.getSortOrder())) {
//                deleteExistingImage(image);
//                log.debug("이미지 삭제 완료 - 순서: {}", image.getSortOrder());
//            }
//        }
//
//        // 남은 이미지들 조회
//        List<MemberAiProfileImage> remainingImages =
//            profileImageRepository.findByMemberOrderBySortOrderAsc(member);
//
//        // 새 이미지 업로드
//        List<String> newUploadUrls = new ArrayList<>();
//        if (newImages != null && newImages.length > 0) {
//            newUploadUrls = uploadNewImages(member, newImages, remainingImages.size());
//        }
//
//        // 최종 URL 목록 생성
//        List<String> finalUrls = new ArrayList<>();
//        finalUrls.addAll(remainingImages.stream()
//            .map(MemberAiProfileImage::getImageUrl)
//            .collect(Collectors.toList()));
//        finalUrls.addAll(newUploadUrls);
//
//        return new ProfileImageResult(5, finalUrls);
//    }

    /**
     * 새 이미지들 업로드
     */
    private List<String> uploadNewImages(Member member, MultipartFile[] newImages, int startOrder) {
        List<String> uploadedUrls = new ArrayList<>();

        for (int i = 0; i < newImages.length; i++) {
            MultipartFile image = newImages[i];
            validateImageFile(image);

            try {
                int sortOrder = startOrder + i + 1;
                String imageUrl = fileStorageService.saveProfileImage(image, member.getId(), sortOrder);

                MemberAiProfileImage profileImage = MemberAiProfileImage.create(
                    member, imageUrl, sortOrder, image.getOriginalFilename(),
                    image.getSize(), image.getContentType()
                );

                profileImageRepository.save(profileImage);
                member.addProfileImage(profileImage);
                uploadedUrls.add(imageUrl);

                log.debug("신규 이미지 업로드 완료 - 순서: {}, URL: {}", sortOrder, imageUrl);

            } catch (Exception e) {
                log.error("신규 이미지 업로드 실패 - 순서: {}", startOrder + i + 1, e);
                cleanupUploadedFiles(uploadedUrls);
                throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
            }
        }

        return uploadedUrls;
    }

    /**
     * 이미지 파일 유효성 검사
     */
    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해주세요.");
        }

        if (imageFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB 이하여야 합니다.");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");
        }

        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }
    }

    /**
     * 기존 이미지 삭제 (파일 + DB)
     */
    private void deleteExistingImage(MemberAiProfileImage image) {
        try {
            fileStorageService.deleteFile(image.getImageUrl());
            profileImageRepository.delete(image);
        } catch (Exception e) {
            log.error("이미지 삭제 중 오류 발생 - ID: {}, URL: {}",
                image.getId(), image.getImageUrl(), e);
            // 파일 삭제 실패해도 DB는 삭제 (정합성 보다는 서비스 연속성 우선)
            profileImageRepository.delete(image);
        }
    }

    /**
     * 업로드 실패 시 정리
     */
    private void cleanupUploadedFiles(List<String> uploadedUrls) {
        for (String url : uploadedUrls) {
            try {
                fileStorageService.deleteFile(url);
            } catch (Exception e) {
                log.warn("업로드 실패 정리 중 오류 - URL: {}", url, e);
            }
        }
    }

    /**
     * 알림 설정 응답 생성
     */
    private Map<String, Object> buildNotificationSettings(Member member) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("pushNotification", member.getPushNotification() != null ? member.getPushNotification() : true);
        settings.put("memorialNotification", member.getMemorialNotification() != null ? member.getMemorialNotification() : true);
        settings.put("paymentNotification", member.getPaymentNotification() != null ? member.getPaymentNotification() : true);
        settings.put("familyNotification", member.getFamilyNotification() != null ? member.getFamilyNotification() : true);
        settings.put("marketingAgreed", member.getMarketingAgreed() != null ? member.getMarketingAgreed() : false);
        return settings;
    }

    /**
     * 프로필 완성도 응답 생성
     */
    private Map<String, Object> buildProfileCompletion(Member member, ProfileImageResult imageResult) {
        Map<String, Object> completion = new HashMap<>();
        completion.put("uploadedImageCount", imageResult.getFinalImageCount());
        completion.put("validImageCount", imageResult.getFinalImageCount());
        completion.put("completionPercentage", imageResult.getFinalImageCount() * 20);
        completion.put("canStartVideoCall", imageResult.getFinalImageCount() >= 5);
        return completion;
    }

    // 기존 메서드들 유지
    @Override
    public void deleteAccount(Long memberId) {
        // 기존 구현 유지
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canStartVideoCall(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return member.canStartVideoCall();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileSettingsDTO getProfileSettingsData(Long memberId) {
        List<MemberAiProfileImage> images = profileImageRepository
            .findByMemberOrderBySortOrderAsc(memberRepository.findById(memberId).orElseThrow());

        List<ProfileImageDTO> imageDTOs = images.stream()
            .map(img -> ProfileImageDTO.builder()
                .sortOrder(img.getSortOrder())
                .imageUrl(img.getImageUrl())
                .originalFilename(img.getOriginalFilename())
                .aiProcessed(img.getAiProcessed())
                .build())
            .collect(Collectors.toList());

        List<Integer> invalidImageNumbers = images.stream()
            .filter(img -> !img.getAiProcessed())
            .map(MemberAiProfileImage::getSortOrder)
            .collect(Collectors.toList());

        return ProfileSettingsDTO.builder()
            .uploadedImageCount(images.size())
            .validImageCount((int) images.stream().filter(MemberAiProfileImage::isValid).count())
            .imageUrls(imageDTOs) // 🔧 개선: sortOrder 포함
            .hasAnyImages(!images.isEmpty())
            .needsCompleteUpload(images.size() > 0 && images.size() < 5)
            .canStartVideoCall(images.size() == 5 && images.stream().allMatch(MemberAiProfileImage::isValid))
            .completionPercentage(images.size() * 20)
            .invalidImageNumbers(invalidImageNumbers)
            .build();
    }
}