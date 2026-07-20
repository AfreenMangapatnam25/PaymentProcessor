package com.paymentprocessor.authorization.api;

import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import com.paymentprocessor.authorization.dto.payment.AuthorizationRequest;
import com.paymentprocessor.authorization.dto.payment.AuthorizationResponse;
import com.paymentprocessor.authorization.dto.payment.CaptureRequest;
import com.paymentprocessor.authorization.dto.payment.ReauthorizationRequest;
import com.paymentprocessor.authorization.dto.payment.ReversalRequest;
import com.paymentprocessor.authorization.service.payment.CaptureService;
import com.paymentprocessor.authorization.service.payment.PaymentAuthorizationService;
import com.paymentprocessor.authorization.service.payment.ReauthorizationService;
import com.paymentprocessor.authorization.service.payment.ReversalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Payment authorization API: authorize, inquiry, capture, reversal (void) and re-authorization.
 */
@Tag(name = "Payment Authorization",
        description = "Authorize, inquire, capture, reverse and re-authorize card payments")
@RestController
@RequestMapping("/api/v1/authorizations")
@RequiredArgsConstructor
public class PaymentAuthorizationController {

    private final PaymentAuthorizationService authorizationService;
    private final CaptureService captureService;
    private final ReversalService reversalService;
    private final ReauthorizationService reauthorizationService;

    @Operation(summary = "Authorize a payment",
            description = "Obtains an approve/decline verdict from the gateway and places a hold.")
    @PostMapping
    public ResponseEntity<AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthorizationRecord record = authorizationService.authorize(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthorizationResponse.from(record));
    }

    @Operation(summary = "Get an authorization by id (inquiry)")
    @GetMapping("/{id}")
    public AuthorizationResponse get(@PathVariable UUID id) {
        return AuthorizationResponse.from(authorizationService.getById(id));
    }

    @Operation(summary = "List authorizations for a payment reference")
    @GetMapping
    public List<AuthorizationResponse> getByPaymentReference(@RequestParam String paymentReference) {
        return authorizationService.getByPaymentReference(paymentReference).stream()
                .map(AuthorizationResponse::from)
                .toList();
    }

    @Operation(summary = "Capture an authorized hold (full or partial)")
    @PostMapping("/{id}/capture")
    public AuthorizationResponse capture(@PathVariable UUID id,
                                         @Valid @RequestBody(required = false) CaptureRequest request) {
        return AuthorizationResponse.from(captureService.capture(id, request));
    }

    @Operation(summary = "Reverse (void) an authorization hold before capture")
    @PostMapping("/{id}/reversal")
    public AuthorizationResponse reverse(@PathVariable UUID id,
                                         @RequestBody(required = false) ReversalRequest request) {
        return AuthorizationResponse.from(reversalService.reverse(id, request));
    }

    @Operation(summary = "Re-authorize an expired or insufficient authorization")
    @PostMapping("/{id}/reauthorize")
    public ResponseEntity<AuthorizationResponse> reauthorize(
            @PathVariable UUID id,
            @Valid @RequestBody ReauthorizationRequest request) {
        AuthorizationRecord record = reauthorizationService.reauthorize(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthorizationResponse.from(record));
    }

    @Operation(summary = "Synchronize an authorization's state from the gateway",
            description = "Useful after a 3-D Secure challenge completes.")
    @PostMapping("/{id}/synchronize")
    public AuthorizationResponse synchronize(@PathVariable UUID id) {
        return AuthorizationResponse.from(authorizationService.synchronize(id));
    }
}
