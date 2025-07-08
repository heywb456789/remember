package com.tomato.naraclub.application.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tomato.naraclub.application.auth.dto.AuthRequestDTO;
import com.tomato.naraclub.application.auth.dto.AuthResponseDTO;
import com.tomato.naraclub.application.auth.service.AuthService;
import com.tomato.naraclub.application.member.dto.MemberDTO;
import com.tomato.naraclub.application.member.repository.MemberRepository;
import com.tomato.naraclub.application.oneld.dto.OneIdResponse;
import com.tomato.naraclub.application.oneld.dto.OneIdVerifyResponse;
import com.tomato.naraclub.application.oneld.service.TomatoAuthService;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.common.code.ResponseStatus;
import com.tomato.naraclub.common.dto.ResponseDTO;
import com.tomato.naraclub.common.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "모바일 웹 전용 로그인/회원가입/토큰 관리 API")
public class AuthRestController {

    private final AuthService authService;
    private final TomatoAuthService tomatoService;
    private final MemberRepository memberRepository;

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
    public Mono<ResponseDTO<?>> login(
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
        return tomatoService.authenticate(req.getPhoneNumber(), req.getPassword())
            .flatMap(resp -> {
                if (!resp.isResult()) {
                    return Mono.just(ResponseDTO.error(ResponseStatus.UNAUTHORIZED));
                }
                AuthResponseDTO responseDTO = authService.createToken(resp, req, servletRequest);
                return Mono.just(ResponseDTO.ok(responseDTO));
            })
            .onErrorResume(ResponseStatusException.class, ex -> {
                HttpStatusCode status = ex.getStatusCode();
                if (status == HttpStatus.UNAUTHORIZED) {
                    return Mono.just(ResponseDTO.error(ResponseStatus.UNAUTHORIZED_ONE_ID));
                } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                    return Mono.just(ResponseDTO.error(ResponseStatus.INTERNAL_SERVER_ERROR));
                }
                return Mono.error(ex);
            });
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
        @Parameter(
            description = "SMS 발송 정보",
            required = true,
            content = @Content(
                schema = @Schema(
                    implementation = AuthRequestDTO.class,
                    example = """
                        {
                            "phoneNumber": "01012341234",
                            "name": "홍길동",
                            "birthday": "19900101"
                        }
                        """
                )
            )
        )
        @RequestBody AuthRequestDTO req
    ) {
        return tomatoService.sendSmsCert(req)
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
        @Parameter(
            description = "SMS 인증 정보",
            required = true,
            content = @Content(
                schema = @Schema(
                    implementation = AuthRequestDTO.class,
                    example = """
                        {
                            "phoneNumber": "01012341234",
                            "certNum": "123456"
                        }
                        """
                )
            )
        )
        @RequestBody AuthRequestDTO req
    ) {
        return tomatoService.verifySmsCert(req)
            .map(ResponseDTO::ok);
    }

    @Operation(
        summary = "회원가입",
        description = "새로운 사용자 계정을 생성하고 JWT 토큰을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
        )
    })
    @PostMapping("/register")
    public ResponseDTO<AuthResponseDTO> register(
        @Parameter(description = "회원가입 정보", required = true)
        @RequestBody @Valid AuthRequestDTO req,
        @Parameter(hidden = true) HttpServletRequest servletRequest
    ) {
        OneIdResponse resp = tomatoService.createOneId(req);

        if (resp == null || !resp.isResult()) {
            throw new BadRequestException("외부 회원 인증 실패");
        }

        return ResponseDTO.ok(authService.createToken(resp, req, servletRequest));
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