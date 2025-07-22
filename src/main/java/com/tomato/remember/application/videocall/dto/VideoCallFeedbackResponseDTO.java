package com.tomato.remember.application.videocall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCallFeedbackResponseDTO {
    private boolean success;
    private String message;
    private Long feedbackId;
}