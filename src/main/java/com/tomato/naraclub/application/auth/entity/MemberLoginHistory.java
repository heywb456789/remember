package com.tomato.naraclub.application.auth.entity;

import com.tomato.naraclub.admin.user.dto.UserLoginHistoryResponse;
import com.tomato.naraclub.application.auth.code.LoginType;
import com.tomato.naraclub.common.audit.Audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.auth.entity
 * @fileName : MemberLoginHistory
 * @date : 2025-05-13
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Entity
@Table(
    name = "t_member_login_history",
    indexes = {
        @Index(name = "idx01_t_member_login_history_created_at", columnList = "created_at"), // 최신순 정렬용// 조회수 기반 핫 마커 계산용
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLoginHistory extends Audit {

    @Comment("회원 아이디")
    @Column(nullable = false)
    private Long memberId;

    @Comment("로그인 타입 : 로그인 / RefreshToken 갱신")
    @Enumerated(EnumType.STRING)
    private LoginType type;

    @Comment("IP 주소 ")
    private String ipAddress;            // IP 주소

    @Comment("기기 타입")
    private String deviceType;           // 디바이스 종류 (PC, Mobile, Tablet 등)

    @Comment("Agent")
    private String userAgent;            // 브라우저 User-Agent

    public UserLoginHistoryResponse convertResponse() {
        return UserLoginHistoryResponse.builder()
            .memberId(memberId)
            .type(type)
            .ipAddress(ipAddress)
            .deviceType(deviceType)
            .userAgent(userAgent)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
