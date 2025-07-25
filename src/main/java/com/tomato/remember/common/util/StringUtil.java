package com.tomato.remember.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 문자열 관련 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 인스턴스 생성 방지
public class StringUtil {

    /**
     * 토큰 마스킹
     * @param token 원본 토큰
     * @return 마스킹된 토큰 (앞 8자리 + "...")
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 8) + "...";
    }

    /**
     * 연락처 마스킹 (이메일/전화번호 자동 판별)
     * @param contact 원본 연락처
     * @return 마스킹된 연락처
     */
    public static String maskContact(String contact) {
        if (contact == null || contact.length() < 4) {
            return "****";
        }

        if (contact.contains("@")) {
            return maskEmail(contact);
        } else {
            return maskPhoneNumber(contact);
        }
    }

    /**
     * 이메일 마스킹
     * @param email 원본 이메일
     * @return 마스킹된 이메일 (예: te****@example.com)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "**" + email.substring(atIndex);
        }

        return email.substring(0, 2) + "****" + email.substring(atIndex);
    }

    /**
     * 전화번호 마스킹
     * @param phoneNumber 원본 전화번호
     * @return 마스킹된 전화번호 (예: 010-****-5678)
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }

        // 하이픈이 있는 경우
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }

        // 하이픈이 없는 경우
        if (phoneNumber.length() >= 8) {
            return phoneNumber.substring(0, 3) + "****" +
                   phoneNumber.substring(phoneNumber.length() - 4);
        }

        return phoneNumber.substring(0, 3) + "****";
    }

    /**
     * 문자열이 비어있는지 확인 (null, 빈 문자열, 공백만 있는 경우)
     * @param str 확인할 문자열
     * @return 비어있으면 true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 문자열이 비어있지 않은지 확인
     * @param str 확인할 문자열
     * @return 비어있지 않으면 true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 문자열을 지정된 길이로 자르기 (말줄임표 추가)
     * @param str 원본 문자열
     * @param maxLength 최대 길이
     * @return 잘린 문자열
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || maxLength <= 0) {
            return "";
        }

        if (str.length() <= maxLength) {
            return str;
        }

        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 카멜케이스를 스네이크케이스로 변환
     * @param camelCase 카멜케이스 문자열
     * @return 스네이크케이스 문자열
     */
    public static String camelToSnake(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }

        return camelCase
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }

    /**
     * 스네이크케이스를 카멜케이스로 변환
     * @param snakeCase 스네이크케이스 문자열
     * @return 카멜케이스 문자열
     */
    public static String snakeToCamel(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = snakeCase.split("_");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                result.append(part.toLowerCase());
            } else {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    /**
     * 랜덤 문자열 생성
     * @param length 길이
     * @return 랜덤 문자열 (영문 대소문자 + 숫자)
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            result.append(chars.charAt(index));
        }

        return result.toString();
    }

    /**
     * 바이트 크기를 읽기 쉬운 형태로 변환
     * @param bytes 바이트 크기
     * @return 읽기 쉬운 형태 (예: 1.5 MB)
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        return String.format("%.1f %s",
                bytes / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    /**
     * JSON 문자열 검증
     * @param jsonString JSON 문자열
     * @return 유효한 JSON이면 true
     */
    public static boolean isValidJson(String jsonString) {
        if (isEmpty(jsonString)) {
            return false;
        }

        try {
            // 간단한 JSON 검증 (실제로는 ObjectMapper 사용 권장)
            jsonString = jsonString.trim();
            return (jsonString.startsWith("{") && jsonString.endsWith("}")) ||
                   (jsonString.startsWith("[") && jsonString.endsWith("]"));
        } catch (Exception e) {
            return false;
        }
    }
}