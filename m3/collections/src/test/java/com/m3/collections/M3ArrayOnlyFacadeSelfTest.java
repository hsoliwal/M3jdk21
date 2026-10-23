/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Dependency-free real-source Java 21 verification for the array-only facade cache. */
public final class M3ArrayOnlyFacadeSelfTest {
    private static long checks;
    private M3ArrayOnlyFacadeSelfTest() { }

    private static void ok(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " at check " + checks);
    }

    private static void reject(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("expected " + kind, problem);
        }
        throw new AssertionError("expected " + kind);
    }

    private static final class Oracle {
        private static final class Info {
            String value;
            int hits = 1;
            long tick;
            Info(String value, long tick) { this.value = value; this.tick = tick; }
        }
        private final Map<Long, Info> cache = new HashMap<>();
        int maximum;
        long clock;
        long hits;
        long misses;
        long evictions;
        Oracle(int maximum) { this.maximum = maximum; }
        String peek(long key) {
            Info value = cache.get(key);
            return value == null ? null : value.value;
        }
        String get(long key) {
            Info value = cache.get(key);
            if (value == null) { misses++; return null; }
            hits++;
            if (value.hits < Integer.MAX_VALUE) value.hits++;
            value.tick = ++clock;
            return value.value;
        }
        String put(long key, String value) {
            if (maximum == 0) return null;
            Info existing = cache.get(key);
            if (existing != null) {
                String previous = existing.value;
                existing.value = value;
                if (existing.hits < Integer.MAX_VALUE) existing.hits++;
                existing.tick = ++clock;
                return previous;
            }
            if (cache.size() >= maximum) evict();
            cache.put(key, new Info(value, ++clock));
            return null;
        }
        String remove(long key) {
            Info previous = cache.remove(key);
            return previous == null ? null : previous.value;
        }
        void setMaximum(int maximum) {
            this.maximum = maximum;
            while (cache.size() > maximum) evict();
        }
        void evict() {
            Long victim = null;
            for (var entry : cache.entrySet()) {
                if (victim == null) { victim = entry.getKey(); continue; }
                Info current = entry.getValue(), selected = cache.get(victim);
                if (current.hits < selected.hits
                        || current.hits == selected.hits && current.tick < selected.tick) {
                    victim = entry.getKey();
                }
            }
            if (victim == null) throw new AssertionError("oracle empty eviction");
            cache.remove(victim);
            evictions++;
        }
        void clear() { cache.clear(); clock = 0; }
    }

    private static void arrayOnlyStorage() {
        for (Field field : M3FacadeLFUCache.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            ok(type.isPrimitive() || type.isArray(), "LFU owner must use scalar or array: " + field);
            if (type.isArray()) {
                ok(type.getComponentType().isPrimitive() || type.getComponentType() == Object.class,
                        "no Node or Entry[] lane: " + field);
            }
        }
        for (Field field : M3ApiEntryFacades.class.getDeclaredFields()) {
            ok(field.getType() == M3FacadeLFUCache.class, "API facades have one optional LFU owner");
        }
    }

    private static void snapshotContracts() {
        var f = new M3ApiEntryFacades<String, Object>(2);
        String key = new String("slot");
        Object value = new Object();
        var old = f.snapshot(0x0000000100000007L, key, value);
        ok(old == f.snapshot(0x0000000100000007L, key, value), "reuse same immutable facade");
        reject(UnsupportedOperationException.class, () -> old.setValue(new Object()));
        Object replacement = new Object();
        var next = f.snapshot(0x0000000100000007L, key, replacement);
        ok(old != next && old.getValue() == value && next.getValue() == replacement,
                "never repoint already handed-out entry");
        var generation = f.snapshot(0x0000000200000007L, key, replacement);
        ok(next != generation && f.cachedFacades() == 2, "generation-safe facade cache");
        f.snapshot(0x0000000300000007L, key, replacement);
        ok(f.cachedFacades() == 2 && f.cacheEvictions() == 1, "bounded LFU facade retention");
        f.invalidateAll();
        f.trimToLiveFacades();
        ok(f.cachedFacades() == 0, "only derived facades cleared");
        f.verifyInvariants();
    }

    private static void randomizedOracle() {
        Random random = new Random(0x7557be3aL);
        for (int pass = 0; pass < 6; pass++) {
            int capacity = pass + 1;
            var m3 = new M3FacadeLFUCache<String>(capacity);
            var oracle = new Oracle(capacity);
            for (int step = 0; step < 3000; step++) {
                long key = switch (random.nextInt(24)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(37) - 18L;
                };
                switch (random.nextInt(9)) {
                    case 0, 1, 2 -> {
                        String value = "v" + random.nextInt(80);
                        ok(Objects.equals(m3.put(key, value), oracle.put(key, value)), "put return");
                    }
                    case 3, 4, 5 -> ok(Objects.equals(m3.get(key), oracle.get(key)), "get value");
                    case 6 -> ok(Objects.equals(m3.invalidate(key), oracle.remove(key)), "remove value");
                    case 7 -> {
                        int target = random.nextInt(10);
                        m3.setMaximumEntries(target);
                        oracle.setMaximum(target);
                    }
                    default -> { if ((step & 63) == 0) { m3.clear(); oracle.clear(); }
                                 else m3.compactStorage(); }
                }
                ok(m3.size() == oracle.cache.size(), "size");
                ok(m3.hits() == oracle.hits && m3.misses() == oracle.misses, "cache counters");
                ok(m3.evictions() == oracle.evictions, "LFU eviction counter");
                ok(m3.maximumEntries() == oracle.maximum, "capacity");
                for (long x = -18; x <= 18; x++) {
                    ok(Objects.equals(m3.peek(x), oracle.peek(x)), "membership and value");
                }
                ok(Objects.equals(m3.peek(Long.MIN_VALUE), oracle.peek(Long.MIN_VALUE)), "min handle");
                ok(Objects.equals(m3.peek(Long.MAX_VALUE), oracle.peek(Long.MAX_VALUE)), "max handle");
                if ((step & 127) == 0) m3.verifyInvariants();
            }
            m3.verifyInvariants();
        }
    }

    public static void main(String[] args) {
        arrayOnlyStorage();
        snapshotContracts();
        randomizedOracle();
        var cache = new M3FacadeLFUCache<String>(0);
        ok("transient".equals(cache.getOrCreate(1L, h -> "transient")), "zero-capacity factory");
        ok(cache.size() == 0, "no retained facade at zero capacity");
        reject(IllegalStateException.class, () -> cache.getOrCreate(2L, h -> {
            cache.put(3L, "must refuse callback reentrancy");
            return "unreachable";
        }));
        reject(IllegalArgumentException.class, () -> cache.setMaximumEntries(-1));
        reject(NullPointerException.class, () -> cache.put(7L, null));
        System.out.println("PASS M3ArrayOnlyFacadeSelfTest checks=" + checks);
    }
}
