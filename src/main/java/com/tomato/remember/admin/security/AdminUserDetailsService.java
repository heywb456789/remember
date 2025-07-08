package com.tomato.remember.admin.security;

import com.tomato.remember.admin.user.repository.AdminRepository;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.UnAuthorizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return adminRepository.findByUsername(username)
            .map(AdminUserDetails::new)
            .orElseThrow(() -> new UnAuthorizationException(ResponseStatus.UNAUTHORIZED_ID_PW));
    }
}
