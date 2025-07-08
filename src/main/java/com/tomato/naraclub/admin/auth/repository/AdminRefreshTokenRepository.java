package com.tomato.naraclub.admin.auth.repository;

import com.tomato.naraclub.admin.auth.entity.AdminRefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    Optional<AdminRefreshToken> findByRefreshToken(String refreshToken);
}
