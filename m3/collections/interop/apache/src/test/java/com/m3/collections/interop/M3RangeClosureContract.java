// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.interop;

import com.m3.collections.M3TreeMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

/** Differential, retained and nested range contract oracle; no transformation authority. */
public final class M3RangeClosureContract {
    private static long checks;
    private static String context = "";
    private static long trace = 0x4d3352616e67654cL;
    private record Pair(NavigableMap<Integer, Integer> expected,
                        NavigableMap<Integer, Integer> actual) { }
    private record Outcome(Object result, Class<?> failure) { }
    private M3RangeClosureContract() { }
    private static Outcome run(Supplier<?> operation) {
        try { return new Outcome(operation.get(), null); }
        catch (RuntimeException failure) { return new Outcome(null, failure.getClass()); }
    }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(context + ": check " + checks + ": " + expected + " != " + actual);
        }
        trace = Long.rotateLeft(trace, 7) ^ Objects.hashCode(expected instanceof Class<?> type ? type.getName() : expected);
    }
    private static void compare(Supplier<?> expected, Supplier<?> actual) {
        Outcome e = run(expected), a = run(actual);
        eq(e.failure(), a.failure());
        if (e.failure() == null) { eq(e.result(), a.result()); }
    }
    private static void state(Pair pair) {
        eq(pair.expected().size(), pair.actual().size());
        eq(pair.expected().isEmpty(), pair.actual().isEmpty());
        eq(new ArrayList<>(pair.expected().entrySet()), new ArrayList<>(pair.actual().entrySet()));
        eq(new ArrayList<>(pair.expected().descendingKeySet()), new ArrayList<>(pair.actual().descendingKeySet()));
        eq(pair.expected().hashCode(), pair.actual().hashCode());
        eq(pair.expected().equals(pair.actual()), true);
        eq(pair.actual().equals(pair.expected()), true);
        compare(() -> pair.expected().firstEntry(), () -> pair.actual().firstEntry());
        compare(() -> pair.expected().lastEntry(), () -> pair.actual().lastEntry());
    }
    private static NavigableMap<Integer, Integer> view(NavigableMap<Integer, Integer> map,
            int kind, Integer low, Integer high, boolean li, boolean hi) {
        return switch (kind) {
            case 0 -> map.descendingMap();
            case 1 -> map.headMap(high, hi);
            case 2 -> map.tailMap(low, li);
            default -> map.subMap(low, li, high, hi);
        };
    }
    private static Object action(NavigableMap<Integer, Integer> map, int op, Integer key, Integer value) {
        return switch (op) {
            case 0 -> map.put(key, value);
            case 1 -> map.remove(key);
            case 2 -> map.putIfAbsent(key, value);
            case 3 -> map.replace(key, value);
            case 4 -> map.remove(key, value);
            case 5 -> map.computeIfAbsent(key, k -> value);
            case 6 -> map.computeIfPresent(key, (k, v) -> value);
            case 7 -> map.compute(key, (k, v) -> value);
            // Avoid two simultaneously-invalid arguments; null merge values are tested separately.
            case 8 -> map.merge(key, value == null ? 1 : value, (x, y) -> y == 0 ? null : x + y);
            case 9 -> map.pollFirstEntry();
            case 10 -> map.pollLastEntry();
            case 11 -> map.navigableKeySet().remove(key);
            case 12 -> map.entrySet().remove(new java.util.AbstractMap.SimpleImmutableEntry<>(key, value));
            case 13 -> map.getOrDefault(key, value);
            case 14 -> { if (!map.isEmpty()) { var it=map.values().iterator(); Integer v=it.next(); it.remove(); yield v; } yield null; }
            case 15 -> { if (!map.isEmpty()) { var it=map.entrySet().iterator(); var e=it.next(); yield e.setValue(value); } yield null; }
            case 16 -> { if (!map.isEmpty()) { var it=map.descendingKeySet().iterator(); Integer k=it.next(); it.remove(); yield k; } yield null; }
            default -> { map.clear(); yield null; }
        };
    }
    public static void main(String[] args) {
        checks=0; trace=0x4d3352616e67654cL;
        if (args.length > 1) { throw new IllegalArgumentException("optional positive operation count only"); }
        int operations = args.length == 0 ? 1500 : Integer.parseInt(args[0]);
        if (operations <= 0) { throw new IllegalArgumentException("positive operation count required"); }
        var nullExpected = new TreeMap<Integer,Integer>();
        var nullActual = new M3TreeMap<Integer,Integer>();
        nullExpected.put(1,2); nullActual.put(1,2);
        compare(() -> nullExpected.subMap(0,true,3,false).merge(1,null,Integer::sum),
                () -> nullActual.subMap(0,true,3,false).merge(1,null,Integer::sum));
        eq(Map.of(1,2), nullActual);
        nullActual.verifyInvariants();
        for (int order = 0; order < 4; order++) {
            Comparator<Integer> comparator = switch (order) {
                case 0 -> Comparator.nullsFirst(Comparator.naturalOrder());
                case 1 -> Comparator.nullsLast(Comparator.reverseOrder());
                case 2 -> Comparator.nullsFirst(Comparator.comparingInt(v -> Math.floorDiv(v, 3)));
                default -> Comparator.naturalOrder();
            };
            for (int seed = 0; seed < 12; seed++) {
                Random random = new Random(0x4d3300L + 31L * order + seed);
                M3TreeMap<Integer, Integer> actual = new M3TreeMap<>(comparator);
                Pair root = new Pair(new TreeMap<>(comparator), actual);
                List<Pair> views = new ArrayList<>(); views.add(root);
                for (int i=0; i<48; i++) { root.expected().put(i-24, i); root.actual().put(i-24, i); }
                for (int step=0; step<operations; step++) {
                    if (step % 100 == 0) { for (int dense=0; dense<48; dense++) { root.expected().put(dense-24,dense); root.actual().put(dense-24,dense); } }
                    Pair selected=views.get(random.nextInt(views.size()));
                    Integer key=random.nextInt(25)==0 && order!=3 ? null : Integer.valueOf(random.nextInt(70)-35);
                    Integer value=random.nextInt(13)==0 ? null : Integer.valueOf(random.nextInt(16));
                    if (random.nextInt(7)==0) {
                        int kind=random.nextInt(4); Integer high=random.nextInt(13)==0 && order!=3 ? null : Integer.valueOf(random.nextInt(70)-35);
                        boolean li=random.nextBoolean(), hi=random.nextBoolean();
                        Outcome e=run(() -> view(selected.expected(), kind, key, high, li, hi));
                        Outcome a=run(() -> view(selected.actual(), kind, key, high, li, hi));
                        eq(e.failure(), a.failure());
                        if (e.failure()==null) {
                            @SuppressWarnings("unchecked") NavigableMap<Integer,Integer> ev=(NavigableMap<Integer,Integer>)e.result();
                            @SuppressWarnings("unchecked") NavigableMap<Integer,Integer> av=(NavigableMap<Integer,Integer>)a.result();
                            if (views.size()==12) { views.remove(1); }
                            views.add(new Pair(ev,av));
                        }
                    } else {
                        int op=random.nextInt(18);
                        context="order="+order+" seed="+seed+" step="+step+" op="+op+" key="+key+" value="+value+" view="+selected.expected();
                        compare(() -> action(selected.expected(),op,key,value), () -> action(selected.actual(),op,key,value));
                    }
                    compare(() -> selected.expected().lowerKey(key), () -> selected.actual().lowerKey(key));
                    compare(() -> selected.expected().floorKey(key), () -> selected.actual().floorKey(key));
                    compare(() -> selected.expected().ceilingKey(key), () -> selected.actual().ceilingKey(key));
                    compare(() -> selected.expected().higherKey(key), () -> selected.actual().higherKey(key));
                    for (Pair p:views) { state(p); }
                    actual.verifyInvariants();
                }
            }
        }
        System.out.println("RANGE_CLOSURE checks="+checks+" trace="+Long.toHexString(trace)+" PASS");
    }
}
