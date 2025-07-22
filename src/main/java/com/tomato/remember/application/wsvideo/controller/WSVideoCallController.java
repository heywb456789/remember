package com.tomato.remember.application.wsvideo.controller;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoResponse;
import com.tomato.remember.application.wsvideo.service.MemorialExternalApiService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket 기반 영상통화 페이지 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/mobile/ws-call")
@RequiredArgsConstructor
public class WSVideoCallController {

    private final MemorialExternalApiService memorialExternalApiService;

    /**
     * WebSocket 영상통화 페이지 진입
     * GET /call/memorial?memorialId=123
     */
    @GetMapping("/memorial")
    public String showVideoCallPage(@RequestParam Long memorialId,
                                  @AuthenticationPrincipal Member member,
                                  Model model) {
        
        log.info("🎬 WebSocket 영상통화 페이지 요청 - 회원ID: {}, 메모리얼ID: {}", 
                member.getId(), memorialId);

        try {
            // 외부 API 호출하여 대기영상 및 연락처 정보 받기
            MemorialVideoResponse apiResponse =
                memorialExternalApiService.getMemorialVideoInfo(member.getId(), memorialId);

            // 모델에 데이터 추가
            model.addAttribute("memberId", member.getId());
            model.addAttribute("memorialId", memorialId);
            model.addAttribute("contactName", apiResponse.getContactName());
            model.addAttribute("waitingVideoUrl", apiResponse.getWaitingVideoUrl());
            model.addAttribute("memberName", member.getName());

            log.info("✅ 외부 API 응답 성공 - 연락처: {}, 대기영상: {}", 
                    apiResponse.getContactName(), apiResponse.getWaitingVideoUrl());

            return "ws-video-call"; // ws-video-call.html 템플릿 반환

        } catch (Exception e) {
            log.error("❌ 외부 API 호출 실패 - 회원ID: {}, 메모리얼ID: {}", 
                    member.getId(), memorialId, e);

            // 실패 시 기본값으로 페이지 제공
            model.addAttribute("memberId", member.getId());
            model.addAttribute("memorialId", memorialId);
            model.addAttribute("contactName", "김근태"); // 기본값
            model.addAttribute("waitingVideoUrl", "https://remember.newstomato.com/static/waiting_no.mp4");
            model.addAttribute("memberName", member.getName());
            model.addAttribute("errorMessage", "일부 정보를 기본값으로 설정했습니다.");

            return "mobile/wsvideocall/ws-video-call";
        }
    }

    /**
     * 피드백 페이지 (통화 종료 후)
     * GET /call/feedback?memberId=123&memorialId=456
     */
    @GetMapping("/feedback")
    public String showFeedbackPage(@RequestParam Long memberId,
                                 @RequestParam Long memorialId,
                                 Model model) {
        
        log.info("📝 피드백 페이지 요청 - 회원ID: {}, 메모리얼ID: {}", memberId, memorialId);

        model.addAttribute("memberId", memberId);
        model.addAttribute("memorialId", memorialId);

        return "call-feedback"; // call-feedback.html 템플릿 반환
    }

    /**
     * 영상통화 테스트 페이지 (개발용)
     * GET /call/test?memorialId=123
     */
    @GetMapping("/test")
    public String showTestPage(@RequestParam(required = false, defaultValue = "1") Long memorialId,
                             @RequestParam(required = false, defaultValue = "1") Long memberId,
                             Model model) {
        
        log.info("🧪 영상통화 테스트 페이지 요청 - 회원ID: {}, 메모리얼ID: {}", memberId, memorialId);

        // 테스트용 기본값
        model.addAttribute("memberId", memberId);
        model.addAttribute("memorialId", memorialId);
        model.addAttribute("contactName", "김근태");
        model.addAttribute("waitingVideoUrl", "https://remember.newstomato.com/static/waiting_no.mp4");
        model.addAttribute("memberName", "테스터");
        model.addAttribute("isTestMode", true);

        return "mobile/wsvideocall/ws-video-call";
    }

    /**
     * 모바일 웹 전용 영상통화 페이지
     * GET /call/mobile?memorialId=123
     */
    @GetMapping("/mobile")
    public String showMobileVideoCallPage(@RequestParam Long memorialId,
                                        @AuthenticationPrincipal Member member,
                                        Model model) {
        
        log.info("📱 모바일 영상통화 페이지 요청 - 회원ID: {}, 메모리얼ID: {}", 
                member.getId(), memorialId);

        try {
            // 외부 API 호출
            MemorialVideoResponse apiResponse =
                memorialExternalApiService.getMemorialVideoInfo(member.getId(), memorialId);

            // 모바일용 모델 데이터
            model.addAttribute("memberId", member.getId());
            model.addAttribute("memorialId", memorialId);
            model.addAttribute("contactName", apiResponse.getContactName());
            model.addAttribute("waitingVideoUrl", apiResponse.getWaitingVideoUrl());
            model.addAttribute("memberName", member.getName());
            model.addAttribute("isMobile", true);

            return "ws-video-call-mobile"; // 모바일 전용 템플릿

        } catch (Exception e) {
            log.error("❌ 모바일 외부 API 호출 실패", e);

            // 기본값으로 폴백
            model.addAttribute("memberId", member.getId());
            model.addAttribute("memorialId", memorialId);
            model.addAttribute("contactName", "김근태");
            model.addAttribute("waitingVideoUrl", "https://remember.newstomato.com/static/waiting_no.mp4");
            model.addAttribute("memberName", member.getName());
            model.addAttribute("isMobile", true);
            model.addAttribute("errorMessage", "기본 설정으로 시작합니다.");

            return "ws-video-call-mobile";
        }
    }

    /**
     * 영상통화 가능 여부 체크 API
     * GET /call/check?memorialId=123
     */
    @ResponseBody
    @GetMapping("/check")
    public ResponseEntity<?> checkVideoCallAvailable(@RequestParam Long memorialId,
                                                   @AuthenticationPrincipal Member member) {
        
        log.info("🔍 영상통화 가능 여부 체크 - 회원ID: {}, 메모리얼ID: {}", 
                member.getId(), memorialId);

        try {
            // 외부 API 호출하여 가능 여부 확인
            boolean available = memorialExternalApiService.checkVideoCallAvailable(member.getId(), memorialId);

            Map<String, Object> response = Map.of(
                "available", available,
                "memberId", member.getId(),
                "memorialId", memorialId,
                "message", available ? "영상통화가 가능합니다" : "현재 영상통화를 이용할 수 없습니다"
            );

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "체크 완료"),
                "response", response
            ));

        } catch (Exception e) {
            log.error("❌ 영상통화 가능 여부 체크 실패", e);

            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "체크 실패"),
                "error", e.getMessage()
            ));
        }
    }
}