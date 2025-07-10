package com.tomato.remember.application.main;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.security.MemberUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 메인 페이지 컨트롤러
 * 로그인 전후 상태에 따른 적절한 페이지 제공
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    // TODO: 실제 서비스들이 구현되면 주석 해제
     private final MemorialService memorialService;
    // private final SubscriptionService subscriptionService;
    // private final PaymentService paymentService;

    @GetMapping("/")
    public String root() {
        log.info("Root path accessed, redirecting to /mobile/home");
        return "redirect:/mobile/home";
    }

    /**
     * 메인 홈페이지
     * 로그인 전: 서비스 소개 + 안내 페이지
     * 로그인 후: 메모리얼 대시보드
     */
    @GetMapping("/mobile/home")
    public String homePage(@AuthenticationPrincipal MemberUserDetails userDetails,
                          Model model,
                          HttpServletRequest request) {

        log.info("Main Page Access - User: {}", userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "토마토리멤버 - 소중한 추억을 영원히 간직하세요");
        model.addAttribute("appName", "토마토리멤버");

        // 로그인 상태에 따른 처리
        if (userDetails != null) {
            // 로그인한 사용자 처리
            Member currentUser = userDetails.getMember();
            log.info("Logged in user: {} (ID: {})", currentUser.getName(), currentUser.getId());

            setupLoggedInUserData(model, currentUser);
        } else {
            // 비로그인 사용자 처리
            log.info("Anonymous user access");
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

        try {
            // 사용자의 메모리얼 목록 조회
            List<Memorial> memorialList = memorialService.getActiveMemorialListByMember(currentUser);
            model.addAttribute("memorialList", memorialList);
            model.addAttribute("memorialCount", memorialList.size());
            /*
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

            log.info("User data loaded successfully for user: {} - Memorials: {}, Trial: {}, Balance: {}",
                    currentUser.getName(), memorialList.size(), trialStatus.isTrialUser(), tokenBalance.getBalance());
            */

            // 임시 데이터 (실제 서비스 구현 전)
            setupTemporaryUserData(model, currentUser);

        } catch (Exception e) {
            log.error("Error loading user data for user: {}", currentUser.getId(), e);
            // 에러 발생 시 기본값으로 설정
            setupDefaultUserData(model);
        }
    }

    /**
     * 임시 사용자 데이터 설정 (개발용)
     */
    private void setupTemporaryUserData(Model model, Member currentUser) {
        // 메모리얼 목록 - 임시로 빈 리스트
        List<Map<String, Object>> memorialList = new ArrayList<>();
        model.addAttribute("memorialList", memorialList);
        model.addAttribute("memorialCount", memorialList.size());

        // 무료체험 정보 - 신규 사용자는 체험 중으로 설정
        model.addAttribute("showTrialBanner", true);
        model.addAttribute("trialDaysRemaining", 90);
        model.addAttribute("isTrialUser", true);
        model.addAttribute("maxMemorialsInTrial", 3);

        // 토큰 잔액 - 기본 체험 토큰
        model.addAttribute("tokenBalance", 50000);

        log.info("Temporary data set for user: {} - Trial: {}, Token: {}",
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

        log.warn("Default user data set due to error");
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

        log.info("Anonymous user data set");
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
        model.addAttribute("experienceUrl", "/mobile/experience");
        model.addAttribute("aboutUrl", "/mobile/about");
        model.addAttribute("contactUrl", "/mobile/support/contact");
        model.addAttribute("helpUrl", "/mobile/support/help");
        model.addAttribute("termsUrl", "/mobile/terms");
        model.addAttribute("privacyUrl", "/mobile/privacy");
    }

    /**
     * 서비스 기본 정보 설정
     */
    private void setupServiceInfo(Model model) {
        model.addAttribute("freeTrialMonths", 3);
        model.addAttribute("maxFamilyMembers", 8);
        model.addAttribute("supportEmail", "tomatoai@etomato.com");
        model.addAttribute("supportPhone", "02-2128-3838");
        model.addAttribute("companyName", "이토마토");
        model.addAttribute("companyAddress", "서울특별시 마포구 양화진 4길 32 이토마토빌딩 2층");
        model.addAttribute("businessNumber", "123-45-67890");
        model.addAttribute("representativeName", "김토마토");
    }

    /**
     * About 페이지
     */
    @GetMapping("/mobile/about")
    public String aboutPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("About Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "서비스 소개 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/main/about";
    }

    /**
     * Contact 페이지
     */
    @GetMapping("/mobile/support/contact")
    public String contactPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Contact Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "문의하기 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/support/contact";
    }

    /**
     * 도움말 페이지
     */
    @GetMapping("/mobile/support/help")
    public String helpPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Help Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "도움말 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/support/help";
    }

    /**
     * FAQ 페이지
     */
    @GetMapping("/mobile/support/faq")
    public String faqPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("FAQ Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "자주 묻는 질문 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/support/faq";
    }

    /**
     * 3개월 무료체험 페이지
     */
    @GetMapping("/mobile/experience")
    public String experiencePage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Experience Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "3개월 무료체험 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/experience/trial";
    }

    /**
     * 개인정보처리방침
     */
    @GetMapping("/mobile/privacy")
    public String privacyPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Privacy Policy Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "개인정보처리방침 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/terms/privacy";
    }

    /**
     * 서비스 약관
     */
    @GetMapping("/mobile/terms")
    public String termsPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Terms of Service Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "서비스 약관 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/terms/service";
    }

    /**
     * 이용약관
     */
    @GetMapping("/mobile/terms/service")
    public String serviceTermsPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Service Terms Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "이용약관 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/terms/service";
    }

    /**
     * 개인정보처리방침 (상세)
     */
    @GetMapping("/mobile/terms/privacy")
    public String privacyTermsPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.info("Privacy Terms Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "개인정보처리방침 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);
        setupServiceInfo(model);

        return "/mobile/terms/privacy";
    }

    /**
     * 404 에러 페이지
     */
    @GetMapping("/mobile/error/404")
    public String notFoundPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.warn("404 Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "페이지를 찾을 수 없습니다 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);

        return "/mobile/error/404";
    }

    /**
     * 500 에러 페이지
     */
    @GetMapping("/mobile/error/500")
    public String serverErrorPage(@AuthenticationPrincipal MemberUserDetails userDetails, Model model) {
        log.error("500 Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

        model.addAttribute("pageTitle", "서버 오류 - 토마토리멤버");
        model.addAttribute("appName", "토마토리멤버");
        model.addAttribute("currentUser", userDetails != null ? userDetails.getMember() : null);
        model.addAttribute("isLoggedIn", userDetails != null);

        setupCommonUrls(model);

        return "/mobile/error/500";
    }
}