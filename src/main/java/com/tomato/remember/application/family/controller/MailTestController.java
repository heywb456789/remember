package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.service.EmailService;
import com.tomato.remember.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 메일 발송 테스트 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/mail/test")
@RequiredArgsConstructor
public class MailTestController {

    private final EmailService emailService;
    private final JavaMailSender mailSender;

    /**
     * 메일 서버 연결 테스트
     * GET /api/mail/test/connection
     */
    @GetMapping("/connection")
    public ResponseDTO<Map<String, Object>> testConnection() {
        log.info("메일 서버 연결 테스트 시작");

        Map<String, Object> result = new HashMap<>();
        
        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
                
                // 연결 정보 수집
                result.put("host", sender.getHost());
                result.put("port", sender.getPort());
                result.put("username", sender.getUsername());
                result.put("protocol", sender.getProtocol());
                
                // 연결 테스트
                sender.testConnection();
                
                result.put("status", "success");
                result.put("message", "메일 서버 연결 성공");
                result.put("connected", true);
                
                log.info("메일 서버 연결 테스트 성공 - {}:{}", sender.getHost(), sender.getPort());
                
            } else {
                result.put("status", "error");
                result.put("message", "JavaMailSenderImpl 타입이 아닙니다.");
                result.put("connected", false);
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "메일 서버 연결 실패: " + e.getMessage());
            result.put("connected", false);
            result.put("error", e.getClass().getSimpleName());
            
            log.error("메일 서버 연결 테스트 실패", e);
        }
        
        return ResponseDTO.ok(result);
    }

    /**
     * 테스트 이메일 발송
     * POST /api/mail/test/send
     */
    @PostMapping("/send")
    public ResponseDTO<Map<String, Object>> sendTestEmail(@RequestParam String to) {
        log.info("테스트 이메일 발송 시작 - 수신자: {}", maskEmail(to));

        Map<String, Object> result = new HashMap<>();
        
        try {
            // 이메일 주소 유효성 검사
            if (!emailService.canSendEmail(to)) {
                result.put("status", "error");
                result.put("message", "유효하지 않은 이메일 주소입니다.");
                return ResponseDTO.ok(result);
            }
            
            // 테스트 이메일 발송
            emailService.sendTestEmail(to);
            
            result.put("status", "success");
            result.put("message", "테스트 이메일 발송 성공");
            result.put("recipient", maskEmail(to));
            result.put("sent", true);
            
            log.info("테스트 이메일 발송 완료 - 수신자: {}", maskEmail(to));
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "테스트 이메일 발송 실패: " + e.getMessage());
            result.put("recipient", maskEmail(to));
            result.put("sent", false);
            result.put("error", e.getClass().getSimpleName());
            
            log.error("테스트 이메일 발송 실패 - 수신자: {}", maskEmail(to), e);
        }
        
        return ResponseDTO.ok(result);
    }

    /**
     * 메일 설정 정보 조회
     * GET /api/mail/test/config
     */
    @GetMapping("/config")
    public ResponseDTO<Map<String, Object>> getMailConfig() {
        log.info("메일 설정 정보 조회");

        Map<String, Object> result = new HashMap<>();
        
        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
                
                result.put("host", sender.getHost());
                result.put("port", sender.getPort());
                result.put("username", sender.getUsername());
                result.put("protocol", sender.getProtocol());
                result.put("defaultEncoding", sender.getDefaultEncoding());
                
                // 속성 정보 (민감한 정보는 제외)
                Map<String, Object> properties = new HashMap<>();
                sender.getJavaMailProperties().forEach((key, value) -> {
                    String keyStr = key.toString();
                    if (!keyStr.toLowerCase().contains("password") && 
                        !keyStr.toLowerCase().contains("secret")) {
                        properties.put(keyStr, value);
                    }
                });
                result.put("properties", properties);
                
                result.put("status", "success");
                result.put("message", "메일 설정 정보 조회 성공");
                
            } else {
                result.put("status", "error");
                result.put("message", "JavaMailSenderImpl 타입이 아닙니다.");
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "메일 설정 정보 조회 실패: " + e.getMessage());
            
            log.error("메일 설정 정보 조회 실패", e);
        }
        
        return ResponseDTO.ok(result);
    }

    /**
     * 메일 서버 상태 종합 체크
     * GET /api/mail/test/health
     */
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> healthCheck() {
        log.info("메일 서버 상태 종합 체크 시작");

        Map<String, Object> result = new HashMap<>();
        
        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
                
                // 기본 설정 확인
                boolean hasHost = sender.getHost() != null && !sender.getHost().trim().isEmpty();
                boolean hasPort = sender.getPort() > 0;
                boolean hasUsername = sender.getUsername() != null && !sender.getUsername().trim().isEmpty();
                boolean hasPassword = sender.getPassword() != null && !sender.getPassword().trim().isEmpty();
                
                result.put("hasHost", hasHost);
                result.put("hasPort", hasPort);
                result.put("hasUsername", hasUsername);
                result.put("hasPassword", hasPassword);
                
                // 연결 테스트
                boolean connected = false;
                String connectionError = null;
                
                try {
                    sender.testConnection();
                    connected = true;
                } catch (Exception e) {
                    connectionError = e.getMessage();
                }
                
                result.put("connected", connected);
                result.put("connectionError", connectionError);
                
                // 전체 상태 판정
                boolean healthy = hasHost && hasPort && hasUsername && hasPassword && connected;
                result.put("healthy", healthy);
                
                result.put("status", healthy ? "success" : "warning");
                result.put("message", healthy ? "메일 서버 상태 정상" : "메일 서버 설정 또는 연결에 문제가 있습니다.");
                
            } else {
                result.put("status", "error");
                result.put("message", "JavaMailSenderImpl 타입이 아닙니다.");
                result.put("healthy", false);
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "메일 서버 상태 체크 실패: " + e.getMessage());
            result.put("healthy", false);
            
            log.error("메일 서버 상태 체크 실패", e);
        }
        
        return ResponseDTO.ok(result);
    }

    /**
     * 네트워크 연결 테스트
     * GET /api/mail/test/network
     */
    @GetMapping("/network")
    public ResponseDTO<Map<String, Object>> testNetworkConnection() {
        log.info("네트워크 연결 테스트 시작");

        Map<String, Object> result = new HashMap<>();
        
        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
                
                String host = sender.getHost();
                int port = sender.getPort();
                
                result.put("host", host);
                result.put("port", port);
                
                // 소켓 연결 테스트
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                    
                    result.put("socketConnected", true);
                    result.put("status", "success");
                    result.put("message", "네트워크 연결 성공");
                    
                } catch (Exception e) {
                    result.put("socketConnected", false);
                    result.put("status", "error");
                    result.put("message", "네트워크 연결 실패: " + e.getMessage());
                    result.put("error", e.getClass().getSimpleName());
                }
                
            } else {
                result.put("status", "error");
                result.put("message", "JavaMailSenderImpl 타입이 아닙니다.");
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "네트워크 연결 테스트 실패: " + e.getMessage());
            
            log.error("네트워크 연결 테스트 실패", e);
        }
        
        return ResponseDTO.ok(result);
    }

    /**
     * 이메일 마스킹 유틸리티
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex > 2) {
            return email.substring(0, 2) + "**" + email.substring(atIndex);
        }
        
        return "**" + email.substring(atIndex);
    }
}