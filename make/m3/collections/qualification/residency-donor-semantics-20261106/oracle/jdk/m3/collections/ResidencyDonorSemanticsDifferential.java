// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SplittableRandom;

/**
 * Donor-semantics oracle (qualification/residency-donor-semantics-20261106): seeded episodes over the public API of
 * M3ObjectResidency that reach the adopted paths, which the census L066 oracle, the transient-facade dump and the
 * victim-progress oracle never reach: callbacks (factory, encode, write, read, decode, delete) that throw an Error,
 * a RuntimeException or an IOException, and reuseUnborrowedProjection with resetters that re-enter the arena, throw
 * (Error, RuntimeException, checked) or succeed, over flag sets with and without LAZY_CREATE, OBSERVE_ONLY and
 * PIN_EXPOSED, with borrowed and pinned slots. Compiled once, run against the classes before and after the change;
 * each op prints one line: outcome (exception class, message and cause class), resident bytes, OVER when resident
 * bytes exceed the budget (OBSERVE_ONLY excluded), slot states, callback events. A fresh arena per episode bounds a
 * divergence to its episode. No threads, no timing, no heap sampling (PRESSURE_AWARE is not used).
 *
 * <p>usage: java -cp CLASSES:ORACLE jdk.m3.collections.ResidencyDonorSemanticsDifferential OUT EPISODES OPS
 */
public final class ResidencyDonorSemanticsDifferential {
    static final long SEED = 0x2D0A5E3A1106L; // named seed "donor-semantics-1106"
    static final int SLOTS = 6;
    static final long BUDGET = 24L;
    static final int REUSE = M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU | M3ObjectResidency.RECREATE
            | M3ObjectResidency.STRICT_NO_ESCAPE;
    static final int[] FLAGS = {
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU,
        REUSE,
        REUSE | M3ObjectResidency.PIN_EXPOSED,
        REUSE & ~M3ObjectResidency.LAZY_CREATE,
        REUSE | M3ObjectResidency.OBSERVE_ONLY,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU
                | M3ObjectResidency.PIN_EXPOSED,
    };

    private final StringBuilder events = new StringBuilder();
    private final byte[][] spill = new byte[SLOTS][];
    private M3ObjectResidency<StringBuilder> arena;
    private SplittableRandom faults;
    private double faultRate;
    private int depth;
    private int created;

    public static void main(String[] args) throws IOException {
        int episodes = Integer.parseInt(args[1]);
        int ops = Integer.parseInt(args[2]);
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (int e = 0; e < episodes; e++) {
                new ResidencyDonorSemanticsDifferential().episode(out, e, ops);
            }
            out.write("TOTAL episodes=" + episodes + " ops=" + (long) episodes * ops + "\n");
        }
    }

    private void episode(BufferedWriter out, int e, int ops) throws IOException {
        SplittableRandom rnd = new SplittableRandom(SEED ^ (e * 0x9E3779B97F4A7C15L));
        faults = new SplittableRandom(~SEED ^ (e * 0xC2B2AE3D27D4EB4FL));
        int flags = FLAGS[e % FLAGS.length];
        faultRate = (e / FLAGS.length) % 2 == 0 ? 0.0 : 0.15;
        arena = newArena(flags);
        long lease = 0L;
        for (int i = 0; i < ops; i++) {
            events.setLength(0);
            int slot = rnd.nextInt(SLOTS);
            int other = rnd.nextInt(SLOTS);
            long bytes = 1 + rnd.nextInt(10);
            int op = rnd.nextInt(12);
            String outcome;
            try {
                if (op < 5) {
                    outcome = "use " + slot + "/" + bytes + "=" + arena.use(slot, bytes,
                            v -> (flags & M3ObjectResidency.PIN_EXPOSED) != 0 && v.length() % 3 == 0 ? v : v.length());
                } else if (op == 5) {
                    outcome = "install " + slot + "/" + bytes;
                    arena.install(slot, new StringBuilder("i" + slot + "-" + ++created), bytes);
                } else if (op == 6) {
                    if (lease == 0L) {
                        outcome = "retain " + slot;
                        lease = arena.retainProjection(slot, bytes);
                    } else {
                        outcome = "release";
                        long held = lease;
                        lease = 0L;
                        arena.releaseProjection(held);
                    }
                } else if (op < 10) {
                    int mode = rnd.nextInt(6);
                    outcome = "reuse " + slot + "->" + other + "/" + bytes + "#" + mode + "="
                            + arena.reuseUnborrowedProjection(slot, other, bytes, (v, target) -> reset(v, target, mode));
                } else if (op == 10) {
                    outcome = "pressure";
                    arena.signalMemoryPressure();
                } else {
                    outcome = "state " + slot + "=" + arena.state(slot);
                }
            } catch (RuntimeException | Error failure) {
                outcome = "op" + op + " " + slot + "/" + bytes + " threw " + describe(failure);
            }
            boolean exceeded = (flags & M3ObjectResidency.OBSERVE_ONLY) == 0 && arena.residentBytes() > BUDGET;
            out.write(e + ":" + i + " " + outcome + " rb=" + arena.residentBytes() + (exceeded ? " OVER" : "")
                    + " st=" + states() + " ev=" + events + "\n");
        }
    }

    private static String describe(Throwable failure) {
        Throwable cause = failure.getCause();
        return failure.getClass().getSimpleName() + ":" + failure.getMessage()
                + (cause == null ? "" : " cause=" + cause.getClass().getSimpleName() + ":" + cause.getMessage());
    }

    /** Resetter modes: 0 reset, 1 nested use of a seeded slot then reset, 2 RuntimeException, 3 IOException,
     *  4 AssertionError, 5 nested use of the source slot (refused) then reset. */
    private void reset(StringBuilder value, int target, int mode) throws IOException {
        events.append("reset ").append(target).append('#').append(mode).append(" rb=").append(arena.residentBytes())
                .append(" st=").append(states()).append(';');
        value.setLength(0);
        value.append("r").append(target).append('-').append(++created);
        switch (mode) {
            case 1 -> nested(faults.nextInt(SLOTS), 1 + faults.nextInt(10));
            case 2 -> throw new IllegalArgumentException("reset runtime " + target);
            case 3 -> throw new IOException("reset checked " + target);
            case 4 -> throw new AssertionError("reset error " + target);
            case 5 -> nested(-1, 4);
            default -> { }
        }
    }

    private void nested(int slot, long bytes) {
        if (depth >= 2) return;
        depth++;
        try {
            int chosen = slot >= 0 ? slot : firstCreating();
            int length = arena.use(chosen, bytes, StringBuilder::length);
            events.append("nest ").append(chosen).append('/').append(bytes).append('=').append(length).append(';');
        } catch (RuntimeException failure) {
            events.append("nest threw ").append(describe(failure)).append(';');
        } finally {
            depth--;
        }
    }

    private int firstCreating() {
        for (int i = 0; i < SLOTS; i++) if (arena.state(i) != M3ObjectResidency.State.RESIDENT
                && arena.state(i) != M3ObjectResidency.State.ABSENT) return i;
        return 0;
    }

    /** Callback fault: Error, RuntimeException or IOException with the episode's fault rate. */
    private void fault(String site) throws IOException {
        if (faultRate == 0.0 || faults.nextDouble() >= faultRate) return;
        int kind = faults.nextInt(3);
        events.append("fault ").append(site).append('#').append(kind).append(';');
        if (kind == 0) throw new AssertionError(site + " error");
        if (kind == 1) throw new IllegalStateException(site + " runtime");
        throw new IOException(site + " checked");
    }

    private M3ObjectResidency<StringBuilder> newArena(int flags) {
        return new M3ObjectResidency<>(SLOTS, flags, BUDGET, 0L, this::create, new M3ObjectResidency.Codec<>() {
            @Override public byte[] encode(StringBuilder value) throws IOException {
                events.append("enc ").append(value).append(';');
                fault("encode");
                return value.toString().getBytes(StandardCharsets.UTF_8);
            }
            @Override public StringBuilder decode(int slot, byte[] data) throws IOException {
                events.append("dec ").append(slot).append(';');
                fault("decode");
                return new StringBuilder(new String(data, StandardCharsets.UTF_8));
            }
        }, new M3ObjectResidency.SpillStore() {
            @Override public void write(int slot, byte[] data) throws IOException {
                events.append("w ").append(slot).append(';');
                fault("write");
                spill[slot] = data.clone();
            }
            @Override public byte[] read(int slot) throws IOException {
                events.append("r ").append(slot).append(';');
                if (spill[slot] == null) throw new IOException("no spill for " + slot);
                fault("read");
                return spill[slot].clone();
            }
            @Override public void delete(int slot) throws IOException {
                events.append("del ").append(slot).append(';');
                fault("delete");
                spill[slot] = null;
            }
        });
    }

    private StringBuilder create(int slot) throws IOException {
        events.append("new ").append(slot).append(';');
        fault("factory");
        return new StringBuilder("v" + slot + "-" + ++created);
    }

    private String states() {
        StringBuilder s = new StringBuilder(SLOTS);
        for (int i = 0; i < SLOTS; i++) s.append("ACRWSO".charAt(arena.state(i).ordinal()));
        return s.toString();
    }
}
