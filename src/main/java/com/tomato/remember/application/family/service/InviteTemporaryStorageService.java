package com.tomato.remember.application.family.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomato.remember.application.family.dto.InviteTemporaryInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 초대 정보 임시 저장 서비스
 * - 로그인/회원가입 시 초대 정보 임시 저장
 * - Redis 대신 메모리 기반 임시 저장소 사용
 * - 실제 운영에서는 Redis로 대체 권장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteTemporaryStorageService {

    private final ObjectMapper objectMapper;
    
    // 임시 저장소 (실제 환경에서는 Redis 사용 권장)
    private final Map<String, InviteTemporaryInfo> temporaryStorage = new ConcurrentHashMap<>();
    
    // 기본 만료 시간 (24시간)
    private static final long DEFAULT_EXPIRY_HOURS = 24;

    /**
     * 초대 정보 저장
     */
    public void storeInviteInfo(String inviteCode, InviteTemporaryInfo inviteInfo) {
        log.info("초대 정보 임시 저장 - 초대 코드: {}, 메모리얼: {}", 
                inviteCode, inviteInfo.getMemorialId());
        
        try {
            // 저장 시간 설정
            InviteTemporaryInfo infoWithStoredTime = InviteTemporaryInfo.withCurrentStoredTime(inviteInfo);
            
            // 임시 저장소에 저장
            temporaryStorage.put(inviteCode, infoWithStoredTime);
            
            log.info("초대 정보 임시 저장 완료 - 초대 코드: {}, 요약: {}", 
                    inviteCode, infoWithStoredTime.getSummary());
            
        } catch (Exception e) {
            log.error("초대 정보 임시 저장 실패 - 초대 코드: {}", inviteCode, e);
            throw new RuntimeException("초대 정보 저장에 실패했습니다.", e);
        }
    }

    /**
     * 초대 정보 조회
     */
    public InviteTemporaryInfo getInviteInfo(String inviteCode) {
        log.info("초대 정보 조회 - 초대 코드: {}", inviteCode);
        
        try {
            InviteTemporaryInfo inviteInfo = temporaryStorage.get(inviteCode);
            
            if (inviteInfo == null) {
                log.warn("초대 정보 없음 - 초대 코드: {}", inviteCode);
                return null;
            }
            
            // 만료 확인
            if (inviteInfo.isStorageExpired()) {
                log.warn("초대 정보 만료 - 초대 코드: {}, 저장 시간: {}", 
                        inviteCode, inviteInfo.getStoredAt());
                
                // 만료된 정보 제거
                temporaryStorage.remove(inviteCode);
                return null;
            }
            
            log.info("초대 정보 조회 성공 - 초대 코드: {}, 요약: {}", 
                    inviteCode, inviteInfo.getSummary());
            
            return inviteInfo;
            
        } catch (Exception e) {
            log.error("초대 정보 조회 실패 - 초대 코드: {}", inviteCode, e);
            return null;
        }
    }

    /**
     * 초대 정보 삭제
     */
    public void removeInviteInfo(String inviteCode) {
        log.info("초대 정보 삭제 - 초대 코드: {}", inviteCode);
        
        try {
            InviteTemporaryInfo removed = temporaryStorage.remove(inviteCode);
            
            if (removed != null) {
                log.info("초대 정보 삭제 완료 - 초대 코드: {}, 요약: {}", 
                        inviteCode, removed.getSummary());
            } else {
                log.warn("삭제할 초대 정보 없음 - 초대 코드: {}", inviteCode);
            }
            
        } catch (Exception e) {
            log.error("초대 정보 삭제 실패 - 초대 코드: {}", inviteCode, e);
        }
    }

    /**
     * 초대 정보 처리 완료 표시
     */
    public void markAsProcessed(String inviteCode) {
        log.info("초대 정보 처리 완료 표시 - 초대 코드: {}", inviteCode);
        
        try {
            InviteTemporaryInfo inviteInfo = temporaryStorage.get(inviteCode);
            
            if (inviteInfo != null) {
                InviteTemporaryInfo processedInfo = inviteInfo.markAsProcessed();
                temporaryStorage.put(inviteCode, processedInfo);
                
                log.info("초대 정보 처리 완료 표시 완료 - 초대 코드: {}", inviteCode);
            } else {
                log.warn("처리 완료 표시할 초대 정보 없음 - 초대 코드: {}", inviteCode);
            }
            
        } catch (Exception e) {
            log.error("초대 정보 처리 완료 표시 실패 - 초대 코드: {}", inviteCode, e);
        }
    }

    /**
     * 초대 정보 존재 여부 확인
     */
    public boolean hasInviteInfo(String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return false;
        }
        
        InviteTemporaryInfo inviteInfo = temporaryStorage.get(inviteCode);
        
        if (inviteInfo == null) {
            return false;
        }
        
        // 만료 확인
        if (inviteInfo.isStorageExpired()) {
            temporaryStorage.remove(inviteCode);
            return false;
        }
        
        return true;
    }

    /**
     * 초대 정보 유효성 확인
     */
    public boolean isInviteInfoValid(String inviteCode) {
        InviteTemporaryInfo inviteInfo = getInviteInfo(inviteCode);
        return inviteInfo != null && inviteInfo.isValid();
    }

    /**
     * 만료된 초대 정보 정리 (스케줄러에서 호출)
     */
    public void cleanupExpiredInviteInfo() {
        log.info("만료된 초대 정보 정리 시작");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int removedCount = 0;
            
            // 만료된 정보들을 찾아서 제거
            temporaryStorage.entrySet().removeIf(entry -> {
                InviteTemporaryInfo info = entry.getValue();
                boolean isExpired = info.isStorageExpired();
                
                if (isExpired) {
                    log.debug("만료된 초대 정보 제거 - 초대 코드: {}, 저장 시간: {}", 
                            entry.getKey(), info.getStoredAt());
                }
                
                return isExpired;
            });
            
            log.info("만료된 초대 정보 정리 완료 - 제거된 개수: {}, 남은 개수: {}", 
                    removedCount, temporaryStorage.size());
            
        } catch (Exception e) {
            log.error("만료된 초대 정보 정리 실패", e);
        }
    }

    /**
     * 전체 초대 정보 통계 조회
     */
    public Map<String, Object> getStorageStatistics() {
        try {
            int totalCount = temporaryStorage.size();
            int validCount = 0;
            int expiredCount = 0;
            int processedCount = 0;
            
            LocalDateTime now = LocalDateTime.now();
            
            for (InviteTemporaryInfo info : temporaryStorage.values()) {
                if (info.isProcessed()) {
                    processedCount++;
                } else if (info.isStorageExpired()) {
                    expiredCount++;
                } else {
                    validCount++;
                }
            }
            
            return Map.of(
                    "totalCount", totalCount,
                    "validCount", validCount,
                    "expiredCount", expiredCount,
                    "processedCount", processedCount,
                    "lastCleanupTime", now
            );
            
        } catch (Exception e) {
            log.error("초대 정보 통계 조회 실패", e);
            return Map.of("error", "통계 조회 실패");
        }
    }

    /**
     * 특정 메모리얼의 초대 정보 조회
     */
    public long countInvitesByMemorial(Long memorialId) {
        try {
            return temporaryStorage.values().stream()
                    .filter(info -> info.getMemorialId().equals(memorialId))
                    .filter(info -> !info.isStorageExpired())
                    .count();
                    
        } catch (Exception e) {
            log.error("메모리얼별 초대 정보 개수 조회 실패 - 메모리얼: {}", memorialId, e);
            return 0;
        }
    }

    /**
     * 특정 초대자의 초대 정보 조회
     */
    public long countInvitesByInviter(Long inviterId) {
        try {
            return temporaryStorage.values().stream()
                    .filter(info -> info.getInviterId().equals(inviterId))
                    .filter(info -> !info.isStorageExpired())
                    .count();
                    
        } catch (Exception e) {
            log.error("초대자별 초대 정보 개수 조회 실패 - 초대자: {}", inviterId, e);
            return 0;
        }
    }

    /**
     * 초대 정보 JSON 직렬화
     */
    public String serializeInviteInfo(InviteTemporaryInfo inviteInfo) {
        try {
            return objectMapper.writeValueAsString(inviteInfo);
        } catch (Exception e) {
            log.error("초대 정보 직렬화 실패", e);
            return null;
        }
    }

    /**
     * 초대 정보 JSON 역직렬화
     */
    public InviteTemporaryInfo deserializeInviteInfo(String json) {
        try {
            return objectMapper.readValue(json, InviteTemporaryInfo.class);
        } catch (Exception e) {
            log.error("초대 정보 역직렬화 실패", e);
            return null;
        }
    }
}