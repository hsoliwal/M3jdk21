// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.staticuse;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * Differential driver of the static-use plan relocation (recipe static-use-plan-20261107; M3JDK-authored, test
 * scope). It walks the whole fact space of M3StaticUsePlan.evaluate (static sites 0 / 1 / 7, every Escape and Effect,
 * all 2^9 combinations of the seven observations and the two proofs: 18,432 facts) and compares every Plan and both
 * reference modes with a reference model restated from the documented contract (unproven ordering or owner lifetime
 * and unknown escape or effect are refused; an observable Java reference or a published escape materializes; a proven
 * local parent-bit effect lowers to the parent, a proven local read-only effect becomes a lazy handle; anything else
 * keeps original execution). The IntLoop entry bound is checked against a loop simulation (small trip counts) or a
 * BigInteger formula (large ones), including the refusal of an int wraparound at the condition exit, over seeded and
 * edge shapes; mustSampleAtGrowth against its comparison; the annotation retention and targets by reflection.
 *
 * <p>The dump prints decisions, reasons, counts and digests, never a class or package name, so the donor build (only
 * the package line replaced) and the relocated build print byte-identical dumps, also under -Xint. Every model
 * disagreement throws. Lines end with a single LF.</p>
 *
 * <p>usage: java M3StaticUsePlanDump [seeded loop shapes, default 200000]</p>
 */
public final class M3StaticUsePlanDump {
    private static final long SEED = 0x2026110753555350L;
    private static final long SIMULATION_LIMIT = 200_000L;

    private final PrintStream out;
    private long checks;

    private M3StaticUsePlanDump(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        int shapes = args.length > 0 ? Integer.parseInt(args[0]) : 200_000;
        PrintStream stdout = new PrintStream(new BufferedOutputStream(System.out, 1 << 16),
                false, StandardCharsets.UTF_8);
        M3StaticUsePlanDump dump = new M3StaticUsePlanDump(stdout);
        dump.factSpace();
        dump.loops(shapes);
        dump.growth();
        dump.annotations();
        dump.line("CHECKS " + dump.checks);
        dump.line("RESULT PASS");
        stdout.flush();
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    private void check(boolean truth, String what) {
        checks++;
        if (!truth) {
            out.flush();
            throw new AssertionError("model disagreement: " + what);
        }
    }

    /** Reference model of evaluate(), restated from the documented contract. */
    private static String[] model(int sites, M3StaticUsePlan.Escape escape, M3StaticUsePlan.Effect effect, int observed,
            boolean ordering, boolean lifetime) {
        if (!ordering || !lifetime) return new String[] {"REFUSE", "ordering-or-owner-lifetime-unproven"};
        if (escape == M3StaticUsePlan.Escape.UNKNOWN || effect == M3StaticUsePlan.Effect.UNKNOWN) {
            return new String[] {"REFUSE", "escape-or-effect-unproven"};
        }
        if (observed != 0 || escape == M3StaticUsePlan.Escape.PUBLISHED) {
            return new String[] {"MATERIALIZE", "observable-java-reference"};
        }
        if (effect == M3StaticUsePlan.Effect.PARENT_BITS_ONLY) return new String[] {"DIRECT_PARENT", "proven-parent-bit-effect"};
        if (effect == M3StaticUsePlan.Effect.READONLY_PARENT) return new String[] {"LAZY_HANDLE", "read-only-lazy-projection"};
        return new String[] {"MATERIALIZE", "external-effect-requires-original-execution"};
    }

    private static String modelMode(String decision, boolean shared) {
        return switch (decision) {
            case "REFUSE" -> "UNKNOWN";
            case "MATERIALIZE" -> "KEEP_JVM_REACHABILITY";
            default -> shared ? "OWNER_EDGE_COUNT_REQUIRED" : "NO_WRAPPER_COUNT";
        };
    }

    private void factSpace() {
        int[] sites = {0, 1, 7};
        int[] decisions = new int[M3StaticUsePlan.Decision.values().length];
        int[] modes = new int[M3StaticUsePlan.ReferenceMode.values().length];
        MessageDigest trace = sha256();
        int facts = 0;
        for (int site : sites) {
            for (M3StaticUsePlan.Escape escape : M3StaticUsePlan.Escape.values()) {
                for (M3StaticUsePlan.Effect effect : M3StaticUsePlan.Effect.values()) {
                    for (int bits = 0; bits < 1 << 9; bits++) {
                        boolean[] o = new boolean[9];
                        for (int i = 0; i < 9; i++) o[i] = (bits >>> i & 1) != 0;
                        M3StaticUsePlan.Facts f = new M3StaticUsePlan.Facts(site, escape, effect,
                                o[0], o[1], o[2], o[3], o[4], o[5], o[6], o[7], o[8]);
                        int observed = bits & 0x7f;
                        check(f.needsConcreteJavaObject() == (observed != 0), "needsConcreteJavaObject " + facts);
                        M3StaticUsePlan.Plan plan = M3StaticUsePlan.evaluate(f);
                        String[] expected = model(site, escape, effect, observed, o[7], o[8]);
                        check(plan.decision().name().equals(expected[0]) && plan.reason().equals(expected[1])
                                && plan.staticUseSites() == site && !plan.runtimeMemorySampleRequired(),
                                "plan " + facts + " " + plan.decision() + " " + plan.reason());
                        decisions[plan.decision().ordinal()]++;
                        for (boolean shared : new boolean[] {false, true}) {
                            M3StaticUsePlan.ReferenceMode mode = M3StaticUsePlan.referenceMode(f, shared);
                            check(mode.name().equals(modelMode(expected[0], shared)), "mode " + facts + " " + mode);
                            modes[mode.ordinal()]++;
                            trace.update((byte) mode.ordinal());
                        }
                        trace.update((byte) plan.decision().ordinal());
                        trace.update(plan.reason().getBytes(StandardCharsets.US_ASCII));
                        facts++;
                    }
                }
            }
        }
        int refusals = 0;
        try {
            new M3StaticUsePlan.Facts(-1, M3StaticUsePlan.Escape.UNKNOWN, M3StaticUsePlan.Effect.UNKNOWN,
                    false, false, false, false, false, false, false, false, false);
        } catch (IllegalArgumentException negative) {
            refusals++;
        }
        try {
            new M3StaticUsePlan.Facts(0, null, M3StaticUsePlan.Effect.UNKNOWN,
                    false, false, false, false, false, false, false, false, false);
        } catch (NullPointerException missing) {
            refusals++;
        }
        try {
            new M3StaticUsePlan.Plan(M3StaticUsePlan.Decision.REFUSE, -1, "x", false);
        } catch (IllegalArgumentException negative) {
            refusals++;
        }
        try {
            M3StaticUsePlan.evaluate(null);
        } catch (NullPointerException missing) {
            refusals++;
        }
        check(refusals == 4, "record refusals");
        line("facts=" + facts + " decisions=" + Arrays.toString(decisions) + " modes=" + Arrays.toString(modes)
                + " refusals=" + refusals + " trace=" + HexFormat.of().formatHex(trace.digest()));
    }

    private static boolean holds(M3StaticUsePlan.Relation relation, long i, long bound) {
        return switch (relation) {
            case LT -> i < bound;
            case LE -> i <= bound;
            case GT -> i > bound;
            case GE -> i >= bound;
        };
    }

    /** Expected entries, or -1 for a wraparound refusal; simulation for small trip counts, BigInteger otherwise. */
    private static long expected(int start, int step, M3StaticUsePlan.Relation relation, int bound) {
        BigInteger distance = step > 0 ? BigInteger.valueOf(bound).subtract(BigInteger.valueOf(start))
                : BigInteger.valueOf(start).subtract(BigInteger.valueOf(bound));
        BigInteger magnitude = BigInteger.valueOf(Math.abs((long) step));
        long count;
        if (distance.signum() >= 0 && distance.divide(magnitude).longValue() < SIMULATION_LIMIT) {
            count = 0;
            for (long i = start; holds(relation, i, bound); i += step) count++;
        } else {
            boolean inclusive = relation == M3StaticUsePlan.Relation.LE || relation == M3StaticUsePlan.Relation.GE;
            if (distance.signum() < 0 || distance.signum() == 0 && !inclusive) {
                count = 0;
            } else if (inclusive) {
                count = distance.divide(magnitude).longValue() + 1;
            } else {
                count = distance.add(magnitude).subtract(BigInteger.ONE).divide(magnitude).longValue();
            }
        }
        long exit = start + count * (long) step;
        return exit < Integer.MIN_VALUE || exit > Integer.MAX_VALUE ? -1 : count;
    }

    private void loops(int shapes) {
        SplittableRandom random = new SplittableRandom(SEED);
        int[] edges = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65_537, -2, -1, 0, 1, 2, 3, 65_536,
            Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        int[] steps = {1, 2, 3, 7, 1 << 16, Integer.MAX_VALUE, -1, -2, -3, -7, -(1 << 16), Integer.MIN_VALUE};
        MessageDigest trace = sha256();
        int evaluated = 0;
        int wraps = 0;
        int invalidShapes = 0;
        for (int n = 0; n < shapes + edges.length * edges.length * steps.length * 4; n++) {
            int start;
            int step;
            int bound;
            M3StaticUsePlan.Relation relation;
            if (n < edges.length * edges.length * steps.length * 4) {
                int k = n;
                relation = M3StaticUsePlan.Relation.values()[k % 4];
                k /= 4;
                step = steps[k % steps.length];
                k /= steps.length;
                bound = edges[k % edges.length];
                start = edges[k / edges.length];
            } else {
                relation = M3StaticUsePlan.Relation.values()[random.nextInt(4)];
                start = random.nextBoolean() ? random.nextInt(-5_000, 5_000) : random.nextInt();
                bound = random.nextBoolean() ? start + random.nextInt(-300_000, 300_000) : random.nextInt();
                int magnitude = random.nextBoolean() ? 1 + random.nextInt(9) : 1 + random.nextInt(Integer.MAX_VALUE);
                step = random.nextBoolean() ? magnitude : -magnitude;
            }
            boolean ascending = relation == M3StaticUsePlan.Relation.LT || relation == M3StaticUsePlan.Relation.LE;
            M3StaticUsePlan.IntLoop loop;
            try {
                loop = new M3StaticUsePlan.IntLoop(start, step, relation);
            } catch (IllegalArgumentException refused) {
                check(step == 0 || ascending != (step > 0), "loop refusal " + n);
                invalidShapes++;
                trace.update((byte) 'I');
                continue;
            }
            check(step != 0 && ascending == (step > 0), "loop accepted " + n);
            long want = expected(start, step, relation, bound);
            long got;
            try {
                got = loop.conditionBound(bound);
            } catch (ArithmeticException wrap) {
                got = -1;
                wraps++;
            }
            check(got == want, "conditionBound " + n + " start=" + start + " step=" + step + " " + relation
                    + " bound=" + bound + " got=" + got + " want=" + want);
            trace.update(longBytes(got));
            evaluated++;
        }
        line("loops shapes=" + (shapes + edges.length * edges.length * steps.length * 4) + " evaluated=" + evaluated
                + " wrapsRefused=" + wraps + " invalidShapes=" + invalidShapes + " trace="
                + HexFormat.of().formatHex(trace.digest()));
    }

    private void growth() {
        int[] values = {0, 1, 2, 15, 16, 17, 1 << 20, Integer.MAX_VALUE};
        int sampled = 0;
        for (int required : values) {
            for (int capacity : values) {
                boolean sample = M3StaticUsePlan.mustSampleAtGrowth(required, capacity);
                check(sample == required > capacity, "growth " + required + " " + capacity);
                if (sample) sampled++;
            }
        }
        int refused = 0;
        for (int[] pair : new int[][] {{-1, 0}, {0, -1}, {Integer.MIN_VALUE, Integer.MIN_VALUE}}) {
            try {
                M3StaticUsePlan.mustSampleAtGrowth(pair[0], pair[1]);
            } catch (IllegalArgumentException negative) {
                refused++;
            }
        }
        check(refused == 3, "growth refusals");
        line("growth pairs=" + values.length * values.length + " sampled=" + sampled + " refused=" + refused);
    }

    private void annotations() {
        Retention analyze = M3StaticUsePlan.Analyze.class.getAnnotation(Retention.class);
        Target analyzeTarget = M3StaticUsePlan.Analyze.class.getAnnotation(Target.class);
        Retention summary = M3StaticUsePlan.CompilerSummary.class.getAnnotation(Retention.class);
        Target summaryTarget = M3StaticUsePlan.CompilerSummary.class.getAnnotation(Target.class);
        check(analyzeTarget.value().length == 1 && analyzeTarget.value()[0] == ElementType.METHOD, "Analyze target");
        check(summaryTarget.value().length == 1 && summaryTarget.value()[0] == ElementType.TYPE, "CompilerSummary target");
        StringBuilder members = new StringBuilder();
        Arrays.stream(M3StaticUsePlan.CompilerSummary.class.getDeclaredMethods())
                .map(method -> method.getName() + ":" + method.getReturnType().getSimpleName())
                .sorted().forEach(member -> members.append(member).append(' '));
        line("annotations Analyze=" + analyze.value() + "/" + analyzeTarget.value()[0] + " CompilerSummary="
                + summary.value() + "/" + summaryTarget.value()[0] + " members=" + members.toString().trim());
    }

    private static byte[] longBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) bytes[i] = (byte) (value >>> (56 - 8 * i));
        return bytes;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }
}
