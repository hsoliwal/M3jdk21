// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Exercises the existing engine without claiming a JDK image was built. */
final class DescriptorRecipeTest {
    private static final String CRATE = "jdk22-descriptor";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/";
    private static final String OWNER =
            "src/java.base/share/classes/jdk/internal/mindex/M3Descriptor.java";
    private static final String TEST =
            "test/jdk/jdk/internal/mindex/descriptor/com/m3/descriptor/DescriptorTest.java";

    @Test
    void exactReplayAndSecondPassFixedPoint() throws IOException {
        Map<String, SourceFile> after = run(recipe(), baseline(), 2);
        assertEquals(read("00-Descriptor.java.txt"), after.get(OWNER).printAll());
        assertEquals(read("01-DescriptorTest.java.txt"), after.get(TEST).printAll());
        assertInstanceOf(J.CompilationUnit.class, after.get(OWNER));
        assertInstanceOf(J.CompilationUnit.class, after.get(TEST));
        run(recipe(), new ArrayList<>(after.values()), 0);
    }

    @Test
    void missingRequiredOwnerIsRefused() {
        refused("required JDK21 source missing", () -> run(recipe(), List.of(), 0));
    }

    @Test
    void driftedOwnerIsRefused() throws IOException {
        SourceFile drift = parse(OWNER, read("before-00-Descriptor.java.txt") + "\n// drift\n");
        refused("source drift", () -> run(recipe(), List.of(drift), 0));
    }

    @Test
    void duplicateOwnerIsRefused() throws IOException {
        List<SourceFile> files = new ArrayList<>(baseline());
        files.add(files.getFirst());
        refused("duplicate target", () -> run(recipe(), files, 0));
    }

    @Test
    void nonJavaAndOccupiedTestAreRefused() throws IOException {
        SourceFile wrongTree = PlainText.builder().sourcePath(Path.of(OWNER))
                .text(read("before-00-Descriptor.java.txt")).build();
        refused("target is not a Java compilation unit", () -> run(recipe(), List.of(wrongTree), 0));
        List<SourceFile> occupied = new ArrayList<>(baseline());
        occupied.add(parse(TEST, "package com.m3.descriptor; class DescriptorTest {}\n"));
        refused("source drift", () -> run(recipe(), occupied, 0));
    }

    @Test
    void partialReplayOnlyAddsTheMissingTest() throws IOException {
        SourceFile owner = parse(OWNER, read("00-Descriptor.java.txt"));
        Map<String, SourceFile> after = run(recipe(), List.of(owner), 1);
        assertEquals(owner.printAll(), after.get(OWNER).printAll());
        assertEquals(read("01-DescriptorTest.java.txt"), after.get(TEST).printAll());
        run(recipe(), new ArrayList<>(after.values()), 0);
    }

    @Test
    void unrelatedSourceIsRetainedExactly() throws IOException {
        List<SourceFile> files = new ArrayList<>(baseline());
        SourceFile unrelated = PlainText.builder().sourcePath(Path.of("notes.txt"))
                .text("String data is not structural authority.\n").build();
        files.add(unrelated);
        Map<String, SourceFile> after = run(recipe(), files, 2);
        assertEquals(unrelated.printAll(), after.get("notes.txt").printAll());
        assertEquals(unrelated.getId(), after.get("notes.txt").getId());
    }

    @Test
    void namedRecipeAndSerializedConfigurationRetainTheCrate() throws IOException {
        RecipeSerializer codec = new RecipeSerializer();
        M3Jdk21HashPinnedSnapshotRecipe restored = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class, codec.read(codec.write(recipe())));
        assertEquals(CRATE, restored.getCrateName());
        run(restored, baseline(), 2);
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.Descriptor");
        run(named, baseline(), 2);
    }

    @Test
    void actualCheckoutOwnerProducesTheExactRuntimeCandidate() throws IOException {
        Path repository = Path.of("..", "..", "..").toAbsolutePath().normalize();
        List<SourceFile> actual = new ArrayList<>();
        String owner = Files.readString(repository.resolve(OWNER), StandardCharsets.UTF_8);
        actual.add(parse(OWNER, owner));
        boolean testExists = Files.exists(repository.resolve(TEST));
        if (testExists) {
            actual.add(parse(TEST, Files.readString(repository.resolve(TEST), StandardCharsets.UTF_8)));
        }
        int expected = (owner.equals(read("00-Descriptor.java.txt")) ? 0 : 1)
                + (testExists ? 0 : 1);
        Map<String, SourceFile> emitted = run(recipe(), actual, expected);
        assertEquals(read("00-Descriptor.java.txt"), emitted.get(OWNER).printAll());
        assertEquals(read("01-DescriptorTest.java.txt"), emitted.get(TEST).printAll());
        Path output = Path.of("target", "descriptor-candidate").toAbsolutePath().normalize();
        for (String path : List.of(OWNER, TEST)) {
            Path destination = output.resolve(path).normalize();
            if (!destination.startsWith(output)) throw new IOException("candidate path escape");
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, emitted.get(path).printAll(), StandardCharsets.UTF_8);
        }
        run(recipe(), new ArrayList<>(emitted.values()), 0);
    }

    private static M3Jdk21HashPinnedSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
    }

    private static List<SourceFile> baseline() throws IOException {
        return List.of(parse(OWNER, read("before-00-Descriptor.java.txt")));
    }

    private static SourceFile parse(String path, String text) {
        List<SourceFile> files = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null, context()).toList();
        assertEquals(1, files.size());
        SourceFile file = files.getFirst();
        assertInstanceOf(J.CompilationUnit.class, file);
        assertEquals(text, file.printAll());
        return file;
    }

    private static Map<String, SourceFile> run(Recipe recipe, List<SourceFile> inputs, int changes) {
        var results = recipe.run(new InMemoryLargeSourceSet(inputs), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(changes, results.size());
        Map<String, SourceFile> after = new TreeMap<>();
        inputs.forEach(file -> after.put(path(file), file));
        results.forEach(result -> {
            SourceFile file = result.getAfter();
            assertNotNull(file);
            after.put(path(file), file);
        });
        return after;
    }

    private static void refused(String reason, Runnable operation) {
        RuntimeException error = assertThrows(RuntimeException.class, operation::run);
        Throwable current = error;
        for (int i = 0; i < 32 && current != null; i++, current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(reason)) return;
        }
        fail("expected refusal reason " + reason + "; got " + error);
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }

    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static String read(String name) throws IOException {
        try (var input = DescriptorRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IOException("missing Descriptor fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
