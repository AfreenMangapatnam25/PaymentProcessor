package com.paymentprocessor.analytics.service.query;

import com.paymentprocessor.analytics.exception.QueryValidationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Compiles a validated {@link QuerySpec} into a {@link SafeQuery}. Column and function
 * identifiers are taken exclusively from the {@link DatasetCatalog}; every literal value
 * is emitted as a {@code ?} placeholder and returned in the params list. No user-supplied
 * string is ever concatenated into the SQL text.
 */
@Component
public class SafeQueryBuilder {

    private static final int MAX_LIMIT = 100_000;

    public SafeQuery build(QuerySpec spec, Dataset dataset) {
        List<Object> params = new ArrayList<>();
        List<QueryColumn> out = new ArrayList<>();
        StringBuilder select = new StringBuilder();

        // ----- SELECT list -----
        List<String> selectExprs = new ArrayList<>();
        for (String dim : spec.dimensions()) {
            Column c = requireColumn(dataset, dim);
            selectExprs.add(c.physical() + " AS " + alias(c.name()));
            out.add(new QueryColumn(c.name(), c.label(), c.type()));
        }
        for (QuerySpec.MeasureRef m : spec.measures()) {
            String a;
            String label;
            ColumnType type = ColumnType.NUMBER;
            if (m.aggregation() == Aggregation.COUNT) {
                a = "cnt";
                label = "Count";
                selectExprs.add("count() AS " + alias(a));
            } else {
                Column c = requireColumn(dataset, m.column());
                if (!c.measure()) {
                    throw new QueryValidationException("Column '" + m.column() + "' is not aggregatable");
                }
                a = m.aggregation().name().toLowerCase() + "_" + c.name();
                label = m.aggregation() + "(" + c.label() + ")";
                selectExprs.add(m.aggregation().apply(c.physical()) + " AS " + alias(a));
            }
            out.add(new QueryColumn(a, label, type));
        }
        if (selectExprs.isEmpty()) {
            throw new QueryValidationException("Query must select at least one dimension or measure");
        }
        select.append("SELECT ").append(String.join(", ", selectExprs));
        select.append(" FROM ").append(dataset.table());

        // ----- WHERE: mandatory date range (partition pruning) + filters -----
        List<String> where = new ArrayList<>();
        where.add(dataset.dateColumn() + " >= ?");
        params.add(spec.fromDate().toString());
        where.add(dataset.dateColumn() + " <= ?");
        params.add(spec.toDate().toString());

        for (QuerySpec.Filter f : spec.filters()) {
            Column c = requireColumn(dataset, f.column());
            if (!f.op().allowedForType(c.type())) {
                throw new QueryValidationException(
                        "Operator " + f.op() + " not allowed for column '" + c.name() + "'");
            }
            where.add(renderFilter(c, f, params));
        }
        select.append(" WHERE ").append(String.join(" AND ", where));

        // ----- GROUP BY (all dimensions when aggregating) -----
        if (spec.isAggregated() && !spec.dimensions().isEmpty()) {
            List<String> groups = new ArrayList<>();
            for (String dim : spec.dimensions()) {
                groups.add(requireColumn(dataset, dim).physical());
            }
            select.append(" GROUP BY ").append(String.join(", ", groups));
        }

        // ----- ORDER BY (aliases must exist in output) -----
        if (spec.orderBy() != null && !spec.orderBy().isEmpty()) {
            List<String> orders = new ArrayList<>();
            for (QuerySpec.OrderBy o : spec.orderBy()) {
                boolean known = out.stream().anyMatch(qc -> qc.alias().equals(o.alias()));
                if (!known) {
                    throw new QueryValidationException("Cannot order by unknown output '" + o.alias() + "'");
                }
                orders.add(alias(o.alias()) + (o.descending() ? " DESC" : " ASC"));
            }
            select.append(" ORDER BY ").append(String.join(", ", orders));
        }

        // ----- LIMIT / OFFSET -----
        int limit = Math.min(spec.limit() <= 0 ? 1000 : spec.limit(), MAX_LIMIT);
        int offset = Math.max(spec.offset(), 0);
        select.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return new SafeQuery(select.toString(), params, out);
    }

    private String renderFilter(Column c, QuerySpec.Filter f, List<Object> params) {
        String col = c.physical();
        switch (f.op()) {
            case IN, NOT_IN -> {
                if (f.values().isEmpty()) {
                    throw new QueryValidationException("IN filter on '" + c.name() + "' needs values");
                }
                String ph = String.join(", ", f.values().stream().map(v -> "?").toList());
                f.values().forEach(v -> params.add(coerce(c, v)));
                return col + " " + f.op().sql() + " (" + ph + ")";
            }
            case BETWEEN -> {
                if (f.values().size() != 2) {
                    throw new QueryValidationException("BETWEEN on '" + c.name() + "' needs exactly 2 values");
                }
                params.add(coerce(c, f.values().get(0)));
                params.add(coerce(c, f.values().get(1)));
                return col + " BETWEEN ? AND ?";
            }
            case LIKE -> {
                params.add(String.valueOf(f.values().get(0)));
                return col + " LIKE ?";
            }
            default -> {
                params.add(coerce(c, f.values().get(0)));
                return col + " " + f.op().sql() + " ?";
            }
        }
    }

    private Object coerce(Column c, Object raw) {
        if (raw == null) return null;
        try {
            return switch (c.type()) {
                case NUMBER -> raw instanceof Number ? raw : Double.valueOf(raw.toString());
                case BOOLEAN -> {
                    if (raw instanceof Boolean b) yield b ? 1 : 0;
                    yield Boolean.parseBoolean(raw.toString()) ? 1 : 0;
                }
                default -> raw.toString();
            };
        } catch (NumberFormatException e) {
            throw new QueryValidationException("Value '" + raw + "' is not valid for column '" + c.name() + "'");
        }
    }

    /** Quote an alias with backticks; alias text is catalog-derived, but quote defensively. */
    private String alias(String a) {
        if (!a.matches("[a-zA-Z0-9_]+")) {
            throw new QueryValidationException("Illegal alias: " + a);
        }
        return "`" + a + "`";
    }

    private Column requireColumn(Dataset ds, String name) {
        Column c = ds.column(name);
        if (c == null) {
            throw new QueryValidationException(
                    "Unknown column '" + name + "' for dataset '" + ds.name() + "'");
        }
        return c;
    }
}
