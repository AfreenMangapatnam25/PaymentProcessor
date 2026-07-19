package com.paymentprocessor.disputeservice.controller;

import com.paymentprocessor.disputeservice.dto.request.ArbitrationDecisionRequest;
import com.paymentprocessor.disputeservice.dto.request.ArbitrationFilingRequest;
import com.paymentprocessor.disputeservice.dto.request.IssuerResponseRequest;
import com.paymentprocessor.disputeservice.dto.request.SubmitRepresentmentRequest;
import com.paymentprocessor.disputeservice.dto.response.DisputeResponse;
import com.paymentprocessor.disputeservice.dto.response.RepresentmentResponse;
import com.paymentprocessor.disputeservice.service.RepresentmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for representment, issuer response, and arbitration handling.
 */
@RestController
@RequestMapping("/api/v1/disputes/{disputeId}")
public class RepresentmentController {

    private final RepresentmentService representmentService;

    public RepresentmentController(RepresentmentService representmentService) {
        this.representmentService = representmentService;
    }

    /** Assembles accepted evidence and submits a representment to the network. */
    @PostMapping("/representments")
    @ResponseStatus(HttpStatus.CREATED)
    public RepresentmentResponse submit(@PathVariable String disputeId,
                                        @Valid @RequestBody SubmitRepresentmentRequest request) {
        return RepresentmentResponse.from(representmentService.submit(disputeId, request));
    }

    /** Lists all representments / filings for a dispute. */
    @GetMapping("/representments")
    public List<RepresentmentResponse> list(@PathVariable String disputeId) {
        return representmentService.forDispute(disputeId).stream()
                .map(RepresentmentResponse::from).toList();
    }

    /** Records the issuer's decision, driving the dispute to won or pre-arbitration. */
    @PostMapping("/issuer-response")
    public DisputeResponse issuerResponse(@PathVariable String disputeId,
                                          @Valid @RequestBody IssuerResponseRequest request) {
        return DisputeResponse.from(representmentService.recordIssuerResponse(disputeId, request));
    }

    /** Escalates a rejected dispute to network arbitration. */
    @PostMapping("/arbitration")
    @ResponseStatus(HttpStatus.CREATED)
    public RepresentmentResponse fileArbitration(@PathVariable String disputeId,
                                                 @Valid @RequestBody ArbitrationFilingRequest request) {
        return RepresentmentResponse.from(representmentService.fileArbitration(disputeId, request));
    }

    /** Records the network's binding arbitration decision. */
    @PostMapping("/arbitration-decision")
    public DisputeResponse arbitrationDecision(@PathVariable String disputeId,
                                               @Valid @RequestBody ArbitrationDecisionRequest request) {
        return DisputeResponse.from(representmentService.recordArbitrationDecision(disputeId, request));
    }
}
