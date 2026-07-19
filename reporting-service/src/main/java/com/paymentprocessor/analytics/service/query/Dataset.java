package com.paymentprocessor.analytics.service.query;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A queryable dataset: a physical ClickHouse table plus its whitelisted columns and
 * the mandatory date column used for range filtering and partition pruning.
 */
public final class Dataset {

    private final String name;
    private final String table;
    private final String dateColumn;
    private final Map<String, Column> columns = new LinkedHashMap<>();

    public Dataset(String name, String table, String dateColumn) {
        this.name = name;
        this.table = table;
        this.dateColumn = dateColumn;
    }

    Dataset add(Column c) {
        columns.put(c.name(), c);
        return this;
    }

    public String name() { return name; }
    public String table() { return table; }
    public String dateColumn() { return dateColumn; }
    public Map<String, Column> columns() { return columns; }
    public Column column(String name) { return columns.get(name); }
    public boolean has(String name) { return columns.containsKey(name); }
}
