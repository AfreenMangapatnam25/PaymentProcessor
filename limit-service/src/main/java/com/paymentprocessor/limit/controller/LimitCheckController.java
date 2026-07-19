package com.paymentprocessor.limit.controller;

import com.paymentprocessor.limit.dto.LimitCheckRequest;
import com.paymentprocessor.limit.dto.LimitCheckResponse;
import com.paymentprocessor.limit.dto.UsageDto;
import com.paymentprocessor.limit.service.LimitEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/limits")
@RequiredArgsConstructor
@Tag(name = "Limit Checks", description = "Read-only limit evaluation and usage queries")
public class LimitCheckController {

    private final LimitEvaluationService evaluationService;

    @PostMapping("/check")
    @Operation(summary = "Evaluate a transaction against applicable limits without reserving")
    public ResponseEntity<LimitCheckResponse> check(@Valid @RequestBody LimitCheckRequest request) {
        return ResponseEntity.ok(evaluationService.check(request));
    }

    @GetMapping("/usage")
    @Operation(summary = "Current consumption and remaining headroom for an entity")
    public ResponseEntity<List<UsageDto>> usage(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String merchantId,
            @RequestParam String currency) {
        return ResponseEntity.ok(evaluationService.usage(customerId, merchantId, currency));
    }
}
