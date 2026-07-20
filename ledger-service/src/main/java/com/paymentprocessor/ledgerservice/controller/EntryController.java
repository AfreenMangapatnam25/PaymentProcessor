package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.service.EntryService;
import com.paymentprocessor.ledgerservice.service.JournalService;
import com.paymentprocessor.ledgerservice.web.dto.EntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/entries")
@Tag(name = "Entries", description = "Read-only access to immutable ledger entry lines")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping(params = "accountId")
    @Operation(summary = "List entries for an account (paged)")
    public Page<EntryResponse> byAccount(@RequestParam String accountId,
                                         @PageableDefault(size = 50) Pageable pageable) {
        return entryService.byAccount(accountId, pageable).map(JournalService::toEntryResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single entry by id")
    public EntryResponse get(@PathVariable Long id) {
        return JournalService.toEntryResponse(entryService.get(id));
    }
}
