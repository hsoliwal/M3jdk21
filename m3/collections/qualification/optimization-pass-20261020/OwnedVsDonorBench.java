/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.m3.collections.apache.M3DualLinkedHashBidiMap;
import com.m3.collections.apache.M3HashBag;
import com.m3.collections.guava.M3ArrayListMultimap;
import com.m3.collections.guava.M3EnumBiMap;
import com.m3.collections.guava.M3EnumHashBiMap;
import com.m3.collections.guava.M3EnumMultiset;
import com.m3.collections.guava.M3HashMultiset;
import com.m3.collections.guava.M3LinkedHashMultiset;
import com.m3.collections.spring.M3MultiValueMapAdapter;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ToLongBiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/**
 * Optimization pass, step 1 (measure first): each owned collection against its donor type at 10^5 and 10^6.
 *
 * <p>Per implementation and size: build time (fill a fresh instance), query time (n lookups plus one full
 * traversal), both as the median of 5 runs after warm-up; bytes allocated per element while building and per
 * query (ThreadMXBean); and heap retained per element after GC (two instances at 10^6, ten at 10^5). Keys and
 * values are shared pre-boxed objects, so they are not counted. Enum-keyed owners hold at most one mapping per
 * constant (64), so their size is an operation count and their retained figure is per instance (10,000 copies). Every
 * measurement runs in its own JVM (see main); results go to a TSV, progress through java.util.logging.
 */
public final class OwnedVsDonorBench {
    private static final Logger LOG = logger();
    private static final Integer[] BOX = boxes(2_000_000);
    private static final Key[] KEYS = Key.values();
    private static final Val[] VALS = Val.values();
    private static final int REPS = 5;
    /** Keeps query results observable so the JIT cannot drop the loops. */
    static volatile long SINK;

    enum Key { K00, K01, K02, K03, K04, K05, K06, K07, K08, K09, K10, K11, K12, K13, K14, K15, K16, K17, K18, K19, K20, K21, K22, K23, K24, K25, K26, K27, K28, K29, K30, K31, K32, K33, K34, K35, K36, K37, K38, K39, K40, K41, K42, K43, K44, K45, K46, K47, K48, K49, K50, K51, K52, K53, K54, K55, K56, K57, K58, K59, K60, K61, K62, K63 }

    enum Val { V00, V01, V02, V03, V04, V05, V06, V07, V08, V09, V10, V11, V12, V13, V14, V15, V16, V17, V18, V19, V20, V21, V22, V23, V24, V25, V26, V27, V28, V29, V30, V31, V32, V33, V34, V35, V36, V37, V38, V39, V40, V41, V42, V43, V44, V45, V46, V47, V48, V49, V50, V51, V52, V53, V54, V55, V56, V57, V58, V59, V60, V61, V62, V63 }

    /** One implementation: build a filled instance of size n; run n queries plus a traversal, returning a sink. */
    record Impl(String name, IntFunction<Object> build, ToLongBiFunction<Object, Integer> query) {
    }

    /** An owner and its donor; perElement false means the retained figure is per instance. */
    record Case(String owner, String donorType, String operation, boolean perElement, Impl donor, Impl owned) {
    }

    private OwnedVsDonorBench() {
    }

    static final String HEADER = "owner\tdonor_type\toperation\tsize\timpl\tbuild_ms\tquery_ms\tbuild_alloc_B_per_elem\tquery_alloc_B_per_op\tretained_B\tretained_unit";

    /**
     * One measurement per JVM, so call-site profiles never mix the donor and the owned class:
     * {@code <out.tsv> <size> <owner> <donor|owned>}; the row is appended (the header is written once).
     */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        int n = Integer.parseInt(args[1]);
        Case c = cases().stream().filter(x -> x.owner().equals(args[2])).findFirst().orElseThrow(() -> new IllegalArgumentException(args[2]));
        Impl impl = args[3].equals("donor") ? c.donor() : c.owned();
        String row = measure(c, impl, n);
        LOG.info(row);
        List<String> rows = new ArrayList<>(Files.exists(out) ? Files.readAllLines(out, StandardCharsets.UTF_8) : List.of(HEADER));
        rows.add(row);
        Files.writeString(out, String.join("\n", rows) + "\n", StandardCharsets.UTF_8);
    }

    /** Median build/query times and minimum allocations over REPS runs after warm-up; no instance outlives the call. */
    private static double[] timing(Impl impl, int n) {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().threadId();
        long sink = 0;
        for (int w = 0; w < 5; w++) { sink += impl.query().applyAsLong(impl.build().apply(n), n); }
        double[] build = new double[REPS];
        double[] query = new double[REPS];
        long buildAlloc = Long.MAX_VALUE;
        long queryAlloc = Long.MAX_VALUE;
        for (int r = 0; r < REPS; r++) {
            long a0 = bean.getThreadAllocatedBytes(id);
            long t0 = System.nanoTime();
            Object s = impl.build().apply(n);
            long t1 = System.nanoTime();
            long a1 = bean.getThreadAllocatedBytes(id);
            sink += impl.query().applyAsLong(s, n);
            long t2 = System.nanoTime();
            long a2 = bean.getThreadAllocatedBytes(id);
            build[r] = (t1 - t0) / 1e6;
            query[r] = (t2 - t1) / 1e6;
            buildAlloc = Math.min(buildAlloc, a1 - a0);
            queryAlloc = Math.min(queryAlloc, a2 - a1);
        }
        SINK += sink;
        return new double[] {median(build), median(query), buildAlloc / (double) n, queryAlloc / (double) n};
    }

    /** Heap retained by {@code copies} instances of size {@code fill}; called after timing() has returned. */
    private static long retained(Impl impl, int fill, int copies) {
        Object[] hold = new Object[copies];
        long base = usedAfterGc();
        for (int i = 0; i < copies; i++) { hold[i] = impl.build().apply(fill); }
        long retained = usedAfterGc() - base;
        Reference.reachabilityFence(hold);
        return retained;
    }

    private static String measure(Case c, Impl impl, int n) {
        double[] t = timing(impl, n);
        double retainedUnit;
        if (c.perElement()) {
            int copies = n >= 1_000_000 ? 2 : 10;
            retainedUnit = retained(impl, n, copies) / ((double) copies * n);
        } else {
            // Enum-keyed owners: the footprint does not grow with the operation count, so measure many small instances.
            retainedUnit = retained(impl, 640, 10_000) / 10_000.0;
        }
        return String.join("\t", c.owner(), c.donorType(), c.operation(), Integer.toString(n), impl.name(), fmt(t[0]), fmt(t[1]),
                fmt(t[2]), fmt(t[3]), fmt(retainedUnit), c.perElement() ? "B/element" : "B/instance");
    }

    private static List<Case> cases() {
        return List.of(
                multiset("M3HashMultiset", "com.google.common.collect.HashMultiset", "guava", HashMultiset::create, "m3", M3HashMultiset::create),
                multiset("M3LinkedHashMultiset", "com.google.common.collect.LinkedHashMultiset", "guava", LinkedHashMultiset::create, "m3",
                        M3LinkedHashMultiset::create),
                new Case("M3HashBag", "org.apache.commons.collections4.bag.HashBag", "add n distinct + n/2 repeats; getCount n; iterate", true,
                        bag("commons", HashBag::new), bag("m3", M3HashBag::new)),
                new Case("M3EnumMultiset", "com.google.common.collect.EnumMultiset", "n add over 64 constants; count n; iterate", false,
                        enumMultiset("guava", () -> EnumMultiset.create(Key.class)), enumMultiset("m3", () -> M3EnumMultiset.create(Key.class))),
                new Case("M3ArrayListMultimap", "com.google.common.collect.ArrayListMultimap", "put n over n/10 keys; get(k).size n; iterate entries", true,
                        multimap("guava", ArrayListMultimap::create), multimap("m3", M3ArrayListMultimap::create)),
                new Case("M3DualLinkedHashBidiMap", "org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap", "put n; get + getKey n; mapIterator", true,
                        bidi("commons", DualLinkedHashBidiMap::new), bidi("m3", M3DualLinkedHashBidiMap::new)),
                new Case("M3MultiValueMapAdapter", "org.springframework.util.MultiValueMapAdapter", "add n over n/10 keys; getFirst n; forEach", true,
                        multiValue("spring", () -> new MultiValueMapAdapter<>(new LinkedHashMap<>())),
                        multiValue("m3", () -> new M3MultiValueMapAdapter<>(new LinkedHashMap<>()))),
                new Case("M3EnumHashBiMap", "com.google.common.collect.EnumHashBiMap", "n put over 64 keys; get + inverse.get n; iterate", false,
                        enumHashBiMap("guava", () -> EnumHashBiMap.create(Key.class)), enumHashBiMap("m3", () -> M3EnumHashBiMap.create(Key.class))),
                new Case("M3EnumBiMap", "com.google.common.collect.EnumBiMap", "n put over 64 keys; get + inverse.get n; iterate", false,
                        enumBiMap("guava", () -> EnumBiMap.create(Key.class, Val.class)), enumBiMap("m3", () -> M3EnumBiMap.create(Key.class, Val.class))));
    }

    @FunctionalInterface
    interface Maker<T> {
        T make();
    }

    private static Case multiset(String owner, String donorType, String dn, Maker<Multiset<Integer>> donor, String on, Maker<Multiset<Integer>> owned) {
        return new Case(owner, donorType, "add n distinct + n/2 repeats; count n; iterate", true, multisetImpl(dn, donor), multisetImpl(on, owned));
    }

    private static Impl multisetImpl(String name, Maker<Multiset<Integer>> make) {
        return new Impl(name, n -> {
            Multiset<Integer> m = make.make();
            for (int i = 0; i < n; i++) { m.add(BOX[i]); }
            for (int i = 0; i < n; i += 2) { m.add(BOX[i]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") Multiset<Integer> m = (Multiset<Integer>) o;
            long s = 0;
            for (int i = 0; i < n; i++) { s += m.count(BOX[i]); }
            for (Multiset.Entry<Integer> e : m.entrySet()) { s += e.getCount(); }
            return s;
        });
    }

    private static Impl bag(String name, Maker<Bag<Integer>> make) {
        return new Impl(name, n -> {
            Bag<Integer> m = make.make();
            for (int i = 0; i < n; i++) { m.add(BOX[i]); }
            for (int i = 0; i < n; i += 2) { m.add(BOX[i]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") Bag<Integer> m = (Bag<Integer>) o;
            long s = 0;
            for (int i = 0; i < n; i++) { s += m.getCount(BOX[i]); }
            for (Integer x : m.uniqueSet()) { s += x; }
            return s;
        });
    }

    private static Impl enumMultiset(String name, Maker<Multiset<Key>> make) {
        return new Impl(name, n -> {
            Multiset<Key> m = make.make();
            for (int i = 0; i < n; i++) { m.add(KEYS[i & 63]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") Multiset<Key> m = (Multiset<Key>) o;
            long s = 0;
            for (int i = 0; i < n; i++) { s += m.count(KEYS[i & 63]); }
            for (Multiset.Entry<Key> e : m.entrySet()) { s += e.getCount(); }
            return s;
        });
    }

    private static Impl multimap(String name, Maker<ListMultimap<Integer, Integer>> make) {
        return new Impl(name, n -> {
            ListMultimap<Integer, Integer> m = make.make();
            int keys = Math.max(1, n / 10);
            for (int i = 0; i < n; i++) { m.put(BOX[i % keys], BOX[i]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") ListMultimap<Integer, Integer> m = (ListMultimap<Integer, Integer>) o;
            int keys = Math.max(1, n / 10);
            long s = 0;
            for (int i = 0; i < n; i++) { s += m.get(BOX[i % keys]).size(); }
            for (java.util.Map.Entry<Integer, Integer> e : m.entries()) { s += e.getValue(); }
            return s;
        });
    }

    private static Impl bidi(String name, Maker<BidiMap<Integer, Integer>> make) {
        return new Impl(name, n -> {
            BidiMap<Integer, Integer> m = make.make();
            for (int i = 0; i < n; i++) { m.put(BOX[i], BOX[i + n]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") BidiMap<Integer, Integer> m = (BidiMap<Integer, Integer>) o;
            long s = 0;
            for (int i = 0; i < n; i++) { s += m.get(BOX[i]) + m.getKey(BOX[i + n]); }
            for (org.apache.commons.collections4.MapIterator<Integer, Integer> it = m.mapIterator(); it.hasNext();) { s += it.next(); }
            return s;
        });
    }

    private static Impl multiValue(String name, Maker<MultiValueMap<Integer, Integer>> make) {
        return new Impl(name, n -> {
            MultiValueMap<Integer, Integer> m = make.make();
            int keys = Math.max(1, n / 10);
            for (int i = 0; i < n; i++) { m.add(BOX[i % keys], BOX[i]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") MultiValueMap<Integer, Integer> m = (MultiValueMap<Integer, Integer>) o;
            int keys = Math.max(1, n / 10);
            long[] s = new long[1];
            for (int i = 0; i < n; i++) { s[0] += m.getFirst(BOX[i % keys]); }
            m.forEach((k, v) -> s[0] += v.size());
            return s[0];
        });
    }

    private static Impl enumHashBiMap(String name, Maker<BiMap<Key, Integer>> make) {
        return new Impl(name, n -> {
            BiMap<Key, Integer> m = make.make();
            for (int i = 0; i < n; i++) { m.put(KEYS[i & 63], BOX[i & 63]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") BiMap<Key, Integer> m = (BiMap<Key, Integer>) o;
            long s = 0;
            BiMap<Integer, Key> inverse = m.inverse();
            for (int i = 0; i < n; i++) { s += m.get(KEYS[i & 63]) + inverse.get(BOX[i & 63]).ordinal(); }
            for (Integer v : m.values()) { s += v; }
            return s;
        });
    }

    private static Impl enumBiMap(String name, Maker<BiMap<Key, Val>> make) {
        return new Impl(name, n -> {
            BiMap<Key, Val> m = make.make();
            for (int i = 0; i < n; i++) { m.put(KEYS[i & 63], VALS[i & 63]); }
            return m;
        }, (o, n) -> {
            @SuppressWarnings("unchecked") BiMap<Key, Val> m = (BiMap<Key, Val>) o;
            long s = 0;
            BiMap<Val, Key> inverse = m.inverse();
            for (int i = 0; i < n; i++) { s += m.get(KEYS[i & 63]).ordinal() + inverse.get(VALS[i & 63]).ordinal(); }
            for (Val v : m.values()) { s += v.ordinal(); }
            return s;
        });
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static long usedAfterGc() {
        for (int i = 0; i < 3; i++) { ManagementFactory.getMemoryMXBean().gc(); }
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static Integer[] boxes(int n) {
        Integer[] b = new Integer[n];
        for (int i = 0; i < n; i++) { b[i] = Integer.valueOf(1_000_000 + i); }
        return b;
    }

    private static Logger logger() {
        Logger log = Logger.getLogger(OwnedVsDonorBench.class.getName());
        log.setUseParentHandlers(false);
        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new Formatter() {
            @Override public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });
        log.addHandler(console);
        return log;
    }
}
