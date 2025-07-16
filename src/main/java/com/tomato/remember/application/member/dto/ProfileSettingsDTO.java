package com.tomato.remember.application.member.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileSettingsDTO {
    private int uploadedImageCount;
    private int validImageCount; 
    private List<ProfileImageDTO> imageUrls;  // 현재 업로드된 이미지 URL들
    private boolean canStartVideoCall;
    private int completionPercentage;
    private boolean hasAnyImages;
    
    // 5장 모두 업로드해야 하는 정책 반영
    private boolean needsCompleteUpload; // 1-4장 상태는 허용 안함
    private List<Integer> invalidImageNumbers;
}