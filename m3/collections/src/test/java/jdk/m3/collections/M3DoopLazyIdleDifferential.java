// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Same deterministic operations against original and candidate Java21 bytecode. */
//FLLDCIM: RELOCATE - donor blob 54c126868d13 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
public final class M3DoopLazyIdleDifferential {
    private static final class Cell {
        long owner = -1;
        String payload;
    }
    private M3DoopLazyIdleDifferential() { }

    public static void main(String[] args) {
        List<String> ledger = new ArrayList<>();
        for (int maximum : new int[] {0, 1, 2, 4, 20, 64, 256}) {
            int[] allocations = {0};
            try (var pool = new M3InternalLeaseRecycler<Cell>(
                    () -> { allocations[0]++; return new Cell(); },
                    (cell, id) -> {
                        if (cell.owner != -1 || cell.payload != null) {
                            throw new AssertionError("invalid prior owner");
                        }
                        cell.owner = id;
                    },
                    cell -> { cell.owner = -1; cell.payload = null; }, maximum)) {
                long seed = 0x9e3779b97f4a7c15L;
                for (int i = 0; i < 2400; i++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    int action = (int) ((seed >>> 17) & 15);
                    if (action <= 9) {
                        long id = (seed >>> 4) & 65535;
                        boolean fail = (i % 97) == 13;
                        try {
                            pool.withBorrowed(id, cell -> {
                                if (cell.owner != id) throw new AssertionError("owner");
                                cell.payload = "payload-" + id;
                                if (fail) throw new IllegalStateException("intentional body exception");
                            });
                            if (fail) throw new AssertionError("missing exception");
                        } catch (IllegalStateException error) {
                            if (!fail || !Objects.equals(error.getMessage(), "intentional body exception")) {
                                throw error;
                            }
                            ledger.add(maximum + ":exception:" + i);
                        }
                    } else if (action == 10) {
                        long innerId = (long) i + 1L;
                        pool.withBorrowed(i, outer -> pool.withBorrowed(innerId,
                                inner -> { if (outer == inner) throw new AssertionError("alias"); }));
                    } else if (action == 11) {
                        pool.trimIdle((int) (seed & 3));
                    } else if (action == 12) {
                        pool.onPressure(M3RuntimeHeap.Pressure.WARM);
                    } else if (action == 13) {
                        pool.onPressure(M3RuntimeHeap.Pressure.HIGH);
                    } else if (action == 14) {
                        pool.onPressure(M3RuntimeHeap.Pressure.NORMAL);
                    } else {
                        var stats = pool.stats();
                        ledger.add(maximum + ":" + i + ":" + stats);
                    }
                }
                var stats = pool.stats();
                if (stats.active() != 0 || allocations[0] != stats.created()) {
                    throw new AssertionError("unbalanced allocation or lease");
                }
                ledger.add(maximum + ":final:" + stats);
            }
        }
        System.out.println(String.join("\n", ledger));
    }
}
