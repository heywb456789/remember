package com.tomato.remember.application.auth.service;

import com.tomato.remember.application.auth.code.LoginType;
import com.tomato.remember.application.auth.dto.AuthRequestDTO;
import com.tomato.remember.application.auth.dto.AuthResponseDTO;
import com.tomato.remember.application.auth.entity.MemberLoginHistory;
import com.tomato.remember.application.auth.entity.RefreshToken;
import com.tomato.remember.application.auth.repository.MemberLoginHistoryRepository;
import com.tomato.remember.application.auth.repository.RefreshTokenRepository;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.oneld.dto.OneIdResponse;
import com.tomato.remember.application.oneld.service.TomatoAuthService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.repository.MemberRepository;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.exception.BadRequestException;
import com.tomato.remember.common.exception.UnAuthorizationException;
import com.tomato.remember.common.security.JwtTokenProvider;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import com.tomato.remember.common.util.InviteCodeGenerator;
import com.tomato.remember.common.util.UserDeviceInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 새로운 JWT 인증 시스템용 AuthService
 *
 * 변경사항:
 * - JwtTokenProvider의 새로운 메서드 사용 (createMemberAccessToken, createMemberRefreshToken)
 * - 토큰 검증 로직 개선 (validateMemberToken)
 * - 기존 비즈니스 로직 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberLoginHistoryRepository loginHistoryRepository;
    private final TomatoAuthService tomatoAuthService;

    /**
     * 로그인 처리 프로세스 (새로운 JWT 시스템 적용)
     * 1. One-ID 로그인 시도
     * 2. 성공 시 -> DB 사용자 생성/조회 후 회원용 JWT 토큰 발급
     * 3. 실패 시 -> DB에서 전화번호로 찾아서 로그인 처리
     */
    @Override
    @Transactional
    public AuthResponseDTO loginProcess(AuthRequestDTO authRequest, HttpServletRequest servletRequest) {
        log.info("Login process started for phone: {}", authRequest.getPhoneNumber());

        try {
            // 1. One-ID 로그인 시도
            OneIdResponse oneIdResponse = tomatoAuthService.authenticate(
                    authRequest.getPhoneNumber(),
                    authRequest.getPassword()
            ).block();

            if (oneIdResponse != null && oneIdResponse.isResult()) {
                log.info("One-ID login successful for phone: {}", authRequest.getPhoneNumber());
                // One-ID 로그인 성공 -> 기존 로직 처리
                return createToken(oneIdResponse, authRequest, servletRequest);
            }
        } catch (Exception e) {
            log.warn("One-ID 로그인 실패, DB 로그인 시도: {}", e.getMessage());
        }

        // 2. One-ID 로그인 실패 시 DB에서 전화번호로 찾아서 로그인 처리
        log.info("Attempting DB login for phone: {}", authRequest.getPhoneNumber());
        return processDbLogin(authRequest, servletRequest);
    }

    /**
     * DB 로그인 처리 (One-ID 실패 시) - 새로운 JWT 토큰 적용
     */
    private AuthResponseDTO processDbLogin(AuthRequestDTO authRequest, HttpServletRequest servletRequest) {
        // 전화번호로 사용자 찾기
        Optional<Member> memberOpt = memberRepository.findByPhoneNumber(authRequest.getPhoneNumber());

        if (memberOpt.isEmpty()) {
            log.warn("Member not found for phone: {}", authRequest.getPhoneNumber());
            throw new APIException(ResponseStatus.UNAUTHORIZED_ONE_ID);
        }

        Member member = memberOpt.get();

        // 비밀번호 확인
        if (!passwordEncoder.matches(authRequest.getPassword(), member.getPassword())) {
            log.warn("Password mismatch for member: {}", member.getId());
            throw new APIException(ResponseStatus.UNAUTHORIZED_ONE_ID);
        }

        // 마지막 접속시간 업데이트
        member.setLastAccessAt();

        // 새로운 JWT 토큰 생성 (회원용)
        String accessToken = tokenProvider.createMemberAccessToken(member);
        String refreshToken = tokenProvider.createMemberRefreshToken(member, authRequest.isAutoLogin());

        // Refresh Token 저장
        saveRefreshToken(member, refreshToken, servletRequest);

        // 로그인 기록 저장
        saveLoginHistory(member, servletRequest, LoginType.LOGIN);

        log.info("DB login successful for member: {} (ID: {})", member.getName(), member.getId());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .member(member.convertDTO())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO createToken(OneIdResponse resp, AuthRequestDTO authRequest,
                                       HttpServletRequest servletRequest) {
        String userKey = resp.getValue().getUserKey();

        // 프로필 이미지 URL 가공: 특정 호스트/포트 부분이 포함된 경우 잘라내기
        String rawProfileImg = resp.getValue().getProfileImg();
        String profileImg;

        if (rawProfileImg != null && rawProfileImg.startsWith("http://api.otongtong.net:28080")) {
            // host+port 길이만큼 부분 문자열을 제거
            profileImg = rawProfileImg.substring("http://api.otongtong.net:28080".length());
        } else {
            profileImg = rawProfileImg;
        }

        //userKey 있으면 일단은 생성 + 초대코드 + 신원 인증 관련 처리 미상태로 저장
        Member member = memberRepository.findByUserKey(userKey)
                .orElseGet(() -> {
                    log.info("Creating new member from One-ID for userKey: {}", userKey);
                    Member m = Member.builder()
                            .userKey(userKey)
                            .password(passwordEncoder.encode(resp.getValue().getPasswd()))
                            .phoneNumber(resp.getValue().getDecPhoneNum())
                            .inviteCode(InviteCodeGenerator.generateUnique(memberRepository))
                            .status(MemberStatus.ACTIVE) // 바로 활성 상태로 변경
                            .role(MemberRole.USER_ACTIVE)      // 일반 사용자로 변경
                            .email(resp.getValue().getEmail())
                            .name(resp.getValue().getName())
                            .verified(true)  // 인증 완료로 설정
                            .profileImg(profileImg)
                            .build();
                    return memberRepository.save(m);
                });

        if (!member.getProfileImg().equals(profileImg)) {
            member.setProfileImg(profileImg);
        }

        //마지막 접속시간 증가
        member.setLastAccessAt();

        // 새로운 JWT 토큰 생성 (회원용)
        String accessToken = tokenProvider.createMemberAccessToken(member);
        String refreshToken = tokenProvider.createMemberRefreshToken(member, authRequest.isAutoLogin());

        // Refresh Token 저장
        saveRefreshToken(member, refreshToken, servletRequest);

        // 로그인 기록 저장
        saveLoginHistory(member, servletRequest, LoginType.LOGIN);

        log.info("One-ID token creation successful for member: {} (ID: {})", member.getName(), member.getId());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .member(member.convertDTO())
                .build();
    }

    /**
     * Refresh Token 저장 (새로운 JWT 시스템용)
     */
    private void saveRefreshToken(Member member, String refreshToken, HttpServletRequest servletRequest) {
        LocalDateTime expiryDate = tokenProvider.getExpirationDate(refreshToken);
        String userAgent = UserDeviceInfoUtil.getUserAgent(servletRequest.getHeader("User-Agent"));

        refreshTokenRepository.save(RefreshToken.builder()
                .member(member)
                .refreshToken(refreshToken)
                .expiryDate(expiryDate)
                .ipAddress(UserDeviceInfoUtil.getClientIp(servletRequest))
                .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))
                .userAgent(userAgent)
                .lastUsedAt(LocalDateTime.now())
                .build());

        log.debug("Refresh token saved for member: {} (ID: {})", member.getName(), member.getId());
    }

    /**
     * 로그인 기록 저장
     */
    private void saveLoginHistory(Member member, HttpServletRequest servletRequest, LoginType loginType) {
        String userAgent = UserDeviceInfoUtil.getUserAgent(servletRequest.getHeader("User-Agent"));

        loginHistoryRepository.save(MemberLoginHistory.builder()
                .memberId(member.getId())
                .type(loginType)
                .userAgent(userAgent)
                .ipAddress(UserDeviceInfoUtil.getClientIp(servletRequest))
                .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))
                .build());

        log.debug("Login history saved for member: {} (ID: {}), type: {}", member.getName(), member.getId(), loginType);
    }

    /**
     * 토큰 갱신 (새로운 JWT 시스템 적용)
     */
    @Override
    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken, HttpServletRequest servletRequest) {
        log.debug("Token refresh requested");

        // 새로운 JWT 검증 방식 사용
        if (!tokenProvider.validateMemberToken(refreshToken)) {
            log.warn("Invalid refresh token provided");
            throw new UnAuthorizationException("Invalid Token");
        }

        RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database");
                    return new UnAuthorizationException("Refresh Token not found");
                });

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token expired for member: {}", token.getMember().getId());
            throw new UnAuthorizationException("Expired Refresh Token");
        }

        Member member = token.getMember(); // Member 객체 직접 참조

        // 새로운 회원용 Access Token 생성
        String newAccessToken = tokenProvider.createMemberAccessToken(member);

        // RefreshToken의 lastUsedAt 갱신
        token.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);

        // 로그인 기록 저장
        saveLoginHistory(member, servletRequest, LoginType.REFRESH);

        log.info("Token refresh successful for member: {} (ID: {})", member.getName(), member.getId());

        return AuthResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken) // 기존 refresh token 재사용
                .member(member.convertDTO()) // Member 정보도 함께 전달 가능
                .build();
    }

    /**
     * 로그아웃 처리 (디바이스별 토큰 정리)
     */
    @Override
    @Transactional
    public void logout(MemberUserDetails userDetails, HttpServletRequest servletRequest) {
        Member member = userDetails.getMember();

        String userAgent = UserDeviceInfoUtil.getUserAgent(servletRequest.getHeader("User-Agent"));
        String deviceType = UserDeviceInfoUtil.getDeviceType(userAgent);
        String ipAddress = UserDeviceInfoUtil.getClientIp(servletRequest);

        // 동일한 디바이스의 refresh token만 삭제
        int deletedTokens = refreshTokenRepository.findAllByMember(member)
                .stream()
                .filter(rt ->
                        deviceType.equals(rt.getDeviceType()) &&
                                userAgent.equals(rt.getUserAgent()) &&
                                ipAddress.equals(rt.getIpAddress()))
                .mapToInt(rt -> {
                    refreshTokenRepository.delete(rt);
                    return 1;
                })
                .sum();

        log.info("Logout successful for member: {} (ID: {}), deleted {} refresh tokens",
                member.getName(), member.getId(), deletedTokens);
    }

    /**
     * 내 정보 조회
     */
    @Override
    public MemberDTO me(MemberUserDetails user) {
        Member member = memberRepository.findById(user.getMember().getId())
                .orElseThrow(() -> new BadRequestException("존재하지 않는 회원입니다."));

        log.debug("Member info retrieved for: {} (ID: {})", member.getName(), member.getId());
        return member.convertDTO();
    }

    /**
     * 회원 탈퇴 처리
     * 포인트 회수 활동내역 모두 삭제 데이터 모두 삭제
     */
    @Override
    @Transactional
    public void delete(MemberUserDetails user, HttpServletRequest request) {
        Member member = memberRepository.findById(user.getMember().getId())
                .orElseThrow(() -> new BadRequestException("존재하지 않는 회원입니다."));

        // 포인트 회수
        member.decreasePoints(member.getPoints());

        // 회원 정보 삭제 처리
        member.deleteMemInfo();

        // 해당 회원의 모든 refresh token 삭제
        refreshTokenRepository.findAllByMember(member)
                .forEach(refreshTokenRepository::delete);

        log.info("Member deletion completed for: {} (ID: {})", member.getName(), member.getId());
    }
}