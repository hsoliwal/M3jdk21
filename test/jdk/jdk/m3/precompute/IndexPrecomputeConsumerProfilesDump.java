// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Differential dump and reference model (gates 3 and 4 of recipe/consumer-profiles-20261107/differential.sh). Drives
 * the consumer-profile class named by args[0], the capability enum named by args[1] and the workload class named by
 * args[2] by reflection, so that the donor classes and the relocated classes run the same driver.
 *
 * <p>Lines: one CAP line per capability (name, id, query cost); one PROFILE line per consumer (its weights in
 * capability order); one RUNTIME line (the languageRuntime policy); one REFUSE line per refusal case (null consumer,
 * empty shares, zero, -1 and 1025 shares, negative build penalty, null key, null share); then CASES seeded BLEND lines
 * (each consumer's share is 0 with probability 1/3, else 1..1024; an all-zero draw is kept; every 64th case asks for
 * build penalty -1): the build penalty, the count, sum and 64-bit digest of the non-zero weights in capability order,
 * or the exception class and message. Every result is checked against a reference model: profile scores in 1..200,
 * forConsumer agreeing with weights, immutable profile and workload maps, and each blended weight equal to the sum of
 * share x profile score computed independently in long arithmetic, with zero weights absent from weights(). A drift
 * aborts the run. The last line totals cases, reference comparisons, refusals and a digest of every line.
 * Usage: {@code java -cp classes;test-classes jdk.m3.precompute.IndexPrecomputeConsumerProfilesDump PROFILES_FQN
 * CAPABILITY_FQN WORKLOAD_FQN [CASES]}
 */
//FLLDCIM: TFG - dump driver from the template M3IntRingBufferDump (recipe m3/arrays/recipe/int-ring-buffer-20261106): reflective, serves donor and relocated classes
public final class IndexPrecomputeConsumerProfilesDump {
    private static final long SEED = 0x5052_4F46_494C_4553L;
    private static final int DEFAULT_CASES = 6_000;
    private static final int MAX_SHARE = 1024;
    private static final int MAX_SCORE = 200;
    private static final int PENALTIES = 8;
    private static final int NEGATIVE_EVERY = 64;
    private static final int ZERO_ODDS = 3;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long FNV_BASIS = 0xcbf29ce484222325L;
    private static final String REFUSED = IllegalArgumentException.class.getName() + ": ";
    private static final String MISSING = NullPointerException.class.getName() + ": ";

    private final Object[] consumers;
    private final Object[] capabilities;
    private final Method forConsumer;
    private final Method weights;
    private final Method blend;
    private final Method id;
    private final Method queryCost;
    private final Method weight;
    private final Method buildPenalty;
    private final Method workloadWeights;
    private final Method languageRuntime;
    private final PrintStream out;
    private long references;
    private long refusals;
    private long digest = FNV_BASIS;

    private IndexPrecomputeConsumerProfilesDump(Class<?> profiles, Class<?> capability, Class<?> workload,
            PrintStream out) throws ReflectiveOperationException {
        Class<?> consumer = Class.forName(profiles.getName() + "$Consumer", true, profiles.getClassLoader());
        this.consumers = consumer.getEnumConstants();
        this.capabilities = capability.getEnumConstants();
        this.forConsumer = profiles.getMethod("forConsumer", consumer);
        this.weights = profiles.getMethod("weights", consumer);
        this.blend = profiles.getMethod("blend", Map.class, int.class);
        this.id = capability.getMethod("id");
        this.queryCost = capability.getMethod("queryCost");
        this.weight = workload.getMethod("weight", capability);
        this.buildPenalty = workload.getMethod("buildPenalty");
        this.workloadWeights = workload.getMethod("weights");
        this.languageRuntime = workload.getMethod("languageRuntime");
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        int cases = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_CASES;
        run(Class.forName(args[0]), Class.forName(args[1]), Class.forName(args[2]), cases, out);
        out.flush();
    }

    /** Runs the corpus; returns {cases, reference comparisons, refusals}. */
    static long[] run(Class<?> profiles, Class<?> capability, Class<?> workload, int cases, PrintStream out)
            throws ReflectiveOperationException {
        IndexPrecomputeConsumerProfilesDump dump = new IndexPrecomputeConsumerProfilesDump(profiles, capability, workload, out);
        dump.capabilityLines();
        dump.profileLines();
        dump.runtimeLine();
        dump.refusalLines();
        Random random = new Random(SEED);
        for (int i = 0; i < cases; i++) {
            dump.blendLine(i, random);
        }
        out.println("TOTAL cases=" + cases + " references=" + dump.references + " refusals=" + dump.refusals
                + " digest=" + Long.toHexString(dump.digest));
        return new long[] {cases, dump.references, dump.refusals};
    }

    static PrintStream discard() {
        return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    /** Every capability id is non-empty and unique. */
    private void capabilityLines() throws ReflectiveOperationException {
        Set<String> ids = new HashSet<>();
        for (Object capability : capabilities) {
            String text = (String) id.invoke(capability);
            expect(!text.isEmpty() && ids.add(text), "id " + capability);
            emit("CAP " + capability + " id=" + text + " cost=" + queryCost.invoke(capability));
        }
    }

    private void profileLines() throws ReflectiveOperationException {
        expect(consumers.length == 7, "consumer count " + consumers.length);
        for (Object consumer : consumers) {
            Map<?, ?> profile = (Map<?, ?>) weights.invoke(null, consumer);
            Object single = forConsumer.invoke(null, consumer);
            expect(!profile.isEmpty() && (int) buildPenalty.invoke(single) == 1, "profile " + consumer);
            expect(immutable(profile) && immutable((Map<?, ?>) workloadWeights.invoke(single)), "immutable " + consumer);
            StringBuilder line = new StringBuilder("PROFILE ").append(consumer);
            for (Object capability : capabilities) {
                int score = score(profile, capability);
                expect(score >= 0 && score <= MAX_SCORE && (profile.containsKey(capability) == score > 0), "score");
                expect((int) weight.invoke(single, capability) == score, "forConsumer " + consumer + " " + capability);
                if (score > 0) {
                    line.append(' ').append(capability).append('=').append(score);
                }
            }
            emit(line.toString());
        }
    }

    private void runtimeLine() throws ReflectiveOperationException {
        Object runtime = languageRuntime.invoke(null);
        StringBuilder line = new StringBuilder("RUNTIME penalty=").append(buildPenalty.invoke(runtime));
        for (Object capability : capabilities) {
            int value = (int) weight.invoke(runtime, capability);
            expect(value == 100 || value == 70 || value == 45 || value == 25, "runtime tier " + capability);
            line.append(' ').append(value);
        }
        emit(line.toString());
    }

    private void refusalLines() throws ReflectiveOperationException {
        refuse("forConsumer(null)", MISSING, forConsumer, (Object) null);
        refuse("blend(empty)", REFUSED, blend, Map.of(), 1);
        refuse("blend(zero)", REFUSED, blend, Map.of(consumers[0], 0), 1);
        refuse("blend(-1)", REFUSED, blend, Map.of(consumers[0], -1), 1);
        refuse("blend(1025)", REFUSED, blend, Map.of(consumers[0], MAX_SHARE + 1), 1);
        refuse("blend(penalty -1)", REFUSED, blend, Map.of(consumers[0], 1), -1);
        Map<Object, Integer> invalid = new LinkedHashMap<>();
        invalid.put(null, 1);
        refuse("blend(null consumer)", MISSING, blend, invalid, 1);
        invalid.clear();
        invalid.put(consumers[0], null);
        refuse("blend(null share)", MISSING, blend, invalid, 1);
    }

    private void refuse(String label, String expected, Method method, Object... args) throws IllegalAccessException {
        String actual = outcome(method, args);
        expect(actual.startsWith(expected), label + " -> " + actual);
        emit("REFUSE " + label + " " + actual);
    }

    private void blendLine(int index, Random random) throws ReflectiveOperationException {
        Map<Object, Integer> shares = new LinkedHashMap<>();
        long[] expected = new long[capabilities.length];
        for (Object consumer : consumers) {
            int share = random.nextInt(ZERO_ODDS) == 0 ? 0 : 1 + random.nextInt(MAX_SHARE);
            shares.put(consumer, share);
            accumulate(expected, (Map<?, ?>) weights.invoke(null, consumer), share);
        }
        int penalty = index % NEGATIVE_EVERY == NEGATIVE_EVERY - 1 ? -1 : random.nextInt(PENALTIES);
        boolean demand = shares.values().stream().anyMatch(share -> share > 0);
        String result = demand && penalty >= 0 ? describe(blend.invoke(null, shares, penalty), expected, penalty)
                : refusedBlend(shares, penalty);
        emit("BLEND " + index + " shares=" + shares.values() + " penalty=" + penalty + " " + result);
    }

    private String refusedBlend(Map<Object, Integer> shares, int penalty) throws IllegalAccessException {
        String actual = outcome(blend, shares, penalty);
        expect(actual.startsWith(REFUSED), "refused blend -> " + actual);
        return actual;
    }

    private String describe(Object workload, long[] expected, int penalty) throws ReflectiveOperationException {
        Map<?, ?> vector = (Map<?, ?>) workloadWeights.invoke(workload);
        expect((int) buildPenalty.invoke(workload) == penalty && immutable(vector), "penalty/immutable");
        long sum = 0;
        long weightsDigest = FNV_BASIS;
        int nonZero = 0;
        for (int c = 0; c < capabilities.length; c++) {
            int actual = (int) weight.invoke(workload, capabilities[c]);
            expect(actual == expected[c] && vector.containsKey(capabilities[c]) == actual > 0, "weight " + capabilities[c]);
            if (actual > 0) {
                nonZero++;
                sum += actual;
                weightsDigest = fnv(weightsDigest, capabilities[c] + "=" + actual);
            }
        }
        return "nonzero=" + nonZero + " sum=" + sum + " weights=" + Long.toHexString(weightsDigest);
    }

    private void accumulate(long[] expected, Map<?, ?> profile, int share) {
        for (int c = 0; c < capabilities.length; c++) {
            expected[c] += (long) share * score(profile, capabilities[c]);
        }
    }

    private static int score(Map<?, ?> profile, Object capability) {
        Object value = profile.get(capability);
        return value == null ? 0 : (Integer) value;
    }

    private static boolean immutable(Map<?, ?> map) {
        try {
            map.clear();
            return false;
        } catch (UnsupportedOperationException expected) {
            return true;
        }
    }

    private String outcome(Method method, Object... args) throws IllegalAccessException {
        try {
            return String.valueOf(method.invoke(null, args));
        } catch (InvocationTargetException failure) {
            refusals++;
            Throwable cause = failure.getCause();
            return cause.getClass().getName() + ": " + cause.getMessage();
        }
    }

    private void expect(boolean holds, String what) {
        references++;
        if (!holds) {
            throw new AssertionError("reference drift: " + what);
        }
    }

    private void emit(String line) {
        out.println(line);
        digest = fnv(fnv(digest, line), "\n");
    }

    private static long fnv(long hash, String text) {
        long h = hash;
        for (int i = 0; i < text.length(); i++) {
            h = (h ^ text.charAt(i)) * FNV_PRIME;
        }
        return h;
    }
}
