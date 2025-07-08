package com.tomato.remember.application;

import com.tomato.remember.application.auth.dto.AuthRequestDTO;
import com.tomato.remember.application.auth.dto.AuthResponseDTO;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.security.MemberUserDetailsService;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MobileApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberUserDetailsService memberUserDetailsService;

    // =========================== 인증 관련 API ===========================

    /**
     * 회원 로그인 API
     */
    @PostMapping("/auth/login")
    public ResponseDTO<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        try {
            // 인증 처리 (토마토 ONE-ID 방식)
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
            );

            MemberUserDetails memberDetails = (MemberUserDetails) authentication.getPrincipal();
            Member member = memberDetails.getMember();
            
            // JWT 토큰 생성
            String accessToken = tokenProvider.createMemberAccessToken(member);
            String refreshToken = tokenProvider.createMemberRefreshToken(member, request.isAutoLogin());

            AuthResponseDTO loginResponse = AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24시간
//                .member(member.builder()
//                    .id(member.getId())
//                    .email(member.getEmail())
//                    .name(member.getName())
//                    .phoneNumber(member.getPhoneNumber())
//                    .role(member.getRole().name())
//                    .status(member.getStatus().name())
//                    .build())
                .build();

            log.info("Member login successful: {}", member.getPhoneNumber());
            return ResponseDTO.ok(loginResponse);

        } catch (Exception e) {
            log.error("Member login failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("휴대폰 번호 또는 비밀번호가 올바르지 않습니다.");
            return ResponseDTO.ok();
        }
    }

//    /**
//     * 회원 토큰 갱신 API
//     */
//    @PostMapping("/auth/refresh")
//    public ResponseDTO<MemberRefreshResponse> refresh(@RequestBody Map<String, String> request) {
//        try {
//            String refreshToken = request.get("refreshToken");
//
//            // Refresh Token 검증
//            if (!tokenProvider.validateMemberToken(refreshToken)) {
//                return ResponseDTO.badRequest("유효하지 않은 리프레시 토큰입니다.");
//            }
//
//            // 회원 정보 추출
//            String memberId = tokenProvider.getSubject(refreshToken);
//            MemberUserDetails memberDetails = (MemberUserDetails) memberUserDetailsService.loadUserByUsername(memberId);
//            Member member = memberDetails.getMember();
//
//            // 새 토큰 생성
//            String newAccessToken = tokenProvider.createMemberAccessToken(member);
//            String newRefreshToken = tokenProvider.createMemberRefreshToken(member, false);
//
//            MemberRefreshResponse refreshResponse = MemberRefreshResponse.builder()
//                .accessToken(newAccessToken)
//                .refreshToken(newRefreshToken)
//                .tokenType("Bearer")
//                .expiresIn(86400)
//                .build();
//
//            log.info("Member token refreshed: {}", memberId);
//            return ResponseDTO.ok(refreshResponse);
//
//        } catch (Exception e) {
//            log.error("Member token refresh failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("토큰 갱신에 실패했습니다.");
//        }
//    }
//
//    /**
//     * 회원 정보 조회 API
//     */
//    @GetMapping("/auth/me")
//    public ResponseDTO<MemberInfo> getCurrentMember(@AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            Member member = memberDetails.getMember();
//
//            MemberInfo memberInfo = MemberInfo.builder()
//                .id(member.getId())
//                .email(member.getEmail())
//                .name(member.getName())
//                .phoneNumber(member.getPhoneNumber())
//                .role(member.getRole().name())
//                .status(member.getStatus().name())
//                .build();
//
//            return ResponseDTO.ok(memberInfo);
//        } catch (Exception e) {
//            log.error("Get current member failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("회원 정보 조회에 실패했습니다.");
//        }
//    }
//
//    /**
//     * 회원 토큰 검증 API (모바일 뷰에서 사용)
//     */
//    @GetMapping("/auth/validate")
//    public ResponseEntity<Void> validateToken() {
//        // ApiJwtFilter에서 이미 토큰 검증이 완료됨
//        // 204 No Content 응답으로 인증 상태 확인
//        return ResponseEntity.noContent().build();
//    }
//
//    /**
//     * 회원 로그아웃 API
//     */
//    @PostMapping("/auth/logout")
//    public ResponseDTO<Void> logout(@AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            if (memberDetails != null) {
//                log.info("Member logout: {}", memberDetails.getMember().getId());
//                // TODO: 토큰 블랙리스트 처리 (필요시)
//            }
//
//            return ResponseDTO.ok();
//        } catch (Exception e) {
//            log.error("Member logout error: {}", e.getMessage());
//            return ResponseDTO.ok(); // 로그아웃은 항상 성공으로 처리
//        }
//    }
//
//    // =========================== 메모리얼 관련 API ===========================
//
//    /**
//     * 메모리얼 목록 조회 API
//     */
//    @GetMapping("/memorial")
//    public ResponseDTO<Map<String, Object>> getMemorials(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            Long memberId = memberDetails.getMember().getId();
//            log.info("Member {} requested memorial list", memberId);
//
//            // TODO: 메모리얼 목록 조회 로직
//            Map<String, Object> result = Map.of(
//                "content", "TODO: 메모리얼 목록",
//                "page", page,
//                "size", size,
//                "totalElements", 0
//            );
//
//            return ResponseDTO.ok(result);
//        } catch (Exception e) {
//            log.error("Get memorials failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("메모리얼 목록 조회에 실패했습니다.");
//        }
//    }
//
//    /**
//     * 메모리얼 생성 API
//     */
//    @PostMapping("/memorial")
//    public ResponseDTO<Map<String, Object>> createMemorial(@Valid @RequestBody CreateMemorialRequest request,
//                                                         @AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            Long memberId = memberDetails.getMember().getId();
//            log.info("Member {} creating memorial: {}", memberId, request.getName());
//
//            // TODO: 메모리얼 생성 로직
//            Map<String, Object> result = Map.of(
//                "memorialId", "TODO: 실제 ID",
//                "message", "메모리얼이 성공적으로 생성되었습니다."
//            );
//
//            return ResponseDTO.ok(result);
//        } catch (Exception e) {
//            log.error("Create memorial failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("메모리얼 생성에 실패했습니다.");
//        }
//    }
//
//    /**
//     * 메모리얼 상세 조회 API
//     */
//    @GetMapping("/memorial/{id}")
//    public ResponseDTO<Map<String, Object>> getMemorial(@PathVariable Long id,
//                                                      @AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            Long memberId = memberDetails.getMember().getId();
//            log.info("Member {} requested memorial detail: {}", memberId, id);
//
//            // TODO: 메모리얼 상세 조회 및 권한 확인 로직
//            Map<String, Object> result = Map.of(
//                "memorial", "TODO: 메모리얼 상세 정보"
//            );
//
//            return ResponseDTO.ok(result);
//        } catch (Exception e) {
//            log.error("Get memorial failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("메모리얼 조회에 실패했습니다.");
//        }
//    }
//
//    // =========================== 영상통화 관련 API ===========================
//
//    /**
//     * 영상통화 시작 API
//     */
//    @PostMapping("/video-call/{memorialId}/start")
//    public ResponseDTO<Map<String, Object>> startVideoCall(@PathVariable Long memorialId,
//                                                         @AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            Long memberId = memberDetails.getMember().getId();
//            log.info("Member {} starting video call with memorial: {}", memberId, memorialId);
//
//            // TODO: 영상통화 시작 로직
//            Map<String, Object> result = Map.of(
//                "sessionId", "TODO: 세션 ID",
//                "status", "started"
//            );
//
//            return ResponseDTO.ok(result);
//        } catch (Exception e) {
//            log.error("Start video call failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("영상통화 시작에 실패했습니다.");
//        }
//    }
//
//    /**
//     * 음성 처리 API
//     */
//    @PostMapping("/video-call/{memorialId}/process")
//    public ResponseDTO<Map<String, Object>> processAudio(
//            @PathVariable Long memorialId,
//            @RequestParam("audio") MultipartFile audioFile,
//            @AuthenticationPrincipal MemberUserDetails memberDetails) {
//        try {
//            Long memberId = memberDetails.getMember().getId();
//            log.info("Member {} processing audio for memorial: {}", memberId, memorialId);
//
//            // TODO: 음성 처리 로직
//            Map<String, Object> result = Map.of(
//                "transcript", "TODO: 음성 인식 결과",
//                "response", "TODO: AI 응답",
//                "videoUrl", "TODO: 응답 영상 URL"
//            );
//
//            return ResponseDTO.ok(result);
//        } catch (Exception e) {
//            log.error("Process audio failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("음성 처리에 실패했습니다.");
//        }
//    }
//
//    // =========================== DTO 클래스들 ===========================
//
//    @Data
//    @Builder
//    public static class MemberLoginRequest {
//        @NotBlank(message = "휴대폰 번호는 필수입니다.")
//        private String phoneNumber; // 토마토 ONE-ID
//
//        @NotBlank(message = "비밀번호는 필수입니다.")
//        private String password;
//
//        private boolean autoLogin = false;
//    }
//
//    @Data
//    @Builder
//    public static class MemberLoginResponse {
//        private String accessToken;
//        private String refreshToken;
//        private String tokenType;
//        private int expiresIn;
//        private MemberInfo memberInfo;
//    }
//
//    @Data
//    @Builder
//    public static class MemberRefreshResponse {
//        private String accessToken;
//        private String refreshToken;
//        private String tokenType;
//        private int expiresIn;
//    }
//
//    @Data
//    @Builder
//    public static class MemberInfo {
//        private Long id;
//        private String email;
//        private String name;
//        private String phoneNumber;
//        private String role;
//        private String status;
//    }
//
//    @Data
//    @Builder
//    public static class CreateMemorialRequest {
//        @NotBlank(message = "이름은 필수입니다.")
//        private String name;
//
//        @NotBlank(message = "관계는 필수입니다.")
//        private String relationship;
//
//        private String birthDate;
//        private String deathDate;
//        private String gender;
//        private String interests;
//    }
}