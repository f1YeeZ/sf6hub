package com.example.hubdemo.service;

import com.example.hubdemo.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_VERIFY_FAILURES = 5;
    private static final int MAX_ACTIVE_CODES = 100_000;
    private static final String PURPOSE_PASSWORD_RESET = "password-reset";
    private static final String PURPOSE_PROFILE_CURRENT_EMAIL = "profile-current-email";
    private static final String PURPOSE_PROFILE_NEW_EMAIL = "profile-new-email";
    private static final String PURPOSE_PROFILE_PASSWORD = "profile-password";

    private final JavaMailSender mailSender;
    private final String from;
    private final Duration ttl;
    private final Duration cooldown;
    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final Object[] sendLocks = new Object[64];

    public EmailVerificationService(JavaMailSender mailSender,
                                    @Value("${app.mail.from:${spring.mail.username}}") String from,
                                    @Value("${app.verification-code.ttl-minutes:10}") long ttlMinutes,
                                    @Value("${app.verification-code.cooldown-seconds:60}") long cooldownSeconds) {
        this.mailSender = mailSender;
        this.from = from;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
        Arrays.setAll(sendLocks, ignored -> new Object());
    }

    public void sendPasswordResetCode(String email) {
        sendCode(email, PURPOSE_PASSWORD_RESET, "Combos Hub 密码重置验证码", "你的密码重置验证码是：%s");
    }

    public void sendProfileCurrentEmailCode(String email) {
        sendCode(email, PURPOSE_PROFILE_CURRENT_EMAIL, "Combos Hub 当前邮箱验证", "你的当前邮箱验证码是：%s");
    }

    public void sendProfileNewEmailCode(String email) {
        sendCode(email, PURPOSE_PROFILE_NEW_EMAIL, "Combos Hub 新邮箱验证", "你的新邮箱验证码是：%s");
    }

    public void sendProfilePasswordCode(String email) {
        sendCode(email, PURPOSE_PROFILE_PASSWORD, "Combos Hub 密码修改验证", "你的密码修改验证码是：%s");
    }

    private void sendCode(String email, String purpose, String subject, String codeLineTemplate) {
        String normalizedEmail = normalizeEmail(email);
        String codeKey = codeKey(normalizedEmail, purpose);
        synchronized (sendLocks[Math.floorMod(codeKey.hashCode(), sendLocks.length)]) {
            sendCodeLocked(normalizedEmail, codeKey, subject, codeLineTemplate);
        }
    }

    private void sendCodeLocked(String normalizedEmail, String codeKey, String subject, String codeLineTemplate) {
        CodeEntry existing = codes.get(codeKey);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null && existing.sentAt().plus(cooldown).isAfter(now)) {
            throw new BizException("验证码发送太频繁，请稍后再试");
        }
        if (codes.size() >= MAX_ACTIVE_CODES) {
            removeExpiredCodes();
            if (codes.size() >= MAX_ACTIVE_CODES) {
                throw new BizException("验证码服务繁忙，请稍后再试");
            }
        }

        String code = createCode();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(normalizedEmail);
        message.setSubject(subject);
        message.setText("""
                %s

                验证码 %d 分钟内有效。若不是你本人操作，请忽略此邮件。
                """.formatted(codeLineTemplate.formatted(code), ttl.toMinutes()));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BizException("验证码邮件发送失败，请检查 QQ 邮箱账号、授权码和 SMTP 设置");
        }

        codes.put(codeKey, new CodeEntry(code, now, now.plus(ttl)));
    }

    public void verifyPasswordReset(String email, String code) {
        verify(email, code, PURPOSE_PASSWORD_RESET);
    }

    @Scheduled(fixedDelayString = "${app.verification-code.cleanup-ms:300000}")
    public void removeExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public void verifyProfileCurrentEmail(String email, String code) {
        verify(email, code, PURPOSE_PROFILE_CURRENT_EMAIL);
    }

    public void verifyProfileNewEmail(String email, String code) {
        verify(email, code, PURPOSE_PROFILE_NEW_EMAIL);
    }

    public void verifyProfilePassword(String email, String code) {
        verify(email, code, PURPOSE_PROFILE_PASSWORD);
    }

    private void verify(String email, String code, String purpose) {
        String normalizedEmail = normalizeEmail(email);
        String codeKey = codeKey(normalizedEmail, purpose);
        CodeEntry entry = codes.get(codeKey);
        if (entry == null) {
            throw new BizException("请先获取邮箱验证码");
        }
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            codes.remove(codeKey);
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!entry.code().equals(code)) {
            if (entry.failures() + 1 >= MAX_VERIFY_FAILURES) {
                codes.remove(codeKey);
                throw new BizException("验证码错误次数过多，请重新获取");
            }
            codes.put(codeKey, entry.withFailure());
            throw new BizException("验证码错误");
        }
        codes.remove(codeKey);
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String createCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String codeKey(String email, String purpose) {
        return purpose + ":" + email;
    }

    private record CodeEntry(String code, LocalDateTime sentAt, LocalDateTime expiresAt, int failures) {
        CodeEntry(String code, LocalDateTime sentAt, LocalDateTime expiresAt) {
            this(code, sentAt, expiresAt, 0);
        }

        CodeEntry withFailure() {
            return new CodeEntry(code, sentAt, expiresAt, failures + 1);
        }
    }
}
