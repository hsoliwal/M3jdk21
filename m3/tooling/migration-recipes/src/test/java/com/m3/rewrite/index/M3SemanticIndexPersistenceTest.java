// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.indexdb.M3IndexDB;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3SemanticIndexPersistenceTest {
    @Test
    void openRewriteIndexPersistsCompactDeterministicM3IndexDbArtifactWithoutSourceChanges()
            throws Exception {
        Path root = Files.createTempDirectory("m3-semantic-index-");
        SourceFile source = parse(
                "src/java.base/share/classes/example/Sample.java",
                """
                package example;

                /** Example documentation. */
                public final class Sample {
                    private static final int SCALE = 31;

                    private static int compute(int a, int b) {
                        return (a + b) * SCALE;
                    }
                }
                """);

        var firstRecipe = new M3SemanticIndexM3DbBridgeRecipe(root.toString());
        var first = firstRecipe.run(
                new InMemoryLargeSourceSet(List.of(source)),
                context(),
                1);
        assertTrue(first.getChangeset().getAllResults().isEmpty());

        String firstHash;
        try (M3IndexDB db = M3IndexDB.open(root)) {
            var artifact = db.requireArtifact("semantic-index");
            firstHash = artifact.contentSha256();
            var header = M3SemanticIndexPayload.inspect(artifact.payload());
            assertEquals(1, header.version());
            assertTrue(header.dictionaryCount() > 10);
            assertTrue(header.nodeCount() >= 8);
            assertTrue(header.edgeCount() >= 7);
            assertEquals(artifact.payloadLength(), header.payloadBytes());
        }

        var secondRecipe = new M3SemanticIndexM3DbBridgeRecipe(root.toString());
        secondRecipe.run(new InMemoryLargeSourceSet(List.of(source)), context(), 1);
        try (M3IndexDB db = M3IndexDB.open(root)) {
            assertEquals(firstHash, db.requireArtifact("semantic-index").contentSha256());
        }
    }

    private static SourceFile parse(String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
