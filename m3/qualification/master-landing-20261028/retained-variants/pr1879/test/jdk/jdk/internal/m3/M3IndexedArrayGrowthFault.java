/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 */

/** Test-only bootstrap hook. Product classes gain no test fields or methods. */
public final class M3IndexedArrayGrowthFault {
    public static final OutOfMemoryError FAILURE =
            new OutOfMemoryError("injected M3 indexed-array copy allocation failure");
    private static int remaining;
    private static int visits;
    private static boolean armed;

    private M3IndexedArrayGrowthFault() { }

    public static void arm(int allocation) {
        if (allocation < 1 || allocation > 7) throw new IllegalArgumentException();
        remaining = allocation;
        visits = 0;
        armed = true;
    }

    public static void beforeCopy() {
        if (!armed) return;
        visits++;
        if (--remaining == 0) {
            armed = false;
            throw FAILURE;
        }
    }

    public static int visits() { return visits; }
    public static void disarm() { armed = false; }
}
