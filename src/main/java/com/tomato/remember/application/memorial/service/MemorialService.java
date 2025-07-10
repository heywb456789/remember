package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialUpdateRequestDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.entity.Member;

import java.util.List;

/**
 * 메모리얼 서비스 인터페이스
 */
public interface MemorialService {
    
    /**
     * 사용자별 메모리얼 목록 조회
     * @param member 조회할 사용자
     * @return 메모리얼 목록
     */
    List<Memorial> getMemorialListByMember(Member member);
    
    /**
     * 사용자별 활성 메모리얼 목록 조회
     * @param member 조회할 사용자
     * @return 활성 메모리얼 목록
     */
    List<Memorial> getActiveMemorialListByMember(Member member);
    
    /**
     * 사용자별 메모리얼 개수 조회
     * @param member 조회할 사용자
     * @return 메모리얼 개수
     */
    int getMemorialCountByMember(Member member);
    
    /**
     * 사용자별 활성 메모리얼 개수 조회
     * @param member 조회할 사용자
     * @return 활성 메모리얼 개수
     */
    int getActiveMemorialCountByMember(Member member);

    /**
     * 메모리얼 생성
     * @param createRequest 메모리얼 생성 요청 DTO
     * @param member 생성할 사용자
     * @return 생성된 메모리얼 응답 DTO
     */
    MemorialResponseDTO createMemorial(MemorialCreateRequestDTO createRequest, Member member);

    /**
     * 메모리얼 ID로 조회
     * @param memorialId 메모리얼 ID
     * @param member 조회할 사용자
     * @return 메모리얼 응답 DTO
     */
    MemorialResponseDTO getMemorialById(Long memorialId, Member member);

    /**
     * 메모리얼 수정
     * @param memorialId 메모리얼 ID
     * @param updateRequest 메모리얼 수정 요청 DTO
     * @param member 수정할 사용자
     * @return 수정된 메모리얼 응답 DTO
     */
    MemorialResponseDTO updateMemorial(Long memorialId, MemorialUpdateRequestDTO updateRequest, Member member);

    /**
     * 메모리얼 수정 폼 데이터 조회
     * @param memorialId 메모리얼 ID
     * @param member 조회할 사용자
     * @return 메모리얼 수정 요청 DTO
     */
    MemorialUpdateRequestDTO getMemorialForUpdate(Long memorialId, Member member);

    /**
     * 메모리얼 방문 기록
     * @param memorialId 메모리얼 ID
     * @param member 방문할 사용자
     */
    void recordMemorialVisit(Long memorialId, Member member);

    /**
     * 메모리얼 삭제 (소프트 삭제)
     * @param memorialId 메모리얼 ID
     * @param member 삭제할 사용자
     */
    void deleteMemorial(Long memorialId, Member member);

    /**
     * 메모리얼 상태 변경 (활성화/비활성화)
     * @param memorialId 메모리얼 ID
     * @param member 상태 변경할 사용자
     */
    void toggleMemorialStatus(Long memorialId, Member member);

    /**
     * 메모리얼 활성화
     * @param memorialId 메모리얼 ID
     * @param member 활성화할 사용자
     */
    void activateMemorial(Long memorialId, Member member);

    /**
     * 메모리얼 비활성화
     * @param memorialId 메모리얼 ID
     * @param member 비활성화할 사용자
     */
    void deactivateMemorial(Long memorialId, Member member);

    /**
     * 메모리얼 복구 (삭제 상태에서 활성화)
     * @param memorialId 메모리얼 ID
     * @param member 복구할 사용자
     */
    void restoreMemorial(Long memorialId, Member member);

    /**
     * 메모리얼 완전 삭제 (물리적 삭제) - 관리자 전용
     * @param memorialId 메모리얼 ID
     * @param member 삭제할 사용자 (관리자 권한 필요)
     */
    void permanentlyDeleteMemorial(Long memorialId, Member member);

    /**
     * 사용자별 삭제된 메모리얼 목록 조회
     * @param member 조회할 사용자
     * @return 삭제된 메모리얼 목록
     */
    List<Memorial> getDeletedMemorialListByMember(Member member);

    /**
     * 사용자별 비활성 메모리얼 목록 조회
     * @param member 조회할 사용자
     * @return 비활성 메모리얼 목록
     */
    List<Memorial> getInactiveMemorialListByMember(Member member);
}