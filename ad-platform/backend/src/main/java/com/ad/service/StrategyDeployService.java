package com.ad.service;

public interface StrategyDeployService {
    void deployToRtb(Long strategyId);
    void undeployFromRtb(Long strategyId);
    boolean isDeployed(Long strategyId);
}
