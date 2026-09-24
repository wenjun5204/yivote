package com.yivote.backend.controller;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.AuthDtos.LoginRequest;
import com.yivote.backend.dto.AuthDtos.LoginResponse;
import com.yivote.backend.dto.VoteDtos.BallotSubmitRequest;
import com.yivote.backend.dto.VoteDtos.LockRequest;
import com.yivote.backend.dto.VoteDtos.MineResponse;
import com.yivote.backend.dto.VoteDtos.VoteCreateRequest;
import com.yivote.backend.dto.VoteDtos.VoteDetail;
import com.yivote.backend.service.AuthService;
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

    public ApiController(AuthService authService, VoteService voteService) {
        this.authService = authService;
        this.voteService = voteService;
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
        return voteService.detail(voteId, CurrentUser.get());
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
}
