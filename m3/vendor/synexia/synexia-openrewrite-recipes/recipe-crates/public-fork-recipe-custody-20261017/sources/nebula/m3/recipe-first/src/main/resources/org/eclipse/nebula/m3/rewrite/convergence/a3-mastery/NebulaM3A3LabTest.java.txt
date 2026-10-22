// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class NebulaM3A3LabTest {
    @TempDir
    Path temporary;

    @Test
    void hostileCorpusConvergesThroughEveryAtomPatternSchedule() throws Exception {
        List<NebulaM3A3Lab.Result> results = NebulaM3A3Lab.run();

        assertEquals(48, NebulaM3A3Lab.fixtureCount());
        assertEquals(6, NebulaM3A3Lab.scheduleCount());
        assertEquals(288, results.size());
        assertTrue(results.stream().allMatch(NebulaM3A3Lab.Result::fixedPoint));
        assertTrue(results.stream().allMatch(NebulaM3A3Lab.Result::behaviorStable));
        assertTrue(results.stream().allMatch(NebulaM3A3Lab.Result::contractStable));
        assertTrue(results.stream().allMatch(NebulaM3A3Lab.Result::lexicalDataStable));
        assertTrue(results.stream().anyMatch(NebulaM3A3Lab.Result::changed));
    }

    @Test
    void redundantMixedSchedulesConvergeToSameNormalForm() throws Exception {
        List<NebulaM3A3Lab.Result> results = NebulaM3A3Lab.run();

        assertEquals(after(results, "A>P"), after(results, "A>P>A"));
        assertEquals(after(results, "P>A"), after(results, "P>A>P"));
        assertEquals(after(results, "A>P"), after(results, "P>A"));
    }

    @Test
    void persistedEvidenceIsDeterministic() throws Exception {
        Path out = temporary.resolve("target/a3-mastery");
        List<NebulaM3A3Lab.Result> first = NebulaM3A3Lab.write(out);
        String firstTsv = Files.readString(out.resolve("results.tsv"));

        List<NebulaM3A3Lab.Result> replay = NebulaM3A3Lab.write(out);
        String secondTsv = Files.readString(out.resolve("results.tsv"));

        assertEquals(first, replay);
        assertEquals(firstTsv, secondTsv);
        assertTrue(firstTsv.startsWith("fixture\tschedule\toperationSet\t"));
        assertTrue(firstTsv.contains("\tA>P>A\tA+P\t"));
        assertTrue(firstTsv.contains("\tP>A>P\tA+P\t"));
    }

    @Test
    void sameNameParserResetMatchesFreshAttributionAndRecoversAfterMalformedInput() throws Exception {
        var method = NebulaM3A3Lab.class.getDeclaredMethod("parse", JavaParser.class, String.class);
        method.setAccessible(true);
        var reused = JavaParser.fromJavaVersion().build();
        for (String type : List.of("int", "String", "long", "java.util.List<Integer>",
                "java.util.List<String>", "int")) {
            String source = "package org.eclipse.nebula.m3.lab; class Subject { "
                    + type + " value(" + type + " input) { return input; } }";
            var cached = (J.CompilationUnit) method.invoke(null, reused, source);
            var fresh = (J.CompilationUnit) method.invoke(null, JavaParser.fromJavaVersion().build(), source);
            var cachedType = cached.getClasses().getFirst().getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast)
                    .findFirst().orElseThrow().getMethodType();
            var freshType = fresh.getClasses().getFirst().getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast)
                    .findFirst().orElseThrow().getMethodType();
            org.junit.jupiter.api.Assertions.assertNotNull(cachedType);
            org.junit.jupiter.api.Assertions.assertNotNull(freshType);
            assertEquals(freshType.getReturnType().toString(), cachedType.getReturnType().toString());
            assertEquals(freshType.getParameterTypes().toString(), cachedType.getParameterTypes().toString());
            assertEquals(source, cached.printAll());
        }
        var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> method.invoke(null, reused, "package org.eclipse.nebula.m3.lab; class Subject {"));
        assertTrue(failure.getCause() instanceof IllegalArgumentException);
        String valid = "package org.eclipse.nebula.m3.lab; class Subject { int value() { return 7; } }";
        assertEquals(valid, ((J.CompilationUnit) method.invoke(null, reused, valid)).printAll());
    }

    private static List<String> after(
            List<NebulaM3A3Lab.Result> results,
            String schedule) {
        return results.stream()
                .filter(result -> result.schedule().equals(schedule))
                .map(NebulaM3A3Lab.Result::afterSha256)
                .toList();
    }
}
