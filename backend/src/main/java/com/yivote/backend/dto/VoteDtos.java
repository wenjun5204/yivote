package com.yivote.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 投票契约模型:与 api/openapi.yaml 的 VoteCreateRequest/BallotSubmitRequest/VoteSummary/VoteDetail/UserBrief 对齐 */
public final class VoteDtos {

    private VoteDtos() {
    }

    public static class SlotCreate {
        private String label;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class VoteCreateRequest {
        private String title;
        private String type;
        private String openGId;
        private LocalDateTime deadline;
        private List<SlotCreate> slots;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getOpenGId() { return openGId; }
        public void setOpenGId(String openGId) { this.openGId = openGId; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
        public List<SlotCreate> getSlots() { return slots; }
        public void setSlots(List<SlotCreate> slots) { this.slots = slots; }
    }

    public static class BallotSubmitRequest {
        private List<Long> slotIds;

        public List<Long> getSlotIds() { return slotIds; }
        public void setSlotIds(List<Long> slotIds) { this.slotIds = slotIds; }
    }

    public static class LockRequest {
        private Long slotId;

        public Long getSlotId() { return slotId; }
        public void setSlotId(Long slotId) { this.slotId = slotId; }
    }

    public static class UserBrief {
        private Long userId;
        private String nickname;
        private String avatarUrl;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class VoteSummary {
        private Long voteId;
        private String title;
        private String type;
        private String status;
        private Integer voterCount;
        private String myRole;   // OWNER / VOTER / NONE
        private LocalDateTime updatedAt;

        public Long getVoteId() { return voteId; }
        public void setVoteId(Long voteId) { this.voteId = voteId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getVoterCount() { return voterCount; }
        public void setVoterCount(Integer voterCount) { this.voterCount = voterCount; }
        public String getMyRole() { return myRole; }
        public void setMyRole(String myRole) { this.myRole = myRole; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class SlotDetail {
        private Long slotId;
        private String label;
        private Integer count;
        private Boolean isBest;
        private List<UserBrief> voters;

        public Long getSlotId() { return slotId; }
        public void setSlotId(Long slotId) { this.slotId = slotId; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Boolean getIsBest() { return isBest; }
        public void setIsBest(Boolean isBest) { this.isBest = isBest; }
        public List<UserBrief> getVoters() { return voters; }
        public void setVoters(List<UserBrief> voters) { this.voters = voters; }
    }

    public static class VoteDetail {
        private Long voteId;
        private String title;
        private String type;
        private String status;
        private UserBrief owner;
        private LocalDateTime deadline;
        private List<SlotDetail> slots;
        private List<Long> mySlotIds;
        private Long lockedSlotId;

        public Long getVoteId() { return voteId; }
        public void setVoteId(Long voteId) { this.voteId = voteId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public UserBrief getOwner() { return owner; }
        public void setOwner(UserBrief owner) { this.owner = owner; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
        public List<SlotDetail> getSlots() { return slots; }
        public void setSlots(List<SlotDetail> slots) { this.slots = slots; }
        public List<Long> getMySlotIds() { return mySlotIds; }
        public void setMySlotIds(List<Long> mySlotIds) { this.mySlotIds = mySlotIds; }
        public Long getLockedSlotId() { return lockedSlotId; }
        public void setLockedSlotId(Long lockedSlotId) { this.lockedSlotId = lockedSlotId; }
    }

    public static class MineResponse {
        private List<VoteSummary> items;

        public List<VoteSummary> getItems() { return items; }
        public void setItems(List<VoteSummary> items) { this.items = items; }
    }
}
