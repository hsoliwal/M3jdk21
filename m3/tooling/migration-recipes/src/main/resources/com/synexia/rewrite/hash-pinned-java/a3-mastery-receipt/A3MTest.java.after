// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class A3MTest {
    @TempDir
    Path root;

    @Test
    void receiptBindsCurrentPinsAndLabEvidenceWithoutRerunningLab() throws Exception {
        materializePinFixture();
        List<A3M.Pin> pins = A3M.currentPins(root);
        String pinText = A3M.renderPins(pins);
        String labText =
                "fixture\tschedule\tsubset\tbeforeSha\tafterSha\tapplications\tcompiles"
                        + "\tchanged\tfixedPoint\tbehaviorStable\tcontractStable"
                        + "\tlexicalDataStable\tregexMatrixStable\n"
                        + "0\tA\tA\t"
                        + "1".repeat(64)
                        + "\t"
                        + "2".repeat(64)
                        + "\t1\t1\ttrue\ttrue\ttrue\ttrue\ttrue\ttrue\n";

        A3M.Receipt receipt =
                new A3M.Receipt(
                        A3M.SCHEMA,
                        "",
                        A3Fs.sha(pinText),
                        A3Fs.sha(labText),
                        1,
                        1,
                        1,
                        1,
                        1,
                        1,
                        1);
        Path evidence = root.resolve("m3/build/a3/mastery");
        Files.createDirectories(evidence.resolve("lab"));
        Files.writeString(evidence.resolve("pins.tsv"), pinText);
        Files.writeString(evidence.resolve("lab/results.tsv"), labText);
        Files.writeString(evidence.resolve("receipt.tsv"), A3MCodec.render(receipt));

        assertEquals(
                receipt,
                A3M.requireCurrent(
                        root,
                        Path.of("m3/build/a3/mastery/receipt.tsv")));

        String drift = A3M.pinPaths().getFirst();
        Files.writeString(root.resolve(drift), "drift\n");
        IllegalStateException failure =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                A3M.requireCurrent(
                                        root,
                                        Path.of("m3/build/a3/mastery/receipt.tsv")));
        assertTrue(failure.getMessage().contains("pins are stale"));
    }

    @Test
    void codecIsStrictAndRootIsContentAddressed() {
        A3M.Receipt receipt =
                new A3M.Receipt(
                        A3M.SCHEMA,
                        "",
                        "a".repeat(64),
                        "b".repeat(64),
                        2,
                        3,
                        6,
                        6,
                        6,
                        1,
                        6);
        String encoded = A3MCodec.render(receipt);

        assertEquals(receipt, A3MCodec.parse(encoded));
        assertEquals(64, receipt.root().length());
        assertThrows(
                IllegalArgumentException.class,
                () -> A3MCodec.parse(encoded.replace(A3M.SCHEMA, "M3-A3-MASTERY/0")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3M.Receipt(
                                A3M.SCHEMA,
                                "",
                                "a".repeat(64),
                                "b".repeat(64),
                                2,
                                3,
                                5,
                                6,
                                6,
                                1,
                                5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new A3M.Pin("./not-canonical", "a".repeat(64)));
    }

    @Test
    void outputGuardStillRejectsMasteryEvidenceOutsideM3Build() {
        assertThrows(
                IllegalArgumentException.class,
                () -> A3M.write(root, Path.of("test/a3-mastery")));
    }

    private void materializePinFixture() throws Exception {
        for (String relative : A3M.pinPaths()) {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "pin:" + relative + "\n");
        }
    }
}
