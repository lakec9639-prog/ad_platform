package com.ad.controller;

import com.ad.common.Result;
import com.ad.dto.AIAnalysisRequest;
import com.ad.service.AIAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIAdviceController {

    private final AIAnalysisService aiAnalysisService;

    @PostMapping("/advice")
    public Result<String> getAdvice(@RequestBody AIAnalysisRequest request) {
        String advice = aiAnalysisService.analyze(request);
        return Result.ok(advice);
    }
}
