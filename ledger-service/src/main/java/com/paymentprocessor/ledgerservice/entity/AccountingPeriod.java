package com.paymentprocessor.ledgerservice.entity;

import com.paymentprocessor.ledgerservice.domain.enums.PeriodState;
import com.paymentprocessor.ledgerservice.domain.enums.PeriodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A discrete, reportable accounting interval. Postings are only permitted into
 * a period whose {@link PeriodState} accepts postings.
 */
@Entity
@Table(name = "accounting_periods")
public class AccountingPeriod {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "code", length = 32, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 16, nullable = false)
    private PeriodType periodType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 16, nullable = false)
    private PeriodState state = PeriodState.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public PeriodState getState() { return state; }
    public void setState(PeriodState state) { this.state = state; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
