package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.Adjustment;
import com.paymentprocessor.reconciliationservice.domain.BankStatement;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import com.paymentprocessor.reconciliationservice.domain.RecordMatchStatus;
import com.paymentprocessor.reconciliationservice.domain.StatementStatus;

import java.util.List;

/** Maps inbound request records to domain entities. */
public final class ReconMapper {

    private ReconMapper() {
    }

    public static ReconRun toRun(ReconRunCreateRequest req) {
        ReconRun run = new ReconRun();
        run.setReconType(req.reconType());
        run.setChannel(req.channel());
        run.setAccountRef(req.accountRef());
        run.setBusinessDate(req.businessDate());
        run.setCurrency(req.currency());
        run.setTriggeredBy(req.triggeredBy());
        return run;
    }

    public static ReconRecord toRecord(ReconRecordRequest req) {
        ReconRecord record = new ReconRecord();
        record.setSource(req.source());
        record.setSourceSystem(req.sourceSystem());
        record.setExternalReference(req.externalReference());
        record.setArn(req.arn());
        record.setInternalPaymentId(req.internalPaymentId());
        record.setAmount(req.amount());
        record.setFeeAmount(req.feeAmount());
        record.setCurrency(req.currency());
        record.setTransactionDate(req.transactionDate());
        record.setValueDate(req.valueDate());
        record.setMerchantId(req.merchantId());
        record.setCounterparty(req.counterparty());
        record.setCardBin(req.cardBin());
        record.setCardLast4(req.cardLast4());
        record.setTransactionStatus(req.transactionStatus());
        record.setRawPayload(req.rawPayload());
        record.setMatchStatus(RecordMatchStatus.UNMATCHED);
        return record;
    }

    public static List<ReconRecord> toRecords(List<ReconRecordRequest> requests) {
        return requests.stream().map(ReconMapper::toRecord).toList();
    }

    public static BankStatement toStatement(StatementIngestRequest req) {
        BankStatement statement = new BankStatement();
        statement.setStatementReference(req.statementReference());
        statement.setBankName(req.bankName());
        statement.setAccountType(req.accountType());
        statement.setAccountNumber(req.accountNumber());
        statement.setFormat(req.format());
        statement.setCurrency(req.currency());
        statement.setStatementDate(req.statementDate());
        statement.setOpeningBalance(req.openingBalance());
        statement.setClosingBalance(req.closingBalance());
        statement.setStatus(StatementStatus.INGESTED);
        return statement;
    }

    public static Adjustment toAdjustment(AdjustmentCreateRequest req) {
        Adjustment adjustment = new Adjustment();
        adjustment.setAdjustmentType(req.adjustmentType());
        adjustment.setAmount(req.amount());
        adjustment.setCurrency(req.currency());
        adjustment.setReason(req.reason());
        adjustment.setCreatedBy(req.createdBy());
        return adjustment;
    }
}
