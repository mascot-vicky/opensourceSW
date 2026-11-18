package com.suwontrip.app.auth.support;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTool {
    public static void main(String[] args) {

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("입력을 숨길 수 있는 대화형 터미널에서 실행해 주세요.");
        }

        char[] password = console.readPassword("시연 계정 비밀번호: ");
        if (password == null) {
            throw new IllegalArgumentException("비밀번호 입력이 취소됐어요.");
        }

        try {
            String value = new String(password);
            if (value.isEmpty() || value.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("비밀번호는 UTF-8 기준 1~72바이트여야 해요.");
            }

            console.printf("%s%n", new BCryptPasswordEncoder().encode(value));
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
