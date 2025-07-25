package com.tomato.remember.application.main;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.dto.MemorialListResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialResponseDTO;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.exception.APIException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 메인 페이지 컨트롤러 (개선된 버전)
 * 로그인 전후 상태에 따른 적절한 페이지 제공
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final MemorialService memorialService;
    // private final SubscriptionService subscriptionService;  // 나중에 구현
    // private final PaymentService paymentService;  // 나중에 구현

    @GetMapping("/")
    public String root() {
        log.info("Root path accessed, redirecting to /mobile/home");
        return "mobile/main/tomato_remember_onboarding";
    }


    private static final Map<String, Contact> CONTACT_MAP = Map.of(
        "rohmoohyun", new Contact("노무현 님", "/images/roh.png", "rohmoohyun"),
        "kimgeuntae", new Contact("김근태 님", "/images/kkt.png", "kimgeuntae")
    );

    @GetMapping("/call/{key}")
    public String videoCallIntro(
            @PathVariable String key,
            Model model
    ) {
        Contact contact = CONTACT_MAP.get(key);
        if (contact == null) {
            // key가 없으면 404 또는 기본값 처리
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }
        model.addAttribute("contact", contact);
        model.addAttribute("contactKey", key);
        return "mobile/call/video-call-intro";
    }

    @GetMapping("/call/feedback")
    public String videoCallFeedback(
            @RequestParam(value = "contact", required = false, defaultValue = "kimgeuntae") String contactKey,
            Model model) {

        // contactKey 유효성 검증
        if (!CONTACT_MAP.containsKey(contactKey)) {
            contactKey = "kimgeuntae"; // 기본값
        }

        Contact contact = CONTACT_MAP.get(contactKey);

        log.info("비디오콜 피드백 페이지 - contactKey: {}, contactName: {}", contactKey, contact.getName());

        model.addAttribute("contactKey", contactKey);
        model.addAttribute("contactName", contact.getName());

        return "mobile/call/video-call-sample-feedback";
    }

    @GetMapping("/mobile/video-call")
    public String videoCall(
            @RequestParam(value = "contact", required = false, defaultValue = "kimgeuntae") String contactKey,
            Model model
    ) {
        // contactKey 유효성 검증
        if (!CONTACT_MAP.containsKey(contactKey)) {
            contactKey = "kimgeuntae"; // 기본값
        }

        Contact contact = CONTACT_MAP.get(contactKey);

        // contactKey에 따라 보여줄 대기 영상 파일명을 결정
        String fileName;
        switch (contactKey) {
            case "rohmoohyun":
                fileName = "waiting_no.mp4";
                break;
            case "kimgeuntae":
                fileName = "waiting_kt.mp4";
                break;
            default:
                fileName = "waiting_kt.mp4";
                break;
        }

        // CDN 또는 로컬 스토리지에 올려둔 경로
        String waitingVideoUrl = "https://remember.newstomato.com/static/" + fileName;

        log.info("비디오콜 페이지 - contactKey: {}, contactName: {}, videoFile: {}",
                contactKey, contact.getName(), fileName);

        model.addAttribute("waitingVideoUrl", waitingVideoUrl);
        model.addAttribute("contactKey", contactKey); // 영문명
        model.addAttribute("contactName", contact.getName()); // 한글명
        model.addAttribute("recordButtonImageUrl", "/images/mic.png");

        return "mobile/call/video-call-sample";
    }

    @GetMapping("/mobile/onboarding")
    public String onboarding() {
        return "mobile/main/tomato_remember_onboarding";
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

        try {
            log.info("Main Page Access - User: {}",
                userDetails != null ? userDetails.getMember().getName() : "Anonymous");

            // 기본 페이지 정보 설정
            setupBasicPageInfo(model);

            // 로그인 상태에 따른 처리
            if (userDetails != null) {
                setupLoggedInUserData(model, userDetails.getMember());
            } else {
                setupAnonymousUserData(model);
            }

            // 공통 설정
            setupCommonData(model);

            return "mobile/main/main";

        } catch (Exception e) {
            log.error("Main page error", e);
            // 에러 발생 시 기본 설정으로 폴백
            setupFallbackData(model);
            return "mobile/main/main";
        }
    }

    /**
     * 기본 페이지 정보 설정
     */
    private void setupBasicPageInfo(Model model) {
        model.addAttribute("pageTitle", "토마토리멤버 - 소중한 추억을 영원히 간직하세요");
        model.addAttribute("appName", "토마토리멤버");
    }

    /**
     * 로그인한 사용자 데이터 설정
     */
    private void setupLoggedInUserData(Model model, Member currentUser) {
        log.info("Setting up logged in user data: {} (ID: {})",
                currentUser.getName(), currentUser.getId());

        // 사용자 기본 정보
        model.addAttribute("currentUser", currentUser.convertDTO());
        model.addAttribute("isLoggedIn", true);

        // 메모리얼 목록 조회 - 5개로 제한
        try {
            Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            ListDTO<MemorialListResponseDTO> memorialList = memorialService.getMyMemorials(currentUser, pageable);

            model.addAttribute("memorialList", memorialList.getData());
            model.addAttribute("memorialCount", memorialList.getPagination().getTotalElements());
            model.addAttribute("hasMoreMemorials", memorialList.getPagination().getTotalElements() > 5);

            log.info("User {} has {} memorials (showing 5)", currentUser.getName(), memorialList.getPagination().getTotalElements());

        } catch (Exception e) {
            log.error("Error loading memorials for user: {}", currentUser.getId(), e);
            // 에러 발생 시 빈 목록으로 설정
            model.addAttribute("memorialList", new ArrayList<MemorialResponseDTO>());
            model.addAttribute("memorialCount", 0);
            model.addAttribute("hasMoreMemorials", false);
        }

        // 체험 관련 정보 (임시)
        setupTrialInfo(model);
    }

    /**
     * 체험 관련 정보 설정 (임시)
     */
    private void setupTrialInfo(Model model) {
        model.addAttribute("showTrialBanner", false);  // 기본값 false
        model.addAttribute("trialDaysRemaining", 90);
        model.addAttribute("isTrialUser", false);  // 기본값 false
        model.addAttribute("maxMemorialsInTrial", 3);
        model.addAttribute("tokenBalance", 0);
    }

    /**
     * 비로그인 사용자 데이터 설정
     */
    private void setupAnonymousUserData(Model model) {
        log.info("Setting up anonymous user data");

        model.addAttribute("currentUser", null);
        model.addAttribute("isLoggedIn", false);
        model.addAttribute("memorialList", new ArrayList<MemorialResponseDTO>());
        model.addAttribute("memorialCount", 0);
        model.addAttribute("showTrialBanner", false);
        model.addAttribute("trialDaysRemaining", 0);
        model.addAttribute("isTrialUser", false);
        model.addAttribute("tokenBalance", 0);
    }

    /**
     * 공통 데이터 설정
     */
    private void setupCommonData(Model model) {
        // URL 정보
        setupUrlInfo(model);

        // 서비스 정보
        setupServiceInfo(model);
    }

    /**
     * URL 정보 설정
     */
    private void setupUrlInfo(Model model) {
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
     * 서비스 정보 설정
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
     * 에러 발생 시 폴백 데이터 설정
     */
    private void setupFallbackData(Model model) {
        log.warn("Setting up fallback data due to error");

        setupBasicPageInfo(model);
        setupAnonymousUserData(model);
        setupCommonData(model);
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

        setupCommonData(model);

        return "mobile/main/about";
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

        setupCommonData(model);

        return "mobile/support/contact";
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

        setupCommonData(model);

        return "mobile/support/help";
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

        setupCommonData(model);

        return "mobile/support/faq";
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

        setupCommonData(model);

        return "mobile/experience/trial";
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

        setupCommonData(model);

        return "mobile/terms/privacy";
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

        setupCommonData(model);

        return "mobile/terms/service";
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

        setupCommonData(model);

        return "mobile/terms/service";
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

        setupCommonData(model);

        return "mobile/terms/privacy";
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

        setupUrlInfo(model);

        return "mobile/error/404";
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

        setupUrlInfo(model);

        return "mobile/error/500";
    }
}