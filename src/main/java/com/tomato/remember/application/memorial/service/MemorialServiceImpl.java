package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialCreateResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialListResponseDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.entity.MemorialFile;
import com.tomato.remember.application.memorial.code.MemorialFileType;
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

    @Override
    @Transactional
    public MemorialCreateResponseDTO createMemorial(
        MemorialCreateRequestDTO memorialData,
        List<MultipartFile> profileImages,
        List<MultipartFile> voiceFiles,
        MultipartFile videoFile,
        Member member) {

        log.info("메모리얼 생성 시작 - 사용자: {}, 메모리얼명: {}", member.getId(), memorialData.getName());

        List<String> uploadedFileUrls = new ArrayList<>();  // 업로드된 파일 URL 추적

        try {
            // 1. 메모리얼 엔티티 생성
            Memorial memorial = createMemorialEntity(memorialData, member);

            // 2. 프로필 이미지 파일 처리
            uploadedFileUrls.addAll(processProfileImages(memorial, profileImages, member.getId()));

            // 3. 음성 파일 처리
            uploadedFileUrls.addAll(processVoiceFiles(memorial, voiceFiles, member.getId()));

            // 4. 영상 파일 처리
            uploadedFileUrls.add(processVideoFile(memorial, videoFile, member.getId()));

            // 5. 메모리얼 저장
            Memorial savedMemorial = memorialRepository.save(memorial);

            log.info("메모리얼 생성 완료 - ID: {}, 이름: {}", savedMemorial.getId(), savedMemorial.getName());

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
        log.info("사용자 메모리얼 목록 조회 - 사용자: {}", member.getId());

        Page<Memorial> memorialPage = memorialRepository.findByOwnerOrderByCreatedAtDesc(member, pageable);

        // Page의 content를 DTO로 변환
        Page<MemorialListResponseDTO> dtoPage = memorialPage.map(this::convertToListResponseDTO);

        // ListDTO.of 사용
        return ListDTO.of(dtoPage);
    }

    @Override
    public MemorialListResponseDTO getMemorial(Long memorialId, Member member) {
        log.info("메모리얼 상세 조회 - ID: {}, 사용자: {}", memorialId, member.getId());

        Memorial memorial = memorialRepository.findByIdWithFiles(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 권한 확인
        if (!memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        return convertToListResponseDTO(memorial);
    }

    @Override
    @Transactional
    public void recordVisit(Long memorialId, Member member) {
        log.info("메모리얼 방문 기록 - ID: {}, 사용자: {}", memorialId, member.getId());

        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 권한 확인
        if (!memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        // 방문 기록
        memorial.recordVisit();
        memorialRepository.save(memorial);

        log.info("메모리얼 방문 기록 완료 - ID: {}, 총 방문 횟수: {}", memorialId, memorial.getTotalVisits());
    }

    /**
     * 사용자가 소유한 메모리얼 목록 조회 (비페이징)
     * FamilyController에서 사용
     */
    @Override
    public List<Memorial> findByOwner(Member owner) {
        log.info("사용자 소유 메모리얼 목록 조회 - 사용자: {}", owner.getId());
        return memorialRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    /**
     * 메모리얼 ID로 조회
     * FamilyController에서 사용
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
     * 사용자의 메모리얼 목록 조회 (API용, 비페이징)
     * API 컨트롤러에서 사용
     */
    @Override
    public List<MemorialListResponseDTO> getMyMemorialsForApi(Member member) {
        log.info("사용자 메모리얼 목록 조회 (API용) - 사용자: {}", member.getId());

        List<Memorial> memorials = memorialRepository.findByOwnerOrderByCreatedAtDesc(member);

        return memorials.stream()
                .map(this::convertToListResponseDTO)
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
            .personality(dto.getPersonality())
            .hobbies(dto.getFavoriteWords())  // favoriteWords -> hobbies
            .favoriteFood(dto.getFavoriteFoods())  // favoriteFoods -> favoriteFood
            .specialMemories(dto.getMemories())  // memories -> specialMemories
            .speechHabits(dto.getHabits())  // habits -> speechHabits
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
     * Memorial 엔티티를 MemorialListResponseDTO로 변환
     */
    private MemorialListResponseDTO convertToListResponseDTO(Memorial memorial) {
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
            .profileImageCount(memorial.getFileCount(MemorialFileType.PROFILE_IMAGE))
            .voiceFileCount(memorial.getFileCount(MemorialFileType.VOICE_FILE))
            .videoFileCount(memorial.getFileCount(MemorialFileType.VIDEO_FILE))
            .build();
    }
}