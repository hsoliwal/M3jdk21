// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3ShortestPaths;
import jdk.m3.tools.cpu.M3RollbackUnionFind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent Bellman-Ford and live-edge BFS oracles; no source algorithm reuse. */
public final class M3WeightedGraphContract {
    private static long checks;
    private M3WeightedGraphContract() { }
    private static void ok(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why + " at check " + checks);
    }
    private static void refuses(Runnable op) {
        try { op.run(); throw new AssertionError("missing refusal"); }
        catch (IllegalArgumentException | IndexOutOfBoundsException | ArithmeticException expected) {
            checks++;
        }
    }
    private static long[] bellman(int n, int[] offsets, int[] targets, long[] weights, int source) {
        long[] d = new long[n];
        Arrays.fill(d, Long.MAX_VALUE);
        d[source] = 0;
        for (int i = 0; i < n - 1; i++) {
            boolean changed = false;
            for (int u = 0; u < n; u++) {
                if (d[u] == Long.MAX_VALUE) continue;
                for (int e = offsets[u]; e < offsets[u + 1]; e++) {
                    long next = d[u] + weights[e];
                    if (next < d[targets[e]]) { d[targets[e]] = next; changed = true; }
                }
            }
            if (!changed) break;
        }
        return d;
    }
    private static void shortest(Random rng, int n, int source, int edgeCount) {
        int[] row = new int[n + 1];
        int[] edge = new int[edgeCount];
        long[] weight = new long[edgeCount];
        for (int i = 0; i < edgeCount; i++) row[rng.nextInt(n) + 1]++;
        for (int i = 1; i <= n; i++) row[i] += row[i - 1];
        for (int i = 0; i < edgeCount; i++) {
            edge[i] = rng.nextInt(n);
            weight[i] = rng.nextInt(12);
        }
        var result = M3ShortestPaths.dijkstra(n, row, edge, weight, source);
        long[] distances = result.distance();
        int[] parent = result.predecessor();
        ok(Arrays.equals(distances, bellman(n, row, edge, weight, source)), "bellman-ford parity");
        ok(parent[source] == -1, "source parent");
        for (int target = 0; target < n; target++) {
            int[] path = result.pathTo(target);
            if (distances[target] == Long.MAX_VALUE) {
                ok(path.length == 0 && parent[target] == -1, "unreachable geometry");
            } else {
                ok(path.length >= 1 && path[0] == source
                        && path[path.length - 1] == target, "path endpoints");
                for (int i = 1; i < path.length; i++) {
                    boolean witnessed = false;
                    int previous = path[i - 1], current = path[i];
                    for (int e = row[previous]; e < row[previous + 1]; e++) {
                        if (edge[e] == current && distances[previous] + weight[e] == distances[current]) {
                            witnessed = true;
                        }
                    }
                    ok(witnessed, "path edge distance witness");
                }
            }
        }
        if (distances.length > 0) {
            long old = distances[0];
            distances[0] = -999;
            ok(result.distance()[0] == old, "distance detached");
            parent[0] = 999;
            ok(result.predecessor()[0] != 999, "parents detached");
        }
    }
    private static boolean connected(int a, int b, int count, List<int[]> edges) {
        boolean[] seen = new boolean[count];
        seen[a] = true;
        for (int step = 0; step < count; step++) {
            boolean progress = false;
            for (int[] e : edges) {
                if (seen[e[0]] && !seen[e[1]]) { seen[e[1]] = true; progress = true; }
                if (seen[e[1]] && !seen[e[0]]) { seen[e[0]] = true; progress = true; }
            }
            if (!progress) break;
        }
        return seen[b];
    }
    private static void dsu(Random rng, int n) {
        M3RollbackUnionFind actual = new M3RollbackUnionFind(n);
        List<int[]> edges = new ArrayList<>();
        List<Integer> snapshots = new ArrayList<>();
        snapshots.add(0);
        for (int step = 0; step < 1_000; step++) {
            int op = rng.nextInt(10);
            if (n == 0) {
                ok(actual.components() == 0 && actual.snapshot() == 0, "empty DSU");
                continue;
            }
            int a = rng.nextInt(n), b = rng.nextInt(n);
            if (op < 5) {
                boolean expected = !connected(a, b, n, edges);
                ok(actual.union(a, b) == expected, "union outcome");
                if (expected) edges.add(new int[]{a, b});
            } else if (op == 5 || op == 6) {
                snapshots.add(actual.snapshot());
            } else if (op == 7 && !snapshots.isEmpty()) {
                int pick = rng.nextInt(snapshots.size());
                int snap = snapshots.get(pick);
                if (snap > actual.snapshot()) {
                    refuses(() -> actual.rollback(snap));
                } else {
                    actual.rollback(snap);
                    while (edges.size() > snap) edges.remove(edges.size() - 1);
                    snapshots.removeIf(v -> v > snap);
                    ok(actual.snapshot() == snap, "snapshot restoration");
                }
            } else {
                ok(actual.connected(a, b) == connected(a, b, n, edges), "connectivity");
                int size = 0;
                for (int j = 0; j < n; j++) if (connected(a, j, n, edges)) size++;
                ok(actual.componentSize(a) == size, "component size");
            }
            ok(actual.snapshot() == edges.size(), "primitive history depth");
            if ((step & 15) == 0) {
                int components = 0;
                for (int j = 0; j < n; j++) {
                    boolean smallest = true;
                    for (int k = 0; k < j; k++) if (connected(j,k,n,edges)) smallest = false;
                    if (smallest) components++;
                }
                ok(actual.components() == components, "component count");
            }
        }
    }
    public static void main(String[] args) {
        Random rng = new Random(0x4d33574156453631L);
        for (int i = 0; i < 13_000; i++) {
            int n = 1 + rng.nextInt(12);
            shortest(rng, n, rng.nextInt(n), rng.nextInt(4 * n + 1));
        }
        for (int i = 0; i < 55; i++) dsu(rng, rng.nextInt(30));
        int n = 32_768;
        int[] off = new int[n + 1], target = new int[n - 1];
        long[] weight = new long[n - 1];
        for (int i = 0; i < n - 1; i++) { off[i + 1] = i + 1; target[i] = i + 1; weight[i] = 1; }
        off[n] = n - 1;
        var big = M3ShortestPaths.dijkstra(n, off, target, weight, 0);
        ok(big.distance()[n-1] == n-1 && big.pathTo(n-1).length == n, "large chain");
        refuses(() -> M3ShortestPaths.dijkstra(1, new int[]{0,1}, new int[]{0}, new long[]{-1},0));
        refuses(() -> M3ShortestPaths.dijkstra(1, new int[]{0,1}, new int[]{1}, new long[]{0},0));
        refuses(() -> M3ShortestPaths.dijkstra(2, new int[]{0,2,1}, new int[]{1}, new long[]{0},0));
        refuses(() -> M3ShortestPaths.dijkstra(2, new int[]{0,1,1}, new int[]{1}, new long[]{Long.MAX_VALUE},0));
        refuses(() -> M3ShortestPaths.dijkstra(0, new int[]{0}, new int[]{}, new long[]{},0));
        M3RollbackUnionFind set = new M3RollbackUnionFind(3);
        refuses(() -> set.find(-1));
        refuses(() -> set.find(3));
        refuses(() -> set.rollback(-1));
        refuses(() -> set.rollback(1));
        ok(set.union(0,1) && !set.union(1,0) && set.snapshot() == 1, "no-op union");
        set.rollback(0);
        ok(set.components() == 3 && !set.connected(0,1), "rollback undo");
        System.out.println("M3_WAVE6_GRAPH_PERSISTENCE_PASS checks="+ checks);
    }
}
