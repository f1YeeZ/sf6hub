package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.AuthDtos.UpdateProfileEmailRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfilePasswordRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfileUsernameRequest;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.PasswordUtil;
import com.example.hubdemo.util.UsernameUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;


@Service
public class UserService {
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;

    public UserService(UserMapper userMapper, EmailVerificationService emailVerificationService) {
        this.userMapper = userMapper;
        this.emailVerificationService = emailVerificationService;
    }

    public User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    public User privateProfile(User user) {
        if (user == null) {
            throw new BizException("用户名已存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    public User updateUsername(User currentUser, UpdateProfileUsernameRequest request) {
        User user = requireUser(currentUser.getId());
        String username = UsernameUtil.normalize(request.username());
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new BizException("用户名已存在");
        }
        userMapper.updateUsername(user.getId(), username);
        return privateProfile(requireUser(user.getId()));
    }

    public User updateEmail(User currentUser, UpdateProfileEmailRequest request) {
        User user = requireUser(currentUser.getId());
        String currentEmail = normalizeEmail(user.getEmail());
        String newEmail = normalizeEmail(request.newEmail());
        if (currentEmail.equals(newEmail)) {
            throw new BizException("新邮箱不能与当前邮箱相同");
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, newEmail));
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new BizException("邮箱已注册");
        }
        emailVerificationService.verifyProfileCurrentEmail(currentEmail, request.currentEmailCode());
        emailVerificationService.verifyProfileNewEmail(newEmail, request.newEmailCode());
        userMapper.updateEmail(user.getId(), newEmail);
        return privateProfile(requireUser(user.getId()));
    }

    public User updatePassword(User currentUser, UpdateProfilePasswordRequest request) {
        User user = requireUser(currentUser.getId());
        String email = normalizeEmail(user.getEmail());
        validatePassword(request.password());
        emailVerificationService.verifyProfilePassword(email, request.emailCode());
        userMapper.updatePasswordAndInvalidateSessions(user.getId(), PasswordUtil.hash(request.password()));
        return privateProfile(requireUser(user.getId()));
    }

    public void sendCurrentEmailProfileCode(User user) {
        emailVerificationService.sendProfileCurrentEmailCode(normalizeEmail(user.getEmail()));
    }

    public void sendNewEmailProfileCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, normalizedEmail));
        if (existing != null) {
            throw new BizException("邮箱已注册");
        }
        emailVerificationService.sendProfileNewEmailCode(normalizedEmail);
    }

    public void sendPasswordProfileCode(User user) {
        emailVerificationService.sendProfilePasswordCode(normalizeEmail(user.getEmail()));
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
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
}
