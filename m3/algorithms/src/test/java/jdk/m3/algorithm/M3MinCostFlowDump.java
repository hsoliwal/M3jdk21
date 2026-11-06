// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

//FLLDCIM: DIFFERENTIAL - driver of recipe/mincost-flow-20261106/differential.sh; reflective, so one class drives the donor solver, the relocated solver and the unchosen donor solver
/**
 * Reflective dump driver of the min-cost-flow differential gate (not a test). It generates seeded networks across
 * the chosen solver's contract (2..64 vertices, 0..256 edges, capacities and demand 0..1000, unit costs
 * 0..1,000,000) in six sections and hands every network to one consumer:
 * <ul>
 *   <li>{@code E} exhaustive tiny networks: 2..5 vertices with every ordered sequence of 0..2 edges (parallel edges
 *       included), capacities {0,1,2}, costs {0,1,5}, demand 0..4; plus 3 vertices with 3 edges and costs {0,1};</li>
 *   <li>{@code R} random networks up to the contract maximum (every 1000th exactly at it), random source and sink,
 *       one edge in eight parallel to its predecessor, one capacity and one cost in four zero;</li>
 *   <li>{@code Z} all-zero capacities, then a zero-cost directed ring through every vertex plus random edges;</li>
 *   <li>{@code P} heavy parallel edges between at most four vertices;</li>
 *   <li>{@code I} infeasible demand: demand 1000 over at most 8 vertices and 32 edges of capacity at most 9 (a smaller flow);</li>
 *   <li>{@code X} refusals: one deterministic case per contract clause (a negative cost, the only way to state a
 *       negative-cost cycle, is refused), then random networks with one corrupted field.</li>
 * </ul>
 * Dump mode prints one line per network (section, ordinal, shape, flow, cost and every edge flow, or the refusal)
 * and a TOTAL line. Cross mode ({@code --oracle}) runs the unchosen solver (constructor {@code (int, int)},
 * {@code addEdge(int, int, long, long)}, {@code minCostFlow(int, int, long)} returning {@code flow()} and
 * {@code cost()}) on every network the chosen solver accepts and compares flow value and cost; it has no per-edge
 * flow, and per-edge flows of two optimal solvers may differ anyway.
 *
 * <p>usage: {@code M3MinCostFlowDump --solver FQN [--random N] [--oracle FQN]}
 */
public final class M3MinCostFlowDump {
    /** Seed of every generator (the leaf's date); each section derives its own stream, so N changes only R. */
    private static final long SEED = 20261106L;
    private static final int MAX_VERTICES = 64;
    private static final int MAX_EDGES = 256;
    private static final int MAX_CAPACITY = 1000;
    private static final int MAX_DEMAND = 1000;
    private static final int MAX_UNIT_COST = 1_000_000;
    private static final int[] TINY_CAPACITIES = {0, 1, 2};
    private static final int[] TINY_COSTS = {0, 1, 5};
    private static final int[] TINY_COSTS_THREE_EDGES = {0, 1};
    private static final int TINY_MAX_DEMAND = 4;
    private static final int[] CAPACITY_BOUNDS = {2, 9, MAX_CAPACITY};
    private static final int[] COST_BOUNDS = {0, 20, MAX_UNIT_COST};
    private static final int CONTRACT_MAXIMUM_EVERY = 1000;
    private static final int SECTION_NETWORKS = 1000;
    private static final int DEFAULT_RANDOM = 100_000;
    private static final int INFEASIBLE_VERTICES = 8;
    private static final int INFEASIBLE_EDGES = 32;
    private static final int INFEASIBLE_CAPACITY = 9;
    private static final int CORRUPTIONS = 8;

    private M3MinCostFlowDump() { }

    /** One network in the chosen solver's column form. */
    record Network(int vertices, int[] from, int[] to, int[] capacity, int[] cost, int source, int sink, int demand) { }

    /** Receives every generated network. */
    interface Sink {
        void accept(char section, int ordinal, Network network) throws IOException, ReflectiveOperationException;
    }

    /** The chosen solver: static {@code solve(...)} returning {@code flow()}, {@code cost()}, {@code edgeFlows()}. */
    record Solver(Method solve, Method flow, Method cost, Method edgeFlows) {
        static Solver of(String name) throws ReflectiveOperationException {
            Method solve = Class.forName(name).getMethod("solve", int.class, int[].class, int[].class, int[].class,
                    int[].class, int.class, int.class, int.class);
            Class<?> result = solve.getReturnType();
            return new Solver(solve, result.getMethod("flow"), result.getMethod("cost"), result.getMethod("edgeFlows"));
        }

        Object run(Network x) throws IllegalAccessException, InvocationTargetException {
            return solve.invoke(null, x.vertices(), x.from(), x.to(), x.capacity(), x.cost(), x.source(), x.sink(),
                    x.demand());
        }
    }

    /** The unchosen solver: a builder with {@code addEdge} and {@code minCostFlow}. */
    record Oracle(Constructor<?> create, Method addEdge, Method minCostFlow, Method flow, Method cost) {
        static Oracle of(String name) throws ReflectiveOperationException {
            Class<?> type = Class.forName(name);
            Method run = type.getMethod("minCostFlow", int.class, int.class, long.class);
            return new Oracle(type.getConstructor(int.class, int.class),
                    type.getMethod("addEdge", int.class, int.class, long.class, long.class), run,
                    run.getReturnType().getMethod("flow"), run.getReturnType().getMethod("cost"));
        }

        long[] run(Network x) throws ReflectiveOperationException {
            Object instance = create.newInstance(x.vertices(), x.from().length);
            for (int edge = 0; edge < x.from().length; edge++) {
                addEdge.invoke(instance, x.from()[edge], x.to()[edge], (long) x.capacity()[edge], (long) x.cost()[edge]);
            }
            Object result = minCostFlow.invoke(instance, x.source(), x.sink(), (long) x.demand());
            return new long[] {(long) flow.invoke(result), (long) cost.invoke(result)};
        }
    }

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        String solverName = null;
        String oracleName = null;
        int random = DEFAULT_RANDOM;
        for (int index = 0; index + 1 < args.length; index += 2) {
            switch (args[index]) {
                case "--solver" -> solverName = args[index + 1];
                case "--oracle" -> oracleName = args[index + 1];
                case "--random" -> random = Integer.parseInt(args[index + 1]);
                default -> throw new IllegalArgumentException("unknown option " + args[index]);
            }
        }
        if (solverName == null) {
            throw new IllegalArgumentException("usage: M3MinCostFlowDump --solver FQN [--random N] [--oracle FQN]");
        }
        Writer out = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), 1 << 16);
        Solver solver = Solver.of(solverName);
        if (oracleName == null) {
            Dump dump = new Dump(solver, out);
            generate(dump, random);
            dump.total();
        } else {
            Cross cross = new Cross(solver, Oracle.of(oracleName));
            generate(cross, random);
            cross.report(out);
        }
        out.flush();
    }

    static void generate(Sink sink, int random) throws IOException, ReflectiveOperationException {
        exhaustive(sink);
        randomNetworks(sink, random, new SplittableRandom(SEED ^ 'R'));
        zeroCapacityAndZeroCostCycles(sink, new SplittableRandom(SEED ^ 'Z'));
        parallel(sink, new SplittableRandom(SEED ^ 'P'));
        infeasible(sink, new SplittableRandom(SEED ^ 'I'));
        refusals(sink, new SplittableRandom(SEED ^ 'X'));
    }

    static void exhaustive(Sink sink) throws IOException, ReflectiveOperationException {
        int ordinal = 0;
        for (int vertices = 2; vertices <= 5; vertices++) {
            for (int edges = 0; edges <= 2; edges++) {
                ordinal = tiny(sink, ordinal, vertices, edges, TINY_COSTS);
            }
        }
        tiny(sink, ordinal, 3, 3, TINY_COSTS_THREE_EDGES);
    }

    static int tiny(Sink sink, int ordinal, int vertices, int edges, int[] costs)
            throws IOException, ReflectiveOperationException {
        int pairs = vertices * (vertices - 1);
        long pairCombos = power(pairs, edges);
        long capacityCombos = power(TINY_CAPACITIES.length, edges);
        long costCombos = power(costs.length, edges);
        int next = ordinal;
        for (long p = 0; p < pairCombos; p++) {
            for (long k = 0; k < capacityCombos; k++) {
                for (long w = 0; w < costCombos; w++) {
                    for (int demand = 0; demand <= TINY_MAX_DEMAND; demand++) {
                        sink.accept('E', next++, decode(vertices, edges, p, k, w, costs, demand));
                    }
                }
            }
        }
        return next;
    }

    static Network decode(int vertices, int edges, long pairDigits, long capacityDigits, long costDigits, int[] costs,
            int demand) {
        int[] from = new int[edges], to = new int[edges], capacity = new int[edges], cost = new int[edges];
        long p = pairDigits, k = capacityDigits, w = costDigits;
        int pairs = vertices * (vertices - 1);
        for (int edge = 0; edge < edges; edge++) {
            int pair = (int) (p % pairs);
            p /= pairs;
            from[edge] = pair / (vertices - 1);
            int target = pair % (vertices - 1);
            to[edge] = target >= from[edge] ? target + 1 : target;
            capacity[edge] = TINY_CAPACITIES[(int) (k % TINY_CAPACITIES.length)];
            k /= TINY_CAPACITIES.length;
            cost[edge] = costs[(int) (w % costs.length)];
            w /= costs.length;
        }
        return new Network(vertices, from, to, capacity, cost, 0, vertices - 1, demand);
    }

    static long power(int base, int exponent) {
        long value = 1;
        for (int i = 0; i < exponent; i++) {
            value *= base;
        }
        return value;
    }

    static void randomNetworks(Sink sink, int count, SplittableRandom r)
            throws IOException, ReflectiveOperationException {
        for (int k = 0; k < count; k++) {
            boolean maximum = k % CONTRACT_MAXIMUM_EVERY == CONTRACT_MAXIMUM_EVERY - 1;
            int vertices = maximum ? MAX_VERTICES : 2 + r.nextInt(r.nextInt(MAX_VERTICES - 1) + 1);
            int edges = maximum ? MAX_EDGES : r.nextInt(MAX_EDGES + 1);
            int capacityBound = maximum ? MAX_CAPACITY : CAPACITY_BOUNDS[r.nextInt(CAPACITY_BOUNDS.length)];
            int costBound = maximum ? MAX_UNIT_COST : COST_BOUNDS[r.nextInt(COST_BOUNDS.length)];
            int source = r.nextInt(vertices);
            int sinkVertex = other(r, vertices, source);
            int demand = maximum || r.nextInt(10) == 0 ? MAX_DEMAND : r.nextInt(MAX_DEMAND + 1);
            sink.accept('R', k, network(r, vertices, edges, capacityBound, costBound, source, sinkVertex, demand, 8));
        }
    }

    static int other(SplittableRandom r, int vertices, int vertex) {
        int other = r.nextInt(vertices - 1);
        return other >= vertex ? other + 1 : other;
    }

    static Network network(SplittableRandom r, int vertices, int edges, int capacityBound, int costBound, int source,
            int sink, int demand, int parallelOneIn) {
        int[] from = new int[edges], to = new int[edges], capacity = new int[edges], cost = new int[edges];
        for (int edge = 0; edge < edges; edge++) {
            if (edge > 0 && r.nextInt(parallelOneIn) == 0) {
                from[edge] = from[edge - 1];
                to[edge] = to[edge - 1];
            } else {
                from[edge] = r.nextInt(vertices);
                to[edge] = other(r, vertices, from[edge]);
            }
            capacity[edge] = r.nextInt(4) == 0 ? 0 : r.nextInt(capacityBound + 1);
            cost[edge] = r.nextInt(4) == 0 ? 0 : r.nextInt(costBound + 1);
        }
        return new Network(vertices, from, to, capacity, cost, source, sink, demand);
    }

    static void zeroCapacityAndZeroCostCycles(Sink sink, SplittableRandom r)
            throws IOException, ReflectiveOperationException {
        for (int k = 0; k < SECTION_NETWORKS; k++) {
            Network x = network(r, 2 + r.nextInt(MAX_VERTICES - 1), r.nextInt(MAX_EDGES + 1), MAX_CAPACITY,
                    MAX_UNIT_COST, 0, 1, r.nextInt(MAX_DEMAND + 1), 8);
            Arrays.fill(x.capacity(), 0);
            sink.accept('Z', k, x);
        }
        for (int k = 0; k < SECTION_NETWORKS; k++) {
            int vertices = 2 + r.nextInt(MAX_VERTICES - 1);
            Network extra = network(r, vertices, r.nextInt(MAX_EDGES - vertices + 1), MAX_CAPACITY, 20, 0,
                    other(r, vertices, 0), r.nextInt(MAX_DEMAND + 1), 8);
            int edges = vertices + extra.from().length;
            int[] from = new int[edges], to = new int[edges], capacity = new int[edges], cost = new int[edges];
            for (int v = 0; v < vertices; v++) {
                from[v] = v;
                to[v] = (v + 1) % vertices;
                capacity[v] = r.nextInt(MAX_CAPACITY + 1);
            }
            System.arraycopy(extra.from(), 0, from, vertices, extra.from().length);
            System.arraycopy(extra.to(), 0, to, vertices, extra.from().length);
            System.arraycopy(extra.capacity(), 0, capacity, vertices, extra.from().length);
            System.arraycopy(extra.cost(), 0, cost, vertices, extra.from().length);
            sink.accept('Z', SECTION_NETWORKS + k,
                    new Network(vertices, from, to, capacity, cost, 0, extra.sink(), extra.demand()));
        }
    }

    static void parallel(Sink sink, SplittableRandom r) throws IOException, ReflectiveOperationException {
        for (int k = 0; k < SECTION_NETWORKS; k++) {
            int vertices = 2 + r.nextInt(3);
            sink.accept('P', k, network(r, vertices, 1 + r.nextInt(MAX_EDGES), CAPACITY_BOUNDS[r.nextInt(3)],
                    COST_BOUNDS[r.nextInt(3)], 0, vertices - 1, r.nextInt(MAX_DEMAND + 1), 2));
        }
    }

    static void infeasible(Sink sink, SplittableRandom r) throws IOException, ReflectiveOperationException {
        for (int k = 0; k < SECTION_NETWORKS; k++) {
            int vertices = 2 + r.nextInt(INFEASIBLE_VERTICES - 1);
            sink.accept('I', k, network(r, vertices, r.nextInt(INFEASIBLE_EDGES + 1), INFEASIBLE_CAPACITY, 20, 0,
                    vertices - 1, MAX_DEMAND, 8));
        }
    }

    static void refusals(Sink sink, SplittableRandom r) throws IOException, ReflectiveOperationException {
        int[] f = {0, 1}, t = {1, 2}, one = {1, 1};
        Network[] cases = {
            new Network(1, f, t, one, one, 0, 2, 1),
            new Network(MAX_VERTICES + 1, f, t, one, one, 0, 2, 1),
            new Network(3, f, t, one, one, 2, 2, 1),
            new Network(3, f, t, one, one, -1, 2, 1),
            new Network(3, f, t, one, one, 0, 3, 1),
            new Network(3, f, t, one, one, 0, 2, -1),
            new Network(3, f, t, one, one, 0, 2, MAX_DEMAND + 1),
            new Network(3, f, new int[] {0, 2}, one, one, 0, 2, 1),
            new Network(3, f, new int[] {1, 3}, one, one, 0, 2, 1),
            new Network(3, new int[] {-1, 1}, t, one, one, 0, 2, 1),
            new Network(3, f, t, new int[] {-1, 1}, one, 0, 2, 1),
            new Network(3, f, t, new int[] {MAX_CAPACITY + 1, 1}, one, 0, 2, 1),
            new Network(3, f, t, one, new int[] {-1, 1}, 0, 2, 1),
            new Network(3, new int[] {0, 1, 2}, new int[] {1, 2, 1}, new int[] {1, 1, 1}, new int[] {0, -2, 1}, 0, 2, 1),
            new Network(3, f, t, one, new int[] {MAX_UNIT_COST + 1, 1}, 0, 2, 1),
            network(r, 3, MAX_EDGES + 1, 2, 2, 0, 2, 1, 8),
            new Network(3, f, new int[] {1}, one, one, 0, 2, 1),
            new Network(3, f, t, new int[] {1}, one, 0, 2, 1),
            new Network(3, f, t, one, new int[] {1}, 0, 2, 1),
            new Network(3, null, t, one, one, 0, 2, 1),
            new Network(3, f, null, one, one, 0, 2, 1),
            new Network(3, f, t, null, one, 0, 2, 1),
            new Network(3, f, t, one, null, 0, 2, 1),
        };
        for (int k = 0; k < cases.length; k++) {
            sink.accept('X', k, cases[k]);
        }
        for (int k = 0; k < SECTION_NETWORKS; k++) {
            int vertices = 2 + r.nextInt(MAX_VERTICES - 1);
            int source = r.nextInt(vertices);
            Network x = network(r, vertices, 1 + r.nextInt(MAX_EDGES), MAX_CAPACITY, MAX_UNIT_COST, source,
                    other(r, vertices, source), r.nextInt(MAX_DEMAND + 1), 8);
            sink.accept('X', cases.length + k, corrupt(x, r.nextInt(CORRUPTIONS), r.nextInt(x.from().length)));
        }
    }

    static Network corrupt(Network x, int kind, int edge) {
        int[] from = x.from().clone(), to = x.to().clone(), capacity = x.capacity().clone(), cost = x.cost().clone();
        int source = x.source(), sink = x.sink(), demand = x.demand();
        switch (kind) {
            case 0 -> cost[edge] = -1 - cost[edge] % MAX_UNIT_COST;
            case 1 -> to[edge] = from[edge];
            case 2 -> to[edge] = x.vertices();
            case 3 -> capacity[edge] = MAX_CAPACITY + 1 + capacity[edge];
            case 4 -> cost[edge] = MAX_UNIT_COST + 1;
            case 5 -> demand = MAX_DEMAND + 1 + demand;
            case 6 -> demand = -1 - demand;
            default -> sink = source;
        }
        return new Network(x.vertices(), from, to, capacity, cost, source, sink, demand);
    }

    /** Dump mode: one line per network, then the TOTAL line. */
    static final class Dump implements Sink {
        private final Solver solver;
        private final Writer out;
        private final Map<Character, Integer> sections = new TreeMap<>();
        private final Map<String, Integer> exceptions = new TreeMap<>();
        private long instances;
        private long solved;
        private long refused;
        private long partial;
        private long zeroFlow;
        private long edgeFlows;

        Dump(Solver solver, Writer out) {
            this.solver = solver;
            this.out = out;
        }

        @Override
        public void accept(char section, int ordinal, Network x) throws IOException, ReflectiveOperationException {
            instances++;
            sections.merge(section, 1, Integer::sum);
            StringBuilder line = new StringBuilder(96).append(section).append(' ').append(ordinal)
                    .append(" n=").append(x.vertices()).append(" m=").append(x.from() == null ? -1 : x.from().length)
                    .append(" s=").append(x.source()).append(" t=").append(x.sink()).append(" d=").append(x.demand());
            try {
                Object result = solver.run(x);
                int flow = (int) solver.flow().invoke(result);
                int[] edges = (int[]) solver.edgeFlows().invoke(result);
                line.append(" flow=").append(flow).append(" cost=").append((long) solver.cost().invoke(result))
                        .append(" edges=");
                for (int edge = 0; edge < edges.length; edge++) {
                    line.append(edge == 0 ? "" : ",").append(edges[edge]);
                }
                solved++;
                edgeFlows += edges.length;
                partial += flow < x.demand() ? 1 : 0;
                zeroFlow += flow == 0 ? 1 : 0;
            } catch (InvocationTargetException refusal) {
                Throwable cause = refusal.getCause();
                line.append(" refused=").append(cause.getClass().getName()).append(": ").append(cause.getMessage());
                refused++;
                exceptions.merge(cause.getClass().getSimpleName(), 1, Integer::sum);
            }
            out.write(line.append('\n').toString());
        }

        void total() throws IOException {
            out.write("TOTAL instances=" + instances + " solved=" + solved + " refused=" + refused + " partial="
                    + partial + " zero_flow=" + zeroFlow + " edge_flows=" + edgeFlows + " sections=" + sections
                    + " exceptions=" + exceptions + "\n");
        }
    }

    /** Cross mode: flow value and cost of the chosen solver against the unchosen one. */
    static final class Cross implements Sink {
        private final Solver solver;
        private final Oracle oracle;
        private final Map<Character, Integer> compared = new TreeMap<>();
        private long instances;
        private long agree;
        private long differ;
        private long chosenRefused;
        private long oracleSolvedChosenRefused;
        private String firstDifference = "none";

        Cross(Solver solver, Oracle oracle) {
            this.solver = solver;
            this.oracle = oracle;
        }

        @Override
        public void accept(char section, int ordinal, Network x) throws ReflectiveOperationException {
            instances++;
            Object result;
            try {
                result = solver.run(x);
            } catch (InvocationTargetException refusal) {
                chosenRefused++;
                oracleSolvedChosenRefused += oracleSolves(x) ? 1 : 0;
                return;
            }
            compared.merge(section, 1, Integer::sum);
            long flow = (int) solver.flow().invoke(result);
            long cost = (long) solver.cost().invoke(result);
            String other;
            try {
                long[] value = oracle.run(x);
                if (value[0] == flow && value[1] == cost) {
                    agree++;
                    return;
                }
                other = value[0] + "/" + value[1];
            } catch (InvocationTargetException refusal) {
                other = refusal.getCause().toString();
            }
            if (differ++ == 0) {
                firstDifference = section + " " + ordinal + " chosen=" + flow + "/" + cost + " oracle=" + other;
            }
        }

        private boolean oracleSolves(Network x) {
            try {
                oracle.run(x);
                return true;
            } catch (ReflectiveOperationException | RuntimeException notSolved) {
                return false;
            }
        }

        void report(Writer out) throws IOException {
            out.write("CROSS instances=" + instances + " compared=" + (agree + differ) + " agree=" + agree + " differ="
                    + differ + " chosen_refused=" + chosenRefused + " oracle_solved_where_chosen_refused="
                    + oracleSolvedChosenRefused + " compared_by_section=" + compared + " first_difference="
                    + firstDifference + "\n");
        }
    }
}
