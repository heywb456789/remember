package com.tomato.naraclub.application.member.repository;

import com.tomato.naraclub.application.member.entity.TwitterOAuthHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.repository
 * @fileName : TwitterOAuthHistoryRepository
 * @date : 2025-05-15
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface TwitterOAuthHistoryRepository extends JpaRepository<TwitterOAuthHistory, Long> {

    Optional<TwitterOAuthHistory> findByOauthToken(String oauthToken);
}
