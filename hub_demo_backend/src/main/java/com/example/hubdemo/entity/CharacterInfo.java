package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("characters")
public class CharacterInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private String description;
    private String archetype;
    private String officialSlug;
    private String officialFrameUrl;
    private Integer displayOrder;
    @TableField(exist = false)
    private Long comboCount;
    @TableField(exist = false)
    private Long pendingComboCount;
    @TableField(exist = false)
    private Long frameCount;
    @TableField(exist = false)
    private CharacterData characterData;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getArchetype() { return archetype; }
    public void setArchetype(String archetype) { this.archetype = archetype; }
    public String getOfficialSlug() { return officialSlug; }
    public void setOfficialSlug(String officialSlug) { this.officialSlug = officialSlug; }
    public String getOfficialFrameUrl() { return officialFrameUrl; }
    public void setOfficialFrameUrl(String officialFrameUrl) { this.officialFrameUrl = officialFrameUrl; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Long getComboCount() { return comboCount; }
    public void setComboCount(Long comboCount) { this.comboCount = comboCount; }
    public Long getPendingComboCount() { return pendingComboCount; }
    public void setPendingComboCount(Long pendingComboCount) { this.pendingComboCount = pendingComboCount; }
    public Long getFrameCount() { return frameCount; }
    public void setFrameCount(Long frameCount) { this.frameCount = frameCount; }
    public CharacterData getCharacterData() { return characterData; }
    public void setCharacterData(CharacterData characterData) { this.characterData = characterData; }
}
