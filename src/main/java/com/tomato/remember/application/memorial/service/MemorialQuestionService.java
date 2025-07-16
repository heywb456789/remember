package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.MemorialQuestionResponse;
import com.tomato.remember.application.memorial.entity.MemorialQuestion;
import com.tomato.remember.application.memorial.repository.MemorialQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 질문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemorialQuestionService {
    
    private final MemorialQuestionRepository memorialQuestionRepository;
    
    /**
     * 활성 질문 목록 조회
     */
    public List<MemorialQuestionResponse> getActiveQuestions() {
        log.info("활성 질문 목록 조회 시작");
        
        List<MemorialQuestion> questions = memorialQuestionRepository.findActiveQuestions();
        
        log.info("활성 질문 목록 조회 완료 - 질문 수: {}", questions.size());
        
        return MemorialQuestionResponse.fromList(questions);
    }
}