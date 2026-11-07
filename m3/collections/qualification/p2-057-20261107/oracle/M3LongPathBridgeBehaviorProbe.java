/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Supplier;

/** Identical baseline/successor tape for validation precedence, diagnostics and window effects. */
public final class M3LongPathBridgeBehaviorProbe {
    private static final MessageDigest TAPE = digest();
    private static int checks;
    private static boolean trace;
    private M3LongPathBridgeBehaviorProbe() { }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void observe(String name, Supplier<?> operation, long[] output) {
        String result;
        try {
            Object value = operation.get();
            result = value instanceof long[] array ? Arrays.toString(array) : String.valueOf(value);
        } catch (RuntimeException failure) {
            result = failure.getClass().getName() + ":" + failure.getMessage();
        }
        String event = name + "=" + result + ";output=" + Arrays.toString(output) + "\n";
        TAPE.update(event.getBytes(StandardCharsets.UTF_8));
        checks++;
        if (trace) System.out.print(event);
    }

    private record Input(long[] rows, int[] offsets, int[][] planes, int[] need, int[] exclude) { }

    private static Input input(int fault) {
        long[] rows = {Long.MIN_VALUE, 7, 7, -2, Long.MAX_VALUE};
        int[] offsets = {0, 2, 3, 1, 4};
        int[][] planes = {{1, 2, 3, 3, 1}, {8, 8, 0, 8, 8}};
        int[] need = {1, 8}, exclude = {2, 0};
        switch (fault) {
            case 0 -> { }
            case 1 -> rows = null;
            case 2 -> offsets = null;
            case 3 -> planes = null;
            case 4 -> need = null;
            case 5 -> exclude = null;
            case 6 -> offsets = new int[4];
            case 7 -> planes = new int[0][];
            case 8 -> need = new int[1];
            case 9 -> exclude = new int[1];
            case 10 -> planes[0] = null;
            case 11 -> planes[1] = null;
            case 12 -> offsets[0] = -1;
            case 13 -> offsets[2] = 99;
            case 14 -> offsets[4] = 99;
            case 15 -> planes[1] = new int[0];
            case 16 -> { need = new int[2]; exclude = new int[2]; }
            case 17 -> { need = new int[]{1, 8}; exclude = new int[]{1, 0}; }
            case 18 -> { need = new int[2]; exclude = new int[2]; offsets[4] = 99; }
            case 19 -> { need = new int[]{1, 8}; exclude = new int[]{1, 0}; offsets[4] = 99; }
            case 20 -> { rows = null; offsets = null; planes = null; need = null; exclude = null; }
            case 21 -> { offsets = new int[4]; planes = new int[0][]; }
            case 22 -> { planes[1] = null; offsets[0] = -1; }
            default -> throw new AssertionError(fault);
        }
        return new Input(rows, offsets, planes, need, exclude);
    }

    private static long[] select(Input in, int first, int count, int stride) {
        return M3LongIndexPathMasks.select(in.rows(), in.offsets(), in.planes(),
                in.need(), in.exclude(), first, count, stride);
    }

    private static long window(Input in, int first, int count, int stride, int next,
            long[] output, int offset, int limit) {
        return M3LongIndexPathMasks.scanWindow(in.rows(), in.offsets(), in.planes(),
                in.need(), in.exclude(), first, count, stride, next, output, offset, limit);
    }

    public static void main(String[] args) {
        trace = args.length != 0 && args[0].equals("--trace");
        for (int fault = 0; fault <= 22; fault++) {
            Input in = input(fault);
            int identity = fault;
            for (int[] path : new int[][] {{0, 5, 1}, {4, 5, -1}, {4, 3, -2},
                    {5, 0, 1}, {0, 0, -1}, {-1, -1, 0}, {0, -1, 1},
                    {5, 1, 1}, {0, 2, Integer.MIN_VALUE}, {4, 2, Integer.MAX_VALUE}}) {
                observe("select:" + identity + ":" + Arrays.toString(path),
                        () -> select(in, path[0], path[1], path[2]), null);
            }
            for (int next : new int[] {-1, 0, 1, 4, 5, 6, Integer.MAX_VALUE}) {
                for (int limit : new int[] {-1, 0, 1, 2, 4, 4097}) {
                    long[] out = {91, 92, 93, 94, 95, 96};
                    observe("window:" + identity + ":" + next + ":" + limit,
                            () -> window(in, 0, 5, 1, next, out, 1, limit), out);
                }
            }
            observe("null-output-first:" + fault,
                    () -> window(in, -1, -1, 0, -1, null, -1, -1), null);
            observe("bad-output-offset:" + fault,
                    () -> window(in, 0, 5, 1, 0, new long[2], -1, 1), null);
        }
        Input valid = input(0);
        for (int next : new int[] {0, 5}) {
            for (int limit : new int[] {0, 1}) {
                observe("alias:" + next + ":" + limit,
                        () -> window(valid, 0, 5, 1, next, valid.rows(), 0, limit), valid.rows());
            }
        }
        for (int fault : new int[] {0, 16, 17}) {
            Input in = input(fault);
            observe("fresh-empty:" + fault,
                    () -> select(in, 5, 0, 1) != select(in, 5, 0, 1), null);
            observe("detached:" + fault, () -> select(in, 0, 5, 1) != in.rows(), null);
        }
        long[] out = {91, 92, 93};
        Input laterInvalid = input(14);
        observe("valid-window-before-invalid-offset",
                () -> window(laterInvalid, 0, 5, 1, 0, out, 1, 1), out);
        observe("subsequent-invalid-window-preserves-buffer",
                () -> window(laterInvalid, 0, 5, 1, 1, out, 1, 1), out);
        for (long packed : new long[]{0, -1, Long.MIN_VALUE, Long.MAX_VALUE, 0x0000000500000002L}) {
            observe("decode:" + packed, () -> M3LongIndexPathMasks.nextOrdinal(packed)
                    + ":" + M3LongIndexPathMasks.written(packed), null);
        }
        System.out.println("LONG_PATH_BRIDGE_BEHAVIOR checks=" + checks
                + " sha256=" + HexFormat.of().formatHex(TAPE.digest()));
    }
}
