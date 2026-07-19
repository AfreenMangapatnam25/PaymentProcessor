package com.paymentprocessor.fraudservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentprocessor.fraudservice.dto.FraudDecisionResponse;
import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskScoringEngine;

/**
 * Real-time fraud evaluation endpoint invoked by the Payment Service after it
 * validates a payment request and before authorization.
 *
 * <pre>
 *   POST /api/fraud/evaluate
 *   {
 *     "intentId": "pi_123",
 *     "merchantId": "mrc_9",
 *     "amount": 5400.00,
 *     "currency": "USD",
 *     "cardBin": "411111",
 *     "ipCountry": "NG",
 *     "billingCountry": "US",
 *     "deviceNew": true
 *   }
 * </pre>
 */
@RestController
@RequestMapping("/api/fraud")
public class FraudEvaluationController {

    private final RiskScoringEngine engine;

    public FraudEvaluationController(RiskScoringEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<FraudDecisionResponse> evaluate(@RequestBody FraudEvaluationRequest request) {
        return ResponseEntity.ok(engine.evaluate(request));
    }
}
