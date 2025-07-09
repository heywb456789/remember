package com.tomato.remember.application.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.repository.MemberRepository;
import com.tomato.remember.common.code.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        Member member = memberRepository.findByUserKeyAndStatusNot(username, MemberStatus.DELETED)
            .orElseThrow(() -> {
                log.warn("Member not found with userKey: {}", username);
                return new UsernameNotFoundException("Member not found with userKey: " + username);
            });

        log.debug("Successfully loaded member: {} with status: {}", username, member.getStatus());
        return new MemberUserDetails(member);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long memberId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", memberId);

        Member member = memberRepository.findByIdAndStatusNot(memberId, MemberStatus.DELETED)
            .orElseThrow(() -> {
                log.warn("Member not found with ID: {}", memberId);
                return new UsernameNotFoundException("Member not found with ID: " + memberId);
            });

        log.debug("Successfully loaded member by ID: {} with userKey: {}", memberId, member.getUserKey());
        return new MemberUserDetails(member);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByPhoneNumber(String phoneNumber) throws UsernameNotFoundException {
        log.debug("Loading user by phone number: {}", phoneNumber);

        Member member = memberRepository.findByPhoneNumberAndStatusNot(phoneNumber, MemberStatus.DELETED)
            .orElseThrow(() -> {
                log.warn("Member not found with phone number: {}", phoneNumber);
                return new UsernameNotFoundException("Member not found with phone number: " + phoneNumber);
            });

        log.debug("Successfully loaded member by phone: {} with userKey: {}", phoneNumber, member.getUserKey());
        return new MemberUserDetails(member);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        Member member = memberRepository.findByEmailAndStatusNot(email, MemberStatus.DELETED)
            .orElseThrow(() -> {
                log.warn("Member not found with email: {}", email);
                return new UsernameNotFoundException("Member not found with email: " + email);
            });

        log.debug("Successfully loaded member by email: {} with userKey: {}", email, member.getUserKey());
        return new MemberUserDetails(member);
    }

    // 사용자 상태 확인 메서드들
    @Transactional(readOnly = true)
    public boolean isUserActive(String userKey) {
        return memberRepository.findByUserKeyAndStatusNot(userKey, MemberStatus.DELETED)
            .map(member -> member.getStatus() == MemberStatus.ACTIVE)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isUserBlocked(String userKey) {
        return memberRepository.findByUserKeyAndStatusNot(userKey, MemberStatus.DELETED)
            .map(member -> member.getStatus() == MemberStatus.BLOCKED)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isUserDeleted(String userKey) {
        return memberRepository.findByUserKey(userKey)
            .map(member -> member.getStatus() == MemberStatus.DELETED)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserKey(String userKey) {
        return memberRepository.existsByUserKeyAndStatusNot(userKey, MemberStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return memberRepository.existsByPhoneNumberAndStatusNot(phoneNumber, MemberStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmailAndStatusNot(email, MemberStatus.DELETED);
    }

    // 사용자 상태 업데이트 메서드들
    @Transactional
    public void updateLastAccess(String userKey) {
        memberRepository.findByUserKeyAndStatusNot(userKey, MemberStatus.DELETED)
            .ifPresent(Member::setLastAccessAt);
    }

    @Transactional
    public void activateUser(String userKey) {
        memberRepository.findByUserKeyAndStatusNot(userKey, MemberStatus.DELETED)
            .ifPresent(member -> member.setStatus(MemberStatus.ACTIVE));
    }

    @Transactional
    public void blockUser(String userKey) {
        memberRepository.findByUserKeyAndStatusNot(userKey, MemberStatus.DELETED)
            .ifPresent(member -> member.setStatus(MemberStatus.BLOCKED));
    }

    // 새로운 MemberUserDetails 생성 메서드
    public MemberUserDetails createMemberUserDetails(Member member) {
        return new MemberUserDetails(member);
    }
}