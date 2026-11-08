/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Bounded DOOP viewport selection over parent-owned primitive ID and bit-mask lanes.
 * No child facade, Entry, iterator, or JNI-owned array is created per logical row.
 *
 * <p>A successful call writes only the returned prefix of {@code output}, in input order.
 * The packed result contains the exclusive next input index (upper 32 bits) and number
 * of written IDs (lower 32 bits). The caller continues at the next index, not at the
 * next visible ordinal. Input arrays must not be mutated concurrently with the scan.
 * Only visited IDs are validated, and validation errors leave output unmodified.
 * This is a logical selection operation, not proof of JVM object reachability.
 *
 * <p>JNI is optional and never retains pointers or owns facade lifetime. Small windows
 * normally favor Java; native selection is explicit and requires differential testing.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DoopMaskedWindowScan.java}
 * blob {@code 598e0eab549ae96d0408ba2430737b47ae83f892} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class DoopMaskedWindowScan {
    /** Maximum output IDs per call, avoiding unbounded viewport allocation. */
    public static final int MAX_WINDOW = 4096;

    private static final boolean NATIVE_AVAILABLE = loadNative();

    private DoopMaskedWindowScan() { }

    /** Whether the optional JNI library could be loaded. */
    public static boolean nativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    /** Java-only path; deterministic fallback on all supported Java 21 installations. */
    public static long scan(long[] ids, short[] flags, int count, int first,
            short required, short excluded, long[] output, int offset, int maximum) {
        return scan(ids, flags, count, first, required, excluded, output, offset, maximum, false);
    }

    /**
     * Selects IDs where every required bit is set and every excluded bit is absent.
     * Unused output positions and all parent-owned source arrays are left untouched.
     * A zero-length demand performs no input scan and no native call.
     *
     * @return packed {@code (nextInputIndex, writtenCount)}
     */
    public static long scan(long[] ids, short[] flags, int count, int first,
            short required, short excluded, long[] output, int offset, int maximum,
            boolean preferNative) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(flags, "flags");
        Objects.requireNonNull(output, "output");
        if (output == ids) throw new IllegalArgumentException("source/output alias");
        if (count < 0 || count > ids.length || count > flags.length
                || first < 0 || first > count || maximum < 0 || maximum > MAX_WINDOW) {
            throw new IllegalArgumentException("invalid masked viewport geometry");
        }
        Objects.checkFromIndexSize(offset, maximum, output.length);
        if (maximum == 0 || first == count) return pack(first, 0);
        long result;
        if (preferNative && NATIVE_AVAILABLE) {
            try {
                result = scanNative(ids, flags, count, first,
                        required, excluded, output, offset, maximum);
            } catch (UnsatisfiedLinkError incompatibleNativeLibrary) {
                result = scanJava(ids, flags, count, first,
                        required, excluded, output, offset, maximum);
            }
        } else {
            result = scanJava(ids, flags, count, first,
                    required, excluded, output, offset, maximum);
        }
        int next = nextIndex(result);
        int written = written(result);
        if (next <= first || next > count || written < 0 || written > maximum
                || written > next - first || (next < count && written != maximum)) {
            throw new IllegalStateException("invalid masked viewport backend result");
        }
        return result;
    }

    /** Upper half: exclusive next input index. */
    public static int nextIndex(long packed) {
        return (int) (packed >>> 32);
    }

    /** Lower half: count written into the requested output slice. */
    public static int written(long packed) {
        return (int) packed;
    }

    /** Package-private reference kernel, only called after public geometry validation. */
    static long scanJava(long[] ids, short[] flags, int count, int first,
            short required, short excluded, long[] output, int offset, int maximum) {
        int next = first;
        int matches = 0;
        // Validate the full visited range before publishing the first output byte.
        while (next < count && matches < maximum) {
            if (ids[next] <= 0L) {
                throw new IllegalArgumentException("invalid logical ID at " + next);
            }
            if (selected(flags[next], required, excluded)) matches++;
            next++;
        }
        // No selected IDs means the validated prefix needs no second scan.
        if (matches == 0) return pack(next, 0);
        int copied = 0;
        for (int i = first; i < next; i++) {
            if (selected(flags[i], required, excluded)) {
                output[offset + copied++] = ids[i];
            }
        }
        return pack(next, matches);
    }

    private static boolean selected(short flags, short required, short excluded) {
        return (flags & required) == required && (flags & excluded) == 0;
    }

    private static long pack(int next, int written) {
        return ((long) next << 32) | (written & 0xffff_ffffL);
    }

    private static boolean loadNative() {
        try {
            System.loadLibrary("m3_doop_mask_window");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException unavailable) {
            return false;
        }
    }

    private static native long scanNative(long[] ids, short[] flags, int count, int first,
            short required, short excluded, long[] output, int offset, int maximum);
}
