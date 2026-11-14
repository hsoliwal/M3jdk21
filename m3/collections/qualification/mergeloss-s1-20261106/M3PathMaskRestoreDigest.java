// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Qualification driver (not a test) of mergeloss-s1-20261106: seeded valid and invalid geometries through every public
 * M3IndexPathMasks operation, one SHA-256 per operation over results, written output windows and exception
 * class/message. Mode "java" requests Backend.JAVA, mode "native" Backend.NATIVE_REQUIRED (firstMatchingIndex has no
 * backend). selectLong is called reflectively so the same source compiles against owners without it (it then prints
 * ABSENT). Printed lines carry no mode: java vs native runs, and master vs restored classes, compare byte for byte.
 */
//FLLDCIM: TOP - qualification/mergeloss-s1-20261106 differential driver; existing int API identity + long-row parity
public final class M3PathMaskRestoreDigest {
    private static final String[] OPS = {"select", "firstMatchingIndex", "scanWindow", "scanWindowBounded", "selectLong"};

    private M3PathMaskRestoreDigest() { }

    public static void main(String[] args) throws Exception {
        boolean nativeMode = "native".equals(args[0]);
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 20261106L;
        int rounds = args.length > 2 ? Integer.parseInt(args[2]) : 3000;
        if (nativeMode && !M3IndexPathMasks.nativeAvailable()) {
            System.err.println("native mode requested but m3_collections_pathmask is unavailable");
            System.exit(3);
        }
        M3IndexPathMasks.Backend backend = nativeMode ? M3IndexPathMasks.Backend.NATIVE_REQUIRED
                : M3IndexPathMasks.Backend.JAVA;
        Method selectLong = longSelector();
        MessageDigest[] digests = new MessageDigest[OPS.length];
        for (int i = 0; i < OPS.length; i++) digests[i] = MessageDigest.getInstance("SHA-256");
        int[] failures = new int[OPS.length];
        Random random = new Random(seed);
        for (int round = 0; round < rounds; round++) {
            Case c = Case.random(random, round);
            failures[0] += record(digests[0], () -> M3IndexPathMasks.select(c.intRows, c.offsets, c.planes,
                    c.required, c.forbidden, c.first, c.count, c.stride, backend));
            failures[1] += record(digests[1], () -> M3IndexPathMasks.firstMatchingIndex(c.intRows, c.offsets,
                    c.planes, c.required, c.forbidden, c.first, c.count, c.stride));
            int[] window = new int[c.outputLength];
            Arrays.fill(window, 0x5a5a5a5a);
            int[] target = c.alias == 1 ? c.intRows : c.alias == 2 ? c.required : window;
            failures[2] += record(digests[2], () -> M3IndexPathMasks.scanWindow(c.intRows, c.offsets, c.planes,
                    c.required, c.forbidden, c.first, c.count, c.stride, c.next, target, c.outputOffset,
                    c.limit, backend));
            digests[2].update(text(Arrays.toString(window)));
            int[] bounded = new int[c.outputLength];
            Arrays.fill(bounded, 0x3c3c3c3c);
            failures[3] += record(digests[3], () -> M3IndexPathMasks.scanWindowBounded(c.intRows, c.offsets,
                    c.planes, c.required, c.forbidden, c.first, c.count, c.stride, c.next, c.budget, bounded,
                    c.outputOffset, c.limit, backend));
            digests[3].update(text(Arrays.toString(bounded)));
            if (selectLong != null) {
                failures[4] += record(digests[4], () -> selectLong.invoke(null, c.longRows, c.offsets, c.planes,
                        c.required, c.forbidden, c.first, c.count, c.stride, backend));
            }
        }
        for (int i = 0; i < OPS.length; i++) {
            boolean absent = i == 4 && selectLong == null;
            System.out.println("OP " + OPS[i] + " seed=" + seed + " cases=" + (absent ? 0 : rounds)
                    + " exceptions=" + failures[i] + " sha256="
                    + (absent ? "ABSENT" : HexFormat.of().formatHex(digests[i].digest())));
        }
    }

    private static Method longSelector() {
        try {
            return M3IndexPathMasks.class.getMethod("selectLong", long[].class, int[].class, int[][].class,
                    int[].class, int[].class, int.class, int.class, int.class, M3IndexPathMasks.Backend.class);
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    private interface Op {
        Object run() throws Exception;
    }

    private static int record(MessageDigest digest, Op op) {
        try {
            Object result = op.run();
            String shown = result instanceof int[] ints ? Arrays.toString(ints)
                    : result instanceof long[] longs ? Arrays.toString(longs) : String.valueOf(result);
            digest.update(text("OK " + shown + "\n"));
            return 0;
        } catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            digest.update(text("EX " + cause.getClass().getName() + ": " + cause.getMessage() + "\n"));
            return 1;
        } catch (Exception | Error thrown) {
            digest.update(text("EX " + thrown.getClass().getName() + ": " + thrown.getMessage() + "\n"));
            return 1;
        }
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /** One seeded geometry; about one case in six is deliberately invalid in one dimension. */
    private static final class Case {
        int[] intRows;
        long[] longRows;
        int[] offsets;
        int[][] planes;
        int[] required;
        int[] forbidden;
        int first;
        int count;
        int stride;
        int next;
        int budget;
        int outputLength;
        int outputOffset;
        int limit;
        int alias;

        static Case random(Random random, int round) {
            Case c = new Case();
            int n = round % 11 == 0 ? random.nextInt(3) : random.nextInt(round % 5 == 0 ? 600 : 90);
            int capacity = 1 + random.nextInt(64);
            int stages = 1 + random.nextInt(round % 4 == 0 ? 11 : 4);
            c.intRows = new int[n];
            c.longRows = new long[n];
            c.offsets = new int[n];
            for (int i = 0; i < n; i++) {
                c.intRows[i] = random.nextInt();
                c.longRows[i] = switch (i % 5) {
                    case 0 -> Long.MIN_VALUE + i;
                    case 1 -> Long.MAX_VALUE - i;
                    default -> random.nextLong();
                };
                c.offsets[i] = random.nextInt(capacity);
            }
            c.planes = new int[stages][];
            c.required = new int[stages];
            c.forbidden = new int[stages];
            int shape = random.nextInt(8);
            for (int s = 0; s < stages; s++) {
                c.planes[s] = new int[capacity + random.nextInt(5)];
                for (int p = 0; p < c.planes[s].length; p++) c.planes[s][p] = random.nextInt();
                c.required[s] = shape == 0 ? 0 : random.nextInt() & random.nextInt();
                c.forbidden[s] = shape == 0 ? 0 : random.nextInt() & random.nextInt() & ~c.required[s];
                if (shape == 1) c.forbidden[s] |= c.required[s] & 1;
            }
            if (n == 0 || random.nextInt(9) == 0) {
                c.first = n;
                c.count = 0;
                c.stride = random.nextBoolean() ? 1 : -1;
            } else {
                c.first = random.nextInt(n);
                c.stride = (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(3));
                int max = c.stride > 0 ? (n - 1 - c.first) / c.stride + 1 : c.first / -c.stride + 1;
                c.count = 1 + random.nextInt(max);
            }
            c.next = c.count == 0 ? 0 : random.nextInt(c.count + 1);
            c.budget = random.nextInt(c.count + 3);
            c.limit = random.nextInt(Math.min(4096, c.count + 4) + 1);
            c.outputOffset = random.nextInt(4);
            c.outputLength = c.outputOffset + c.limit + random.nextInt(3);
            if (random.nextInt(6) == 0) c.invalidate(random, n, capacity);
            return c;
        }

        private void invalidate(Random random, int n, int capacity) {
            switch (random.nextInt(9)) {
                case 0 -> offsets = Arrays.copyOf(offsets, n + 1);
                case 1 -> { if (n > 0) offsets[random.nextInt(n)] = capacity + 7; }
                case 2 -> { if (n > 0) offsets[random.nextInt(n)] = -1; }
                case 3 -> stride = 0;
                case 4 -> count = n + 2;
                case 5 -> required = Arrays.copyOf(required, required.length + 1);
                case 6 -> planes[random.nextInt(planes.length)] = null;
                case 7 -> first = -1;
                default -> alias = 1 + random.nextInt(2);
            }
        }
    }
}
