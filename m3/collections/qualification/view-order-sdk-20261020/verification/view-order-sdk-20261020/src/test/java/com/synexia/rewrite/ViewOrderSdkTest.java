// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
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

final class ViewOrderSdkTest {
    private static final String CRATE = "m3-view-order-sdk-20261020";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String DIR = "m3/collections/src/main/java/com/m3/collections/";
    private static final String TARGET = DIR + "M3LongIndexProjection.java";
    private static final String AFTER = "a29b0bc589565924611538c6fabe15a70d3629bc4eb3db0977d19f411ef4eb4e";

    private static String resource(String name) throws Exception {
        try (var in = ViewOrderSdkTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException("SDK failure", error); });
    }
    private static List<SourceFile> parse(String text, boolean all) throws Exception {
        var inputs = new ArrayList<Parser.Input>();
        inputs.add(Parser.Input.fromString(Path.of(TARGET), text));
        for (String name : List.of("M3IndexMaskCompactor.java", "M3LongAddressKernel.java")) {
            inputs.add(Parser.Input.fromString(Path.of(DIR + name), resource(name + ".txt")));
        }
        var files = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context()).toList();
        assertEquals(3, files.size());
        return all ? files : files.stream().filter(f -> f.getSourcePath().toString().equals(TARGET)).toList();
    }
    private static M3HashPinnedJavaSnapshotRecipe recipe() {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE);
    }
    @Test void exactGenerationAndReceipt() throws Exception {
        var r = recipe();
        assertEquals(List.of(TARGET), r.targetPaths());
        assertEquals(M3EditScope.FILE, r.requiredScope());
        var results = r.run(new InMemoryLargeSourceSet(parse(resource("M3LongIndexProjection.before.java.txt"), false)), context(), 1).getChangeset().getAllResults();
        assertEquals(1, results.size());
        var result = results.getFirst();
        assertNotNull(result.getBefore());
        var output = result.getAfter();
        assertNotNull(output);
        assertEquals(TARGET, output.getSourcePath().toString());
        assertEquals(resource("M3LongIndexProjection.java.txt"), output.printAll());
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(output.printAll().getBytes(StandardCharsets.UTF_8)));
        assertEquals(AFTER, hash);
        Path destination = Path.of("target/generated-receiver").resolve(TARGET);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, output.printAll(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("OPENREWRITE_GENERATED_SHA256=" + hash);
    }
    @Test void fixedPoint() throws Exception {
        var changes = recipe().run(new InMemoryLargeSourceSet(parse(resource("M3LongIndexProjection.java.txt"), false)), context(), 1).getChangeset().getAllResults();
        assertTrue(changes.isEmpty());
    }
    @Test void refusesSourceDrift() throws Exception {
        var files = parse(resource("M3LongIndexProjection.before.java.txt") + "\n", false);
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(files), context(), 1));
    }
    @Test void refusesMissingSource() {
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(List.of()), context(), 1));
    }
    @Test void refusesDuplicateSource() throws Exception {
        var files = new ArrayList<>(parse(resource("M3LongIndexProjection.before.java.txt"), false));
        files.addAll(parse(resource("M3LongIndexProjection.before.java.txt"), false));
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(files), context(), 1));
    }
    @Test void refusesWrongPath() throws Exception {
        var files = parse(resource("M3LongIndexProjection.before.java.txt"), false);
        var wrong = files.getFirst().withSourcePath(Path.of("unrelated/M3LongIndexProjection.java"));
        assertThrows(RuntimeException.class, () -> recipe().run(new InMemoryLargeSourceSet(List.of(wrong)), context(), 1));
    }
    @Test void contextOrderCannotBroadenMutation() throws Exception {
        var files = new ArrayList<>(parse(resource("M3LongIndexProjection.before.java.txt"), true));
        Collections.reverse(files);
        var results = recipe().run(new InMemoryLargeSourceSet(files), context(), 1).getChangeset().getAllResults();
        assertEquals(1, results.size());
        assertEquals(TARGET, results.getFirst().getAfter().getSourcePath().toString());
        assertEquals(resource("M3LongIndexProjection.java.txt"), results.getFirst().getAfter().printAll());
    }
}
