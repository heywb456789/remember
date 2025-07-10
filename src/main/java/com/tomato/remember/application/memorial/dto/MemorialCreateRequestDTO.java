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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialCreateRequestDTO {

    @NotBlank(message = "망자 이름은 필수입니다.")
    @Size(max = 50, message = "망자 이름은 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "호칭은 필수입니다.")
    @Size(max = 30, message = "호칭은 30자 이하여야 합니다.")
    private String nickname;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    private LocalDate birthDate;

    private LocalDate deathDate;

    @NotNull(message = "관계는 필수입니다.")
    private Relationship relationship;

    private List<InterestType> interests;

    private String profileImageUrl;

    private String voiceFileUrl;

    private String videoFileUrl;

    private String userImageUrl;

    @Builder.Default
    private Boolean isPublic = false;

    @Size(max = 1000, message = "추가 정보는 1000자 이하여야 합니다.")
    private String additionalInfo;
}