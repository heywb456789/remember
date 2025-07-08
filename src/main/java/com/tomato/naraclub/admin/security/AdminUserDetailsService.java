package com.tomato.naraclub.admin.security;

import com.tomato.naraclub.admin.user.repository.AdminRepository;
import com.tomato.naraclub.common.code.ResponseStatus;
import com.tomato.naraclub.common.exception.UnAuthorizationException;
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
