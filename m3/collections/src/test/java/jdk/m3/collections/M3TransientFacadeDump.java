// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * Differential dump of the transient facade residency family (recipe transient-facade-20261106, M3JDK-authored).
 *
 * <p>Seeded facade lifecycles over the public and package-private API of M3TransientFacadeStore,
 * M3InternalLeaseRecycler and M3ResidentTrimPlanner: create/expose/internal lease/return/trim/remove/close,
 * reentrant close from every callback, post-close calls, injected heap pressure, top-k victim plans and
 * fault-injected backing growth. Every observable outcome (values, identities, callback order, exceptions with
 * their suppressed chains, stats) is digested per scenario family. differential.sh compiles this same source in
 * the donor package; both outputs must be byte-identical. No threads, timing or real heap sampling.</p>
 *
 * <p>usage: M3TransientFacadeDump [ops-per-family=100000] [seed=20261106] (-Dm3.facade.dump.events=true echoes events)</p>
 */
public final class M3TransientFacadeDump {
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final String PACKAGE = M3TransientFacadeDump.class.getPackageName();
    private static final long MAX_HEAP = 1_000_000_000L;
    private static final long[] USED = {100_000_000L, 680_000_000L, 830_000_000L}; // NORMAL, WARM, HIGH
    /** -Dm3.facade.dump.events=true also prints every event (to locate a digest difference). */
    private static final boolean ECHO = Boolean.getBoolean("m3.facade.dump.events");

    private M3TransientFacadeDump() { }

    /** A facade with a creation serial; identity is observed through ==, never through equals. */
    static final class Facade {
        long id;
        final int serial;
        boolean scrubbed;
        Facade(long id, int serial) { this.id = id; this.serial = serial; }
        @Override public String toString() { return "F" + id + "#" + serial + (scrubbed ? "s" : ""); }
    }

    /** Mutable reference for callbacks that reach their own owner. */
    static final class Box<S> { S value; }

    /** One family's digested event stream. */
    static final class Log {
        private final MessageDigest digest;
        long events;
        Log() {
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        }
        void add(String event) {
            digest.update(event.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            events++;
            if (ECHO) System.out.println(event);
        }
        String sha256() { return HexFormat.of().formatHex(digest.digest()); }
    }

    @FunctionalInterface interface Op { Object run() throws Exception; }

    public static void main(String[] args) {
        int ops = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 20261106L;
        String[] families = {"lifecycle-recreate", "lifecycle-retain", "recycle", "reentrant-close",
            "pressure", "topk", "recycler", "growth"};
        for (int f = 0; f < families.length; f++) {
            long familySeed = seed + GOLDEN * (f + 1);
            Log log = new Log();
            run(families[f], log, new SplittableRandom(familySeed), ops);
            System.out.println("family=" + families[f] + " seed=" + familySeed + " ops=" + ops
                    + " events=" + log.events + " sha256=" + log.sha256());
        }
        System.out.println("M3TransientFacadeDump families=" + families.length + " ops=" + ops + " seed=" + seed);
    }

    private static void run(String family, Log log, SplittableRandom r, int ops) {
        int defaults = M3TransientFacadeStore.DEFAULT_MODE;
        int retain = M3TransientFacadeStore.RETAIN_UNEXPOSED | M3TransientFacadeStore.PIN_EXPOSED
                | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        switch (family) {
            case "lifecycle-recreate" -> lifecycle(log, r, ops, defaults);
            case "lifecycle-retain" -> lifecycle(log, r, ops, retain);
            case "recycle" -> recycle(log, r, ops);
            case "reentrant-close" -> reentrant(log, r, ops);
            case "pressure" -> pressure(log, r, ops);
            case "topk" -> topk(log, r, ops);
            case "recycler" -> recycler(log, r, ops);
            case "growth" -> growth(log, r, ops);
            default -> throw new IllegalArgumentException(family);
        }
    }

    static void call(Log log, String name, Op op) {
        try {
            log.add(name + "=" + op.run());
        } catch (Exception | Error failure) {
            log.add(name + "!" + describe(failure));
        }
    }

    /**
     * Class, message and suppressed chain. The message of an implicit VM exception is not part of the
     * contract: C2 may rethrow a preallocated instance without a message (OmitStackTraceInFastThrow), so
     * -Xint and JIT runs would differ. Such messages (and helpful-NPE text) are recorded as {@code <implicit>}.
     */
    static String describe(Throwable failure) {
        String message = String.valueOf(failure.getMessage());
        boolean implicit = failure instanceof ArrayIndexOutOfBoundsException
                || failure instanceof ArithmeticException && !message.endsWith(" overflow")
                || failure instanceof ClassCastException || failure instanceof ArrayStoreException
                || failure instanceof NegativeArraySizeException
                || failure instanceof NullPointerException && (failure.getMessage() == null || message.startsWith("Cannot "));
        StringBuilder text = new StringBuilder(failure.getClass().getSimpleName()).append(':')
                .append(implicit ? "<implicit>" : message.replace(PACKAGE, "P"));
        for (Throwable suppressed : failure.getSuppressed()) text.append(" +").append(describe(suppressed));
        return text.toString();
    }

    private static M3TransientFacadeStore.Factory<Facade> factory(Log log, int[] serial) {
        return id -> {
            Facade made = new Facade(id, ++serial[0]);
            log.add("create " + made);
            return made;
        };
    }

    private static M3TransientFacadeStore.Disposer<Facade> disposer(Log log) {
        return (id, facade) -> log.add("dispose " + id + " " + facade);
    }

    private static void lifecycle(Log log, SplittableRandom r, int ops, int mode) {
        int[] serial = {0};
        M3TransientFacadeStore<Facade> store = null;
        Map<Long, Facade> exposed = new HashMap<>();
        for (int i = 0; i < ops; i++) {
            if (i % 25_000 == 0) {
                if (store != null) closeAndProbe(log, store);
                exposed.clear();
                store = new M3TransientFacadeStore<>(factory(log, serial), disposer(log), mode);
            }
            lifecycleStep(log, r, store, exposed, r.nextInt(384));
        }
        if (store != null) closeAndProbe(log, store);
    }

    private static void closeAndProbe(Log log, M3TransientFacadeStore<Facade> store) {
        call(log, "close", () -> { store.close(); return "ok"; });
        postClose(log, store);
    }

    private static void lifecycleStep(Log log, SplittableRandom r, M3TransientFacadeStore<Facade> store,
            Map<Long, Facade> exposed, long id) {
        switch (r.nextInt(12)) {
            case 0, 1 -> call(log, "expose", () -> {
                Facade facade = store.expose(id);
                Facade before = exposed.put(id, facade);
                return facade + (before == facade ? " same" : before == null ? " first" : " new");
            });
            case 2, 3, 4 -> call(log, "internal", () -> {
                Facade[] seen = new Facade[1];
                store.withInternal(id, facade -> seen[0] = facade);
                return seen[0] + (seen[0] == exposed.get(id) ? " exposed" : "");
            });
            case 5 -> call(log, "peek", () -> store.peekResident(id));
            case 6 -> call(log, "flags", () -> store.residencyFlags(id) + "/" + store.isResident(id) + "/" + store.isPinned(id));
            case 7 -> call(log, "remove", () -> { exposed.remove(id); return store.remove(id); });
            case 8 -> call(log, "trim", () -> store.releaseOptionalResidency(r.nextInt(96) - 1));
            case 9 -> call(log, "counts", () -> store.residentCount() + "/" + store.pinnedCount() + "/" + store.mode());
            case 10 -> call(log, "negative", () -> store.expose(-1 - id));
            default -> call(log, "stats", store::stats);
        }
    }

    static void postClose(Log log, M3TransientFacadeStore<Facade> store) {
        call(log, "closed.expose", () -> store.expose(1));
        call(log, "closed.internal", () -> { store.withInternal(1, facade -> { }); return "ran"; });
        call(log, "closed.peek", () -> store.peekResident(1));
        call(log, "closed.flags", () -> store.residencyFlags(1));
        call(log, "closed.trim", () -> store.releaseOptionalResidency(0));
        call(log, "closed.idle", store::reusableIdleCount);
        call(log, "closed.reuse", store::reuseCount);
        call(log, "closed.release", () -> store.releaseReusable(0));
        call(log, "closed.remove", () -> store.remove(1));
        call(log, "closed.stats", store::stats);
        call(log, "closed.close", () -> { store.close(); return "ok"; });
    }

    /** Fault plan for recycler callbacks: 0 none, 1 scrub throws, 2 rebind throws, 3 action throws. */
    private static M3TransientFacadeStore.Recycler<Facade> recyclerOf(Log log, int[] fault) {
        return new M3TransientFacadeStore.Recycler<>() {
            @Override public void scrub(Facade facade) {
                if (fault[0] == 1) throw new IllegalStateException("scrub fault");
                facade.scrubbed = true;
                log.add("scrub " + facade);
            }
            @Override public void rebind(Facade facade, long logicalId) {
                if (fault[0] == 2) throw new IllegalStateException("rebind fault");
                log.add("rebind " + facade + "->" + logicalId);
                facade.id = logicalId;
                facade.scrubbed = false;
            }
        };
    }

    private static void recycle(Log log, SplittableRandom r, int ops) {
        int[] serial = {0};
        int[] fault = {0};
        int mode = M3TransientFacadeStore.DEFAULT_MODE;
        M3TransientFacadeStore<Facade> store = null;
        for (int i = 0; i < ops; i++) {
            if (i % 5_000 == 0) {
                if (store != null) { M3TransientFacadeStore<Facade> old = store; call(log, "close", () -> { old.close(); return "ok"; }); postClose(log, old); }
                int maxIdle = 1 + r.nextInt(16);
                log.add("store maxIdle=" + maxIdle);
                store = new M3TransientFacadeStore<>(factory(log, serial), disposer(log), mode, recyclerOf(log, fault), maxIdle);
            }
            fault[0] = r.nextInt(16) == 0 ? 1 + r.nextInt(3) : 0;
            recycleStep(log, r, store, fault[0] == 3, r.nextInt(256));
        }
        M3TransientFacadeStore<Facade> last = store;
        call(log, "close", () -> { last.close(); return "ok"; });
        postClose(log, last);
    }

    private static void recycleStep(Log log, SplittableRandom r, M3TransientFacadeStore<Facade> store,
            boolean actionFails, long id) {
        switch (r.nextInt(9)) {
            case 0, 1, 2, 3 -> call(log, "internal", () -> {
                store.withInternal(id, facade -> {
                    log.add("use " + facade);
                    if (actionFails) throw new IllegalArgumentException("action fault");
                });
                return "ok";
            });
            case 4 -> call(log, "expose", () -> store.expose(id));
            case 5 -> call(log, "release", () -> store.releaseReusable(r.nextInt(5)));
            case 6 -> call(log, "idle", () -> store.reusableIdleCount() + "/" + store.reuseCount());
            case 7 -> call(log, "remove", () -> store.remove(id));
            default -> call(log, "stats", store::stats);
        }
    }

    /** Callback kinds that close the owner reentrantly: factory, rebind, scrub, disposer, action. */
    private static void reentrant(Log log, SplittableRandom r, int ops) {
        int done = 0;
        while (done < ops) {
            int where = r.nextInt(5);
            int[] countdown = {1 + r.nextInt(4)};
            int[] serial = {0};
            Box<M3TransientFacadeStore<Facade>> box = new Box<>();
            Runnable maybeClose = () -> { if (--countdown[0] == 0) { log.add("reentrant close"); box.value.close(); } };
            M3TransientFacadeStore.Factory<Facade> factory = id -> {
                if (where == 0) maybeClose.run();
                return new Facade(id, ++serial[0]);
            };
            M3TransientFacadeStore.Disposer<Facade> disposer = (id, facade) -> {
                log.add("dispose " + id + " " + facade);
                if (where == 3) maybeClose.run();
            };
            M3TransientFacadeStore.Recycler<Facade> recycler = new M3TransientFacadeStore.Recycler<>() {
                @Override public void scrub(Facade facade) { if (where == 2) maybeClose.run(); facade.scrubbed = true; }
                @Override public void rebind(Facade facade, long logicalId) { if (where == 1) maybeClose.run(); facade.id = logicalId; }
            };
            box.value = new M3TransientFacadeStore<>(factory, disposer, M3TransientFacadeStore.DEFAULT_MODE, recycler, 4);
            log.add("owner where=" + where + " countdown=" + countdown[0]);
            for (int step = 0; step < 12 && done < ops; step++, done++) {
                long id = r.nextInt(8);
                M3TransientFacadeStore<Facade> store = box.value;
                switch (r.nextInt(4)) {
                    case 0, 1 -> call(log, "internal", () -> {
                        store.withInternal(id, facade -> { if (where == 4) maybeClose.run(); log.add("use " + facade); });
                        return "ok";
                    });
                    case 2 -> call(log, "expose", () -> store.expose(id));
                    default -> call(log, "trim", () -> store.releaseOptionalResidency(r.nextInt(2)) + "/" + store.releaseReusable(0));
                }
            }
            closeAndProbe(log, box.value);
        }
    }

    private static M3RuntimeHeap.Snapshot heap(int level) {
        return new M3RuntimeHeap.Snapshot(MAX_HEAP, MAX_HEAP, USED[level]);
    }

    private static void pressure(Log log, SplittableRandom r, int ops) {
        int[] serial = {0};
        int[] level = {0};
        int[] fault = {0};
        Supplier<M3RuntimeHeap.Snapshot> samples = () -> { log.add("sample " + level[0]); return heap(level[0]); };
        int retain = M3TransientFacadeStore.RETAIN_UNEXPOSED | M3TransientFacadeStore.PIN_EXPOSED
                | M3TransientFacadeStore.PRESSURE_TRIMMABLE | M3TransientFacadeStore.AUTO_HEAP_PRESSURE;
        int recreate = M3TransientFacadeStore.DEFAULT_MODE | M3TransientFacadeStore.AUTO_HEAP_PRESSURE;
        M3TransientFacadeStore<Facade> kept = new M3TransientFacadeStore<>(factory(log, serial), disposer(log), retain, samples, null, 0);
        M3TransientFacadeStore<Facade> pooled = new M3TransientFacadeStore<>(factory(log, serial), disposer(log), recreate, samples, recyclerOf(log, fault), 32);
        for (int i = 0; i < ops; i++) {
            if (r.nextInt(512) == 0) level[0] = r.nextInt(3);
            M3TransientFacadeStore<Facade> store = (i & 1) == 0 ? kept : pooled;
            long id = r.nextInt(2048);
            switch (r.nextInt(8)) {
                case 0, 1, 2, 3, 4 -> call(log, "internal", () -> { store.withInternal(id, facade -> { }); return store.residentCount() + "/" + store.reusableIdleCount(); });
                case 5 -> call(log, "expose", () -> store.expose(id));
                case 6 -> call(log, "trim", () -> store.releaseOptionalResidency(r.nextInt(64)));
                default -> call(log, "stats", store::stats);
            }
        }
        call(log, "close", () -> { kept.close(); pooled.close(); return "ok"; });
        postClose(log, kept);
        postClose(log, pooled);
    }

    private static void topk(Log log, SplittableRandom r, int ops) {
        for (int i = 0; i < ops; i++) {
            int capacity = r.nextInt(300);
            int count = r.nextInt(16) == 0 ? capacity + 1 : (capacity == 0 ? 0 : r.nextInt(capacity + 1));
            short[] flags = new short[capacity];
            long[] lastUse = new long[capacity];
            int pinnedOdds = 1 + r.nextInt(8);
            int epochs = 1 + r.nextInt(capacity + 2);
            for (int slot = 0; slot < capacity; slot++) {
                if (r.nextInt(pinnedOdds) == 0) flags[slot] = M3LazyElementFlags.PINNED;
                if (r.nextInt(5) == 0) flags[slot] |= M3LazyElementFlags.MATERIALIZED;
                lastUse[slot] = r.nextInt(16) == 0 ? Long.MIN_VALUE + r.nextInt(3) : r.nextInt(epochs);
            }
            int requested = r.nextInt(32) == 0 ? -1 : r.nextInt(capacity + 3);
            call(log, "plan", () -> java.util.Arrays.toString(M3ResidentTrimPlanner.oldestSlots(flags, lastUse, count, requested)));
        }
    }

    /** Fault plan: 0 none, 1 factory throws, 2 rebind throws, 3 reset throws, 4 action throws, 5 rebind closes. */
    private static void recycler(Log log, SplittableRandom r, int ops) {
        int[] serial = {0};
        int[] fault = {0};
        Box<M3InternalLeaseRecycler<Facade>> box = new Box<>();
        for (int i = 0; i < ops; i++) {
            if (box.value == null || i % 2_000 == 0) {
                if (box.value != null) recyclerClose(log, box.value);
                int maxIdle = r.nextInt(9);
                log.add("recycler maxIdle=" + maxIdle);
                box.value = new M3InternalLeaseRecycler<>(
                        () -> { if (fault[0] == 1) throw new IllegalStateException("factory fault"); return new Facade(-1, ++serial[0]); },
                        (facade, id) -> {
                            if (fault[0] == 2) throw new IllegalStateException("rebind fault");
                            if (fault[0] == 5) box.value.close();
                            facade.id = id;
                        },
                        facade -> { if (fault[0] == 3) throw new IllegalStateException("reset fault"); facade.id = -1; },
                        maxIdle);
            }
            fault[0] = r.nextInt(12) == 0 ? 1 + r.nextInt(5) : 0;
            recyclerStep(log, r, box.value, fault[0] == 4, r.nextInt(4096));
            if (fault[0] == 5) { recyclerClose(log, box.value); box.value = null; }
        }
        if (box.value != null) recyclerClose(log, box.value);
    }

    private static void recyclerStep(Log log, SplittableRandom r, M3InternalLeaseRecycler<Facade> pool,
            boolean actionFails, long id) {
        switch (r.nextInt(10)) {
            case 0, 1, 2, 3, 4 -> call(log, "borrow", () -> {
                Facade[] seen = new Facade[1];
                pool.withBorrowed(id, facade -> {
                    seen[0] = facade;
                    if (actionFails) throw new IllegalArgumentException("action fault");
                });
                return seen[0];
            });
            case 5 -> call(log, "nested", () -> {
                boolean[] alias = new boolean[1];
                pool.withBorrowed(id, outer -> pool.withBorrowed(id + 1, inner -> alias[0] = outer == inner));
                return alias[0];
            });
            case 6 -> call(log, "trim", () -> pool.trimIdle(r.nextInt(4) - (r.nextInt(16) == 0 ? 5 : 0)));
            case 7 -> call(log, "pressure", () -> pool.onPressure(M3RuntimeHeap.Pressure.values()[r.nextInt(3)]));
            case 8 -> call(log, "negative", () -> { pool.withBorrowed(-1 - id, facade -> { }); return "ran"; });
            default -> call(log, "stats", pool::stats);
        }
    }

    private static void recyclerClose(Log log, M3InternalLeaseRecycler<Facade> pool) {
        call(log, "recycler.close", () -> { pool.close(); return pool.stats(); });
        call(log, "recycler.closed.borrow", () -> { pool.withBorrowed(1, facade -> { }); return "ran"; });
        call(log, "recycler.closed.trim", () -> pool.trimIdle(0));
    }

    /** Fault-injected four-lane growth: an allocator throws, returns null or a short lane at a seeded call. */
    private static void growth(Log log, SplittableRandom r, int ops) {
        int[] serial = {0};
        int retain = M3TransientFacadeStore.RETAIN_UNEXPOSED | M3TransientFacadeStore.PIN_EXPOSED
                | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        M3TransientFacadeStore<Facade> store = new M3TransientFacadeStore<>(factory(log, serial), disposer(log), retain);
        int[] plan = {0, 0};
        M3TransientFacadeStore.BackingCopier copies = new M3TransientFacadeStore.BackingCopier() {
            private void fault(int lane) {
                if (plan[0] == lane && plan[1] == 1) throw new OutOfMemoryError("injected lane " + lane);
            }
            @Override public long[] longs(long[] source, int capacity) {
                fault(1);
                return plan[0] == 1 && plan[1] == 2 ? null : java.util.Arrays.copyOf(source, plan[0] == 1 && plan[1] == 3 ? capacity - 1 : capacity);
            }
            @Override public short[] shorts(short[] source, int capacity) {
                fault(2);
                return plan[0] == 2 && plan[1] == 2 ? null : java.util.Arrays.copyOf(source, plan[0] == 2 && plan[1] == 3 ? capacity - 1 : capacity);
            }
            @Override public Object[] objects(Object[] source, int capacity) {
                fault(3);
                return plan[0] == 3 && plan[1] == 2 ? null : java.util.Arrays.copyOf(source, plan[0] == 3 && plan[1] == 3 ? capacity - 1 : capacity);
            }
        };
        for (int i = 0; i < ops; i++) {
            long id = r.nextInt(4096);
            switch (r.nextInt(6)) {
                case 0 -> {
                    plan[0] = 1 + r.nextInt(3);
                    plan[1] = r.nextInt(4);
                    int required = store.residentCount() + 1 + r.nextInt(64);
                    call(log, "grow " + plan[0] + "/" + plan[1], () -> { store.ensureCapacity(required, copies); return store.residentCount(); });
                }
                case 1, 2 -> call(log, "internal", () -> { store.withInternal(id, facade -> { }); return store.residentCount(); });
                case 3 -> call(log, "expose", () -> store.expose(id));
                case 4 -> call(log, "trim", () -> store.releaseOptionalResidency(r.nextInt(512)));
                default -> call(log, "peek", () -> store.peekResident(id) + "/" + store.pinnedCount());
            }
        }
        call(log, "close", () -> { store.close(); return "ok"; });
        postClose(log, store);
    }
}
