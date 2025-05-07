package com.scorevo.service.impl;

import com.scorevo.model.Activity;
import com.scorevo.model.Invitation;
import com.scorevo.model.User;
import com.scorevo.payload.request.ActivityRequest;
import com.scorevo.payload.response.MessageResponse;
import com.scorevo.repository.ActivityRepository;
import com.scorevo.repository.UserRepository;
import com.scorevo.service.ActivityService;
import com.scorevo.service.EmailService;
import com.scorevo.service.InvitationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);


    @Autowired
    public ActivityServiceImpl(
            ActivityRepository activityRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    @Override
    public List<Activity> getUserActivities(Long userId) {
        return activityRepository.findByParticipantsId(userId);
    }

    @Override
    public Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));
    }

    @Override
    @Transactional
    public Activity createActivity(ActivityRequest activityRequest, Long creatorUserId) {
        // Find the creator user
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + creatorUserId));

        // Create new activity
        Activity activity = new Activity();
        activity.setName(activityRequest.getName());
        activity.setDescription(activityRequest.getDescription());
        activity.setMode(activityRequest.getMode());

        // Add creator as a participant
        Set<User> participants = new HashSet<>();
        participants.add(creator);
        activity.setParticipants(participants);

        // Save the activity
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public Activity updateActivity(Long activityId, ActivityRequest activityRequest, Long userId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if user is a participant
        if (!isParticipant(activity, userId)) {
            throw new IllegalStateException("User is not a participant in this activity");
        }

        // Update activity fields
        activity.setName(activityRequest.getName());
        activity.setDescription(activityRequest.getDescription());

        // We don't allow changing the mode once the activity is created
        // as it would invalidate existing scores

        // Save the updated activity
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId, Long userId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if user is a participant
        if (!isParticipant(activity, userId)) {
            throw new IllegalStateException("User is not a participant in this activity");
        }

        // Delete the activity
        activityRepository.deleteById(activityId);
    }

    @Override
    @Transactional
    public Activity addParticipant(Long activityId, Long userId, Long currentUserId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if current user is a participant
        if (!isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to add others to this activity");
        }

        // Find the user to add
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Add the user as a participant if not already
        if (!isParticipant(activity, userId)) {
            activity.getParticipants().add(user);
            return activityRepository.save(activity);
        } else {
            throw new IllegalStateException("User is already a participant in this activity");
        }
    }

//    @Override
//    @Transactional
//    public MessageResponse addParticipantByEmail(Long activityId, String email, Long currentUserId) {
//        // Find the activity
//        Activity activity = activityRepository.findById(activityId)
//                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));
//
//        // Check if current user is a participant
//        if (!isParticipant(activity, currentUserId)) {
//            throw new IllegalStateException("You must be a participant to add others to this activity");
//        }
//
//        // Check if user with email exists
//        Optional<User> userOptional = userRepository.findByEmail(email);
//
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//
//            // Add the user as a participant if not already
//            if (!isParticipant(activity, user.getId())) {
//                activity.getParticipants().add(user);
//                activityRepository.save(activity);
//
//                // Try to send email but don't break if it fails
//                try {
//                    emailService.sendActivityInvitation(activityId, email, currentUserId);
//                } catch (Exception e) {
//                    logger.error("Failed to send invitation email, but user was added", e);
//                }
//
//                return new MessageResponse("User has been added to the activity");
//            } else {
//                return new MessageResponse("User is already a participant in this activity");
//            }
//        } else {
//            // User doesn't exist, try to send invitation email
//            try {
//                boolean emailSent = emailService.sendActivityInvitation(activityId, email, currentUserId);
//                if (emailSent) {
//                    return new MessageResponse("Invitation has been sent to " + email);
//                } else {
//                    return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
//                }
//            } catch (Exception e) {
//                logger.error("Failed to send invitation email", e);
//                return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
//            }
//        }
//    }

    @Autowired
    private InvitationService invitationService;

    @Override
    @Transactional
    public MessageResponse addParticipantByEmail(Long activityId, String email, Long currentUserId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if current user is a participant
        if (!isParticipant(activity, currentUserId)) {
            throw new IllegalStateException("You must be a participant to add others to this activity");
        }

        // Check if user with email exists
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Add the user as a participant if not already
            if (!isParticipant(activity, user.getId())) {
                activity.getParticipants().add(user);
                activityRepository.save(activity);

                // Create invitation record for tracking
                invitationService.createInvitation(activityId, email, currentUserId);

                // Try to send email but don't break if it fails
                try {
                    emailService.sendActivityInvitation(activityId, email, currentUserId);
                } catch (Exception e) {
                    logger.error("Failed to send invitation email, but user was added", e);
                }

                return new MessageResponse("User has been added to the activity");
            } else {
                return new MessageResponse("User is already a participant in this activity");
            }
        } else {
            // User doesn't exist, create invitation and send email
            Invitation invitation = invitationService.createInvitation(activityId, email, currentUserId);

            try {
                boolean emailSent = emailService.sendActivityInvitation(activityId, email, currentUserId);
                if (emailSent) {
                    return new MessageResponse("Invitation has been sent to " + email);
                } else {
                    return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
                }
            } catch (Exception e) {
                logger.error("Failed to send invitation email", e);
                return new MessageResponse("Failed to send invitation to " + email + ". Please try again later.");
            }
        }
    }

    @Override
    @Transactional
    public Activity removeParticipant(Long activityId, Long userId, Long currentUserId) {
        // Find the activity
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + activityId));

        // Check if current user is a participant (self-removal is allowed)
        if (!isParticipant(activity, currentUserId) && !currentUserId.equals(userId)) {
            throw new IllegalStateException("You must be a participant to remove others from this activity");
        }

        // Find the user to remove
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Remove the user as a participant
        if (isParticipant(activity, userId)) {
            activity.getParticipants().remove(user);
            return activityRepository.save(activity);
        } else {
            throw new IllegalStateException("User is not a participant in this activity");
        }
    }

    @Override
    public boolean isParticipant(Activity activity, Long userId) {
        return activity.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));
    }


}