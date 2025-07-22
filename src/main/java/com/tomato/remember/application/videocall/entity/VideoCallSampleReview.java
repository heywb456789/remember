package com.tomato.remember.application.videocall.entity;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.videocall.entity
 * @fileName : VideoCallSampleReview
 * @date : 2025-07-21
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Table(
    name = "t_video_call_sample_review",
    indexes = {
        @Index(name = "idx01_t_video_call_sample_review", columnList = "created_at")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCallSampleReview extends Audit {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private Member caller;

    @Comment("평가 메시지")
    @Column(columnDefinition = "TEXT", name = "review_message")
    private String reviewMessage;

    @Comment("통화 대상 구분 키 (rohmoohyun, kimgeuntae)")
    @Column(name = "contact_key", length = 50)
    private String contactKey;
}
