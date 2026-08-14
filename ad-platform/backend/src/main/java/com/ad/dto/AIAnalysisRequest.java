package com.ad.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIAnalysisRequest {
    private List<TrendPoint> trend;

    @Data
    public static class TrendPoint {
        private String statDate;
        private double ctr;
        private double cvr;
        private double cpa;
    }
}
