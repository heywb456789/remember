package com.tomato.remember.application.auth.controller;

import com.tomato.remember.application.auth.dto.AuthRequestDTO;
import com.tomato.remember.application.auth.dto.AuthResponseDTO;
import com.tomato.remember.application.auth.service.AuthService;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "모바일 웹 전용 로그인/회원가입/토큰 관리 API")
public class AuthRestController {

    private final AuthService authService;

    @Operation(
        summary = "토큰 유효성 검증",
        description = "현재 JWT 토큰이 유효한지 검증합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "토큰 유효"),
        @ApiResponse(responseCode = "401", description = "토큰 무효",
            content = @Content(schema = @Schema(implementation = ResponseDTO.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/validate")
    public ResponseEntity<Void> validate(
        @Parameter(hidden = true) Authentication auth
    ) {
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
        return ResponseDTO.ok(authService.me(user));
    }

    @Operation(
        summary = "로그인",
        description = "휴대폰 번호와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = ResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseDTO<AuthResponseDTO> login(
        @Parameter(
            description = "로그인 정보",
            required = true,
            content = @Content(
                schema = @Schema(
                    implementation = AuthRequestDTO.class,
                    example = """
                        {
                            "phoneNumber": "01012341234",
                            "password": "password123"
                        }
                        """
                )
            )
        )
        @RequestBody AuthRequestDTO req,
        @Parameter(hidden = true) HttpServletRequest servletRequest
    ) {
        AuthResponseDTO authResponse = authService.loginProcess(req, servletRequest);
        return ResponseDTO.ok(authResponse);
    }

    @Operation(
        summary = "로그아웃",
        description = "현재 세션을 종료하고 토큰을 무효화합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/logout")
    public ResponseDTO<?> logout(
        @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
        @Parameter(hidden = true) HttpServletRequest request
    ) {
        authService.logout(userDetails, request);
        return ResponseDTO.ok();
    }

    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    @PostMapping("/refresh")
    public ResponseDTO<AuthResponseDTO> refreshToken(
        @Parameter(
            description = "리프레시 토큰 정보",
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
        @Parameter(hidden = true) HttpServletRequest servletRequest
    ) {
        return ResponseDTO.ok(
            authService.refreshToken(request.get("refreshToken"), servletRequest));
    }

    @Operation(
        summary = "회원탈퇴",
        description = "사용자 계정을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/delete")
    public ResponseDTO<?> delete(
        @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
        @Parameter(hidden = true) HttpServletRequest request
    ) {
        authService.delete(userDetails, request);
        return ResponseDTO.ok();
    }
}