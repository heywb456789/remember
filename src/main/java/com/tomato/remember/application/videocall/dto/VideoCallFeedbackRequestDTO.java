package com.tomato.remember.application.videocall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCallFeedbackRequestDTO {
    @Size(max = 500, message = "피드백 메시지는 500자를 초과할 수 없습니다.")
    private String reviewMessage;
    
    @NotBlank(message = "contact_key는 필수입니다.")
    @Pattern(regexp = "^(rohmoohyun|kimgeuntae)$", message = "올바르지 않은 contact_key입니다.")
    private String contactKey;
}