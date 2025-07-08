package com.tomato.naraclub.admin.user.repository;

import com.tomato.naraclub.application.auth.entity.MemberLoginHistory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository
 * @fileName : AppUserLoginHistoryRepository
 * @date : 2025-05-13
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AppUserLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

    Page<MemberLoginHistory> findByMemberId(long id, Pageable pageable);
}
