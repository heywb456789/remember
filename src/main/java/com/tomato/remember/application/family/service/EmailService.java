package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 이메일 발송 서비스 (에러 처리 강화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from:noreply@tomatoremember.com}")
    private String fromEmail;

    @Value("${spring.mail.domain:http://114.31.52.64:8096}")
    private String appDomain;

    @Value("${spring.mail.name:토마토리멤버}")
    private String appName;

    /**
     * 가족 초대 이메일 발송
     */
    public void sendFamilyInviteEmail(FamilyInviteToken inviteToken) {
        log.info("가족 초대 이메일 발송 시작 - 토큰: {}, 수신자: {}",
                inviteToken.getToken().substring(0, 8) + "...",
                inviteToken.getMaskedContact());

        try {
            // 1. 연결 테스트
            testMailConnection();

            // 2. 이메일 내용 생성
            String subject = createInviteSubject(inviteToken);
            String htmlContent = createInviteHtmlContent(inviteToken);

            // 3. 이메일 전송
            sendHtmlEmail(inviteToken.getContact(), subject, htmlContent);

            log.info("가족 초대 이메일 발송 완료 - 토큰: {}, 수신자: {}",
                    inviteToken.getToken().substring(0, 8) + "...",
                    inviteToken.getMaskedContact());

        } catch (Exception e) {
            log.error("가족 초대 이메일 발송 실패 - 토큰: {}, 수신자: {}, 오류: {}",
                    inviteToken.getToken().substring(0, 8) + "...",
                    inviteToken.getMaskedContact(),
                    e.getMessage(), e);

            // 상세한 오류 정보 제공
            String errorMessage = getDetailedErrorMessage(e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + errorMessage, e);
        }
    }

    /**
     * 메일 연결 테스트
     */
    private void testMailConnection() {
        try {
            if (mailSender instanceof JavaMailSenderImpl) {
                JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
                sender.testConnection();
                log.info("메일 서버 연결 테스트 성공");
            }
        } catch (Exception e) {
            log.error("메일 서버 연결 테스트 실패: {}", e.getMessage());
            throw new RuntimeException("메일 서버 연결에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * HTML 이메일 발송 (에러 처리 강화)
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent)
        throws MessagingException, MailException, UnsupportedEncodingException {

        log.info("HTML 이메일 발송 시작 - 받는 사람: {}", maskEmail(to));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // 발송자 설정
        helper.setFrom(fromEmail, appName);
        log.debug("발송자 설정 완료: {} <{}>", appName, fromEmail);

        // 수신자 설정
        helper.setTo(to);
        log.debug("수신자 설정 완료: {}", maskEmail(to));

        // 제목 설정
        helper.setSubject(subject);
        log.debug("제목 설정 완료: {}", subject);

        // 내용 설정
        helper.setText(htmlContent, true);
        log.debug("HTML 내용 설정 완료 (길이: {}자)", htmlContent.length());

        // 발송 실행
        mailSender.send(message);
        log.info("HTML 이메일 발송 완료 - 받는 사람: {}", maskEmail(to));
    }

    /**
     * 초대 이메일 제목 생성
     */
    private String createInviteSubject(FamilyInviteToken inviteToken) {
        return String.format("[%s] %s님이 가족 메모리얼에 초대했습니다",
                appName, inviteToken.getInviterName());
    }

    /**
     * 초대 이메일 HTML 내용 생성
     */
    private String createInviteHtmlContent(FamilyInviteToken inviteToken) {
        Context context = new Context(Locale.KOREAN);

        try {
            // 템플릿 변수 설정
            context.setVariable("appName", appName);
            context.setVariable("inviterName", inviteToken.getInviterName());
            context.setVariable("memorialName", inviteToken.getMemorialName());
            context.setVariable("relationshipDisplayName", inviteToken.getRelationshipDisplayName());
            context.setVariable("inviteMessage", inviteToken.getInviteMessage());
            context.setVariable("inviteLink", createInviteLink(inviteToken.getToken()));
            context.setVariable("expiresAt", formatExpirationDate(inviteToken));
            context.setVariable("remainingHours", inviteToken.getRemainingHours());

            // 고인 정보 (간소화)
            Memorial memorial = inviteToken.getMemorial();
            context.setVariable("deceasedName", memorial.getName());
            context.setVariable("deceasedNickname", memorial.getNickname());

            // 템플릿 처리
            return templateEngine.process("email/family-invite", context);

        } catch (Exception e) {
            log.error("이메일 HTML 내용 생성 실패: {}", e.getMessage());

            // 폴백 HTML 생성
            return createFallbackHtmlContent(inviteToken);
        }
    }

    /**
     * 폴백 HTML 내용 생성 (템플릿 오류 시)
     */
    private String createFallbackHtmlContent(FamilyInviteToken inviteToken) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head><body>");
        html.append("<h2>").append(appName).append(" 가족 메모리얼 초대</h2>");
        html.append("<p>").append(inviteToken.getInviterName()).append("님이 ");
        html.append(inviteToken.getMemorialName()).append(" 메모리얼에 ");
        html.append(inviteToken.getRelationshipDisplayName()).append(" 관계로 초대했습니다.</p>");

        if (inviteToken.getInviteMessage() != null && !inviteToken.getInviteMessage().trim().isEmpty()) {
            html.append("<p><strong>메시지:</strong> ").append(inviteToken.getInviteMessage()).append("</p>");
        }

        html.append("<p><a href='").append(createInviteLink(inviteToken.getToken())).append("'");
        html.append(" style='background: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>");
        html.append("초대 수락하기</a></p>");

        html.append("<p><small>이 초대는 ").append(formatExpirationDate(inviteToken)).append("에 만료됩니다.</small></p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * 초대 링크 생성
     */
    private String createInviteLink(String token) {
        return appDomain + "/mobile/family/invite/" + token;
    }

    /**
     * 만료 날짜 포맷팅
     */
    private String formatExpirationDate(FamilyInviteToken inviteToken) {
        return inviteToken.getExpiresAt()
                .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"));
    }

    /**
     * 이메일 발송 가능 여부 확인
     */
    public boolean canSendEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // 이메일 형식 검증
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 테스트 이메일 발송
     */
    public void sendTestEmail(String to) {
        try {
            String subject = "[" + appName + "] 테스트 이메일";
            String content = createTestEmailContent();

            sendHtmlEmail(to, subject, content);

            log.info("테스트 이메일 발송 완료 - 수신자: {}", maskEmail(to));
        } catch (Exception e) {
            log.error("테스트 이메일 발송 실패 - 수신자: {}", maskEmail(to), e);
            throw new RuntimeException("테스트 이메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 테스트 이메일 내용 생성
     */
    private String createTestEmailContent() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>테스트 이메일</title>
            </head>
            <body>
                <h2>%s 테스트 이메일</h2>
                <p>이메일 발송이 정상적으로 작동합니다.</p>
                <p>발송 시간: %s</p>
                <p>서버 정보: %s</p>
            </body>
            </html>
            """.formatted(
                appName,
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                appDomain
            );
    }

    /**
     * 상세한 오류 메시지 생성
     */
    private String getDetailedErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            return "알 수 없는 오류가 발생했습니다.";
        }

        // 일반적인 오류 패턴 분석
        if (message.contains("Connection timed out") || message.contains("연결하지 못했거나")) {
            return "메일 서버 연결 시간 초과 - 네트워크 또는 방화벽 설정을 확인해주세요.";
        }

        if (message.contains("Authentication failed") || message.contains("인증")) {
            return "메일 서버 인증 실패 - 사용자명/비밀번호 또는 앱 비밀번호를 확인해주세요.";
        }

        if (message.contains("SSL") || message.contains("TLS")) {
            return "SSL/TLS 연결 오류 - 메일 서버 보안 설정을 확인해주세요.";
        }

        if (message.contains("Invalid Addresses") || message.contains("주소")) {
            return "잘못된 이메일 주소 - 발송자 또는 수신자 이메일을 확인해주세요.";
        }

        // 원본 메시지 반환 (앞의 100자만)
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }

    /**
     * 이메일 마스킹
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