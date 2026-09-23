package com.ptds.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingSummaryResponse {
    private double average;
    private long count;
    private Short myScore;
}
