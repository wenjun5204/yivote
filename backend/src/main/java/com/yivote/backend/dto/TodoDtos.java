package com.yivote.backend.dto;

import com.yivote.backend.dto.VoteDtos.UserBrief;
import java.time.LocalDateTime;
import java.util.List;

/** 群待办契约模型:对齐 api/openapi.yaml 的 Todo 系列 schema(PRD §7B 凑分工) */
public final class TodoDtos {

    private TodoDtos() {
    }

    public static class ItemCreate {
        private String title;
        private Integer capacity;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
    }

    public static class TodoCreateRequest {
        private String title;
        private String openGId;
        private String sourceType;   // VOTE / null
        private Long sourceId;
        private List<ItemCreate> items;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getOpenGId() { return openGId; }
        public void setOpenGId(String openGId) { this.openGId = openGId; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public List<ItemCreate> getItems() { return items; }
        public void setItems(List<ItemCreate> items) { this.items = items; }
    }

    public static class TodoClaimBrief {
        private UserBrief user;
        private Boolean done;

        public UserBrief getUser() { return user; }
        public void setUser(UserBrief user) { this.user = user; }
        public Boolean getDone() { return done; }
        public void setDone(Boolean done) { this.done = done; }
    }

    public static class TodoItemDetail {
        private Long itemId;
        private String title;
        private Integer capacity;
        private String state;           // OPEN / CLAIMED / DONE
        private Integer slotsLeft;
        private List<TodoClaimBrief> claims;
        private Boolean myClaimed;
        private Boolean myDone;

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public Integer getSlotsLeft() { return slotsLeft; }
        public void setSlotsLeft(Integer slotsLeft) { this.slotsLeft = slotsLeft; }
        public List<TodoClaimBrief> getClaims() { return claims; }
        public void setClaims(List<TodoClaimBrief> claims) { this.claims = claims; }
        public Boolean getMyClaimed() { return myClaimed; }
        public void setMyClaimed(Boolean myClaimed) { this.myClaimed = myClaimed; }
        public Boolean getMyDone() { return myDone; }
        public void setMyDone(Boolean myDone) { this.myDone = myDone; }
    }

    /** 看板汇总:盯催视角的「X 个事项 · 已认领 N · 已完成 M · 空缺 K」 */
    public static class TodoCounts {
        private Integer total;
        private Integer claimed;
        private Integer done;
        private Integer open;

        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public Integer getClaimed() { return claimed; }
        public void setClaimed(Integer claimed) { this.claimed = claimed; }
        public Integer getDone() { return done; }
        public void setDone(Integer done) { this.done = done; }
        public Integer getOpen() { return open; }
        public void setOpen(Integer open) { this.open = open; }
    }

    public static class TodoSummary {
        private Long todoId;
        private String title;
        private String status;      // OPEN / DONE
        private String myRole;       // OWNER / CLAIMER / NONE
        private TodoCounts summary;
        private LocalDateTime updatedAt;

        public Long getTodoId() { return todoId; }
        public void setTodoId(Long todoId) { this.todoId = todoId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMyRole() { return myRole; }
        public void setMyRole(String myRole) { this.myRole = myRole; }
        public TodoCounts getSummary() { return summary; }
        public void setSummary(TodoCounts summary) { this.summary = summary; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class TodoDetail {
        private Long todoId;
        private String title;
        private String status;
        private UserBrief owner;
        private String sourceType;
        private Long sourceId;
        private List<TodoItemDetail> items;
        private TodoCounts summary;

        public Long getTodoId() { return todoId; }
        public void setTodoId(Long todoId) { this.todoId = todoId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public UserBrief getOwner() { return owner; }
        public void setOwner(UserBrief owner) { this.owner = owner; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public List<TodoItemDetail> getItems() { return items; }
        public void setItems(List<TodoItemDetail> items) { this.items = items; }
        public TodoCounts getSummary() { return summary; }
        public void setSummary(TodoCounts summary) { this.summary = summary; }
    }

    public static class TodoMineResponse {
        private List<TodoSummary> items;

        public List<TodoSummary> getItems() { return items; }
        public void setItems(List<TodoSummary> items) { this.items = items; }
    }

    /** 群板「进行中的分工」:本群 OPEN 待办列表(复用 TodoSummary) */
    public static class TodoGroupResponse {
        private List<TodoSummary> items;

        public List<TodoSummary> getItems() { return items; }
        public void setItems(List<TodoSummary> items) { this.items = items; }
    }
}
