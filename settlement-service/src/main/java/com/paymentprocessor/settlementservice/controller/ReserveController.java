package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.service.ReserveService;
import com.paymentprocessor.settlementservice.web.dto.ReserveResponse;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Query reserves and trigger due-reserve releases. */
@RestController
@RequestMapping("/api/reserves")
public class ReserveController {

    private final ReserveService service;
    private final SettlementMapper mapper;

    public ReserveController(ReserveService service, SettlementMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ReserveResponse> list(@RequestParam(required = false) String merchantId) {
        return mapper.toReserveResponses(
                merchantId != null ? service.findByMerchant(merchantId) : service.findAll());
    }

    @GetMapping("/{id}")
    public ReserveResponse get(@PathVariable String id) {
        return mapper.toReserveResponse(service.findById(id));
    }

    /** Releases every reserve whose hold period has elapsed as of today. */
    @PostMapping("/release-due")
    public Map<String, Integer> releaseDue() {
        int released = service.releaseDueReserves(LocalDate.now());
        return Map.of("released", released);
    }
}
