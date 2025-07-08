package com.tomato.remember.application.member.repository;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.MemberStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserKey(String userKey);

    boolean existsByInviteCode(String code);

    Optional<Member> findByInviteCode(String inviteCode);

    Optional<Member> findByIdAndStatus(Long currentId, MemberStatus memberStatus);

    Optional<Member> findByPhoneNumber(String phoneNumber);
}
