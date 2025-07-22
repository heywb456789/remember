package com.tomato.remember.application.videocall.repository;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.videocall.entity.VideoCallSampleReview;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoCallSampleReviewRepository extends JpaRepository<VideoCallSampleReview, Long> {
    
    List<VideoCallSampleReview> findByCallerAndContactKeyOrderByCreatedAtDesc(Member caller, String contactKey);
    
    List<VideoCallSampleReview> findByCallerOrderByCreatedAtDesc(Member caller);
    
    Page<VideoCallSampleReview> findByCaller(Member caller, Pageable pageable);
    
    @Query("SELECT COUNT(v) FROM VideoCallSampleReview v WHERE v.contactKey = :contactKey")
    long countByContactKey(@Param("contactKey") String contactKey);
    
    @Query("SELECT v.contactKey, COUNT(v) FROM VideoCallSampleReview v GROUP BY v.contactKey")
    List<Object[]> countByContactKeyGrouped();
}