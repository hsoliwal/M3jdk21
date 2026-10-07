// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** UNBOUND recovery scaffold: real source publication and reviewed resources are required before execution. */
final class JccSourceRecoveryHandoffTest {
    private static final String CRATE = "jcc-source-recovery-handoff-20261005";
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private static final String PREVIOUS_SOURCE = "d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906";
    private static final String DESTINATION_PREIMAGE_COMMIT = "0994ecd65e86600f417f4d8702838d8c2663af61";
    private static final String DESTINATION_PREIMAGE_ROOT = "7e8f39ac999174f8d5b6028d638a68d8c317b070";
    // Deliberately unbound until the actual recovery commit, root, ref and scoped inventory are published/read back.
    private static final String FINAL_SOURCE_COMMIT = "0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8";
    private static final String FINAL_SOURCE_ROOT_TREE = "0361e4a07b77a01df170523033fef01c4052ddc5";
    private static final String FINAL_SOURCE_REF = "refs/heads/aix/jcc-source-merge-recovery-20261005";
    private static final String RECOVERY_INPUT = "be92c62ece9023b5c33676716a1076d00e26120a";
    private static final String RECOVERY_INPUT_ROOT = "3c4f32b66633a251ba2c117090830252a7e2da03";
    private static final String RECEIVER_COMMIT = "df06cdee5a8526f573b3ad89622824d0b49e2f25";
    private static final String PREVIOUS_BINDING_RESOURCE = "com/m3/rewrite/backport/jdk21-hash-pinned-text/"
            + "jcc-source-final-handoff-20261005/after02-source-destination-bindings.json.txt";
    private static final int EXPECTED_RECOVERY_SOURCE_COUNT = 19;
    private static final String REGISTRY = "m3/docs/name-mapping.json";
    private static final String LAB = "synexia.jcc-recipe-laboratory";
    private static final String JNI = "synexia.jcc-java-jni-regression";
    private static final String RECEIVER = "com.m3.rewrite.atom.JccReceivingFixtureTest";
    private static final Set<String> JCC_IDS = Set.of(LAB, JNI);
    private static final ObjectMapper JSON = new ObjectMapper();

    private record Target(String path, String beforeResource, String afterResource, String beforeHash) {}

    private static final List<Target> OUTPUTS = List.of(
            new Target(REGISTRY, "before00-name-mapping.json.txt", "after00-name-mapping.json.txt",
                    "e4ae516fea9822a4bfe26669e9749e167f52bb30049d520339ee42f3779bc0b3"),
            new Target("m3/docs/jcc-source-handoff.md", "before01-jcc-source-handoff.md.txt",
                    "after01-jcc-source-handoff.md.txt",
                    "59ed76969f86486e7adb377409e284f69577fe02754006fdfe5b3938a16760a0"),
            new Target("m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
                    "before02-source-destination-bindings.json.txt", "after02-source-destination-bindings.json.txt",
                    "049c7c1987c05a15f4152fcb8f261a65245d72a25688d5a530a4afa5405cd3ef"),
            new Target("m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv",
                    "before03-root-coverage-obligations.tsv.txt", "after03-root-coverage-obligations.tsv.txt",
                    "59c4c970ac75a56aa5d5a3e2f3e88f5caa831eb06be788266c725202b8c64b12"));

    @Test
    void namedEntryPointAndSerializedRetainedOwnerReplayAllFourExistingInputs() throws IOException {
        verifyManifest();
        RecipeSerializer serializer = new RecipeSerializer();
        M3Jdk21HashPinnedTextSnapshotRecipe restored = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                serializer.read(serializer.write(recipe())));
        assertEquals(CRATE, restored.getCrateName());
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.JccSourceRecoveryHandoff");
        for (Recipe entry : List.of(restored, named)) {
            var errors = new ArrayList<Throwable>();
            var result = entry.run(new InMemoryLargeSourceSet(beforeSources()),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            var actual = new TreeMap<String, String>();
            for (var change : result.getChangeset().getAllResults()) {
                assertNotNull(change.getBefore(), "The successor creates no formerly absent target");
                SourceFile after = assertInstanceOf(PlainText.class, change.getAfter());
                String path = normalized(after);
                assertFalse(actual.containsKey(path));
                actual.put(path, after.printAll());
            }
            assertEquals(expectedAfter(), actual, entry.getName());
        }
    }

    @Test
    void allFourUpdatesPreserveIdentityAndFreshSecondPassIsUnchanged() throws IOException {
        verifyManifest();
        var input = beforeSources();
        var beforeByPath = new TreeMap<String, SourceFile>();
        for (SourceFile source : input) beforeByPath.put(normalized(source), source);
        SourceFile unrelated = text("unrelated.txt", "retain\n");
        SourceFile neighboring = text(REGISTRY + ".unrelated", "retain adjacent path\n");
        input.add(unrelated);
        input.add(neighboring);
        var errors = new ArrayList<Throwable>();
        var result = recipe().run(new InMemoryLargeSourceSet(input),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(4, result.getChangeset().getAllResults().size());
        var actual = new TreeMap<String, String>();
        var afterInputs = new ArrayList<SourceFile>();
        for (var change : result.getChangeset().getAllResults()) {
            SourceFile output = assertInstanceOf(PlainText.class, change.getAfter());
            String path = normalized(output);
            assertFalse(actual.containsKey(path));
            SourceFile original = beforeByPath.get(path);
            assertNotNull(original, path);
            assertNotNull(change.getBefore());
            assertEquals(original.getId(), output.getId());
            assertEquals(original.printAll(), change.getBefore().printAll());
            assertEquals(original.getCharset(), output.getCharset());
            actual.put(path, output.printAll());
            afterInputs.add(output);
        }
        assertEquals(expectedAfter(), actual);
        afterInputs.add(unrelated);
        afterInputs.add(neighboring);
        var freshErrors = new ArrayList<Throwable>();
        var replay = recipe().run(new InMemoryLargeSourceSet(afterInputs),
                new InMemoryExecutionContext(freshErrors::add), 1);
        assertTrue(freshErrors.isEmpty(), freshErrors.toString());
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
        assertEquals("retain\n", unrelated.printAll());
        assertEquals("retain adjacent path\n", neighboring.printAll());
        for (Target target : OUTPUTS)
            assertEquals(target.beforeHash(), hash(beforeByPath.get(target.path()).printAll()));
    }

    @Test
    void fortyFourOtherRecordsAllIdsOrderGlobalsAndTwentyGatesStayExact() throws IOException {
        JsonNode before = json("before00-name-mapping.json.txt");
        JsonNode after = json("after00-name-mapping.json.txt");
        ObjectNode oldGlobals = before.deepCopy();
        ObjectNode newGlobals = after.deepCopy();
        ((ObjectNode) oldGlobals.path("migration")).remove("records");
        ((ObjectNode) newGlobals.path("migration")).remove("records");
        assertEquals(oldGlobals, newGlobals);
        assertEquals(20, before.path("migration").path("gates").size());
        JsonNode oldRows = before.path("migration").path("records");
        JsonNode newRows = after.path("migration").path("records");
        assertEquals(46, oldRows.size());
        assertEquals(46, newRows.size());
        var ids = new HashSet<String>();
        int unchanged = 0;
        for (int i = 0; i < oldRows.size(); i++) {
            JsonNode old = oldRows.get(i);
            JsonNode current = newRows.get(i);
            String id = old.path("id").asText();
            assertTrue(ids.add(id));
            assertEquals(old.path("id"), current.path("id"), "Existing record order");
            if (!JCC_IDS.contains(id)) {
                assertEquals(old, current, id);
                unchanged++;
            } else {
                for (String field : List.of("kind", "owner", "identity", "contract", "format", "dependencies", "provenance"))
                    assertEquals(old.path(field), current.path(field), id + ": " + field);
            }
        }
        assertEquals(44, unchanged, "Includes the complete Descriptor record and its history");
        assertTrue(ids.containsAll(JCC_IDS));
    }

    @Test
    void completeSeventeenSourceObjectsAndAllEarlierLineageAreRetainedExactly() throws IOException {
        requireBoundSource();
        int retainedSources = 0;
        int activeSources = 0;
        var activeSymbols = new HashSet<String>();
        for (String id : JCC_IDS) {
            JsonNode old = record("before00-name-mapping.json.txt", id);
            JsonNode current = record("after00-name-mapping.json.txt", id);
            var expectedHistory = JSON.createArrayNode();
            for (JsonNode item : old.path("lineage").path("previous_sources")) expectedHistory.add(item);
            for (JsonNode previous : old.path("sources")) {
                assertEquals(PREVIOUS_SOURCE, previous.path("commit").asText());
                expectedHistory.add(previous);
                int continued = 0;
                for (JsonNode source : current.path("sources")) if (sameLocation(previous, source)) continued++;
                assertEquals(1, continued, previous.path("symbol").asText());
                retainedSources++;
            }
            assertEquals(expectedHistory, current.path("lineage").path("previous_sources"),
                    "Preserve full previous objects and order, not only locations or selected hashes");
            for (String field : List.of("previous_targets", "supersedes"))
                assertEquals(old.path("lineage").path(field), current.path("lineage").path(field));
            for (JsonNode source : current.path("sources")) {
                assertEquals("hsoliwal/com.synexia", source.path("repo").asText());
                assertEquals(FINAL_SOURCE_COMMIT, source.path("commit").asText());
                assertEquals("pinned", source.path("revision_role").asText());
                assertEquals(FINAL_SOURCE_REF, source.path("tracking_ref").asText());
                assertTrue(source.path("sha256").asText().matches("[0-9a-f]{64}"));
                assertTrue(source.path("git_blob_sha1").asText().matches("[0-9a-f]{40}"));
                assertTrue(activeSymbols.add(source.path("symbol").asText()), "Duplicate scoped owner");
                activeSources++;
            }
        }
        assertEquals(17, retainedSources);
        assertEquals(EXPECTED_RECOVERY_SOURCE_COUNT, activeSources,
                "Bind the actual qualified selected inventory, not a guessed helper count");
        assertTrue(activeSymbols.containsAll(Set.of("com.synexia.rewrite.M3RecipeMasteryLab",
                "com.synexia.rewrite.M3MasteryClassLoader", "com.synexia.rewrite.M3MasteryContractSurface",
                "com.synexia.rewrite.M3OpenRewriteTranspiler")), "Retained Mastery helper and shared runner obligations");
    }

    @Test
    void everyReceiverTargetAndItsNullCandidateHistoryRemainExact() throws IOException {
        for (String id : JCC_IDS) {
            JsonNode old = record("before00-name-mapping.json.txt", id);
            JsonNode current = record("after00-name-mapping.json.txt", id);
            assertEquals(old.path("targets"), current.path("targets"));
            assertEquals(old.path("lineage").path("previous_targets"),
                    current.path("lineage").path("previous_targets"));
        }
        JsonNode current = record("after00-name-mapping.json.txt", LAB);
        int receiver = 0;
        for (JsonNode target : current.path("targets")) if (RECEIVER.equals(target.path("symbol").asText())) {
            assertEquals(RECEIVER_COMMIT, target.path("commit").asText());
            assertEquals("pinned", target.path("revision_role").asText());
            assertEquals(target, json("after02-source-destination-bindings.json.txt")
                    .path("independent_receiving_fixture").path("candidate_artifact"));
            receiver++;
        }
        assertEquals(1, receiver);
        int original = 0;
        for (JsonNode old : current.path("lineage").path("previous_targets"))
            if (RECEIVER.equals(old.path("symbol").asText())) {
                assertTrue(old.path("commit").isNull());
                assertEquals("candidate", old.path("revision_role").asText());
                original++;
            }
        assertEquals(1, original, "The exact original null candidate remains historical");
    }

    @Test
    void historicalDescriptorAndCompleteExecutedReceiverEvidenceRemainExact() throws IOException {
        JsonNode before = json("before02-source-destination-bindings.json.txt");
        JsonNode after = json("after02-source-destination-bindings.json.txt");
        assertEquals(before.path("descriptor_reconciliation"), after.path("descriptor_reconciliation"));
        assertEquals(before.path("independent_receiving_fixture"), after.path("independent_receiving_fixture"),
                "No receiver repin, behavior rerun or rewriting of its original proof epoch");
        assertEquals(before.path("existing_destination_reuse_artifacts"),
                after.path("existing_destination_reuse_artifacts"));
    }

    @Test
    void previousSourceProofPacketIsRetainedAndCurrentEvidenceHasItsOwnInputEpoch() throws IOException {
        requireBoundSource();
        String retained;
        try (var input = JccSourceRecoveryHandoffTest.class.getResourceAsStream("/" + PREVIOUS_BINDING_RESOURCE)) {
            assertNotNull(input, "Complete published E3 source/proof packet is required");
            retained = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String beforeText = resource("before02-source-destination-bindings.json.txt");
        assertEquals(beforeText, retained, "Retain complete E3 publication/execution/prior-handoff evidence");
        JsonNode before = JSON.readTree(beforeText);
        JsonNode after = json("after02-source-destination-bindings.json.txt");
        JsonNode prior = after.path("prior_handoff");
        assertEquals(DESTINATION_PREIMAGE_COMMIT, prior.path("commit").asText());
        assertEquals(DESTINATION_PREIMAGE_ROOT, prior.path("root_tree").asText());
        assertEquals(PREVIOUS_SOURCE, prior.path("old_source_commit").asText());
        assertEquals("m3/tooling/migration-recipes/src/main/resources/" + PREVIOUS_BINDING_RESOURCE,
                prior.path("old_binding_resource").asText());
        assertEquals(hash(retained), prior.path("old_binding_sha256").asText());
        JsonNode historical = before.path("source_execution");
        assertEquals("0b8dc32b9e8a616b7b7141bbdd88722839dc64bc",
                historical.path("input_remote_revision").asText());
        assertEquals(PREVIOUS_SOURCE, historical.path("published_output_revision").asText());
        assertEquals(1, historical.path("driver_exit_code").asInt(-1));
        assertEquals(13, historical.path("unrun_driver_gates").size());
        JsonNode context = after.path("source_current_context");
        assertEquals(RECOVERY_INPUT, context.path("input_commit").asText());
        assertEquals(RECOVERY_INPUT_ROOT, context.path("input_root_tree").asText());
        assertEquals(EXPECTED_RECOVERY_SOURCE_COUNT, context.path("designated_binding_count").asInt(-1));
        assertEquals(503, context.path("original_source_packet_count").asInt(-1));
        assertEquals(527, context.path("current_context_count").asInt(-1));
        assertEquals(446, context.path("current_main_explicit_count").asInt(-1));
        assertEquals(109, context.path("current_parent_count").asInt(-1));
        assertEquals(88, context.path("current_parent_main").asInt(-1));
        assertEquals(19, context.path("current_parent_release8_vendor").asInt(-1));
        Set<String> requiredContext = Set.of(
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/PSource.java",
                "synexia-mindex/native-jni/include/synexia_mindex.h",
                "synexia-openrewrite-recipes/recipes/atom-pattern-mastery-20261005/pom.xml");
        var actualContext = new HashSet<String>();
        for (JsonNode input : context.path("explicit_context_artifacts")) {
            assertEquals(RECOVERY_INPUT, input.path("commit").asText());
            assertTrue(input.path("sha256").asText().matches("[0-9a-f]{64}"));
            assertTrue(input.path("git_blob_sha1").asText().matches("[0-9a-f]{40}"));
            assertEquals("CURRENT_INPUT_AVAILABILITY_ONLY", input.path("qualification").asText());
            assertTrue(actualContext.add(input.path("path").asText()));
        }
        assertEquals(requiredContext, actualContext);
        for (String receipt : List.of("CURRENT_SOURCE_MANIFEST.json", "PARENT_BUILD_CONTEXT.json",
                "CURRENT_PROJECTION.json", "CURRENT_CONTEXT_MANIFEST.json", "BUILD_ROLE_MAP.json",
                "RECOVERY_SOURCE_INCLUDES.txt")) {
            assertFalse(context.path("receipts").path(receipt).path("path").asText().isBlank());
            assertTrue(context.path("receipts").path(receipt).path("sha256").asText().matches("[0-9a-f]{64}"));
        }
        JsonNode recovery = after.path("source_execution");
        assertEquals(RECOVERY_INPUT, recovery.path("input_remote_revision").asText());
        assertEquals(RECOVERY_INPUT_ROOT, recovery.path("input_remote_root_tree").asText());
        assertEquals(FINAL_SOURCE_COMMIT, recovery.path("published_output_revision").asText());
        assertNotEquals(historical, recovery, "Original pass counts and failures cannot be transferred");
        assertTrue(recovery.path("proofs").isArray());
        assertFalse(recovery.path("proofs").isEmpty());
        for (JsonNode proof : recovery.path("proofs")) {
            assertEquals(FINAL_SOURCE_COMMIT, proof.path("commit").asText());
            assertTrue(proof.path("sha256").asText().matches("[0-9a-f]{64}"));
            assertTrue(proof.path("git_blob_sha1").asText().matches("[0-9a-f]{40}"));
        }
    }

    @Test
    void recoverySourceIdentityNeverPromotesBlockedCapabilityOrTransfersProof() throws IOException {
        requireBoundSource();
        for (String id : JCC_IDS) {
            JsonNode row = record("after00-name-mapping.json.txt", id);
            assertEquals("blocked", row.path("status").asText());
            assertTrue(row.path("tests").isArray());
            assertEquals(0, row.path("tests").size());
            assertEquals(FINAL_SOURCE_COMMIT, row.path("sync").path("source_revision").asText());
            assertEquals(DESTINATION_PREIMAGE_COMMIT, row.path("sync").path("target_revision").asText());
            assertTrue(row.path("sync").path("pending").size() > 0);
            JsonNode old = record("before00-name-mapping.json.txt", id);
            assertEquals(old.path("recipe").path("path"), row.path("recipe").path("path"));
            assertEquals(old.path("recipe").path("sha256"), row.path("recipe").path("sha256"));
        }
        JsonNode binding = json("after02-source-destination-bindings.json.txt");
        assertEquals(FINAL_SOURCE_COMMIT, binding.path("source_commit").asText());
        assertEquals(FINAL_SOURCE_ROOT_TREE, binding.path("source_root_tree").asText());
        assertEquals(DESTINATION_PREIMAGE_COMMIT, binding.path("destination_commit").asText());
        assertEquals(DESTINATION_PREIMAGE_ROOT, binding.path("destination_root_tree").asText());
        JsonNode plan = json("plan.json");
        assertEquals("jcc-source-recovery-handoff-20261005/1", plan.path("recipe_id").asText());
        assertEquals(FINAL_SOURCE_COMMIT, plan.path("source_commit").asText());
        assertEquals(DESTINATION_PREIMAGE_COMMIT, plan.path("target_commit").asText());
        JsonNode acceptance = binding.path("acceptance");
        Set<String> obligations = Set.of("source_export_admitted", "destination_materialized",
                "destination_gates_passed", "remote_read_back_delivered");
        assertEquals(obligations.size(), acceptance.size());
        for (String obligation : obligations) {
            assertTrue(acceptance.path(obligation).isBoolean(), obligation);
            assertFalse(acceptance.path(obligation).booleanValue(), obligation);
        }
        JsonNode fixture = binding.path("independent_receiving_fixture");
        assertTrue(fixture.path("materialized_locally").asBoolean());
        assertFalse(fixture.path("source_export_or_jdk_acceptance").asBoolean(true));
    }

    @Test
    void everyDriftedExistingTargetRefusesWithoutPartialResults() throws IOException {
        for (Target changed : OUTPUTS) {
            var input = beforeSources();
            input.removeIf(source -> normalized(source).equals(changed.path()));
            input.add(text(changed.path(), "foreign input\n"));
            reject(input, "source drift: " + changed.path());
        }
    }

    @Test
    void everyDuplicateExistingTargetRefusesEvenWithValidPostimageBytes() throws IOException {
        for (Target duplicate : OUTPUTS) {
            var input = beforeSources();
            input.add(text(duplicate.path(), resource(duplicate.afterResource())));
            reject(input, "duplicate target: " + duplicate.path());
        }
    }

    @Test
    void everyMissingExistingPreimageRefusesTheWholeTextPass() throws IOException {
        for (Target missing : OUTPUTS) {
            var input = beforeSources();
            input.removeIf(source -> normalized(source).equals(missing.path()));
            reject(input, "required JDK21 text source missing: " + missing.path());
        }
    }

    @Test
    void everyMutationBetweenCompleteScanAndVisitRefuses() throws IOException {
        for (Target changed : OUTPUTS) {
            var owner = recipe();
            var context = new InMemoryExecutionContext(failure -> {
                throw new AssertionError("Unexpected phase error", failure);
            });
            var inventory = owner.getInitialValue(context);
            var scanner = owner.getScanner(inventory);
            for (SourceFile source : beforeSources()) scanner.visit(source, context);
            assertTrue(owner.generate(inventory, context).isEmpty(), "Four updates, zero additions");
            var visitor = owner.getVisitor(inventory);
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> visitor.visit(text(changed.path(), "changed after scan\n"), context));
            assertTrue(messages(failure).contains("JDK21 text target changed after scan: " + changed.path()));
        }
    }

    private static void verifyManifest() throws IOException {
        var expected = new TreeMap<String, Target>();
        for (Target target : OUTPUTS) expected.put(target.path(), target);
        var seen = new HashSet<String>();
        String previous = "";
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            assertTrue(previous.compareTo(cells[0]) < 0);
            previous = cells[0];
            assertTrue(seen.add(cells[0]));
            Target target = expected.get(cells[0]);
            assertNotNull(target, cells[0]);
            assertEquals(target.beforeHash(), hash(resource(target.beforeResource())));
            assertEquals(target.beforeHash(), cells[1], "No successor target may reuse ABSENT");
            assertEquals(target.afterResource(), cells[3]);
            assertEquals(hash(resource(target.afterResource())), cells[2]);
            assertNotEquals(cells[1], cells[2], "All four declared updates must change reviewed bytes");
        }
        assertEquals(expected.keySet(), seen);
    }

    private static void requireBoundSource() {
        assertTrue(EXPECTED_RECOVERY_SOURCE_COUNT > 17, "Actual recovery source closure is not yet bound");
        assertNotEquals(RECOVERY_INPUT, FINAL_SOURCE_COMMIT);
        assertTrue(FINAL_SOURCE_COMMIT.matches("[0-9a-f]{40}"), "Actual recovery commit is not yet bound");
        assertNotEquals(PREVIOUS_SOURCE, FINAL_SOURCE_COMMIT);
        assertTrue(FINAL_SOURCE_ROOT_TREE.matches("[0-9a-f]{40}"), "Actual recovery root tree is not yet bound");
        assertTrue(FINAL_SOURCE_REF.startsWith("refs/heads/"), "Actual observed source feature ref is not yet bound");
        assertNotEquals("refs/heads/develop", FINAL_SOURCE_REF);
    }

    private static ArrayList<SourceFile> beforeSources() throws IOException {
        var result = new ArrayList<SourceFile>();
        for (Target target : OUTPUTS) {
            String before = resource(target.beforeResource());
            assertEquals(target.beforeHash(), hash(before));
            result.add(text(target.path(), before));
        }
        return result;
    }

    private static Map<String, String> expectedAfter() throws IOException {
        var result = new TreeMap<String, String>();
        for (Target target : OUTPUTS) result.put(target.path(), resource(target.afterResource()));
        return result;
    }

    private static JsonNode record(String resource, String id) throws IOException {
        JsonNode found = null;
        for (JsonNode row : json(resource).path("migration").path("records")) {
            if (!id.equals(row.path("id").asText())) continue;
            assertTrue(found == null, "Duplicate record: " + id);
            found = row;
        }
        assertNotNull(found, id);
        return found;
    }

    private static void assertContainsExact(JsonNode values, JsonNode expected, String reason) {
        assertTrue(values.isArray(), reason);
        boolean found = false;
        for (JsonNode value : values) found |= value.equals(expected);
        assertTrue(found, reason);
    }

    private static boolean sameLocation(JsonNode a, JsonNode b) {
        return a.path("repo").equals(b.path("repo")) && a.path("path").equals(b.path("path"))
                && a.path("symbol").equals(b.path("symbol"));
    }

    private static void reject(List<SourceFile> input, String reason) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = recipe().run(new InMemoryLargeSourceSet(input), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(run.getChangeset().getAllResults().isEmpty(), "Refusal must be atomic");
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), "Expected refusal: " + reason);
        assertTrue(errors.stream().anyMatch(failure -> messages(failure).contains(reason)),
                () -> "Missing refusal reason '" + reason + "': " + errors);
    }

    private static String messages(Throwable failure) {
        var value = new StringBuilder();
        for (int depth = 0; failure != null && depth < 32; depth++, failure = failure.getCause())
            value.append(failure.getMessage()).append('\n');
        return value.toString();
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static M3Jdk21HashPinnedTextSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static String hash(String value) {
        return M3Jdk21HashPinnedTextSnapshotRecipe.sha256(value);
    }

    private static JsonNode json(String name) throws IOException {
        return JSON.readTree(resource(name));
    }

    private static String resource(String name) throws IOException {
        try (var input = JccSourceRecoveryHandoffTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, "Recovery-bound recipe input has not been supplied: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
