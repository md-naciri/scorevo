package com.scorevo.payload.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Points value is required")
    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;
}