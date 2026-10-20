// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Standalone Java21 assertions; the Maven JUnit test also invokes this contract. */
public final class M3BuildStateContract {
    private static int checks;
    private M3BuildStateContract() {}
    private static void check(boolean truth) {
        checks++;
        if (!truth) throw new AssertionError("M3 build state check " + checks);
    }
    private static void refusal(Class<? extends Throwable> kind, Throwing operation) {
        try { operation.run(); }
        catch (Throwable actual) { check(kind.isInstance(actual)); return; }
        throw new AssertionError("expected " + kind.getName());
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
    private static byte[] digest(int value) {
        byte[] result = new byte[32]; Arrays.fill(result, (byte) value); return result;
    }
    private static byte[] hashes(int... values) {
        byte[] result = new byte[values.length * 32];
        for (int i = 0; i < values.length; i++) Arrays.fill(result, i * 32, i * 32 + 32, (byte) values[i]);
        return result;
    }
    private static M3BuildState graph(int sourceBody, int sourceAbi, int binaryBody, int binaryAbi,
            int config, int inputContent, boolean knownAbi) {
        String[] paths = {"A.java", "B.java", "C.java", "External.class", "pom.xml"};
        byte[] kinds = {M3BuildState.SOURCE, M3BuildState.SOURCE, M3BuildState.SOURCE,
                M3BuildState.CLASS, M3BuildState.INPUT};
        byte[] trusted = {(byte)(knownAbi ? 1 : 0), 1, 1, 1, 0};
        return M3BuildState.of(digest(config), paths, kinds,
                hashes(sourceBody, 2, 3, binaryBody, inputContent),
                hashes(knownAbi ? sourceAbi : 0, 12, 13, binaryAbi, 0), trusted,
                new int[]{0, 1, 2}, new int[]{3, 0, 1});
    }
    private static void rows(M3BuildState.Plan plan, int... expected) {
        check(Arrays.equals(expected, plan.recompileRows()));
    }
    public static void main(String[] ignored) throws Exception {
        M3BuildState old = graph(1, 11, 4, 14, 7, 5, true);
        check(old.rows() == 5 && old.edges() == 3);
        check(old.identityAt(0).equals("A.java"));
        check(old.kindAt(3) == M3BuildState.CLASS);
        byte[] extracted = old.contentAt(0); extracted[0] = 99;
        check(old.contentAt(0)[0] == 1);
        check(Arrays.equals(old.encode(), M3BuildState.decode(old.encode()).encode()));
        rows(old.planAfter(old));
        rows(M3BuildState.decode(old.encode()).planAfter(old));
        rows(old.planAfter(null), 0, 1, 2);
        check((old.planAfter(null).reasonAt(0) & M3BuildState.CONTEXT) != 0);
        rows(graph(9, 11, 4, 14, 7, 5, true).planAfter(old), 0);
        rows(graph(9, 99, 4, 14, 7, 5, true).planAfter(old), 0, 1, 2);
        rows(graph(1, 99, 4, 14, 7, 5, true).planAfter(old), 0, 1, 2);
        rows(graph(1, 11, 9, 14, 7, 5, true).planAfter(old));
        rows(graph(1, 11, 9, 99, 7, 5, true).planAfter(old), 0, 1, 2);
        rows(graph(9, 0, 4, 14, 7, 5, false).planAfter(old), 0, 1, 2);
        rows(graph(1, 11, 4, 14, 8, 5, true).planAfter(old), 0, 1, 2);
        M3BuildState.Plan inputPlan = graph(1, 11, 4, 14, 7, 9, true).planAfter(old);
        rows(inputPlan, 0, 1, 2);
        check((inputPlan.reasonAt(0) & M3BuildState.INPUT_CHANGED) != 0);
        check((inputPlan.reasonAt(0) & M3BuildState.REMOVAL) == 0);
        M3BuildState removed = M3BuildState.of(digest(7),
                new String[]{"A.java", "B.java", "C.java", "pom.xml"},
                new byte[]{1,1,1,3}, hashes(1,2,3,5), hashes(11,12,13,0),
                new byte[]{1,1,1,0}, new int[]{1,2}, new int[]{0,1});
        rows(removed.planAfter(old), 0, 1, 2);
        check((removed.planAfter(old).reasonAt(1) & M3BuildState.REMOVAL) != 0);
        M3BuildState cycle = M3BuildState.of(digest(1),
                new String[]{"A.java","B.java"}, new byte[]{1,1},
                hashes(1,2), hashes(11,12), new byte[]{1,1},
                new int[]{0,1}, new int[]{1,0});
        M3BuildState cycleNew = M3BuildState.of(digest(1),
                new String[]{"A.java","B.java"}, new byte[]{1,1},
                hashes(9,2), hashes(91,12), new byte[]{1,1},
                new int[]{0,1}, new int[]{1,0});
        rows(cycleNew.planAfter(cycle), 0, 1);
        M3BuildState empty = M3BuildState.of(digest(0), new String[0], new byte[0],
                new byte[0], new byte[0], new byte[0], new int[0], new int[0]);
        rows(empty.planAfter(empty));
        check(M3BuildState.decode(empty.encode()).rows() == 0);
        M3BuildState fresh = M3BuildState.of(digest(7),
                new String[]{"A.java", "B.java"}, new byte[]{1,1},
                hashes(1,2), hashes(11,12), new byte[]{1,1}, new int[]{1}, new int[]{0});
        M3BuildState oldSingle = M3BuildState.of(digest(7),
                new String[]{"A.java"}, new byte[]{1}, hashes(1), hashes(11),
                new byte[]{1}, new int[0], new int[0]);
        rows(fresh.planAfter(oldSingle), 1);
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"z","a"}, new byte[]{1,1}, hashes(1,2), hashes(1,2),
                new byte[]{1,1}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x","x"}, new byte[]{1,1}, hashes(1,2), hashes(1,2),
                new byte[]{1,1}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x"}, new byte[]{9}, hashes(1), hashes(1),
                new byte[]{1}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x"}, new byte[]{1}, hashes(1), hashes(1),
                new byte[]{0}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x"}, new byte[]{1}, hashes(1), hashes(0),
                new byte[]{2}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x"}, new byte[]{1}, hashes(1), hashes(1),
                new byte[]{1}, new int[]{1}, new int[]{0}));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x"}, new byte[]{1}, hashes(1), hashes(1),
                new byte[]{1}, new int[]{0,0}, new int[]{0,0}));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(new byte[31],
                new String[0], new byte[0], new byte[0], new byte[0], new byte[0], new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"bad\0name"}, new byte[]{1}, hashes(1), hashes(1),
                new byte[]{1}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{""}, new byte[]{1}, hashes(1), hashes(1),
                new byte[]{1}, new int[0], new int[0]));
        refusal(IllegalArgumentException.class, () -> M3BuildState.of(digest(0),
                new String[]{"x"}, new byte[0], hashes(1), hashes(1),
                new byte[]{1}, new int[0], new int[0]));
        refusal(IndexOutOfBoundsException.class, () -> old.contentAt(7));
        refusal(IndexOutOfBoundsException.class, () -> old.planAfter(null).reasonAt(-1));
        byte[] corrupted = old.encode(); corrupted[0] = 0;
        byte[] invalidMagic = corrupted;
        refusal(IOException.class, () -> M3BuildState.decode(invalidMagic));
        corrupted = old.encode(); corrupted[7] = 9;
        byte[] invalidVersion = corrupted;
        refusal(IOException.class, () -> M3BuildState.decode(invalidVersion));
        corrupted = old.encode(); corrupted[47] = 99;
        byte[] absurdCount = corrupted;
        refusal(IOException.class, () -> M3BuildState.decode(absurdCount));
        byte[] truncated = Arrays.copyOf(old.encode(), 47);
        refusal(IOException.class, () -> M3BuildState.decode(truncated));
        corrupted = Arrays.copyOf(old.encode(), old.encode().length + 1);
        byte[] trailing = corrupted;
        refusal(IOException.class, () -> M3BuildState.decode(trailing));
        corrupted = old.encode(); corrupted[56] = 9;
        byte[] corruptKind = corrupted;
        refusal(IOException.class, () -> M3BuildState.decode(corruptKind));
        Path dir = Files.createTempDirectory("m3-build-state-");
        try (M3IndexDB db = M3IndexDB.open(dir)) {
            old.store(db, "maven.build");
            check(Arrays.equals(old.encode(), M3BuildState.load(db, "maven.build").encode()));
            db.putArtifact("wrong", "m3.other", 1, old.encode());
            refusal(IOException.class, () -> M3BuildState.load(db, "wrong"));
        }
        try (M3IndexDB db = M3IndexDB.open(dir)) {
            check(Arrays.equals(old.encode(), M3BuildState.load(db, "maven.build").encode()));
        }
        System.out.println("M3_BUILD_STATE_PASS checks=" + checks);
    }
}
