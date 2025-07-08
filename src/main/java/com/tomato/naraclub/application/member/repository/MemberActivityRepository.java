package com.tomato.naraclub.application.member.repository;

import com.tomato.naraclub.application.member.entity.Member;
import com.tomato.naraclub.application.member.entity.MemberActivity;
import com.tomato.naraclub.application.member.repository.custom.MemberActivityCustomRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.repository
 * @fileName : MemberActivityRepository
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface MemberActivityRepository extends JpaRepository<MemberActivity, Long>,
    MemberActivityCustomRepository {

    Optional<MemberActivity> findByAuthorAndShareLink(Member member, String shareLink);
}
