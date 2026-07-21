package com.example.hubdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hubdemo.entity.DailyVisitStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface DailyVisitStatsMapper extends BaseMapper<DailyVisitStats> {
    @Insert("""
            INSERT INTO daily_visit_stats (stat_date, uv, pv, updated_at)
            VALUES (#{date}, #{uv}, #{pv}, CURRENT_TIMESTAMP)
            ON CONFLICT (stat_date)
            DO UPDATE SET uv = EXCLUDED.uv, pv = EXCLUDED.pv, updated_at = CURRENT_TIMESTAMP
            """)
    void upsert(@Param("date") LocalDate date, @Param("uv") long uv, @Param("pv") long pv);
}
