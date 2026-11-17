// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;

/** Standalone Java21 differential source oracle, runnable against before/after unchanged ABI. */
public final class TokenCursorSingleLaneProof {
    private TokenCursorSingleLaneProof() { }
    private static volatile TokenCursor escape;
    private static volatile long used;

    static void require(boolean check, String message) {
        if (!check) throw new AssertionError(message);
    }

    static long verifySingle(int[] source, long seed) {
        TokenCursor cursor = TokenCursor.of(source);
        require(cursor.totalLength() == source.length, "length " + seed);
        require(cursor.offset() == 0, "initial offset " + seed);
        require(cursor.chunkLength() == source.length, "chunk length " + seed);
        long mask = 0;
        long sum = 0xcbf29ce484222325L;
        for (int i = 0; i < source.length; i++) {
            int value = source[i];
            mask |= 1L << (value & 63);
            cursor.seek(i);
            require(cursor.offset() == 0, "seek offset " + i + " seed=" + seed);
            require(cursor.unitAt(i) == value, "unit " + i + " seed=" + seed);
            sum = (sum ^ value) * 0x100000001b3L;
        }
        cursor.seek(source.length);
        require(cursor.unitPresenceMask() == mask, "presence mask seed=" + seed);
        require(!cursor.advance() && !cursor.backtrack(), "single lane navigation seed=" + seed);
        if (source.length > 0) {
            int original = source[0];
            int replacement = original == 42 ? 43 : 42;
            source[0] = replacement;
            require(cursor.unitAt(0) == replacement, "live shared input contract seed=" + seed);
            source[0] = original;
        }
        sum ^= mask;
        return sum;
    }

    static long verifyInvalidInputs() {
        try {
            TokenCursor.of(null);
            throw new AssertionError("null was accepted");
        } catch (NullPointerException expected) {
            require("tokens".equals(expected.getMessage()), "null exception message");
        }
        try {
            TokenCursor.of(new int[] {1, 2, -7, 3});
            throw new AssertionError("negative ID was accepted");
        } catch (IllegalArgumentException expected) {
            require("negative token id -7".equals(expected.getMessage()), "negative exception message");
        }
        try {
            TokenCursor.lanes(new int[][] {new int[0], null, new int[] {2}});
            throw new AssertionError("null nested lane was accepted");
        } catch (NullPointerException expected) {
            require(expected.getMessage() == null, "nested null exception message");
        }
        TokenCursor many = TokenCursor.lanes(new int[][] {
                {}, {0, 1}, {}, {Integer.MAX_VALUE}, {2, 3, 4}, {}});
        require(many.totalLength() == 6, "multi-lane total");
        require(many.unitAt(0) == 0 && many.unitAt(2) == Integer.MAX_VALUE
                && many.unitAt(5) == 4, "multi-lane values");
        require(many.unitPresenceMask() == (CursorFacts.bit(0) | CursorFacts.bit(1)
                | CursorFacts.bit(Integer.MAX_VALUE) | CursorFacts.bit(2)
                | CursorFacts.bit(3) | CursorFacts.bit(4)), "multi-lane presence");
        many.seek(2);
        require(many.offset() == 2 && many.chunkLength() == 1, "multi-lane boundary");
        many.seek(6);
        require(many.offset() == 3 && many.chunkLength() == 3, "multi-lane EOF seek");
        TokenCursor empty = TokenCursor.of(new int[0]);
        require(empty.totalLength() == 0 && empty.chunkLength() == 0 && empty.offset() == 0,
                "empty special case");
        empty.seek(0);
        require(!empty.advance() && !empty.backtrack(), "empty navigator");
        return 12;
    }

    static long verifyRandom(int rounds) {
        long checksum = verifyInvalidInputs();
        Random random = new Random(0x2026_CAFE_BABEL);
        final int[] alphabet = {0, 1, 2, 7, 63, 64, 65, 127, 128, 1024, 65535,
                65536, 1_000_000, Integer.MAX_VALUE};
        for (int round = 0; round < rounds; round++) {
            int[] row = new int[random.nextInt(97)];
            for (int k = 0; k < row.length; k++) {
                row[k] = alphabet[random.nextInt(alphabet.length)];
            }
            checksum = Long.rotateLeft(checksum, 5) ^ verifySingle(row, round);
            if ((round & 0xff) == 0) {
                // A logically equal lane from a discontiguous source has the same symbolic facts.
                int cut = row.length / 2;
                TokenCursor split = TokenCursor.lanes(new int[][] {
                        {}, Arrays.copyOfRange(row, 0, cut), {},
                        Arrays.copyOfRange(row, cut, row.length), {}});
                TokenCursor single = TokenCursor.of(row);
                require(split.totalLength() == single.totalLength(), "split total " + round);
                require(split.unitPresenceMask() == single.unitPresenceMask(), "split presence " + round);
                for (int j = 0; j < row.length; j++) {
                    require(split.unitAt(j) == single.unitAt(j), "split token " + round + ":" + j);
                }
            }
        }
        System.out.println("M3_TOKEN_CURSOR_SINGLE_LANE_DIFFERENTIAL_PASS rounds=" + rounds
                + " checksum=" + Long.toUnsignedString(checksum));
        return checksum;
    }

    static long allocatedBytes(int rounds, int[] tokens) {
        var m = ManagementFactory.getThreadMXBean();
        if (!(m instanceof ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation counter unavailable");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long threadId = Thread.currentThread().threadId();
        long start = bean.getThreadAllocatedBytes(threadId);
        long rolling = 0;
        for (int i = 0; i < rounds; i++) {
            TokenCursor cursor = TokenCursor.of(tokens);
            rolling += cursor.totalLength();
            escape = cursor;
        }
        used = rolling;
        return bean.getThreadAllocatedBytes(threadId) - start;
    }

    static void benchmark() {
        int[] tokens = new int[] {7, 9, 63, 64, 65, 1024};
        for (int i = 0; i < 5; i++) allocatedBytes(150_000, tokens);
        long[] measurements = new long[7];
        final int iterations = 150_000;
        for (int i = 0; i < measurements.length; i++) {
            measurements[i] = allocatedBytes(iterations, tokens) / iterations;
        }
        Arrays.sort(measurements);
        System.out.println("M3_TOKEN_CURSOR_SINGLE_LANE_ALLOC_BYTES_PER_OF median=" + measurements[3]
                + " samples=" + Arrays.toString(measurements));
    }

    public static void main(String[] args) {
        verifyRandom(120_000);
        benchmark();
    }
}
