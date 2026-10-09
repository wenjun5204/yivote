package com.yivote.backend.dto;

import java.util.List;

/** 收集/群档案契约模型:对齐 api/openapi.yaml 的 Collect / Group / Calendar 系列 schema */
public final class CollectDtos {

    private CollectDtos() {
    }

    public static class FieldCreate {
        private String label;
        private String kind;          // TEXT / CHIP
        private List<String> options;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
    }

    public static class CollectCreateRequest {
        private String title;
        private String openGId;
        private List<FieldCreate> fields;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getOpenGId() { return openGId; }
        public void setOpenGId(String openGId) { this.openGId = openGId; }
        public List<FieldCreate> getFields() { return fields; }
        public void setFields(List<FieldCreate> fields) { this.fields = fields; }
    }

    public static class EntrySubmitRequest {
        private List<String> values;

        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
    }

    public static class FieldDetail {
        private Long fieldId;
        private String label;
        private String kind;
        private List<String> options;

        public Long getFieldId() { return fieldId; }
        public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
    }

    public static class EntryDetail {
        private VoteDtos.UserBrief user;
        private List<String> values;

        public VoteDtos.UserBrief getUser() { return user; }
        public void setUser(VoteDtos.UserBrief user) { this.user = user; }
        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
    }

    public static class CollectDetail {
        private Long collectId;
        private String title;
        private String status;
        private VoteDtos.UserBrief owner;
        private List<FieldDetail> fields;
        private List<EntryDetail> entries;
        private List<String> myValues;
        private Integer filledCount;
        private String notFilledHint;

        public Long getCollectId() { return collectId; }
        public void setCollectId(Long collectId) { this.collectId = collectId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public VoteDtos.UserBrief getOwner() { return owner; }
        public void setOwner(VoteDtos.UserBrief owner) { this.owner = owner; }
        public List<FieldDetail> getFields() { return fields; }
        public void setFields(List<FieldDetail> fields) { this.fields = fields; }
        public List<EntryDetail> getEntries() { return entries; }
        public void setEntries(List<EntryDetail> entries) { this.entries = entries; }
        public List<String> getMyValues() { return myValues; }
        public void setMyValues(List<String> myValues) { this.myValues = myValues; }
        public Integer getFilledCount() { return filledCount; }
        public void setFilledCount(Integer filledCount) { this.filledCount = filledCount; }
        public String getNotFilledHint() { return notFilledHint; }
        public void setNotFilledHint(String notFilledHint) { this.notFilledHint = notFilledHint; }
    }

    public static class ExportResponse {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class RenameRequest {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class GroupProfile {
        private String openGId;
        private String name;
        private Integer activityCount;
        private Integer attendeeTotal;
        private Integer cohesionScore;

        public String getOpenGId() { return openGId; }
        public void setOpenGId(String openGId) { this.openGId = openGId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getActivityCount() { return activityCount; }
        public void setActivityCount(Integer activityCount) { this.activityCount = activityCount; }
        public Integer getAttendeeTotal() { return attendeeTotal; }
        public void setAttendeeTotal(Integer attendeeTotal) { this.attendeeTotal = attendeeTotal; }
        public Integer getCohesionScore() { return cohesionScore; }
        public void setCohesionScore(Integer cohesionScore) { this.cohesionScore = cohesionScore; }
    }

    public static class CalendarActivity {
        private String date;          // yyyy-MM-dd
        private String title;
        private String kind;          // VOTE / COLLECT / TODO
        private Long refId;
        private List<String> chain;
        private Integer attendeeCount;
        private Boolean fullChain;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public Long getRefId() { return refId; }
        public void setRefId(Long refId) { this.refId = refId; }
        public List<String> getChain() { return chain; }
        public void setChain(List<String> chain) { this.chain = chain; }
        public Integer getAttendeeCount() { return attendeeCount; }
        public void setAttendeeCount(Integer attendeeCount) { this.attendeeCount = attendeeCount; }
        public Boolean getFullChain() { return fullChain; }
        public void setFullChain(Boolean fullChain) { this.fullChain = fullChain; }
    }

    public static class GroupCalendar {
        private String month;         // yyyy-MM
        private List<CalendarActivity> activities;
        private GroupProfile profile;

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public List<CalendarActivity> getActivities() { return activities; }
        public void setActivities(List<CalendarActivity> activities) { this.activities = activities; }
        public GroupProfile getProfile() { return profile; }
        public void setProfile(GroupProfile profile) { this.profile = profile; }
    }

    /** 「我的事」聚合用的收集列表项 */
    public static class CollectSummary {
        private Long collectId;
        private String title;
        private String status;
        private String myRole;       // OWNER / FILLER / NONE
        private Integer filledCount;
        private Boolean myFilled;
        private java.time.LocalDateTime updatedAt;

        public Long getCollectId() { return collectId; }
        public void setCollectId(Long collectId) { this.collectId = collectId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMyRole() { return myRole; }
        public void setMyRole(String myRole) { this.myRole = myRole; }
        public Integer getFilledCount() { return filledCount; }
        public void setFilledCount(Integer filledCount) { this.filledCount = filledCount; }
        public Boolean getMyFilled() { return myFilled; }
        public void setMyFilled(Boolean myFilled) { this.myFilled = myFilled; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class CollectMineResponse {
        private List<CollectSummary> items;

        public List<CollectSummary> getItems() { return items; }
        public void setItems(List<CollectSummary> items) { this.items = items; }
    }
}
