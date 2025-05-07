package com.scorevo.repository;

import com.scorevo.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    Optional<Invitation> findByEmailAndActivityIdAndIsAccepted(String email, Long activityId, Boolean isAccepted);
    List<Invitation> findByEmailAndIsAccepted(String email, Boolean isAccepted);
    List<Invitation> findByActivityId(Long activityId);
    List<Invitation> findByExpiresAtBeforeAndIsAccepted(LocalDateTime now, Boolean isAccepted);
}