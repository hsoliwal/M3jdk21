/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * @test
 * @summary Whole-token predicates, scoped identity, state masks and existing JNI effects
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3TokenRegexPlanContract
 */
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import jdk.internal.mindex.M3TokenRegexPlan;
import jdk.internal.mindex.M3TokenRegexPlan.Input;
import jdk.internal.mindex.M3TokenRegexPlan.Match;
import jdk.internal.mindex.M3TokenRegexPlan.Predicate;

public class M3TokenRegexPlanContract {
    private static final Object DOMAIN = new Object();
    private static long checks;
    private static native void registerKernel();
    private static native long nativeCalls();
    private static final Predicate[] CHOICES = {
        new Predicate(0, 0, 0, 0, M3TokenRegexPlan.KIND),
        new Predicate(1, Long.MIN_VALUE, 0, 0, M3TokenRegexPlan.KIND | M3TokenRegexPlan.SYMBOL),
        new Predicate(0, 0, 0, 0, 0),
        new Predicate(0, 0, 1, 2, 0)
    };

    private static void check(boolean value, String detail) {
        if (!value) throw new AssertionError(detail);
        checks++;
    }

    private static void reject(Class<? extends Throwable> type, Runnable task) {
        try { task.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) { checks++; return; }
            throw new AssertionError("expected " + type, failure);
        }
        throw new AssertionError("expected " + type);
    }

    private static boolean predicate(Predicate p, Input in, int t) {
        if ((p.attributes() & M3TokenRegexPlan.KIND) != 0 && p.kind() != in.kindAt(t)) return false;
        if ((p.attributes() & M3TokenRegexPlan.SYMBOL) != 0 && p.symbol() != in.symbolAt(t)) return false;
        for (int bit = 0; bit < 64; bit++) {
            long mask = 1L << bit;
            if ((p.requiredFlags() & mask) != 0 && (in.flagsAt(t) & mask) == 0) return false;
            if ((p.forbiddenFlags() & mask) != 0 && (in.flagsAt(t) & mask) != 0) return false;
        }
        return true;
    }

    // Independent two-dimensional regular-language recurrence, not production bit-state code.
    private static boolean oracle(Predicate[] atoms, long repeated, Input in, int from, int to) {
        boolean[][] dp = new boolean[to - from + 1][atoms.length + 1];
        dp[0][0] = true;
        for (int j = 1; j <= atoms.length; j++) {
            if ((repeated & (1L << (j - 1))) != 0) dp[0][j] = dp[0][j - 1];
        }
        for (int i = 1; i <= to - from; i++) for (int j = 1; j <= atoms.length; j++) {
            boolean hit = predicate(atoms[j - 1], in, from + i - 1);
            dp[i][j] = (repeated & (1L << (j - 1))) != 0
                    ? dp[i][j - 1] || hit && dp[i - 1][j]
                    : hit && dp[i - 1][j - 1];
        }
        return dp[to - from][atoms.length];
    }

    private static Match findOracle(Predicate[] atoms, long repeated, Input in) {
        for (int first = 0; first <= in.tokenCount(); first++) {
            for (int last = first; last <= in.tokenCount(); last++) {
                if (oracle(atoms, repeated, in, first, last)) return new Match(first, last);
            }
        }
        return null;
    }

    private static Input encoded(int code, int count) {
        int[] kinds = new int[count];
        long[] symbols = new long[count], flags = new long[count];
        for (int i = 0; i < count; i++, code /= 3) {
            int value = code % 3;
            kinds[i] = value == 0 ? 0 : 1;
            symbols[i] = value == 2 ? Long.MAX_VALUE : Long.MIN_VALUE;
            flags[i] = value == 0 ? 1 : value == 1 ? 0 : Long.MIN_VALUE;
        }
        return Input.of(DOMAIN, kinds, symbols, flags);
    }

    private static void exhaustive() {
        for (int length = 0, patterns = 1; length <= 3; length++, patterns *= CHOICES.length) {
            for (int p = 0; p < patterns; p++) {
                Predicate[] atoms = new Predicate[length];
                for (int i = 0, value = p; i < length; i++, value /= CHOICES.length) atoms[i] = CHOICES[value % CHOICES.length];
                for (int repeated = 0; repeated < 1 << length; repeated++) {
                    M3TokenRegexPlan plan = M3TokenRegexPlan.compile(DOMAIN, atoms, repeated);
                    for (int n = 0, words = 1; n <= 4; n++, words *= 3) for (int code = 0; code < words; code++) {
                        Input in = encoded(code, n);
                        check(plan.matches(in) == oracle(atoms, repeated, in, 0, n), "full DP parity");
                        check(Objects.equals(plan.find(in), findOracle(atoms, repeated, in)), "earliest/shortest parity");
                    }
                }
            }
        }
    }

    private static void effects(Random random) {
        for (int trial = 0; trial < 500; trial++) {
            int count = random.nextInt(64), length = random.nextInt(24);
            Predicate[] atoms = new Predicate[count];
            for (int i = 0; i < count; i++) atoms[i] = CHOICES[random.nextInt(CHOICES.length)];
            long bound = count == 63 ? Long.MAX_VALUE : (1L << count) - 1;
            long repeated = random.nextLong() & bound;
            M3TokenRegexPlan plan = M3TokenRegexPlan.compile(DOMAIN, atoms, repeated);
            Input input = encoded(random.nextInt(Integer.MAX_VALUE), length);
            boolean truth = oracle(atoms, repeated, input, 0, length);
            check(plan.matches(input) == truth, "wide DP parity");
            int split = random.nextInt(length + 1);
            M3TokenRegexPlan.Effect left = plan.summarize(input.slice(0, split), null);
            M3TokenRegexPlan.Effect right = plan.summarize(input.slice(split, length), null);
            M3TokenRegexPlan.Effect whole = plan.summarize(input, null);
            check(whole.matches() == truth, "effect truth");
            M3TokenRegexPlan.Effect joined = left.then(right, null);
            check(joined.tokenCount() == length && joined.matches() == truth, "joined effect");
            long universe = count == 63 ? -1L : (1L << (count + 1)) - 1;
            for (int state = 0; state <= count; state++) {
                check(joined.apply(1L << state) == whole.apply(1L << state), "all-entry composition");
                check(plan.identity().apply(1L << state) == 1L << state, "raw identity");
            }
            long frontier = random.nextLong() & universe;
            check(joined.apply(frontier) == whole.apply(frontier), "union composition");
            M3TokenRegexPlan.Effect power = whole.repeated(5, null), slow = plan.identity();
            for (int i = 0; i < 5; i++) slow = slow.then(whole, () -> { });
            check(power.tokenCount() == 5L * length && power.apply(frontier) == slow.apply(frontier), "repeat algebra");
        }
    }

    private static void boundaries(boolean nativeMode) {
        Predicate any = CHOICES[2];
        Predicate[] all = new Predicate[63];
        Arrays.fill(all, any);
        M3TokenRegexPlan exact = M3TokenRegexPlan.compile(DOMAIN, all, 0);
        check(exact.matches(encoded(0, 63)), "accept bit 63");
        check(!exact.matches(encoded(0, 62)) && !exact.matches(encoded(0, 64)), "63 boundary");
        M3TokenRegexPlan nullable = M3TokenRegexPlan.compile(DOMAIN, all, Long.MAX_VALUE);
        check(nullable.initialState() == -1L && nullable.matches(encoded(0, 0)), "all nullable bits");
        int[] kinds = {8, 9, 10}; long[] symbols = {1, Long.MIN_VALUE, 3}, flags = {0, Long.MIN_VALUE, 0};
        Input input = Input.of(DOMAIN, kinds, symbols, flags);
        Input slice = input.slice(1, 3).slice(0, 1);
        kinds[1] = 0; symbols[1] = 0; flags[1] = 0;
        Predicate[] atom = {new Predicate(9, Long.MIN_VALUE, Long.MIN_VALUE, 1, M3TokenRegexPlan.KIND | M3TokenRegexPlan.SYMBOL)};
        M3TokenRegexPlan plan = M3TokenRegexPlan.compile(DOMAIN, atom, 0);
        atom[0] = any;
        check(plan.matches(slice) && plan.find(input).equals(new Match(1, 2)), "snapshot and slice identity");
        reject(IllegalArgumentException.class, () -> plan.matches(Input.of(new Object(), new int[]{9}, new long[]{Long.MIN_VALUE}, new long[]{Long.MIN_VALUE})));
        reject(IllegalArgumentException.class, () -> plan.summarize(slice, null).then(exact.identity(), null));
        reject(IllegalArgumentException.class, () -> new Predicate(0, 0, 1, 1, 0));
        reject(IllegalArgumentException.class, () -> new Predicate(0, 0, 0, 0, 4));
        reject(IllegalArgumentException.class, () -> M3TokenRegexPlan.compile(DOMAIN, new Predicate[64], 0));
        reject(IllegalArgumentException.class, () -> M3TokenRegexPlan.compile(DOMAIN, new Predicate[]{any}, 2));
        reject(IllegalArgumentException.class, () -> M3TokenRegexPlan.compile(DOMAIN, all, Long.MIN_VALUE));
        reject(NullPointerException.class, () -> M3TokenRegexPlan.compile(DOMAIN, new Predicate[]{null}, 0));
        reject(NullPointerException.class, () -> M3TokenRegexPlan.compile(null, all, 0));
        reject(IndexOutOfBoundsException.class, () -> slice.slice(0, 2));
        reject(IndexOutOfBoundsException.class, () -> slice.symbolAt(-1));
        reject(IllegalArgumentException.class, () -> Input.of(DOMAIN, new int[0], new long[1], new long[0]));
        reject(IllegalArgumentException.class, () -> plan.accepts(4));
        reject(IllegalArgumentException.class, () -> plan.identity().repeated(-1, null));
        reject(ArithmeticException.class, () -> exact.summarize(encoded(0, 2), null).repeated(Long.MAX_VALUE, null));
        RuntimeException marker = new RuntimeException("checkpoint marker");
        try { plan.summarize(slice, () -> { throw marker; }); throw new AssertionError("callback"); }
        catch (RuntimeException expected) { check(expected == marker, "callback identity"); }
        int[] checkpoints = {0};
        nullable.summarize(encoded(0, 513), () -> checkpoints[0]++);
        check(checkpoints[0] == 4, "token checkpoint cadence");
        M3TokenRegexPlan.Effect effect = nullable.summarize(encoded(0, 1), null);
        long calls = nativeMode ? nativeCalls() : 0;
        check(effect.then(effect, null).matches(), "wide native composition");
        check(effect.repeated(256, null).matches(), "wide native repetition");
        if (nativeMode) check(nativeCalls() >= calls + 2, "actual production JNI dispatch");
    }

    public static void main(String[] args) {
        boolean nativeMode = args.length != 0;
        if (nativeMode) { System.load(args[0]); registerKernel(); }
        exhaustive();
        effects(new Random(0x3D0A_2026L));
        boundaries(nativeMode);
        System.out.println("M3TokenRegexPlanContract PASS checks=" + checks + " native=" + nativeMode);
    }
}
