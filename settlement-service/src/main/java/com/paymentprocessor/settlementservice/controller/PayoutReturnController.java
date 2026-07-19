package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.entity.PayoutReturn;
import com.paymentprocessor.settlementservice.service.PayoutReturnService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read access to recorded bank returns. */
@RestController
@RequestMapping("/api/payout-returns")
public class PayoutReturnController {

    private final PayoutReturnService service;

    public PayoutReturnController(PayoutReturnService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayoutReturn> list(@RequestParam(required = false) String payoutId) {
        return (payoutId != null) ? service.findByPayout(payoutId) : service.findAll();
    }
}
