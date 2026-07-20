package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.entity.SettlementItem;
import com.paymentprocessor.settlementservice.service.SettlementItemService;
import com.paymentprocessor.settlementservice.web.dto.IngestItemRequest;
import com.paymentprocessor.settlementservice.web.dto.ItemResponse;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingests and queries settlement line items. Ingestion is idempotent on the
 * item's idempotency key.
 */
@RestController
@RequestMapping("/api/settlement-items")
public class SettlementItemController {

    private final SettlementItemService service;
    private final SettlementMapper mapper;

    public SettlementItemController(SettlementItemService service, SettlementMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> ingest(@Valid @RequestBody IngestItemRequest request) {
        SettlementItem item = new SettlementItem();
        item.setMerchantId(request.merchantId());
        item.setType(request.type());
        item.setSourceType(request.sourceType());
        item.setSourceId(request.sourceId());
        item.setAmountMinor(request.amountMinor());
        item.setCurrency(request.currency());
        item.setEffectiveAt(request.effectiveAt());
        item.setIdempotencyKey(request.idempotencyKey());
        SettlementItem saved = service.ingest(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toItemResponse(saved));
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return mapper.toItemResponse(service.findById(id));
    }

    @GetMapping
    public List<ItemResponse> list(@RequestParam(required = false) String batchId) {
        List<SettlementItem> items = (batchId != null)
                ? service.findByBatch(batchId)
                : service.findAll();
        return mapper.toItemResponses(items);
    }
}
