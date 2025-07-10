package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.*;
import com.tomato.remember.application.member.entity.Member;
import java.util.List;

/**
 * 메모리얼 서비스 인터페이스
 */
public interface MemorialService {

    // ===== 새로운 단계별 메모리얼 생성 =====

    /**
     * 1단계: 메모리얼 임시 생성 (DRAFT 상태)
     * @param request 1단계 요청 데이터
     * @param member 사용자
     * @return 생성된 메모리얼 응답
     */
    MemorialCreateResponseDTO createMemorialStep1(MemorialStep1RequestDTO request, Member member);

    /**
     * 2단계: 고인 정보 업데이트
     * @param memorialId 메모리얼 ID
     * @param request 2단계 요청 데이터
     * @param member 사용자
     * @return 업데이트된 메모리얼 응답
     */
    MemorialResponseDTO updateMemorialStep2(Long memorialId, MemorialStep2RequestDTO request, Member member);

    /**
     * 3단계: 미디어 파일 정보 업데이트
     * @param memorialId 메모리얼 ID
     * @param fileType 파일 타입
     * @param uploadedFiles 업로드된 파일 정보
     * @param member 사용자
     */
    void updateMemorialFiles(Long memorialId, String fileType, List<FileUploadResponseDTO> uploadedFiles, Member member);

    /**
     * 메모리얼 완료 처리 (DRAFT → ACTIVE)
     * @param memorialId 메모리얼 ID
     * @param member 사용자
     * @return 완료된 메모리얼 응답
     */
    MemorialResponseDTO completeMemorial(Long memorialId, Member member);

    // ===== 기본 CRUD 메서드 =====

    /**
     * 메모리얼 상세 조회
     * @param memorialId 메모리얼 ID
     * @param member 조회할 사용자
     * @return 메모리얼 응답 DTO
     */
    MemorialResponseDTO getMemorialById(Long memorialId, Member member);

    /**
     * 사용자별 활성 메모리얼 목록 조회
     * @param member 조회할 사용자
     * @return 활성 메모리얼 목록
     */
    List<MemorialResponseDTO> getActiveMemorialListByMember(Member member);

    /**
     * 메모리얼 삭제 (소프트 삭제)
     * @param memorialId 메모리얼 ID
     * @param member 삭제할 사용자
     */
    void deleteMemorial(Long memorialId, Member member);

    /**
     * 메모리얼 방문 기록
     * @param memorialId 메모리얼 ID
     * @param member 방문할 사용자
     */
    void recordMemorialVisit(Long memorialId, Member member);

    // ===== 유틸리티 메서드 =====

    /**
     * 메모리얼 접근 권한 검증
     * @param memorialId 메모리얼 ID
     * @param member 사용자
     */
    void validateMemorialAccess(Long memorialId, Member member);

    /**
     * 사용자별 메모리얼 개수 조회
     * @param member 조회할 사용자
     * @return 메모리얼 개수
     */
    int getMemorialCountByMember(Member member);
}