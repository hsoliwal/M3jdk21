// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Differential dump and reference model (gates 3 and 4 of recipe/planner-family-20261107/differential.sh). The classes
 * are reached by reflection from a package and a type-name prefix (args[0], args[1]: the donor package with an empty
 * prefix, or jdk.m3.precompute with M3), so the donor classes and the relocated classes run the same driver.
 *
 * <p>Per case: one PLAN line (a seeded candidate catalogue of 0..40 capabilities with retained bytes, build cost,
 * reuse count, dependencies and mandatory flags, a seeded workload and byte budget, sometimes a duplicate, a missing
 * dependency or a negative budget), every fourth case a TUPLE line (a seeded frozen tuple index of arity 1..4 with
 * duplicate and shape refusals, 200 queries through the scalar getters, the batch tuple and pair lookups and the Java
 * accelerator), every sixteenth case a HASH line (the domain-separated string hash). Root hashes of tuple indexes are
 * printed as {@code H:<hex>} (their domain literal is renamed by the recipe, so a raw-donor comparison masks them).
 *
 * <p>Reference model (a drift aborts): up to 12 candidates, a brute-force search over every subset (mandatory
 * candidates present, dependencies closed, bytes within budget; best score, then fewer bytes, then the smaller set
 * text) that must equal the recommendation, or the refusal when no subset fits; 13..20 candidates, a valid
 * dependency-closed exact recommendation with recomputed bytes and score, refused exactly when the mandatory closure
 * exceeds the budget; more than 20, the documented greedy pass recomputed (mandatory closures, then the densest
 * positive closure that fits, exact cross-product density and the planner's tie-breaks) that must equal it;
 * tuple indexes against a HashMap of the rows (first duplicate row, capacity, retained bytes, every lookup, the root
 * hash recomputed with MessageDigest from the domain prefix args[2]); string hashes recomputed the same way.
 * Usage: {@code java -cp ... jdk.m3.precompute.M3IndexPrecomputePlannerDump PACKAGE PREFIX TUPLE_DOMAIN [CASES]}
 */
//FLLDCIM: TFG - dump driver from the template M3IndexPrecomputeConsumerProfilesDump (recipe consumer-profiles-20261107): reflective, serves donor and relocated classes
public final class M3IndexPrecomputePlannerDump {
    private static final long SEED = 0x504C_414E_4E45_5253L;
    private static final int DEFAULT_CASES = 4_000;
    private static final int BRUTE_LIMIT = 12;
    private static final int EXACT_LIMIT = 20;
    private static final int MAX_CANDIDATES = 40;
    private static final int QUERIES = 200;
    private static final int MISSING = -7;
    private static final int NO_THRESHOLD = Integer.MAX_VALUE;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long FNV_BASIS = 0xcbf29ce484222325L;
    private static final String NONE_FITS = "java.lang.IllegalStateException: no valid precompute subset fits the byte budget";
    private static final String MANDATORY = "java.lang.IllegalStateException: mandatory precompute capabilities exceed byte budget";
    private static final String REFUSED = IllegalArgumentException.class.getName() + ": ";

    private final Object[] capabilities;
    private final Method capabilityId;
    private final Constructor<?> candidate;
    private final Object planner;
    private final Method recommend;
    private final Method builder;
    private final Method weight;
    private final Method buildPenalty;
    private final Method build;
    private final Method workloadWeight;
    private final Method penaltyOf;
    private final Method[] recommendation = new Method[4];
    private final Class<?> index;
    private final Method freeze;
    private final Method rootHash;
    private final Method lookupTuples;
    private final Method lookupPairs;
    private final Object javaAccelerator;
    private final Method acceleratePairs;
    private final Method sha256;
    private final String tupleDomain;
    private final PrintStream out;
    private long references;
    private long refusals;
    private long digest = FNV_BASIS;

    private M3IndexPrecomputePlannerDump(String pkg, String prefix, String tupleDomain, PrintStream out)
            throws ReflectiveOperationException {
        Class<?> capability = type(pkg, prefix, "IndexPrecomputeCapability");
        Class<?> workload = type(pkg, prefix, "IndexPrecomputeWorkload");
        Class<?> workloadBuilder = type(pkg, prefix, "IndexPrecomputeWorkload$Builder");
        Class<?> rec = type(pkg, prefix, "IndexPrecomputeRecommendation");
        this.capabilities = capability.getEnumConstants();
        this.capabilityId = capability.getMethod("id");
        this.candidate = type(pkg, prefix, "IndexPrecomputeCandidate")
                .getConstructor(capability, long.class, long.class, long.class, Set.class, boolean.class);
        Class<?> plannerType = type(pkg, prefix, "IndexPrecomputePlanner");
        this.planner = plannerType.getConstructor().newInstance();
        this.recommend = plannerType.getMethod("recommend", List.class, long.class, workload);
        this.builder = workload.getMethod("builder");
        this.weight = workloadBuilder.getMethod("weight", capability, int.class);
        this.buildPenalty = workloadBuilder.getMethod("buildPenalty", int.class);
        this.build = workloadBuilder.getMethod("build");
        this.workloadWeight = workload.getMethod("weight", capability);
        this.penaltyOf = workload.getMethod("buildPenalty");
        String[] parts = {"capabilities", "retainedBytes", "score", "exactSearch"};
        for (int i = 0; i < parts.length; i++) {
            recommendation[i] = rec.getMethod(parts[i]);
        }
        this.index = type(pkg, prefix, "FrozenLongTupleIndex");
        this.freeze = index.getMethod("freeze", capability, int.class, long[].class, int[].class);
        this.rootHash = index.getMethod("rootHash");
        Class<?> ops = type(pkg, prefix, "NativePrecomputeOps");
        this.lookupTuples = ops.getMethod("lookupTuples", index, long[].class, int.class, int.class);
        this.lookupPairs = ops.getMethod("lookupPairs", index, long[].class, long[].class, int.class, int.class);
        Class<?> accelerator = type(pkg, prefix, "JavaIndexPrecomputeAccelerator");
        this.javaAccelerator = accelerator.getField("INSTANCE").get(null);
        this.acceleratePairs = accelerator.getMethod("lookupPairs", index, long[].class, long[].class, int.class, int[].class);
        this.sha256 = type(pkg, prefix, "PrecomputeHashes").getDeclaredMethod("sha256", String.class, String[].class);
        this.sha256.setAccessible(true);
        this.tupleDomain = tupleDomain;
        this.out = out;
    }

    private static Class<?> type(String pkg, String prefix, String name) throws ClassNotFoundException {
        return Class.forName(pkg + "." + prefix + name);
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        int cases = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_CASES;
        run(args[0], args[1], args[2], cases, out);
        out.flush();
    }

    /** Runs the corpus; returns {cases, reference comparisons, refusals}. */
    static long[] run(String pkg, String prefix, String tupleDomain, int cases, PrintStream out)
            throws ReflectiveOperationException {
        M3IndexPrecomputePlannerDump dump = new M3IndexPrecomputePlannerDump(pkg, prefix, tupleDomain, out);
        dump.cycleProbe();
        Random random = new Random(SEED);
        for (int i = 0; i < cases; i++) {
            dump.planCase(i, random);
            if ((i & 3) == 0) {
                dump.tupleCase(i, random);
            }
            if ((i & 15) == 0) {
                dump.hashCase(i, random);
            }
        }
        out.println("TOTAL cases=" + cases + " references=" + dump.references + " refusals=" + dump.refusals
                + " digest=" + Long.toHexString(dump.digest));
        return new long[] {cases, dump.references, dump.refusals};
    }

    static PrintStream discard() {
        return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- planner

    /** One generated catalogue: parallel arrays over the candidate list, plus the injected refusal. */
    private record Catalogue(int[] caps, long[] bytes, long[] cost, long[] reuse, int[][] deps, boolean[] mandatory,
            String injection) { }

    private void planCase(int caseIndex, Random random) throws ReflectiveOperationException {
        int kind = random.nextInt(100);
        int n = kind < 70 ? random.nextInt(BRUTE_LIMIT + 1)
                : kind < 88 ? BRUTE_LIMIT + 1 + random.nextInt(EXACT_LIMIT - BRUTE_LIMIT)
                : EXACT_LIMIT + 1 + random.nextInt(MAX_CANDIDATES - EXACT_LIMIT);
        Catalogue catalogue = catalogue(n, random);
        Object workload = workload(random);
        long total = Arrays.stream(catalogue.bytes()).sum();
        long budget = "budget".equals(catalogue.injection()) ? -1 : random.nextInt((int) (total + total / 5 + 1));
        String actual = outcome(() -> describe(recommend.invoke(planner, candidates(catalogue), budget, workload)));
        String expected = expectedPlan(catalogue, budget, workload);
        expect(expected == null ? !actual.startsWith("java.") : actual.startsWith(expected),
                "plan " + caseIndex + " -> " + actual + " expected " + expected);
        if (expected == null) {
            checkRecommendation(catalogue, budget, workload, actual);
        }
        emit("PLAN " + caseIndex + " n=" + n + " budget=" + budget + " inj=" + catalogue.injection() + " -> " + actual);
    }

    private Catalogue catalogue(int n, Random random) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < capabilities.length; i++) {
            order.add(i);
        }
        java.util.Collections.shuffle(order, random);
        int[] caps = new int[n];
        long[] bytes = new long[n];
        long[] cost = new long[n];
        long[] reuse = new long[n];
        int[][] deps = new int[n][];
        boolean[] mandatory = new boolean[n];
        for (int j = 0; j < n; j++) {
            caps[j] = order.get(j);
            bytes[j] = random.nextInt(8) == 0 ? 0 : random.nextInt(5_000);
            cost[j] = random.nextInt(51);
            reuse[j] = random.nextInt(1_001);
            deps[j] = n > EXACT_LIMIT ? forward(j, n, random) : dependencies(j, n, random);
            mandatory[j] = random.nextInt(10) == 0;
        }
        int injection = random.nextInt(100);
        String injected = n > 0 && injection < 3 ? "duplicate" : n > 0 && injection < 6 ? "missing"
                : injection < 7 ? "budget" : "none";
        if ("missing".equals(injected)) {
            deps[0] = new int[] {order.get(n)};
        }
        return new Catalogue(caps, bytes, cost, reuse, deps, mandatory, injected);
    }

    /** Dependencies are positions in the catalogue, never the candidate itself (cycles are allowed). */
    private static int[] dependencies(int j, int n, Random random) {
        if (n < 2 || random.nextInt(10) >= 3) {
            return new int[0];
        }
        int first = (j + 1 + random.nextInt(n - 1)) % n;
        int second = (j + 1 + random.nextInt(n - 1)) % n;
        return first == second || random.nextBoolean() ? new int[] {first} : new int[] {first, second};
    }

    /** Greedy catalogues depend only on later positions (acyclic): the greedy closure recursion does not end on a cycle. */
    private static int[] forward(int j, int n, Random random) {
        if (j >= n - 1 || random.nextInt(10) >= 3) {
            return new int[0];
        }
        return new int[] {j + 1 + random.nextInt(n - 1 - j)};
    }

    /**
     * Records a donor behaviour, not a contract: a greedy catalogue (more than 20 candidates) whose dependencies form a
     * cycle makes the closure recursion overflow the stack (both sides must report the same StackOverflowError).
     */
    private void cycleProbe() throws ReflectiveOperationException {
        int n = EXACT_LIMIT + 1;
        int[] caps = new int[n];
        int[][] deps = new int[n][];
        for (int j = 0; j < n; j++) {
            caps[j] = j;
            deps[j] = j < 2 ? new int[] {1 - j} : new int[0];
        }
        Catalogue c = new Catalogue(caps, new long[n], new long[n], new long[n], deps, new boolean[n], "cycle");
        Object b = builder.invoke(null);
        Object workload = build.invoke(b);
        String actual = outcome(() -> describe(recommend.invoke(planner, candidates(c), 0L, workload)));
        expect(actual.startsWith(StackOverflowError.class.getName()), "cycle probe -> " + actual);
        emit("CYCLE_PROBE n=" + n + " -> " + actual);
    }

    private Object workload(Random random) throws ReflectiveOperationException {
        Object b = builder.invoke(null);
        for (Object capability : capabilities) {
            if (random.nextBoolean()) {
                weight.invoke(b, capability, random.nextInt(201));
            }
        }
        buildPenalty.invoke(b, random.nextInt(6));
        return build.invoke(b);
    }

    /** The catalogue as candidate records; a missing dependency names a capability outside the catalogue. */
    private List<Object> candidates(Catalogue c) throws ReflectiveOperationException {
        List<Object> list = new ArrayList<>();
        for (int j = 0; j < c.caps().length; j++) {
            Set<Object> dependencies = new HashSet<>();
            for (int d : c.deps()[j]) {
                dependencies.add(capabilities["missing".equals(c.injection()) && j == 0 ? d : c.caps()[d]]);
            }
            list.add(candidate.newInstance(capabilities[c.caps()[j]], c.bytes()[j], c.cost()[j], c.reuse()[j],
                    dependencies, c.mandatory()[j]));
        }
        if ("duplicate".equals(c.injection())) {
            list.add(list.get(0));
        }
        return list;
    }

    private String describe(Object rec) throws ReflectiveOperationException {
        Set<?> chosen = (Set<?>) recommendation[0].invoke(rec);
        return "caps=" + text(chosen) + " bytes=" + recommendation[1].invoke(rec) + " score=" + recommendation[2].invoke(rec)
                + " exact=" + recommendation[3].invoke(rec);
    }

    /** EnumSet text: names in ordinal order. */
    private String text(Set<?> chosen) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Object capability : capabilities) {
            if (chosen.contains(capability)) {
                joiner.add(capability.toString());
            }
        }
        return joiner.toString();
    }

    /** The expected refusal prefix, or null when a recommendation is expected (checked by checkRecommendation). */
    private String expectedPlan(Catalogue c, long budget, Object workload) throws ReflectiveOperationException {
        switch (c.injection()) {
            case "budget": return REFUSED + "negative byteBudget";
            case "duplicate": return REFUSED + "duplicate candidate";
            case "missing": return REFUSED + capabilities[c.caps()[0]] + " depends on missing";
            default: break;
        }
        int n = c.caps().length;
        long mandatoryBytes = bytes(c, closure(c, mandatorySet(c), new boolean[n]));
        if (mandatoryBytes > budget) {
            return n <= EXACT_LIMIT ? NONE_FITS : MANDATORY;
        }
        return n <= BRUTE_LIMIT ? "caps=" + brute(c, budget, workload)
                : n > EXACT_LIMIT ? "caps=" + greedy(c, budget, workload) : null;
    }

    private static boolean[] mandatorySet(Catalogue c) {
        boolean[] set = new boolean[c.caps().length];
        for (int j = 0; j < set.length; j++) {
            set[j] = c.mandatory()[j];
        }
        return set;
    }

    /** Positions reachable from the seeds through dependencies, minus those already taken. */
    private static boolean[] closure(Catalogue c, boolean[] seeds, boolean[] taken) {
        boolean[] result = new boolean[seeds.length];
        List<Integer> stack = new ArrayList<>();
        for (int j = 0; j < seeds.length; j++) {
            if (seeds[j] && !taken[j]) {
                stack.add(j);
            }
        }
        while (!stack.isEmpty()) {
            int j = stack.remove(stack.size() - 1);
            if (result[j] || taken[j]) {
                continue;
            }
            result[j] = true;
            for (int d : c.deps()[j]) {
                stack.add(d);
            }
        }
        return result;
    }

    private static long bytes(Catalogue c, boolean[] set) {
        long sum = 0;
        for (int j = 0; j < set.length; j++) {
            sum += set[j] ? c.bytes()[j] : 0;
        }
        return sum;
    }

    private long score(Catalogue c, boolean[] set, Object workload) throws ReflectiveOperationException {
        long penalty = (int) penaltyOf.invoke(workload);
        long sum = 0;
        for (int j = 0; j < set.length; j++) {
            if (set[j]) {
                sum += (int) workloadWeight.invoke(workload, capabilities[c.caps()[j]]) * c.reuse()[j] - penalty * c.cost()[j];
            }
        }
        return sum;
    }

    private static boolean valid(Catalogue c, boolean[] set, long budget) {
        for (int j = 0; j < set.length; j++) {
            if (c.mandatory()[j] && !set[j]) {
                return false;
            }
            for (int d : set[j] ? c.deps()[j] : new int[0]) {
                if (!set[d]) {
                    return false;
                }
            }
        }
        return bytes(c, set) <= budget;
    }

    /** Brute force over every subset: best score, then fewer bytes, then the smaller set text. */
    private String brute(Catalogue c, long budget, Object workload) throws ReflectiveOperationException {
        int n = c.caps().length;
        long[] scores = new long[n];
        for (int j = 0; j < n; j++) {
            boolean[] one = new boolean[n];
            one[j] = true;
            scores[j] = score(c, one, workload);
        }
        String best = null;
        long bestScore = Long.MIN_VALUE;
        long bestBytes = 0;
        for (int mask = 0; mask < 1 << n; mask++) {
            boolean[] set = new boolean[n];
            long sum = 0;
            for (int j = 0; j < n; j++) {
                set[j] = (mask >>> j & 1) != 0;
                sum += set[j] ? scores[j] : 0;
            }
            if (!valid(c, set, budget)) {
                continue;
            }
            long b = bytes(c, set);
            String t = positionsText(c, set);
            if (best == null || sum > bestScore || sum == bestScore && (b < bestBytes || b == bestBytes && t.compareTo(best) < 0)) {
                best = t;
                bestScore = sum;
                bestBytes = b;
            }
        }
        return best + " bytes=" + bestBytes + " score=" + bestScore + " exact=true";
    }

    /**
     * The documented greedy pass, recomputed: mandatory closures in capability order, then repeatedly the remaining
     * closure that fits with a positive score and the best score/byte density (cross products compared exactly),
     * then the higher score, the fewer bytes, the smaller set text.
     */
    private String greedy(Catalogue c, long budget, Object workload) throws ReflectiveOperationException {
        int n = c.caps().length;
        Integer[] order = new Integer[n];
        for (int j = 0; j < n; j++) {
            order[j] = j;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(c.caps()[a], c.caps()[b]));
        boolean[] chosen = new boolean[n];
        for (int j : order) {
            if (c.mandatory()[j]) {
                take(chosen, closure(c, single(n, j), chosen));
            }
        }
        while (true) {
            boolean[] best = null;
            for (int j : order) {
                boolean[] more = chosen[j] ? null : closure(c, single(n, j), chosen);
                if (more == null || bytes(c, chosen) + bytes(c, more) > budget || score(c, more, workload) <= 0) {
                    continue;
                }
                if (best == null || denser(c, more, best, workload)) {
                    best = more;
                }
            }
            if (best == null) {
                break;
            }
            take(chosen, best);
        }
        return positionsText(c, chosen) + " bytes=" + bytes(c, chosen) + " score=" + score(c, chosen, workload) + " exact=false";
    }

    private boolean denser(Catalogue c, boolean[] left, boolean[] right, Object workload) throws ReflectiveOperationException {
        long ls = score(c, left, workload);
        long rs = score(c, right, workload);
        long lb = bytes(c, left);
        long rb = bytes(c, right);
        int compared = java.math.BigInteger.valueOf(ls).multiply(java.math.BigInteger.valueOf(Math.max(1L, rb)))
                .compareTo(java.math.BigInteger.valueOf(rs).multiply(java.math.BigInteger.valueOf(Math.max(1L, lb))));
        if (compared != 0) {
            return compared > 0;
        }
        if (ls != rs) {
            return ls > rs;
        }
        if (lb != rb) {
            return lb < rb;
        }
        return positionsText(c, left).compareTo(positionsText(c, right)) < 0;
    }

    private static boolean[] single(int n, int j) {
        boolean[] set = new boolean[n];
        set[j] = true;
        return set;
    }

    private static void take(boolean[] chosen, boolean[] more) {
        for (int j = 0; j < chosen.length; j++) {
            chosen[j] |= more[j];
        }
    }

    private String positionsText(Catalogue c, boolean[] set) {
        Set<Object> chosen = new HashSet<>();
        for (int j = 0; j < set.length; j++) {
            if (set[j]) {
                chosen.add(capabilities[c.caps()[j]]);
            }
        }
        return text(chosen);
    }

    /** Validity, recomputed bytes and score; greedy maximality for more than 20 candidates. */
    private void checkRecommendation(Catalogue c, long budget, Object workload, String actual)
            throws ReflectiveOperationException {
        int n = c.caps().length;
        Set<String> names = new HashSet<>(Arrays.asList(
                actual.substring(actual.indexOf('[') + 1, actual.indexOf(']')).split(", ")));
        boolean[] set = new boolean[n];
        for (int j = 0; j < n; j++) {
            set[j] = names.contains(capabilities[c.caps()[j]].toString());
        }
        expect(valid(c, set, budget), "valid recommendation " + actual);
        expect(actual.endsWith(" bytes=" + bytes(c, set) + " score=" + score(c, set, workload) + " exact=" + (n <= EXACT_LIMIT)),
                "recomputed " + actual);
        for (int j = 0; j < n && n > EXACT_LIMIT; j++) {
            boolean[] seed = new boolean[n];
            seed[j] = true;
            boolean[] more = closure(c, seed, set);
            expect(score(c, more, workload) <= 0 || bytes(c, set) + bytes(c, more) > budget, "greedy maximal at " + j);
        }
    }

    // ---------------------------------------------------------------- tuples and hashes

    private void tupleCase(int caseIndex, Random random) throws ReflectiveOperationException {
        int arity = random.nextInt(50) == 0 ? random.nextInt(2) * 5 : 1 + random.nextInt(4);
        int rows = random.nextInt(300);
        long range = random.nextInt(3) == 0 ? 4 : 1_000_000;
        long[] keys = new long[Math.max(arity, 1) * rows];
        int[] values = new int[rows];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = random.nextLong(-range, range + 1);
        }
        for (int i = 0; i < rows; i++) {
            values[i] = random.nextInt();
        }
        long[] flattened = random.nextInt(50) == 0 && keys.length > 0 ? Arrays.copyOf(keys, keys.length - 1) : keys;
        Object capability = capabilities[random.nextInt(capabilities.length)];
        String frozen = outcome(() -> "ok" + freeze.invoke(null, capability, arity, flattened, values).hashCode() * 0);
        String expected = expectedFreeze(arity, flattened, values);
        expect(frozen.startsWith(expected), "freeze " + caseIndex + " -> " + frozen + " expected " + expected);
        if (!"ok".equals(expected)) {
            emit("TUPLE " + caseIndex + " arity=" + arity + " rows=" + rows + " -> " + frozen);
            return;
        }
        Object frozenIndex = freeze.invoke(null, capability, arity, flattened, values);
        emit("TUPLE " + caseIndex + " arity=" + arity + " rows=" + rows + " " + tupleFacts(frozenIndex, capability, arity,
                keys, values, random));
    }

    private static String expectedFreeze(int arity, long[] keys, int[] values) {
        if (arity < 1 || arity > 4) {
            return REFUSED + "arity must be 1..4";
        }
        if (keys.length != values.length * arity) {
            return REFUSED + "flattened key/value cardinality mismatch";
        }
        Set<List<Long>> seen = new HashSet<>();
        for (int row = 0; row < values.length; row++) {
            if (!seen.add(row(keys, row, arity))) {
                return REFUSED + "duplicate tuple row " + row;
            }
        }
        return "ok";
    }

    private static List<Long> row(long[] keys, int row, int arity) {
        List<Long> tuple = new ArrayList<>();
        for (int k = 0; k < arity; k++) {
            tuple.add(keys[row * arity + k]);
        }
        return tuple;
    }

    private String tupleFacts(Object frozenIndex, Object capability, int arity, long[] keys, int[] values, Random random)
            throws ReflectiveOperationException {
        Map<List<Long>, Integer> model = new HashMap<>();
        for (int row = 0; row < values.length; row++) {
            model.put(row(keys, row, arity), values[row]);
        }
        int capacity = 2;
        while (capacity < Math.max(2, values.length * 2)) {
            capacity <<= 1;
        }
        long retained = (long) keys.length * Long.BYTES + (long) values.length * Integer.BYTES + (long) capacity * Integer.BYTES;
        expect((int) index.getMethod("capacity").invoke(frozenIndex) == capacity
                && (long) index.getMethod("retainedBytes").invoke(frozenIndex) == retained, "capacity/retained");
        String hash = (String) rootHash.invoke(frozenIndex);
        expect(hash.equals(rootDigest(tupleDomain + capabilityId.invoke(capability) + ":" + arity, keys, values)), "root hash");
        long[] queries = queries(keys, values.length, arity, random);
        int[] expected = new int[queries.length / arity];
        for (int q = 0; q < expected.length; q++) {
            expected[q] = model.getOrDefault(row(queries, q, arity), MISSING);
        }
        boolean[] present = new boolean[expected.length];
        for (int q = 0; q < expected.length; q++) {
            present[q] = model.containsKey(row(queries, q, arity));
        }
        long results = lookups(frozenIndex, arity, queries, expected, present);
        return "cap=" + capacity + " bytes=" + retained + " H:" + hash + " hits=" + model.size() + " results="
                + Long.toHexString(results);
    }

    private static long[] queries(long[] keys, int rows, int arity, Random random) {
        long[] queries = new long[QUERIES * arity];
        for (int q = 0; q < QUERIES; q++) {
            int source = rows > 0 && random.nextBoolean() ? random.nextInt(rows) : -1;
            for (int k = 0; k < arity; k++) {
                queries[q * arity + k] = source >= 0 ? keys[source * arity + k] : random.nextLong(-5, 6);
            }
        }
        return queries;
    }

    /** Every lookup path against the model; returns a digest of the results. */
    private long lookups(Object frozenIndex, int arity, long[] queries, int[] expected, boolean[] present)
            throws ReflectiveOperationException {
        long h = FNV_BASIS;
        Method array = index.getMethod("get", long[].class, int.class, int.class);
        Class<?>[] scalar = new Class<?>[arity + 1];
        Arrays.fill(scalar, long.class);
        scalar[arity] = int.class;
        Method getter = index.getMethod("get", scalar);
        for (int q = 0; q < expected.length; q++) {
            Object[] args = new Object[arity + 1];
            for (int k = 0; k < arity; k++) {
                args[k] = queries[q * arity + k];
            }
            args[arity] = MISSING;
            expect((int) array.invoke(frozenIndex, queries, q * arity, MISSING) == expected[q]
                    && (int) getter.invoke(frozenIndex, args) == expected[q], "get " + q);
            h = (h ^ expected[q]) * FNV_PRIME;
        }
        for (int threshold : new int[] {0, NO_THRESHOLD}) {
            expect(Arrays.equals((int[]) lookupTuples.invoke(null, frozenIndex, queries, MISSING, threshold), expected),
                    "lookupTuples " + threshold);
        }
        if (arity == 2) {
            pairLookups(frozenIndex, queries, expected, present);
        }
        return h;
    }

    private void pairLookups(Object frozenIndex, long[] queries, int[] expected, boolean[] present)
            throws ReflectiveOperationException {
        long[] left = new long[expected.length];
        long[] right = new long[expected.length];
        for (int q = 0; q < expected.length; q++) {
            left[q] = queries[2 * q];
            right[q] = queries[2 * q + 1];
            expect((int) index.getMethod("getPair", long.class, long.class, int.class).invoke(frozenIndex, left[q], right[q], MISSING)
                    == expected[q] && (boolean) index.getMethod("containsPair", long.class, long.class)
                    .invoke(frozenIndex, left[q], right[q]) == present[q], "pair " + q);
        }
        int[] accelerated = new int[expected.length];
        acceleratePairs.invoke(javaAccelerator, frozenIndex, left, right, MISSING, accelerated);
        expect(Arrays.equals((int[]) lookupPairs.invoke(null, frozenIndex, left, right, MISSING, 0), expected)
                && Arrays.equals(accelerated, expected), "lookupPairs");
    }

    private void hashCase(int caseIndex, Random random) throws ReflectiveOperationException {
        String domain = "D" + random.nextInt(1_000);
        String[] values = new String[random.nextInt(4)];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.toString(random.nextInt(), Character.MAX_RADIX) + (char) ('a' + random.nextInt(26));
        }
        String actual = (String) sha256.invoke(null, domain, values);
        expect(actual.equals(stringDigest(domain, values)), "sha256 strings");
        emit("HASH " + caseIndex + " " + domain + " " + Arrays.toString(values) + " " + actual);
    }

    private static String rootDigest(String domain, long[] keys, int[] values) {
        MessageDigest digest = sha();
        digest.update(domain.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.allocate(keys.length * Long.BYTES + values.length * Integer.BYTES);
        for (long key : keys) {
            buffer.putLong(key);
        }
        for (int value : values) {
            buffer.putInt(value);
        }
        digest.update(buffer.array());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String stringDigest(String domain, String[] values) {
        MessageDigest digest = sha();
        for (String part : prepend(domain, values)) {
            byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            digest.update(bytes);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String[] prepend(String first, String[] rest) {
        String[] all = new String[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    private static MessageDigest sha() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    // ---------------------------------------------------------------- plumbing

    @FunctionalInterface
    private interface Call {
        String run() throws ReflectiveOperationException;
    }

    private String outcome(Call call) throws ReflectiveOperationException {
        try {
            return call.run();
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
        for (int i = 0; i < line.length(); i++) {
            digest = (digest ^ line.charAt(i)) * FNV_PRIME;
        }
        digest = (digest ^ '\n') * FNV_PRIME;
    }
}
