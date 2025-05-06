package com.scorevo.service;

import com.scorevo.model.Score;
import com.scorevo.payload.request.ScoreRequest;

import java.util.List;
import java.util.Map;

public interface ScoreService {

    /**
     * Get all scores for an activity
     */
    List<Score> getActivityScores(Long activityId, Long currentUserId);

    /**
     * Get scores for a specific user in an activity
     */
    List<Score> getUserActivityScores(Long activityId, Long userId, Long currentUserId);

    /**
     * Get the current total score for each user in an activity
     */
    Map<Long, Integer> getCurrentScores(Long activityId, Long currentUserId);

    /**
     * Add a new score in FREE_INCREMENT mode
     */
    Score addFreeIncrementScore(Long activityId, ScoreRequest scoreRequest, Long currentUserId);

    /**
     * Add a new score in PENALTY_BALANCE mode
     */
    Score addPenaltyBalanceScore(Long activityId, ScoreRequest scoreRequest, Long currentUserId);

    /**
     * Delete a score
     */
    void deleteScore(Long scoreId, Long currentUserId);
}