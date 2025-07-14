package com.tomato.remember.application.auth.controller;

import com.tomato.remember.application.auth.dto.AuthRequestDTO;
import com.tomato.remember.application.auth.dto.AuthResponseDTO;
import com.tomato.remember.application.auth.service.AuthService;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.oneld.dto.OneIdResponse;
import com.tomato.remember.application.oneld.dto.OneIdVerifyResponse;
import com.tomato.remember.application.oneld.service.TomatoAuthService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.BadRequestException;
import com.tomato.remember.common.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 모바일/앱 공용 API 컨트롤러 (JWT Bearer 토큰 기반)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "모바일/앱 공용 JWT Bearer 토큰 기반 인증 API")
public class AuthRestController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final TomatoAuthService tomatoService;

    @Operation(
            summary = "토큰 유효성 검증",
            description = "현재 JWT Bearer 토큰이 유효한지 검증합니다. (ApiJwtFilter에서 자동 처리)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "토큰 유효"),
            @ApiResponse(responseCode = "401", description = "토큰 무효 또는 만료",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/validate")
    public ResponseEntity<Void> validate(
            @Parameter(hidden = true) Authentication auth
    ) {
        log.debug("Token validation successful for: {}", auth.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인된 사용자의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/me")
    public ResponseDTO<MemberDTO> me(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails user
    ) {
        log.debug("Member info requested for: {}", user.getMember().getName());
        return ResponseDTO.ok(authService.me(user));
    }

    @Operation(
            summary = "로그인",
            description = "휴대폰 번호와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/login")
    public ResponseDTO<AuthResponseDTO> login(
            @Valid @RequestBody AuthRequestDTO req,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse
    ) {
        String clientInfo = String.format("%s %s", servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
        log.info("API login attempt for phone: {}, client: {}", req.getPhoneNumber(), clientInfo);

        // 현재 쿠키 상태 로깅 (로그인 전)
        cookieUtil.logCookieStatus(servletRequest);

        try {
            // 기존 비즈니스 로직 유지 (One-ID → DB 순서)
            AuthResponseDTO authResponse = authService.loginProcess(req, servletRequest);

            log.info("API login successful for member: {} (ID: {})",
                    authResponse.getMember().getName(), authResponse.getMember().getId());

            // 모바일 뷰에서 사용할 수 있도록 쿠키에도 토큰 설정
            log.debug("Setting tokens in cookies for mobile view compatibility");
            cookieUtil.setMemberTokensWithSync(servletResponse,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken());

            // 쿠키 설정 후 상태 확인을 위한 로깅
            log.debug("Tokens set in response cookies. Client should verify cookie receipt.");

            return ResponseDTO.ok(authResponse);

        } catch (Exception e) {
            log.error("API login failed for phone: {}, error: {}", req.getPhoneNumber(), e.getMessage());

            // 로그인 실패 시 쿠키 정리
            try {
                cookieUtil.clearMemberTokenCookies(servletResponse);
                log.debug("Cleared any existing cookies after login failure");
            } catch (Exception clearEx) {
                log.warn("Failed to clear cookies after login failure", clearEx);
            }

            throw e;
        }
    }

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자 계정을 생성하고 JWT 토큰을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/register")
    public ResponseDTO<AuthResponseDTO> register(
            @RequestBody @Valid AuthRequestDTO req,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse
    ) {
        String clientInfo = String.format("%s %s", servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
        log.info("API registration attempt for phone: {}, client: {}", req.getPhoneNumber(), clientInfo);

        try {
            OneIdResponse resp = tomatoService.createOneId(req);

            if (resp == null || !resp.isResult()) {
                log.warn("One-ID registration failed for phone: {}", req.getPhoneNumber());
                throw new BadRequestException("외부 회원 인증 실패");
            }

            AuthResponseDTO authResponse = authService.createToken(resp, req, servletRequest);

            log.info("API registration successful for member: {} (ID: {})",
                    authResponse.getMember().getName(), authResponse.getMember().getId());

            // 쿠키에도 토큰 설정
            cookieUtil.setMemberTokensWithSync(servletResponse,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken());

            return ResponseDTO.ok(authResponse);

        } catch (Exception e) {
            log.error("API registration failed for phone: {}, error: {}", req.getPhoneNumber(), e.getMessage());

            // 회원가입 실패 시 쿠키 정리
            try {
                cookieUtil.clearMemberTokenCookies(servletResponse);
            } catch (Exception clearEx) {
                log.warn("Failed to clear cookies after registration failure", clearEx);
            }

            throw e;
        }
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 세션을 종료하고 해당 디바이스의 Refresh Token을 무효화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/logout")
    public ResponseDTO<?> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        log.info("API logout requested for member: {} (ID: {})",
                userDetails.getMember().getName(),
                userDetails.getMember().getId());

        try {
            authService.logout(userDetails, request);

            // 쿠키도 함께 정리
            cookieUtil.clearMemberTokenCookies(response);

            log.info("API logout successful for member: {} (ID: {})",
                    userDetails.getMember().getName(),
                    userDetails.getMember().getId());

            return ResponseDTO.ok();

        } catch (Exception e) {
            log.error("API logout failed for member: {} (ID: {}), error: {}",
                    userDetails.getMember().getName(),
                    userDetails.getMember().getId(),
                    e.getMessage());
            throw e;
        }
    }

    @Operation(
            summary = "토큰 갱신",
            description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 또는 만료된 리프레시 토큰")
    })
    @PostMapping("/refresh")
    public ResponseDTO<AuthResponseDTO> refreshToken(
            @RequestBody Map<String, String> request,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse
    ) {
        String refreshToken = request.get("refreshToken");
        String clientInfo = String.format("%s %s", servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));

        log.info("API token refresh requested from client: {}", clientInfo);
        log.debug("Refresh token provided: {}", refreshToken != null ? "YES (length=" + refreshToken.length() + ")" : "NO");

        try {
            AuthResponseDTO authResponse = authService.refreshToken(refreshToken, servletRequest);

            // 모바일 뷰에서 사용할 수 있도록 쿠키에도 토큰 업데이트
            log.debug("Updating cookies with refreshed tokens");
            cookieUtil.setMemberTokensWithSync(servletResponse,
                    authResponse.getToken(),
                    authResponse.getRefreshToken());

            log.info("API token refresh successful for member: {} (ID: {})",
                    authResponse.getMember().getName(),
                    authResponse.getMember().getId());

            return ResponseDTO.ok(authResponse);

        } catch (Exception e) {
            log.error("API token refresh failed, error: {}", e.getMessage());

            // 갱신 실패 시 쿠키 정리
            try {
                cookieUtil.clearMemberTokenCookies(servletResponse);
                log.debug("Cleared cookies after token refresh failure");
            } catch (Exception clearEx) {
                log.warn("Failed to clear cookies after token refresh failure", clearEx);
            }

            throw e;
        }
    }

    @Operation(
            summary = "회원탈퇴",
            description = "사용자 계정을 삭제하고 모든 관련 데이터를 정리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/delete")
    public ResponseDTO<?> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        log.info("API member deletion requested for member: {} (ID: {})",
                userDetails.getMember().getName(),
                userDetails.getMember().getId());

        try {
            authService.delete(userDetails, request);

            // 쿠키도 함께 정리
            cookieUtil.clearMemberTokenCookies(response);

            log.info("API member deletion successful for member: {} (ID: {})",
                    userDetails.getMember().getName(),
                    userDetails.getMember().getId());

            return ResponseDTO.ok();

        } catch (Exception e) {
            log.error("API member deletion failed for member: {} (ID: {}), error: {}",
                    userDetails.getMember().getName(),
                    userDetails.getMember().getId(),
                    e.getMessage());
            throw e;
        }
    }

    @Operation(
            summary = "토큰 정보 조회",
            description = "현재 토큰의 상세 정보를 조회합니다. (디버깅 및 모니터링용)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/token-info")
    public ResponseDTO<Map<String, Object>> tokenInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @Parameter(hidden = true) Authentication auth,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        log.debug("Token info requested for member: {} (ID: {})",
                userDetails.getMember().getName(),
                userDetails.getMember().getId());

        // 쿠키 상태도 함께 로깅
        cookieUtil.logCookieStatus(request);

        return ResponseDTO.ok(Map.of(
                "memberId", userDetails.getMember().getId(),
                "memberName", userDetails.getMember().getName(),
                "memberRole", userDetails.getMember().getRole().getDisplayName(),
                "memberStatus", userDetails.getMember().getStatus().getDisplayName(),
                "authenticated", auth.isAuthenticated(),
                "authorities", auth.getAuthorities(),
                "tokenType", "MEMBER_ACCESS_TOKEN",
                "cookieConfig", cookieUtil.getCookieConfigInfo()
        ));
    }

    @Operation(
            summary = "SMS 인증번호 발송",
            description = "회원가입을 위한 SMS 인증번호를 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/smsCert/send")
    public Mono<ResponseDTO<OneIdResponse>> sendSmsCert(
            @RequestBody AuthRequestDTO req
    ) {
        log.info("SMS certification send requested for phone: {}", req.getPhoneNumber());

        return tomatoService.sendSmsCert(req)
                .doOnSuccess(response -> log.info("SMS certification sent for phone: {}", req.getPhoneNumber()))
                .doOnError(error -> log.error("SMS certification send failed for phone: {}, error: {}",
                        req.getPhoneNumber(), error.getMessage()))
                .map(ResponseDTO::ok);
    }

    @Operation(
            summary = "SMS 인증번호 확인",
            description = "발송된 SMS 인증번호를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증번호")
    })
    @PostMapping("/smsCert/verify")
    public Mono<ResponseDTO<OneIdVerifyResponse>> verifySmsCert(
            @RequestBody AuthRequestDTO req
    ) {
        log.info("SMS certification verify requested for phone: {}", req.getPhoneNumber());

        return tomatoService.verifySmsCert(req)
                .doOnSuccess(response -> log.info("SMS certification verified for phone: {}", req.getPhoneNumber()))
                .doOnError(error -> log.error("SMS certification verify failed for phone: {}, error: {}",
                        req.getPhoneNumber(), error.getMessage()))
                .map(ResponseDTO::ok);
    }
}