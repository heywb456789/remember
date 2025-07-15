package com.tomato.remember.application.memorial.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialListResponseDTO {
    
    private Long memorialId;
    private String name;
    private String nickname;
    private String mainProfileImageUrl;
    private LocalDate lastVisitAt;
    private Integer totalVisits;
    private Integer memoryCount;
    private Boolean aiTrainingCompleted;
    private String formattedAge;
    private String relationshipDescription;
    
    // 영상통화 가능 여부
    private Boolean canStartVideoCall;
    private Boolean hasRequiredProfileImages;
    
    // 파일 업로드 상태
    private Boolean hasRequiredFiles;
    private Integer profileImageCount;
    private Integer voiceFileCount;
    private Integer videoFileCount;
}