package com.yivote.backend.repository;

import com.yivote.backend.entity.TodoClaimEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoClaimRepository extends JpaRepository<TodoClaimEntity, Long> {

    List<TodoClaimEntity> findByTodoIdOrderByCreatedAtAsc(Long todoId);

    List<TodoClaimEntity> findByItemIdOrderByCreatedAtAsc(Long itemId);

    /** 放回池子(转给别人):release 用;done 后前端不展示入口,但兜底也走这里删 */
    @Modifying
    @Query("delete from TodoClaimEntity c where c.itemId = :itemId and c.userId = :userId")
    void deleteByItemIdAndUserId(@Param("itemId") Long itemId, @Param("userId") Long userId);
}
