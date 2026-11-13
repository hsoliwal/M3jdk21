/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.collections.apache.M3TreeList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

/** Tests the production array-index cursor, not a synthetic tree implementation. */
public final class M3TreeListCursorContract {
    private static long checks;
    private static void ok(boolean pass, String reason) {
        checks++;
        if (!pass) throw new AssertionError(reason + " check=" + checks);
    }
    private static void throwsType(Class<? extends Throwable> wanted, Runnable job) {
        checks++;
        try { job.run(); }
        catch (Throwable observed) {
            if (wanted.isInstance(observed)) return;
            throw new AssertionError("wanted " + wanted + ", got " + observed, observed);
        }
        throw new AssertionError("did not throw " + wanted);
    }
    private static Object value(Random random) {
        return switch (random.nextInt(12)) {
            case 0, 1 -> null;
            case 2, 3 -> -1;
            case 4, 5 -> "same";
            default -> random.nextInt(100) - 50;
        };
    }
    private static void compare(ArrayList<Object> model, M3TreeList<Object> actual) {
        ok(model.size() == actual.size(), "size");
        ok(model.equals(actual), "logical equals");
        ok(model.hashCode() == actual.hashCode(), "hash");
        ok(Arrays.equals(model.toArray(), actual.toArray()), "detached Object[]");
        ok(Arrays.equals(model.toArray(new Object[0]), actual.toArray(new Object[0])), "typed Object[]");
        for (Object target : new Object[]{null, -1, "same", 0, 17, new Object()}) {
            ok(model.indexOf(target) == actual.indexOf(target), "forward search");
            ok(model.contains(target) == actual.contains(target), "contains");
            ok(model.lastIndexOf(target) == actual.lastIndexOf(target), "reverse search");
        }
        Iterator<Object> expected = model.iterator();
        Iterator<Object> observed = actual.iterator();
        while (expected.hasNext()) {
            ok(observed.hasNext(), "iterator length");
            ok(Objects.equals(expected.next(), observed.next()), "iterator element");
        }
        ok(!observed.hasNext(), "iterator exhaustion");
        throwsType(NoSuchElementException.class, observed::next);
    }
    private static void iteratorRemoves(ArrayList<Object> model, M3TreeList<Object> actual) {
        Iterator<Object> expected = model.iterator();
        Iterator<Object> observed = actual.iterator();
        throwsType(IllegalStateException.class, observed::remove);
        int index = 0;
        while (expected.hasNext()) {
            ok(observed.hasNext(), "mutable iterator hasNext");
            ok(Objects.equals(expected.next(), observed.next()), "mutable iterator next");
            if ((index++ % 7) == 3) {
                expected.remove();
                observed.remove();
                throwsType(IllegalStateException.class, observed::remove);
            }
        }
        ok(!observed.hasNext(), "mutable iterator completed");
        compare(model, actual);
    }
    private static void structuralFailure(ArrayList<Object> model, M3TreeList<Object> actual) {
        if (model.isEmpty()) {
            model.add("item"); actual.add("item");
        }
        Iterator<Object> expected = model.iterator();
        Iterator<Object> observed = actual.iterator();
        ok(Objects.equals(expected.next(), observed.next()), "first value");
        model.add(0, "inserted");
        actual.add(0, "inserted");
        throwsType(ConcurrentModificationException.class, expected::next);
        throwsType(ConcurrentModificationException.class, observed::next);
        throwsType(ConcurrentModificationException.class, expected::remove);
        throwsType(ConcurrentModificationException.class, observed::remove);
    }
    private static void nonStructuralSet(ArrayList<Object> model, M3TreeList<Object> actual) {
        if (model.size() < 2) return;
        Iterator<Object> expected = model.iterator();
        Iterator<Object> observed = actual.iterator();
        ok(Objects.equals(expected.next(), observed.next()), "value before set");
        int target = Math.min(1, model.size() - 1);
        model.set(target, "rewritten");
        actual.set(target, "rewritten");
        ok(Objects.equals(expected.next(), observed.next()), "set does not invalidate traversal");
    }
    public static void main(String[] args) {
        Random random = new Random(0x13f11e48L);
        ArrayList<Object> oracle = new ArrayList<>();
        M3TreeList<Object> tree = new M3TreeList<>();
        long transitions = 0;
        for (int size : new int[]{0, 1, 7, 8, 31, 32, 63, 64, 65, 127, 128, 255, 256, 511, 512}) {
            oracle.clear(); tree.clear();
            for (int i = 0; i < size; i++) {
                Object v = value(random);
                oracle.add(v); tree.add(v);
            }
            compare(oracle, tree);
            if (size >= 64) iteratorRemoves(oracle, tree);
            compare(oracle, tree);
        }
        oracle.clear(); tree.clear();
        for (int i = 0; i < 150; i++) {
            Object v = value(random); oracle.add(v); tree.add(v);
        }
        for (int step = 0; step < 12_000; step++) {
            int op = random.nextInt(12);
            if (op <= 2 || oracle.size() < 80) {
                Object v = value(random);
                int at = random.nextInt(oracle.size() + 1);
                oracle.add(at, v); tree.add(at, v);
            } else if (op <= 4) {
                int at = random.nextInt(oracle.size());
                ok(Objects.equals(oracle.remove(at), tree.remove(at)), "remove");
            } else if (op == 5) {
                int at = random.nextInt(oracle.size()); Object v = value(random);
                ok(Objects.equals(oracle.set(at, v), tree.set(at, v)), "set");
            } else if (op == 6) {
                iteratorRemoves(oracle, tree);
            } else if (op == 7) {
                structuralFailure(oracle, tree);
            } else if (op == 8) {
                nonStructuralSet(oracle, tree);
            } else if (op == 9) {
                Object probe = value(random);
                ok(oracle.indexOf(probe) == tree.indexOf(probe), "random indexOf");
                ok(oracle.contains(probe) == tree.contains(probe), "random contains");
            } else if (op == 10 && oracle.size() > 350) {
                oracle.subList(0, oracle.size() - 80).clear();
                tree.subList(0, tree.size() - 80).clear();
            } else {
                List<Object> additions = Arrays.asList(value(random), value(random), null);
                int at = random.nextInt(oracle.size() + 1);
                oracle.addAll(at, additions); tree.addAll(at, additions);
            }
            if ((step & 31) == 0) compare(oracle, tree);
            transitions++;
        }
        compare(oracle, tree);
        oracle.clear(); tree.clear();
        for (int i = 0; i < 12_345; i++) oracle.add(i);
        tree.addAll(oracle);
        compare(oracle, tree);
        ok(tree.indexOf(12_344) == 12_344, "late hit");
        ok(tree.indexOf(12_345) == -1, "miss");
        iteratorRemoves(oracle, tree);
        System.out.println("M3_TREE_INDEX_CURSOR_PASS transitions=" + transitions + " checks=" + checks);
    }
}
