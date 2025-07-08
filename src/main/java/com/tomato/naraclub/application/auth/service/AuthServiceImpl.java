package com.tomato.naraclub.application.auth.service;

import com.tomato.naraclub.application.auth.code.LoginType;
import com.tomato.naraclub.application.auth.dto.AuthRequestDTO;
import com.tomato.naraclub.application.auth.dto.AuthResponseDTO;
import com.tomato.naraclub.application.auth.entity.MemberLoginHistory;
import com.tomato.naraclub.application.auth.entity.RefreshToken;
import com.tomato.naraclub.application.auth.repository.MemberLoginHistoryRepository;
import com.tomato.naraclub.application.auth.repository.RefreshTokenRepository;
import com.tomato.naraclub.application.member.dto.MemberDTO;
import com.tomato.naraclub.application.oneld.dto.OneIdResponse;
import com.tomato.naraclub.application.member.entity.Member;
import com.tomato.naraclub.application.member.repository.MemberRepository;
import com.tomato.naraclub.common.exception.BadRequestException;
import com.tomato.naraclub.common.security.JwtTokenProvider;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.common.code.MemberRole;
import com.tomato.naraclub.common.code.MemberStatus;
import com.tomato.naraclub.common.exception.UnAuthorizationException;
import com.tomato.naraclub.common.util.InviteCodeGenerator;
import com.tomato.naraclub.common.util.UserDeviceInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.service
 * @fileName : AuthServiceImpl
 * @date : 2025-04-18
 * @description :
 * @AUTHOR : MinjaeKim
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
                Member m = Member.builder()
                    .userKey(userKey)
                    .password(passwordEncoder.encode(resp.getValue().getPasswd()))
                    .phoneNumber(resp.getValue().getDecPhoneNum())
                    .inviteCode(InviteCodeGenerator.generateUnique(memberRepository))
                    .status(MemberStatus.TEMPORARY_INVITE)
                    .role(MemberRole.USER_INACTIVE)
                    .email(resp.getValue().getEmail())
                    .name(resp.getValue().getName())
                    .verified(false)
                    .profileImg(profileImg)
                    .build();
                return memberRepository.save(m);
            });

        if (!member.getProfileImg().equals(profileImg)) {
            member.setProfileImg(profileImg);
        }

        //마지막 접속시간 증가
        member.setLastAccessAt();

        String accessToken = tokenProvider.createAccessToken(member);
        String refreshToken = tokenProvider.createRefreshToken(member, authRequest.isAutoLogin());

        LocalDateTime expiryDate = tokenProvider.getExpirationDate(refreshToken);
        String userAgent = UserDeviceInfoUtil.getUserAgent(servletRequest.getHeader("User-Agent"));

        refreshTokenRepository.save(RefreshToken.builder()
            .member(member)
            .refreshToken(refreshToken)
            .expiryDate(expiryDate)
            .ipAddress(UserDeviceInfoUtil.getClientIp(servletRequest))  // IP 주소 가져오기
            .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))  // Custom 헤더 또는 파싱
            .userAgent(userAgent)
            .lastUsedAt(LocalDateTime.now())
            .build());

        // 유저 로그인 기록 저장
        loginHistoryRepository.save(MemberLoginHistory.builder()
            .memberId(member.getId())
            .type(LoginType.LOGIN)
            .userAgent(userAgent)
            .ipAddress(UserDeviceInfoUtil.getClientIp(servletRequest))
            .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))
            .build());

        return AuthResponseDTO.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .member(member.convertDTO())
            .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken, HttpServletRequest servletRequest) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnAuthorizationException("Invalid Token");
        }

        RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new UnAuthorizationException("Refresh Token not found"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnAuthorizationException("Expired Refresh Token");
        }

        Member member = token.getMember(); // Member 객체 직접 참조

        String newAccessToken = tokenProvider.createAccessToken(member);

        // RefreshToken의 lastUsedAt 갱신
        token.setLastUsedAt(LocalDateTime.now());

        refreshTokenRepository.save(token);

        String userAgent = UserDeviceInfoUtil.getUserAgent(servletRequest.getHeader("User-Agent"));

        // 유저 로그인 기록 저장
        loginHistoryRepository.save(MemberLoginHistory.builder()
            .memberId(member.getId())
            .type(LoginType.REFRESH)
            .userAgent(userAgent)
            .ipAddress(UserDeviceInfoUtil.getClientIp(servletRequest))
            .deviceType(UserDeviceInfoUtil.getDeviceType(userAgent))
            .build());

        return AuthResponseDTO.builder()
            .token(newAccessToken)
            .refreshToken(refreshToken)
            .member(member.convertDTO()) // Member 정보도 함께 전달 가능
            .build();
    }

    @Override
    public void logout(MemberUserDetails userDetails, HttpServletRequest servletRequest) {
        Member member = userDetails.getMember();

        String userAgent = UserDeviceInfoUtil.getUserAgent(servletRequest.getHeader("User-Agent"));
        String deviceType = UserDeviceInfoUtil.getDeviceType(userAgent);
        String ipAddress = UserDeviceInfoUtil.getClientIp(servletRequest);

        refreshTokenRepository.findAllByMember(member)
            .stream()
            .filter(rt ->
                deviceType.equals(rt.getDeviceType()) &&
                    userAgent.equals(rt.getUserAgent()) &&
                    ipAddress.equals(rt.getIpAddress()))
            .forEach(refreshTokenRepository::delete);
    }

    @Override
    public MemberDTO me(MemberUserDetails user) {
        Member member = memberRepository.findById(user.getMember().getId())
            .orElseThrow(() -> new BadRequestException("존재하지 않는 회원입니다."));

        return member.convertDTO();
    }

    /**
     * 포인트 회수 활동내역 모두 삭제 데이터 모두 삭제
     */
    @Override
    @Transactional
    public void delete(MemberUserDetails user, HttpServletRequest request) {
        Member member = memberRepository.findById(user.getMember().getId())
            .orElseThrow(() -> new BadRequestException("존재하지 않는 회원입니다."));

        member.decreasePoints(member.getPoints());

        member.deleteMemInfo();
    }
}
