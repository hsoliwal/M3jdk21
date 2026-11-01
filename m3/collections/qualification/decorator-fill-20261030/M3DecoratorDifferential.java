/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.SortedBag;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.DualTreeBidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.apache.commons.collections4.trie.PatriciaTrie;

/**
 * Pattern DECORATOR_DIFFERENTIAL v2: seeded reflective differential run of one generated owned
 * decorator against the Apache Commons Collections 4.5.0 decorator it mirrors (test oracle only).
 * Two equal backings are built (collection, map, bidi map, multi-valued map, trie or entry set)
 * and each is wrapped by the named static factory of its side; extra factory parameters get the
 * same policy on both sides (Predicate, Transformer, Factory, Comparator, int). Every step either
 * mutates both backings directly (views must follow) or calls one public method the M3 class
 * declares, on both sides with equal arguments. Return values, exception classes, drained
 * iterators, map iterators, entries and views (with non-destructive mutation probes) and the
 * backing state are compared after every step; factory null rejection, factory identity on its
 * own result and a serialization round trip are checked once.
 *
 * <p>v3 PREDICATED mode (the factory takes Commons Predicate lanes): each lane gets its own
 * serializable counting {@link Pol} per side (policy both, keynull, valuenull or rejectnull from
 * TARGETS.tsv), backings hold accepted elements only, the per-lane predicate call counts are
 * compared after every step, exception messages of IllegalArgumentException and of non-helpful
 * NullPointerException are compared, every factory argument null combination is compared once
 * with its message (R3: null-check order), and returned views get rejected-element probes and an
 * interface-shape bitmap. Targets without Predicate lanes run exactly as in v2.1.
 *
 * <p>Usage: {@code java -cp commons.jar;classes M3DecoratorDifferential.java <m3 fqn>
 * <commons fqn> <backing> <factory> <out.tsv> [policy]}. Exit 0 = no mismatch, 4 = mismatches
 * appended to out.tsv.
 */
public final class M3DecoratorDifferential {
    private static final Logger LOG = Logger.getLogger(M3DecoratorDifferential.class.getName());
    /**
     * v3: PREDICATED mode and its rejected element (every Pol rejects hashCode % 5 == 4; 9 is the
     * other rejected key in 0..9).
     */
    private static boolean pred;
    private static boolean rejectNull;
    private static final Integer REJECTED = 4;

    /**
     * v3: serializable counting predicate with a stable toString, one instance per side and lane.
     */
    static final class Pol
            implements org.apache.commons.collections4.Predicate<Object>, Serializable {
        private static final long serialVersionUID = 1L;
        private int calls;

        @Override
        public boolean evaluate(final Object v) {
            calls++;
            return !rejects(v);
        }

        @Override
        public String toString() {
            return "Pol";
        }
    }

    static boolean rejects(final Object v) {
        return v == null ? rejectNull : v.hashCode() % 5 == 4;
    }

    private String policy = "both";
    private final List<Pol> pols = new ArrayList<>();
    private int[] sides = {0, 0, 0};
    private static final long[] SEEDS =
            {2L, 11L, 42L, 1_009L, 20_261_030L, 0xFEEDL, -5L, 555_555_555L};
    private static final int STEPS = 4_000;
    /** Warm-up calls per side and method atom before {@link #perf} measures (C2 compiles). */
    private static final int WARMUP = Integer.getInteger("m3.perf.warmup", 20_000);
    /** {@code --jit} mode: CLI flag, TARGETS.tsv layout and the measured calls and rounds. */
    static final String JIT_MODE = "--jit";
    private static final String TAB = "\t";
    private static final String TARGETS_HEADER = "m3_class";
    private static final String DEFAULT_POLICY = "-";
    private static final int POLICY_COLUMN = 5;
    private static final int JIT_CALLS = 100;
    private static final int JIT_ROUNDS = 1;
    private static final Object ABSENT = "absent-key";
    private static final Map<String, Supplier<Object>> BACKINGS = Map.ofEntries(
            Map.entry("list", ArrayList::new), Map.entry("deque", LinkedList::new),
            Map.entry("treeset", TreeSet::new), Map.entry("hashset", HashSet::new),
            Map.entry("hashbag", HashBag::new),
            Map.entry("treebag", TreeBag::new),
            Map.entry("hashmultiset", HashMultiSet::new),
            Map.entry("hashmap", HashMap::new), Map.entry("treemap", TreeMap::new),
            Map.entry("linkedmap", LinkedMap::new),
            Map.entry("dualhashbidi", DualHashBidiMap::new),
            Map.entry("dualtreebidi", DualTreeBidiMap::new),
            Map.entry("treebidi", TreeBidiMap::new),
            Map.entry("multimap", ArrayListValuedHashMap::new),
            Map.entry("trie", PatriciaTrie::new),
            Map.entry("entryset", HashMap::new));

    private final String kind;
    private final Method m3Factory;
    private final Method oracleFactory;
    private final List<Method> methods;
    private final List<String> mismatches = new ArrayList<>();
    /** Per method atom ({@code name[parameter types]}): how many seeded steps exercised it. */
    private final Map<String, Long> exercised = new java.util.TreeMap<>();
    private long checks;

    /**
     * The outcome of one differential run.
     *
     * @param mismatches one row per mismatch ({@code where, oracle=.., m3=..})
     * @param exercised per method atom key, the number of seeded steps that called it
     * @param checks the number of comparisons made
     */
    record Result(List<String> mismatches, Map<String, Long> exercised, long checks) {
    }

    private M3DecoratorDifferential(Class<?> m3, Class<?> oracle, String kind, String factory)
            throws NoSuchMethodException {
        this.kind = kind;
        this.oracleFactory = Arrays.stream(oracle.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals(factory))
                .min(Comparator.comparing(Method::toGenericString)).orElseThrow();
        this.m3Factory = m3.getMethod(factory, oracleFactory.getParameterTypes());
        this.methods = Arrays.stream(m3.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())
                        && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic())
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
    }

    /**
     * Runs the differential of one M3 decorator against its Commons oracle and appends every
     * mismatch to the output file.
     *
     * @param args the M3 class name, the Commons oracle class name, the backing kind, the static
     *         factory name, the output TSV path and an optional policy ({@code -} selects the
     *         default policy {@code both})
     * @throws Exception if a class or factory method cannot be found or the output file cannot
     *         be written
     */
    public static void main(String[] args) throws Exception {
        if (args[0].equals(JIT_MODE)) {
            jit(Path.of(args[1]));
            return;
        }
        Result run = run(args[0], args[1], args[2], args[3],
                args.length > 5 ? args[5] : "-");
        Files.write(Path.of(args[4]),
                run.mismatches().stream().map(r -> args[0] + "\t" + r).toList(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        LOG.info(() -> "M3_DECORATOR_DIFF class=" + args[0] + " oracle=" + args[1]
                + " factory=" + args[3] + " methods=" + run.exercised().size()
                + " seeds=" + SEEDS.length + " steps=" + STEPS + " checks=" + run.checks()
                + " mismatches=" + run.mismatches().size());
        System.exit(run.mismatches().isEmpty() ? 0 : 4);
    }

    /**
     * Runs the differential of one M3 decorator against its Commons oracle in process (the JUnit
     * atom tests and {@link #main} share it).
     *
     * @param m3 the M3 class name
     * @param oracle the Commons oracle class name
     * @param backing the backing kind
     * @param factory the static factory name
     * @param policy the lane policy, or {@code -} for the default {@code both}
     * @return the mismatches, the per-method exercise counts and the number of checks
     * @throws ReflectiveOperationException if a class or factory method cannot be found
     */
    static synchronized Result run(String m3, String oracle, String backing, String factory,
            String policy) throws ReflectiveOperationException {
        M3DecoratorDifferential run = new M3DecoratorDifferential(
                Class.forName(m3), Class.forName(oracle), backing, factory);
        pred = Arrays.asList(run.oracleFactory.getParameterTypes())
                .contains(org.apache.commons.collections4.Predicate.class);
        run.policy = !policy.equals("-") ? policy : "both";
        rejectNull = run.policy.equals("rejectnull");
        run.methods.forEach(m -> run.exercised.put(key(m), 0L));
        run.once();
        if (pred) {
            run.predicatedOnce();
        }
        Arrays.stream(SEEDS).forEach(run::seed);
        return new Result(List.copyOf(run.mismatches), Map.copyOf(run.exercised), run.checks);
    }

    /**
     * Times every method atom of one owned decorator against its Commons oracle: equal backings,
     * the same seeded arguments on both sides, {@code m3.perf.warmup} warm-up calls per side
     * (default 20,000), then the median of {@code rounds} measured rounds of {@code calls} calls,
     * alternating which side runs first. Both sides are called reflectively with access checks
     * off, so the ratio compares the decorators, not the call path.
     *
     * @param m3 the M3 class name
     * @param oracle the Commons oracle class name
     * @param backing the backing kind
     * @param factory the static factory name
     * @param policy the lane policy, or {@code -} for the default {@code both}
     * @param calls the calls per round and side
     * @param rounds the measured rounds
     * @return per atom key, {M3 ns per call, oracle ns per call} ({@code NaN} when the oracle
     *         has no such method or a factory fails)
     * @throws ReflectiveOperationException if a class or factory method cannot be found
     */
    static synchronized Map<String, double[]> perf(String m3, String oracle, String backing,
            String factory, String policy, int calls, int rounds)
            throws ReflectiveOperationException {
        M3DecoratorDifferential run = new M3DecoratorDifferential(
                Class.forName(m3), Class.forName(oracle), backing, factory);
        pred = Arrays.asList(run.oracleFactory.getParameterTypes())
                .contains(org.apache.commons.collections4.Predicate.class);
        run.policy = !policy.equals("-") ? policy : "both";
        rejectNull = run.policy.equals("rejectnull");
        Map<String, double[]> out = new java.util.TreeMap<>();
        run.methods.forEach(m -> out.put(key(m), run.time(m, calls, rounds)));
        return out;
    }

    private double[] time(Method m, int calls, int rounds) {
        Object o = build(oracleFactory, filled(new Random(7)));
        Object x = build(m3Factory, filled(new Random(7)));
        Method om;
        Method xm;
        try {
            // the same reflective path on both sides: access checks off for both methods
            om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            om.setAccessible(true);
            xm = x.getClass().getMethod(m.getName(), m.getParameterTypes());
            xm.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return new double[] {Double.NaN, Double.NaN};
        }
        if (o instanceof Throwable || x instanceof Throwable) {
            return new double[] {Double.NaN, Double.NaN};
        }
        Object[][] ao = new Object[calls][];
        Object[][] ax = new Object[calls][];
        java.util.stream.IntStream.range(0, calls).forEach(i -> {
            ao[i] = args(m.getParameterTypes(), new Random(i));
            ax[i] = args(m.getParameterTypes(), new Random(i));
        });
        // warm-up past the C2 compile threshold: Commons decorators inherit shared base methods
        // that earlier targets already compiled, while each owned class has its own method
        java.util.stream.IntStream.range(0, WARMUP).forEach(i -> {
            call(om, o, ao[i % calls]);
            call(xm, x, ax[i % calls]);
        });
        long[] to = new long[rounds];
        long[] tx = new long[rounds];
        for (int round = 0; round < rounds; round++) {
            // alternate which side runs first, so neither always gets the warmer caches
            boolean oracleFirst = (round & 1) == 0;
            long first = timeCalls(oracleFirst ? om : xm, oracleFirst ? o : x,
                    oracleFirst ? ao : ax);
            long second = timeCalls(oracleFirst ? xm : om, oracleFirst ? x : o,
                    oracleFirst ? ax : ao);
            to[round] = oracleFirst ? first : second;
            tx[round] = oracleFirst ? second : first;
        }
        Arrays.sort(to);
        Arrays.sort(tx);
        return new double[] {tx[rounds / 2] / (double) calls, to[rounds / 2] / (double) calls};
    }

    private static long timeCalls(Method m, Object target, Object[][] calls) {
        long start = System.nanoTime();
        Arrays.stream(calls).forEach(c -> call(m, target, c));
        return System.nanoTime() - start;
    }

    /**
     * {@code --jit <TARGETS.tsv>}: the warmed {@link #perf} over every target, for a JVM started
     * with {@code -XX:+PrintCompilation}; the JIT test reads which owned methods the JIT compiled.
     */
    private static void jit(Path targets) throws IOException, ReflectiveOperationException {
        for (String line : Files.readAllLines(targets, StandardCharsets.UTF_8)) {
            String[] t = line.split(TAB, -1);
            if (line.isBlank() || t[0].equals(TARGETS_HEADER)) {
                continue;
            }
            String policy = t.length > POLICY_COLUMN && !t[POLICY_COLUMN].isBlank()
                    ? t[POLICY_COLUMN].strip() : DEFAULT_POLICY;
            perf(t[0], t[1], t[2], t[3], policy, JIT_CALLS, JIT_ROUNDS);
        }
    }

    /** The atom key of a method, as the step loop names it in mismatch rows. */
    static String key(Method m) {
        return m.getName() + Arrays.toString(m.getParameterTypes());
    }

    private void once() {
        compare("factory(null)", norm(build(oracleFactory, null)), norm(build(m3Factory, null)));
        Object o = build(oracleFactory, filled(new Random(7)));
        Object x = build(m3Factory, filled(new Random(7)));
        compare("factory(own result) identity", build(oracleFactory, o) == o,
                build(m3Factory, x) == x);
        compare("serial round trip", norm(roundTrip(o)), norm(roundTrip(x)));
    }

    private Object build(Method factory, Object owner) {
        Object[] args = new Object[factory.getParameterCount()];
        Random r = new Random(99);
        args[0] = owner == null || factory.getParameterTypes()[0].isInstance(owner)
                ? owner : target(owner);
        for (int i = 1; i < args.length; i++) {
            args[i] = pred ? lane(i) : arg(factory.getParameterTypes()[i], r);
        }
        return call(factory, null, args);
    }

    /**
     * v3: a fresh Pol for lane i (registered for call counting); null when the policy switches
     * that map lane off.
     */
    private Object lane(int i) {
        Pol p = new Pol();
        pols.add(p);
        boolean off = policy.equals("keynull") && i == 1 || policy.equals("valuenull") && i == 2;
        return off ? null : p;
    }

    /**
     * v3 once-only checks: factory over a backing that already holds a rejected element, and
     * every combination of null factory arguments (R3: which argument is checked first and with
     * which message, e.g. NPE "collection" before "predicate", or "map" with null map lanes
     * accepted).
     */
    @SuppressWarnings("unchecked")
    private void predicatedOnce() {
        Object bad1 = filled(new Random(7));
        Object bad2 = filled(new Random(7));
        for (Object bad : List.of(bad1, bad2)) {
            if (bad instanceof Map<?, ?> m) {
                ((Map<Object, Object>) m).put(1, REJECTED);
            } else {
                ((Collection<Object>) bad).add(REJECTED);
            }
        }
        compare("factory(rejected element)", norm(build(oracleFactory, bad1)),
                norm(build(m3Factory, bad2)));
        int n = oracleFactory.getParameterCount();
        for (int mask = 1; mask < 1 << n; mask++) {
            compare("factory(null mask " + Integer.toBinaryString(mask) + ")",
                    norm(nulls(oracleFactory, mask)), norm(nulls(m3Factory, mask)));
        }
    }

    private Object nulls(Method factory, int mask) {
        Object[] args = new Object[factory.getParameterCount()];
        args[0] = (mask & 1) != 0 ? null : target(filled(new Random(7)));
        for (int i = 1; i < args.length; i++) {
            Object lane = lane(i);
            args[i] = (mask & 1 << i) != 0 ? null : lane;
        }
        return call(factory, null, args);
    }

    private List<Integer> laneCalls(int from, int to) {
        return pols.subList(from, to).stream().map(p -> p.calls).toList();
    }

    private Object target(Object owner) {
        return kind.equals("entryset") ? ((Map<?, ?>) owner).entrySet() : owner;
    }

    private static Object roundTrip(Object v) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(v);
            }
            try (ObjectInputStream in =
                    new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return in.readObject();
            }
        } catch (Exception e) {
            return e;
        }
    }

    private Object filled(Random r) {
        Object owner = BACKINGS.get(kind).get();
        for (int i = r.nextInt(12); i > 0; i--) {
            mutate(owner, r, true);
        }
        if (pred && owner instanceof Map<?, ?> m) {
            m.entrySet().removeIf(e -> rejects(e.getKey()) || rejects(e.getValue()));
        } else if (pred) {
            ((Collection<?>) owner).removeIf(M3DecoratorDifferential::rejects);
        }
        return owner;
    }

    private Object key(Random r) {
        return kind.equals("trie") ? "k" + r.nextInt(10) : (Object) r.nextInt(10);
    }

    @SuppressWarnings("unchecked")
    private Object mutate(Object owner, Random r, boolean add) {
        Object k = key(r);
        int v = r.nextInt(10);
        if (owner instanceof MultiValuedMap<?, ?> mv) {
            return add ? ((MultiValuedMap<Object, Object>) mv).put(k, v) : mv.remove(k);
        }
        if (owner instanceof Map<?, ?> map) {
            return add
                    ? ((Map<Object, Object>) map).put(k,
                            kind.endsWith("bidi") ? (Object) (v + 10 * r.nextInt(3)) : v)
                    : map.remove(k);
        }
        return add ? ((Collection<Object>) owner).add(k) : ((Collection<?>) owner).remove(k);
    }

    private void seed(long seed) {
        Random r = new Random(seed);
        Object b1 = filled(new Random(seed));
        Object b2 = filled(new Random(seed));
        int p0 = pols.size();
        Object o = build(oracleFactory, b1);
        int p1 = pols.size();
        Object x = build(m3Factory, b2);
        sides = new int[] {p0, p1, pols.size()};
        if (o instanceof Throwable || x instanceof Throwable) {
            compare("seed " + seed + " factory", norm(o), norm(x));
            return;
        }
        for (int step = 0; step < STEPS && mismatches.size() < 50; step++) {
            step(r, b1, b2, o, x, seed + "@" + step);
        }
    }

    private void step(Random r, Object b1, Object b2, Object o, Object x, String where) {
        if (r.nextInt(5) == 0) {
            long mutationSeed = r.nextLong();
            boolean add = r.nextBoolean();
            compare(where + " backing " + (add ? "add" : "remove"),
                    norm(mutate(b1, new Random(mutationSeed), add)),
                    norm(mutate(b2, new Random(mutationSeed), add)));
        } else {
            Method m = methods.get(r.nextInt(methods.size()));
            exercised.merge(key(m), 1L, Long::sum);
            long argSeed = r.nextLong();
            if (isEquals(m) && r.nextInt(3) == 0) {
                // equal but not identical argument: the true path of equals, which identity and
                // Integer arguments never reach
                compare(where + " equals(copy of backing)", norm(callOracle(o, m, copy(b1))),
                        norm(call(m, x, new Object[] {copy(b2)})));
            } else {
                compare(where + " " + m.getName() + Arrays.toString(m.getParameterTypes()),
                        invokeOracle(o, m, argSeed),
                        norm(call(m, x, args(m.getParameterTypes(), new Random(argSeed)))));
            }
        }
        compare(where + " backing state", b1.toString(), b2.toString());
        if (pred) {
            compare(where + " predicate calls per lane", laneCalls(sides[0], sides[1]),
                    laneCalls(sides[1], sides[2]));
        }
    }

    private static boolean isEquals(Method m) {
        return m.getName().equals("equals") && m.getParameterCount() == 1
                && m.getParameterTypes()[0] == Object.class;
    }

    private static Object callOracle(Object o, Method m, Object arg) {
        try {
            Method om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            om.setAccessible(true);
            return call(om, o, new Object[] {arg});
        } catch (NoSuchMethodException e) {
            return "NO_ORACLE_METHOD";
        }
    }

    /** A fresh backing of the same kind with the same content: equal, never identical. */
    @SuppressWarnings("unchecked")
    private Object copy(Object owner) {
        Object c = BACKINGS.get(kind).get();
        if (c instanceof MultiValuedMap<?, ?> mv) {
            ((MultiValuedMap<Object, Object>) mv).putAll((MultiValuedMap<Object, Object>) owner);
        } else if (c instanceof Map<?, ?> map) {
            ((Map<Object, Object>) map).putAll((Map<Object, Object>) owner);
        } else {
            ((Collection<Object>) c).addAll((Collection<Object>) owner);
        }
        return kind.equals("entryset") ? ((Map<?, ?>) c).entrySet() : c;
    }

    private Object invokeOracle(Object o, Method m, long argSeed) {
        try {
            Method om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            om.setAccessible(true);
            return norm(call(om, o, args(m.getParameterTypes(), new Random(argSeed))));
        } catch (NoSuchMethodException e) {
            return "NO_ORACLE_METHOD";
        }
    }

    private static Object call(Method m, Object target, Object[] args) {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (IllegalAccessException e) {
            return e;
        }
    }

    private Object[] args(Class<?>[] types, Random r) {
        return Arrays.stream(types).map(t -> arg(t, r)).toArray();
    }

    private Object arg(Class<?> t, Random r) {
        if (t == int.class || t == Integer.class || t == long.class) {
            int v = r.nextInt(8) - 1;
            return t == long.class ? (Object) (long) v : (Object) v;
        }
        if (t == boolean.class) {
            return r.nextBoolean();
        }
        if (t.isArray()) {
            return kind.equals("trie") ? new String[r.nextInt(6)] : new Integer[r.nextInt(6)];
        }
        if (Collection.class.isAssignableFrom(t) || t == Iterable.class) {
            List<Object> list = new ArrayList<>();
            for (int i = r.nextInt(4); i > 0; i--) {
                list.add(key(r));
            }
            return list;
        }
        return structured(t, r);
    }

    private Object structured(Class<?> t, Random r) {
        if (Map.class.isAssignableFrom(t)) {
            Map<Object, Object> map = new HashMap<>();
            for (int i = r.nextInt(3); i > 0; i--) {
                map.put(key(r), r.nextInt(10));
            }
            return map;
        }
        if (MultiValuedMap.class.isAssignableFrom(t)) {
            MultiValuedMap<Object, Object> mv = new ArrayListValuedHashMap<>();
            for (int i = r.nextInt(3); i > 0; i--) {
                mv.put(key(r), r.nextInt(10));
            }
            return mv;
        }
        if (t == Map.Entry.class) {
            return new AbstractMap.SimpleEntry<>(key(r), r.nextInt(10));
        }
        return functional(t, r);
    }

    private Object functional(Class<?> t, Random r) {
        int pick = r.nextInt(10);
        return switch (t.getName()) {
            case "java.util.function.Predicate" -> (java.util.function.Predicate<Object>)
                    v -> v != null && v.hashCode() % 3 == pick % 3;
            case "java.util.function.Consumer" -> (Consumer<Object>) v -> { };
            case "java.util.function.BiConsumer" -> (BiConsumer<Object, Object>) (k, v) -> { };
            case "java.util.function.IntFunction" -> (IntFunction<Object[]>) Object[]::new;
            case "java.util.function.UnaryOperator" -> (UnaryOperator<Object>) v -> v;
            case "java.util.function.Function" -> (Function<Object, Object>) k -> pick;
            case "java.util.function.BiFunction" -> (BiFunction<Object, Object, Object>)
                    (k, v) -> pick % 4 == 0 ? null : pick;
            case "java.util.Comparator" ->
                    Comparator.nullsFirst(Comparator.comparing(Object::toString));
            case "org.apache.commons.collections4.Predicate" ->
                    (org.apache.commons.collections4.Predicate<Object>)
                            v -> v == null || v.hashCode() % 5 != 4;
            case "org.apache.commons.collections4.Transformer" -> (Transformer<Object, Object>)
                    v -> v instanceof Integer i ? (Object) (i + 1) : v;
            case "org.apache.commons.collections4.Factory" -> (Factory<Object>) () -> 7;
            case "java.lang.Object" -> r.nextInt(12) == 0 ? null : key(r);
            default -> r.nextInt(12) == 0 ? null : key(r);
        };
    }

    private static Object norm(Object v) {
        if (!pred) {
            return norm0(v);
        }
        try {
            return norm0(v);
        } catch (RuntimeException e) {
            // v3: a predicated sub-view revalidates on construction, so rendering a view can
            // throw on both sides
            return "NORM_EX:" + e.getClass().getName() + message(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object norm0(Object v) {
        if (v == null || v instanceof Number || v instanceof String || v instanceof Boolean) {
            return String.valueOf(v);
        }
        if (v instanceof Throwable t) {
            return "EX:" + t.getClass().getName() + (pred ? message(t) : "");
        }
        if (v instanceof Object[] a) {
            return "A" + Arrays.stream(a).map(M3DecoratorDifferential::norm).toList();
        }
        if (v instanceof MapIterator<?, ?> mi) {
            return "MI" + drainMap(mi) + attempt(mi::remove);
        }
        if (v instanceof ListIterator<?> li && pred) {
            ListIterator<Object> lo = (ListIterator<Object>) li;
            return "LI" + drain(li) + attempt(() -> lo.set(REJECTED))
                    + attempt(() -> lo.add(REJECTED)) + attempt(() -> li.add(null))
                    + attempt(() -> li.set(null)) + attempt(li::remove);
        }
        if (v instanceof ListIterator<?> li) {
            return "LI" + drain(li) + attempt(() -> li.add(null)) + attempt(() -> li.set(null))
                    + attempt(li::remove);
        }
        if (v instanceof Iterator<?> it) {
            return "I" + drain(it) + attempt(it::remove);
        }
        return structural(v);
    }

    @SuppressWarnings("unchecked")
    private static Object structural(Object v) {
        if (v instanceof Spliterator<?> s) {
            List<Object> out = new ArrayList<>();
            s.forEachRemaining(out::add);
            return "S" + out;
        }
        if (v instanceof Stream<?> s) {
            return "T" + s.toList();
        }
        if (v instanceof Map.Entry<?, ?> e && pred) {
            return "E" + e.getKey() + "=" + e.getValue()
                    + attempt(() -> ((Map.Entry<Object, Object>) e).setValue(REJECTED))
                    + attempt(() -> ((Map.Entry<Object, Object>) e).setValue(e.getValue()));
        }
        if (v instanceof Map.Entry<?, ?> e) {
            return "E" + e.getKey() + "=" + e.getValue()
                    + attempt(() -> ((Map.Entry<Object, Object>) e).setValue(e.getValue()));
        }
        if (v instanceof Collection<?> c && pred) {
            return "C" + shape(c) + c.size() + c + "|" + norm(c.toArray()) + "|"
                    + norm(c.toArray(new Object[c.size() + 2]))
                    + attempt(() -> ((Collection<Object>) c).add(REJECTED))
                    + attempt(() -> c.add(null)) + attempt(() -> c.removeIf(e -> false))
                    + attempt(() -> c.iterator().remove());
        }
        if (v instanceof Collection<?> c) {
            return "C" + c.size() + c + attempt(() -> c.add(null))
                    + attempt(() -> c.removeIf(e -> false))
                    + attempt(() -> c.iterator().remove());
        }
        if (v instanceof Map<?, ?> m && pred) {
            return "M" + shape(m) + m.size() + m
                    + attempt(() -> ((Map<Object, Object>) m).put(1, REJECTED))
                    + attempt(() -> ((Map<Object, Object>) m).put(REJECTED, 1))
                    + attempt(() -> ((Map<Object, Object>) m).putAll(Map.of()))
                    + attempt(() -> m.remove(ABSENT));
        }
        if (v instanceof Map<?, ?> m) {
            return "M" + m.size() + m
                    + attempt(() -> ((Map<Object, Object>) m).putAll(Map.of()))
                    + attempt(() -> m.remove(ABSENT));
        }
        if (v instanceof MultiValuedMap<?, ?> mv) {
            return "V" + mv.size() + mv + attempt(() -> mv.remove(ABSENT));
        }
        if (v instanceof Comparator<?> c) {
            Comparator<Object> cmp = (Comparator<Object>) c;
            return "K" + Integer.signum(cmp.compare(1, 2));
        }
        return "OBJ";
    }

    private static List<Object> drain(Iterator<?> it) {
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < 64 && it.hasNext(); i++) {
            out.add(norm(it.next()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> drainMap(MapIterator<?, ?> mi) {
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < 64 && mi.hasNext(); i++) {
            Object k = mi.next();
            out.add(k + "=" + mi.getValue()
                    + (pred
                            ? attempt(() -> ((MapIterator<Object, Object>) mi).setValue(REJECTED))
                            : "")
                    + attempt(() -> ((MapIterator<Object, Object>) mi).setValue(mi.getValue())));
        }
        return out;
    }

    private static String attempt(Runnable r) {
        try {
            r.run();
            return "/ok";
        } catch (RuntimeException e) {
            return "/" + e.getClass().getSimpleName() + (pred ? message(e) : "");
        }
    }

    /**
     * v3: interface shape of a returned view (a SortedSet view must not become a NavigableSet,
     * ...).
     */
    private static final List<Class<?>> SHAPES = List.of(List.class, Set.class, SortedSet.class,
            NavigableSet.class, Queue.class, Deque.class, RandomAccess.class,
            Serializable.class, Bag.class, SortedBag.class, MultiSet.class, SortedMap.class,
            NavigableMap.class, IterableMap.class, OrderedMap.class, BidiMap.class);

    private static String shape(Object v) {
        StringBuilder bits = new StringBuilder("<");
        SHAPES.forEach(c -> bits.append(c.isInstance(v) ? '1' : '0'));
        return bits.append('>').toString();
    }

    /**
     * v3: messages of IAE and of non-helpful NPEs (requireNonNull names) are contract; helpful
     * NPE text depends on local names.
     */
    static String message(Throwable t) {
        boolean keep = t instanceof IllegalArgumentException || t instanceof NullPointerException
                && t.getMessage() != null && !t.getMessage().contains("because");
        return keep ? ":" + t.getMessage() : "";
    }

    private void compare(String where, Object oracle, Object m3) {
        checks++;
        if (!String.valueOf(oracle).equals(String.valueOf(m3))) {
            mismatches.add(where + "\toracle=" + oracle + "\tm3=" + m3);
        }
    }
}
