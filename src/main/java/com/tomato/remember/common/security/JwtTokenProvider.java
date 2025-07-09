package com.tomato.remember.common.security;

import com.tomato.remember.admin.user.entity.Admin;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.exception.UnAuthorizationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    // 토큰 타입 구분
    public enum TokenType {
        ADMIN_ACCESS("ADMIN_ACCESS"),
        ADMIN_REFRESH("ADMIN_REFRESH"),
        MEMBER_ACCESS("MEMBER_ACCESS"),
        MEMBER_REFRESH("MEMBER_REFRESH");

        private final String value;
        TokenType(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private final SecretKey adminKey;
    private final SecretKey memberKey;
    private final long accessTokenValidityInMillis;
    private final long refreshTokenValidityInMillis;
    private final long autoLoginValidityInMillis;

    public JwtTokenProvider(
            @Value("${spring.security.jwt.admin-secret}") String adminSecret,
            @Value("${spring.security.jwt.member-secret}") String memberSecret,
            @Value("${spring.security.jwt.access-token-expiration}") long accessTokenValidityInMillis,
            @Value("${spring.security.jwt.refresh-token-expiration}") long refreshTokenValidityInMillis,
            @Value("${spring.security.jwt.auto-login-expiration}") long autoLoginValidityInMillis) {

        this.adminKey = Keys.hmacShaKeyFor(adminSecret.getBytes(StandardCharsets.UTF_8));
        this.memberKey = Keys.hmacShaKeyFor(memberSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMillis = accessTokenValidityInMillis;
        this.refreshTokenValidityInMillis = refreshTokenValidityInMillis;
        this.autoLoginValidityInMillis = autoLoginValidityInMillis;
    }

    // =========================== 관리자 토큰 ===========================

    /**
     * 관리자 Access Token 생성
     */
    public String createAdminAccessToken(Admin admin) {
        return createAdminToken(admin, accessTokenValidityInMillis, TokenType.ADMIN_ACCESS);
    }

    /**
     * 관리자 Refresh Token 생성
     */
    public String createAdminRefreshToken(Admin admin, boolean autoLogin) {
        long validity = autoLogin ? autoLoginValidityInMillis : refreshTokenValidityInMillis;
        return createAdminToken(admin, validity, TokenType.ADMIN_REFRESH);
    }

    private String createAdminToken(Admin admin, long validityInMillis, TokenType tokenType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusNanos(validityInMillis * 1_000_000);

        return Jwts.builder()
                .setSubject(admin.getUsername())
                .claim("type", tokenType.getValue())
                .claim("role", admin.getRole().name())
                .claim("adminId", admin.getId())
                .setIssuedAt(toDate(now))
                .setExpiration(toDate(expiration))
                .signWith(adminKey)
                .compact();
    }

    // =========================== 회원 토큰 ===========================

    /**
     * 회원 Access Token 생성
     */
    public String createMemberAccessToken(Member member) {
        return createMemberToken(member, accessTokenValidityInMillis, TokenType.MEMBER_ACCESS);
    }

    /**
     * 회원 Refresh Token 생성
     */
    public String createMemberRefreshToken(Member member, boolean autoLogin) {
        long validity = autoLogin ? autoLoginValidityInMillis : refreshTokenValidityInMillis;
        return createMemberToken(member, validity, TokenType.MEMBER_REFRESH);
    }

    private String createMemberToken(Member member, long validityInMillis, TokenType tokenType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusNanos(validityInMillis * 1_000_000);

        return Jwts.builder()
                .setSubject(String.valueOf(member.getId()))
                .claim("type", tokenType.getValue())
                .claim("email", member.getEmail())
                .setIssuedAt(toDate(now))
                .setExpiration(toDate(expiration))
                .signWith(memberKey)
                .compact();
    }

    // =========================== 토큰 검증 ===========================

    /**
     * 관리자 토큰 검증
     */
    public boolean validateAdminToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(adminKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = (String) claims.get("type");
            return tokenType != null && (
                tokenType.equals(TokenType.ADMIN_ACCESS.getValue()) ||
                tokenType.equals(TokenType.ADMIN_REFRESH.getValue())
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid admin token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 회원 토큰 검증
     */
    public boolean validateMemberToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(memberKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = (String) claims.get("type");
            return tokenType != null && (
                tokenType.equals(TokenType.MEMBER_ACCESS.getValue()) ||
                tokenType.equals(TokenType.MEMBER_REFRESH.getValue())
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid member token: {}", e.getMessage());
            return false;
        }
    }

    // =========================== 토큰 정보 추출 ===========================

    /**
     * 관리자 토큰에서 정보 추출
     */
    public Map<String, Object> getAdminClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(adminKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 회원 토큰에서 정보 추출
     */
    public Map<String, Object> getMemberClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(memberKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰에서 Subject 추출 (토큰 타입 자동 감지)
     */
    public String getSubject(String token) {
        // 먼저 관리자 토큰으로 시도
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(adminKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            // 관리자 토큰이 아니면 회원 토큰으로 시도
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(memberKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject();
            } catch (JwtException e2) {
                throw new UnAuthorizationException("Invalid token");
            }
        }
    }

    /**
     * 토큰 타입 확인
     */
    public TokenType getTokenType(String token) {
        try {
            // 관리자 토큰 확인
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(adminKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String type = (String) claims.get("type");
            if (TokenType.ADMIN_ACCESS.getValue().equals(type)) return TokenType.ADMIN_ACCESS;
            if (TokenType.ADMIN_REFRESH.getValue().equals(type)) return TokenType.ADMIN_REFRESH;

        } catch (JwtException e) {
            // 회원 토큰 확인
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(memberKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String type = (String) claims.get("type");
                if (TokenType.MEMBER_ACCESS.getValue().equals(type)) return TokenType.MEMBER_ACCESS;
                if (TokenType.MEMBER_REFRESH.getValue().equals(type)) return TokenType.MEMBER_REFRESH;

            } catch (JwtException e2) {
                throw new UnAuthorizationException("Invalid token type");
            }
        }

        throw new UnAuthorizationException("Unknown token type");
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    public String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    /**
     * 토큰 만료 시간 확인
     */
    public LocalDateTime getExpirationDate(String token) {
        Date expiration;

        try {
            // 관리자 토큰으로 시도
            expiration = Jwts.parserBuilder()
                    .setSigningKey(adminKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        } catch (JwtException e) {
            // 회원 토큰으로 시도
            expiration = Jwts.parserBuilder()
                    .setSigningKey(memberKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        }

        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    // =========================== 유틸리티 메서드 ===========================

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    // Getter 메서드들 (필터에서 직접 키 접근이 필요한 경우)
    public SecretKey getAdminKey() { return adminKey; }
    public SecretKey getMemberKey() { return memberKey; }
}