/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * The deterministic primitive, String and array-length sources of the jdk.m3.graphics atom tests. The first
 * {@link #BOUNDARY_ROUNDS} rounds draw from boundary pools (zeros, signs, extremes, NaN and infinities, empty and
 * non-ASCII strings, array lengths 0..20), rotated by the parameter position; later rounds draw from a
 * SplittableRandom seeded by the member, the round and the position (full bit patterns, small ranges, wide ranges), so
 * every run generates the same values.
 */
final class M3GraphicsAtomsPools {
    static final int BOUNDARY_ROUNDS = 12;
    static final int POSITION_STRIDE = 5;
    private static final long SEED = 20261031L;
    private static final long ROUND_MIX = 0x9E3779B97F4A7C15L;
    private static final long POSITION_MIX = 0xC2B2AE3D27D4EB4FL;
    private static final char BOOLEAN = 'Z';
    private static final char BYTE = 'B';
    private static final char CHAR = 'C';
    private static final char SHORT = 'S';
    private static final char INT = 'I';
    private static final char LONG = 'J';
    private static final char FLOAT = 'F';
    private static final char DOUBLE = 'D';
    private static final int SHAPES = 3;
    private static final int SHAPE_FULL = 0;
    private static final int SHAPE_SMALL = 1;
    private static final int SMALL = 512;
    private static final int BYTE_RANGE = 256;
    private static final double UNIT = 1.0;
    private static final double LARGE = 4096.0;
    private static final int CHAR_BOUND = Character.MAX_VALUE + 1;
    private static final int MAX_RANDOM_LENGTH = 20;
    private static final int MAX_RANDOM_VALUES = 3;
    private static final int MAX_STRING = 16;
    private static final int E_ACUTE = 0xE9;
    private static final int HAN_ZHONG = 0x4E2D;
    private static final int HAN_WEN = 0x6587;
    private static final int GRINNING_FACE = 0x1F600;
    private static final int[][] CODE_POINT_RANGES = {{'a', 'z'}, {'A', 'Z'}, {'0', '9'}, {' ', '/'},
            {0xC0, 0xFF}, {0x4E00, 0x4E80}, {0x1F600, 0x1F610}};
    private static final int[] ARRAY_LENGTHS = {0, 1, 2, 3, 4, 6, 9, 16, 20};
    private static final int[] VALUE_ARRAY_LENGTHS = {0, 1, 3};
    private static final Object[] BOOLEANS = {false, true};
    private static final Object[] BYTES = {(byte) 0, (byte) 1, (byte) -1, Byte.MAX_VALUE, Byte.MIN_VALUE, (byte) 0x55};
    private static final Object[] CHARS = {Character.MIN_VALUE, 'a', 'Z', ' ', Character.MAX_VALUE, (char) E_ACUTE,
            (char) HAN_ZHONG};
    private static final Object[] SHORTS = {(short) 0, (short) 1, (short) -1, Short.MAX_VALUE, Short.MIN_VALUE};
    private static final Object[] INTS = {0, 1, -1, 2, 3, 4, 255, 256, 0xFF000000, 0x80808080, 0x12345678,
            Integer.MAX_VALUE, Integer.MIN_VALUE, 65535, 100};
    private static final Object[] LONGS = {0L, 1L, -1L, 255L, 4096L, Long.MAX_VALUE, Long.MIN_VALUE,
            (long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE, 0x123456789ABCDEFL};
    private static final Object[] FLOATS = {0f, -0f, 1f, -1f, 0.5f, 2f, 255f, 1e-7f, 100.25f, Float.MIN_VALUE,
            Float.MAX_VALUE, -Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.75f};
    private static final Object[] DOUBLES = {0d, -0d, 1d, -1d, 0.5d, 2d, 255d, 1e-300, Double.MIN_VALUE,
            Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.1d};
    private static final String[] STRINGS = {"", "a", "Skia", "kern", "liga=0", "+kern[1:5]=1", "-liga", "wght",
            "Hello, World", Character.toString(E_ACUTE) + "t" + Character.toString(E_ACUTE),
            Character.toString(HAN_ZHONG) + Character.toString(HAN_WEN), Character.toString(GRINNING_FACE), " x ",
            "0", "-1", "UNKNOWN"};
    private static final Map<Character, Object[]> POOLS = Map.of(BOOLEAN, BOOLEANS, BYTE, BYTES, CHAR, CHARS,
            SHORT, SHORTS, INT, INTS, LONG, LONGS, FLOAT, FLOATS, DOUBLE, DOUBLES);

    private M3GraphicsAtomsPools() {}

    /** The seed of a member (or recipe) key: stable across runs (String.hashCode is specified). */
    static long seed(String key) {
        return SEED ^ ((long) key.hashCode() * ROUND_MIX);
    }

    /** The boxed value of a primitive descriptor letter for one round and position. */
    static Object primitive(char kind, long seed, int round, int position) {
        if (round < BOUNDARY_ROUNDS) {
            Object[] pool = POOLS.get(kind);
            return pool[(round + position * POSITION_STRIDE) % pool.length];
        }
        SplittableRandom r = random(seed, round, position);
        return switch (kind) {
            case BOOLEAN -> r.nextBoolean();
            case BYTE -> (byte) r.nextInt();
            case CHAR -> (char) r.nextInt(CHAR_BOUND);
            case SHORT -> (short) r.nextInt();
            case INT -> randomInt(r);
            case LONG -> randomLong(r);
            case FLOAT -> (float) randomDouble(r, true);
            default -> randomDouble(r, false);
        };
    }

    /** A String: the enum constant names first (valueOf), then the boundary pool, then random code points. */
    static String string(List<String> names, long seed, int round, int position) {
        int r = round - names.size();
        if (r < 0) {
            return names.get(round);
        }
        if (r < BOUNDARY_ROUNDS) {
            return STRINGS[(r + position * POSITION_STRIDE) % STRINGS.length];
        }
        SplittableRandom rnd = random(seed, round, position);
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, rnd.nextInt(MAX_STRING + 1)).forEach(k -> sb.appendCodePoint(codePoint(rnd)));
        return sb.toString();
    }

    /** An array length: 0..20 for primitive and String arrays, 0..3 for arrays of values. */
    static int arrayLength(boolean values, long seed, int round, int position) {
        int[] lengths = values ? VALUE_ARRAY_LENGTHS : ARRAY_LENGTHS;
        int bound = values ? MAX_RANDOM_VALUES : MAX_RANDOM_LENGTH;
        return round < BOUNDARY_ROUNDS ? lengths[(round + position) % lengths.length]
                : random(seed, round, position).nextInt(bound + 1);
    }

    private static int randomInt(SplittableRandom r) {
        return switch (r.nextInt(SHAPES)) {
            case SHAPE_FULL -> r.nextInt();
            case SHAPE_SMALL -> r.nextInt(-SMALL, SMALL + 1);
            default -> r.nextInt(BYTE_RANGE);
        };
    }

    private static long randomLong(SplittableRandom r) {
        return switch (r.nextInt(SHAPES)) {
            case SHAPE_FULL -> r.nextLong();
            case SHAPE_SMALL -> r.nextLong(-SMALL, SMALL + 1);
            default -> r.nextInt();
        };
    }

    private static double randomDouble(SplittableRandom r, boolean single) {
        return switch (r.nextInt(SHAPES)) {
            case SHAPE_FULL -> single ? Float.intBitsToFloat(r.nextInt()) : Double.longBitsToDouble(r.nextLong());
            case SHAPE_SMALL -> r.nextDouble(-UNIT, UNIT);
            default -> r.nextDouble(-LARGE, LARGE);
        };
    }

    private static int codePoint(SplittableRandom r) {
        int[] range = CODE_POINT_RANGES[r.nextInt(CODE_POINT_RANGES.length)];
        return r.nextInt(range[0], range[1] + 1);
    }

    private static SplittableRandom random(long seed, int round, int position) {
        return new SplittableRandom(seed + round * ROUND_MIX + position * POSITION_MIX);
    }
}
