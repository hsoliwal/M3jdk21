// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import java.util.List;

/** Dependency-free executable kernel tests; no claim of OpenRewrite SDK execution. */
public final class M3StringGuardRepairTest {
    private M3StringGuardRepairTest() {}
    public static void main(String[] args) {
        String before = "header\nold route\nfooter\n";
        String after = "header\nnew route\nfooter\n";
        var atoms = List.of(new M3StringGuardRepair.Atom("old route", "new route"));
        String seal = M3StringGuardRepair.sha256(before);
        check(M3StringGuardRepair.replay(before, seal, atoms).equals(after), "exact replay");
        check(M3StringGuardRepair.replay(after, seal, atoms).equals(after), "fixed point");
        refused(() -> M3StringGuardRepair.replay(before + "drift", seal, atoms));
        refused(() -> M3StringGuardRepair.replay(after + "drift", seal, atoms));
        refused(() -> M3StringGuardRepair.replay(before.replace("header", "HEAD"), seal, atoms));
        refused(() -> M3StringGuardRepair.replay("old route\nold route", seal, atoms));
        refused(() -> M3StringGuardRepair.replay("", seal, atoms));
        refused(() -> M3StringGuardRepair.replay(before, "0".repeat(64), atoms));
        refused(() -> M3StringGuardRepair.replay(before, seal,
                List.of(new M3StringGuardRepair.Atom("not here", "new route"))));
        String repeated = "old route\nold route\n";
        refused(() -> M3StringGuardRepair.replay(repeated, M3StringGuardRepair.sha256(repeated), atoms));
        String prefix = "before\nold route\nfooter\n";
        var nested = List.of(new M3StringGuardRepair.Atom("old route", "old route\nnew route"));
        String result = M3StringGuardRepair.replay(prefix, M3StringGuardRepair.sha256(prefix), nested);
        check(M3StringGuardRepair.replay(result, M3StringGuardRepair.sha256(prefix), nested).equals(result), "nested fixed point");
        System.out.println("M3_STRING_GUARD_KERNEL_TEST_PASS checks=11");
    }
    private static void check(boolean test, String label) {
        if (!test) throw new AssertionError(label);
    }
    private static void refused(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("drift was admitted");
    }
}
