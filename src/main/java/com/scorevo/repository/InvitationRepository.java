package com.scorevo.repository;

import com.scorevo.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    Optional<Invitation> findByEmailAndActivityIdAndIsAccepted(String email, Long activityId, Boolean isAccepted);
    List<Invitation> findByEmailAndIsAccepted(String email, Boolean isAccepted);
    List<Invitation> findByActivityId(Long activityId);
    List<Invitation> findByExpiresAtBeforeAndIsAccepted(LocalDateTime now, Boolean isAccepted);

    @Modifying
    @Transactional
    @Query("DELETE FROM Invitation i WHERE i.activity.id = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
}