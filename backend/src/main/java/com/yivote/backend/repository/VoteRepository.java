package com.yivote.backend.repository;

import com.yivote.backend.entity.VoteEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<VoteEntity, Long> {

    /** 我发起或我参与的投票(「我的事」tab),按更新时间倒序 */
    @Query("""
            select distinct v from VoteEntity v
            where v.ownerId = :userId
               or exists (
                    select 1 from BallotEntity b
                    where b.voteId = v.id and b.userId = :userId
               )
            order by v.updatedAt desc
            """)
    List<VoteEntity> findMine(@Param("userId") Long userId);
}
