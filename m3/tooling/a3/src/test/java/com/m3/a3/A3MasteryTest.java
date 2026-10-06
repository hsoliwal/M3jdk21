// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3MasteryTest {

    @TempDir
    Path temp;

    @Test
    void roundTripsPortableV6AndPinsExternalRoot() throws Exception {
        A3Mastery.Receipt receipt = receipt();
        String tsv = receipt.toTsv();

        assertEquals(receipt, A3Mastery.parse(tsv));
        assertEquals(A3Mastery.REGEX_CASES, receipt.regexCaseCount());
        assertEquals("EXHAUSTIVE", receipt.scheduleCoverage());
        assertFalse(receipt.sourceMutationAuthority());
        assertFalse(receipt.semanticAuthority());
        assertFalse(receipt.donorSourceCopyAuthority());
        assertFalse(receipt.replacementAuthority());
        assertFalse(receipt.mergeAuthority());
        assertFalse(receipt.promotionAuthority());

        Path file = temp.resolve("mastery.tsv");
        Files.writeString(file, tsv);
        assertEquals(
                receipt,
                A3Mastery.read(temp, file, receipt.root()));
        assertThrows(
                IllegalStateException.class,
                () -> A3Mastery.read(temp, file, "f".repeat(64)));
    }

    @Test
    void rejectsAuthorityCoverageCountBooleanAndRootDrift() {
        String tsv = receipt().toTsv();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.parse(
                                tsv.replace(
                                        "sourceMutationAuthority\tfalse",
                                        "sourceMutationAuthority\ttrue")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.parse(
                                tsv.replace(
                                        "scheduleCoverage\tEXHAUSTIVE",
                                        "scheduleCoverage\tPAIRWISE")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.parse(
                                tsv.replace(
                                        "regexCaseCount\t10000",
                                        "regexCaseCount\t9999")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.parse(
                                tsv.replace(
                                        "jniRequired\tfalse",
                                        "jniRequired\tFALSE")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.parse(
                                tsv.replace(
                                        "root\t" + receipt().root(),
                                        "root\t" + "0".repeat(64))));
    }

    @Test
    void optionalJniEvidenceIsRootBound() {
        A3Mastery.Receipt receipt =
                new A3Mastery.Receipt(
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
                        2,
                        4,
                        "c".repeat(64),
                        "d".repeat(64),
                        A3Mastery.REGEX_CASES,
                        true,
                        "e".repeat(64),
                        16,
                        true,
                        "");

        assertTrue(receipt.jniRequired());
        assertEquals(receipt, A3Mastery.parse(receipt.toTsv()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Mastery.Receipt(
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
                                2,
                                4,
                                "c".repeat(64),
                                "d".repeat(64),
                                A3Mastery.REGEX_CASES,
                                true,
                                "",
                                0,
                                true,
                                ""));
    }

    static A3Mastery.Receipt receipt() {
        return new A3Mastery.Receipt(
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
                2,
                4,
                "c".repeat(64),
                "d".repeat(64),
                A3Mastery.REGEX_CASES,
                false,
                "",
                0,
                true,
                "");
    }
}
