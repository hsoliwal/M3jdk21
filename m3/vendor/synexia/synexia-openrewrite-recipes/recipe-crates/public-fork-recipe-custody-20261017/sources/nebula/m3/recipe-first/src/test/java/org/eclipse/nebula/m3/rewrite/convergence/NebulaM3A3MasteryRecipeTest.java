// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3A3MasteryRecipeTest {
    @Test
    void generatesExactReviewedLabAndSecondRunIsFixedPoint() {
        var context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new AssertionError(failure);
                        });
        SourceFile anchor =
                parse(
                        "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java",
                        """
                        package org.eclipse.nebula.m3.rewrite;
                        public final class NebulaM3Java21ConvergenceRecipe {}
                        """,
                        context);

        var recipe = new NebulaM3A3MasteryRecipe();
        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(anchor)),
                        context,
                        2);
        var results = first.getChangeset().getAllResults();

        assertEquals(3, results.size());
        Map<String, String> targets = NebulaM3A3MasteryRecipe.reviewedTargets();
        assertEquals(3, targets.size());

        List<SourceFile> generated =
                results.stream()
                        .map(result -> Objects.requireNonNull(result.getAfter(), "after"))
                        .toList();
        generated.forEach(
                file ->
                        assertEquals(
                                targets.get(
                                        file.getSourcePath()
                                                .toString()
                                                .replace('\\', '/')),
                                gitBlob(file.printAll())));

        assertTrue(
                generated.stream()
                        .anyMatch(file -> file.printAll().contains("48, NebulaM3A3Lab.fixtureCount()")));
        assertTrue(
                generated.stream()
                        .anyMatch(file -> file.printAll().contains("assertEquals(288, results.size())")));

        var repeat =
                recipe.run(
                        new InMemoryLargeSourceSet(
                                java.util.stream.Stream.concat(
                                                java.util.stream.Stream.of(anchor),
                                                generated.stream())
                                        .toList()),
                        context,
                        2);
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void namedConvergenceOwnerRemainsTheAnchor() {
        assertTrue(
                new NebulaM3Java21ConvergenceRecipe()
                        .getTags()
                        .contains("atomization"));
        assertTrue(
                new NebulaM3Java21ConvergenceRecipe()
                        .getTags()
                        .contains("patternization"));
    }

    @Test
    void exactParserSessionUpgradeReplaysOldImagesAndRefusesDriftOrOtherPaths() throws Exception {
        var errors = new java.util.ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var recipe = new NebulaM3A3ParserSessionRecipe();
        var before = new java.util.ArrayList<SourceFile>();
        for (String name : List.of("NebulaM3A3Lab", "NebulaM3A3LabTest")) {
            String directory = name.endsWith("Test") ? "src/test/java/" : "src/main/java/";
            String path = directory + "org/eclipse/nebula/m3/" + name + ".java";
            try (var input = getClass().getResourceAsStream(
                    "/org/eclipse/nebula/m3/rewrite/convergence/a3-parser-session/"
                            + name + ".before.java.txt")) {
                org.junit.jupiter.api.Assertions.assertNotNull(input);
                before.add(parse(path, new String(input.readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8), context));
            }
        }
        var changes = recipe.run(new InMemoryLargeSourceSet(before), context, 2)
                .getChangeset().getAllResults();
        assertEquals(2, changes.size());
        var after = changes.stream().map(change -> Objects.requireNonNull(change.getAfter())).toList();
        for (SourceFile file : after) {
            String path = file.getSourcePath().toString().replace('\\', '/');
            assertEquals(NebulaM3A3MasteryRecipe.reviewedTargets().get(path), gitBlob(file.printAll()));
            SourceFile original = before.stream().filter(item -> item.getSourcePath().equals(file.getSourcePath()))
                    .findFirst().orElseThrow();
            assertEquals(original.getId(), file.getId());
            assertEquals(original.getMarkers().getId(), file.getMarkers().getId());
            assertTrue(file.getMarkers().getMarkers().containsAll(original.getMarkers().getMarkers()));
            assertEquals(original.getCharset(), file.getCharset());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context, 2)
                .getChangeset().getAllResults().isEmpty());
        assertTrue(errors.isEmpty(), errors.toString());
        SourceFile drift = parse(before.getFirst().getSourcePath().toString(),
                before.getFirst().printAll() + "// unrelated change\n", context);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context, 2)
                .getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty(), "unknown occupied preimage must be refused");
        errors.clear();
        SourceFile foreign = before.getFirst().withSourcePath(Path.of("other/Subject.java"));
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(foreign)), context, 2)
                .getChangeset().getAllResults().isEmpty());
        SourceFile text = org.openrewrite.text.PlainText.builder().text(before.getFirst().printAll())
                .sourcePath(before.getFirst().getSourcePath()).build();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(text)), context, 2)
                .getChangeset().getAllResults().isEmpty());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    private static SourceFile parse(
            String path,
            String source,
            InMemoryExecutionContext context) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), source)),
                                null,
                                context)
                        .toList();
        assertEquals(1, parsed.size());
        return parsed.getFirst();
    }

    private static String gitBlob(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] prefix =
                ("blob " + bytes.length + "\0")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
