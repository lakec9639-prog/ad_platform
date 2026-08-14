package com.ad.service.impl;

import com.ad.dto.AIAnalysisRequest;
import com.ad.service.AIAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class AIAnalysisServiceImpl implements AIAnalysisService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(AIAnalysisServiceImpl.class);

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.model:claude-sonnet-4-20250514}")
    private String model;

    @Value("${claude.base-url:https://api.anthropic.com}")
    private String baseUrl;

    public AIAnalysisServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String analyze(AIAnalysisRequest request) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_API_KEY_HERE")) {
            return "";
        }
        if (request.getTrend() == null || request.getTrend().isEmpty()) {
            return "";
        }

        String userPrompt = buildUserPrompt(request);
        String systemPrompt = buildSystemPrompt();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 50);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        try {
            String json = objectMapper.writeValueAsString(body);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            log.info("Calling Claude API at: {} with model: {}", baseUrl + "/v1/messages", model);
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
                if (content != null && !content.isEmpty()) {
                    String text = (String) content.get(0).get("text");
                    return text != null ? text.trim() : "";
                }
            } else {
                log.warn("Claude API returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            log.debug("Detail: ", e);
        }
        return "";
    }

    private String buildSystemPrompt() {
        return """
                你是一个程序化广告投放策略分析师。根据素材的CTR(点击率)、CVR(转化率)、CPA(获客成本)趋势，推荐最应该调整预算的策略。

                现有6个策略及特点：
                S1=高价值人群精准转化，ROI导向，适合CTR和CVR都好的素材
                S2=新品破圈拉新，拉新导向，适合CTR好但CVR低时拓展新用户
                S3=竞品截流抢夺，截流导向，适合CTR高但CVR低的素材
                S4=弃单重定向强转化，召回导向，适合CPA飙升时加强老客召回
                S5=智能通投探索，探索导向，适合稳定期补充预算消耗
                S6=兜底保量，填充导向，适合其他策略消耗完后自动补量

                分析规则：
                - CTR上升且CVR稳定 → 素材吸引力强，加大S1/S3预算
                - CTR高但CVR下降 → 点击多转化少，加大S2/S3拉新截流
                - CTR下降 → 素材疲劳，减少当前渠道，加大S2拓新或S5探索
                - CVR上升CPA下降 → 转化效率好，加大S1/S4最大化ROI
                - CPA飙升 → 成本失控，加大S4重定向降本，减S5/S6探索
                - 所有指标平稳 → 保持S5维持消耗

                输出规则：
                1. 必须≤15个汉字
                2. 格式如：CTR太高,加大S3力度
                3. 可组合如：CTR降CVR升,加S1减S5
                4. 只输出结果，不加解释""";
    }

    private String buildUserPrompt(AIAnalysisRequest request) {
        List<AIAnalysisRequest.TrendPoint> trend = request.getTrend();
        int size = Math.min(trend.size(), 14);
        List<AIAnalysisRequest.TrendPoint> recent = trend.subList(trend.size() - size, trend.size());

        StringBuilder sb = new StringBuilder();
        sb.append("该素材最近").append(size).append("天CTR/CVR/CPA数据：\n\n");
        sb.append("日期\tCTR(%)\tCVR(%)\tCPA(¥)\n");
        for (var point : recent) {
            sb.append(point.getStatDate()).append("\t")
                    .append(String.format("%.2f", point.getCtr())).append("\t")
                    .append(String.format("%.2f", point.getCvr())).append("\t")
                    .append(String.format("%.2f", point.getCpa())).append("\n");
        }
        sb.append("\n请输出投放建议：");
        return sb.toString();
    }
}
