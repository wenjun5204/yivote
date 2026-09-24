package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 一张选票:一人一票(对每个投票),重新提交即整票覆盖(信任三原则:可改不可逃)。
 * TIME 多行(每时段一行),CHOICE 一行。
 */
@Entity
@Table(name = "ballot",
        uniqueConstraints = @UniqueConstraint(name = "uk_ballot_user_slot", columnNames = {"user_id", "slot_id"}))
public class BallotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVoteId() { return voteId; }
    public void setVoteId(Long voteId) { this.voteId = voteId; }
    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
