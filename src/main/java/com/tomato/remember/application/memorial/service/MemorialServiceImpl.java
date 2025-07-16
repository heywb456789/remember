package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.repository.MemberRepository;
import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialCreateResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialListResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialQuestionAnswerDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.entity.MemorialAnswer;
import com.tomato.remember.application.memorial.entity.MemorialFile;
import com.tomato.remember.application.memorial.code.MemorialFileType;
import com.tomato.remember.application.memorial.entity.MemorialQuestion;
import com.tomato.remember.application.memorial.repository.MemorialAnswerRepository;
import com.tomato.remember.application.memorial.repository.MemorialQuestionRepository;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.code.StorageCategory;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.util.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 메모리얼 서비스 구현체 - FileStorageService 사용으로 리팩토링
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemorialServiceImpl implements MemorialService {

    private final MemorialRepository memorialRepository;
    private final FileStorageService fileStorageService;
    private final MemberRepository memberRepository;
    private final MemorialAnswerRepository memorialAnswerRepository;
    private final MemorialQuestionRepository memorialQuestionRepository;

    /**
     * 메모리얼 생성 (업데이트된 버전)
     */
    @Override
    @Transactional
    public MemorialCreateResponseDTO createMemorial(
            MemorialCreateRequestDTO memorialData,
            List<MultipartFile> profileImages,
            List<MultipartFile> voiceFiles,
            MultipartFile videoFile,
            Member member) {

        log.info("메모리얼 생성 시작 - 사용자: {}, 메모리얼명: {}, 답변 수: {}",
                member.getId(), memorialData.getName(), memorialData.getAnsweredQuestionCount());

        List<String> uploadedFileUrls = new ArrayList<>();

        try {
            // 1. 빈 답변 제거
            memorialData.removeEmptyAnswers();

            // 2. 메모리얼 엔티티 생성
            Memorial memorial = createMemorialEntity(memorialData, member);

            // 3. 프로필 이미지 파일 처리
            uploadedFileUrls.addAll(processProfileImages(memorial, profileImages, member.getId()));

            // 4. 음성 파일 처리
            uploadedFileUrls.addAll(processVoiceFiles(memorial, voiceFiles, member.getId()));

            // 5. 영상 파일 처리
            uploadedFileUrls.add(processVideoFile(memorial, videoFile, member.getId()));

            // 6. 메모리얼 저장
            Memorial savedMemorial = memorialRepository.save(memorial);

            // 7. 동적 질문 답변 처리
            processQuestionAnswers(savedMemorial, memorialData.getQuestionAnswers(), member);

            log.info("메모리얼 생성 완료 - ID: {}, 이름: {}, 답변 수: {}",
                    savedMemorial.getId(), savedMemorial.getName(),
                    memorialData.getAnsweredQuestionCount());

            return MemorialCreateResponseDTO.success(
                    savedMemorial.getId(),
                    savedMemorial.getName(),
                    savedMemorial.getNickname()
            );

        } catch (Exception e) {
            log.error("메모리얼 생성 실패 - 사용자: {}, 오류: {}", member.getId(), e.getMessage(), e);

            // 파일 롤백 처리
            rollbackUploadedFiles(uploadedFileUrls);

            throw new APIException("메모리얼 생성에 실패했습니다.", ResponseStatus.MEMORIAL_CREATION_FAILED);
        }
    }

    /**
     * 업로드된 파일들 롤백
     */
    private void rollbackUploadedFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            try {
                // FileStorageService의 deleteFile 메서드 사용
                fileStorageService.deleteFile(fileUrl);
                log.info("파일 롤백 완료 - URL: {}", fileUrl);
            } catch (Exception e) {
                log.error("파일 롤백 실패 - URL: {}, 오류: {}", fileUrl, e.getMessage());
            }
        }
    }

    @Override
    public ListDTO<MemorialListResponseDTO> getMyMemorials(Member member, Pageable pageable) {
        log.info("사용자 접근 가능한 메모리얼 목록 조회 (페이징) - 사용자: {}", member.getId());

        // 기존: 소유한 메모리얼만 조회
        // Page<Memorial> memorialPage = memorialRepository.findByOwnerOrderByCreatedAtDesc(member, pageable);

        // 수정: 접근 가능한 모든 메모리얼 조회 (소유한 메모리얼 + 가족 구성원으로 참여한 메모리얼)
        Page<Memorial> memorialPage = memorialRepository.findAccessibleMemorialsByMember(member, pageable);

        Member memberWithImages = memberRepository.findByIdWithProfileImages(member.getId());
        boolean hasRequiredProfileImages = memberWithImages.hasRequiredProfileImages();

        // Page의 content를 DTO로 변환 (접근 권한 정보 포함)
        Page<MemorialListResponseDTO> dtoPage = memorialPage.map(memorial -> {
            // 현재 사용자가 소유자인지 확인
            boolean isOwner = memorial.getOwner().getId().equals(member.getId());

            // 가족 구성원으로서의 관계 정보 조회
            FamilyMember familyMember = null;
            if (! isOwner) {
                familyMember = memorial.getFamilyMember(member);
            }

            return convertToListResponseDTOWithAccessInfo(memorial, hasRequiredProfileImages, member, isOwner,
                familyMember);
        });

        // ListDTO.of 사용
        ListDTO<MemorialListResponseDTO> result = ListDTO.of(dtoPage);

        log.info("사용자 접근 가능한 메모리얼 목록 조회 완료 - 사용자: {}, 총 개수: {}, 페이지: {}/{}",
            member.getId(), result.getPagination().getTotalElements(),
            result.getPagination().getCurrentPage() + 1, result.getPagination().getTotalPages());

        return result;
    }

    @Override
    public MemorialListResponseDTO getMemorial(Long memorialId, Member member) {
        log.info("메모리얼 상세 조회 - ID: {}, 사용자: {}", memorialId, member.getId());

        Memorial memorial = memorialRepository.findByIdWithFiles(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 권한 확인
        if (! memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        Member memberWithImages = memberRepository.findByIdWithProfileImages(member.getId());

        boolean hasRequiredProfileImages = memberWithImages.hasRequiredProfileImages();

        return convertToListResponseDTO(memorial, hasRequiredProfileImages);
    }

    @Override
    @Transactional
    public void recordVisit(Long memorialId, Member member) {
        log.info("메모리얼 방문 기록 - ID: {}, 사용자: {}", memorialId, member.getId());

        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 권한 확인
        if (! memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        // 방문 기록
        memorial.recordVisit();
        memorialRepository.save(memorial);

        log.info("메모리얼 방문 기록 완료 - ID: {}, 총 방문 횟수: {}", memorialId, memorial.getTotalVisits());
    }

    /**
     * 사용자가 소유한 메모리얼 목록 조회 (비페이징) FamilyController에서 사용
     */
    @Override
    public List<Memorial> findByOwner(Member owner) {
        log.info("사용자 소유 메모리얼 목록 조회 - 사용자: {}", owner.getId());
        return memorialRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    /**
     * 메모리얼 ID로 조회 FamilyController에서 사용
     */
    @Override
    public Memorial findById(Long memorialId) {
        log.info("메모리얼 ID로 조회 - ID: {}", memorialId);
        return memorialRepository.findById(memorialId)
            .orElseThrow(() -> {
                log.warn("메모리얼을 찾을 수 없음 - ID: {}", memorialId);
                return new IllegalArgumentException("메모리얼을 찾을 수 없습니다.");
            });
    }

    /**
     * 사용자의 메모리얼 목록 조회 (API용, 비페이징) API 컨트롤러에서 사용
     */
    @Override
    public List<MemorialListResponseDTO> getMyMemorialsForApi(Member member) {

        log.info("사용자 접근 가능한 메모리얼 목록 조회 (API용) - 사용자: {}", member.getId());

        List<Memorial> memorials = memorialRepository.findAccessibleMemorialsByMember(member);

        Member memberWithImages = memberRepository.findByIdWithProfileImages(member.getId());
        boolean hasRequiredProfileImages = memberWithImages.hasRequiredProfileImages();

        return memorials.stream()
            .map(memorial -> convertToListResponseDTO(memorial, hasRequiredProfileImages, member))
            .collect(Collectors.toList());
    }

    /**
     * 메모리얼 엔티티 생성
     */
    private Memorial createMemorialEntity(MemorialCreateRequestDTO dto, Member member) {
        return Memorial.builder()
                .name(dto.getName())
                .nickname(dto.getNickname())
                .gender(dto.getGender())
                .relationship(dto.getRelationship())
                .birthDate(dto.getBirthDate())
                .deathDate(dto.getDeathDate())
                .description(dto.getDescription())
                .isPublic(dto.getIsPublic())
                .owner(member)
                .build();
    }

    /**
     * 프로필 이미지 파일 처리 - FileStorageService 사용
     */
    private List<String> processProfileImages(Memorial memorial, List<MultipartFile> profileImages, Long memberId) {
        if (profileImages == null || profileImages.size() != 5) {
            throw new APIException("프로필 이미지는 정확히 5개가 필요합니다.", ResponseStatus.PROFILE_IMAGE_LIMIT_EXCEEDED);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (int i = 0; i < profileImages.size(); i++) {
            MultipartFile file = profileImages.get(i);

            if (file.isEmpty()) {
                throw new APIException("빈 파일은 업로드할 수 없습니다.", ResponseStatus.FILE_EMPTY);
            }

            try {
                // FileStorageService를 사용하여 프로필 이미지 저장
                String fileUrl = fileStorageService.saveProfileImage(file, memberId, i + 1);
                uploadedUrls.add(fileUrl);

                // MemorialFile 엔티티 생성
                MemorialFile memorialFile = MemorialFile.createProfileImage(
                    memorial,
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    i + 1  // 정렬 순서
                );

                memorial.addFile(memorialFile);

                log.debug("프로필 이미지 파일 처리 완료 - 순서: {}, URL: {}", i + 1, fileUrl);

            } catch (Exception e) {
                log.error("프로필 이미지 업로드 실패 - 순서: {}, 파일: {}", i + 1, file.getOriginalFilename(), e);
                throw new APIException("프로필 이미지 업로드에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
            }
        }

        log.info("프로필 이미지 처리 완료 - 개수: {}", profileImages.size());
        return uploadedUrls;
    }

    /**
     * 음성 파일 처리 - FileStorageService 사용
     */
    private List<String> processVoiceFiles(Memorial memorial, List<MultipartFile> voiceFiles, Long memberId) {
        if (voiceFiles == null || voiceFiles.size() != 3) {
            throw new APIException("음성 파일은 정확히 3개가 필요합니다.", ResponseStatus.VOICE_FILE_LIMIT_EXCEEDED);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (int i = 0; i < voiceFiles.size(); i++) {
            MultipartFile file = voiceFiles.get(i);

            if (file.isEmpty()) {
                throw new APIException("빈 파일은 업로드할 수 없습니다.", ResponseStatus.FILE_EMPTY);
            }

            try {
                // FileStorageService를 사용하여 일반 파일 업로드 (StorageCategory.MEMORIAL 사용)
                String relativePath = fileStorageService.upload(file, StorageCategory.MEMORIAL, memberId);
                String fileUrl = fileStorageService.toAbsoluteUrl(relativePath);
                uploadedUrls.add(fileUrl);

                // MemorialFile 엔티티 생성
                MemorialFile memorialFile = MemorialFile.createVoiceFile(
                    memorial,
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    i + 1
                );

                memorial.addFile(memorialFile);

                log.debug("음성 파일 처리 완료 - 순서: {}, URL: {}", i + 1, fileUrl);

            } catch (Exception e) {
                log.error("음성 파일 업로드 실패 - 순서: {}, 파일: {}", i + 1, file.getOriginalFilename(), e);
                throw new APIException("음성 파일 업로드에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
            }
        }

        log.info("음성 파일 처리 완료 - 개수: {}", voiceFiles.size());
        return uploadedUrls;
    }

    /**
     * 영상 파일 처리 - FileStorageService 사용
     */
    private String processVideoFile(Memorial memorial, MultipartFile videoFile, Long memberId) {
        if (videoFile == null || videoFile.isEmpty()) {
            throw new APIException("영상 파일은 필수입니다.", ResponseStatus.FILE_EMPTY);
        }

        try {
            // FileStorageService를 사용하여 비디오 파일 업로드 (자동 변환 포함)
            String relativePath = fileStorageService.uploadVideo(videoFile, StorageCategory.MEMORIAL, memberId);
            String fileUrl = fileStorageService.toAbsoluteUrl(relativePath);

            // MemorialFile 엔티티 생성
            MemorialFile memorialFile = MemorialFile.createVideoFile(
                memorial,
                fileUrl,
                videoFile.getOriginalFilename(),
                videoFile.getSize(),
                videoFile.getContentType()
            );

            memorial.addFile(memorialFile);

            log.info("영상 파일 처리 완료 - 파일명: {}, URL: {}", videoFile.getOriginalFilename(), fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("영상 파일 업로드 실패 - 파일: {}", videoFile.getOriginalFilename(), e);
            throw new APIException("영상 파일 업로드에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Memorial 엔티티를 MemorialListResponseDTO로 변환 (접근 권한 정보 포함)
     */
    private MemorialListResponseDTO convertToListResponseDTO(Memorial memorial, boolean hasRequiredProfileImages,
        Member currentUser) {
        // 현재 사용자가 소유자인지 확인
        boolean isOwner = memorial.getOwner().getId().equals(currentUser.getId());

        // 가족 구성원으로서의 접근 권한 확인
        boolean canAccess = memorial.canBeViewedBy(currentUser);

        return MemorialListResponseDTO.builder()
            .memorialId(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .lastVisitAt(memorial.getLastVisitAt())
            .totalVisits(memorial.getTotalVisits())
            .memoryCount(memorial.getMemoryCount())
            .aiTrainingCompleted(memorial.getAiTrainingCompleted())
            .formattedAge(memorial.getFormattedAge())
            .relationshipDescription(memorial.getRelationship().getDisplayName())
            .canStartVideoCall(memorial.canStartVideoCall())
            .hasRequiredFiles(memorial.hasRequiredFiles())
            .hasRequiredProfileImages(hasRequiredProfileImages)
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .isOwner(isOwner)  // 소유자 여부 추가
            .canAccess(canAccess)  // 접근 권한 여부 추가
            .build();
    }

    /**
     * 기존 메서드와의 호환성을 위한 오버로드 메서드
     */
    private MemorialListResponseDTO convertToListResponseDTO(Memorial memorial, boolean hasRequiredProfileImages) {
        return MemorialListResponseDTO.builder()
            .memorialId(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .lastVisitAt(memorial.getLastVisitAt())
            .totalVisits(memorial.getTotalVisits())
            .memoryCount(memorial.getMemoryCount())
            .aiTrainingCompleted(memorial.getAiTrainingCompleted())
            .formattedAge(memorial.getFormattedAge())
            .relationshipDescription(memorial.getRelationship().getDisplayName())
            .canStartVideoCall(memorial.canStartVideoCall())
            .hasRequiredFiles(memorial.hasRequiredFiles())
            .hasRequiredProfileImages(hasRequiredProfileImages)
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .isOwner(true)  // 기존 로직에서는 소유자만 조회했으므로 true
            .canAccess(true)  // 기존 로직에서는 접근 가능한 것만 조회했으므로 true
            .build();
    }

    /**
     * 접근 권한 정보를 포함한 DTO 변환
     */
    private MemorialListResponseDTO convertToListResponseDTOWithAccessInfo(Memorial memorial,
        boolean hasRequiredProfileImages,
        Member currentUser,
        boolean isOwner,
        FamilyMember familyMember) {

        MemorialListResponseDTO.MemorialListResponseDTOBuilder builder = MemorialListResponseDTO.builder()
            .memorialId(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .lastVisitAt(memorial.getLastVisitAt())
            .totalVisits(memorial.getTotalVisits())
            .memoryCount(memorial.getMemoryCount())
            .aiTrainingCompleted(memorial.getAiTrainingCompleted())
            .formattedAge(memorial.getFormattedAge())
            .relationshipDescription(memorial.getRelationship().getDisplayName())
            .canStartVideoCall(memorial.canStartVideoCall())
            .hasRequiredFiles(memorial.hasRequiredFiles())
            .hasRequiredProfileImages(hasRequiredProfileImages)
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .isOwner(isOwner)
            .canAccess(true); // 조회 가능한 상태에서만 호출되므로 true

        // 소유자인 경우
        if (isOwner) {
            builder.accessType("OWNER")
                .canModify(true)
                .canVideoCall(true); // 소유자는 모든 권한 보유
        }
        // 가족 구성원인 경우
        else if (familyMember != null) {
            builder.accessType("FAMILY_MEMBER")
                .canModify(false)
                .canVideoCall(familyMember.getVideoCallAccess())
                .familyRelationship(familyMember.getRelationship().name())
                .familyRelationshipDisplay(familyMember.getRelationshipDisplayName());
        }

        return builder.build();
    }

    /**
     * 동적 질문 답변 처리 (간단한 버전)
     */
    private void processQuestionAnswers(Memorial memorial, List<MemorialQuestionAnswerDTO> questionAnswers, Member member) {
        if (questionAnswers == null || questionAnswers.isEmpty()) {
            log.warn("질문 답변이 없습니다 - 메모리얼: {}", memorial.getId());
            return;
        }

        log.info("질문 답변 처리 시작 - 메모리얼: {}, 답변 수: {}",
                memorial.getId(), questionAnswers.size());

        try {
            int savedCount = 0;

            for (MemorialQuestionAnswerDTO answerDTO : questionAnswers) {
                // 빈 답변 건너뛰기
                if (!answerDTO.hasAnswer()) {
                    continue;
                }

                // 질문 엔티티 조회
                MemorialQuestion question = memorialQuestionRepository.findById(answerDTO.getQuestionId())
                        .orElse(null);

                if (question == null) {
                    log.warn("존재하지 않는 질문 ID: {}", answerDTO.getQuestionId());
                    continue;
                }

                // 답변 엔티티 생성 및 저장
                MemorialAnswer answer = MemorialAnswer.createOwnerAnswer(
                        memorial, member, question, answerDTO.getTrimmedAnswer());

                memorialAnswerRepository.save(answer);
                savedCount++;

                log.debug("질문 답변 저장 완료 - 질문ID: {}, 답변 길이: {}자",
                        answerDTO.getQuestionId(), answerDTO.getAnswerLength());
            }

            log.info("질문 답변 처리 완료 - 메모리얼: {}, 저장된 답변 수: {}",
                    memorial.getId(), savedCount);

        } catch (Exception e) {
            log.error("질문 답변 처리 실패 - 메모리얼: {}, 오류: {}",
                    memorial.getId(), e.getMessage(), e);
            throw new APIException("질문 답변 처리에 실패했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}