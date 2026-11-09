// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * Seeded differential dump of one M3PackedSpatialIndex implementation, or of a linear-scan reference.
 * <pre>
 * java M3PackedSpatialIndexDifferential &lt;impl&gt; &lt;dump-file&gt;
 *   impl = fully qualified M3PackedSpatialIndex class (donor or relocated): C/A lines (every case, every query with an
 *          ample budget) and B lines (result and visit budgets, refusals, input and output non-aliasing)
 *   impl = BRUTE: C/A lines from a linear scan of the same rectangles (exact half-open intersection)
 *   impl = fully qualified M3SpatialQueries class: C/A lines of the POINT1 family only (unit-cell points), from its
 *          inclusive point filter over [a, c - 1] x [b, d - 1]
 * </pre>
 * Families: RECT (mixed rectangles), POINT0 (zero-area points, never hit), POINT1 (unit-cell points, duplicates),
 * TIES (few distinct minX), EXTREME (coordinates at both ends of int). Every case and query is drawn from a
 * SplittableRandom seeded by family and case, so every implementation sees the same input.
 */
public final class M3PackedSpatialIndexDifferential {
    private static final long SEED = 0x5A7A_2026_1106L;
    private static final String[] FAMILIES = {"RECT", "POINT0", "POINT1", "TIES", "EXTREME"};
    private static final int[] CASES = {48, 8, 32, 24, 24};
    private static final int QUERIES = 200;
    private static final long AMPLE_VISITS = 20_000_000L;
    private static final long[] VISIT_PROBES = {0L, 1L, 3L, 17L};

    private final StringBuilder dump = new StringBuilder(1 << 20);
    private final Impl impl;
    private long queries;
    private long hits;
    private long bLines;

    private M3PackedSpatialIndexDifferential(Impl impl) {
        this.impl = impl;
    }

    /** One implementation under test; build returns an opaque index. */
    private interface Impl {
        Object build(int[] l, int[] b, int[] u, int[] t) throws Throwable;

        int size(Object index) throws Throwable;

        int[] query(Object index, int a, int b, int c, int d, int maxResults, long maxVisits) throws Throwable;

        boolean full();

        boolean family(String name);
    }

    /** Donor or relocated M3PackedSpatialIndex through reflection (no compile-time dependency on either). */
    private static final class Reflective implements Impl {
        private final Method build;
        private final Method size;
        private final Method query;

        Reflective(String name) throws ReflectiveOperationException {
            Class<?> type = Class.forName(name);
            build = type.getMethod("build", int[].class, int[].class, int[].class, int[].class);
            size = build.getReturnType().getMethod("size");
            query = build.getReturnType().getMethod("query", int.class, int.class, int.class, int.class,
                    int.class, long.class);
        }

        public Object build(int[] l, int[] b, int[] u, int[] t) throws Throwable {
            return call(build, null, l, b, u, t);
        }

        public int size(Object index) throws Throwable {
            return (Integer) call(size, index);
        }

        public int[] query(Object index, int a, int b, int c, int d, int maxResults, long maxVisits)
                throws Throwable {
            return (int[]) call(query, index, a, b, c, d, maxResults, maxVisits);
        }

        public boolean full() {
            return true;
        }

        public boolean family(String name) {
            return true;
        }
    }

    /** Linear scan reference: exact half-open intersection of positive-area rectangles, ascending ids. */
    private static final class Brute implements Impl {
        public Object build(int[] l, int[] b, int[] u, int[] t) {
            return new int[][] {l.clone(), b.clone(), u.clone(), t.clone()};
        }

        public int size(Object index) {
            return ((int[][]) index)[0].length;
        }

        public int[] query(Object index, int a, int b, int c, int d, int maxResults, long maxVisits) {
            int[][] lanes = (int[][]) index;
            int[] hit = new int[lanes[0].length];
            int used = 0;
            for (int id = 0; id < hit.length; id++) {
                if (lanes[0][id] < lanes[2][id] && lanes[1][id] < lanes[3][id] && a < c && b < d
                        && lanes[0][id] < c && lanes[2][id] > a && lanes[1][id] < d && lanes[3][id] > b) {
                    hit[used++] = id;
                }
            }
            return java.util.Arrays.copyOf(hit, used);
        }

        public boolean full() {
            return false;
        }

        public boolean family(String name) {
            return true;
        }
    }

    /** First-party point filter M3SpatialQueries.rectangle over the unit-cell points (lower-left corners). */
    private static final class PointFilter implements Impl {
        private final Method rectangle;

        PointFilter(String name) throws ReflectiveOperationException {
            rectangle = Class.forName(name).getMethod("rectangle", int[].class, int[].class, int.class, int.class,
                    int.class, int.class);
        }

        public Object build(int[] l, int[] b, int[] u, int[] t) {
            return new int[][] {l.clone(), b.clone()};
        }

        public int size(Object index) {
            return ((int[][]) index)[0].length;
        }

        public int[] query(Object index, int a, int b, int c, int d, int maxResults, long maxVisits)
                throws Throwable {
            int[][] points = (int[][]) index;
            if (a == c || b == d) {
                return new int[0];
            }
            long[] mask = (long[]) call(rectangle, null, points[0], points[1], a, b, c - 1, d - 1);
            int[] hit = new int[points[0].length];
            int used = 0;
            for (int id = 0; id < hit.length; id++) {
                if ((mask[id >>> 6] & (1L << (id & 63))) != 0L) {
                    hit[used++] = id;
                }
            }
            return java.util.Arrays.copyOf(hit, used);
        }

        public boolean full() {
            return false;
        }

        public boolean family(String name) {
            return "POINT1".equals(name);
        }
    }

    private static Object call(Method method, Object target, Object... args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static String ids(int[] values) {
        if (values.length == 0) {
            return "-";
        }
        StringBuilder text = new StringBuilder(values.length * 4);
        for (int i = 0; i < values.length; i++) {
            text.append(i == 0 ? "" : ",").append(values[i]);
        }
        return text.toString();
    }

    private String outcome(Object index, int a, int b, int c, int d, int maxResults, long maxVisits) {
        try {
            return "ok:" + ids(impl.query(index, a, b, c, d, maxResults, maxVisits));
        } catch (Throwable failure) {
            return "throw:" + failure.getClass().getName() + ":" + failure.getMessage();
        }
    }

    private String buildOutcome(int[] l, int[] b, int[] u, int[] t) {
        try {
            return "ok:size=" + impl.size(impl.build(l, b, u, t));
        } catch (Throwable failure) {
            return "throw:" + failure.getClass().getName() + ":" + failure.getMessage();
        }
    }

    private void line(String text) {
        dump.append(text).append('\n');
    }

    /** Coordinate draw of one family: {low, high (exclusive), max extent}. */
    private static int[] range(String family) {
        return switch (family) {
            case "RECT" -> new int[] {-1000, 1001, 121};
            case "POINT0" -> new int[] {-200, 201, 1};
            case "POINT1" -> new int[] {-150, 151, 2};
            case "TIES" -> new int[] {-100, 101, 61};
            default -> new int[] {0, 0, 501};
        };
    }

    private static int coordinate(SplittableRandom r, String family) {
        if ("EXTREME".equals(family)) {
            return r.nextBoolean() ? Integer.MIN_VALUE + r.nextInt(0, 3000)
                    : Integer.MAX_VALUE - 3000 + r.nextInt(0, 2500);
        }
        int[] range = range(family);
        return r.nextInt(range[0], range[1]);
    }

    private static int extent(SplittableRandom r, String family) {
        return switch (family) {
            case "POINT0" -> 0;
            case "POINT1" -> 1;
            default -> r.nextInt(0, range(family)[2]);
        };
    }

    private static int far(int start, int length) {
        return (int) Math.min((long) start + length, Integer.MAX_VALUE);
    }

    private void runCase(int familyIndex, int caseIndex) throws Throwable {
        String family = FAMILIES[familyIndex];
        SplittableRandom r = new SplittableRandom(SEED ^ (familyIndex * 1_000_003L + caseIndex));
        int n = r.nextInt(0, "POINT1".equals(family) ? 2000 : 1500);
        int[] l = new int[n], b = new int[n], u = new int[n], t = new int[n];
        for (int k = 0; k < n; k++) {
            l[k] = "TIES".equals(family) ? r.nextInt(-5, 6) : coordinate(r, family);
            b[k] = coordinate(r, family);
            u[k] = l[k] + extent(r, family);
            t[k] = b[k] + extent(r, family);
        }
        if (!impl.family(family)) {
            return;
        }
        Object index = impl.build(l, b, u, t);
        line("C " + family + " " + caseIndex + " n=" + n + " size=" + impl.size(index));
        int[][] asked = new int[QUERIES][];
        for (int q = 0; q < QUERIES; q++) {
            int a = "TIES".equals(family) ? r.nextInt(-30, 31) : coordinate(r, family);
            int c = r.nextInt(16) == 0 ? a : far(a, r.nextInt(0, 400));
            int y = coordinate(r, family);
            int d = r.nextInt(16) == 0 ? y : far(y, r.nextInt(0, 400));
            int[] result = impl.query(index, a, y, c, d, n, AMPLE_VISITS);
            asked[q] = new int[] {a, y, c, d, result.length};
            queries++;
            hits += result.length;
            line("A " + family + " " + caseIndex + " " + q + " " + a + " " + y + " " + c + " " + d + " : "
                    + ids(result));
        }
        if (impl.full()) {
            budgets(family, caseIndex, index, asked, n);
            aliasing(family, caseIndex, index, asked, l, b, u, t, n);
        }
    }

    private void budgets(String family, int caseIndex, Object index, int[][] asked, int n) {
        for (int q = 0; q < QUERIES; q += 8) {
            int[] s = asked[q];
            String head = "B " + family + " " + caseIndex + " " + q + " ";
            int fewer = Math.max(0, s[4] - 1);
            line(head + "results=" + fewer + " " + outcome(index, s[0], s[1], s[2], s[3], fewer, AMPLE_VISITS));
            line(head + "results=" + s[4] + " " + outcome(index, s[0], s[1], s[2], s[3], s[4], AMPLE_VISITS));
            for (long visits : VISIT_PROBES) {
                line(head + "visits=" + visits + " " + outcome(index, s[0], s[1], s[2], s[3], n, visits));
            }
            bLines += 2 + VISIT_PROBES.length;
        }
        String head = "B " + family + " " + caseIndex + " refusal ";
        line(head + "reversed " + outcome(index, 5, 0, 4, 1, n, AMPLE_VISITS));
        line(head + "results=-1 " + outcome(index, 0, 0, 1, 1, -1, AMPLE_VISITS));
        line(head + "visits=-1 " + outcome(index, 0, 0, 1, 1, n, -1L));
        line(head + "visits=max+1 " + outcome(index, 0, 0, 1, 1, n, AMPLE_VISITS + 1L));
        bLines += 4;
    }

    private void aliasing(String family, int caseIndex, Object index, int[][] asked,
            int[] l, int[] b, int[] u, int[] t, int n) throws Throwable {
        int[] first = impl.query(index, asked[0][0], asked[0][1], asked[0][2], asked[0][3], n, AMPLE_VISITS);
        if (first.length > 0) {
            first[0] = -2;
        }
        java.util.Arrays.fill(l, Integer.MAX_VALUE);
        java.util.Arrays.fill(u, Integer.MIN_VALUE);
        java.util.Arrays.fill(b, 0);
        java.util.Arrays.fill(t, 0);
        String head = "B " + family + " " + caseIndex + " alias ";
        for (int q = 0; q < Math.min(4, QUERIES); q++) {
            line(head + q + " " + outcome(index, asked[q][0], asked[q][1], asked[q][2], asked[q][3], n,
                    AMPLE_VISITS));
            bLines++;
        }
    }

    private void buildRefusals() {
        int[] one = {0};
        int[] two = {0, 1};
        line("B build reversed-x " + buildOutcome(new int[] {2}, one, new int[] {1}, new int[] {1}));
        line("B build reversed-y " + buildOutcome(one, new int[] {2}, new int[] {1}, new int[] {1}));
        line("B build length " + buildOutcome(two, one, two, two));
        line("B build null " + buildOutcome(null, one, one, one));
        line("B build empty " + buildOutcome(new int[0], new int[0], new int[0], new int[0]));
        int[] big = new int[200_001];
        line("B build over-cap " + buildOutcome(big, big, big, big));
        int[] cap = new int[200_000];
        line("B build at-cap " + buildOutcome(cap, cap, cap, cap));
        bLines += 7;
    }

    private void run() throws Throwable {
        for (int f = 0; f < FAMILIES.length; f++) {
            for (int k = 0; k < CASES[f]; k++) {
                runCase(f, k);
            }
        }
        if (impl.full()) {
            buildRefusals();
        }
    }

    private static Impl select(String name) throws ReflectiveOperationException {
        if ("BRUTE".equals(name)) {
            return new Brute();
        }
        return name.endsWith(".M3SpatialQueries") ? new PointFilter(name) : new Reflective(name);
    }

    public static void main(String[] args) throws Throwable {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: <impl> <dump-file>");
        }
        M3PackedSpatialIndexDifferential run = new M3PackedSpatialIndexDifferential(select(args[0]));
        run.run();
        byte[] bytes = run.dump.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(Path.of(args[1]), bytes);
        System.out.println("M3_PACKED_SPATIAL_INDEX_DIFF queries=" + run.queries + " hits=" + run.hits
                + " b_lines=" + run.bLines + " bytes=" + bytes.length + " sha256=" + sha256(bytes));
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
