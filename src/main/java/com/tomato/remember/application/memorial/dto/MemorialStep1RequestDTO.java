package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 메모리얼 1단계 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialStep1RequestDTO {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
    private String description;

    private Boolean isPublic = false;
}