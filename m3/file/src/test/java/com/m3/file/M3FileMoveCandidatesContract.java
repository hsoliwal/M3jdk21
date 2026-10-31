// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Standalone Java 21 contract for the existing M3File delta snapshot owner. */
public final class M3FileMoveCandidatesContract {
    private static int checks;
    private M3FileMoveCandidatesContract() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-file-move-");
        try {
            Path old = root.resolve("src/Old.java");
            Path next = root.resolve("src/New.java");
            Files.createDirectories(old.getParent());
            Files.writeString(old, "class A {}");
            var baseline = M3FileDeltaLedger.scan(root, null, List.of(old));
            Files.move(old, next);
            var moved = M3FileDeltaLedger.scan(root, baseline, List.of(next));
            var index = M3FileMoveCandidates.index(moved);
            ok(index.candidateCount() == 1, "unique move by digest");
            int from = moved.physicalSlot(old);
            int to = moved.physicalSlot(next);
            ok(Arrays.equals(index.removedCandidateSlots(), new int[] {from}), "source slot");
            ok(Arrays.equals(index.createdCandidateSlots(), new int[] {to}), "target slot");
            ok(index.ambiguityCount() == 0 && index.unmatchedCreatedSlots().length == 0,
                    "unique join complete");
            int[] detached = index.removedCandidateSlots();
            detached[0] = -999;
            ok(index.removedCandidateSlots()[0] == from, "defensive slot projection");
            ok(index.candidateCount() == M3FileMoveCandidates.index(moved).candidateCount(),
                    "deterministic replay");

            Path old2 = root.resolve("src/Duplicate.java");
            Path new2 = root.resolve("src/Copy.java");
            Files.writeString(old2, "class A {}");
            var withDuplicate = M3FileDeltaLedger.scan(root, moved, List.of(next, old2));
            Files.delete(next);
            Files.delete(old2);
            Files.writeString(new2, "class A {}");
            var oneNew = M3FileDeltaLedger.scan(root, withDuplicate, List.of(new2));
            var ambiguousSources = M3FileMoveCandidates.index(oneNew);
            ok(ambiguousSources.candidateCount() == 0 && ambiguousSources.ambiguityCount() == 1,
                    "two removed paths, one created path is ambiguous");
            ok(Arrays.equals(ambiguousSources.ambiguousCreatedSlots(),
                    new int[] {oneNew.physicalSlot(new2)}), "ambiguity retains created physical slot");

            Files.writeString(old, "same");
            var oneOld = M3FileDeltaLedger.scan(root, oneNew, List.of(new2, old));
            Files.delete(old);
            Path copyA = root.resolve("src/CopyA.java");
            Path copyB = root.resolve("src/CopyB.java");
            Files.writeString(copyA, "same");
            Files.writeString(copyB, "same");
            var twoNew = M3FileDeltaLedger.scan(root, oneOld, List.of(new2, copyA, copyB));
            var ambiguousTargets = M3FileMoveCandidates.index(twoNew);
            ok(ambiguousTargets.candidateCount() == 0 && ambiguousTargets.ambiguityCount() == 2,
                    "one removed path, two created paths is ambiguous");

            Path unrelated = root.resolve("src/Unrelated.java");
            Files.writeString(unrelated, "different");
            var unmatched = M3FileDeltaLedger.scan(root, twoNew,
                    List.of(new2, copyA, copyB, unrelated));
            var noMatch = M3FileMoveCandidates.index(unmatched);
            ok(noMatch.candidateCount() == 0 && noMatch.ambiguityCount() == 0,
                    "no removed paths cannot match");
            ok(Arrays.equals(noMatch.unmatchedCreatedSlots(),
                    new int[] {unmatched.physicalSlot(unrelated)}), "unmatched retained");

            Path config = root.resolve("src/rename.txt");
            Files.writeString(config, "different");
            var prior = M3FileDeltaLedger.scan(root, unmatched,
                    List.of(new2, copyA, copyB, unrelated, config));
            Files.delete(config);
            Path unlike = root.resolve("src/unlike.java");
            Files.writeString(unlike, "different");
            var crossKind = M3FileDeltaLedger.scan(root, prior,
                    List.of(new2, copyA, copyB, unrelated, unlike));
            var byKind = M3FileMoveCandidates.index(crossKind);
            ok(byKind.candidateCount() == 0 && byKind.ambiguityCount() == 0,
                    "different file-kind never joins");
            ok(Arrays.equals(byKind.unmatchedCreatedSlots(),
                    new int[] {crossKind.physicalSlot(unlike)}), "cross-kind unmatched");
            try {
                M3FileMoveCandidates.index(null);
                throw new AssertionError("null snapshot admitted");
            } catch (NullPointerException expected) { checks++; }
            System.out.println("M3_FILE_MOVES_PASS; checks=" + checks);
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void ok(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
