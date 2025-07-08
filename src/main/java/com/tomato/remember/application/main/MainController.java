package com.tomato.remember.application.main;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * 메인 페이지 컨트롤러
 * - "/" "/home" 등 기본 URL 처리
 * - SecurityConfig Order(5)에서 처리
 * - 로그인 전후 상태에 따른 적절한 페이지 제공
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    /**
     * 메인 홈페이지
     * - 로그인 전: 서비스 소개 + 체험 페이지
     * - 로그인 후: 대시보드로 리다이렉트
     */
    @GetMapping({"/", "/home"})
    public String homePage(Model model, HttpServletRequest request) {
        log.info("🏠 === Home Page Access ===");
        log.info("📍 Request URI: {}", request.getRequestURI());
        log.info("🌐 User Agent: {}", request.getHeader("User-Agent"));
        log.info("👤 Remote Address: {}", request.getRemoteAddr());

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "토마토리멤버 - 소중한 추억을 영원히 간직하세요");
        model.addAttribute("appName", "토마토리멤버");

        // 체험용 캐릭터 정보
        model.addAttribute("experienceCharacters", new String[]{
            "노무현", "김광석"
        });

        // API 엔드포인트 정보 제공
        model.addAttribute("loginUrl", "/mobile/login");
        model.addAttribute("registerUrl", "/mobile/register");
        model.addAttribute("videoCallExperienceUrl", "/experience/video-call");
        model.addAttribute("serviceGuideUrl", "https://youtube.com/watch?v=example"); // 실제 URL로 변경

        return "/mobile/main/index";
    }

    /**
     * About 페이지
     */
    @GetMapping("/about")
    public String aboutPage(Model model) {
        log.info("📖 === About Page Access ===");

        model.addAttribute("pageTitle", "서비스 소개 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");

        return "main/about";
    }

    /**
     * Contact 페이지
     */
    @GetMapping("/contact")
    public String contactPage(Model model) {
        log.info("📞 === Contact Page Access ===");

        model.addAttribute("pageTitle", "문의하기 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        
        // 연락처 정보
        model.addAttribute("companyInfo", Map.of(
            "name", "이토마토",
            "address", "서울특별시 마포구 양화진 4길 32 이토마토빌딩 2층",
            "phone", "02-2128-3838",
            "email", "tomatoai@etomato.com"
        ));

        return "main/contact";
    }

    /**
     * 체험하기 - 노무현 대통령님/김광석님 영상통화 체험
     */
    @GetMapping("/experience")
    public String experiencePage(Model model) {
        log.info("🎭 === Experience Page Access ===");

        model.addAttribute("pageTitle", "체험하기 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");

        // 체험 캐릭터 정보
        model.addAttribute("characters", List.of(
            Map.of(
                "id", "roh",
                "name", "노무현 전 대통령님",
                "description", "따뜻한 리더십과 소통의 정치인",
                "imageUrl", "/images/characters/roh.jpg",
                "videoCallUrl", "/experience/video-call/roh"
            ),
            Map.of(
                "id", "kim",
                "name", "김광석님",
                "description", "한국을 대표하는 싱어송라이터",
                "imageUrl", "/images/characters/kim.jpg",
                "videoCallUrl", "/experience/video-call/kim"
            )
        ));

        return "main/experience";
    }

    /**
     * 체험용 영상통화 페이지
     */
    @GetMapping("/experience/video-call/{characterId}")
    public String experienceVideoCall(@PathVariable String characterId, Model model) {
        log.info("📹 === Experience Video Call - Character: {} ===", characterId);

        // 캐릭터별 정보 설정
        String characterName;
        String characterImage;
        switch (characterId) {
            case "roh":
                characterName = "노무현 전 대통령님";
                characterImage = "/images/characters/roh.jpg";
                break;
            case "kim":
                characterName = "김광석님";
                characterImage = "/images/characters/kim.jpg";
                break;
            default:
                // 잘못된 캐릭터 ID면 체험 페이지로 리다이렉트
                return "redirect:/experience";
        }

        model.addAttribute("pageTitle", characterName + " 통화 체험 - 토마토리멤버");
        model.addAttribute("characterId", characterId);
        model.addAttribute("characterName", characterName);
        model.addAttribute("characterImage", characterImage);
        
        // 체험용 API 엔드포인트 (인증 불필요)
        model.addAttribute("experienceApiUrl", "/api/experience/video-call/" + characterId);
        model.addAttribute("backUrl", "/experience");
        model.addAttribute("registerUrl", "/mobile/register");

        return "main/experience-video-call";
    }

    /**
     * 서비스 가이드 - YouTube 동영상 임베드
     */
    @GetMapping("/guide")
    public String guidePage(Model model) {
        log.info("📺 === Service Guide Page Access ===");

        model.addAttribute("pageTitle", "사용자 가이드 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        
        // YouTube 동영상 ID (실제 ID로 변경)
        model.addAttribute("guideVideoId", "dQw4w9WgXcQ"); // 예시 ID
        model.addAttribute("backUrl", "/");

        return "main/guide";
    }

    /**
     * 개인정보처리방침
     */
    @GetMapping("/privacy")
    public String privacyPage(Model model) {
        log.info("📋 === Privacy Policy Page Access ===");

        model.addAttribute("pageTitle", "개인정보처리방침 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");

        return "main/privacy";
    }

    /**
     * 서비스 약관
     */
    @GetMapping("/terms")
    public String termsPage(Model model) {
        log.info("📋 === Terms of Service Page Access ===");

        model.addAttribute("pageTitle", "서비스 약관 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");

        return "main/terms";
    }

    /**
     * 404 에러 페이지 (fallback)
     */
    @GetMapping("/404")
    public String notFoundPage(Model model) {
        log.info("❌ === 404 Page Access ===");

        model.addAttribute("pageTitle", "페이지를 찾을 수 없습니다 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("homeUrl", "/");

        return "error/404";
    }

    /**
     * 500 에러 페이지 (fallback)
     */
    @GetMapping("/500")
    public String serverErrorPage(Model model) {
        log.info("💥 === 500 Page Access ===");

        model.addAttribute("pageTitle", "서버 오류 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("homeUrl", "/");

        return "error/500";
    }

    /**
     * 임시 모바일 메인 페이지 (무한루프 방지용)
     * 실제로는 AuthController에서 처리하지만, 404 방지용으로 추가
     */
    @GetMapping("/mobile/main")
    public String mobileMainTemporary(Model model) {
        log.info("📱 === Temporary Mobile Main Page Access ===");

        model.addAttribute("pageTitle", "메인 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("message", "메인 페이지 준비 중입니다...");
        model.addAttribute("loginUrl", "/mobile/login");

        return "/mobile/main/mobile-temp";
    }
}