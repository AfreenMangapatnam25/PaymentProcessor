package com.paymentprocessor.auditservice.crypto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deterministic ("canonical") JSON serialization used exclusively for hashing.
 *
 * <p>The rules are intentionally fixed and independent of any JSON library's defaults:
 * object keys are sorted lexicographically, arrays preserve order, and scalar
 * representations are stable. This guarantees that the same logical record always
 * produces the same bytes — a prerequisite for a reproducible hash chain.
 *
 * <p>These rules MUST NOT change once records exist, or previously computed hashes
 * would no longer reproduce. Only {@link Map}, {@link Collection}, {@link String},
 * {@link Number}, {@link Boolean}, {@link Instant} and {@code null} are supported;
 * anything else is a programming error and fails fast.
 */
public final class CanonicalJson {

    private CanonicalJson() {
    }

    public static String canonicalize(Object value) {
        StringBuilder sb = new StringBuilder(256);
        write(value, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map<?, ?> map) {
            writeObject((Map<String, Object>) map, sb);
        } else if (value instanceof Collection<?> collection) {
            writeArray(collection, sb);
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Boolean b) {
            sb.append(b.booleanValue() ? "true" : "false");
        } else if (value instanceof Instant instant) {
            writeString(instant.toString(), sb);
        } else if (value instanceof Number n) {
            sb.append(n.toString());
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type for canonical serialization: " + value.getClass().getName());
        }
    }

    private static void writeObject(Map<String, Object> map, StringBuilder sb) {
        // TreeMap gives deterministic lexicographic key ordering.
        TreeMap<String, Object> sorted = new TreeMap<>(map);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(e.getKey(), sb);
            sb.append(':');
            write(e.getValue(), sb);
        }
        sb.append('}');
    }

    private static void writeArray(Collection<?> collection, StringBuilder sb) {
        List<Object> items = new ArrayList<>(collection);
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            write(items.get(i), sb);
        }
        sb.append(']');
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
