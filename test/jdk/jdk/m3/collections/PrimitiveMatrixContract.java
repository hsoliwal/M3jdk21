/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Independent primitive-carrier, callback and array-ownership regression oracle. */
public final class PrimitiveMatrixContract {
    private static long checks;
    private static long fingerprint = 0x6415b7a0L;
    private static final long ABSENT = 0x123456789abcdef0L;

    private PrimitiveMatrixContract() { }

    public static void main(String[] args) {
        for (PrimitiveKind key : PrimitiveKind.values()) {
            for (PrimitiveKind value : PrimitiveKind.values()) {
                trace(key, value);
                callbacks(key, value);
                tinyLoad(key, value);
            }
        }
        System.out.println("MATRIX_CONTRACT_PASS pairs=64 checks=" + checks
                + " fingerprint=" + Long.toUnsignedString(fingerprint, 16));
    }

    private static void trace(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        PrimitivePrimitiveHashMap map = new PrimitivePrimitiveHashMap(keyKind, valueKind, 0, 0.65f);
        TreeMap<Long, Long> oracle = new TreeMap<>();
        Random random = new Random(0x151164L + keyKind.ordinal() * 31L + valueKind.ordinal());
        for (int step = 0; step < 4_096; step++) {
            long key = carrier(random, step);
            long value = carrier(random, step + 3);
            long normalized = normalize(keyKind, key);
            boolean present = oracle.containsKey(normalized);
            long previous = oracle.getOrDefault(normalized, ABSENT);
            long stored = storage(valueKind, value);
            int[] calls = {0};
            switch (step % 14) {
                case 0 -> {
                    equal(previous, map.putBits(key, value, ABSENT));
                    oracle.put(normalized, stored);
                }
                case 1 -> {
                    equal(previous, map.putIfAbsentBits(key, value, ABSENT));
                    if (!present) oracle.put(normalized, stored);
                }
                case 2 -> {
                    equal(previous, map.removeOrDefaultBits(key, ABSENT));
                    oracle.remove(normalized);
                }
                case 3 -> {
                    equal(previous, map.replaceOrDefaultBits(key, value, ABSENT));
                    if (present) oracle.put(normalized, stored);
                }
                case 4 -> {
                    long expected = step % 28 == 4 ? previous : value;
                    boolean match = present && normalize(valueKind, previous) == normalize(valueKind, expected);
                    truth(match == map.replaceBits(key, expected, value));
                    if (match) oracle.put(normalized, stored);
                }
                case 5 -> {
                    long expected = step % 28 == 5 ? previous : value;
                    boolean match = present && normalize(valueKind, previous) == normalize(valueKind, expected);
                    truth(match == map.removeBits(key, expected));
                    if (match) oracle.remove(normalized);
                }
                case 6 -> {
                    equal(present ? previous : value, map.computeIfAbsentBits(key, raw -> {
                        equal(key, raw); calls[0]++; return value;
                    }));
                    equal(present ? 0 : 1, calls[0]);
                    if (!present) oracle.put(normalized, stored);
                }
                case 7 -> {
                    long next = previous ^ value;
                    equal(present ? next : ABSENT, map.computeIfPresentBits(key, ABSENT, (raw, old) -> {
                        equal(key, raw); equal(previous, old); calls[0]++; return next;
                    }));
                    equal(present ? 1 : 0, calls[0]);
                    if (present) oracle.put(normalized, storage(valueKind, next));
                }
                case 8 -> {
                    long next = previous + value;
                    equal(next, map.computeBits(key, ABSENT, (raw, old) -> {
                        equal(key, raw); equal(previous, old); calls[0]++; return next;
                    }));
                    equal(1, calls[0]);
                    oracle.put(normalized, storage(valueKind, next));
                }
                case 9 -> {
                    long next = present ? previous + value : value;
                    equal(next, map.mergeBits(key, value, (old, raw) -> {
                        equal(previous, old); equal(value, raw); calls[0]++; return old + raw;
                    }));
                    equal(present ? 1 : 0, calls[0]);
                    oracle.put(normalized, storage(valueKind, next));
                }
                case 10 -> equal(previous, map.getOrDefaultBits(key, ABSENT));
                case 11 -> {
                    boolean expected = false;
                    for (long old : oracle.values()) {
                        if (normalize(valueKind, old) == normalize(valueKind, value)) expected = true;
                    }
                    truth(expected == map.containsValueBits(value));
                }
                case 12 -> map.compact();
                default -> {
                    if (step % 127 == 13) { map.clear(); oracle.clear(); }
                }
            }
            equal(oracle.size(), map.size());
            truth(oracle.containsKey(normalized) == map.containsKeyBits(key));
            equal(oracle.getOrDefault(normalized, ABSENT), map.getOrDefaultBits(key, ABSENT));
            equal((long) map.capacity() * (width(keyKind) + width(valueKind)), map.payloadBytes());
            fingerprint = Long.rotateLeft(fingerprint, 7) ^ map.size() ^ map.capacity();
            if ((step & 63) == 0) contents(map, oracle, keyKind);
        }
        contents(map, oracle, keyKind);
        truth(map.keyArray().getClass().isArray());
        truth(map.valueArray().getClass().isArray());
        truth(map.keyArray().getClass().getComponentType().isPrimitive());
        truth(map.valueArray().getClass().getComponentType().isPrimitive());
        PrimitivePrimitiveHashMap.EntryConsumer callback = (k, v) -> {
            fingerprint = Long.rotateLeft(fingerprint, 11) ^ k ^ v;
        };
        map.forEach(callback);
    }

    private static void callbacks(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        PrimitivePrimitiveHashMap map = new PrimitivePrimitiveHashMap(keyKind, valueKind, 0, 0.65f);
        long key = 1L;
        map.putBits(key, 3, ABSENT);
        equal(513, map.computeIfPresentBits(key, ABSENT, (k, old) -> {
            map.removeOrDefaultBits(key, ABSENT);
            return 513;
        }));
        truth(!map.containsKeyBits(key));
        equal(513, map.computeIfAbsentBits(key, k -> {
            map.putBits(key, 7, ABSENT);
            for (long next = 2; next < 50; next++) map.putBits(next, next, ABSENT);
            return 513;
        }));
        equal(storage(valueKind, 513), map.getOrDefaultBits(key, ABSENT));
        equal(-129, map.mergeBits(key, 2, (old, v) -> {
            map.clear(); return -129;
        }));
        truth(map.isEmpty());
        map.putBits(0, 7, ABSENT);
        equal(9, map.computeIfPresentBits(0, ABSENT, (k, old) -> {
            map.clear(); map.putBits(0, 8, ABSENT); return 9;
        }));
        equal(storage(valueKind, 9), map.getOrDefaultBits(0, ABSENT));
        expectNull(() -> map.computeIfAbsentBits(0, null));
        expectNull(() -> map.computeIfPresentBits(0, ABSENT, null));
        expectNull(() -> map.mergeBits(0, 1, null));
        map.clear();
        try {
            map.computeIfAbsentBits(key, k -> {
                map.putBits(0, 6, ABSENT);
                throw new Marker();
            });
            throw new AssertionError("callback exception missing");
        } catch (Marker expected) {
            equal(1, map.size());
            equal(storage(valueKind, 6), map.getOrDefaultBits(0, ABSENT));
            truth(!map.containsKeyBits(key));
        }
    }

    private static void tinyLoad(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        PrimitivePrimitiveHashMap map = new PrimitivePrimitiveHashMap(keyKind, valueKind, 0, 0.01f);
        map.putBits(1, 11, ABSENT);
        map.putBits(0, 22, ABSENT);
        equal(16, map.capacity());
        for (int i = 0; i < 10; i++) {
            equal(storage(valueKind, 22), map.putIfAbsentBits(0, 33, ABSENT));
            equal(storage(valueKind, 11), map.computeIfAbsentBits(1, k -> { throw new AssertionError(); }));
            equal(16, map.capacity());
        }
    }

    private static void contents(PrimitivePrimitiveHashMap map, TreeMap<Long, Long> oracle, PrimitiveKind keyKind) {
        TreeMap<Long, Long> seen = new TreeMap<>();
        map.forEach((k, v) -> truth(seen.put(normalize(keyKind, k), v) == null));
        truth(oracle.equals(seen));
        for (Map.Entry<Long, Long> entry : seen.entrySet()) {
            fingerprint = Long.rotateLeft(fingerprint, 3) ^ entry.getKey() ^ entry.getValue();
        }
    }

    private static long carrier(Random random, int index) {
        return switch (index % 17) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1L;
            case 4 -> 0x7ff8_0000_0000_1234L;
            case 5 -> 0x7fff_ffff_ffff_ffffL;
            case 6 -> 0x7fc0_1234L;
            case 7 -> 0x8000_0000L;
            case 8 -> 0x1_0000_0000L;
            default -> random.nextInt(1_024) - 512L;
        };
    }

    private static long normalize(PrimitiveKind kind, long raw) {
        return switch (kind) {
            case FLOAT -> Integer.toUnsignedLong(Float.floatToIntBits(Float.intBitsToFloat((int) raw)));
            case DOUBLE -> Double.doubleToLongBits(Double.longBitsToDouble(raw));
            default -> storage(kind, raw);
        };
    }

    private static long storage(PrimitiveKind kind, long raw) {
        return switch (kind) {
            case BOOLEAN -> raw == 0 ? 0 : 1;
            case BYTE -> (byte) raw;
            case SHORT -> (short) raw;
            case CHAR -> (char) raw;
            case INT -> (int) raw;
            case LONG -> raw;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits(Float.intBitsToFloat((int) raw)));
            case DOUBLE -> Double.doubleToRawLongBits(Double.longBitsToDouble(raw));
        };
    }

    private static int width(PrimitiveKind kind) {
        return switch (kind) {
            case BOOLEAN, BYTE -> 1;
            case SHORT, CHAR -> 2;
            case INT, FLOAT -> 4;
            case LONG, DOUBLE -> 8;
        };
    }

    private static void expectNull(Runnable action) {
        try { action.run(); throw new AssertionError("null callback accepted"); }
        catch (NullPointerException expected) { checks++; }
    }

    private static void equal(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError("expected " + expected + " but got " + actual);
    }

    private static void truth(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("contract mismatch");
    }

    private static final class Marker extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
