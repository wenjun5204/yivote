package com.yivote.backend.repository;

import com.yivote.backend.entity.TodoItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoItemRepository extends JpaRepository<TodoItemEntity, Long> {
    List<TodoItemEntity> findByTodoIdOrderBySortOrderAsc(Long todoId);
}
