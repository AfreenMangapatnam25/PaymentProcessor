package com.paymentprocessor.analytics.service.storage;

import java.io.IOException;
import java.io.OutputStream;

/** Streams content directly into storage without materializing it fully in memory. */
@FunctionalInterface
public interface StreamWriter {
    void writeTo(OutputStream out) throws IOException;
}
