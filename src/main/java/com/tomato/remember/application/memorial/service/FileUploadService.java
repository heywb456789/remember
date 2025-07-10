package com.tomato.remember.application.memorial.service;

import com.tomato.remember.application.memorial.dto.FileUploadResponseDTO;
import com.tomato.remember.application.member.entity.Member;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 파일 업로드 서비스 인터페이스
 */
public interface FileUploadService {

    /**
     * 단일 파일 업로드
     *
     * @param file 업로드할 파일
     * @param fileType 파일 타입 (profileImages, voiceFiles, videoFile, userImage)
     * @param member 업로드하는 사용자
     * @return 업로드 결과 정보
     */
    FileUploadResponseDTO uploadFile(MultipartFile file, String fileType, Member member);

    /**
     * 다중 파일 업로드
     *
     * @param files 업로드할 파일들
     * @param fileType 파일 타입
     * @param member 업로드하는 사용자
     * @return 업로드 결과 정보 목록
     */
    List<FileUploadResponseDTO> uploadFiles(List<MultipartFile> files, String fileType, Member member);

    /**
     * 파일 삭제
     *
     * @param fileUrl 삭제할 파일 URL
     * @param member 삭제하는 사용자
     */
    void deleteFile(String fileUrl, Member member);

    /**
     * 파일 존재 여부 확인
     *
     * @param fileUrl 확인할 파일 URL
     * @return 파일 존재 여부
     */
    boolean existsFile(String fileUrl);

    /**
     * 파일 크기 확인
     *
     * @param fileUrl 확인할 파일 URL
     * @return 파일 크기 (bytes)
     */
    long getFileSize(String fileUrl);
}