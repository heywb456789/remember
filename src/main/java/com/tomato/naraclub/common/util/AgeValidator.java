package com.tomato.naraclub.common.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * 생년월일(yyyyMMdd)로부터 만 나이를 계산하여
 * 20세 이상 39세 이하인지 검증하는 로직
 */
public class AgeValidator {

    private static final DateTimeFormatter BIRTHDAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * @param birthdateStr "yyyyMMdd" 형식의 생년월일 문자열
     * @return 20 ≤ age ≤ 39 이면 true, 아니면 false
     */
    public static boolean isAgeBetween19And39(String birthdateStr) {
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(birthdateStr, BIRTHDAY_FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException("생년월일 형식이 올바르지 않습니다: " + birthdateStr, e);
        }

        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();

        return age >= 19 && age <= 39;
    }
}
