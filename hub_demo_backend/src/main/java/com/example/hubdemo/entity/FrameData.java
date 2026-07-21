package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("frame_data")
public class FrameData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long characterId;
    private String controlType;
    private String moveName;
    private String startup;
    private String active;
    private String recovery;
    private String onBlock;
    private String onHit;
    private String cancel;
    private String damage;
    private String comboScaling;
    private String driveGainOnHit;
    private String driveLossOnBlock;
    private String driveLossOnPunishCounter;
    private String superArtGain;
    private String properties;
    private String miscellaneous;
    private String sourceUrl;
    private String sourceCharacterSlug;
    private String sourceLang;
    private LocalDateTime sourceSyncedAt;
    private Integer displayOrder;
    private Boolean manualOverride;
    @TableField(exist = false)
    private Boolean recentlyChanged;
    @TableField(exist = false)
    private Boolean manuallyCorrected;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCharacterId() { return characterId; }
    public void setCharacterId(Long characterId) { this.characterId = characterId; }
    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }
    public String getMoveName() { return moveName; }
    public void setMoveName(String moveName) { this.moveName = moveName; }
    public String getStartup() { return startup; }
    public void setStartup(String startup) { this.startup = startup; }
    public String getActive() { return active; }
    public void setActive(String active) { this.active = active; }
    public String getRecovery() { return recovery; }
    public void setRecovery(String recovery) { this.recovery = recovery; }
    public String getOnBlock() { return onBlock; }
    public void setOnBlock(String onBlock) { this.onBlock = onBlock; }
    public String getOnHit() { return onHit; }
    public void setOnHit(String onHit) { this.onHit = onHit; }
    public String getCancel() { return cancel; }
    public void setCancel(String cancel) { this.cancel = cancel; }
    public String getDamage() { return damage; }
    public void setDamage(String damage) { this.damage = damage; }
    public String getComboScaling() { return comboScaling; }
    public void setComboScaling(String comboScaling) { this.comboScaling = comboScaling; }
    public String getDriveGainOnHit() { return driveGainOnHit; }
    public void setDriveGainOnHit(String driveGainOnHit) { this.driveGainOnHit = driveGainOnHit; }
    public String getDriveLossOnBlock() { return driveLossOnBlock; }
    public void setDriveLossOnBlock(String driveLossOnBlock) { this.driveLossOnBlock = driveLossOnBlock; }
    public String getDriveLossOnPunishCounter() { return driveLossOnPunishCounter; }
    public void setDriveLossOnPunishCounter(String driveLossOnPunishCounter) { this.driveLossOnPunishCounter = driveLossOnPunishCounter; }
    public String getSuperArtGain() { return superArtGain; }
    public void setSuperArtGain(String superArtGain) { this.superArtGain = superArtGain; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
    public String getMiscellaneous() { return miscellaneous; }
    public void setMiscellaneous(String miscellaneous) { this.miscellaneous = miscellaneous; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceCharacterSlug() { return sourceCharacterSlug; }
    public void setSourceCharacterSlug(String sourceCharacterSlug) { this.sourceCharacterSlug = sourceCharacterSlug; }
    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }
    public LocalDateTime getSourceSyncedAt() { return sourceSyncedAt; }
    public void setSourceSyncedAt(LocalDateTime sourceSyncedAt) { this.sourceSyncedAt = sourceSyncedAt; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getManualOverride() { return manualOverride; }
    public void setManualOverride(Boolean manualOverride) { this.manualOverride = manualOverride; }
    public Boolean getRecentlyChanged() { return recentlyChanged; }
    public void setRecentlyChanged(Boolean recentlyChanged) { this.recentlyChanged = recentlyChanged; }
    public Boolean getManuallyCorrected() { return manuallyCorrected; }
    public void setManuallyCorrected(Boolean manuallyCorrected) { this.manuallyCorrected = manuallyCorrected; }
}
