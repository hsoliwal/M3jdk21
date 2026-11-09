/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent unindexed row-by-row reference for exact mixed-type AND/OR masks. */
public final class SignalBooleanQuerySelfTest {
    private static final String SHA = "d".repeat(64);
    private static final long VERSION = 31L;
    private static long checks;
    private SignalBooleanQuerySelfTest() { }

    private static void check(boolean pass, String why) {
        checks++;
        if (!pass) throw new AssertionError("failed: " + why + " check=" + checks);
    }
    private static void reject(Class<? extends Throwable> kind, Runnable call) {
        checks++;
        try { call.run(); }
        catch (Throwable actual) {
            if (kind.isInstance(actual)) return;
            throw new AssertionError("wrong refusal type: " + actual, actual);
        }
        throw new AssertionError("missing exception " + kind.getSimpleName());
    }
    private static long[] mask(boolean[] valid) {
        long[] result = new long[(valid.length + 63) >>> 6];
        for (int i = 0; i < valid.length; i++) {
            if (valid[i]) result[i >>> 6] |= 1L << (i & 63);
        }
        return result;
    }

    private record Data(int[] ints, long[] longs, double[] doubles,
                        boolean[][] valid, SignalQualitySchema schema) { }

    private static Data data(int[] ints, long[] longs, double[] doubles, boolean[][] validity) {
        int rows = ints.length;
        var columns = new SignalColumns(rows,
                new SignalColumns.Column(SignalColumns.Kind.INTEGER,
                        SignalColumns.Origin.STATIC, 11L, false, mask(validity[0]),
                        ints, null, null),
                new SignalColumns.Column(SignalColumns.Kind.LONG,
                        SignalColumns.Origin.DOMAIN, 12L, false, mask(validity[1]),
                        null, longs, null),
                new SignalColumns.Column(SignalColumns.Kind.DOUBLE,
                        SignalColumns.Origin.PROFILED, 13L, false, mask(validity[2]),
                        null, null, doubles));
        var graph = SignalRelations.of(rows, new int[0], new int[0],
                new byte[0], new int[0]);
        var epochs = SignalDependencyEpochs.from(graph, false);
        var schema = SignalQualitySchema.declared(columns, epochs, VERSION, SHA,
                new SignalQualitySchema.Spec("javac.declarations",
                        SignalQualitySchema.Family.STRUCTURAL,
                        SignalQualitySchema.Phase.COMPILER,
                        SignalQualitySchema.Reliability.EXACT_FACT,
                        SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "count", "M3SourceSignals", Double.NaN, ""),
                new SignalQualitySchema.Spec("domain.ordinal",
                        SignalQualitySchema.Family.ORDERING,
                        SignalQualitySchema.Phase.INDEX,
                        SignalQualitySchema.Reliability.EXACT_FACT,
                        SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "ordinal", "DomainPolicy", Double.NaN, ""),
                new SignalQualitySchema.Spec("runtime.costEstimate",
                        SignalQualitySchema.Family.EXECUTION,
                        SignalQualitySchema.Phase.RUNTIME,
                        SignalQualitySchema.Reliability.ESTIMATE,
                        SignalQualitySchema.Invalidation.PROFILE_REFRESH,
                        "cycles", "Profiling", 0.7, ""));
        return new Data(ints, longs, doubles, validity, schema);
    }

    private static boolean matches(Data data, int row, int dim,
            SignalBooleanQuery.Predicate predicate) {
        if (predicate instanceof SignalBooleanQuery.Missing) return !data.valid[dim][row];
        if (!data.valid[dim][row]) return false;
        if (predicate instanceof SignalBooleanQuery.IntBetween range) {
            return Integer.compare(data.ints[row], range.lower()) >= 0
                    && Integer.compare(data.ints[row], range.upper()) <= 0;
        }
        if (predicate instanceof SignalBooleanQuery.LongBetween range) {
            return Long.compare(data.longs[row], range.lower()) >= 0
                    && Long.compare(data.longs[row], range.upper()) <= 0;
        }
        if (predicate instanceof SignalBooleanQuery.DoubleBetween range) {
            return Double.compare(data.doubles[row], range.lower()) >= 0
                    && Double.compare(data.doubles[row], range.upper()) <= 0;
        }
        throw new AssertionError("unexpected predicate");
    }

    private static int[] reference(Data source, int[] dimensions,
            SignalBooleanQuery.Predicate[] predicates, boolean union) {
        int[] found = new int[source.ints.length];
        int cursor = 0;
        for (int row = 0; row < source.ints.length; row++) {
            boolean accepted = !union;
            for (int i = 0; i < dimensions.length; i++) {
                boolean passed = matches(source, row, dimensions[i], predicates[i]);
                accepted = union ? accepted || passed : accepted && passed;
            }
            if (accepted) found[cursor++] = row;
        }
        return Arrays.copyOf(found, cursor);
    }

    private static void fixedCases() {
        int[] ints = {-10, 0, 0, 9, Integer.MIN_VALUE, Integer.MAX_VALUE, 4};
        long[] longs = {Long.MIN_VALUE, (1L << 53) + 1, (1L << 53),
                Long.MAX_VALUE, -1, 0, 42};
        double[] doubles = {Double.NEGATIVE_INFINITY, -0.0, 0.0, Double.NaN,
                Double.POSITIVE_INFINITY, -0.0, 3.5};
        boolean[][] validity = {
            {true, true, false, true, true, true, true},
            {true, true, true, true, false, true, true},
            {true, true, true, true, true, false, true}
        };
        Data data = data(ints, longs, doubles, validity);
        var plan = SignalBooleanQuery.build(data.schema, 1, 0, 2);
        check(plan.predicateCount() == 3 && plan.dimensionAt(1) == 0,
                "stable caller-declared column coordinates");
        var matchLong = new SignalBooleanQuery.LongBetween((1L << 53) + 1,
                (1L << 53) + 1);
        var missing = SignalBooleanQuery.Missing.INSTANCE;
        var plusZero = new SignalBooleanQuery.DoubleBetween(0.0, 0.0);
        var predicates = new SignalBooleanQuery.Predicate[]{matchLong, missing, plusZero};
        check(Arrays.equals(plan.and(VERSION, SHA, predicates), new int[0]),
                "non-overlapping conjunction");
        check(Arrays.equals(plan.or(VERSION, SHA, predicates),
                reference(data, new int[]{1, 0, 2}, predicates, true)),
                "mixed-type OR and missing union");
        var p2 = SignalBooleanQuery.build(data.schema, 0, 1);
        check(Arrays.equals(p2.and(VERSION, SHA,
                new SignalBooleanQuery.IntBetween(0, 0), matchLong), new int[]{1}),
                "exact full-width long AND int equality");
        check(Arrays.equals(SignalBooleanQuery.build(data.schema, 2).and(VERSION, SHA,
                new SignalBooleanQuery.DoubleBetween(-0.0, -0.0)), new int[]{1}),
                "signed negative zero selected only when present");
        check(Arrays.equals(SignalBooleanQuery.build(data.schema, 2).and(VERSION, SHA,
                new SignalBooleanQuery.DoubleBetween(Double.NaN, Double.NaN)), new int[]{3}),
                "Double.compare NaN total-order equivalence");
        check(Arrays.equals(SignalBooleanQuery.build(data.schema, 0).and(VERSION, SHA,
                SignalBooleanQuery.Missing.INSTANCE), new int[]{2}),
                "explicit missing not confused with zero");
        reject(IllegalArgumentException.class, () -> SignalBooleanQuery.build(data.schema));
        reject(IllegalArgumentException.class, () -> SignalBooleanQuery.build(data.schema, 1, 1));
        reject(IndexOutOfBoundsException.class, () -> SignalBooleanQuery.build(data.schema, 3));
        reject(IllegalArgumentException.class, () -> new SignalBooleanQuery.IntBetween(3, 2));
        reject(IllegalArgumentException.class, () -> new SignalBooleanQuery.LongBetween(3, 2));
        reject(IllegalArgumentException.class, () -> new SignalBooleanQuery.DoubleBetween(+0.0, -0.0));
        reject(IllegalArgumentException.class, () -> plan.and(VERSION, SHA,
                new SignalBooleanQuery.IntBetween(0, 1), missing, plusZero));
        reject(IllegalArgumentException.class, () -> plan.or(VERSION, SHA, missing));
        reject(NullPointerException.class, () -> plan.and(VERSION, SHA,
                matchLong, null, plusZero));
        reject(IllegalStateException.class, () -> plan.or(VERSION + 1, SHA, predicates));
        reject(IllegalStateException.class, () -> plan.or(VERSION, "0".repeat(64), predicates));
        int[] old = plan.or(VERSION, SHA, predicates);
        old[0] = Integer.MAX_VALUE;
        check(!Arrays.equals(old, plan.or(VERSION, SHA, predicates)),
                "defensive result array ownership");
        data.schema.invalidationEpochsOwner().invalidateAll();
        reject(IllegalStateException.class, () -> plan.and(VERSION, SHA, predicates));
        reject(IllegalStateException.class, () -> plan.or(VERSION, SHA, predicates));
    }

    private static void fuzzCases() {
        Random random = new Random(0x5349474e414c3231L);
        for (int trial = 0; trial < 450; trial++) {
            int size = random.nextInt(165);
            int[] ints = new int[size];
            long[] longs = new long[size];
            double[] doubles = new double[size];
            boolean[][] validity = new boolean[3][size];
            for (int row = 0; row < size; row++) {
                ints[row] = random.nextInt(31) - 15;
                longs[row] = random.nextInt(12) == 0
                        ? (random.nextBoolean() ? Long.MAX_VALUE : Long.MIN_VALUE)
                        : (1L << 53) + random.nextInt(251) - 125;
                doubles[row] = switch (random.nextInt(12)) {
                    case 0 -> Double.NaN;
                    case 1 -> Double.NEGATIVE_INFINITY;
                    case 2 -> Double.POSITIVE_INFINITY;
                    case 3 -> -0.0;
                    case 4 -> +0.0;
                    default -> (random.nextInt(23) - 11) / 4.0;
                };
                for (int d = 0; d < 3; d++) validity[d][row] = random.nextInt(5) != 0;
            }
            var input = data(ints, longs, doubles, validity);
            int[] dimensions = {0, 1, 2};
            var plan = SignalBooleanQuery.build(input.schema, dimensions);
            for (int q = 0; q < 31; q++) {
                SignalBooleanQuery.Predicate[] predicates = new SignalBooleanQuery.Predicate[3];
                for (int dim = 0; dim < 3; dim++) {
                    if (random.nextInt(9) == 0) {
                        predicates[dim] = SignalBooleanQuery.Missing.INSTANCE;
                    } else if (dim == 0) {
                        int a = random.nextInt(41) - 20, b = random.nextInt(41) - 20;
                        predicates[dim] = new SignalBooleanQuery.IntBetween(Math.min(a,b), Math.max(a,b));
                    } else if (dim == 1) {
                        long a = (1L << 53) + random.nextInt(301) - 150;
                        long b = (1L << 53) + random.nextInt(301) - 150;
                        predicates[dim] = new SignalBooleanQuery.LongBetween(Math.min(a,b), Math.max(a,b));
                    } else {
                        double a = random.nextInt(9) == 0 ? Double.NaN : (random.nextInt(15)-7)*0.5;
                        double b = random.nextInt(9) == 0 ? Double.NaN : (random.nextInt(15)-7)*0.5;
                        predicates[dim] = Double.compare(a,b) <= 0
                                ? new SignalBooleanQuery.DoubleBetween(a,b)
                                : new SignalBooleanQuery.DoubleBetween(b,a);
                    }
                }
                int[] expectedAnd = reference(input, dimensions, predicates, false);
                int[] expectedOr = reference(input, dimensions, predicates, true);
                check(Arrays.equals(expectedAnd, plan.and(VERSION, SHA, predicates)),
                        "AND differential trial="+trial);
                check(Arrays.equals(expectedOr, plan.or(VERSION, SHA, predicates)),
                        "OR differential trial="+trial);
            }
        }
    }

    /** Deliberately independent, unindexed per-row stack oracle. */
    private static int[] postfixReference(Data input, int[] planDimensions,
            SignalBooleanQuery.Step[] instructions) {
        int rows = input.ints.length;
        boolean[][] stack = new boolean[instructions.length][];
        int top = 0;
        for (var instruction : instructions) {
            if (instruction instanceof SignalBooleanQuery.Term term) {
                boolean[] present = new boolean[rows];
                for (int row = 0; row < rows; row++) {
                    present[row] = matches(input, row, planDimensions[term.slot()], term.predicate());
                }
                stack[top++] = present;
            } else if (instruction instanceof SignalBooleanQuery.Logic logic) {
                switch (logic) {
                    case ALL, NONE -> {
                        boolean[] present = new boolean[rows];
                        Arrays.fill(present, logic == SignalBooleanQuery.Logic.ALL);
                        stack[top++] = present;
                    }
                    case NOT -> {
                        for (int row = 0; row < rows; row++) stack[top-1][row] = !stack[top-1][row];
                    }
                    case AND, OR -> {
                        for (int row = 0; row < rows; row++) {
                            stack[top-2][row] = logic == SignalBooleanQuery.Logic.AND
                                    ? stack[top-2][row] && stack[top-1][row]
                                    : stack[top-2][row] || stack[top-1][row];
                        }
                        top--;
                    }
                }
            } else throw new AssertionError("unknown postfix expression kind");
        }
        if (top != 1) throw new AssertionError("reference postfix stack was not singleton");
        int[] result = new int[rows];
        int size = 0;
        for (int row = 0; row < rows; row++) if (stack[0][row]) result[size++] = row;
        return Arrays.copyOf(result, size);
    }

    private static void fixedPostfixCases() {
        int[] ints = {0, 1, 2, 3, 4, 5, 6};
        long[] longs = {(1L << 53) + 1, Long.MIN_VALUE, Long.MAX_VALUE,
                (1L << 53), (1L << 53) + 1, -20, 0};
        double[] doubles = {Double.NaN, -0.0, +0.0, -0.0,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.25};
        boolean[][] valid = {{true, true, false, true, true, true, true},
                {true, true, true, true, false, true, true},
                {true, true, true, false, true, true, true}};
        Data data = data(ints, longs, doubles, valid);
        var query = SignalBooleanQuery.build(data.schema, 0, 1, 2);
        var termA = new SignalBooleanQuery.Term(0, new SignalBooleanQuery.IntBetween(1, 4));
        var termB = new SignalBooleanQuery.Term(1, new SignalBooleanQuery.LongBetween(
                (1L << 53) + 1, Long.MAX_VALUE));
        var termC = new SignalBooleanQuery.Term(2,
                new SignalBooleanQuery.DoubleBetween(-0.0, -0.0));
        SignalBooleanQuery.Step[] expression = {termA, termB, termC,
                SignalBooleanQuery.Logic.NOT, SignalBooleanQuery.Logic.AND,
                SignalBooleanQuery.Logic.OR};
        var compiled = query.compile(expression);
        check(compiled.instructionCount() == 6 && compiled.maximumStackDepth() == 3,
                "postfix compiled primitive opcode and stack layout");
        check(Arrays.equals(compiled.evaluate(VERSION, SHA),
                        postfixReference(data, new int[] {0, 1, 2}, expression)),
                "original nested OR(TERM,AND(TERM,NOT(TERM))) matches independent oracle");
        expression[0] = SignalBooleanQuery.Logic.NONE;
        check(Arrays.equals(compiled.evaluate(VERSION, SHA),
                postfixReference(data, new int[]{0,1,2},new SignalBooleanQuery.Step[] {
                        termA, termB, termC, SignalBooleanQuery.Logic.NOT,
                        SignalBooleanQuery.Logic.AND, SignalBooleanQuery.Logic.OR})),
                "compiled bytecode has no caller-owned instruction array alias");
        check(Arrays.equals(query.compile(SignalBooleanQuery.Logic.ALL).evaluate(VERSION, SHA),
                new int[]{0,1,2,3,4,5,6}), "constant ALL clipped to original seven rows");
        check(Arrays.equals(query.compile(SignalBooleanQuery.Logic.NONE).evaluate(VERSION, SHA),
                new int[0]), "constant NONE is empty");
        check(Arrays.equals(query.compile(SignalBooleanQuery.Logic.NONE,
                SignalBooleanQuery.Logic.NOT).evaluate(VERSION, SHA),
                new int[]{0,1,2,3,4,5,6}), "NOT masks absent tail bits");
        check(Arrays.equals(query.compile(termA, SignalBooleanQuery.Logic.NOT,
                        SignalBooleanQuery.Logic.NOT).evaluate(VERSION, SHA),
                query.compile(termA).evaluate(VERSION,SHA)), "double complement preserves exact membership");
        check(Arrays.equals(query.compile(termA, termA,
                        SignalBooleanQuery.Logic.AND).evaluate(VERSION,SHA),
                query.compile(termA).evaluate(VERSION,SHA)),
                "repeated query term allowed without stored duplicate index");
        reject(IllegalArgumentException.class, () -> query.compile());
        reject(IllegalArgumentException.class, () -> query.compile(SignalBooleanQuery.Logic.NOT));
        reject(IllegalArgumentException.class, () -> query.compile(SignalBooleanQuery.Logic.AND));
        reject(IllegalArgumentException.class, () -> query.compile(termA, termB));
        reject(IllegalArgumentException.class, () -> query.compile(termA, termB,
                SignalBooleanQuery.Logic.AND, SignalBooleanQuery.Logic.OR));
        reject(IndexOutOfBoundsException.class, () -> query.compile(
                new SignalBooleanQuery.Term(3, SignalBooleanQuery.Missing.INSTANCE)));
        reject(IllegalArgumentException.class, () -> query.compile(
                new SignalBooleanQuery.Term(0, new SignalBooleanQuery.LongBetween(0,1))));
        reject(NullPointerException.class, () -> query.compile((SignalBooleanQuery.Step)null));
        reject(NullPointerException.class, () -> new SignalBooleanQuery.Term(0, null));
        reject(IllegalArgumentException.class, () -> query.compile(
                new SignalBooleanQuery.Step[4097]));
        reject(IllegalStateException.class, () -> compiled.evaluate(VERSION+1, SHA));
        reject(IllegalStateException.class, () -> compiled.evaluate(VERSION, "f".repeat(64)));
        data.schema.invalidationEpochsOwner().invalidateAll();
        reject(IllegalStateException.class, () -> compiled.evaluate(VERSION, SHA));
        reject(IllegalStateException.class, () -> query.compile(termA));
    }

    private static void randomizedPostfixCases() {
        Random random = new Random(0x4d33504f53544658L);
        for (int trial = 0; trial < 400; trial++) {
            int size = switch (trial % 8) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 63;
                case 3 -> 64;
                case 4 -> 65;
                case 5 -> 127;
                case 6 -> 128;
                default -> random.nextInt(150);
            };
            int[] ints = new int[size];
            long[] longs = new long[size];
            double[] doubles = new double[size];
            boolean[][] valid = new boolean[3][size];
            for (int row = 0; row < size; row++) {
                ints[row] = random.nextInt(29) - 14;
                longs[row] = random.nextInt(20) == 0 ? Long.MIN_VALUE
                        : (1L << 53) + random.nextInt(101) - 50;
                doubles[row] = switch (random.nextInt(11)) {
                    case 0 -> Double.NaN;
                    case 1 -> Double.NEGATIVE_INFINITY;
                    case 2 -> Double.POSITIVE_INFINITY;
                    case 3 -> -0.0;
                    case 4 -> +0.0;
                    default -> (random.nextInt(45) - 22) / 4.0;
                };
                for (int d = 0; d < 3; d++) valid[d][row] = random.nextInt(6) != 0;
            }
            Data sample = data(ints, longs, doubles, valid);
            int[] chosen = {0,1,2};
            var query = SignalBooleanQuery.build(sample.schema, chosen);
            for (int attempt = 0; attempt < 28; attempt++) {
                List<SignalBooleanQuery.Step> steps = new ArrayList<>();
                int depth = 0;
                int operations = 1 + random.nextInt(35);
                for (int op = 0; op < operations; op++) {
                    int action = random.nextInt(9);
                    if (depth < 2 || action <= 4) {
                        if (action == 0) steps.add(SignalBooleanQuery.Logic.ALL);
                        else if (action == 1) steps.add(SignalBooleanQuery.Logic.NONE);
                        else {
                            int dim = random.nextInt(3);
                            SignalBooleanQuery.Predicate p;
                            if (random.nextInt(9) == 0) {
                                p = SignalBooleanQuery.Missing.INSTANCE;
                            } else if (dim == 0) {
                                int a = random.nextInt(37)-18,b = random.nextInt(37)-18;
                                p = new SignalBooleanQuery.IntBetween(Math.min(a,b),Math.max(a,b));
                            } else if (dim == 1) {
                                long a = (1L << 53) + random.nextInt(125) - 62;
                                long b = (1L << 53) + random.nextInt(125) - 62;
                                p = new SignalBooleanQuery.LongBetween(Math.min(a,b),Math.max(a,b));
                            } else {
                                double a = random.nextInt(12) == 0 ? Double.NaN
                                        : (random.nextInt(33)-16)/4.0;
                                double b = random.nextInt(12) == 0 ? Double.NaN
                                        : (random.nextInt(33)-16)/4.0;
                                p = new SignalBooleanQuery.DoubleBetween(
                                        Double.compare(a,b)<=0?a:b,Double.compare(a,b)<=0?b:a);
                            }
                            steps.add(new SignalBooleanQuery.Term(dim,p));
                        }
                        depth++;
                    } else if (action == 5) {
                        steps.add(SignalBooleanQuery.Logic.NOT);
                    } else {
                        steps.add(random.nextBoolean()
                                ? SignalBooleanQuery.Logic.AND : SignalBooleanQuery.Logic.OR);
                        depth--;
                    }
                }
                while (depth > 1) {
                    steps.add(random.nextBoolean()
                            ? SignalBooleanQuery.Logic.AND : SignalBooleanQuery.Logic.OR);
                    depth--;
                }
                SignalBooleanQuery.Step[] program = steps.toArray(SignalBooleanQuery.Step[]::new);
                int[] expected = postfixReference(sample, chosen, program);
                var compiled = query.compile(program);
                int[] actual = compiled.evaluate(VERSION,SHA);
                check(Arrays.equals(expected,actual),"postfix independent row scan trial="+trial);
                check(Arrays.equals(actual,compiled.evaluate(VERSION,SHA)),"repeat deterministic program");
                check(actual.length == 0 || actual[actual.length-1] < size,
                        "complement never leaks beyond row capacity");
            }
        }
    }

    public static void main(String[] args) {
        fixedCases();
        fuzzCases();
        fixedPostfixCases();
        randomizedPostfixCases();
        System.out.println("M3_SIGNAL_BOOLEAN_QUERY_PASS checks=" + checks);
    }
}
