// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class RuntimeCountSkipRecipeTest {
    private static final String CRATE = "m3jdk21-bitlane-runtime-count-skip-20261019";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String DIR = "src/java.base/share/classes/jdk/internal/mindex/";
    private static final String TARGET = DIR + "M3BitLane28.java";
    private static final String AFTER = "e87553ae33e4d05f07c3b57e081a88d076e871499fe98a0f69df8ddb9d0d2fb2";
    private static String text(String name) throws Exception {
        try (var in = RuntimeCountSkipRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(e -> { throw new IllegalStateException("SDK gate failed", e); });
    }
    private static List<SourceFile> parse(String value, boolean includeContext) throws Exception {
        var inputs = new ArrayList<Parser.Input>();
        inputs.add(Parser.Input.fromString(Path.of(TARGET), value));
        for (String name : List.of("M3Address28.java", "M3Bits.java")) {
            inputs.add(Parser.Input.fromString(Path.of(DIR + name), text(name + ".txt")));
        }
        var files = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context()).toList();
        assertEquals(3, files.size());
        return includeContext ? files : files.stream().filter(f -> f.getSourcePath().toString().equals(TARGET)).toList();
    }
    private static M3HashPinnedJavaSnapshotRecipe recipe() { return new M3HashPinnedJavaSnapshotRecipe(CRATE); }
    @Test void exactGenerationAndReceipt() throws Exception {
        var r = recipe();
        assertEquals(List.of(TARGET), r.targetPaths());
        assertEquals(M3EditScope.FILE, r.requiredScope());
        var results = r.run(new InMemoryLargeSourceSet(parse(text("M3BitLane28.before.java.txt"), false)), context(), 1).getChangeset().getAllResults();
        assertEquals(1, results.size());
        var result = results.getFirst();
        assertNotNull(result.getBefore());
        var output = result.getAfter();
        assertNotNull(output);
        assertEquals(TARGET, output.getSourcePath().toString());
        assertEquals(text("M3BitLane28.java.txt"), output.printAll());
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(output.printAll().getBytes(StandardCharsets.UTF_8)));
        assertEquals(AFTER, hash);
        Path destination = Path.of("target/generated-receiver").resolve(TARGET);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, output.printAll(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("OPENREWRITE_GENERATED_SHA256=" + hash);
    }
    @Test void fixedPoint() throws Exception {
        var results = recipe().run(new InMemoryLargeSourceSet(parse(text("M3BitLane28.java.txt"), false)), context(), 1).getChangeset().getAllResults();
        assertTrue(results.isEmpty());
    }
    @Test void refusesDrift() throws Exception {
        var files = parse(text("M3BitLane28.before.java.txt") + "\n", false);
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(files), context(), 1));
    }
    @Test void refusesMissingSource() {
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(List.of()), context(), 1));
    }
    @Test void refusesDuplicateSource() throws Exception {
        var files = new ArrayList<>(parse(text("M3BitLane28.before.java.txt"), false));
        files.addAll(parse(text("M3BitLane28.before.java.txt"), false));
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(files), context(), 1));
    }
    @Test void refusesChangedBehavior() throws Exception {
        String before = text("M3BitLane28.before.java.txt");
        String changed = before.replace("private int cardinality;", "private int cardinality = 1;");
        assertNotEquals(before, changed);
        var files = parse(changed, false);
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(files), context(), 1));
    }
    @Test void contextPermutationPreservesOnlyOneTarget() throws Exception {
        var files = new ArrayList<>(parse(text("M3BitLane28.before.java.txt"), true));
        Collections.reverse(files);
        var results = recipe().run(new InMemoryLargeSourceSet(files), context(), 1).getChangeset().getAllResults();
        assertEquals(1, results.size());
        assertEquals(TARGET, results.getFirst().getAfter().getSourcePath().toString());
        assertEquals(text("M3BitLane28.java.txt"), results.getFirst().getAfter().printAll());
    }
}
