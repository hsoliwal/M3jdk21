// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Objects;

/**
 * Materialized byte[] concatenation through the existing managed JNI array
 * owner. No global references, retained native pointers, or separate counters.
 *
 * <p>The caller must have registered each ID against its actual array through
 * M3ScopedLeaseTable.registerJniArray. This adapter cannot prove that mapping
 * or infer all JVM aliases. Each input occurrence holds a registered JNI borrow,
 * including both occurrences when the same array/ID appears twice. The borrow
 * ends after the native function completes all array reads or on failure.</p>
 *
 * <p>Prefer M3ByteSlices/M3CompositeBytes in M3JDK 21 for true zero-copy
 * immutable concatenation; this API deliberately materializes one fresh
 * mutable Java byte[] for compatibility at a JNI boundary.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/java/com/synexia/primitives/M3JniByteConcat.java blob 5aa88a35bf31; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3JniByteConcat {
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
    // Java-only calls never attempt to load an optional native library.
    private static final class Native {
        private static final boolean LOADED = loadNative();
        private Native() { }
    }

    private M3JniByteConcat() { }

    /** Library-load status; actual symbol availability is verified at invocation. */
    public static boolean nativeLibraryLoaded() {
        return Native.LOADED;
    }

    private static boolean loadNative() {
        try {
            System.loadLibrary("m3_scoped_leases");
            return true;
        } catch (UnsatisfiedLinkError unavailable) {
            return false;
        }
    }

    /**
     * One new mutable output array. Source IDs are qualified and registered
     * before this call. The opt-in native path falls back to the exact Java
     * contract if an older optional library lacks the new symbol.
     */
    public static byte[] concat(M3ScopedLeaseTable owner, long leftId, byte[] left,
                                long rightId, byte[] right, boolean preferNative) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        long required = (long) left.length + right.length;
        if (required > MAX_ARRAY_LENGTH) {
            throw new OutOfMemoryError("M3 byte concatenation exceeds Java array limit");
        }
        int length = (int) required;
        return M3JniArrayReferences.withConcat(owner, leftId, rightId,
                () -> perform(left, right, length, preferNative));
    }

    private static byte[] perform(byte[] left, byte[] right, int length,
                                  boolean preferNative) {
        if (preferNative && Native.LOADED) {
            try {
                return concatNative(left, right);
            } catch (UnsatisfiedLinkError missingSymbol) {
                if (Boolean.getBoolean("jdk.m3.jni.requireNative")) {
                    throw missingSymbol;
                }
                // Old optional library: use the contract-preserving Java path.
            }
        }
        byte[] out = new byte[length];
        System.arraycopy(left, 0, out, 0, left.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }

    private static native byte[] concatNative(byte[] left, byte[] right);
}
