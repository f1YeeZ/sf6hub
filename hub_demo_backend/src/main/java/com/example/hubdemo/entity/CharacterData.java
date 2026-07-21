package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("character_data")
public class CharacterData {
    @TableId(value = "character_id", type = IdType.INPUT)
    private Long characterId;
    private String hp;
    private String throwRange;
    private String forwardWalkSpeed;
    private String backWalkSpeed;
    private String forwardDashSpeed;
    private String backDashSpeed;
    private String forwardDashDistance;
    private String backDashDistance;
    private String jumpSpeed;
    private String forwardJumpDistance;
    private String backJumpDistance;
    private String fastestNormal;
    private String sourceName;
    private String sourceUrl;
    private LocalDateTime updatedAt;

    public Long getCharacterId() { return characterId; }
    public void setCharacterId(Long characterId) { this.characterId = characterId; }
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }
    public String getThrowRange() { return throwRange; }
    public void setThrowRange(String throwRange) { this.throwRange = throwRange; }
    public String getForwardWalkSpeed() { return forwardWalkSpeed; }
    public void setForwardWalkSpeed(String forwardWalkSpeed) { this.forwardWalkSpeed = forwardWalkSpeed; }
    public String getBackWalkSpeed() { return backWalkSpeed; }
    public void setBackWalkSpeed(String backWalkSpeed) { this.backWalkSpeed = backWalkSpeed; }
    public String getForwardDashSpeed() { return forwardDashSpeed; }
    public void setForwardDashSpeed(String forwardDashSpeed) { this.forwardDashSpeed = forwardDashSpeed; }
    public String getBackDashSpeed() { return backDashSpeed; }
    public void setBackDashSpeed(String backDashSpeed) { this.backDashSpeed = backDashSpeed; }
    public String getForwardDashDistance() { return forwardDashDistance; }
    public void setForwardDashDistance(String forwardDashDistance) { this.forwardDashDistance = forwardDashDistance; }
    public String getBackDashDistance() { return backDashDistance; }
    public void setBackDashDistance(String backDashDistance) { this.backDashDistance = backDashDistance; }
    public String getJumpSpeed() { return jumpSpeed; }
    public void setJumpSpeed(String jumpSpeed) { this.jumpSpeed = jumpSpeed; }
    public String getForwardJumpDistance() { return forwardJumpDistance; }
    public void setForwardJumpDistance(String forwardJumpDistance) { this.forwardJumpDistance = forwardJumpDistance; }
    public String getBackJumpDistance() { return backJumpDistance; }
    public void setBackJumpDistance(String backJumpDistance) { this.backJumpDistance = backJumpDistance; }
    public String getFastestNormal() { return fastestNormal; }
    public void setFastestNormal(String fastestNormal) { this.fastestNormal = fastestNormal; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
