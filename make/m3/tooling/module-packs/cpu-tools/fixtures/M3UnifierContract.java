// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import jdk.m3.tools.cpu.M3TermUnifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent exhaustive finite-domain and deep-term witness oracle. */
public final class M3UnifierContract {
    private static long checks;
    private M3UnifierContract() { }
    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) { throw new AssertionError(why + " check=" + checks); }
    }
    private static void refuse(Runnable work, Class<? extends Throwable> clazz) {
        checks++;
        try { work.run(); } catch (Throwable error) {
            if (clazz.isInstance(error)) { return; }
            throw new AssertionError("wrong refusal", error);
        }
        throw new AssertionError("expected refusal " + clazz);
    }

    private static final class DAG {
        final int vars;
        final List<Integer> kinds = new ArrayList<>();
        final List<Integer> symbols = new ArrayList<>();
        final List<int[]> args = new ArrayList<>();
        DAG(int variables) {
            vars = variables;
            for (int i = 0; i < vars; i++) { add(0, i); }
        }
        private int add(int kind, int symbol, int... children) {
            int node = kinds.size();
            for (int child : children) {
                if (child < 0 || child >= node) { throw new AssertionError("bad test DAG"); }
            }
            kinds.add(kind); symbols.add(symbol); args.add(children.clone());
            return node;
        }
        int atom(int value) { return add(1, value); }
        int app(int function, int... children) { return add(2, function, children); }
        int[] kinds() { return kinds.stream().mapToInt(Integer::intValue).toArray(); }
        int[] symbols() { return symbols.stream().mapToInt(Integer::intValue).toArray(); }
        int[] offsets() {
            int[] offsets = new int[args.size() + 1];
            for (int i = 0; i < args.size(); i++) { offsets[i + 1] = offsets[i] + args.get(i).length; }
            return offsets;
        }
        int[] children() {
            int[] result = new int[offsets()[args.size()]];
            int pos = 0;
            for (int[] values : args) { for (int x : values) { result[pos++] = x; } }
            return result;
        }
        int[] unify(int left, int right, int budget) {
            return M3TermUnifier.unify(kinds(), symbols(), offsets(), children(), vars, left, right, budget);
        }
        String materialize(int node, int[] binding) {
            return materialize(node, binding, 0);
        }
        String materialize(int node, int[] binding, int depth) {
            if (depth > 512) { throw new AssertionError("cyclic substitution"); }
            int kind = kinds.get(node);
            if (kind == 0) {
                int target = binding[node];
                return target < 0 ? "VAR" + node : materialize(target, binding, depth + 1);
            }
            if (kind == 1) { return "ATOM" + symbols.get(node); }
            StringBuilder result = new StringBuilder("APP").append(symbols.get(node)).append('(');
            for (int child : args.get(node)) { result.append(materialize(child, binding, depth + 1)).append(','); }
            return result.append(')').toString();
        }
    }

    private static void examples() {
        DAG data = new DAG(2);
        int atomA = data.atom(10), atomB = data.atom(11);
        int fx = data.app(20, 0, atomA), fb = data.app(20, atomB, atomA);
        int[] yes = data.unify(fx, fb, 400);
        check(yes != null && yes[0] == atomB, "f(x,a)=f(b,a)");
        check(data.materialize(fx, yes).equals(data.materialize(fb, yes)), "witness validation");
        int repeated = data.app(21, 0, 0);
        int mismatch = data.app(21, atomA, atomB);
        check(data.unify(repeated, mismatch, 400) == null, "repeated variable contradiction");
        int cycle = data.app(40, 0);
        check(data.unify(0, cycle, 400) == null, "occurs check");
        int empty = data.app(40, atomA);
        int[] resolved = data.unify(0, empty, 400);
        check(resolved != null && resolved[0] == empty, "variable binding");
        int[] alias = data.unify(0, 1, 400);
        check(alias != null && alias[1] == 0 && alias[0] == -1, "canonical alias");
        check(Arrays.equals(alias, data.unify(1, 0, 400)), "orientation-independent variable alias");
        check(data.unify(atomA, atomB, 400) == null, "distinct atom mismatch");
        check(data.unify(fx, repeated, 400) == null, "distinct functor mismatch");
        int[] detached = data.unify(atomA, atomA, 400);
        if (detached.length > 0) { detached[0] = 1000; }
        int[] again = data.unify(atomA, atomA, 400);
        check(again.length == 2 && again[0] == -1, "caller-owned copy");
        refuse(() -> data.unify(fx, fb, 1), IllegalStateException.class);
        refuse(() -> data.unify(fx, fb, 0), IllegalArgumentException.class);
        refuse(() -> M3TermUnifier.unify(new int[] {0}, new int[] {1},
                new int[] {0,0}, new int[0], 1, 0, 0, 100), IllegalArgumentException.class);
        refuse(() -> M3TermUnifier.unify(new int[] {2}, new int[] {5},
                new int[] {0,1}, new int[] {0}, 0, 0, 0, 100), IllegalArgumentException.class);
        refuse(() -> M3TermUnifier.unify(new int[] {1}, new int[] {5},
                new int[] {0,0}, new int[0], 0, -1, 0, 100), IllegalArgumentException.class);
        refuse(() -> M3TermUnifier.unify(new int[] {1}, new int[] {5},
                new int[] {0,1}, new int[] {0}, 0, 0, 0, 100), IllegalArgumentException.class);
    }

    private static void exhaustiveFiniteGround() {
        Random rng = new Random(0xC0DECABAL);
        for (int iter = 0; iter < 50000; iter++) {
            int vars = 1 + rng.nextInt(5);
            DAG dag = new DAG(vars);
            int atom0 = dag.atom(300), atom1 = dag.atom(301);
            int left = rng.nextInt(vars + 2);
            int right = rng.nextInt(vars + 2);
            boolean exists = false;
            for (int bits = 0; bits < (1 << vars); bits++) {
                int l = left < vars ? (bits >>> left) & 1 : left - atom0;
                int r = right < vars ? (bits >>> right) & 1 : right - atom0;
                if (l == r) { exists = true; break; }
            }
            int[] binding = dag.unify(left, right, 200);
            check((binding != null) == exists, "exhaustive finite-ground satisfiability");
            if (binding != null) {
                check(dag.materialize(left, binding).equals(dag.materialize(right, binding)),
                        "oracle projection equality");
            }
            int[] reverse = dag.unify(right, left, 200);
            check((reverse != null) == exists, "symmetry of unifiability");
        }
    }

    private static void structuralProperties() {
        Random rng = new Random(0xF00D25B5L);
        for (int iter = 0; iter < 25000; iter++) {
            DAG dag = new DAG(3);
            int a = dag.atom(10), b = dag.atom(20);
            int inner = dag.app(101, a, b);
            int x = dag.app(102, 0, inner);
            int y = dag.app(102, a, inner);
            int[] unifier = dag.unify(x, y, 500);
            check(unifier != null, "nested graph positive");
            check(dag.materialize(x, unifier).equals(dag.materialize(y, unifier)),
                    "nested graph certificate");
            int occurs = dag.app(103, 1, iter % 2 == 0 ? x : y);
            check(dag.unify(1, occurs, 500) == null, "nested occurs check");
            int wrong = dag.app(104, a, inner);
            check(dag.unify(x, wrong, 500) == null, "different function identifiers");
            int variable = rng.nextInt(3);
            int nonoccurring = dag.app(105, a, b);
            int[] bound = dag.unify(variable, nonoccurring, 500);
            check(bound != null && dag.materialize(variable, bound).equals(
                    dag.materialize(nonoccurring, bound)), "safe binding");
        }
    }

    public static void main(String[] args) {
        examples();
        exhaustiveFiniteGround();
        structuralProperties();
        System.out.println("M3_WAVE6_UNIFIER_PASS checks=" + checks);
    }
}
