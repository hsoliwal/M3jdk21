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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Caller-owned slice conversion for binary16 and bfloat16 primitive storage.
 *
 * <p>{@code bfloat16 == false} selects IEEE binary16; true selects bfloat16.
 * Both formats round to nearest, ties to even, preserve signed zero/infinities,
 * and canonicalize NaNs through {@link M3PrimitiveBits}. Source and destination
 * bounds are validated completely before the first destination write. Concurrent
 * mutation of the participating arrays during conversion is unsupported.
 *
 * <p>The default entry points are Java operations. Native entry points are an
 * explicit alternative and require the caller to load the qualified JNI library.
 * They throw {@link UnsatisfiedLinkError} when linkage is unavailable, including
 * valid zero-length calls; there is no silent fallback or process-global loader.
 * Neither path retains the arrays or allocates an output array.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveFloat16.java}
 * blob {@code 3fcdb56ba5967816a99906361e4c21158e6ec749} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives batch (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveFloat16 {
    private M3PrimitiveFloat16() { }

    /** Encode a float slice into the selected 16-bit format. */
    public static void encode(float[] source, int sourceOffset, short[] target,
            int targetOffset, int length, boolean bfloat16) {
        checkEncode(source, sourceOffset, target, targetOffset, length);
        if (bfloat16) {
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = M3PrimitiveBits.toBFloat16(source[sourceOffset + i]);
            }
        } else {
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = M3PrimitiveBits.toFloat16(source[sourceOffset + i]);
            }
        }
    }

    /** Decode a 16-bit slice into binary32 float values. */
    public static void decode(short[] source, int sourceOffset, float[] target,
            int targetOffset, int length, boolean bfloat16) {
        checkDecode(source, sourceOffset, target, targetOffset, length);
        if (bfloat16) {
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = M3PrimitiveBits.fromBFloat16(source[sourceOffset + i]);
            }
        } else {
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = M3PrimitiveBits.fromFloat16(source[sourceOffset + i]);
            }
        }
    }

    /** Explicit JNI encoding; requires loaded native linkage even for an empty slice. */
    public static void encodeNative(float[] source, int sourceOffset, short[] target,
            int targetOffset, int length, boolean bfloat16) {
        checkEncode(source, sourceOffset, target, targetOffset, length);
        encode0(source, sourceOffset, target, targetOffset, length, bfloat16);
    }

    /** Explicit JNI decoding; requires loaded native linkage even for an empty slice. */
    public static void decodeNative(short[] source, int sourceOffset, float[] target,
            int targetOffset, int length, boolean bfloat16) {
        checkDecode(source, sourceOffset, target, targetOffset, length);
        decode0(source, sourceOffset, target, targetOffset, length, bfloat16);
    }

    private static void checkEncode(float[] source, int sourceOffset, short[] target,
            int targetOffset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceOffset, length, source.length);
        Objects.checkFromIndexSize(targetOffset, length, target.length);
    }

    private static void checkDecode(short[] source, int sourceOffset, float[] target,
            int targetOffset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceOffset, length, source.length);
        Objects.checkFromIndexSize(targetOffset, length, target.length);
    }

    private static native void encode0(float[] source, int sourceOffset, short[] target,
            int targetOffset, int length, boolean bfloat16);

    private static native void decode0(short[] source, int sourceOffset, float[] target,
            int targetOffset, int length, boolean bfloat16);
}
