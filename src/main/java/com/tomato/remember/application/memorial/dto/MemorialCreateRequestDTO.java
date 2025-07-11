package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.memorial.code.InterestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 메모리얼 생성 요청 DTO
 */
/**
 * 메모리얼 생성 요청 DTO (다중 파일 지원)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialCreateRequestDTO {

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요")
    private String name;

    @NotBlank(message = "호칭은 필수입니다")
    @Size(max = 30, message = "호칭은 30자 이내로 입력해주세요")
    private String nickname;

    @NotNull(message = "성별을 선택해주세요")
    private Gender gender;

    @NotNull(message = "나와의 관계를 선택해주세요")
    private Relationship relationship;

    private LocalDate birthDate;
    private LocalDate deathDate;

    // 고인 상세 정보
    @Size(max = 500, message = "성격 설명은 500자 이내로 입력해주세요")
    private String personality;

    @Size(max = 300, message = "취미 설명은 300자 이내로 입력해주세요")
    private String favoriteWords; // -> hobbies로 매핑

    @Size(max = 300, message = "좋아하는 음식 설명은 300자 이내로 입력해주세요")
    private String favoriteFoods; // -> favoriteFood로 매핑

    @Size(max = 300, message = "추억 설명은 300자 이내로 입력해주세요")
    private String memories; // -> specialMemories로 매핑

    @Size(max = 300, message = "습관 설명은 300자 이내로 입력해주세요")
    private String habits; // -> speechHabits로 매핑

    private String description;

    @Builder.Default
    private Boolean isPublic = false;
}