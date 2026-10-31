/*
 * Copyright (C) 2007 The Guava Authors.
 * Copyright 2026 Hitesh Soliwal and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Modified 2026: adapt the thirteen pinned Guava HashBiMapTest behaviors to an
 * independent standalone, two-direction provider oracle and add discriminators.
 * Test donor: google/guava f06690fa3e874f65515e8fd338a74d636e2c792f,
 * HashBiMapTest blob f4bee7bf9830b47d6519453c093a999c77ebd7f9.
 */
package com.m3.collections.guava;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;

/**
 * Strict common-API oracle against the actual Guava 33.4.8 JRE provider.
 *
 * <p>This is a separately named derivation of the preserved comprehensive test.
 * It executes its unchanged force-position and dense-growth groups, followed by
 * the original 29-operation, 12000-step random structure. Random inputs use one
 * interned token per logical key/value ID, shared by both bodies. Equal-but-distinct
 * representative ambiguities are excluded by input construction; no result,
 * callback, exception or state mismatch is waived. The original Android-valid
 * profile and the separate variant probe retain representative coverage.
 * The two cross-variant keySet/inverse.values and values/inverse.keySet object
 * identity comparisons are excluded here: BiMap requires equivalent live Set
 * contents, not identical view objects. Their strict Android observations and
 * the actual JRE identity failure remain in the unchanged original test/proofs.
 *
 * <p>Root binds actual jar checksums, classpath, compilation and execution.
 * Public BiMap/Map/Set state and both inverse directions remain strictly checked.
 * Concrete wire/class identity, capacity and entries used after an external
 * structural mutation are excluded. This does not assert whole-test completion,
 * all Guava APIs, concurrency, GC behavior or comparative performance.
 */
public final class M3HashBiMapCommonSelfTest {
    private static long checks;
    private static long operations;
    private static long cases;
    private static long donorCases;
    private static long randomOperations;
    private static String runtimeVariant;

    private M3HashBiMapCommonSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static final class Token implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        final String domain;
        final int id;
        final int representative;

        Token(String domain, int id, int representative) {
            this.domain = domain; this.id = id; this.representative = representative;
        }
        @Override public boolean equals(Object other) {
            return other instanceof Token token && id == token.id && domain.equals(token.domain);
        }
        @Override public int hashCode() { return domain.endsWith("C") ? 1 : 31 * domain.hashCode() + id; }
        @Override public String toString() { return domain + id + "#" + representative; }
    }

    private record Rep(String domain, int id, int representative) { }
    private record FrozenEntry(Object key, Object value) { }
    private record Outcome(Object value, Class<?> failure) { }

    private static final class Boom extends RuntimeException {
        @Serial private static final long serialVersionUID = 1L;
        Boom() { super("deliberate provider-oracle callback failure"); }
    }

    @FunctionalInterface
    private interface Call { Object run() throws Exception; }

    @FunctionalInterface
    private interface Operation { Object run(Context context) throws Exception; }

    private static Token key(int id) { return new Token("K", id, 0); }
    private static Token value(int id) { return new Token("V", id, 0); }
    private static Token collisionKey(int id) { return new Token("KC", id, 0); }
    private static Token collisionValue(int id) { return new Token("VC", id, 0); }

    private static Object freeze(Object object) {
        if (object instanceof Token token) return new Rep(token.domain, token.id, token.representative);
        if (object instanceof Map.Entry<?, ?> entry) {
            return new FrozenEntry(freeze(entry.getKey()), freeze(entry.getValue()));
        }
        if (object instanceof Map<?, ?> map) {
            List<Object> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) entries.add(freeze(entry));
            return entries;
        }
        if (object instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : collection) result.add(freeze(item));
            return result;
        }
        if (object instanceof Object[] array) {
            List<Object> result = new ArrayList<>();
            for (Object item : array) result.add(freeze(item));
            return result;
        }
        return object;
    }

    private static Outcome capture(Call call) {
        try { return new Outcome(freeze(call.run()), null); }
        catch (Exception failure) { return new Outcome(null, failure.getClass()); }
    }

    private static final class Context {
        final boolean candidate;
        final BiMap<Token, Token> map;
        final List<String> trace = new ArrayList<>();

        Context(boolean candidate, int expectedSize) {
            this.candidate = candidate;
            map = candidate ? M3HashBiMap.create(expectedSize) : HashBiMap.create(expectedSize);
        }
        BiMap<Token, Token> direction(boolean inverse) { return inverse ? map.inverse() : map; }
        void mark(String event) { trace.add(event); }
        BiMap<Token, Token> copy(Map<? extends Token, ? extends Token> source) {
            return candidate ? M3HashBiMap.create(source) : HashBiMap.create(source);
        }
        BiMap<Token, Token> empty(int expectedSize) {
            return candidate ? M3HashBiMap.create(expectedSize) : HashBiMap.create(expectedSize);
        }
    }

    private static final class Pair {
        final String name;
        final Context expected;
        final Context actual;

        Pair(String name) { this(name, 0); }
        Pair(String name, int expectedSize) {
            this.name = name;
            expected = new Context(false, expectedSize);
            actual = new Context(true, expectedSize);
            cases++;
        }
        void step(String label, Operation operation) {
            operations++;
            expected.trace.clear(); actual.trace.clear();
            Outcome left = capture(() -> operation.run(expected));
            Outcome right = capture(() -> operation.run(actual));
            String at = name + ": " + label;
            check(Objects.equals(left.failure(), right.failure()),
                    at + " exception classes: " + left + " / " + right + " runtime=" + runtimeVariant);
            check(Objects.equals(left.value(), right.value()),
                    at + " result/representative: " + left + " / " + right + " runtime=" + runtimeVariant);
            check(expected.trace.equals(actual.trace),
                    at + " callback trace: " + expected.trace + " / " + actual.trace);
            state(expected.map, actual.map, at);
        }
    }

    private static void invariant(BiMap<Token, Token> map, String label) {
        check(map.inverse().inverse() == map, label + " inverse involution identity");
        check(map.inverse() == map.inverse(), label + " cached inverse identity");
        check(map.size() == map.inverse().size(), label + " inverse size");
        check(map.values() instanceof Set<?>, label + " set-valued values view");
        Set<Token> seen = new HashSet<>();
        for (Map.Entry<Token, Token> entry : map.entrySet()) {
            Token key = entry.getKey(); Token value = entry.getValue();
            check(seen.add(value), label + " unique value");
            check(map.containsKey(key) && map.containsValue(value), label + " entry presence");
            check(map.get(key) == value, label + " stored value representative");
            check(map.inverse().get(value) == key, label + " inverse key representative");
        }
        check(seen.size() == map.size(), label + " value count");
    }

    private static void state(BiMap<Token, Token> expected, BiMap<Token, Token> actual, String label) {
        Object left = freeze(expected); Object right = freeze(actual);
        check(Objects.equals(left, right), label + " ordered forward state: " + left + " / " + right);
        check(Objects.equals(freeze(expected.inverse()), freeze(actual.inverse())), label + " ordered inverse state");
        check(expected.equals(actual) && actual.equals(expected), label + " forward equality");
        check(expected.inverse().equals(actual.inverse()) && actual.inverse().equals(expected.inverse()), label + " inverse equality");
        check(expected.size() == actual.size() && expected.isEmpty() == actual.isEmpty(), label + " size/empty");
        check(expected.hashCode() == actual.hashCode(), label + " map hash");
        check(expected.inverse().hashCode() == actual.inverse().hashCode(), label + " inverse hash");
        check(expected.toString().equals(actual.toString()), label + " map string");
        check(Objects.equals(freeze(expected.keySet()), freeze(actual.keySet())), label + " ordered key view");
        check(Objects.equals(freeze(expected.values()), freeze(actual.values())), label + " ordered value view");
        check(expected.keySet().equals(actual.keySet()) && actual.keySet().equals(expected.keySet()), label + " key-set equals");
        check(expected.values().equals(actual.values()) && actual.values().equals(expected.values()), label + " value-set equals");
        check(expected.keySet().hashCode() == actual.keySet().hashCode(), label + " key-set hash");
        check(expected.values().hashCode() == actual.values().hashCode(), label + " value-set hash");
        check(expected.entrySet().equals(actual.entrySet()) && actual.entrySet().equals(expected.entrySet()), label + " entry-set equals");
        check(expected.entrySet().hashCode() == actual.entrySet().hashCode(), label + " entry-set hash");
        // API-law scope: contents/hash/order/live effects remain compared above.
        // Provider-specific cross-view object identity is preserved in the frozen
        // Android-valid corpus, and its separate JRE negative is never relabeled.
        invariant(expected, label + " provider");
        invariant(actual, label + " owned");
    }

    private static Pair seeded(String name) {
        Pair pair = new Pair(name);
        pair.step("put 0", context -> context.map.put(key(0), value(0)));
        pair.step("put 1", context -> context.map.put(key(1), value(1)));
        pair.step("put 2", context -> context.map.put(key(2), value(2)));
        return pair;
    }

    private static void factoriesAndRepresentatives() {
        Pair pair = new Pair("factories/markers");
        pair.step("default create", context -> context.candidate
                ? M3HashBiMap.<Token, Token>create() : HashBiMap.<Token, Token>create());
        pair.step("expected-size create", context -> context.empty(17));
        pair.step("negative expected size", context -> context.empty(-1));
        pair.step("null map factory", context -> context.copy(null));
        pair.step("markers", context -> List.of(context.map instanceof Serializable,
                context.map.inverse() instanceof Serializable, context.map instanceof Cloneable));
        pair.step("copy factory aliases representatives and isolates directory", context -> {
            Token originalKey = new Token("K", 10, 11);
            Token originalValue = new Token("V", 10, 12);
            context.map.put(originalKey, originalValue);
            BiMap<Token, Token> copy = context.copy(context.map);
            boolean aliases = copy.keySet().iterator().next() == originalKey && copy.get(originalKey) == originalValue;
            copy.put(key(11), value(11));
            return Arrays.asList(aliases, !context.map.containsKey(key(11)), copy);
        });
        pair.step("create rejects duplicate values in plain Map", context -> {
            Map<Token, Token> input = new LinkedHashMap<>();
            input.put(key(20), value(20)); input.put(key(21), value(20));
            return context.copy(input);
        });
        Pair reps = new Pair("equal object representatives");
        reps.step("same forward mapping uses incoming return but retains stored pair", context -> {
            Token storedKey = new Token("K", 1, 10);
            Token storedValue = new Token("V", 1, 20);
            context.map.put(storedKey, storedValue);
            Token aliasKey = new Token("K", 1, 30);
            Token aliasValue = new Token("V", 1, 40);
            Token returned = context.map.put(aliasKey, aliasValue);
            return List.of(returned == aliasValue, context.map.keySet().iterator().next() == storedKey,
                    context.map.get(aliasKey) == storedValue, context.map.inverse().get(aliasValue) == storedKey);
        });
        reps.step("same inverse mapping keeps original key/value references", context -> {
            Token oldKey = context.map.keySet().iterator().next();
            Token oldValue = context.map.get(oldKey);
            Token aliasKey = new Token(oldKey.domain, oldKey.id, 50);
            Token aliasValue = new Token(oldValue.domain, oldValue.id, 60);
            Token returned = context.map.inverse().put(aliasValue, aliasKey);
            return List.of(returned == aliasKey, context.map.keySet().iterator().next() == oldKey,
                    context.map.get(aliasKey) == oldValue, context.map.inverse().get(aliasValue) == oldKey);
        });
        reps.step("forward entry equal setter representative", context -> {
            Map.Entry<Token, Token> entry = context.map.entrySet().iterator().next();
            Token original = entry.getValue(); Token alias = new Token(original.domain, original.id, 70);
            Token returned = entry.setValue(alias);
            return List.of(returned == alias, context.map.get(entry.getKey()) == original);
        });
        reps.step("inverse entry equal setter representative", context -> {
            Map.Entry<Token, Token> entry = context.map.inverse().entrySet().iterator().next();
            Token original = entry.getValue(); Token alias = new Token(original.domain, original.id, 80);
            Token returned = entry.setValue(alias);
            return List.of(returned == alias, context.map.inverse().get(entry.getKey()) == original);
        });
    }

    private static void donorThirteen() {
        donorCases++;
        Pair constructor = new Pair("donor testMapConstructor");
        constructor.step("three source mappings", context -> {
            Map<Token, Token> source = new LinkedHashMap<>();
            source.put(key(1), value(1)); source.put(key(2), value(2)); source.put(key(3), value(3));
            BiMap<Token, Token> copy = context.copy(source);
            check(copy.get(key(1)).equals(value(1)), "donor constructor forward lookup");
            check(copy.inverse().get(value(1)).equals(key(1)), "donor constructor inverse lookup");
            return copy;
        });
        donorCases++;
        Pair bash = new Pair("donor testBashIt", 1000);
        for (int i = 0; i < 1000; i++) {
            int id = i;
            bash.step("insert " + id, context -> context.map.put(key(2 * id), value(2 * id + 1)));
        }
        for (int i = 0; i < 1000; i++) {
            int id = i;
            bash.step("shift value " + id, context -> context.map.put(key(2 * id), value(2 * id - 1)));
        }
        bash.step("entry value shift", context -> {
            for (Map.Entry<Token, Token> entry : context.map.entrySet()) {
                entry.setValue(value(entry.getValue().id + 2000));
            }
            return null;
        });
        donorCases++;
        Pair remove = new Pair("donor testBiMapEntrySetIteratorRemove");
        remove.step("put", context -> context.map.put(key(1), value(1)));
        remove.step("setter then iterator remove", context -> {
            Iterator<Map.Entry<Token, Token>> iterator = context.map.entrySet().iterator();
            Map.Entry<Token, Token> entry = iterator.next(); entry.setValue(value(2));
            iterator.remove(); check(context.map.isEmpty(), "donor updated entry removed"); return null;
        });
        donorCases++;
        Pair order = seeded("donor testInsertionOrder");
        order.step("explicit key order", context -> {
            check(new ArrayList<>(context.map.keySet()).equals(List.of(key(0), key(1), key(2))), "donor insertion order");
            return context.map.entrySet();
        });
        for (int position = 0; position < 3; position++) {
            donorCases++;
            int selected = position;
            Pair delete = seeded("donor remove insertion position " + selected);
            delete.step("remove", context -> context.map.remove(key(selected)));
        }
        donorCases++;
        Pair force = seeded("donor testInsertionOrderAfterForcePut");
        force.step("force existing value", context -> context.map.forcePut(key(2), value(0)));
        donorCases++;
        Pair inverseForce = seeded("donor testInsertionOrderAfterInverseForcePut");
        inverseForce.step("inverse force", context -> context.map.inverse().forcePut(value(0), key(2)));
        donorCases++;
        Pair inverseOrder = new Pair("donor testInverseInsertionOrderAfterInverse");
        inverseOrder.step("first", context -> context.map.put(key(1), value(2)));
        inverseOrder.step("second", context -> context.map.put(key(2), value(1)));
        inverseOrder.step("inverse order", context -> context.map.inverse().entrySet());
        donorCases++;
        Pair inverseOrderForce = seeded("donor testInverseInsertionOrderAfterInverseForcePut");
        inverseOrderForce.step("force/order", context -> {
            context.map.inverse().forcePut(value(0), key(2)); return context.map.inverse().entrySet();
        });
        donorCases++;
        Pair present = seeded("donor testInverseInsertionOrderAfterInverseForcePutPresentKey");
        present.step("fourth", context -> context.map.put(key(3), value(3)));
        present.step("inverse existing key position", context -> context.map.inverse().forcePut(value(3), key(1)));
        donorCases++;
        Pair setter = new Pair("donor testInverseEntrySetValueNewKey");
        setter.step("first", context -> context.map.put(key(1), value(1)));
        setter.step("second", context -> context.map.put(key(2), value(2)));
        setter.step("inverse iterator retains next entry after rename", context -> {
            Iterator<Map.Entry<Token, Token>> iterator = context.map.inverse().entrySet().iterator();
            Map.Entry<Token, Token> entry = iterator.next(); entry.setValue(key(3));
            Map.Entry<Token, Token> second = iterator.next();
            check(second.getKey().equals(value(2)), "donor inverse setter next value");
            check(!iterator.hasNext(), "donor inverse iterator exhausted");
            return context.map.entrySet();
        });
        check(donorCases == 13, "all thirteen named donor behaviors adapted");
    }

    private static void forcePutPositionsAndNulls() {
        for (boolean inverse : List.of(false, true)) {
            for (int owner = 0; owner < 4; owner++) {
                for (int replaced = 0; replaced < 4; replaced++) {
                    int first = owner; int second = replaced;
                    Pair pair = new Pair("force position inverse=" + inverse + " owner=" + first + " replacement=" + second);
                    pair.step("four colliding mappings", context -> {
                        for (int i = 0; i < 4; i++) context.map.put(collisionKey(i), collisionValue(i));
                        return null;
                    });
                    pair.step("ordinary duplicate rejects without effects", context -> {
                        Object before = freeze(context.map);
                        Outcome attempted = capture(() -> inverse
                                ? context.map.inverse().put(collisionValue(first), collisionKey(second))
                                : context.map.put(collisionKey(first), collisionValue(second)));
                        check(attempted.failure() == (first == second ? null : IllegalArgumentException.class),
                                "ordinary duplicate rejection class");
                        check(Objects.equals(before, freeze(context.map)), "ordinary rejected/same mapping leaves state unchanged");
                        return attempted;
                    });
                    pair.step("force collision", context -> inverse
                            ? context.map.inverse().forcePut(collisionValue(first), collisionKey(second))
                            : context.map.forcePut(collisionKey(first), collisionValue(second)));
                    pair.step("append after dense move", context -> context.map.put(collisionKey(20), collisionValue(20)));
                }
            }
        }
        Pair nulls = new Pair("nullable bijection");
        nulls.step("null key", context -> context.map.put(null, value(1)));
        nulls.step("null value", context -> context.map.put(key(2), null));
        nulls.step("duplicate null value rejection", context -> context.map.put(key(3), null));
        nulls.step("same null mapping", context -> context.map.put(key(2), null));
        nulls.step("inverse null queries", context -> Arrays.asList(context.map.inverse().get(null),
                context.map.containsKey(null), context.map.containsValue(null), context.map.get(null)));
        nulls.step("force null eviction", context -> context.map.forcePut(key(3), null));
        nulls.step("inverse null key rename", context -> context.map.inverse().forcePut(null, key(4)));
        nulls.step("inverse null forward key", context -> context.map.inverse().forcePut(value(5), null));
        nulls.step("remove null key", context -> context.map.remove(null));
        nulls.step("remove inverse null", context -> context.map.inverse().remove(null));
    }

    private static void denseGrowthAndDeletion() {
        for (int capacity : new int[] {0, 1, 3, 17}) {
            Pair pair = new Pair("growth/dense deletion capacity=" + capacity, capacity);
            for (int i = 0; i < 128; i++) {
                int id = i;
                pair.step("colliding append " + id, context -> context.map.put(collisionKey(id), collisionValue(id)));
            }
            for (int position : new int[] {0, 63, 127, 1, 126, 64, 2, 125}) {
                pair.step("remove dense location " + position, context -> context.map.remove(collisionKey(position)));
                pair.step("reinsert deleted key at tail " + position, context -> context.map.put(collisionKey(position), collisionValue(position)));
            }
            pair.step("every other inverse removal", context -> {
                Iterator<Token> iterator = context.map.inverse().keySet().iterator(); boolean remove = true;
                while (iterator.hasNext()) { iterator.next(); if (remove) iterator.remove(); remove = !remove; }
                return null;
            });
            pair.step("grow after recycled dense slots", context -> {
                for (int i = 128; i < 300; i++) context.map.put(collisionKey(i), collisionValue(i));
                return context.map.size();
            });
            pair.step("clear retained live views", context -> {
                Set<Token> keys = context.map.keySet(); Set<Token> values = context.map.values();
                Set<Map.Entry<Token, Token>> entries = context.map.entrySet();
                context.map.clear();
                check(keys.isEmpty() && values.isEmpty() && entries.isEmpty(), "retained views see clear");
                context.map.put(collisionKey(500), collisionValue(500));
                return Arrays.asList(keys, values, entries);
            });
        }
    }

    private static Collection<?> selectedView(BiMap<Token, Token> map, int kind) {
        return switch (kind) {
            case 0 -> map.keySet();
            case 1 -> map.values();
            default -> map.entrySet();
        };
    }

    private static void viewMutationsAndIteratorStates() {
        for (boolean inverse : List.of(false, true)) {
            for (int kind = 0; kind < 3; kind++) {
                int selected = kind;
                Pair pair = seeded("view mutation inverse=" + inverse + " kind=" + selected);
                pair.step("view cached", context -> {
                    BiMap<Token, Token> map = context.direction(inverse);
                    return selectedView(map, selected) == selectedView(map, selected);
                });
                pair.step("unsupported add", context -> selectedView(context.direction(inverse), selected).add(null));
                pair.step("empty addAll", context -> selectedView(context.direction(inverse), selected).addAll(List.of()));
                pair.step("nonempty addAll", context -> selectedView(context.direction(inverse), selected).addAll(java.util.Collections.singletonList(null)));
                pair.step("null addAll", context -> selectedView(context.direction(inverse), selected).addAll(null));
                pair.step("null removeAll", context -> selectedView(context.direction(inverse), selected).removeAll(null));
                pair.step("null retainAll", context -> selectedView(context.direction(inverse), selected).retainAll(null));
                pair.step("null removeIf", context -> selectedView(context.direction(inverse), selected).removeIf(null));
                pair.step("null collection forEach", context -> { selectedView(context.direction(inverse), selected).forEach(null); return null; });
                pair.step("null iterator forEachRemaining", context -> {
                    selectedView(context.direction(inverse), selected).iterator().forEachRemaining(null); return null;
                });
                pair.step("null spliterator tryAdvance", context -> selectedView(context.direction(inverse), selected).spliterator().tryAdvance(null));
                pair.step("null spliterator forEachRemaining", context -> {
                    selectedView(context.direction(inverse), selected).spliterator().forEachRemaining(null); return null;
                });
                pair.step("null typed array", context -> selectedView(context.direction(inverse), selected).toArray((Object[]) null));
                pair.step("empty removeAll", context -> selectedView(context.direction(inverse), selected).removeAll(List.of()));
                pair.step("iterator remove before next", context -> {
                    selectedView(context.direction(inverse), selected).iterator().remove(); return null;
                });
                pair.step("iterator remove and continue", context -> {
                    Iterator<?> iterator = selectedView(context.direction(inverse), selected).iterator();
                    // Capture an entry while it is valid; never query a removed entry afterward.
                    Object delivered = freeze(iterator.next()); iterator.remove();
                    List<Object> remaining = new ArrayList<>();
                    iterator.forEachRemaining(remaining::add);
                    return Arrays.asList(delivered, remaining);
                });
                pair.step("removeIf commits callback prefix", context -> selectedView(context.direction(inverse), selected).removeIf(item -> {
                    context.mark("visit:" + freeze(item));
                    Token target = selected == 1 ? (inverse ? key(2) : value(2)) : (inverse ? value(2) : key(2));
                    if (Objects.equals(item, target)
                            || item instanceof Map.Entry<?, ?> entry && Objects.equals(entry.getKey(), inverse ? value(2) : key(2))) {
                        throw new Boom();
                    }
                    return true;
                }));
                pair.step("retainAll empty clears", context -> selectedView(context.direction(inverse), selected).retainAll(List.of()));
                pair.step("exhausted iterator next", context -> selectedView(context.direction(inverse), selected).iterator().next());
                pair.step("clear empty", context -> { selectedView(context.direction(inverse), selected).clear(); return null; });
                pair.step("empty null removeIf", context -> selectedView(context.direction(inverse), selected).removeIf(null));
                pair.step("empty null iterator consumer", context -> {
                    selectedView(context.direction(inverse), selected).iterator().forEachRemaining(null); return null;
                });
                for (int mutation = 0; mutation < 3; mutation++) {
                    int changed = mutation;
                    Pair external = seeded("iterator structural mutation inverse=" + inverse + " kind=" + selected + " mutation=" + changed);
                    external.step("public iterator observations after external mutation", context -> {
                        Iterator<?> iterator = selectedView(context.direction(inverse), selected).iterator();
                        // An entry returned before external mutation is not queried again.
                        Object delivered = freeze(iterator.next());
                        if (changed == 0) context.map.put(key(100), value(100));
                        else if (changed == 1) context.map.remove(key(2));
                        else context.map.clear();
                        Outcome hasNext = capture(iterator::hasNext);
                        Outcome next = capture(iterator::next);
                        Outcome remove = capture(() -> { iterator.remove(); return null; });
                        return Arrays.asList(delivered, hasNext, next, remove);
                    });
                }
            }
        }
        for (boolean inverse : List.of(false, true)) {
            Pair pair = seeded("entry setter/iterator lifecycle inverse=" + inverse);
            pair.step("setter then removes updated pair", context -> {
                BiMap<Token, Token> map = context.direction(inverse);
                Iterator<Map.Entry<Token, Token>> iterator = map.entrySet().iterator();
                Map.Entry<Token, Token> entry = iterator.next(); Token stableKey = entry.getKey();
                Token previous = entry.setValue(inverse ? key(100) : value(100));
                iterator.remove();
                check(!map.containsKey(stableKey), "valid setter/iterator.remove must remove the updated pair");
                return Arrays.asList(previous, iterator.hasNext());
            });
            Pair duplicate = seeded("entry duplicate rejection inverse=" + inverse);
            duplicate.step("duplicate entry setter", context -> {
                Iterator<Map.Entry<Token, Token>> iterator = context.direction(inverse).entrySet().iterator();
                Map.Entry<Token, Token> first = iterator.next(); Map.Entry<Token, Token> second = iterator.next();
                return first.setValue(second.getValue());
            });
            Pair repeat = seeded("iterator double remove inverse=" + inverse);
            repeat.step("double remove", context -> {
                Iterator<Token> iterator = context.direction(inverse).keySet().iterator();
                iterator.next(); iterator.remove(); iterator.remove(); return null;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<Token, Token> typedEntry(Object object) {
        if (!(object instanceof Map.Entry<?, ?>)) throw new AssertionError("entry traversal returned a non-entry");
        // Inputs come exclusively from entrySet() of the statically typed BiMap<Token,Token>.
        return (Map.Entry<Token, Token>) object;
    }

    private static List<Map.Entry<Token, Token>> traversedEntries(Context context, boolean inverse, int kind) {
        Set<Map.Entry<Token, Token>> entries = context.direction(inverse).entrySet();
        List<Map.Entry<Token, Token>> reached = new ArrayList<>();
        switch (kind) {
            case 0 -> entries.iterator().forEachRemaining(reached::add);
            case 1 -> { for (Object item : entries.toArray()) reached.add(typedEntry(item)); }
            case 2 -> { for (Map.Entry<?, ?> item : entries.toArray(new Map.Entry<?, ?>[0])) reached.add(typedEntry(item)); }
            case 3 -> {
                for (Map.Entry<?, ?> item : entries.toArray(length -> {
                    context.mark("generator:" + length); return new Map.Entry<?, ?>[length];
                })) reached.add(typedEntry(item));
            }
            case 4 -> entries.stream().forEachOrdered(reached::add);
            case 5 -> entries.forEach(reached::add);
            case 6 -> {
                Spliterator<Map.Entry<Token, Token>> cursor = entries.spliterator();
                while (cursor.tryAdvance(reached::add)) { /* consume */ }
            }
            default -> entries.spliterator().forEachRemaining(reached::add);
        }
        return reached;
    }

    private static void arraysStreamsAndSetters() {
        for (boolean inverse : List.of(false, true)) {
            for (int kind = 0; kind < 8; kind++) {
                int traversal = kind;
                Pair pair = seeded("entry traversal inverse=" + inverse + " kind=" + traversal);
                pair.step("traversal and first setter", context -> {
                    List<Map.Entry<Token, Token>> reached = traversedEntries(context, inverse, traversal);
                    List<Object> before = new ArrayList<>();
                    for (Map.Entry<Token, Token> entry : reached) before.add(freeze(entry));
                    Map.Entry<Token, Token> first = reached.get(0);
                    Token old = first.setValue(inverse ? key(70) : value(70));
                    return Arrays.asList(before, old, first.getKey(), first.getValue());
                });
            }
            Pair arrays = seeded("typed arrays inverse=" + inverse);
            arrays.step("key array canaries", context -> {
                Token[] target = new Token[6]; Arrays.fill(target, key(999));
                Token[] result = context.direction(inverse).keySet().toArray(target);
                return Arrays.asList(result == target, target);
            });
            arrays.step("value array canaries", context -> {
                Token[] target = new Token[6]; Arrays.fill(target, value(999));
                Token[] result = context.direction(inverse).values().toArray(target);
                return Arrays.asList(result == target, target);
            });
            arrays.step("entry array canaries", context -> {
                Map.Entry<?, ?>[] target = new Map.Entry<?, ?>[6];
                Arrays.fill(target, new AbstractMap.SimpleImmutableEntry<>(key(999), value(999)));
                Map.Entry<?, ?>[] result = context.direction(inverse).entrySet().toArray(target);
                return Arrays.asList(result == target, target);
            });
            arrays.step("generated array reentrant insertion before traversal", context ->
                    context.direction(inverse).keySet().toArray(length -> {
                        context.mark("generator:" + length); context.map.put(key(20), value(20)); return new Token[length];
                    }));
            arrays.step("generator failure", context -> context.direction(inverse).values().toArray(length -> {
                context.mark("generator:" + length); throw new Boom();
            }));
            arrays.step("incompatible typed array", context -> context.direction(inverse).keySet().toArray(new String[0]));
            arrays.step("spliterator flags/size", context -> {
                Spliterator<Token> cursor = context.direction(inverse).keySet().spliterator();
                return List.of(cursor.characteristics(), cursor.estimateSize(), cursor.getExactSizeIfKnown());
            });
            arrays.step("readonly stream order and tuples", context -> context.direction(inverse).entrySet().stream()
                    .map(entry -> new FrozenEntry(freeze(entry.getKey()), freeze(entry.getValue()))).toList());
            Pair stream = seeded("stream entry setter inverse=" + inverse);
            stream.step("in-stream unique setters", context -> {
                List<Object> returned = new ArrayList<>();
                context.direction(inverse).entrySet().stream().forEachOrdered(entry -> {
                    context.mark("set:" + entry.getKey());
                    Token old = entry.setValue(inverse ? key(100 + entry.getKey().id) : value(100 + entry.getKey().id));
                    returned.add(freeze(old));
                });
                return returned;
            });
        }
    }

    private static final class PrefixMap extends AbstractMap<Token, Token> {
        @Override public Set<Entry<Token, Token>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return 2; }
                @Override public Iterator<Entry<Token, Token>> iterator() {
                    return new Iterator<>() {
                        private int position;
                        @Override public boolean hasNext() { return position < 2; }
                        @Override public Entry<Token, Token> next() {
                            if (position++ == 0) return new SimpleImmutableEntry<>(key(50), value(50));
                            throw new Boom();
                        }
                    };
                }
            };
        }
    }

    private static void mapDefaultsAndCallbacks() {
        for (boolean inverse : List.of(false, true)) {
            Pair pair = seeded("Map defaults inverse=" + inverse);
            Token query = inverse ? value(0) : key(0);
            Token replacement = inverse ? key(30) : value(30);
            Token missing = inverse ? value(40) : key(40);
            pair.step("getOrDefault", context -> context.direction(inverse).getOrDefault(missing, replacement));
            pair.step("putIfAbsent existing", context -> context.direction(inverse).putIfAbsent(query, replacement));
            pair.step("computeIfAbsent null function", context -> context.direction(inverse).computeIfAbsent(query, null));
            pair.step("computeIfPresent null function", context -> context.direction(inverse).computeIfPresent(missing, null));
            pair.step("compute null function", context -> context.direction(inverse).compute(missing, null));
            pair.step("computeIfAbsent existing skips callback", context -> context.direction(inverse).computeIfAbsent(query, key -> {
                context.mark("unexpected"); return replacement;
            }));
            pair.step("computeIfAbsent structural callback", context -> context.direction(inverse).computeIfAbsent(missing, key -> {
                context.mark("absent:" + key); context.map.put(key(60), value(60)); return replacement;
            }));
            pair.step("computeIfPresent callback effect then throws", context -> context.direction(inverse).computeIfPresent(query, (key, old) -> {
                context.mark("present:" + key + ":" + old); context.map.put(key(61), value(61)); throw new Boom();
            }));
            pair.step("compute removes current then reinstalls", context -> context.direction(inverse).compute(query, (key, old) -> {
                context.mark("compute:" + key + ":" + old); context.direction(inverse).remove(key);
                return inverse ? key(31) : value(31);
            }));
            pair.step("conditional replace mismatch", context -> context.direction(inverse).replace(query, replacement, replacement));
            pair.step("conditional replace current", context -> context.direction(inverse).replace(query,
                    context.direction(inverse).get(query), inverse ? key(32) : value(32)));
            pair.step("replace absent", context -> context.direction(inverse).replace(inverse ? value(99) : key(99), replacement));
            pair.step("conditional remove mismatch", context -> context.direction(inverse).remove(query, replacement));
            pair.step("conditional remove present", context -> context.direction(inverse).remove(query, context.direction(inverse).get(query)));
            pair.step("merge null value", context -> context.direction(inverse).merge(query, null, (old, supplied) -> supplied));
            pair.step("merge null function", context -> context.direction(inverse).merge(query, replacement, null));
            pair.step("merge callback removes", context -> context.direction(inverse).merge(missing, replacement, (old, supplied) -> {
                context.mark("merge:" + old + ":" + supplied); return null;
            }));
            Pair prefix = seeded("replaceAll callback prefix inverse=" + inverse);
            prefix.step("replaceAll exception", context -> {
                context.direction(inverse).replaceAll((key, old) -> {
                    context.mark("replace:" + key + ":" + old);
                    if (key.id == 1) throw new Boom();
                    return inverse ? key(80 + key.id) : value(80 + key.id);
                }); return null;
            });
            Pair duplicate = seeded("replaceAll duplicate inverse=" + inverse);
            duplicate.step("replaceAll duplicate rejection state", context -> {
                context.direction(inverse).replaceAll((key, old) -> {
                    context.mark("replace:" + key); return inverse ? key(1) : value(1);
                }); return null;
            });
            Pair swap = seeded("replaceAll cycle inverse=" + inverse);
            swap.step("swap requires guarded in-place donor semantics", context -> {
                context.direction(inverse).replaceAll((key, old) -> {
                    context.mark("cycle:" + key); int next = (old.id + 1) % 3;
                    return inverse ? key(next) : value(next);
                }); return null;
            });
            Pair each = seeded("forEach callbacks inverse=" + inverse);
            each.step("ordinary ordered forEach", context -> {
                context.direction(inverse).forEach((key, val) -> context.mark(key + ":" + val)); return null;
            });
            each.step("forEach exception prefix", context -> {
                context.direction(inverse).forEach((key, val) -> { context.mark(key + ":" + val); if (key.id == 1) throw new Boom(); });
                return null;
            });
            Pair structural = seeded("forEach structural callback inverse=" + inverse);
            structural.step("forEach insertion trace and failure", context -> {
                context.direction(inverse).forEach((key, val) -> {
                    context.mark(key + ":" + val); if (key.id == 0) context.map.put(key(90), value(90));
                }); return null;
            });
            Pair empty = new Pair("empty null callbacks inverse=" + inverse);
            empty.step("forEach null", context -> { context.direction(inverse).forEach(null); return null; });
            empty.step("replaceAll null", context -> { context.direction(inverse).replaceAll(null); return null; });
        }
        Pair prefix = new Pair("putAll prefix failures");
        prefix.step("iterator callback prefix", context -> { context.map.putAll(new PrefixMap()); return null; });
        prefix.step("duplicate input commits earlier mappings", context -> {
            Map<Token, Token> input = new LinkedHashMap<>();
            input.put(key(51), value(51)); input.put(key(52), value(50)); input.put(key(53), value(53));
            context.map.putAll(input); return null;
        });
        prefix.step("self putAll", context -> { context.map.putAll(context.map); return null; });
        Pair nulls = new Pair("Map default null mappings");
        nulls.step("nullable entry", context -> context.map.put(null, null));
        nulls.step("contains/get default", context -> Arrays.asList(context.map.containsKey(null),
                context.map.containsValue(null), context.map.getOrDefault(null, value(7))));
        nulls.step("computeIfAbsent replaces null value", context -> context.map.computeIfAbsent(null, key -> {
            context.mark("null-key"); return value(7);
        }));
        nulls.step("conditional null removal mismatch", context -> context.map.remove(null, null));
        nulls.step("inverse putIfAbsent null forward key", context -> context.map.inverse().putIfAbsent(value(7), key(7)));
    }

    private static Object roundTrip(Object object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(object); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return input.readObject();
        }
    }

    private static void serializationGraph(boolean candidate, boolean inverseFirst) throws Exception {
        cases++;
        BiMap<Token, Token> map = candidate ? M3HashBiMap.create() : HashBiMap.create();
        Token shared = new Token("shared", 1, 10);
        Token nullKeyValue = new Token("V", 2, 20);
        Token nullValueKey = new Token("K", 3, 30);
        map.put(shared, shared); map.put(null, nullKeyValue); map.put(nullValueKey, null);
        BiMap<Token, Token> inverse = map.inverse();
        Object[] envelope = inverseFirst
                ? new Object[] {inverse, map, map, shared, nullKeyValue, nullValueKey}
                : new Object[] {map, inverse, inverse, shared, nullKeyValue, nullValueKey};
        Object reopened = roundTrip(envelope);
        check(reopened instanceof Object[], "serialized graph envelope");
        Object[] graph = (Object[]) reopened;
        check(graph[0] instanceof BiMap<?, ?> && graph[1] instanceof BiMap<?, ?>, "serialized forward/inverse maps");
        BiMap<?, ?> forward = (BiMap<?, ?>) graph[inverseFirst ? 1 : 0];
        BiMap<?, ?> backward = (BiMap<?, ?>) graph[inverseFirst ? 0 : 1];
        check(forward.inverse() == backward && backward.inverse() == forward, "serialized inverse involution");
        check(graph[1] == graph[2], "serialized repeated graph reference");
        check(forward.get(graph[3]) == graph[3] && backward.get(graph[3]) == graph[3], "key/value payload graph alias");
        check(forward.get(null) == graph[4], "serialized null-key value representative");
        check(backward.get(null) == graph[5], "serialized null-value key representative");
        check(forward.size() == 3 && backward.size() == 3, "serialized mapping counts");
        check(new ArrayList<>(forward.keySet()).equals(Arrays.asList(shared, null, nullValueKey)), "serialized key order");
        check(new ArrayList<>(backward.keySet()).equals(Arrays.asList(shared, nullKeyValue, null)), "serialized inverse order");
        Object removed = backward.remove(graph[3]);
        check(removed == graph[3] && !forward.containsKey(graph[3]), "restored inverse mutates restored forward");
        check(map.size() == 3 && map.containsKey(shared), "restored directory detached from original");
        check(forward instanceof Serializable && backward instanceof Serializable, "serialized map markers");
    }

    private static void randomTrace() {
        Token[] canonicalKeys = new Token[48];
        Token[] canonicalValues = new Token[48];
        for (int id = 0; id < 48; id++) {
            canonicalKeys[id] = new Token("KC", id, 0);
            canonicalValues[id] = new Token("VC", id, 0);
        }
        Pair pair = new Pair("fixed-seed common canonical-object random trace", 1);
        Random random = new Random(0x6d334775617661L);
        for (int step = 0; step < 12000; step++) {
            int keyId = random.nextInt(13) == 0 ? -1 : random.nextInt(48);
            int valueId = random.nextInt(13) == 0 ? -1 : random.nextInt(48);
            int op = random.nextInt(29); boolean inverse = random.nextBoolean();
            boolean returnNull = random.nextInt(5) == 0;
            randomOperations++;
            pair.step("random " + step + " op=" + op + " inverse=" + inverse, context -> {
                Token key = keyId < 0 ? null : canonicalKeys[keyId];
                Token value = valueId < 0 ? null : canonicalValues[valueId];
                BiMap<Token, Token> map = context.direction(inverse);
                Token query = inverse ? value : key;
                Token supplied = inverse ? key : value;
                switch (op) {
                    case 0, 1 -> { return map.put(query, supplied); }
                    case 2 -> { return map.forcePut(query, supplied); }
                    case 3 -> { return map.remove(query); }
                    case 4 -> { return map.get(query); }
                    case 5 -> { return map.containsKey(query); }
                    case 6 -> { return map.containsValue(supplied); }
                    case 7 -> { return map.putIfAbsent(query, supplied); }
                    case 8 -> { return map.replace(query, supplied); }
                    case 9 -> { return map.replace(query, map.get(query), supplied); }
                    case 10 -> { return map.remove(query, supplied); }
                    case 11 -> { return map.computeIfAbsent(query, current -> {
                        context.mark("absent:" + current); return returnNull ? null : supplied;
                    }); }
                    case 12 -> { return map.computeIfPresent(query, (current, old) -> {
                        context.mark("present:" + current + ":" + old); return returnNull ? null : supplied;
                    }); }
                    case 13 -> { return map.compute(query, (current, old) -> {
                        context.mark("compute:" + current + ":" + old); return returnNull ? null : supplied;
                    }); }
                    case 14 -> { return map.merge(query, supplied, (old, next) -> {
                        context.mark("merge:" + old + ":" + next); return returnNull ? null : next;
                    }); }
                    case 15 -> { return map.keySet().remove(query); }
                    case 16 -> { return map.values().remove(supplied); }
                    case 17 -> { return map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(query, supplied)); }
                    case 18 -> { return map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(query, supplied)); }
                    case 19 -> {
                        if (map.isEmpty()) return null;
                        Map.Entry<Token, Token> entry = map.entrySet().iterator().next();
                        return entry.setValue(supplied);
                    }
                    case 20 -> {
                        if (map.isEmpty()) return null;
                        Iterator<Token> iterator = map.keySet().iterator(); Token delivered = iterator.next();
                        iterator.remove(); return delivered;
                    }
                    case 21 -> {
                        if (returnNull) map.clear();
                        return map.entrySet().toArray();
                    }
                    case 22 -> { return context.copy(context.map); }
                    case 23 -> { return map.getOrDefault(query, supplied); }
                    case 24 -> {
                        Map<Token, Token> input = new LinkedHashMap<>();
                        input.put(query, supplied); map.putAll(input); return null;
                    }
                    case 25 -> { return map.keySet().removeIf(current -> {
                        context.mark("key:" + current); return Objects.equals(current, query);
                    }); }
                    case 26 -> { return map.values().removeIf(current -> {
                        context.mark("value:" + current); return Objects.equals(current, supplied);
                    }); }
                    case 27 -> { return map.entrySet().stream()
                            .map(entry -> new FrozenEntry(freeze(entry.getKey()), freeze(entry.getValue()))).toList(); }
                    default -> {
                        map.forEach((current, val) -> context.mark(current + ":" + val)); return null;
                    }
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        runtimeVariant = args.length == 0 ? "UNSPECIFIED_PROVIDER_CLASSPATH" : args[0];
        check(args.length <= 1, "optional argument is one root-supplied actual artifact label");
        System.out.println("BEGIN M3HashBiMapCommonSelfTest runtime=" + runtimeVariant
                + " scope=canonical-objects-per-logical-id target=JRE-33.4.8 strict=true");
        forcePutPositionsAndNulls();
        denseGrowthAndDeletion();
        randomTrace();
        check(randomOperations == 12000, "exactly 12000 fixed-seed canonical-object operations");
        System.out.println("PASS M3HashBiMapCommonSelfTest checks=" + checks + " comparedOperations=" + operations
                + " cases=" + cases + " randomOperations=" + randomOperations
                + " runtime=" + runtimeVariant + " scope=canonical-objects-per-logical-id target=JRE-33.4.8 strict=true"
                + " originalWholeSelfTest=NOT_CLAIMED equalDistinctRandomObjects=NOT_EXERCISED"
                + " wireIdentity=NOT_CLAIMED performance=NOT_MEASURED");
    }
}
