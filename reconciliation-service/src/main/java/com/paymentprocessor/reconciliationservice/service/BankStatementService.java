package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.domain.BankStatement;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.RecordSource;
import com.paymentprocessor.reconciliationservice.domain.StatementStatus;
import com.paymentprocessor.reconciliationservice.exception.InvalidOperationException;
import com.paymentprocessor.reconciliationservice.exception.ResourceNotFoundException;
import com.paymentprocessor.reconciliationservice.repository.BankStatementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registers ingested external statements and normalizes their line items into EXTERNAL
 * {@link ReconRecord}s attached to a reconciliation run.
 */
@Slf4j
@Service
public class BankStatementService {

    private final BankStatementRepository bankStatementRepository;
    private final CsvRecordParser csvRecordParser;
    private final ReconRecordService reconRecordService;

    public BankStatementService(BankStatementRepository bankStatementRepository,
                                CsvRecordParser csvRecordParser,
                                ReconRecordService reconRecordService) {
        this.bankStatementRepository = bankStatementRepository;
        this.csvRecordParser = csvRecordParser;
        this.reconRecordService = reconRecordService;
    }

    /**
     * Ingest a CSV statement: parse the rows into external records, attach them to the run, and
     * persist the statement metadata. The whole operation is atomic.
     */
    @Transactional
    public BankStatement ingestCsv(Long runId, BankStatement statement, String csvContent) {
        if (bankStatementRepository.existsByStatementReference(statement.getStatementReference())) {
            throw new InvalidOperationException(
                    "Statement already ingested: " + statement.getStatementReference());
        }
        List<ReconRecord> records = csvRecordParser.parse(
                csvContent, RecordSource.EXTERNAL, statement.getBankName());
        reconRecordService.addRecords(runId, records);

        statement.setRecordCount(records.size());
        statement.setStatus(StatementStatus.NORMALIZED);
        BankStatement saved = bankStatementRepository.save(statement);
        log.info("Ingested statement {} ({} records) into run {}",
                saved.getStatementReference(), saved.getRecordCount(), runId);
        return saved;
    }

    @Transactional(readOnly = true)
    public BankStatement getById(Long id) {
        return bankStatementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank statement not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<BankStatement> list(Pageable pageable) {
        return bankStatementRepository.findAll(pageable);
    }
}
