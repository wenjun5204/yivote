package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 一个人的一行填报:一人一行,重新提交整行覆盖(可改不可逃) */
@Entity
@Table(name = "collect_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_entry_user", columnNames = {"collect_id", "user_id"}))
public class CollectEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collect_id", nullable = false)
    private Long collectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 按字段顺序的值,逗号分隔转义存储(值内逗号以\,转义) */
    @Column(nullable = false, length = 500)
    private String valuesCsv;

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCollectId() { return collectId; }
    public void setCollectId(Long collectId) { this.collectId = collectId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getValuesCsv() { return valuesCsv; }
    public void setValuesCsv(String valuesCsv) { this.valuesCsv = valuesCsv; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
