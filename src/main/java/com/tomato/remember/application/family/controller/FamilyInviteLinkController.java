package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.family.repository.FamilyInviteTokenRepository;
import com.tomato.remember.application.security.MemberUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 가족 초대 링크 처리 컨트롤러 (단순화된 버전)
 * - 초대 링크 접근 처리
 * - 토큰 유효성 검증
 * - sessionStorage 활용한 토큰 전달
 */
@Slf4j
@Controller
@RequestMapping("/mobile/family/invite")
@RequiredArgsConstructor
public class FamilyInviteLinkController {

    private final FamilyInviteTokenRepository inviteTokenRepository;

    /**
     * 초대 링크 접근 처리 (랜딩 페이지 방식)
     * GET /mobile/family/invite/{token}
     */
    @GetMapping("/{token}")
    public String handleInviteLink(
            @PathVariable String token,
            @AuthenticationPrincipal MemberUserDetails currentUser,
            HttpServletRequest request,
            Model model) {

        log.info("초대 링크 접근 - 토큰: {}, 사용자: {}, IP: {}",
                token.substring(0, 8) + "...",
                currentUser != null ? currentUser.getMember().getId() : "anonymous",
                getClientIp(request));

        try {
            // 1. 초대 토큰 검증
            Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                log.warn("유효하지 않은 초대 링크 접근 - 토큰: {}", token.substring(0, 8) + "...");
                return handleInvalidToken(model);
            }

            FamilyInviteToken inviteToken = tokenOpt.get();

            // 2. 토큰 사용 표시
            inviteToken.markAsUsed();
            inviteTokenRepository.save(inviteToken);

            // 3. 랜딩 페이지에서 사용할 초대 정보 설정
            model.addAttribute("inviteToken", token);
            model.addAttribute("inviterName", inviteToken.getInviterName());
            model.addAttribute("memorialName", inviteToken.getMemorialName());
            model.addAttribute("relationshipDisplayName", inviteToken.getRelationshipDisplayName());
            model.addAttribute("inviteMessage", inviteToken.getInviteMessage());
            model.addAttribute("method", inviteToken.getMethod());
            model.addAttribute("contact", inviteToken.getMaskedContact());

            // 4. 로그인 상태 정보 추가
            model.addAttribute("isLoggedIn", currentUser != null);
            if (currentUser != null) {
                model.addAttribute("currentUserName", currentUser.getMember().getName());
            }

            log.info("초대 랜딩 페이지로 이동 - 토큰: {}, 초대자: {}, 메모리얼: {}",
                    token.substring(0, 8) + "...",
                    inviteToken.getInviterName(),
                    inviteToken.getMemorialName());

            // 5. 랜딩 페이지로 이동
            return "mobile/family/invite-landing";

        } catch (Exception e) {
            log.error("초대 링크 처리 중 오류 발생 - 토큰: {}", token.substring(0, 8) + "...", e);

            model.addAttribute("error", "초대 링크 처리 중 오류가 발생했습니다.");
            return "mobile/family/invite-error";
        }
    }

    /**
     * 초대 토큰 검증 API (JavaScript에서 호출)
     * GET /mobile/family/invite/validate/{token}
     */
    @GetMapping("/validate/{token}")
    @ResponseBody
    public Map<String, Object> validateInviteToken(@PathVariable String token) {
        log.info("초대 토큰 검증 요청 - 토큰: {}", token.substring(0, 8) + "...");

        try {
            Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                return Map.of(
                        "valid", false,
                        "message", "유효하지 않거나 만료된 초대 토큰입니다."
                );
            }

            FamilyInviteToken inviteToken = tokenOpt.get();

            return Map.of(
                    "valid", true,
                    "message", "유효한 초대 토큰입니다.",
                    "inviterName", inviteToken.getInviterName(),
                    "memorialName", inviteToken.getMemorialName(),
                    "relationshipDisplayName", inviteToken.getRelationshipDisplayName(),
                    "remainingHours", inviteToken.getRemainingHours()
            );

        } catch (Exception e) {
            log.error("초대 토큰 검증 실패 - 토큰: {}", token.substring(0, 8) + "...", e);

            return Map.of(
                    "valid", false,
                    "message", "토큰 검증 중 오류가 발생했습니다."
            );
        }
    }

    // ===== 헬퍼 메서드 =====

    /**
     * 유효하지 않은 토큰 처리
     */
    private String handleInvalidToken(Model model) {
        model.addAttribute("error", "유효하지 않거나 만료된 초대 링크입니다.");
        model.addAttribute("errorType", "invalid_token");
        return "mobile/family/invite-error";
    }

    /**
     * 클라이언트 IP 조회
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}