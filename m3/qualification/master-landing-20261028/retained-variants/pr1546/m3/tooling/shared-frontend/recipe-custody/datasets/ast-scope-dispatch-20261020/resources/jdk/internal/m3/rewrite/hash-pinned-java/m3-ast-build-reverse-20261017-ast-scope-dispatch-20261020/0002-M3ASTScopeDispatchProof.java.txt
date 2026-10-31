/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.compiletime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Direct M3JDK21 scope-routing proof over the original MIndexAST-derived
 * leaf hierarchy. No compiler/parser is started and no source is reparsed.
 */
public final class M3ASTScopeDispatchProof {
    private static long checks;

    private M3ASTScopeDispatchProof() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("unexpected failure " + problem, problem);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static M3ASTLeafIndex.FileFacts file(String project, String module,
            String pack, String path, int[] atoms, long[] states) {
        return new M3ASTLeafIndex.FileFacts(project, module, pack, path,
                "a".repeat(64), atoms, states);
    }

    private static M3ASTLeafIndex fixture() {
        return M3ASTLeafIndex.build("repository",
                file("P2", "M1", "pkg1", "src/Main.java",
                        new int[] {13}, new long[] {M3ASTLeafIndex.TOMBSTONE}),
                file("P1", "M2", "pkg1", "src/Main.java",
                        new int[] {12}, new long[] {Long.MIN_VALUE}),
                file("P1", "M1", "pkg2", "src/Other.java",
                        new int[0], new long[0]),
                file("P1", "M1", "pkg1", "src/Main.java",
                        new int[] {7, 7}, new long[] {
                                M3ASTLeafIndex.LIVE | M3ASTLeafIndex.DIRTY,
                                M3ASTLeafIndex.LIVE}));
    }

    private static void existingScopes() {
        var index = fixture();
        int repository = index.repositoryRow();
        int project = index.projectRow("P1");
        int module = index.moduleRow("P1", "M1");
        int pack = index.packageRow("P1", "M1", "pkg1");
        int main = index.fileRow("P1", "M1", "src/Main.java");
        int other = index.fileRow("P1", "M1", "src/Other.java");
        int second = index.fileRow("P1", "M2", "src/Main.java");
        int third = index.fileRow("P2", "M1", "src/Main.java");
        check(project >= 0 && index.level(project) == M3ASTLeafIndex.PROJECT,
                "existing project row");
        check(module >= 0 && index.level(module) == M3ASTLeafIndex.MODULE,
                "existing module row");
        check(pack >= 0 && index.level(pack) == M3ASTLeafIndex.PACKAGE,
                "existing package row");
        check(index.parent(main) == pack && index.parent(pack) == module,
                "original hierarchy identities");
        check(index.parent(module) == project && index.parent(project) == repository,
                "original project/repository ancestry");
        check(main != second && second != third && main != third,
                "module/project qualify identical relative paths");
        check(index.fileCount(module) == 2 && index.atomCount(module) == 2,
                "module selects exactly two files and two atom occurrences");
        check(index.atomCount(other) == 0, "empty original source remains selectable");
        check(index.projectRow("missing") == -1
                && index.moduleRow("P1", "missing") == -1
                && index.packageRow("P1", "M1", "missing") == -1
                && index.fileRow("P1", "M1", "missing.java") == -1,
                "missing identities never trigger a parser");
        check(index.fileRow("P1", "M1", "src\\Main.java") == main,
                "existing normalized path owner");
        rejects(IllegalArgumentException.class,
                () -> index.fileRow("P1", "M1", "../escape.java"));

        ArrayList<Integer> fileRows = new ArrayList<>();
        ArrayList<Integer> factRows = new ArrayList<>();
        int visited = index.dispatchFiles(module, (fileRow, factsRow) -> {
            fileRows.add(fileRow);
            factRows.add(factsRow);
            check(index.childAt(fileRow, 0) == factsRow,
                    "exact original L0 source identity");
            check(index.parent(factsRow) == fileRow,
                    "original file-to-facts relationship");
        });
        check(visited == 2 && fileRows.equals(List.of(main, other)),
                "module dispatch selects only existing file rows");
        check(factRows.get(1) == index.childAt(other, 0), "zero-atom file remains visible");

        int packageVisits = index.dispatchFiles(pack,
                (fileRow, factsRow) -> check(fileRow == main, "only package file"));
        check(packageVisits == 1, "package scope count");
        check(index.dispatchFiles(second,
                (fileRow, factsRow) -> check(fileRow == second, "one isolated file")) == 1,
                "file scope count");
        check(index.dispatchFiles(project, (fileRow, factsRow) -> { }) == 3,
                "project scope file count");
        check(index.dispatchFiles(repository, (fileRow, factsRow) -> { }) == 4,
                "repository scope file count");
        rejects(IllegalArgumentException.class,
                () -> index.dispatchFiles(index.childAt(main, 0), (f, a) -> { }));
        rejects(NullPointerException.class, () -> index.dispatchFiles(module, null));

        var again = M3ASTLeafIndex.build("repository",
                file("P1", "M1", "pkg1", "src/Main.java",
                        new int[] {7, 7}, new long[] {
                                M3ASTLeafIndex.LIVE | M3ASTLeafIndex.DIRTY,
                                M3ASTLeafIndex.LIVE}),
                file("P1", "M1", "pkg2", "src/Other.java", new int[0], new long[0]),
                file("P1", "M2", "pkg1", "src/Main.java",
                        new int[] {12}, new long[] {Long.MIN_VALUE}),
                file("P2", "M1", "pkg1", "src/Main.java",
                        new int[] {13}, new long[] {M3ASTLeafIndex.TOMBSTONE}));
        check(index.sha256(repository).equals(again.sha256(again.repositoryRow())),
                "input order does not change source identity");
        check(index.fileRow("P1", "M1", "src/Main.java")
                == again.fileRow("P1", "M1", "src/Main.java"),
                "same original row coordinates");
    }

    private static void originalAtomOccurrences() {
        var index = fixture();
        int root = index.repositoryRow();
        int main = index.fileRow("P1", "M1", "src/Main.java");
        List<Integer> handles = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        List<Long> stateWords = new ArrayList<>();
        int visited = index.dispatchAtoms(root, M3ASTLeafIndex.LIVE,
                M3ASTLeafIndex.TOMBSTONE, (atom, physical, state) -> {
                    handles.add(atom);
                    positions.add(physical);
                    stateWords.add(state);
                });
        check(visited == 2 && handles.equals(List.of(7, 7)),
                "same AST handle remains two distinct source occurrences");
        check(positions.equals(List.of(0, 1))
                && stateWords.equals(List.of(3L, 1L)),
                "original primitive physical offsets and masks preserved");
        check(index.dispatchAtoms(main, Long.MIN_VALUE, 0, (atom, offset, state) -> {
            throw new AssertionError("unexpected atom in isolated file");
        }) == 0, "mask match respects exact selected file");

        List<Integer> high = new ArrayList<>();
        check(index.dispatchAtoms(root, Long.MIN_VALUE, 0,
                (atom, offset, state) -> high.add(offset)) == 1
                && high.equals(List.of(2)),
                "full-width long signed-bit selection");
        int[] legacy = index.selectOffsets(root, M3ASTLeafIndex.LIVE,
                M3ASTLeafIndex.TOMBSTONE);
        check(Arrays.equals(legacy, positions.stream().mapToInt(Integer::intValue).toArray()),
                "new dispatch agrees with original index selector");
        int[] observed = {0};
        rejects(IllegalArgumentException.class, () -> index.dispatchAtoms(root,
                M3ASTLeafIndex.LIVE, M3ASTLeafIndex.LIVE,
                (atom, offset, state) -> observed[0]++));
        check(observed[0] == 0, "invalid mask refuses before dispatch");
        rejects(NullPointerException.class, () -> index.dispatchAtoms(root, 0, 0, null));
        rejects(IndexOutOfBoundsException.class,
                () -> index.dispatchAtoms(index.rowCount(), 0, 0, (atom, p, s) -> { }));

        RuntimeException originalFailure = new RuntimeException("owner failure");
        rejects(RuntimeException.class, () -> index.dispatchAtoms(root, 0, 0,
                (atom, offset, state) -> {
                    throw originalFailure;
                }));
        check(index.atomCount(root) == 4, "foreign callback never mutates original index");
    }

    private static void randomDifferential() {
        Random random = new Random(0x4d334153544cL);
        for (int round = 0; round < 140; round++) {
            var files = new M3ASTLeafIndex.FileFacts[1 + random.nextInt(35)];
            for (int file = 0; file < files.length; file++) {
                int length = random.nextInt(12);
                int[] ids = new int[length];
                long[] states = new long[length];
                for (int i = 0; i < length; i++) {
                    ids[i] = random.nextInt(9);
                    states[i] = random.nextLong();
                }
                files[file] = file("Project" + random.nextInt(3),
                        "Module" + random.nextInt(4),
                        "pkg" + random.nextInt(5),
                        "src/Unique" + file + ".java", ids, states);
            }
            var index = M3ASTLeafIndex.build("random", files);
            long required = round % 2 == 0 ? 1L : Long.MIN_VALUE;
            long forbidden = round % 2 == 0 ? 2L : 0x100L;
            for (int row = 0; row < index.rowCount(); row++) {
                int[] expected = index.selectOffsets(row, required, forbidden);
                List<Integer> actual = new ArrayList<>();
                int count = index.dispatchAtoms(row, required, forbidden,
                        (atom, offset, state) -> {
                            check(atom == index.atomIdAtPhysical(offset),
                                    "original parser-owned atom identity");
                            check(state == index.stateAtPhysical(offset),
                                    "original state lane identity");
                            actual.add(offset);
                        });
                check(count == expected.length, "constant-space dispatch count");
                for (int n = 0; n < expected.length; n++) {
                    check(expected[n] == actual.get(n), "original encounter order");
                }
                if (index.level(row) >= M3ASTLeafIndex.FILE) {
                    int[] filesSeen = {0};
                    int total = index.dispatchFiles(row, (f, facts) -> filesSeen[0]++);
                    check(total == filesSeen[0] && total == index.fileCount(row),
                            "scope dispatch matches original file hierarchy");
                }
            }
        }
    }

    public static void main(String[] args) {
        existingScopes();
        originalAtomOccurrences();
        randomDifferential();
        System.out.println("PASS M3ASTScopeDispatchProof checks=" + checks);
    }
}
