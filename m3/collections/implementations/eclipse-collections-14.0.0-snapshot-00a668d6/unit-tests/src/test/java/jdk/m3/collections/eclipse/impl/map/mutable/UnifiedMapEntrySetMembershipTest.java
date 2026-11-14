/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.eclipse.impl.map.mutable;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Independent public-view and subclass-hook contracts for entry membership. */
public class UnifiedMapEntrySetMembershipTest
{
    @Test
    public void membershipContracts() throws Exception
    {
        nullsAndViews();
        callbackOrderAndFailures();
        callbackMutations();
        collisionContinuation();
        randomizedPublicView();
    }

    public static void main(String[] args) throws Exception
    {
        new UnifiedMapEntrySetMembershipTest().membershipContracts();
        System.out.println("PASS UnifiedMap entry membership contracts: nulls, views, callbacks, mutations, collisions, 20000 model steps");
    }

    private static void nullsAndViews() throws Exception
    {
        UnifiedMap<Object, Object> map = new UnifiedMap<>();
        map.put(null, null);
        map.put("null-value", null);
        map.put("value", "original");
        Set<Map.Entry<Object, Object>> view = map.entrySet();
        check(view.contains(entry(null, null)), "null key and value");
        check(view.contains(entry("null-value", null)), "null value");
        check(!view.contains(entry("missing", null)), "absent null value");
        check(!view.contains(entry(null, "wrong")), "wrong null-key value");
        check(!view.contains(null) && !view.contains("not an entry"), "non-entry membership");
        check(view.containsAll(Arrays.asList(entry(null, null), entry("value", "original"))), "containsAll");
        check(view.equals(new HashSet<>(view)) && new HashSet<>(view).equals(view), "set equality");
        byte[] before = serialize(map);
        for (int i = 0; i < 100; i++)
        {
            check(view.contains(entry("value", "original")), "repeat membership");
        }
        check(Arrays.equals(before, serialize(map)), "membership preserves serialized map state");
        map.put("later", 9);
        check(view.contains(entry("later", 9)), "view remains backed");
        Map.Entry<Object, Object> retained = view.stream().filter(e -> "value".equals(e.getKey())).findFirst().orElseThrow();
        map.put("value", "live");
        check(Objects.equals(retained.getValue(), "original"), "entry preserves its captured value");
        check(Objects.equals(retained.setValue("updated"), "live"), "entry setValue uses current owner");
        check(Objects.equals(map.get("value"), "updated"), "entry updates owner");
        map.remove("value");
        check(retained.setValue("detached") == null && !map.containsKey("value"), "removed entry does not reinsert");
        check(view.retainAll(List.of(entry(null, null), entry("later", 9))), "retainAll still uses entry projection");
        check(map.size() == 2 && view.contains(entry(null, null)), "retainAll result");
    }

    private static void callbackOrderAndFailures()
    {
        for (boolean chain : new boolean[]{false, true})
        {
            Scenario success = new Scenario(chain);
            success.hooks.put("query.equals", () -> { throw new AssertionError("wrong key equality direction"); });
            success.hooks.put("storedValue.equals", () -> { throw new AssertionError("wrong value equality direction"); });
            check(success.probe(), "equal distinct entry");
            equal(success.events, expected(chain), "callback order");
            for (String stage : expected(chain))
            {
                Scenario failed = new Scenario(chain);
                RuntimeException marker = new RuntimeException(stage);
                failed.hooks.put(stage, () -> { throw marker; });
                try
                {
                    failed.probe();
                    throw new AssertionError("missing exception at " + stage);
                }
                catch (RuntimeException actual)
                {
                    check(actual == marker, "exact callback exception identity");
                }
                List<String> trace = expected(chain);
                equal(failed.events, trace.subList(0, trace.indexOf(stage) + 1), "abrupt completion at " + stage);
            }
            Scenario direct = new Scenario(chain);
            check(direct.map.direct(direct.query), "protected EntrySet public containsEntry");
            equal(direct.events, expected(chain), "direct method order");
            Scenario shortCircuit = new Scenario(chain);
            check(!shortCircuit.map.entrySet().containsAll(Arrays.asList("not an entry", shortCircuit.query)), "containsAll stops at first miss");
            check(shortCircuit.events.isEmpty(), "containsAll does not read later entry");
        }
    }

    private static void callbackMutations()
    {
        for (boolean chain : new boolean[]{false, true})
        {
            for (String stage : List.of("entry.key", "entry.value", "index", "query.hash"))
            {
                Scenario cleared = new Scenario(chain);
                cleared.hooks.put(stage, cleared.map::clear);
                check(!cleared.probe(), "clear before table read at " + stage);
                equal(cleared.events, List.of("entry.key", "entry.value", "index", "query.hash"), "early clear order");
            }
            for (String stage : List.of("stored.equals", "queryValue.equals"))
            {
                Scenario cleared = new Scenario(chain);
                cleared.hooks.put(stage, cleared.map::clear);
                check(cleared.probe() && cleared.map.isEmpty(), "captured lookup survives clear at " + stage);
                equal(cleared.events, expected(chain), "late clear order");
            }
            Scenario overwritten = new Scenario(chain);
            overwritten.hooks.put("stored.equals", () -> overwritten.map.replaceStoredValue(overwritten.replacement));
            check(overwritten.probe() != chain, "flat value snapshot precedes key equality; chain value follows it");
            equal(overwritten.events, expected(chain), "value replacement order");
            Scenario replacedOwner = new Scenario(chain);
            replacedOwner.hooks.put("stored.equals", replacedOwner.map::replaceOwnerTable);
            check(replacedOwner.probe(), "probe keeps captured slot or chain when owner table changes");
            equal(replacedOwner.events, expected(chain), "owner replacement order");
            Scenario getterChange = new Scenario(chain);
            getterChange.hooks.put("entry.key", () -> getterChange.query.value = getterChange.replacement);
            check(!getterChange.probe(), "getValue reads changes made by getKey");
            List<String> changedTrace = new ArrayList<>(expected(chain));
            changedTrace.set(changedTrace.size() - 1, "replacement.equals");
            equal(getterChange.events, changedTrace, "getter mutation order");
        }
    }

    private static void collisionContinuation()
    {
        Scenario duplicate = new Scenario(false);
        duplicate.map.duplicateChain(duplicate.stored, duplicate.replacement, duplicate.storedValue);
        check(duplicate.probe(), "scan continues after equal key with unequal value");
        equal(duplicate.events, List.of("entry.key", "entry.value", "index", "query.hash", "stored.equals", "queryValue.equals", "stored.equals", "queryValue.equals"), "collision continuation trace");
        Scenario terminated = new Scenario(false);
        terminated.map.terminatedChain(terminated.stored, terminated.storedValue);
        check(!terminated.probe(), "null collision slot terminates search");
        equal(terminated.events, List.of("entry.key", "entry.value", "index", "query.hash"), "termination callback order");
    }

    private static void randomizedPublicView()
    {
        UnifiedMap<Object, Object> actual = new UnifiedMap<>();
        Map<Object, Object> expected = new HashMap<>();
        Random random = new Random(0x4d334543L);
        for (int step = 0; step < 20_000; step++)
        {
            Object key = random.nextInt(13) == 0 ? null : new CollisionKey(random.nextInt(128));
            Object value = random.nextInt(5) == 0 ? null : random.nextInt(24);
            switch (random.nextInt(7))
            {
                case 0, 1 -> equal(actual.put(key, value), expected.put(key, value), "put result");
                case 2 -> equal(actual.remove(key), expected.remove(key), "remove result");
                case 3 -> equal(actual.entrySet().remove(entry(key, value)), expected.entrySet().remove(entry(key, value)), "view remove result");
                case 4 -> {
                    if (step % 71 == 0)
                    {
                        actual.clear();
                        expected.clear();
                    }
                }
                default -> { }
            }
            Map.Entry<Object, Object> query = entry(key, value);
            equal(actual.entrySet().contains(query), expected.entrySet().contains(query), "model membership at " + step);
            check(actual.size() == expected.size(), "model size");
            if (step % 97 == 0)
            {
                check(actual.entrySet().equals(expected.entrySet()), "model set equality");
                check(actual.entrySet().containsAll(expected.entrySet()), "model containsAll");
            }
        }
    }

    private static List<String> expected(boolean chain)
    {
        return chain
                ? List.of("entry.key", "entry.value", "index", "query.hash", "other.equals", "stored.equals", "queryValue.equals")
                : List.of("entry.key", "entry.value", "index", "query.hash", "stored.equals", "queryValue.equals");
    }

    private static Map.Entry<Object, Object> entry(Object key, Object value)
    {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    private static byte[] serialize(Object value) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes))
        {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static void check(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }

    private static void equal(Object actual, Object expected, String message)
    {
        check(Objects.equals(actual, expected), message + ": expected " + expected + ", actual " + actual);
    }

    private record CollisionKey(int id)
    {
        @Override
        public int hashCode()
        {
            return this.id & 3;
        }
    }

    private static final class Scenario
    {
        final List<String> events = new ArrayList<>();
        final Map<String, Runnable> hooks = new HashMap<>();
        final ProbeMap map = new ProbeMap(this);
        final Token stored = new Token(this, "stored", 1);
        final Token storedValue = new Token(this, "storedValue", 7);
        final Token replacement = new Token(this, "replacement", 8);
        final ProbeEntry query = new ProbeEntry(this, new Token(this, "query", 1), new Token(this, "queryValue", 7));
        boolean active;

        Scenario(boolean chain)
        {
            if (chain)
            {
                this.map.put(new Token(this, "other", 2), "other value");
            }
            this.map.put(this.stored, this.storedValue);
            this.active = true;
        }

        void event(String name)
        {
            if (this.active)
            {
                this.events.add(name);
                Runnable action = this.hooks.remove(name);
                if (action != null)
                {
                    action.run();
                }
            }
        }

        boolean probe()
        {
            return this.map.entrySet().contains(this.query);
        }
    }

    private static final class Token
    {
        final Scenario scenario;
        final String name;
        final int id;

        Token(Scenario scenario, String name, int id)
        {
            this.scenario = scenario;
            this.name = name;
            this.id = id;
        }

        @Override
        public int hashCode()
        {
            this.scenario.event(this.name + ".hash");
            return 0;
        }

        @Override
        public boolean equals(Object other)
        {
            this.scenario.event(this.name + ".equals");
            return other instanceof Token token && this.id == token.id;
        }
    }

    private static final class ProbeEntry implements Map.Entry<Object, Object>
    {
        final Scenario scenario;
        final Object key;
        Object value;

        ProbeEntry(Scenario scenario, Object key, Object value)
        {
            this.scenario = scenario;
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey()
        {
            this.scenario.event("entry.key");
            return this.key;
        }

        @Override
        public Object getValue()
        {
            this.scenario.event("entry.value");
            return this.value;
        }

        @Override
        public Object setValue(Object value)
        {
            throw new UnsupportedOperationException();
        }
    }

    private static final class ProbeMap extends UnifiedMap<Object, Object>
    {
        final Scenario scenario;

        ProbeMap(Scenario scenario)
        {
            this.scenario = scenario;
        }

        @Override
        protected int index(Object key)
        {
            this.scenario.event("index");
            return super.index(key);
        }

        boolean direct(Map.Entry<?, ?> entry)
        {
            return ((EntrySet) this.entrySet()).containsEntry(entry);
        }

        void replaceStoredValue(Object value)
        {
            if (this.table[0] == CHAINED_KEY)
            {
                ((Object[]) this.table[1])[3] = value;
            }
            else
            {
                this.table[1] = value;
            }
        }

        void replaceOwnerTable()
        {
            this.table = new Object[this.table.length];
            this.occupied = 0;
        }

        void duplicateChain(Object key, Object first, Object second)
        {
            this.table[0] = CHAINED_KEY;
            this.table[1] = new Object[]{key, first, key, second};
            this.occupied = 2;
        }

        void terminatedChain(Object key, Object value)
        {
            this.table[0] = CHAINED_KEY;
            this.table[1] = new Object[]{null, null, key, value};
        }
    }
}
