package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.entity.Journal;
import com.paymentprocessor.ledgerservice.service.JournalService;
import com.paymentprocessor.ledgerservice.service.PostingService;
import com.paymentprocessor.ledgerservice.service.ReversalService;
import com.paymentprocessor.ledgerservice.web.dto.JournalResponse;
import com.paymentprocessor.ledgerservice.web.dto.PostJournalRequest;
import com.paymentprocessor.ledgerservice.web.dto.ReverseJournalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journals")
@Tag(name = "Journals", description = "Post, reverse, and query immutable double-entry journals")
public class JournalController {

    private final PostingService postingService;
    private final ReversalService reversalService;
    private final JournalService journalService;

    public JournalController(PostingService postingService,
                             ReversalService reversalService,
                             JournalService journalService) {
        this.postingService = postingService;
        this.reversalService = reversalService;
        this.journalService = journalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a balanced double-entry journal (idempotent by idempotencyKey)")
    public JournalResponse post(@Valid @RequestBody PostJournalRequest request) {
        Journal journal = postingService.post(request);
        return journalService.toResponse(journal);
    }

    @GetMapping
    @Operation(summary = "List journals (paged), optionally filtered by externalRef")
    public Object list(@RequestParam(required = false) String externalRef,
                       @PageableDefault(size = 50) Pageable pageable) {
        if (externalRef != null && !externalRef.isBlank()) {
            return journalService.findByExternalRef(externalRef).stream()
                    .map(journalService::toResponse).toList();
        }
        Page<Journal> page = journalService.list(pageable);
        return page.map(journalService::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a journal and its entries")
    public JournalResponse get(@PathVariable String id) {
        return journalService.toResponse(journalService.get(id));
    }

    @GetMapping("/{id}/entries")
    @Operation(summary = "Get the entries of a journal")
    public List<?> entries(@PathVariable String id) {
        journalService.get(id); // 404 if missing
        return journalService.entriesOf(id).stream().map(JournalService::toEntryResponse).toList();
    }

    @PostMapping("/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reverse a posted journal by generating a compensating journal")
    public JournalResponse reverse(@PathVariable String id,
                                   @Valid @RequestBody ReverseJournalRequest request) {
        Journal reversal = reversalService.reverse(id, request);
        return journalService.toResponse(reversal);
    }
}
