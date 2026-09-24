package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 群投票主表:TIME=凑时间(多选聚合),CHOICE=拿主意(单选) */
@Entity
@Table(name = "vote")
public class VoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String title;

    /** TIME / CHOICE */
    @Column(nullable = false, length = 10)
    private String type;

    /** 发起人 */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 群维度归档键(分享票据解出;可空=未从群进入) */
    @Column(name = "open_gid", length = 64)
    private String openGId;

    /** 截止时间(可空=不限,组织者随时可定档) */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    /** OPEN / LOCKED */
    @Column(nullable = false, length = 10)
    private String status;

    /** 定档时段(LOCKED 时非空;该时段投票者=到场名单) */
    @Column(name = "locked_slot_id")
    private Long lockedSlotId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOpenGId() { return openGId; }
    public void setOpenGId(String openGId) { this.openGId = openGId; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLockedSlotId() { return lockedSlotId; }
    public void setLockedSlotId(Long lockedSlotId) { this.lockedSlotId = lockedSlotId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
