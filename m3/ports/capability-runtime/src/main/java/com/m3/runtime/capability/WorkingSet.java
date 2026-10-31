// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.util.Objects;

/**
 * CPU-first explicit working-set admission and exclusive operation leases.
 * Reserve logical resident bytes, not JVM heap residency: this does not spill or
 * change GC ownership, and its budget is never advertised as total RSS/Xmx.
 */
public final class WorkingSet {
    private final long budgetBytes;
    private long reserved;

    public WorkingSet(long budgetBytes) {
        if (budgetBytes < 0) throw new IllegalArgumentException("negative working-set budget");
        this.budgetBytes = budgetBytes;
    }

    public long budgetBytes() { return budgetBytes; }
    public synchronized long reservedBytes() { return reserved; }

    /** A failed admission never increments the working-set counter. */
    public synchronized Lease acquire(SourceArtifact.Span span) {
        Objects.requireNonNull(span, "span");
        int bytes = span.length();
        if ((long) bytes > budgetBytes - reserved) {
            throw new IllegalStateException("insufficient working-set headroom");
        }
        reserved += bytes;
        return new Lease(span);
    }

    private synchronized void release(int bytes) {
        if (bytes > reserved) throw new IllegalStateException("working-set accounting corrupt");
        reserved -= bytes;
    }

    /**
     * A lease owns only a stable source/range coordinate, never a native pointer.
     * Reads and close serialize on this lease; use-after-close fails closed.
     */
    public final class Lease implements AutoCloseable {
        private SourceArtifact.Span span;
        private Lease(SourceArtifact.Span span) { this.span = span; }

        private SourceArtifact.Span active() {
            if (span == null) throw new IllegalStateException("working-set lease closed");
            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("source scan interrupted");
            }
            return span;
        }
        public synchronized String sourceRoot() { return active().source().root(); }
        public synchronized String rangeRoot() { return active().sha256(); }
        public synchronized int length() { return active().length(); }
        public synchronized byte byteAt(int index) { return active().byteAt(index); }
        public synchronized byte[] copy() { return active().copy(); }
        public synchronized long countLf() {
            var selected = active();
            long result = 0;
            for (int i = 0; i < selected.length(); i++) {
                if ((i & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("source scan interrupted");
                }
                if (selected.byteAt(i) == (byte) '\n') result++;
            }
            return result;
        }
        @Override public synchronized void close() {
            if (span == null) return;
            int count = span.length();
            span = null;
            WorkingSet.this.release(count);
        }
    }
}
