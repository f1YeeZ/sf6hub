package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("frame_change_history")
public class FrameChangeHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long frameId;
    private Long characterId;
    private String moveName;
    private String action;
    private String adminName;
    private String detail;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFrameId() { return frameId; }
    public void setFrameId(Long frameId) { this.frameId = frameId; }
    public Long getCharacterId() { return characterId; }
    public void setCharacterId(Long characterId) { this.characterId = characterId; }
    public String getMoveName() { return moveName; }
    public void setMoveName(String moveName) { this.moveName = moveName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
