// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.indexdb.M3IndexDB;
import com.m3.indexdb.M3IndexDbSemanticIndex;
import com.m3.indexdb.M3IndexDbSemanticKind;
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
    void openRewriteIndexPersistsQueryableM3IndexDbGraphWithoutSourceChanges()
            throws Exception {
        Path root = Files.createTempDirectory("m3-semantic-index-");
        SourceFile first = parse(
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
        SourceFile second = parse(
                "src/java.base/share/classes/example/Other.java",
                """
                package example;

                interface Other {
                    int apply(int value);
                }
                """);

        var recipe = new M3SemanticIndexM3DbBridgeRecipe(root.toString());
        var run = recipe.run(
                new InMemoryLargeSourceSet(List.of(first, second)),
                context(),
                1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());

        try (M3IndexDB db = M3IndexDB.open(root)) {
            var artifact = db.requireArtifact("semantic-index");
            var header = M3SemanticIndexPayload.inspect(artifact.payload());
            assertEquals(1, header.version());
            assertTrue(header.dictionaryCount() > 10);
            assertTrue(header.nodeCount() >= 12);
            assertTrue(header.edgeCount() >= 11);
            assertEquals(artifact.payloadLength(), header.payloadBytes());

            M3IndexDbSemanticIndex index =
                    M3IndexDbSemanticIndex.load(db, "semantic-index");
            assertEquals(2, index.nodesOfKind(M3IndexDbSemanticKind.FILE).size());
            assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.INTERFACE).size());
            assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.IMPLEMENTATION).size());
            assertFalse(index.nodesOfKind(M3IndexDbSemanticKind.ATOM).isEmpty());
            assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.PACKAGE).size());
            assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.REPOSITORY).size());

            var atom = index.nodesOfKind(M3IndexDbSemanticKind.ATOM).getFirst();
            assertTrue(index.logicSha256(atom.fingerprint().logicSha256()).contains(atom));
            assertTrue(index.structuralSha256(atom.fingerprint().structuralSha256()).contains(atom));
            assertTrue(index.nearSimHash(atom.fingerprint().simHash64(), 0)
                    .stream()
                    .anyMatch(match -> match.node().equals(atom)));
        }
    }

    @Test
    void parallelFileInputOrderProducesTheSameCanonicalM3IndexDbArtifact()
            throws Exception {
        SourceFile alpha = parse(
                "src/java.base/share/classes/example/Alpha.java",
                """
                package example;
                final class Alpha {
                    private static int value(int a, int b) {
                        return a + b;
                    }
                }
                """);
        SourceFile beta = parse(
                "src/java.base/share/classes/example/Beta.java",
                """
                package example;
                final class Beta {
                    private static int value(int a, int b) {
                        return a * b;
                    }
                }
                """);

        Path leftRoot = Files.createTempDirectory("m3-semantic-left-");
        Path rightRoot = Files.createTempDirectory("m3-semantic-right-");

        runBridge(leftRoot, List.of(alpha, beta));
        runBridge(rightRoot, List.of(beta, alpha));

        try (M3IndexDB left = M3IndexDB.open(leftRoot);
                M3IndexDB right = M3IndexDB.open(rightRoot)) {
            var leftArtifact = left.requireArtifact("semantic-index");
            var rightArtifact = right.requireArtifact("semantic-index");

            assertEquals(leftArtifact.contentSha256(), rightArtifact.contentSha256());
            M3IndexDbSemanticIndex leftIndex =
                    M3IndexDbSemanticIndex.load(left, "semantic-index");
            M3IndexDbSemanticIndex rightIndex =
                    M3IndexDbSemanticIndex.load(right, "semantic-index");
            assertEquals(leftIndex.nodes(), rightIndex.nodes());
            assertEquals(leftIndex.edges(), rightIndex.edges());

            var leftRootNode =
                    leftIndex.nodesOfKind(M3IndexDbSemanticKind.REPOSITORY).getFirst();
            var rightRootNode =
                    rightIndex.nodesOfKind(M3IndexDbSemanticKind.REPOSITORY).getFirst();
            assertEquals(leftRootNode.fingerprint(), rightRootNode.fingerprint());

            var atomLogic = leftIndex.nodesOfKind(M3IndexDbSemanticKind.ATOM)
                    .stream()
                    .map(node -> node.fingerprint().logicSha256())
                    .distinct()
                    .toList();
            assertTrue(atomLogic.size() >= 2);
            assertNotEquals(atomLogic.get(0), atomLogic.get(1));
        }
    }

    private static void runBridge(Path root, List<SourceFile> sources) {
        var run = new M3SemanticIndexM3DbBridgeRecipe(root.toString())
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
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
