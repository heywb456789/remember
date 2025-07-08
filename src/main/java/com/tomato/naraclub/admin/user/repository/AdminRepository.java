package com.tomato.naraclub.admin.user.repository;

import com.tomato.naraclub.admin.user.entity.Admin;
import java.nio.channels.FileChannel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository
 * @fileName : AdminRepository
 * @date : 2025-04-28
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);
}
