package com.tomato.remember.application.oneld.service;

import com.tomato.remember.application.auth.dto.AuthRequestDTO;
import com.tomato.remember.application.auth.dto.PasswordResetOneIdResponse;
import com.tomato.remember.application.auth.dto.PasswordResetRequestDTO;
import com.tomato.remember.application.auth.dto.PasswordResetVerifyResponse;
import com.tomato.remember.application.oneld.dto.OneIdImageResponse;
import com.tomato.remember.application.oneld.dto.OneIdResponse;
import com.tomato.remember.application.oneld.dto.OneIdVerifyResponse;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.exception.BadRequestException;
import com.tomato.remember.common.util.AES256;
import com.tomato.remember.common.util.WebClientLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TomatoAuthService {

    @Value("${one-id.endpoints.login}")
    private String loginUrl;

    @Value("${one-id.endpoints.send-sms-cert}")
    private String sendSmsCert;

    @Value("${one-id.endpoints.verify-sms-cert}")
    private String verifySmsCert;

    @Value("${one-id.endpoints.register}")
    private String register;

    @Value("${one-id.app-type}")
    private String appType;

    @Value("${one-id.nation}")
    private String nationCode;

    @Value("${one-id.endpoints.coin-info}")
    private String coinInfo;

    @Value("${one-id.endpoints.profile-img}")
    private String profileImg;

    @Value("${one-id.endpoints.password-reset-cert}")
    private String passwordResetCert;

    @Value("${one-id.endpoints.password-reset-verify}")
    private String passwordResetVerify;

    @Value("${one-id.endpoints.password-reset}")
    private String passwordReset;

    private final WebClient webClient;

    public TomatoAuthService(@Value("${one-id.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .filter(WebClientLoggingFilter.logRequest())   // 요청 로깅
            .filter(WebClientLoggingFilter.logResponseBody())  // 응답 로깅
            .build();
    }

    public Mono<OneIdResponse> authenticate(String phoneNumber, String password) {
        if (phoneNumber == null || password == null) {
            return Mono.error(
                new BadRequestException("Invalid phone number or password"));
        }

        return webClient.post()
            .uri(loginUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("phonenum", phoneNumber)
                    .with("passwd", password)
            )
            .retrieve()
            // 4xx 응답 오면 BadRequestException 던지기
            .onStatus(
                HttpStatusCode::is4xxClientError,
                clientResp -> Mono.error(new APIException(ResponseStatus.UNAUTHORIZED_ONE_ID)))
            // 5xx 응답 오면 APIException (서버 에러) 던지기
            .onStatus(
                HttpStatusCode::is5xxServerError,
                clientResp -> Mono.error(
                    new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
            .bodyToMono(OneIdResponse.class);
    }

    public Mono<OneIdResponse> sendSmsCert(AuthRequestDTO req) {
        if (req.getPhoneNumber() == null) {
            throw new BadRequestException("missing phone number");
        }
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedPhoneNumber = AES256.encrypt(req.getPhoneNumber(), timestamp);
        log.debug("encode>>>>>{}", encodedPhoneNumber);

        return webClient.post()
            .uri(sendSmsCert, appType)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("phonenum", encodedPhoneNumber)
                    .with("nation", nationCode)
                    .with("timestamp", timestamp)
                    .with("agree", "false")
            )
            .retrieve()
            // 4xx 응답 오면 BadRequestException 던지기
            .onStatus(
                HttpStatusCode::is4xxClientError,
                clientResp -> Mono.error(new BadRequestException("One-ID 인증 실패")))
            // 5xx 응답 오면 APIException (서버 에러) 던지기
            .onStatus(
                HttpStatusCode::is5xxServerError,
                clientResp -> Mono.error(
                    new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
            .bodyToMono(OneIdResponse.class);
//            .map(oneIdResponse -> {
//                oneIdResponse.getValue().setTimeStamp(timestamp);
//                return oneIdResponse;
//            });

    }

    public Mono<OneIdVerifyResponse> verifySmsCert(AuthRequestDTO req) {
        if (req.getVerificationCode() == null) {
            throw new BadRequestException("missing Verification code");
        }
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedUserKey = AES256.encrypt(req.getUserKey(), timestamp);

        return webClient.post()
            .uri(verifySmsCert, appType)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("userkey", encodedUserKey)
                    .with("code", req.getVerificationCode())
                    .with("timestamp", timestamp)
            )
            .retrieve()
            // 4xx 응답 오면 BadRequestException 던지기
            .onStatus(
                HttpStatusCode::is4xxClientError,
                clientResp -> Mono.error(new BadRequestException("One-ID 인증 실패")))
            // 5xx 응답 오면 APIException (서버 에러) 던지기
            .onStatus(
                HttpStatusCode::is5xxServerError,
                clientResp -> Mono.error(
                    new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
            .bodyToMono(OneIdVerifyResponse.class);

    }


    public OneIdResponse createOneId(AuthRequestDTO req) {
        if (req.getPassword() == null && req.getUserKey() == null && req.getName() == null) {
            throw new BadRequestException("missing Verification code");
        }

        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedUserKey = AES256.encrypt(req.getUserKey(), timestamp);
        String encodedPasswd = AES256.encrypt(req.getPassword(), timestamp);

        // Mono<OneIdResponse> 생성
        Mono<OneIdResponse> mono = webClient.post()
            .uri(register, appType)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters
                .fromFormData("userkey", encodedUserKey)
                .with("passwd", encodedPasswd)
                .with("timestamp", timestamp)
                .with("name", req.getName())
            )
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                resp -> Mono.error(new BadRequestException("One-ID 인증 실패")))
            .onStatus(HttpStatusCode::is5xxServerError,
                resp -> Mono.error(
                    new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
            .bodyToMono(OneIdResponse.class);

        // 블록해서 OneIdResponse 객체를 동기적으로 리턴
        return mono.block();
    }

    public OneIdResponse getWalletInfo(AuthRequestDTO req) {
        if (req.getUserKey() == null || req.getPhoneNumber() == null) {
            throw new BadRequestException("userKey 또는 전화번호 누락");
        }

        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedPhone = AES256.encrypt(req.getPhoneNumber(), timestamp); // 암호화 버전 사용
        String resolvedPath = coinInfo.replace("{apptype}", appType);

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(resolvedPath)
                .queryParam("userkey", req.getUserKey())
                .queryParam("phone", encodedPhone)
                .queryParam("nation", nationCode)
                .queryParam("timestamp", timestamp)
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                res -> Mono.error(new BadRequestException("One-ID 지갑 조회 실패")))
            .onStatus(HttpStatusCode::is5xxServerError,
                res -> Mono.error(
                    new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
            .bodyToMono(OneIdResponse.class)
            .doOnNext(res -> log.info("지갑 조회 응답: {}", res))
            .block(); // 동기 응답
    }

    public OneIdImageResponse uploadProfileImageExternal(String userKey, MultipartFile file) {
        // 1) 멀티파트 바디 빌더에 “file” 파트 추가
        MultipartBodyBuilder mpBuilder = new MultipartBodyBuilder();
        mpBuilder.part("cType", 2);
        mpBuilder.part("fType", 0);
        mpBuilder.part("file", file.getResource())
            // 실제 전송 시 filename 헤더가 필요하면 Content-Disposition 을 수동으로도 추가할 수 있습니다
            .header("Content-Disposition",
                "form-data; name=file; filename=\"" + file.getOriginalFilename() + "\"");

        return webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path(profileImg)
                .build(appType, userKey)
            )
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(mpBuilder.build()))
            .retrieve()
            // 4xx → 인증 오류라면 BadRequestException
            .onStatus(HttpStatusCode::is4xxClientError,
                resp -> Mono.error(new BadRequestException("프로필 이미지 업로드 실패 (클라이언트 오류)")))
            // 5xx → 외부 서버 오류라면 APIException
            .onStatus(HttpStatusCode::is5xxServerError,
                resp -> Mono.error(
                    new APIException("외부 프로필 이미지 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
            // 성공 시 리턴 바디를 별도로 쓰지 않는다면 Mono<Void>
            .bodyToMono(OneIdImageResponse.class)
            // 블록해서 동기 호출로 전환
            .block();
    }

    /**
     * 비밀번호 재설정 인증번호 요청
     */
    public Mono<PasswordResetOneIdResponse> sendPasswordResetCert(PasswordResetRequestDTO req) {
        if (req.getPhoneNumber() == null) {
            throw new BadRequestException("휴대폰 번호가 필요합니다");
        }

        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedPhoneNumber = AES256.encrypt(req.getPhoneNumber(), timestamp);

        log.debug("비밀번호 재설정 인증번호 요청: phoneNumber={}", req.getPhoneNumber());

        return webClient.post()
                .uri(passwordResetCert, appType)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("phonenum", encodedPhoneNumber)
                        .with("nation", nationCode)
                        .with("timestamp", timestamp)
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        clientResp -> Mono.error(new BadRequestException("비밀번호 재설정 인증번호 요청 실패")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResp -> Mono.error(
                                new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
                .bodyToMono(PasswordResetOneIdResponse.class)
                .doOnNext(response -> log.debug("비밀번호 재설정 인증번호 요청 응답: {}", response))
                .doOnError(error -> log.error("비밀번호 재설정 인증번호 요청 오류: {}", error.getMessage()));
    }

    /**
     * 비밀번호 재설정 인증번호 확인
     */
    public Mono<PasswordResetVerifyResponse> verifyPasswordResetCert(PasswordResetRequestDTO req) {
        if (req.getPhoneNumber() == null || req.getVerificationCode() == null) {
            throw new BadRequestException("휴대폰 번호와 인증번호가 필요합니다");
        }

        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedPhoneNumber = AES256.encrypt(req.getPhoneNumber(), timestamp);

        log.debug("비밀번호 재설정 인증번호 확인: phoneNumber={}, code={}",
                req.getPhoneNumber(), req.getVerificationCode());

        return webClient.post()
                .uri(passwordResetVerify, appType)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("phonenum", encodedPhoneNumber)
                        .with("code", req.getVerificationCode())
                        .with("nation", nationCode)
                        .with("timestamp", timestamp)
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        clientResp -> Mono.error(new BadRequestException("인증번호 확인 실패")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResp -> Mono.error(
                                new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
                .bodyToMono(PasswordResetVerifyResponse.class)
                .doOnNext(response -> log.debug("비밀번호 재설정 인증번호 확인 응답: {}", response))
                .doOnError(error -> log.error("비밀번호 재설정 인증번호 확인 오류: {}", error.getMessage()));
    }

    /**
     * 비밀번호 재설정 실행
     */
    public Mono<PasswordResetVerifyResponse> resetPassword(PasswordResetRequestDTO req) {
        if (req.getPhoneNumber() == null || req.getNewPassword() == null || req.getVerificationCode() == null) {
            throw new BadRequestException("휴대폰 번호, 새 비밀번호, 인증번호가 모두 필요합니다");
        }

        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String encodedPhoneNumber = AES256.encrypt(req.getPhoneNumber(), timestamp);
        String encodedNewPassword = AES256.encrypt(req.getNewPassword(), timestamp);

        log.debug("비밀번호 재설정 실행: phoneNumber={}", req.getPhoneNumber());

        return webClient.post()
                .uri(passwordReset, appType)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("phonenum", encodedPhoneNumber)
                        .with("newpasswd", encodedNewPassword)
                        .with("nation", nationCode)
                        .with("code", req.getVerificationCode())
                        .with("timestamp", timestamp)
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        clientResp -> Mono.error(new BadRequestException("비밀번호 재설정 실패")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResp -> Mono.error(
                                new APIException("One-ID 서버 오류", ResponseStatus.INTERNAL_SERVER_ERROR)))
                .bodyToMono(PasswordResetVerifyResponse.class)
                .doOnNext(response -> log.debug("비밀번호 재설정 실행 응답: {}", response))
                .doOnError(error -> log.error("비밀번호 재설정 실행 오류: {}", error.getMessage()));
    }

}