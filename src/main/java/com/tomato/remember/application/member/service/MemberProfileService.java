package com.tomato.remember.application.member.service;

import com.tomato.remember.application.member.dto.ProfileSettingsDTO;
import com.tomato.remember.application.member.dto.ProfileUpdateRequest;
import com.tomato.remember.application.member.dto.ProfileUpdateResponse;

/**
 * 회원 프로필 관리 서비스 인터페이스 (수정된 버전)
 */
public interface MemberProfileService {

    /**
     * 통합 프로필 업데이트 (Request DTO 사용)
     *
     * @param memberId 회원 ID
     * @param request 프로필 업데이트 요청 정보
     * @return 업데이트 결과
     */
    ProfileUpdateResponse updateProfileUnified(Long memberId, ProfileUpdateRequest request);

    /**
     * 회원 탈퇴 처리
     *
     * @param memberId 회원 ID
     */
    void deleteAccount(Long memberId);

    /**
     * 영상통화 가능 여부 확인
     *
     * @param memberId 회원 ID
     * @return 영상통화 가능 여부
     */
    boolean canStartVideoCall(Long memberId);

    /**
     * 프로필 설정 페이지 데이터 조회
     *
     * @param memberId 회원 ID
     * @return 프로필 설정 데이터
     */
    ProfileSettingsDTO getProfileSettingsData(Long memberId);
}