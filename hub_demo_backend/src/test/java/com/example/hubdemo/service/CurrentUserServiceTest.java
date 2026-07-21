package com.example.hubdemo.service;

import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.JwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {
    @Test
    void rejectsTokenIssuedBeforePasswordChange() {
        UserMapper userMapper = mock(UserMapper.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        User user = new User();
        user.setId(42L);
        user.setTokenVersion(2L);
        when(jwtUtil.parseClaims("old-token")).thenReturn(Optional.of(new JwtUtil.TokenClaims(42L, 1L)));
        when(userMapper.selectById(42L)).thenReturn(user);

        CurrentUserService service = new CurrentUserService(userMapper, jwtUtil, mock(AuthCookieService.class));

        assertThat(service.fromToken("old-token")).isEmpty();
    }
}
