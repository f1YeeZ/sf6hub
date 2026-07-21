package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT avatar FROM users WHERE avatar IS NOT NULL AND avatar <> ''")
    List<String> selectReferencedAvatarUrls();

    @Select("""
            SELECT CAST(created_at AS date) AS day, COUNT(*) AS value
            FROM users
            WHERE created_at >= #{start} AND created_at < #{end}
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """)
    List<Map<String, Object>> countCreatedByDay(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
    @Update("UPDATE users SET username = #{username} WHERE id = #{id}")
    int updateUsername(@Param("id") Long id, @Param("username") String username);

    @Update("UPDATE users SET email = #{email} WHERE id = #{id}")
    int updateEmail(@Param("id") Long id, @Param("email") String email);

    @Update("UPDATE users SET password_hash = #{passwordHash}, token_version = token_version + 1 WHERE id = #{id}")
    int updatePasswordAndInvalidateSessions(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("UPDATE users SET last_login_at = #{lastLoginAt} WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Update("""
            UPDATE users
            SET banned = FALSE, ban_reason = '', banned_until = NULL
            WHERE id = #{id} AND banned = TRUE AND banned_until < #{now}
            """)
    int clearExpiredBan(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE users
            SET banned = #{banned}, ban_reason = #{banReason}, banned_until = #{bannedUntil}
            WHERE id = #{id}
            """)
    int updateBanState(@Param("id") Long id, @Param("banned") boolean banned,
                       @Param("banReason") String banReason, @Param("bannedUntil") LocalDateTime bannedUntil);

    @Update("UPDATE users SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @Update("UPDATE users SET admin_permissions = #{permissions} WHERE id = #{id}")
    int updateAdminPermissions(@Param("id") Long id, @Param("permissions") String permissions);
}
