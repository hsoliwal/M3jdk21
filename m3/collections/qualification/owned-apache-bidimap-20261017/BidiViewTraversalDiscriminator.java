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
import com.m3.collections.apache.M3BidiMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntFunction;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

/**
 * Emits observed provider/candidate traversal differences; it asserts no assumed outcome.
 * Every case starts with fresh maps. Streams extract/mutate one selected API entry only;
 * no mutation occurs concurrently with entry extraction. Production storage is untouched.
 */
public final class BidiViewTraversalDiscriminator {
    private enum Fixture { EMPTY, SINGLE, TWO, ORDERED_NULLS }
    private enum EntryPath { ITERATOR, ARRAY, TYPED_ARRAY, GENERATOR_ARRAY, SPLITERATOR, STREAM, PARALLEL_STREAM }
    private enum Mutation { REPLACE, DUPLICATE_OTHER, SAME, NULL_VALUE }
    private enum ArrayCase { CORRECT, OVERSIZED, WRONG_COMPONENT, RETURNS_NULL, THROWS, NULL_FUNCTION }
    private enum CallbackPath {
        ENTRY_FOR_EACH, ENTRY_TRY_ADVANCE, ENTRY_FOR_EACH_REMAINING,
        ENTRY_STREAM_ONE, ENTRY_PARALLEL_ONE, KEY_FOR_EACH, VALUE_FOR_EACH, ENTRY_REMOVE_IF, NULL_FOR_EACH
    }

    private record Pair(Object key, Object value) { }
    private record State(List<Pair> forward, List<Pair> inverse,
                         List<Object> keys, List<Object> values,
                         List<Object> inverseKeys, List<Object> inverseValues,
                         List<Object> reverseLookups, boolean consistent) { }
    private record Result(Object returned, String error, String message,
                          State before, State after, List<Integer> generatorCalls,
                          List<Object> callbackEvents, String arrayComponent,
                          int arrayLength, String entryClass, int spliteratorFlags) { }
    private static final class Facts {
        final List<Integer> generatorCalls = new ArrayList<>();
        final List<Object> events = new ArrayList<>();
        String arrayComponent = "";
        int arrayLength = -1;
        String entryClass = "";
        int spliteratorFlags = -1;
    }
    private static final class ProbeFailure extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;
        ProbeFailure(String operation) { super(operation); }
    }
    @FunctionalInterface
    private interface Operation { Object apply(BidiMap<String, Integer> map, Facts facts); }
    private static int cases;
    private static int equivalent;

    private BidiViewTraversalDiscriminator() { }

    private static BidiMap<String, Integer> fresh(boolean oracle, Fixture fixture) {
        BidiMap<String, Integer> map = oracle ? new DualLinkedHashBidiMap<>() : new M3BidiMap<>();
        if (fixture != Fixture.EMPTY) map.put("a", 1);
        if (fixture == Fixture.TWO) map.put("b", 2);
        if (fixture == Fixture.ORDERED_NULLS) {
            map.put(null, 2); map.put("c", null); map.put("d", 4);
        }
        return map;
    }

    private static List<Pair> entries(Map<?, ?> map) {
        List<Pair> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) result.add(new Pair(entry.getKey(), entry.getValue()));
        return result;
    }

    private static List<Object> copy(Collection<?> collection) { return new ArrayList<>(collection); }

    private static State state(BidiMap<String, Integer> map) {
        BidiMap<Integer, String> inverse = map.inverseBidiMap();
        List<Pair> forward = entries(map); List<Pair> reverse = entries(inverse);
        boolean consistent = map.size() == inverse.size();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            consistent &= inverse.containsKey(entry.getValue()) && Objects.equals(inverse.get(entry.getValue()), entry.getKey());
        }
        for (Map.Entry<Integer, String> entry : inverse.entrySet()) {
            consistent &= map.containsKey(entry.getValue()) && Objects.equals(map.get(entry.getValue()), entry.getKey());
        }
        List<Object> lookups = new ArrayList<>();
        for (Integer value : Arrays.asList(1, 2, 3, null)) {
            lookups.add(Arrays.asList(value, map.containsValue(value), map.getKey(value)));
        }
        return new State(forward, reverse, copy(map.keySet()), copy(map.values()),
                copy(inverse.keySet()), copy(inverse.values()), lookups, consistent);
    }

    private static Object snapshot(Object value) {
        if (value instanceof Map.Entry<?, ?> entry) return new Pair(entry.getKey(), entry.getValue());
        if (value instanceof Object[] array) {
            List<Object> result = new ArrayList<>();
            for (Object item : array) result.add(snapshot(item));
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : collection) result.add(snapshot(item));
            return result;
        }
        return value;
    }

    private static Result run(boolean oracle, Fixture fixture, Operation operation) {
        BidiMap<String, Integer> map = fresh(oracle, fixture); Facts facts = new Facts();
        State before = state(map); Object result = null; String error = ""; String message = "";
        try { result = snapshot(operation.apply(map, facts)); }
        catch (RuntimeException failure) { error = failure.getClass().getName(); message = String.valueOf(failure.getMessage()); }
        return new Result(result, error, message, before, state(map),
                new ArrayList<>(facts.generatorCalls), snapshotEvents(facts.events),
                facts.arrayComponent, facts.arrayLength, facts.entryClass, facts.spliteratorFlags);
    }

    private static List<Object> snapshotEvents(List<Object> events) {
        List<Object> result = new ArrayList<>();
        for (Object event : events) result.add(snapshot(event));
        return result;
    }

    private static void compare(String label, Fixture fixture, Operation operation) {
        Result oracle = run(true, fixture, operation); Result candidate = run(false, fixture, operation);
        boolean same = Objects.equals(oracle.returned(), candidate.returned()) && oracle.error().equals(candidate.error())
                && oracle.before().equals(candidate.before()) && oracle.after().equals(candidate.after())
                && oracle.generatorCalls().equals(candidate.generatorCalls()) && oracle.callbackEvents().equals(candidate.callbackEvents())
                && oracle.arrayComponent().equals(candidate.arrayComponent()) && oracle.arrayLength() == candidate.arrayLength()
                && oracle.spliteratorFlags() == candidate.spliteratorFlags();
        Map<String, Object> row = new LinkedHashMap<>(); row.put("case", label); row.put("fixture", fixture.name());
        row.put("behaviorEquivalent", same); row.put("oracle", resultMap(oracle)); row.put("candidate", resultMap(candidate));
        System.out.println(json(row)); cases++; if (same) equivalent++;
    }

    private static Map<String, Object> resultMap(Result result) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("returned", result.returned()); fields.put("error", result.error()); fields.put("message", result.message());
        fields.put("before", result.before()); fields.put("after", result.after()); fields.put("generatorCalls", result.generatorCalls());
        fields.put("callbackEvents", result.callbackEvents()); fields.put("arrayComponent", result.arrayComponent());
        fields.put("arrayLength", result.arrayLength()); fields.put("entryClassMetadataOnly", result.entryClass());
        fields.put("spliteratorFlags", result.spliteratorFlags());
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<String, Integer> entry(Object value) {
        if (!(value instanceof Map.Entry<?, ?>)) throw new IllegalStateException("traversal did not return an API entry");
        return (Map.Entry<String, Integer>) value;
    }

    private static void arrayFacts(Object[] array, Facts facts) {
        facts.arrayComponent = array.getClass().getComponentType().getName(); facts.arrayLength = array.length;
    }

    private static Map.Entry<String, Integer> extract(BidiMap<String, Integer> map, EntryPath path, Facts facts) {
        facts.spliteratorFlags = map.entrySet().spliterator().characteristics();
        Map.Entry<String, Integer> selected;
        switch (path) {
            case ITERATOR -> selected = map.entrySet().iterator().next();
            case ARRAY -> { Object[] array = map.entrySet().toArray(); arrayFacts(array, facts); selected = entry(array[0]); }
            case TYPED_ARRAY -> {
                Map.Entry<?, ?>[] array = map.entrySet().toArray(new Map.Entry<?, ?>[0]);
                arrayFacts(array, facts); selected = entry(array[0]);
            }
            case GENERATOR_ARRAY -> {
                Map.Entry<?, ?>[] array = map.entrySet().toArray(size -> {
                    facts.generatorCalls.add(size); return new Map.Entry<?, ?>[size];
                });
                arrayFacts(array, facts); selected = entry(array[0]);
            }
            case SPLITERATOR -> {
                Spliterator<Map.Entry<String, Integer>> cursor = map.entrySet().spliterator();
                facts.spliteratorFlags = cursor.characteristics();
                List<Map.Entry<String, Integer>> one = new ArrayList<>(1);
                facts.events.add(cursor.tryAdvance(one::add)); selected = one.get(0);
            }
            case STREAM -> selected = map.entrySet().stream().filter(item -> "a".equals(item.getKey())).findFirst().orElseThrow();
            case PARALLEL_STREAM -> {
                // One selected entry; mutation begins only after the terminal operation finishes.
                selected = map.entrySet().parallelStream().filter(item -> "a".equals(item.getKey())).limit(1).findAny().orElseThrow();
            }
            default -> throw new AssertionError(path);
        }
        facts.entryClass = selected.getClass().getName(); facts.events.add(new Pair(selected.getKey(), selected.getValue()));
        return selected;
    }

    private static Integer assignment(Mutation mutation) {
        return switch (mutation) { case REPLACE -> 3; case DUPLICATE_OTHER -> 2; case SAME -> 1; case NULL_VALUE -> null; };
    }

    private static Object arrayGenerator(BidiMap<String, Integer> map, ArrayCase kind, Facts facts) {
        Object[] result = switch (kind) {
            case CORRECT -> map.entrySet().toArray(size -> { facts.generatorCalls.add(size); return new Map.Entry<?, ?>[size]; });
            case OVERSIZED -> map.entrySet().toArray(size -> {
                facts.generatorCalls.add(size); Object[] array = new Object[8]; Arrays.fill(array, "tail"); return array;
            });
            case WRONG_COMPONENT -> map.entrySet().toArray((IntFunction<String[]>) size -> {
                facts.generatorCalls.add(size); facts.arrayComponent = String.class.getName(); return new String[size];
            });
            case RETURNS_NULL -> map.entrySet().toArray((IntFunction<Object[]>) size -> { facts.generatorCalls.add(size); return null; });
            case THROWS -> map.entrySet().toArray((IntFunction<Object[]>) size -> {
                facts.generatorCalls.add(size); throw new ProbeFailure("array generator");
            });
            case NULL_FUNCTION -> map.entrySet().toArray((IntFunction<Object[]>) null);
        };
        arrayFacts(result, facts); return result;
    }

    private static void mutateThenThrow(Map.Entry<String, Integer> entry, Facts facts, boolean alwaysThrow) {
        facts.entryClass = entry.getClass().getName(); facts.events.add(new Pair(entry.getKey(), entry.getValue()));
        if ("a".equals(entry.getKey())) facts.events.add(entry.setValue(3));
        if (alwaysThrow || "b".equals(entry.getKey())) throw new ProbeFailure("entry callback");
    }

    private static Object callback(BidiMap<String, Integer> map, CallbackPath path, Facts facts) {
        switch (path) {
            case ENTRY_FOR_EACH -> map.entrySet().forEach(item -> mutateThenThrow(item, facts, false));
            case ENTRY_TRY_ADVANCE -> {
                Spliterator<Map.Entry<String, Integer>> cursor = map.entrySet().spliterator();
                facts.spliteratorFlags = cursor.characteristics(); cursor.tryAdvance(item -> mutateThenThrow(item, facts, true));
            }
            case ENTRY_FOR_EACH_REMAINING -> {
                Spliterator<Map.Entry<String, Integer>> cursor = map.entrySet().spliterator();
                facts.spliteratorFlags = cursor.characteristics(); cursor.forEachRemaining(item -> mutateThenThrow(item, facts, false));
            }
            case ENTRY_STREAM_ONE -> map.entrySet().stream().filter(item -> "a".equals(item.getKey()))
                    .limit(1).forEachOrdered(item -> mutateThenThrow(item, facts, true));
            case ENTRY_PARALLEL_ONE -> map.entrySet().parallelStream().filter(item -> "a".equals(item.getKey()))
                    .limit(1).forEachOrdered(item -> mutateThenThrow(item, facts, true));
            case KEY_FOR_EACH -> map.keySet().forEach(key -> {
                facts.events.add(key); if ("b".equals(key)) throw new ProbeFailure("key callback");
            });
            case VALUE_FOR_EACH -> map.values().forEach(value -> {
                facts.events.add(value); if (Objects.equals(value, 2)) throw new ProbeFailure("value callback");
            });
            case ENTRY_REMOVE_IF -> {
                return map.entrySet().removeIf(item -> {
                    facts.events.add(new Pair(item.getKey(), item.getValue()));
                    if ("b".equals(item.getKey())) throw new ProbeFailure("entry predicate");
                    return true;
                });
            }
            case NULL_FOR_EACH -> map.entrySet().forEach(null);
            default -> throw new AssertionError(path);
        }
        return null;
    }

    private static Object order(Collection<?> collection, String traversal, Facts facts) {
        facts.spliteratorFlags = collection.spliterator().characteristics();
        return switch (traversal) {
            case "iterator" -> { List<Object> values = new ArrayList<>(); collection.iterator().forEachRemaining(values::add); yield values; }
            case "array" -> { Object[] array = collection.toArray(); arrayFacts(array, facts); yield array; }
            case "typedArray" -> { Object[] array = collection.toArray(new Object[0]); arrayFacts(array, facts); yield array; }
            case "generatorArray" -> {
                Object[] array = collection.toArray(size -> { facts.generatorCalls.add(size); return new Object[size]; });
                arrayFacts(array, facts); yield array;
            }
            case "spliterator" -> {
                Spliterator<?> cursor = collection.spliterator(); facts.spliteratorFlags = cursor.characteristics();
                List<Object> values = new ArrayList<>(); while (cursor.tryAdvance(values::add)) { /* observe encounter order */ }
                yield values;
            }
            case "stream" -> collection.stream().toList();
            case "parallelStream" -> collection.parallelStream().toList();
            default -> throw new AssertionError(traversal);
        };
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Pair pair) return json(Arrays.asList(pair.key(), pair.value()));
        if (value instanceof State state) {
            Map<String, Object> fields = new LinkedHashMap<>(); fields.put("forward", state.forward()); fields.put("inverse", state.inverse());
            fields.put("keys", state.keys()); fields.put("values", state.values()); fields.put("inverseKeys", state.inverseKeys());
            fields.put("inverseValues", state.inverseValues()); fields.put("reverseLookups", state.reverseLookups());
            fields.put("consistent", state.consistent()); return json(fields);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder text = new StringBuilder("{"); boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) text.append(','); first = false; text.append(json(String.valueOf(entry.getKey()))).append(':').append(json(entry.getValue()));
            }
            return text.append('}').toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder text = new StringBuilder("["); boolean first = true;
            for (Object item : collection) { if (!first) text.append(','); first = false; text.append(json(item)); }
            return text.append(']').toString();
        }
        throw new IllegalArgumentException("unexpected diagnostic payload type " + value.getClass().getName());
    }

    public static void main(String[] args) {
        for (EntryPath path : EntryPath.values()) for (Mutation mutation : Mutation.values()) {
            compare("entry/" + path + "/" + mutation, Fixture.TWO,
                    (map, facts) -> extract(map, path, facts).setValue(assignment(mutation)));
        }
        for (Mutation mutation : List.of(Mutation.REPLACE, Mutation.SAME, Mutation.NULL_VALUE)) {
            compare("entry/PARALLEL_STREAM_SINGLE/" + mutation, Fixture.SINGLE,
                    (map, facts) -> extract(map, EntryPath.PARALLEL_STREAM, facts).setValue(assignment(mutation)));
        }
        for (String traversal : List.of("iterator", "array", "typedArray", "generatorArray", "spliterator", "stream", "parallelStream")) {
            compare("keyOrder/" + traversal, Fixture.ORDERED_NULLS, (map, facts) -> order(map.keySet(), traversal, facts));
            compare("valueOrder/" + traversal, Fixture.ORDERED_NULLS, (map, facts) -> order(map.values(), traversal, facts));
        }
        for (Fixture fixture : List.of(Fixture.EMPTY, Fixture.TWO)) for (ArrayCase kind : ArrayCase.values()) {
            compare("arrayGenerator/" + kind, fixture, (map, facts) -> arrayGenerator(map, kind, facts));
        }
        for (CallbackPath path : CallbackPath.values()) {
            compare("callback/" + path, Fixture.TWO, (map, facts) -> callback(map, path, facts));
        }
        Map<String, Object> summary = new LinkedHashMap<>(); summary.put("summary", "BIDI_VIEW_TRAVERSAL_OBSERVATIONS");
        summary.put("cases", cases); summary.put("equivalent", equivalent); summary.put("different", cases - equivalent);
        summary.put("scope", "observable source-pinned provider differential, not admission");
        summary.put("entryClassAndExceptionMessage", "metadata; concrete class identity and message text excluded from comparison");
        summary.put("parallelOrdering", "observed order only; spliterator flags record whether ORDERED is promised");
        System.out.println(json(summary));
    }
}
