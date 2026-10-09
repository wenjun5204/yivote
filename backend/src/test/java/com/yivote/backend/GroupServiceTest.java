package com.yivote.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.AuthDtos.LoginRequest;
import com.yivote.backend.dto.AuthDtos.LoginResponse;
import com.yivote.backend.dto.CollectDtos.CollectCreateRequest;
import com.yivote.backend.dto.CollectDtos.CollectDetail;
import com.yivote.backend.dto.CollectDtos.FieldCreate;
import com.yivote.backend.dto.CollectDtos.GroupCalendar;
import com.yivote.backend.dto.CollectDtos.GroupProfile;
import com.yivote.backend.dto.CollectDtos.RenameRequest;
import com.yivote.backend.dto.TodoDtos.ItemCreate;
import com.yivote.backend.dto.TodoDtos.TodoCreateRequest;
import com.yivote.backend.dto.TodoDtos.TodoDetail;
import com.yivote.backend.dto.VoteDtos.BallotSubmitRequest;
import com.yivote.backend.dto.VoteDtos.VoteCreateRequest;
import com.yivote.backend.dto.VoteDtos.VoteDetail;
import com.yivote.backend.service.AuthService;
import com.yivote.backend.service.CollectService;
import com.yivote.backend.service.GroupService;
import com.yivote.backend.service.TodoService;
import com.yivote.backend.service.VoteService;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 群档案 + 群日历:openGId 聚合,凝聚力分只升不降,月视图活动分布 */
@SpringBootTest
class GroupServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private VoteService voteService;
    @Autowired
    private CollectService collectService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private TodoService todoService;

    @Test
    void profileAggregatesAndRenameShared() {
        String gid = "og-agg-" + System.nanoTime();
        Long owner = login("g-owner-" + System.nanoTime());
        Long member = login("g-member-" + System.nanoTime());

        // 一场投票 + 一张收集表,群友都参与
        VoteDetail vote = createVote(owner, gid, "球局");
        CurrentUser.set(member);
        BallotSubmitRequest ballot = new BallotSubmitRequest();
        ballot.setSlotIds(List.of(vote.getSlots().get(0).getSlotId()));
        voteService.submitBallot(vote.getVoteId(), ballot);

        CollectDetail collect = createCollect(owner, gid, "报名表");
        EntrySubmit(member, collect.getCollectId());

        // 群档案:2 活动 2 人次,凝聚力分 = 2*10 + 2 = 22
        GroupProfile profile = groupService.profile(gid);
        assertEquals(2, profile.getActivityCount());
        assertEquals(2, profile.getAttendeeTotal());
        assertEquals(22, profile.getCohesionScore());
        assertEquals("这个群", profile.getName(), "未改名时用默认名");

        // 成员改名,全群共享
        CurrentUser.set(member);
        RenameRequest rename = new RenameRequest();
        rename.setName("常打球的这群人");
        GroupProfile renamed = groupService.rename(gid, rename);
        assertEquals("常打球的这群人", renamed.getName());
        assertEquals("常打球的这群人", groupService.profile(gid).getName(), "改名对其他成员可见");
    }

    @Test
    void calendarListsCurrentMonthOnly() {
        String gid = "og-cal-" + System.nanoTime();
        Long owner = login("cal-owner-" + System.nanoTime());
        createVote(owner, gid, "本月球局");
        createCollect(owner, gid, "本月报名");

        GroupCalendar calendar = groupService.calendar(gid, YearMonth.now().toString());
        assertEquals(2, calendar.getActivities().size());
        assertEquals(YearMonth.now().toString(), calendar.getMonth());
        assertTrue(calendar.getActivities().get(0).getChain().contains("vote"));

        // 下个月应无活动
        GroupCalendar next = groupService.calendar(gid, YearMonth.now().plusMonths(1).toString());
        assertEquals(0, next.getActivities().size());
    }

    @Test
    void missingGidRejected() {
        assertThrows(ApiException.class, () -> groupService.profile(""));
        assertThrows(ApiException.class, () -> groupService.calendar(" ", "2026-09"));
    }

    @Test
    void todoAggregatesIntoProfileAndCalendar() {
        String gid = "og-todo-agg-" + System.nanoTime();
        Long owner = login("todo-agg-owner-" + System.nanoTime());
        Long member = login("todo-agg-member-" + System.nanoTime());

        // 一场投票 + 挂在它下面的待办(定档页一键发起)+ 一张独立待办
        VoteDetail vote = createVote(owner, gid, "球局");
        CurrentUser.set(owner);
        TodoCreateRequest linked = new TodoCreateRequest();
        linked.setTitle("球局分工");
        linked.setOpenGId(gid);
        linked.setSourceType("VOTE");
        linked.setSourceId(vote.getVoteId());
        linked.setItems(List.of(todoItem("订场")));
        todoService.create(linked);

        TodoCreateRequest standalone = new TodoCreateRequest();
        standalone.setTitle("聚餐分工");
        standalone.setOpenGId(gid);
        standalone.setItems(List.of(todoItem("定餐厅"), todoItem("买水")));
        TodoDetail standaloneTodo = todoService.create(standalone);

        // 群友认领,产生人次
        CurrentUser.set(member);
        Long itemId = standaloneTodo.getItems().get(0).getItemId();
        todoService.claim(standaloneTodo.getTodoId(), itemId);

        // 档案:投票 1 + 独立待办 1(挂在投票下的不重复计)= 2 场
        GroupProfile profile = groupService.profile(gid);
        assertEquals(2, profile.getActivityCount(), "挂靠待办不重复计场");

        // 日历:投票卡点亮 todo 链路点;独立待办自己立卡
        GroupCalendar calendar = groupService.calendar(gid, YearMonth.now().toString());
        assertEquals(2, calendar.getActivities().size());
        var voteActivity = calendar.getActivities().stream()
                .filter(a -> "VOTE".equals(a.getKind())).findFirst().orElseThrow();
        assertTrue(voteActivity.getChain().contains("todo"), "挂靠待办点亮投票的 todo 链路点");
        var todoActivity = calendar.getActivities().stream()
                .filter(a -> "TODO".equals(a.getKind())).findFirst().orElseThrow();
        assertEquals(standaloneTodo.getTodoId(), todoActivity.getRefId());
        assertEquals(1, todoActivity.getAttendeeCount(), "认领人次聚合");
    }

    @Test
    void detailOpensArchiveToGroupFirstComeFirstServed() {
        Long owner = login("arc-owner-" + System.nanoTime());
        Long member = login("arc-member-" + System.nanoTime());

        // 发起时不带群标识(创建在小程序内,群归属未知)
        VoteDetail vote = createVote(owner, null, "散投球局");
        CollectDetail collect = createCollect(owner, null, "散投报名表");

        // 群友 A 从「og-arc-a」的群卡片打开 → 归档到该群
        CurrentUser.set(member);
        voteService.detail(vote.getVoteId(), "og-arc-a", member);
        collectService.detail(collect.getCollectId(), "og-arc-a", member);
        assertEquals(2, groupService.profile("og-arc-a").getActivityCount(), "首次带卡片打开应归档");

        // 群友 B 从另一个群的卡片打开 → 不覆盖已归档的群
        Long other = login("arc-other-" + System.nanoTime());
        CurrentUser.set(other);
        voteService.detail(vote.getVoteId(), "og-arc-b", other);
        collectService.detail(collect.getCollectId(), "og-arc-b", other);
        assertEquals(0, groupService.profile("og-arc-b").getActivityCount(), "后续打开不重复归档");
        assertEquals(2, groupService.profile("og-arc-a").getActivityCount());
    }

    /* ---------------- helpers ---------------- */

    private static ItemCreate todoItem(String title) {
        ItemCreate create = new ItemCreate();
        create.setTitle(title);
        return create;
    }

    private Long login(String code) {
        LoginRequest req = new LoginRequest();
        req.setCode(code);
        LoginResponse resp = authService.login(req, null);
        return resp.getUserId();
    }

    private VoteDetail createVote(Long userId, String gid, String title) {
        CurrentUser.set(userId);
        VoteCreateRequest request = new VoteCreateRequest();
        request.setTitle(title);
        request.setType("TIME");
        request.setOpenGId(gid);
        request.setSlots(List.of("周六下午", "周日晚").stream().map(label -> {
            com.yivote.backend.dto.VoteDtos.SlotCreate slot = new com.yivote.backend.dto.VoteDtos.SlotCreate();
            slot.setLabel(label);
            return slot;
        }).toList());
        return voteService.create(request);
    }

    private CollectDetail createCollect(Long userId, String gid, String title) {
        CurrentUser.set(userId);
        CollectCreateRequest request = new CollectCreateRequest();
        request.setTitle(title);
        request.setOpenGId(gid);
        FieldCreate field = new FieldCreate();
        field.setLabel("手机号");
        field.setKind("TEXT");
        request.setFields(List.of(field));
        return collectService.create(request);
    }

    private void EntrySubmit(Long userId, Long collectId) {
        CurrentUser.set(userId);
        com.yivote.backend.dto.CollectDtos.EntrySubmitRequest entry =
                new com.yivote.backend.dto.CollectDtos.EntrySubmitRequest();
        entry.setValues(List.of("13800001111"));
        collectService.submitEntry(collectId, entry);
    }
}
