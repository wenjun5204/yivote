package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 群待办(凑分工,PRD §7B):待办挂在活动上,不挂在人上——
 * 一次群活动从「定了」到「成了」之间的分工认领,活动收口后归档进群日历。
 * sourceType=VOTE 时挂在定档的投票下(日历不另立活动卡,只给投票加 todo 链路点)。
 */
@Entity
@Table(name = "todo")
public class TodoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String title;

    /** 发起人 */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "open_gid", length = 64)
    private String openGId;

    /** OPEN / DONE(全部事项搞定即收口) */
    @Column(nullable = false, length = 10)
    private String status;

    /** 上游活动类型(VOTE);独立发起为 null */
    @Column(name = "source_type", length = 10)
    private String sourceType;

    /** 上游活动 id;独立发起为 null */
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOpenGId() { return openGId; }
    public void setOpenGId(String openGId) { this.openGId = openGId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
