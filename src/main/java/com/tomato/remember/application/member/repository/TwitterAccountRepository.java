package com.tomato.remember.application.member.repository;

import com.tomato.remember.application.member.entity.TwitterAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwitterAccountRepository extends JpaRepository<TwitterAccount, Long> {

    Optional<TwitterAccount> findByMemberId(Long id);
}
