package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.VisitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {
    @Select("""
            SELECT COUNT(1)
            FROM visit_logs
            WHERE visitor_id = #{visitorId}
              AND path = #{path}
              AND created_at >= #{since}
            """)
    long countRecentSamePath(@Param("visitorId") String visitorId, @Param("path") String path, @Param("since") LocalDateTime since);

    @Select("""
            SELECT COUNT(DISTINCT visitor_id)
            FROM visit_logs
            WHERE visit_date = #{date}
            """)
    long countUvByDate(@Param("date") LocalDate date);

    @Select("""
            SELECT COUNT(1)
            FROM visit_logs
            WHERE visit_date = #{date}
            """)
    long countPvByDate(@Param("date") LocalDate date);

    @Delete("DELETE FROM visit_logs WHERE created_at < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
