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

/** Adapts the existing bounded workflow SDK to the preserved compiler worker and exact canonical mapping SDK. */
final class M3CanonicalMappingHostedWorkflowRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/";
    private static final String RECEIVING = ".github/workflows/m3-first-release-admission.yml";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3CanonicalMappingHostedWorkflowRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3CanonicalMappingHostedWorkflowRecipeTest.class::getResourceAsStream,
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
                .build().activateRecipes("com.synexia.m3.CanonicalMappingHostedWorkflow");
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.CanonicalMappingHostedWorkflow",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3CanonicalMappingHostedWorkflowRecipe.class),
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

    @Test void canonicalMappingWorkflowEveryPartialStateAndOrder() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(1, targets.size());
        var target = targets.get(0);
        assertEquals(RECEIVING, target.path());
        assertTrue(!target.addition() && !target.removal());
        assertTrue(!target.before().equals(target.after()));
        assertEquals("689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2", target.before());
        assertEquals("88ca043886bf8fcc4c9cc5488ad23bdb17ac005187fe44e17794874dd626d18d", target.after());
        assertEquals(target.before(), M3GpuToolJoinManifest.sha256(
                contents("before/m3-first-release-admission.yml")));
        assertEquals(target.after(), M3GpuToolJoinManifest.sha256(
                contents("out/m3-first-release-admission.yml")));
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


    private record Change(String label, String before, String after) { }

    private static void assertWorkflowPreservation(String receiving) throws IOException {
        String original = contents("before/m3-first-release-admission.yml");
        assertEquals(contents("out/m3-first-release-admission.yml"), receiving);
        // The exact eight edits are independent of the sealed after fixture.
        // Replay and reverse replay prove that every other workflow byte is retained.
        List<Change> changes = List.of(
                new Change("finite mapping SDK and ten receiver custody paths", "            /m3/runtime-integration/javac-regex-current-owner-20261017/\n", "            /m3/runtime-integration/javac-regex-current-owner-20261017/\n            /m3/migration/regex-reconciliation-20261017/\n            /m3/docs/name-mapping.schema.json\n            /m3/migration/migration.py\n            /src/java.base/share/classes/jdk/internal/mindex/M3TQ.java\n            /src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java\n            /src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java\n            /src/java.base/share/native/libjava/M3StateRelation.c\n            /src/java.base/share/native/libjava/m3_state_relation.h\n            /src/java.base/share/classes/java/lang/M3StringRegexDp.java\n            /test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java\n            /m3/docs/name-mapping.json\n"),
                new Change("separate immutable mapping and successor workflow SDK roots", "          workflow = receiving / \"workflow-canonical\"\n", "          workflow = receiving / \"workflow-canonical\"\n          mapping = source / \"m3/migration/regex-reconciliation-20261017/sdk\"\n          mapping_workflow = receiving / \"mapping-workflow-canonical\"\n"),
                new Change("frozen predecessor and mapping custody plus independent actual successor binding", "          workflow_metadata = json.loads((receiving / \"workflow-canonical-source.json\").read_bytes())\n          if workflow_metadata[\"repository\"] != \"hsoliwal/com.synexia\" or workflow_metadata[\"sourcePath\"] != \"synexia-openrewrite-recipes/recipes/m3-javac-regex-hosted-receiving-qualification-20261017/workflow-sdk\" or workflow_metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical\":\n              raise SystemExit(\"compiler workflow SDK source metadata drift\")\n          if workflow_metadata[\"runtimeApplied\"] is not False or workflow_metadata[\"phaseAdvanced\"] is not False:\n              raise SystemExit(\"compiler workflow SDK cannot advance runtime or phase\")\n          if workflow_metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\" or workflow_metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\":\n              raise SystemExit(\"compiler workflow SDK provider identity drift\")\n          workflow_tree = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:\" + workflow_metadata[\"receiverCorePath\"]], cwd=source, text=True).strip()\n          if workflow_tree != workflow_metadata[\"moduleGitTree\"] or hashlib.sha256((workflow / \"pins.json\").read_bytes()).hexdigest() != workflow_metadata[\"pinsSha256\"]:\n              raise SystemExit(\"compiler workflow SDK tree or pins drift\")\n          checked_workflow = source / \".github/workflows/m3-first-release-admission.yml\"\n          sealed_workflow = workflow / \"src/main/resources/com/synexia/rewrite/m3-javac-regex-hosted-receiving-qualification-20261017/out/m3-first-release-admission.yml\"\n          if hashlib.sha256(checked_workflow.read_bytes()).digest() != hashlib.sha256(sealed_workflow.read_bytes()).digest():\n              raise SystemExit(\"checked-out compiler workflow differs from sealed recipe output\")\n", "          # The original workflow SDK remains immutable and executes against its frozen predecessor fixture.\n          predecessor_metadata = (receiving / \"workflow-canonical-source.json\").read_bytes()\n          if hashlib.sha256(predecessor_metadata).hexdigest() != \"bcf127cea1e729efeae22e1a098f3935c869d8d04218169c7a6bbddc7a75413c\":\n              raise SystemExit(\"frozen predecessor workflow SDK metadata drift\")\n          workflow_metadata = json.loads(predecessor_metadata)\n          if workflow_metadata[\"repository\"] != \"hsoliwal/com.synexia\" or workflow_metadata[\"sourcePath\"] != \"synexia-openrewrite-recipes/recipes/m3-javac-regex-hosted-receiving-qualification-20261017/workflow-sdk\" or workflow_metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical\":\n              raise SystemExit(\"compiler workflow SDK source metadata drift\")\n          if workflow_metadata[\"runtimeApplied\"] is not False or workflow_metadata[\"phaseAdvanced\"] is not False:\n              raise SystemExit(\"compiler workflow SDK cannot advance runtime or phase\")\n          if workflow_metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\" or workflow_metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\":\n              raise SystemExit(\"compiler workflow SDK provider identity drift\")\n          workflow_tree = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:\" + workflow_metadata[\"receiverCorePath\"]], cwd=source, text=True).strip()\n          if workflow_tree != workflow_metadata[\"moduleGitTree\"] or hashlib.sha256((workflow / \"pins.json\").read_bytes()).hexdigest() != workflow_metadata[\"pinsSha256\"]:\n              raise SystemExit(\"compiler workflow SDK tree or pins drift\")\n          checked_workflow = source / \".github/workflows/m3-first-release-admission.yml\"\n          sealed_workflow = workflow / \"src/main/resources/com/synexia/rewrite/m3-javac-regex-hosted-receiving-qualification-20261017/out/m3-first-release-admission.yml\"\n          if hashlib.sha256(sealed_workflow.read_bytes()).hexdigest() != \"689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2\":\n              raise SystemExit(\"frozen predecessor workflow fixture drift\")\n          mapping_binding = (mapping.parent / \"source-binding.json\").read_bytes()\n          if hashlib.sha256(mapping_binding).hexdigest() != \"8c6aa71568828f0ddef6b6ecf0df637c478cb066adb204252bf531477f7e509e\":\n              raise SystemExit(\"canonical mapping SDK source binding drift\")\n          mapping_metadata = json.loads(mapping_binding)\n          mapping_tree = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:m3/migration/regex-reconciliation-20261017/sdk\"], cwd=source, text=True).strip()\n          if mapping_tree != \"acb7e31ace0871a7a4d1630ff899a19e922a9d52\" or mapping_metadata[\"sdk\"][\"core\"] != mapping_tree:\n              raise SystemExit(\"canonical mapping SDK tree drift\")\n          if hashlib.sha256((mapping / \"pins.json\").read_bytes()).hexdigest() != \"99e64f97550dfebede86c8d0fabfd3d2de2700a51e3b302af4676b1aeb2c9ab2\" or mapping_metadata[\"sdk\"][\"pinsSha256\"] != \"99e64f97550dfebede86c8d0fabfd3d2de2700a51e3b302af4676b1aeb2c9ab2\":\n              raise SystemExit(\"canonical mapping SDK pins drift\")\n          successor_metadata = json.loads((receiving / \"mapping-workflow-canonical-source.json\").read_bytes())\n          if successor_metadata[\"repository\"] != \"hsoliwal/com.synexia\" or successor_metadata[\"sourcePath\"] != \"synexia-openrewrite-recipes/recipes/m3-canonical-mapping-hosted-qualification-20261017/workflow-sdk\" or successor_metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical\":\n              raise SystemExit(\"mapping workflow SDK source metadata drift\")\n          if successor_metadata[\"runtimeApplied\"] is not False or successor_metadata[\"phaseAdvanced\"] is not False:\n              raise SystemExit(\"mapping workflow SDK cannot advance runtime or phase\")\n          if successor_metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\" or successor_metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\":\n              raise SystemExit(\"mapping workflow SDK provider identity drift\")\n          successor_tree = subprocess.check_output([\"git\", \"rev-parse\", \"HEAD:\" + successor_metadata[\"receiverCorePath\"]], cwd=source, text=True).strip()\n          if successor_tree != successor_metadata[\"moduleGitTree\"] or hashlib.sha256((mapping_workflow / \"pins.json\").read_bytes()).hexdigest() != successor_metadata[\"pinsSha256\"]:\n              raise SystemExit(\"mapping workflow SDK tree or pins drift\")\n          successor_workflow = mapping_workflow / \"src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/out/m3-first-release-admission.yml\"\n          if hashlib.sha256(checked_workflow.read_bytes()).digest() != hashlib.sha256(successor_workflow.read_bytes()).digest():\n              raise SystemExit(\"checked-out workflow differs from sealed successor recipe output\")\n"),
                new Change("six separately supervised mapping and successor recipe qualification commands", "              maven + [\"-f\", str(context / \"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml\"), \"verify\"],\n", "              python + [str(mapping / \"verify.py\"), \"--self-test\", \"--repository\", str(source)],\n              maven + [\"-f\", str(mapping / \"pom.xml\"), \"test\"],\n              python + [str(mapping / \"verify.py\"), \"--reports\", \"--repository\", str(source)],\n              python + [str(mapping_workflow / \"verify.py\")],\n              maven + [\"-f\", str(mapping_workflow / \"pom.xml\"), \"test\"],\n              python + [str(mapping_workflow / \"verify.py\"), \"--reports\"],\n              maven + [\"-f\", str(context / \"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml\"), \"verify\"],\n"),
                new Change("explicit combined SDK and command counts", "                  \"donatedSdkTests\": 7, \"workflowSdkTests\": 2,\n", "                  \"donatedSdkTests\": 7, \"workflowSdkTests\": 2,\n                  \"mappingSdkTests\": 3, \"mappingWorkflowSdkTests\": 2,\n                  \"totalSdkTests\": 14, \"supervisedCommands\": 16,\n"),
                new Change("executed mapping and successor receipt provenance", "                  \"workflowSdkReceiptSha256\": hashlib.sha256((workflow / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n", "                  \"workflowSdkReceiptSha256\": hashlib.sha256((workflow / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n                  \"mappingSdkReceiptSha256\": hashlib.sha256((mapping / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n                  \"mappingWorkflowSdkReceiptSha256\": hashlib.sha256((mapping_workflow / \"target/sdk-receipt.json\").read_bytes()).hexdigest(),\n                  \"mappingDonorCommit\": \"347ac7363aea1e9a7527df64ad62184534089fdf\",\n                  \"mappingSdkTree\": mapping_tree, \"mappingWorkflowSdkTree\": successor_tree,\n                  \"predecessorWorkflowFixtureSha256\": \"689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2\",\n                  \"wholeMigrationValidator\": \"NOT_RUN\",\n"),
                new Change("explicit supplemental successful receipt scope", "              print(\"PASS donated compiler SDK7 plus workflow SDK2 and JDK-only four-source contracts\")\n", "              print(\"PASS donated compiler SDK7 plus workflow SDK2 and JDK-only four-source contracts\")\n              print(\"PASS canonical mapping SDK3 plus successor workflow SDK2; combined SDK14 supervisedCommands16\")\n"),
                new Change("separate mapping and successor SDK artifacts", "            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/emitted/\n", "            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/emitted/\n            m3-bounded-read-source/m3/migration/regex-reconciliation-20261017/sdk/target/sdk-receipt.json\n            m3-bounded-read-source/m3/migration/regex-reconciliation-20261017/sdk/target/surefire-reports/TEST-*.xml\n            m3-bounded-read-source/m3/migration/regex-reconciliation-20261017/sdk/target/emitted/\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical/target/sdk-receipt.json\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical/target/surefire-reports/TEST-*.xml\n            m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical/target/emitted/\n"));
        assertEquals(8, changes.size());
        String replayed = original;
        for (Change change : changes) {
            replayed = replaceOnce(replayed, change.before(), change.after(), change.label());
        }
        assertEquals(receiving, replayed, "reviewed edits do not produce the exact emitted workflow");
        String restored = receiving;
        for (int index = changes.size() - 1; index >= 0; index--) {
            Change change = changes.get(index);
            restored = replaceOnce(restored, change.after(), change.before(), change.label());
        }
        assertEquals(original, restored, "unreviewed predecessor release workflow mutation");
        assertEquals(6L, original.substring(original.indexOf("jobs:\n")).lines()
                .filter(line -> line.matches("  [a-zA-Z0-9_-]+:")).count());
        assertEquals(6L, receiving.substring(receiving.indexOf("jobs:\n")).lines()
                .filter(line -> line.matches("  [a-zA-Z0-9_-]+:")).count());
        for (String id : List.of("contract", "foundation", "precompute", "control-reactor",
                "release-proof-summary")) {
            assertEquals(job(original, id), job(receiving, id), "existing job changed: " + id);
        }
        String bounded = job(receiving, "compiler-current-owner-qualification");
        assertEquals(6L, bounded.lines().filter(line -> line.startsWith("      - name: ")).count());
        assertTrue(!bounded.contains("    needs:"), "qualification must remain independent of older gates");
        for (String anchor : List.of(
                "    runs-on: ubuntu-24.04\n",
                "    timeout-minutes: 15\n",
                "reserve, cap = 8 * 1024**3, 512 * 1024**2",
                "HOLD_STORAGE: reserve after planned 512 MiB",
                "HOLD_STORAGE: aggregate source/cache/output exceeds 512 MiB",
                "HOLD_STORAGE: 8 GiB reserve after remaining output",
                "HOLD_STORAGE: footprint traversal failed",
                "HOLD_TOOLCHAIN: preinstalled Temurin 21 required; no download",
                "cache.glob(\"21.*\")",
                "version / \"x64.complete\"",
                "actions/checkout@11d5960a326750d5838078e36cf38b85af677262",
                "fetch-depth: 1\n",
                "fetch-tags: false\n",
                "lfs: false\n",
                "submodules: false\n",
                "persist-credentials: false\n",
                "sparse-checkout-cone-mode: false\n",
                "actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3",
                "distribution: temurin\n",
                "check-latest: false\n",
                "overwrite-settings: false\n",
                "token: ''\n",
                "settings-path: ${{ env.M3_PROOF_ROOT }}/home/.m2\n",
                "env.pop(\"GITHUB_TOKEN\", None)",
                "env.pop(\"GH_TOKEN\", None)",
                "env[\"MAVEN_USER_HOME\"] = str(root / \"home\" / \".m2\")",
                "env[\"JAVA_TOOL_OPTIONS\"] = \"-Dfile.encoding=UTF-8 -Djava.io.tmpdir=\" + str(root / \"tmp\") + \" -Duser.home=\" + str(root / \"home\") + \" -XX:+PerfDisableSharedMem\"",
                "-Dmaven.repo.local=",
                "-Duser.home=",
                "metadata[\"repository\"] != \"hsoliwal/com.synexia\"",
                "metadata[\"commit\"] != \"515f2f2153aa6afbc758da34c495c7223ee7997a\"",
                "metadata[\"runtimeApplied\"] is not False",
                "metadata[\"phaseAdvanced\"] is not False",
                "metadata[\"openrewriteAdapterGitBlob\"] != \"1c1b1b568d369f688f6953161636bf36b484bf5a\"",
                "metadata[\"manifestKernelGitBlob\"] != \"38df5f97bbeec8a8a8cfc77091d56bda925c07f2\"",
                "d81ce3df2695611b8fb1f4b0670a317c78d81d30",
                "0da95840e225e9e019967a449bd1343bda01085888572d985f6fc54a25927532",
                "ca001a5aaa98c0fb4f849bdee480065ccaec2f66fb250b970bf9a2668a766a7c",
                "8b044787430444c4ffd01d6bd0d312356b136ad2",
                "8de03168a897f3001b9f47b5dd18d539303eb60513eb5bc0f74ac0bd0944ba65",
                "b9253a8b08c4ea5fb3983aa58313c8181c2317b22f4145d3c5aa8d5082eee78a",
                "2514cebccb3d6f907dae29518f32ba30b3e076896f22b551288338c3bcd5290e",
                "75162bca704fd006ab80c655d65184217e6ecc67",
                "if len(rows) != 17 or len({row[\"path\"] for row in rows}) != 17:",
                "if actual != expected:",
                "identity != row[\"gitBlob\"]",
                "workflow_metadata[\"runtimeApplied\"] is not False",
                "workflow_metadata[\"phaseAdvanced\"] is not False",
                "workflow_metadata[\"receiverCorePath\"] != \"m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical\"",
                "workflow_metadata[\"sourcePath\"] != \"synexia-openrewrite-recipes/recipes/m3-javac-regex-hosted-receiving-qualification-20261017/workflow-sdk\"",
                "workflow_tree != workflow_metadata[\"moduleGitTree\"]",
                "hashlib.sha256((workflow / \"pins.json\").read_bytes()).hexdigest() != workflow_metadata[\"pinsSha256\"]",
                "                  for owned_command in commands:\n                      gate()\n",
                "child = subprocess.Popen(owned_command, cwd=source, env=env, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)",
                "                      while child.poll() is None:\n                          gate()\n                          time.sleep(1)\n",
                "os.killpg(child.pid, signal.SIGTERM)",
                "os.killpg(child.pid, signal.SIGKILL)",
                "output.seek(max(0, log.stat().st_size - 32768))",
                "if any(count != 1 for count in observed.values()) or len(matches) != 1:",
                "plan_match = re.fullmatch(r\"MIndexJdkConstantRegexPlanContract checks=([1-9][0-9]*) PASS\", line)",
                "if len(plan_counts) > 1:",
                "if len(plan_counts) != 1 or not 70326 <= plan_counts[0] <= 70330:",
                "\"originalPlanCorpusChecks\": plan_counts[0], \"newContractChecks\": 11552,",
                "\"MIndexJdkConstantRegexCurrentOwnerContract addedChecks=11552 charSequenceComparisons=10800 scope=JDK_CHARSEQUENCE_ONLY PASS\"",
                "\"M3_JAVAC_CURRENT_OWNER_REGEX_JDK21=PASS scope=JDK_ONLY_FOUR_FILE_CONTRACTS\"",
                "\"M3_JAVAC_CURRENT_OWNER_SOURCE_SEALS=PASS owners=6 historicalInputs=6 qualificationOutputs=3 originalCorpus=EXACT\"",
                "\"modifiedJdkQualified\": False, \"realMIndexBridgeQualified\": False,",
                "\"jniQualified\": False, \"runtimeApplied\": False, \"phaseAdvanced\": False,",
                "PASS donated compiler SDK7 plus workflow SDK2 and JDK-only four-source contracts",
                "NOT_RUN real MIndex bridge, modified JDK/JNI/runtime/performance; phase remains unchanged",
                "          mapping = source / \"m3/migration/regex-reconciliation-20261017/sdk\"\n",
                "          mapping_workflow = receiving / \"mapping-workflow-canonical\"\n",
                "predecessor_metadata).hexdigest() != \"bcf127cea1e729efeae22e1a098f3935c869d8d04218169c7a6bbddc7a75413c\"",
                "hashlib.sha256(sealed_workflow.read_bytes()).hexdigest() != \"689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2\"",
                "mapping_binding).hexdigest() != \"8c6aa71568828f0ddef6b6ecf0df637c478cb066adb204252bf531477f7e509e\"",
                "mapping_tree != \"acb7e31ace0871a7a4d1630ff899a19e922a9d52\"",
                "mapping_metadata[\"sdk\"][\"core\"] != mapping_tree",
                "mapping_metadata[\"sdk\"][\"pinsSha256\"] != \"99e64f97550dfebede86c8d0fabfd3d2de2700a51e3b302af4676b1aeb2c9ab2\"",
                "successor_metadata[\"runtimeApplied\"] is not False",
                "successor_metadata[\"phaseAdvanced\"] is not False",
                "successor_tree != successor_metadata[\"moduleGitTree\"]",
                "hashlib.sha256((mapping_workflow / \"pins.json\").read_bytes()).hexdigest() != successor_metadata[\"pinsSha256\"]",
                "\"mappingSdkTests\": 3, \"mappingWorkflowSdkTests\": 2,",
                "\"totalSdkTests\": 14, \"supervisedCommands\": 16,",
                "\"wholeMigrationValidator\": \"NOT_RUN\",",
                "\"mappingDonorCommit\": \"347ac7363aea1e9a7527df64ad62184534089fdf\",",
                "checked-out workflow differs from sealed successor recipe output",
                "PASS canonical mapping SDK3 plus successor workflow SDK2; combined SDK14 supervisedCommands16")) {
            assertTrue(bounded.contains(anchor), anchor);
        }
        for (String name : List.of("TMPDIR", "TEMP", "TMP", "XDG_CACHE_HOME", "XDG_CONFIG_HOME", "XDG_DATA_HOME")) {
            assertTrue(bounded.contains("          " + name + ": "), name);
        }
        assertTrue(bounded.lines().noneMatch(line -> line.matches("\\s*HOME:.*")));
        assertTrue(!bounded.contains("${{ env.HOME }}"));
        assertTrue(!bounded.contains("env[\"HOME\"]"));
        assertTrue(!bounded.contains("os.environ[\"HOME\"] ="));
        assertTrue(!bounded.contains("successor_tree != \""),
                "successor self-metadata must avoid a core hash cycle");
        assertEquals(List.of(
                "/m3/runtime-integration/javac-regex-current-owner-20261017/",
                "/m3/migration/regex-reconciliation-20261017/",
                "/m3/docs/name-mapping.schema.json",
                "/m3/migration/migration.py",
                "/src/java.base/share/classes/jdk/internal/mindex/M3TQ.java",
                "/src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java",
                "/src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java",
                "/src/java.base/share/native/libjava/M3StateRelation.c",
                "/src/java.base/share/native/libjava/m3_state_relation.h",
                "/src/java.base/share/classes/java/lang/M3StringRegexDp.java",
                "/test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java",
                "/m3/docs/name-mapping.json",
                "/.github/workflows/m3-first-release-admission.yml",
                "/.gitattributes"),
                section(bounded, "          sparse-checkout: |\n", "      # Sparse checkout")
                        .lines().skip(1).map(String::strip).toList());
        String actualSourceGate =
                "          if hashlib.sha256(checked_workflow.read_bytes()).digest() != hashlib.sha256(successor_workflow.read_bytes()).digest():\n";
        assertTrue(bounded.contains(actualSourceGate));
        assertTrue(bounded.indexOf(actualSourceGate) < bounded.indexOf("          commands = [\n"));
        assertTrue(!bounded.contains(
                "hashlib.sha256(checked_workflow.read_bytes()).digest() != hashlib.sha256(sealed_workflow.read_bytes()).digest()"),
                "actual workflow must be bound by the successor, not the frozen predecessor");
        String commands = section(bounded, "          commands = [\n", "          child = None\n");
        assertEquals("          commands = [\n              python + [str(current / \"verify.py\"), \"--inputs\", \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(current / \"pom.xml\"), \"test\"],\n              python + [str(current / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(fixture / \"verify.py\"), \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(fixture / \"pom.xml\"), \"test\"],\n              python + [str(fixture / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(workflow / \"verify.py\")],\n              maven + [\"-f\", str(workflow / \"pom.xml\"), \"test\"],\n              python + [str(workflow / \"verify.py\"), \"--reports\"],\n              python + [str(mapping / \"verify.py\"), \"--self-test\", \"--repository\", str(source)],\n              maven + [\"-f\", str(mapping / \"pom.xml\"), \"test\"],\n              python + [str(mapping / \"verify.py\"), \"--reports\", \"--repository\", str(source)],\n              python + [str(mapping_workflow / \"verify.py\")],\n              maven + [\"-f\", str(mapping_workflow / \"pom.xml\"), \"test\"],\n              python + [str(mapping_workflow / \"verify.py\"), \"--reports\"],\n              maven + [\"-f\", str(context / \"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml\"), \"verify\"],\n          ]\n", commands, "all sixteen owned commands retain exact order");
        assertEquals(16L, commands.lines().filter(line -> line.startsWith("              python +")
                || line.startsWith("              maven +")).count());
        assertEquals("          commands = [\n              python + [str(current / \"verify.py\"), \"--inputs\", \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(current / \"pom.xml\"), \"test\"],\n              python + [str(current / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(fixture / \"verify.py\"), \"--self-test\", \"--repository\", str(context)],\n              maven + [\"-f\", str(fixture / \"pom.xml\"), \"test\"],\n              python + [str(fixture / \"verify.py\"), \"--reports\", \"--repository\", str(context)],\n              python + [str(workflow / \"verify.py\")],\n              maven + [\"-f\", str(workflow / \"pom.xml\"), \"test\"],\n              python + [str(workflow / \"verify.py\"), \"--reports\"],\n              maven + [\"-f\", str(context / \"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml\"), \"verify\"],\n          ]\n",
                replaceOnce(commands, "              python + [str(mapping / \"verify.py\"), \"--self-test\", \"--repository\", str(source)],\n              maven + [\"-f\", str(mapping / \"pom.xml\"), \"test\"],\n              python + [str(mapping / \"verify.py\"), \"--reports\", \"--repository\", str(source)],\n              python + [str(mapping_workflow / \"verify.py\")],\n              maven + [\"-f\", str(mapping_workflow / \"pom.xml\"), \"test\"],\n              python + [str(mapping_workflow / \"verify.py\"), \"--reports\"],\n", "",
                        "six mapping and successor commands"),
                "all ten predecessor commands must remain byte exact and ordered");
        String artifacts = bounded.substring(bounded.indexOf(
                "      - name: Retain exact compiler SDK and contract evidence\n"));
        for (String anchor : List.of("        if: always()\n",
                "actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02",
                "          include-hidden-files: true\n", "          retention-days: 90\n",
                "          if-no-files-found: error\n")) {
            assertTrue(artifacts.contains(anchor), anchor);
        }
        assertEquals(List.of(
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/current-canonical/target/emitted/",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/fixture-canonical/target/emitted/",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/workflow-canonical/target/emitted/",
                "m3-bounded-read-source/m3/migration/regex-reconciliation-20261017/sdk/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/migration/regex-reconciliation-20261017/sdk/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/migration/regex-reconciliation-20261017/sdk/target/emitted/",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical/target/sdk-receipt.json",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical/target/surefire-reports/TEST-*.xml",
                "m3-bounded-read-source/m3/runtime-integration/javac-regex-current-owner-20261017/mapping-workflow-canonical/target/emitted/",
                "${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/maven.log",
                "${{ runner.temp }}/m3-bounded-read-${{ github.run_id }}-${{ github.run_attempt }}/compiler-contract-receipt.json"),
                section(artifacts, "          path: |\n", "          include-hidden-files: true\n")
                        .lines().skip(1).map(String::strip).toList());
    }

    private static String replaceOnce(String text, String before, String after, String label) {
        int first = text.indexOf(before);
        assertTrue(first >= 0, "missing reviewed edit: " + label);
        assertEquals(first, text.lastIndexOf(before), "duplicate reviewed edit: " + label);
        return text.substring(0, first) + after + text.substring(first + before.length());
    }

    private static String job(String text, String id) {
        String header = "  " + id + ":\n";
        int begin = text.indexOf(header);
        assertTrue(begin >= 0, header);
        assertEquals(begin, text.lastIndexOf(header), "duplicate job " + id);
        var following = java.util.regex.Pattern.compile("(?m)^  [a-zA-Z0-9_-]+:\\n")
                .matcher(text).region(begin + header.length(), text.length());
        return text.substring(begin, following.find() ? following.start() : text.length());
    }

    private static String section(String text, String start, String end) {
        int begin = text.indexOf(start);
        assertTrue(begin >= 0, start);
        int finish = text.indexOf(end, begin + start.length());
        assertTrue(finish > begin, end);
        return text.substring(begin, finish);
    }

    @Test void canonicalMappingWorkflowRefusesDriftMissingAndDuplicateOwnership() throws Exception {
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
                .resolve("target/emitted/canonical-mapping-hosted-workflow").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
