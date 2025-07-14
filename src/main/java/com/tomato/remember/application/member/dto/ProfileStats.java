package com.tomato.remember.application.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileStats {

    private int completionPercentage;    // 프로필 완성도 (0-100%)
    private int uploadedImageCount;      // 업로드된 이미지 개수
    private int validImageCount;         // AI 처리 완료된 이미지 개수
    private boolean canStartVideoCall;   // 영상통화 가능 여부
    private int maxProfileImages;        // 최대 프로필 이미지 개수
}