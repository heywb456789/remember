package com.tomato.remember.admin.auth.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String password;
    private String phoneNumber;
    private String role;
    private String email;
    private String name;
    private LocalDateTime lastAccessAt;
}
