package com.ad.service.impl;

import com.ad.common.PageResult;
import com.ad.dto.RuleCreateDTO;
import com.ad.dto.RuleDTO;
import com.ad.dto.SandboxTestResult;
import com.ad.entity.Rule;
import com.ad.entity.RuleExecutionLog;
import com.ad.enums.ScopeType;
import com.ad.mapper.RuleExecutionLogMapper;
import com.ad.mapper.RuleMapper;
import com.ad.mapper.StatsHourlyMapper;
import com.ad.service.RuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleMapper ruleMapper;
    private final RuleExecutionLogMapper ruleExecutionLogMapper;
    private final StatsHourlyMapper statsHourlyMapper;

    @Override
    public List<RuleDTO> listAll() {
        List<Rule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<Rule>()
                        .orderByDesc(Rule::getPriority)
        );
        return rules.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RuleCreateDTO dto) {
        Rule rule = new Rule();
        rule.setName(dto.getName());
        rule.setTriggerMetric(dto.getTriggerMetric());
        rule.setTriggerOperator(dto.getTriggerOperator());
        rule.setTriggerThreshold(dto.getTriggerThreshold());
        rule.setTriggerWindowHours(dto.getTriggerWindowHours());
        rule.setActionType(dto.getActionType());
        rule.setActionParams(dto.getActionParams());
        rule.setScopeType(dto.getScopeType());
        rule.setScopeValue(dto.getScopeValue());
        rule.setPriority(dto.getPriority());
        rule.setCooldownMinutes(dto.getCooldownMinutes());
        rule.setIsSystem(false);
        rule.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RuleCreateDTO dto) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("Rule not found: " + id);
        }
        if (Boolean.TRUE.equals(rule.getIsSystem())) {
            throw new RuntimeException("System rules cannot be modified");
        }

        rule.setName(dto.getName());
        rule.setTriggerMetric(dto.getTriggerMetric());
        rule.setTriggerOperator(dto.getTriggerOperator());
        rule.setTriggerThreshold(dto.getTriggerThreshold());
        rule.setTriggerWindowHours(dto.getTriggerWindowHours());
        rule.setActionType(dto.getActionType());
        rule.setActionParams(dto.getActionParams());
        rule.setScopeType(dto.getScopeType());
        rule.setScopeValue(dto.getScopeValue());
        rule.setPriority(dto.getPriority());
        rule.setCooldownMinutes(dto.getCooldownMinutes());
        rule.setStatus(dto.getStatus());
        ruleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("Rule not found: " + id);
        }
        if (Boolean.TRUE.equals(rule.getIsSystem())) {
            throw new RuntimeException("System rules cannot be disabled");
        }
        rule.setStatus(status);
        ruleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("Rule not found: " + id);
        }
        if (Boolean.TRUE.equals(rule.getIsSystem())) {
            throw new RuntimeException("System rules cannot be deleted");
        }
        ruleMapper.deleteById(id);
    }

    @Override
    public SandboxTestResult simulate(Long id, LocalDate startDate, LocalDate endDate) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("Rule not found: " + id);
        }

        String triggerMetric = rule.getTriggerMetric();
        String triggerOperator = rule.getTriggerOperator();
        BigDecimal triggerThreshold = new BigDecimal(rule.getTriggerThreshold());
        String scopeType = rule.getScopeType();
        String scopeValue = rule.getScopeValue();

        // Determine which campaigns/strategies to check
        List<Long> scopeIds = resolveScopeIds(scopeType, scopeValue);

        // For each scope ID, query stats and evaluate trigger
        List<SandboxTestResult.SandboxTrigger> triggers = new ArrayList<>();
        BigDecimal totalBudgetSaved = BigDecimal.ZERO;
        Set<Long> affectedCampaigns = new HashSet<>();

        for (Long scopeId : scopeIds) {
            Long strategyId = ScopeType.STRATEGY.getCode().equals(scopeType) ? scopeId : null;
            Long campaignId = ScopeType.CAMPAIGN.getCode().equals(scopeType) ? scopeId : null;

            if (ScopeType.CHANNEL.getCode().equals(scopeType)) {
                // Scope is channel — query all channels matching scopeValue
                evaluateChannelTrigger(rule, triggerMetric, triggerOperator, triggerThreshold,
                        startDate, endDate, triggers, totalBudgetSaved, affectedCampaigns);
                continue;
            }

            List<Map<String, Object>> statsList = statsHourlyMapper.sumByDateRange(
                    strategyId, campaignId, null, startDate, endDate);

            if (statsList.isEmpty()) continue;

            Map<String, Object> stats = statsList.get(0);
            BigDecimal metricValue = extractMetric(triggerMetric, stats);

            if (evaluateCondition(metricValue, triggerOperator, triggerThreshold)) {
                String actionDescription = buildActionDescription(rule, metricValue);

                // Determine which campaign(s) were affected
                if (campaignId != null) {
                    affectedCampaigns.add(campaignId);
                }

                // Estimate budget saved: the cost that exceeded threshold
                BigDecimal cost = toBigDecimal(stats.get("total_cost"));
                totalBudgetSaved = totalBudgetSaved.add(cost);

                triggers.add(createTrigger(null, campaignId != null ? campaignId : scopeId,
                        metricValue, actionDescription));
            }
        }

        SandboxTestResult result = new SandboxTestResult();
        result.setTriggerCount(triggers.size());
        result.setAffectedCampaignCount(affectedCampaigns.size());
        result.setEstimatedBudgetSaved(totalBudgetSaved);
        result.setTriggers(triggers);
        return result;
    }

    @Override
    public PageResult<RuleDTO> getLogs(Long ruleId, int page, int size) {
        if (ruleId != null) {
            Rule rule = ruleMapper.selectById(ruleId);
            if (rule == null) {
                throw new RuntimeException("Rule not found: " + ruleId);
            }
        }

        // Get execution logs
        IPage<RuleExecutionLog> pageResult = ruleExecutionLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<RuleExecutionLog>()
                        .eq(ruleId != null, RuleExecutionLog::getRuleId, ruleId)
                        .orderByDesc(RuleExecutionLog::getExecutedAt)
        );

        // Convert logs to DTOs
        // Since RuleDTO is used for rules not logs, we return a simplified view
        // The UI will consume log rows as a list
        List<RuleDTO> logDtos = pageResult.getRecords().stream().map(log -> {
            RuleDTO dto = new RuleDTO();
            dto.setId(log.getId());
            dto.setName(log.getActionTaken());
            dto.setTriggerThreshold(log.getTriggerValue());
            dto.setActionParams(log.getResult());
            return dto;
        }).collect(Collectors.toList());

        return PageResult.of(
                new Page<RuleDTO>()
                        .setRecords(logDtos)
                        .setTotal(pageResult.getTotal())
                        .setCurrent(pageResult.getCurrent())
                        .setSize(pageResult.getSize())
        );
    }

    private List<Long> resolveScopeIds(String scopeType, String scopeValue) {
        // For CAMPAIGN scope: scopeValue can be a comma-separated list of campaign IDs
        // For STRATEGY scope: a comma-separated list of strategy IDs
        // For CHANNEL scope: scopeValue is the channel code
        if (scopeValue == null || scopeValue.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(scopeValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private void evaluateChannelTrigger(Rule rule, String triggerMetric, String triggerOperator,
                                         BigDecimal triggerThreshold, LocalDate startDate, LocalDate endDate,
                                         List<SandboxTestResult.SandboxTrigger> triggers,
                                         BigDecimal totalBudgetSaved, Set<Long> affectedCampaigns) {
        // For channel scope, sum by date and evaluate
        List<Map<String, Object>> dailyData = statsHourlyMapper.dailyTrends(
                null, null, rule.getScopeValue(), startDate, endDate);

        Map<LocalDate, List<Map<String, Object>>> groupedByDate = new LinkedHashMap<>();
        for (Map<String, Object> row : dailyData) {
            Object dateObj = row.get("stat_date");
            if (dateObj == null) continue;
            LocalDate date = extractLocalDate(dateObj);
            groupedByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<LocalDate, List<Map<String, Object>>> entry : groupedByDate.entrySet()) {
            long totalCost = 0;
            long totalClicks = 0;
            long totalConversions = 0;
            long totalImpressions = 0;

            for (Map<String, Object> row : entry.getValue()) {
                totalCost += toLong(row.get("cost"));
                totalClicks += toLong(row.get("clicks"));
                totalConversions += toLong(row.get("conversions"));
                totalImpressions += toLong(row.get("impressions"));
            }

            BigDecimal metricValue = calculateMetricValue(triggerMetric, totalCost, totalClicks,
                    totalConversions, totalImpressions);

            if (evaluateCondition(metricValue, triggerOperator, triggerThreshold)) {
                String actionDescription = buildActionDescription(rule, metricValue);
                BigDecimal dayCost = BigDecimal.valueOf(totalCost);
                totalBudgetSaved.add(dayCost);

                triggers.add(createTrigger(entry.getKey(), null, metricValue, actionDescription));
            }
        }
    }

    private BigDecimal calculateMetricValue(String metric, long cost, long clicks,
                                             long conversions, long impressions) {
        switch (metric.toLowerCase()) {
            case "cpa":
                return conversions > 0
                        ? BigDecimal.valueOf(cost).divide(BigDecimal.valueOf(conversions), 2, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(cost);
            case "ctr":
                return impressions > 0
                        ? BigDecimal.valueOf(clicks * 100.0 / impressions).setScale(4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            case "cvr":
                return clicks > 0
                        ? BigDecimal.valueOf(conversions * 100.0 / clicks).setScale(4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            case "consume":
                return BigDecimal.valueOf(cost);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal extractMetric(String metric, Map<String, Object> stats) {
        switch (metric.toLowerCase()) {
            case "cpa": {
                BigDecimal cost = toBigDecimal(stats.get("total_cost"));
                BigDecimal conversions = toBigDecimal(stats.get("total_conversions"));
                return conversions.compareTo(BigDecimal.ZERO) > 0
                        ? cost.divide(conversions, 2, RoundingMode.HALF_UP)
                        : cost;
            }
            case "ctr": {
                BigDecimal clicks = toBigDecimal(stats.get("total_clicks"));
                BigDecimal impressions = toBigDecimal(stats.get("total_impressions"));
                return impressions.compareTo(BigDecimal.ZERO) > 0
                        ? clicks.multiply(BigDecimal.valueOf(100))
                        .divide(impressions, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            }
            case "cvr": {
                BigDecimal conversions = toBigDecimal(stats.get("total_conversions"));
                BigDecimal clicks = toBigDecimal(stats.get("total_clicks"));
                return clicks.compareTo(BigDecimal.ZERO) > 0
                        ? conversions.multiply(BigDecimal.valueOf(100))
                        .divide(clicks, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
            }
            case "consume":
                return toBigDecimal(stats.get("total_cost"));
            default:
                return BigDecimal.ZERO;
        }
    }

    private boolean evaluateCondition(BigDecimal metricValue, String operator, BigDecimal threshold) {
        int cmp = metricValue.compareTo(threshold);
        switch (operator.toUpperCase()) {
            case "GT":
                return cmp > 0;
            case "GE":
            case "GTE":
                return cmp >= 0;
            case "LT":
                return cmp < 0;
            case "LE":
            case "LTE":
                return cmp <= 0;
            case "EQ":
                return cmp == 0;
            case "NEQ":
                return cmp != 0;
            default:
                return false;
        }
    }

    private String buildActionDescription(Rule rule, BigDecimal metricValue) {
        return String.format("[%s] %s %s -> %s (threshold: %s, actual: %s)",
                rule.getScopeType(),
                rule.getActionType(),
                rule.getScopeValue(),
                rule.getActionParams() != null ? rule.getActionParams() : "N/A",
                rule.getTriggerThreshold(),
                metricValue.toPlainString()
        );
    }

    private SandboxTestResult.SandboxTrigger createTrigger(LocalDate date, Long campaignId,
                                                            BigDecimal value, String action) {
        SandboxTestResult.SandboxTrigger trigger = new SandboxTestResult.SandboxTrigger();
        trigger.setDate(date);
        trigger.setCampaignId(campaignId);
        trigger.setTriggerValue(value);
        trigger.setActionDescription(action);
        return trigger;
    }

    private LocalDate extractLocalDate(Object obj) {
        if (obj instanceof LocalDate) return (LocalDate) obj;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        return LocalDate.parse(obj.toString());
    }

    private RuleDTO toDTO(Rule rule) {
        RuleDTO dto = new RuleDTO();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setTriggerMetric(rule.getTriggerMetric());
        dto.setTriggerOperator(rule.getTriggerOperator());
        dto.setTriggerThreshold(rule.getTriggerThreshold());
        dto.setTriggerWindowHours(rule.getTriggerWindowHours());
        dto.setActionType(rule.getActionType());
        dto.setActionParams(rule.getActionParams());
        dto.setScopeType(rule.getScopeType());
        dto.setScopeValue(rule.getScopeValue());
        dto.setPriority(rule.getPriority());
        dto.setCooldownMinutes(rule.getCooldownMinutes());
        dto.setIsSystem(rule.getIsSystem());
        dto.setStatus(rule.getStatus());
        return dto;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
