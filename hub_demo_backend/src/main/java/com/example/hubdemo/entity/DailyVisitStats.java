package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("daily_visit_stats")
public class DailyVisitStats {
    @TableId
    private LocalDate statDate;
    private Long uv;
    private Long pv;
    private LocalDateTime updatedAt;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Long getUv() { return uv; }
    public void setUv(Long uv) { this.uv = uv; }
    public Long getPv() { return pv; }
    public void setPv(Long pv) { this.pv = pv; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
