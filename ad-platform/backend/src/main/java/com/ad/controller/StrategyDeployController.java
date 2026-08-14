package com.ad.controller;

import com.ad.common.Result;
import com.ad.service.StrategyDeployService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyDeployController {

    private final StrategyDeployService deployService;

    @PostMapping("/{id}/deploy")
    public Result<Void> deploy(@PathVariable Long id) {
        deployService.deployToRtb(id);
        return Result.ok();
    }

    @PostMapping("/{id}/undeploy")
    public Result<Void> undeploy(@PathVariable Long id) {
        deployService.undeployFromRtb(id);
        return Result.ok();
    }

    @GetMapping("/{id}/deploy-status")
    public Result<Boolean> getDeployStatus(@PathVariable Long id) {
        return Result.ok(deployService.isDeployed(id));
    }
}
