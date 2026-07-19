package com.paymentprocessor.analytics.service.query;

/** Describes one output column: its result alias, human label, and type. */
public record QueryColumn(String alias, String label, ColumnType type) { }
