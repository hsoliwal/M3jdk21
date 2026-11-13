// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.LongFunction;

/** Reduced-state Java21 oracle. NOT production M3TransientFacadeStore. */
//FLLDCIM: RELOCATE - donor blob 73ff5f3d56ca at 6a534f23a85f; recipe make/m3/collections/qualification/doop-facade-fixes-20261107/apply.py v1
public final class DuplicateAdmissionOracle {
    private DuplicateAdmissionOracle() { }
    private record Facade(long id, int generation) { }

    private static final class Store {
        LongFunction<Facade> factory;
        BiConsumer<Long, Facade> disposer;
        final TreeMap<Long, Facade> residents = new TreeMap<>();
        Facade expose(long id) {
            Facade live = residents.get(id);
            if (live != null) return live;
            Facade created = factory.apply(id);
            insertCreated(id, created);
            return created;
        }
        private void insertCreated(long id, Facade facade) {
            try {
                if (residents.containsKey(id)) throw new IllegalStateException("duplicate facade id: " + id);
                residents.put(id, facade);
            } catch (RuntimeException | Error failure) {
                boolean live = false;
                for (Facade resident : residents.values()) {
                    if (resident == facade) { live = true; break; }
                }
                if (!live) {
                    try { disposer.accept(id, facade); }
                    catch (RuntimeException | Error cleanup) {
                        if (cleanup != failure) failure.addSuppressed(cleanup);
                    }
                }
                throw failure;
            }
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (int cycle = 0; cycle < 10_000; cycle++) {
            Store store = new Store();
            int[] calls = {0};
            List<Facade> disposed = new ArrayList<>();
            store.disposer = (id, facade) -> disposed.add(facade);
            store.factory = id -> {
                int number = ++calls[0];
                if (number == 1) store.expose(id);
                return new Facade(id, number);
            };
            try { store.expose(7); throw new AssertionError("outer admitted"); }
            catch (IllegalStateException expected) {
                check("duplicate facade id: 7".equals(expected.getMessage()), "wrong failure");
            }
            check(store.residents.get(7L).generation() == 2, "nested lost");
            check(disposed.size() == 1 && disposed.get(0).generation() == 1, "outer disposal");

            Store alias = new Store();
            Facade shared = new Facade(9, 1);
            int[] entries = {0};
            List<Facade> aliasDisposed = new ArrayList<>();
            alias.disposer = (id, facade) -> aliasDisposed.add(facade);
            alias.factory = id -> {
                if (++entries[0] == 1) alias.expose(id);
                return shared;
            };
            try { alias.expose(9); throw new AssertionError("shared outer admitted"); }
            catch (IllegalStateException expected) {
                check(alias.residents.get(9L) == shared, "alias identity lost");
                check(aliasDisposed.isEmpty(), "live resident disposed");
            }

            Store failing = new Store();
            IllegalArgumentException cleanup = new IllegalArgumentException("cleanup");
            int[] invocations = {0};
            failing.factory = id -> {
                int number = ++invocations[0];
                if (number == 1) failing.expose(id);
                return new Facade(id, number);
            };
            failing.disposer = (id, facade) -> { throw cleanup; };
            try { failing.expose(11); throw new AssertionError("suppression hidden"); }
            catch (IllegalStateException expected) {
                check(expected.getSuppressed().length == 1, "missing suppressed cleanup");
                check(expected.getSuppressed()[0] == cleanup, "wrong suppressed cleanup");
            }
            Store normal = new Store();
            normal.factory = id -> new Facade(id, 1);
            normal.disposer = (id, facade) -> { throw new AssertionError("unexpected dispose"); };
            check(normal.expose(3) == normal.expose(3), "normal identity changed");
        }
        System.out.println("PASS: 10000 duplicate, alias, suppression and normal cycles");
    }
}
