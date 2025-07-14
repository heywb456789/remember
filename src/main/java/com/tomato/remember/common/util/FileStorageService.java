package com.tomato.remember.common.util;

import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.code.StorageCategory;
import com.tomato.remember.common.exception.APIException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 파일 저장 서비스 (로컬 파일 시스템) - 단순화 버전
 * - 프로필 이미지, 동영상, Base64 이미지 등 모든 파일 타입 지원
 * - 환경별 local/dev 경로 제거로 단순화
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.upload-dir:/uploads}")
    private String uploadRoot;

    @Value("${app.file.base-url:http://localhost:8080}")
    private String baseUrl;

    // 허용된 이미지 확장자
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"
    );

    // 허용된 비디오 확장자
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "mov", "avi", "wmv", "mkv", "webm", "flv", "mpg", "mpeg", "m4v"
    );

    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // ===== 프로필 이미지 업로드 =====

    /**
     * 프로필 이미지 저장
     *
     * @param file 업로드할 파일
     * @param memberId 회원 ID
     * @param sortOrder 정렬 순서
     * @return 저장된 파일의 URL
     */
    public String saveProfileImage(MultipartFile file, Long memberId, Integer sortOrder) throws IOException {
        log.info("프로필 이미지 저장 시작 - 회원 ID: {}, 순서: {}, 파일: {}",
                memberId, sortOrder, file.getOriginalFilename());

        validateImageFile(file);

        // 프로필 이미지 전용 경로 생성 (단순화)
        String subDirectory = createProfileImagePath(memberId);
        Path uploadPath = Paths.get(uploadRoot, subDirectory);
        createDirectoryIfNotExists(uploadPath);

        // 파일명 생성
        String fileName = generateProfileImageFileName(memberId, sortOrder, file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);

        try {
            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL 생성 (단순화)
            String fileUrl = generateFileUrl(subDirectory, fileName);

            log.info("프로필 이미지 저장 완료 - 경로: {}, URL: {}", filePath, fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("프로필 이미지 저장 실패 - 경로: {}", filePath, e);
            throw new APIException("파일 저장에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        }
    }

    // ===== 범용 파일 업로드 (기존 로직 통합) =====

    /**
     * MultipartFile 업로드 (범용)
     *
     * @param file 업로드할 파일
     * @param category 저장 카테고리
     * @param postId 게시물 ID
     * @return 저장된 파일의 상대 경로
     */
    public String upload(MultipartFile file, StorageCategory category, Long postId) {
        validateFile(file);

        String ext = getFileExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;

        return storeFile(category, postId, filename, () -> file.getInputStream());
    }

    /**
     * 비디오 파일 변환 및 업로드
     * 비디오 파일을 검증하고, 필요시 MP4로 변환 후 업로드합니다.
     *
     * @param videoFile 업로드할 비디오 파일
     * @param category 파일 카테고리
     * @param postId 연관된 게시물 ID
     * @return 업로드된 파일의 상대 경로
     * @throws APIException 파일 처리 과정에서 오류 발생 시
     */
    public String uploadVideo(MultipartFile videoFile, StorageCategory category, Long postId) {
        log.info("비디오 파일 업로드 시작 - 카테고리: {}, 게시물 ID: {}, 파일: {}",
                category, postId, videoFile.getOriginalFilename());

        // 파일명 및 확장자 확인
        String originalFilename = videoFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new APIException("파일명이 없습니다.", ResponseStatus.BAD_REQUEST);
        }

        // 확장자 검증
        String ext = getFileExtension(originalFilename);
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(ext)) {
            throw new APIException("지원하지 않는 비디오 형식입니다. 지원 형식: " + String.join(", ", ALLOWED_VIDEO_EXTENSIONS), ResponseStatus.INVALID_FILE_TYPE);
        }

        // 임시 파일 생성
        File tempFile;
        try {
            tempFile = File.createTempFile("upload-", "." + ext);
            videoFile.transferTo(tempFile);
        } catch (IOException e) {
            log.error("임시 파일 생성 실패", e);
            throw new APIException("임시 파일 생성에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        }

        try {
            // MP4가 아닌 경우 FFmpeg로 변환
            File finalFile;
            boolean needsConversion = !"mp4".equals(ext);

            if (needsConversion) {
                finalFile = convertToMp4(tempFile, ext);
                String filename = UUID.randomUUID() + ".mp4";
                String result = storeFile(category, postId, filename,
                        () -> Files.newInputStream(finalFile.toPath()));

                // 변환된 파일 정리
                finalFile.delete();
                log.info("비디오 변환 및 업로드 완료 - 원본: {}, 결과: {}", ext, result);
                return result;
            } else {
                // MP4는 바로 업로드
                String filename = UUID.randomUUID() + ".mp4";
                String result = storeFile(category, postId, filename,
                        () -> Files.newInputStream(tempFile.toPath()));

                log.info("비디오 업로드 완료 - 파일: {}", result);
                return result;
            }
        } catch (Exception e) {
            log.error("비디오 파일 처리 실패", e);
            throw new APIException("비디오 파일 처리에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        } finally {
            // 임시 파일 정리
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Base64 이미지 업로드
     *
     * @param base64Data Base64 인코딩된 이미지 데이터 (data URI 형태도 지원)
     * @param category 파일 카테고리
     * @param postId 게시물 ID
     * @return 저장된 파일의 상대 경로
     */
    public String uploadBase64Image(String base64Data, StorageCategory category, Long postId) {
        log.info("Base64 이미지 업로드 시작 - 카테고리: {}, 게시물 ID: {}", category, postId);

        String dataPart;
        String ext = "png"; // 기본값

        // data URI 형태인지 확인 (data:image/jpeg;base64,...)
        if (base64Data.startsWith("data:")) {
            String[] parts = base64Data.split(",", 2);
            if (parts.length < 2) {
                throw new APIException("올바르지 않은 data URI 형식입니다.", ResponseStatus.BAD_REQUEST);
            }

            String meta = parts[0]; // data:image/jpeg;base64
            dataPart = parts[1];

            // MIME 타입에서 확장자 추출
            if (meta.contains("image/")) {
                String mime = meta.substring(meta.indexOf("image/") + 6, meta.indexOf(';'));
                switch (mime) {
                    case "jpeg": ext = "jpg"; break;
                    case "png": ext = "png"; break;
                    case "gif": ext = "gif"; break;
                    case "webp": ext = "webp"; break;
                    case "svg+xml": ext = "svg"; break;
                    default: ext = "png"; break;
                }
            }
        } else {
            dataPart = base64Data;
        }

        // Base64 디코딩
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(dataPart.trim());
        } catch (IllegalArgumentException e) {
            throw new APIException("유효하지 않은 Base64 데이터입니다.", ResponseStatus.BAD_REQUEST);
        }

        String filename = UUID.randomUUID() + "." + ext;
        String result = storeFile(category, postId, filename, () -> new ByteArrayInputStream(decoded));

        log.info("Base64 이미지 업로드 완료 - 파일: {}", result);
        return result;
    }

    // ===== 파일 삭제 =====

    /**
     * 파일 삭제 (URL 기반)
     *
     * @param fileUrl 삭제할 파일의 URL
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            log.warn("삭제할 파일 URL이 비어있습니다.");
            return false;
        }

        try {
            // URL에서 파일 경로 추출 (단순화)
            String relativePath = extractRelativePathFromUrl(fileUrl);
            Path filePath = Paths.get(uploadRoot, relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 완료 - 경로: {}", filePath);

                // 빈 디렉토리 정리 시도
                cleanupEmptyDirectories(filePath.getParent());
                return true;
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다 - 경로: {}", filePath);
                return false;
            }

        } catch (Exception e) {
            log.error("파일 삭제 실패 - URL: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * 파일 삭제 (상대 경로 기반)
     *
     * @param relativePath 상대 경로 (예: profile/2024/01/123/file.jpg)
     * @return 삭제 성공 여부
     */
    public boolean delete(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            log.warn("삭제할 파일 경로가 비어있습니다.");
            return false;
        }

        try {
            Path filePath = Paths.get(uploadRoot, relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 완료 - 경로: {}", filePath);

                // 빈 디렉토리 정리 시도
                cleanupEmptyDirectories(filePath.getParent());
                return true;
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다 - 경로: {}", filePath);
                return false;
            }

        } catch (Exception e) {
            log.error("파일 삭제 실패 - 경로: {}", relativePath, e);
            return false;
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 범용 파일 저장 로직
     */
    private String storeFile(StorageCategory category, Long postId, String filename, StreamSupplier supplier) {
        try {
            // 저장 경로 생성
            String subDirectory = createDirectoryPath(category, postId);
            Path uploadPath = Paths.get(uploadRoot, subDirectory);
            createDirectoryIfNotExists(uploadPath);

            // 파일 저장
            Path filePath = uploadPath.resolve(filename);
            try (InputStream inputStream = supplier.get()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 상대 경로 반환 (URL 생성용)
            String relativePath = subDirectory + "/" + filename;

            log.info("파일 저장 완료 - 경로: {}, 상대경로: {}", filePath, relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("파일 저장 실패 - 카테고리: {}, ID: {}, 파일명: {}", category, postId, filename, e);
            throw new APIException("파일 저장에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 비디오 파일을 MP4로 변환
     * FFmpeg를 사용하여 다양한 비디오 형식을 MP4로 변환
     */
    private File convertToMp4(File inputFile, String sourceExt) {
        File outputFile = new File(inputFile.getParent(), UUID.randomUUID() + ".mp4");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",                    // 기존 파일 덮어쓰기
                    "-i", inputFile.getAbsolutePath(), // 입력 파일
                    "-c:v", "libx264",                 // 비디오 코덱: H.264
                    "-preset", "fast",                 // 인코딩 속도 설정
                    "-c:a", "aac",                     // 오디오 코덱: AAC
                    outputFile.getAbsolutePath()       // 출력 파일
            );

            Process process = pb.redirectErrorStream(true).start();

            // FFmpeg 출력 로깅
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[ffmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 프로세스 실패 - 종료 코드: {}", exitCode);
                throw new APIException("비디오 변환에 실패했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
            }

            log.info("비디오 변환 완료 - 입력: {}, 출력: {}", inputFile.getName(), outputFile.getName());
            return outputFile;

        } catch (IOException | InterruptedException e) {
            log.error("비디오 변환 중 오류 발생", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new APIException("비디오 변환 중 오류가 발생했습니다.", ResponseStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 카테고리별 저장 경로 생성 (단순화)
     */
    private String createDirectoryPath(StorageCategory category, Long postId) {
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));

        return String.format("%s/%s/%d", category.getFolder(), yearMonth, postId);
    }

    /**
     * 프로필 이미지 저장 경로 생성 (단순화)
     */
    private String createProfileImagePath(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return String.format("profile/%s/%d", yearMonth, memberId);
    }

    /**
     * 프로필 이미지 파일명 생성
     */
    private String generateProfileImageFileName(Long memberId, Integer sortOrder, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("profile_%d_%d_%s_%s.%s",
                memberId, sortOrder, timestamp, uuid, extension);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "jpg";

        String ext = FilenameUtils.getExtension(filename);
        return ext.isEmpty() ? "jpg" : ext.toLowerCase();
    }

    /**
     * 디렉토리 생성
     */
    private void createDirectoryIfNotExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.debug("디렉토리 생성: {}", path);
        }
    }

    /**
     * 파일 URL 생성 (단순화)
     */
    private String generateFileUrl(String subDirectory, String fileName) {
        return String.format("%s/uploads/%s/%s", baseUrl, subDirectory, fileName);
    }

    /**
     * 상대 경로를 절대 URL로 변환 (단순화)
     */
    public String toAbsoluteUrl(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        return String.format("%s/uploads/%s", baseUrl, relativePath);
    }

    /**
     * URL에서 상대 경로 추출 (단순화)
     */
    private String extractRelativePathFromUrl(String fileUrl) {
        String uploadsPrefix = "/uploads/";
        int index = fileUrl.indexOf(uploadsPrefix);

        if (index == -1) {
            throw new IllegalArgumentException("올바르지 않은 파일 URL입니다: " + fileUrl);
        }

        return fileUrl.substring(index + uploadsPrefix.length());
    }

    /**
     * 빈 디렉토리 정리 (상위 디렉토리까지 재귀적으로)
     */
    private void cleanupEmptyDirectories(Path directory) {
        try {
            if (directory == null || !Files.exists(directory)) {
                return;
            }

            // 디렉토리가 비어있고, 업로드 루트가 아닌 경우 삭제
            Path uploadRootPath = Paths.get(uploadRoot);
            if (Files.isDirectory(directory) &&
                    isDirEmpty(directory) &&
                    !directory.equals(uploadRootPath)) {

                Files.delete(directory);
                log.debug("빈 디렉토리 삭제: {}", directory);

                // 상위 디렉토리도 확인
                cleanupEmptyDirectories(directory.getParent());
            }
        } catch (IOException e) {
            log.debug("디렉토리 정리 중 오류 (무시됨): {}", e.getMessage());
        }
    }

    /**
     * 디렉토리가 비어있는지 확인
     */
    private boolean isDirEmpty(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return !stream.findAny().isPresent();
        }
    }

    /**
     * 파일 유효성 검사 (범용)
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new APIException("파일을 선택해주세요.", ResponseStatus.FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new APIException("파일 크기는 " + formatFileSize(MAX_FILE_SIZE) + " 이하여야 합니다.", ResponseStatus.FILE_SIZE_EXCEEDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new APIException("올바르지 않은 파일명입니다.", ResponseStatus.BAD_REQUEST);
        }
    }

    /**
     * 이미지 파일 유효성 검사
     */
    private void validateImageFile(MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType();
        if (!isSupportedImageFormat(contentType)) {
            throw new APIException("지원하지 않는 이미지 형식입니다. 지원 형식: JPEG, PNG, WebP, GIF", ResponseStatus.INVALID_FILE_TYPE);
        }

        String ext = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new APIException("지원하지 않는 이미지 확장자입니다. 지원 확장자: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS), ResponseStatus.INVALID_FILE_TYPE);
        }
    }

    /**
     * 지원되는 이미지 형식인지 확인
     */
    private boolean isSupportedImageFormat(String contentType) {
        if (contentType == null) return false;

        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/bmp") ||
                contentType.equals("image/svg+xml");
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.1f %s",
                size / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    /**
     * 함수형 인터페이스 - InputStream 공급자
     */
    @FunctionalInterface
    private interface StreamSupplier {
        InputStream get() throws IOException;
    }
}