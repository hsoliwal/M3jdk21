// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//FLLDCIM: DIFFERENTIAL - driver of recipe/graph-trie-20261106/differential.sh; reflective, so one class drives the donor and the relocated graph and trie families
/**
 * Reflective dump driver of the graph-and-trie differential gate (not a test): replays seeded random graphs through
 * the five public static graph methods (label {@code M3GraphAlgorithms}) and a seeded workload of inserts, removes,
 * lookups and word counts through trie instances (label {@code M3StringTrie}), printing one line per case: inputs,
 * monitor, result (maps sorted by key, since {@code Map.copyOf} iterates in a per-JVM salted order) or exception, and
 * the progress trace the monitor saw. Long values are printed as length and sha256 (a graph's vertex and edge counts
 * stay readable before the digest of its edge list). Sizes are bounded: at most 2,000
 * vertices and 20,000 edges per graph, most graphs small, so a run of 100,000 graph cases ends in well under two
 * minutes; every call runs under a timeout on a daemon thread, so a hang is a recorded TIMEOUT, not a dead gate.
 *
 * <p>usage: {@code M3GraphTrieDump --cases N --seed S [--trie-ops M] M3GraphAlgorithms=fqn M3StringTrie=fqn
 * M3ProgressMonitor=fqn [Label=fqn]...} (other labels are listed as unused).
 */
public final class GraphTrieDump {
    static final long SEED = 20261106L;
    private static final int DEFAULT_CASES = 100_000;
    private static final int DEFAULT_TRIE_OPS = 100_000;
    private static final int MAX_VERTICES = 2_000;
    private static final int MAX_EDGES = 20_000;
    private static final int SMALL_VERTICES = 12;
    private static final int MEDIUM_VERTICES = 200;
    private static final int SMALL_PER_MILLE = 990;
    private static final int MEDIUM_PER_MILLE = 999;
    private static final long TIMEOUT_MILLIS = 10_000L;
    private static final int LONG_VALUE = 240;
    private static final int MONITOR_CANCEL_PERCENT = 30;
    private static final int MAX_WORD_LENGTH = 8;
    private static final int MAX_ROUND_OPS = 2_000;
    private static final String[] GRAPH_METHODS = {
        "breadthFirst", "shortestPaths", "topologicalOrder", "dijkstra", "stronglyConnectedComponents"
    };
    private static final long[] WEIGHT_EXTREMES = {
        Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE / 2, Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE / 2 - 1,
        Integer.MAX_VALUE, 0L
    };
    /** Code points of the trie alphabet: ASCII, Latin-1, CJK, three supplementary (two emoji, U+10FFFF). */
    private static final int[] ALPHABET = {'a', 'b', 'c', 0xE9, 0x4E2D, 0x1F600, 0x1F4A1, 0x10FFFF};
    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();

    private final PrintWriter out;
    private final long seed;
    private final Class<?> monitorType;
    private ExecutorService executor = newExecutor();
    private long cases;
    private long thrown;
    private long timeouts;
    private int maxVertices;
    private int maxEdges;
    private long edgesTotal;

    private GraphTrieDump(PrintWriter out, long seed, Class<?> monitorType) {
        this.out = out;
        this.seed = seed;
        this.monitorType = monitorType;
    }

    public static void main(String[] args) throws Exception {
        int cases = DEFAULT_CASES;
        int trieOps = DEFAULT_TRIE_OPS;
        long seed = SEED;
        Map<String, String> targets = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cases" -> cases = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--trie-ops" -> trieOps = Integer.parseInt(args[++i]);
                default -> {
                    String[] pair = args[i].split("=", 2);
                    targets.put(pair[0], pair[1]);
                }
            }
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), 1 << 20));
        GraphTrieDump dump = new GraphTrieDump(out, seed, Class.forName(targets.get("ProgressMonitor")));
        for (String label : targets.keySet()) {
            if (!label.equals("GraphAlgorithms") && !label.equals("StringTrie")
                    && !label.equals("ProgressMonitor")) {
                out.print("SKIP unused " + label + "\n");
            }
        }
        dump.graphs(Class.forName(targets.get("GraphAlgorithms")), cases);
        dump.tries(Class.forName(targets.get("StringTrie")), trieOps);
        out.print("TOTAL graph_cases=" + cases + " trie_ops=" + trieOps + " cases=" + dump.cases + " throws="
                + dump.thrown + " timeouts=" + dump.timeouts + " max_vertices=" + dump.maxVertices + " max_edges="
                + dump.maxEdges + " edges_total=" + dump.edgesTotal + "\n");
        out.flush();
        System.exit(0);
    }

    private static ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dump");
            thread.setDaemon(true);
            return thread;
        });
    }

    // ---------------------------------------------------------------- graphs

    private void graphs(Class<?> graphType, int count) throws Exception {
        Class<?> edgeType = Arrays.stream(graphType.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("WeightedEdge")).findFirst().orElseThrow();
        Constructor<?> edge = edgeType.getConstructor(Object.class, long.class);
        Method[] methods = new Method[GRAPH_METHODS.length];
        for (int m = 0; m < methods.length; m++) {
            String name = GRAPH_METHODS[m];
            boolean comparator = name.equals("topologicalOrder") || name.equals("stronglyConnectedComponents");
            methods[m] = graphType.getMethod(name, Map.class, comparator ? Comparator.class : Object.class,
                    monitorType);
        }
        for (int c = 0; c < count; c++) {
            int m = c % methods.length;
            SplittableRandom random = new SplittableRandom(seed * 1_000_003L + m * 7919L + c);
            graphCase("GraphAlgorithms", methods[m], edge, c, random);
        }
    }

    private void graphCase(String label, Method method, Constructor<?> edge, int index, SplittableRandom random)
            throws Exception {
        String name = method.getName();
        int tier = random.nextInt(1000);
        int vertices = tier < SMALL_PER_MILLE ? random.nextInt(1, SMALL_VERTICES + 1)
                : tier < MEDIUM_PER_MILLE ? random.nextInt(SMALL_VERTICES + 1, MEDIUM_VERTICES + 1)
                : random.nextInt(MEDIUM_VERTICES + 1, MAX_VERTICES + 1);
        int edgeBound = Math.min(MAX_EDGES, vertices * (tier < SMALL_PER_MILLE ? 3 : tier < MEDIUM_PER_MILLE ? 5 : 10));
        int edges = random.nextInt(edgeBound + 1);
        boolean dag = name.equals("topologicalOrder") ? random.nextInt(10) < 6 : random.nextInt(10) < 3;
        boolean split = random.nextInt(5) == 0;
        boolean allKeys = random.nextInt(5) != 0;
        boolean weighted = name.equals("dijkstra");
        int cut = split ? random.nextInt(1, vertices + 1) : vertices;
        int[] from = new int[edges];
        int[] to = new int[edges];
        long[] weight = new long[edges];
        for (int e = 0; e < edges; e++) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (split && (a < cut) != (b < cut)) {
                b = a;
            }
            if (dag && a > b) {
                int swap = a;
                a = b;
                b = swap;
            }
            if (dag && a == b) {
                b = Math.min(vertices - 1, a + 1);
                if (a == b) {
                    a = Math.max(0, b - 1);
                }
            }
            from[e] = a;
            to[e] = b;
            weight[e] = weighted ? weight(random) : 0L;
        }
        StringBuilder shown = new StringBuilder(dag ? "dag" : "any").append(split ? " split@" + cut : "").append(allKeys ? " keys=all" : " keys=sources")
                .append(" edges=[");
        for (int e = 0; e < edges; e++) {
            shown.append(e == 0 ? "" : ",").append(from[e]).append('>').append(to[e]);
            if (weighted) {
                shown.append(':').append(weight[e]);
            }
        }
        shown.append(']');
        Map<Integer, List<Object>> graph = new LinkedHashMap<>();
        if (allKeys) {
            for (int v = 0; v < vertices; v++) {
                graph.put(v, new ArrayList<>());
            }
        }
        String construction = null;
        for (int e = 0; e < edges && construction == null; e++) {
            Object target = weighted ? newEdge(edge, to[e], weight[e]) : Integer.valueOf(to[e]);
            if (target instanceof String refused) {
                construction = refused;
            } else {
                graph.computeIfAbsent(from[e], ignored -> new ArrayList<>()).add(target);
            }
        }
        int k = random.nextInt(100) < MONITOR_CANCEL_PERCENT ? random.nextInt(vertices * 2 + 2) : -1;
        Trace trace = new Trace(k, -1);
        Object monitor = k < 0 ? none() : proxy(trace);
        boolean comparator = method.getParameterTypes()[1] == Comparator.class;
        Object second = comparator ? NATURAL : Integer.valueOf(random.nextInt(20) == 0 ? vertices + 1 : random.nextInt(vertices));
        shown.append(comparator ? " natural" : " start=" + second).append(k < 0 ? " none" : " cancel@" + k);
        maxVertices = Math.max(maxVertices, vertices);
        maxEdges = Math.max(maxEdges, edges);
        edgesTotal += edges;
        StringBuilder line = new StringBuilder(label).append('.').append(name).append(' ').append(index).append(" | V=")
                .append(vertices).append(" E=").append(edges).append(' ').append(digest(shown.toString())).append(" -> ");
        cases++;
        if (construction != null) {
            thrown++;
            line.append("construct throws ").append(construction);
        } else {
            Object[] argv = {graph, second, monitor};
            invoke(method, null, argv, line);
            line.append(k < 0 ? "" : " " + trace);
        }
        out.print(line.append('\n'));
    }

    private static long weight(SplittableRandom random) {
        int kind = random.nextInt(100);
        if (kind < 10) {
            return 0L;
        }
        if (kind < 30) {
            return WEIGHT_EXTREMES[random.nextInt(WEIGHT_EXTREMES.length)];
        }
        if (kind < 31) {
            return -random.nextLong(1L, 5L);
        }
        return random.nextLong(1L, 101L);
    }

    /** The edge, or the refusal of its compact constructor as "Type: message". */
    private static Object newEdge(Constructor<?> edge, int to, long weight) throws Exception {
        try {
            return edge.newInstance(Integer.valueOf(to), weight);
        } catch (InvocationTargetException refused) {
            Throwable cause = refused.getCause();
            return cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
    }

    // ----------------------------------------------------------------- tries

    private void tries(Class<?> trieType, int budget) throws Exception {
        Method insert = trieType.getMethod("insert", String.class, monitorType);
        Method contains = trieType.getMethod("contains", String.class, monitorType);
        Method startsWith = trieType.getMethod("startsWith", String.class, monitorType);
        Method remove = trieType.getMethod("remove", String.class, monitorType);
        Method wordCount = trieType.getMethod("wordCount", monitorType);
        int done = 0;
        int round = 0;
        while (done < budget) {
            SplittableRandom random = new SplittableRandom(seed * 1_000_003L + 104_729L * (round + 1));
            Object trie = trieType.getConstructor().newInstance();
            int ops = Math.min(budget - done, random.nextInt(1, MAX_ROUND_OPS + 1));
            for (int op = 0; op < ops; op++) {
                int kind = random.nextInt(100);
                Method method = kind < 45 ? insert : kind < 60 ? remove : kind < 75 ? contains
                        : kind < 85 ? startsWith : wordCount;
                trieOp("StringTrie", trie, method, startsWith, round, done + op, random);
            }
            done += ops;
            round++;
        }
    }

    private void trieOp(String label, Object trie, Method method, Method startsWith, int round, int index,
            SplittableRandom random) throws Exception {
        String name = method.getName();
        String word = method.getParameterCount() == 1 ? null : word(random);
        int length = word == null ? 0 : word.codePointCount(0, word.length());
        int chance = random.nextInt(100);
        int cancelAt = -1;
        int failAt = -1;
        if (name.equals("insert") && chance < 15 || name.equals("remove") && chance < 10 || chance < 2) {
            cancelAt = random.nextInt(length * 2 + 2);
        } else if (name.equals("insert") && chance < 20) {
            failAt = random.nextInt(1, length + 2);
        }
        Trace trace = new Trace(cancelAt, failAt);
        Object monitor = cancelAt < 0 && failAt < 0 ? none() : proxy(trace);
        StringBuilder line = new StringBuilder(label).append('.').append(name).append(" r").append(round).append(' ')
                .append(index).append(" | ").append(word == null ? "" : quote(word) + " ")
                .append(cancelAt >= 0 ? "cancel@" + cancelAt : failAt >= 0 ? "fail@" + failAt : "none").append(" -> ");
        cases++;
        Object[] argv = word == null ? new Object[] {monitor} : new Object[] {word, monitor};
        boolean completed = invoke(method, trie, argv, line);
        line.append(monitor == none() ? "" : " " + trace);
        out.print(line.append('\n'));
        if (!completed && name.equals("insert")) {
            prefixes(label, trie, startsWith, word, round, index);
        }
    }

    /** After a failed insert every prefix of the word is probed: the atomic insert publishes none of them anew. */
    private void prefixes(String label, Object trie, Method startsWith, String word, int round, int index)
            throws Exception {
        int[] codePoints = word.codePoints().toArray();
        for (int end = 1; end <= codePoints.length; end++) {
            String prefix = new String(codePoints, 0, end);
            StringBuilder line = new StringBuilder(label).append(".startsWith r").append(round).append(' ').append(index)
                    .append(" after-failed-insert | ").append(quote(prefix)).append(" none -> ");
            cases++;
            invoke(startsWith, trie, new Object[] {prefix, none()}, line);
            out.print(line.append('\n'));
        }
    }

    private static String word(SplittableRandom random) {
        int length = random.nextInt(MAX_WORD_LENGTH + 1);
        StringBuilder word = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            word.appendCodePoint(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return word.toString();
    }

    private static String quote(String word) {
        StringBuilder quoted = new StringBuilder("\"").append(word).append("\" cps=[");
        int[] codePoints = word.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            quoted.append(i == 0 ? "" : ",").append(Integer.toHexString(codePoints[i]));
        }
        return quoted.append(']').toString();
    }

    // -------------------------------------------------------------- invoking

    /** Appends the result or the exception; true when the call completed normally. */
    private boolean invoke(Method method, Object receiver, Object[] argv, StringBuilder line) throws Exception {
        Future<Object> future = executor.submit(() -> method.invoke(receiver, argv));
        try {
            Object result = future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            line.append(digest(show(result)));
            return true;
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof InvocationTargetException invocation) {
                cause = invocation.getCause();
            }
            thrown++;
            line.append("throws ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
            return false;
        } catch (TimeoutException timeout) {
            timeouts++;
            future.cancel(true);
            executor.shutdownNow();
            executor = newExecutor();
            line.append("TIMEOUT");
            return false;
        }
    }

    private static String show(Object value) {
        if (value instanceof Map<?, ?> map) {
            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Comparator.comparingInt(entry -> (Integer) entry.getKey()));
            StringBuilder text = new StringBuilder("{");
            for (Map.Entry<?, ?> entry : entries) {
                text.append(text.length() == 1 ? "" : ", ").append(entry.getKey()).append('=').append(entry.getValue());
            }
            return text.append('}').toString();
        }
        return String.valueOf(value);
    }

    private static String digest(String text) {
        if (text.length() <= LONG_VALUE) {
            return text;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return "len=" + text.length() + " sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    // -------------------------------------------------------------- monitors

    private Object none() throws Exception {
        return monitorType.getMethod("none").invoke(null);
    }

    /** The progress trace a proxy monitor records: begins, ticks, dones, cancellation checks, the task names. */
    private static final class Trace {
        final int cancelAt;
        final int failAt;
        int begins;
        int ticks;
        int dones;
        int checks;
        String task = "";
        long total;

        Trace(int cancelAt, int failAt) {
            this.cancelAt = cancelAt;
            this.failAt = failAt;
        }

        @Override
        public String toString() {
            return "trace[begins=" + begins + " task=" + task + " total=" + total + " ticks=" + ticks + " checks="
                    + checks + " dones=" + dones + "]";
        }
    }

    /**
     * A monitor over the labelled interface: cancelled once {@code cancelAt} work units were reported (checked by
     * {@code isCanceled}), or failing with IllegalStateException inside {@code worked} once {@code failAt} units were
     * reported; {@code checkCanceled} runs the interface's default (CancellationException when cancelled).
     */
    private Object proxy(Trace trace) {
        InvocationHandler handler = (proxy, method, a) -> switch (method.getName()) {
            case "isCanceled" -> {
                trace.checks++;
                yield trace.cancelAt >= 0 && trace.ticks >= trace.cancelAt;
            }
            case "worked" -> {
                trace.ticks += ((Number) a[0]).intValue();
                if (trace.failAt >= 0 && trace.ticks >= trace.failAt) {
                    throw new IllegalStateException("monitor failed");
                }
                yield null;
            }
            case "beginTask" -> {
                trace.begins++;
                trace.task = String.valueOf(a[0]);
                trace.total = ((Number) a[1]).longValue();
                yield null;
            }
            case "done" -> {
                trace.dones++;
                yield null;
            }
            case "checkCanceled" -> InvocationHandler.invokeDefault(proxy, method, a);
            case "toString" -> trace.toString();
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == a[0];
            default -> method.getReturnType() == boolean.class ? Boolean.FALSE
                    : method.getReturnType() == int.class ? Integer.valueOf(0) : null;
        };
        return Proxy.newProxyInstance(monitorType.getClassLoader(), new Class<?>[] {monitorType}, handler);
    }
}
