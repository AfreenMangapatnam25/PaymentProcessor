package com.paymentprocessor.limit.controller;

import com.paymentprocessor.limit.dto.CommitRequest;
import com.paymentprocessor.limit.dto.ReleaseRequest;
import com.paymentprocessor.limit.dto.ReservationResponse;
import com.paymentprocessor.limit.dto.ReserveRequest;
import com.paymentprocessor.limit.service.ReservationService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Reserve → commit → release lifecycle endpoints called by the Payment Service.
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reserve, commit and release limit capacity")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Evaluate and reserve capacity for a transaction",
            description = "Returns 201 with the reservation, or 422 if a hard limit is exceeded.")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.reserve(request));
    }

    @PostMapping("/{reservationId}/commit")
    @Operation(summary = "Commit (capture) a reservation, optionally partial")
    public ResponseEntity<ReservationResponse> commit(@PathVariable UUID reservationId,
                                                      @Valid @RequestBody CommitRequest request) {
        return ResponseEntity.ok(reservationService.commit(reservationId, request.capturedAmount()));
    }

    @PostMapping("/{reservationId}/release")
    @Operation(summary = "Release a reservation (failure, cancellation, manual override)")
    public ResponseEntity<ReservationResponse> release(@PathVariable UUID reservationId,
                                                       @Valid @RequestBody(required = false) ReleaseRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(reservationService.release(reservationId, reason));
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Fetch a reservation by id")
    public ResponseEntity<ReservationResponse> get(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.getByReservationId(reservationId));
    }

    @GetMapping
    @Operation(summary = "Fetch reservations for a transaction")
    public ResponseEntity<List<ReservationResponse>> byTransaction(@RequestParam String transactionId) {
        return ResponseEntity.ok(reservationService.getByTransaction(transactionId));
    }
}
