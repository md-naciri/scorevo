package com.scorevo.service.impl;

import com.scorevo.model.Activity;
import com.scorevo.model.Score;
import com.scorevo.model.User;
import com.scorevo.payload.request.ScoreRequest;
import com.scorevo.repository.ActivityRepository;
import com.scorevo.repository.ScoreRepository;
import com.scorevo.repository.UserRepository;
import com.scorevo.service.ActivityService;
import com.scorevo.service.EmailService;
import com.scorevo.service.ScoreService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final EmailService emailService;

    @Autowired
    public ScoreServiceImpl(
            ScoreRepository scoreRepository,
            ActivityRepository activityRepository,
            UserRepository userRepository,
            ActivityService activityService,
            EmailService emailService) {
        this.scoreRepository = scoreRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.emailService = emailService;
    }

    @Override
    public List<Score> getActivityScores(Long activityId, Long currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if the current user is a participant
        if (!activityService.isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to view scores for this activity");
        }

        return scoreRepository.findByActivityId(activityId);
    }

    @Override
    public List<Score> getUserActivityScores(Long activityId, Long userId, Long currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if the current user is a participant
        if (!activityService.isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to view scores for this activity");
        }

        return scoreRepository.findByActivityIdAndUserId(activityId, userId);
    }

    @Override
    public Map<Long, Integer> getCurrentScores(Long activityId, Long currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if the current user is a participant
        if (!activityService.isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to view scores for this activity");
        }

        List<Score> scores = scoreRepository.findByActivityId(activityId);

        // Group scores by user and sum the points
        return scores.stream()
                .collect(Collectors.groupingBy(
                        score -> score.getUser().getId(),
                        Collectors.summingInt(Score::getPoints)
                ));
    }

    @Override
    @Transactional
    public Score addFreeIncrementScore(Long activityId, ScoreRequest scoreRequest, Long currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if the activity is in FREE_INCREMENT mode
        if (activity.getMode() != Activity.ActivityMode.FREE_INCREMENT) {
            throw new IllegalStateException("This activity is not in FREE_INCREMENT mode");
        }

        // Check if the current user is a participant
        if (!activityService.isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to add scores to this activity");
        }

        // Get the user who scored
        User user = userRepository.findById(scoreRequest.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + scoreRequest.getUserId()));

        // Check if the user is a participant
        if (!activityService.isParticipant(activity, user.getId())) {
            throw new IllegalStateException("The user must be a participant in this activity");
        }

        // Create and save the score
        Score score = new Score();
        score.setActivity(activity);
        score.setUser(user);
        score.setPoints(scoreRequest.getPoints());
        score.setTimestamp(LocalDateTime.now());

        Score savedScore = scoreRepository.save(score);

        // Send score notification email
        try {
            emailService.sendScoreNotification(activityId, user.getId(), scoreRequest.getPoints());
        } catch (Exception e) {
            // Log the error but don't fail the operation if email sending fails
            System.err.println("Failed to send score notification email: " + e.getMessage());
        }

        return savedScore;
    }

//    @Override
//    @Transactional
//    public Score addPenaltyBalanceScore(Long activityId, ScoreRequest scoreRequest, Long currentUserId) {
//        Activity activity = activityRepository.findById(activityId)
//                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));
//
//        // Check if the activity is in PENALTY_BALANCE mode
//        if (activity.getMode() != Activity.ActivityMode.PENALTY_BALANCE) {
//            throw new IllegalStateException("This activity is not in PENALTY_BALANCE mode");
//        }
//
//        // Check if the current user is a participant
//        if (!activityService.isParticipant(activity, currentUserId)) {
//            throw new IllegalStateException("You must be a participant to add scores to this activity");
//        }
//
//        // Get the user who made the mistake (the one getting penalty points)
//        User userWithMistake = userRepository.findById(scoreRequest.getUserId())
//                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + scoreRequest.getUserId()));
//
//        // Check if the user is a participant
//        if (!activityService.isParticipant(activity, userWithMistake.getId())) {
//            throw new IllegalStateException("The user must be a participant in this activity");
//        }
//
//        // Get current scores for all participants
//        Map<Long, Integer> currentScores = getCurrentScores(activityId, currentUserId);
//
//        // Get the current score of the user who made the mistake (default to 0 if not found)
////        int userCurrentScore = currentScores.getOrDefault(userWithMistake.getId(), 0);
//
//        // Create and save the mistake score
//        Score score = new Score();
//        score.setActivity(activity);
//        score.setUser(userWithMistake);
//        score.setPoints(scoreRequest.getPoints());
//        score.setTimestamp(LocalDateTime.now());
//
//        Score savedScore = scoreRepository.save(score);
//
//        // Send score notification to the user who made the mistake
//        try {
//            emailService.sendScoreNotification(activityId, userWithMistake.getId(), scoreRequest.getPoints());
//        } catch (Exception e) {
//            // Log the error but don't fail the operation if email sending fails
//            System.err.println("Failed to send score notification email: " + e.getMessage());
//        }
//
//        // Now adjust the other participants' scores if necessary
//        // In Penalty Balance mode, we need to handle the balancing of points
//
//        // IMPORTANT: Create a new list to avoid ConcurrentModificationException
//        List<User> otherParticipants = new ArrayList<>();
//        for (User participant : activity.getParticipants()) {
//            if (!participant.getId().equals(userWithMistake.getId())) {
//                otherParticipants.add(participant);
//            }
//        }
//
//        // For each other participant, reduce their score (if they have any)
//        for (User otherUser : otherParticipants) {
//            // Get the current score of the other user
//            int otherUserCurrentScore = currentScores.getOrDefault(otherUser.getId(), 0);
//
//            // Calculate reduction (can't reduce below 0)
//            int reduction = Math.min(otherUserCurrentScore, scoreRequest.getPoints());
//
//            if (reduction > 0) {
//                // Create a negative score entry to reduce points
//                Score reductionScore = new Score();
//                reductionScore.setActivity(activity);
//                reductionScore.setUser(otherUser);
//                reductionScore.setPoints(-reduction);
//                reductionScore.setTimestamp(LocalDateTime.now());
//
//                scoreRepository.save(reductionScore);
//
//                // Send notification about the score reduction
//                try {
//                    emailService.sendScoreNotification(activityId, otherUser.getId(), -reduction);
//                } catch (Exception e) {
//                    // Log the error but don't fail the operation if email sending fails
//                    System.err.println("Failed to send score reduction notification email: " + e.getMessage());
//                }
//            }
//        }
//
//        return savedScore;
//    }

    @Override
    @Transactional
    public Score addPenaltyBalanceScore(Long activityId, ScoreRequest scoreRequest, Long currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if the activity is in PENALTY_BALANCE mode
        if (activity.getMode() != Activity.ActivityMode.PENALTY_BALANCE) {
            throw new IllegalStateException("This activity is not in PENALTY_BALANCE mode");
        }

        // Check if the current user is a participant (the one reporting the mistake)
        if (!activityService.isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to add scores to this activity");
        }

        // Get the user who made the mistake (the one getting penalty points)
        User userWithMistake = userRepository.findById(scoreRequest.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + scoreRequest.getUserId()));

        // Check if the userWithMistake is a participant
        if (!activityService.isParticipant(activity, userWithMistake.getId())) {
            throw new IllegalStateException("The user making the mistake must be a participant in this activity");
        }

        int pointsFromRequest = scoreRequest.getPoints();
        if (pointsFromRequest <= 0) {
            // Assuming penalties should always be positive values.
            throw new IllegalArgumentException("Penalty points must be positive.");
        }

        // Calculate current total scores for all participants in this activity
        List<Score> allScoresForActivity = scoreRepository.findByActivityId(activityId); // IMPORTANT: This must fetch ALL scores for the activity
        Map<Long, Integer> currentScores = new HashMap<>();
        for (Score s : allScoresForActivity) {
            if (s.getUser() != null) { // Defensive check
                currentScores.merge(s.getUser().getId(), s.getPoints(), Integer::sum);
            }
        }

        // This variable will hold the points that eventually get assigned to userWithMistake.
        // It starts as the full penalty and may be reduced if it offsets others' scores.
        int netPointsForMistakeMaker = pointsFromRequest;

        // IMPORTANT: Create a new list to avoid ConcurrentModificationException if activity.getParticipants() is a live collection
        List<User> otherParticipants = new ArrayList<>();
        for (User participant : activity.getParticipants()) {
            if (!participant.getId().equals(userWithMistake.getId())) {
                otherParticipants.add(participant);
            }
        }

        // For each other participant, try to offset their existing penalty scores
        for (User otherUser : otherParticipants) {
            // If all penalty points from the current mistake have been "used up" by offsetting, no need to continue
            if (netPointsForMistakeMaker <= 0) {
                break;
            }

            int otherUserCurrentScore = currentScores.getOrDefault(otherUser.getId(), 0);

            // Only offset if the other participant has an existing positive (penalty) score
            if (otherUserCurrentScore > 0) {
                // Determine how much of the other user's score can be reduced by the current mistake's points
                int reductionAmount = Math.min(otherUserCurrentScore, netPointsForMistakeMaker);

                if (reductionAmount > 0) {
                    // Create a negative score entry to reduce otherUser's points
                    Score reductionScore = new Score();
                    reductionScore.setActivity(activity);
                    reductionScore.setUser(otherUser);
                    reductionScore.setPoints(-reductionAmount); // Negative points to offset their penalty
                    reductionScore.setTimestamp(LocalDateTime.now());
                    scoreRepository.save(reductionScore);

                    // Decrease the points remaining from the current mistake
                    netPointsForMistakeMaker -= reductionAmount;

                    // Send notification about the score reduction
                    try {
                        emailService.sendScoreNotification(activityId, otherUser.getId(), -reductionAmount);
                    } catch (Exception e) {
                        System.err.println("Failed to send score reduction notification email to user "
                                + otherUser.getId() + ": " + e.getMessage());
                    }
                }
            }
        }

        // create and save the score for the user who made the mistake,
        // with the net points (which might be 0 if fully offset, or the original pointsFromRequest or something in between).
        Score mistakeMakerFinalScore = new Score();
        mistakeMakerFinalScore.setActivity(activity);
        mistakeMakerFinalScore.setUser(userWithMistake);
        mistakeMakerFinalScore.setPoints(netPointsForMistakeMaker); // This is the actual points added to the mistake maker
        mistakeMakerFinalScore.setTimestamp(LocalDateTime.now());

        Score savedScore = scoreRepository.save(mistakeMakerFinalScore);

        // Send score notification to the user who made the mistake,
        // reflecting the actual points added after any offsets.
        try {
            emailService.sendScoreNotification(activityId, userWithMistake.getId(), netPointsForMistakeMaker);
        } catch (Exception e) {
            System.err.println("Failed to send score notification email to user "
                    + userWithMistake.getId() + ": " + e.getMessage());
        }

        return savedScore; // Return the score object created for the user who made the mistake
    }

    @Override
    @Transactional
    public void deleteScore(Long scoreId, Long currentUserId) {
        Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new EntityNotFoundException("Score not found with id: " + scoreId));

        Activity activity = score.getActivity();

        // Check if the current user is a participant
        if (!activityService.isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to delete scores in this activity");
        }

        scoreRepository.deleteById(scoreId);
    }
}