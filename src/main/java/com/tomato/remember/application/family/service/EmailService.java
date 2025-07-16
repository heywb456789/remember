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

    @Value("${spring.mail.domain:http://114.31.52.64:8095}")
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
            // 메모리얼 정보
            Memorial memorial = inviteToken.getMemorial();

            // 디버깅 로그 추가
            log.info("이메일 템플릿 변수 설정 - 메모리얼: name={}, nickname={}, 관계={}",
                    memorial.getName(), memorial.getNickname(), inviteToken.getRelationshipDisplayName());

            // 템플릿 변수 설정 (null 체크 강화)
            context.setVariable("appName", appName != null ? appName : "토마토리멤버");
            context.setVariable("inviterName", inviteToken.getInviterName() != null ? inviteToken.getInviterName() : "알 수 없음");
            context.setVariable("memorialName", memorial.getName() != null && !memorial.getName().trim().isEmpty() ? memorial.getName() : "이름 없음");
            context.setVariable("deceasedName", memorial.getName() != null && !memorial.getName().trim().isEmpty() ? memorial.getName() : null);
            context.setVariable("deceasedNickname", memorial.getNickname() != null && !memorial.getNickname().trim().isEmpty() ? memorial.getNickname() : "별명 없음");
            context.setVariable("relationshipDisplayName", inviteToken.getRelationshipDisplayName() != null ? inviteToken.getRelationshipDisplayName() : "관계 미설정");
            context.setVariable("inviteMessage", inviteToken.getInviteMessage() != null && !inviteToken.getInviteMessage().trim().isEmpty() ? inviteToken.getInviteMessage() : null);
            context.setVariable("inviteLink", createInviteLink(inviteToken.getToken()));
            context.setVariable("expiresAt", formatExpirationDate(inviteToken));
            context.setVariable("remainingHours", inviteToken.getRemainingHours());

            // 템플릿 처리
            String htmlContent = templateEngine.process("email/family-invite", context);

            // 생성된 HTML 내용 로그 (처음 500자만)
            log.info("생성된 HTML 내용 미리보기: {}",
                    htmlContent.length() > 500 ? htmlContent.substring(0, 500) + "..." : htmlContent);

            return htmlContent;

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
        Memorial memorial = inviteToken.getMemorial();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>가족 메모리얼 초대</title>");
        html.append("<style>");
        html.append("body { font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f8f9fa; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 32px 24px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 24px; font-weight: 700; }");
        html.append(".header p { margin: 8px 0 0 0; font-size: 16px; opacity: 0.9; }");
        html.append(".content { padding: 32px 24px; }");
        html.append(".invite-card { background: #f8f9fa; border-radius: 12px; padding: 24px; margin-bottom: 24px; border-left: 4px solid #28a745; }");
        html.append(".memorial-info h3 { margin: 0 0 16px 0; font-size: 18px; font-weight: 600; color: #333; }");
        html.append(".invite-message { background: #fff; border-radius: 8px; padding: 16px; margin: 16px 0; border: 1px solid #e9ecef; font-style: italic; color: #555; }");
        html.append(".invite-button { display: inline-block; padding: 16px 32px; background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; text-decoration: none; border-radius: 8px; font-size: 16px; font-weight: 600; margin: 16px 0; }");
        html.append(".invite-button:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(40,167,69,0.3); }");
        html.append(".expiration-info { background: #fff3cd; border-radius: 8px; padding: 16px; margin: 24px 0; border-left: 4px solid #ffc107; }");
        html.append(".footer { background: #f8f9fa; padding: 24px; text-align: center; border-top: 1px solid #e9ecef; color: #6c757d; font-size: 12px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // 컨테이너 시작
        html.append("<div class='container'>");

        // 헤더
        html.append("<div class='header'>");
        html.append("<h1>").append(appName).append("</h1>");
        html.append("<p>소중한 추억을 영원히 간직하세요</p>");
        html.append("</div>");

        // 메인 콘텐츠
        html.append("<div class='content'>");

        // 초대 카드
        html.append("<div class='invite-card'>");
        html.append("<div class='memorial-info'>");
        html.append("<h3>").append(memorial.getNickname()).append(" (").append(memorial.getName()).append(") 메모리얼에 초대되었습니다</h3>");
        html.append("<p><strong>").append(inviteToken.getInviterName()).append("</strong>님이 ");
        html.append("<span style='background: #28a745; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600;'>");
        html.append(inviteToken.getRelationshipDisplayName()).append("</span> 관계로 초대했습니다</p>");
        html.append("</div>");

        // 초대 메시지
        if (inviteToken.getInviteMessage() != null && !inviteToken.getInviteMessage().trim().isEmpty()) {
            html.append("<div class='invite-message'>");
            html.append("<p>").append(inviteToken.getInviteMessage()).append("</p>");
            html.append("</div>");
        }

        // 초대 수락 버튼
        html.append("<div style='text-align: center;'>");
        html.append("<a href='").append(createInviteLink(inviteToken.getToken())).append("' class='invite-button'>");
        html.append("초대 수락하기</a>");
        html.append("</div>");

        html.append("</div>"); // invite-card 끝

        // 만료 정보
        html.append("<div class='expiration-info'>");
        html.append("<h4 style='margin: 0 0 8px 0; font-size: 14px; font-weight: 600; color: #856404;'>⏰ 초대 만료 안내</h4>");
        html.append("<p style='margin: 0; font-size: 13px; color: #856404;'>");
        html.append("이 초대는 <strong>").append(formatExpirationDate(inviteToken)).append("</strong>에 만료됩니다. ");
        html.append("(남은 시간: <strong>").append(inviteToken.getRemainingHours()).append("시간</strong>)");
        html.append("</p>");
        html.append("</div>");

        // 도움말
        html.append("<div style='text-align: center; color: #666; font-size: 14px; margin-top: 24px;'>");
        html.append("<p>초대 링크가 작동하지 않나요?</p>");
        html.append("<p>아래 링크를 복사하여 브라우저에 직접 입력해 주세요:</p>");
        html.append("<p style='word-break: break-all; background: #f8f9fa; padding: 8px; border-radius: 4px; font-family: monospace; font-size: 12px;'>");
        html.append(createInviteLink(inviteToken.getToken()));
        html.append("</p>");
        html.append("</div>");

        html.append("</div>"); // content 끝

        // 푸터
        html.append("<div class='footer'>");
        html.append("<p><strong>").append(appName).append("</strong></p>");
        html.append("<p>이 이메일은 ").append(inviteToken.getInviterName()).append("님이 요청하여 발송되었습니다.<br>");
        html.append("문의사항이 있으시면 <a href='mailto:support@tomatoremember.com' style='color: #667eea;'>support@tomatoremember.com</a>으로 연락주세요.</p>");
        html.append("</div>");

        html.append("</div>"); // container 끝
        html.append("</body>");
        html.append("</html>");

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