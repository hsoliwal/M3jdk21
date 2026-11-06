// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * JNI array ownership bridges to the existing M3ScopedLeaseTable.
 *
 * <p>Every source occurrence in a JNI concatenation is one registered
 * cooperative lease, including repeated uses of the same source array.
 * The table remains the only count owner; no new reference-count ledger,
 * per-edge entry, JNI global reference or GC-derived reuse permission is
 * constructed here. A native caller must bind each logical ID to the
 * corresponding array through its qualified owner contract.</p>
 *
 * <p>The scoped variants require the native call to release ALL JNI
 * Get*ArrayElements/GetPrimitiveArrayCritical access before returning.
 * JNI_COMMIT alone does not terminate a copied-elements access. If a
 * zero-copy native composite outlives the call, retainSources/releaseSources
 * must instead cover its entire lifetime. Native strong global references
 * use pinGlobal/unpinGlobal across successful NewGlobalRef/DeleteGlobalRef.
 * Unregistered native aliases, weak globals and unknown escapes always
 * disqualify DOOP recycling. Array IDs must first be registered with M3ScopedLeaseTable.registerJniArray. Counts are not arbitrary JVM references.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/java/com/synexia/primitives/M3JniArrayReferences.java blob 9986c7a6b84a; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3JniArrayReferences {
    private M3JniArrayReferences() { }

    /** Count one registered JNI array borrow using the canonical JNI pin lane. */
    public static void withArray(M3ScopedLeaseTable table, long arrayId,
                                 Runnable nativeCall) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(nativeCall, "nativeCall");
        table.beginJniArrayAccess(arrayId);
        Throwable primary = null;
        try {
            nativeCall.run();
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                table.endJniArrayAccess(arrayId);
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (primary != cleanup) primary.addSuppressed(cleanup);
            }
        }
    }

    /** Count both input-array edges during a copied JNI concatenation. */
    public static void withConcat(M3ScopedLeaseTable table, long leftId,
                                  long rightId, Runnable nativeCall) {
        Objects.requireNonNull(nativeCall, "nativeCall");
        retainSources(table, leftId, rightId);
        Throwable primary = null;
        try {
            nativeCall.run();
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                releaseSources(table, leftId, rightId);
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (primary != cleanup) primary.addSuppressed(cleanup);
            }
        }
    }

    /** Value-returning form; a returned ALIAS needs independent retained edges. */
    public static <R> R withConcat(M3ScopedLeaseTable table, long leftId,
                                    long rightId, Supplier<? extends R> nativeCall) {
        Objects.requireNonNull(nativeCall, "nativeCall");
        retainSources(table, leftId, rightId);
        Throwable primary = null;
        try {
            return nativeCall.get();
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                releaseSources(table, leftId, rightId);
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (primary != cleanup) primary.addSuppressed(cleanup);
            }
        }
    }

    /**
     * Retain each source once for the entire lifetime of a zero-copy
     * native concatenation. An aliased pair intentionally counts twice.
     * All-or-nothing even if the second acquire fails.
     */
    public static void retainSources(M3ScopedLeaseTable table, long leftId,
                                     long rightId) {
        Objects.requireNonNull(table, "table");
        table.beginJniArrayAccess(leftId);
        try {
            table.beginJniArrayAccess(rightId);
        } catch (RuntimeException | Error failure) {
            try {
                table.endJniArrayAccess(leftId);
            } catch (RuntimeException | Error cleanup) {
                if (failure != cleanup) failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    /** Release both retained source edges, including aliased occurrences. */
    public static void releaseSources(M3ScopedLeaseTable table, long leftId,
                                      long rightId) {
        Objects.requireNonNull(table, "table");
        try {
            table.endJniArrayAccess(rightId);
        } catch (RuntimeException | Error first) {
            try {
                table.endJniArrayAccess(leftId);
            } catch (RuntimeException | Error second) {
                if (first != second) first.addSuppressed(second);
            }
            throw first;
        }
        table.endJniArrayAccess(leftId);
    }

    /**
     * Scoped native critical access. The native function must complete its
     * ReleasePrimitiveArrayCritical before returning to this Java scope.
     */
    public static void withCritical(M3ScopedLeaseTable table, long arrayId,
                                    Runnable nativeCall) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(nativeCall, "nativeCall");
        table.beginJniArrayAccess(arrayId);
        Throwable primary = null;
        try {
            nativeCall.run();
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                table.endJniArrayAccess(arrayId);
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (primary != cleanup) primary.addSuppressed(cleanup);
            }
        }
    }


    /** Maximum number of per-occurrence JNI array borrows in one scoped batch. */
    public static final int MAX_SCOPED_ARRAYS = 4096;

    /**
     * Scope a bounded sequence of JNI array reads/writes under the existing lease table.
     * Every occurrence counts, including duplicate IDs. One defensive long[] snapshot
     * prevents the caller from changing IDs during native execution: no per-edge objects.
     * All native Get*ArrayElements/critical pointers must be released before callback
     * return (JNI_COMMIT alone does not terminate a copied JNI elements access).
     * Native critical regions must not invoke Java callbacks or blocking JNI operations.
     * Returned aliases/asynchronous native access require independent retained leases.
     */
    public static void withArrayBatch(M3ScopedLeaseTable table, long[] arrayIds,
                                      Runnable nativeCall) {
        Objects.requireNonNull(nativeCall, "nativeCall");
        long[] frozen = checkedArrayIds(table, arrayIds);
        beginBatch(table, frozen);
        Throwable primary = null;
        try {
            nativeCall.run();
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            endBatchPreservingFailure(table, frozen, primary);
        }
    }

    /** Value-returning scoped form; returned native aliases need separate leases. */
    public static <R> R withArrayBatch(M3ScopedLeaseTable table, long[] arrayIds,
                                       Supplier<? extends R> nativeCall) {
        Objects.requireNonNull(nativeCall, "nativeCall");
        long[] frozen = checkedArrayIds(table, arrayIds);
        beginBatch(table, frozen);
        Throwable primary = null;
        try {
            return nativeCall.get();
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            endBatchPreservingFailure(table, frozen, primary);
        }
    }

    private static long[] checkedArrayIds(M3ScopedLeaseTable table, long[] arrayIds) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(arrayIds, "arrayIds");
        if (arrayIds.length == 0 || arrayIds.length > MAX_SCOPED_ARRAYS) {
            throw new IllegalArgumentException("JNI array batch size out of bounds");
        }
        long[] frozen = arrayIds.clone();
        for (long id : frozen) {
            if (id <= 0) throw new IllegalArgumentException("invalid JNI array ID: " + id);
        }
        return frozen;
    }

    /**
     * One atomic managed-reservation transition. The lease table is the only
     * ownership monitor, and its synchronized per-ID methods reenter this lock.
     * No observer may mistake an incomplete array batch for a publishable state.
     */
    private static void beginBatch(M3ScopedLeaseTable table, long[] ids) {
        synchronized (table) {
            int acquired = 0;
            try {
                for (long id : ids) {
                    table.beginJniArrayAccess(id);
                    acquired++;
                }
            } catch (RuntimeException | Error failure) {
                try {
                    endBatch(table, ids, acquired);
                } catch (RuntimeException | Error cleanup) {
                    if (failure != cleanup) failure.addSuppressed(cleanup);
                }
                throw failure;
            }
        }
    }

    private static void endBatchPreservingFailure(M3ScopedLeaseTable table, long[] ids,
                                                   Throwable primary) {
        try {
            endBatch(table, ids, ids.length);
        } catch (RuntimeException | Error cleanup) {
            if (primary == null) throw cleanup;
            if (primary != cleanup) primary.addSuppressed(cleanup);
        }
    }

    /**
     * Publish the whole release as one owner-monitor transition. Even if a
     * particular release fails, drain every other edge and retain the first
     * cleanup error with later errors suppressed. The caller preserves its own
     * callback error in preference to a cleanup failure.
     */
    private static void endBatch(M3ScopedLeaseTable table, long[] ids, int acquired) {
        synchronized (table) {
            Throwable first = null;
            for (int i = acquired - 1; i >= 0; i--) {
                try {
                    table.endJniArrayAccess(ids[i]);
                } catch (RuntimeException | Error cleanup) {
                    if (first == null) first = cleanup;
                    else if (first != cleanup) first.addSuppressed(cleanup);
                }
            }
            if (first instanceof RuntimeException runtime) throw runtime;
            if (first instanceof Error error) throw error;
        }
    }

    /** Call only AFTER a strong JNI NewGlobalRef has succeeded. */
    public static void pinGlobal(M3ScopedLeaseTable table, long arrayId) {
        Objects.requireNonNull(table, "table").pin(arrayId);
    }

    /** Call when its matching strong global is deleted via DeleteGlobalRef. */
    public static void unpinGlobal(M3ScopedLeaseTable table, long arrayId) {
        Objects.requireNonNull(table, "table").unpin(arrayId);
    }
}
