// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3CpuInt8Receiver20261017RecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/m3-cpu-int8-receiver-20261017/";
    private static final String CHECKS =
            "m3/tooling/jmod-recipes/src/test/java/com/m3/rewrite/jmod/CpuToolReceiverAdmissionChecks.java";

    private static InMemoryExecutionContext ctx() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String fixture(String filename) {
        try (var input = M3CpuInt8Receiver20261017RecipeTest.class
                .getResourceAsStream(ROOT + filename)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) { throw new IllegalStateException(error); }
    }

    private static SourceFile parse(String path, String code) {
        return JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), code)), null, ctx())
                .findFirst().orElseThrow();
    }

    @Test void applyAndExactFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-cpu-int8-receiver-20261017");
        var original = parse(CHECKS, fixture("pre/CpuToolReceiverAdmissionChecks.java.txt"));
        var pass = recipe.run(new InMemoryLargeSourceSet(List.of(original)), ctx(), 1);
        Map<String, SourceFile> targets = new TreeMap<>();
        for (var item : pass.getChangeset().getAllResults()) {
            assertNotNull(item.getAfter());
            var output = item.getAfter();
            targets.put(output.getSourcePath().toString().replace('\\', '/'), output);
        }
        assertEquals(4, targets.size());
        for (String line : fixture("manifest.tsv").strip().split("\n")) {
            var item = line.split("\t", -1);
            assertEquals(fixture(item[3]), targets.get(item[0]).printAll());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.copyOf(targets.values())),
                ctx(), 1).getChangeset().getAllResults().isEmpty());
    }

    @Test void driftIsRefused() {
        var unrelated = parse(CHECKS,
                "package com.m3.rewrite.jmod; final class CpuToolReceiverAdmissionChecks {}");
        assertThrows(RuntimeException.class,
                () -> new M3HashPinnedJavaSnapshotRecipe("m3-cpu-int8-receiver-20261017")
                        .run(new InMemoryLargeSourceSet(List.of(unrelated)), ctx(), 1));
    }
}
