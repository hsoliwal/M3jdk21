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
package jdk.m3.collections;

import java.util.Objects;

/**
 * Universal primitive map facade that chooses a storage representation from
 * primitive key-domain cardinality.
 *
 * <p>BOOLEAN is direct, BYTE/SHORT/CHAR are adaptive sparse-to-direct maps, and
 * INT/LONG/FLOAT/DOUBLE use the exact-width open-addressed hash map. Specialized
 * classes remain available when zero dispatch is more important than convenience.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveAdaptiveMap.java}
 * blob {@code c5c8ac0d02568434102c85b59e06f21bfed79c02} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveAdaptiveMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final Object backend;

    public M3PrimitiveAdaptiveMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 16);
    }

    public M3PrimitiveAdaptiveMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int expectedSize) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize < 0");
        backend = switch (keyKind) {
            case BOOLEAN -> {
                boundedExpected(expectedSize, 2);
                yield new M3PrimitiveDirectBooleanMap(valueKind);
            }
            case BYTE -> new M3PrimitiveAdaptiveByteMap(valueKind, boundedExpected(expectedSize, 1 << Byte.SIZE));
            case SHORT -> new M3PrimitiveAdaptiveShortMap(valueKind, boundedExpected(expectedSize, 1 << Short.SIZE));
            case CHAR -> new M3PrimitiveAdaptiveCharMap(valueKind, boundedExpected(expectedSize, 1 << Character.SIZE));
            case INT, LONG, FLOAT, DOUBLE ->
                    new M3PrimitivePrimitiveHashMap(keyKind, valueKind, expectedSize, M3DenseHash.DEFAULT_LOAD_FACTOR);
        };
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }

    public int size() {
        return switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend).size();
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).size();
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).size();
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).size();
            case INT, LONG, FLOAT, DOUBLE -> ((M3PrimitivePrimitiveHashMap) backend).size();
        };
    }

    public boolean isEmpty() { return size() == 0; }

    public long payloadBytes() {
        return switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend).payloadBytes();
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).payloadBytes();
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).payloadBytes();
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).payloadBytes();
            case INT, LONG, FLOAT, DOUBLE -> ((M3PrimitivePrimitiveHashMap) backend).payloadBytes();
        };
    }

    public boolean isDirect() {
        return switch (keyKind) {
            case BOOLEAN -> true;
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).isDirect();
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).isDirect();
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).isDirect();
            case INT, LONG, FLOAT, DOUBLE -> false;
        };
    }

    public boolean containsKeyBits(long keyBits) {
        return switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend).containsKey(keyBits != 0L);
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).containsKey((byte) keyBits);
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).containsKey((short) keyBits);
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).containsKey((char) keyBits);
            case INT, LONG, FLOAT, DOUBLE ->
                    ((M3PrimitivePrimitiveHashMap) backend).containsKeyBits(keyBits);
        };
    }

    public long getOrDefaultBits(long keyBits, long defaultBits) {
        return switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend)
                    .getOrDefaultBits(keyBits != 0L, defaultBits);
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend)
                    .getOrDefaultBits((byte) keyBits, defaultBits);
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend)
                    .getOrDefaultBits((short) keyBits, defaultBits);
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend)
                    .getOrDefaultBits((char) keyBits, defaultBits);
            case INT, LONG, FLOAT, DOUBLE ->
                    ((M3PrimitivePrimitiveHashMap) backend).getOrDefaultBits(keyBits, defaultBits);
        };
    }

    public long putBits(long keyBits, long valueBits, long absentBits) {
        return switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend)
                    .putBits(keyBits != 0L, valueBits, absentBits);
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend)
                    .putBits((byte) keyBits, valueBits, absentBits);
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend)
                    .putBits((short) keyBits, valueBits, absentBits);
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend)
                    .putBits((char) keyBits, valueBits, absentBits);
            case INT, LONG, FLOAT, DOUBLE ->
                    ((M3PrimitivePrimitiveHashMap) backend).putBits(keyBits, valueBits, absentBits);
        };
    }

    public long removeOrDefaultBits(long keyBits, long defaultBits) {
        return switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend)
                    .removeOrDefaultBits(keyBits != 0L, defaultBits);
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend)
                    .removeOrDefaultBits((byte) keyBits, defaultBits);
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend)
                    .removeOrDefaultBits((short) keyBits, defaultBits);
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend)
                    .removeOrDefaultBits((char) keyBits, defaultBits);
            case INT, LONG, FLOAT, DOUBLE ->
                    ((M3PrimitivePrimitiveHashMap) backend).removeOrDefaultBits(keyBits, defaultBits);
        };
    }

    public void clear() {
        switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend).clear();
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).clear();
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).clear();
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).clear();
            case INT, LONG, FLOAT, DOUBLE -> ((M3PrimitivePrimitiveHashMap) backend).clear();
        }
    }

    public void compact() {
        switch (keyKind) {
            case BOOLEAN -> { }
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).compact();
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).compact();
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).compact();
            case INT, LONG, FLOAT, DOUBLE -> ((M3PrimitivePrimitiveHashMap) backend).compact();
        }
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        switch (keyKind) {
            case BOOLEAN -> ((M3PrimitiveDirectBooleanMap) backend).forEach(
                    (key, valueBits) -> consumer.accept(key ? 1L : 0L, valueBits));
            case BYTE -> ((M3PrimitiveAdaptiveByteMap) backend).forEach(
                    (key, valueBits) -> consumer.accept(key, valueBits));
            case SHORT -> ((M3PrimitiveAdaptiveShortMap) backend).forEach(
                    (key, valueBits) -> consumer.accept(key, valueBits));
            case CHAR -> ((M3PrimitiveAdaptiveCharMap) backend).forEach(
                    (key, valueBits) -> consumer.accept(key, valueBits));
            case INT, LONG, FLOAT, DOUBLE ->
                    ((M3PrimitivePrimitiveHashMap) backend).forEach(consumer::accept);
        }
    }

    Object backend() { return backend; }

    private static int boundedExpected(int expected, int domainSize) {
        if (expected > domainSize) {
            throw new IllegalArgumentException("expectedSize exceeds key domain " + domainSize);
        }
        return expected;
    }
}
