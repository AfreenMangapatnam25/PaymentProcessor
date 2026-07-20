package com.paymentprocessor.reconciliationservice.controller;

import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import com.paymentprocessor.reconciliationservice.dto.*;
import com.paymentprocessor.reconciliationservice.service.ExceptionRecordService;
import com.paymentprocessor.reconciliationservice.service.MatchService;
import com.paymentprocessor.reconciliationservice.service.ReconRecordService;
import com.paymentprocessor.reconciliationservice.service.ReconRunService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** REST API for reconciliation runs, their records, execution, matches and exceptions. */
@RestController
@RequestMapping("/api/v1/recon-runs")
public class ReconRunController {

    private final ReconRunService reconRunService;
    private final ReconRecordService reconRecordService;
    private final MatchService matchService;
    private final ExceptionRecordService exceptionRecordService;

    public ReconRunController(ReconRunService reconRunService,
                              ReconRecordService reconRecordService,
                              MatchService matchService,
                              ExceptionRecordService exceptionRecordService) {
        this.reconRunService = reconRunService;
        this.reconRecordService = reconRecordService;
        this.matchService = matchService;
        this.exceptionRecordService = exceptionRecordService;
    }

    @PostMapping
    public ResponseEntity<ReconRunResponse> create(@Valid @RequestBody ReconRunCreateRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        ReconRun run = reconRunService.createRun(ReconMapper.toRun(request));
        URI location = uriBuilder.path("/api/v1/recon-runs/{id}").buildAndExpand(run.getId()).toUri();
        return ResponseEntity.created(location).body(ReconRunResponse.from(run));
    }

    @GetMapping("/{id}")
    public ReconRunResponse get(@PathVariable Long id) {
        return ReconRunResponse.from(reconRunService.getRun(id));
    }

    @GetMapping("/uuid/{uuid}")
    public ReconRunResponse getByUuid(@PathVariable UUID uuid) {
        return ReconRunResponse.from(reconRunService.getRunByUuid(uuid));
    }

    @GetMapping
    public Page<ReconRunResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return reconRunService.listRuns(pageable).map(ReconRunResponse::from);
    }

    @PostMapping("/{id}/records")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ReconRecordResponse> addRecords(@PathVariable Long id,
                                                @Valid @RequestBody AddRecordsRequest request) {
        return reconRecordService.addRecords(id, ReconMapper.toRecords(request.records()))
                .stream().map(ReconRecordResponse::from).toList();
    }

    @GetMapping("/{id}/records")
    public Page<ReconRecordResponse> listRecords(@PathVariable Long id,
                                                 @PageableDefault(size = 50) Pageable pageable) {
        return reconRecordService.listRecords(id, pageable).map(ReconRecordResponse::from);
    }

    @PostMapping("/{id}/execute")
    public ReconRunResponse execute(@PathVariable Long id) {
        return ReconRunResponse.from(reconRunService.execute(id));
    }

    @GetMapping("/{id}/matches")
    public Page<MatchResponse> listMatches(@PathVariable Long id,
                                           @PageableDefault(size = 50) Pageable pageable) {
        return matchService.listByRun(id, pageable).map(MatchResponse::from);
    }

    @GetMapping("/{id}/exceptions")
    public Page<ExceptionResponse> listExceptions(@PathVariable Long id,
                                                  @PageableDefault(size = 50) Pageable pageable) {
        return exceptionRecordService.list(null, null, null, id, pageable).map(ExceptionResponse::from);
    }
}
