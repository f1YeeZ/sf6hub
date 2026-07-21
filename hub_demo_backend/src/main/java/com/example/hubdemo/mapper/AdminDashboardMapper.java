package com.example.hubdemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface AdminDashboardMapper {
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users) AS "users",
              (SELECT COUNT(*) FROM users WHERE created_at >= #{todayStart}) AS "todayUsers",
              (SELECT COUNT(*) FROM users WHERE banned = TRUE) AS "bannedUsers",
              (SELECT COUNT(*) FROM characters) AS "characters",
              (SELECT COUNT(*) FROM frame_data) AS "frames",
              (SELECT COUNT(*) FROM combos) AS "combos",
              (SELECT COUNT(*) FROM combos WHERE status = 'pending') AS "pendingCombos",
              (SELECT COUNT(*) FROM combos WHERE status = 'manual_review') AS "manualReviewCombos",
              (SELECT COUNT(*) FROM combos WHERE status = 'approved') AS "approvedCombos",
              (SELECT COUNT(*) FROM combos WHERE status = 'rejected') AS "rejectedCombos",
              (SELECT COUNT(*) FROM combos WHERE created_at = #{today}) AS "todayCombos",
              (SELECT COUNT(*) FROM announcements) AS "announcements",
              (SELECT COUNT(*) FROM announcements WHERE published = TRUE) AS "publishedAnnouncements",
              (SELECT COUNT(*) FROM reports WHERE target_type = 'combo' AND status = 'pending') AS "pendingReports",
              (SELECT COUNT(*) FROM reports WHERE target_type = 'combo' AND status = 'resolved') AS "resolvedReports",
              (SELECT COUNT(*) FROM reports WHERE target_type = 'combo' AND status = 'rejected') AS "rejectedReports",
              (SELECT COUNT(*) FROM reports WHERE target_type = 'combo' AND created_at >= #{todayStart}) AS "todayReports",
              (SELECT COUNT(*) FROM feedbacks WHERE status = 'pending') AS "pendingFeedbacks",
              (SELECT COUNT(*) FROM feedbacks WHERE status = 'resolved') AS "resolvedFeedbacks",
              (SELECT COUNT(*) FROM feedbacks WHERE status = 'rejected') AS "rejectedFeedbacks",
              (SELECT COUNT(*) FROM feedbacks WHERE created_at >= #{todayStart}) AS "todayFeedbacks"
            """)
    Map<String, Object> counts(@Param("today") LocalDate today,
                               @Param("todayStart") LocalDateTime todayStart);
}
