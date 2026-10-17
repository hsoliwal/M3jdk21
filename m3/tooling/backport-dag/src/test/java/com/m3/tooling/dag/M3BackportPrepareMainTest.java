// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3BackportPrepareMainTest {
    @Test
    void explicitPathFileRunsJavaA3AndClassifiesNativeTarget() throws Exception {
        Path root = Files.createTempDirectory("m3-prepare-main-");
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

        Path paths = root.resolve("targets.txt");
        Files.writeString(
                paths,
                """
                # sorted input is not required; preparation canonicalizes order
                src/java.base/share/classes/example/Converged.java

                src/hotspot/share/example/native.cpp
                """);
        Path output = root.resolve("m3/build/prepared");

        // The package-private verified-entry test fixture exercises source behavior.
        // The public CLI independently verifies an actual source-pinned receipt.
        var summary =
                M3BackportPrepareMain.run(root, output, paths, fixtureMastery());

        assertEquals(2, summary.targets());
        assertEquals(1, summary.javaFiles());
        assertEquals(1, summary.nonJavaFiles());
        assertEquals(1, summary.changedJava());
        assertTrue(Files.isRegularFile(output.resolve("backport-preparation.tsv")));
        assertTrue(
                Files.readString(output.resolve("backport-preparation.tsv"))
                        .contains("JAVA_A3_FIXED_POINT"));
    }

    @Test
    void invalidCliInputsFailClosed() throws Exception {
        Path root = Files.createTempDirectory("m3-prepare-main-invalid-");
        Path empty = root.resolve("empty.txt");
        Files.writeString(empty, "# no targets\\n");
        Path out = root.resolve("out");

        assertThrows(IllegalArgumentException.class,
                () -> M3BackportPrepareMain.run(new String[] {}));
        // Old unsigned three-argument invocation cannot silently run A3.
        assertThrows(IllegalArgumentException.class,
                () -> M3BackportPrepareMain.run(
                        new String[]{root.toString(), out.toString(), empty.toString()}));
        assertThrows(IllegalArgumentException.class,
                () -> M3BackportPrepareMain.run(
                        root.resolve("missing-root"), out, empty, fixtureMastery()));
        assertThrows(IllegalArgumentException.class,
                () -> M3BackportPrepareMain.run(
                        root, out, root.resolve("missing.txt"), fixtureMastery()));
        assertThrows(IllegalArgumentException.class,
                () -> M3BackportPrepareMain.run(root, out, empty, fixtureMastery()));

        // Even a syntactically complete CLI may never forge a verified receipt.
        assertThrows(java.io.IOException.class,
                () -> M3BackportPrepareMain.run(new String[]{
                        root.toString(), out.toString(), empty.toString(),
                        root.resolve("missing-mastery.tsv").toString(), "0".repeat(64)}));
        assertFalse(Files.exists(out));
    }

    private static M3RecipeMasteryPortableReceipt.Verified fixtureMastery() {
        return new M3RecipeMasteryPortableReceipt.Verified(
                "1".repeat(64), "2".repeat(64), "3".repeat(64),
                "EXHAUSTIVE", 3, 1, 1, 10_000, false, 0);
    }

    @Test
    void summaryValidationFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPrepareMain.ReceiptSummary(0, 0, 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPrepareMain.ReceiptSummary(2, 1, 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPrepareMain.ReceiptSummary(1, 0, 1, 1));
    }
}
