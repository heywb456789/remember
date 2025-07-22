package com.tomato.remember.application.wsvideo.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 기반 영상통화의 세밀한 상태 관리
 */
@Getter
@RequiredArgsConstructor
public enum VideoCallFlowState {
    // === 초기화 단계 ===
    INITIALIZING("초기화 중", "시스템을 준비하고 있습니다"),
    PERMISSION_REQUESTING("권한 요청 중", "카메라와 마이크 권한을 요청합니다"),
    PERMISSION_GRANTED("권한 승인됨", "미디어 권한이 승인되었습니다"),
    
    // === 대기 단계 ===
    WAITING_READY("대기 준비", "대기영상을 준비합니다"),
    WAITING_PLAYING("대기영상 재생 중", "대기영상이 재생되고 있습니다"),
    
    // === 녹화 단계 ===
    RECORDING_COUNTDOWN("녹화 카운트다운", "녹화 시작까지 3초 전"),
    RECORDING_ACTIVE("녹화 중", "음성이 녹화되고 있습니다"),
    RECORDING_COMPLETE("녹화 완료", "녹화가 완료되었습니다"),
    
    // === 처리 단계 ===
    PROCESSING_UPLOAD("업로드 중", "영상을 서버에 업로드하고 있습니다"),
    PROCESSING_AI("AI 처리 중", "AI가 응답을 생성하고 있습니다"),
    PROCESSING_COMPLETE("처리 완료", "AI 처리가 완료되었습니다"),
    
    // === 응답 단계 ===
    RESPONSE_READY("응답 준비", "응답영상을 준비합니다"),
    RESPONSE_PLAYING("응답영상 재생 중", "응답영상이 재생되고 있습니다"),
    RESPONSE_COMPLETE("응답 완료", "응답영상 재생이 완료되었습니다"),
    
    // === 종료 단계 ===
    CALL_ENDING("통화 종료 중", "통화를 종료하고 있습니다"),
    CALL_COMPLETED("통화 완료", "통화가 완료되었습니다"),
    
    // === 오류 단계 ===
    ERROR_NETWORK("네트워크 오류", "네트워크 연결에 문제가 있습니다"),
    ERROR_PERMISSION("권한 오류", "카메라 또는 마이크 권한이 필요합니다"),
    ERROR_PROCESSING("처리 오류", "영상 처리 중 오류가 발생했습니다"),
    ERROR_TIMEOUT("시간 초과", "요청 시간이 초과되었습니다");

    private final String displayName;
    private final String description;

    /**
     * 다음 상태로 전환 가능한지 검증
     */
    public boolean canTransitionTo(VideoCallFlowState targetState) {
        return switch (this) {
            case INITIALIZING -> targetState == PERMISSION_REQUESTING || 
                               targetState == WAITING_READY ||
                               isErrorState(targetState);
                               
            case PERMISSION_REQUESTING -> targetState == PERMISSION_GRANTED ||
                                        targetState == ERROR_PERMISSION ||
                                        isErrorState(targetState);
                                        
            case PERMISSION_GRANTED -> targetState == WAITING_READY ||
                                     isErrorState(targetState);
                                     
            case WAITING_READY -> targetState == WAITING_PLAYING ||
                                isErrorState(targetState);
                                
            case WAITING_PLAYING -> targetState == RECORDING_COUNTDOWN ||
                                  targetState == CALL_ENDING ||
                                  isErrorState(targetState);
                                  
            case RECORDING_COUNTDOWN -> targetState == RECORDING_ACTIVE ||
                                      targetState == WAITING_PLAYING ||
                                      isErrorState(targetState);
                                      
            case RECORDING_ACTIVE -> targetState == RECORDING_COMPLETE ||
                                   targetState == WAITING_PLAYING ||
                                   isErrorState(targetState);
                                   
            case RECORDING_COMPLETE -> targetState == PROCESSING_UPLOAD ||
                                     isErrorState(targetState);
                                     
            case PROCESSING_UPLOAD -> targetState == PROCESSING_AI ||
                                    isErrorState(targetState);
                                    
            case PROCESSING_AI -> targetState == PROCESSING_COMPLETE ||
                                isErrorState(targetState);
                                
            case PROCESSING_COMPLETE -> targetState == RESPONSE_READY ||
                                      isErrorState(targetState);
                                      
            case RESPONSE_READY -> targetState == RESPONSE_PLAYING ||
                                 isErrorState(targetState);
                                 
            case RESPONSE_PLAYING -> targetState == RESPONSE_COMPLETE ||
                                   isErrorState(targetState);
                                   
            case RESPONSE_COMPLETE -> targetState == WAITING_PLAYING ||
                                    targetState == CALL_ENDING ||
                                    isErrorState(targetState);
                                    
            case CALL_ENDING -> targetState == CALL_COMPLETED;
            
            case CALL_COMPLETED -> false; // 최종 상태
            
            // 오류 상태에서는 대기 상태나 종료 상태로만 전환 가능
            case ERROR_NETWORK, ERROR_PERMISSION, ERROR_PROCESSING, ERROR_TIMEOUT -> 
                targetState == WAITING_PLAYING || 
                targetState == CALL_ENDING ||
                targetState == INITIALIZING; // 재시작 가능
        };
    }

    /**
     * 오류 상태인지 확인
     */
    public boolean isErrorState() {
        return isErrorState(this);
    }
    
    private static boolean isErrorState(VideoCallFlowState state) {
        return state == ERROR_NETWORK || 
               state == ERROR_PERMISSION || 
               state == ERROR_PROCESSING || 
               state == ERROR_TIMEOUT;
    }

    /**
     * 사용자에게 표시할 상태 메시지
     */
    public String getStatusMessage() {
        return displayName;
    }

    /**
     * 상세 설명
     */
    public String getDetailedDescription() {
        return description;
    }
}