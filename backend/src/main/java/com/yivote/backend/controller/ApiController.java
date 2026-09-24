package com.yivote.backend.controller;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.AuthDtos.LoginRequest;
import com.yivote.backend.dto.AuthDtos.LoginResponse;
import com.yivote.backend.dto.CollectDtos.CollectCreateRequest;
import com.yivote.backend.dto.CollectDtos.CollectDetail;
import com.yivote.backend.dto.CollectDtos.EntrySubmitRequest;
import com.yivote.backend.dto.CollectDtos.ExportResponse;
import com.yivote.backend.dto.CollectDtos.GroupCalendar;
import com.yivote.backend.dto.CollectDtos.GroupProfile;
import com.yivote.backend.dto.CollectDtos.RenameRequest;
import com.yivote.backend.dto.VoteDtos.BallotSubmitRequest;
import com.yivote.backend.dto.VoteDtos.LockRequest;
import com.yivote.backend.dto.VoteDtos.MineResponse;
import com.yivote.backend.dto.VoteDtos.VoteCreateRequest;
import com.yivote.backend.dto.VoteDtos.VoteDetail;
import com.yivote.backend.service.AuthService;
import com.yivote.backend.service.CollectService;
import com.yivote.backend.service.GroupService;
import com.yivote.backend.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 接口层:出入参严格对齐 api/openapi.yaml(SSOT) */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthService authService;
    private final VoteService voteService;
    private final CollectService collectService;
    private final GroupService groupService;

    public ApiController(AuthService authService, VoteService voteService,
                         CollectService collectService, GroupService groupService) {
        this.authService = authService;
        this.voteService = voteService;
        this.collectService = collectService;
        this.groupService = groupService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/votes")
    public VoteDetail createVote(@RequestBody VoteCreateRequest request) {
        return voteService.create(request);
    }

    @GetMapping("/votes/{voteId}")
    public VoteDetail getVote(@PathVariable Long voteId, @RequestParam(required = false) String openGId) {
        return voteService.detail(voteId, openGId, CurrentUser.get());
    }

    @PutMapping("/votes/{voteId}/ballots")
    public VoteDetail submitBallot(@PathVariable Long voteId, @RequestBody BallotSubmitRequest request) {
        return voteService.submitBallot(voteId, request);
    }

    @PostMapping("/votes/{voteId}/lock")
    public VoteDetail lockVote(@PathVariable Long voteId, @RequestBody LockRequest request) {
        return voteService.lock(voteId, request);
    }

    @GetMapping("/votes/mine")
    public MineResponse myVotes() {
        MineResponse response = new MineResponse();
        response.setItems(voteService.mine(CurrentUser.get()));
        return response;
    }

    /* ---------------- 收集填表(v1.1) ---------------- */

    @PostMapping("/collects")
    public CollectDetail createCollect(@RequestBody CollectCreateRequest request) {
        return collectService.create(request);
    }

    @GetMapping("/collects/{collectId}")
    public CollectDetail getCollect(@PathVariable Long collectId,
                                     @RequestParam(required = false) String openGId) {
        return collectService.detail(collectId, openGId, CurrentUser.get());
    }

    @PutMapping("/collects/{collectId}/entries")
    public CollectDetail submitEntry(@PathVariable Long collectId, @RequestBody EntrySubmitRequest request) {
        return collectService.submitEntry(collectId, request);
    }

    @GetMapping("/collects/{collectId}/export")
    public ExportResponse exportCollect(@PathVariable Long collectId) {
        return collectService.export(collectId);
    }

    /* ---------------- 群板 / 群日历(v1.3) ---------------- */

    @GetMapping("/groups/{openGId}")
    public GroupProfile getGroupProfile(@PathVariable String openGId) {
        return groupService.profile(openGId);
    }

    @PutMapping("/groups/{openGId}/name")
    public GroupProfile renameGroup(@PathVariable String openGId, @RequestBody RenameRequest request) {
        return groupService.rename(openGId, request);
    }

    @GetMapping("/groups/{openGId}/calendar")
    public GroupCalendar groupCalendar(@PathVariable String openGId, @RequestParam String month) {
        return groupService.calendar(openGId, month);
    }
}
