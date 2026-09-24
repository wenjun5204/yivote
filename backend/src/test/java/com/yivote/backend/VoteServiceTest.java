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
import com.yivote.backend.dto.VoteDtos.BallotSubmitRequest;
import com.yivote.backend.dto.VoteDtos.LockRequest;
import com.yivote.backend.dto.VoteDtos.SlotCreate;
import com.yivote.backend.dto.VoteDtos.VoteCreateRequest;
import com.yivote.backend.dto.VoteDtos.VoteDetail;
import com.yivote.backend.service.AuthService;
import com.yivote.backend.service.VoteService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 投票核心链路:发起 → 投票/改投 → 定档 → 我的列表 */
@SpringBootTest
class VoteServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private VoteService voteService;

    private Long ownerId;
    private Long voterId;

    @BeforeEach
    void setUp() {
        LoginRequest req = new LoginRequest();
        req.setCode("test-owner-" + System.nanoTime());
        LoginResponse owner = authService.login(req, null);
        this.ownerId = owner.getUserId();

        LoginRequest req2 = new LoginRequest();
        req2.setCode("test-voter-" + System.nanoTime());
        LoginResponse voter = authService.login(req2, null);
        this.voterId = voter.getUserId();
    }

    private VoteDetail createVote(Long userId, String title, String... slotLabels) {
        CurrentUser.set(userId);
        VoteCreateRequest request = new VoteCreateRequest();
        request.setTitle(title);
        request.setType("TIME");
        request.setSlots(List.of(slotLabels).stream().map(label -> {
            SlotCreate slot = new SlotCreate();
            slot.setLabel(label);
            return slot;
        }).toList());
        return voteService.create(request);
    }

    @Test
    void createThenDetailHasBestSlot() {
        VoteDetail created = createVote(ownerId, "下周球局，哪天人最齐？", "周五晚上", "周六下午");
        assertNotNull(created.getVoteId());
        assertEquals(2, created.getSlots().size());
        assertEquals("OPEN", created.getStatus());

        CurrentUser.set(voterId);
        BallotSubmitRequest ballot = new BallotSubmitRequest();
        ballot.setSlotIds(List.of(created.getSlots().get(1).getSlotId()));
        VoteDetail after = voteService.submitBallot(created.getVoteId(), ballot);

        assertEquals(1, after.getSlots().get(1).getCount());
        assertTrue(after.getSlots().get(1).getIsBest());
        assertFalse(after.getSlots().get(0).getIsBest());
        assertEquals(1, after.getSlots().get(1).getVoters().size());
    }

    @Test
    void submitOverwritesPreviousBallot() {
        VoteDetail created = createVote(ownerId, "聚餐定时间", "周六上午", "周六下午", "周六晚上");
        Long slotA = created.getSlots().get(0).getSlotId();
        Long slotB = created.getSlots().get(1).getSlotId();

        CurrentUser.set(voterId);
        BallotSubmitRequest first = new BallotSubmitRequest();
        first.setSlotIds(List.of(slotA));
        voteService.submitBallot(created.getVoteId(), first);

        // 改投:档期变了,整票覆盖
        BallotSubmitRequest second = new BallotSubmitRequest();
        second.setSlotIds(List.of(slotA, slotB));
        VoteDetail after = voteService.submitBallot(created.getVoteId(), second);

        assertEquals(1, after.getSlots().get(0).getCount());
        assertEquals(1, after.getSlots().get(1).getCount());
        assertEquals(2, after.getMySlotIds().size(), "改投后我的选票应为新选择");
        assertEquals(1, after.getSlots().get(0).getVoters().size(), "时段聚人头不重复计票");
    }

    @Test
    void lockByOwnerProducesRoster() {
        VoteDetail created = createVote(ownerId, "定档测试", "周五晚上", "周六下午");
        Long slotB = created.getSlots().get(1).getSlotId();

        CurrentUser.set(voterId);
        BallotSubmitRequest ballot = new BallotSubmitRequest();
        ballot.setSlotIds(List.of(slotB));
        voteService.submitBallot(created.getVoteId(), ballot);

        CurrentUser.set(ownerId);
        LockRequest lock = new LockRequest();
        lock.setSlotId(slotB);
        VoteDetail locked = voteService.lock(created.getVoteId(), lock);

        assertEquals("LOCKED", locked.getStatus());
        assertEquals(slotB, locked.getLockedSlotId());
        assertEquals(1, locked.getSlots().get(1).getVoters().size(), "定档时段投票者=到场名单");
    }

    @Test
    void lockByNonOwnerRejected() {
        VoteDetail created = createVote(ownerId, "只有发起人能定", "周六下午");
        CurrentUser.set(voterId);
        LockRequest lock = new LockRequest();
        lock.setSlotId(created.getSlots().get(0).getSlotId());
        ApiException ex = assertThrows(ApiException.class,
                () -> voteService.lock(created.getVoteId(), lock));
        assertEquals("40300", ex.getCode());
    }

    @Test
    void mineListCoversOwnerAndVoter() {
        VoteDetail created = createVote(ownerId, "我的列表", "周六下午");

        CurrentUser.set(voterId);
        BallotSubmitRequest ballot = new BallotSubmitRequest();
        ballot.setSlotIds(List.of(created.getSlots().get(0).getSlotId()));
        voteService.submitBallot(created.getVoteId(), ballot);

        CurrentUser.set(ownerId);
        assertEquals("OWNER", voteService.mine(ownerId).get(0).getMyRole());
        CurrentUser.set(voterId);
        assertEquals("VOTER", voteService.mine(voterId).get(0).getMyRole());
    }

    @Test
    void createRejectsEmptySlotsAndOverlongTitle() {
        CurrentUser.set(ownerId);
        VoteCreateRequest badSlots = new VoteCreateRequest();
        badSlots.setTitle("没选项");
        badSlots.setType("TIME");
        badSlots.setSlots(List.of());
        assertThrows(ApiException.class, () -> voteService.create(badSlots));

        String longTitle = "标题超过三十个字".repeat(5);
        assertThrows(ApiException.class, () -> createVote(ownerId, longTitle, "周六下午"));
    }

    @Test
    void choiceVoteSingleSelectionOnly() {
        CurrentUser.set(ownerId);
        VoteCreateRequest request = new VoteCreateRequest();
        request.setTitle("吃什么");
        request.setType("CHOICE");
        request.setSlots(List.of("火锅", "烤肉", "日料").stream().map(label -> {
            SlotCreate slot = new SlotCreate();
            slot.setLabel(label);
            return slot;
        }).toList());
        VoteDetail created = voteService.create(request);

        CurrentUser.set(voterId);
        BallotSubmitRequest multi = new BallotSubmitRequest();
        multi.setSlotIds(created.getSlots().stream().map(s -> s.getSlotId()).limit(2).toList());
        ApiException ex = assertThrows(ApiException.class,
                () -> voteService.submitBallot(created.getVoteId(), multi));
        assertEquals("40000", ex.getCode());
    }
}
