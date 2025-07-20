package com.tomato.remember.application.auth.controller;

import com.tomato.remember.application.auth.dto.PasswordResetRequestDTO;
import com.tomato.remember.application.auth.dto.PasswordResetResponseDTO;
import com.tomato.remember.application.auth.service.PasswordResetService;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
@Tag(name = "비밀번호 재설정", description = "비밀번호 찾기 및 재설정 API")
public class PasswordResetRestController {

    private final PasswordResetService passwordResetService;

    @Operation(
        summary = "비밀번호 재설정 인증번호 발송",
        description = "휴대폰 번호로 비밀번호 재설정용 인증번호를 발송합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "가입되지 않은 휴대폰 번호")
    })
    @PostMapping("/send-code")
    public ResponseDTO<PasswordResetResponseDTO> sendVerificationCode(
            @Valid @RequestBody PasswordResetRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    ) {
        String clientInfo = String.format("%s %s", 
                servletRequest.getRemoteAddr(), 
                servletRequest.getHeader("User-Agent"));
        
        log.info("비밀번호 재설정 인증번호 발송 요청: phone={}, client={}", 
                 request.getPhoneNumber(), clientInfo);

        PasswordResetResponseDTO response = passwordResetService.sendVerificationCode(request);
        
        if (response.isSuccess()) {
            log.info("비밀번호 재설정 인증번호 발송 성공: {}", request.getPhoneNumber());
            return ResponseDTO.ok(response);
        } else {
            log.warn("비밀번호 재설정 인증번호 발송 실패: {} - {}", 
                     request.getPhoneNumber(), response.getMessage());
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "비밀번호 재설정 인증번호 확인",
        description = "발송된 인증번호가 올바른지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증번호 확인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 인증번호")
    })
    @PostMapping("/verify-code")
    public ResponseDTO<PasswordResetResponseDTO> verifyCode(
            @Valid @RequestBody PasswordResetRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    ) {
        String clientInfo = String.format("%s %s", 
                servletRequest.getRemoteAddr(), 
                servletRequest.getHeader("User-Agent"));
                
        log.info("비밀번호 재설정 인증번호 확인 요청: phone={}, client={}", 
                 request.getPhoneNumber(), clientInfo);

        PasswordResetResponseDTO response = passwordResetService.verifyCode(request);
        
        if (response.isSuccess()) {
            log.info("비밀번호 재설정 인증번호 확인 성공: {}", request.getPhoneNumber());
            return ResponseDTO.ok(response);
        } else {
            log.warn("비밀번호 재설정 인증번호 확인 실패: {}", request.getPhoneNumber());
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "비밀번호 재설정",
        description = "인증 완료 후 새로운 비밀번호로 변경합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
        @ApiResponse(responseCode = "400", description = "비밀번호 재설정 실패")
    })
    @PostMapping("/reset")
    public ResponseDTO<PasswordResetResponseDTO> resetPassword(
            @Valid @RequestBody PasswordResetRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    ) {
        String clientInfo = String.format("%s %s", 
                servletRequest.getRemoteAddr(), 
                servletRequest.getHeader("User-Agent"));
                
        log.info("비밀번호 재설정 요청: phone={}, client={}", 
                 request.getPhoneNumber(), clientInfo);

        PasswordResetResponseDTO response = passwordResetService.resetPassword(request);
        
        if (response.isSuccess()) {
            log.info("비밀번호 재설정 성공: {}", request.getPhoneNumber());
            return ResponseDTO.ok(response);
        } else {
            log.warn("비밀번호 재설정 실패: {} - {}", 
                     request.getPhoneNumber(), response.getMessage());
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}