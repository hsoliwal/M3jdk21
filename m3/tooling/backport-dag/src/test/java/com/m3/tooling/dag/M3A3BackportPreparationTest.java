// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3A3BackportPreparationTest {
    @Test
    void mixedJavaAndNativeTargetsProduceExplicitPreparationRows() throws Exception {
        Path root = Files.createTempDirectory("m3-a3-backport-");
        Path java = root.resolve("src/java.base/share/classes/example/Converged.java");
        Path nativeSource = root.resolve("src/hotspot/share/example/native.cpp");
        Files.createDirectories(java.getParent());
        Files.createDirectories(nativeSource.getParent());
        Files.writeString(
                java,
                """
                package example;
                public final class Converged {
                    private Converged() {}
                    public static int eval(int a, int b) {
                        return compute(a, b);
                    }
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """);
        Files.writeString(nativeSource, "int m3_native_fixture() { return 7; }\n");

        var receipt =
                prepare(
                        root,
                        root.resolve("m3/build/backport-preparation"),
                        List.of(
                                "src/hotspot/share/example/native.cpp",
                                "src/java.base/share/classes/example/Converged.java"));

        assertEquals(2, receipt.rows().size());
        assertEquals(1, receipt.javaFiles());
        assertEquals(1, receipt.nonJavaFiles());

        var nativeRow = receipt.rows().get(0);
        assertEquals(
                M3A3BackportPreparation.Lane.NON_JAVA_SOURCE_SEALED,
                nativeRow.lane());
        assertFalse(nativeRow.changed());
        assertFalse(nativeRow.fixedPoint());
        assertEquals(nativeRow.beforeSha(), nativeRow.preparedSha());

        var javaRow = receipt.rows().get(1);
        assertEquals(
                M3A3BackportPreparation.Lane.JAVA_A3_FIXED_POINT,
                javaRow.lane());
        assertTrue(javaRow.changed());
        assertTrue(javaRow.fixedPoint());
        assertFalse(javaRow.beforeSha().equals(javaRow.preparedSha()));

        Path candidate =
                root.resolve(
                        "m3/build/backport-preparation/a3/candidate/"
                                + "src/java.base/share/classes/example/Converged.java");
        assertTrue(Files.isRegularFile(candidate));
        String converged = Files.readString(candidate);
        assertTrue(converged.contains("m3$pureIntAtom"));
        assertTrue(converged.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(converged.contains("M3-ATOM: m3$pureIntAtom"));

        Path ledger =
                root.resolve("m3/build/backport-preparation/backport-preparation.tsv");
        assertTrue(Files.isRegularFile(ledger));
        assertTrue(Files.readString(ledger).contains("JAVA_A3_FIXED_POINT"));
        assertTrue(Files.readString(ledger).contains("NON_JAVA_SOURCE_SEALED"));
    }

    @Test
    void preparationFailsClosedForMissingEscapedOrEmptyTargets() throws Exception {
        Path root = Files.createTempDirectory("m3-a3-backport-invalid-");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        prepare(
                                root,
                                root.resolve("m3/build/out"),
                                List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        prepare(
                                root,
                                root.resolve("m3/build/out"),
                                List.of("../escape.java")));

        assertThrows(
                java.io.IOException.class,
                () ->
                        prepare(
                                root,
                                root.resolve("m3/build/out"),
                                List.of("src/java.base/share/classes/example/Missing.java")));
    }

    @Test
    void missingMasteryFailsClosedEvenForOtherwiseValidSource() throws Exception {
        Path root = Files.createTempDirectory("m3-a3-mastery-required-");
        Path source = root.resolve("src/java.base/share/classes/example/Sample.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "final class Sample {}\\n");
        Path out = root.resolve("m3/build/backport-preparation");
        assertThrows(IllegalStateException.class,
                () -> M3A3BackportPreparation.prepare(
                        root, out, List.of("src/java.base/share/classes/example/Sample.java")));
        assertThrows(NullPointerException.class,
                () -> M3A3BackportPreparation.prepare(
                        root, out, List.of("src/java.base/share/classes/example/Sample.java"), null));
        assertFalse(Files.exists(out));
    }

    private static M3A3BackportPreparation.Receipt prepare(
            Path root, Path out, List<String> paths) throws IOException {
        return M3A3BackportPreparation.prepare(root, out, paths, fixtureMastery());
    }

    // The synthetic verified receipt is used only in the existing FILE-level
    // unit test, as in A3ApplyTest. Production callers must verify actual
    // signed/source-pinned mastery through A3Mastery.verify before invocation.
    private static M3RecipeMasteryPortableReceipt.Verified fixtureMastery() {
        return new M3RecipeMasteryPortableReceipt.Verified(
                "1".repeat(64), "2".repeat(64), "3".repeat(64),
                "EXHAUSTIVE", 3, 1, 1, 10_000, false, 0);
    }

    @Test
    void rowAndReceiptValidationRejectInvalidEvidence() {
        String sha = "0".repeat(64);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3A3BackportPreparation.Row(
                                "x.java",
                                M3A3BackportPreparation.Lane.JAVA_A3_FIXED_POINT,
                                sha,
                                sha,
                                false,
                                false));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3A3BackportPreparation.Row(
                                "x.cpp",
                                M3A3BackportPreparation.Lane.NON_JAVA_SOURCE_SEALED,
                                sha,
                                sha,
                                true,
                                false));

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3A3BackportPreparation.Receipt(List.of()));

        var b =
                new M3A3BackportPreparation.Row(
                        "b.cpp",
                        M3A3BackportPreparation.Lane.NON_JAVA_SOURCE_SEALED,
                        sha,
                        sha,
                        false,
                        false);
        var a =
                new M3A3BackportPreparation.Row(
                        "a.cpp",
                        M3A3BackportPreparation.Lane.NON_JAVA_SOURCE_SEALED,
                        sha,
                        sha,
                        false,
                        false);
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3A3BackportPreparation.Receipt(List.of(b, a)));
    }
}
