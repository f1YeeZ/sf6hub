package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("combo_followups")
public class ComboFollowup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentComboId;
    private Long followupComboId;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentComboId() { return parentComboId; }
    public void setParentComboId(Long parentComboId) { this.parentComboId = parentComboId; }
    public Long getFollowupComboId() { return followupComboId; }
    public void setFollowupComboId(Long followupComboId) { this.followupComboId = followupComboId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
