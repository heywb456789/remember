package com.tomato.remember.application.memorial.entity;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 파일 업로드 기록 엔티티
 */
@Table(
    name = "t_file_upload_record",
    indexes = {
        @Index(name = "idx01_t_file_upload_record", columnList = "created_at"),
        @Index(name = "idx02_t_file_upload_record", columnList = "uploader_id"),
        @Index(name = "idx03_t_file_upload_record", columnList = "upload_id"),
        @Index(name = "idx04_t_file_upload_record", columnList = "file_url"),
        @Index(name = "idx05_t_file_upload_record", columnList = "file_type"),
        @Index(name = "idx06_t_file_upload_record", columnList = "is_deleted")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRecord extends Audit {

    @Comment("업로드 ID (UUID)")
    @Column(nullable = false, length = 36, name = "upload_id")
    private String uploadId;

    @Comment("원본 파일명")
    @Column(nullable = false, length = 255, name = "original_file_name")
    private String originalFileName;

    @Comment("저장된 파일명")
    @Column(nullable = false, length = 255, name = "file_name")
    private String fileName;

    @Comment("파일 저장 경로")
    @Column(nullable = false, columnDefinition = "TEXT", name = "file_path")
    private String filePath;

    @Comment("파일 접근 URL")
    @Column(nullable = false, columnDefinition = "TEXT", name = "file_url")
    private String fileUrl;

    @Comment("파일 크기 (bytes)")
    @Column(nullable = false, name = "file_size")
    private Long fileSize;

    @Comment("파일 MIME 타입")
    @Column(nullable = false, length = 100, name = "content_type")
    private String contentType;

    @Comment("파일 타입 (profileImages, voiceFiles, videoFile, userImage)")
    @Column(nullable = false, length = 50, name = "file_type")
    private String fileType;

    @Comment("업로드 일시")
    @Column(nullable = false, name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Comment("삭제 여부")
    @Column(nullable = false, name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Comment("삭제 일시")
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Comment("삭제 사유")
    @Column(length = 500, name = "delete_reason")
    private String deleteReason;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Member uploader;

    // 비즈니스 메서드
    
    /**
     * 파일을 삭제 상태로 마킹
     */
    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 파일을 삭제 상태로 마킹 (사유 포함)
     */
    public void markAsDeleted(String reason) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deleteReason = reason;
    }

    /**
     * 파일 삭제 상태 복원
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deleteReason = null;
    }

    /**
     * 파일 활성 상태 확인
     */
    public boolean isActive() {
        return !isDeleted;
    }

    /**
     * 파일 크기를 읽기 쉬운 형태로 반환
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 파일 확장자 반환
     */
    public String getFileExtension() {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 이미지 파일 여부 확인
     */
    public boolean isImageFile() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 오디오 파일 여부 확인
     */
    public boolean isAudioFile() {
        return contentType != null && contentType.startsWith("audio/");
    }

    /**
     * 비디오 파일 여부 확인
     */
    public boolean isVideoFile() {
        return contentType != null && contentType.startsWith("video/");
    }

    /**
     * 업로드 후 경과 시간 (분)
     */
    public long getMinutesSinceUpload() {
        if (uploadedAt == null) return 0;
        return java.time.Duration.between(uploadedAt, LocalDateTime.now()).toMinutes();
    }

    /**
     * 파일 타입 표시명 반환
     */
    public String getFileTypeDisplayName() {
        return switch (fileType) {
            case "profileImages" -> "프로필 이미지";
            case "voiceFiles" -> "음성 파일";
            case "videoFile" -> "영상 파일";
            case "userImage" -> "사용자 이미지";
            default -> "알 수 없음";
        };
    }
}