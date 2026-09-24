package com.yivote.backend.service;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.VoteDtos.BallotSubmitRequest;
import com.yivote.backend.dto.VoteDtos.LockRequest;
import com.yivote.backend.dto.VoteDtos.SlotCreate;
import com.yivote.backend.dto.VoteDtos.SlotDetail;
import com.yivote.backend.dto.VoteDtos.UserBrief;
import com.yivote.backend.dto.VoteDtos.VoteCreateRequest;
import com.yivote.backend.dto.VoteDtos.VoteDetail;
import com.yivote.backend.dto.VoteDtos.VoteSummary;
import com.yivote.backend.entity.BallotEntity;
import com.yivote.backend.entity.UserEntity;
import com.yivote.backend.entity.VoteEntity;
import com.yivote.backend.entity.VoteSlotEntity;
import com.yivote.backend.repository.BallotRepository;
import com.yivote.backend.repository.UserRepository;
import com.yivote.backend.repository.VoteRepository;
import com.yivote.backend.repository.VoteSlotRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 群投票核心逻辑:凑时间(多选聚合+最优时段高亮)与拿主意(单选)。
 * 信任三原则:实名(票附 userId) / 可改不可逃(整票覆盖式改投) / 永不惩罚(无截止强锁定)。
 */
@Service
public class VoteService {

    private static final int MAX_SLOTS = 8;
    private static final int MAX_GID_LEN = 64;

    private final VoteRepository voteRepository;
    private final VoteSlotRepository slotRepository;
    private final BallotRepository ballotRepository;
    private final UserRepository userRepository;

    public VoteService(VoteRepository voteRepository,
                       VoteSlotRepository slotRepository,
                       BallotRepository ballotRepository,
                       UserRepository userRepository) {
        this.voteRepository = voteRepository;
        this.slotRepository = slotRepository;
        this.ballotRepository = ballotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VoteDetail create(VoteCreateRequest request) {
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.isEmpty()) {
            throw ApiException.param("给投票起个标题吧");
        }
        if (title.length() > 30) {
            throw ApiException.param("标题太长了,30 字以内");
        }
        String type = request.getType();
        if (!"TIME".equals(type) && !"CHOICE".equals(type)) {
            throw ApiException.param("投票类型不对,请重新发起");
        }
        List<SlotCreate> slots = request.getSlots();
        if (slots == null || slots.isEmpty()) {
            throw ApiException.param("至少加一个时间或选项");
        }
        if (slots.size() > MAX_SLOTS) {
            throw ApiException.param("选项最多 " + MAX_SLOTS + " 个,太多了大家懒得投");
        }

        LocalDateTime now = LocalDateTime.now();
        VoteEntity vote = new VoteEntity();
        vote.setTitle(title);
        vote.setType(type);
        vote.setOwnerId(CurrentUser.get());
        vote.setOpenGId(trimToNull(request.getOpenGId()));
        vote.setDeadline(request.getDeadline());
        vote.setStatus("OPEN");
        vote.setCreatedAt(now);
        vote.setUpdatedAt(now);
        vote = voteRepository.save(vote);

        int order = 0;
        for (SlotCreate slotCreate : slots) {
            String label = slotCreate.getLabel() == null ? "" : slotCreate.getLabel().trim();
            if (label.isEmpty()) {
                throw ApiException.param("有个选项是空的,补一下再发");
            }
            VoteSlotEntity slot = new VoteSlotEntity();
            slot.setVoteId(vote.getId());
            slot.setLabel(label);
            slot.setSortOrder(order++);
            slotRepository.save(slot);
        }
        return detail(vote.getId(), CurrentUser.get());
    }

    @Transactional(readOnly = true)
    public VoteDetail detail(Long voteId, Long userId) {
        return detail(voteId, null, userId);
    }

    /**
     * 详情(带群归档):首次有人带 openGId 从群卡片打开时,把投票归到该群——
     * 只在原值为空时写入(先到先得),后续不同群的卡片打开不覆盖。
     */
    @Transactional
    public VoteDetail detail(Long voteId, String openGId, Long userId) {
        VoteEntity vote = requireVote(voteId);
        String gid = trimToNull(openGId);
        if (gid != null && vote.getOpenGId() == null && gid.length() <= MAX_GID_LEN) {
            vote.setOpenGId(gid);
            vote = voteRepository.save(vote);
        }
        return toDetail(vote, userId);
    }

    /** 提交/改投:整票覆盖(旧票全删再插新票),「可改不可逃」的契约实现 */
    @Transactional
    public VoteDetail submitBallot(Long voteId, BallotSubmitRequest request) {
        VoteEntity vote = requireVote(voteId);
        if (!"OPEN".equals(vote.getStatus())) {
            throw ApiException.param("这个投票已经定档了,看看结果吧");
        }
        List<Long> slotIds = request.getSlotIds();
        if (slotIds == null || slotIds.isEmpty()) {
            throw ApiException.param("点亮至少一个时间");
        }
        Long userId = CurrentUser.get();

        List<VoteSlotEntity> slots = slotRepository.findByVoteIdOrderBySortOrderAsc(voteId);
        Set<Long> validSlotIds = new HashSet<>();
        slots.forEach(s -> validSlotIds.add(s.getId()));
        for (Long slotId : slotIds) {
            if (!validSlotIds.contains(slotId)) {
                throw ApiException.param("选的时间不在列表里,刷新一下再试");
            }
        }
        if ("CHOICE".equals(vote.getType()) && slotIds.size() > 1) {
            throw ApiException.param("这个投票只能选一个");
        }

        ballotRepository.deleteByVoteIdAndUserId(voteId, userId);
        for (Long slotId : slotIds) {
            BallotEntity ballot = new BallotEntity();
            ballot.setVoteId(voteId);
            ballot.setSlotId(slotId);
            ballot.setUserId(userId);
            ballotRepository.save(ballot);
        }
        vote.setUpdatedAt(LocalDateTime.now());
        voteRepository.save(vote);
        return toDetail(vote, userId);
    }

    /** 定档:组织者收口;该时段投票者=到场名单 */
    @Transactional
    public VoteDetail lock(Long voteId, LockRequest request) {
        VoteEntity vote = requireVote(voteId);
        Long userId = CurrentUser.get();
        if (!vote.getOwnerId().equals(userId)) {
            throw ApiException.forbidden("只有发起人能定时间");
        }
        if (!"OPEN".equals(vote.getStatus())) {
            throw ApiException.param("已经定过档了");
        }
        Long slotId = request.getSlotId();
        List<VoteSlotEntity> slots = slotRepository.findByVoteIdOrderBySortOrderAsc(voteId);
        boolean valid = slots.stream().anyMatch(s -> s.getId().equals(slotId));
        if (!valid) {
            throw ApiException.param("这个时间不在列表里");
        }
        vote.setStatus("LOCKED");
        vote.setLockedSlotId(slotId);
        vote.setUpdatedAt(LocalDateTime.now());
        voteRepository.save(vote);
        return toDetail(vote, userId);
    }

    /** 「我的事」tab:我发起 + 我参与 */
    @Transactional(readOnly = true)
    public List<VoteSummary> mine(Long userId) {
        List<VoteEntity> votes = voteRepository.findMine(userId);
        List<VoteSummary> items = new ArrayList<>();
        for (VoteEntity vote : votes) {
            VoteSummary item = new VoteSummary();
            item.setVoteId(vote.getId());
            item.setTitle(vote.getTitle());
            item.setType(vote.getType());
            item.setStatus(vote.getStatus());
            item.setVoterCount(countVoters(vote.getId()));
            item.setMyRole(vote.getOwnerId().equals(userId) ? "OWNER"
                    : ballotRepository.findByVoteIdAndUserId(vote.getId(), userId).isEmpty() ? "NONE" : "VOTER");
            item.setUpdatedAt(vote.getUpdatedAt());
            items.add(item);
        }
        return items;
    }

    /* ---------------- 汇总 ---------------- */

    private VoteDetail toDetail(VoteEntity vote, Long userId) {
        VoteDetail detail = new VoteDetail();
        detail.setVoteId(vote.getId());
        detail.setTitle(vote.getTitle());
        detail.setType(vote.getType());
        detail.setStatus(vote.getStatus());
        detail.setOwner(toBrief(userRepository.findById(vote.getOwnerId()).orElse(null)));
        detail.setDeadline(vote.getDeadline());
        detail.setLockedSlotId(vote.getLockedSlotId());

        List<VoteSlotEntity> slots = slotRepository.findByVoteIdOrderBySortOrderAsc(vote.getId());
        List<BallotEntity> ballots = ballotRepository.findByVoteId(vote.getId());

        // 按时段聚人头
        Map<Long, List<UserBrief>> bySlot = new HashMap<>();
        Set<Long> voterIds = new HashSet<>();
        for (BallotEntity ballot : ballots) {
            voterIds.add(ballot.getUserId());
            bySlot.computeIfAbsent(ballot.getSlotId(), k -> new ArrayList<>())
                    .add(toBrief(userRepository.findById(ballot.getUserId()).orElse(null)));
        }

        int maxCount = 0;
        Map<Long, Integer> countBySlot = new HashMap<>();
        for (VoteSlotEntity slot : slots) {
            int count = bySlot.getOrDefault(slot.getId(), List.of()).size();
            countBySlot.put(slot.getId(), count);
            maxCount = Math.max(maxCount, count);
        }

        List<SlotDetail> slotDetails = new ArrayList<>();
        for (VoteSlotEntity slot : slots) {
            SlotDetail sd = new SlotDetail();
            sd.setSlotId(slot.getId());
            sd.setLabel(slot.getLabel());
            int count = countBySlot.get(slot.getId());
            sd.setCount(count);
            sd.setIsBest(maxCount > 0 && count == maxCount);
            sd.setVoters(bySlot.getOrDefault(slot.getId(), List.of()));
            slotDetails.add(sd);
        }
        detail.setSlots(slotDetails);

        List<Long> mySlotIds = new ArrayList<>();
        for (BallotEntity ballot : ballots) {
            if (ballot.getUserId().equals(userId)) {
                mySlotIds.add(ballot.getSlotId());
            }
        }
        detail.setMySlotIds(mySlotIds);
        return detail;
    }

    private int countVoters(Long voteId) {
        Set<Long> ids = new HashSet<>();
        ballotRepository.findByVoteId(voteId).forEach(b -> ids.add(b.getUserId()));
        return ids.size();
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

    private VoteEntity requireVote(Long voteId) {
        return voteRepository.findById(voteId)
                .orElseThrow(() -> ApiException.notFound("这个投票不存在,可能已被删除"));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
