package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.AuthDtos.AuthResponse;
import com.example.hubdemo.dto.AuthDtos.LoginRequest;
import com.example.hubdemo.dto.AuthDtos.RegisterRequest;
import com.example.hubdemo.dto.AuthDtos.ResetPasswordRequest;
import com.example.hubdemo.dto.AuthDtos.VerifyResetCodeRequest;
import com.example.hubdemo.dto.AuthDtos.VerifyResetCodeResponse;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.JwtUtil;
import com.example.hubdemo.util.PasswordUtil;
import com.example.hubdemo.util.UsernameUtil;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    private static final String COMBO_REVIEW_PERMISSION = "combo_review";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;
    private final Map<String, ResetTokenEntry> resetTokens = new ConcurrentHashMap<>();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, EmailVerificationService emailVerificationService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.emailVerificationService = emailVerificationService;
    }

    public void sendPasswordResetCode(String email) {
        emailVerificationService.sendPasswordResetCode(normalizeEmail(email));
    }

    public AuthResponse register(RegisterRequest request) {
        String username = UsernameUtil.normalize(request.username());
        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            throw new BizException("用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BizException("邮箱已注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(request.password()));
        user.setRole("user");
        user.setTokenVersion(0L);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = authenticate(request);
        markSuccessfulLogin(user);
        return toAuthResponse(user);
    }

    public AuthResponse adminLogin(LoginRequest request) {
        User user = authenticate(request);
        if (!hasAdminAccess(user)) {
            throw new BizException("仅允许后台管理员登录");
        }
        markSuccessfulLogin(user);
        return toAuthResponse(user);
    }

    private User authenticate(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null || !PasswordUtil.matches(request.password(), user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        if (Boolean.TRUE.equals(user.getBanned()) && user.getBannedUntil() != null && user.getBannedUntil().isBefore(LocalDateTime.now())) {
            userMapper.clearExpiredBan(user.getId(), LocalDateTime.now());
            user = userMapper.selectById(user.getId());
            if (user == null) {
                throw new BizException("用户不存在");
            }
        }
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new BizException("账号已被封禁，请联系管理员");
        }
        return user;
    }

    private void markSuccessfulLogin(User user) {
        LocalDateTime lastLoginAt = LocalDateTime.now();
        user.setLastLoginAt(lastLoginAt);
        userMapper.updateLastLoginAt(user.getId(), lastLoginAt);
    }

    public VerifyResetCodeResponse verifyPasswordResetCode(VerifyResetCodeRequest request) {
        emailVerificationService.verifyPasswordReset(request.email(), request.code());
        User user = findByEmail(request.email());
        String token = createResetToken();
        resetTokens.put(normalizeEmail(request.email()), new ResetTokenEntry(user.getId(), token, LocalDateTime.now().plus(RESET_TOKEN_TTL)));
        return new VerifyResetCodeResponse(token);
    }

    public void resetPassword(ResetPasswordRequest request) {
        validatePassword(request.password());
        String normalizedEmail = normalizeEmail(request.email());
        ResetTokenEntry entry = resetTokens.get(normalizedEmail);
        if (entry == null || !entry.token().equals(request.resetToken())) {
            throw new BizException("密码重置凭证无效，请重新验证邮箱");
        }
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            resetTokens.remove(normalizedEmail);
            throw new BizException("密码重置凭证已过期，请重新验证邮箱");
        }
        User user = findByEmail(request.email());
        if (!entry.userId().equals(user.getId())) {
            throw new BizException("密码重置凭证无效，请重新验证邮箱");
        }
        userMapper.updatePasswordAndInvalidateSessions(user.getId(), PasswordUtil.hash(request.password()));
        resetTokens.remove(normalizedEmail);
    }

    public AuthResponse refresh(User user) {
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                jwtUtil.createToken(user.getId(), user.getUsername(), user.getTokenVersion()),
                user.getRole(),
                user.getAvatar(),
                user.getAdminPermissions()
        );
    }

    private static boolean hasAdminAccess(User user) {
        if ("admin".equals(user.getRole())) {
            return true;
        }
        return StringUtils.hasText(user.getAdminPermissions())
                && List.of(user.getAdminPermissions().split(",")).stream()
                .map(String::trim)
                .anyMatch(COMBO_REVIEW_PERMISSION::equals);
    }

    private User findByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, normalizeEmail(email)));
        if (user == null) {
            throw new BizException("该邮箱未注册");
        }
        return user;
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String createResetToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 128) {
            throw new BizException("密码长度需为 8-128 个字符");
        }
        String normalized = password.toLowerCase(Locale.ROOT);
        if (List.of("password", "12345678", "qwerty123", "11111111").contains(normalized)) {
            throw new BizException("密码过于简单");
        }
    }

    private record ResetTokenEntry(Long userId, String token, LocalDateTime expiresAt) {
    }

    @Scheduled(fixedDelayString = "${app.auth-state-cleanup-ms:300000}")
    public void removeExpiredAuthState() {
        LocalDateTime now = LocalDateTime.now();
        resetTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

}
