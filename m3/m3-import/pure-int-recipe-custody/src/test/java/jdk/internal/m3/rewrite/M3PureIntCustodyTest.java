// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3PureIntCustodyTest {
    private static final Map<String, String> BLOBS =
            Map.of(
                    "M3PureIntAtomizeRecipe.java",
                    "95cd29e51a28307d9a4bfca3e54065c1177e3ddf",
                    "M3PureIntConvergenceRecipe.java",
                    "db03d0c99fa9ae6c369d856c39a460153d4eecaa",
                    "M3PureIntDocumentationRecipe.java",
                    "8c975be4668eef7169f37c29bad9866ef0eedc3c",
                    "M3PureIntInventoryRecipe.java",
                    "6ea761a4299a3ab61a0552d16f3c32b78965d6c8",
                    "M3PureIntLeaf.java",
                    "8f3724b2a82c75bbbc13f3da184ce303796153ef",
                    "M3PureIntPatternizeRecipe.java",
                    "1ebd87cbd163298dd620b12b12c296724c16ceb6",
                    "M3EditScope.java",
                    "274360816db1e44a2f1796a3bc5cdeff6b0ce6db",
                    "M3ScopeInference.java",
                    "a993ed2192188729cefeb45d6bcb5e49594e19b0",
                    "M3CanonicalMultiPassPlan.java",
                    "513f953f27d5aca5c3c2d4a9141c30f95d1d3392",
                    "M3ContractMode.java",
                    "eadcb6a7c3bef8fb3e45bd7c47ea6a91c0bf48b6");

    @Test
    void custodySourcesRemainExactM3GitBlobs() throws Exception {
        Path root =
                Path.of(System.getProperty("basedir", "."))
                        .toAbsolutePath()
                        .normalize()
                        .resolve("src/main/java/jdk/internal/m3/rewrite");
        for (var entry : BLOBS.entrySet()) {
            byte[] bytes = Files.readAllBytes(root.resolve(entry.getKey()));
            assertEquals(entry.getValue(), gitBlob(bytes), entry.getKey());
        }
    }

    @Test
    void convergenceRecipeKeepsSmallOrderedAtoms() {
        Recipe recipe = new M3PureIntConvergenceRecipe();

        assertEquals(1, recipe.maxCycles());
        assertEquals(
                List.of(
                        M3PureIntInventoryRecipe.class,
                        M3PureIntAtomizeRecipe.class,
                        M3PureIntPatternizeRecipe.class,
                        M3PureIntDocumentationRecipe.class),
                recipe.getRecipeList().stream().map(Object::getClass).toList());
    }

    @Test
    void scopeAndMultipassCustodyRetainsCanonicalM3Behavior() {
        assertTrue(M3EditScope.MODULE.permits(M3EditScope.FILE));
        assertFalse(M3EditScope.FILE.permits(M3EditScope.PACKAGE));
        assertEquals(M3EditScope.VISIBILITY, M3EditScope.FILE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.MULTI_MODULE.next());

        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.inferJavaTargets(
                        List.of("src/main/java/p/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.inferJavaTargets(
                        List.of(
                                "src/main/java/p/A.java",
                                "src/test/java/p/ATest.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.inferJavaTargets(
                        List.of(
                                "src/java.base/share/classes/java/lang/String.java",
                                "src/java.base/share/classes/java/util/List.java")));
        assertEquals(
                M3EditScope.MULTI_MODULE,
                M3ScopeInference.inferJavaTargets(
                        List.of(
                                "src/java.base/share/classes/java/lang/String.java",
                                "src/java.logging/share/classes/java/util/logging/Logger.java")));

        assertEquals(11, M3CanonicalMultiPassPlan.passes().size());
        assertEquals(
                List.of(
                        "INVENTORY",
                        "FILE_ATOMIZATION",
                        "FILE_PATTERNIZATION",
                        "FILE_DOCUMENTATION",
                        "FILE_FIXED_POINT"),
                M3CanonicalMultiPassPlan.fileConvergencePasses().stream()
                        .map(M3CanonicalMultiPassPlan.Pass::id)
                        .toList());
        assertEquals(64, M3CanonicalMultiPassPlan.root().length());
        assertEquals(
                List.of(
                        M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                        M3ContractMode.EXPLICIT_CONTRACT_CHANGE),
                List.of(M3ContractMode.values()));
    }

    @Test
    void admittedLeafTransformsAndSecondPassIsFixedPoint() {
        String path = "src/main/java/example/Sample.java";
        String source =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a << 3) ^ b;
                    }
                }
                """;

        Map<String, String> first =
                apply(new M3PureIntConvergenceRecipe(), Map.of(path, source));

        assertEquals(1, first.size());
        String transformed = first.get(path);
        assertTrue(transformed.contains("int m3$pureIntAtom ="));
        assertTrue(transformed.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(transformed.contains("M3-ATOM: m3$pureIntAtom"));
        assertTrue(
                apply(new M3PureIntConvergenceRecipe(), first).isEmpty(),
                "second recipe application must be unchanged");
    }

    @Test
    void broaderOrUnsafeShapesRemainUnchanged() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put(
                "src/main/java/example/PublicMethod.java",
                """
                package example;
                final class PublicMethod {
                    public static int compute(int a, int b) { return a + b; }
                }
                """);
        sources.put(
                "src/main/java/example/Division.java",
                """
                package example;
                final class Division {
                    private static int compute(int a, int b) { return a / b; }
                }
                """);
        sources.put(
                "src/main/java/example/Invocation.java",
                """
                package example;
                final class Invocation {
                    private static int id(int a) { return a; }
                    private static int compute(int a) { return id(a) + 1; }
                }
                """);

        assertTrue(apply(new M3PureIntConvergenceRecipe(), sources).isEmpty());
    }

    private static Map<String, String> apply(
            Recipe recipe, Map<String, String> sources) {
        var context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new AssertionError(failure);
                        });
        List<Parser.Input> inputs =
                sources.entrySet().stream()
                        .map(
                                entry ->
                                        Parser.Input.fromString(
                                                Path.of(entry.getKey()),
                                                entry.getValue()))
                        .toList();
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context)
                        .toList();

        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1);
        Map<String, String> after = new LinkedHashMap<>();
        run.getChangeset()
                .getAllResults()
                .forEach(
                        result -> {
                            SourceFile file = result.getAfter();
                            if (file == null) {
                                throw new AssertionError("unexpected deletion");
                            }
                            after.put(
                                    file.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    file.printAll());
                        });
        return after;
    }

    private static String gitBlob(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(
                ("blob " + bytes.length + "\0")
                        .getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }
}
