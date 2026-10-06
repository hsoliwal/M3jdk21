// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3GateTest {

    @TempDir
    Path root;

    @Test
    void strictV6ReceiptRoundTripsAndRequiresCallerPinnedRoot() throws Exception {
        A3Gate.Receipt receipt = fixture(false);
        Path file = root.resolve("mastery-v6.tsv");
        Files.writeString(file, receipt.toTsv());

        assertEquals(receipt, A3Gate.parse(receipt.toTsv()));
        assertEquals(receipt, A3Gate.admit(file, receipt.root()));
        assertFalse(receipt.sourceMutationAuthority());
        assertFalse(receipt.promotionAuthority());

        assertThrows(
                IllegalStateException.class,
                () -> A3Gate.admit(file, "f".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Gate.parse(
                                receipt.toTsv()
                                        .replace(
                                                "sourceMutationAuthority\tfalse",
                                                "sourceMutationAuthority\ttrue")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Gate.parse(
                                receipt.toTsv()
                                        .replace("complete\ttrue", "complete\tfalse")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Gate.parse(
                                receipt.toTsv()
                                        .replace("jniRequired\tfalse", "jniRequired\tFALSE")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Gate.parse(
                                receipt.toTsv()
                                        .replace(
                                                "regexCaseCount\t10000",
                                                "regexCaseCount\t9999")));
    }

    @Test
    void masteredApplyDelegatesToExistingA3ApplyWithoutWritingJdkSource() throws Exception {
        String relative =
                "src/java.base/share/classes/example/Mastered.java";
        String before =
                """
                package example;
                final class Mastered {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;
        Path source = root.resolve(relative);
        Files.createDirectories(source.getParent());
        Files.writeString(source, before);

        A3Gate.Receipt receipt = fixture(false);
        Path receiptFile = root.resolve("m3/build/evidence/mastery-v6.tsv");
        Files.createDirectories(receiptFile.getParent());
        Files.writeString(receiptFile, receipt.toTsv());

        A3Gate.Run run =
                A3Gate.apply(
                        root,
                        Path.of("m3/build/a3/mastered"),
                        List.of(relative),
                        receiptFile,
                        receipt.root());

        assertEquals(receipt, run.mastery());
        assertEquals(1, run.candidates().size());
        assertEquals(relative, run.candidates().getFirst().path());
        assertTrue(run.candidates().getFirst().fixedPoint());
        assertEquals(before, Files.readString(source));

        Path candidate =
                root.resolve("m3/build/a3/mastered/candidate")
                        .resolve(relative);
        assertTrue(Files.isRegularFile(candidate));
        String after = Files.readString(candidate);
        assertTrue(after.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(after.contains("M3-ATOM: m3$pureIntAtom"));

        assertEquals(
                receipt.toTsv(),
                Files.readString(
                        root.resolve(
                                "m3/build/a3/mastered/mastery-v6.tsv")));
    }

    @Test
    void jniEvidencePresenceIsExact() {
        A3Gate.Receipt nativeReceipt = fixture(true);
        assertTrue(nativeReceipt.jniRequired());
        assertEquals("9".repeat(64), nativeReceipt.nativeParityRoot());
        assertEquals(552, nativeReceipt.nativeParityPairCount());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Gate.Receipt(
                                "1".repeat(64),
                                "2".repeat(64),
                                "3".repeat(64),
                                "4".repeat(64),
                                "5".repeat(64),
                                "6".repeat(64),
                                "EXHAUSTIVE",
                                "7".repeat(64),
                                "8".repeat(64),
                                "9".repeat(64),
                                "a".repeat(64),
                                "b".repeat(64),
                                3,
                                1,
                                1,
                                "c".repeat(64),
                                "d".repeat(64),
                                10_000,
                                false,
                                "e".repeat(64),
                                1,
                                ""));
    }

    private static A3Gate.Receipt fixture(boolean jni) {
        return new A3Gate.Receipt(
                "1".repeat(64),
                "2".repeat(64),
                "3".repeat(64),
                "4".repeat(64),
                "5".repeat(64),
                "6".repeat(64),
                "EXHAUSTIVE",
                "7".repeat(64),
                "8".repeat(64),
                "9".repeat(64),
                "a".repeat(64),
                "b".repeat(64),
                12,
                2,
                4,
                "c".repeat(64),
                "d".repeat(64),
                10_000,
                jni,
                jni ? "9".repeat(64) : "",
                jni ? 552 : 0,
                "");
    }
}
