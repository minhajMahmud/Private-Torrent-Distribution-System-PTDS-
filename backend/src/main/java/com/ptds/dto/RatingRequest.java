package com.ptds.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Short score;
}
