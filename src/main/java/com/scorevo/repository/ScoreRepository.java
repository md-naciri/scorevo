package com.scorevo.repository;

import com.scorevo.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByActivityId(Long activityId);
    List<Score> findByActivityIdAndUserId(Long activityId, Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Score s WHERE s.activity.id = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
}