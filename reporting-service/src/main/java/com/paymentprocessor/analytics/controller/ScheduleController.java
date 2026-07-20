package com.paymentprocessor.analytics.controller;

import com.paymentprocessor.analytics.dto.CreateScheduleRequest;
import com.paymentprocessor.analytics.dto.ScheduleResponse;
import com.paymentprocessor.analytics.dto.UpdateScheduleRequest;
import com.paymentprocessor.analytics.service.schedule.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules", description = "Recurring (daily/weekly/monthly) report definitions")
public class ScheduleController {

    private final ScheduleService schedules;

    public ScheduleController(ScheduleService schedules) {
        this.schedules = schedules;
    }

    @PostMapping
    @Operation(summary = "Create a recurring report schedule")
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody CreateScheduleRequest req,
                                                    UriComponentsBuilder uri) {
        var s = schedules.create(req);
        URI location = uri.path("/api/v1/schedules/{id}").buildAndExpand(s.getId()).toUri();
        return ResponseEntity.created(location).body(ScheduleResponse.from(s));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a schedule")
    public ScheduleResponse get(@PathVariable("id") UUID id) {
        return ScheduleResponse.from(schedules.get(id));
    }

    @GetMapping
    @Operation(summary = "List a merchant's schedules")
    public Page<ScheduleResponse> list(@RequestParam("merchantId") String merchantId,
                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
        return schedules.listForMerchant(merchantId, PageRequest.of(page, Math.min(size, 100)))
                .map(ScheduleResponse::from);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a schedule (partial)")
    public ScheduleResponse update(@PathVariable("id") UUID id,
                                   @Valid @RequestBody UpdateScheduleRequest req) {
        return ScheduleResponse.from(schedules.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        schedules.delete(id);
        return ResponseEntity.noContent().build();
    }
}
