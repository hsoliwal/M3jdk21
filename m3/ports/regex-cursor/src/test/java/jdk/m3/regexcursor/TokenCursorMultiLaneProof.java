// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Differential against an independent flattened-array oracle; one process per before/after. */
public final class TokenCursorMultiLaneProof {
    private TokenCursorMultiLaneProof() { }

    static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void eq(long expected, long actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
    }

    static void refuseNull(Runnable callable) {
        try { callable.run(); }
        catch (NullPointerException expected) { return; }
        throw new AssertionError("null admitted or wrong exception");
    }

    static void refuseNegative(Runnable callable) {
        try { callable.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("negative token admitted or wrong exception");
    }

    static long inspect(int[][] lanes, String label) {
        int nonempty = 0, total = 0;
        long presence = 0;
        ArrayList<int[]> populated = new ArrayList<>();
        for (int[] lane : lanes) {
            require(lane != null, "oracle null inner: " + label);
            if (lane.length == 0) continue;
            nonempty++;
            populated.add(lane);
            total = Math.addExact(total, lane.length);
            for (int id : lane) {
                require(id >= 0, "oracle negative token " + label);
                presence |= CursorFacts.bit(id);
            }
        }
        TokenCursor cursor = TokenCursor.lanes(lanes);
        eq(total, cursor.totalLength(), label + " total");
        eq(presence, cursor.unitPresenceMask(), label + " presence");
        long digest = total * 0x9e3779b97f4a7c15L + presence;
        if (nonempty == 0) {
            eq(0, cursor.chunkLength(), label + " empty length");
            eq(0, cursor.offset(), label + " empty offset");
            cursor.seek(0);
            require(!cursor.advance() && !cursor.backtrack(), label + " empty step");
            return digest ^ 0x12345L;
        }
        int[] offsets = new int[nonempty + 1];
        for (int i = 0; i < nonempty; i++) offsets[i + 1] = offsets[i] + populated.get(i).length;
        cursor.seek(0);
        int logical = 0;
        for (int k = 0; k < nonempty; k++) {
            int[] expected = populated.get(k);
            eq(offsets[k], cursor.offset(), label + " forward offset " + k);
            eq(expected.length, cursor.chunkLength(), label + " forward length " + k);
            for (int value : expected) {
                eq(value, cursor.unit(logical - offsets[k]), label + " unit " + logical);
                digest = Long.rotateLeft(digest ^ value ^ logical, 9) * 0x9e3779b97f4a7c15L;
                logical++;
            }
            require(cursor.advance() == (k != nonempty - 1), label + " forward advance " + k);
        }
        for (int k = nonempty - 1; k >= 0; k--) {
            eq(offsets[k], cursor.offset(), label + " backward offset " + k);
            require(cursor.backtrack() == (k != 0), label + " backward backtrack " + k);
        }
        for (int k = 0; k < nonempty; k++) {
            cursor.seek(offsets[k]);
            eq(offsets[k], cursor.offset(), label + " exact seek " + k);
            cursor.seek(offsets[k + 1] - 1);
            eq(offsets[k], cursor.offset(), label + " last seek " + k);
        }
        cursor.seek(total);
        eq(offsets[nonempty - 1], cursor.offset(), label + " seek total last lane");
        int[] flattened = new int[total];
        logical = 0;
        for (int[] item : populated) for (int value : item) flattened[logical++] = value;
        for (int i = 0; i < flattened.length; i++) {
            eq(flattened[i], cursor.unitAt(i), label + " absolute unit " + i);
        }
        // An outer-array replacement after construction must not redirect the cursor's lane.
        if (lanes.length > 0) {
            int first = -1;
            for (int i = 0; i < lanes.length; i++) if (lanes[i].length > 0) { first = i; break; }
            if (first >= 0) {
                int[] old = lanes[first];
                lanes[first] = new int[] {42};
                cursor.seek(0);
                eq(old[0], cursor.unit(0), label + " outer alias independence");
                lanes[first] = old;
            }
        }
        return digest;
    }

    public static long verify(int samples) {
        refuseNull(() -> TokenCursor.lanes(null));
        refuseNull(() -> TokenCursor.lanes(new int[][] {new int[0], null, new int[] {7}}));
        refuseNull(() -> TokenCursor.lanes(new int[][] {new int[] {-2}, null}));
        refuseNegative(() -> TokenCursor.lanes(new int[][] {new int[0], new int[] {3, -1}}));
        eq(0, TokenCursor.lanes(new int[][] {}).totalLength(), "zero lanes");
        eq(0, TokenCursor.lanes(new int[][] {new int[0], new int[0]}).totalLength(), "all empty");
        long checksum = inspect(new int[][] {{1, 2}, {}, {3, 4}, {}, {0, Integer.MAX_VALUE}}, "fixed");
        checksum ^= inspect(new int[][] {{0}}, "zero token allowed");
        Random random = new Random(0xC0FFEE2026L);
        int negative = 0, nulls = 0, valid = 0;
        for (int attempt = 0; attempt < samples; attempt++) {
            int count = random.nextInt(18);
            int[][] parts = new int[count][];
            boolean hasNull = false;
            boolean hasNegative = false;
            for (int k = 0; k < count; k++) {
                int category = random.nextInt(100);
                if (category == 0) { parts[k] = null; hasNull = true; continue; }
                int length = category < 32 ? 0 : random.nextInt(22);
                parts[k] = new int[length];
                for (int j = 0; j < length; j++) {
                    int kind = random.nextInt(28);
                    int id = kind == 0 ? -random.nextInt(100) - 1
                            : kind == 1 ? Integer.MAX_VALUE
                            : kind == 2 ? 0
                            : random.nextInt(1500);
                    parts[k][j] = id;
                    if (id < 0) hasNegative = true;
                }
            }
            if (hasNull) { refuseNull(() -> TokenCursor.lanes(parts)); nulls++; }
            else if (hasNegative) { refuseNegative(() -> TokenCursor.lanes(parts)); negative++; }
            else { checksum = Long.rotateLeft(checksum, 7) ^ inspect(parts, "random-" + attempt); valid++; }
        }
        System.out.printf("M3_MULTILANE_PARITY_PASS samples=%d valid=%d null=%d negative=%d checksum=%016x%n",
                samples, valid, nulls, negative, checksum);
        return checksum;
    }

    public static void main(String[] arguments) {
        int n = arguments.length == 0 ? 120_000 : Integer.parseInt(arguments[0]);
        verify(n);
    }
}
