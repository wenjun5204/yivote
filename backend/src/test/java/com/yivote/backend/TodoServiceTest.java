package com.yivote.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.AuthDtos.LoginRequest;
import com.yivote.backend.dto.AuthDtos.LoginResponse;
import com.yivote.backend.dto.TodoDtos.ItemCreate;
import com.yivote.backend.dto.TodoDtos.TodoCreateRequest;
import com.yivote.backend.dto.TodoDtos.TodoDetail;
import com.yivote.backend.dto.TodoDtos.TodoItemDetail;
import com.yivote.backend.dto.TodoDtos.TodoSummary;
import com.yivote.backend.service.AuthService;
import com.yivote.backend.service.TodoService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 群待办链路(PRD §7B):发起 → 认领 → 转让/搞定 → 收口 */
@SpringBootTest
class TodoServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private TodoService todoService;

    private Long ownerId;
    private Long memberA;
    private Long memberB;

    @BeforeEach
    void setUp() {
        ownerId = login("todo-owner-" + System.nanoTime());
        memberA = login("todo-a-" + System.nanoTime());
        memberB = login("todo-b-" + System.nanoTime());
    }

    private Long login(String code) {
        LoginRequest req = new LoginRequest();
        req.setCode(code);
        LoginResponse resp = authService.login(req, null);
        return resp.getUserId();
    }

    private TodoDetail createBallGameTodo(Long userId) {
        CurrentUser.set(userId);
        TodoCreateRequest request = new TodoCreateRequest();
        request.setTitle("周六球局");
        request.setOpenGId("og-todo-" + System.nanoTime());
        request.setItems(List.of(
                item("订场", 1),
                item("带球", 1),
                item("买水", 2)));
        return todoService.create(request);
    }

    private static ItemCreate item(String title, int capacity) {
        ItemCreate create = new ItemCreate();
        create.setTitle(title);
        create.setCapacity(capacity);
        return create;
    }

    @Test
    void createWithCapacity() {
        TodoDetail created = createBallGameTodo(ownerId);
        assertNotNull(created.getTodoId());
        assertEquals("OPEN", created.getStatus());
        assertEquals(3, created.getItems().size());
        TodoItemDetail buyWater = created.getItems().get(2);
        assertEquals(2, buyWater.getCapacity());
        assertEquals("OPEN", buyWater.getState());
        assertEquals(3, created.getSummary().getTotal());
        assertEquals(3, created.getSummary().getOpen());
    }

    @Test
    void createValidatesItems() {
        CurrentUser.set(ownerId);
        TodoCreateRequest empty = new TodoCreateRequest();
        empty.setTitle("空事项");
        empty.setItems(List.of());
        assertThrows(ApiException.class, () -> todoService.create(empty));

        TodoCreateRequest longTitle = new TodoCreateRequest();
        longTitle.setTitle("名字超长");
        longTitle.setItems(List.of(item("这是一个超过十个字的事项名字", 1)));
        ApiException ex = assertThrows(ApiException.class, () -> todoService.create(longTitle));
        assertTrue(ex.getMessage().contains("10 字以内"));
    }

    @Test
    void claimFillCapacityAndRelease() {
        TodoDetail created = createBallGameTodo(ownerId);
        Long buyWaterId = created.getItems().get(2).getItemId();

        // 多人可认领:两人都点「我来」
        CurrentUser.set(memberA);
        TodoDetail afterA = todoService.claim(created.getTodoId(), buyWaterId);
        assertTrue(afterA.getItems().get(2).getMyClaimed());
        assertEquals(1, afterA.getItems().get(2).getSlotsLeft());

        CurrentUser.set(memberB);
        todoService.claim(created.getTodoId(), buyWaterId);
        // 满员后第三人被明确拒绝
        CurrentUser.set(ownerId);
        ApiException full = assertThrows(ApiException.class,
                () -> todoService.claim(created.getTodoId(), buyWaterId));
        assertTrue(full.getMessage().contains("满了"));

        // 放回池子:memberB 转给别人
        CurrentUser.set(memberB);
        TodoDetail released = todoService.release(created.getTodoId(), buyWaterId);
        assertFalse(released.getItems().get(2).getMyClaimed());
        assertEquals(1, released.getItems().get(2).getSlotsLeft());
    }

    @Test
    void doneRequiresClaimAndClosesTodoWhenAllDone() {
        TodoDetail created = createBallGameTodo(ownerId);
        Long bookId = created.getItems().get(0).getItemId();
        Long ballId = created.getItems().get(1).getItemId();
        Long waterId = created.getItems().get(2).getItemId();

        // 没认领就想标搞定 → 拒绝
        CurrentUser.set(memberA);
        ApiException noClaim = assertThrows(ApiException.class,
                () -> todoService.done(created.getTodoId(), bookId));
        assertTrue(noClaim.getMessage().contains("认领"));

        CurrentUser.set(memberA);
        todoService.claim(created.getTodoId(), bookId);
        CurrentUser.set(memberB);
        todoService.claim(created.getTodoId(), ballId);
        todoService.claim(created.getTodoId(), waterId);
        CurrentUser.set(ownerId);
        todoService.claim(created.getTodoId(), waterId);

        CurrentUser.set(memberA);
        todoService.done(created.getTodoId(), bookId);
        CurrentUser.set(memberB);
        todoService.done(created.getTodoId(), ballId);
        todoService.done(created.getTodoId(), waterId);

        // 还差 owner 的水没搞定:未收口
        CurrentUser.set(ownerId);
        TodoDetail notYet = todoService.detail(created.getTodoId(), null, ownerId);
        assertEquals("OPEN", notYet.getStatus());

        // 已搞定的不能悄悄放回
        CurrentUser.set(memberB);
        ApiException doneRelease = assertThrows(ApiException.class,
                () -> todoService.release(created.getTodoId(), ballId));
        assertTrue(doneRelease.getMessage().contains("搞定"));

        CurrentUser.set(ownerId);
        todoService.done(created.getTodoId(), waterId);
        TodoDetail closed = todoService.detail(created.getTodoId(), null, ownerId);
        assertEquals("DONE", closed.getStatus(), "全部满员且搞定即收口");
        assertEquals(3, closed.getSummary().getDone());
    }

    @Test
    void openGidArchivedFirstComeFirstServed() {
        TodoDetail created = createBallGameTodo(ownerId);
        String gid = "og-first-" + System.nanoTime();

        // 首次带群标识打开:归档
        CurrentUser.set(memberA);
        todoService.detail(created.getTodoId(), gid, memberA);
        // 第二个群打开:不覆盖
        todoService.detail(created.getTodoId(), "og-other-" + System.nanoTime(), memberA);
        TodoDetail after = todoService.detail(created.getTodoId(), null, memberA);
        // 契约不回传 openGId,验证方式:第二个群日历里查不到
        // (由 GroupServiceTest 覆盖档案聚合,这里只保证不抛错且状态不变)
        assertEquals("OPEN", after.getStatus());
    }

    @Test
    void mineListsOwnedAndClaimed() {
        TodoDetail created = createBallGameTodo(ownerId);
        Long ballId = created.getItems().get(1).getItemId();

        CurrentUser.set(memberA);
        todoService.claim(created.getTodoId(), ballId);

        CurrentUser.set(memberA);
        List<TodoSummary> mine = todoService.mine(memberA);
        assertTrue(mine.stream().anyMatch(s -> s.getTodoId().equals(created.getTodoId())),
                "认领过的出现在我的事");
        assertEquals("CLAIMER", mine.stream()
                .filter(s -> s.getTodoId().equals(created.getTodoId()))
                .findFirst().orElseThrow().getMyRole());

        CurrentUser.set(memberB);
        assertTrue(todoService.mine(memberB).stream()
                        .noneMatch(s -> s.getTodoId().equals(created.getTodoId())),
                "没参与过的不出现");
    }
}
