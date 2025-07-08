package com.tomato.naraclub.admin.user.repository;

import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import com.tomato.naraclub.admin.user.entity.Admin;
import com.tomato.naraclub.admin.user.repository.custom.AdminUserCustomRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository
 * @fileName : AdminUserRepository
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AdminUserRepository extends JpaRepository<Admin, Long>, AdminUserCustomRepository {

    Optional<Admin> findByIdAndRole(Long id, AdminRole adminRole);

    Optional<Admin> findByIdAndStatus(Long id, AdminStatus adminStatus);

    long countByRoleAndStatus(AdminRole role, AdminStatus adminStatus);
}
