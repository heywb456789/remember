package com.tomato.remember.application.wsvideo.service;

import com.tomato.remember.application.wsvideo.config.MemorialVideoSessionManager;
import com.tomato.remember.application.wsvideo.config.MemorialVideoWebSocketHandler;
import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.wsvideo.code.WebSocketMessageType;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 멀티 디바이스 관리 서비스
 * 여러 디바이스가 동일 세션에 접속할 수 있도록 관리
 */
@Slf4j
@Service
public class MultiDeviceManager {

    private final MemorialVideoSessionManager sessionManager;
    private final MemorialVideoWebSocketHandler webSocketHandler;

    // 세션별 등록된 디바이스 목록 관리
    private final Map<String, Set<DeviceInfo>> sessionDevices = new ConcurrentHashMap<>();

    public MultiDeviceManager(
            MemorialVideoSessionManager sessionManager,
            @Lazy MemorialVideoWebSocketHandler webSocketHandler
    ) {
        this.sessionManager = sessionManager;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 디바이스 등록
     */
    public void registerDevice(String sessionKey, String deviceId, DeviceType deviceType) {
        try {
            DeviceInfo deviceInfo = new DeviceInfo(deviceId, deviceType, System.currentTimeMillis());
            
            sessionDevices.computeIfAbsent(sessionKey, k -> ConcurrentHashMap.newKeySet())
                          .add(deviceInfo);

            log.info("📱 디바이스 등록 - 세션: {}, 디바이스: {} ({})", 
                    sessionKey, deviceId, deviceType);

            // 기존 디바이스들에게 새 디바이스 등록 알림
            broadcastDeviceRegistration(sessionKey, deviceId, deviceType);

        } catch (Exception e) {
            log.error("❌ 디바이스 등록 실패 - 세션: {}, 디바이스: {}", sessionKey, deviceId, e);
        }
    }

    /**
     * 디바이스 등록 해제
     */
    public void unregisterDevice(String sessionKey, String deviceId) {
        try {
            Set<DeviceInfo> devices = sessionDevices.get(sessionKey);
            if (devices != null) {
                devices.removeIf(device -> device.getDeviceId().equals(deviceId));
                
                if (devices.isEmpty()) {
                    sessionDevices.remove(sessionKey);
                }

                log.info("📱 디바이스 등록 해제 - 세션: {}, 디바이스: {}", sessionKey, deviceId);

                // 남은 디바이스들에게 연결 해제 알림
                broadcastDeviceDisconnection(sessionKey, deviceId);
            }

        } catch (Exception e) {
            log.error("❌ 디바이스 등록 해제 실패 - 세션: {}, 디바이스: {}", sessionKey, deviceId, e);
        }
    }

    /**
     * 모든 디바이스에 메시지 브로드캐스트
     */
    public void broadcastToAllDevices(String sessionKey, Map<String, Object> message) {
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                log.warn("⚠️ 세션을 찾을 수 없음 - 브로드캐스트 실패: {}", sessionKey);
                return;
            }

            // 현재 연결된 WebSocket으로 메시지 전송
            webSocketHandler.sendMessageToSession(sessionKey, message);

            // 등록된 디바이스 수 로깅
            Set<DeviceInfo> devices = sessionDevices.get(sessionKey);
            int deviceCount = devices != null ? devices.size() : 0;

            log.debug("📡 멀티 디바이스 브로드캐스트 - 세션: {}, 디바이스 수: {}, 메시지: {}", 
                    sessionKey, deviceCount, message.get("type"));

        } catch (Exception e) {
            log.error("❌ 멀티 디바이스 브로드캐스트 실패 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 주 제어 디바이스 설정
     */
    public void setPrimaryDevice(String sessionKey, String deviceId) {
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return;
            }

            // 세션의 주 디바이스 설정
            session.setDeviceId(deviceId);
            session.setPrimaryDevice(true);
            sessionManager.saveSession(session);

            // 우선순위 변경 알림
            Map<String, Object> priorityMessage = Map.of(
                "type", WebSocketMessageType.PRIORITY_CHANGED.name(),
                "sessionKey", sessionKey,
                "primaryDeviceId", deviceId,
                "timestamp", System.currentTimeMillis()
            );

            broadcastToAllDevices(sessionKey, priorityMessage);

            log.info("👑 주 제어 디바이스 변경 - 세션: {}, 디바이스: {}", sessionKey, deviceId);

        } catch (Exception e) {
            log.error("❌ 주 제어 디바이스 설정 실패 - 세션: {}", sessionKey, e);
        }
    }

    /**
     * 디바이스 등록 브로드캐스트
     */
    public void broadcastDeviceRegistration(String sessionKey, String deviceId, DeviceType deviceType) {
        Map<String, Object> registrationMessage = Map.of(
            "type", WebSocketMessageType.DEVICE_REGISTERED.name(),
            "sessionKey", sessionKey,
            "deviceId", deviceId,
            "deviceType", deviceType.name(),
            "deviceDisplayName", deviceType.getDisplayName(),
            "timestamp", System.currentTimeMillis()
        );

        broadcastToAllDevices(sessionKey, registrationMessage);
    }

    /**
     * 디바이스 연결 해제 브로드캐스트
     */
    public void broadcastDeviceDisconnection(String sessionKey, String deviceId) {
        Map<String, Object> disconnectionMessage = Map.of(
            "type", WebSocketMessageType.DEVICE_DISCONNECTED.name(),
            "sessionKey", sessionKey,
            "deviceId", deviceId,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToAllDevices(sessionKey, disconnectionMessage);
    }

    /**
     * 세션의 등록된 디바이스 목록 조회
     */
    public Set<DeviceInfo> getSessionDevices(String sessionKey) {
        return sessionDevices.getOrDefault(sessionKey, Set.of());
    }

    /**
     * 세션의 디바이스 수 조회
     */
    public int getDeviceCount(String sessionKey) {
        Set<DeviceInfo> devices = sessionDevices.get(sessionKey);
        return devices != null ? devices.size() : 0;
    }

    /**
     * 전체 활성 디바이스 통계
     */
    public Map<String, Object> getDeviceStatistics() {
        int totalSessions = sessionDevices.size();
        int totalDevices = sessionDevices.values().stream()
                                        .mapToInt(Set::size)
                                        .sum();

        Map<DeviceType, Long> deviceTypeCount = sessionDevices.values().stream()
            .flatMap(Set::stream)
            .collect(java.util.stream.Collectors.groupingBy(
                DeviceInfo::getDeviceType,
                java.util.stream.Collectors.counting()
            ));

        return Map.of(
            "totalSessions", totalSessions,
            "totalDevices", totalDevices,
            "deviceTypeDistribution", deviceTypeCount,
            "avgDevicesPerSession", totalSessions > 0 ? (double) totalDevices / totalSessions : 0.0
        );
    }

    /**
     * 세션 정리 시 디바이스 목록도 함께 정리
     */
    public void cleanupSession(String sessionKey) {
        Set<DeviceInfo> removed = sessionDevices.remove(sessionKey);
        if (removed != null && !removed.isEmpty()) {
            log.info("🧹 세션 디바이스 목록 정리 - 세션: {}, 디바이스 수: {}", sessionKey, removed.size());
        }
    }

    // ========== Inner Classes ==========

    public static class DeviceInfo {
        private final String deviceId;
        private final DeviceType deviceType;
        private final long registeredAt;

        public DeviceInfo(String deviceId, DeviceType deviceType, long registeredAt) {
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.registeredAt = registeredAt;
        }

        public String getDeviceId() { return deviceId; }
        public DeviceType getDeviceType() { return deviceType; }
        public long getRegisteredAt() { return registeredAt; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            DeviceInfo that = (DeviceInfo) obj;
            return deviceId.equals(that.deviceId);
        }

        @Override
        public int hashCode() {
            return deviceId.hashCode();
        }

        @Override
        public String toString() {
            return String.format("DeviceInfo{id='%s', type=%s, registered=%d}", 
                                deviceId, deviceType, registeredAt);
        }
    }
}