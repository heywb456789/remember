package com.tomato.remember.application.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 가족 구성원용 고인 상세 정보 입력 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FamilyInfoRequestDTO {

    /**
     * 성격이나 특징
     */
    @NotBlank(message = "성격이나 특징을 입력해주세요.")
    @Size(max = 500, message = "성격이나 특징은 500자 이내로 입력해주세요.")
    private String personality;

    /**
     * 취미나 관심사
     */
    @NotBlank(message = "취미나 관심사를 입력해주세요.")
    @Size(max = 300, message = "취미나 관심사는 300자 이내로 입력해주세요.")
    private String hobbies;

    /**
     * 좋아하는 음식
     */
    @NotBlank(message = "좋아하는 음식을 입력해주세요.")
    @Size(max = 300, message = "좋아하는 음식은 300자 이내로 입력해주세요.")
    private String favoriteFood;

    /**
     * 기억에 남는 일화나 추억
     */
    @NotBlank(message = "기억에 남는 일화나 추억을 입력해주세요.")
    @Size(max = 300, message = "기억에 남는 일화나 추억은 300자 이내로 입력해주세요.")
    private String specialMemories;

    /**
     * 습관이나 말버릇
     */
    @NotBlank(message = "습관이나 말버릇을 입력해주세요.")
    @Size(max = 300, message = "습관이나 말버릇은 300자 이내로 입력해주세요.")
    private String speechHabits;

    /**
     * 모든 필드가 입력되었는지 확인
     */
    public boolean isComplete() {
        return personality != null && !personality.trim().isEmpty() &&
               hobbies != null && !hobbies.trim().isEmpty() &&
               favoriteFood != null && !favoriteFood.trim().isEmpty() &&
               specialMemories != null && !specialMemories.trim().isEmpty() &&
               speechHabits != null && !speechHabits.trim().isEmpty();
    }

    /**
     * 입력된 필드 개수 반환
     */
    public int getFilledFieldCount() {
        int count = 0;
        if (personality != null && !personality.trim().isEmpty()) count++;
        if (hobbies != null && !hobbies.trim().isEmpty()) count++;
        if (favoriteFood != null && !favoriteFood.trim().isEmpty()) count++;
        if (specialMemories != null && !specialMemories.trim().isEmpty()) count++;
        if (speechHabits != null && !speechHabits.trim().isEmpty()) count++;
        return count;
    }
}