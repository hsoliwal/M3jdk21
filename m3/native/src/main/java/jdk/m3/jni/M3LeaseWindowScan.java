// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Objects;

/**
 * Bounded scans over explicit, parent-owned logical lease metadata.
 *
 * <p>The original public APIs of M3ScopedLeaseTable and M3NativeLeaseScan are unchanged.
 * A scan needs a caller-owned output buffer of any positive size, not a long[] sized for
 * every table entry. Start with cursor zero, read the written prefix, then resume at
 * nextIndex(result) until the cursor equals table.size(). IDs retain allocation order.
 *
 * <p>Results are observational candidates only, NOT JVM reference counts, garbage-collector
 * reachability facts, or permission to release native/UI/Java resources. Separate calls are
 * not a frozen snapshot: after each batch, the owner must recheck eligible(id), identity,
 * generation and any external lifetime conditions before retiring or releasing a resource.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/java/com/synexia/primitives/M3LeaseWindowScan.java blob 467b47d69065; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3LeaseWindowScan {
    private M3LeaseWindowScan() { }

    /**
     * Uses the Java path by default, with no JNI requirement or temporary per-handle objects.
     * @return packed cursor/result: high 32 bits next index, low 32 bits copied count
     */
    public static long scan(M3ScopedLeaseTable table, int firstIndex, long[] output) {
        return scan(table, firstIndex, output, false);
    }

    /** Optional native path, using fixed-size stack chunks instead of full-array JNI copies. */
    public static long scan(M3ScopedLeaseTable table, int firstIndex, long[] output,
                            boolean preferNative) {
        Objects.requireNonNull(table, "table");
        return table.collectEligibleWindow(firstIndex, output, preferNative);
    }

    /** Exclusive next index; increases across any non-terminal call. */
    public static int nextIndex(long packed) {
        return M3NativeLeaseScan.nextIndex(packed);
    }

    /** Number of eligible IDs written to the output's prefix. */
    public static int written(long packed) {
        return M3NativeLeaseScan.written(packed);
    }
}
