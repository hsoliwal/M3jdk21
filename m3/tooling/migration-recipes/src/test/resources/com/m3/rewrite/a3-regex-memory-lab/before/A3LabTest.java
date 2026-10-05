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

final class A3LabTest {
    @TempDir
    Path root;

    @Test
    void hostileCorpusConvergesThroughAllAtomPatternSchedules() throws Exception {
        List<A3Lab.Result> results = A3Lab.run();

        assertEquals(48, A3Lab.fixtureCount());
        assertEquals(6, A3Lab.scheduleCount());
        assertEquals(288, results.size());
        assertTrue(results.stream().allMatch(A3Lab.Result::fixedPoint));
        assertTrue(results.stream().allMatch(A3Lab.Result::behaviorStable));
        assertTrue(results.stream().allMatch(A3Lab.Result::contractStable));
        assertTrue(results.stream().allMatch(A3Lab.Result::lexicalDataStable));
        assertTrue(results.stream().anyMatch(A3Lab.Result::changed));
        assertTrue(
                results.stream()
                        .filter(result -> result.subset().equals("A+P"))
                        .map(A3Lab.Result::afterSha)
                        .distinct()
                        .count()
                        <= A3Lab.fixtureCount());
    }

    @Test
    void persistedEvidenceStaysUnderM3BuildAndIsDeterministic() throws Exception {
        Path out = Path.of("m3/build/a3/lab");
        List<A3Lab.Result> first = A3Lab.write(root, out);
        String firstTsv = Files.readString(root.resolve(out).resolve("results.tsv"));

        List<A3Lab.Result> replay = A3Lab.write(root, out);
        String secondTsv = Files.readString(root.resolve(out).resolve("results.tsv"));

        assertEquals(first, replay);
        assertEquals(firstTsv, secondTsv);
        assertTrue(firstTsv.startsWith("fixture\tschedule\tsubset\t"));
        assertTrue(firstTsv.contains("\tA>P\tA+P\t"));
        assertTrue(firstTsv.contains("\tP>A\tA+P\t"));
        assertTrue(firstTsv.contains("\tA>P>A\tA+P\t"));
        assertTrue(firstTsv.contains("\tP>A>P\tA+P\t"));
    }

    @Test
    void rootWriteGuardStillRejectsLabOutputOutsideM3Build() {
        assertThrows(
                IllegalArgumentException.class,
                () -> A3Lab.write(root, Path.of("src/a3-lab")));
    }

    @Test
    void patternizeOnlyIsHonestNoChangeWhileAtomizeChanges() throws Exception {
        List<A3Lab.Result> results = A3Lab.run();

        assertFalse(
                results.stream()
                        .filter(result -> result.schedule().equals("P"))
                        .anyMatch(A3Lab.Result::changed));
        assertTrue(
                results.stream()
                        .filter(result -> result.schedule().equals("A"))
                        .allMatch(A3Lab.Result::changed));
        assertEquals(
                results.stream()
                        .filter(result -> result.schedule().equals("A>P"))
                        .map(A3Lab.Result::afterSha)
                        .toList(),
                results.stream()
                        .filter(result -> result.schedule().equals("A>P>A"))
                        .map(A3Lab.Result::afterSha)
                        .toList());
        assertEquals(
                results.stream()
                        .filter(result -> result.schedule().equals("P>A"))
                        .map(A3Lab.Result::afterSha)
                        .toList(),
                results.stream()
                        .filter(result -> result.schedule().equals("P>A>P"))
                        .map(A3Lab.Result::afterSha)
                        .toList());
    }
}
