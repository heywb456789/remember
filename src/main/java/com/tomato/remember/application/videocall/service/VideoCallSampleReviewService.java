package com.tomato.remember.application.videocall.service;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.videocall.entity.VideoCallSampleReview;
import com.tomato.remember.application.videocall.repository.VideoCallSampleReviewRepository;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.APIException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoCallSampleReviewService {

    private final VideoCallSampleReviewRepository videoCallSampleReviewRepository;

    public VideoCallSampleReview save(VideoCallSampleReview review) {
        // 유효성 검증
        if (review.getCaller() == null) {
//            throw new APIException(ResponseStatus.BAD_REQUEST, "호출자 정보가 없습니다.");
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        if (review.getContactKey() == null || review.getContactKey().trim().isEmpty()) {
//            throw new APIException(ResponseStatus.BAD_REQUEST, "contact_key가 없습니다.");
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        // contact_key 검증
        if (! Arrays.asList("rohmoohyun", "kimgeuntae").contains(review.getContactKey())) {
//            throw new APIException(ResponseStatus.BAD_REQUEST, "올바르지 않은 contact_key입니다.");
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        // 피드백 메시지는 비어있어도 OK (선택사항)
        if (review.getReviewMessage() != null && review.getReviewMessage().length() > 500) {
//            throw new APIException(ResponseStatus.BAD_REQUEST, "피드백 메시지는 500자를 초과할 수 없습니다.");
            throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        VideoCallSampleReview savedReview = videoCallSampleReviewRepository.save(review);
        
        log.info("비디오콜 피드백 저장 - 사용자: {}, 대상: {}, 메시지 길이: {}", 
                review.getCaller().getName(), 
                review.getContactKey(), 
                review.getReviewMessage() != null ? review.getReviewMessage().length() : 0);

        return savedReview;
    }

    @Transactional(readOnly = true)
    public List<VideoCallSampleReview> findByCallerAndContactKey(Member caller, String contactKey) {
        return videoCallSampleReviewRepository.findByCallerAndContactKeyOrderByCreatedAtDesc(caller, contactKey);
    }

    @Transactional(readOnly = true)
    public List<VideoCallSampleReview> findByCaller(Member caller) {
        return videoCallSampleReviewRepository.findByCallerOrderByCreatedAtDesc(caller);
    }

    @Transactional(readOnly = true)
    public Page<VideoCallSampleReview> findByCallerWithPaging(Member caller, Pageable pageable) {
        return videoCallSampleReviewRepository.findByCaller(caller, pageable);
    }
}