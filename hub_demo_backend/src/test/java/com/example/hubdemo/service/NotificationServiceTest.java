package com.example.hubdemo.service;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.entity.Notification;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.NotificationMapper;
import com.example.hubdemo.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {
    private NotificationMapper notificationMapper;
    private UserMapper userMapper;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        userMapper = mock(UserMapper.class);
        notificationService = new NotificationService(notificationMapper, userMapper);
    }

    @Test
    void notifyUserByIdUsesCurrentUsername() {
        User author = new User();
        author.setId(7L);
        author.setUsername("renamed_player");
        when(userMapper.selectById(7L)).thenReturn(author);

        notificationService.notifyUserById(7L, "admin", "combo_review", "连招审核未通过", "伤害数值不准确", "/combos/42");

        verify(notificationMapper).insert(org.mockito.ArgumentMatchers.<Notification>argThat(notification ->
                Long.valueOf(7L).equals(notification.getUserId())
                        && "renamed_player".equals(notification.getUsername())
                        && "combo_review".equals(notification.getType())
                        && "连招审核未通过".equals(notification.getTitle())
                        && "伤害数值不准确".equals(notification.getContent())
                        && "/combos/42".equals(notification.getTargetUrl())
                        && Boolean.FALSE.equals(notification.getRead())
                        && notification.getCreatedAt() != null
        ));
    }

    @Test
    void notifyUserByIdSkipsMissingUser() {
        when(userMapper.selectById(7L)).thenReturn(null);

        notificationService.notifyUserById(7L, "admin", "combo_review", "连招审核未通过", "伤害数值不准确", "/combos/42");

        verify(notificationMapper, never()).insert(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void notifyUserByIdSkipsSameActor() {
        User author = new User();
        author.setId(7L);
        author.setUsername("renamed_player");
        when(userMapper.selectById(7L)).thenReturn(author);

        notificationService.notifyUserById(7L, "renamed_player", "combo_like", "你的连招收到了新的点赞", "renamed_player 点赞了你的连招", "/combos/42");

        verify(notificationMapper, never()).insert(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void rejectsRetiredFollowNotificationType() {
        User recipient = new User();
        recipient.setId(7L);
        recipient.setUsername("player");
        when(userMapper.selectById(7L)).thenReturn(recipient);

        assertThatThrownBy(() -> notificationService.notifyUserById(
                7L,
                "another_player",
                "followed_combo_new",
                "你关注的玩家发布了新连招",
                "关注动态",
                "/combos/42"
        )).isInstanceOf(BizException.class)
                .hasMessage("不支持的通知类型");

        verify(notificationMapper, never()).insert(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void rejectsRetiredNotificationTypeFilter() {
        assertThatThrownBy(() -> notificationService.list(7L, 1, 20, false, "follow"))
                .isInstanceOf(BizException.class)
                .hasMessage("不支持的通知类型");
    }

    @Test
    void supportedTypesMatchCurrentProductScope() {
        assertThat(NotificationService.SUPPORTED_TYPES)
                .containsExactlyInAnyOrder(
                        "combo_review",
                        "combo_like",
                        "combo_favorite",
                        "feedback",
                        "system"
                );
    }
}
