package com.tomato.remember.application.member.entity;

import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

/**
 * 회원 AI 프로필 이미지 엔티티
 * - 영상통화용 AI 학습을 위한 프로필 사진 관리
 * - 회원당 최대 5장까지 저장
 */
@Table(
    name = "t_member_ai_profile_image",
    indexes = {
        @Index(name = "idx01_t_member_ai_profile_image", columnList = "member_id, sort_order"),
        @Index(name = "idx02_t_member_ai_profile_image", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk01_t_member_ai_profile_image", columnNames = {"member_id", "sort_order"})
    }
)
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAiProfileImage extends Audit {

    // ===== 연관관계 =====

    @Comment("회원")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // ===== 파일 정보 =====

    @Comment("이미지 URL")
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Comment("정렬 순서 (1-5)")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Comment("파일 크기 (bytes)")
    @Column(name = "file_size")
    private Long fileSize;

    @Comment("원본 파일명")
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Comment("파일 MIME 타입")
    @Column(name = "content_type", length = 100)
    private String contentType;

    // ===== AI 학습 관련 =====

    @Comment("AI 학습 완료 여부")
    @Column(name = "ai_processed", nullable = false)
    @Builder.Default
    private Boolean aiProcessed = false;

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
     * AI 처리 완료 표시
     */
    public void markAsAiProcessed() {
        this.aiProcessed = true;
    }

    /**
     * AI 처리 미완료 표시
     */
    public void markAsAiPending() {
        this.aiProcessed = false;
    }

    /**
     * 유효한 프로필 이미지인지 확인 (AI 학습 완료 여부로만 판단)
     */
    public boolean isValid() {
        return aiProcessed;
    }

    // ===== 팩토리 메서드 =====

    /**
     * 새 프로필 이미지 생성
     */
    public static MemberAiProfileImage create(Member member, String imageUrl, Integer sortOrder,
                                            String originalFilename, Long fileSize, String contentType) {
        return MemberAiProfileImage.builder()
            .member(member)
            .imageUrl(imageUrl)
            .sortOrder(sortOrder)
            .originalFilename(originalFilename)
            .fileSize(fileSize)
            .contentType(contentType)
            .aiProcessed(true)  // 기본값: AI 처리 안됨 TODO: 임시로 true
            .build();
    }

    // ===== 연관관계 편의 메서드 =====

    /**
     * 연관관계 설정
     */
    public void setMember(Member member) {
        this.member = member;
        if (member != null && !member.getProfileImages().contains(this)) {
            member.getProfileImages().add(this);
        }
    }
}