package com.yivote.backend.repository;

import com.yivote.backend.entity.VoteSlotEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteSlotRepository extends JpaRepository<VoteSlotEntity, Long> {
    List<VoteSlotEntity> findByVoteIdOrderBySortOrderAsc(Long voteId);
}
