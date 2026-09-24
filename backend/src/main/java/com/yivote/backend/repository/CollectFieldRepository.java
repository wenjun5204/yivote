package com.yivote.backend.repository;

import com.yivote.backend.entity.CollectFieldEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectFieldRepository extends JpaRepository<CollectFieldEntity, Long> {
    List<CollectFieldEntity> findByCollectIdOrderBySortOrderAsc(Long collectId);
}
