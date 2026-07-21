package com.example.hubdemo.controller;

import com.example.hubdemo.dto.AuthDtos.AuthResponse;
import com.example.hubdemo.dto.AuthDtos.LoginRequest;
import com.example.hubdemo.dto.AuthDtos.RegisterRequest;
import com.example.hubdemo.dto.AuthDtos.ResetPasswordRequest;
import com.example.hubdemo.dto.AuthDtos.SendCodeRequest;
import com.example.hubdemo.dto.AuthDtos.VerifyResetCodeRequest;
import com.example.hubdemo.dto.AuthDtos.VerifyResetCodeResponse;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.service.AuthCookieService;
import com.example.hubdemo.service.AuthService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.CsrfTokenService;
import com.example.hubdemo.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final CsrfTokenService csrfTokenService;
    private final CurrentUserService currentUserService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, AuthCookieService authCookieService, CsrfTokenService csrfTokenService, CurrentUserService currentUserService,
                          RateLimitService rateLimitService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.csrfTokenService = csrfTokenService;
        this.currentUserService = currentUserService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/password-reset/send-code")
    public Map<String, String> sendPasswordResetCode(@Valid @RequestBody SendCodeRequest request,
                                                     HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "auth:password-reset-code", 5, Duration.ofMinutes(10));
        authService.sendPasswordResetCode(request.email());
        return Map.of("message", "密码重置验证码已发送到你的邮箱");
    }

    @PostMapping("/password-reset/verify-code")
    public VerifyResetCodeResponse verifyPasswordResetCode(@Valid @RequestBody VerifyResetCodeRequest request,
                                                            HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "auth:password-reset-verify", 10, Duration.ofMinutes(10));
        return authService.verifyPasswordResetCode(request);
    }

    @PostMapping("/password-reset/reset")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                             HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "auth:password-reset", 10, Duration.ofMinutes(10));
        authService.resetPassword(request);
        return Map.of("message", "密码已重置，请使用新密码登录");
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletRequest servletRequest,
                                 HttpServletResponse response) {
        rateLimitService.check(servletRequest, "auth:register", 10, Duration.ofMinutes(10));
        return withAuthCookie(response, authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest,
                              HttpServletResponse response) {
        rateLimitService.check(servletRequest, "auth:login", 20, Duration.ofMinutes(10));
        return withAuthCookie(response, authService.login(request));
    }

    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest servletRequest,
                                   HttpServletResponse response) {
        rateLimitService.check(servletRequest, "auth:admin-login", 20, Duration.ofMinutes(10));
        return withAdminAuthCookie(response, authService.adminLogin(request));
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh(HttpServletRequest request, HttpServletResponse response) {
        User user = currentUserService.from(request).orElse(null);
        if (user == null) {
            authCookieService.clearAuthCookie(response);
            return Map.of("token", "");
        }
        AuthResponse authResponse = authService.refresh(user);
        authCookieService.setAuthCookie(response, authResponse.token());
        return Map.of("token", "");
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
        csrfTokenService.clear(response);
        return Map.of("message", "已退出登录");
    }

    @PostMapping("/admin/logout")
    public Map<String, String> adminLogout(HttpServletResponse response) {
        authCookieService.clearAdminAuthCookie(response);
        return Map.of("message", "已退出后台登录");
    }

    private AuthResponse withAuthCookie(HttpServletResponse response, AuthResponse authResponse) {
        authCookieService.setAuthCookie(response, authResponse.token());
        csrfTokenService.rotate(response);
        return new AuthResponse(
                authResponse.id(),
                authResponse.username(),
                authResponse.email(),
                "",
                authResponse.role(),
                authResponse.avatar(),
                authResponse.adminPermissions()
        );
    }

    private AuthResponse withAdminAuthCookie(HttpServletResponse response, AuthResponse authResponse) {
        authCookieService.setAdminAuthCookie(response, authResponse.token());
        csrfTokenService.rotate(response);
        return new AuthResponse(
                authResponse.id(),
                authResponse.username(),
                authResponse.email(),
                "",
                authResponse.role(),
                authResponse.avatar(),
                authResponse.adminPermissions()
        );
    }
}
