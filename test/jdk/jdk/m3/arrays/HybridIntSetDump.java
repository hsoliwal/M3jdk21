// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.stream.IntStream;

/**
 * Differential dump and reference model (gates 3 and 4 of recipe/hybrid-int-set-20261106/differential.sh). Replays one
 * seeded corpus of int-set pairs (empty, sparse over the whole non-negative range, single-chunk ARRAY, run-heavy,
 * dense BITMAP, chunk keys 0 / 1 / 32,767 near Integer.MAX_VALUE, and overlapping derived pairs; every input unsorted
 * with duplicates) through the hybrid set class named by args[0], reached by reflection so that the donor class and
 * the relocated class run the same driver. For each set it prints size, chunk count, payload bytes, every chunk key
 * and kind, a digest of toArray and a bit string of 64 contains probes (members, neighbours, chunk boundaries,
 * negatives, MAX_VALUE); for each pair the same summary of union, intersect, xor and minus; and the exception class
 * of every refused call. Every toArray, contains and algebra result is also checked against java.util.TreeSet (an
 * independent reference model); a mismatch aborts the run. Two sides agree when their dumps are byte-identical.
 * Usage: {@code java -cp classes;test-classes jdk.m3.arrays.HybridIntSetDump FQN [CASES]}
 */
//FLLDCIM: TFG - dump driver from the template M3CsrIndexDump (recipe csr-index-20261106): reflective, serves donor and relocated class
public final class HybridIntSetDump {
    private static final long SEED = 0x4859_4252_4944_3236L;
    private static final int DEFAULT_CASES = 2_000;
    private static final int SHAPES = 7;
    private static final int CHUNK_BITS = 16;
    private static final int LOW_MASK = 0xFFFF;
    private static final int TOP_KEY = Integer.MAX_VALUE >>> CHUNK_BITS;
    private static final int SPARSE_MAX = 300;
    private static final int ARRAY_MAX = 4_096;
    private static final int RUNS_MAX = 600;
    private static final int RUN_LENGTH_MAX = 40;
    private static final int BITMAP_MIN = 4_200;
    private static final int BITMAP_SPAN = 20_000;
    private static final int PROBES = 64;
    private static final long DIGEST_MULTIPLIER = 0x9E37_79B9_7F4A_7C15L;
    private static final String[] OPERATIONS = {"union", "intersect", "xor", "minus"};

    private final Method of;
    private final Method size;
    private final Method chunks;
    private final Method payload;
    private final Method chunkKind;
    private final Method chunkKey;
    private final Method contains;
    private final Method toArray;
    private final Method[] operations = new Method[OPERATIONS.length];
    private final PrintStream out;
    private long references;
    private long refusals;

    private HybridIntSetDump(Class<?> set, PrintStream out) throws ReflectiveOperationException {
        this.of = set.getMethod("of", int[].class);
        this.size = set.getMethod("size");
        this.chunks = set.getMethod("chunks");
        this.payload = set.getMethod("primitivePayloadBytes");
        this.chunkKind = set.getMethod("chunkKind", int.class);
        this.chunkKey = set.getMethod("chunkKey", int.class);
        this.contains = set.getMethod("contains", int.class);
        this.toArray = set.getMethod("toArray");
        for (int op = 0; op < OPERATIONS.length; op++) {
            operations[op] = set.getMethod(OPERATIONS[op], set);
        }
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        run(Class.forName(args[0]), args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_CASES, out);
        out.flush();
    }

    /** Runs the corpus; returns {cases, reference comparisons, refusals}. */
    static long[] run(Class<?> set, int cases, PrintStream out) throws Exception {
        HybridIntSetDump dump = new HybridIntSetDump(set, out);
        dump.refusalCases();
        SplittableRandom random = new SplittableRandom(SEED);
        for (int id = 0; id < cases; id++) {
            dump.pairCase(id, random);
        }
        out.println("TOTAL cases=" + cases + " references=" + dump.references + " refusals=" + dump.refusals);
        return new long[] {cases, dump.references, dump.refusals};
    }

    static PrintStream discard() {
        return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    private void pairCase(int id, SplittableRandom random) throws Exception {
        int[] left = values(id % SHAPES, random, null);
        int[] right = values(random.nextInt(SHAPES), random, left);
        Object a = of.invoke(null, (Object) left);
        Object b = of.invoke(null, (Object) right);
        TreeSet<Integer> ra = reference(left);
        TreeSet<Integer> rb = reference(right);
        StringBuilder line = new StringBuilder(512).append("CASE ").append(id);
        line.append(" A{").append(summary(a, ra, random)).append('}');
        line.append(" B{").append(summary(b, rb, random)).append('}');
        for (int op = 0; op < OPERATIONS.length; op++) {
            line.append(' ').append(OPERATIONS[op]).append('{')
                    .append(summary(operations[op].invoke(a, b), expected(op, ra, rb), random)).append('}');
        }
        out.println(line);
    }

    private static TreeSet<Integer> expected(int op, TreeSet<Integer> a, TreeSet<Integer> b) {
        TreeSet<Integer> result = new TreeSet<>(a);
        if (op == 0) {
            result.addAll(b);
        } else if (op == 1) {
            result.retainAll(b);
        } else if (op == 3) {
            result.removeAll(b);
        } else {
            result.addAll(b);
            TreeSet<Integer> both = new TreeSet<>(a);
            both.retainAll(b);
            result.removeAll(both);
        }
        return result;
    }

    private static TreeSet<Integer> reference(int[] values) {
        TreeSet<Integer> set = new TreeSet<>();
        Arrays.stream(values).forEach(set::add);
        return set;
    }

    private int[] values(int shape, SplittableRandom random, int[] other) {
        int key = random.nextInt(TOP_KEY + 1);
        int[] v = switch (shape) {
            case 0 -> new int[0];
            case 1 -> random.ints(1 + random.nextInt(SPARSE_MAX), 0, Integer.MAX_VALUE).toArray();
            case 2 -> random.ints(1 + random.nextInt(ARRAY_MAX), 0, LOW_MASK + 1).map(low -> key << CHUNK_BITS | low).toArray();
            case 3 -> runs(random, key);
            case 4 -> random.ints(BITMAP_MIN + random.nextInt(BITMAP_SPAN), 0, LOW_MASK + 1).map(low -> key << CHUNK_BITS | low).toArray();
            case 5 -> random.ints(1 + random.nextInt(ARRAY_MAX), 0, 1 << (CHUNK_BITS + 1))
                    .map(x -> (x & 1) == 0 ? x >>> 1 : (TOP_KEY << CHUNK_BITS) | ((x >>> 1) & LOW_MASK)).toArray();
            default -> other == null || other.length == 0 ? new int[] {0, LOW_MASK, LOW_MASK + 1, Integer.MAX_VALUE}
                    : Arrays.stream(other).filter(x -> random.nextBoolean()).map(x -> random.nextInt(4) == 0 ? x + 1 & Integer.MAX_VALUE : x).toArray();
        };
        int[] shuffled = Arrays.copyOf(v, v.length + v.length / 8);
        for (int i = v.length; i < shuffled.length; i++) {
            shuffled[i] = v[random.nextInt(v.length)];
        }
        for (int i = shuffled.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = t;
        }
        return shuffled;
    }

    private static int[] runs(SplittableRandom random, int key) {
        int count = 1 + random.nextInt(RUNS_MAX);
        return random.ints(count, 0, LOW_MASK + 1 - RUN_LENGTH_MAX)
                .flatMap(start -> IntStream.range(start, start + 1 + random.nextInt(RUN_LENGTH_MAX)))
                .map(low -> key << CHUNK_BITS | low).toArray();
    }

    private String summary(Object set, TreeSet<Integer> expected, SplittableRandom random) throws Exception {
        int[] content = (int[]) toArray.invoke(set);
        int[] reference = expected.stream().mapToInt(Integer::intValue).toArray();
        check(Arrays.equals(content, reference), "toArray");
        int n = (int) size.invoke(set);
        check(n == reference.length, "size");
        int c = (int) chunks.invoke(set);
        StringBuilder line = new StringBuilder(64).append(n).append(';').append(c).append(';').append(payload.invoke(set));
        for (int i = 0; i < c; i++) {
            line.append(';').append(chunkKey.invoke(set, i)).append(((Enum<?>) chunkKind.invoke(set, i)).name().charAt(0));
        }
        long digest = n;
        for (int x : content) {
            digest = digest * DIGEST_MULTIPLIER + x;
        }
        return line.append(';').append(Long.toHexString(digest)).append(';').append(probes(set, content, expected, random)).toString();
    }

    private String probes(Object set, int[] content, TreeSet<Integer> expected, SplittableRandom random) throws Exception {
        long bits = 0;
        for (int p = 0; p < PROBES; p++) {
            int probe = probe(p, content, random);
            boolean member = (boolean) contains.invoke(set, probe);
            check(member == expected.contains(probe), "contains " + probe);
            bits |= member ? 1L << p : 0L;
        }
        return Long.toHexString(bits);
    }

    private static int probe(int p, int[] content, SplittableRandom random) {
        int base = content.length == 0 ? random.nextInt(Integer.MAX_VALUE) : content[random.nextInt(content.length)];
        return switch (p % 8) {
            case 0, 1 -> base;
            case 2 -> base + 1;
            case 3 -> base - 1;
            case 4 -> base & ~LOW_MASK;
            case 5 -> base | LOW_MASK;
            case 6 -> p < 8 ? Integer.MIN_VALUE : -1 - random.nextInt(Integer.MAX_VALUE);
            default -> p < 8 ? Integer.MAX_VALUE : random.nextInt(Integer.MAX_VALUE);
        };
    }

    private void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError("REFERENCE_DRIFT " + what);
        }
        references++;
    }

    private void refusalCases() throws Exception {
        Object set = of.invoke(null, (Object) new int[] {3, 1, 70_000});
        StringBuilder line = new StringBuilder("REFUSALS");
        line.append(" null=").append(refusal(() -> of.invoke(null, (Object) null)));
        line.append(" negative=").append(refusal(() -> of.invoke(null, (Object) new int[] {-1})));
        line.append(" laterNegative=").append(refusal(() -> of.invoke(null, (Object) new int[] {7, 3, -9})));
        line.append(" kindLow=").append(refusal(() -> chunkKind.invoke(set, -1)));
        line.append(" kindHigh=").append(refusal(() -> chunkKind.invoke(set, 2)));
        line.append(" keyHigh=").append(refusal(() -> chunkKey.invoke(set, 2)));
        line.append(" unionNull=").append(refusal(() -> operations[0].invoke(set, (Object) null)));
        line.append(" containsNegative=").append(contains.invoke(set, -1));
        out.println(line);
    }

    @FunctionalInterface
    private interface Call { Object call() throws ReflectiveOperationException; }

    /** The exception class of a refused call, or OK when the call returns. */
    private String refusal(Call call) {
        try {
            call.call();
            return "OK";
        } catch (InvocationTargetException thrown) {
            refusals++;
            return thrown.getCause().getClass().getName();
        } catch (ReflectiveOperationException broken) {
            throw new AssertionError("reflection failed", broken);
        }
    }
}
