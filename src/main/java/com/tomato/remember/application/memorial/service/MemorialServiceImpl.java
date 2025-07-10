package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.*;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 메모리얼 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemorialServiceImpl implements MemorialService {

    private final MemorialRepository memorialRepository;

    // ===== 새로운 단계별 메모리얼 생성 =====

    /**
     * 1단계: 메모리얼 임시 생성 (DRAFT 상태)
     */
    @Override
    @Transactional
    public MemorialCreateResponseDTO createMemorialStep1(MemorialStep1RequestDTO request, Member member) {
        log.info("Creating memorial step1 for member: {} (ID: {}), name: {}",
                member.getName(), member.getId(), request.getName());

        // Memorial 엔티티 생성 (DRAFT 상태)
        Memorial memorial = Memorial.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .owner(member)
                .status(MemorialStatus.DRAFT)
                .totalVisits(0)
                .build();

        // 저장
        Memorial savedMemorial = memorialRepository.save(memorial);

        log.info("Memorial step1 created successfully: ID={}, Name={}, Status={}",
                savedMemorial.getId(), savedMemorial.getName(), savedMemorial.getStatus());

        return MemorialCreateResponseDTO.of(savedMemorial.getId(), savedMemorial.getName());
    }

    /**
     * 2단계: 고인 정보 업데이트
     */
    @Override
    @Transactional
    public MemorialResponseDTO updateMemorialStep2(Long memorialId, MemorialStep2RequestDTO request, Member member) {
        log.info("Updating memorial step2 for memorialId: {} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        Memorial memorial = findMemorialByIdAndValidateAccess(memorialId, member);

        // 고인 정보 업데이트
        memorial.updateDeceasedInfo(
                request.getGender(),
                request.getRelationship(),
                request.getBirthDate(),
                request.getDeathDate(),
                request.getBirthPlace(),
                request.getResidence(),
                request.getOccupation(),
                request.getContactNumber(),
                request.getEmail(),
                request.getLifeStory(),
                request.getHobbies(),
                request.getPersonality(),
                request.getSpecialMemories(),
                request.getSpeechHabits(),
                request.getFavoriteFood(),
                request.getFavoritePlace(),
                request.getFavoriteMusic()
        );

        Memorial savedMemorial = memorialRepository.save(memorial);

        log.info("Memorial step2 updated successfully: memorialId={}", memorialId);

        return savedMemorial.toResponseDTO();
    }

    /**
     * 3단계: 미디어 파일 정보 업데이트
     */
    @Override
    @Transactional
    public void updateMemorialFiles(Long memorialId, String fileType, List<FileUploadResponseDTO> uploadedFiles, Member member) {
        log.info("Updating memorial files for memorialId: {} by member: {} (ID: {}), fileType: {}, fileCount: {}",
                memorialId, member.getName(), member.getId(), fileType, uploadedFiles.size());

        Memorial memorial = findMemorialByIdAndValidateAccess(memorialId, member);

        // 파일 타입별 처리
        switch (fileType.toLowerCase()) {
            case "profileimages":
                List<String> profileImageUrls = uploadedFiles.stream()
                        .map(FileUploadResponseDTO::getFileUrl)
                        .collect(Collectors.toList());
                memorial.updateProfileImages(profileImageUrls);
                break;

            case "voicefiles":
                List<String> voiceFileUrls = uploadedFiles.stream()
                        .map(FileUploadResponseDTO::getFileUrl)
                        .collect(Collectors.toList());
                memorial.updateVoiceFiles(voiceFileUrls);
                break;

            case "videofile":
                if (!uploadedFiles.isEmpty()) {
                    memorial.updateVideoFile(uploadedFiles.get(0).getFileUrl());
                }
                break;

            case "userimage":
                if (!uploadedFiles.isEmpty()) {
                    memorial.updateUserImage(uploadedFiles.get(0).getFileUrl());
                }
                break;

            default:
                log.warn("Unknown file type: {}", fileType);
                throw new APIException(ResponseStatus.INVALID_FILE_TYPE);
        }

        memorialRepository.save(memorial);

        log.info("Memorial files updated successfully: memorialId={}, fileType={}", memorialId, fileType);
    }

    /**
     * 메모리얼 완료 처리 (DRAFT → ACTIVE)
     */
    @Override
    @Transactional
    public MemorialResponseDTO completeMemorial(Long memorialId, Member member) {
        log.info("Completing memorial: memorialId={} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        Memorial memorial = findMemorialByIdAndValidateAccess(memorialId, member);

        // DRAFT 상태 확인
        if (memorial.getStatus() != MemorialStatus.DRAFT) {
            log.error("Memorial is not in DRAFT status: memorialId={}, currentStatus={}",
                    memorialId, memorial.getStatus());
            throw new APIException(ResponseStatus.MEMORIAL_NOT_DRAFT);
        }

        // 필수 정보 확인
        validateMemorialCompleteness(memorial);

        // 상태를 ACTIVE로 변경
        memorial.setStatus(MemorialStatus.ACTIVE);
        Memorial savedMemorial = memorialRepository.save(memorial);

        log.info("Memorial completed successfully: memorialId={}", memorialId);

        return savedMemorial.toResponseDTO();
    }

    // ===== 기본 CRUD 메서드 =====

    /**
     * 메모리얼 상세 조회
     */
    @Override
    public MemorialResponseDTO getMemorialById(Long memorialId, Member member) {
        log.debug("Getting memorial by ID: {} for member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        Memorial memorial = findMemorialByIdAndValidateAccess(memorialId, member);

        log.debug("Memorial found: ID={}, Name={}", memorial.getId(), memorial.getName());

        return memorial.toResponseDTO();
    }

    /**
     * 사용자별 활성 메모리얼 목록 조회
     */
    @Override
    public List<MemorialResponseDTO> getActiveMemorialListByMember(Member member) {
        log.debug("Getting active memorial list for member: {} (ID: {})",
                member.getName(), member.getId());

        List<Memorial> activeMemorials = memorialRepository.findByOwnerAndStatus(member, MemorialStatus.ACTIVE);

        log.debug("Found {} active memorials for member: {} (ID: {})",
                activeMemorials.size(), member.getName(), member.getId());

        return activeMemorials.stream()
                .map(Memorial::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 메모리얼 삭제 (소프트 삭제)
     */
    @Override
    @Transactional
    public void deleteMemorial(Long memorialId, Member member) {
        log.info("Deleting memorial: memorialId={} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        Memorial memorial = findMemorialByIdAndValidateOwnership(memorialId, member);

        // 삭제 상태로 변경
        memorial.setStatus(MemorialStatus.DELETED);
        memorialRepository.save(memorial);

        log.info("Memorial deleted successfully: memorialId={}", memorialId);
    }

    /**
     * 메모리얼 방문 기록
     */
    @Override
    @Transactional
    public void recordMemorialVisit(Long memorialId, Member member) {
        log.debug("Recording memorial visit: memorialId={} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        Memorial memorial = findMemorialByIdAndValidateAccess(memorialId, member);

        // 방문 기록
        memorial.recordVisit();
        memorialRepository.save(memorial);

        log.debug("Memorial visit recorded: memorialId={}, totalVisits={}",
                memorialId, memorial.getTotalVisits());
    }

    // ===== 유틸리티 메서드 =====

    /**
     * 메모리얼 접근 권한 검증
     */
    @Override
    public void validateMemorialAccess(Long memorialId, Member member) {
        findMemorialByIdAndValidateAccess(memorialId, member);
    }

    /**
     * 사용자별 메모리얼 개수 조회
     */
    @Override
    public int getMemorialCountByMember(Member member) {
        log.debug("Getting memorial count for member: {} (ID: {})",
                member.getName(), member.getId());

        long count = memorialRepository.countByOwnerAndStatus(member, MemorialStatus.ACTIVE);

        log.debug("Memorial count for member: {} (ID: {}) = {}",
                member.getName(), member.getId(), count);

        return (int) count;
    }

    // ===== 내부 헬퍼 메서드 =====

    /**
     * 메모리얼 조회 및 접근 권한 검증
     */
    private Memorial findMemorialByIdAndValidateAccess(Long memorialId, Member member) {
        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
                .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 접근 권한 확인
        if (!memorial.canBeViewedBy(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_MEMORIAL);
        }

        return memorial;
    }

    /**
     * 메모리얼 조회 및 소유권 검증
     */
    private Memorial findMemorialByIdAndValidateOwnership(Long memorialId, Member member) {
        Memorial memorial = memorialRepository.findByIdAndStatusNot(memorialId, MemorialStatus.DELETED)
                .orElseThrow(() -> new APIException(ResponseStatus.CANNOT_FIND_MEMORIAL));

        // 소유권 확인
        if (!memorial.getOwner().equals(member)) {
            throw new APIException(ResponseStatus.CANNOT_ACCESS_MEMORIAL);
        }

        return memorial;
    }

    /**
     * 메모리얼 완성도 검증
     */
    private void validateMemorialCompleteness(Memorial memorial) {
        if (memorial.getName() == null || memorial.getName().trim().isEmpty()) {
            throw new APIException(ResponseStatus.MEMORIAL_INCOMPLETE_NAME);
        }

        if (memorial.getGender() == null) {
            throw new APIException(ResponseStatus.MEMORIAL_INCOMPLETE_GENDER);
        }

        if (memorial.getRelationship() == null) {
            throw new APIException(ResponseStatus.MEMORIAL_INCOMPLETE_RELATIONSHIP);
        }

        // 추가 검증 로직을 여기에 추가할 수 있습니다
        log.debug("Memorial completeness validated successfully: memorialId={}", memorial.getId());
    }
}