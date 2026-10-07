// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Exact patch mechanics and ordered composition, not a whole-JDK conformance certificate. */
final class M3JdkExecutionFollowthroughRecipeTest {
    private static final String TEXT_CRATE = "jdk21-developer-image-proof-20261004";
    private static final String JAVA_CRATE = "jdk27-password-policy-proof-20261004";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + TEXT_CRATE + "/";
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + JAVA_CRATE + "/";
    private static final String BUILDER = "m3/tooling/module-packs/build_pack.py";
    private static final String PROBE =
            "test/jdk/sun/security/util/Password/M3PasswordPolicyCases.java";
    private static final String NAME = "com.m3.rewrite.packs.JdkExecutionFollowthrough";

    @Test
    void namedCompositionMaterializesExactTargetsAndReachesFixedPoint() throws Exception {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.m3.rewrite")
                .build().activateRecipes(NAME);
        var errors = new ArrayList<Throwable>();
        var unrelated = text("unrelated.txt", "preserve\n");
        var input = new ArrayList<SourceFile>(baseline());
        input.add(unrelated);
        var results = recipe.run(new InMemoryLargeSourceSet(input),
                new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        Map<String, String> expected = expectedOutputs();
        assertEquals(7, expected.size());
        assertEquals(expected.size(), results.size());
        var after = new ArrayList<SourceFile>();
        var actual = new TreeMap<String, String>();
        for (var result : results) {
            SourceFile file = result.getAfter();
            assertNotNull(file);
            String path = file.getSourcePath().toString().replace('\\', '/');
            actual.put(path, file.printAll());
            if (path.equals(PROBE)) assertTrue(file instanceof J.CompilationUnit);
            else assertTrue(file instanceof PlainText);
            if (path.equals(BUILDER)) assertEquals(input.getFirst().getId(), file.getId());
            after.add(file);
        }
        assertEquals(expected, actual);
        after.add(unrelated);
        var repeat = recipe.run(new InMemoryLargeSourceSet(after),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertEquals("preserve\n", unrelated.printAll());
    }

    @Test
    void textAdapterRefusesMissingDriftDuplicateAndOccupiedTargets() throws IOException {
        reject(new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE), List.of());
        reject(new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE),
                List.of(text(BUILDER, "drift\n")));
        List<SourceFile> duplicates = new ArrayList<>(baseline());
        duplicates.add(text(BUILDER, duplicates.getFirst().printAll()));
        reject(new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE), duplicates);
        List<SourceFile> occupied = new ArrayList<>(baseline());
        occupied.add(text("m3/tooling/module-packs/developer_tools.py", "occupied\n"));
        reject(new M3Jdk21HashPinnedTextSnapshotRecipe(TEXT_CRATE), occupied);
    }

    @Test
    void javaAdditionRefusesAnOccupiedNonJavaTree() {
        reject(new M3Jdk21HashPinnedSnapshotRecipe(JAVA_CRATE), List.of(text(PROBE, "occupied\n")));
    }

    @Test
    void sealedPreimageAndPostimagesMatchTheirDeclaredByteHashes() throws Exception {
        for (String root : List.of(TEXT_ROOT, JAVA_ROOT)) {
            String previous = "";
            for (String line : resource(root + "manifest.tsv").lines().toList()) {
                String[] cells = line.split("\t", -1);
                assertEquals(4, cells.length);
                assertTrue(previous.compareTo(cells[0]) < 0);
                previous = cells[0];
                assertEquals(cells[2], sha(resource(root + cells[3])));
                if (!cells[1].equals("ABSENT")) {
                    assertEquals(cells[1], sha(resource(root + "before-" + cells[3])));
                }
            }
        }
    }

    private static List<SourceFile> baseline() throws IOException {
        for (String line : resource(TEXT_ROOT + "manifest.tsv").lines().toList()) {
            String[] cells = line.split("\t", -1);
            if (cells[0].equals(BUILDER)) {
                return List.of(text(BUILDER, resource(TEXT_ROOT + "before-" + cells[3])));
            }
        }
        throw new IllegalStateException("missing builder preimage");
    }

    private static Map<String, String> expectedOutputs() throws IOException {
        var result = new TreeMap<String, String>();
        for (String root : List.of(TEXT_ROOT, JAVA_ROOT)) {
            for (String line : resource(root + "manifest.tsv").lines().toList()) {
                String[] cells = line.split("\t", -1);
                result.put(cells[0], resource(root + cells[3]));
            }
        }
        return result;
    }

    private static void reject(Recipe recipe, List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = recipe.run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(run.getChangeset().getAllResults().isEmpty());
            assertFalse(errors.isEmpty(), "refusal must be reported");
        } catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static PlainText text(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }

    private static String resource(String path) throws IOException {
        try (var input = M3JdkExecutionFollowthroughRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
