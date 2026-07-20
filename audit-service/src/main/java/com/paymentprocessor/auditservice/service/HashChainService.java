package com.paymentprocessor.auditservice.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.paymentprocessor.auditservice.crypto.CanonicalJson;
import com.paymentprocessor.auditservice.crypto.Sha256;
import com.paymentprocessor.auditservice.domain.Actor;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.domain.ResourceRef;

/**
 * Computes the tamper-evident hash of an audit record.
 *
 * <p>{@code hash = sha256( canonical(content) )} where {@code content} includes the
 * record's business fields <em>and</em> its {@code prevHash}. Chaining {@code prevHash}
 * into every hash means altering, deleting or reordering any record breaks every hash
 * that follows it.
 */
@Service
public class HashChainService {

    /**
     * Computes the hash for a record whose {@code seq} and {@code prevHash} have already
     * been assigned. Does not mutate the record.
     */
    public String computeHash(AuditRecord record) {
        return Sha256.hashPrefixed(CanonicalJson.canonicalize(toCanonicalContent(record)));
    }

    /**
     * The exact field set covered by the hash. Everything immutable is included; only
     * {@code hash} itself and the post-hoc {@code batchId} are excluded.
     */
    private Map<String, Object> toCanonicalContent(AuditRecord r) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("id", r.getId());
        content.put("seq", r.getSeq());
        content.put("ts", r.getTs());
        content.put("recordedAt", r.getRecordedAt());
        content.put("event_id", r.getEventId());
        content.put("actor", actorMap(r.getActor()));
        content.put("action", r.getAction());
        content.put("resource", resourceMap(r.getResource()));
        content.put("merchant_id", r.getMerchantId());
        content.put("before", r.getBefore());
        content.put("after", r.getAfter());
        content.put("request_id", r.getRequestId());
        content.put("trace_id", r.getTraceId());
        content.put("prev_hash", r.getPrevHash());
        return content;
    }

    private Map<String, Object> actorMap(Actor a) {
        if (a == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", a.getType());
        m.put("id", a.getId());
        m.put("ip", a.getIp());
        m.put("ua", a.getUa());
        return m;
    }

    private Map<String, Object> resourceMap(ResourceRef r) {
        if (r == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", r.getType());
        m.put("id", r.getId());
        return m;
    }
}
