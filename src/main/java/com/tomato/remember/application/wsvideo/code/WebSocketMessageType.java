package com.tomato.remember.application.wsvideo.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 메시지 타입 정의 - 인증 관련 타입 추가
 * 클라이언트 ↔ 서버 간 통신에 사용
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketMessageType {

    // ========== 클라이언트 → 서버 ==========

    // === 연결 관리 ===
    CONNECT("연결 요청", "클라이언트가 세션 연결을 요청"),
    DISCONNECT("연결 해제", "클라이언트가 연결 해제를 요청"),
    HEARTBEAT_RESPONSE("하트비트 응답", "서버 하트비트에 대한 클라이언트 응답"),

    // === 인증 관련 (새로 추가) ===
    TOKEN_REFRESH("토큰 갱신", "클라이언트에서 새로운 토큰으로 갱신 요청"),
    AUTHENTICATION_STATUS("인증 상태 확인", "클라이언트에서 현재 인증 상태 확인 요청"),

    // === 상태 보고 ===
    CLIENT_STATE_CHANGE("클라이언트 상태 변경", "클라이언트에서 상태 변경 알림"),
    PERMISSION_STATUS("권한 상태 보고", "미디어 권한 상태 보고"),
    DEVICE_INFO("디바이스 정보", "클라이언트 디바이스 정보 전송"),

    // === 영상 관련 ===
    WAITING_VIDEO_STARTED("대기영상 시작", "대기영상 재생 시작 알림"),
    WAITING_VIDEO_ERROR("대기영상 오류", "대기영상 재생 오류 알림"),

    RECORDING_READY("녹화 준비 완료", "녹화 준비가 완료됨"),
    RECORDING_STARTED("녹화 시작", "녹화가 시작됨"),
    RECORDING_STOPPED("녹화 중지", "녹화가 중지됨"),
    RECORDING_ERROR("녹화 오류", "녹화 중 오류 발생"),

    VIDEO_UPLOAD_COMPLETE("영상 업로드 완료", "영상 파일 업로드 완료"),
    VIDEO_UPLOAD_ERROR("영상 업로드 오류", "영상 업로드 실패"),

    RESPONSE_VIDEO_STARTED("응답영상 시작", "응답영상 재생 시작"),
    RESPONSE_VIDEO_ENDED("응답영상 종료", "응답영상 재생 완료"),
    RESPONSE_VIDEO_ERROR("응답영상 오류", "응답영상 재생 오류"),

    // ========== 서버 → 클라이언트 ==========

    // === 연결 응답 ===
    CONNECTED("연결 완료", "서버에서 연결 완료 알림"),
    HEARTBEAT("하트비트", "서버에서 클라이언트로 생존 신호"),

    // === 인증 응답 (새로 추가) ===
    AUTHENTICATION_FAILED("인증 실패", "서버에서 인증 실패 알림"),
    TOKEN_EXPIRED("토큰 만료", "서버에서 토큰 만료 알림"),
    TOKEN_REFRESHED("토큰 갱신됨", "서버에서 토큰 갱신 성공 알림"),
    PERMISSION_DENIED("권한 거부", "서버에서 권한 부족으로 요청 거부"),
    SESSION_ACCESS_DENIED("세션 접근 거부", "해당 세션에 접근할 권한이 없음"),

    // === 상태 지시 ===
    STATE_TRANSITION("상태 전환 지시", "서버에서 상태 전환 지시"),
    FORCE_STATE_CHANGE("강제 상태 변경", "서버에서 강제로 상태 변경"),

    // === 영상 제어 ===
    PLAY_WAITING_VIDEO("대기영상 재생 지시", "대기영상 재생하도록 지시"),
    STOP_WAITING_VIDEO("대기영상 중지 지시", "대기영상 중지하도록 지시"),

    START_RECORDING("녹화 시작 지시", "녹화를 시작하도록 지시"),
    STOP_RECORDING("녹화 중지 지시", "녹화를 중지하도록 지시"),

    PLAY_RESPONSE_VIDEO("응답영상 재생 지시", "응답영상을 재생하도록 지시"),
    STOP_RESPONSE_VIDEO("응답영상 중지 지시", "응답영상을 중지하도록 지시"),

    // === 진행 상황 ===
    PROCESSING_PROGRESS("처리 진행 상황", "AI 처리 진행 상황 알림"),
    UPLOAD_PROGRESS("업로드 진행 상황", "파일 업로드 진행 상황"),

    // === 멀티 디바이스 ===
    DEVICE_REGISTERED("디바이스 등록됨", "새 디바이스가 등록됨"),
    DEVICE_DISCONNECTED("디바이스 연결 해제", "디바이스 연결이 해제됨"),
    PRIORITY_CHANGED("우선순위 변경", "디바이스 우선순위 변경"),

    // === 공통 ===
    ERROR("오류", "오류 메시지"),
    INFO("정보", "일반 정보 메시지"),
    WARNING("경고", "경고 메시지"),
    SUCCESS("성공", "성공 메시지");

    private final String displayName;
    private final String description;

    /**
     * 클라이언트에서 서버로 보내는 메시지인지 확인
     */
    public boolean isClientToServer() {
        return switch (this) {
            case CONNECT, DISCONNECT, HEARTBEAT_RESPONSE,
                 TOKEN_REFRESH, AUTHENTICATION_STATUS,
                 CLIENT_STATE_CHANGE, PERMISSION_STATUS, DEVICE_INFO,
                 WAITING_VIDEO_STARTED, WAITING_VIDEO_ERROR,
                 RECORDING_READY, RECORDING_STARTED, RECORDING_STOPPED, RECORDING_ERROR,
                 VIDEO_UPLOAD_COMPLETE, VIDEO_UPLOAD_ERROR,
                 RESPONSE_VIDEO_STARTED, RESPONSE_VIDEO_ENDED, RESPONSE_VIDEO_ERROR -> true;
            default -> false;
        };
    }

    /**
     * 서버에서 클라이언트로 보내는 메시지인지 확인
     */
    public boolean isServerToClient() {
        return !isClientToServer();
    }

    /**
     * 인증 관련 메시지인지 확인
     */
    public boolean isAuthenticationMessage() {
        return switch (this) {
            case TOKEN_REFRESH, AUTHENTICATION_STATUS, AUTHENTICATION_FAILED,
                 TOKEN_EXPIRED, TOKEN_REFRESHED, PERMISSION_DENIED, SESSION_ACCESS_DENIED -> true;
            default -> false;
        };
    }

    /**
     * 오류 관련 메시지인지 확인
     */
    public boolean isErrorMessage() {
        return this == ERROR ||
               this.name().contains("ERROR") ||
               this == WARNING ||
               this == AUTHENTICATION_FAILED ||
               this == PERMISSION_DENIED ||
               this == SESSION_ACCESS_DENIED;
    }

    /**
     * 상태 전환 관련 메시지인지 확인
     */
    public boolean isStateTransitionMessage() {
        return this == STATE_TRANSITION ||
               this == FORCE_STATE_CHANGE ||
               this == CLIENT_STATE_CHANGE;
    }

    /**
     * 보안 관련 심각도 레벨 반환
     */
    public SecurityLevel getSecurityLevel() {
        return switch (this) {
            case AUTHENTICATION_FAILED, TOKEN_EXPIRED, PERMISSION_DENIED, SESSION_ACCESS_DENIED -> SecurityLevel.HIGH;
            case TOKEN_REFRESH, AUTHENTICATION_STATUS -> SecurityLevel.MEDIUM;
            case CONNECT, DISCONNECT -> SecurityLevel.LOW;
            default -> SecurityLevel.NONE;
        };
    }

    /**
     * 보안 심각도 레벨
     */
    public enum SecurityLevel {
        NONE("보안 무관", 0),
        LOW("낮음", 1),
        MEDIUM("보통", 2),
        HIGH("높음", 3);

        private final String displayName;
        private final int level;

        SecurityLevel(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }

        public boolean isHigherThan(SecurityLevel other) {
            return this.level > other.level;
        }
    }
}