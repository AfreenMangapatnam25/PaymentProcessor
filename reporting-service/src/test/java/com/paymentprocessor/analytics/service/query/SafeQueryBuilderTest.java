package com.paymentprocessor.analytics.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentprocessor.analytics.exception.QueryValidationException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SafeQueryBuilderTest {

    private DatasetCatalog catalog;
    private SafeQueryBuilder builder;

    @BeforeEach
    void setup() {
        catalog = new DatasetCatalog();
        catalog.init(); // package-private; wires the whitelists
        builder = new SafeQueryBuilder();
    }

    @Test
    void buildsParameterizedAggregateQuery() {
        QuerySpec spec = new QuerySpec("fees",
                List.of("connector"),
                List.of(new QuerySpec.MeasureRef("fee_minor", Aggregation.SUM)),
                List.of(new QuerySpec.Filter("currency", FilterOp.EQ, List.of("USD"))),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                List.of(new QuerySpec.OrderBy("sum_fee_minor", true)),
                100, 0);

        SafeQuery q = builder.build(spec, catalog.get("fees"));

        assertThat(q.sql())
                .contains("FROM fact_payments")
                .contains("sum(fee_minor) AS `sum_fee_minor`")
                .contains("GROUP BY connector")
                .contains("WHERE created_date >= ? AND created_date <= ? AND currency = ?")
                .contains("ORDER BY `sum_fee_minor` DESC")
                .contains("LIMIT ? OFFSET ?");
        // date-from, date-to, currency, limit, offset
        assertThat(q.params()).containsExactly("2026-06-01", "2026-06-30", "USD", 100, 0);
    }

    @Test
    void rejectsUnknownColumnInsteadOfInterpolating() {
        QuerySpec spec = new QuerySpec("fees",
                List.of("connector; DROP TABLE fact_payments"),
                List.of(), List.of(),
                LocalDate.now().minusDays(1), LocalDate.now(),
                List.of(), 10, 0);
        assertThatThrownBy(() -> builder.build(spec, catalog.get("fees")))
                .isInstanceOf(QueryValidationException.class)
                .hasMessageContaining("Unknown column");
    }

    @Test
    void rejectsAggregatingADimension() {
        QuerySpec spec = new QuerySpec("transactions",
                List.of(),
                List.of(new QuerySpec.MeasureRef("status", Aggregation.SUM)),
                List.of(),
                LocalDate.now().minusDays(1), LocalDate.now(),
                List.of(), 10, 0);
        assertThatThrownBy(() -> builder.build(spec, catalog.get("transactions")))
                .isInstanceOf(QueryValidationException.class)
                .hasMessageContaining("not aggregatable");
    }

    @Test
    void rejectsLikeOnNumericColumn() {
        QuerySpec spec = new QuerySpec("transactions",
                List.of("intent_id"),
                List.of(),
                List.of(new QuerySpec.Filter("amount_minor", FilterOp.LIKE, List.of("1%"))),
                LocalDate.now().minusDays(1), LocalDate.now(),
                List.of(), 10, 0);
        assertThatThrownBy(() -> builder.build(spec, catalog.get("transactions")))
                .isInstanceOf(QueryValidationException.class)
                .hasMessageContaining("not allowed");
    }
}
