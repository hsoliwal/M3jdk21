// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Victim-progress oracle (qualification/residency-victim-progress-20261106): seeded episodes over the public API of
 * M3ObjectResidency whose eviction callbacks (Codec.encode, SpillStore.write) re-enter the arena: nested use of
 * another slot, nested use, install and retain/release of the slot being admitted. The census L066 oracle
 * (CensusResidencyDifferential) re-enters only from factory/decode/delete, so it never runs a nested admission while
 * a victim is SWAPPING. Each episode runs on a worker thread under a watchdog: an op that does not finish within the
 * watchdog prints HANG (the livelock in admit()); the stuck worker holds the monitor of its own fresh arena only.
 * After MAX_STUCK stuck workers the run writes RESUME and exits 3; the driver restarts at the next episode (append
 * mode). Each op prints one line: outcome, resident bytes, OVER when resident bytes exceed the budget (OBSERVE_ONLY
 * excluded), slot states, callback events.
 *
 * <p>usage: java -cp CLASSES:ORACLE jdk.m3.collections.ResidencyEvictionReentryDifferential OUT FIRST EPISODES OPS
 */
public final class ResidencyEvictionReentryDifferential {
    static final long SEED = 0x7C705E5510611106L; // named seed "victim-progress-20261106"
    static final int SLOTS = 8;
    static final long BUDGET = 32L;
    static final long WATCHDOG_MILLIS = 1000L;
    static final int MAX_STUCK = 12;
    static final int[] FLAGS = {
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU
                | M3ObjectResidency.PIN_EXPOSED,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.RECREATE | M3ObjectResidency.LRU,
        M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY | M3ObjectResidency.LRU
                | M3ObjectResidency.OBSERVE_ONLY,
    };

    private final StringBuilder events = new StringBuilder();
    private final byte[][] spill = new byte[SLOTS][];
    private final List<String> lines = new ArrayList<>();
    private boolean abandoned;
    private M3ObjectResidency<String> arena;
    private SplittableRandom callbacks;
    private double reentry;
    private int depth;
    private int created;
    private int requested = -1;
    private volatile int progress;

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        int first = Integer.parseInt(args[1]);
        int episodes = Integer.parseInt(args[2]);
        int ops = Integer.parseInt(args[3]);
        int stuck = 0;
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (int e = first; e < episodes; e++) {
                ResidencyEvictionReentryDifferential run = new ResidencyEvictionReentryDifferential();
                int episode = e;
                Thread worker = new Thread(() -> run.episode(episode, ops), "episode-" + e);
                worker.setDaemon(true);
                worker.start();
                worker.join(WATCHDOG_MILLIS);
                boolean hung;
                synchronized (run.lines) {
                    hung = worker.isAlive();
                    for (String line : run.lines) {
                        w.write(line);
                        w.write('\n');
                    }
                    if (hung) {
                        w.write(e + ":" + run.progress + " HANG watchdog=" + WATCHDOG_MILLIS + "ms\n");
                        run.abandoned = true;
                    }
                }
                if (hung && ++stuck >= MAX_STUCK && e + 1 < episodes) {
                    w.write("RESUME " + (e + 1) + "\n");
                    w.flush();
                    System.exit(3);
                }
            }
            w.write("TOTAL episodes=" + episodes + " ops=" + (long) episodes * ops + "\n");
        }
    }

    private void emit(String line) {
        synchronized (lines) {
            if (!abandoned) lines.add(line);
        }
    }

    private void episode(int e, int ops) {
        SplittableRandom rnd = new SplittableRandom(SEED ^ (e * 0x9E3779B97F4A7C15L));
        callbacks = new SplittableRandom(~SEED ^ (e * 0xC2B2AE3D27D4EB4FL));
        int flags = FLAGS[e % FLAGS.length];
        reentry = (e / FLAGS.length) % 2 == 0 ? 0.0 : 0.35;
        arena = newArena(flags);
        for (int i = 0; i < ops; i++) {
            progress = i;
            events.setLength(0);
            int slot = rnd.nextInt(SLOTS);
            long bytes = 1 + rnd.nextInt(12);
            int op = rnd.nextInt(10);
            requested = slot;
            String outcome;
            try {
                if (op < 7) {
                    outcome = "use " + slot + "/" + bytes + "=" + arena.use(slot, bytes, String::length);
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
            requested = -1;
            boolean exceeded = (flags & M3ObjectResidency.OBSERVE_ONLY) == 0 && arena.residentBytes() > BUDGET;
            emit(e + ":" + i + " " + outcome + " rb=" + arena.residentBytes() + (exceeded ? " OVER" : "")
                    + " st=" + states() + " ev=" + events);
        }
    }

    private M3ObjectResidency<String> newArena(int flags) {
        return new M3ObjectResidency<>(SLOTS, flags, BUDGET, 0L, this::create, new M3ObjectResidency.Codec<>() {
            @Override public byte[] encode(String value) {
                events.append("enc ").append(value).append(';');
                reenter("encode");
                return value.getBytes(StandardCharsets.UTF_8);
            }
            @Override public String decode(int slot, byte[] data) {
                events.append("dec ").append(slot).append(';');
                return new String(data, StandardCharsets.UTF_8);
            }
        }, new M3ObjectResidency.SpillStore() {
            @Override public void write(int slot, byte[] data) {
                events.append("w ").append(slot).append(';');
                reenter("write");
                spill[slot] = data.clone();
            }
            @Override public byte[] read(int slot) throws IOException {
                events.append("r ").append(slot).append(';');
                if (spill[slot] == null) throw new IOException("no spill for " + slot);
                return spill[slot].clone();
            }
            @Override public void delete(int slot) {
                events.append("del ").append(slot).append(';');
                spill[slot] = null;
            }
        });
    }

    private String create(int slot) {
        events.append("new ").append(slot).append(';');
        return "v" + slot + "-" + (++created);
    }

    /** Seeded nested request from inside an eviction callback: another slot, or the slot being admitted. */
    private void reenter(String phase) {
        if (depth >= 2 || reentry == 0.0 || callbacks.nextDouble() >= reentry) return;
        int kind = callbacks.nextInt(4);
        int other = kind == 0 || requested < 0 ? callbacks.nextInt(SLOTS) : requested;
        long bytes = 1 + callbacks.nextInt(12);
        depth++;
        try {
            events.append(phase).append("-nest").append(kind).append(' ').append(other).append('/').append(bytes);
            if (kind <= 1) {
                int length = arena.use(other, bytes, String::length);
                events.append('=').append(length);
            } else if (kind == 2) {
                arena.install(other, "n" + other + "-" + (++created), bytes);
                events.append("=installed");
            } else {
                long lease = arena.retainProjection(other, bytes);
                arena.releaseProjection(lease);
                events.append("=retained");
            }
            events.append(';');
        } catch (RuntimeException failure) {
            events.append(" threw ").append(failure.getClass().getSimpleName()).append(':')
                    .append(failure.getMessage()).append(';');
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
