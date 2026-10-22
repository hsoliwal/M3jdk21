// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/**
 * JUnit trust gate for the Nebula one-build postimage Tycho recipe.
 *
 * <p>This proves the recipe and negative/drift paths, not execution of Tycho itself. Only the
 * real GitHub Actions step may establish the product-build evidence.</p>
 */
final class NebulaM3OneBuildTychoProofRecipeTest {
    private static final String ROOT = NebulaM3OneBuildTychoProofRecipe.RESOURCE_ROOT;

    @Test
    void recipeIsAnOrderedTwoAtomSourceSealedDag() {
        var recipe = new NebulaM3OneBuildTychoProofRecipe();
        assertEquals(1, recipe.maxCycles());
        assertEquals(2, recipe.getRecipeList().size());
        assertEquals(
                NebulaM3OneBuildTychoProofRecipe.WorkflowLeaf.class,
                recipe.getRecipeList().get(0).getClass());
        assertEquals(
                NebulaM3OneBuildTychoProofRecipe.DocumentationLeaf.class,
                recipe.getRecipeList().get(1).getClass());
        assertTrue(recipe.getTags().contains("candidate-only"));
    }

    @Test
    void declarativeEntryPointSelectsTheExactTwoLeafRecipeDag() throws Exception {
        String yamlPath = "/META-INF/rewrite/nebula-one-build-tycho-proof.yml";
        var url = getClass().getResource(yamlPath);
        assertNotNull(url, "declarative OpenRewrite entry point");
        try (var input = url.openStream()) {
            var loader = new org.openrewrite.config.YamlResourceLoader(
                    input, url.toURI(), new java.util.Properties());
            var environment = org.openrewrite.config.Environment.builder()
                    .load(loader).build();
            var composite = environment.activateRecipes(
                    "org.eclipse.nebula.m3.OneBuildTychoPostimageProof");
            assertTrue(composite.validateAll().stream()
                    .allMatch(org.openrewrite.Validated::isValid));
            assertEquals(1, composite.getRecipeList().size());
            assertEquals(NebulaM3OneBuildTychoProofRecipe.class,
                    composite.getRecipeList().getFirst().getClass());
        }
    }

    @Test
    void beforeAndAfterResourcesHaveExactReviewedHashes() throws IOException {
        assertEquals(
                NebulaM3OneBuildTychoProofRecipe.WorkflowLeaf.BEFORE,
                NebulaM3ExactTextSnapshotRecipe.sha256(resource("workflow.before.txt")));
        assertEquals(
                NebulaM3OneBuildTychoProofRecipe.WorkflowLeaf.AFTER,
                NebulaM3ExactTextSnapshotRecipe.sha256(resource("workflow.after.txt")));
        assertEquals(
                NebulaM3OneBuildTychoProofRecipe.DocumentationLeaf.BEFORE,
                NebulaM3ExactTextSnapshotRecipe.sha256(resource("documentation.before.txt")));
        assertEquals(
                NebulaM3OneBuildTychoProofRecipe.DocumentationLeaf.AFTER,
                NebulaM3ExactTextSnapshotRecipe.sha256(resource("documentation.after.txt")));
        assertTrue(new NebulaM3OneBuildTychoProofRecipe.WorkflowLeaf()
                .matches(Path.of(NebulaM3OneBuildTychoProofRecipe.WORKFLOW_PATH)));
        assertFalse(new NebulaM3OneBuildTychoProofRecipe.WorkflowLeaf()
                .matches(Path.of(".github/workflows/other.yml")));
    }

    @Test
    void postimageBuildRunsAfterFixedPointAndDoesNotChangeFileCandidates()
            throws IOException {
        String before = resource("workflow.before.txt");
        String after = resource("workflow.after.txt");
        assertFalse(before.contains("Original Nebula Tycho reactor on transformed candidate"));
        int fixedPoint = after.indexOf("7 - Second pass must be fixed point");
        int recipeProof = after.indexOf("8 - Re-prove recipe tests after candidate application");
        int tycho = after.indexOf("9 - Original Nebula Tycho reactor on transformed candidate");
        int patchGuard = after.indexOf("10 - Verify Tycho left exact FILE postimage untouched");
        int evidence = after.indexOf("11 - Preserve candidate and DAG evidence");
        assertTrue(0 <= fixedPoint && fixedPoint < recipeProof);
        assertTrue(recipeProof < tycho && tycho < patchGuard && patchGuard < evidence);
        assertTrue(after.contains(
                "xvfb-run -a mvn -V -B -ntp clean verify -Dtycho.localArtifacts=ignore"));
        assertTrue(after.contains("cmp \"$OUT/candidate.patch\" \"$OUT/candidate-after-tycho.patch\""));
        assertTrue(after.contains(
                "cmp \"$OUT/candidate-paths.txt\" \"$OUT/candidate-paths-after-tycho.txt\""));
        assertTrue(after.contains("test -z \"$(git diff --name-only --cached)\""));
        assertTrue(resource("documentation.after.txt")
                .contains("The postimage Tycho stage is a mandatory product-build gate"));
    }

    @Test
    void compositeRecipeReplaysTwoExactTextFilesAndSecondPassIsUnchanged()
            throws IOException {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> original = List.of(
                file(NebulaM3OneBuildTychoProofRecipe.WORKFLOW_PATH,
                        resource("workflow.before.txt")),
                file(NebulaM3OneBuildTychoProofRecipe.DOCUMENT_PATH,
                        resource("documentation.before.txt")),
                file("unrelated/Other.txt", "untouched\n"));
        var recipe = new NebulaM3OneBuildTychoProofRecipe();
        List<Result> first = recipe.run(
                new InMemoryLargeSourceSet(original), context, 1)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(2, first.size());
        Map<Path, SourceFile> changed = first.stream().collect(Collectors.toMap(
                r -> r.getAfter().getSourcePath(),
                Result::getAfter));
        assertEquals(resource("workflow.after.txt"),
                changed.get(Path.of(NebulaM3OneBuildTychoProofRecipe.WORKFLOW_PATH)).printAll());
        assertEquals(resource("documentation.after.txt"),
                changed.get(Path.of(NebulaM3OneBuildTychoProofRecipe.DOCUMENT_PATH)).printAll());

        List<SourceFile> secondInputs = original.stream()
                .map(source -> changed.getOrDefault(source.getSourcePath(), source))
                .toList();
        List<Result> second = recipe.run(
                new InMemoryLargeSourceSet(secondInputs), context, 1)
                .getChangeset().getAllResults();
        assertTrue(second.isEmpty());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void stalePreimageIsRejectedAndUnrelatedSourceRemainsUnchanged() throws IOException {
        var recipe = new NebulaM3OneBuildTychoProofRecipe.WorkflowLeaf();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var source = file(NebulaM3OneBuildTychoProofRecipe.WORKFLOW_PATH,
                resource("workflow.before.txt") + "\n# drift\n");
        SourceFile result = (SourceFile) recipe.getVisitor().visit(source, context);
        assertSame(source, result);
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getMessage().contains("preimage drift"));

        errors.clear();
        var unrelated = file("unrelated/workflow.yml", resource("workflow.before.txt"));
        SourceFile unchanged = (SourceFile) recipe.getVisitor().visit(unrelated, context);
        assertSame(unrelated, unchanged);
        assertTrue(errors.isEmpty());
    }

    private static PlainText file(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }

    private static String resource(String suffix) throws IOException {
        try (var stream = NebulaM3OneBuildTychoProofRecipeTest.class
                .getResourceAsStream(ROOT + suffix)) {
            assertNotNull(stream, suffix);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
