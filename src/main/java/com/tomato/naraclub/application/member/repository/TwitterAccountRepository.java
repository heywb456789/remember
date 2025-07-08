package com.tomato.naraclub.application.member.repository;

import com.tomato.naraclub.application.member.entity.TwitterAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwitterAccountRepository extends JpaRepository<TwitterAccount, Long> {

    Optional<TwitterAccount> findByMemberId(Long id);
}
