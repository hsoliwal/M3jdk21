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
 * Residency fold leaf PS oracle: seeded episodes over the public API of M3ObjectResidency that reach all four budget
 * boundaries (admission, signalMemoryPressure, both reuse budget checks), with and without PRESSURE_AWARE. The
 * pressure-aware owners use the two regimes whose sampled target is deterministic: reserve 0 (the sampled headroom
 * exceeds the 48-byte maximum, so the maximum governs) and reserve Long.MAX_VALUE (target 0 at every boundary).
 * Callbacks (factory, decode, spill delete, reset) re-enter the arena. Compiled once and run against the classes
 * before and after the leaf; each op prints one line. Derived from census-small-batch-20261106's
 * CensusResidencyDifferential (same line shape).
 * usage: java -cp CLASSES:ORACLE jdk.m3.collections.PressureSamplingDifferential OUT_FILE EPISODES OPS_PER_EPISODE
 */
public final class PressureSamplingDifferential {
    static final long SEED = 0x5053504153504153L; // named seed "PSPASPAS"
    static final int SLOTS = 12;
    static final long BUDGET = 48L;
    static final int SWAP = M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU;
    static final int REUSE = M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.RECREATE | M3ObjectResidency.LRU
            | M3ObjectResidency.STRICT_NO_ESCAPE;
    static final int AWARE = M3ObjectResidency.PRESSURE_AWARE;
    static final int[] FLAGS = {SWAP, REUSE, SWAP | AWARE, REUSE | AWARE, REUSE | AWARE, SWAP | AWARE,
        SWAP | AWARE | M3ObjectResidency.OBSERVE_ONLY};
    static final long[] RESERVES = {0L, 0L, 0L, 0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L};

    private final StringBuilder events = new StringBuilder();
    private final byte[][] spill = new byte[SLOTS][];
    private M3ObjectResidency<String> arena;
    private SplittableRandom callbacks;
    private double reentry;
    private int depth;
    private int created;

    public static void main(String[] args) throws IOException {
        int episodes = Integer.parseInt(args[1]);
        int ops = Integer.parseInt(args[2]);
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (int e = 0; e < episodes; e++) new PressureSamplingDifferential().episode(out, e, ops);
            out.write("TOTAL episodes=" + episodes + " ops=" + (long) episodes * ops + "\n");
        }
    }

    private void episode(BufferedWriter out, int e, int ops) throws IOException {
        SplittableRandom rnd = new SplittableRandom(SEED ^ (e * 0x9E3779B97F4A7C15L));
        callbacks = new SplittableRandom(~SEED ^ (e * 0xC2B2AE3D27D4EB4FL));
        int kind = e % FLAGS.length;
        reentry = (e / FLAGS.length) % 2 == 0 ? 0.0 : 0.3;
        arena = newArena(FLAGS[kind], RESERVES[kind]);
        for (int i = 0; i < ops; i++) {
            events.setLength(0);
            int slot = rnd.nextInt(SLOTS);
            long bytes = 1 + rnd.nextInt(16);
            int op = rnd.nextInt(10);
            String outcome;
            try {
                if (op < 6) {
                    outcome = "use " + slot + "/" + bytes + "=" + arena.use(slot, bytes, String::length);
                } else if (op == 6) {
                    outcome = "install " + slot + "/" + bytes;
                    arena.install(slot, "i" + slot + "-" + (++created), bytes);
                } else if (op == 7) {
                    outcome = "pressure";
                    arena.signalMemoryPressure();
                } else if (op == 8) {
                    int target = (slot + 1 + rnd.nextInt(SLOTS - 1)) % SLOTS;
                    long weight = rnd.nextInt(4) == 0 ? BUDGET + 1 : bytes;
                    outcome = "reuse " + slot + ">" + target + "/" + weight + "="
                            + arena.reuseUnborrowedProjection(slot, target, weight, this::reset);
                } else {
                    outcome = "state " + slot + "=" + arena.state(slot);
                }
            } catch (RuntimeException failure) {
                outcome = "op" + op + " " + slot + "/" + bytes + " threw " + failure.getClass().getSimpleName()
                        + ":" + failure.getMessage();
            }
            M3ObjectResidency.Decision last = arena.lastDecision();
            out.write(e + ":" + i + " " + outcome + " rb=" + arena.residentBytes() + " st=" + states() + " ev="
                    + events + " last=" + (last == null ? "-" : last.slot() + ":" + last.reason()) + "\n");
        }
    }

    private M3ObjectResidency<String> newArena(int flags, long reserve) {
        boolean swap = (flags & M3ObjectResidency.SWAP_SECONDARY) != 0;
        return new M3ObjectResidency<>(SLOTS, flags, BUDGET, reserve, this::create, !swap ? null
                : new M3ObjectResidency.Codec<>() {
            @Override public byte[] encode(String value) {
                events.append("enc ").append(value).append(';');
                return value.getBytes(StandardCharsets.UTF_8);
            }
            @Override public String decode(int slot, byte[] data) {
                events.append("dec ").append(slot).append(';');
                reenter(slot);
                return new String(data, StandardCharsets.UTF_8);
            }
        }, !swap ? null : new M3ObjectResidency.SpillStore() {
            @Override public void write(int slot, byte[] data) {
                events.append("w ").append(slot).append(';');
                spill[slot] = data.clone();
            }
            @Override public byte[] read(int slot) throws IOException {
                events.append("r ").append(slot).append(';');
                if (spill[slot] == null) throw new IOException("no spill for " + slot);
                return spill[slot].clone();
            }
            @Override public void delete(int slot) {
                events.append("del ").append(slot).append(';');
                reenter(slot);
                spill[slot] = null;
            }
        });
    }

    private String create(int slot) {
        events.append("new ").append(slot).append(';');
        reenter(slot);
        return "v" + slot + "-" + (++created);
    }

    private void reset(String projection, int target) {
        events.append("reset ").append(projection).append('>').append(target).append(';');
        reenter(target);
    }

    /** Seeded nested use of another slot from inside a callback. */
    private void reenter(int slot) {
        if (depth >= 2 || reentry == 0.0 || callbacks.nextDouble() >= reentry) return;
        int other = (slot + 1 + callbacks.nextInt(SLOTS - 1)) % SLOTS;
        long bytes = 1 + callbacks.nextInt(16);
        depth++;
        try {
            events.append("nest ").append(other).append('/').append(bytes).append('=')
                    .append(arena.use(other, bytes, String::length)).append(';');
        } catch (RuntimeException failure) {
            events.append("nest ").append(other).append(" threw ").append(failure.getClass().getSimpleName()).append(';');
        } finally {
            depth--;
        }
    }

    private String states() {
        StringBuilder s = new StringBuilder(SLOTS);
        for (int i = 0; i < SLOTS; i++) s.append("ACRWSO".charAt(arena.state(i).ordinal()));
        return s.toString();
    }
}
