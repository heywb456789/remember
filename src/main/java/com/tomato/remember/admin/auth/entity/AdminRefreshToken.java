package com.tomato.remember.admin.auth.entity;

import com.tomato.remember.admin.user.entity.Admin;
import com.tomato.remember.common.audit.CreatedAndModifiedAudit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "t_admin_refresh_token",
    indexes = {
        @Index(name = "idx01_t_admin_refresh_token_created_at", columnList = "created_at"), // 최신순 정렬용// 조회수 기반 핫 마커 계산용
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRefreshToken extends CreatedAndModifiedAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(nullable = false, unique = true, length = 500)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // 디바이스 정보 필드 추가
    private String ipAddress;            // IP 주소
    private String deviceType;           // 디바이스 종류 (PC, Mobile, Tablet 등)
    private String userAgent;            // 브라우저 User-Agent
    private LocalDateTime lastUsedAt;    // 마지막 사용 시간 (최근 접속 관리)

    public void setLastUsedAt(LocalDateTime param) {
        this.lastUsedAt = param;
    }
}
