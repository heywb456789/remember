package com.tomato.naraclub.admin.user.entity;

import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import com.tomato.naraclub.common.audit.Audit;
import com.tomato.naraclub.common.code.MemberStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.entity
 * @fileName : AuthorityHistory
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Table(
    name = "t_authority_history",
    indexes = {
        @Index(name = "idx01_t_authority_history", columnList = "created_at") // 최신순 정렬용
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityHistory extends Audit {

    @Comment("유저 아이디")
    private Long userId;

    @Comment("앱 유저 변경 스테이터스")
    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus;

    @Comment("어드민 아이디")
    private Long adminId;

    @Comment("어드민 유저 변경 스테이터스")
    @Enumerated(EnumType.STRING)
    private AdminStatus adminStatus;

    @Comment("어드민 롤 ")
    @Enumerated(EnumType.STRING)
    private AdminRole adminRole;

    @Comment("이유")
    private String reason;
}
