package com.tomato.remember.application.memorial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메모리얼 2단계 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialStep2RequestDTO {
    // 메모리얼 ID
    @NotNull(message = "메모리얼 ID는 필수입니다.")
    private Long tempMemorialId;

    // 고인 학습 정보
    @Size(max = 500, message = "성격 및 특징은 500자 이하여야 합니다.")
    private String personality;

    @Size(max = 300, message = "자주 하던 말은 300자 이하여야 합니다.")
    private String favoriteWords;

    @Size(max = 300, message = "말투 및 억양은 300자 이하여야 합니다.")
    private String speakingStyle;

    // 관심사
    private List<String> interests;

    @Size(max = 50, message = "기타 관심사는 50자 이하여야 합니다.")
    private String otherInterest;

    // 소중한 추억
    @Size(max = 500, message = "특별한 추억은 500자 이하여야 합니다.")
    private String specialMemories;

    @Size(max = 300, message = "가족에게 했던 말은 300자 이하여야 합니다.")
    private String familyMessages;
}