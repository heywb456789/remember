package com.tomato.naraclub.admin.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : ActivityResponse
 * @date : 2025-05-20
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityResponse {
    private String           type;        // POST, COMMENT, LIKE, VIEW
    private String           title;       // 게시글 제목 등
    private String           description; // 댓글 내용, "좋아요" 등
    private String           link;        // 바로가기 URL
    private LocalDateTime createdAt;   // 발생 시간
}
