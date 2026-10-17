/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Independent executable protocol oracle using Spring Framework 6.2.0.
 * No provider implementation is used by the candidate body under test.
 */
package com.m3.collections.spring;

import com.m3.collections.M3LinkedHashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/**
 * Run with assertions enabled or disabled: every check is explicit.
 *
 * <p>Ordinary operations, callback failures, Map defaults, aliases, views, copies,
 * and serialization graph relationships are compared to the actual provider.
 * The existing SDK's delegated forEach structural-mutation protocol is compared
 * separately to a real MultiValueMapAdapter over the admitted M3 map owner.
 * Its concrete LinkedMultiValueMap trace difference is reported explicitly.
 *
 * <p>Concrete ArrayList capacity, wire format, no-op/replacement epochs, inherited
 * sublist removal and structural predicate timing are diagnostic boundaries of
 * the independently admitted M3ArrayList. They do not prove concrete ArrayList
 * parity. Provider class names, exception text and held entries after external
 * structural changes are excluded. No performance or whole-Spring claim is made.
 */
public final class M3MultiValueMapSelfTest {
    private static long checks;
    private static long operations;
    private static long randomOperations;
    private static long diagnosticCases;
    private static long boundaryDifferences;

    private M3MultiValueMapSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private record FrozenEntry(Object key, Object value) { }
    private record Outcome(Object value, Class<?> failure) { }

    @FunctionalInterface
    private interface Call { Object run() throws Exception; }

    @FunctionalInterface
    private interface Operation { Object run(Context context) throws Exception; }

    private static final class Boom extends RuntimeException {
        @Serial private static final long serialVersionUID = 1L;
        Boom() { super("deliberate oracle callback failure"); }
    }

    private static Object freeze(Object value) {
        if (value instanceof Map.Entry<?, ?> entry) {
            return new FrozenEntry(freeze(entry.getKey()), freeze(entry.getValue()));
        }
        if (value instanceof Map<?, ?> map) {
            List<Object> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) entries.add(freeze(entry));
            return entries;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> values = new ArrayList<>();
            for (Object item : collection) values.add(freeze(item));
            return values;
        }
        if (value instanceof Object[] array) {
            List<Object> values = new ArrayList<>();
            for (Object item : array) values.add(freeze(item));
            return values;
        }
        return value;
    }

    private static Outcome capture(Call call) {
        try { return new Outcome(freeze(call.run()), null); }
        catch (Exception failure) { return new Outcome(null, failure.getClass()); }
    }

    private static final class Context {
        final MultiValueMap<String, Integer> map;
        final LinkedMultiValueMap<String, Integer> provider;
        final M3MultiValueMap<String, Integer> owned;
        final List<String> trace = new ArrayList<>();
        private Map<String, Integer> scalar;

        Context(boolean candidate) {
            provider = candidate ? null : new LinkedMultiValueMap<>();
            owned = candidate ? new M3MultiValueMap<>() : null;
            map = candidate ? owned : provider;
        }

        Context(MultiValueMap<String, Integer> oracle) {
            map = oracle;
            provider = null;
            owned = null;
        }

        Map<String, Integer> view() {
            if (scalar == null) scalar = map.asSingleValueMap();
            return scalar;
        }

        void mark(String event) { trace.add(event); }

        MultiValueMap<String, Integer> copy(boolean deep) {
            if (owned != null) return deep ? owned.deepCopy() : owned.clone();
            if (provider != null) return deep ? provider.deepCopy() : provider.clone();
            throw new AssertionError("copy requested from alternate protocol oracle");
        }

        MultiValueMap<String, Integer> construct(Map<String, List<Integer>> source) {
            return owned != null ? new M3MultiValueMap<>(source) : new LinkedMultiValueMap<>(source);
        }
    }

    private static final class Pair {
        final String name;
        final Context expected;
        final Context actual = new Context(true);

        Pair(String name) {
            this.name = name;
            expected = new Context(false);
        }

        Pair(String name, MultiValueMap<String, Integer> oracle) {
            this.name = name;
            expected = new Context(oracle);
        }

        void step(String label, Operation operation) {
            operations++;
            expected.trace.clear();
            actual.trace.clear();
            Outcome left = capture(() -> operation.run(expected));
            Outcome right = capture(() -> operation.run(actual));
            String at = name + ": " + label;
            check(Objects.equals(left.failure(), right.failure()),
                    at + " exception classes: " + left + " / " + right);
            check(Objects.equals(left.value(), right.value()),
                    at + " results: " + left + " / " + right);
            check(expected.trace.equals(actual.trace),
                    at + " callbacks: " + expected.trace + " / " + actual.trace);
            state(expected.map, actual.map, at);
        }
    }

    private static void state(MultiValueMap<String, Integer> expected,
                              MultiValueMap<String, Integer> actual, String label) {
        Object left = freeze(expected);
        Object right = freeze(actual);
        check(Objects.equals(left, right), label + " ordered mappings: " + left + " / " + right);
        check(expected.size() == actual.size(), label + " mapping count");
        check(expected.isEmpty() == actual.isEmpty(), label + " empty");
        check(expected.equals(actual) && actual.equals(expected), label + " symmetric equality");
        check(expected.hashCode() == actual.hashCode(), label + " hash");
        check(expected.toString().equals(actual.toString()), label + " string");
        check(Objects.equals(freeze(expected.toSingleValueMap()), freeze(actual.toSingleValueMap())),
                label + " detached first-value mappings");
        for (String key : expected.keySet()) {
            check(Objects.equals(expected.getFirst(key), actual.getFirst(key)), label + " getFirst " + key);
            check(actual.containsKey(key), label + " key retained " + key);
        }
        check(Objects.equals(expected.getFirst("missing"), actual.getFirst("missing")), label + " absent first");
    }

    private static List<Integer> list(Integer... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static Pair populated(String name) {
        Pair pair = new Pair(name);
        seed(pair);
        return pair;
    }

    private static void seed(Pair pair) {
        pair.step("seed a", context -> context.map.put("a", list(1, 2)));
        pair.step("seed b", context -> context.map.put("b", list(3)));
        pair.step("seed c", context -> context.map.put("c", list(4)));
    }

    private static void constructorsAndMarkers() {
        Pair pair = new Pair("constructors");
        pair.step("map markers", context -> List.of(context.map instanceof Serializable,
                context.map instanceof Cloneable));
        pair.step("zero capacity", context -> context.owned != null
                ? new M3MultiValueMap<String, Integer>(0) : new LinkedMultiValueMap<String, Integer>(0));
        pair.step("expected capacity", context -> context.owned != null
                ? new M3MultiValueMap<String, Integer>(17) : new LinkedMultiValueMap<String, Integer>(17));
        pair.step("negative expected capacity", context -> context.owned != null
                ? new M3MultiValueMap<String, Integer>(-1) : new LinkedMultiValueMap<String, Integer>(-1));
        pair.step("null copy source", context -> context.construct(null));
        pair.step("add creates list", context -> { context.map.add("add", null); return null; });
        pair.step("set creates list", context -> { context.map.set("set", 2); return null; });
        pair.step("addAll creates list", context -> { context.map.addAll("all", list(3, null)); return null; });
        pair.step("created-list markers", context -> {
            List<Object> markers = new ArrayList<>();
            for (List<Integer> values : context.map.values()) {
                markers.add(List.of(values instanceof RandomAccess, values instanceof Serializable,
                        values instanceof Cloneable));
            }
            return markers;
        });
        check(pair.actual.map instanceof Serializable && pair.actual.map instanceof Cloneable,
                "owned map markers required");
        for (List<Integer> values : pair.actual.map.values()) {
            check(values instanceof RandomAccess && values instanceof Serializable && values instanceof Cloneable,
                    "owned mutable value-list markers required");
        }
    }

    private static void multiValueOperations() {
        Pair pair = new Pair("multi-value operations");
        pair.step("empty missing", context -> Arrays.asList(context.map.get("a"), context.map.getFirst("a")));
        pair.step("null key/value", context -> { context.map.add(null, null); return null; });
        pair.step("duplicate values", context -> { context.map.add("a", 1); context.map.add("a", 1); return null; });
        pair.step("null-list mapping", context -> context.map.put("null-list", null));
        pair.step("empty mapping", context -> { context.map.addAll("empty", List.of()); return null; });
        pair.step("addIfAbsent null-list", context -> { context.map.addIfAbsent("null-list", 9); return null; });
        pair.step("addIfAbsent empty", context -> { context.map.addIfAbsent("empty", 9); return null; });
        pair.step("addIfAbsent absent", context -> { context.map.addIfAbsent("new", 9); return null; });
        pair.step("add replaces null-list", context -> { context.map.add("null-list", null); return null; });
        pair.step("set replaces list", context -> { context.map.set("a", null); return null; });
        pair.step("setAll prefix", context -> {
            Map<String, Integer> input = new LinkedHashMap<>();
            input.put("a", 8); input.put("other", null);
            context.map.setAll(input); return null;
        });
        pair.step("addAll multimap", context -> {
            MultiValueMap<String, Integer> input = new LinkedMultiValueMap<>();
            input.add("a", 7); input.addAll("more", list(null, 4)); input.addAll("empty2", List.of());
            context.map.addAll(input); return null;
        });
        pair.step("detached result", context -> {
            Map<String, Integer> copy = context.map.toSingleValueMap();
            copy.put("detached", 99);
            return Arrays.asList(context.map.containsKey("detached"), copy);
        });
        pair.step("empty list clear retains key", context -> {
            context.map.get("more").clear(); return context.map.containsKey("more");
        });
        pair.step("null addAll multimap", context -> { context.map.addAll((MultiValueMap<String, Integer>) null); return null; });
        pair.step("null setAll", context -> { context.map.setAll(null); return null; });
    }

    private static final class ProbeList extends AbstractList<Integer> {
        private final Context context;
        private final Integer[] values;
        private final boolean failSize;
        private final boolean failArray;

        ProbeList(Context context, boolean failSize, boolean failArray, Integer... values) {
            this.context = context; this.values = values; this.failSize = failSize; this.failArray = failArray;
        }
        @Override public int size() {
            context.mark("size");
            if (failSize) throw new Boom();
            return values.length;
        }
        @Override public Integer get(int index) { context.mark("get:" + index); return values[index]; }
        @Override public Object[] toArray() {
            context.mark("toArray");
            if (failArray) throw new Boom();
            return values.clone();
        }
    }

    private static final class PrefixMap extends AbstractMap<String, List<Integer>> {
        @Override public Set<Entry<String, List<Integer>>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return 2; }
                @Override public Iterator<Entry<String, List<Integer>>> iterator() {
                    return new Iterator<>() {
                        private int position;
                        @Override public boolean hasNext() { return position < 2; }
                        @Override public Entry<String, List<Integer>> next() {
                            if (position++ == 0) return new SimpleImmutableEntry<>("prefix", list(8));
                            throw new Boom();
                        }
                    };
                }
            };
        }
    }

    private static void aliasesAndBulkFailures() {
        Pair pair = populated("list aliases and bulk effects");
        pair.step("borrowed list identity", context -> {
            List<Integer> borrowed = list(5);
            context.map.put("borrowed", borrowed);
            context.map.add("borrowed", 6);
            borrowed.add(7);
            return Arrays.asList(context.map.get("borrowed") == borrowed, borrowed);
        });
        pair.step("putAll aliases lists but not directory", context -> {
            List<Integer> shared = list(31);
            Map<String, List<Integer>> input = new LinkedHashMap<>();
            input.put("alias-one", shared); input.put("alias-two", shared);
            context.map.putAll(input); context.map.get("alias-one").add(32);
            input.put("input-only", list(33));
            return Arrays.asList(context.map.get("alias-one") == shared,
                    context.map.get("alias-one") == context.map.get("alias-two"),
                    !context.map.containsKey("input-only"));
        });
        pair.step("immutable borrowed list", context -> context.map.put("fixed", List.of(9)));
        pair.step("immutable list add fails", context -> { context.map.add("fixed", 10); return null; });
        pair.step("immutable list null batch", context -> { context.map.addAll("fixed", null); return null; });
        pair.step("absent null batch", context -> { context.map.addAll("absent", null); return null; });
        pair.step("present null batch", context -> { context.map.addAll("a", null); return null; });
        pair.step("absent size observation", context -> { context.map.addAll("probe-new", new ProbeList(context, false, false, 1, 2)); return null; });
        pair.step("present size skipped", context -> { context.map.addAll("a", new ProbeList(context, true, false, 3)); return null; });
        pair.step("size failure does not install", context -> { context.map.addAll("size-failure", new ProbeList(context, true, false, 3)); return null; });
        pair.step("array failure leaves installed empty mapping", context -> { context.map.addAll("array-failure", new ProbeList(context, false, true, 3)); return null; });
        pair.step("self list addition", context -> { context.map.addAll("a", context.map.get("a")); return null; });
        pair.step("aliased sublist addition", context -> {
            List<Integer> values = context.map.get("a");
            context.map.addAll("a", values.subList(0, 2)); return null;
        });
        Pair self = populated("self multimap addition");
        self.step("self addAll", context -> { context.map.addAll(context.map); return null; });
        Pair prefix = new Pair("bulk prefix failure");
        prefix.step("putAll iterator failure", context -> { context.map.putAll(new PrefixMap()); return null; });
        prefix.step("addAll null-list prefix", context -> {
            MultiValueMap<String, Integer> input = new LinkedMultiValueMap<>();
            input.add("first", 1); input.put("second", null); input.add("third", 3);
            context.map.addAll(input); return null;
        });
        prefix.step("setAll source callback failure", context -> {
            Map<String, Integer> input = new LinkedHashMap<>() {
                @Serial private static final long serialVersionUID = 1L;
                @Override public void forEach(java.util.function.BiConsumer<? super String, ? super Integer> action) {
                    action.accept("set-prefix", 7); throw new Boom();
                }
            };
            context.map.setAll(input); return null;
        });
    }

    private static void mapDefaults() {
        Pair pair = populated("Map defaults");
        pair.step("getOrDefault missing", context -> context.map.getOrDefault("missing", list(9)));
        pair.step("containsValue", context -> context.map.containsValue(list(1, 2)));
        pair.step("put null mapping", context -> context.map.put("null", null));
        pair.step("getOrDefault present null", context -> context.map.getOrDefault("null", list(9)));
        pair.step("putIfAbsent present null", context -> context.map.putIfAbsent("null", list(5)));
        pair.step("putIfAbsent present", context -> context.map.putIfAbsent("a", list(5)));
        pair.step("conditional remove mismatch", context -> context.map.remove("a", list(7)));
        pair.step("replace mismatch", context -> context.map.replace("a", list(7), list(8)));
        pair.step("replace match", context -> context.map.replace("a", list(1, 2), list(8)));
        pair.step("replace absent", context -> context.map.replace("missing", list(8)));
        pair.step("computeIfAbsent existing", context -> context.map.computeIfAbsent("a", key -> { context.mark("unexpected"); return list(9); }));
        pair.step("computeIfAbsent null function", context -> context.map.computeIfAbsent("a", null));
        pair.step("computeIfPresent null function", context -> context.map.computeIfPresent("missing", null));
        pair.step("compute null function", context -> context.map.compute("missing", null));
        pair.step("computeIfAbsent structural callback", context -> context.map.computeIfAbsent("new", key -> {
            context.mark("absent:" + key); context.map.put("side", list(6)); return list(7);
        }));
        pair.step("computeIfPresent absent", context -> context.map.computeIfPresent("missing", (key, old) -> {
            context.mark("unexpected"); return list(9);
        }));
        pair.step("computeIfPresent removes current then returns", context -> context.map.computeIfPresent("a", (key, old) -> {
            context.mark("present:" + key + ":" + old); context.map.remove(key); return list(11);
        }));
        pair.step("compute failure leaves callback effect", context -> context.map.compute("b", (key, old) -> {
            context.mark("compute:" + key + ":" + old); context.map.put("failure-side", list(12)); throw new Boom();
        }));
        pair.step("compute null deletes present", context -> context.map.compute("null", (key, old) -> {
            context.mark("delete:" + key); return null;
        }));
        pair.step("compute null absent", context -> context.map.compute("missing", (key, old) -> {
            context.mark("missing:" + key); return null;
        }));
        pair.step("merge null value", context -> context.map.merge("a", null, (old, value) -> value));
        pair.step("merge null function", context -> context.map.merge("a", list(1), null));
        pair.step("merge callback changes map", context -> context.map.merge("a", list(13), (old, value) -> {
            context.mark("merge:" + old + ":" + value); context.map.put("merge-side", list(14)); return value;
        }));
        pair.step("merge removes", context -> context.map.merge("a", list(1), (old, value) -> {
            context.mark("merge-delete"); return null;
        }));
        Pair replace = populated("replaceAll prefix exception");
        replace.step("callback throws", context -> {
            context.map.replaceAll((key, values) -> {
                context.mark("replace:" + key);
                if ("b".equals(key)) throw new Boom();
                return list(20);
            }); return null;
        });
        Pair structural = populated("replaceAll structural callback");
        structural.step("first replacement before iterator failure", context -> {
            context.map.replaceAll((key, values) -> {
                context.mark("replace:" + key);
                context.map.put("new", list(21)); return list(22);
            }); return null;
        });
        Pair each = populated("forEach ordinary callbacks");
        each.step("ordered action", context -> { context.map.forEach((key, values) -> context.mark(key + ":" + values)); return null; });
        each.step("action exception prefix", context -> {
            context.map.forEach((key, values) -> { context.mark(key); if ("b".equals(key)) throw new Boom(); });
            return null;
        });
        each.step("null action", context -> { context.map.forEach(null); return null; });
        Pair empty = new Pair("empty null callbacks");
        empty.step("forEach null", context -> { context.map.forEach(null); return null; });
        empty.step("replaceAll null", context -> { context.map.replaceAll(null); return null; });
        Pair nulls = new Pair("conditional null mappings");
        nulls.step("present null", context -> context.map.put("null", null));
        nulls.step("absent null conditional remove", context -> context.map.remove("missing", null));
        nulls.step("present null conditional replace", context -> context.map.replace("null", null, list(7)));
        nulls.step("restore null", context -> context.map.put("null", null));
        nulls.step("present null conditional remove", context -> context.map.remove("null", null));
        Pair collisions = new Pair("colliding equal-key representatives");
        collisions.step("collision and canonical key", context -> {
            String canonical = new String("Aa");
            context.map.put(canonical, list(1));
            context.map.put("BB", list(2));
            context.map.put(new String("Aa"), list(3));
            String retained = context.map.keySet().stream()
                    .filter(key -> "Aa".equals(key)).findFirst().orElseThrow();
            return List.of(retained == canonical, context.map.size());
        });
    }

    private static void baseViews() {
        Pair keys = populated("key view");
        keys.step("key array", context -> context.map.keySet().toArray());
        keys.step("empty addAll", context -> context.map.keySet().addAll(List.of()));
        keys.step("add forbidden", context -> context.map.keySet().add("new"));
        keys.step("iterator remove before next", context -> { context.map.keySet().iterator().remove(); return null; });
        keys.step("iterator removes mapping", context -> {
            Iterator<String> iterator = context.map.keySet().iterator(); String key = iterator.next();
            iterator.remove(); return key;
        });
        keys.step("removeIf prefix exception", context -> context.map.keySet().removeIf(key -> {
            context.mark(key); if ("c".equals(key)) throw new Boom(); return true;
        }));
        Pair values = populated("value view");
        values.step("value array", context -> context.map.values().toArray());
        values.step("empty addAll", context -> context.map.values().addAll(List.of()));
        values.step("add forbidden", context -> context.map.values().add(list(7)));
        values.step("remove equal list", context -> context.map.values().remove(list(1, 2)));
        values.step("retainAll", context -> context.map.values().retainAll(List.of(list(3))));
        values.step("clear", context -> { context.map.values().clear(); return null; });
        Pair entries = populated("entry view");
        entries.step("entry array", context -> context.map.entrySet().toArray());
        entries.step("empty addAll", context -> context.map.entrySet().addAll(List.of()));
        entries.step("entry setValue and iterator remove", context -> {
            Iterator<Map.Entry<String, List<Integer>>> iterator = context.map.entrySet().iterator();
            Map.Entry<String, List<Integer>> entry = iterator.next();
            List<Integer> old = entry.setValue(list(8)); iterator.remove(); return old;
        });
        entries.step("remove matching entry", context -> context.map.entrySet().remove(
                new AbstractMap.SimpleImmutableEntry<>("b", list(3))));
        entries.step("entry removeAll", context -> context.map.entrySet().removeAll(List.of(
                new AbstractMap.SimpleImmutableEntry<>("c", list(4)))));
    }

    private static void scalarViews() {
        Pair pair = populated("scalar view");
        pair.step("fresh maps and cached child views", context -> {
            Map<String, Integer> view = context.view();
            return List.of(view != context.map.asSingleValueMap(), view.values() == view.values(),
                    view.entrySet() == view.entrySet());
        });
        pair.step("scalar query and quirks", context -> {
            Map<String, Integer> view = context.view();
            Map<String, Integer> ordinary = new LinkedHashMap<>();
            ordinary.put("a", 1); ordinary.put("b", 3); ordinary.put("c", 4);
            return List.of(view.equals(ordinary), view.hashCode(), view.toString(),
                    view.size(), view.containsValue(1), view.containsKey("a"));
        });
        pair.step("typed values array canaries", context -> {
            Integer[] target = new Integer[6]; Arrays.fill(target, -99);
            Integer[] result = context.view().values().toArray(target);
            return Arrays.asList(result == target, target);
        });
        pair.step("generated values array", context -> context.view().values().toArray(size -> {
            context.mark("generator:" + size); return new Integer[size];
        }));
        pair.step("generated entries array", context -> context.view().entrySet().toArray(size -> {
            context.mark("generator:" + size); return new Map.Entry<?, ?>[size];
        }));
        pair.step("generator failure before iteration", context -> context.view().values().toArray(size -> {
            context.mark("generator:" + size); throw new Boom();
        }));
        pair.step("immutable scalar entry setter", context -> context.view().entrySet().iterator().next().setValue(9));
        pair.step("scalar entry iterator remove before next", context -> { context.view().entrySet().iterator().remove(); return null; });
        pair.step("scalar value iterator remove after next", context -> {
            Iterator<Integer> iterator = context.view().values().iterator(); iterator.next(); iterator.remove(); return null;
        });
        pair.step("matching scalar value removal forbidden", context -> context.view().values().remove(1));
        pair.step("matching scalar entry removal forbidden", context -> context.view().entrySet().remove(
                new AbstractMap.SimpleImmutableEntry<>("a", 1)));
        pair.step("absent scalar value removal", context -> context.view().values().remove(99));
        pair.step("replaceAll calls then immutable setter", context -> {
            context.view().replaceAll((key, value) -> { context.mark(key + ":" + value); return value + 1; });
            return null;
        });
        pair.step("scalar forEach action failure", context -> {
            context.view().forEach((key, value) -> { context.mark(key); if ("b".equals(key)) throw new Boom(); });
            return null;
        });
        pair.step("scalar put singleton", context -> context.view().put("single", 17));
        pair.step("singleton markers", context -> {
            List<Integer> singleton = context.map.get("single");
            return List.of(singleton instanceof RandomAccess, singleton instanceof Serializable,
                    singleton instanceof Cloneable);
        });
        pair.step("singleton empty addAll", context -> context.map.get("single").addAll(List.of()));
        pair.step("singleton absent remove", context -> context.map.get("single").remove(Integer.valueOf(99)));
        pair.step("singleton retain current", context -> context.map.get("single").retainAll(List.of(17)));
        pair.step("singleton replaceAll skips callback", context -> {
            context.map.get("single").replaceAll(value -> { context.mark("replace"); return 18; }); return null;
        });
        pair.step("singleton sort skips comparator", context -> {
            context.map.get("single").sort((left, right) -> { context.mark("compare"); throw new Boom(); }); return null;
        });
        pair.step("singleton removeIf false", context -> context.map.get("single").removeIf(value -> {
            context.mark("predicate:" + value); return false;
        }));
        pair.step("singleton removeIf true", context -> context.map.get("single").removeIf(value -> {
            context.mark("predicate:" + value); return true;
        }));
        pair.step("singleton removeIf null", context -> context.map.get("single").removeIf(null));
        pair.step("append to scalar singleton fails", context -> { context.map.add("single", 18); return null; });
        pair.step("scalar put null installs null-list", context -> context.view().put("single", null));
        pair.step("scalar key removes mapping", context -> context.view().keySet().remove("b"));
        pair.step("scalar remove", context -> context.view().remove("c"));
        Pair empty = new Pair("empty scalar view");
        empty.step("null forEach on empty view", context -> { context.view().forEach(null); return null; });
        empty.step("empty value clear", context -> { context.view().values().clear(); return null; });
        empty.step("empty entry clear", context -> { context.view().entrySet().clear(); return null; });
        empty.step("empty scalar replaceAll", context -> { context.view().replaceAll((key, value) -> value); return null; });
        for (boolean nullList : List.of(false, true)) {
            Pair bad = new Pair(nullList ? "null-list scalar view" : "empty-list scalar view");
            bad.step("bad mapping", context -> context.map.put("bad", nullList ? null : list()));
            bad.step("get remains null-safe", context -> Arrays.asList(context.view().get("bad"),
                    context.view().containsKey("bad"), context.view().size()));
            bad.step("values unsafe first access", context -> context.view().values().iterator().next());
            bad.step("entries unsafe first access", context -> context.view().entrySet().iterator().next());
            bad.step("containsValue unsafe access", context -> context.view().containsValue(null));
            bad.step("values array unsafe access", context -> context.view().values().toArray());
            bad.step("entries array unsafe access", context -> context.view().entrySet().toArray());
            bad.step("forEach unsafe access", context -> { context.view().forEach((key, value) -> context.mark(key)); return null; });
            bad.step("null-safe equals catches bad-list access", context -> {
                Map<String, Integer> other = new LinkedHashMap<>(); other.put("bad", null);
                return context.view().equals(other);
            });
        }
    }

    private static void scalarDefaultsAndReentrantArrays() {
        Pair pair = populated("scalar Map defaults and reentrant arrays");
        pair.step("putAll keeps order and null-list adaptation", context -> {
            Map<String, Integer> input = new LinkedHashMap<>();
            input.put("a", 11); input.put("new-null", null); input.put("new-value", 12);
            context.view().putAll(input); return null;
        });
        pair.step("present-null getOrDefault", context -> context.view().getOrDefault("new-null", 99));
        pair.step("putIfAbsent replaces present null", context -> context.view().putIfAbsent("new-null", 13));
        pair.step("conditional scalar remove mismatch", context -> context.view().remove("a", 99));
        pair.step("conditional scalar replace", context -> context.view().replace("a", 11, 14));
        pair.step("scalar replace existing", context -> context.view().replace("a", 15));
        pair.step("scalar computeIfAbsent reentrant effect", context -> context.view().computeIfAbsent("computed", key -> {
            context.mark("absent:" + key); context.map.set("callback-side", 16); return 17;
        }));
        pair.step("scalar computeIfPresent exception effect", context -> context.view().computeIfPresent("a", (key, value) -> {
            context.mark("present:" + key + ":" + value); context.map.set("exception-side", 18); throw new Boom();
        }));
        pair.step("scalar compute deletes", context -> context.view().compute("a", (key, value) -> {
            context.mark("compute:" + key + ":" + value); return null;
        }));
        pair.step("scalar merge callback", context -> context.view().merge("computed", 19, (old, value) -> {
            context.mark("merge:" + old + ":" + value); return old + value;
        }));
        pair.step("scalar merge null value", context -> context.view().merge("computed", null, Integer::sum));
        pair.step("scalar merge null function", context -> context.view().merge("computed", 1, null));
        pair.step("reentrant array generator before traversal", context -> context.view().values().toArray(size -> {
            context.mark("generator:" + size); context.map.set("generator-side", 20); return new Integer[size];
        }));
        pair.step("scalar value removeIf rejects first removal", context -> context.view().values().removeIf(value -> {
            context.mark("value:" + value); return true;
        }));
        pair.step("scalar entry removeIf rejects first removal", context -> context.view().entrySet().removeIf(entry -> {
            context.mark("entry:" + entry.getKey()); return true;
        }));
        pair.step("scalar clear writes through", context -> { context.view().clear(); return null; });
        pair.step("cleared cached views stay live", context -> {
            context.map.set("later", 21);
            return Arrays.asList(context.view().values().toArray(), context.view().entrySet().toArray());
        });
    }

    private static void copies() {
        Pair pair = new Pair("copies and aliases");
        pair.step("shared source lists", context -> {
            List<Integer> shared = list(5000, null);
            context.map.put("a", shared); context.map.put("b", shared); context.map.put("empty", list());
            return null;
        });
        pair.step("Map constructor aliases lists and isolates directory", context -> {
            MultiValueMap<String, Integer> copy = context.construct(context.map);
            boolean alias = copy.get("a") == context.map.get("a");
            copy.put("copy-only", list(7)); copy.get("a").add(8);
            return Arrays.asList(alias, !context.map.containsKey("copy-only"),
                    copy.get("a") == copy.get("b"), copy);
        });
        pair.step("shallow clone aliases", context -> {
            MultiValueMap<String, Integer> copy = context.copy(false);
            boolean alias = copy.get("a") == context.map.get("a");
            copy.remove("b"); copy.get("a").add(9);
            return Arrays.asList(alias, context.map.containsKey("b"), copy);
        });
        pair.step("deep copy separates each list and preserves elements", context -> {
            MultiValueMap<String, Integer> copy = context.copy(true);
            boolean detached = copy.get("a") != context.map.get("a") && copy.get("a") != copy.get("b");
            boolean elementAlias = copy.get("a").get(0) == context.map.get("a").get(0);
            copy.get("a").add(10);
            return Arrays.asList(detached, elementAlias, copy.get("a") instanceof RandomAccess,
                    copy.get("a") instanceof Serializable, copy.get("a") instanceof Cloneable, copy);
        });
        pair.step("null-list clone retained", context -> { context.map.put("null", null); return context.copy(false); });
        pair.step("null-list deepCopy fails", context -> context.copy(true));
        pair.step("immutable borrowed list deepCopy becomes mutable", context -> {
            context.map.remove("null"); context.map.put("fixed", List.of(17));
            MultiValueMap<String, Integer> copy = context.copy(true); copy.add("fixed", 18); return copy;
        });
    }

    private static Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return input.readObject();
        }
    }

    private static void serializationGraph(boolean candidate) throws Exception {
        MultiValueMap<String, Object> map = candidate
                ? new M3MultiValueMap<>() : new LinkedMultiValueMap<>();
        List<Object> shared = new ArrayList<>();
        shared.add("first"); shared.add(map); shared.add(shared); shared.add(null);
        map.put("a", shared); map.put("b", shared); map.put("empty", new ArrayList<>());
        map.put("null", null);
        Map<String, Object> view = map.asSingleValueMap();
        view.values(); view.entrySet();
        Object reopened = roundTrip(new Object[] { map, view, view, shared });
        check(reopened instanceof Object[], "serialization graph envelope");
        Object[] graph = (Object[]) reopened;
        check(graph[0] instanceof Map<?, ?> && graph[1] instanceof Map<?, ?>, "serialized map/view");
        Map<?, ?> copy = (Map<?, ?>) graph[0];
        Map<?, ?> scalar = (Map<?, ?>) graph[1];
        check(graph[1] == graph[2], "same scalar view graph reference");
        check(copy.get("a") == copy.get("b") && copy.get("a") == graph[3], "shared list graph aliases");
        check(graph[3] instanceof List<?>, "serialized list");
        List<?> copiedList = (List<?>) graph[3];
        check(copiedList.get(1) == copy && copiedList.get(2) == copiedList, "map/list cycles preserved");
        check(copiedList.get(3) == null, "serialized null element");
        check("first".equals(scalar.get("a")), "serialized view observes restored target");
        check(copy.containsKey("null") && copy.get("null") == null, "serialized null-list entry");
        check(copy.containsKey("empty") && ((List<?>) copy.get("empty")).isEmpty(), "serialized empty entry");
        scalar.clear();
        check(copy.isEmpty(), "restored view writes through same restored map");
        check(map.size() == 4, "roundtrip directory detached from original");
    }

    private static void ownedListProtocols() {
        Pair pair = new Pair("ordinary created-list protocols");
        pair.step("seed", context -> { context.map.addAll("a", list(1, 2, null, 3)); return null; });
        pair.step("predicate failure has no removals", context -> context.map.get("a").removeIf(value -> {
            context.mark("predicate:" + value); if (value == null) throw new Boom(); return value > 1;
        }));
        pair.step("successful predicate once per occurrence", context -> context.map.get("a").removeIf(value -> {
            context.mark("predicate:" + value); return value == null || value == 2;
        }));
        pair.step("replacement failure commits prefix", context -> {
            context.map.get("a").replaceAll(value -> {
                context.mark("replace:" + value); if (value == 3) throw new Boom(); return value + 10;
            }); return null;
        });
        pair.step("list typed array", context -> context.map.get("a").toArray(new Integer[0]));
    }

    private static void structuralForEach() {
        Operation mutation = context -> {
            context.map.forEach((key, values) -> {
                context.mark(key);
                if ("a".equals(key)) context.map.put("new", list(7));
            }); return null;
        };
        Pair existing = new Pair("existing delegated M3 forEach protocol",
                new MultiValueMapAdapter<>(new M3LinkedHashMap<>()));
        seed(existing);
        existing.step("structural callback", mutation);
        Pair concrete = populated("concrete forEach diagnostic");
        diagnostic(concrete, "NON_ADMITTED_CONCRETE_PROVIDER_BOUNDARY forEach callback trace", mutation, false, false);
        Operation scalarMutation = context -> {
            context.view().forEach((key, value) -> {
                context.mark(key);
                if ("a".equals(key)) context.map.put("new", list(7));
            }); return null;
        };
        Pair scalarExisting = new Pair("existing scalar delegated M3 forEach protocol",
                new MultiValueMapAdapter<>(new M3LinkedHashMap<>()));
        seed(scalarExisting);
        scalarExisting.step("structural scalar callback", scalarMutation);
        diagnostic(populated("concrete scalar forEach diagnostic"),
                "NON_ADMITTED_CONCRETE_PROVIDER_BOUNDARY scalar forEach callback trace", scalarMutation, false, false);
    }

    private static void diagnostic(Pair pair, String label, Operation operation,
                                   boolean stateMayDiffer, boolean failureMayDiffer) {
        diagnosticCases++;
        pair.expected.trace.clear(); pair.actual.trace.clear();
        Outcome left = capture(() -> operation.run(pair.expected));
        Outcome right = capture(() -> operation.run(pair.actual));
        Object before = freeze(pair.expected.map);
        Object after = freeze(pair.actual.map);
        if (!stateMayDiffer) {
            check(Objects.equals(before, after), label + " required equal state");
        }
        if (!failureMayDiffer) check(Objects.equals(left.failure(), right.failure()),
                label + " required equal exception class");
        boolean differs = !Objects.equals(left, right) || !pair.expected.trace.equals(pair.actual.trace)
                || !Objects.equals(before, after);
        if (differs) boundaryDifferences++;
        System.out.println(label + " observed=" + differs + " provider=" + left
                + " owned=" + right + " callbacks=" + pair.expected.trace + "/" + pair.actual.trace);
        if (stateMayDiffer) System.out.println(label + " providerState=" + before + " ownedState=" + after);
    }

    private static void listBoundaryDiagnostics() {
        Pair emptyAdd = populated("empty-add epoch diagnostic");
        diagnostic(emptyAdd, "NON_ADMITTED_ARRAYLIST_BOUNDARY empty addAll iterator epoch", context -> {
            List<Integer> values = context.map.get("a"); Iterator<Integer> iterator = values.iterator();
            values.addAll(List.of()); return iterator.next();
        }, false, true);
        Pair replace = populated("replace epoch diagnostic");
        diagnostic(replace, "NON_ADMITTED_ARRAYLIST_BOUNDARY replaceAll iterator epoch", context -> {
            List<Integer> values = context.map.get("a"); Iterator<Integer> iterator = values.iterator();
            values.replaceAll(value -> value + 1); return iterator.next();
        }, false, true);
        Pair sublist = populated("sublist predicate diagnostic");
        diagnostic(sublist, "NON_ADMITTED_ARRAYLIST_BOUNDARY sublist predicate failure state", context ->
                context.map.get("a").subList(0, 2).removeIf(value -> {
                    context.mark("predicate:" + value); if (value == 2) throw new Boom(); return true;
                }), true, false);
        Pair structural = populated("structural predicate diagnostic");
        diagnostic(structural, "NON_ADMITTED_ARRAYLIST_BOUNDARY structural predicate callback timing", context ->
                context.map.get("a").removeIf(value -> {
                    context.mark("predicate:" + value); if (value == 1) context.map.get("a").add(9); return false;
                }), false, false);
        System.out.println("NON_ADMITTED_ARRAYLIST_BOUNDARY capacity and concrete serialized wire excluded");
    }

    private static void randomTrace() {
        Pair pair = new Pair("fixed-seed random trace");
        Random random = new Random(0x6d33537072696e67L);
        for (int step = 0; step < 5000; step++) {
            String key = random.nextInt(7) == 0 ? null : "k" + random.nextInt(12);
            Integer value = random.nextInt(6) == 0 ? null : random.nextInt(17);
            Integer other = random.nextInt(6) == 0 ? null : random.nextInt(17);
            int op = random.nextInt(27);
            boolean absentResult = random.nextInt(4) == 0;
            randomOperations++;
            pair.step("random " + step + " op=" + op, context -> {
                switch (op) {
                    case 0 -> { context.map.add(key, value); return null; }
                    case 1 -> { context.map.set(key, value); return null; }
                    case 2 -> { context.map.addIfAbsent(key, value); return null; }
                    case 3 -> { context.map.addAll(key, list(value, other)); return null; }
                    case 4 -> { context.map.addAll(key, List.of()); return null; }
                    case 5 -> { return context.map.put(key, absentResult ? null : list(value, other)); }
                    case 6 -> { return context.map.remove(key); }
                    case 7 -> { return context.map.putIfAbsent(key, list(value)); }
                    case 8 -> { return context.map.replace(key, list(value)); }
                    case 9 -> { return context.map.replace(key, list(value), list(other)); }
                    case 10 -> { return context.map.remove(key, absentResult ? null : list(value)); }
                    case 11 -> { return context.map.computeIfAbsent(key, supplied -> {
                        context.mark("absent:" + supplied); return absentResult ? null : list(value);
                    }); }
                    case 12 -> { return context.map.computeIfPresent(key, (supplied, old) -> {
                        context.mark("present:" + supplied + ":" + old); return absentResult ? null : list(value);
                    }); }
                    case 13 -> { return context.map.compute(key, (supplied, old) -> {
                        context.mark("compute:" + supplied + ":" + old); return absentResult ? null : list(value);
                    }); }
                    case 14 -> { return context.map.merge(key, list(value), (old, supplied) -> {
                        context.mark("merge:" + old + ":" + supplied); return absentResult ? null : list(other);
                    }); }
                    case 15 -> { return context.map.getOrDefault(key, list(99)); }
                    case 16 -> { return context.view().put(key, value); }
                    case 17 -> { return context.view().remove(key); }
                    case 18 -> { return context.map.keySet().remove(key); }
                    case 19 -> { return context.map.values().remove(list(value)); }
                    case 20 -> { return context.map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(key, list(value))); }
                    case 21 -> { return context.view().containsValue(value); }
                    case 22 -> {
                        List<Integer> values = context.map.get(key);
                        if (values == null) return false;
                        values.clear(); return true;
                    }
                    case 23 -> {
                        Map<String, Integer> input = new LinkedHashMap<>(); input.put(key, value);
                        context.map.setAll(input); return null;
                    }
                    case 24 -> {
                        MultiValueMap<String, Integer> input = new LinkedMultiValueMap<>();
                        input.add(key, value); context.map.addAll(input); return null;
                    }
                    case 25 -> {
                        Map<String, List<Integer>> input = new LinkedHashMap<>();
                        input.put(key, list(value)); context.map.putAll(input); return null;
                    }
                    default -> {
                        if (absentResult) context.map.clear();
                        return context.map.containsValue(absentResult ? null : list(value));
                    }
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        constructorsAndMarkers();
        multiValueOperations();
        aliasesAndBulkFailures();
        mapDefaults();
        baseViews();
        scalarViews();
        scalarDefaultsAndReentrantArrays();
        copies();
        serializationGraph(false);
        serializationGraph(true);
        ownedListProtocols();
        structuralForEach();
        listBoundaryDiagnostics();
        randomTrace();
        System.out.println("PASS M3MultiValueMapSelfTest checks=" + checks
                + " comparedOperations=" + operations + " randomOperations=" + randomOperations
                + " diagnosticCases=" + diagnosticCases + " nonAdmittedBoundaryDifferences=" + boundaryDifferences
                + " provider=Spring-6.2.0 performance=NOT_MEASURED ownWireCompatibility=NOT_CLAIMED");
    }
}
