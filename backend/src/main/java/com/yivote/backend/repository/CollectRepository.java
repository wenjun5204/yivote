package com.yivote.backend.repository;

import com.yivote.backend.entity.CollectEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectRepository extends JpaRepository<CollectEntity, Long> {
    List<CollectEntity> findByOpenGIdOrderByUpdatedAtDesc(String openGId);
}
