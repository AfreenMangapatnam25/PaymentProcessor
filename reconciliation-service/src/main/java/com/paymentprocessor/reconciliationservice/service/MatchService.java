package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.domain.MatchRecord;
import com.paymentprocessor.reconciliationservice.exception.ResourceNotFoundException;
import com.paymentprocessor.reconciliationservice.repository.MatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Queries the matches produced by reconciliation runs. */
@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Transactional(readOnly = true)
    public Page<MatchRecord> listByRun(Long runId, Pageable pageable) {
        return matchRepository.findByReconRunId(runId, pageable);
    }

    @Transactional(readOnly = true)
    public MatchRecord getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }
}
