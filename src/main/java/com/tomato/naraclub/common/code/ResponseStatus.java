package com.tomato.naraclub.common.code;

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

  BOARD_POST_NOT_EXIST("ER_2001", "게시글이 없습니다. 잘못 된 요청입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ALREADY_LIKED("ER_2002","이미 좋아요 처리가 되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),
  ALREADY_DELETED_LIKE("ER_2003","좋아요 취소 처리가 이미 완료되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),


  USER_NOT_EXIST("ER_3001", "회원 정보를 찾을수가 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  USER_NOT_ACTIVE("ER_3002", "인증된 회원이 아닙니다.", HttpStatus.INTERNAL_SERVER_ERROR),

  VOTE_POST_NOT_EXIST("ER_4001", "투표 게시글이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  VOTE_POST_OPTIONS_NOT_EXIST("ER_4002", "투표 옵션이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ALREADY_VOTED("ER_4003","이미 투표 처리가 완료되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),

  VIDEO_NOT_EXIST("ER_5001","비디오가 존재하지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),

  ARTICLE_NOT_EXIST("ER_6001","비디오가 존재하지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),
  THUMBNAIL_IS_NECESSARY("ER_6002","썸네일은 필수 입니다.", HttpStatus.BAD_REQUEST ),

  ACTIVITY_NOT_EXIST("ER_7001","활동내역이 존재하지 않습니다.", HttpStatus.BAD_REQUEST ),
  ACTIVITY_IDS_NOT_EXIST("ER_7002","승인할 활동 ID 목록이 비어있습니다.", HttpStatus.BAD_REQUEST ),
  ACTIVITY_POINT_TYPE_NOT_EXIST("ER_7003","잘못 된 포인트 타입", HttpStatus.BAD_REQUEST ),
  ACTIVITY_PROCESS_LIST_NOT_EXIST("ER_7004","처리 가능한 활동이 없습니다.", HttpStatus.BAD_REQUEST ),

  TWITTER_NOT_FOUND("ER_8001","X(구 트위터) 계정 연동을 진행해주세요!", HttpStatus.INTERNAL_SERVER_ERROR ),
  DUPLICATE_POST("ER_8002","중복된 내용의 트윗은 공유할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),
  CANNOT_TWEET("ER_8003","트위터 발행 권한이 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),
  FAIL_TWEET("ER_8004","트위터 공유에 실패 하였습니다. 문제가 계속 된다면 관리자에게 문의해주세요.", HttpStatus.INTERNAL_SERVER_ERROR ),

  ALREADY_EXIST_URL("ER_9001","이미 존재하는 활동 링크입니다.", HttpStatus.INTERNAL_SERVER_ERROR ),

  CANNOT_EXCHANGE_TTR("ER_A001","현재 교환이 불가 합니다. 관리자에게 문의해 주세요. CODE_3469", HttpStatus.INTERNAL_SERVER_ERROR ),
  CANNOT_FIND_BALANCE("ER_A002","현재 교환이 불가 합니다. 관리자에게 문의해 주세요. CODE_3470", HttpStatus.INTERNAL_SERVER_ERROR ),
  CANNOT_FIND_WALLET("ER_A003","지갑 정보를 찾을수 없습니다. 문제가 지속될 경우 관리자에게 문의해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR ),
  TTR_BAD_REQUEST("ER_A004","현재 교환이 불가 합니다. 관리자에게 문의해 주세요. CODE_3471", HttpStatus.BAD_REQUEST ),
  TTR_CONNECTION_FAIL("ER_A005","현재 교환이 불가 합니다. 관리자에게 문의해 주세요. CODE_3472", HttpStatus.INTERNAL_SERVER_ERROR ),
  TTR_EXCHANGE_FAIL("ER_A006","현재 교환이 불가 합니다. 관리자에게 문의해 주세요. CODE_3473", HttpStatus.INTERNAL_SERVER_ERROR ),
  TTR_EXCHANGE_ZERO_VALUE("ER_A007","교환가능한 TTR이 없습니다. CODE_3474", HttpStatus.INTERNAL_SERVER_ERROR ),

  FILE_UPLOAD_FAIL("ER_B001","파일 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR ),

  SUBSCRIPTION_NOT_EXIST("ER_C001","구독 정보가 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR ),

  NICE_CRYPTO_TOKEN_FAIL("ER_D001","NICE 인증 실패 CODE_4001", HttpStatus.INTERNAL_SERVER_ERROR ),
  NICE_CRYPTO_TOKEN_REQUEST_FAIL("ER_D002","NICE 인증 실패 CODE_4002", HttpStatus.INTERNAL_SERVER_ERROR ),
  NICE_CRYPTO_TOKEN_REQUEST_RESPONSE_FAIL("ER_D003","NICE 인증 실패 CODE_4003", HttpStatus.INTERNAL_SERVER_ERROR ),
  NICE_ACCESS_TOKEN_REQUEST_FAIL("ER_D004","NICE 인증 실패 CODE_4004", HttpStatus.INTERNAL_SERVER_ERROR ),
  NICE_ACCESS_TOKEN_REQUEST_RESPONSE_FAIL("ER_D005","NICE 인증 실패 CODE_4005", HttpStatus.INTERNAL_SERVER_ERROR ),
  NICE_CRYPTO_TOKEN_INFO_NOT_FOUND("ER_D006","NICE 인증 실패 CODE_4006", HttpStatus.BAD_REQUEST),
  NICE_INTEGRITY_CHECK_FAIL("ER_D007","NICE 인증 실패 CODE_4007", HttpStatus.BAD_REQUEST),
  NICE_DECRYPTION_FAIL("ER_D008","NICE 인증 실패 CODE_4008", HttpStatus.INTERNAL_SERVER_ERROR),
  NICE_PASS_RESULT_FAIL("ER_D009","NICE 인증 실패 CODE_4009", HttpStatus.INTERNAL_SERVER_ERROR),
  NICE_PASS_BIRTH_DAY_FAIL("ER_D010","만 20세 이상 39세 이하만 가입 가능합니다.", HttpStatus.BAD_REQUEST),

  INVALID_YOUTUBE_ID("ER_E001","유효하지 않은 YouTube URL 또는 ID입니다.", HttpStatus.BAD_REQUEST),
  YOUTUBE_UPLOAD_FAIL("ER_E002","YouTube 비디오 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),


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

