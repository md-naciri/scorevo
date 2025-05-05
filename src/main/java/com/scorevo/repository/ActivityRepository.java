package com.scorevo.repository;

import com.scorevo.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByParticipantsId(Long userId);
}