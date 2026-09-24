package com.yivote.backend.repository;

import com.yivote.backend.entity.GroupProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupProfileRepository extends JpaRepository<GroupProfileEntity, Long> {
    Optional<GroupProfileEntity> findByOpenGId(String openGId);
}
