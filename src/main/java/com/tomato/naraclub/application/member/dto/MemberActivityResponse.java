package com.tomato.naraclub.application.member.dto;

import com.tomato.naraclub.application.member.code.ActivityReviewStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.dto
 * @fileName : MemberActivityResponse
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberActivityResponse {
    private Long activityId;
    private Long memberId;
    private String memberName;
    private String title;
    private String shareLink;
    private ActivityReviewStage stage;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
