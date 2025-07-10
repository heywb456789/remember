package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    @NotNull(message = "관계는 필수입니다.")
    private Relationship relationship;

    private LocalDate birthDate;

    private LocalDate deathDate;

    @Size(max = 100, message = "출생지는 100자 이하여야 합니다.")
    private String birthPlace;

    @Size(max = 100, message = "거주지는 100자 이하여야 합니다.")
    private String residence;

    @Size(max = 100, message = "직업은 100자 이하여야 합니다.")
    private String occupation;

    @Size(max = 15, message = "연락처는 15자 이하여야 합니다.")
    private String contactNumber;

    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    @Size(max = 1000, message = "생전 이야기는 1000자 이하여야 합니다.")
    private String lifeStory;

    @Size(max = 500, message = "취미는 500자 이하여야 합니다.")
    private String hobbies;

    @Size(max = 500, message = "성격은 500자 이하여야 합니다.")
    private String personality;

    @Size(max = 1000, message = "특별한 기억은 1000자 이하여야 합니다.")
    private String specialMemories;

    @Size(max = 500, message = "말버릇은 500자 이하여야 합니다.")
    private String speechHabits;

    @Size(max = 500, message = "좋아하는 음식은 500자 이하여야 합니다.")
    private String favoriteFood;

    @Size(max = 500, message = "좋아하는 장소는 500자 이하여야 합니다.")
    private String favoritePlace;

    @Size(max = 500, message = "좋아하는 음악은 500자 이하여야 합니다.")
    private String favoriteMusic;
}