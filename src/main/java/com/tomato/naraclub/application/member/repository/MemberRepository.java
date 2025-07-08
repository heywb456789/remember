package com.tomato.naraclub.application.member.repository;

import com.tomato.naraclub.application.member.entity.Member;
import com.tomato.naraclub.common.code.MemberStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserKey(String userKey);

    boolean existsByInviteCode(String code);

    Optional<Member> findByInviteCode(String inviteCode);

    Optional<Member> findByIdAndStatus(Long currentId, MemberStatus memberStatus);
}
