package com.tomato.remember.admin.user.repository;

import com.tomato.remember.admin.user.repository.custom.AppUserCustomRepository;
import com.tomato.remember.application.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository
 * @fileName : AppUserRepository
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AppUserRepository extends JpaRepository<Member, Long>, AppUserCustomRepository {

}
