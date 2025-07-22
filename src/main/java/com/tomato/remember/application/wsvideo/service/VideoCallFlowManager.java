package com.tomato.remember.application.wsvideo.service;

import com.tomato.remember.application.wsvideo.config.MemorialVideoSessionManager;
import com.tomato.remember.application.wsvideo.config.MemorialVideoWebSocketHandler;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import com.tomato.remember.application.wsvideo.code.WebSocketMessageType;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

/**
 * 영상통화 상태 전환 관리 서비스
 * 상태 변경 시 WebSocket을 통해 모든 디바이스에 브로드캐스트
 */
@Slf4j
@Service
public class VideoCallFlowManager {

    private final MemorialVideoSessionManager sessionManager;
    private final MemorialVideoWebSocketHandler webSocketHandler;
    private final MultiDeviceManager deviceManager;

    public VideoCallFlowManager(
            MemorialVideoSessionManager sessionManager,
            @Lazy MemorialVideoWebSocketHandler webSocketHandler,
            @Lazy MultiDeviceManager deviceManager
    ) {
        this.sessionManager = sessionManager;
        this.webSocketHandler = webSocketHandler;
        this.deviceManager = deviceManager;
    }

    /**
     * 상태 전환 수행 및 브로드캐스트
     */
    public boolean transitionToState(String sessionKey, VideoCallFlowState newState) {
        return transitionToState(sessionKey, newState, null);
    }

    /**
     * 상태 전환 수행 및 브로드캐스트 (추가 데이터 포함)
     */
    public boolean transitionToState(String sessionKey, VideoCallFlowState newState, Map<String, Object> additionalData) {
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                log.warn("⚠️ 세션을 찾을 수 없음 - 상태 전환 실패: {} -> {}", sessionKey, newState);
                return false;
            }

            VideoCallFlowState previousState = session.getFlowState();

            // 상태 전환 시도
            boolean transitionSuccess = session.transitionToState(newState);
            if (!transitionSuccess) {
                log.warn("⚠️ 잘못된 상태 전환 - 세션: {}, {} -> {}", 
                        sessionKey, previousState, newState);
                return false;
            }

            // 세션 저장
            sessionManager.saveSession(session);

            log.info("🔄 상태 전환 성공 - 세션: {}, {} -> {}", 
                    sessionKey, previousState, newState);

            // 모든 디바이스에 상태 변경 브로드캐스트
            broadcastStateChange(sessionKey, previousState, newState, additionalData);

            // 상태별 특별 처리
            handleStateSpecificActions(sessionKey, newState, session);

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
                return;
            }

            VideoCallFlowState previousState = session.getFlowState();
            session.setFlowState(newState);
            session.setLastStateChange(java.time.LocalDateTime.now());
            sessionManager.saveSession(session);

            log.warn("⚡ 강제 상태 변경 - 세션: {}, {} -> {}, 사유: {}", 
                    sessionKey, previousState, newState, reason);

            // 강제 변경 브로드캐스트
            Map<String, Object> forceChangeData = Map.of(
                "forced", true,
                "reason", reason
            );
            broadcastForceStateChange(sessionKey, previousState, newState, forceChangeData);

        } catch (Exception e) {
            log.error("❌ 강제 상태 변경 오류 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 상태 변경 브로드캐스트
     */
    private void broadcastStateChange(String sessionKey, VideoCallFlowState previousState, 
                                    VideoCallFlowState newState, Map<String, Object> additionalData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", WebSocketMessageType.STATE_TRANSITION.name());
            message.put("sessionKey", sessionKey);
            message.put("previousState", previousState.name());
            message.put("newState", newState.name());
            message.put("stateDisplayName", newState.getDisplayName());
            message.put("stateDescription", newState.getDescription());
            message.put("timestamp", System.currentTimeMillis());
            message.put("canTransitionTo", getNextPossibleStates(newState));

            if (additionalData != null) {
                message.putAll(additionalData);
            }

            // 모든 연결된 디바이스에 브로드캐스트
            deviceManager.broadcastToAllDevices(sessionKey, message);

            log.debug("📡 상태 변경 브로드캐스트 완료 - 세션: {}, 상태: {}", sessionKey, newState);

        } catch (Exception e) {
            log.error("❌ 상태 변경 브로드캐스트 실패 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 강제 상태 변경 브로드캐스트
     */
    private void broadcastForceStateChange(String sessionKey, VideoCallFlowState previousState,
                                         VideoCallFlowState newState, Map<String, Object> additionalData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", WebSocketMessageType.FORCE_STATE_CHANGE.name());
            message.put("sessionKey", sessionKey);
            message.put("previousState", previousState.name());
            message.put("newState", newState.name());
            message.put("stateDisplayName", newState.getDisplayName());
            message.put("stateDescription", newState.getDescription());
            message.put("timestamp", System.currentTimeMillis());

            if (additionalData != null) {
                message.putAll(additionalData);
            }

            deviceManager.broadcastToAllDevices(sessionKey, message);

        } catch (Exception e) {
            log.error("❌ 강제 상태 변경 브로드캐스트 실패 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 상태별 특별 처리
     */
    private void handleStateSpecificActions(String sessionKey, VideoCallFlowState newState, MemorialVideoSession session) {
        switch (newState) {
            case WAITING_PLAYING -> {
                // 대기영상 재생 지시
                Map<String, Object> playMessage = Map.of(
                    "type", WebSocketMessageType.PLAY_WAITING_VIDEO.name(),
                    "sessionKey", sessionKey,
                    "waitingVideoUrl", session.getWaitingVideoUrl(),
                    "loop", true
                );
                deviceManager.broadcastToAllDevices(sessionKey, playMessage);
            }
            
            case RECORDING_COUNTDOWN -> {
                // 녹화 카운트다운 시작 지시
                Map<String, Object> countdownMessage = Map.of(
                    "type", WebSocketMessageType.START_RECORDING.name(),
                    "sessionKey", sessionKey,
                    "countdown", 3,
                    "maxDuration", 30
                );
                deviceManager.broadcastToAllDevices(sessionKey, countdownMessage);
            }
            
            case PROCESSING_AI -> {
                // 처리 진행 상황 알림 시작
                startProcessingProgress(sessionKey);
            }
            
            case RESPONSE_PLAYING -> {
                // 응답영상 재생 지시
                Map<String, Object> responseMessage = Map.of(
                    "type", WebSocketMessageType.PLAY_RESPONSE_VIDEO.name(),
                    "sessionKey", sessionKey,
                    "responseVideoUrl", session.getResponseVideoUrl(),
                    "autoReturn", true
                );
                deviceManager.broadcastToAllDevices(sessionKey, responseMessage);
            }
            
            case CALL_COMPLETED -> {
                // 통화 완료 처리
                handleCallCompletion(sessionKey, session);
            }
        }
    }

    /**
     * 처리 진행 상황 알림 시작
     */
    private void startProcessingProgress(String sessionKey) {
        // 실제로는 외부 API의 진행률을 받아와야 하지만, 
        // 여기서는 예상 시간 기반으로 가상의 진행률 전송
        new Thread(() -> {
            try {
                for (int progress = 0; progress <= 100; progress += 10) {
                    Thread.sleep(2000); // 2초마다

                    MemorialVideoSession session = sessionManager.getSession(sessionKey);
                    if (session == null || session.getFlowState() != VideoCallFlowState.PROCESSING_AI) {
                        break; // 세션 없거나 상태 변경됨
                    }

                    Map<String, Object> progressMessage = Map.of(
                        "type", WebSocketMessageType.PROCESSING_PROGRESS.name(),
                        "sessionKey", sessionKey,
                        "progress", progress,
                        "message", getProgressMessage(progress)
                    );
                    deviceManager.broadcastToAllDevices(sessionKey, progressMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 진행률별 메시지
     */
    private String getProgressMessage(int progress) {
        return switch (progress) {
            case 0 -> "AI 분석을 시작합니다...";
            case 20 -> "음성을 분석하고 있습니다...";
            case 40 -> "응답을 생성하고 있습니다...";
            case 60 -> "영상을 렌더링하고 있습니다...";
            case 80 -> "마무리 작업 중입니다...";
            case 100 -> "처리가 완료되었습니다!";
            default -> "처리 중... (" + progress + "%)";
        };
    }

    /**
     * 통화 완료 처리
     */
    private void handleCallCompletion(String sessionKey, MemorialVideoSession session) {
        log.info("🏁 통화 완료 처리 - 세션: {}, 총 소요시간: {}분", 
                sessionKey, session.getAgeInMinutes());

        // 완료 알림 브로드캐스트
        Map<String, Object> completionMessage = Map.of(
            "type", WebSocketMessageType.INFO.name(),
            "sessionKey", sessionKey,
            "message", "통화가 완료되었습니다",
            "callDuration", session.getAgeInMinutes(),
            "reconnectCount", session.getReconnectCount()
        );
        deviceManager.broadcastToAllDevices(sessionKey, completionMessage);
    }

    /**
     * 다음 가능한 상태들 반환
     */
    private String[] getNextPossibleStates(VideoCallFlowState currentState) {
        if (currentState == null) {
            return new String[0];
        }

        return Arrays.stream(VideoCallFlowState.values())
            .filter(state -> currentState.canTransitionTo(state))
            .map(Enum::name)
            .toArray(String[]::new);
    }

    /**
     * 상태 전환 검증
     */
    public boolean validateStateTransition(VideoCallFlowState from, VideoCallFlowState to) {
        return from.canTransitionTo(to);
    }

    /**
     * 현재 상태 조회
     */
    public VideoCallFlowState getCurrentState(String sessionKey) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        return session != null ? session.getFlowState() : null;
    }
}