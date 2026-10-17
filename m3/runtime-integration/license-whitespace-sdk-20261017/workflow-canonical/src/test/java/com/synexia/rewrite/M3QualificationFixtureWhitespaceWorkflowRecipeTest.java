/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Source-sealed SDK qualification of one existing workflow output. */
final class M3QualificationFixtureWhitespaceWorkflowRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-qualification-fixture-whitespace-workflow-20261017/";
    private static final String RECEIVING = ".github/workflows/m3-first-release-admission.yml";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3QualificationFixtureWhitespaceWorkflowRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3QualificationFixtureWhitespaceWorkflowRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            input.put(target.path(), contents("before/" + Path.of(target.path()).getFileName()));
        }
        input.put("opaque/Unrelated.txt", "fake workflow: make images and exit 0 stay opaque\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Recipe namedRecipe() {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes("com.synexia.m3.QualificationFixtureWhitespaceWorkflow");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.QualificationFixtureWhitespaceWorkflow",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3QualificationFixtureWhitespaceWorkflowRecipe.class),
                recipe.getRecipeList().get(0).getRecipeList().stream()
                        .map(Object::getClass).toList());
        return recipe;
    }
    private static Applied apply(Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(new InMemoryLargeSourceSet(sources(input)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var changes = run.getChangeset().getAllResults();
        for (var change : changes) {
            assertNotNull(change.getAfter());
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        }
        return new Applied(output, changes.size());
    }

    @Test void everyPartialStateAndOrderPreservesExistingWorkflow() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(1, targets.size());
        var target = targets.get(0);
        assertEquals(RECEIVING, target.path());
        assertTrue(!target.addition() && !target.removal());
        assertTrue(!target.before().equals(target.after()));
        for (int state = 0; state < 2; state++) {
            var input = before();
            if (state == 1) input.put(target.path(), target.text());
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(1 - state, result.changes());
                assertEquals(target.after(),
                        M3GpuToolJoinManifest.sha256(result.files().get(target.path())));
                assertEquals(input.get("opaque/Unrelated.txt"), result.files().get("opaque/Unrelated.txt"));
                assertEquals(0, apply(result.files()).changes());
                assertWorkflowPreservation(result.files().get(RECEIVING));
                if (state == 0 && !reverse) retain(target.path(), result.files().get(target.path()));
            }
        }
    }

    private static void assertWorkflowPreservation(String receiving) throws IOException {
        String first = "\n      - name: Qualify exact license-fixture whitespace policy SDK\n";
        String second = "      - name: Retain exact license-fixture whitespace SDK evidence\n";
        String next = "\n  control-reactor:\n";
        int start = receiving.indexOf(first);
        int end = receiving.indexOf(next, start);
        assertTrue(start >= 0 && end > start, "bounded SDK insertion must precede control reactor");
        String insertion = receiving.substring(start, end);
        assertEquals(2L, insertion.lines().filter(line -> line.startsWith("      - ")).count());
        assertEquals(1, receiving.split(java.util.regex.Pattern.quote(first), -1).length - 1);
        assertEquals(1, receiving.split(java.util.regex.Pattern.quote(second), -1).length - 1);
        assertEquals(contents("before/m3-first-release-admission.yml"),
                receiving.substring(0, start) + receiving.substring(end));
        assertTrue(receiving.substring(0, start).endsWith(
                "      - name: JNI/native parity proof\n" +
                "        run: mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test\n"));
        assertTrue(insertion.contains(second));
        assertTrue(insertion.contains("d9f99d9d99ff66ae4afd37f147af66f63da61e55"));
        assertTrue(insertion.contains("03b940aa136078498dd9cbd6349c0abda59c8c8123d0a7672fb213171fdbc99b"));
        assertTrue(insertion.contains("1c1b1b568d369f688f6953161636bf36b484bf5a"));
        assertTrue(insertion.contains("38df5f97bbeec8a8a8cfc77091d56bda925c07f2"));
        assertTrue(insertion.contains("synexia-openrewrite-recipes/recipes/m3-string-successor-chain-20261014/license-whitespace-sdk-20261017/sdk"));
        assertTrue(insertion.contains("assert actual == metadata[\"moduleGitTree\"]"));
        assertTrue(insertion.contains("shutil.disk_usage(\".\").free - (512 << 20) >= (8 << 30)"));
        assertTrue(insertion.contains("metadata[\"runtimeApplied\"] is False and metadata[\"phaseAdvanced\"] is False"));
        String canonical = "m3/runtime-integration/license-whitespace-sdk-20261017/canonical";
        assertTrue(insertion.contains("python3 -I -S -B " + canonical + "/verify.py\n"));
        assertTrue(insertion.contains("mvn -B -ntp -f " + canonical + "/pom.xml test\n"));
        assertTrue(insertion.contains("python3 -I -S -B " + canonical + "/verify.py --reports\n"));
        assertTrue(!insertion.contains("--repository"));
        assertTrue(insertion.contains("workflow-canonical-source.json"));
        assertTrue(insertion.contains("guard[\"repository\"] == \"hsoliwal/com.synexia\""));
        assertTrue(insertion.contains("guard[\"sourcePath\"] == \"synexia-openrewrite-recipes/recipes/m3-string-successor-chain-20261014/license-whitespace-sdk-20261017/workflow-sdk\""));
        assertTrue(insertion.contains("guard[\"receiverCorePath\"] == str(root / \"workflow-canonical\")"));
        assertTrue(insertion.contains("assert guard_tree == guard[\"moduleGitTree\"]"));
        assertTrue(insertion.contains("hashlib.sha256(guard_pins).hexdigest() == guard[\"pinsSha256\"]"));
        assertTrue(insertion.contains("guard[\"runtimeApplied\"] is False and guard[\"phaseAdvanced\"] is False"));
        assertTrue(insertion.contains("guard[\"openrewriteAdapterGitBlob\"] == \"1c1b1b568d369f688f6953161636bf36b484bf5a\""));
        assertTrue(insertion.contains("guard[\"manifestKernelGitBlob\"] == \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\""));
        String workflowCanonical = "m3/runtime-integration/license-whitespace-sdk-20261017/workflow-canonical";
        String workflowCommands = "          python3 -I -S -B " + workflowCanonical + "/verify.py\n" +
                "          mvn -B -ntp -f " + workflowCanonical + "/pom.xml test\n" +
                "          python3 -I -S -B " + workflowCanonical + "/verify.py --reports\n";
        assertTrue(insertion.contains("          python3 -I -S -B " + canonical +
                "/verify.py --reports\n" + workflowCommands));
        assertTrue(insertion.contains(workflowCanonical + "/target/sdk-receipt.json\n"));
        assertTrue(insertion.contains(workflowCanonical + "/target/surefire-reports/TEST-*.xml\n"));
        assertTrue(insertion.contains(workflowCanonical + "/target/emitted/\n"));
        assertTrue(insertion.contains("        if: always()\n"));
        assertTrue(insertion.contains("actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02"));
        assertTrue(insertion.contains(canonical + "/target/sdk-receipt.json\n"));
        assertTrue(insertion.contains(canonical + "/target/surefire-reports/TEST-*.xml\n"));
        assertTrue(insertion.contains(canonical + "/target/emitted/\n"));
        assertTrue(insertion.contains("if-no-files-found: error"));
        assertTrue(insertion.contains("          include-hidden-files: true\n"));
    }

    @Test void workflowRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign workflow source\n");
            reject(sources(input), "source drift: " + target.path());
            input = before();
            input.remove(target.path());
            reject(sources(input), "required source missing: " + target.path());
            var duplicate = sources(before());
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(before().get(target.path())).build());
            reject(duplicate, "duplicate source target: " + target.path());
        }
    }

    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        StringBuilder observed = new StringBuilder();
        for (Throwable error : errors) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
    }

    private static void retain(String path, String content) throws IOException {
        Path output = Path.of(System.getProperty("m3.fixture.root"))
                .resolve("target/emitted/license-whitespace-workflow").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
