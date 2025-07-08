package com.tomato.remember.common.util;

import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.code.StorageCategory;
import com.tomato.remember.common.exception.APIException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class FileStorageService {

    @Value("${spring.app.upload.ftp.host}")
    private String ftpHost;

    @Value("${spring.app.upload.ftp.port:21}")
    private int ftpPort;

    @Value("${spring.app.upload.ftp.user}")
    private String ftpUser;

    @Value("${spring.app.upload.ftp.password}")
    private String ftpPassword;

    @Value("${spring.app.upload.root:/uploads}")
    private String uploadRoot;

    // 허용된 비디오 확장자 목록
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS =
        List.of("mp4", "mov", "avi", "wmv", "mkv", "webm", "flv", "mpg", "mpeg", "m4v");

    /**
     * MultipartFile 업로드
     */
    public String upload(MultipartFile file, StorageCategory category, Long postId) {
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        return storeFile(category, postId, filename, () -> file.getInputStream());
    }

    /**
     * 비디오 파일 변환 및 업로드
     * 비디오 파일을 검증하고, 필요시 MP4로 변환 후 업로드합니다.
     *
     * @param videoFile      업로드할 비디오 파일
     * @param category       파일 카테고리
     * @param postId         연관된 게시물 ID
     * @return               업로드된 파일의 상대 경로
     * @throws APIException  파일 처리 과정에서 오류 발생 시
     */
    public String uploadVideo(MultipartFile videoFile, StorageCategory category, Long postId) {
        // 1. 파일명 및 확장자 확인
        String originalFilename = videoFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        // 2. 확장자 검증
        String ext = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(ext)) {
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        // 3. 임시 파일 생성
        File tempFile;
        try {
            tempFile = File.createTempFile("upload-", "." + ext);
            videoFile.transferTo(tempFile);
        } catch (IOException e) {
            log.error("Failed to create temporary file", e);
            throw new APIException(ResponseStatus.FILE_UPLOAD_FAIL);
        }

        // 4. MP4가 아닌 경우 FFmpeg로 변환
        File finalFile;
        boolean needsConversion = !"mp4".equals(ext);

        if (needsConversion) {
            finalFile = convertToMp4(tempFile, ext);
            // 변환된 파일을 업로드
            try (InputStream in = java.nio.file.Files.newInputStream(finalFile.toPath())) {
                String filename = UUID.randomUUID() + ".mp4";
                String result = storeFile(category, postId, filename, () -> in);

                // 임시 파일들 정리
                tempFile.delete();
                finalFile.delete();

                return result;
            } catch (IOException e) {
                log.error("Failed to upload converted video file", e);
                // 임시 파일 정리
                tempFile.delete();
                if (finalFile.exists()) {
                    finalFile.delete();
                }
                throw new APIException(ResponseStatus.FILE_UPLOAD_FAIL);
            }
        } else {
            // MP4는 바로 업로드
            String filename = UUID.randomUUID() + ".mp4";
            String result = storeFile(category, postId, filename,
                () -> java.nio.file.Files.newInputStream(tempFile.toPath()));

            // 임시 파일 정리
            tempFile.delete();

            return result;
        }
    }

    /**
     * 비디오 파일을 MP4로 변환
     *
     * @param inputFile      입력 파일
     * @param sourceExt      소스 파일 확장자
     * @return               변환된 MP4 파일
     * @throws APIException  변환 실패 시
     * ffmpeg -y -i input.file -c:v libx264 -preset fast -c:a aac output.file
     * ffmpeg - 프로그램 자체를 호출합니다.
     *      -y - 출력 파일이 이미 존재할 경우 확인 없이 덮어씁니다(yes to overwrite).
     *      -i inputFile.getAbsolutePath() - 입력 파일을 지정합니다.
     *      -c:v libx264 - 비디오 코덱을 H.264로, 즉 비디오 스트림을 어떻게 인코딩할지를 지정합니다.
     *      -c:v는 video codec을 지정하는 플래그이며, libx264는 H.264 코덱 구현체입니다.
     *      H.264는 효율적인 압축률과 높은 호환성을 제공하는 인기 있는 비디오 코덱입니다.
     *      -preset fast - 인코딩 속도와 압축 품질 간의 균형을 조정합니다.
     *
     *      FFmpeg는 ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow 등의 프리셋을 제공합니다.
     *      fast는 빠른 인코딩 속도를 위해 어느 정도 품질을 희생합니다.
     *      속도가 빠를수록 품질이 낮아지고, 속도가 느릴수록 품질이 높아집니다.
     *
     *      -c:a aac - 오디오 코덱을 AAC로 지정합니다.
     *
     *      -c:a는 audio codec을 지정하는 플래그입니다.
     *      AAC(Advanced Audio Coding)는 MP3의 후속으로 설계된 디지털 오디오 압축 표준입니다.
     *      AAC는 효율적인 압축률과 높은 품질, 좋은 호환성을 제공합니다.
     */
    private File convertToMp4(File inputFile, String sourceExt) {
        File outputFile = new File(inputFile.getParent(), UUID.randomUUID() + ".mp4");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", inputFile.getAbsolutePath(),
                "-c:v", "libx264",
                "-preset", "fast",
                "-c:a", "aac",
                outputFile.getAbsolutePath()
            );

            Process process = pb.redirectErrorStream(true).start();

            // FFmpeg 출력 로깅
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[ffmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg process exited with code {}", exitCode);
                throw new APIException(ResponseStatus.FILE_UPLOAD_FAIL);
            }

            return outputFile;
        } catch (IOException | InterruptedException e) {
            log.error("Error during video conversion", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new APIException(ResponseStatus.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 순수 Base64 문자열 업로드 (data URI 형태도 지원)
     */
    public String uploadBase64Image(String base64Data,
                                    StorageCategory category,
                                    Long postId) {
        String dataPart;
        String ext = "png";
        if (base64Data.startsWith("data:")) {
            String[] parts = base64Data.split(",", 2);
            String meta = parts[0];      // ex) data:image/jpeg;base64
            dataPart = parts.length > 1 ? parts[1] : "";
            String mime = meta.substring(5, meta.indexOf(';'));
            switch (mime) {
                case "image/jpeg": ext = "jpg"; break;
                case "image/png":  ext = "png"; break;
                case "image/gif":  ext = "gif"; break;
                case "image/svg+xml": ext = "svg"; break;
            }
        } else {
            dataPart = base64Data;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(dataPart.getBytes());
        } catch (IllegalArgumentException e) {
            throw new UncheckedIOException(new IOException("유효하지 않은 Base64 데이터입니다.", e));
        }

        String filename = UUID.randomUUID() + "." + ext;
        return storeFile(category, postId, filename, () -> new ByteArrayInputStream(decoded));
    }

    /**
     * DTO 그대로 받아서 업로드 + URL 세팅
     */
//    public String uploadBase64Image(Base64ImageData imageData,
//                                    StorageCategory category,
//                                    Long postId) {
//        // 1) 확장자 지정 (DTO.imageType 예: "png", "jpeg")
//        String ext = imageData.getImageType();
//        if (ext == null || ext.isBlank()) {
//            // fallback: mime 추출 로직 재사용
//            ext = FilenameUtils.getExtension(imageData.getOriginalTag());
//            if (ext == null || ext.isBlank()) {
//                ext = "png";
//            }
//        }
//        // jpeg → jpg
//        if ("jpeg".equalsIgnoreCase(ext)) {
//            ext = "jpg";
//        }
//
//        // 2) 파일명
//        String filename = UUID.randomUUID() + "." + ext;
//
//        // 3) FTP 저장
//        String urlPath = storeFile(category, postId, filename,
//            () -> new ByteArrayInputStream(imageData.decodeBase64()));
//
//        // 4) DTO에 새 URL 세팅
//        imageData.setNewImageUrl(urlPath);
//        return urlPath;
//    }

    /**
     * FTP 삭제
     */
    public boolean delete(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            log.warn("Attempt to delete null or blank URL path");
            return false;
        }

        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(ftpHost, ftpPort);
            if (!ftp.login(ftpUser, ftpPassword)) {
                throw new IOException("FTP 로그인 실패: " + ftpUser);
            }
            ftp.enterLocalPassiveMode();
            boolean deleted = ftp.deleteFile(urlPath);
            if (deleted) {
                String dir = urlPath.substring(0, urlPath.lastIndexOf('/'));
                ftp.removeDirectory(dir);
            }
            ftp.logout();
            return deleted;
        } catch (IOException e) {
            log.error("FTP 파일 삭제 실패: " + urlPath, e);
            throw new UncheckedIOException("FTP 파일 삭제 실패: " + urlPath, e);
        } finally {
            if (ftp.isConnected()) {
                try { ftp.disconnect(); } catch (IOException ignored) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────────

    private String storeFile(StorageCategory category,
                             Long postId,
                             String filename,
                             StreamSupplier supplier) {
        String remoteDir  = String.join("/", uploadRoot, category.getFolder(), postId.toString());
        String remoteFile = remoteDir + "/" + filename;
        FTPClient ftp = new FTPClient();
        try (InputStream in = supplier.get()) {
            ftp.connect(ftpHost, ftpPort);
            if (!ftp.login(ftpUser, ftpPassword)) {
                throw new IOException("FTP 로그인 실패: " + ftpUser);
            }
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            // 디렉토리 생성
            createDirectoryHierarchy(ftp, remoteDir);

            // 파일 업로드
            if (!ftp.storeFile(remoteFile, in)) {
                throw new IOException("파일 전송 실패: " + remoteFile);
            }
            ftp.logout();
            return remoteFile;
        } catch (IOException e) {
            log.error("FTP 파일 저장 실패", e);
            throw new UncheckedIOException("FTP 파일 저장 실패", e);
        } finally {
            if (ftp.isConnected()) {
                try { ftp.disconnect(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * FTP 서버에 디렉토리 계층 구조 생성
     */
    private void createDirectoryHierarchy(FTPClient ftp, String remoteDir) throws IOException {
        String path = "";
        for (String dir : remoteDir.split("/")) {
            if (dir.isEmpty()) continue;
            path += "/" + dir;
            if (!ftp.changeWorkingDirectory(path) && !ftp.makeDirectory(path)) {
                throw new IOException("디렉토리 생성 실패: " + path);
            }
        }
    }

    @FunctionalInterface
    private interface StreamSupplier {
        InputStream get() throws IOException;
    }
}