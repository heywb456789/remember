package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.dto.MemorialQuestionResponse;
import com.tomato.remember.application.memorial.service.MemorialQuestionService;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 메모리얼 뷰 컨트롤러 (Thymeleaf 템플릿 전용)
 */
@Slf4j
@Controller
@RequestMapping("/mobile/memorial")
@RequiredArgsConstructor
public class MemorialController {

    private final MemorialService memorialService;
    private final MemorialQuestionService memorialQuestionService;

    /**
     * 메모리얼 등록 페이지
     * GET /mobile/memorial/create
     */
    @GetMapping("/create")
    public String create(Model model, @AuthenticationPrincipal MemberUserDetails userDetails) {
        log.info("메모리얼 등록 페이지 접근 - 사용자: {}", userDetails.getMember().getId());

        try {
            // 활성 질문 목록 조회
            List<MemorialQuestionResponse> questions = memorialQuestionService.getActiveQuestions();

            // 모델에 데이터 추가
            model.addAttribute("questions", questions);
            model.addAttribute("questionCount", questions.size());

            // 필수 질문 개수 계산
            long requiredQuestionCount = questions.stream()
                    .filter(MemorialQuestionResponse::getIsRequired)
                    .count();
            model.addAttribute("requiredQuestionCount", requiredQuestionCount);

            log.info("메모리얼 등록 페이지 데이터 준비 완료 - 질문 수: {}, 필수 질문 수: {}",
                    questions.size(), requiredQuestionCount);

            return "mobile/memorial/create";

        } catch (Exception e) {
            log.error("메모리얼 등록 페이지 로드 실패", e);
            model.addAttribute("error", "페이지를 불러오는 중 오류가 발생했습니다.");
            return "mobile/memorial/create";
        }
    }
}