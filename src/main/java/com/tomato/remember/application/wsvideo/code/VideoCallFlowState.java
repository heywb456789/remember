package com.tomato.remember.application.wsvideo.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 간소화된 영상통화 상태 관리 (9개 상태)
 * 실제 비즈니스 플로우에 맞춘 핵심 상태만 정의
 */
@Getter
@RequiredArgsConstructor
public enum VideoCallFlowState {

    // === 초기화 ===
    INITIALIZING("초기화 중", "시스템을 준비하고 있습니다"),

    // === 권한 요청 ===
    PERMISSION_REQUESTING("권한 요청", "카메라와 마이크 권한을 요청합니다"),

    // === 대기 (핵심 상태) ===
    WAITING("대기 중", "대기영상이 재생되고 있습니다"),

    // === 녹화 (핵심 상태) ===
    RECORDING("녹화 중", "음성이 녹화되고 있습니다"),

    // === 처리 (핵심 상태) ===
    PROCESSING("처리 중", "AI가 응답을 생성하고 있습니다"),

    // === 응답 재생 (핵심 상태) ===
    RESPONSE_PLAYING("응답 재생 중", "응답영상이 재생되고 있습니다"),

    // === 통화 종료 ===
    CALL_ENDING("통화 종료 중", "통화를 종료하고 있습니다"),
    CALL_COMPLETED("통화 완료", "통화가 완료되었습니다"),

    // === 오류 ===
    ERROR("오류", "오류가 발생했습니다");

    private final String displayName;
    private final String description;

    /**
     * 간소화된 상태 전환 규칙
     * 실제 비즈니스 플로우만 허용
     */
    public boolean canTransitionTo(VideoCallFlowState targetState) {
        return switch (this) {
            // 초기화 → 권한요청 or 대기 or 오류
            case INITIALIZING -> targetState == PERMISSION_REQUESTING ||
                               targetState == WAITING ||
                               targetState == ERROR;

            // 권한요청 → 대기 or 오류
            case PERMISSION_REQUESTING -> targetState == WAITING ||
                                        targetState == ERROR;

            // 대기 → 녹화 or 통화종료 or 오류
            case WAITING -> targetState == RECORDING ||
                          targetState == CALL_ENDING ||
                          targetState == ERROR;

            // 녹화 → 처리 or 대기(취소) or 오류
            case RECORDING -> targetState == PROCESSING ||
                            targetState == WAITING ||
                            targetState == ERROR;

            // 처리 → 응답재생 or 대기(실패시) or 오류
            case PROCESSING -> targetState == RESPONSE_PLAYING ||
                             targetState == WAITING ||
                             targetState == ERROR;

            // 응답재생 → 대기(반복) or 오류
            case RESPONSE_PLAYING -> targetState == WAITING ||
                                   targetState == ERROR;

            // 통화종료 → 통화완료 (단방향)
            case CALL_ENDING -> targetState == CALL_COMPLETED;

            // 통화완료 → 더 이상 전환 불가 (최종 상태)
            case CALL_COMPLETED -> false;

            // 오류 → 대기(복구) or 통화종료 or 초기화(재시작)
            case ERROR -> targetState == WAITING ||
                        targetState == CALL_ENDING ||
                        targetState == INITIALIZING;
        };
    }

    /**
     * 오류 상태인지 확인
     */
    public boolean isErrorState() {
        return this == ERROR;
    }

    /**
     * 최종 상태인지 확인 (더 이상 전환되지 않음)
     */
    public boolean isFinalState() {
        return this == CALL_COMPLETED;
    }

    /**
     * 녹화 가능한 상태인지 확인
     */
    public boolean canRecord() {
        return this == WAITING;
    }

    /**
     * 로딩 표시가 필요한 상태인지 확인
     */
    public boolean showLoading() {
        return this == INITIALIZING ||
               this == PROCESSING;
    }

    /**
     * 사용자 상호작용이 가능한 상태인지 확인
     */
    public boolean allowUserInteraction() {
        return this == WAITING ||
               this == PERMISSION_REQUESTING;
    }

    /**
     * 안정적인 상태인지 확인 (장시간 유지 가능)
     */
    public boolean isStableState() {
        return this == WAITING ||
               this == RESPONSE_PLAYING ||
               this == CALL_COMPLETED;
    }

    /**
     * 일시적인 상태인지 확인 (빠르게 전환되어야 함)
     */
    public boolean isTransientState() {
        return this == INITIALIZING ||
               this == PERMISSION_REQUESTING ||
               this == RECORDING ||
               this == PROCESSING ||
               this == CALL_ENDING;
    }

    /**
     * WebSocket으로 브로드캐스트해야 하는 상태인지 확인
     */
    public boolean shouldBroadcast() {
        return true; // 모든 상태 변경은 브로드캐스트
    }

    /**
     * 상태 표시 메시지 (사용자용)
     */
    public String getStatusMessage() {
        return displayName;
    }

    /**
     * 상세 설명 (개발자/로그용)
     */
    public String getDetailedDescription() {
        return description;
    }

    /**
     * UI에서 표시할 상태 아이콘 반환
     */
    public String getStateIcon() {
        return switch (this) {
            case INITIALIZING -> "⚙️";
            case PERMISSION_REQUESTING -> "🔒";
            case WAITING -> "⏳";
            case RECORDING -> "🔴";
            case PROCESSING -> "🤖";
            case RESPONSE_PLAYING -> "🎬";
            case CALL_ENDING -> "👋";
            case CALL_COMPLETED -> "✅";
            case ERROR -> "❌";
        };
    }

    /**
     * 상태의 우선순위 반환 (높을수록 중요)
     * 동시에 여러 상태 전환 요청이 있을 때 사용
     */
    public int getPriority() {
        return switch (this) {
            case ERROR -> 9; // 최고 우선순위
            case CALL_COMPLETED -> 8;
            case CALL_ENDING -> 7;
            case PROCESSING -> 6;
            case RECORDING -> 5;
            case RESPONSE_PLAYING -> 4;
            case WAITING -> 3;
            case PERMISSION_REQUESTING -> 2;
            case INITIALIZING -> 1; // 최저 우선순위
        };
    }

    /**
     * 다음에 가능한 상태들 반환 (디버깅용)
     */
    public VideoCallFlowState[] getNextPossibleStates() {
        return java.util.Arrays.stream(VideoCallFlowState.values())
                .filter(this::canTransitionTo)
                .toArray(VideoCallFlowState[]::new);
    }

    /**
     * 상태 전환 경로 검증 (여러 단계 전환)
     */
    public boolean canReach(VideoCallFlowState targetState, int maxSteps) {
        if (maxSteps <= 0) return false;
        if (this == targetState) return true;
        if (this.canTransitionTo(targetState)) return true;

        // 재귀적으로 다음 상태들 확인
        for (VideoCallFlowState nextState : this.getNextPossibleStates()) {
            if (nextState.canReach(targetState, maxSteps - 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.name(), this.displayName);
    }
}