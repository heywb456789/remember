package com.tomato.naraclub.application.auth.repository;

import com.tomato.naraclub.application.auth.entity.MemberLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.auth.repository
 * @fileName : MemberLoginHistoryRepository
 * @date : 2025-05-13
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

}
