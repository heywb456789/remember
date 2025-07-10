package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.FileUploadResponseDTO;
import com.tomato.remember.application.memorial.entity.FileUploadRecord;
import com.tomato.remember.application.memorial.repository.FileUploadRecordRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.code.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 파일 업로드 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileUploadServiceImpl implements FileUploadService {

    private final FileUploadRecordRepository fileUploadRecordRepository;

    @Value("${app.file.upload.base-path:/tmp/uploads}")
    private String uploadBasePath;

    @Value("${app.file.upload.base-url:http://localhost:8080/files}")
    private String uploadBaseUrl;

    // 파일 타입별 제한 설정
    private static final Map<String, FileTypeConfig> FILE_TYPE_CONFIGS = Map.of(
        "profileImages", new FileTypeConfig(5 * 1024 * 1024, List.of("image/jpeg", "image/png", "image/jpg"), 5),
        "voiceFiles", new FileTypeConfig(50 * 1024 * 1024, List.of("audio/mp3", "audio/wav", "audio/m4a", "audio/mpeg"), 3),
        "videoFile", new FileTypeConfig(100 * 1024 * 1024, List.of("video/mp4", "video/mov", "video/avi", "video/quicktime"), 1),
        "userImage", new FileTypeConfig(3 * 1024 * 1024, List.of("image/jpeg", "image/png", "image/jpg"), 1)
    );

    @Override
    public FileUploadResponseDTO uploadFile(MultipartFile file, String fileType, Member member) {
        log.info("Uploading file: {} (type: {}) for user: {} (ID: {})", 
                file.getOriginalFilename(), fileType, member.getName(), member.getId());

        try {
            // 파일 유효성 검사
            validateFile(file, fileType);

            // 업로드 ID 생성
            String uploadId = UUID.randomUUID().toString();
            
            // 파일명 생성
            String fileName = generateFileName(file.getOriginalFilename(), uploadId);
            
            // 파일 저장
            String filePath = saveFile(file, fileName, fileType);
            
            // 파일 URL 생성
            String fileUrl = generateFileUrl(filePath, fileType);

            // 업로드 기록 저장
            FileUploadRecord record = FileUploadRecord.builder()
                    .uploadId(uploadId)
                    .originalFileName(file.getOriginalFilename())
                    .fileName(fileName)
                    .filePath(filePath)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .fileType(fileType)
                    .uploader(member)
                    .uploadedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            fileUploadRecordRepository.save(record);

            // 응답 DTO 생성
            FileUploadResponseDTO response = FileUploadResponseDTO.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadId(uploadId)
                    .build();

            log.info("File uploaded successfully: uploadId={}, fileUrl={}", uploadId, fileUrl);
            return response;

        } catch (Exception e) {
            log.error("File upload failed: {}", file.getOriginalFilename(), e);
            throw new APIException(ResponseStatus.FILE_UPLOAD_FAILED, e.getMessage());
        }
    }

    @Override
    public List<FileUploadResponseDTO> uploadFiles(List<MultipartFile> files, String fileType, Member member) {
        log.info("Uploading {} files (type: {}) for user: {} (ID: {})", 
                files.size(), fileType, member.getName(), member.getId());

        // 파일 개수 제한 확인
        FileTypeConfig config = FILE_TYPE_CONFIGS.get(fileType);
        if (config != null && files.size() > config.maxCount) {
            throw new APIException(ResponseStatus.FILE_COUNT_EXCEEDED, 
                    String.format("최대 %d개 파일까지 업로드할 수 있습니다.", config.maxCount));
        }

        List<FileUploadResponseDTO> results = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                FileUploadResponseDTO result = uploadFile(file, fileType, member);
                results.add(result);
            } catch (Exception e) {
                log.error("Individual file upload failed: {}", file.getOriginalFilename(), e);
                failedFiles.add(file.getOriginalFilename());
            }
        }

        if (!failedFiles.isEmpty()) {
            log.warn("Some files failed to upload: {}", failedFiles);
            // 부분 실패시에도 성공한 파일들은 반환
        }

        log.info("Multiple file upload completed: {} successful, {} failed", 
                results.size(), failedFiles.size());
        return results;
    }

    @Override
    public void deleteFile(String fileUrl, Member member) {
        log.info("Deleting file: {} for user: {} (ID: {})", fileUrl, member.getName(), member.getId());

        try {
            // 업로드 기록 조회
            FileUploadRecord record = fileUploadRecordRepository.findByFileUrlAndIsDeletedFalse(fileUrl)
                    .orElseThrow(() -> new APIException(ResponseStatus.FILE_NOT_FOUND));

            // 권한 확인 (업로드한 사용자만 삭제 가능)
            if (!record.getUploader().getId().equals(member.getId())) {
                throw new APIException(ResponseStatus.FILE_ACCESS_DENIED);
            }

            // 물리적 파일 삭제
            try {
                Path filePath = Paths.get(record.getFilePath());
                Files.deleteIfExists(filePath);
                log.debug("Physical file deleted: {}", record.getFilePath());
            } catch (IOException e) {
                log.warn("Failed to delete physical file: {}", record.getFilePath(), e);
                // 물리적 파일 삭제 실패해도 논리적 삭제는 진행
            }

            // 논리적 삭제 (소프트 삭제)
            record.markAsDeleted();
            fileUploadRecordRepository.save(record);

            log.info("File deleted successfully: {}", fileUrl);

        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("File deletion failed: {}", fileUrl, e);
            throw new APIException(ResponseStatus.FILE_DELETE_FAILED, e.getMessage());
        }
    }

    @Override
    public boolean existsFile(String fileUrl) {
        return fileUploadRecordRepository.findByFileUrlAndIsDeletedFalse(fileUrl).isPresent();
    }

    @Override
    public long getFileSize(String fileUrl) {
        return fileUploadRecordRepository.findByFileUrlAndIsDeletedFalse(fileUrl)
                .map(FileUploadRecord::getFileSize)
                .orElse(0L);
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file, String fileType) {
        if (file.isEmpty()) {
            throw new APIException(ResponseStatus.FILE_EMPTY);
        }

        FileTypeConfig config = FILE_TYPE_CONFIGS.get(fileType);
        if (config == null) {
            throw new APIException(ResponseStatus.FILE_TYPE_NOT_SUPPORTED);
        }

        // 파일 크기 확인
        if (file.getSize() > config.maxSize) {
            double maxSizeMB = config.maxSize / (1024.0 * 1024.0);
            throw new APIException(ResponseStatus.FILE_SIZE_EXCEEDED, 
                    String.format("파일 크기가 %.1fMB를 초과합니다.", maxSizeMB));
        }

        // 파일 타입 확인
        String contentType = file.getContentType();
        if (contentType == null || !config.allowedTypes.contains(contentType.toLowerCase())) {
            throw new APIException(ResponseStatus.FILE_TYPE_NOT_ALLOWED, 
                    String.format("지원하지 않는 파일 형식입니다. 허용된 형식: %s", config.allowedTypes));
        }

        // 파일 확장자 확인
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename, fileType)) {
            throw new APIException(ResponseStatus.FILE_EXTENSION_NOT_ALLOWED);
        }
    }

    /**
     * 파일 확장자 유효성 검사
     */
    private boolean hasValidExtension(String filename, String fileType) {
        String extension = getFileExtension(filename).toLowerCase();
        
        return switch (fileType) {
            case "profileImages", "userImage" -> 
                List.of("jpg", "jpeg", "png").contains(extension);
            case "voiceFiles" -> 
                List.of("mp3", "wav", "m4a").contains(extension);
            case "videoFile" -> 
                List.of("mp4", "mov", "avi").contains(extension);
            default -> false;
        };
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 파일명 생성
     */
    private String generateFileName(String originalFilename, String uploadId) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        return String.format("%s_%s.%s", uploadId, timestamp, extension);
    }

    /**
     * 파일 저장
     */
    private String saveFile(MultipartFile file, String fileName, String fileType) throws IOException {
        // 저장 경로 생성
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadDir = Paths.get(uploadBasePath, fileType, datePath);
        
        // 디렉토리 생성
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 파일 저장
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    /**
     * 파일 URL 생성
     */
    private String generateFileUrl(String filePath, String fileType) {
        // 상대 경로 생성
        String relativePath = filePath.replace(uploadBasePath, "").replace("\\", "/");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        return String.format("%s/%s", uploadBaseUrl, relativePath);
    }

    /**
     * 파일 타입별 설정 클래스
     */
    private static class FileTypeConfig {
        final long maxSize;
        final List<String> allowedTypes;
        final int maxCount;

        FileTypeConfig(long maxSize, List<String> allowedTypes, int maxCount) {
            this.maxSize = maxSize;
            this.allowedTypes = allowedTypes;
            this.maxCount = maxCount;
        }
    }
}