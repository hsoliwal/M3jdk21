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

/** Adapts the existing provider proof to four outputs and two immutable historical owners. */
final class M3RuntimeCurrentCustodyRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-runtime-current-custody-20261017/";
    private static final String OPAQUE = "opaque/Unrelated.txt";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3RuntimeCurrentCustodyRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3RuntimeCurrentCustodyRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }

    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            if (!target.addition()) {
                input.put(target.path(), contents("before/" + Path.of(target.path()).getFileName()));
            }
        }
        input.put(OPAQUE, "opaque unrelated file: never execute, rewrite or delete\n");
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
                .build().activateRecipes("com.synexia.m3.RuntimeCurrentCustody");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.RuntimeCurrentCustody",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3RuntimeCurrentCustodyRecipe.class),
                recipe.getRecipeList().get(0).getRecipeList().stream().map(Object::getClass).toList());
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

    @Test void runtimeCurrentCustodyEveryPartialStateAndOrder() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(6, targets.size());
        assertEquals(EXPECTED_IDENTITIES, targets.stream()
                .map(target -> target.path() + "\t" + target.before() + "\t" + target.after()).toList());
        assertEquals(2L, targets.stream().filter(M3GpuToolJoinManifest.Target::addition).count());
        assertTrue(targets.stream().noneMatch(M3GpuToolJoinManifest.Target::removal));
        var variable = targets.stream().filter(target -> !target.before().equals(target.after())).toList();
        assertEquals(4, variable.size());
        assertEquals(2L, targets.stream().filter(target -> target.text() == null).count());
        for (var target : targets) {
            if (target.text() != null) {
                assertEquals(target.after(), M3GpuToolJoinManifest.sha256(target.text()), target.path());
            }
            if (!target.addition()) {
                assertEquals(target.before(), M3GpuToolJoinManifest.sha256(
                        contents("before/" + Path.of(target.path()).getFileName())), target.path());
            }
        }
        int applications = 0;
        for (int mask = 0; mask < 16; mask++) {
            var input = before();
            for (int index = 0; index < variable.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    var target = variable.get(index);
                    input.put(target.path(), target.text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(4 - Integer.bitCount(mask), result.changes(), "mask=" + mask);
                assertEquals(7, result.files().size());
                for (var target : targets) {
                    assertEquals(target.after(),
                            M3GpuToolJoinManifest.sha256(result.files().get(target.path())), target.path());
                    if (target.text() == null) {
                        assertEquals(ordered.get(target.path()), result.files().get(target.path()), target.path());
                    }
                }
                assertEquals(input.get(OPAQUE), result.files().get(OPAQUE));
                assertEquals(0, apply(result.files()).changes(), "full fixed point mask=" + mask);
                if (mask == 0 && !reverse) {
                    for (var target : variable) retain(target.path(), result.files().get(target.path()));
                }
                applications++;
            }
        }
        assertEquals(32, applications);
        assertOutputContracts();
    }

    private static final List<String> EXPECTED_IDENTITIES = List.of(
            "m3/runtime-integration/recipe/apply.py\td1e98695bbb175acd6661f1d6f2ad2eb9dc90c5a67cc18da280b4c20aee771a5\tc25a6b4271d4c7e8c881adcd2a1500d06940ec58ab582d0ab4160a3436dfd2ad",
            "m3/runtime-integration/recipe/test_recipe.py\t46d731debe97ab67d455078e493ce92f75c40481c2727868baa79ef6a7f8bad8\ta8f86801f9343b9b61ce6159f3f978065caebf25f5f8d588b1fda4c66025872d",
            "m3/runtime-integration/recipe/current-cohort-20261017.json\tABSENT\ta2481edf200087aa9efb0d2825ef95a083124bf32a8182359b04c56b2d022bb3",
            ".github/workflows/m3-runtime-current-custody.yml\tABSENT\tfa76297daada18a5b638fb3dbb117cc5b1d36dcc3ac1f7611e39accfd85d845c",
            "m3/runtime-integration/recipe/manifest.json\t12a749485fcd401d8fba311e5b8911f30e10988a63a14ebcbd11edac3fbee643\t12a749485fcd401d8fba311e5b8911f30e10988a63a14ebcbd11edac3fbee643",
            "m3/runtime-integration/recipe/runtime.patch\tbfc58c6eb3c7540808839e937a0f64788d65eb64097d10407ecccf899560d0eb\tbfc58c6eb3c7540808839e937a0f64788d65eb64097d10407ecccf899560d0eb"
    );

    private static void assertOutputContracts() throws IOException {
        String applier = contents("out/apply.py");
        String original = contents("before/apply.py");
        String tests = contents("out/test_recipe.py");
        String cohort = contents("out/current-cohort-20261017.json");
        String workflow = contents("out/m3-runtime-current-custody.yml");
        assertTrue(applier.contains("def validate_current_cohort("));
        assertTrue(applier.contains("CURRENT_COHORT_SHA256 = \"a2481edf200087aa9efb0d2825ef95a083124bf32a8182359b04c56b2d022bb3\""));
        assertTrue(applier.contains("current-custody"));
        assertTrue(!original.contains("CURRENT_COHORT_SHA256"));
        assertTrue(cohort.contains("\"sourceCustodyOnly\": true"));
        for (String flag : List.of("runtimeQualified", "jniQualified", "jdkQualified",
                "performanceQualified", "phaseAdvanced")) {
            assertTrue(cohort.contains("\"" + flag + "\": false"), flag);
        }
        for (String method : List.of(
                "test_manifest_is_upstream_to_master_v2",
                "test_current_tree_is_exact_superseded_fixed_point",
                "test_every_current_target_matches_after_or_exact_superseded",
                "test_patch_bytes_remain_content_addressed",
                "test_each_unknown_drift_is_refused_without_partial_write",
                "test_modified_utf8_owner_fanin_matches_canonical_donor",
                "test_modified_utf8_partial_and_unreviewed_sources_are_refused",
                "test_symlink_target_is_refused",
                "test_exact_current_72_all_modes",
                "test_historical_patch_66_round_trip",
                "test_all_64_successor_masks",
                "test_all_16_modified_utf8_masks",
                "test_each_72_drift_missing_symlink_no_partial_writes",
                "test_cohort_shape_path_and_hash_refusals",
                "test_fixture_source_custody_guards",
                "test_legacy_does_not_read_auxiliaries_or_fixtures",
                "test_cli_does_not_read_fixture_data"
        )) {
            assertTrue(tests.contains("    def " + method + "("), method);
        }
        for (String anchor : List.of(
                "timeout-minutes: 15\n",
                "reserve, cap = 8 * 1024**3, 512 * 1024**2",
                "HOLD_STORAGE: 8 GiB reserve after remaining output",
                "preinstalled Temurin 21 required; no download",
                "ref: ${{ github.event.pull_request.head.sha || github.sha }}",
                "fetch-depth: 1\n", "fetch-tags: false\n",
                "persist-credentials: false\n", "sparse-checkout-cone-mode: false\n",
                "env.pop(\"GITHUB_TOKEN\", None)", "env.pop(\"GH_TOKEN\", None)",
                "start_new_session=True", "os.killpg(child.pid, signal.SIGTERM)",
                "os.killpg(child.pid, signal.SIGKILL)",
                "output.seek(max(0, log.stat().st_size - 32768))",
                "runtime custody checkout is not the exact candidate head",
                "runtime custody workflow differs from sealed recipe output",
                "pins[\\\"runtimeTests\\\"]",
                "actual runtime test inventory differs from pinned named methods",
                "runtime source custody test gate failed",
                "sourceCustodyOnly", "SOURCE_CUSTODY_AND_HISTORICAL_PATCH_REPLAY_ONLY",
                "NOT_RUN modified JDK/JNI/jtreg/runtime/performance",
                "if-no-files-found: error\n",
                "actions/checkout@11d5960a326750d5838078e36cf38b85af677262",
                "actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3",
                "actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02")) {
            assertTrue(workflow.contains(anchor), anchor);
        }
        int start = workflow.indexOf("          commands = [\n");
        int end = workflow.indexOf("          child = None\n", start);
        assertTrue(start >= 0 && end > start);
        assertEquals("          commands = [\n"
                + "              python + [str(sdk / \"verify.py\"), \"--self-test\", \"--repository\", str(source)],\n"
                + "              maven + [\"-f\", str(sdk / \"pom.xml\"), \"test\"],\n"
                + "              python + [str(sdk / \"verify.py\"), \"--reports\", \"--repository\", str(source)],\n"
                + "              python + [\"-c\", runtime_driver, str(source), str(root / \"runtime-custody-tests.json\")],\n"
                + "          ]\n", workflow.substring(start, end));
        assertTrue(!workflow.contains("metadata[\"moduleGitTree\"] != \""));
        assertTrue(!workflow.contains("metadata[\"pinsSha256\"] != \""));
        assertTrue(!workflow.contains("env[\"HOME\"]"));
        assertTrue(!workflow.contains("${{ env.HOME }}"));
    }

    @Test void runtimeCurrentCustodyRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        int refusals = 0;
        for (var target : manifest().targets()) {
            var baseline = before();
            assertEquals(4, apply(baseline).changes());
            var drifted = new LinkedHashMap<>(baseline);
            drifted.put(target.path(), "foreign source: " + target.path() + "\n");
            assertOnlyChanged(baseline, drifted, target.path());
            reject(sources(drifted), "source drift: " + target.path());
            refusals++;

            baseline = before();
            if (target.addition()) baseline.put(target.path(), target.text());
            assertEquals(target.addition() ? 3 : 4, apply(baseline).changes());
            var duplicate = sources(baseline);
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(baseline.get(target.path())).build());
            assertEquals(baseline.size() + 1, duplicate.size());
            reject(duplicate, "duplicate source target: " + target.path());
            refusals++;

            if (!target.addition()) {
                baseline = before();
                assertEquals(4, apply(baseline).changes());
                var missing = new LinkedHashMap<>(baseline);
                assertNotNull(missing.remove(target.path()));
                assertOnlyChanged(baseline, missing, target.path());
                reject(sources(missing), "required source missing: " + target.path());
                refusals++;
            }
        }
        assertEquals(16, refusals);
    }

    private static void assertOnlyChanged(Map<String, String> baseline,
                                           Map<String, String> changed, String culprit) {
        var beforeOther = new LinkedHashMap<>(baseline);
        var afterOther = new LinkedHashMap<>(changed);
        beforeOther.remove(culprit);
        afterOther.remove(culprit);
        assertEquals(beforeOther, afterOther, "only the exact intended target may change");
        assertTrue(!java.util.Objects.equals(baseline.get(culprit), changed.get(culprit)), culprit);
    }

    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty(),
                    "refused batch must emit no other pending owner");
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
                .resolve("target/emitted/runtime-current-custody").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
