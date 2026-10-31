// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Exact deterministic schema field compatibility over sorted numeric field IDs.
 * Not a JSON Schema, XSD, Protobuf, Avro, or OpenAPI language implementation.
 */
public final class M3SchemaCompatibility {
    private M3SchemaCompatibility() { }
    public static final byte INT32 = 1, INT64 = 2, FLOAT32 = 3, FLOAT64 = 4, TEXT = 5, BYTES = 6;

    /** Flags incompatible *reader* field indices. Missing optional fields are allowed.
     * Required reader fields must exist in writer; widening INT32->INT64 and
     * FLOAT32->FLOAT64 is allowed, with no narrowing or cross-type coercion.
     */
    public static long[] incompatible(int[] writerIds, byte[] writerTypes,
                                      int[] readerIds, byte[] readerTypes, long[] readerRequired) {
        validate(writerIds, writerTypes); validate(readerIds, readerTypes);
        Objects.requireNonNull(readerRequired);
        if (readerRequired.length != ((long) readerIds.length + 63) / 64)
            throw new IllegalArgumentException("required bitmask length");
        if ((readerIds.length & 63) != 0 && readerRequired.length > 0
                && (readerRequired[readerRequired.length - 1] >>> (readerIds.length & 63)) != 0)
            throw new IllegalArgumentException("required mask domain");
        long[] conflicts = new long[readerRequired.length];
        int writer = 0;
        for (int read = 0; read < readerIds.length; read++) {
            int id = readerIds[read];
            while (writer < writerIds.length && writerIds[writer] < id) writer++;
            boolean present = writer < writerIds.length && writerIds[writer] == id;
            boolean required = (readerRequired[read >>> 6] & (1L << (read & 63))) != 0;
            if ((!present && required) || (present && !canRead(writerTypes[writer], readerTypes[read])))
                conflicts[read >>> 6] |= 1L << (read & 63);
        }
        return conflicts;
    }

    private static boolean canRead(byte written, byte expected) {
        return written == expected || (written == INT32 && expected == INT64)
                || (written == FLOAT32 && expected == FLOAT64);
    }

    private static void validate(int[] ids, byte[] types) {
        Objects.requireNonNull(ids); Objects.requireNonNull(types);
        if (ids.length != types.length || ids.length > 1_000_000) throw new IllegalArgumentException("schema geometry");
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] < 0 || (i > 0 && ids[i - 1] >= ids[i]))
                throw new IllegalArgumentException("sorted distinct schema fields");
            if (types[i] < INT32 || types[i] > BYTES)
                throw new IllegalArgumentException("unknown schema type");
        }
    }
}
