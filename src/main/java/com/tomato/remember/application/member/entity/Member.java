package com.tomato.remember.application.member.entity;

import com.tomato.remember.admin.user.dto.AppUserResponse;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.common.audit.Audit;
import com.tomato.remember.common.code.MemberStatus;
import com.tomato.remember.common.code.MemberRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Table(
    name = "t_member",
    indexes = {
        @Index(name = "idx01_t_member", columnList = "created_at") // 최신순 정렬용
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Member extends Audit {

    @Comment("tongtongOneId")
    @Column(unique = true, length = 100)
    private String userKey;

    @Comment("tongtongPasswd")
    @Column(length = 100)
    private String password;

    @Comment("폰번호")
    @Column(length = 20)
    private String phoneNumber;

    @Comment("초대코드 - 가입시 자동생성")
    @Column(nullable = false, length = 10)
    private String inviteCode;

    @Comment("유저 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Comment("유저 롤")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;

    @Comment("이메일")
    @Column(length = 100)
    private String email;

    @Comment("사용자 명")
    @Column(length = 50, nullable = false)
    private String name;

    @Comment("마지막 접속 시간")
    @Column
    private LocalDateTime lastAccessAt;

    @Comment("신원인증 여부")
    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Comment("프로필 이미지")
    @Column
    private String profileImg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id")
    private Member inviter;    // 추천인

    @Comment("회원 포인트")
    @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 0")
    @Builder.Default
    private Double points = 0.0;

    @Comment("활동 내역")
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberActivity> memberActivities = new ArrayList<>();

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public void setRole(MemberRole memberRole) {
        this.role = memberRole;
    }

    public void setInviter(Member inviter) {
        this.inviter = inviter;
    }

    public MemberDTO convertDTO() {
        return MemberDTO.builder()
            .id(id)
            .createdAt(createdAt)
            .password(password)
            .phoneNumber(phoneNumber)
            .inviteCode(inviteCode)
            .status(status.name())
            .role(role.name())
            .email(email)
            .name(name)
            .lastAccessAt(lastAccessAt)
            .verified(verified)
            .profileImg(profileImg)
            .points(points)
            .build();
    }

    public AppUserResponse convertAppUserResponse() {
        return AppUserResponse.builder()
            .userId(id)
            .userName(name)
            .userKey(userKey)
            .phoneNumber(phoneNumber)
            .inviteCode(inviteCode)
            .profileImageUrl(profileImg)
            .status(status)
            .role(role)
            .email(email)
            .verified(verified)
            .points(points)
            .build();
    }

    public void setLastAccessAt() {
        this.lastAccessAt = LocalDateTime.now();
    }

    public void increasePoint(double amount) {
        this.points += amount;
    }

    public void decreasePoints(double amountToTransfer) {
        this.points -= amountToTransfer;
    }

    public void deleteMemInfo() {
        this.name = "";
        this.phoneNumber = "";
        this.userKey = UUID.randomUUID().toString();
        this.status = MemberStatus.DELETED;
        this.role = MemberRole.USER_INACTIVE;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }
}
