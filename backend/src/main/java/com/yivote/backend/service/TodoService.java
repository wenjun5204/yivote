package com.yivote.backend.service;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.TodoDtos.ItemCreate;
import com.yivote.backend.dto.TodoDtos.TodoClaimBrief;
import com.yivote.backend.dto.TodoDtos.TodoCounts;
import com.yivote.backend.dto.TodoDtos.TodoCreateRequest;
import com.yivote.backend.dto.TodoDtos.TodoDetail;
import com.yivote.backend.dto.TodoDtos.TodoItemDetail;
import com.yivote.backend.dto.TodoDtos.TodoSummary;
import com.yivote.backend.dto.VoteDtos.UserBrief;
import com.yivote.backend.entity.TodoClaimEntity;
import com.yivote.backend.entity.TodoEntity;
import com.yivote.backend.entity.TodoItemEntity;
import com.yivote.backend.entity.UserEntity;
import com.yivote.backend.repository.TodoClaimRepository;
import com.yivote.backend.repository.TodoItemRepository;
import com.yivote.backend.repository.TodoRepository;
import com.yivote.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 群待办(凑分工,PRD §7B):待办挂在活动上不挂在人上,不制造待办焦虑——
 * 不做逾期红点、不做个人任务中心;认领/放回/搞定全部实名可见(可改不可逃)。
 */
@Service
public class TodoService {

    private static final int MAX_ITEMS = 10;
    private static final int MAX_GID_LEN = 64;

    private final TodoRepository todoRepository;
    private final TodoItemRepository itemRepository;
    private final TodoClaimRepository claimRepository;
    private final UserRepository userRepository;

    public TodoService(TodoRepository todoRepository,
                       TodoItemRepository itemRepository,
                       TodoClaimRepository claimRepository,
                       UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TodoDetail create(TodoCreateRequest request) {
        String title = trim(request.getTitle());
        if (title.isEmpty()) {
            throw ApiException.param("给这顿起个名吧，如「周六球局」");
        }
        if (title.length() > 30) {
            throw ApiException.param("标题 30 字以内");
        }
        List<ItemCreate> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw ApiException.param("至少加一个事项");
        }
        if (items.size() > MAX_ITEMS) {
            throw ApiException.param("事项最多 " + MAX_ITEMS + " 个");
        }

        LocalDateTime now = LocalDateTime.now();
        TodoEntity todo = new TodoEntity();
        todo.setTitle(title);
        todo.setOwnerId(CurrentUser.get());
        todo.setOpenGId(trimToNull(request.getOpenGId()));
        todo.setStatus("OPEN");
        // 上游挂靠:目前只支持挂在投票(定档页一键发起);独立发起不填
        if ("VOTE".equals(request.getSourceType())) {
            todo.setSourceType("VOTE");
            todo.setSourceId(request.getSourceId());
        }
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        todo = todoRepository.save(todo);

        int order = 0;
        for (ItemCreate itemCreate : items) {
            String itemTitle = trim(itemCreate.getTitle());
            if (itemTitle.isEmpty()) {
                throw ApiException.param("有个事项名是空的，补一下再发");
            }
            if (itemTitle.length() > 10) {
                throw ApiException.param("事项名 10 字以内，如「订场」「买水」");
            }
            int capacity = itemCreate.getCapacity() == null ? 1 : itemCreate.getCapacity();
            if (capacity < 1 || capacity > 9) {
                throw ApiException.param("「" + itemTitle + "」的认领人数要在 1-9 之间");
            }
            TodoItemEntity item = new TodoItemEntity();
            item.setTodoId(todo.getId());
            item.setTitle(itemTitle);
            item.setCapacity(capacity);
            item.setSortOrder(order++);
            itemRepository.save(item);
        }
        return toDetail(todo, CurrentUser.get());
    }

    @Transactional(readOnly = true)
    public TodoDetail detail(Long todoId, Long userId) {
        return detail(todoId, null, userId);
    }

    /**
     * 详情(带群归档):首次有人带 openGId 从群卡片打开时归到该群——
     * 只在原值为空时写入(先到先得),与投票/收集同款。
     */
    @Transactional
    public TodoDetail detail(Long todoId, String openGId, Long userId) {
        TodoEntity todo = requireTodo(todoId);
        String gid = trimToNull(openGId);
        if (gid != null && todo.getOpenGId() == null && gid.length() <= MAX_GID_LEN) {
            todo.setOpenGId(gid);
            todo = todoRepository.save(todo);
        }
        return toDetail(todo, userId);
    }

    /** 我来:1 次点击认领成立;满员/重复点给明确反馈(不静默) */
    @Transactional
    public TodoDetail claim(Long todoId, Long itemId) {
        TodoEntity todo = requireTodo(todoId);
        requireItem(todoId, itemId);
        Long userId = CurrentUser.get();

        List<TodoClaimEntity> claims = claimRepository.findByItemIdOrderByCreatedAtAsc(itemId);
        for (TodoClaimEntity claim : claims) {
            if (claim.getUserId().equals(userId)) {
                throw ApiException.param("这项你已经认领了，去看看别的");
            }
        }
        int capacity = itemRepository.findById(itemId).map(TodoItemEntity::getCapacity).orElse(1);
        if (claims.size() >= capacity) {
            throw ApiException.param("这项位置满了，看看还有没有别的能搭把手");
        }
        TodoClaimEntity claim = new TodoClaimEntity();
        claim.setTodoId(todoId);
        claim.setItemId(itemId);
        claim.setUserId(userId);
        claim.setDone(false);
        claim.setCreatedAt(LocalDateTime.now());
        claimRepository.save(claim);

        touch(todo);
        return toDetail(todo, userId);
    }

    /** 转给别人:放回池子,全群可见;已搞定的不许悄悄消失 */
    @Transactional
    public TodoDetail release(Long todoId, Long itemId) {
        TodoEntity todo = requireTodo(todoId);
        requireItem(todoId, itemId);
        Long userId = CurrentUser.get();

        TodoClaimEntity mine = null;
        for (TodoClaimEntity claim : claimsOf(itemId)) {
            if (claim.getUserId().equals(userId)) {
                mine = claim;
                break;
            }
        }
        if (mine == null) {
            throw ApiException.param("这项你还没认领");
        }
        if (Boolean.TRUE.equals(mine.getDone())) {
            throw ApiException.param("已经搞定了就不用放回啦");
        }
        claimRepository.deleteByItemIdAndUserId(itemId, userId);

        touch(todo);
        return toDetail(todo, userId);
    }

    /** 搞定了:认领人打钩;全部满员且全部搞定 → 待办收口(DONE,进群日历) */
    @Transactional
    public TodoDetail done(Long todoId, Long itemId) {
        TodoEntity todo = requireTodo(todoId);
        requireItem(todoId, itemId);
        Long userId = CurrentUser.get();

        TodoClaimEntity mine = null;
        for (TodoClaimEntity claim : claimsOf(itemId)) {
            if (claim.getUserId().equals(userId)) {
                mine = claim;
                break;
            }
        }
        if (mine == null) {
            throw ApiException.param("先点「我来」认领这项，才能标搞定");
        }
        mine.setDone(true);
        claimRepository.save(mine);

        if (allDone(todoId)) {
            todo.setStatus("DONE");
        }
        touch(todo);
        return toDetail(todo, userId);
    }

    /** 我发起 + 我认领的(「我的事」聚合用) */
    @Transactional(readOnly = true)
    public List<TodoSummary> mine(Long userId) {
        Set<Long> todoIds = new HashSet<>();
        Map<Long, TodoEntity> byId = new LinkedHashMap<>();
        for (TodoEntity todo : todoRepository.findAll()) {
            if (todo.getOwnerId().equals(userId)) {
                todoIds.add(todo.getId());
                byId.put(todo.getId(), todo);
            }
        }
        for (TodoClaimEntity claim : claimRepository.findAll()) {
            if (claim.getUserId().equals(userId)) {
                todoIds.add(claim.getTodoId());
            }
        }
        for (Long todoId : todoIds) {
            if (!byId.containsKey(todoId)) {
                todoRepository.findById(todoId).ifPresent(t -> byId.put(todoId, t));
            }
        }
        return byId.values().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(todo -> toSummary(todo, userId))
                .toList();
    }

    /** 群板「进行中的分工」:本群 OPEN 状态的待办(群板 tab 数据源;DONE 的已沉淀进群日历) */
    @Transactional(readOnly = true)
    public List<TodoSummary> groupTodos(String openGId, Long userId) {
        return todoRepository.findByOpenGIdOrderByUpdatedAtDesc(trimToNull(openGId)).stream()
                .filter(todo -> "OPEN".equals(todo.getStatus()))
                .map(todo -> toSummary(todo, userId))
                .toList();
    }

    /* ---------------- 汇总 ---------------- */

    private TodoDetail toDetail(TodoEntity todo, Long userId) {
        TodoDetail detail = new TodoDetail();
        detail.setTodoId(todo.getId());
        detail.setTitle(todo.getTitle());
        detail.setStatus(todo.getStatus());
        detail.setOwner(toBrief(userRepository.findById(todo.getOwnerId()).orElse(null)));
        detail.setSourceType(todo.getSourceType());
        detail.setSourceId(todo.getSourceId());

        List<TodoItemEntity> items = itemRepository.findByTodoIdOrderBySortOrderAsc(todo.getId());
        List<TodoClaimEntity> claims = claimRepository.findByTodoIdOrderByCreatedAtAsc(todo.getId());

        Map<Long, List<TodoClaimEntity>> claimsByItem = new LinkedHashMap<>();
        for (TodoClaimEntity claim : claims) {
            claimsByItem.computeIfAbsent(claim.getItemId(), k -> new ArrayList<>()).add(claim);
        }

        List<TodoItemDetail> itemDetails = new ArrayList<>();
        TodoCounts counts = new TodoCounts();
        counts.setTotal(items.size());
        int doneCount = 0;
        int claimedCount = 0;
        for (TodoItemEntity item : items) {
            List<TodoClaimEntity> itemClaims = claimsByItem.getOrDefault(item.getId(), List.of());
            boolean allDone = !itemClaims.isEmpty()
                    && itemClaims.stream().allMatch(c -> Boolean.TRUE.equals(c.getDone()));
            boolean full = itemClaims.size() >= item.getCapacity();
            // 三态:空缺 OPEN / 已认领 CLAIMED(满员)/ 已完成 DONE;多人认领的未满员也亮头像但不计 CLAIMED
            String state = allDone ? "DONE" : (full ? "CLAIMED" : (itemClaims.isEmpty() ? "OPEN" : "CLAIMED"));

            TodoItemDetail id = new TodoItemDetail();
            id.setItemId(item.getId());
            id.setTitle(item.getTitle());
            id.setCapacity(item.getCapacity());
            id.setState(state);
            id.setSlotsLeft(Math.max(0, item.getCapacity() - itemClaims.size()));

            List<TodoClaimBrief> claimBriefs = new ArrayList<>();
            boolean myClaimed = false;
            boolean myDone = false;
            for (TodoClaimEntity claim : itemClaims) {
                TodoClaimBrief brief = new TodoClaimBrief();
                brief.setUser(toBrief(userRepository.findById(claim.getUserId()).orElse(null)));
                brief.setDone(claim.getDone());
                claimBriefs.add(brief);
                if (claim.getUserId().equals(userId)) {
                    myClaimed = true;
                    myDone = Boolean.TRUE.equals(claim.getDone());
                }
            }
            id.setClaims(claimBriefs);
            id.setMyClaimed(myClaimed);
            id.setMyDone(myDone);
            itemDetails.add(id);

            if (allDone) {
                doneCount++;
            } else if (full) {
                claimedCount++;
            }
        }
        counts.setDone(doneCount);
        counts.setClaimed(claimedCount);
        counts.setOpen(items.size() - doneCount - claimedCount);
        detail.setItems(itemDetails);
        detail.setSummary(counts);
        return detail;
    }

    private TodoSummary toSummary(TodoEntity todo, Long userId) {
        TodoSummary summary = new TodoSummary();
        summary.setTodoId(todo.getId());
        summary.setTitle(todo.getTitle());
        summary.setStatus(todo.getStatus());
        summary.setMyRole(todo.getOwnerId().equals(userId) ? "OWNER" : myRoleOf(todo.getId(), userId));
        summary.setUpdatedAt(todo.getUpdatedAt());

        // 汇总数字复用 toDetail 的推导(量级小,直接算)
        TodoDetail detail = toDetail(todo, userId);
        summary.setSummary(detail.getSummary());
        return summary;
    }

    /** 非发起人:认领过 = CLAIMER;纯路过(群板视角) = NONE */
    private String myRoleOf(Long todoId, Long userId) {
        for (TodoClaimEntity claim : claimRepository.findByTodoIdOrderByCreatedAtAsc(todoId)) {
            if (claim.getUserId().equals(userId)) {
                return "CLAIMER";
            }
        }
        return "NONE";
    }

    private boolean allDone(Long todoId) {
        List<TodoItemEntity> items = itemRepository.findByTodoIdOrderBySortOrderAsc(todoId);
        if (items.isEmpty()) {
            return false;
        }
        List<TodoClaimEntity> claims = claimRepository.findByTodoIdOrderByCreatedAtAsc(todoId);
        Map<Long, List<TodoClaimEntity>> byItem = new LinkedHashMap<>();
        for (TodoClaimEntity claim : claims) {
            byItem.computeIfAbsent(claim.getItemId(), k -> new ArrayList<>()).add(claim);
        }
        for (TodoItemEntity item : items) {
            List<TodoClaimEntity> itemClaims = byItem.getOrDefault(item.getId(), List.of());
            if (itemClaims.isEmpty()
                    || itemClaims.size() < item.getCapacity()
                    || itemClaims.stream().anyMatch(c -> !Boolean.TRUE.equals(c.getDone()))) {
                return false;
            }
        }
        return true;
    }

    private void touch(TodoEntity todo) {
        todo.setUpdatedAt(LocalDateTime.now());
        todoRepository.save(todo);
    }

    private List<TodoClaimEntity> claimsOf(Long itemId) {
        return claimRepository.findByItemIdOrderByCreatedAtAsc(itemId);
    }

    private TodoEntity requireTodo(Long todoId) {
        return todoRepository.findById(todoId)
                .orElseThrow(() -> ApiException.notFound("这个待办不存在，可能已被删除"));
    }

    private void requireItem(Long todoId, Long itemId) {
        TodoItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("这个事项不存在"));
        if (!item.getTodoId().equals(todoId)) {
            throw ApiException.notFound("这个事项不存在");
        }
    }

    private UserBrief toBrief(UserEntity user) {
        if (user == null) {
            return null;
        }
        UserBrief brief = new UserBrief();
        brief.setUserId(user.getId());
        brief.setNickname(user.getNickname());
        brief.setAvatarUrl(user.getAvatarUrl());
        return brief;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimToNull(String s) {
        String t = trim(s);
        return t.isEmpty() ? null : t;
    }
}
