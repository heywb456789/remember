package com.tomato.remember.application.main;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * 메인 페이지 컨트롤러 - "/" "/home" 등 기본 URL 처리 - SecurityConfig Order(5)에서 처리 - 로그인 전후 상태에 따른 적절한 페이지 제공
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    // TODO: 실제 서비스들이 구현되면 주석 해제
    // private final MemorialService memorialService;
    // private final SubscriptionService subscriptionService;
    // private final PaymentService paymentService;

    @GetMapping("/")
    public String root() {
        log.info("🏠 Root path accessed, redirecting to /mobile/home");
        return "redirect:/mobile/home";
    }

    /**
     * 메인 홈페이지 - 로그인 전: 서비스 소개 + 안내 페이지 - 로그인 후: 메모리얼 대시보드
     */
    @GetMapping("/mobile/home")
    public String homePage(@AuthenticationPrincipal MemberUserDetails userDetails,
                          Model model,
                          HttpServletRequest request) {

        log.info("🏠 === Main Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "토마토리멤버 - 소중한 추억을 영원히 간직하세요");
        model.addAttribute("appName", "토마토리멤버");

        // 로그인 상태에 따른 처리
        if (userDetails != null) {
            // 로그인한 사용자 처리
            Member currentUser = userDetails.getMember();
            log.info("👤 Logged in user: {} (ID: {})", currentUser.getName(), currentUser.getId());

            setupLoggedInUserData(model, currentUser);
        } else {
            // 비로그인 사용자 처리
            log.info("👤 Anonymous user access");
            setupAnonymousUserData(model);
        }

        // 공통 URL 정보
        setupCommonUrls(model);

        // 서비스 기본 정보
        setupServiceInfo(model);

        return "/mobile/main/main";
    }

    /**
     * 로그인한 사용자 데이터 설정
     */
    private void setupLoggedInUserData(Model model, Member currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isLoggedIn", true);

        // TODO: 실제 서비스 구현 시 주석 해제하고 수정
        /*
        try {
            // 사용자의 메모리얼 목록 조회
            List<Memorial> memorialList = memorialService.getMemorialListByUserId(currentUser.getId());
            model.addAttribute("memorialList", memorialList);
            model.addAttribute("memorialCount", memorialList.size());

            // 무료체험 상태 확인
            TrialStatus trialStatus = subscriptionService.getTrialStatus(currentUser.getId());
            model.addAttribute("showTrialBanner", trialStatus.isTrialUser());
            model.addAttribute("trialDaysRemaining", trialStatus.getDaysRemaining());
            model.addAttribute("isTrialUser", trialStatus.isTrialUser());
            model.addAttribute("maxMemorialsInTrial", trialStatus.getMaxMemorials());

            // 메모리얼 상태 정보 (온라인/오프라인, 마지막 방문일 등)
            if (!memorialList.isEmpty()) {
                List<MemorialStatus> statusList = memorialService.getMemorialStatusList(currentUser.getId());
                model.addAttribute("memorialStatusList", statusList);
            }

            // 사용자 토큰 잔액 정보
            TokenBalance tokenBalance = paymentService.getTokenBalance(currentUser.getId());
            model.addAttribute("tokenBalance", tokenBalance.getBalance());

        } catch (Exception e) {
            log.error("Error loading user data for user: {}", currentUser.getId(), e);
            // 에러 발생 시 기본값으로 설정
            setupDefaultUserData(model);
        }
        */

        // 임시 데이터 (실제 서비스 구현 전)
        setupMockUserData(model, currentUser);
    }

    /**
     * 임시 사용자 데이터 설정 (개발용)
     */
    private void setupMockUserData(Model model, Member currentUser) {
        // 메모리얼 목록 - 임시로 빈 리스트
        model.addAttribute("memorialList", new ArrayList<>());
        model.addAttribute("memorialCount", 0);

        // 무료체험 정보 - 신규 사용자는 체험 중으로 설정
        model.addAttribute("showTrialBanner", true);
        model.addAttribute("trialDaysRemaining", 90);
        model.addAttribute("isTrialUser", true);
        model.addAttribute("maxMemorialsInTrial", 3);

        // 토큰 잔액 - 기본 체험 토큰
        model.addAttribute("tokenBalance", 50000);

        log.info("📊 Mock data set for user: {} - Trial: {}, Token: {}",
                currentUser.getName(), true, 50000);
    }

    /**
     * 기본 사용자 데이터 설정 (에러 발생 시)
     */
    private void setupDefaultUserData(Model model) {
        model.addAttribute("memorialList", new ArrayList<>());
        model.addAttribute("memorialCount", 0);
        model.addAttribute("showTrialBanner", false);
        model.addAttribute("trialDaysRemaining", 0);
        model.addAttribute("isTrialUser", false);
        model.addAttribute("maxMemorialsInTrial", 0);
        model.addAttribute("tokenBalance", 0);
    }

    /**
     * 비로그인 사용자 데이터 설정
     */
    private void setupAnonymousUserData(Model model) {
        model.addAttribute("currentUser", null);
        model.addAttribute("isLoggedIn", false);
        model.addAttribute("memorialList", new ArrayList<>());
        model.addAttribute("memorialCount", 0);
        model.addAttribute("showTrialBanner", false);
        model.addAttribute("trialDaysRemaining", 0);
        model.addAttribute("isTrialUser", false);
        model.addAttribute("tokenBalance", 0);
    }

    /**
     * 공통 URL 정보 설정
     */
    private void setupCommonUrls(Model model) {
        model.addAttribute("loginUrl", "/mobile/login");
        model.addAttribute("registerUrl", "/mobile/register");
        model.addAttribute("memorialCreateUrl", "/mobile/memorial/create");
        model.addAttribute("paymentUrl", "/mobile/payment");
        model.addAttribute("profileUrl", "/mobile/profile");
        model.addAttribute("familyUrl", "/mobile/family");
    }

    /**
     * 서비스 기본 정보 설정
     */
    private void setupServiceInfo(Model model) {
        model.addAttribute("freeTrialMonths", 3);
        model.addAttribute("maxFamilyMembers", 8);
        model.addAttribute("supportEmail", "tomatoai@etomato.com");
        model.addAttribute("supportPhone", "02-2128-3838");
    }

    /**
     * About 페이지
     */
    @GetMapping("/about")
    public String aboutPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("📖 === About Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "서비스 소개 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);

        return "main/about";
    }

    /**
     * Contact 페이지
     */
    @GetMapping("/contact")
    public String contactPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("📞 === Contact Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "문의하기 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);

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
     * 개인정보처리방침
     */
    @GetMapping("/privacy")
    public String privacyPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("📋 === Privacy Policy Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "개인정보처리방침 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);

        return "main/privacy";
    }

    /**
     * 서비스 약관
     */
    @GetMapping("/terms")
    public String termsPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("📋 === Terms of Service Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "서비스 약관 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);

        return "main/terms";
    }

    /**
     * 404 에러 페이지 (fallback)
     */
    @GetMapping("/404")
    public String notFoundPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("❌ === 404 Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "페이지를 찾을 수 없습니다 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("homeUrl", "/");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);

        return "error/404";
    }

    /**
     * 500 에러 페이지 (fallback)
     */
    @GetMapping("/500")
    public String serverErrorPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("💥 === 500 Page Access - User: {} ===",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "서버 오류 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("homeUrl", "/");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);

        return "error/500";
    }
}