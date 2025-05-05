package com.scorevo.repository;

import com.scorevo.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByActivityId(Long activityId);
    List<Score> findByActivityIdAndUserId(Long activityId, Long userId);
}