package com.tomato.naraclub.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    /**
     * startDate ~ endDate 기간 중, endDate까지 남은 일수의 퍼센테이지를 구합니다.
     *
     * @param startDate 기간 시작
     * @param endDate   기간 종료
     * @return 남은 일수 비율 (0.0 ~ 100.0)
     */
    public static double remainingDaysPercentage(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 총 기간(초)
        long totalSeconds = ChronoUnit.SECONDS.between(startDate, endDate);
        if (totalSeconds <= 0) {
            return 0.0; // 이미 종료된 경우
        }

        // 2) 남은 기간(초)
        long remainingSeconds;
        if (now.isBefore(startDate)) {
            // 아직 시작 전이면 100%
            return 100.0;
        } else if (now.isAfter(endDate)) {
            // 이미 종료된 경우
            return 0.0;
        } else {
            remainingSeconds = ChronoUnit.SECONDS.between(now, endDate);
        }

        // 3) 퍼센테이지 계산
        double percent = (double) remainingSeconds * 100.0 / totalSeconds;

        // 4) 남은일 기준 몇퍼 남았는지
        return Math.round(percent * 100.0) / 100.0;
    }

    public static double progressDaysPercentage(LocalDateTime start, LocalDateTime end) {
        LocalDateTime today = LocalDateTime.now();

        if (today.isBefore(start)) {
            // 아직 시작 전이면 0%
            return 0.0;
        }
        if (!today.isBefore(end)) {
            // 종료일 이후면 100%
            return 100.0;
        }

        long totalDays = ChronoUnit.DAYS.between(start, end);
        long elapsedDays = ChronoUnit.DAYS.between(start, today);

        double pct = elapsedDays * 100.0 / totalDays;

        return Math.round(pct * 100.0) / 100.0;
    }

    public static double progressMillsPercentage(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(start)) {
            return 0.0;
        }
        if (!now.isBefore(end)) {
            return 100.0;
        }

        // 전체 구간과 경과 구간을 밀리초 단위로 계산
        long totalMillis = Duration.between(start, end).toMillis();
        long elapsedMillis = Duration.between(start, now).toMillis();

        double pct = elapsedMillis * 100.0 / totalMillis;
        // 소수점 둘째 자리에서 반올림
        return Math.round(pct * 100.0) / 100.0;
    }

    /**
     * 잘못된 한글 날짜 형식을 정상적인 형식으로 변환
     * 예: "2025-5월5월-1010 ~ 2025-5월5월-1212" → "2025-05-10 ~ 2025-05-12"
     */
    public static String normalizeKoreanDateFormat(String dateRange) {
        if (dateRange == null) {
            return "";
        }

        try {
            // "yyyy-M월M월-DD" 패턴을 찾는 정규식
            Pattern pattern = Pattern.compile("(\\d{4})-(\\d{1,2})월\\2월-(\\d{1,2})(\\d{1,2})");
            Matcher matcher = pattern.matcher(dateRange);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String year = matcher.group(1);
                String month = String.format("%02d", Integer.parseInt(matcher.group(2)));
                String day = String.format("%02d", Integer.parseInt(matcher.group(3) + matcher.group(4)));
                matcher.appendReplacement(sb, year + "-" + month + "-" + day);
            }
            matcher.appendTail(sb);

            return sb.toString();
        } catch (Exception e) {
            return dateRange; // 오류 시 원본 반환
        }
    }

    /**
     * 다양한 형식의 날짜 문자열을 LocalDateTime으로 파싱
     */
    public static LocalDateTime parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // 정규식으로 숫자만 추출
        String digitsOnly = dateStr.replaceAll("[^0-9]", "");

        // 길이에 따라 다른 파싱 방법 적용
        if (digitsOnly.length() >= 8) { // yyyyMMdd 이상
            int year = Integer.parseInt(digitsOnly.substring(0, 4));
            int month = Integer.parseInt(digitsOnly.substring(4, 6));
            int day = Integer.parseInt(digitsOnly.substring(6, 8));

            // 유효성 검사
            if (year >= 1900 && year <= 2100 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return LocalDateTime.of(year, month, day, 0, 0);
            }
        }

        // 실패 시 null 반환
        return null;
    }
}
