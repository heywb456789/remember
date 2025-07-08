package com.tomato.naraclub.application.auth.repository;

import com.tomato.naraclub.application.auth.entity.RefreshToken;
import com.tomato.naraclub.application.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String token);

    List<RefreshToken> findAllByMember(Member member);
}