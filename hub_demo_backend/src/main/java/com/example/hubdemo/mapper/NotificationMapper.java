package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface NotificationMapper extends BaseMapper<Notification> {
    @Insert("""
            INSERT INTO notifications (user_id, username, type, title, content, target_url, read, created_at)
            SELECT id, username, #{type}, #{title}, #{content}, #{targetUrl}, FALSE, CURRENT_TIMESTAMP
            FROM users
            """)
    int broadcastAll(@Param("type") String type, @Param("title") String title,
                     @Param("content") String content, @Param("targetUrl") String targetUrl);

    @Insert("""
            INSERT INTO notifications (user_id, username, type, title, content, target_url, read, created_at)
            SELECT id, username, #{type}, #{title}, #{content}, #{targetUrl}, FALSE, CURRENT_TIMESTAMP
            FROM users WHERE username = #{username}
            """)
    int broadcastToUsername(@Param("username") String username, @Param("type") String type,
                            @Param("title") String title, @Param("content") String content,
                            @Param("targetUrl") String targetUrl);

    @Delete("DELETE FROM notifications WHERE created_at < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
