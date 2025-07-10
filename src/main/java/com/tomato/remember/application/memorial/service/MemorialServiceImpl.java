package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialUpdateRequestDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.APIException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 메모리얼 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemorialServiceImpl implements MemorialService {

    private final MemorialRepository memorialRepository;

    /**
     * 사용자별 메모리얼 목록 조회
     *
     * @param member 조회할 사용자
     * @return 메모리얼 목록
     */
    @Override
    public List<Memorial> getMemorialListByMember(Member member) {
        log.debug("Getting memorial list for member: {} (ID: {})", member.getName(), member.getId());

        List<Memorial> memorialList = memorialRepository.findByOwnerAndStatusNot(member, MemorialStatus.DELETED);

        log.debug("Found {} memorials for member: {} (ID: {})", memorialList.size(), member.getName(), member.getId());

        return memorialList;
    }

    /**
     * 사용자별 활성 메모리얼 목록 조회
     *
     * @param member 조회할 사용자
     * @return 활성 메모리얼 목록
     */
    @Override
    public List<Memorial> getActiveMemorialListByMember(Member member) {
        log.debug("Getting active memorial list for member: {} (ID: {})", member.getName(), member.getId());

        List<Memorial> activeMemorialList = memorialRepository.findByOwnerAndStatus(member, MemorialStatus.ACTIVE);

        log.debug("Found {} active memorials for member: {} (ID: {})", activeMemorialList.size(), member.getName(),
            member.getId());

        return activeMemorialList;
    }

    /**
     * 사용자별 메모리얼 개수 조회
     *
     * @param member 조회할 사용자
     * @return 메모리얼 개수
     */
    @Override
    public int getMemorialCountByMember(Member member) {
        log.debug("Getting memorial count for member: {} (ID: {})", member.getName(), member.getId());

        long count = memorialRepository.countByOwnerAndStatus(member, MemorialStatus.ACTIVE);

        log.debug("Memorial count for member: {} (ID: {}) = {}",
            member.getName(), member.getId(), count);

        return (int) count;
    }

    /**
     * 사용자별 활성 메모리얼 개수 조회
     *
     * @param member 조회할 사용자
     * @return 활성 메모리얼 개수
     */
    @Override
    public int getActiveMemorialCountByMember(Member member) {
        log.debug("Getting active memorial count for member: {} (ID: {})", member.getName(), member.getId());

        long count = memorialRepository.countByOwnerAndStatus(member, MemorialStatus.ACTIVE);

        log.debug("Active memorial count for member: {} (ID: {}) = {}",
            member.getName(), member.getId(), count);

        return (int) count;
    }

    /**
     * 메모리얼 생성
     *
     * @param createRequest 메모리얼 생성 요청 DTO
     * @param member        생성할 사용자
     * @return 생성된 메모리얼 응답 DTO
     */
    @Override
    @Transactional
    public MemorialResponseDTO createMemorial(MemorialCreateRequestDTO createRequest, Member member) {
        log.info("Creating memorial for member: {} (ID: {}), memorial name: {}",
            member.getName(), member.getId(), createRequest.getName());

        // 입력 검증
        validateMemorialCreation(createRequest, member);

        // Memorial 엔티티 생성
        Memorial memorial = Memorial.fromCreateRequestDTO(createRequest, member);

        // 저장
        Memorial savedMemorial = memorialRepository.save(memorial);

        log.info("Memorial created successfully: ID={}, Name={}, Owner={}",
            savedMemorial.getId(), savedMemorial.getName(), member.getName());

        return savedMemorial.toResponseDTO();
    }

    /**
     * 메모리얼 ID로 조회
     *
     * @param memorialId 메모리얼 ID
     * @param member     조회할 사용자
     * @return 메모리얼 응답 DTO
     */
    @Override
    public MemorialResponseDTO getMemorialById(Long memorialId, Member member) {
        log.debug("Getting memorial by ID: {} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 접근 권한 확인
        if (! memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_MEMORIAL);
        }

        log.debug("Memorial found: ID={}, Name={}", memorial.getId(), memorial.getName());

        return memorial.toResponseDTO();
    }

    /**
     * 메모리얼 수정
     *
     * @param memorialId    메모리얼 ID
     * @param updateRequest 메모리얼 수정 요청 DTO
     * @param member        수정할 사용자
     * @return 수정된 메모리얼 응답 DTO
     */
    @Override
    @Transactional
    public MemorialResponseDTO updateMemorial(Long memorialId, MemorialUpdateRequestDTO updateRequest, Member member) {
        log.info("Updating memorial: ID={} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 수정 권한 확인
        if (! memorial.canBeEditedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_UPDATE_MEMORIAL);
        }

        // 입력 검증
        validateMemorialUpdate(updateRequest, memorial);

        // 메모리얼 정보 수정
        memorial.updateMemorial(updateRequest);

        // 저장
        Memorial savedMemorial = memorialRepository.save(memorial);

        log.info("Memorial updated successfully: ID={}, Name={}", savedMemorial.getId(), savedMemorial.getName());

        return savedMemorial.toResponseDTO();
    }

    /**
     * 메모리얼 수정 폼 데이터 조회
     *
     * @param memorialId 메모리얼 ID
     * @param member     조회할 사용자
     * @return 메모리얼 수정 요청 DTO
     */
    @Override
    public MemorialUpdateRequestDTO getMemorialForUpdate(Long memorialId, Member member) {
        log.debug("Getting memorial for update: ID={} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 수정 권한 확인
        if (! memorial.canBeEditedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_UPDATE_MEMORIAL);
        }

        log.debug("Memorial found for update: ID={}, Name={}", memorial.getId(), memorial.getName());

        return memorial.toUpdateRequestDTO();
    }

    /**
     * 메모리얼 방문 기록
     *
     * @param memorialId 메모리얼 ID
     * @param member     방문할 사용자
     */
    @Override
    @Transactional
    public void recordMemorialVisit(Long memorialId, Member member) {
        log.debug("Recording memorial visit: ID={} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 접근 권한 확인
        if (! memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_MEMORIAL);
        }

        // 방문 기록
        memorial.recordVisit();
        memorialRepository.save(memorial);

        log.debug("Memorial visit recorded: ID={}, Total visits: {}", memorial.getId(), memorial.getTotalVisits());
    }

    /**
     * 메모리얼 삭제 (소프트 삭제)
     *
     * @param memorialId 메모리얼 ID
     * @param member     삭제할 사용자
     */
    @Override
    @Transactional
    public void deleteMemorial(Long memorialId, Member member) {
        log.info("Deleting memorial: ID={} for member: {} (ID: {})", memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 삭제 권한 확인 (소유자만 삭제 가능)
        if (! memorial.getOwner().equals(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_DELETE_MEMORIAL);
        }

        // 삭제 가능 여부 검증
        validateMemorialDeletion(memorial);

        // 삭제 상태로 변경
        memorial.setStatus(MemorialStatus.DELETED);
        memorialRepository.save(memorial);

        log.info("Memorial deleted successfully: ID={}, Name={}", memorial.getId(), memorial.getName());
    }

    /**
     * 메모리얼 상태 변경 (활성화/비활성화)
     *
     * @param memorialId 메모리얼 ID
     * @param member     상태 변경할 사용자
     */
    @Override
    @Transactional
    public void toggleMemorialStatus(Long memorialId, Member member) {
        log.info("Toggling memorial status: ID={} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 상태 변경 권한 확인
        if (! memorial.canBeEditedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_UPDATE_MEMORIAL);
        }

        // 상태 토글
        if (memorial.isActive()) {
            memorial.setStatus(MemorialStatus.INACTIVE);
            log.info("Memorial deactivated: ID={}", memorial.getId());
        } else if (memorial.isInactive()) {
            memorial.setStatus(MemorialStatus.ACTIVE);
            log.info("Memorial activated: ID={}", memorial.getId());
        }

        memorialRepository.save(memorial);
    }

    /**
     * 메모리얼 활성화
     *
     * @param memorialId 메모리얼 ID
     * @param member     활성화할 사용자
     */
    @Override
    @Transactional
    public void activateMemorial(Long memorialId, Member member) {
        log.info("Activating memorial: ID={} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 활성화 권한 확인
        if (! memorial.canBeEditedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_UPDATE_MEMORIAL);
        }

        memorial.setStatus(MemorialStatus.ACTIVE);
        memorialRepository.save(memorial);

        log.info("Memorial activated successfully: ID={}", memorial.getId());
    }

    /**
     * 메모리얼 비활성화
     *
     * @param memorialId 메모리얼 ID
     * @param member     비활성화할 사용자
     */
    @Override
    @Transactional
    public void deactivateMemorial(Long memorialId, Member member) {
        log.info("Deactivating memorial: ID={} for member: {} (ID: {})",
            memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 비활성화 권한 확인
        if (! memorial.canBeEditedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_UPDATE_MEMORIAL);
        }

        memorial.setStatus(MemorialStatus.INACTIVE);
        memorialRepository.save(memorial);

        log.info("Memorial deactivated successfully: ID={}", memorial.getId());
    }

    /**
     * 메모리얼 복구 (삭제 상태에서 활성화)
     *
     * @param memorialId 메모리얼 ID
     * @param member     복구할 사용자
     */
    @Override
    @Transactional
    public void restoreMemorial(Long memorialId, Member member) {
        log.info("Restoring memorial: ID={} for member: {} (ID: {})", memorialId, member.getName(), member.getId());

        Memorial memorial = memorialRepository.findByIdAndStatus(memorialId, MemorialStatus.DELETED)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 복구 권한 확인 (소유자만 복구 가능)
        if (! memorial.getOwner().equals(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_DELETE_MEMORIAL);
        }

        memorial.setStatus(MemorialStatus.ACTIVE);
        memorialRepository.save(memorial);

        log.info("Memorial restored successfully: ID={}", memorial.getId());
    }

    /**
     * 메모리얼 완전 삭제 (물리적 삭제) - 관리자 전용
     *
     * @param memorialId 메모리얼 ID
     * @param member     삭제할 사용자 (관리자 권한 필요)
     */
    @Override
    @Transactional
    public void permanentlyDeleteMemorial(Long memorialId, Member member) {
        log.warn("Permanently deleting memorial: ID={} for member: {} (ID: {})", memorialId, member.getName(),
            member.getId());

        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 완전 삭제 권한 확인 (소유자만 가능)
        if (! memorial.getOwner().equals(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_DELETE_MEMORIAL);
        }

        // 완전 삭제 가능 여부 검증
        validateMemorialPermanentDeletion(memorial);

        // 물리적 삭제
        memorialRepository.delete(memorial);

        log.warn("Memorial permanently deleted: ID={}", memorialId);
    }

    /**
     * 사용자별 삭제된 메모리얼 목록 조회
     *
     * @param member 조회할 사용자
     * @return 삭제된 메모리얼 목록
     */
    @Override
    public List<Memorial> getDeletedMemorialListByMember(Member member) {
        log.debug("Getting deleted memorial list for member: {} (ID: {})", member.getName(), member.getId());

        List<Memorial> deletedMemorialList = memorialRepository.findByOwnerAndStatus(member, MemorialStatus.DELETED);

        log.debug("Found {} deleted memorials for member: {} (ID: {})",
            deletedMemorialList.size(), member.getName(), member.getId());

        return deletedMemorialList;
    }

    /**
     * 사용자별 비활성 메모리얼 목록 조회
     *
     * @param member 조회할 사용자
     * @return 비활성 메모리얼 목록
     */
    @Override
    public List<Memorial> getInactiveMemorialListByMember(Member member) {
        log.debug("Getting inactive memorial list for member: {} (ID: {})", member.getName(), member.getId());

        List<Memorial> inactiveMemorialList = memorialRepository.findByOwnerAndStatus(member, MemorialStatus.INACTIVE);

        log.debug("Found {} inactive memorials for member: {} (ID: {})",
            inactiveMemorialList.size(), member.getName(), member.getId());

        return inactiveMemorialList;
    }

    /**
     * 메모리얼 생성 시 검증
     *
     * @param createRequest 생성 요청 DTO
     * @param member        생성할 사용자
     */
    private void validateMemorialCreation(MemorialCreateRequestDTO createRequest, Member member) {

        // 미래 날짜 확인
        LocalDate today = LocalDate.now();
        if (createRequest.getBirthDate() != null && createRequest.getBirthDate().isAfter(today)) {
            throw new APIException(ResponseStatus.BAD_REQUEST_MEMORIAL_BIRTH);
        }

        if (createRequest.getDeathDate() != null && createRequest.getDeathDate().isAfter(today)) {
            throw new APIException(ResponseStatus.BAD_REQUEST_MEMORIAL_DEATH);
        }

        // TODO: 추후 구독 서비스 연동 시 메모리얼 생성 제한 검증 추가
    }

    /**
     * 메모리얼 수정 시 검증
     *
     * @param updateRequest 수정 요청 DTO
     * @param memorial      수정할 메모리얼
     */
    private void validateMemorialUpdate(MemorialUpdateRequestDTO updateRequest, Memorial memorial) {

        // 미래 날짜 확인
        LocalDate today = LocalDate.now();
        if (updateRequest.getBirthDate() != null && updateRequest.getBirthDate().isAfter(today)) {
            throw new APIException(ResponseStatus.BAD_REQUEST_MEMORIAL_BIRTH);
        }

        if (updateRequest.getDeathDate() != null && updateRequest.getDeathDate().isAfter(today)) {
            throw new APIException(ResponseStatus.BAD_REQUEST_MEMORIAL_DEATH);
        }

        // AI 학습 중인 경우 일부 필드 수정 제한
        if (memorial.isAiTrainingInProgress()) {
            if (! updateRequest.getProfileImageUrl().equals(memorial.getProfileImageUrl()) ||
                ! updateRequest.getVoiceFileUrl().equals(memorial.getVoiceFileUrl()) ||
                ! updateRequest.getVideoFileUrl().equals(memorial.getVideoFileUrl()) ||
                ! updateRequest.getUserImageUrl().equals(memorial.getUserImageUrl())) {

                throw new APIException(ResponseStatus.BAD_REQUEST_MEMORIAL_DEATH);
            }
        }
    }

    /**
     * 메모리얼 삭제 가능 여부 검증
     *
     * @param memorial 삭제할 메모리얼
     */
    private void validateMemorialDeletion(Memorial memorial) {
        // 이미 삭제된 메모리얼인지 확인
        if (memorial.isDeleted()) {
            throw new APIException(ResponseStatus.MEMORIAL_ALREADY_DELETED);
        }

        // AI 학습 중인 메모리얼은 삭제 불가
        if (memorial.isAiTrainingInProgress()) {
            throw new APIException(ResponseStatus.CANNOT_DELETE_MEMORIAL_DURING_AI_TRAINING);
        }

        // 가족 구성원이 있는 경우 삭제 불가 (선택사항)
        if (memorial.getFamilyMemberCount() > 0) {
            throw new APIException(ResponseStatus.CANNOT_DELETE_MEMORIAL_WITH_FAMILY_MEMBERS);
        }

        // 저장된 메모리가 있는 경우 삭제 불가 (선택사항)
        if (memorial.getMemoryCount() > 0) {
            throw new APIException(ResponseStatus.CANNOT_DELETE_MEMORIAL_WITH_MEMORIES);
        }
    }

    /**
     * 메모리얼 완전 삭제 가능 여부 검증
     *
     * @param memorial 완전 삭제할 메모리얼
     */
    private void validateMemorialPermanentDeletion(Memorial memorial) {
        // 삭제된 메모리얼만 완전 삭제 가능
        if (! memorial.isDeleted()) {
            throw new APIException(ResponseStatus.CANNOT_PERMANENT_DELETE_MEMORIAL);
        }

        // AI 학습 중인 메모리얼은 완전 삭제 불가
        if (memorial.isAiTrainingInProgress()) {
            throw new APIException(ResponseStatus.CANNOT_DELETE_MEMORIAL_DURING_AI_TRAINING);
        }
    }
}