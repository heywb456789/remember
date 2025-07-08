package com.tomato.naraclub.application.member.dto;

import com.tomato.naraclub.common.code.MemberRole;
import com.tomato.naraclub.common.code.MemberStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
@ToString
public class MemberDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String password;
    private String phoneNumber;
    private String inviteCode;
    private String status;
    private String role;
    private String email;
    private String name;
    private LocalDateTime lastAccessAt;
    @Builder.Default
    private Boolean verified = false;
    private String profileImg;
    private Double points;
//    private Member inviter;    // 추천인
    private boolean twitterConnected;


}