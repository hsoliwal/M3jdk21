// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.smoke;

import com.m3.ast.M3ASTHierarchy;
import com.m3.collections.M3LongIndexProjection;
import com.m3.collections.M3IndexMaskCompactor;
import java.util.Arrays;
import com.m3.eclipse.core.M3EclipseCore;
import com.m3.file.M3File;
import com.m3.text.M3StringPiece;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

/** Standalone Java21 actual JPMS/JMOD contract tests; never included in the product. */
public final class M3EclipseCoreSelfTest {
    private static int assertions;

    private M3EclipseCoreSelfTest() { }

    public static void main(String[] args) throws IOException {
        M3EclipseCore m3 = ServiceLoader.load(M3EclipseCore.class)
                .findFirst().orElseThrow(() -> new AssertionError("M3 Eclipse provider missing"));

        String edge = "A\ud800x\udc00\ud83d\ude42";
        M3StringPiece piece = m3.copyUtf16(edge);
        require(edge.equals(piece.flatten()), "UTF-16 with unmatched surrogate must round-trip");
        require(m3.retainText(piece) == piece, "existing M3 text backing identity");
        require(m3.utf16(edge).length() == edge.length(), "M3 UTF-16 array facts");
        require(m3.utf16(edge).javaHashCode() == edge.hashCode(), "UTF-16 Java hash parity");

        long[] values = {5L, Long.MIN_VALUE, 5L, 0L};
        M3LongIndexProjection projection = m3.index(values);
        values[0] = 9L;
        require(projection.get(0) == 5L, "M3 index source snapshot isolation");
        M3LongIndexProjection sorted = projection.sortedAscending();
        require(sorted.size() == 4 && sorted.get(0) == Long.MIN_VALUE,
                "M3 signed stable index order");
        require(sorted.physicalIndex(2) == 0 && sorted.physicalIndex(3) == 2,
                "stable equal-valued physical occurrences");
        require(sorted.slice(1, 2).size() == 2, "M3 offset slice");
        int[] rowIds = {7, 0, 7, -1};
        byte[] stateBytes = {1, 3, 0, 1};
        int[] javaRows = M3IndexMaskCompactor.select(rowIds, stateBytes, 1,
                M3IndexMaskCompactor.Backend.JAVA);
        require(Arrays.equals(javaRows, new int[]{7, 0, -1}),
                "M3 Java index-mask encounter order and duplicates");
        if (M3IndexMaskCompactor.nativeAvailable()) {
            int[] nativeRows = M3IndexMaskCompactor.select(rowIds, stateBytes, 1,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            require(Arrays.equals(javaRows, nativeRows), "existing M3 JNI parity");
        } else {
            expect(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.select(
                    rowIds, stateBytes, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                    "missing M3 JNI must fail explicitly");
        }

        M3ASTHierarchy.FileFact file = new M3ASTHierarchy.FileFact("p", "m", "pkg",
                "p/src/C.java", "a".repeat(64), "b".repeat(64),
                M3ASTHierarchy.LIST, new int[] {7}, new byte[] {3});
        M3ASTHierarchy ast = m3.hierarchy("repo", List.of(file));
        require(ast.totalFiles() == 1 && ast.totalOccurrences() == 1,
                "M3 AST L0/L5 hierarchy delegate");
        require(ast.atomHandle(0) == 7 && ast.stateMask(0) == 3,
                "M3 AST aligned masks and atom handles");

        Path directory = Files.createTempDirectory("m3-eclipse-jmod-");
        Path filePath = directory.resolve("payload.txt");
        try {
            Files.writeString(filePath, "abc", StandardCharsets.UTF_8);
            M3File resource = m3.file(filePath);
            require(resource.path() == filePath, "preserve original Path/provider ownership");
            require(resource.metadata().regularFile(), "NIO regular file metadata");
            require(new String(resource.readAllBytes(3), StandardCharsets.UTF_8).equals("abc"),
                    "bounded regular-file read");
            expect(IOException.class, () -> resource.readAllBytes(2), "reject over-budget read");
            expect(IllegalArgumentException.class, () -> resource.readAllBytes(-1),
                    "reject invalid byte budget");
            expect(IOException.class, () -> m3.file(directory).readAllBytes(9),
                    "refuse reading a directory as a regular file");
            try {
                Path link = directory.resolve("link");
                Files.createSymbolicLink(link, filePath);
                require(m3.file(link).metadata().symbolicLink(), "expose symlink metadata");
                expect(IOException.class, () -> m3.file(link).readAllBytes(3),
                        "nofollow must reject symlink reads");
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException unsupported) {
                // The actual provider or host may disallow symbolic links.
            }
            Files.writeString(filePath, "12345", StandardCharsets.UTF_8);
            expect(IOException.class, () -> resource.readAllBytes(3),
                    "external file changes must be observed, not cached");
            Files.delete(filePath);
            expect(IOException.class, resource::metadata, "missing file propagates provider error");
        } finally {
            try (var paths = Files.list(directory)) {
                for (Path member : paths.toList()) Files.deleteIfExists(member);
            }
            Files.delete(directory);
        }
        System.out.println("M3_ECLIPSE_OPTIONAL_JMOD_REAL_JPMS_PASS assertions=" + assertions);
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends Throwable> void expect(
            Class<T> type, ThrowingRunnable action, String message) {
        assertions++;
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(message + ": wrong exception " + thrown, thrown);
        }
        throw new AssertionError(message + ": no exception");
    }

    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}
