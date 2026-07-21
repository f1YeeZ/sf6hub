package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.AuthDtos.LoginRequest;
import com.example.hubdemo.dto.AuthDtos.RegisterRequest;
import com.example.hubdemo.dto.AuthDtos.ResetPasswordRequest;
import com.example.hubdemo.dto.AuthDtos.VerifyResetCodeRequest;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserMapper userMapper;
    private EmailVerificationService emailVerificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        emailVerificationService = mock(EmailVerificationService.class);
        authService = new AuthService(userMapper, mock(JwtUtil.class), emailVerificationService);
    }

    @Test
    void registerRejectsUsernameWithWhitespaceInside() {
        RegisterRequest request = new RegisterRequest("bad name", "player@example.com", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名只能包含字母、数字、下划线或短横线");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerRejectsTooShortUsernameAfterTrimming() {
        RegisterRequest request = new RegisterRequest(" ab ", "player@example.com", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名长度需为 3-20 个字符");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerTrimsValidUsernameBeforeSaving() {
        RegisterRequest request = new RegisterRequest(" player_1 ", "PLAYER@EXAMPLE.COM", "secret123");
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        authService.register(request);

        verify(userMapper).insert(org.mockito.ArgumentMatchers.<User>argThat(user ->
                "player_1".equals(user.getUsername()) && "player@example.com".equals(user.getEmail())
        ));
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void loginUsesEmailInsteadOfUsername() {
        User user = new User();
        user.setId(1L);
        user.setUsername("player");
        user.setEmail("player@example.com");
        user.setPasswordHash("00000000000000000000000000000000:bcfa9d35208eaff87d3d179ed05636b0b659157926158e3c645c1605a0c7d43f");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        authService.login(new LoginRequest(" PLAYER@EXAMPLE.COM ", "123456"));

        verify(userMapper).selectOne(any(Wrapper.class));
    }

    @Test
    void adminLoginRejectsRegularUser() {
        User user = validUser("player", "user", "");
        user.setId(1L);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        assertThatThrownBy(() -> authService.adminLogin(new LoginRequest("player@example.com", "123456")))
                .isInstanceOf(BizException.class)
                .hasMessage("仅允许后台管理员登录");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void adminLoginAcceptsAdminUser() {
        User user = validUser("admin", "admin", "");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        authService.adminLogin(new LoginRequest("player@example.com", "123456"));

        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userMapper).updateLastLoginAt(org.mockito.ArgumentMatchers.eq(user.getId()), any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void adminLoginAcceptsComboReviewer() {
        User user = validUser("reviewer", "user", "combo_review");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        authService.adminLogin(new LoginRequest("player@example.com", "123456"));

        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userMapper).updateLastLoginAt(org.mockito.ArgumentMatchers.eq(user.getId()), any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void passwordResetUpdatesOnlyPasswordAndAtomicallyInvalidatesSessions() {
        User user = validUser("player", "user", "");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        String resetToken = authService.verifyPasswordResetCode(
                new VerifyResetCodeRequest("player@example.com", "123456")
        ).resetToken();

        authService.resetPassword(new ResetPasswordRequest(
                "player@example.com", resetToken, "new-secret-123"
        ));

        verify(userMapper).updatePasswordAndInvalidateSessions(org.mockito.ArgumentMatchers.eq(user.getId()), any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    private static User validUser(String username, String role, String adminPermissions) {
        User user = new User();
        user.setId(2L);
        user.setUsername(username);
        user.setEmail("player@example.com");
        user.setRole(role);
        user.setAdminPermissions(adminPermissions);
        user.setPasswordHash("00000000000000000000000000000000:bcfa9d35208eaff87d3d179ed05636b0b659157926158e3c645c1605a0c7d43f");
        return user;
    }
}
