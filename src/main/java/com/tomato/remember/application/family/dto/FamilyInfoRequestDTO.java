package com.tomato.remember.application.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
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

    @NotEmpty(message = "답변을 입력해주세요.")
    private List<FamilyQuestionAnswerDTO> questionAnswers;
}