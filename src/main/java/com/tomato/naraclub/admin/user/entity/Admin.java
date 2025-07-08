package com.tomato.naraclub.admin.user.entity;

import com.tomato.naraclub.admin.auth.dto.AdminAuthResponseDTO;
import com.tomato.naraclub.admin.auth.dto.AdminDTO;
import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import com.tomato.naraclub.admin.user.dto.AdminUserResponse;
import com.tomato.naraclub.common.audit.Audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;


@Table(
    name = "t_admin",
    indexes = {
        @Index(name = "idx01_t_admin", columnList = "created_at") // 최신순 정렬용
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Admin extends Audit {
    @Comment("로그인 아이디")
    @Column(unique = true, nullable = false)
    private String username;      // 로그인 아이디
    
    @Comment("비밀번호")
    @Column(nullable = false)
    private String password;      // BCrypt 암호화된 비밀번호

    @Comment("이름")
    @Column(nullable = false)
    private String name;

    @Comment("이메일")
    @Column(nullable = false)
    private String email;

    @Comment("전화번호")
    @Column(nullable = false)
    private String phoneNumber;

    @Comment("롤")
    @Enumerated(EnumType.STRING)
    private AdminRole role;       // ADMIN 등

    @Comment("상태")
    @Enumerated(EnumType.STRING)
    private AdminStatus status;

    @Comment("마지막 접속 시간")
    @Column
    private LocalDateTime lastAccessAt;


    public void updateLastAccess() {
        this.lastAccessAt = LocalDateTime.now();
    }

    public void updateRole(AdminRole role) {
        this.role = role;
    }

    public void approveStatus() {
        this.status = AdminStatus.ACTIVE;
    }

    public void updateStatus(AdminStatus status) {
        this.status = status;
    }

    public AdminDTO convertDTO() {
        return AdminDTO.builder()
            .id(id)
            .createdAt(createdAt)
            .phoneNumber(phoneNumber)
            .role(role.name())
            .email(email)
            .name(name)
            .lastAccessAt(lastAccessAt)
            .build();
    }

    public AdminUserResponse convertAdminUserResponse() {
        return AdminUserResponse.builder()
            .adminId(id)
            .loginId(username)
            .name(name)
            .email(email)
            .phoneNumber(phoneNumber)
            .role(role)
            .status(status)
            .profileImage("")
            .lastAccessAt(lastAccessAt)
            .createAt(LocalDateTime.now())
            .updateAt(LocalDateTime.now())
            .build();
    }


}
