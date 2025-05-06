package com.scorevo.payload.response;

import com.scorevo.model.Score;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScoreDTO {
    private Long id;
    private Long activityId;
    private Long userId;
    private String username;
    private Integer points;
    private LocalDateTime timestamp;
    
    public static ScoreDTO fromScore(Score score) {
        ScoreDTO dto = new ScoreDTO();
        dto.setId(score.getId());
        dto.setActivityId(score.getActivity().getId());
        dto.setUserId(score.getUser().getId());
        dto.setUsername(score.getUser().getUsername());
        dto.setPoints(score.getPoints());
        dto.setTimestamp(score.getTimestamp());
        return dto;
    }
}