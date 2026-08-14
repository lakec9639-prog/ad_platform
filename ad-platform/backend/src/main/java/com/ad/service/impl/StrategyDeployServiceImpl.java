package com.ad.service.impl;

import com.ad.entity.Strategy;
import com.ad.mapper.StrategyMapper;
import com.ad.service.StrategyDeployService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyDeployServiceImpl implements StrategyDeployService {

    private final StrategyMapper strategyMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String RTB_STRATEGY_KEY = "rtb:strategy:";

    @Override
    @Transactional
    public void deployToRtb(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("Strategy not found: " + strategyId);
        }

        if (strategy.getTargetCpa() == null) {
            throw new RuntimeException("target_cpa required for RTB deployment");
        }

        Map<String, String> config = new HashMap<>();
        config.put("id", String.valueOf(strategy.getId()));
        config.put("name", strategy.getName());
        config.put("targetCpa", strategy.getTargetCpa().toString());
        config.put("bidRate", strategy.getBidRate() != null ? strategy.getBidRate().toString() : "0.3");
        config.put("frequencyCap", strategy.getFrequencyCap() != null ? String.valueOf(strategy.getFrequencyCap()) : "10");
        config.put("timeRange", strategy.getTimeRange() != null ? strategy.getTimeRange() : "00:00-23:59");

        redisTemplate.opsForHash().putAll(RTB_STRATEGY_KEY + strategyId, config);

        redisTemplate.convertAndSend("config:changed",
                "strategy:deploy:" + strategyId);

        log.info("Strategy {} deployed to RTB pipeline", strategyId);
    }

    @Override
    @Transactional
    public void undeployFromRtb(Long strategyId) {
        redisTemplate.delete(RTB_STRATEGY_KEY + strategyId);
        redisTemplate.convertAndSend("config:changed",
                "strategy:undeploy:" + strategyId);
        log.info("Strategy {} removed from RTB pipeline", strategyId);
    }

    @Override
    public boolean isDeployed(Long strategyId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RTB_STRATEGY_KEY + strategyId));
    }
}
