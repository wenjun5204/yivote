package com.yivote.backend.repository;

import com.yivote.backend.entity.TodoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<TodoEntity, Long> {

    List<TodoEntity> findByOpenGIdOrderByUpdatedAtDesc(String openGId);

    /** 群维度下挂在某投票上的待办(链路小点用) */
    Optional<TodoEntity> findFirstByOpenGIdAndSourceTypeAndSourceId(String openGId, String sourceType, Long sourceId);
}
