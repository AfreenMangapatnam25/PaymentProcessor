package com.paymentprocessor.analytics.service.query;

public enum Aggregation {
    SUM("sum"), COUNT("count"), COUNT_DISTINCT("uniqExact"),
    AVG("avg"), MIN("min"), MAX("max");

    private final String fn;
    Aggregation(String fn) { this.fn = fn; }

    /** Build the aggregate SQL from a whitelisted physical column expression. */
    public String apply(String physical) {
        if (this == COUNT) return "count()";
        return fn + "(" + physical + ")";
    }
}
