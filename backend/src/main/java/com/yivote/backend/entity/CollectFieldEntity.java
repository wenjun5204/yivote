package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 收集字段定义:TEXT=文本框,CHIP=点选(options 逗号分隔存) */
@Entity
@Table(name = "collect_field")
public class CollectFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collect_id", nullable = false)
    private Long collectId;

    @Column(nullable = false, length = 24)
    private String label;

    /** TEXT / CHIP */
    @Column(nullable = false, length = 8)
    private String kind;

    /** CHIP 候选项,逗号分隔(如 "S,M,L,XL");TEXT 为 null */
    @Column(length = 200)
    private String options;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCollectId() { return collectId; }
    public void setCollectId(Long collectId) { this.collectId = collectId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
