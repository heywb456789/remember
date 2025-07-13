package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialCreateResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialListResponseDTO;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.common.dto.ListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* 메모리얼 서비스 인터페이스
*/
public interface MemorialService {

   /**
    * 메모리얼 생성
    *
    * @param memorialData 메모리얼 기본 정보
    * @param profileImages 프로필 이미지 파일들 (5장 필수)
    * @param voiceFiles 음성 파일들 (3개 필수)
    * @param videoFile 영상 파일 (1개 필수)
    * @param member 현재 로그인된 사용자
    * @return 메모리얼 생성 결과
    */
   MemorialCreateResponseDTO createMemorial(
           MemorialCreateRequestDTO memorialData,
           List<MultipartFile> profileImages,
           List<MultipartFile> voiceFiles,
           MultipartFile videoFile,
           Member member
   );

   /**
    * 사용자의 메모리얼 목록 조회
    *
    * @param member 현재 로그인된 사용자
    * @param pageable 페이징 정보
    * @return 메모리얼 목록
    */
   ListDTO<MemorialListResponseDTO> getMyMemorials(Member member, Pageable pageable);

   /**
    * 메모리얼 상세 조회
    *
    * @param memorialId 메모리얼 ID
    * @param member 현재 로그인된 사용자
    * @return 메모리얼 상세 정보
    */
   MemorialListResponseDTO getMemorial(Long memorialId, Member member);

   /**
    * 메모리얼 방문 기록
    *
    * @param memorialId 메모리얼 ID
    * @param member 현재 로그인된 사용자
    */
   void recordVisit(Long memorialId, Member member);

   List<MemorialListResponseDTO> getMyMemorialsForApi(Member member);

   Memorial findById(Long memorialId);

   List<Memorial> findByOwner(Member currentUser);
}