/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Integer-only vector operations. Results are numerical evidence, never authority grants.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/quantized/QuantizedOps.java}
 * blob {@code e9b697664d3e9fbe0f0fb7fcd55bc549418e2e12} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedOps {
    private QuantizedOps() { }

    public static long dot(QuantizedSchema schema, QuantizedContextValues left, QuantizedContextValues right) {
        pair(schema, left, right);
        long sum = 0L;
        for (int i = 0; i < left.size(); i++) sum += (long) left.strengthU8(i) * right.strengthU8(i);
        return sum;
    }

    public static long l1(QuantizedSchema schema, QuantizedContextValues left, QuantizedContextValues right) {
        pair(schema, left, right);
        long sum = 0L;
        for (int i = 0; i < left.size(); i++) sum += Math.abs(left.strengthU8(i) - right.strengthU8(i));
        return sum;
    }

    public static long squaredDistance(
            QuantizedSchema schema, QuantizedContextValues left, QuantizedContextValues right) {
        pair(schema, left, right);
        long sum = 0L;
        for (int i = 0; i < left.size(); i++) {
            long delta = left.strengthU8(i) - right.strengthU8(i);
            sum += delta * delta;
        }
        return sum;
    }

    /** Alpha 0 selects left strengths; 255 selects right. Flags always remain exactly left's flags. */
    public static QuantizedContextValues blend(
            QuantizedSchema schema, QuantizedContextValues left, QuantizedContextValues right, int alpha) {
        pair(schema, left, right);
        u8(alpha);
        if (alpha == 0) return left;
        byte[] out = new byte[left.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ((left.strengthU8(i) * (255 - alpha) + right.strengthU8(i) * alpha + 127) / 255);
        }
        return QuantizedContextValues.copyOf(left.flags(), out);
    }

    /** O(N + 256 + K) stable selection, descending strength then ascending slot. No boxed candidates. */
    public static int[] topK(QuantizedSchema schema, QuantizedContextValues values, int count) {
        Objects.requireNonNull(schema, "schema").validate(values);
        if (count < 0 || count > values.size()) throw new IllegalArgumentException("top-K bound");
        int[] result = new int[count];
        if (count == 0) return result;
        int[] buckets = new int[256];
        for (int i = 0; i < values.size(); i++) buckets[values.strengthU8(i)]++;
        int start = 0;
        for (int level = 255; level >= 0; level--) {
            int size = buckets[level];
            buckets[level] = start;
            start += size;
        }
        for (int i = 0; i < values.size(); i++) {
            int position = buckets[values.strengthU8(i)]++;
            if (position < count) result[position] = i;
        }
        return result;
    }

    /** Query result bitset: bit i means strength i >= threshold, with zeroed unused tail bits. */
    public static long[] atLeast(QuantizedSchema schema, QuantizedContextValues values, int threshold) {
        Objects.requireNonNull(schema, "schema").validate(values);
        u8(threshold);
        long[] result = new long[(values.size() + 63) >>> 6];
        for (int i = 0; i < values.size(); i++) {
            if (values.strengthU8(i) >= threshold) result[i >>> 6] |= 1L << (i & 63);
        }
        return result;
    }

    private static void pair(QuantizedSchema schema, QuantizedContextValues left, QuantizedContextValues right) {
        Objects.requireNonNull(schema, "schema").validate(left);
        schema.validate(right);
    }

    private static void u8(int value) {
        if (value < 0 || value > 255) throw new IllegalArgumentException("U8 range");
    }
}
