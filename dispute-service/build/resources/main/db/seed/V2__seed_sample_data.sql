-- Sample seed data for the `local` Spring profile only.
-- Loaded via spring.flyway.locations = classpath:db/migration,classpath:db/seed
-- (see application.yml, profile: local). Never applied to the default/prod profile.
--
-- Fixed, deterministic ids are used throughout so API_TESTING.md examples can be
-- exercised directly against this data.

-- ---------------------------------------------------------------------------
-- reason_code_catalog
-- ---------------------------------------------------------------------------
INSERT INTO reason_code_catalog
    (network, code, category, description, win_rate, required_evidence, optional_evidence, response_days)
VALUES
    ('VISA', '10.4', 'Fraud', 'Other Fraud - Card Absent Environment', 'Low',
     'AVS/CVV result, 3DS proof, device fingerprint', 'Prior undisputed transaction history', 20),
    ('VISA', '13.1', 'Consumer Dispute', 'Merchandise / Services Not Received', 'Medium',
     'Proof of delivery, tracking, communication with customer', 'Refund policy', 20),
    ('MASTERCARD', '4853', 'Cardholder Dispute', 'Cardholder Dispute - Defective/Not as Described', 'Medium',
     'Product description, communication, refund policy', 'Return/cancellation proof', 45),
    ('MASTERCARD', '4837', 'Fraud', 'No Cardholder Authorization', 'Low',
     'AVS/CVV result, 3DS proof, authorization record', 'Device fingerprint', 45),
    ('AMEX', 'C08', 'Merchandise/Services', 'Goods/Services Not Received', 'Medium',
     'Proof of delivery, tracking', 'Communication with customer', 20),
    ('DISCOVER', 'UA02', 'Fraud', 'Fraudulent Transaction - Card Not Present', 'Low',
     'AVS/CVV result, 3DS proof', 'Device fingerprint', 30);

-- ---------------------------------------------------------------------------
-- disputes
-- ---------------------------------------------------------------------------
INSERT INTO disputes
    (id, chargeback_id, transaction_id, payment_id, merchant_id, customer_id, network, type, source,
     stage, status, reason_code, reason_description, amount_minor, currency, chargeback_fee_minor,
     is_partial, liability_party, received_at, opened_at, deadline_at, resolved_at, closed_at,
     merchant_notified, created_at, updated_at, version)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'CB-SEED-0001', 'TXN-SEED-0001', 'PAY-SEED-0001',
     'MERCH-SEED-01', 'CUST-SEED-01', 'VISA', 'CHARGEBACK', 'CARD_NETWORK',
     'CHARGEBACK', 'OPEN', '10.4', 'Other Fraud - Card Absent Environment', 15999, 'USD', 1500,
     FALSE, 'PENDING', '2026-07-01 09:00:00', '2026-07-01 09:05:00', '2026-07-21 09:05:00', NULL, NULL,
     FALSE, '2026-07-01 09:05:00', '2026-07-01 09:05:00', 0),

    ('11111111-1111-1111-1111-111111111112', 'CB-SEED-0002', 'TXN-SEED-0002', 'PAY-SEED-0002',
     'MERCH-SEED-01', 'CUST-SEED-02', 'VISA', 'CHARGEBACK', 'CARD_NETWORK',
     'CHARGEBACK', 'PENDING_EVIDENCE', '13.1', 'Merchandise / Services Not Received', 8999, 'USD', 1500,
     FALSE, 'PENDING', '2026-07-02 10:00:00', '2026-07-02 10:05:00', '2026-07-22 10:05:00', NULL, NULL,
     TRUE, '2026-07-02 10:05:00', '2026-07-02 10:05:00', 0),

    ('11111111-1111-1111-1111-111111111113', 'CB-SEED-0003', 'TXN-SEED-0003', 'PAY-SEED-0003',
     'MERCH-SEED-02', 'CUST-SEED-03', 'MASTERCARD', 'CHARGEBACK', 'ACQUIRER',
     'CHARGEBACK', 'EVIDENCE_REVIEW', '4853', 'Cardholder Dispute - Defective/Not as Described', 24999, 'USD', 1500,
     FALSE, 'MERCHANT', '2026-07-03 08:30:00', '2026-07-03 08:35:00', '2026-08-17 08:35:00', NULL, NULL,
     TRUE, '2026-07-03 08:35:00', '2026-07-05 12:00:00', 1),

    ('11111111-1111-1111-1111-111111111114', 'CB-SEED-0004', 'TXN-SEED-0004', 'PAY-SEED-0004',
     'MERCH-SEED-02', 'CUST-SEED-04', 'AMEX', 'CHARGEBACK', 'CARD_NETWORK',
     'REPRESENTMENT', 'REPRESENTED', 'C08', 'Goods/Services Not Received', 12000, 'USD', 1500,
     FALSE, 'MERCHANT', '2026-07-04 11:00:00', '2026-07-04 11:05:00', '2026-07-24 11:05:00', NULL, NULL,
     TRUE, '2026-07-04 11:05:00', '2026-07-06 09:00:00', 2),

    ('11111111-1111-1111-1111-111111111115', 'CB-SEED-0005', 'TXN-SEED-0005', 'PAY-SEED-0005',
     'MERCH-SEED-03', 'CUST-SEED-05', 'DISCOVER', 'CHARGEBACK', 'CARD_NETWORK',
     'REPRESENTMENT', 'WON', 'UA02', 'Fraudulent Transaction - Card Not Present', 5000, 'USD', 1500,
     FALSE, 'PLATFORM', '2026-06-20 09:00:00', '2026-06-20 09:05:00', '2026-07-10 09:05:00', '2026-07-08 14:00:00', '2026-07-08 14:05:00',
     TRUE, '2026-06-20 09:05:00', '2026-07-08 14:05:00', 3),

    ('11111111-1111-1111-1111-111111111116', 'CB-SEED-0006', 'TXN-SEED-0006', 'PAY-SEED-0006',
     'MERCH-SEED-03', 'CUST-SEED-06', 'MASTERCARD', 'CHARGEBACK', 'CARD_NETWORK',
     'ARBITRATION', 'LOST', '4837', 'No Cardholder Authorization', 34999, 'USD', 1500,
     FALSE, 'MERCHANT', '2026-06-10 09:00:00', '2026-06-10 09:05:00', '2026-06-30 09:05:00', '2026-07-15 16:00:00', '2026-07-15 16:05:00',
     TRUE, '2026-06-10 09:05:00', '2026-07-15 16:05:00', 4);

-- ---------------------------------------------------------------------------
-- evidence
-- ---------------------------------------------------------------------------
INSERT INTO evidence
    (id, dispute_id, file_name, storage_key, type, category, size_bytes, sha256, status, description,
     malware_scanned, ocr_text, uploaded_by, uploaded_at, reviewed_at, submitted_at)
VALUES
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111112',
     'shipping_confirmation.pdf', 'evidence/seed/shipping_confirmation.pdf', 'PDF', 'TRACKING', 245000,
     'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', 'UPLOADED',
     'Carrier tracking showing delivery to billing address', TRUE, NULL, 'merchant-ops-01',
     '2026-07-03 09:00:00', NULL, NULL),

    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111113',
     'product_photo.png', 'evidence/seed/product_photo.png', 'IMAGE', 'PRODUCT_DESCRIPTION', 1500000,
     'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3', 'ACCEPTED',
     'Photo of item as delivered, matches listing', TRUE, NULL, 'merchant-ops-02',
     '2026-07-04 10:00:00', '2026-07-05 08:00:00', NULL),

    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111113',
     'customer_emails.pdf', 'evidence/seed/customer_emails.pdf', 'PDF', 'COMMUNICATION', 98000,
     'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', 'REJECTED',
     'Email thread with customer about condition of item', TRUE, NULL, 'merchant-ops-02',
     '2026-07-04 10:10:00', '2026-07-05 08:05:00', NULL),

    ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111114',
     'delivery_signature.jpg', 'evidence/seed/delivery_signature.jpg', 'IMAGE', 'SIGNATURE_PROOF', 320000,
     'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5', 'SUBMITTED',
     'Signed proof-of-delivery from carrier', TRUE, NULL, 'merchant-ops-03',
     '2026-07-05 09:00:00', '2026-07-05 15:00:00', '2026-07-06 09:00:00');

-- ---------------------------------------------------------------------------
-- liability
-- ---------------------------------------------------------------------------
INSERT INTO liability
    (id, dispute_id, party, disputed_amount_minor, fee_minor, total_minor, currency, reserve_tier,
     reserve_percentage, ledger_hold_id, ledger_journal_id, reversed, recorded_at, reversed_at)
VALUES
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111113',
     'MERCHANT', 24999, 1500, 26499, 'USD', 'STANDARD', 10, 'HOLD-SEED-0003', 'JRNL-SEED-0003',
     FALSE, '2026-07-05 12:00:00', NULL),

    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111114',
     'MERCHANT', 12000, 1500, 13500, 'USD', 'STANDARD', 10, 'HOLD-SEED-0004', 'JRNL-SEED-0004',
     FALSE, '2026-07-06 09:00:00', NULL),

    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111115',
     'PLATFORM', 5000, 1500, 6500, 'USD', 'STANDARD', 10, 'HOLD-SEED-0005', 'JRNL-SEED-0005',
     TRUE, '2026-06-20 09:10:00', '2026-07-08 14:05:00'),

    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111116',
     'MERCHANT', 34999, 1500, 36499, 'USD', 'ELEVATED', 25, 'HOLD-SEED-0006', 'JRNL-SEED-0006',
     FALSE, '2026-07-15 16:05:00', NULL);

-- ---------------------------------------------------------------------------
-- representments
-- ---------------------------------------------------------------------------
INSERT INTO representments
    (id, dispute_id, stage, status, network_reference, evidence_count, fee_minor, issuer_response,
     narrative, submitted_by, submitted_at, decided_at, created_at)
VALUES
    ('44444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111114',
     'REPRESENTMENT', 'SUBMITTED', 'NETREF-SEED-0004', 1, 500, NULL,
     'Signed delivery confirmation attached, goods received by customer.', 'merchant-ops-03',
     '2026-07-06 09:00:00', NULL, '2026-07-06 08:55:00'),

    ('44444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111115',
     'REPRESENTMENT', 'ACCEPTED', 'NETREF-SEED-0005', 2, 500, 'ACCEPTED',
     'AVS/CVV match and 3DS authentication proof attached.', 'merchant-ops-04',
     '2026-06-25 10:00:00', '2026-07-08 14:00:00', '2026-06-25 09:55:00'),

    ('44444444-4444-4444-4444-444444444443', '11111111-1111-1111-1111-111111111116',
     'ARBITRATION', 'REJECTED', 'NETREF-SEED-0006', 1, 500, 'ESCALATED',
     'Authorization record submitted; issuer rejected and case escalated to arbitration.', 'merchant-ops-05',
     '2026-06-15 10:00:00', '2026-07-15 16:00:00', '2026-06-15 09:55:00');

-- ---------------------------------------------------------------------------
-- dispute_events (ids auto-assigned via BIGSERIAL)
-- ---------------------------------------------------------------------------
INSERT INTO dispute_events (dispute_id, type, actor, description, from_status, to_status, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'DISPUTE_CREATED', 'system',
     'Dispute opened from chargeback CB-SEED-0001', NULL, 'OPEN', '2026-07-01 09:05:00'),

    ('11111111-1111-1111-1111-111111111112', 'DISPUTE_CREATED', 'system',
     'Dispute opened from chargeback CB-SEED-0002', NULL, 'OPEN', '2026-07-02 10:05:00'),
    ('11111111-1111-1111-1111-111111111112', 'EVIDENCE_REQUESTED', 'system',
     'Evidence requested from merchant', 'OPEN', 'PENDING_EVIDENCE', '2026-07-02 10:10:00'),

    ('11111111-1111-1111-1111-111111111113', 'DISPUTE_CREATED', 'system',
     'Dispute opened from chargeback CB-SEED-0003', NULL, 'OPEN', '2026-07-03 08:35:00'),
    ('11111111-1111-1111-1111-111111111113', 'EVIDENCE_REQUESTED', 'system',
     'Evidence requested from merchant', 'OPEN', 'PENDING_EVIDENCE', '2026-07-03 08:40:00'),
    ('11111111-1111-1111-1111-111111111113', 'STATUS_CHANGED', 'merchant-ops-02',
     'Evidence submitted, moved to review', 'PENDING_EVIDENCE', 'EVIDENCE_REVIEW', '2026-07-05 12:00:00'),

    ('11111111-1111-1111-1111-111111111114', 'REPRESENTMENT_SUBMITTED', 'merchant-ops-03',
     'Representment submitted to network', 'EVIDENCE_REVIEW', 'REPRESENTED', '2026-07-06 09:00:00'),

    ('11111111-1111-1111-1111-111111111115', 'ISSUER_RESPONSE_RECEIVED', 'system',
     'Issuer accepted representment, dispute won', 'REPRESENTED', 'WON', '2026-07-08 14:00:00'),
    ('11111111-1111-1111-1111-111111111115', 'DISPUTE_WON', 'system',
     'Dispute closed as won', 'WON', 'CLOSED', '2026-07-08 14:05:00'),

    ('11111111-1111-1111-1111-111111111116', 'ARBITRATION_FILED', 'merchant-ops-05',
     'Case escalated to network arbitration', 'PRE_ARBITRATION', 'ARBITRATION', '2026-06-15 10:00:00'),
    ('11111111-1111-1111-1111-111111111116', 'ARBITRATION_DECISION', 'system',
     'Arbitration decided against merchant', 'ARBITRATION', 'LOST', '2026-07-15 16:00:00'),
    ('11111111-1111-1111-1111-111111111116', 'DISPUTE_LOST', 'system',
     'Dispute closed as lost', 'LOST', 'CLOSED', '2026-07-15 16:05:00');
