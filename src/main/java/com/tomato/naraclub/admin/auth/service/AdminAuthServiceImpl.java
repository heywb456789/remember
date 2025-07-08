package com.tomato.naraclub.admin.auth.service;

import com.tomato.naraclub.admin.auth.dto.AdminAuthRequest;
import com.tomato.naraclub.admin.auth.dto.AdminAuthResponseDTO;
import com.tomato.naraclub.admin.auth.entity.AdminRefreshToken;
import com.tomato.naraclub.admin.auth.repository.AdminRefreshTokenRepository;
import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.security.AdminUserDetailsService;
import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import com.tomato.naraclub.admin.user.entity.Admin;
import com.tomato.naraclub.admin.user.repository.AdminRepository;
import com.tomato.naraclub.common.security.JwtTokenProvider;
import com.tomato.naraclub.common.code.ResponseStatus;
import com.tomato.naraclub.common.exception.APIException;
import com.tomato.naraclub.common.exception.UnAuthorizationException;
import com.tomato.naraclub.common.util.UserDeviceInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final JwtTokenProvider jwtProvider;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public AdminAuthResponseDTO createToken(AdminAuthRequest req, HttpServletRequest request) {
        AdminUserDetails user = (AdminUserDetails)
            userDetailsService.loadUserByUsername(req.getUsername());

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new APIException(ResponseStatus.UNAUTHORIZED_ID_PW);
        }

        // ◆ isEnabled() 체크 ◆
        if (!user.isEnabled()) {
            throw new APIException(ResponseStatus.UNAUTHORIZED_ROLE);
        }

        user.getAdmin().updateLastAccess();

        String accessToken = jwtProvider.createAccessTokenForAdmin(user.getAdmin());
        String refreshToken = jwtProvider.createRefreshTokenForAdmin(user.getAdmin(),
            req.getAutoLogin());

        LocalDateTime expiryDate = jwtProvider.getExpirationDate(refreshToken);
        String userAgent = UserDeviceInfoUtil.getUserAgent(request.getHeader("User-Agent"));

        adminRefreshTokenRepository.save(AdminRefreshToken.builder()
            .admin(user.getAdmin())
            .refreshToken(refreshToken)
            .expiryDate(expiryDate)
            .ipAddress(UserDeviceInfoUtil.getClientIp(request))  // IP 주소 가져오기
            .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))  // Custom 헤더 또는 파싱
            .userAgent(userAgent)
            .lastUsedAt(LocalDateTime.now())
            .build());

        return AdminAuthResponseDTO.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .admin(user.getAdmin().convertDTO())
            .build();
    }

    @Override
    @Transactional
    public AdminAuthResponseDTO createUserAndToken(AdminAuthRequest req,
        HttpServletRequest request) {
        //1) 체크
        if (adminRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new APIException(ResponseStatus.EXIST_USER);
        }

        //2) 가입
        Admin saved = adminRepository.save(Admin.builder()
            .username(req.getUsername())
            .password(passwordEncoder.encode(req.getPassword()))
            .name(req.getUsername())
            .email(req.getEmail())
            .phoneNumber(req.getPhoneNumber())
            .role(AdminRole.COMMON)
            .status(AdminStatus.INACTIVE)
            .lastAccessAt(LocalDateTime.now())
            .build());

        String accessToken = jwtProvider.createAccessTokenForAdmin(saved);
        String refreshToken = jwtProvider.createRefreshTokenForAdmin(saved, false);

        LocalDateTime expiryDate = jwtProvider.getExpirationDate(refreshToken);
        String userAgent = UserDeviceInfoUtil.getUserAgent(request.getHeader("User-Agent"));

        adminRefreshTokenRepository.save(AdminRefreshToken.builder()
            .admin(saved)
            .refreshToken(refreshToken)
            .expiryDate(expiryDate)
            .ipAddress(UserDeviceInfoUtil.getClientIp(request))  // IP 주소 가져오기
            .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))  // Custom 헤더 또는 파싱
            .userAgent(userAgent)
            .lastUsedAt(LocalDateTime.now())
            .build());

        return AdminAuthResponseDTO.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .admin(saved.convertDTO())
            .build();
    }

    @Override
    public Boolean checkUserName(String username) {
        //조회
        Optional<Admin> admin = adminRepository.findByUsername(username);

        return admin.isEmpty();
    }

    @Override
    @Transactional
    public AdminAuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new UnAuthorizationException("Invalid Token");
        }

        AdminRefreshToken token = adminRefreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new UnAuthorizationException("Refresh Token not found"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnAuthorizationException("Expired Refresh Token");
        }

        Admin admin = token.getAdmin(); // Member 객체 직접 참조

        String newAccessToken = jwtProvider.createAccessTokenForAdmin(admin);

        // RefreshToken의 lastUsedAt 갱신
        token.setLastUsedAt(LocalDateTime.now());

        return AdminAuthResponseDTO.builder()
            .token(newAccessToken)
            .refreshToken(refreshToken)
            .admin(admin.convertDTO()) // Member 정보도 함께 전달 가능
            .build();
    }
}
