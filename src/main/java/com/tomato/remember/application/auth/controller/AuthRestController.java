package com.tomato.remember.application.auth.controller;

import com.tomato.remember.application.auth.dto.AuthRequestDTO;
import com.tomato.remember.application.auth.dto.AuthResponseDTO;
import com.tomato.remember.application.auth.service.AuthService;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.dto.ResponseDTO;
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

/**
 * 모바일/앱 공용 API 컨트롤러 (JWT Bearer 토큰 기반)
 *
 * 새로운 인증 시스템:
 * - ApiJwtFilter에서 Bearer 토큰 검증
 * - 자동 토큰 갱신 없음 (클라이언트에서 수동 처리)
 * - 회원용 JWT 토큰만 처리
 * - JSON 요청/응답만 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "모바일/앱 공용 JWT Bearer 토큰 기반 인증 API")
public class AuthRestController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

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
            description = "휴대폰 번호와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다. One-ID 로그인을 우선 시도하고, 실패 시 DB 로그인을 시도합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 잘못된 휴대폰 번호 또는 비밀번호"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/login")
    public ResponseDTO<AuthResponseDTO> login(
            @Parameter(
                    description = "로그인 정보 (JSON 형식)",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = AuthRequestDTO.class,
                                    example = """
                        {
                            "phoneNumber": "01012341234",
                            "password": "password123",
                            "autoLogin": false
                        }
                        """
                            )
                    )
            )
            @Valid @RequestBody AuthRequestDTO req,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse
    ) {
        log.info("API login attempt for phone: {}", req.getPhoneNumber());

        try {
            // 기존 비즈니스 로직 유지 (One-ID → DB 순서)
            AuthResponseDTO authResponse = authService.loginProcess(req, servletRequest);

            // 모바일 뷰에서 사용할 수 있도록 쿠키에도 토큰 설정
            cookieUtil.setMemberTokensWithSync(servletResponse,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken());

            log.info("API login successful for member: {} (ID: {})",
                    authResponse.getMember().getName(),
                    authResponse.getMember().getId());

            return ResponseDTO.ok(authResponse);

        } catch (Exception e) {
            log.error("API login failed for phone: {}, error: {}", req.getPhoneNumber(), e.getMessage());
            throw e; // 기존 예외 처리 로직 유지
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
            description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. API에서는 자동 갱신이 없으므로 클라이언트에서 수동으로 호출해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 또는 만료된 리프레시 토큰")
    })
    @PostMapping("/refresh")
    public ResponseDTO<AuthResponseDTO> refreshToken(
            @Parameter(
                    description = "리프레시 토큰 정보 (JSON 형식)",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = """
                        {
                            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                        }
                        """
                            )
                    )
            )
            @RequestBody Map<String, String> request,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse
    ) {
        String refreshToken = request.get("refreshToken");

        log.debug("API token refresh requested");

        try {
            AuthResponseDTO authResponse = authService.refreshToken(refreshToken, servletRequest);

            // 모바일 뷰에서 사용할 수 있도록 쿠키에도 토큰 업데이트
            cookieUtil.setMemberTokensWithSync(servletResponse,
                    authResponse.getToken(),
                    authResponse.getRefreshToken());

            log.info("API token refresh successful for member: {} (ID: {})",
                    authResponse.getMember().getName(),
                    authResponse.getMember().getId());

            return ResponseDTO.ok(authResponse);

        } catch (Exception e) {
            log.error("API token refresh failed, error: {}", e.getMessage());
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

            // 🆕 쿠키도 함께 정리
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

    /**
     * 추가 기능: 토큰 정보 조회 (디버깅용)
     */
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
            @Parameter(hidden = true) Authentication auth
    ) {
        log.debug("Token info requested for member: {} (ID: {})",
                userDetails.getMember().getName(),
                userDetails.getMember().getId());

        return ResponseDTO.ok(Map.of(
                "memberId", userDetails.getMember().getId(),
                "memberName", userDetails.getMember().getName(),
                "memberRole", userDetails.getMember().getRole().getDisplayName(),
                "memberStatus", userDetails.getMember().getStatus().getDisplayName(),
                "authenticated", auth.isAuthenticated(),
                "authorities", auth.getAuthorities(),
                "tokenType", "MEMBER_ACCESS_TOKEN"
        ));
    }
}