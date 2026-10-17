// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/** Source transformation custody; this suite does not claim runtime or global migration acceptance. */
final class M3CanonicalRegexMappingReconciliationTest {
    private static final String PATH = "m3/docs/name-mapping.json";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    private static final String FULL = "com.m3.CanonicalRegexMappingReconciliation";
    private static final String APPEND = "com.m3.CanonicalRegexMappingAppend";
    private static final String LINEAGE = "com.m3.CanonicalTqTargetLineage";
    private static final String SOURCE = "com.m3.CanonicalSparseRecipeSourceCustody";
    private static final String ROLLBACK = "com.m3.CanonicalRegexMappingRollback";
    private static final String TQ = "synexia.counterpart.MIndexRegexTrigramQuery";
    private static final String SPARSE = TQ + ".sparseLookup";
    private static final ObjectMapper JSON = new ObjectMapper();
    private record Applied(Map<String, String> files, int changes) { }

    private static String resource(String name) throws IOException {
        try (var stream = M3CanonicalRegexMappingReconciliationTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static String original() throws IOException {
        return resource("m3-regex-family-reconciliation-20261017/before.json");
    }
    private static String appended() throws IOException {
        return resource("m3-regex-family-append-20261017/after.json");
    }
    private static String tqReconciled() throws IOException {
        return resource("m3-tq-target-lineage-20261017/after.json");
    }
    private static String reconciled() throws IOException {
        return resource("m3-regex-family-reconciliation-20261017/after.json");
    }
    private static Map<String, String> inputs(String map) throws IOException {
        var result = new LinkedHashMap<String, String>();
        result.put(PATH, map);
        result.put("m3/docs/name-mapping.schema.json", resource("custody/name-mapping.schema.json"));
        result.put("m3/migration/migration.py", resource("custody/migration.py"));
        result.put("opaque/Unrelated.txt", "retain arbitrary unrelated source exactly\n");
        return result;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Recipe recipe(String name) {
        Recipe selected = Environment.builder()
                .scanRuntimeClasspath("com.m3.rewrite.backport")
                .build().activateRecipes(name);
        assertEquals(1, selected.getRecipeList().size());
        assertEquals(name, selected.getRecipeList().get(0).getName());
        assertEquals(List.of(M3Jdk21HashPinnedTextSnapshotRecipe.class),
                selected.getRecipeList().get(0).getRecipeList().stream()
                        .map(Object::getClass).toList());
        return selected;
    }
    private static Applied apply(String name, Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = recipe(name).run(new InMemoryLargeSourceSet(sources(input)),
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
    private static void assertOnlyMapChanges(
            Map<String, String> input, Applied result, String expected, int count) {
        var desired = new LinkedHashMap<>(input);
        desired.put(PATH, expected);
        assertEquals(desired, result.files());
        assertEquals(count, result.changes());
    }
    private static ObjectNode withoutRecords(JsonNode input) {
        ObjectNode copy = input.deepCopy();
        ((ObjectNode) copy.path("migration")).remove("records");
        return copy;
    }
    private static Map<String, JsonNode> byId(JsonNode document) {
        var result = new LinkedHashMap<String, JsonNode>();
        for (JsonNode record : document.path("migration").path("records")) {
            assertTrue(result.put(record.path("id").asText(), record) == null,
                    "stable record IDs must remain unique");
        }
        return result;
    }

    @Test void completeChangePreservesContractsAndReachesFixedPoint() throws Exception {
        String before = original(), after = reconciled();
        for (boolean reverse : new boolean[]{false, true}) {
            var input = inputs(before);
            if (reverse) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                java.util.Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                input = ordered;
            }
            var result = apply(FULL, input);
            assertOnlyMapChanges(input, result, after, 1);
            assertEquals(0, apply(FULL, result.files()).changes());
            if (!reverse) retain("full", after);
        }
        JsonNode a = JSON.readTree(before), b = JSON.readTree(appended()), c = JSON.readTree(after);
        var oldRecords = byId(a);
        var appendedRecords = byId(b);
        var finalRecords = byId(c);
        assertEquals(56, oldRecords.size());
        assertEquals(60, appendedRecords.size());
        assertEquals(60, finalRecords.size());
        assertEquals(withoutRecords(a), withoutRecords(b));
        assertEquals(withoutRecords(a), withoutRecords(c));
        int index = 0;
        for (var entry : oldRecords.entrySet()) {
            assertEquals(entry.getKey(), b.path("migration").path("records").get(index).path("id").asText());
            assertEquals(entry.getKey(), c.path("migration").path("records").get(index).path("id").asText());
            assertEquals(entry.getValue(), appendedRecords.get(entry.getKey()));
            if (!TQ.equals(entry.getKey()) && !SPARSE.equals(entry.getKey())) {
                assertEquals(entry.getValue(), finalRecords.get(entry.getKey()));
            }
            index++;
        }
        String[] additions = {
                "synexia.regex.ordered-relations.adapter",
                "synexia.regex.ordered-relations.dispatch",
                "synexia.regex.metadata-pressure",
                "synexia.regex.hierarchical-route-acceptance"
        };
        for (int pos = 0; pos < additions.length; pos++) {
            JsonNode item = c.path("migration").path("records").get(56 + pos);
            assertEquals(additions[pos], item.path("id").asText());
            assertEquals(pos == 0 ? "implemented-unverified" : "pending",
                    item.path("status").asText());
            assertTrue(item.path("recipe").path("path").isNull());
            assertEquals(appendedRecords.get(additions[pos]), finalRecords.get(additions[pos]));
        }
        JsonNode old = oldRecords.get(TQ), current = finalRecords.get(TQ);
        ObjectNode oldProtected = old.deepCopy(), newProtected = current.deepCopy();
        for (String field : List.of("targets", "lineage", "sync", "observation")) {
            oldProtected.remove(field);
            newProtected.remove(field);
        }
        assertEquals(oldProtected, newProtected);
        assertEquals("implemented-unverified", current.path("status").asText());
        assertEquals("78aa154b8aa25134edd9a16c746e4fdc7bcd3984b8f76cc96ef81437247e08d4",
                current.path("targets").get(0).path("sha256").asText());
        var oldLineage = old.path("lineage").path("previous_targets");
        var newLineage = current.path("lineage").path("previous_targets");
        assertEquals(oldLineage.size() + 1, newLineage.size());
        for (int pos = 0; pos < oldLineage.size(); pos++) {
            assertEquals(oldLineage.get(pos), newLineage.get(pos));
        }
        assertEquals(old.path("targets").get(0), newLineage.get(newLineage.size() - 1));
        assertEquals(old.path("lineage").path("previous_sources"),
                current.path("lineage").path("previous_sources"));
        assertEquals(old.path("lineage").path("supersedes"),
                current.path("lineage").path("supersedes"));
        assertEquals(old.path("sync").path("source_revision"), current.path("sync").path("source_revision"));
        assertEquals(old.path("sync").path("pending"), current.path("sync").path("pending"));
        assertEquals(old.path("sync").path("conflicts"), current.path("sync").path("conflicts"));
        JsonNode oldSparse = oldRecords.get(SPARSE), newSparse = finalRecords.get(SPARSE);
        ObjectNode sparseBefore = oldSparse.deepCopy(), sparseAfter = newSparse.deepCopy();
        for (String field : List.of("sources", "recipe", "sync", "observation")) {
            sparseBefore.remove(field);
            sparseAfter.remove(field);
        }
        assertEquals(sparseBefore, sparseAfter);
        assertEquals("implemented-unverified", newSparse.path("status").asText());
        assertTrue(newSparse.path("recipe").path("path").isNull());
        assertTrue(newSparse.path("recipe").path("sha256").isNull());
        for (String field : List.of("id", "version", "rollback")) {
            assertEquals(oldSparse.path("recipe").path(field), newSparse.path("recipe").path(field));
        }
        var oldSources = oldSparse.path("sources");
        var newSources = newSparse.path("sources");
        assertEquals(oldSources.size() + 1, newSources.size());
        for (int pos = 0; pos < oldSources.size(); pos++) {
            assertEquals(oldSources.get(pos), newSources.get(pos));
        }
        var manifest = newSources.get(newSources.size() - 1);
        assertEquals("hsoliwal/com.synexia", manifest.path("repo").asText());
        assertEquals("f1e9ff1f2426b20d55f82e50033c0aa8ed66bf72", manifest.path("commit").asText());
        assertEquals("2fdaa30d27463bb9e9bff5064cbd4015571d3aa9",
                manifest.path("git_blob_sha1").asText());
        assertEquals("725a5dbccbfa4ff834586c21f9c495fffec985dd9d9033f86fc6e808ea421fa2",
                manifest.path("sha256").asText());
        assertEquals(oldSparse.path("recipe").path("path"), manifest.path("path"));
        String historicalManifest = resource("custody/historical-sparse-manifest.tsv");
        String historicalDigest = M3Jdk21HashPinnedTextSnapshotRecipe.sha256(historicalManifest);
        assertEquals("725a5dbccbfa4ff834586c21f9c495fffec985dd9d9033f86fc6e808ea421fa2",
                historicalDigest);
        assertEquals(historicalDigest, manifest.path("sha256").asText());
        var oldConditions = oldSparse.path("recipe").path("preconditions");
        var newConditions = newSparse.path("recipe").path("preconditions");
        assertEquals(oldConditions.size() + 1, newConditions.size());
        for (int pos = 0; pos < oldConditions.size(); pos++) {
            assertEquals(oldConditions.get(pos), newConditions.get(pos));
        }
        assertEquals(oldSparse.path("sync").path("source_revision"),
                newSparse.path("sync").path("source_revision"));
        String insertion = resource("custody/append-insertion.txt");
        assertEquals(1, count(appended(), insertion));
        assertEquals(before, appended().replace(insertion, ""));
    }

    @Test void serialStagesAndSealedRollbackRestoreOwnedStates() throws Exception {
        String a = original(), b = appended(), c = tqReconciled(), d = reconciled();
        var first = inputs(a);
        var append = apply(APPEND, first);
        assertOnlyMapChanges(first, append, b, 1);
        assertEquals(0, apply(APPEND, append.files()).changes());
        retain("append", b);
        var lineage = apply(LINEAGE, append.files());
        assertOnlyMapChanges(append.files(), lineage, c, 1);
        assertEquals(0, apply(LINEAGE, lineage.files()).changes());
        retain("lineage", c);
        var source = apply(SOURCE, lineage.files());
        assertOnlyMapChanges(lineage.files(), source, d, 1);
        assertEquals(0, apply(SOURCE, source.files()).changes());
        assertEquals(0, apply(FULL, source.files()).changes());
        retain("recipe-custody", d);
        // Intermediate B/C states use their explicit next-stage recipe.
        reject(FULL, sources(inputs(b)), "source drift: " + PATH);
        reject(FULL, sources(inputs(c)), "source drift: " + PATH);
        var rollback = apply(ROLLBACK, source.files());
        assertOnlyMapChanges(source.files(), rollback, a, 1);
        assertEquals(0, apply(ROLLBACK, rollback.files()).changes());
        assertEquals(d, apply(FULL, rollback.files()).files().get(PATH));
        retain("rollback", a);
    }

    @Test void everyNamedOperationRejectsForeignMissingAndDuplicateOwners() throws Exception {
        String[] names = {FULL, APPEND, LINEAGE, SOURCE, ROLLBACK};
        String[] before = {original(), original(), appended(), tqReconciled(), reconciled()};
        int refusals = 0;
        for (int pos = 0; pos < names.length; pos++) {
            var drift = inputs(before[pos]);
            drift.put(PATH, before[pos] + " ");
            reject(names[pos], sources(drift), "source drift: " + PATH);
            refusals++;
            var missing = inputs(before[pos]);
            missing.remove(PATH);
            reject(names[pos], sources(missing), "required JDK21 text source missing: " + PATH);
            refusals++;
            var duplicate = sources(inputs(before[pos]));
            duplicate.add(PlainText.builder().sourcePath(Path.of(PATH)).text(before[pos]).build());
            reject(names[pos], duplicate, "duplicate target: " + PATH);
            refusals++;
        }
        assertEquals(15, refusals);
    }

    private static int count(String haystack, String needle) {
        int result = 0, start = 0;
        while ((start = haystack.indexOf(needle, start)) >= 0) {
            result++;
            start += needle.length();
        }
        return result;
    }
    private static void reject(String name, List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = recipe(name).run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        var observed = new StringBuilder();
        for (Throwable failure : errors) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
    }
    private static void retain(String stage, String value) throws IOException {
        Path output = Path.of(System.getProperty("m3.fixture.root")).toAbsolutePath()
                .resolve("target/emitted/canonical-map").resolve(stage).resolve(PATH);
        Files.createDirectories(output.getParent());
        Files.writeString(output, value, StandardCharsets.UTF_8);
    }
}
