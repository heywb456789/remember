package com.tomato.remember.common.code;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@AllArgsConstructor
public enum ResponseStatus {
    OK("OK_0000", "정상 처리되었습니다.", HttpStatus.OK),
    DATA_NOT_FOUND("ER_0001", "데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NO_CHANGE_DATA("ER_0002", "수정된 데이터가 없습니다.", HttpStatus.NO_CONTENT),
    BAD_REQUEST("ER_0003", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNPROCESSABLE_ENTITY("ER_9998", "처리할 수 없는 요청입니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    INTERNAL_SERVER_ERROR("ER_9999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EXIST_USER("ER_1001", "이미 가입된 계정입니다.", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_USER("ER_1002", "탈퇴한 회원입니다. 재가입 해주세요.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ER_1003", "권한이 없는 사용자입니다. 고객센터에 문의 부탁드립니다.", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("ER_1004", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ID_PW("ER_1005", "아이디 비밀번호를 확인해주세요.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ROLE("ER_1006", "계정이 활성화되어 있지 않거나 권한이 없습니다.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ONE_ID("ER_1007", "원아이디 로그인 실패 ID / PW를 확인해주세요. ", HttpStatus.UNAUTHORIZED),
    ALREADY_MODIFIED_STATUS("ER_1008", "이미 변경된 상태 입니다.", HttpStatus.BAD_REQUEST),
    CANNOT_GRANT_SUPER_ADMIN("ER_1009", "슈퍼 관리자는 3명까지만 부여 가능합니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_GRANT_UPLOADER("ER_1010", "콘텐츠 관리자는 10명까지만 부여 가능합니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_GRANT_OPERATOR("ER_1011", "운영진은 10명까지만 부여 가능합니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    CANNOT_FIND_MEMORIAL("ER_2001", "메모리얼을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_ACCESS_MEMORIAL("ER_2002", "메모리얼에 접근할 권한이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_ACCESS_UPDATE_MEMORIAL("ER_2003", "메모리얼을 수정할 권한이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_ACCESS_DELETE_MEMORIAL("ER_2004", "메모리얼을 삭제할 권한이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_UPDATE_MEMORIAL_INFO("ER_2005", "AI 학습 중인 메모리얼의 미디어 파일은 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CANNOT_PERMANENT_DELETE_MEMORIAL("ER_2006", "메모리얼을 완전히 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_MEMORIAL_DURING_AI_TRAINING("ER_2007", "AI 학습 중인 메모리얼은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_MEMORIAL_WITH_FAMILY_MEMBERS("ER_2008", "가족 구성원이 있는 메모리얼은 삭제할 수 없습니다. 먼저 가족 구성원을 제거해주세요.",
        HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_MEMORIAL_WITH_MEMORIES("ER_2009", "저장된 메모리가 있는 메모리얼은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    BAD_REQUEST_MEMORIAL_BIRTH("ER_2010", "생일은 미래 날짜일 수 없습니다.", HttpStatus.BAD_REQUEST),
    BAD_REQUEST_MEMORIAL_DEATH("ER_2011", "기일은 미래 날짜일 수 없습니다.", HttpStatus.BAD_REQUEST),
    CONFIRM_TEXT_MISMATCH("ER_2012", "확인 텍스트가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_NOT_DELETED("ER_2013", "삭제되지 않은 메모리얼입니다.", HttpStatus.BAD_REQUEST),
    CANNOT_ACCESS_CHANGE_MEMORIAL_STATUS("ER_2014", "메모리얼 상태를 변경할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    MEMORIAL_ALREADY_DELETED("ER-2015", "이미 삭제된 메모리얼입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    MEMORIAL_STEP1_SAVE_FAILED("ER-2016", "STEP1 단계 저장 실패", HttpStatus.BAD_REQUEST),
    MEMORIAL_STEP1_DATA_NOT_FOUND("ER-2017", "1단계 데이터가 없습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_STEP2_SAVE_FAILED("ER-2018", "STEP2 단계 저장 실패", HttpStatus.BAD_REQUEST),
    MEMORIAL_STEP2_DATA_NOT_FOUND("ER-2019", "2단계 데이터가 없습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_STEP3_SAVE_FAILED("ER-2020", "STEP3 단계 저장 실패", HttpStatus.BAD_REQUEST),
    MEMORIAL_INCOMPLETE_DATA("ER-2021", "데이터 저장에 실패하였습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_TEMP_DATA_CLEAR_FAILED("ER-2023", "임시 데이터 클렌징에 실패하였습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_FILE_NOT_FOUND("ER-2024", "파일을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    FILE_COUNT_EXCEEDED("ER-2025", "파일 갯수 초과", HttpStatus.BAD_REQUEST),
    FILE_ACCESS_DENIED("ER-2026", "파일 접근 오류", HttpStatus.BAD_REQUEST),

    // 메모리얼 관련 에러 코드
    MEMORIAL_NOT_DRAFT("ER_M001", "메모리얼이 임시저장 상태가 아닙니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_INCOMPLETE_NAME("ER_M002", "메모리얼 이름이 입력되지 않았습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_INCOMPLETE_GENDER("ER_M003", "성별이 선택되지 않았습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_INCOMPLETE_RELATIONSHIP("ER_M004", "관계가 선택되지 않았습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_INCOMPLETE_FILES("ER_M005", "필수 파일이 업로드되지 않았습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_CREATION_FAILED("ER_M006", "메모리얼 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    MEMORIAL_UPDATE_FAILED("ER_M007", "메모리얼 업데이트에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    MEMORIAL_ALREADY_ACTIVE("ER_M008", "이미 활성화된 메모리얼입니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_ALREADY_COMPLETED("ER_M009", "이미 완료된 메모리얼입니다.", HttpStatus.BAD_REQUEST),

    // 파일 업로드 관련 에러 코드
    INVALID_FILE_TYPE("ER_F001", "지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("ER_F002", "파일 크기가 제한을 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED("ER_F003", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND("ER_F004", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FILE_DELETE_FAILED("ER_F005", "파일 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    TOO_MANY_FILES("ER_F006", "업로드 가능한 파일 개수를 초과했습니다.", HttpStatus.BAD_REQUEST),
    PROFILE_IMAGE_LIMIT_EXCEEDED("ER_F007", "프로필 이미지는 최대 5개까지 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
    VOICE_FILE_LIMIT_EXCEEDED("ER_F008", "음성 파일은 최대 3개까지 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
    VIDEO_FILE_LIMIT_EXCEEDED("ER_F009", "비디오 파일은 1개만 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
    USER_IMAGE_LIMIT_EXCEEDED("ER_F010", "사용자 이미지는 1개만 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
    FILE_EMPTY("ER_F011", "파일이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TYPE_NOT_SUPPORTED("ER_F012", "지원하지 않는 파일 타입입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 권한 관련 에러 코드
    MEMORIAL_ACCESS_DENIED("ER_A001", "메모리얼에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    MEMORIAL_EDIT_DENIED("ER_A002", "메모리얼을 수정할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    MEMORIAL_DELETE_DENIED("ER_A003", "메모리얼을 삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    MEMORIAL_OWNER_ONLY("ER_A004", "메모리얼 소유자만 수행할 수 있습니다.", HttpStatus.FORBIDDEN),

    // 상태 관련 에러 코드
    MEMORIAL_STATUS_INVALID("ER_S001", "메모리얼 상태가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_STATUS_CHANGE_DENIED("ER_S002", "현재 상태에서는 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_DRAFT_REQUIRED("ER_S003", "임시저장 상태의 메모리얼만 완료할 수 있습니다.", HttpStatus.BAD_REQUEST),

    // 데이터 검증 에러 코드
    MEMORIAL_NAME_REQUIRED("ER_V001", "메모리얼 이름은 필수 항목입니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_NAME_TOO_LONG("ER_V002", "메모리얼 이름이 너무 깁니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_DESCRIPTION_TOO_LONG("ER_V003", "메모리얼 설명이 너무 깁니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_GENDER_REQUIRED("ER_V004", "성별은 필수 항목입니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_RELATIONSHIP_REQUIRED("ER_V005", "관계는 필수 항목입니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_BIRTH_DATE_INVALID("ER_V006", "올바른 생년월일을 입력해주세요.", HttpStatus.BAD_REQUEST),
    MEMORIAL_DEATH_DATE_INVALID("ER_V007", "올바른 기일을 입력해주세요.", HttpStatus.BAD_REQUEST),
    MEMORIAL_DATE_RANGE_INVALID("ER_V008", "기일은 생년월일 이후여야 합니다.", HttpStatus.BAD_REQUEST),

    // 비즈니스 로직 에러 코드
    MEMORIAL_LIMIT_EXCEEDED("ER_B001", "생성 가능한 메모리얼 개수를 초과했습니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_ALREADY_EXISTS("ER_B002", "동일한 이름의 메모리얼이 이미 존재합니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_CREATION_IN_PROGRESS("ER_B003", "메모리얼 생성이 진행 중입니다.", HttpStatus.BAD_REQUEST),
    MEMORIAL_STEP_ORDER_INVALID("ER_B004", "메모리얼 생성 단계 순서가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    // AI 관련 에러 코드
    AI_TRAINING_NOT_READY("ER_AI001", "AI 학습 준비가 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),
    AI_TRAINING_IN_PROGRESS("ER_AI002", "AI 학습이 진행 중입니다.", HttpStatus.BAD_REQUEST),
    AI_TRAINING_FAILED("ER_AI003", "AI 학습에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_CALL_NOT_AVAILABLE("ER_AI004", "영상통화가 불가능한 상태입니다.", HttpStatus.BAD_REQUEST),


    USER_NOT_EXIST("ER_3001", "회원 정보를 찾을수가 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_ACTIVE("ER_3002", "인증된 회원이 아닙니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    VOTE_POST_NOT_EXIST("ER_4001", "투표 게시글이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VOTE_POST_OPTIONS_NOT_EXIST("ER_4002", "투표 옵션이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_VOTED("ER_4003", "이미 투표 처리가 완료되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    VIDEO_NOT_EXIST("ER_5001", "비디오가 존재하지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    ARTICLE_NOT_EXIST("ER_6001", "비디오가 존재하지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    THUMBNAIL_IS_NECESSARY("ER_6002", "썸네일은 필수 입니다.", HttpStatus.BAD_REQUEST),

    ACTIVITY_NOT_EXIST("ER_7001", "활동내역이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    ACTIVITY_IDS_NOT_EXIST("ER_7002", "승인할 활동 ID 목록이 비어있습니다.", HttpStatus.BAD_REQUEST),
    ACTIVITY_POINT_TYPE_NOT_EXIST("ER_7003", "잘못 된 포인트 타입", HttpStatus.BAD_REQUEST),
    ACTIVITY_PROCESS_LIST_NOT_EXIST("ER_7004", "처리 가능한 활동이 없습니다.", HttpStatus.BAD_REQUEST),

    TWITTER_NOT_FOUND("ER_8001", "X(구 트위터) 계정 연동을 진행해주세요!", HttpStatus.INTERNAL_SERVER_ERROR),
    DUPLICATE_POST("ER_8002", "중복된 내용의 트윗은 공유할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_TWEET("ER_8003", "트위터 발행 권한이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FAIL_TWEET("ER_8004", "트위터 공유에 실패 하였습니다. 문제가 계속 된다면 관리자에게 문의해주세요.", HttpStatus.INTERNAL_SERVER_ERROR),

    ALREADY_EXIST_URL("ER_9001", "이미 존재하는 활동 링크입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    FILE_UPLOAD_FAIL("ER_B001", "파일 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR),

    // 가족 초대 관련 에러 코드 (ER_FI로 시작)
    FAMILY_INVITE_EMAIL_SENT("OK_0000", "이메일로 초대 링크가 발송되었습니다.", HttpStatus.OK),
    FAMILY_INVITE_SMS_READY("OK_0000", "SMS 초대 준비가 완료되었습니다.", HttpStatus.OK),
    FAMILY_INVITE_TOKEN_CREATED("OK_0000", "초대 토큰이 생성되었습니다.", HttpStatus.OK),
    FAMILY_INVITE_ACCEPTED("OK_0000", "가족 초대가 수락되었습니다.", HttpStatus.OK),

    // 가족 초대 에러 코드들
    FAMILY_INVITE_INVALID_CONTACT("ER_FI001", "유효하지 않은 연락처입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_SELF_INVITE("ER_FI002", "자기 자신을 초대할 수 없습니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_ALREADY_MEMBER("ER_FI003", "이미 가족 구성원으로 등록된 연락처입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_DUPLICATE_PENDING("ER_FI004", "이미 처리되지 않은 초대가 있습니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_TOKEN_INVALID("ER_FI005", "유효하지 않거나 만료된 초대 토큰입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_TOKEN_EXPIRED("ER_FI006", "만료된 초대 토큰입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_METHOD_NOT_SUPPORTED("ER_FI007", "지원하지 않는 초대 방법입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_EMAIL_FAILED("ER_FI008", "이메일 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FAMILY_INVITE_SMS_FAILED("ER_FI009", "SMS 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FAMILY_INVITE_PHONE_INVALID("ER_FI010", "유효하지 않은 전화번호 형식입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_EMAIL_INVALID("ER_FI011", "유효하지 않은 이메일 형식입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_OWNER_SELF_INVITE("ER_FI012", "메모리얼 소유자는 자기 자신을 초대할 수 없습니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_ALREADY_ACCEPTED("ER_FI013", "이미 수락된 초대입니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_NOT_SMS_METHOD("ER_FI014", "SMS 방식의 초대가 아닙니다.", HttpStatus.BAD_REQUEST),
    FAMILY_INVITE_CONTACT_VALIDATION_FAILED("ER_FI015", "연락처 유효성 검사에 실패했습니다.", HttpStatus.BAD_REQUEST),
    ;
    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    private final HttpStatus httpStatus;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static ResponseStatus forValues(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message
    ) {
        return Arrays.stream(values())
            .filter(s -> s.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown code: " + code));
    }

    public static ResponseStatus of(String code) {
        return Arrays.stream(values())
            .filter(s -> s.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid code: " + code));
    }
}

