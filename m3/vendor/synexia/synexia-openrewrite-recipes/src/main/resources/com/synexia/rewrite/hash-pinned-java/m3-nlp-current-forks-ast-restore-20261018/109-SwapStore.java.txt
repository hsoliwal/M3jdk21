// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.swap;

import java.io.IOException;
import java.util.Map;

/** Immutable, namespace/codec-bound durable records. Implementations must commit atomically. */
public interface SwapStore extends AutoCloseable {
    record Head(long revision, long recordId) {
        public Head {
            if (revision < 0 || (revision == 0 ? recordId != -1 : recordId < 0)) {
                throw new IllegalArgumentException("invalid head");
            }
        }
    }
    record Publication(String name, long expectedRevision, long recordId) {
        public Publication {
            requireName(name);
            if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE || recordId < 0) {
                throw new IllegalArgumentException("invalid publication");
            }
        }
    }
    /** Null means absent, never an empty record. Returned arrays are caller-owned. */
    byte[] read(long id) throws IOException;
    /** Exact duplicate writes are idempotent; conflicting bytes must roll back the whole batch. */
    void write(Map<Long, byte[]> records, Publication publication) throws IOException;
    default void write(Map<Long, byte[]> records) throws IOException { write(records, null); }
    Head head(String name) throws IOException;
    /** Bounded ascending keyset page; afterExclusive=-1 starts enumeration. */
    long[] ids(long afterExclusive, int limit) throws IOException;
    long count() throws IOException;

    /**
     * Whether this namespace can transactionally delete old records that are not referenced by
     * any publication head.
     */
    default boolean supportsUnreferencedPrune() { return false; }

    /**
     * Delete at most {@code maxDeletes} unreferenced records below {@code beforeExclusive}.
     * Publication targets must never be deleted.
     */
    default int pruneUnreferencedBefore(long beforeExclusive, int maxDeletes) throws IOException {
        throw new UnsupportedOperationException("unreferenced pruning is not supported");
    }

    int maxRecordBytes();
    int maxBatchBytes();
    int maxBatchRecords();
    @Override void close() throws IOException;

    static void requireId(long id) {
        if (id < 0) throw new IllegalArgumentException("negative record id");
    }
    /**
     * Validate the canonical 1..128-character ASCII namespace/publication grammar.
     * Returns the original reference; never normalizes, copies, or interns the name.
     * The bounded scan agrees with the SQLite JNI identifier gate without compiling a regex.
     * Nulls, empty/oversized names, surrogates and all non-ASCII characters remain invalid.
     */
    static String requireName(String name) {
        if (!validName(name)) {
            throw new IllegalArgumentException("name must contain 1..128 ASCII identifier characters");
        }
        return name;
    }

    /** Internal predicate only: no retained matcher, table, payload or initialization state. */
    private static boolean validName(String name) {
        if (name == null || name.isEmpty() || name.length() > 128) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '.' || c == '_' || c == ':' || c == '/' || c == '-')) return false;
        }
        return true;
    }
}
