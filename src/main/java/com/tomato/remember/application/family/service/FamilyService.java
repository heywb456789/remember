package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.dto.FamilyAllDataResponse;
import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.dto.FamilyMemberResponse;
import com.tomato.remember.application.family.dto.FamilyPageData;
import com.tomato.remember.application.family.dto.FamilySearchCondition;
import com.tomato.remember.application.family.dto.MemorialSummaryResponse;
import com.tomato.remember.application.family.dto.FamilyPermissionRequest;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.repository.FamilyMemberRepository;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.common.dto.ListDTO;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 가족 관리 서비스 - SSR용 메서드: *ForSSR - 앱용 메서드: *ForApp - 공통 액션 메서드: invite, updatePermissions 등
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
     * 가족 페이지 데이터 조회
     *
     * @param member     현재 사용자
     * @param memorialId 선택된 메모리얼 ID (null이면 최신 메모리얼 선택)
     * @return FamilyPageData
     */
    public FamilyPageData getFamilyPageData(Member member, Long memorialId) {
        log.info("가족 페이지 데이터 조회 - 사용자: {}, 메모리얼ID: {}", member.getId(), memorialId);

        // 1. 내가 소유한 메모리얼 조회 (최신순)
        List<Memorial> myMemorials = memorialService.findByOwner(member);

        if (myMemorials.isEmpty()) {
            log.warn("소유한 메모리얼이 없음 - 사용자: {}", member.getId());
            return FamilyPageData.builder()
                .memorials(Collections.emptyList())
                .selectedMemorial(null)
                .familyMembers(Collections.emptyList())
                .totalMemorials(0)
                .totalMembers(0)
                .build();
        }

        // 2. 선택된 메모리얼 결정
        Memorial selectedMemorial = determineSelectedMemorial(myMemorials, memorialId);
        log.info("선택된 메모리얼 결정 - ID: {}, 이름: {}", selectedMemorial.getId(), selectedMemorial.getName());

        // 3. 선택된 메모리얼의 가족 구성원 조회 (소유자 포함)
        List<FamilyMemberResponse> familyMembers = getFamilyMembersWithOwner(selectedMemorial, member);

        // 4. 페이지 데이터 구성
        return FamilyPageData.builder()
            .memorials(myMemorials)
            .selectedMemorial(selectedMemorial)
            .familyMembers(familyMembers)
            .totalMemorials(myMemorials.size())
            .totalMembers(familyMembers.size())
            .build();
    }

    /**
     * 선택된 메모리얼 결정
     *
     * @param myMemorials 내 소유 메모리얼 목록
     * @param memorialId  요청된 메모리얼 ID
     * @return 선택된 메모리얼
     */
    private Memorial determineSelectedMemorial(List<Memorial> myMemorials, Long memorialId) {
        if (memorialId != null) {
            // 파라미터로 받은 메모리얼이 내 소유인지 확인
            return myMemorials.stream()
                .filter(m -> m.getId().equals(memorialId))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("요청된 메모리얼이 내 소유가 아님 - 메모리얼ID: {}, 최신 메모리얼로 대체", memorialId);
                    return myMemorials.get(0);
                });
        } else {
            // 첫 진입 시 가장 최신 메모리얼 선택
            log.info("첫 진입 - 최신 메모리얼 자동 선택");
            return myMemorials.get(0);
        }
    }

    /**
     * 특정 메모리얼의 가족 구성원 조회 (소유자 포함)
     *
     * @param memorial 메모리얼
     * @param owner    소유자
     * @return 가족 구성원 목록 (소유자 포함)
     */
    public List<FamilyMemberResponse> getFamilyMembersWithOwner(Memorial memorial, Member owner) {
        log.info("메모리얼 가족 구성원 조회 (소유자 포함) - 메모리얼: {}", memorial.getId());

        List<FamilyMemberResponse> result = new ArrayList<>();

        // 1. 소유자 정보 먼저 추가
        FamilyMemberResponse ownerResponse = createOwnerResponse(memorial, owner);
        result.add(ownerResponse);

        // 2. 일반 가족 구성원 조회 및 추가
        List<FamilyMember> familyMembers = familyMemberRepository.findByMemorialOrderByCreatedAtDesc(memorial);
        List<FamilyMemberResponse> memberResponses = familyMembers.stream()
            .map(FamilyMemberResponse::from)
            .collect(Collectors.toList());

        result.addAll(memberResponses);

        log.info("가족 구성원 조회 완료 - 총 {}명 (소유자 포함)", result.size());
        return result;
    }

    /**
     * 소유자 응답 DTO 생성
     */
    private FamilyMemberResponse createOwnerResponse(Memorial memorial, Member owner) {
        return FamilyMemberResponse.builder()
            .id(- 1L) // 소유자는 음수 ID 사용
            .memorial(FamilyMemberResponse.MemorialInfo.builder()
                .id(memorial.getId())
                .name(memorial.getName())
                .nickname(memorial.getNickname())
                .mainProfileImageUrl(memorial.getMainProfileImageUrl())
                .isActive(memorial.isActive())
                .build())
            .member(FamilyMemberResponse.MemberInfo.builder()
                .id(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .phoneNumber(owner.getPhoneNumber())
                .profileImageUrl(owner.getProfileImageUrl())
                .isActive(owner.isActive())
                .build())
            .relationship(Relationship.SELF)
            .relationshipDisplayName("본인")
            .inviteStatus(InviteStatus.ACCEPTED)
            .inviteStatusDisplayName("활성")
            .permissions(FamilyMemberResponse.PermissionInfo.builder()
                .memorialAccess(true)
                .videoCallAccess(true)
                .canModify(true)
                .build())
            .build();
    }


    /**
     * SSR용: 전체 가족 구성원 조회 (DTO 형태) 서버 렌더링에서 바로 사용 가능한 형태로 반환
     */
    public List<FamilyMemberResponse> getAllFamilyMembersForSSR(Member member) {
        log.debug("SSR용 전체 가족 구성원 조회 - 사용자: {}", member.getId());

        List<FamilyMemberResponse> result = new ArrayList<>();

        try {
            //  1. 내가 소유한 메모리얼들 조회 (디버깅 강화)
            List<Memorial> myMemorials = memorialService.findByOwner(member);
            log.info("🔍 디버깅: 소유한 메모리얼 수: {} (사용자: {})", myMemorials.size(), member.getId());

            for (Memorial memorial : myMemorials) {
                log.info("🔍 디버깅: 메모리얼 처리 중 - ID: {}, 이름: {}, 소유자: {}",
                    memorial.getId(), memorial.getName(), memorial.getOwner().getId());

                try {
                    FamilyMemberResponse myInfo = createOwnerAsFamilyMember(memorial, member);
                    result.add(myInfo);
                    log.info("✅ 소유자 정보 추가 완료: 메모리얼={}, 소유자={}",
                        memorial.getId(), myInfo.getMember().getName());
                } catch (Exception e) {
                    log.error("❌ 소유자 정보 생성 실패: 메모리얼={}", memorial.getId(), e);
                }
            }

            //  2. 초대된 가족 구성원들 추가 (디버깅 강화)
            List<FamilyMember> familyMembers = familyMemberRepository.findAllAccessibleFamilyMembers(member);
            log.info("🔍 디버깅: 초대된 가족 구성원 수: {} (사용자: {})", familyMembers.size(), member.getId());

            List<FamilyMemberResponse> invitedMembers = familyMembers.stream()
                .map(fm -> {
                    log.debug("🔍 디버깅: 초대받은 구성원 - ID: {}, 이름: {}, 메모리얼: {}",
                        fm.getId(), fm.getMember().getName(), fm.getMemorial().getId());
                    return FamilyMemberResponse.from(fm);
                })
                .collect(Collectors.toList());

            result.addAll(invitedMembers);

            log.info("🎯 SSR용 전체 가족 구성원 조회 완료 - 사용자: {}, 총 구성원 수: {} (소유자: {}, 초대된 구성원: {})",
                member.getId(), result.size(), myMemorials.size(), invitedMembers.size());

            //  3. 결과 상세 로깅
            for (FamilyMemberResponse familyMemberResponse : result) {
                log.debug("📋 결과 구성원: ID={}, 이름={}, 관계={}, 메모리얼={}",
                    familyMemberResponse.getId(), familyMemberResponse.getMember().getName(),
                    familyMemberResponse.getRelationship(), familyMemberResponse.getMemorial().getId());
            }

        } catch (Exception e) {
            log.error("❌ 가족 구성원 조회 중 오류 발생 - 사용자: {}", member.getId(), e);
            // 오류가 발생해도 빈 리스트라도 반환
        }

        return result;
    }

    /**
     * 소유자 정보를 FamilyMemberResponse로 변환
     */
    //  public으로 변경해서 테스트
    public FamilyMemberResponse createOwnerAsFamilyMember(Memorial memorial, Member owner) {
        log.debug("🏠 소유자 정보 생성 중 - 메모리얼: {}, 소유자: {}", memorial.getId(), owner.getId());

        //  널 체크 추가
        if (memorial == null) {
            log.error("❌ Memorial이 null입니다!");
            throw new IllegalArgumentException("Memorial cannot be null");
        }

        if (owner == null) {
            log.error("❌ Owner가 null입니다!");
            throw new IllegalArgumentException("Owner cannot be null");
        }

        //  소유권 확인
        if (! memorial.getOwner().equals(owner)) {
            log.warn("⚠️ 소유권 불일치: 메모리얼 소유자={}, 요청자={}",
                memorial.getOwner().getId(), owner.getId());
        }

        FamilyMemberResponse ownerResponse = FamilyMemberResponse.builder()
            .id(- memorial.getId()) // 음수 ID로 구분 (메모리얼별 고유)
            .memorial(FamilyMemberResponse.MemorialInfo.builder()
                .id(memorial.getId())
                .name(memorial.getName())
                .nickname(memorial.getNickname())
                .mainProfileImageUrl(memorial.getMainProfileImageUrl())
                .isActive(memorial.isActive())
                .build())
            .member(FamilyMemberResponse.MemberInfo.builder()
                .id(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail()) // 소유자는 이메일 표시
                .phoneNumber(owner.getPhoneNumber()) // 소유자는 전화번호 표시
                .profileImageUrl(owner.getProfileImageUrl())
                .isActive(owner.isActive())
                .build())
            .invitedBy(FamilyMemberResponse.MemberInfo.builder()
                .id(owner.getId())
                .name(owner.getName())
                .build())
            .relationship(Relationship.SELF) //  핵심: SELF 관계
            .relationshipDisplayName("메모리얼 소유자") //  표시명 명확화
            .inviteStatus(InviteStatus.ACCEPTED)
            .inviteStatusDisplayName("메모리얼 소유자")
            .permissions(FamilyMemberResponse.PermissionInfo.builder()
                .memorialAccess(true)
                .videoCallAccess(true)
                .canModify(false) // 소유자는 권한 수정 불가
                .build())
            .dateTime(FamilyMemberResponse.DateTimeInfo.builder()
                .createdAt(memorial.getCreatedAt())
                .lastAccessAt(LocalDateTime.now())
                .formattedLastAccess("방금 전")
                .build())
            .build();

        log.info("✅ 소유자 정보 생성 완료: ID={}, 이름={}, 관계={}",
            ownerResponse.getId(), ownerResponse.getMember().getName(), ownerResponse.getRelationship());

        return ownerResponse;
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
     * 앱용: 전체 가족 관리 데이터 조회 SSR과 동일한 데이터를 JSON 형태로 제공
     */
    public FamilyAllDataResponse getAllFamilyDataForApp(Member member) {
        log.debug("앱용 전체 가족 데이터 조회 - 사용자: {}", member.getId());

        //  1. SSR과 동일한 로직으로 데이터 조회 (소유자 포함)
        List<FamilyMemberResponse> allFamilyMembers = getAllFamilyMembersForSSR(member);

        //  2. 접근 가능한 모든 메모리얼 조회
        List<Memorial> accessibleMemorials = getAccessibleMemorials(member);

        //  3. 통계 정보 계산
        FamilyAllDataResponse.StatisticsInfo statistics = buildStatisticsInfo(
            accessibleMemorials, allFamilyMembers);

        //  4. 메모리얼 정보 변환
        List<FamilyAllDataResponse.MemorialInfo> memorialInfos = accessibleMemorials.stream()
            .map(this::buildMemorialInfo)
            .collect(Collectors.toList());

        FamilyAllDataResponse response = FamilyAllDataResponse.builder()
            .memorials(memorialInfos)
            .familyMembers(allFamilyMembers) //  소유자 포함된 전체 목록
            .statistics(statistics)
            .build();

        log.debug("앱용 전체 가족 데이터 조회 완료 - 사용자: {}, 메모리얼: {}, 가족 구성원: {} (소유자 포함)",
            member.getId(), memorialInfos.size(), allFamilyMembers.size());

        return response;
    }

    /**
     * 특정 메모리얼의 가족 구성원 조회 (SSR용)
     */
    public List<FamilyMemberResponse> getFamilyMembersByMemorial(Long memorialId) {
        log.info("메모리얼 가족 구성원 조회 - 메모리얼: {}", memorialId);

        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없습니다."));

        List<FamilyMember> familyMembers = familyMemberRepository.findByMemorialOrderByCreatedAtDesc(memorial);

        return familyMembers.stream()
            .map(FamilyMemberResponse::from)
            .collect(Collectors.toList());
    }


    /**
     * 앱용: 특정 메모리얼의 가족 구성원 조회 (페이징)
     */
    public ListDTO<FamilyMemberResponse> getFamilyMembersForApp(Long memorialId, Member currentUser,
        Pageable pageable) {
        log.debug("앱용 메모리얼 가족 구성원 조회 (페이징) - 메모리얼: {}, 사용자: {}", memorialId, currentUser.getId());

        Memorial memorial = getMemorialAndCheckAccess(memorialId, currentUser);

        // 페이징 조회
        Page<FamilyMember> familyMembersPage = familyMemberRepository.findByMemorialOrderByCreatedAtDesc(memorial,
            pageable);

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
     * 메모리얼의 가족 구성원 목록 조회 (Entity 형태) 기존 코드와의 호환성을 위해 유지
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
     * 전체 가족 구성원 조회 (Entity 형태) 기존 코드와의 호환성을 위해 유지
     */
    public List<FamilyMember> getAllFamilyMembers(Member currentUser) {
        log.debug("전체 가족 구성원 조회 (Entity) - 사용자: {}", currentUser.getId());

        return familyMemberRepository.findAllAccessibleFamilyMembers(currentUser);
    }

    /**
     * 권한 설정 변경
     */
    @Transactional
    public void updatePermissions(Long familyMemberId, FamilyPermissionRequest request, Member currentUser) {
        log.info("권한 설정 변경 - 구성원: {}, 사용자: {}, 권한: {}",
            familyMemberId, currentUser.getId(), request.getSummary());

        FamilyMember familyMember = getFamilyMemberAndCheckOwnership(familyMemberId, currentUser);

        if (! familyMember.isActive()) {
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
     * ID로 가족 구성원 조회 (SSR용)
     */
    public FamilyMember getFamilyMemberById(Long familyMemberId) {
        return familyMemberRepository.findById(familyMemberId)
            .orElseThrow(() -> new IllegalArgumentException("가족 구성원을 찾을 수 없습니다."));
    }

    // ===== 내부 헬퍼 메서드 =====

    /**
     * 사용자가 접근 가능한 모든 메모리얼 조회 (Memorial Entity 리스트) 소유한 메모리얼 + 가족 구성원으로 등록된 메모리얼 (중복 제거)
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
            if (! allMemorials.contains(memorial)) {
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

        if (! memorial.canBeViewedBy(currentUser)) {
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

        if (! memorial.getOwner().equals(currentUser)) {
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

        if (! canAccessFamilyMember(currentUser, familyMember)) {
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

        if (! familyMember.getMemorial().getOwner().equals(currentUser)) {
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

    /**
     * 메모리얼 접근 권한 검증
     */
    public Memorial validateMemorialAccess(Member member, Long memorialId) {
        Memorial memorial = memorialRepository.findByIdAndOwner(memorialId, member)
            .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없거나 접근 권한이 없습니다."));

        if (! memorial.isActive()) {
            throw new IllegalArgumentException("비활성화된 메모리얼입니다.");
        }

        return memorial;
    }

    /**
     * 특정 메모리얼의 가족 구성원 조회 (페이징)
     */
    public Page<FamilyMemberResponse> getFamilyMembersWithOwnerPaged(Member member, Long memorialId,
        Pageable pageable) {
        log.info("메모리얼 가족 구성원 페이징 조회 - 사용자: {}, 메모리얼: {}, 페이지: {}",
            member.getId(), memorialId, pageable.getPageNumber());

        // 메모리얼 접근 권한 검증
        Memorial memorial = validateMemorialAccess(member, memorialId);

        // 가족 구성원 페이징 조회
        Page<FamilyMember> familyMemberPage = familyMemberRepository.findByMemorialOrderByCreatedAtDesc(memorial,
            pageable);

        // 소유자 정보 추가 처리
        List<FamilyMemberResponse> responses = new ArrayList<>();

        // 첫 번째 페이지인 경우 소유자 정보 추가
        if (pageable.getPageNumber() == 0) {
            FamilyMemberResponse ownerResponse = createOwnerResponse(memorial, member);
            responses.add(ownerResponse);
        }

        // 일반 가족 구성원 추가
        List<FamilyMemberResponse> memberResponses = familyMemberPage.getContent().stream()
            .map(FamilyMemberResponse::from)
            .collect(Collectors.toList());
        responses.addAll(memberResponses);

        // 전체 개수에 소유자 1명 추가
        long totalElements = familyMemberPage.getTotalElements() + 1;

        return new PageImpl<>(responses, pageable, totalElements);
    }

    /**
     * 내 메모리얼 목록 조회 (페이징)
     */
    public Page<MemorialSummaryResponse> getMyMemorialSummariesPaged(Member member, Pageable pageable) {
        log.info("내 메모리얼 목록 페이징 조회 - 사용자: {}, 페이지: {}",
            member.getId(), pageable.getPageNumber());

        Page<Memorial> memorialPage = memorialRepository.findByOwnerOrderByCreatedAtDesc(member, pageable);

        List<MemorialSummaryResponse> responses = memorialPage.getContent().stream()
            .map(MemorialSummaryResponse::from)
            .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, memorialPage.getTotalElements());
    }

    /**
     * 가족 구성원 검색 (페이징)
     */
    public Page<FamilyMemberResponse> searchFamilyMembers(Member member, FamilySearchCondition condition,
        Pageable pageable) {
        log.info("가족 구성원 검색 - 사용자: {}, 조건: {}, 페이지: {}",
            member.getId(), condition, pageable.getPageNumber());

        Page<FamilyMember> searchResults = familyMemberRepository.searchFamilyMembers(member, condition, pageable);

        List<FamilyMemberResponse> responses = searchResults.getContent().stream()
            .map(FamilyMemberResponse::from)
            .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, searchResults.getTotalElements());
    }

    /**
     * 가족 구성원 초대
     */
    @Transactional
    public String inviteFamilyMember(Member member, FamilyInviteRequest request) {
        log.info("가족 구성원 초대 - 사용자: {}, 요청: {}", member.getId(), request);

        // 메모리얼 접근 권한 검증
        Memorial memorial = validateMemorialAccess(member, request.getMemorialId());

        // 연락처 유효성 검사
        validateContact(request.getMethod(), request.getContact());

        // 중복 초대 확인
        if (isDuplicateInvite(memorial, request.getContact())) {
            throw new IllegalArgumentException("이미 초대된 연락처입니다.");
        }

        // 초대 처리 로직 (실제 구현 필요)
        // TODO: 이메일/SMS 발송 로직 구현

        log.info("가족 구성원 초대 완료 - 메모리얼: {}, 연락처: {}",
            memorial.getId(), request.getContact());

        return "초대가 완료되었습니다.";
    }

    /**
     * 가족 구성원 권한 수정
     */
    @Transactional
    public void updateMemberPermissions(Member member, Long memberId, FamilyPermissionRequest request) {
        log.info("가족 구성원 권한 수정 - 사용자: {}, 구성원: {}, 요청: {}",
            member.getId(), memberId, request);

        // 가족 구성원 조회 및 권한 확인
        FamilyMember familyMember = familyMemberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("가족 구성원을 찾을 수 없습니다."));

        // 소유자 권한 확인
        if (! familyMember.getMemorial().getOwner().equals(member)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 자기 자신은 권한 수정 불가
        if (familyMember.getMember().equals(member)) {
            throw new IllegalArgumentException("자기 자신의 권한은 수정할 수 없습니다.");
        }

        // 권한 수정
        familyMember.updateMemorialAccess(request.getMemorialAccess());
        familyMember.updateVideoCallAccess(request.getVideoCallAccess());

        log.info("가족 구성원 권한 수정 완료 - 구성원: {}", memberId);
    }

    /**
     * 가족 구성원 삭제
     */
    @Transactional
    public void removeFamilyMember(Member member, Long memberId) {
        log.info("가족 구성원 삭제 - 사용자: {}, 구성원: {}", member.getId(), memberId);

        // 가족 구성원 조회 및 권한 확인
        FamilyMember familyMember = familyMemberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("가족 구성원을 찾을 수 없습니다."));

        // 소유자 권한 확인
        if (! familyMember.getMemorial().getOwner().equals(member)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 자기 자신은 삭제 불가
        if (familyMember.getMember().equals(member)) {
            throw new IllegalArgumentException("자기 자신은 삭제할 수 없습니다.");
        }

        // 가족 구성원 삭제
        familyMemberRepository.delete(familyMember);

        log.info("가족 구성원 삭제 완료 - 구성원: {}", memberId);
    }

    /**
     * 연락처 유효성 검사
     */
    private void validateContact(String method, String contact) {
        if ("email".equals(method)) {
            if (! contact.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
            }
        } else if ("sms".equals(method)) {
            if (! contact.matches("^010-\\d{4}-\\d{4}$")) {
                throw new IllegalArgumentException("유효하지 않은 전화번호 형식입니다.");
            }
        }
    }

    /**
     * 중복 초대 확인
     */
    private boolean isDuplicateInvite(Memorial memorial, String contact) {
        List<FamilyMember> existingMembers = familyMemberRepository.findByContact(contact, contact);

        return existingMembers.stream()
            .anyMatch(fm -> fm.getMemorial().getId().equals(memorial.getId())
                && fm.getInviteStatus() != InviteStatus.REJECTED
                && fm.getInviteStatus() != InviteStatus.CANCELLED);
    }

}