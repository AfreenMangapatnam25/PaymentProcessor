package com.paymentprocessor.auditservice.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.paymentprocessor.auditservice.config.AuditProperties;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.domain.ChainState;
import com.paymentprocessor.auditservice.repository.AuditRecordRepository;
import com.paymentprocessor.auditservice.repository.ChainStateStore;
import com.paymentprocessor.auditservice.service.exception.ResourceNotFoundException;

/**
 * Read-only access to the audit query copy. Enforces sane result-size limits so a query
 * can never attempt to page the entire multi-year collection into memory.
 */
@Service
public class AuditQueryService {

    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 50;

    private final AuditRecordRepository records;
    private final ChainStateStore chainState;
    private final String genesisHash;

    public AuditQueryService(AuditRecordRepository records, ChainStateStore chainState,
                             AuditProperties props) {
        this.records = records;
        this.chainState = chainState;
        this.genesisHash = props.getChain().getGenesisHash();
    }

    public AuditRecord getById(String id) {
        return records.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit record not found: " + id));
    }

    public List<AuditRecord> byMerchant(String merchantId, int limit) {
        return records.findByMerchantIdOrderByTsDesc(merchantId, page(limit));
    }

    public List<AuditRecord> byResource(String type, String id, int limit) {
        return records.findByResourceTypeAndResourceIdOrderByTsDesc(type, id, page(limit));
    }

    public List<AuditRecord> byAction(String action, int limit) {
        return records.findByActionOrderByTsDesc(action, page(limit));
    }

    /** Current head sequence of the chain (0 when empty). */
    public long headSeq() {
        ChainState state = chainState.getOrInitialise(genesisHash);
        return state.getSeq();
    }

    private Pageable page(int limit) {
        int effective = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return PageRequest.of(0, effective);
    }
}
