package com.scorevo.controller;

import com.scorevo.model.Activity;
import com.scorevo.model.Score;
import com.scorevo.payload.request.ScoreRequest;
import com.scorevo.payload.response.ScoreDTO;
import com.scorevo.security.model.SecurityUser;
import com.scorevo.service.ActivityService;
import com.scorevo.service.ScoreService;

import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/activities/{activityId}/scores")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private ActivityService activityService;

    /**
     * Get all scores for an activity
     */
    @GetMapping
    public ResponseEntity<List<ScoreDTO>> getActivityScores(@PathVariable("activityId") Long activityId) {
        Long userId = getCurrentUserId();
        
        try {
            List<Score> scores = scoreService.getActivityScores(activityId, userId);
            List<ScoreDTO> scoreDTOs = scores.stream()
                    .map(ScoreDTO::fromScore)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(scoreDTOs);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Get scores for a specific user in an activity
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ScoreDTO>> getUserActivityScores(
            @PathVariable("activityId") Long activityId,
            @PathVariable("userId") Long targetUserId) {
        
        Long currentUserId = getCurrentUserId();
        
        try {
            List<Score> scores = scoreService.getUserActivityScores(activityId, targetUserId, currentUserId);
            List<ScoreDTO> scoreDTOs = scores.stream()
                    .map(ScoreDTO::fromScore)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(scoreDTOs);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Get the current total score for each user in an activity
     */
    @GetMapping("/totals")
    public ResponseEntity<Map<Long, Integer>> getCurrentScores(@PathVariable("activityId") Long activityId) {
        Long userId = getCurrentUserId();
        
        try {
            Map<Long, Integer> scores = scoreService.getCurrentScores(activityId, userId);
            return ResponseEntity.ok(scores);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Add a new score
     */
    @PostMapping
    public ResponseEntity<ScoreDTO> addScore(
            @PathVariable("activityId") Long activityId,
            @Valid @RequestBody ScoreRequest scoreRequest) {
        
        Long userId = getCurrentUserId();
        
        try {
            // Get the activity to determine the mode
            Activity activity = activityService.getActivityById(activityId);
            
            Score savedScore;
            
            // Route to the appropriate service method based on activity mode
            if (activity.getMode() == Activity.ActivityMode.FREE_INCREMENT) {
                savedScore = scoreService.addFreeIncrementScore(activityId, scoreRequest, userId);
            } else if (activity.getMode() == Activity.ActivityMode.PENALTY_BALANCE) {
                savedScore = scoreService.addPenaltyBalanceScore(activityId, scoreRequest, userId);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            ScoreDTO scoreDTO = ScoreDTO.fromScore(savedScore);
            return ResponseEntity.status(HttpStatus.CREATED).body(scoreDTO);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Delete a score
     */
    @DeleteMapping("/{scoreId}")
    public ResponseEntity<Void> deleteScore(
            @PathVariable("activityId") Long activityId,
            @PathVariable("scoreId") Long scoreId) {
        
        Long userId = getCurrentUserId();
        
        try {
            scoreService.deleteScore(scoreId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Helper method to get the current authenticated user's ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}