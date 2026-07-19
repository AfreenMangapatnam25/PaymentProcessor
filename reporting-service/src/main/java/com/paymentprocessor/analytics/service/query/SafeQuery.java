package com.paymentprocessor.analytics.service.query;

import java.util.List;

/** A fully parameterized SQL statement: identifiers are catalog-derived, values bound. */
public record SafeQuery(String sql, List<Object> params, List<QueryColumn> outputColumns) { }
