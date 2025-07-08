package com.tomato.naraclub.application.member.entity;

import com.tomato.naraclub.application.member.code.ActivityReviewStage;
import com.tomato.naraclub.application.member.dto.MemberActivityResponse;
import com.tomato.naraclub.common.audit.Audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.entity
 * @fileName : MemberActivity
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Table(
    name = "t_member_activity",
    indexes = {
        @Index(name = "idx01_t_member_activity_created_at", columnList = "created_at"),   // 최신순 정렬용
        @Index(name = "idx02_t_member_activity_author",     columnList = "author_id"),    // author 조회용
        @Index(name = "idx03_t_member_activity_stage",      columnList = "stage"),        // 심사 단계별 조회용
        @Index(name = "idx04_t_member_activity_deleted",    columnList = "is_deleted")    // 삭제 여부 필터링용
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivity extends Audit {

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @Comment("제목")
    @Column(nullable = false, length = 200)
    private String title;

    @Comment("링크")
    @Column(nullable = false, length = 2000)
    private String shareLink;

    @Comment("심사 단계")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityReviewStage stage;

    @Comment("거절시 등록하는 사유")
    private String reason;

    @Comment("삭제여부")
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    public MemberActivityResponse convertDTO() {
        return MemberActivityResponse.builder()
            .activityId(id)
            .memberId(author.getId())
            .title(title)
            .shareLink(shareLink)
            .stage(stage)
            .isDeleted(isDeleted)
            .createdAt(createdAt)
            .build();
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void setStage(ActivityReviewStage activityReviewStage) {
        this.stage = activityReviewStage;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
