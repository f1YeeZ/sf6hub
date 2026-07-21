package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.hubdemo.entity.Combo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ComboMapper extends BaseMapper<Combo> {
    @Select("SELECT video_url FROM combos WHERE video_url IS NOT NULL AND video_url <> ''")
    List<String> selectReferencedVideoUrls();

    @Select("""
            SELECT created_at AS day, COUNT(*) AS value
            FROM combos
            WHERE created_at >= #{start} AND created_at <= #{end}
            GROUP BY created_at
            ORDER BY day
            """)
    List<Map<String, Object>> countCreatedByDay(@Param("start") LocalDate start,
                                                 @Param("end") LocalDate end);

    @Select("""
            SELECT user_account.id AS "userId",
                   user_account.username AS username,
                   COALESCE(user_account.avatar, '') AS avatar,
                   COUNT(combo.id) AS "comboCount"
            FROM combos combo
            INNER JOIN users user_account ON user_account.id = combo.author_id
            WHERE combo.status = 'approved'
              AND combo.submitted_at >= #{weekStart}
              AND combo.submitted_at < #{weekEnd}
              AND COALESCE(user_account.banned, FALSE) = FALSE
            GROUP BY user_account.id, user_account.username, user_account.avatar
            ORDER BY "comboCount" DESC, user_account.id ASC
            LIMIT 3
            """)
    List<Map<String, Object>> selectWeeklyContributors(@Param("weekStart") LocalDateTime weekStart,
                                                        @Param("weekEnd") LocalDateTime weekEnd);

    @Select("""
            <script>
            SELECT combo.*,
                   combo.route_character_ids AS route_character_ids_data
            FROM combos combo
            WHERE combo.status = 'approved'
              AND NOT EXISTS (
                  SELECT 1 FROM combo_followups followup WHERE followup.followup_combo_id = combo.id
              )
            <if test="characterId != null">
              AND combo.character_id = #{characterId}
            </if>
            <if test="controlType != null and controlType != ''">
              AND combo.control_type = #{controlType}
            </if>
            <if test="starter != null and starter != ''">
              AND combo.starter = #{starter}
            </if>
            <if test="saCost != null">
              AND combo.sa_cost = #{saCost}
            </if>
            <if test="driveCost != null">
              AND combo.drive_cost = #{driveCost}
            </if>
            <if test="damageMin != null">
              AND combo.damage &gt;= #{damageMin}
            </if>
            <if test="damageMax != null">
              AND combo.damage &lt;= #{damageMax}
            </if>
            <if test="tags != null and !tags.isEmpty()">
              <foreach collection="tags" item="tag">
                AND CONCAT(',', COALESCE(combo.tags, ''), ',') LIKE CONCAT('%,', #{tag}, ',%')
              </foreach>
            </if>
            <choose>
              <when test="sort == 'likes'">ORDER BY combo.likes DESC, combo.id DESC</when>
              <when test="sort == 'createdAt'">ORDER BY combo.created_at DESC, combo.id DESC</when>
              <otherwise>ORDER BY combo.damage DESC, combo.id DESC</otherwise>
            </choose>
            </script>
            """)
    Page<Combo> selectTopLevelPage(Page<Combo> page,
                                   @Param("characterId") Long characterId,
                                   @Param("controlType") String controlType,
                                   @Param("starter") String starter,
                                   @Param("tags") List<String> tags,
                                   @Param("saCost") Integer saCost,
                                   @Param("driveCost") BigDecimal driveCost,
                                   @Param("damageMin") Integer damageMin,
                                   @Param("damageMax") Integer damageMax,
                                   @Param("sort") String sort);

    @Select("""
            SELECT combo.*,
                   COUNT(report.id) AS report_count,
                   (CASE WHEN combo.status = 'manual_review' THEN 60 ELSE 0 END
                    + LEAST(40, COUNT(report.id) * 12)
                    + CASE WHEN COALESCE(combo.video_url, '') = '' THEN 12 ELSE 0 END
                    + CASE WHEN combo.video_review_status = 'video_rejected' THEN 80 ELSE 0 END
                    + CASE WHEN combo.status = 'pending' THEN 10 ELSE 0 END) AS review_priority
            FROM combos combo
            LEFT JOIN reports report
              ON report.target_type = 'combo'
             AND report.target_id = combo.id
             AND report.status = 'pending'
            WHERE combo.status IN ('pending', 'manual_review')
               OR combo.video_review_status = 'video_rejected'
               OR COALESCE(combo.video_url, '') = ''
               OR EXISTS (
                   SELECT 1 FROM reports pending
                   WHERE pending.target_type = 'combo'
                     AND pending.target_id = combo.id
                     AND pending.status = 'pending'
               )
            GROUP BY combo.id
            ORDER BY review_priority DESC, combo.id DESC
            LIMIT #{limit}
            """)
    List<Combo> selectRiskCombos(@Param("limit") int limit);
    @Update("UPDATE combos SET likes = GREATEST(0, likes + #{delta}) WHERE id = #{comboId}")
    int adjustLikes(@Param("comboId") Long comboId, @Param("delta") int delta);

    @Update("UPDATE combos SET favorites = GREATEST(0, favorites + #{delta}) WHERE id = #{comboId}")
    int adjustFavorites(@Param("comboId") Long comboId, @Param("delta") int delta);

    @Select("""
            <script>
            SELECT combo.id, combo.starter, combo.tags, combo.type
            FROM combos combo
            WHERE combo.status = 'approved'
              AND NOT EXISTS (
                  SELECT 1 FROM combo_followups followup WHERE followup.followup_combo_id = combo.id
              )
            <if test="characterId != null">
              AND combo.character_id = #{characterId}
            </if>
            <if test="controlType != null and controlType != ''">
              AND combo.control_type = #{controlType}
            </if>
            ORDER BY combo.starter ASC
            </script>
            """)
    List<Combo> selectFilterOptionRows(@Param("characterId") Long characterId,
                                       @Param("controlType") String controlType);

    @Select("""
            SELECT combo.*,
                   combo.route_character_ids AS route_character_ids_data
            FROM combos combo
            WHERE combo.status = 'approved'
              AND combo.character_id = #{characterId}
              AND combo.control_type = #{controlType}
              AND NOT EXISTS (
                  SELECT 1 FROM combo_followups followup WHERE followup.followup_combo_id = combo.id
              )
            ORDER BY combo.created_at DESC, combo.id DESC
            """)
    List<Combo> selectParentComboOptions(@Param("characterId") Long characterId,
                                         @Param("controlType") String controlType);

    @Update("""
            UPDATE combos
            SET character_id = #{combo.characterId},
                starter = #{combo.starter},
                route = #{combo.route},
                combo_text = #{combo.comboText},
                damage = #{combo.damage},
                drive_cost = #{combo.driveCost},
                sa_cost = #{combo.saCost},
                advantage_frames = #{combo.advantageFrames},
                difficulty = #{combo.difficulty},
                corner_only = #{combo.cornerOnly},
                control_type = #{combo.controlType},
                dedupe_key = #{combo.dedupeKey},
                route_character_ids = #{combo.routeCharacterIdsData},
                type = #{combo.type},
                tags = #{combo.tags},
                video_url = #{combo.videoUrl},
                training_notes = #{combo.trainingNotes},
                difficulty_note = #{combo.difficultyNote}
            WHERE id = #{combo.id}
            """)
    int updateContentFields(@Param("combo") Combo combo);

    @Update("""
            UPDATE combos
            SET status = 'manual_review',
                rejection_reason = '',
                reviewed_by = '',
                reviewed_at = NULL,
                difficulty_calibrated = FALSE,
                submitted_at = #{submittedAt},
                manual_review_reason = '用户重新提交，等待管理员审核',
                video_review_status = 'unchecked',
                video_review_reason = '',
                video_reviewed_at = NULL
            WHERE id = #{id}
            """)
    int markResubmitted(@Param("id") Long id, @Param("submittedAt") LocalDateTime submittedAt);

    @Update("""
            UPDATE combos
            SET status = #{combo.status},
                rejection_reason = #{combo.rejectionReason},
                manual_review_reason = #{combo.manualReviewReason},
                difficulty = #{combo.difficulty},
                difficulty_note = #{combo.difficultyNote},
                difficulty_calibrated = #{combo.difficultyCalibrated},
                reviewed_by = #{combo.reviewedBy},
                reviewed_at = #{combo.reviewedAt}
            WHERE id = #{combo.id}
            """)
    int updateReviewFields(@Param("combo") Combo combo);

    @Update("""
            UPDATE combos
            SET video_review_status = #{status},
                video_review_reason = #{reason},
                video_reviewed_at = #{reviewedAt}
            WHERE id = #{id}
            """)
    int updateVideoReviewFields(@Param("id") Long id, @Param("status") String status,
                                @Param("reason") String reason, @Param("reviewedAt") LocalDateTime reviewedAt);

    @Update("""
            UPDATE combos
            SET status = 'rejected',
                rejection_reason = '视频违规',
                manual_review_reason = '',
                reviewed_by = #{reviewedBy},
                reviewed_at = #{reviewedAt}
            WHERE id = #{id}
            """)
    int rejectForVideo(@Param("id") Long id, @Param("reviewedBy") String reviewedBy,
                       @Param("reviewedAt") LocalDateTime reviewedAt);
}
