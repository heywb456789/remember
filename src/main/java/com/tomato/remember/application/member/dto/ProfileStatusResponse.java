package com.tomato.remember.application.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatusResponse {

    private int uploadedImageCount;    // 업로드된 이미지 수
    private int validImageCount;       // AI 처리 완료된 이미지 수
    private int completionPercentage;  // 완성도 퍼센트
    private boolean canStartVideoCall; // 영상통화 가능 여부
    private boolean isUploadComplete; // 업로드 완료 여부 (5장)
    private boolean isProcessingComplete; // AI 처리 완료 여부
}