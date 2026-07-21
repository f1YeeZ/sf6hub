package com.example.hubdemo.service;

import com.example.hubdemo.dto.AuthDtos.UpdateProfileEmailRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfilePasswordRequest;
import com.example.hubdemo.dto.AuthDtos.UpdateProfileUsernameRequest;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private UserMapper userMapper;
    private EmailVerificationService verificationService;
    private UserService userService;
    private User current;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        verificationService = mock(EmailVerificationService.class);
        userService = new UserService(userMapper, verificationService);
        current = user("old_name", "old@example.com");
    }

    @Test
    void usernameUpdateWritesOnlyUsernameColumn() {
        User refreshed = user("new_name", "newer@example.com");
        when(userMapper.selectById(7L)).thenReturn(current, refreshed);
        when(userMapper.selectOne(any())).thenReturn(null);

        User result = userService.updateUsername(current, new UpdateProfileUsernameRequest("new_name"));

        verify(userMapper).updateUsername(7L, "new_name");
        verify(userMapper, never()).updateById(any(User.class));
        assertThat(result.getEmail()).isEqualTo("newer@example.com");
    }

    @Test
    void emailUpdateWritesOnlyEmailColumn() {
        User refreshed = user("newer_name", "new@example.com");
        when(userMapper.selectById(7L)).thenReturn(current, refreshed);
        when(userMapper.selectOne(any())).thenReturn(null);

        User result = userService.updateEmail(current,
                new UpdateProfileEmailRequest("new@example.com", "111111", "222222"));

        verify(userMapper).updateEmail(7L, "new@example.com");
        verify(userMapper, never()).updateById(any(User.class));
        assertThat(result.getUsername()).isEqualTo("newer_name");
    }

    @Test
    void passwordUpdateAtomicallyInvalidatesSessionsWithoutWritingProfileFields() {
        User refreshed = user("newer_name", "newer@example.com");
        refreshed.setTokenVersion(4L);
        when(userMapper.selectById(7L)).thenReturn(current, refreshed);

        User result = userService.updatePassword(current,
                new UpdateProfilePasswordRequest("123456", "new-secret-123"));

        verify(userMapper).updatePasswordAndInvalidateSessions(org.mockito.ArgumentMatchers.eq(7L), any());
        verify(userMapper, never()).updateById(any(User.class));
        assertThat(result.getTokenVersion()).isEqualTo(4L);
        assertThat(result.getUsername()).isEqualTo("newer_name");
    }

    private static User user(String username, String email) {
        User user = new User();
        user.setId(7L);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setTokenVersion(3L);
        return user;
    }
}
