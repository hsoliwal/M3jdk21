/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Preserve all 64 primitive map lane pairs across growth and compaction
 * @modules jdk.m3.collections
 * @run main PrimitivePrimitiveMapResizeTest
 */

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.PrimitivePrimitiveHashMap;

/** Independent ordinary-map oracle; iteration order is also retained in a trace. */
public class PrimitivePrimitiveMapResizeTest {
    private static final long ABSENT = 0x3456_789a_bcde_f012L;
    private static long checks;
    private static final ByteBuffer TAPE_WORD = ByteBuffer.allocate(Long.BYTES);

    public static void main(String[] args) throws Exception {
        MessageDigest trace = MessageDigest.getInstance("SHA-256");
        for (PrimitiveKind keys : PrimitiveKind.values()) {
            for (PrimitiveKind values : PrimitiveKind.values()) {
                for (float load : new float[] {0.125f, 0.65f, 0.9f}) {
                    exercise(keys, values, load, trace);
                }
            }
        }
        System.out.println("pairs=64 loadFactors=3 checks=" + checks
                + " trace=" + HexFormat.of().formatHex(trace.digest()));
    }

    private static void exercise(PrimitiveKind keys, PrimitiveKind values,
            float load, MessageDigest trace) {
        PrimitivePrimitiveHashMap map = new PrimitivePrimitiveHashMap(keys, values, 0, load);
        Map<Long, Long> oracle = new HashMap<>();
        Random random = new Random(0x5eeda11L + keys.ordinal() * 83L + values.ordinal());
        long[] special = {0, 1, -1, 2, -2, Long.MIN_VALUE, Long.MAX_VALUE,
                0x8000_0000L, 0x7f80_0000L, 0xff80_0000L, 0x7fc0_0001L,
                0x7fc1_2345L, 0x7ff0_0000_0000_0000L, 0xfff0_0000_0000_0000L,
                0x7ff8_0000_0000_0001L, 0x7ff8_1234_5678_9abcL};
        word(trace, keys.ordinal());
        word(trace, values.ordinal());
        word(trace, Float.floatToRawIntBits(load));
        for (int pass = 0; pass < 4; pass++) {
            for (int i = 0; i < 320; i++) {
                long key = i < special.length ? special[i] : random.nextLong();
                long value = i < special.length ? special[special.length - i - 1] : random.nextLong();
                long normalizedKey = normalized(keys, key);
                Long previous = oracle.put(normalizedKey, stored(values, value));
                equal(map.putBits(key, value, ABSENT), previous == null ? ABSENT : previous,
                        "put return");
                equal(map.getOrDefaultBits(key, ABSENT), stored(values, value), "get raw value");
                if ((i & 31) == 0) verify(map, oracle, keys, values, trace);
            }
            verify(map, oracle, keys, values, trace);
            map.compact();
            verify(map, oracle, keys, values, trace);
            Long[] removalKeys = oracle.keySet().toArray(Long[]::new);
            for (int i = 0; i < removalKeys.length; i += 2) {
                long key = removalKeys[i];
                equal(map.removeOrDefaultBits(key, ABSENT), oracle.remove(key), "remove return");
            }
            map.compact();
            verify(map, oracle, keys, values, trace);
            for (long key : special) {
                long carrier = key ^ 0x7ff8_0000_1234_5678L;
                long normalizedKey = normalized(keys, key);
                Long old = oracle.put(normalizedKey, stored(values, carrier));
                equal(map.putBits(key, carrier, ABSENT), old == null ? ABSENT : old, "reinsert");
            }
            verify(map, oracle, keys, values, trace);
            map.clear();
            oracle.clear();
            verify(map, oracle, keys, values, trace);
            map.compact();
            verify(map, oracle, keys, values, trace);
        }
    }

    private static void verify(PrimitivePrimitiveHashMap map, Map<Long, Long> oracle,
            PrimitiveKind keys, PrimitiveKind values, MessageDigest trace) {
        equal(map.size(), oracle.size(), "size");
        equal(map.isEmpty() ? 1 : 0, oracle.isEmpty() ? 1 : 0, "empty");
        equal(map.payloadBytes(), (long) map.capacity()
                * (keys.widthBytes() + values.widthBytes()), "payload");
        Set<Long> seen = new HashSet<>();
        word(trace, map.capacity());
        word(trace, map.size());
        map.forEach((key, value) -> {
            long normalizedKey = normalized(keys, key);
            if (!seen.add(normalizedKey)) throw new AssertionError("duplicate key");
            Long wanted = oracle.get(normalizedKey);
            if (wanted == null) throw new AssertionError("unknown key");
            equal(value, wanted, "iteration raw value");
            word(trace, key);
            word(trace, value);
        });
        if (!seen.equals(oracle.keySet())) throw new AssertionError("iteration membership");
        for (Map.Entry<Long, Long> entry : oracle.entrySet()) {
            if (!map.containsKeyBits(entry.getKey())) throw new AssertionError("contains key");
            equal(map.getOrDefaultBits(entry.getKey(), ABSENT), entry.getValue(), "oracle get");
        }
        checks++;
    }

    private static long normalized(PrimitiveKind kind, long bits) {
        return switch (kind) {
            case BOOLEAN -> bits == 0 ? 0 : 1;
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG -> bits;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToIntBits(Float.intBitsToFloat((int) bits)));
            case DOUBLE -> Double.doubleToLongBits(Double.longBitsToDouble(bits));
        };
    }

    private static long stored(PrimitiveKind kind, long bits) {
        return switch (kind) {
            case BOOLEAN -> bits == 0 ? 0 : 1;
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG, DOUBLE -> bits;
            case FLOAT -> Integer.toUnsignedLong((int) bits);
        };
    }

    private static void word(MessageDigest trace, long value) {
        TAPE_WORD.clear();
        TAPE_WORD.putLong(value);
        trace.update(TAPE_WORD.array());
    }

    private static void equal(long actual, long expected, String context) {
        checks++;
        if (actual != expected) throw new AssertionError(context + ": " + actual + " != " + expected);
    }
}
