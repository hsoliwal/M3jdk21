// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Seeded differential over the existing public API of M3TransientFacadeStore (recipe
 * qualification/doop-facade-fixes-20261107, M3JDK-authored). Compiled once and run against the master classes and the
 * head classes; classify_diff.py compares the two traces episode by episode.
 *
 * <p>Each episode owns a fresh store (default mode with or without a scrub/rebind recycler of 1, 2 or 4 idle slots,
 * or retaining mode: the owner refuses a recycler there) and runs seeded expose / withInternal / peek / pin / remove /
 * trim / idle-release / count / stats calls. The factory and the recycler's rebind reenter the owner once with a seeded probability (expose or withInternal of
 * the same id, expose of another id, remove of the same id). An operation whose callback made its own id resident is
 * tagged dup=1: only such operations reach the leaf-A path (insertCreated). Leaf B's copy-fault path needs the
 * package-private seam, absent on master; its normal growth path runs in every recycler episode. Every episode ends
 * with close() and a ledger line: created facades, disposed facades, double disposals and leaked facades (created,
 * never disposed).</p>
 *
 * <p>usage: DoopFacadeFixesDifferential OUT_FILE [episodes=20000] [ops=40] [seed=20261107]</p>
 */
public final class DoopFacadeFixesDifferential {
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final int IDS = 12;
    private static final int REENTER_PERCENT = 20;

    private DoopFacadeFixesDifferential() { }

    /** A facade with a creation serial; identity is observed through the serial. */
    static final class Facade {
        long id;
        final int serial;
        Facade(long id, int serial) { this.id = id; this.serial = serial; }
        @Override public String toString() { return "F" + id + "#" + serial; }
    }

    /** One episode: its store, its callbacks' notes and the disposal ledger. */
    static final class Episode {
        final SplittableRandom random;
        final List<String> notes = new ArrayList<>();
        final Map<Integer, Integer> disposals = new HashMap<>();
        M3TransientFacadeStore<Facade> owner;
        int serials;
        int depth;
        boolean dup;
        long current;
        Episode(SplittableRandom random) { this.random = random; }

        Facade create(long id) {
            Facade made = new Facade(id, ++serials);
            notes.add("new" + made);
            reenter(id);
            return made;
        }

        void dispose(long id, Facade facade) {
            disposals.merge(facade.serial, 1, Integer::sum);
            notes.add("dispose(" + id + ")" + facade);
        }

        void rebind(Facade facade, long id) {
            facade.id = id;
            notes.add("rebind" + facade);
            reenter(id);
        }

        /** At most one nested call per callback chain; marks dup when the callback made its own id resident. */
        void reenter(long id) {
            if (depth > 0 || random.nextInt(100) >= REENTER_PERCENT) return;
            depth++;
            int kind = random.nextInt(4);
            long other = 1 + (id + random.nextInt(IDS - 1)) % IDS;
            String result;
            try {
                result = switch (kind) {
                    case 0 -> "nested.expose(" + id + ")=" + owner.expose(id);
                    case 1 -> {
                        StringBuilder seen = new StringBuilder();
                        owner.withInternal(id, live -> seen.append(live));
                        yield "nested.withInternal(" + id + ")=" + seen;
                    }
                    case 2 -> "nested.expose(" + other + ")=" + owner.expose(other);
                    default -> "nested.remove(" + id + ")=" + owner.remove(id);
                };
            } catch (RuntimeException failure) {
                result = "nested!" + describe(failure);
            } finally {
                depth--;
            }
            notes.add(result);
            if (id == current && owner.isResident(id)) dup = true;
        }
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[0]);
        int episodes = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        int ops = args.length > 2 ? Integer.parseInt(args[2]) : 40;
        long seed = args.length > 3 ? Long.parseLong(args[3]) : 20261107L;
        long dupOps = 0;
        long leaked = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            for (int e = 0; e < episodes; e++) {
                long[] tally = episode(w, e, ops, new SplittableRandom(seed + GOLDEN * (e + 1)));
                dupOps += tally[0];
                leaked += tally[1];
            }
        }
        System.out.println("DoopFacadeFixesDifferential episodes=" + episodes + " ops=" + ops + " seed=" + seed
                + " dupOps=" + dupOps + " leaked=" + leaked);
    }

    private static long[] episode(PrintWriter w, int e, int ops, SplittableRandom r) {
        Episode ep = new Episode(r);
        int mode = r.nextBoolean() ? M3TransientFacadeStore.DEFAULT_MODE
                : M3TransientFacadeStore.RETAIN_UNEXPOSED | M3TransientFacadeStore.PIN_EXPOSED
                        | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        int idle = r.nextInt(4);
        if (mode != M3TransientFacadeStore.DEFAULT_MODE) idle = 0; // a recycler requires recreate-unexposed mode
        M3TransientFacadeStore.Recycler<Facade> recycler = new M3TransientFacadeStore.Recycler<>() {
            @Override public void scrub(Facade facade) { ep.notes.add("scrub" + facade); facade.id = -1L; }
            @Override public void rebind(Facade facade, long id) { ep.rebind(facade, id); }
        };
        ep.owner = idle == 0 ? new M3TransientFacadeStore<>(ep::create, ep::dispose, mode)
                : new M3TransientFacadeStore<>(ep::create, ep::dispose, mode, recycler, 1 << (idle - 1));
        w.println("e" + e + " BEGIN mode=" + mode + " maxIdle=" + (idle == 0 ? 0 : 1 << (idle - 1)));
        long dupOps = 0;
        for (int o = 0; o < ops; o++) {
            long id = 1 + r.nextInt(IDS);
            ep.current = id;
            ep.dup = false;
            String call = call(ep, r.nextInt(16), id, r);
            dupOps += ep.dup ? 1 : 0;
            w.println("e" + e + " o" + o + " dup=" + (ep.dup ? 1 : 0) + " " + call + " " + ep.notes);
            ep.notes.clear();
        }
        ep.current = 0;
        String closed = run(() -> { ep.owner.close(); return "ok"; });
        long created = ep.serials;
        long disposed = ep.disposals.size();
        long twice = ep.disposals.values().stream().filter(n -> n > 1).count();
        w.println("e" + e + " END close=" + closed + " " + ep.notes + " created=" + created + " disposed=" + disposed
                + " double=" + twice + " leaked=" + (created - disposed) + " dupOps=" + dupOps);
        ep.notes.clear();
        return new long[] {dupOps, created - disposed};
    }

    private static String call(Episode ep, int kind, long id, SplittableRandom r) {
        M3TransientFacadeStore<Facade> s = ep.owner;
        return switch (kind) {
            case 0, 1, 2 -> "expose(" + id + ")=" + run(() -> s.expose(id));
            case 3, 4, 5 -> {
                boolean fail = r.nextInt(20) == 0;
                StringBuilder seen = new StringBuilder();
                yield "withInternal(" + id + (fail ? ",throw" : "") + ")=" + run(() -> {
                    s.withInternal(id, live -> {
                        seen.append(live);
                        if (fail) throw new IllegalArgumentException("action");
                    });
                    return seen;
                }) + " seen=" + seen;
            }
            case 6 -> "peekResident(" + id + ")=" + run(() -> s.peekResident(id));
            case 7 -> "isPinned(" + id + ")=" + run(() -> s.isPinned(id)) + " isResident=" + run(() -> s.isResident(id));
            case 8, 9 -> "remove(" + id + ")=" + run(() -> s.remove(id));
            case 10 -> {
                int keep = r.nextInt(4);
                yield "releaseOptionalResidency(" + keep + ")=" + run(() -> s.releaseOptionalResidency(keep));
            }
            case 11 -> {
                int keep = r.nextInt(3);
                yield "releaseReusable(" + keep + ")=" + run(() -> s.releaseReusable(keep));
            }
            case 12 -> "counts=" + run(() -> s.residentCount() + "/" + s.pinnedCount() + "/" + s.reusableIdleCount()
                    + "/" + s.reuseCount());
            case 13 -> "stats=" + run(s::stats);
            case 14 -> "flags(" + id + ")=" + run(() -> s.residencyFlags(id));
            default -> r.nextInt(25) == 0 ? "close=" + run(() -> { s.close(); return "ok"; })
                    : "mode=" + run(s::mode);
        };
    }

    @FunctionalInterface interface Op { Object get(); }

    private static String run(Op op) {
        try {
            return String.valueOf(op.get());
        } catch (RuntimeException | Error failure) {
            return "!" + describe(failure);
        }
    }

    /** Class, message and suppressed chain; implicit VM exception messages are not compared (C2 may drop them). */
    static String describe(Throwable failure) {
        boolean implicit = failure instanceof ArrayIndexOutOfBoundsException || failure instanceof ClassCastException
                || failure instanceof NullPointerException && String.valueOf(failure.getMessage()).startsWith("Cannot ");
        StringBuilder text = new StringBuilder(failure.getClass().getSimpleName()).append(':')
                .append(implicit ? "<implicit>" : failure.getMessage());
        for (Throwable suppressed : failure.getSuppressed()) {
            text.append("+suppressed[").append(describe(suppressed)).append(']');
        }
        return text.toString();
    }
}
