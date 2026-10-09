// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                M3A3BackportPreparation.prepare(
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

        var phases = receipt.phaseProofs();
        assertEquals(
                List.of(
                        M3A3BackportPreparation.ProofPhase.ATOMIZATION,
                        M3A3BackportPreparation.ProofPhase.PATTERN_IOP,
                        M3A3BackportPreparation.ProofPhase.DOCUMENTATION,
                        M3A3BackportPreparation.ProofPhase.FIXED_POINT),
                phases.stream()
                        .map(M3A3BackportPreparation.PhaseProof::phase)
                        .toList());
        assertTrue(phases.stream().allMatch(M3A3BackportPreparation.PhaseProof::applicable));
        assertTrue(phases.stream().allMatch(proof -> proof.javaFiles() == 1));
        assertTrue(phases.stream().allMatch(proof -> proof.root().matches("[0-9a-f]{64}")));
        assertEquals(
                receipt.requirePhase(M3A3BackportPreparation.ProofPhase.FIXED_POINT),
                phases.getLast());

        Path phaseLedger =
                root.resolve("m3/build/backport-preparation/backport-preparation-phases.tsv");
        assertTrue(Files.isRegularFile(phaseLedger));
        String phaseText = Files.readString(phaseLedger);
        assertTrue(phaseText.contains("ATOMIZATION"));
        assertTrue(phaseText.contains("PATTERN_IOP"));
        assertTrue(phaseText.contains("DOCUMENTATION"));
        assertTrue(phaseText.contains("FIXED_POINT"));
    }

    @Test
    void nativeOnlyPreparationEmitsExplicitNonApplicablePhaseProofs() throws Exception {
        Path root = Files.createTempDirectory("m3-a3-backport-native-only-");
        Path nativeSource = root.resolve("src/hotspot/share/example/native.cpp");
        Files.createDirectories(nativeSource.getParent());
        Files.writeString(nativeSource, "int m3_native_fixture() { return 7; }\n");

        var receipt =
                M3A3BackportPreparation.prepare(
                        root,
                        root.resolve("m3/build/backport-preparation"),
                        List.of("src/hotspot/share/example/native.cpp"));

        assertEquals(0, receipt.javaFiles());
        assertEquals(1, receipt.nonJavaFiles());
        assertEquals(4, receipt.phaseProofs().size());
        assertTrue(receipt.phaseProofs().stream()
                .allMatch(proof -> !proof.applicable() && proof.javaFiles() == 0));
        assertTrue(receipt.phaseProofs().stream()
                .allMatch(proof -> proof.root().matches("[0-9a-f]{64}")));
    }

    @Test
    void preparationFailsClosedForMissingEscapedOrEmptyTargets() throws Exception {
        Path root = Files.createTempDirectory("m3-a3-backport-invalid-");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3A3BackportPreparation.prepare(
                                root,
                                root.resolve("m3/build/out"),
                                List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3A3BackportPreparation.prepare(
                                root,
                                root.resolve("m3/build/out"),
                                List.of("../escape.java")));

        assertThrows(
                java.io.IOException.class,
                () ->
                        M3A3BackportPreparation.prepare(
                                root,
                                root.resolve("m3/build/out"),
                                List.of("src/java.base/share/classes/example/Missing.java")));
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
