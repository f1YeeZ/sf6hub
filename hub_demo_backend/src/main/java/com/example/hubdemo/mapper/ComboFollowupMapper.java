package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.ComboFollowup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface ComboFollowupMapper extends BaseMapper<ComboFollowup> {
    @Insert("""
            INSERT INTO combo_followups (parent_combo_id, followup_combo_id, created_by, created_at)
            VALUES (#{parentComboId}, #{followupComboId}, #{createdBy}, #{createdAt})
            ON CONFLICT (followup_combo_id) DO UPDATE
            SET parent_combo_id = EXCLUDED.parent_combo_id,
                created_by = EXCLUDED.created_by,
                created_at = EXCLUDED.created_at
            """)
    int upsertParent(@Param("parentComboId") Long parentComboId,
                     @Param("followupComboId") Long followupComboId,
                     @Param("createdBy") Long createdBy,
                     @Param("createdAt") LocalDateTime createdAt);
}
