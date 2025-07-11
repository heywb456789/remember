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
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.exception.APIException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 메모리얼 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemorialServiceImpl implements MemorialService {

    private final MemorialRepository memorialRepository;

    @Value("${app.upload.path:/uploads}")
    private String uploadBasePath;

    @Value("${app.upload.url:/uploads}")
    private String uploadBaseUrl;

    @Override
    @Transactional
    public MemorialCreateResponseDTO createMemorial(
        MemorialCreateRequestDTO memorialData,
        List<MultipartFile> profileImages,
        List<MultipartFile> voiceFiles,
        MultipartFile videoFile,
        Member member) {

        log.info("메모리얼 생성 시작 - 사용자: {}, 메모리얼명: {}", member.getId(), memorialData.getName());

        try {
            // 1. 메모리얼 엔티티 생성
            Memorial memorial = createMemorialEntity(memorialData, member);

            // 2. 프로필 이미지 파일 처리
            processProfileImages(memorial, profileImages);

            // 3. 음성 파일 처리
            processVoiceFiles(memorial, voiceFiles);

            // 4. 영상 파일 처리
            processVideoFile(memorial, videoFile);

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
            throw new APIException(ResponseStatus.MEMORIAL_CREATION_FAILED);
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
        if (! memorial.canBeViewedBy(member)) {
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
        if (! memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        }

        // 방문 기록
        memorial.recordVisit();
        memorialRepository.save(memorial);

        log.info("메모리얼 방문 기록 완료 - ID: {}, 총 방문 횟수: {}", memorialId, memorial.getTotalVisits());
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
     * 프로필 이미지 파일 처리
     */
    private void processProfileImages(Memorial memorial, List<MultipartFile> profileImages) {
        if (profileImages == null || profileImages.size() != 5) {
            throw new APIException(ResponseStatus.PROFILE_IMAGE_LIMIT_EXCEEDED);
        }

        for (int i = 0; i < profileImages.size(); i++) {
            MultipartFile file = profileImages.get(i);

            if (file.isEmpty()) {
                throw new APIException(ResponseStatus.FILE_EMPTY);
            }

            // 파일 유효성 검사
            validateFile(file, MemorialFileType.PROFILE_IMAGE);

            // 파일 업로드 및 MemorialFile 생성 (실제 구현에서는 파일 저장 로직 필요)
            String fileUrl = uploadFile(file, MemorialFileType.PROFILE_IMAGE);

            MemorialFile memorialFile = MemorialFile.createProfileImage(
                memorial,
                fileUrl,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                i + 1  // 정렬 순서
            );

            memorial.addFile(memorialFile);
        }

        log.info("프로필 이미지 처리 완료 - 개수: {}", profileImages.size());
    }

    /**
     * 음성 파일 처리
     */
    private void processVoiceFiles(Memorial memorial, List<MultipartFile> voiceFiles) {
        if (voiceFiles == null || voiceFiles.size() != 3) {
            throw new APIException(ResponseStatus.VOICE_FILE_LIMIT_EXCEEDED);
        }

        for (int i = 0; i < voiceFiles.size(); i++) {
            MultipartFile file = voiceFiles.get(i);

            if (file.isEmpty()) {
                throw new APIException(ResponseStatus.FILE_EMPTY);
            }

            // 파일 유효성 검사
            validateFile(file, MemorialFileType.VOICE_FILE);

            // 파일 업로드 및 MemorialFile 생성
            String fileUrl = uploadFile(file, MemorialFileType.VOICE_FILE);

            MemorialFile memorialFile = MemorialFile.createVoiceFile(
                memorial,
                fileUrl,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                i + 1  // 정렬 순서
            );

            memorial.addFile(memorialFile);
        }

        log.info("음성 파일 처리 완료 - 개수: {}", voiceFiles.size());
    }

    /**
     * 영상 파일 처리
     */
    private void processVideoFile(Memorial memorial, MultipartFile videoFile) {
        if (videoFile == null || videoFile.isEmpty()) {
            throw new APIException(ResponseStatus.USER_IMAGE_LIMIT_EXCEEDED);
        }

        // 파일 유효성 검사
        validateFile(videoFile, MemorialFileType.VIDEO_FILE);

        // 파일 업로드 및 MemorialFile 생성
        String fileUrl = uploadFile(videoFile, MemorialFileType.VIDEO_FILE);

        MemorialFile memorialFile = MemorialFile.createVideoFile(
            memorial,
            fileUrl,
            videoFile.getOriginalFilename(),
            videoFile.getSize(),
            videoFile.getContentType()
        );

        memorial.addFile(memorialFile);

        log.info("영상 파일 처리 완료 - 파일명: {}", videoFile.getOriginalFilename());
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file, MemorialFileType fileType) {
        // 파일 크기 검사
        if (file.getSize() > fileType.getMaxFileSize()) {
            throw new APIException(ResponseStatus.TOO_MANY_FILES);
        }

        // 파일 타입 검사
        if (! fileType.isValidContentType(file.getContentType())) {
            throw new APIException(ResponseStatus.INVALID_FILE_TYPE);
        }
    }

    /**
     * 파일 업로드 (실제 구현에서는 클라우드 스토리지 등을 사용)
     * TODO: 실제 파일 저장 로직 구현 필요
     */
    private String uploadFile(MultipartFile file, MemorialFileType fileType) {
        try {
            // 1. 업로드 디렉토리 생성
            String uploadDir = createUploadDirectory(fileType);

            // 2. 고유한 파일명 생성
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());

            // 3. 파일 저장 경로 생성
            Path filePath = Paths.get(uploadDir, uniqueFileName);

            // 4. 파일 저장
            Files.copy(file.getInputStream(), filePath);

            // 5. 웹 접근 가능한 URL 반환
            String webUrl = String.format("%s/%s/%s/%s",
                uploadBaseUrl,
                fileType.name().toLowerCase(),
                getCurrentDatePath(),
                uniqueFileName);

            log.info("파일 업로드 완료 - 원본명: {}, 저장명: {}, URL: {}",
                file.getOriginalFilename(), uniqueFileName, webUrl);

            return webUrl;

        } catch (IOException e) {
            log.error("파일 업로드 실패 - 파일명: {}, 오류: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new APIException(ResponseStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 업로드 디렉토리 생성
     */
    private String createUploadDirectory(MemorialFileType fileType) throws IOException {
        // 업로드 경로: /uploads/{fileType}/{yyyy/MM/dd}/
        String datePath = getCurrentDatePath();
        String fullPath = String.format("%s/%s/%s",
            uploadBasePath,
            fileType.name().toLowerCase(),
            datePath);

        Path directory = Paths.get(fullPath);

        // 디렉토리가 없으면 생성
        if (! Files.exists(directory)) {
            Files.createDirectories(directory);
            log.info("업로드 디렉토리 생성: {}", directory.toString());
        }

        return directory.toString();
    }

    /**
     * 현재 날짜 기반 경로 생성 (yyyy/MM/dd)
     */
    private String getCurrentDatePath() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new APIException(ResponseStatus.FILE_EMPTY);
        }

        // 확장자 추출
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        // UUID + 타임스탬프 + 확장자로 고유한 파일명 생성
        String uniqueName = String.format("%s_%d%s",
            UUID.randomUUID().toString().replace("-", ""),
            System.currentTimeMillis(),
            extension);

        return uniqueName;
    }

    /**
     * 파일 삭제 (메모리얼 삭제 시 사용)
     */
    private void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.startsWith(uploadBaseUrl)) {
                // URL에서 실제 파일 경로 추출
                String relativePath = fileUrl.substring(uploadBaseUrl.length());
                Path filePath = Paths.get(uploadBasePath + relativePath);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("파일 삭제 완료: {}", filePath.toString());
                }
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패 - URL: {}, 오류: {}", fileUrl, e.getMessage(), e);
            // 파일 삭제 실패는 로그만 남기고 계속 진행
        }
    }

    /**
     * 디렉토리 용량 확인 (선택사항)
     */
    private void checkDiskSpace() {
        try {
            Path uploadPath = Paths.get(uploadBasePath);
            long freeSpace = Files.getFileStore(uploadPath).getUsableSpace();
            long freeSpaceGB = freeSpace / (1024 * 1024 * 1024);

            if (freeSpaceGB < 1) { // 1GB 미만인 경우 경고
                log.warn("디스크 용량 부족 - 남은 용량: {}GB", freeSpaceGB);
            }
        } catch (IOException e) {
            log.error("디스크 용량 확인 실패: {}", e.getMessage());
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