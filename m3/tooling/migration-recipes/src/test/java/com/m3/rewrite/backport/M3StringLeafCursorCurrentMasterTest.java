// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3StringLeafCursorCurrentMasterTest {
    private static final String DIR = "src/java.base/share/classes/java/lang/";
    private static final String ROOT = "/com/m3/rewrite/backport/m3string-leaf-cursor-20261021/";
    private record Run(Map<String,String> changes, List<SourceFile> sources) {}

    @Test void previousBranchAndCurrentMasterConvergeToSamePinnedOutputs() throws IOException {
        List<SourceFile> previous = List.of(
            java("M3StringPool.java", "M3StringPool.previous-after.java.txt"),
            java("M3StringTokenRegex.java", "M3StringTokenRegex.after.java.txt"),
            java("M3StringLeafCursor.java", "M3StringLeafCursor.previous-after.java.txt"));
        List<SourceFile> current = List.of(
            java("M3StringPool.java", "M3StringPool.before.java.txt"),
            java("M3StringTokenRegex.java", "M3StringTokenRegex.after.java.txt"));
        Run a = run(previous), b = run(current);
        Map<String,String> expected = Map.of(
            DIR + "M3StringPool.java", resource("M3StringPool.after.java.txt"),
            DIR + "M3StringLeafCursor.java", resource("M3StringLeafCursor.java.txt"));
        assertEquals(expected, a.changes());
        assertEquals(a.changes(), b.changes());
        assertTrue(run(a.sources()).changes().isEmpty());
        assertTrue(run(b.sources()).changes().isEmpty());
        Path out = Path.of(System.getProperty("m3.receiver.output", "target/generated-target"));
        for (var row : a.changes().entrySet()) {
            Path target = out.resolve(row.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, row.getValue(), StandardCharsets.UTF_8);
        }
    }

    @Test void reversedMixedStatesPreserveAlreadyAppliedCursorAndTokenRegex() {
        List<SourceFile> forward = List.of(
            java("M3StringPool.java", "M3StringPool.previous-after.java.txt"),
            java("M3StringTokenRegex.java", "M3StringTokenRegex.after.java.txt"),
            java("M3StringLeafCursor.java", "M3StringLeafCursor.java.txt"));
        List<SourceFile> reverse = new ArrayList<>(forward);
        Collections.reverse(reverse);
        Run a = run(forward), b = run(reverse);
        assertEquals(Map.of(DIR + "M3StringPool.java", resource("M3StringPool.after.java.txt")), a.changes());
        assertEquals(a.changes(), b.changes());
        assertTrue(run(a.sources()).changes().isEmpty());
    }

    @Test void obsoleteOriginalPreimageAndTamperedPredecessorCursorRefuse() {
        List<SourceFile> old = List.of(
            java("M3StringPool.java", "M3StringPool.original-before.java.txt"),
            java("M3StringTokenRegex.java", "M3StringTokenRegex.after.java.txt"));
        assertThrows(RuntimeException.class, () -> run(old));
        List<SourceFile> drift = List.of(
            java("M3StringPool.java", "M3StringPool.before.java.txt"),
            java("M3StringTokenRegex.java", "M3StringTokenRegex.after.java.txt"),
            parsed("M3StringLeafCursor.java", resource("M3StringLeafCursor.previous-after.java.txt") + "// drift\n"));
        assertThrows(RuntimeException.class, () -> run(drift));
    }

    @Test void neighboringPoolOwnersAndCurrentNativeJoinRemainExact() {
        String current = resource("M3StringPool.before.java.txt");
        String after = resource("M3StringPool.after.java.txt");
        String retainedOwners = "    private static final class TupleBucket ";
        assertEquals(current.substring(current.indexOf(retainedOwners)),
                     after.substring(after.indexOf(retainedOwners)));
        String nativeJoin = "    static native M3String concatViewsNative0(M3String[] parts);";
        assertTrue(current.contains(nativeJoin));
        assertTrue(after.contains(nativeJoin));
        assertTrue(after.contains("static void copyCharsFromPath("));
        assertTrue(after.contains("static void copyBytesFromPath("));
    }

    private static Run run(List<SourceFile> input) {
        var executed = new M3StringLeafCursorConvergenceRecipe()
            .run(new InMemoryLargeSourceSet(input), context(), 1);
        Map<String,String> changes = new LinkedHashMap<>();
        Map<String,SourceFile> all = new LinkedHashMap<>();
        for (SourceFile source : input) all.put(source.getSourcePath().toString(), source);
        for (var result : executed.getChangeset().getAllResults()) {
            assertNotNull(result.getAfter());
            SourceFile after = result.getAfter();
            String path = after.getSourcePath().toString();
            changes.put(path, after.printAll());
            all.put(path, after);
        }
        return new Run(changes, new ArrayList<>(all.values()));
    }

    private static SourceFile java(String name, String fixture) {
        return parsed(name, resource(fixture));
    }
    private static SourceFile parsed(String name, String text) {
        List<SourceFile> sources = JavaParser.fromJavaVersion().build().parseInputs(
            List.of(Parser.Input.fromString(Path.of(DIR + name), text)), null, context()).toList();
        assertEquals(1, sources.size());
        assertEquals(text, sources.getFirst().printAll());
        return sources.getFirst();
    }
    private static String resource(String name) {
        try (var input = M3StringLeafCursorCurrentMasterTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }
}
