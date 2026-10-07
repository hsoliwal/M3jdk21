// SPDX-License-Identifier: Apache-2.0
package com.synexia.handoff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the existing repository-owned recipe; the portable probe is not a substitute. */
final class ApacheReuseRecipeTest {
    private static final Path CRATE = Path.of(System.getProperty("m3.reuse.crate"));
    private static final Path RES = CRATE.resolve("src/main/resources/"
        + System.getProperty("m3.reuse.resourceRoot"));
    private static final Path ROOT = Path.of(System.getProperty("m3.reuse.root")).normalize();

    private static Recipe recipe() throws ReflectiveOperationException {
        return (Recipe) Class.forName(System.getProperty("m3.reuse.engine"))
            .getConstructor(String.class).newInstance("apache-public-reuse-v1");
    }

    private static List<SourceFile> apply(List<SourceFile> files) throws ReflectiveOperationException {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        return recipe().run(new InMemoryLargeSourceSet(files), context, 1)
            .getChangeset().getAllResults().stream().map(result -> {
                assertNotNull(result.getAfter(), "no deletions");
                return result.getAfter();
            }).toList();
    }

    private static Map<String, String> expected() throws Exception {
        var packet = ApacheReuseProof.read(RES);
        ApacheReuseProof.verify(packet);
        var result = new HashMap<String, String>();
        packet.manifest().lines().filter(line -> !line.startsWith("#")).forEach(line -> {
            String[] row = line.split("\t", -1);
            result.put(row[0], packet.templates().get(row[3]));
        });
        Set<String> paths = Set.of(System.getProperty("m3.reuse.paths").split(","));
        assertEquals(paths, result.keySet(), "exact repository target set");
        return Map.copyOf(result);
    }

    private static List<SourceFile> sources(Map<String, String> values) {
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .map(e -> (SourceFile) PlainText.builder().sourcePath(Path.of(e.getKey())).text(e.getValue()).build())
            .toList();
    }

    @Test void existingEngineIdentityAndNoUnreviewedWorkingTreeOverwrite() throws Exception {
        String enginePath = System.getProperty("m3.reuse.enginePath");
        byte[] content = Files.readAllBytes(ROOT.resolve(enginePath));
        byte[] header = ("blob " + content.length + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var digest = java.security.MessageDigest.getInstance("SHA-1");
        digest.update(header);
        assertEquals(System.getProperty("m3.reuse.engineBlob"), java.util.HexFormat.of().formatHex(digest.digest(content)));
        for (var entry : expected().entrySet()) {
            Path path = ROOT.resolve(entry.getKey());
            if (Files.exists(path)) assertEquals(entry.getValue(), Files.readString(path), "existing target drift");
        }
    }

    @Test void exactGenerationAndFixedPoint() throws Exception {
        var expected = expected();
        List<SourceFile> generated = apply(List.of());
        assertEquals(expected, generated.stream().collect(Collectors.toMap(
            s -> s.getSourcePath().toString().replace('\\', '/'), SourceFile::printAll)));
        assertTrue(apply(generated).isEmpty());
        for (SourceFile file : generated) {
            Path staged = CRATE.resolve("target/generated").resolve(file.getSourcePath());
            Files.createDirectories(staged.getParent());
            Files.writeString(staged, file.printAll());
        }
    }

    @Test void partialFixedPointGeneratesOnlyMissingTargets() throws Exception {
        List<SourceFile> all = sources(expected());
        List<SourceFile> changes = apply(List.of(all.getFirst()));
        assertEquals(2, changes.size());
        var complete = new ArrayList<SourceFile>(changes);
        complete.add(all.getFirst());
        assertTrue(apply(complete).isEmpty());
    }

    @Test void driftAndDuplicatesRefuse() throws Exception {
        List<SourceFile> all = sources(expected());
        var first = all.getFirst();
        var changed = new ArrayList<SourceFile>(all);
        changed.set(0, PlainText.builder().sourcePath(first.getSourcePath()).text(first.printAll() + "drift").build());
        assertThrows(RuntimeException.class, () -> apply(changed));
        var duplicate = new ArrayList<SourceFile>(all);
        duplicate.add(first);
        assertThrows(RuntimeException.class, () -> apply(duplicate));
    }

    @Test void unrelatedOwnerHasNoResultAndPolicyMutantsRefuse() throws Exception {
        var owner = PlainText.builder().sourcePath(Path.of("unrelated.md")).text("preserved").build();
        List<SourceFile> changes = apply(List.of(owner));
        assertEquals(3, changes.size());
        assertTrue(changes.stream().noneMatch(s -> s.getSourcePath().equals(owner.getSourcePath())));
        assertEquals(50, ApacheReuseProof.selfTest(ApacheReuseProof.read(RES)));
    }
}
