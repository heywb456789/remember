package com.tomato.naraclub.common.security;

import com.tomato.naraclub.admin.user.entity.Admin;
import com.tomato.naraclub.application.member.entity.Member;
import com.tomato.naraclub.common.exception.UnAuthorizationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenValidityInMillis;
    private final long refreshTokenValidityInMillis;
    private final long autoLoginValidityInMillis;

    public JwtTokenProvider(
            @Value("${spring.security.jwt.secret}") String secret,
            @Value("${spring.security.jwt.access-token-expiration}") long accessTokenValidityInMillis,
            @Value("${spring.security.jwt.refresh-token-expiration}") long refreshTokenValidityInMillis,
            @Value("${spring.security.jwt.auto-login-expiration}") long autoLoginValidityInMillis) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMillis = accessTokenValidityInMillis;
        this.refreshTokenValidityInMillis = refreshTokenValidityInMillis;
        this.autoLoginValidityInMillis = autoLoginValidityInMillis;
    }

    public SecretKey getKey() {
        return this.key;
    }

    // 액세스 토큰 생성
    public String createAccessToken(Member member) {
        return createToken(member, accessTokenValidityInMillis);
    }

    public String createAccessTokenForAdmin(Admin admin) {
        return createTokenForAdmin(admin, accessTokenValidityInMillis);
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(Member member, boolean autoLogin) {
        long validity = autoLogin ? autoLoginValidityInMillis : refreshTokenValidityInMillis;
        return createToken(member, validity);
    }

     public String createRefreshTokenForAdmin(Admin admin, boolean autoLogin) {
        long validity = autoLogin ? autoLoginValidityInMillis : refreshTokenValidityInMillis;
        return createTokenForAdmin(admin, validity);
    }

    private String createToken(Member member, long validityInMillis) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusNanos(validityInMillis * 1_000_000);

        return Jwts.builder()
                .setSubject(String.valueOf(member.getId()))
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                .claim("status", member.getStatus().name())
                .setIssuedAt(toDate(now))
                .setExpiration(toDate(expiration))
                .signWith(key)
                .compact();
    }

    public String createTokenForAdmin(Admin admin, long validityInMillis) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusNanos(validityInMillis * 1_000_000);

        return Jwts.builder()
            .setSubject(admin.getUsername())   // 구분용 prefix
            .claim("role", admin.getRole().name())
            .setIssuedAt(toDate(now))
            .setExpiration(toDate(expiration))
            .signWith(key)
            .compact();
}

    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public LocalDateTime getExpirationDate(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    public Map<String, Object> getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new UnAuthorizationException("로그인 정보가 없습니다.");
    }

    // LocalDateTime -> Date 변환 유틸 메서드
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Date -> LocalDateTime 변환 유틸 메서드 (필요시 사용)
    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
