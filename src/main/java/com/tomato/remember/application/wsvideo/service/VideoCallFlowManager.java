package com.tomato.remember.application.wsvideo.service;

import com.tomato.remember.application.wsvideo.config.MemorialVideoSessionManager;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import com.tomato.remember.application.wsvideo.code.WebSocketMessageType;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 간소화된 영상통화 상태 전환 관리 서비스
 * 9개 상태만 관리하며 각 상태별 단순한 처리만 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCallFlowManager {

    private final MemorialVideoSessionManager sessionManager;
    private final MultiDeviceManager deviceManager;

    private final ConcurrentHashMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 상태 전환 수행 (기본)
     */
    public boolean transitionToState(String sessionKey, VideoCallFlowState newState) {
        return transitionToState(sessionKey, newState, null);
    }

    /**
     * 상태 전환 수행 (추가 데이터 포함)
     */
    public boolean transitionToState(String sessionKey, VideoCallFlowState newState, Map<String, Object> additionalData) {
        // 세션별 락 획득 (최대 5초 대기)
        ReentrantLock sessionLock = sessionLocks.computeIfAbsent(sessionKey, k -> new ReentrantLock());

        try {
            boolean lockAcquired = sessionLock.tryLock(5, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("⏰ 상태 전환 락 획득 타임아웃: {} -> {}", sessionKey, newState);
                return false;
            }

            try {
                return performStateTransition(sessionKey, newState, additionalData);
            } finally {
                sessionLock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 상태 전환 중 인터럽트: {} -> {}", sessionKey, newState);
            return false;
        }
    }

    /**
     * 🔧 실제 상태 전환 로직 (락 보호 하에서 실행)
     */
    private boolean performStateTransition(String sessionKey, VideoCallFlowState newState, Map<String, Object> additionalData) {
        try {
            // 🔧 강제 새로고침으로 최신 상태 확인
            MemorialVideoSession session = sessionManager.getSession(sessionKey, true);
            if (session == null) {
                log.warn("⚠️ 세션을 찾을 수 없음 - 상태 전환 실패: {} -> {}", sessionKey, newState);
                return false;
            }

            VideoCallFlowState previousState = session.getFlowState();

            // 🔧 중복 상태 전환 재확인 (락 내에서)
            if (previousState == newState) {
                log.info("ℹ️ 이미 동일한 상태 - 전환 불필요: {} ({})", sessionKey, newState);
                return true; // 이미 원하는 상태이므로 성공으로 처리
            }

            log.info("🔍 상태 전환 시도 (락 보호): {} - {} -> {}", sessionKey, previousState, newState);

            // 상태 전환 규칙 확인
            if (!previousState.canTransitionTo(newState)) {
                log.warn("⚠️ 잘못된 상태 전환 - 세션: {}, {} -> {} (허용되지 않는 전환)",
                        sessionKey, previousState, newState);
                return false;
            }

            // 상태 전환 시도
            boolean transitionSuccess = session.transitionToState(newState);
            if (!transitionSuccess) {
                log.warn("⚠️ 세션 객체 상태 전환 실패 - 세션: {}, {} -> {}",
                        sessionKey, previousState, newState);
                return false;
            }

            // 🔧 즉시 저장 및 검증
            sessionManager.saveSession(session);

            // 🔧 저장 후 즉시 재조회로 동기화 검증 강화
            MemorialVideoSession verifySession = sessionManager.getSession(sessionKey, true);
            if (verifySession == null) {
                log.error("❌ 상태 저장 후 세션 조회 실패: {}", sessionKey);
                return false;
            }

            if (verifySession.getFlowState() != newState) {
                log.error("❌ 상태 저장 후 검증 실패 - 세션: {}, 저장 시도: {}, 실제: {}",
                         sessionKey, newState, verifySession.getFlowState());

                // 🔧 재시도 로직 (한 번만)
                log.warn("🔄 상태 동기화 재시도: {} -> {}", sessionKey, newState);
                session.setFlowState(newState);
                session.setLastStateChange(java.time.LocalDateTime.now());
                sessionManager.saveSession(session);

                // 재검증
                MemorialVideoSession retryVerifySession = sessionManager.getSession(sessionKey, true);
                if (retryVerifySession == null || retryVerifySession.getFlowState() != newState) {
                    log.error("❌ 재시도 후에도 상태 동기화 실패: {}", sessionKey);
                    return false;
                }
            }

            log.info("🔄 상태 전환 성공 - 세션: {}, {} -> {} (검증 완료)",
                    sessionKey, previousState, newState);

            // 상태 변경 브로드캐스트
            broadcastStateChange(sessionKey, previousState, newState, additionalData);

            // 상태별 특별 처리
            handleStateActions(sessionKey, newState, session);

            return true;

        } catch (Exception e) {
            log.error("❌ 상태 전환 오류 - 세션: {}, 목표 상태: {}", sessionKey, newState, e);
            return false;
        }
    }

    /**
     * 강제 상태 변경 (검증 없이)
     */
    public void forceStateChange(String sessionKey, VideoCallFlowState newState, String reason) {
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                log.warn("⚠️ 강제 상태 변경 실패 - 세션 없음: {}", sessionKey);
                return;
            }

            VideoCallFlowState previousState = session.getFlowState();

            // 강제로 상태 변경 (검증 생략)
            session.setFlowState(newState);
            session.setLastStateChange(java.time.LocalDateTime.now());
            sessionManager.saveSession(session);

            log.warn("⚡ 강제 상태 변경 - 세션: {}, {} -> {}, 사유: {}",
                    sessionKey, previousState, newState, reason);

            // 강제 변경 브로드캐스트
            Map<String, Object> forceData = Map.of(
                "forced", true,
                "reason", reason,
                "previousState", previousState.name()
            );
            broadcastForceStateChange(sessionKey, newState, forceData);

            // 상태별 처리도 수행
            handleStateActions(sessionKey, newState, session);

        } catch (Exception e) {
            log.error("❌ 강제 상태 변경 오류 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 상태 변경 브로드캐스트 (간소화)
     */
    private void broadcastStateChange(String sessionKey, VideoCallFlowState previousState,
                                    VideoCallFlowState newState, Map<String, Object> additionalData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", WebSocketMessageType.STATE_TRANSITION.name());
            message.put("sessionKey", sessionKey);
            message.put("previousState", previousState != null ? previousState.name() : null);
            message.put("newState", newState.name());
            message.put("stateDisplayName", newState.getDisplayName());
            message.put("stateDescription", newState.getDescription());
            message.put("stateIcon", newState.getStateIcon());

            // UI 제어 정보 (간소화)
            message.put("canRecord", newState.canRecord());
            message.put("showLoading", newState.showLoading());
            message.put("allowUserInteraction", newState.allowUserInteraction());
            message.put("isErrorState", newState.isErrorState());
            message.put("timestamp", System.currentTimeMillis());

            if (additionalData != null) {
                message.putAll(additionalData);
            }

            deviceManager.broadcastToAllDevices(sessionKey, message);

            log.debug("📡 상태 브로드캐스트 완료: {} -> {}",
                     previousState != null ? previousState.name() : "null", newState.name());

        } catch (Exception e) {
            log.error("❌ 상태 브로드캐스트 실패: {}", sessionKey, e);
        }
    }

    /**
     * 강제 상태 변경 브로드캐스트
     */
    private void broadcastForceStateChange(String sessionKey, VideoCallFlowState newState, Map<String, Object> forceData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", WebSocketMessageType.FORCE_STATE_CHANGE.name());
            message.put("sessionKey", sessionKey);
            message.put("newState", newState.name());
            message.put("stateDisplayName", newState.getDisplayName());
            message.put("stateDescription", newState.getDescription());
            message.put("stateIcon", newState.getStateIcon());
            message.put("timestamp", System.currentTimeMillis());

            if (forceData != null) {
                message.putAll(forceData);
            }

            deviceManager.broadcastToAllDevices(sessionKey, message);

        } catch (Exception e) {
            log.error("❌ 강제 상태 변경 브로드캐스트 실패 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 상태별 특별 처리 (9개 상태만 처리)
     */
    private void handleStateActions(String sessionKey, VideoCallFlowState newState, MemorialVideoSession session) {
        switch (newState) {
            case INITIALIZING -> handleInitializing(sessionKey, session);
            case PERMISSION_REQUESTING -> handlePermissionRequesting(sessionKey, session);
            case WAITING -> handleWaiting(sessionKey, session);
            case RECORDING -> handleRecording(sessionKey, session);
            case PROCESSING -> handleProcessing(sessionKey, session);
            case RESPONSE_PLAYING -> handleResponsePlaying(sessionKey, session);
            case CALL_ENDING -> handleCallEnding(sessionKey, session);
            case CALL_COMPLETED -> handleCallCompleted(sessionKey, session);
            case ERROR -> handleError(sessionKey, session);
        }
    }

    /**
     * 초기화 상태 처리
     */
    private void handleInitializing(String sessionKey, MemorialVideoSession session) {
        log.debug("⚙️ 초기화 처리: {}", sessionKey);

        // 초기화 완료 메시지
        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.INFO.name(),
            "message", "시스템을 초기화하고 있습니다...",
            "showLoading", true
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);

        // 2초 후 권한 요청 또는 대기 상태로 자동 전환
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
            transitionToState(sessionKey, VideoCallFlowState.WAITING);
        });
    }

    /**
     * 권한 요청 상태 처리
     */
    private void handlePermissionRequesting(String sessionKey, MemorialVideoSession session) {
        log.debug("🔒 권한 요청 처리: {}", sessionKey);

        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.INFO.name(),
            "message", "카메라와 마이크 권한을 확인해주세요",
            "showPermissionModal", true
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);
    }

    /**
     * 대기 상태 처리 (핵심)
     */
    private void handleWaiting(String sessionKey, MemorialVideoSession session) {
        log.info("⏳ 대기 상태 처리: {}", sessionKey);

        // 대기영상 재생 명령
        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.PLAY_WAITING_VIDEO.name(),
            "sessionKey", sessionKey,
            "waitingVideoUrl", session.getWaitingVideoUrl() != null ? session.getWaitingVideoUrl() : "",
            "contactName", session.getContactName(),
            "loop", true,
            "enableRecordButton", true
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);
    }

    /**
     * 녹화 상태 처리 (핵심)
     */
    private void handleRecording(String sessionKey, MemorialVideoSession session) {
        log.info("🔴 녹화 상태 처리: {}", sessionKey);
         Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.START_RECORDING.name(),
            "sessionKey", sessionKey,
            "maxDuration", 10, // 최대 10초
            "allowUserStop", true, // 사용자가 중간에 중지 가능
            "timestamp", System.currentTimeMillis(),
            "message", "녹화를 시작합니다"
        );

         deviceManager.broadcastToAllDevices(sessionKey, message);
        log.debug("🔴 녹화 상태 활성화 완료: {} (클라이언트에서 이미 녹화 시작)", sessionKey);
    }

    /**
     * 처리 상태 처리 (핵심)
     */
    private void handleProcessing(String sessionKey, MemorialVideoSession session) {
        log.info("🤖 처리 상태 처리: {}", sessionKey);

        // 처리 중 메시지
        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.PROCESSING_PROGRESS.name(),
            "sessionKey", sessionKey,
            "message", "AI가 응답을 생성하고 있습니다...",
            "showLoading", false, // 🔧 오버레이 표시하지 않음
                "disableAllButtons", false, // 🔧 버튼도 비활성화하지 않음
                "processingInBackground", true // 🔧 백그라운드 처리 표시
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);
    }

    /**
     * 응답 재생 상태 처리 (핵심)
     */
    private void handleResponsePlaying(String sessionKey, MemorialVideoSession session) {
        log.info("🎬 응답 재생 상태 처리: {}", sessionKey);

        if (session.getResponseVideoUrl() != null && !session.getResponseVideoUrl().isEmpty()) {
            // 응답영상 재생 명령
            Map<String, Object> message = Map.of(
                "type", WebSocketMessageType.PLAY_RESPONSE_VIDEO.name(),
                "sessionKey", sessionKey,
                "responseVideoUrl", session.getResponseVideoUrl(),
                "videoUrl" , session.getResponseVideoUrl(),
                "contactName", session.getContactName(),
                "autoPlayNext", false,
                "disableRecordButton", true
            );
            log.info("📤 응답영상 메시지 전송: {}", message);
            deviceManager.broadcastToAllDevices(sessionKey, message);
        } else {
            log.error("❌ 응답영상 URL 없음 - 오류 상태로 전환: {}", sessionKey);
            transitionToState(sessionKey, VideoCallFlowState.ERROR);
        }
    }

    /**
     * 통화 종료 상태 처리
     */
    private void handleCallEnding(String sessionKey, MemorialVideoSession session) {
        log.info("👋 통화 종료 처리: {}", sessionKey);

        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.INFO.name(),
            "message", "통화를 종료하고 있습니다...",
            "showLoading", true,
            "disableAllButtons", true
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);

        // 2초 후 완료 상태로 전환
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
            transitionToState(sessionKey, VideoCallFlowState.CALL_COMPLETED);
        });
    }

    /**
     * 통화 완료 상태 처리
     */
    private void handleCallCompleted(String sessionKey, MemorialVideoSession session) {
        log.info("✅ 통화 완료 처리: {}", sessionKey);

        // 완료 메시지 + 리다이렉트 정보
        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.SUCCESS.name(),
            "sessionKey", sessionKey,
            "message", "통화가 완료되었습니다",
            "callDuration", session.getAgeInMinutes(),
            "reconnectCount", session.getReconnectCount(),
            "redirectUrl", String.format("/mobile/ws-call/feedback?memberId=%d&memorialId=%d",
                                       session.getCallerId(), session.getMemorialId()),
            "redirectDelay", 3000, // 3초 후 리다이렉트
            "timestamp", System.currentTimeMillis()
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);

        // 3초 후 세션 정리
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
            cleanupCompletedSession(sessionKey);
        });
    }

    /**
     * 오류 상태 처리
     */
    private void handleError(String sessionKey, MemorialVideoSession session) {
        log.error("❌ 오류 상태 처리: {}", sessionKey);

        // 오류 메시지
        Map<String, Object> message = Map.of(
            "type", WebSocketMessageType.ERROR.name(),
            "sessionKey", sessionKey,
            "message", "오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            "recoverable", true,
            "showRetryButton", true
        );
        deviceManager.broadcastToAllDevices(sessionKey, message);

        // 5초 후 대기 상태로 자동 복구
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            MemorialVideoSession currentSession = sessionManager.getSession(sessionKey);
            if (currentSession != null && !currentSession.isExpired()) {
                log.info("🔄 오류 복구: {} -> WAITING", sessionKey);
                transitionToState(sessionKey, VideoCallFlowState.WAITING);
            }
        });
    }

    /**
     * 완료된 세션 정리
     */
    private void cleanupCompletedSession(String sessionKey) {
        try {
            log.info("🧹 완료된 세션 정리 시작: {}", sessionKey);

            deviceManager.cleanupSession(sessionKey);
            sessionManager.deleteSession(sessionKey);

            log.info("✅ 세션 정리 완료: {}", sessionKey);

        } catch (Exception e) {
            log.error("❌ 세션 정리 오류: {}", sessionKey, e);
        }
    }

    public void cleanupSession(String sessionKey) {
        try {
            log.info("🧹 플로우 매니저 세션 정리: {}", sessionKey);

            // 락 제거
            ReentrantLock removedLock = sessionLocks.remove(sessionKey);
            if (removedLock != null) {
                // 혹시 대기 중인 스레드가 있으면 정리
                if (removedLock.hasQueuedThreads()) {
                    log.warn("⚠️ 정리 중인 세션에 대기 중인 락 요청 있음: {}", sessionKey);
                }
                log.debug("🗑️ 세션 락 제거: {}", sessionKey);
            }

        } catch (Exception e) {
            log.error("❌ 플로우 매니저 세션 정리 오류: {}", sessionKey, e);
        }
    }

    /**
     * 현재 상태 조회
     */
    public VideoCallFlowState getCurrentState(String sessionKey) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        return session != null ? session.getFlowState() : null;
    }

    /**
     * 상태 전환 가능 여부 확인
     */
    public boolean canTransition(String sessionKey, VideoCallFlowState targetState) {
        VideoCallFlowState currentState = getCurrentState(sessionKey);
        return currentState != null && currentState.canTransitionTo(targetState);
    }

    /**
     * 다음 가능한 상태들 조회
     */
    public VideoCallFlowState[] getNextPossibleStates(String sessionKey) {
        VideoCallFlowState currentState = getCurrentState(sessionKey);
        return currentState != null ? currentState.getNextPossibleStates() : new VideoCallFlowState[0];
    }
}