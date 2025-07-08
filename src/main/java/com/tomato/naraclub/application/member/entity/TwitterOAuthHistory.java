package com.tomato.naraclub.application.member.entity;

import com.tomato.naraclub.common.audit.Audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.entity
 * @fileName : TwitterOAuthHistory
 * @date : 2025-05-15
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Table(
        name = "t_twitter_history",
        indexes = {
                @Index(name = "idx01_t_twitter_history_created_at", columnList = "created_at") // 최신순 정렬용
        }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TwitterOAuthHistory extends Audit {

    @Comment("회원 번호 ")
    private Long memberId;

    @Comment("1회성 인증 토큰")
    private String oauthToken;

    @Comment("1회성 인증토큰 시크릿")
    private String oauthTokenSecret;

}
