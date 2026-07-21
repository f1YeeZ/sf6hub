package com.example.hubdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@TableName("combos")
public class Combo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long characterId;
    private Long authorId;
    private String author;
    private String starter;
    private String route;
    private String comboText;
    private Integer damage;
    private BigDecimal driveCost;
    private Integer saCost;
    private String advantageFrames;
    private String difficulty;
    private Boolean cornerOnly;
    private String controlType;
    private String dedupeKey;
    @TableField("route_character_ids")
    private String routeCharacterIdsData;
    private String type;
    private String tags;
    private String videoUrl;
    private String trainingNotes;
    private String rejectionReason;
    private String difficultyNote;
    private Boolean difficultyCalibrated;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private Integer likes;
    private Integer favorites;
    @TableField(exist = false)
    private Boolean liked;
    @TableField(exist = false)
    private Boolean favorited;
    @TableField(exist = false)
    private Long followupParentId;
    private String status;
    private LocalDate createdAt;
    private LocalDateTime submittedAt;
    private String manualReviewReason;
    private String videoReviewStatus;
    private String videoReviewReason;
    private LocalDateTime videoReviewedAt;
    @TableField(exist = false)
    private Combo followupParent;
    @TableField(exist = false)
    private Long reportCount;
    @TableField(exist = false)
    private Integer reviewPriority;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCharacterId() { return characterId; }
    public void setCharacterId(Long characterId) { this.characterId = characterId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getStarter() { return starter; }
    public void setStarter(String starter) { this.starter = starter; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getComboText() { return comboText; }
    public void setComboText(String comboText) { this.comboText = comboText; }
    public Integer getDamage() { return damage; }
    public void setDamage(Integer damage) { this.damage = damage; }
    public BigDecimal getDriveCost() { return driveCost; }
    public void setDriveCost(BigDecimal driveCost) { this.driveCost = driveCost; }
    public Integer getSaCost() { return saCost; }
    public void setSaCost(Integer saCost) { this.saCost = saCost; }
    public String getAdvantageFrames() { return advantageFrames; }
    public void setAdvantageFrames(String advantageFrames) { this.advantageFrames = advantageFrames; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Boolean getCornerOnly() { return cornerOnly; }
    public void setCornerOnly(Boolean cornerOnly) { this.cornerOnly = cornerOnly; }
    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }
    @JsonIgnore
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    @JsonIgnore
    public String getRouteCharacterIdsData() { return routeCharacterIdsData; }
    public void setRouteCharacterIdsData(String routeCharacterIdsData) { this.routeCharacterIdsData = routeCharacterIdsData; }
    @JsonProperty("routeCharacterIds")
    public List<Long> getRouteCharacterIds() {
        if (routeCharacterIdsData == null || routeCharacterIdsData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(routeCharacterIdsData.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
    public void setRouteCharacterIds(List<Long> routeCharacterIds) {
        routeCharacterIdsData = routeCharacterIds == null
                ? ""
                : routeCharacterIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    @JsonIgnore
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    @JsonProperty("tags")
    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getTrainingNotes() { return trainingNotes; }
    public void setTrainingNotes(String trainingNotes) { this.trainingNotes = trainingNotes; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getDifficultyNote() { return difficultyNote; }
    public void setDifficultyNote(String difficultyNote) { this.difficultyNote = difficultyNote; }
    public Boolean getDifficultyCalibrated() { return difficultyCalibrated; }
    public void setDifficultyCalibrated(Boolean difficultyCalibrated) { this.difficultyCalibrated = difficultyCalibrated; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public Integer getFavorites() { return favorites; }
    public void setFavorites(Integer favorites) { this.favorites = favorites; }
    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public Long getFollowupParentId() { return followupParentId; }
    public void setFollowupParentId(Long followupParentId) { this.followupParentId = followupParentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getManualReviewReason() { return manualReviewReason; }
    public void setManualReviewReason(String manualReviewReason) { this.manualReviewReason = manualReviewReason; }
    public String getVideoReviewStatus() { return videoReviewStatus; }
    public void setVideoReviewStatus(String videoReviewStatus) { this.videoReviewStatus = videoReviewStatus; }
    public String getVideoReviewReason() { return videoReviewReason; }
    public void setVideoReviewReason(String videoReviewReason) { this.videoReviewReason = videoReviewReason; }
    public LocalDateTime getVideoReviewedAt() { return videoReviewedAt; }
    public void setVideoReviewedAt(LocalDateTime videoReviewedAt) { this.videoReviewedAt = videoReviewedAt; }
    public Combo getFollowupParent() { return followupParent; }
    public void setFollowupParent(Combo followupParent) { this.followupParent = followupParent; }
    public Long getReportCount() { return reportCount; }
    public void setReportCount(Long reportCount) { this.reportCount = reportCount; }
    public Integer getReviewPriority() { return reviewPriority; }
    public void setReviewPriority(Integer reviewPriority) { this.reviewPriority = reviewPriority; }
}
