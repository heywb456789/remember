package com.tomato.remember.application.videocall.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Memorial Video Call SSR 컨트롤러
 * Thymeleaf 뷰 렌더링 전용
 */
@Slf4j
@Controller
@RequestMapping("/videocall")
public class MemorialVideoCallController {
    
    /**
     * Memorial Video Call 메인 페이지 (WebSocket 기반)
     */
    @GetMapping("/memorial-video-call")
    public String memorialVideoCallPage(
            @RequestParam(value = "contact", required = false, defaultValue = "김근태") String contactName,
            @RequestParam(value = "memorialId", required = false) Long memorialId,
            @RequestParam(value = "callerId", required = false) Long callerId,
            @RequestParam(value = "sessionKey", required = false) String sessionKey,
            @RequestParam(value = "debug", required = false, defaultValue = "false") boolean debug,
            Model model) {
        
        log.info("🎬 Memorial Video Call 페이지 접근 - 연락처: {}, Memorial ID: {}, Caller ID: {}", 
                contactName, memorialId, callerId);
        
        // 모델에 파라미터 전달
        model.addAttribute("contactName", contactName);
        model.addAttribute("memorialId", memorialId != null ? memorialId : "");
        model.addAttribute("callerId", callerId != null ? callerId : "");
        model.addAttribute("existingSessionKey", sessionKey != null ? sessionKey : "");
        model.addAttribute("debugMode", debug);
        
        // 서버 정보
        model.addAttribute("serverInfo", 
            "Memorial Video Call WebSocket - TTL: 1시간, 하트비트: 30초");
        
        return "videocall/memorial-video-call";
    }
    
    /**
     * Memorial Video Call 관리자 페이지
     */
    @GetMapping("/memorial-video-call/admin")
    public String memorialVideoCallAdminPage(Model model) {
        log.info("👨‍💼 Memorial Video Call 관리자 페이지 접근");
        
        model.addAttribute("pageTitle", "Memorial Video Call 관리자");
        model.addAttribute("apiEndpoint", "/api/memorial-video");
        model.addAttribute("wsEndpoint", "/ws/memorial-video");
        
        return "videocall/memorial-video-call-admin";
    }
    
    /**
     * Memorial Video Call 테스트 페이지
     */
    @GetMapping("/memorial-video-call/test")
    public String memorialVideoCallTestPage(
            @RequestParam(value = "scenario", required = false, defaultValue = "basic") String scenario,
            Model model) {
        
        log.info("🧪 Memorial Video Call 테스트 페이지 접근 - 시나리오: {}", scenario);
        
        model.addAttribute("testScenario", scenario);
        model.addAttribute("pageTitle", "Memorial Video Call 테스트");
        
        // 테스트 시나리오별 설정
        switch (scenario) {
            case "reconnect":
                model.addAttribute("testDescription", "재연결 기능 테스트");
                model.addAttribute("autoReconnect", true);
                break;
            case "heartbeat":
                model.addAttribute("testDescription", "하트비트 기능 테스트");
                model.addAttribute("heartbeatVisible", true);
                break;
            case "ttl":
                model.addAttribute("testDescription", "TTL 만료 테스트");
                model.addAttribute("shortTtl", true);
                break;
            default:
                model.addAttribute("testDescription", "기본 기능 테스트");
                break;
        }
        
        return "videocall/memorial-video-call-test";
    }
    
    /**
     * Memorial Video Call 모바일 페이지
     */
    @GetMapping("/memorial-video-call/mobile")
    public String memorialVideoCallMobilePage(
            @RequestParam(value = "contact", required = false, defaultValue = "김근태") String contactName,
            @RequestParam(value = "memorialId", required = false) Long memorialId,
            @RequestParam(value = "callerId", required = false) Long callerId,
            Model model) {
        
        log.info("📱 Memorial Video Call 모바일 페이지 접근 - 연락처: {}", contactName);
        
        model.addAttribute("contactName", contactName);
        model.addAttribute("memorialId", memorialId != null ? memorialId : "");
        model.addAttribute("callerId", callerId != null ? callerId : "");
        model.addAttribute("isMobile", true);
        model.addAttribute("optimizedForMobile", true);
        
        return "videocall/memorial-video-call-mobile";
    }
    
    /**
     * 특정 세션으로 바로 접근 (딥링크)
     */
    @GetMapping("/memorial-video-call/session/{sessionKey}")
    public String joinExistingSession(
            @PathVariable String sessionKey,
            @RequestParam(value = "contact", required = false) String contactName,
            Model model) {
        
        log.info("🔗 기존 세션 접근 - 세션: {}, 연락처: {}", sessionKey, contactName);
        
        model.addAttribute("existingSessionKey", sessionKey);
        model.addAttribute("contactName", contactName != null ? contactName : "알 수 없음");
        model.addAttribute("directJoin", true);
        model.addAttribute("pageTitle", "Memorial Video Call - 세션 복구");
        
        return "videocall/memorial-video-call";
    }
    
    /**
     * WebSocket 연결 상태 페이지
     */
    @GetMapping("/memorial-video-call/status")
    public String webSocketStatusPage(Model model) {
        log.info("📊 WebSocket 상태 페이지 접근");
        
        model.addAttribute("pageTitle", "Memorial Video Call WebSocket 상태");
        model.addAttribute("refreshInterval", 5); // 5초마다 자동 새로고침
        
        return "videocall/memorial-video-call-status";
    }
    
    /**
     * 에러 페이지
     */
    @GetMapping("/memorial-video-call/error")
    public String errorPage(
            @RequestParam(value = "code", required = false, defaultValue = "UNKNOWN") String errorCode,
            @RequestParam(value = "message", required = false) String errorMessage,
            Model model) {
        
        log.warn("❌ Memorial Video Call 에러 페이지 접근 - 코드: {}, 메시지: {}", errorCode, errorMessage);
        
        model.addAttribute("errorCode", errorCode);
        model.addAttribute("errorMessage", errorMessage != null ? errorMessage : "알 수 없는 오류가 발생했습니다");
        model.addAttribute("pageTitle", "Memorial Video Call - 오류");
        
        // 에러 코드별 안내 메시지
        String guidanceMessage = switch (errorCode) {
            case "SESSION_EXPIRED" -> "세션이 만료되었습니다. 새로 시작해주세요.";
            case "WEBSOCKET_ERROR" -> "WebSocket 연결에 문제가 있습니다. 네트워크를 확인해주세요.";
            case "PERMISSION_DENIED" -> "카메라 또는 마이크 권한이 필요합니다.";
            case "BROWSER_NOT_SUPPORTED" -> "지원하지 않는 브라우저입니다. Chrome, Safari, Firefox를 사용해주세요.";
            default -> "문제가 지속되면 페이지를 새로고침하거나 관리자에게 문의하세요.";
        };
        
        model.addAttribute("guidanceMessage", guidanceMessage);
        
        return "videocall/memorial-video-call-error";
    }
    
    /**
     * 도움말 페이지
     */
    @GetMapping("/memorial-video-call/help")
    public String helpPage(Model model) {
        log.info("❓ Memorial Video Call 도움말 페이지 접근");
        
        model.addAttribute("pageTitle", "Memorial Video Call 도움말");
        model.addAttribute("version", "WebSocket v1.0");
        
        // 도움말 섹션들
        model.addAttribute("features", new String[]{
            "실시간 WebSocket 통신",
            "세션 자동 복구 (새로고침 시)",
            "TTL 기반 세션 관리 (1시간)",
            "하트비트 연결 유지 (30초)",
            "모바일/데스크톱 최적화",
            "권한 자동 감지 및 복구"
        });
        
        model.addAttribute("troubleshooting", new String[]{
            "연결이 끊어지면 자동으로 재연결됩니다",
            "새로고침해도 세션이 유지됩니다",
            "1시간 후 자동으로 세션이 만료됩니다",
            "카메라/마이크 권한을 허용해주세요",
            "Chrome, Safari, Firefox 브라우저를 권장합니다"
        });
        
        return "videocall/memorial-video-call-help";
    }
}