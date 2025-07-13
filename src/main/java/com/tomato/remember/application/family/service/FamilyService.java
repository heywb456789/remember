package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.dto.FamilyAllDataResponse;
import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.dto.FamilyMemberResponse;
import com.tomato.remember.application.family.dto.PermissionUpdateRequest;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.repository.FamilyMemberRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.common.dto.ListDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 가족 관리 서비스
 * - SSR용 메서드: *ForSSR
 * - 앱용 메서드: *ForApp
 * - 공통 액션 메서드: invite, updatePermissions 등
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private final FamilyMemberRepository familyMemberRepository;
    private final MemorialRepository memorialRepository;
    private final MemorialService memorialService;
    private final FamilyInviteService familyInviteService;

    // ===== SSR 전용 메서드 =====

    /**
     * SSR용: 전체 가족 구성원 조회 (DTO 형태)
     * 서버 렌더링에서 바로 사용 가능한 형태로 반환
     */
    public List<FamilyMemberResponse> getAllFamilyMembersForSSR(Member member) {
        log.debug("SSR용 전체 가족 구성원 조회 - 사용자: {}", member.getId());

        List<FamilyMember> familyMembers = familyMemberRepository.findAllAccessibleFamilyMembers(member);

        List<FamilyMemberResponse> responses = familyMembers.stream()
                .map(FamilyMemberResponse::from)
                .collect(Collectors.toList());

        log.debug("SSR용 전체 가족 구성원 조회 완료 - 사용자: {}, 구성원 수: {}",
                member.getId(), responses.size());

        return responses;
    }

    /**
     * SSR용: 받은 초대 목록 조회 (DTO 형태)
     */
    public List<FamilyMemberResponse> getReceivedInvitationsForSSR(Member member) {
        log.debug("SSR용 받은 초대 목록 조회 - 사용자: {}", member.getId());

        List<FamilyMember> invitations = familyMemberRepository.findPendingInvitations(member);

        List<FamilyMemberResponse> responses = invitations.stream()
                .map(FamilyMemberResponse::from)
                .collect(Collectors.toList());

        log.debug("SSR용 받은 초대 목록 조회 완료 - 사용자: {}, 초대 수: {}",
                member.getId(), responses.size());

        return responses;
    }

    /**
     * SSR용: 보낸 초대 목록 조회 (DTO 형태)
     */
    public List<FamilyMemberResponse> getSentInvitationsForSSR(Member member) {
        log.debug("SSR용 보낸 초대 목록 조회 - 사용자: {}", member.getId());

        List<FamilyMember> invitations = familyMemberRepository.findByInvitedByOrderByCreatedAtDesc(member);

        List<FamilyMemberResponse> responses = invitations.stream()
                .map(FamilyMemberResponse::from)
                .collect(Collectors.toList());

        log.debug("SSR용 보낸 초대 목록 조회 완료 - 사용자: {}, 초대 수: {}",
                member.getId(), responses.size());

        return responses;
    }

    /**
     * SSR용: 가족 구성원 상세 조회 (DTO 형태)
     */
    public FamilyMemberResponse getFamilyMemberForSSR(Long familyMemberId, Member currentUser) {
        log.debug("SSR용 가족 구성원 상세 조회 - 구성원: {}, 사용자: {}", familyMemberId, currentUser.getId());

        FamilyMember familyMember = getFamilyMemberAndCheckAccess(familyMemberId, currentUser);

        FamilyMemberResponse response = FamilyMemberResponse.from(familyMember);

        log.debug("SSR용 가족 구성원 상세 조회 완료 - 구성원: {}", familyMemberId);

        return response;
    }

    // ===== 앱 전용 메서드 =====

    /**
     * 앱용: 전체 가족 관리 데이터 조회
     * SSR과 동일한 데이터를 JSON 형태로 제공
     */
    public FamilyAllDataResponse getAllFamilyDataForApp(Member member) {
        log.debug("앱용 전체 가족 데이터 조회 - 사용자: {}", member.getId());

        // 1. 접근 가능한 모든 메모리얼 조회
        List<Memorial> accessibleMemorials = getAccessibleMemorials(member);

        // 2. 전체 가족 구성원 조회
        List<FamilyMemberResponse> allFamilyMembers = getAllFamilyMembersForSSR(member);

        // 3. 통계 정보 계산
        FamilyAllDataResponse.StatisticsInfo statistics = buildStatisticsInfo(
                accessibleMemorials, allFamilyMembers);

        // 4. 메모리얼 정보 변환
        List<FamilyAllDataResponse.MemorialInfo> memorialInfos = accessibleMemorials.stream()
                .map(this::buildMemorialInfo)
                .collect(Collectors.toList());

        FamilyAllDataResponse response = FamilyAllDataResponse.builder()
                .memorials(memorialInfos)
                .familyMembers(allFamilyMembers)
                .statistics(statistics)
                .build();

        log.debug("앱용 전체 가족 데이터 조회 완료 - 사용자: {}, 메모리얼: {}, 가족 구성원: {}",
                member.getId(), memorialInfos.size(), allFamilyMembers.size());

        return response;
    }

    /**
     * 앱용: 특정 메모리얼의 가족 구성원 조회 (페이징)
     */
    public ListDTO<FamilyMemberResponse> getFamilyMembersForApp(Long memorialId, Member currentUser, Pageable pageable) {
        log.debug("앱용 메모리얼 가족 구성원 조회 (페이징) - 메모리얼: {}, 사용자: {}", memorialId, currentUser.getId());

        Memorial memorial = getMemorialAndCheckAccess(memorialId, currentUser);

        // 페이징 조회
        Page<FamilyMember> familyMembersPage = familyMemberRepository.findByMemorialOrderByCreatedAtDesc(memorial, pageable);

        // DTO 변환
        Page<FamilyMemberResponse> responsePage = familyMembersPage.map(FamilyMemberResponse::from);

        ListDTO<FamilyMemberResponse> result = ListDTO.of(responsePage);

        log.debug("앱용 메모리얼 가족 구성원 조회 완료 - 메모리얼: {}, 구성원 수: {}",
                memorialId, result.getPagination().getTotalElements());

        return result;
    }

    /**
     * 앱용: 받은 초대 목록 조회
     */
    public List<FamilyMemberResponse> getReceivedInvitationsForApp(Member member) {
        log.debug("앱용 받은 초대 목록 조회 - 사용자: {}", member.getId());

        // SSR과 동일한 로직 재사용
        return getReceivedInvitationsForSSR(member);
    }

    /**
     * 앱용: 보낸 초대 목록 조회
     */
    public List<FamilyMemberResponse> getSentInvitationsForApp(Member member) {
        log.debug("앱용 보낸 초대 목록 조회 - 사용자: {}", member.getId());

        // SSR과 동일한 로직 재사용
        return getSentInvitationsForSSR(member);
    }

    /**
     * 앱용: 가족 구성원 상세 조회
     */
    public FamilyMemberResponse getFamilyMemberForApp(Long familyMemberId, Member currentUser) {
        log.debug("앱용 가족 구성원 상세 조회 - 구성원: {}, 사용자: {}", familyMemberId, currentUser.getId());

        // SSR과 동일한 로직 재사용
        return getFamilyMemberForSSR(familyMemberId, currentUser);
    }

    // ===== 기존 메서드 (호환성 유지) =====

    /**
     * 메모리얼의 가족 구성원 목록 조회 (Entity 형태)
     * 기존 코드와의 호환성을 위해 유지
     */
    public List<FamilyMember> getFamilyMembers(Memorial memorial) {
        log.debug("가족 구성원 목록 조회 (Entity) - 메모리얼: {}", memorial.getId());

        return familyMemberRepository.findByMemorialWithDetails(memorial);
    }

    /**
     * 사용자가 가족 구성원으로 속한 메모리얼 목록 조회 (FamilyMember Entity 형태)
     */
    public List<FamilyMember> getAccessibleFamilyMemberships(Member member) {
        log.debug("가족 구성원 자격으로 접근 가능한 메모리얼 조회 - 사용자: {}", member.getId());

        return familyMemberRepository.findAccessibleMemorialsWithDetails(member);
    }

    /**
     * 전체 가족 구성원 조회 (Entity 형태)
     * 기존 코드와의 호환성을 위해 유지
     */
    public List<FamilyMember> getAllFamilyMembers(Member currentUser) {
        log.debug("전체 가족 구성원 조회 (Entity) - 사용자: {}", currentUser.getId());

        return familyMemberRepository.findAllAccessibleFamilyMembers(currentUser);
    }

    // ===== 공통 액션 메서드 =====

    /**
     * 가족 구성원 초대
     */
    @Transactional
    public String inviteFamilyMember(FamilyInviteRequest request, Member inviter) {
        log.info("가족 구성원 초대 시작 - 메모리얼: {}, 초대자: {}, 연락처: {}",
                request.getMemorialId(), inviter.getId(), request.getMaskedContact());

        // 메모리얼 조회 및 권한 확인
        Memorial memorial = getMemorialAndCheckOwnership(request.getMemorialId(), inviter);

        // 초대 토큰 생성
        String inviteToken = generateInviteToken();

        // 초대 정보 저장 (임시)
        familyInviteService.saveInviteInfo(inviteToken, request, memorial, inviter);

        // 초대 발송
        familyInviteService.sendInvite(request, inviteToken, memorial, inviter);

        log.info("가족 구성원 초대 완료 - 메모리얼: {}, 토큰: {}, 연락처: {}",
                request.getMemorialId(), inviteToken.substring(0, 8) + "...", request.getMaskedContact());

        return inviteToken;
    }

    /**
     * 초대 수락
     */
    @Transactional
    public void acceptInvite(String inviteToken, Member member) {
        log.info("초대 수락 시작 - 토큰: {}, 사용자: {}",
                inviteToken.substring(0, 8) + "...", member.getId());

        // 초대 정보 조회
        var inviteInfo = familyInviteService.getInviteInfo(inviteToken);
        if (inviteInfo == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 초대 링크입니다.");
        }

        // 이미 가족 구성원인지 확인
        if (familyMemberRepository.existsByMemorialAndMember(inviteInfo.getMemorial(), member)) {
            throw new IllegalArgumentException("이미 가족 구성원으로 등록되어 있습니다.");
        }

        // 가족 구성원 생성
        FamilyMember familyMember = FamilyMember.builder()
                .memorial(inviteInfo.getMemorial())
                .member(member)
                .invitedBy(inviteInfo.getInviter())
                .relationship(inviteInfo.getRelationship())
                .inviteMessage(inviteInfo.getMessage())
                .build();

        // 초대 수락
        familyMember.acceptInvite();

        familyMemberRepository.save(familyMember);

        // 초대 정보 삭제
        familyInviteService.removeInviteInfo(inviteToken);

        log.info("초대 수락 완료 - 구성원: {}, 메모리얼: {}", member.getId(), inviteInfo.getMemorial().getId());
    }

    /**
     * 초대 거절
     */
    @Transactional
    public void rejectInvite(String inviteToken, Member member) {
        log.info("초대 거절 시작 - 토큰: {}, 사용자: {}",
                inviteToken.substring(0, 8) + "...", member.getId());

        // 초대 정보 조회
        var inviteInfo = familyInviteService.getInviteInfo(inviteToken);
        if (inviteInfo == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 초대 링크입니다.");
        }

        // 초대 정보 삭제
        familyInviteService.removeInviteInfo(inviteToken);

        log.info("초대 거절 완료 - 사용자: {}, 메모리얼: {}", member.getId(), inviteInfo.getMemorial().getId());
    }

    /**
     * 초대 취소
     */
    @Transactional
    public void cancelInvitation(Long familyMemberId, Member currentUser) {
        log.info("초대 취소 시작 - 구성원: {}, 사용자: {}", familyMemberId, currentUser.getId());

        FamilyMember familyMember = getFamilyMemberAndCheckOwnership(familyMemberId, currentUser);

        if (!familyMember.isPending()) {
            throw new IllegalArgumentException("대기 중인 초대만 취소할 수 있습니다.");
        }

        familyMember.cancelInvite();
        familyMemberRepository.save(familyMember);

        log.info("초대 취소 완료 - 구성원: {}", familyMemberId);
    }

    /**
     * 권한 설정 변경
     */
    @Transactional
    public void updatePermissions(Long familyMemberId, PermissionUpdateRequest request, Member currentUser) {
        log.info("권한 설정 변경 - 구성원: {}, 사용자: {}, 권한: {}",
                familyMemberId, currentUser.getId(), request.getSummary());

        FamilyMember familyMember = getFamilyMemberAndCheckOwnership(familyMemberId, currentUser);

        if (!familyMember.isActive()) {
            throw new IllegalArgumentException("활성 상태인 가족 구성원만 권한을 변경할 수 있습니다.");
        }

        // 권한 업데이트
        familyMember.updateMemorialAccess(request.getMemorialAccess());
        familyMember.updateVideoCallAccess(request.getVideoCallAccess());

        familyMemberRepository.save(familyMember);

        log.info("권한 설정 변경 완료 - 구성원: {}, 메모리얼: {}, 영상통화: {}",
                familyMemberId, request.getMemorialAccess(), request.getVideoCallAccess());
    }

    /**
     * 가족 구성원 제거
     */
    @Transactional
    public String removeFamilyMember(Long familyMemberId, Member currentUser) {
        log.info("가족 구성원 제거 시작 - 구성원: {}, 사용자: {}", familyMemberId, currentUser.getId());

        FamilyMember familyMember = getFamilyMemberAndCheckOwnership(familyMemberId, currentUser);
        String memberName = familyMember.getMemberName();

        familyMemberRepository.delete(familyMember);

        log.info("가족 구성원 제거 완료 - 구성원: {}, 이름: {}", familyMemberId, memberName);

        return memberName;
    }

    // ===== 권한 확인 메서드 =====

    /**
     * 가족 구성원 접근 권한 확인
     */
    public boolean canAccessFamilyMember(Member currentUser, FamilyMember familyMember) {
        // 메모리얼 소유자인 경우
        if (familyMember.getMemorial().getOwner().equals(currentUser)) {
            return true;
        }

        // 같은 메모리얼의 가족 구성원인 경우
        return familyMemberRepository.existsActiveRelation(familyMember.getMemorial(), currentUser);
    }

    /**
     * 초대 정보 조회 (SSR용)
     */
    public FamilyInviteService.InviteInfo getInviteInfo(String inviteToken) {
        return familyInviteService.getInviteInfo(inviteToken);
    }

    /**
     * ID로 가족 구성원 조회 (SSR용)
     */
    public FamilyMember getFamilyMemberById(Long familyMemberId) {
        return familyMemberRepository.findById(familyMemberId)
                .orElseThrow(() -> new IllegalArgumentException("가족 구성원을 찾을 수 없습니다."));
    }

    // ===== 내부 헬퍼 메서드 =====

    /**
     * 사용자가 접근 가능한 모든 메모리얼 조회 (Memorial Entity 리스트)
     * 소유한 메모리얼 + 가족 구성원으로 등록된 메모리얼 (중복 제거)
     */
    public List<Memorial> getAccessibleMemorials(Member member) {
        log.debug("접근 가능한 메모리얼 조회 - 사용자: {}", member.getId());

        // 소유한 메모리얼
        List<Memorial> ownedMemorials = memorialService.findByOwner(member);

        // 가족 구성원으로 접근 가능한 메모리얼
        List<FamilyMember> accessibleFamilyMembers = familyMemberRepository.findAccessibleMemorialsWithDetails(member);

        // 중복 제거하여 통합
        List<Memorial> allMemorials = new ArrayList<>(ownedMemorials);

        for (FamilyMember familyMember : accessibleFamilyMembers) {
            Memorial memorial = familyMember.getMemorial();
            if (!allMemorials.contains(memorial)) {
                allMemorials.add(memorial);
            }
        }

        log.debug("접근 가능한 메모리얼 조회 완료 - 사용자: {}, 개수: {}",
                member.getId(), allMemorials.size());

        return allMemorials;
    }

    /**
     * 통계 정보 생성
     */
    private FamilyAllDataResponse.StatisticsInfo buildStatisticsInfo(
            List<Memorial> memorials, List<FamilyMemberResponse> familyMembers) {

        int totalMemorials = memorials.size();
        int totalMembers = familyMembers.size();
        int activeMembers = (int) familyMembers.stream().filter(FamilyMemberResponse::isActive).count();
        int pendingInvitations = (int) familyMembers.stream().filter(FamilyMemberResponse::isPending).count();

        return FamilyAllDataResponse.StatisticsInfo.builder()
                .totalMemorials(totalMemorials)
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .pendingInvitations(pendingInvitations)
                .build();
    }

    /**
     * 메모리얼 정보 변환
     */
    private FamilyAllDataResponse.MemorialInfo buildMemorialInfo(Memorial memorial) {
        return FamilyAllDataResponse.MemorialInfo.builder()
                .id(memorial.getId())
                .name(memorial.getName())
                .nickname(memorial.getNickname())
                .mainProfileImageUrl(memorial.getMainProfileImageUrl())
                .isActive(memorial.isActive())
                .familyMemberCount(familyMemberRepository.countActiveMembers(memorial))
                .build();
    }

    /**
     * 메모리얼 조회 및 접근 권한 확인
     */
    private Memorial getMemorialAndCheckAccess(Long memorialId, Member currentUser) {
        Memorial memorial = memorialRepository.findById(memorialId)
                .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없습니다."));

        if (!memorial.canBeViewedBy(currentUser)) {
            throw new IllegalArgumentException("메모리얼에 접근할 권한이 없습니다.");
        }

        return memorial;
    }

    /**
     * 메모리얼 조회 및 소유권 확인
     */
    private Memorial getMemorialAndCheckOwnership(Long memorialId, Member currentUser) {
        Memorial memorial = memorialRepository.findById(memorialId)
                .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없습니다."));

        if (!memorial.getOwner().equals(currentUser)) {
            throw new SecurityException("메모리얼 소유자만 가족 구성원을 관리할 수 있습니다.");
        }

        return memorial;
    }

    /**
     * 가족 구성원 조회 및 접근 권한 확인
     */
    private FamilyMember getFamilyMemberAndCheckAccess(Long familyMemberId, Member currentUser) {
        FamilyMember familyMember = familyMemberRepository.findById(familyMemberId)
                .orElseThrow(() -> new IllegalArgumentException("가족 구성원을 찾을 수 없습니다."));

        if (!canAccessFamilyMember(currentUser, familyMember)) {
            throw new IllegalArgumentException("가족 구성원에 접근할 권한이 없습니다.");
        }

        return familyMember;
    }

    /**
     * 가족 구성원 조회 및 소유권 확인
     */
    private FamilyMember getFamilyMemberAndCheckOwnership(Long familyMemberId, Member currentUser) {
        FamilyMember familyMember = familyMemberRepository.findById(familyMemberId)
                .orElseThrow(() -> new IllegalArgumentException("가족 구성원을 찾을 수 없습니다."));

        if (!familyMember.getMemorial().getOwner().equals(currentUser)) {
            throw new SecurityException("메모리얼 소유자만 가족 구성원을 관리할 수 있습니다.");
        }

        return familyMember;
    }

    /**
     * 초대 토큰 생성
     */
    private String generateInviteToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis();
    }
}