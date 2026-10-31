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
        String labText = syntheticLab();
        A3MCodec.LabSummary summary = A3MCodec.summarizeLab(labText);

        A3M.Receipt receipt =
                new A3M.Receipt(
                        A3M.SCHEMA,
                        "",
                        A3Fs.sha(pinText),
                        A3Fs.sha(labText),
                        summary.fixtureCount(),
                        summary.scheduleCount(),
                        summary.resultCount(),
                        summary.applicationCount(),
                        summary.compileCount(),
                        summary.changedCount(),
                        summary.regexStableCount());
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
        assertEquals("A3M source/dependency pins are stale", failure.getMessage());
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
        String qualified = syntheticLab();
        assertThrows(
                IllegalArgumentException.class,
                () -> A3MCodec.summarizeLab(
                        qualified.replaceFirst("\\ttrue\\ttrue\\ttrue\\ttrue\\ttrue",
                                "\\ttrue\\tfalse\\ttrue\\ttrue\\ttrue")));
        String firstRow = qualified.lines().skip(1).findFirst().orElseThrow();
        assertThrows(
                IllegalArgumentException.class,
                () -> A3MCodec.summarizeLab(qualified + firstRow + "\n"));
    }

    @Test
    void outputGuardStillRejectsMasteryEvidenceOutsideM3Build() {
        assertThrows(
                IllegalArgumentException.class,
                () -> A3M.write(root, Path.of("test/a3-mastery")));
    }

    private static String syntheticLab() {
        String[] schedules = {
            "A", "P", "D", "A>P", "P>A", "A>D", "D>A", "P>D", "D>P",
            "A>P>D", "A>D>P", "P>A>D", "P>D>A", "D>A>P", "D>P>A", "A>P>A", "P>A>P"
        };
        StringBuilder out =
                new StringBuilder(
                        "fixture\tschedule\tsubset\tbeforeSha\tafterSha\tapplications\tcompiles"
                                + "\tchanged\tfixedPoint\tbehaviorStable\tcontractStable"
                                + "\tlexicalDataStable\tregexMatrixStable\n");
        for (int fixture = 0; fixture < 48; fixture++) {
            for (String schedule : schedules) {
                out.append(fixture)
                        .append('\t')
                        .append(schedule)
                        .append('\t')
                        .append(schedule.contains("D") ? "A+D+P" : "A+P")
                        .append('\t')
                        .append("1".repeat(64))
                        .append('\t')
                        .append("2".repeat(64))
                        .append("\t1\t1\t")
                        .append(schedule.contains("A"))
                        .append("\ttrue\ttrue\ttrue\ttrue\ttrue\n");
            }
        }
        return out.toString();
    }

    private void materializePinFixture() throws Exception {
        for (String relative : A3M.pinPaths()) {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "pin:" + relative + "\n");
        }
    }
}
