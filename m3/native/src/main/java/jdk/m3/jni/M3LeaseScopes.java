// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Allocation-free, exception-safe lexical leases for explicitly registered M3 logical handles.
 * The table remains the canonical owner of counts and identity; this helper creates no lease
 * object and never observes or replaces JVM references or garbage collection.
 *
 * <p>Callers MUST NOT treat a callback's completion as proof that objects passed into or
 * retained by external code have become unreachable. Nested callbacks and exceptions balance
 * their explicitly registered counts; public Java facades keep their normal identity.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/java/com/synexia/primitives/M3LeaseScopes.java blob d8ac566afd4f; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3LeaseScopes {
    private M3LeaseScopes() { }

    /** Registers a logical lease throughout the callback, releasing it on every exit. */
    public static void withLease(M3ScopedLeaseTable table, long id, Runnable body) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(body, "body");
        table.acquire(id);
        try {
            body.run();
        } finally {
            table.release(id);
        }
    }

    /** Value-returning lexical lease; callback result may escape, so no object is reclaimed. */
    public static <T> T withLease(M3ScopedLeaseTable table, long id,
                                  Supplier<? extends T> body) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(body, "body");
        table.acquire(id);
        try {
            return body.get();
        } finally {
            table.release(id);
        }
    }

    /** Pins a registered logical resource throughout a bounded callback. */
    public static void withPin(M3ScopedLeaseTable table, long id, Runnable body) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(body, "body");
        table.pin(id);
        try {
            body.run();
        } finally {
            table.unpin(id);
        }
    }
}
