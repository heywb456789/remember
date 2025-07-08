package com.tomato.naraclub.common.util;

import com.tomato.naraclub.application.member.repository.MemberRepository;
import java.security.SecureRandom;

public class InviteCodeGenerator {
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RNG = new SecureRandom();
    private static final int LENGTH = 7;

    /** 한 번에 임의의 7자리 코드를 생성합니다. */
    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARSET.charAt(RNG.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    /**
     * DB에 중복이 없도록, 반복해서 고유 값을 생성합니다.
     */
    public static String generateUnique(MemberRepository repo) {
        String code;
        do {
            code = generate();
        } while (repo.existsByInviteCode(code));
        return code;
    }
}
