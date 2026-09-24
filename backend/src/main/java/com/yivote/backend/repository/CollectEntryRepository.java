package com.yivote.backend.repository;

import com.yivote.backend.entity.CollectEntryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectEntryRepository extends JpaRepository<CollectEntryEntity, Long> {

    List<CollectEntryEntity> findByCollectIdOrderByUpdatedAtAsc(Long collectId);

    Optional<CollectEntryEntity> findByCollectIdAndUserId(Long collectId, Long userId);

    /** 改填前清旧行:JPQL 直接执行(派生 delete 不立刻 flush 会撞唯一键,与 ballot 同款踩坑) */
    @Modifying
    @Query("delete from CollectEntryEntity e where e.collectId = :collectId and e.userId = :userId")
    void deleteByCollectIdAndUserId(@Param("collectId") Long collectId, @Param("userId") Long userId);
}
