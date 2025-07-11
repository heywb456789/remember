package com.tomato.remember.application.memorial.entity;

import com.tomato.remember.application.memorial.code.MemorialFileType;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

/**
 * 메모리얼 파일 엔티티
 */
@Table(
    name = "t_memorial_file",
    indexes = {
        @Index(name = "idx01_t_memorial_file", columnList = "memorial_id, file_type"),
        @Index(name = "idx02_t_memorial_file", columnList = "memorial_id, file_type, sort_order")
    }
)
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialFile extends Audit {

    @Comment("메모리얼 ID")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;

    @Comment("파일 타입")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "file_type")
    private MemorialFileType fileType;

    @Comment("파일 URL")
    @Column(nullable = false, columnDefinition = "TEXT", name = "file_url")
    private String fileUrl;

    @Comment("원본 파일명")
    @Column(nullable = false, length = 255, name = "original_filename")
    private String originalFilename;

    @Comment("파일 크기 (bytes)")
    @Column(nullable = false, name = "file_size")
    private Long fileSize;

    @Comment("파일 MIME 타입")
    @Column(nullable = false, length = 100, name = "content_type")
    private String contentType;

    @Comment("정렬 순서")
    @Column(nullable = false, name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Comment("파일 설명")
    @Column(length = 500)
    private String description;

    // ===== 헬퍼 메서드 =====

    /**
     * 파일 크기를 읽기 쉬운 형태로 반환
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 파일 확장자 반환
     */
    public String getFileExtension() {
        if (originalFilename == null) return "";
        
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1) return "";
        
        return originalFilename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 이미지 파일인지 확인
     */
    public boolean isImageFile() {
        return fileType == MemorialFileType.PROFILE_IMAGE;
    }

    /**
     * 오디오 파일인지 확인
     */
    public boolean isAudioFile() {
        return fileType == MemorialFileType.VOICE_FILE;
    }

    /**
     * 비디오 파일인지 확인
     */
    public boolean isVideoFile() {
        return fileType == MemorialFileType.VIDEO_FILE;
    }

    /**
     * 파일 생성
     */
    public static MemorialFile createProfileImage(Memorial memorial, String fileUrl, 
                                                  String originalFilename, Long fileSize, 
                                                  String contentType, Integer sortOrder) {
        return MemorialFile.builder()
            .memorial(memorial)
            .fileType(MemorialFileType.PROFILE_IMAGE)
            .fileUrl(fileUrl)
            .originalFilename(originalFilename)
            .fileSize(fileSize)
            .contentType(contentType)
            .sortOrder(sortOrder)
            .build();
    }

    public static MemorialFile createVoiceFile(Memorial memorial, String fileUrl, 
                                               String originalFilename, Long fileSize, 
                                               String contentType, Integer sortOrder) {
        return MemorialFile.builder()
            .memorial(memorial)
            .fileType(MemorialFileType.VOICE_FILE)
            .fileUrl(fileUrl)
            .originalFilename(originalFilename)
            .fileSize(fileSize)
            .contentType(contentType)
            .sortOrder(sortOrder)
            .build();
    }

    public static MemorialFile createVideoFile(Memorial memorial, String fileUrl, 
                                               String originalFilename, Long fileSize, 
                                               String contentType) {
        return MemorialFile.builder()
            .memorial(memorial)
            .fileType(MemorialFileType.VIDEO_FILE)
            .fileUrl(fileUrl)
            .originalFilename(originalFilename)
            .fileSize(fileSize)
            .contentType(contentType)
            .sortOrder(0)
            .build();
    }
}