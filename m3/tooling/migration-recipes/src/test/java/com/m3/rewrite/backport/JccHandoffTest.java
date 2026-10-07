// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** Text-recipe proof only: no source export or JDK/JNI acceptance is granted by these tests. */
final class JccHandoffTest {
    private static final String CRATE = "jcc-handoff-20261005";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private static final String SOURCE = "0b8dc32b9e8a616b7b7141bbdd88722839dc64bc";
    private static final String DESTINATION = "da958d00d24154c0db87beca0ec80a7df2b43b73";
    private static final String REGISTRY = "m3/docs/name-mapping.json";
    private static final String BASELINE_SHA256 =
            "67d40925b97a38c3c1a85f337806524ac590a540bd39647b3e4b0988f2ae3fbb";
    private static final String DESCRIPTOR = "synexia.counterpart.MIndexJvmDescriptor";
    private static final Set<String> NEW_IDS = Set.of(
            "synexia.jcc-recipe-laboratory", "synexia.jcc-java-jni-regression");
    private static final Map<String, String> OUTPUTS = Map.of(
            REGISTRY, "after00-name-mapping.json.txt",
            "m3/docs/jcc-source-handoff.md", "01-jcc-source-handoff.md.txt",
            "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
                    "02-source-destination-bindings.json.txt",
            "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv",
                    "03-root-coverage-obligations.tsv.txt");
    private static final ObjectMapper JSON = new ObjectMapper();

    private record Target(String path, String before, String after, String resource) {}

    @Test
    void namedEntryPointAndSerializedOwnerReplayTheActualPacket() throws IOException {
        RecipeSerializer serializer = new RecipeSerializer();
        M3Jdk21HashPinnedTextSnapshotRecipe restored = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                serializer.read(serializer.write(recipe())));
        assertEquals(CRATE, restored.getCrateName());
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.JccSourceHandoff");
        var expected = new TreeMap<String, String>();
        for (Target target : targets()) expected.put(target.path(), resource(target.resource()));
        for (Recipe entryPoint : List.of(restored, named)) {
            var errors = new ArrayList<Throwable>();
            var result = entryPoint.run(new InMemoryLargeSourceSet(List.of(baseline())),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            var actual = new TreeMap<String, String>();
            for (var change : result.getChangeset().getAllResults()) {
                SourceFile after = change.getAfter();
                assertNotNull(after);
                assertTrue(after instanceof PlainText);
                String path = after.getSourcePath().toString().replace('\\', '/');
                assertFalse(actual.containsKey(path));
                actual.put(path, after.printAll());
            }
            assertEquals(expected, actual, entryPoint.getName());
        }
    }

    @Test
    void exactPacketReplaysAndFreshSecondPassIsUnchanged() throws IOException {
        List<Target> targets = targets();
        SourceFile unrelated = text("unrelated.txt", "retain\n");
        SourceFile neighboring = text(REGISTRY + ".unrelated", "retain adjacent path\n");
        SourceFile baseline = baseline();
        var errors = new ArrayList<Throwable>();
        var result = recipe().run(
                new InMemoryLargeSourceSet(List.of(baseline, unrelated, neighboring)),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = result.getChangeset().getAllResults();
        assertEquals(4, changes.size());
        var expected = new TreeMap<String, String>();
        for (Target target : targets) expected.put(target.path(), resource(target.resource()));
        var actual = new TreeMap<String, String>();
        var after = new ArrayList<SourceFile>();
        for (var change : changes) {
            SourceFile output = change.getAfter();
            assertNotNull(output);
            assertTrue(output instanceof PlainText);
            String path = output.getSourcePath().toString().replace('\\', '/');
            assertTrue(OUTPUTS.containsKey(path), path);
            assertFalse(actual.containsKey(path), path);
            actual.put(path, output.printAll());
            if (REGISTRY.equals(path)) {
                assertNotNull(change.getBefore());
                assertEquals(baseline.getId(), output.getId());
                assertEquals(BASELINE_SHA256,
                        M3Jdk21HashPinnedTextSnapshotRecipe.sha256(change.getBefore().printAll()));
            } else {
                assertNull(change.getBefore(), path);
            }
            after.add(output);
        }
        assertEquals(expected, actual);
        after.add(unrelated);
        after.add(neighboring);
        var freshErrors = new ArrayList<Throwable>();
        var repeat = recipe().run(new InMemoryLargeSourceSet(after),
                new InMemoryExecutionContext(freshErrors::add), 1);
        assertTrue(freshErrors.isEmpty(), freshErrors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertEquals("retain\n", unrelated.printAll());
        assertEquals("retain adjacent path\n", neighboring.printAll());
        assertEquals(BASELINE_SHA256, M3Jdk21HashPinnedTextSnapshotRecipe.sha256(baseline.printAll()));
    }

    @Test
    void existingGlobalsGatesAndFortyThreeRecordsRemainExact() throws IOException {
        JsonNode before = JSON.readTree(resource("before00-name-mapping.json.txt"));
        JsonNode after = JSON.readTree(resource("after00-name-mapping.json.txt"));
        assertEquals(20, before.path("migration").path("gates").size());
        ObjectNode beforeGlobals = before.deepCopy();
        ObjectNode afterGlobals = after.deepCopy();
        ((ObjectNode) beforeGlobals.path("migration")).remove("records");
        ((ObjectNode) afterGlobals.path("migration")).remove("records");
        assertEquals(beforeGlobals, afterGlobals,
                "All existing globals, historical pins, coverage and 20 gates must remain exact");
        JsonNode oldRecords = before.path("migration").path("records");
        JsonNode newRecords = after.path("migration").path("records");
        assertEquals(44, oldRecords.size());
        assertEquals(46, newRecords.size());
        Set<String> ids = new HashSet<>();
        int reconciled = 0;
        for (int i = 0; i < newRecords.size(); i++) {
            JsonNode current = newRecords.get(i);
            String id = current.path("id").asText();
            assertTrue(ids.add(id), "Duplicate registry id: " + id);
            if (i >= oldRecords.size()) {
                assertTrue(NEW_IDS.contains(id), id);
                continue;
            }
            JsonNode old = oldRecords.get(i);
            assertEquals(old.path("id"), current.path("id"), "Existing order must remain exact");
            if (DESCRIPTOR.equals(id)) {
                reconciled++;
                assertDescriptorReconciliation(old, current);
            } else {
                assertEquals(old, current, id);
            }
        }
        assertEquals(1, reconciled);
        assertTrue(ids.containsAll(NEW_IDS));
    }

    @Test
    void newJccRowsRemainBlockedAndCarryNoExportOrDestinationAcceptance() throws IOException {
        JsonNode registry = JSON.readTree(resource("after00-name-mapping.json.txt"));
        int checked = 0;
        for (JsonNode record : registry.path("migration").path("records")) {
            if (!NEW_IDS.contains(record.path("id").asText())) continue;
            checked++;
            assertEquals("blocked", record.path("status").asText());
            assertTrue(record.path("tests").isArray());
            assertEquals(0, record.path("tests").size());
            assertFalse(record.path("provenance").path("copied_code").asBoolean(true));
            JsonNode sync = record.path("sync");
            assertEquals(SOURCE, sync.path("source_revision").asText());
            assertEquals(DESTINATION, sync.path("target_revision").asText());
            assertTrue(sync.path("pending").isArray());
            assertTrue(sync.path("pending").size() > 0);
            assertTrue(record.path("sources").size() > 0);
            for (JsonNode source : record.path("sources")) {
                assertEquals("hsoliwal/com.synexia", source.path("repo").asText());
                assertEquals(SOURCE, source.path("commit").asText());
                assertEquals("pinned", source.path("revision_role").asText());
                assertTrue(source.path("sha256").asText().matches("[0-9a-f]{64}"));
                assertTrue(source.path("git_blob_sha1").asText().matches("[0-9a-f]{40}"));
            }
        }
        assertEquals(2, checked);
        JsonNode acceptance = JSON.readTree(resource("02-source-destination-bindings.json.txt"))
                .path("acceptance");
        Set<String> obligations = Set.of("source_export_admitted", "destination_materialized",
                "destination_gates_passed", "remote_read_back_delivered");
        assertTrue(acceptance.isObject());
        assertEquals(obligations.size(), acceptance.size());
        for (String obligation : obligations) {
            assertTrue(acceptance.path(obligation).isBoolean(), obligation);
            assertFalse(acceptance.path(obligation).booleanValue(), obligation);
        }
    }

    @Test
    void everyChangedOrOccupiedTargetRefusesWithoutPartialResults() throws IOException {
        for (Target target : targets()) {
            var input = new ArrayList<SourceFile>();
            if (!REGISTRY.equals(target.path())) input.add(baseline());
            input.add(text(target.path(), "changed or occupied\n"));
            reject(input, "source drift: " + target.path());
        }
    }

    @Test
    void everyDuplicateTargetRefusesEvenAtTheExactPostimage() throws IOException {
        for (Target target : targets()) {
            var input = new ArrayList<SourceFile>();
            if (!REGISTRY.equals(target.path())) input.add(baseline());
            input.add(text(target.path(), resource(target.resource())));
            input.add(text(target.path(), resource(target.resource())));
            reject(input, "duplicate target: " + target.path());
        }
    }

    @Test
    void missingRequiredRegistryRefusesAllAdditions() throws IOException {
        reject(List.of(text("unrelated.txt", "retain\n")),
                "required JDK21 text source missing: " + REGISTRY);
        var additionsOnly = new ArrayList<SourceFile>();
        for (Target target : targets()) {
            if (!REGISTRY.equals(target.path()))
                additionsOnly.add(text(target.path(), resource(target.resource())));
        }
        reject(additionsOnly, "required JDK21 text source missing: " + REGISTRY);
    }

    @Test
    void targetMutationBetweenScanAndVisitRefuses() throws IOException {
        var recipe = recipe();
        var context = new InMemoryExecutionContext(failure -> {
            throw new AssertionError("Unexpected phase error", failure);
        });
        var inventory = recipe.getInitialValue(context);
        recipe.getScanner(inventory).visit(baseline(), context);
        assertEquals(3, recipe.generate(inventory, context).size());
        var visitor = recipe.getVisitor(inventory);
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> visitor.visit(text(REGISTRY, "changed after scan\n"), context));
        assertTrue(messages(failure).contains("JDK21 text target changed after scan: " + REGISTRY));
    }

    private static void assertDescriptorReconciliation(JsonNode old, JsonNode current) throws IOException {
        assertEquals("implemented-unverified", current.path("status").asText());
        JsonNode reconciliation = JSON.readTree(resource("02-source-destination-bindings.json.txt"))
                .path("descriptor_reconciliation");
        assertEquals(old, reconciliation.path("previous_record"),
                "The complete historical descriptor record and all prior evidence must remain exact");
        ObjectNode retainedOld = old.deepCopy();
        ObjectNode retainedCurrent = current.deepCopy();
        List<String> lifecycleFields = List.of(
                "status", "reason", "targets", "contract", "recipe", "sync", "lineage", "observation");
        retainedOld.remove(lifecycleFields);
        retainedCurrent.remove(lifecycleFields);
        assertEquals(retainedOld, retainedCurrent,
                "Descriptor source, owner, identity, tests and unchanged lifecycle fields must remain exact");
        ObjectNode oldContract = old.path("contract").deepCopy();
        ObjectNode currentContract = current.path("contract").deepCopy();
        oldContract.remove("differences");
        currentContract.remove("differences");
        assertEquals(oldContract, currentContract);
        JsonNode oldDifferences = old.path("contract").path("differences");
        JsonNode currentDifferences = current.path("contract").path("differences");
        assertEquals(oldDifferences.size() + 1, currentDifferences.size());
        for (int i = 0; i < oldDifferences.size(); i++)
            assertEquals(oldDifferences.get(i), currentDifferences.get(i));
        JsonNode snapshot = current.path("recipe");
        assertEquals("com.m3.rewrite.backport.Descriptor", snapshot.path("id").asText());
        assertEquals("snapshot@39702564163c017f9dc0350a305b032b06274857",
                snapshot.path("version").asText());
        assertEquals("m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
                        + "jdk21-hash-pinned/jdk22-descriptor/manifest.tsv",
                snapshot.path("path").asText());
        assertTrue(snapshot.path("sha256").asText().matches("[0-9a-f]{64}"));
        assertTrue(snapshot.path("preconditions").size() > 0);
        assertFalse(snapshot.path("rollback").asText().isBlank());
        JsonNode previousTargets = current.path("lineage").path("previous_targets");
        assertTrue(previousTargets.isArray());
        for (JsonNode target : old.path("targets")) {
            boolean retained = false;
            for (JsonNode previous : previousTargets) retained |= target.equals(previous);
            assertTrue(retained, "Historical descriptor target must remain in canonical lineage");
        }
        assertEquals(old.path("lineage").path("previous_sources"),
                current.path("lineage").path("previous_sources"));
        assertEquals(old.path("lineage").path("supersedes"),
                current.path("lineage").path("supersedes"));
        assertEquals(DESTINATION, current.path("sync").path("target_revision").asText());
        assertEquals(old.path("sync").path("source_revision"),
                current.path("sync").path("source_revision"));
        assertTrue(current.path("sync").path("pending").size() > 0);
        assertTrue(current.path("targets").size() > 0);
        for (JsonNode target : current.path("targets")) {
            assertEquals(DESTINATION, target.path("commit").asText());
            assertEquals("pinned", target.path("revision_role").asText());
            assertTrue(target.path("sha256").asText().matches("[0-9a-f]{64}"));
            assertTrue(target.path("git_blob_sha1").asText().matches("[0-9a-f]{40}"));
        }
    }

    private static List<Target> targets() throws IOException {
        assertEquals(BASELINE_SHA256,
                M3Jdk21HashPinnedTextSnapshotRecipe.sha256(resource("before00-name-mapping.json.txt")));
        var targets = new ArrayList<Target>();
        var seen = new HashSet<String>();
        String previous = "";
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            assertEquals(4, row.length);
            assertTrue(previous.compareTo(row[0]) < 0, row[0]);
            previous = row[0];
            assertTrue(seen.add(row[0]), row[0]);
            assertEquals(OUTPUTS.get(row[0]), row[3], row[0]);
            assertEquals(REGISTRY.equals(row[0]) ? BASELINE_SHA256 : "ABSENT", row[1]);
            assertEquals(row[2], M3Jdk21HashPinnedTextSnapshotRecipe.sha256(resource(row[3])));
            targets.add(new Target(row[0], row[1], row[2], row[3]));
        }
        assertEquals(OUTPUTS.keySet(), seen);
        assertEquals(4, targets.size());
        return List.copyOf(targets);
    }

    private static void reject(List<SourceFile> input, String reason) {
        var errors = new ArrayList<Throwable>();
        try {
            var result = recipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(result.getChangeset().getAllResults().isEmpty(), "Refusal must be atomic");
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), "Expected refusal: " + reason);
        assertTrue(errors.stream().anyMatch(failure -> messages(failure).contains(reason)),
                () -> "Missing refusal reason '" + reason + "': " + errors);
    }

    private static String messages(Throwable failure) {
        var messages = new StringBuilder();
        for (int depth = 0; failure != null && depth < 32; depth++, failure = failure.getCause())
            messages.append(failure.getMessage()).append('\n');
        return messages.toString();
    }

    private static M3Jdk21HashPinnedTextSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
    }

    private static PlainText baseline() throws IOException {
        return text(REGISTRY, resource("before00-name-mapping.json.txt"));
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static String resource(String name) throws IOException {
        try (var input = JccHandoffTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
