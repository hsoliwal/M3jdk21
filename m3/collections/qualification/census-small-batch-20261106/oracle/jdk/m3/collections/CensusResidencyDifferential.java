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
 * Census leaf L066 oracle (census-small-batch-20261106): seeded episodes over the public API of
 * M3ObjectResidency, with factory/decode/delete callbacks that re-enter the arena. Compiled once and run
 * against the master class and the edited class; each op prints one line (outcome, resident bytes, slot
 * states, callback events). A fresh arena per episode bounds any divergence to its episode. The PUBLICATION
 * probes repeat the crate's three publication-budget probes (same output format as its evidence logs).
 * usage: java -cp CLASSES:ORACLE jdk.m3.collections.CensusResidencyDifferential OUT_FILE EPISODES OPS_PER_EPISODE
 */
public final class CensusResidencyDifferential {
    static final long SEED = 0x4C0664C0664C066L; // named seed "census-small-L066"
    static final int SLOTS = 12;
    static final long BUDGET = 48L;
    static final int[] FLAGS = {
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.RECREATE | M3ObjectResidency.LRU,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU
                | M3ObjectResidency.OBSERVE_ONLY,
    };

    private final StringBuilder events = new StringBuilder();
    private final byte[][] spill = new byte[SLOTS][];
    private M3ObjectResidency<String> arena;
    private SplittableRandom callbacks;
    private double reentry;
    private int depth;
    private int created;
    private String probePhase = "";

    public static void main(String[] args) throws IOException {
        int episodes = Integer.parseInt(args[1]);
        int ops = Integer.parseInt(args[2]);
        long over = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (String phase : new String[] {"factory", "decode", "delete"}) {
                out.write(new CensusResidencyDifferential().probe(phase));
                out.write('\n');
            }
            for (int e = 0; e < episodes; e++) {
                over += new CensusResidencyDifferential().episode(out, e, ops);
            }
            out.write("TOTAL episodes=" + episodes + " ops=" + (long) episodes * ops + " overBudgetStates=" + over + "\n");
        }
    }

    private long episode(BufferedWriter out, int e, int ops) throws IOException {
        SplittableRandom rnd = new SplittableRandom(SEED ^ (e * 0x9E3779B97F4A7C15L));
        callbacks = new SplittableRandom(~SEED ^ (e * 0xC2B2AE3D27D4EB4FL));
        int flags = FLAGS[e % FLAGS.length];
        reentry = (e / FLAGS.length) % 2 == 0 ? 0.0 : 0.3;
        arena = newArena(flags, BUDGET);
        long over = 0;
        for (int i = 0; i < ops; i++) {
            events.setLength(0);
            int slot = rnd.nextInt(SLOTS);
            long bytes = 1 + rnd.nextInt(16);
            int op = rnd.nextInt(10);
            String outcome;
            try {
                if (op < 7) {
                    outcome = "use " + slot + "/" + bytes + "=" + arena.use(slot, bytes, v -> v);
                } else if (op == 7) {
                    outcome = "install " + slot + "/" + bytes;
                    arena.install(slot, "i" + slot + "-" + (++created), bytes);
                } else if (op == 8) {
                    outcome = "pressure";
                    arena.signalMemoryPressure();
                } else {
                    outcome = "state " + slot + "=" + arena.state(slot);
                }
            } catch (RuntimeException failure) {
                outcome = "op" + op + " " + slot + "/" + bytes + " threw " + failure.getClass().getSimpleName()
                        + ":" + failure.getMessage();
            }
            boolean exceeded = (flags & M3ObjectResidency.OBSERVE_ONLY) == 0 && arena.residentBytes() > BUDGET;
            if (exceeded) over++;
            out.write(e + ":" + i + " " + outcome + " rb=" + arena.residentBytes() + (exceeded ? " OVER" : "")
                    + " st=" + states() + " ev=" + events + "\n");
        }
        return over;
    }

    private M3ObjectResidency<String> newArena(int flags, long budget) {
        return new M3ObjectResidency<>(SLOTS, flags, budget, 0L, this::create, new M3ObjectResidency.Codec<>() {
            @Override public byte[] encode(String value) {
                events.append("enc ").append(value).append(';');
                return value.getBytes(StandardCharsets.UTF_8);
            }
            @Override public String decode(int slot, byte[] data) {
                events.append("dec ").append(slot).append(';');
                reenter(slot, "decode");
                return new String(data, StandardCharsets.UTF_8);
            }
        }, new M3ObjectResidency.SpillStore() {
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
                reenter(slot, "delete");
                spill[slot] = null;
            }
        });
    }

    private String create(int slot) {
        events.append("new ").append(slot).append(';');
        reenter(slot, "factory");
        return "v" + slot + "-" + (++created);
    }

    /** Seeded nested use of another slot from inside a callback; probes re-enter slot 2 exactly once. */
    private void reenter(int slot, String phase) {
        if (!probePhase.isEmpty()) {
            if (probePhase.equals(phase) && depth == 0) {
                depth++;
                try { arena.use(2, 8, String::length); } finally { depth--; }
            }
            return;
        }
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

    /** The crate's publication probe: budget 8, slot 0 requested with 8 bytes, the callback uses slot 2 (8 bytes). */
    private String probe(String phase) {
        arena = newArena(M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU, 8L);
        if (!phase.equals("factory")) {
            arena.use(0, 8, String::length);
            arena.use(2, 8, String::length); // swaps slot 0 out
        }
        probePhase = phase;
        boolean refused = false;
        try {
            arena.use(0, 8, String::length);
        } catch (M3ObjectResidency.BudgetExceededException expected) {
            refused = true;
        }
        probePhase = "";
        return "PUBLICATION phase=" + phase + " refused=" + refused + " requested=" + arena.state(0) + " nested="
                + arena.state(2) + " bytes=" + arena.residentBytes() + " stored0=" + (spill[0] != null);
    }
}
