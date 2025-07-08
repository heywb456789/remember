package com.tomato.naraclub.application.member.entity;

import com.tomato.naraclub.common.audit.Audit;
import com.tomato.naraclub.common.audit.CreatedAndModifiedAudit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Table(
    name = "t_member_twitter",
    indexes = {
        @Index(name = "idx01_t_member_twitter_created_at", columnList = "created_at") // 최신순 정렬용
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TwitterAccount extends CreatedAndModifiedAudit {

    @Id
    @Column(name = "member_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Comment("x_access_token")
    @Column(nullable = false, length = 300)
    private String accessToken;

    @Comment("x_access_token_secret")
    @Column(nullable = false, length = 300)
    private String accessTokenSecret;

    @Comment("name")
    @Column(nullable = false, length = 100)
    private String screenName;

    public void setMember(Member member) {
        this.member = member;
    }
}
