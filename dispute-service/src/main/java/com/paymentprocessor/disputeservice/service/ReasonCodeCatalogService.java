package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.domain.enums.EvidenceCategory;
import com.paymentprocessor.disputeservice.domain.enums.Network;
import com.paymentprocessor.disputeservice.entity.ReasonCodeCatalog;
import com.paymentprocessor.disputeservice.entity.ReasonCodeCatalogId;
import com.paymentprocessor.disputeservice.repository.ReasonCodeCatalogRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reference catalogue of network dispute reason codes. Seeds a representative
 * set on startup and answers guidance queries: reason lookups, the evidence a
 * reason requires, and whether a gathered evidence package is complete.
 */
@Service
public class ReasonCodeCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ReasonCodeCatalogService.class);

    private final ReasonCodeCatalogRepository repository;

    public ReasonCodeCatalogService(ReasonCodeCatalogRepository repository) {
        this.repository = repository;
    }

    /** Seeds the catalogue with default network reason codes if it is empty. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfEmpty() {
        try {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(defaultCatalog());
            log.info("Seeded reason code catalogue with default network reason codes");
        } catch (Exception ex) {
            // A missing datastore at startup should not crash the service.
            log.warn("Could not seed reason code catalogue: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<ReasonCodeCatalog> lookup(Network network, String code) {
        return repository.findById(new ReasonCodeCatalogId(network.name(), code));
    }

    @Transactional(readOnly = true)
    public List<ReasonCodeCatalog> byNetwork(Network network) {
        return repository.findByNetwork(network.name());
    }

    @Transactional(readOnly = true)
    public List<ReasonCodeCatalog> findAll() {
        return repository.findAll();
    }

    /**
     * The evidence categories required to fight the given reason code. Returns an
     * empty set when the reason code is unknown.
     */
    @Transactional(readOnly = true)
    public Set<EvidenceCategory> requiredCategories(Network network, String code) {
        return lookup(network, code)
                .map(rc -> parseCategories(rc.getRequiredEvidence()))
                .orElse(EnumSet.noneOf(EvidenceCategory.class));
    }

    /**
     * The subset of required categories that are missing from {@code present}.
     */
    public Set<EvidenceCategory> missingCategories(Network network, String code,
                                                   Set<EvidenceCategory> present) {
        Set<EvidenceCategory> required = requiredCategories(network, code);
        required.removeAll(present);
        return required;
    }

    // ----- seed data ----------------------------------------------------------

    private static Set<EvidenceCategory> parseCategories(String csv) {
        Set<EvidenceCategory> result = EnumSet.noneOf(EvidenceCategory.class);
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String token : csv.split(",")) {
            String name = token.trim();
            if (!name.isEmpty()) {
                result.add(EvidenceCategory.valueOf(name));
            }
        }
        return result;
    }

    private static List<ReasonCodeCatalog> defaultCatalog() {
        List<ReasonCodeCatalog> list = new ArrayList<>();

        // Visa
        list.add(new ReasonCodeCatalog("VISA", "10.4",
                "Fraud", "Other Fraud — Card-Absent Environment", "MEDIUM",
                "THREE_DS_PROOF,AVS_CVV_RESULT,DEVICE_FINGERPRINT",
                "COMMUNICATION,DELIVERY_CONFIRMATION", 10));
        list.add(new ReasonCodeCatalog("VISA", "12.6",
                "Processing Error", "Duplicate Processing", "HIGH",
                "AUTHORIZATION_RECORD,RECEIPT",
                "COMMUNICATION", 10));
        list.add(new ReasonCodeCatalog("VISA", "13.1",
                "Consumer Dispute", "Merchandise/Services Not Received", "LOW",
                "TRACKING,DELIVERY_CONFIRMATION,SIGNATURE_PROOF",
                "COMMUNICATION,REFUND_POLICY", 10));
        list.add(new ReasonCodeCatalog("VISA", "13.2",
                "Consumer Dispute", "Canceled Recurring", "MEDIUM",
                "CANCELLATION_PROOF,TERMS_OF_SERVICE,REFUND_POLICY",
                "COMMUNICATION", 10));
        list.add(new ReasonCodeCatalog("VISA", "13.3",
                "Consumer Dispute", "Not as Described or Defective", "LOW",
                "PRODUCT_DESCRIPTION,COMMUNICATION,TERMS_OF_SERVICE",
                "RECEIPT", 10));
        list.add(new ReasonCodeCatalog("VISA", "13.6",
                "Consumer Dispute", "Credit Not Processed", "MEDIUM",
                "REFUND_POLICY,REFUND_PROOF",
                "COMMUNICATION", 10));

        // Mastercard
        list.add(new ReasonCodeCatalog("MASTERCARD", "4837",
                "Fraud", "No Cardholder Authorization", "MEDIUM",
                "THREE_DS_PROOF,AUTHORIZATION_RECORD",
                "COMMUNICATION", 7));
        list.add(new ReasonCodeCatalog("MASTERCARD", "4841",
                "Consumer Dispute", "Canceled Recurring", "MEDIUM",
                "CANCELLATION_PROOF,TERMS_OF_SERVICE",
                "REFUND_POLICY", 7));
        list.add(new ReasonCodeCatalog("MASTERCARD", "4855",
                "Consumer Dispute", "Goods or Services Not Provided", "LOW",
                "DELIVERY_CONFIRMATION,TRACKING",
                "COMMUNICATION", 7));
        list.add(new ReasonCodeCatalog("MASTERCARD", "4860",
                "Consumer Dispute", "Credit Not Processed", "MEDIUM",
                "REFUND_PROOF,REFUND_POLICY",
                "COMMUNICATION", 7));

        // Amex
        list.add(new ReasonCodeCatalog("AMEX", "C08",
                "Consumer Dispute", "Goods/Services Not Received", "LOW",
                "TRACKING,DELIVERY_CONFIRMATION",
                "COMMUNICATION", 7));
        list.add(new ReasonCodeCatalog("AMEX", "C02",
                "Consumer Dispute", "Credit Not Processed", "MEDIUM",
                "REFUND_PROOF",
                "REFUND_POLICY", 7));
        list.add(new ReasonCodeCatalog("AMEX", "F29",
                "Fraud", "Card Not Present", "MEDIUM",
                "AVS_CVV_RESULT,THREE_DS_PROOF",
                "COMMUNICATION", 7));

        // Discover
        list.add(new ReasonCodeCatalog("DISCOVER", "4755",
                "Consumer Dispute", "Non-Receipt of Goods or Services", "LOW",
                "TRACKING,DELIVERY_CONFIRMATION",
                "COMMUNICATION", 10));

        return list;
    }
}
