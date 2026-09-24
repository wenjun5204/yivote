package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 投票候选时段/选项 */
@Entity
@Table(name = "vote_slot")
public class VoteSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    /** 展示名,如「周六下午」 */
    @Column(nullable = false, length = 24)
    private String label;

    /** 同一投票内的展示顺序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVoteId() { return voteId; }
    public void setVoteId(Long voteId) { this.voteId = voteId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
