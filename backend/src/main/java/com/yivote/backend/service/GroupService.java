package com.yivote.backend.service;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.CollectDtos.CalendarActivity;
import com.yivote.backend.dto.CollectDtos.GroupCalendar;
import com.yivote.backend.dto.CollectDtos.GroupProfile;
import com.yivote.backend.dto.CollectDtos.RenameRequest;
import com.yivote.backend.entity.BallotEntity;
import com.yivote.backend.entity.CollectEntity;
import com.yivote.backend.entity.GroupProfileEntity;
import com.yivote.backend.entity.TodoClaimEntity;
import com.yivote.backend.entity.TodoEntity;
import com.yivote.backend.entity.VoteEntity;
import com.yivote.backend.repository.BallotRepository;
import com.yivote.backend.repository.CollectEntryRepository;
import com.yivote.backend.repository.CollectRepository;
import com.yivote.backend.repository.GroupProfileRepository;
import com.yivote.backend.repository.TodoClaimRepository;
import com.yivote.backend.repository.TodoRepository;
import com.yivote.backend.repository.VoteRepository;
import com.yivote.backend.repository.VoteSlotRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 群板 + 群日历(v1.3 沉淀):openGId 维度聚合。
 * 挂在群上,不挂在人上(退群重进历史还在);凝聚力分只升不降。
 */
@Service
public class GroupService {

    private static final String DEFAULT_NAME = "这个群";

    private final GroupProfileRepository profileRepository;
    private final VoteRepository voteRepository;
    private final CollectRepository collectRepository;
    private final BallotRepository ballotRepository;
    private final CollectEntryRepository entryRepository;
    private final VoteSlotRepository slotRepository;
    private final TodoRepository todoRepository;
    private final TodoClaimRepository todoClaimRepository;

    public GroupService(GroupProfileRepository profileRepository,
                        VoteRepository voteRepository,
                        CollectRepository collectRepository,
                        BallotRepository ballotRepository,
                        CollectEntryRepository entryRepository,
                        VoteSlotRepository slotRepository,
                        TodoRepository todoRepository,
                        TodoClaimRepository todoClaimRepository) {
        this.profileRepository = profileRepository;
        this.voteRepository = voteRepository;
        this.collectRepository = collectRepository;
        this.ballotRepository = ballotRepository;
        this.entryRepository = entryRepository;
        this.slotRepository = slotRepository;
        this.todoRepository = todoRepository;
        this.todoClaimRepository = todoClaimRepository;
    }

    /** 群档案(不存在则返回默认,不懒建——避免任意 openGId 刷行) */
    @Transactional(readOnly = true)
    public GroupProfile profile(String openGId) {
        requireGid(openGId);
        List<VoteEntity> votes = voteRepository.findByOpenGId(openGId);
        List<CollectEntity> collects = collectRepository.findByOpenGIdOrderByUpdatedAtDesc(openGId);
        List<TodoEntity> todos = todosOf(openGId);
        return buildProfile(openGId, votes, collects, todos);
    }

    @Transactional
    public GroupProfile rename(String openGId, RenameRequest request) {
        requireGid(openGId);
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw ApiException.param("群名不能是空的");
        }
        if (name.length() > 16) {
            throw ApiException.param("群名 16 字以内");
        }
        GroupProfileEntity profile = profileRepository.findByOpenGId(openGId).orElseGet(() -> {
            GroupProfileEntity p = new GroupProfileEntity();
            p.setOpenGId(openGId);
            return p;
        });
        profile.setName(name);
        profile.setRenamedBy(CurrentUser.get());
        profileRepository.save(profile);

        List<VoteEntity> votes = voteRepository.findByOpenGId(openGId);
        List<CollectEntity> collects = collectRepository.findByOpenGIdOrderByUpdatedAtDesc(openGId);
        List<TodoEntity> todos = todosOf(openGId);
        GroupProfile result = buildProfile(openGId, votes, collects, todos);
        result.setName(name);
        return result;
    }

    /** 群日历:某月活动分布(定档日优先,无定档用更新日)+ 链路小点 + 满链 */
    @Transactional(readOnly = true)
    public GroupCalendar calendar(String openGId, String month) {
        requireGid(openGId);
        YearMonth ym = parseMonth(month);

        List<VoteEntity> votes = voteRepository.findByOpenGId(openGId);
        List<CollectEntity> collects = collectRepository.findByOpenGIdOrderByUpdatedAtDesc(openGId);
        List<TodoEntity> todos = todosOf(openGId);

        GroupCalendar calendar = new GroupCalendar();
        calendar.setMonth(ym.toString());
        calendar.setProfile(buildProfile(openGId, votes, collects, todos));

        List<CalendarActivity> activities = new ArrayList<>();
        for (VoteEntity vote : votes) {
            LocalDate date = activityDate(vote.getUpdatedAt());
            if (date == null || !YearMonth.from(date).equals(ym)) {
                continue;
            }
            CalendarActivity activity = new CalendarActivity();
            activity.setDate(date.toString());
            activity.setTitle(vote.getTitle());
            activity.setKind("VOTE");
            activity.setRefId(vote.getId());
            activity.setAttendeeCount(countVoters(vote.getId()));
            // 链路小点:投票下挂着待办(定档页一键发起的分工)则点亮 todo
            boolean hasTodo = todos.stream().anyMatch(t ->
                    vote.getId().equals(t.getSourceId()) && "VOTE".equals(t.getSourceType()));
            activity.setChain(hasTodo ? List.of("vote", "todo") : List.of("vote"));
            activity.setFullChain(false);
            activities.add(activity);
        }
        for (CollectEntity collect : collects) {
            LocalDate date = activityDate(collect.getUpdatedAt());
            if (date == null || !YearMonth.from(date).equals(ym)) {
                continue;
            }
            CalendarActivity activity = new CalendarActivity();
            activity.setDate(date.toString());
            activity.setTitle(collect.getTitle());
            activity.setKind("COLLECT");
            activity.setRefId(collect.getId());
            activity.setAttendeeCount(entryRepository.findByCollectIdOrderByUpdatedAtAsc(collect.getId()).size());
            activity.setChain(List.of("collect"));
            activity.setFullChain(false);
            activities.add(activity);
        }
        // 独立待办是自己的活动;挂在投票下的不另立卡(体现为投票卡的 todo 小点)
        for (TodoEntity todo : todos) {
            if (todo.getSourceType() != null) {
                continue;
            }
            LocalDate date = activityDate(todo.getUpdatedAt());
            if (date == null || !YearMonth.from(date).equals(ym)) {
                continue;
            }
            CalendarActivity activity = new CalendarActivity();
            activity.setDate(date.toString());
            activity.setTitle(todo.getTitle());
            activity.setKind("TODO");
            activity.setRefId(todo.getId());
            activity.setAttendeeCount(countClaimants(todo.getId()));
            activity.setChain(List.of("todo"));
            activity.setFullChain(false);
            activities.add(activity);
        }
        activities.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        calendar.setActivities(activities);
        return calendar;
    }

    /* ---------------- 汇总 ---------------- */

    private GroupProfile buildProfile(String openGId, List<VoteEntity> votes,
                                      List<CollectEntity> collects, List<TodoEntity> todos) {
        GroupProfile profile = new GroupProfile();
        profile.setOpenGId(openGId);
        profile.setName(profileRepository.findByOpenGId(openGId)
                .map(GroupProfileEntity::getName).orElse(DEFAULT_NAME));
        // 场活动数:投票 + 收集 + 独立待办;挂在投票下的待办不重复计
        long standaloneTodos = todos.stream().filter(t -> t.getSourceType() == null).count();
        profile.setActivityCount((int) (votes.size() + collects.size() + standaloneTodos));

        int attendeeTotal = 0;
        for (VoteEntity vote : votes) {
            attendeeTotal += countVoters(vote.getId());
        }
        for (CollectEntity collect : collects) {
            attendeeTotal += entryRepository.findByCollectIdOrderByUpdatedAtAsc(collect.getId()).size();
        }
        for (TodoEntity todo : todos) {
            attendeeTotal += countClaimants(todo.getId());
        }
        profile.setAttendeeTotal(attendeeTotal);

        // 凝聚力分(简化公式,只升不降):活动×10 + 人次×1,封顶 100
        int score = Math.min(100, profile.getActivityCount() * 10 + attendeeTotal);
        profile.setCohesionScore(score);
        return profile;
    }

    /** 群下的待办(含挂在投票下的——用于链路小点) */
    private List<TodoEntity> todosOf(String openGId) {
        return todoRepository.findByOpenGIdOrderByUpdatedAtDesc(openGId);
    }

    private int countClaimants(Long todoId) {
        Set<Long> ids = new HashSet<>();
        for (TodoClaimEntity c : todoClaimRepository.findByTodoIdOrderByCreatedAtAsc(todoId)) {
            ids.add(c.getUserId());
        }
        return ids.size();
    }

    private int countVoters(Long voteId) {
        Set<Long> ids = new HashSet<>();
        for (BallotEntity b : ballotRepository.findByVoteId(voteId)) {
            ids.add(b.getUserId());
        }
        return ids.size();
    }

    private static LocalDate activityDate(java.time.LocalDateTime updatedAt) {
        return updatedAt == null ? null : updatedAt.toLocalDate();
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw ApiException.param("月份格式不对，应该像 2026-09 这样");
        }
    }

    private static void requireGid(String openGId) {
        if (openGId == null || openGId.isBlank()) {
            throw ApiException.param("缺少群标识，请从群里的卡片进入");
        }
    }
}
