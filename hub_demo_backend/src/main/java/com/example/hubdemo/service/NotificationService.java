package com.example.hubdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.entity.Notification;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.NotificationMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {
    static final Set<String> SUPPORTED_TYPES = Set.of(
            "combo_review",
            "combo_like",
            "combo_favorite",
            "feedback",
            "system"
    );

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final int retentionDays;

    @Autowired
    public NotificationService(NotificationMapper notificationMapper, UserMapper userMapper,
                               @Value("${app.notification-retention-days:180}") int retentionDays) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.retentionDays = Math.max(30, retentionDays);
    }

    NotificationService(NotificationMapper notificationMapper, UserMapper userMapper) {
        this(notificationMapper, userMapper, 180);
    }

    public PageResult<Notification> list(String username, long page, long pageSize, boolean unreadOnly) {
        return list(username, page, pageSize, unreadOnly, "");
    }

    public PageResult<Notification> list(String username, long page, long pageSize, boolean unreadOnly, String type) {
        User user = findUser(username);
        return list(user == null ? null : user.getId(), page, pageSize, unreadOnly, type);
    }

    public PageResult<Notification> list(Long userId, long page, long pageSize, boolean unreadOnly, String type) {
        requireSupportedTypeIfPresent(type);
        Page<Notification> result = notificationMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.pageSize(pageSize)),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(unreadOnly, Notification::getRead, false)
                        .eq(StringUtils.hasText(type), Notification::getType, type)
                        .in(!StringUtils.hasText(type), Notification::getType, SUPPORTED_TYPES)
                        .orderByDesc(Notification::getCreatedAt));
        return new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    public Notification read(Long id, String username) {
        User user = findUser(username);
        return read(id, user == null ? null : user.getId());
    }

    public Notification read(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BizException("通知不存在");
        }
        if (userId == null || !userId.equals(notification.getUserId())) {
            throw new BizException("无权操作该通知");
        }
        notification.setRead(true);
        notificationMapper.updateById(notification);
        return notification;
    }

    public int readAll(String username, String type) {
        User user = findUser(username);
        return readAll(user == null ? null : user.getId(), type);
    }

    public int readAll(Long userId, String type) {
        if (userId == null) {
            return 0;
        }
        requireSupportedTypeIfPresent(type);
        return notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getRead, false)
                .eq(StringUtils.hasText(type), Notification::getType, type)
                .in(!StringUtils.hasText(type), Notification::getType, SUPPORTED_TYPES)
                .set(Notification::getRead, true));
    }

    public void notifyUser(String recipient, String actor, String type, String title, String content) {
        notifyUser(recipient, actor, type, title, content, null);
    }

    public void notifyUser(String recipient, String actor, String type, String title, String content, String targetUrl) {
        requireSupportedType(type);
        if (!StringUtils.hasText(recipient) || recipient.equals(actor)) {
            return;
        }
        User user = findUser(recipient);
        if (user != null) {
            insertNotification(user, type, title, content, targetUrl);
        }
    }

    public void notifyUserById(Long recipientId, String actor, String type, String title, String content, String targetUrl) {
        requireSupportedType(type);
        if (recipientId == null) {
            return;
        }
        User recipient = userMapper.selectById(recipientId);
        if (recipient == null || !StringUtils.hasText(recipient.getUsername())) {
            return;
        }
        if (recipient.getUsername().equals(actor)) {
            return;
        }
        insertNotification(recipient, type, title, content, targetUrl);
    }

    private void insertNotification(User recipient, String type, String title, String content, String targetUrl) {
        Notification notification = new Notification();
        notification.setUserId(recipient.getId());
        notification.setUsername(recipient.getUsername());
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetUrl(targetUrl);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    public int broadcast(String title, String content, String targetUrl, String username) {
        return broadcast(title, content, targetUrl, username, "system");
    }

    public int broadcast(String title, String content, String targetUrl, String username, String type) {
        requireSupportedType(type);
        return StringUtils.hasText(username)
                ? notificationMapper.broadcastToUsername(username, type, title, content, targetUrl)
                : notificationMapper.broadcastAll(type, title, content, targetUrl);
    }

    @Scheduled(cron = "${app.notification-cleanup-cron:0 15 4 * * *}")
    public void deleteExpiredNotifications() {
        notificationMapper.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
    }

    private static void requireSupportedTypeIfPresent(String type) {
        if (StringUtils.hasText(type)) {
            requireSupportedType(type);
        }
    }

    private static void requireSupportedType(String type) {
        if (!StringUtils.hasText(type) || !SUPPORTED_TYPES.contains(type)) {
            throw new BizException("不支持的通知类型");
        }
    }

    private User findUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

}
