package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.dto.MemorialQuestionResponse;
import com.tomato.remember.application.memorial.service.MemorialQuestionService;
import com.tomato.remember.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 질문 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/memorial/questions")
@RequiredArgsConstructor
public class MemorialQuestionController {
    
    private final MemorialQuestionService memorialQuestionService;
    
    /**
     * 활성 질문 목록 조회
     * GET /api/memorial/questions
     */
    @GetMapping
    public ResponseDTO<List<MemorialQuestionResponse>> getQuestions() {
        log.info("질문 목록 조회 API 요청");
        
        try {
            List<MemorialQuestionResponse> questions = memorialQuestionService.getActiveQuestions();
            
            log.info("질문 목록 조회 API 완료 - 질문 수: {}", questions.size());
            
            return ResponseDTO.ok(questions);
            
        } catch (Exception e) {
            log.error("질문 목록 조회 API 실패", e);
            return ResponseDTO.internalServerError();
        }
    }
}