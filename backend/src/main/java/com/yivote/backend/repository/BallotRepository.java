package com.yivote.backend.repository;

import com.yivote.backend.entity.BallotEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BallotRepository extends JpaRepository<BallotEntity, Long> {

    List<BallotEntity> findByVoteId(Long voteId);

    List<BallotEntity> findByVoteIdAndUserId(Long voteId, Long userId);

    /** 改投前清旧票:JPQL 直接执行(派生 delete 不立刻 flush,会撞唯一键——改投测试踩过) */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from BallotEntity b where b.voteId = :voteId and b.userId = :userId")
    void deleteByVoteIdAndUserId(@org.springframework.data.repository.query.Param("voteId") Long voteId,
                                 @org.springframework.data.repository.query.Param("userId") Long userId);

    List<BallotEntity> findBySlotIdIn(Collection<Long> slotIds);
}
