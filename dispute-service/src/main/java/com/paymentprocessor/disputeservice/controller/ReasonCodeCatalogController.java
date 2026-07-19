package com.paymentprocessor.disputeservice.controller;

import com.paymentprocessor.disputeservice.domain.enums.Network;
import com.paymentprocessor.disputeservice.dto.response.ReasonCodeResponse;
import com.paymentprocessor.disputeservice.exception.DisputeNotFoundException;
import com.paymentprocessor.disputeservice.service.ReasonCodeCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST API exposing the network reason-code catalogue that guides
 * merchants on evidence requirements.
 */
@RestController
@RequestMapping("/api/v1/reason-codes")
public class ReasonCodeCatalogController {

    private final ReasonCodeCatalogService catalogService;

    public ReasonCodeCatalogController(ReasonCodeCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** All catalogued reason codes. */
    @GetMapping
    public List<ReasonCodeResponse> all() {
        return catalogService.findAll().stream().map(ReasonCodeResponse::from).toList();
    }

    /** Reason codes for a specific network. */
    @GetMapping("/{network}")
    public List<ReasonCodeResponse> byNetwork(@PathVariable Network network) {
        return catalogService.byNetwork(network).stream().map(ReasonCodeResponse::from).toList();
    }

    /** A single reason code for a network. */
    @GetMapping("/{network}/{code}")
    public ReasonCodeResponse get(@PathVariable Network network, @PathVariable String code) {
        return catalogService.lookup(network, code)
                .map(ReasonCodeResponse::from)
                .orElseThrow(() -> new DisputeNotFoundException(
                        "Reason code not found: " + network + " " + code));
    }
}
