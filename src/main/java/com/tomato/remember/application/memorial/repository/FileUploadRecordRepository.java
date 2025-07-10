package com.tomato.remember.application.memorial.repository;

import com.tomato.remember.application.memorial.entity.FileUploadRecord;
import com.tomato.remember.application.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 파일 업로드 기록 Repository
 */
@Repository
public interface FileUploadRecordRepository extends JpaRepository<FileUploadRecord, Long> {

    // 기본 조회
    Optional<FileUploadRecord> findByUploadId(String uploadId);
    Optional<FileUploadRecord> findByFileUrl(String fileUrl);
    Optional<FileUploadRecord> findByFileUrlAndIsDeletedFalse(String fileUrl);
    
    // 업로더별 조회
    List<FileUploadRecord> findByUploader(Member uploader);
    List<FileUploadRecord> findByUploaderAndIsDeletedFalse(Member uploader);
    Page<FileUploadRecord> findByUploaderAndIsDeletedFalse(Member uploader, Pageable pageable);
    
    // 파일 타입별 조회
    List<FileUploadRecord> findByFileType(String fileType);
    List<FileUploadRecord> findByFileTypeAndIsDeletedFalse(String fileType);
    List<FileUploadRecord> findByUploaderAndFileType(Member uploader, String fileType);
    List<FileUploadRecord> findByUploaderAndFileTypeAndIsDeletedFalse(Member uploader, String fileType);
    
    // 삭제 상태별 조회
    List<FileUploadRecord> findByIsDeleted(Boolean isDeleted);
    List<FileUploadRecord> findByUploaderAndIsDeleted(Member uploader, Boolean isDeleted);
    
    // 날짜별 조회
    List<FileUploadRecord> findByUploadedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<FileUploadRecord> findByUploadedAtBetweenAndIsDeletedFalse(LocalDateTime startDate, LocalDateTime endDate);
    
    // 통계 쿼리
    @Query("SELECT COUNT(f) FROM FileUploadRecord f WHERE f.uploader = :uploader AND f.isDeleted = false")
    long countActiveFilesByUploader(@Param("uploader") Member uploader);
    
    @Query("SELECT COUNT(f) FROM FileUploadRecord f WHERE f.uploader = :uploader AND f.fileType = :fileType AND f.isDeleted = false")
    long countActiveFilesByUploaderAndType(@Param("uploader") Member uploader, @Param("fileType") String fileType);
    
    @Query("SELECT SUM(f.fileSize) FROM FileUploadRecord f WHERE f.uploader = :uploader AND f.isDeleted = false")
    Long getTotalFileSizeByUploader(@Param("uploader") Member uploader);
    
    @Query("SELECT f.fileType, COUNT(f) FROM FileUploadRecord f WHERE f.uploader = :uploader AND f.isDeleted = false GROUP BY f.fileType")
    List<Object[]> getFileCountByTypeForUploader(@Param("uploader") Member uploader);
    
    // 정리용 쿼리
    @Query("SELECT f FROM FileUploadRecord f WHERE f.isDeleted = true AND f.deletedAt < :beforeDate")
    List<FileUploadRecord> findDeletedFilesBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    @Query("SELECT f FROM FileUploadRecord f WHERE f.uploadedAt < :beforeDate AND f.isDeleted = false")
    List<FileUploadRecord> findOldActiveFiles(@Param("beforeDate") LocalDateTime beforeDate);
    
    // 파일 URL 중복 확인
    boolean existsByFileUrlAndIsDeletedFalse(String fileUrl);
    
    // 업로드 ID로 활성 파일 존재 확인
    boolean existsByUploadIdAndIsDeletedFalse(String uploadId);
}