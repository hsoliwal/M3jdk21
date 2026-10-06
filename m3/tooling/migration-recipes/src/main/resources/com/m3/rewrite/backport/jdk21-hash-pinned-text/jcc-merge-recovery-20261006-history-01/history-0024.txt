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

/** Actual source publication binding proof; capability export and JDK/JNI acceptance remain blocked. */
final class JccSourceFinalHandoffTest {
    private static final String CRATE = "jcc-source-final-handoff-20261005";
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private static final String E2_SOURCE = "0b8dc32b9e8a616b7b7141bbdd88722839dc64bc";
    private static final String E2_COMMIT = "df06cdee5a8526f573b3ad89622824d0b49e2f25";
    private static final String E2_ROOT_TREE = "e3e079ba36553bae517c3e53a5a834b24452a82e";
    // Frozen from the source implementation publication receipt, separately from execution inputs.
    private static final String FINAL_SOURCE_COMMIT = "d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906";
    private static final String FINAL_SOURCE_ROOT_TREE = "6f9b6a48f8cefa7a643f222c7aa1f621497725e7";
    private static final String FINAL_SOURCE_REF = "refs/heads/aix/jcc-canonical-integration-20261005";
    private static final String REGISTRY = "m3/docs/name-mapping.json";
    private static final String LAB = "synexia.jcc-recipe-laboratory";
    private static final String JNI = "synexia.jcc-java-jni-regression";
    private static final String RECEIVER = "com.m3.rewrite.atom.JccReceivingFixtureTest";
    private static final Set<String> JCC_IDS = Set.of(LAB, JNI);
    private static final ObjectMapper JSON = new ObjectMapper();

    private record Target(String path, String beforeResource, String afterResource, String beforeHash) {}

    private static final List<Target> OUTPUTS = List.of(
            new Target(REGISTRY, "before00-name-mapping.json.txt", "after00-name-mapping.json.txt",
                    "536af76dd7b7b7217707662c2f1437af4adaa53af898627b0884e0b4f4603ace"),
            new Target("m3/docs/jcc-source-handoff.md", "before01-jcc-source-handoff.md.txt",
                    "after01-jcc-source-handoff.md.txt",
                    "fff2e02f72c07f89c6f8bcabab3839878ff8628f0b31b04bb3632eb62be10337"),
            new Target("m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
                    "before02-source-destination-bindings.json.txt", "after02-source-destination-bindings.json.txt",
                    "cdb5d7f4a0746fe561f51fa9a13cbd9fa319f8196bfd3075ca2288be7f3d1126"),
            new Target("m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv",
                    "before03-root-coverage-obligations.tsv.txt", "after03-root-coverage-obligations.tsv.txt",
                    "cba3b3315df643f328c4acb2fd08b5194262dc7ad7b40557107a8c614355f55b"));

    @Test
    void namedEntryPointAndSerializedRetainedOwnerReplayAllFourExistingInputs() throws IOException {
        verifyManifest();
        RecipeSerializer serializer = new RecipeSerializer();
        M3Jdk21HashPinnedTextSnapshotRecipe restored = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                serializer.read(serializer.write(recipe())));
        assertEquals(CRATE, restored.getCrateName());
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.JccSourceFinalHandoff");
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
    void exactPreviousSourceEpochObjectsAndExistingHistoryAreRetained() throws IOException {
        requireBoundSource();
        int retainedSources = 0;
        boolean sharedTranspiler = false;
        for (String id : JCC_IDS) {
            JsonNode old = record("before00-name-mapping.json.txt", id);
            JsonNode current = record("after00-name-mapping.json.txt", id);
            for (String history : List.of("previous_sources", "previous_targets", "supersedes"))
                for (JsonNode previous : old.path("lineage").path(history))
                    assertContainsExact(current.path("lineage").path(history), previous, id + ": retained " + history);
            for (JsonNode previous : old.path("sources")) {
                assertEquals(E2_SOURCE, previous.path("commit").asText());
                assertContainsExact(current.path("lineage").path("previous_sources"), previous,
                        "Location-only validator checks do not preserve complete old source epochs");
                boolean active = false;
                for (JsonNode source : current.path("sources")) active |= sameLocation(previous, source);
                assertTrue(active, previous.path("symbol").asText());
                retainedSources++;
            }
            for (JsonNode source : current.path("sources")) {
                assertEquals("hsoliwal/com.synexia", source.path("repo").asText());
                assertEquals(FINAL_SOURCE_COMMIT, source.path("commit").asText());
                assertEquals("pinned", source.path("revision_role").asText());
                assertEquals(FINAL_SOURCE_REF, source.path("tracking_ref").asText());
                assertTrue(source.path("sha256").asText().matches("[0-9a-f]{64}"));
                assertTrue(source.path("git_blob_sha1").asText().matches("[0-9a-f]{40}"));
                sharedTranspiler |= "com.synexia.rewrite.M3OpenRewriteTranspiler".equals(source.path("symbol").asText());
            }
        }
        assertEquals(4, retainedSources);
        assertTrue(sharedTranspiler, "The reviewed shared transpiler must be accounted explicitly");
    }

    @Test
    void receiverRepinPreservesItsExactCandidateAndDoesNotAlterOtherTargets() throws IOException {
        JsonNode old = record("before00-name-mapping.json.txt", LAB);
        JsonNode current = record("after00-name-mapping.json.txt", LAB);
        assertEquals(6, old.path("targets").size());
        assertEquals(6, current.path("targets").size());
        int repinned = 0;
        for (int i = 0; i < old.path("targets").size(); i++) {
            JsonNode previous = old.path("targets").get(i);
            JsonNode active = current.path("targets").get(i);
            if (!RECEIVER.equals(previous.path("symbol").asText())) {
                assertEquals(previous, active);
                continue;
            }
            assertTrue(previous.path("commit").isNull());
            assertEquals("candidate", previous.path("revision_role").asText());
            ObjectNode expected = previous.deepCopy();
            expected.put("commit", E2_COMMIT);
            expected.put("revision_role", "pinned");
            assertEquals(expected, active, "Publication changes identity metadata only");
            assertContainsExact(current.path("lineage").path("previous_targets"), previous,
                    "The exact null-commit candidate must survive, including its original role and hashes");
            assertEquals(active, json("after02-source-destination-bindings.json.txt")
                    .path("independent_receiving_fixture").path("candidate_artifact"));
            repinned++;
        }
        assertEquals(1, repinned);
        assertEquals(record("before00-name-mapping.json.txt", JNI).path("targets"),
                record("after00-name-mapping.json.txt", JNI).path("targets"));
    }

    @Test
    void historicalDescriptorAndExecutedReceiverEvidenceRemainExact() throws IOException {
        JsonNode before = json("before02-source-destination-bindings.json.txt");
        JsonNode after = json("after02-source-destination-bindings.json.txt");
        assertEquals(before.path("descriptor_reconciliation"), after.path("descriptor_reconciliation"),
                "Retain the complete historical Descriptor record, target history and scoped receipts");
        ObjectNode oldFixture = before.path("independent_receiving_fixture").deepCopy();
        ObjectNode newFixture = after.path("independent_receiving_fixture").deepCopy();
        // The artifact's exact two-field publication transition has its own assertion above.
        // Only its classification/qualification prose may change to describe that read-back.
        List<String> publicationFields = List.of("candidate_artifact", "classification", "qualification");
        oldFixture.remove(publicationFields);
        newFixture.remove(publicationFields);
        assertEquals(oldFixture, newFixture,
                "Retain exact POM, owner, corpus, input, execution, log, XML, result and frontier evidence");
        for (String prose : List.of("classification", "qualification"))
            assertFalse(after.path("independent_receiving_fixture").path(prose).asText().isBlank());
    }

    @Test
    void finalSourceIdentityNeverPromotesBlockedCapabilityOrTransfersProof() throws IOException {
        requireBoundSource();
        for (String id : JCC_IDS) {
            JsonNode row = record("after00-name-mapping.json.txt", id);
            assertEquals("blocked", row.path("status").asText());
            assertTrue(row.path("tests").isArray());
            assertEquals(0, row.path("tests").size());
            assertEquals(FINAL_SOURCE_COMMIT, row.path("sync").path("source_revision").asText());
            assertEquals(E2_COMMIT, row.path("sync").path("target_revision").asText());
            assertTrue(row.path("sync").path("pending").size() > 0);
            JsonNode old = record("before00-name-mapping.json.txt", id);
            assertEquals(old.path("recipe").path("path"), row.path("recipe").path("path"));
            assertEquals(old.path("recipe").path("sha256"), row.path("recipe").path("sha256"));
        }
        JsonNode binding = json("after02-source-destination-bindings.json.txt");
        assertEquals(FINAL_SOURCE_COMMIT, binding.path("source_commit").asText());
        assertEquals(FINAL_SOURCE_ROOT_TREE, binding.path("source_root_tree").asText());
        assertEquals(E2_COMMIT, binding.path("destination_commit").asText());
        assertEquals(E2_ROOT_TREE, binding.path("destination_root_tree").asText());
        JsonNode plan = json("plan.json");
        assertEquals("jcc-source-final-handoff-20261005/1", plan.path("recipe_id").asText());
        assertEquals(FINAL_SOURCE_COMMIT, plan.path("source_commit").asText());
        assertEquals(E2_COMMIT, plan.path("target_commit").asText());
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
        assertTrue(FINAL_SOURCE_COMMIT.matches("[0-9a-f]{40}"), "Actual source-final commit is not yet bound");
        assertNotEquals(E2_SOURCE, FINAL_SOURCE_COMMIT);
        assertTrue(FINAL_SOURCE_ROOT_TREE.matches("[0-9a-f]{40}"), "Actual source-final root tree is not yet bound");
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
        try (var input = JccSourceFinalHandoffTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, "Final-bound recipe input has not been supplied: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
