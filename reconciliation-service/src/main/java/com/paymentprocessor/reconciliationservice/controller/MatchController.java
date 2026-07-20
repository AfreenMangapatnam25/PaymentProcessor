package com.paymentprocessor.reconciliationservice.controller;

import com.paymentprocessor.reconciliationservice.dto.MatchResponse;
import com.paymentprocessor.reconciliationservice.service.MatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read access to individual matches. */
@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable Long id) {
        return MatchResponse.from(matchService.getMatch(id));
    }
}
