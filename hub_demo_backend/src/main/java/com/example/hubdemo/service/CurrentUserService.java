package com.example.hubdemo.service;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AuthCookieService authCookieService;

    public CurrentUserService(UserMapper userMapper, JwtUtil jwtUtil, AuthCookieService authCookieService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.authCookieService = authCookieService;
    }

    public Optional<User> from(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            authorization = authCookieService.readAuthCookie(request);
        }
        return fromToken(authorization);
    }

    public Optional<User> fromAdmin(HttpServletRequest request) {
        return fromToken(authCookieService.readAdminAuthCookie(request));
    }

    public Optional<User> fromToken(String token) {
        return jwtUtil.parseClaims(token).flatMap(claims -> {
            User user = userMapper.selectById(claims.userId());
            long currentVersion = user == null || user.getTokenVersion() == null ? 0L : user.getTokenVersion();
            return user != null && currentVersion == claims.tokenVersion() ? Optional.of(user) : Optional.empty();
        });
    }

    public Optional<User> fromUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    public User require(HttpServletRequest request) {
        User user = from(request).orElseThrow(() -> new BizException("请先登录"));
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new BizException("账号已被封禁");
        }
        return user;
    }

    public String usernameOrDefault(HttpServletRequest request, String fallback) {
        return from(request).map(User::getUsername).orElse(fallback);
    }

    public User requireAdminSession(HttpServletRequest request) {
        User user = fromAdmin(request).orElseThrow(() -> new BizException("请先登录后台"));
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new BizException("账号已被封禁");
        }
        return user;
    }

    public Long userIdOrNull(HttpServletRequest request) {
        return from(request).map(User::getId).orElse(null);
    }
}
