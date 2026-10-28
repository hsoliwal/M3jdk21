// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** All transitions and refusals below execute the actual OpenRewrite scheduler. */
class IntegratedUtilitiesRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/integrated-utilities-fixture/";
    private static final class FixtureRecipe extends M3GpuToolJoinRecipe {
        @Override protected String resourceRoot() { return ROOT; }
    }
    @TempDir Path temporary;

    private static M3GpuToolJoinManifest fixture() {
        return M3GpuToolJoinManifest.load(ROOT, IntegratedUtilitiesRecipeTest.class::getResourceAsStream, () -> { });
    }
    private static SourceFile text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }
    private static String resource(String path) throws IOException {
        try (InputStream in = IntegratedUtilitiesRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static List<SourceFile> inputs(boolean after) throws IOException {
        var list = new ArrayList<SourceFile>();
        for (var target : fixture().targets()) {
            if (after) {
                list.add(text(target.path(), target.text() == null ? resource("before/" + target.path()) : target.text()));
            } else if (!target.addition()) {
                list.add(text(target.path(), resource("before/" + target.path())));
            }
        }
        return list;
    }
    private static List<Result> run(List<SourceFile> sources) {
        return IntegratedUtilitiesRunner.qualifiedResults(new FixtureRecipe(), sources, () -> { });
    }
    private static Map<String, String> postimages(List<Result> results) {
        var map = new LinkedHashMap<String, String>();
        for (var result : results) {
            assertNotNull(result.getAfter());
            var after = result.getAfter();
            assertNull(map.put(after.getSourcePath().toString().replace('\\', '/'), after.printAll()));
        }
        return map;
    }
    private Path sourceRoot() throws IOException {
        Path root = Files.createDirectory(temporary.resolve("source"));
        for (var input : inputs(false)) {
            Path file = root.resolve(input.getSourcePath());
            Files.createDirectories(file.getParent());
            Files.writeString(file, input.printAll(), StandardCharsets.UTF_8);
        }
        return root;
    }

    @Test void actualSdkProducesExactJavaNativeAndMetadataPostimages() throws IOException {
        Map<String, String> actual = postimages(run(inputs(false)));
        assertEquals(4, actual.size());
        for (var target : fixture().targets()) if (target.text() != null) {
            assertEquals(target.text(), actual.get(target.path()), target.path());
            assertEquals(target.after(), M3GpuToolJoinManifest.sha256(actual.get(target.path())));
        }
    }
    @Test void actualSecondSdkRunIsFixedPoint() throws IOException {
        assertTrue(run(inputs(true)).isEmpty());
    }
    @Test void actualSdkAcceptsEveryMixedPrePostState() throws IOException {
        var targets = new ArrayList<>(fixture().targets());
        for (int bits = 0; bits < (1 << targets.size()); bits++) {
            var mixed = new ArrayList<SourceFile>();
            int expected = 0;
            for (int i = 0; i < targets.size(); i++) {
                var target = targets.get(i);
                boolean after = (bits & (1 << i)) != 0;
                if (after) mixed.add(text(target.path(), target.text() == null
                        ? resource("before/" + target.path()) : target.text()));
                else if (!target.addition()) mixed.add(text(target.path(), resource("before/" + target.path())));
                if (!after && target.text() != null) expected++;
            }
            assertEquals(expected, run(mixed).size(), "mixed state " + bits);
        }
    }
    @Test void actualSdkRefusesDrift() throws IOException {
        var input = inputs(false);
        var first = input.get(0);
        input.set(0, text(first.getSourcePath().toString(), first.printAll() + "drift"));
        assertThrows(IllegalStateException.class, () -> run(input));
    }
    @Test void actualSdkRefusesMissingRequiredOwner() throws IOException {
        var input = inputs(false); input.remove(0);
        assertThrows(IllegalStateException.class, () -> run(input));
    }
    @Test void actualSdkRefusesDuplicateTarget() throws IOException {
        var input = inputs(false); input.add(input.get(0));
        assertThrows(IllegalStateException.class, () -> run(input));
    }
    @Test void actualSdkRefusesOccupiedAbsentTarget() throws IOException {
        var input = inputs(false); input.add(text("src/example/Added.java", "occupied\n"));
        assertThrows(IllegalStateException.class, () -> run(input));
    }
    @Test void actualSdkCancellationCannotAdmitOutput() throws IOException {
        var input = inputs(false);
        assertThrows(IllegalStateException.class, () -> IntegratedUtilitiesRunner.qualifiedResults(
                new FixtureRecipe(), input, () -> { throw new CancellationException("fixture cancellation"); }));
    }
    @Test void unrelatedSourceIsNotAResult() throws IOException {
        var input = inputs(false); input.add(text("unrelated/unchanged.txt", "untouched\n"));
        Map<String, String> results = postimages(run(input));
        assertEquals(4, results.size()); assertFalse(results.containsKey("unrelated/unchanged.txt"));
    }
    @Test void namedYamlRecipeUsesExistingEngine() throws IOException {
        String path = "/META-INF/rewrite/m3jdk21-integrated-utilities-20261018.yml";
        try (InputStream yaml = getClass().getResourceAsStream(path)) {
            assertNotNull(yaml, path);
            var loader = new YamlResourceLoader(yaml, URI.create("classpath:" + path),
                    new Properties(), getClass().getClassLoader());
            var recipe = Environment.builder().load(loader).build()
                    .activateRecipes("com.synexia.m3.Jdk21IntegratedUtilities20261018");
            assertTrue(recipe.validateAll().stream().allMatch(validation -> validation.isValid()));
            assertEquals(M3GpuToolJoinRecipe.class, M3IntegratedUtilitiesRecipe.class.getSuperclass());
        }
    }
    @Test void actualSdkRunnerEmitsOnlyIntoFreshDisjointDirectory() throws IOException {
        Path source = sourceRoot(), output = temporary.resolve("candidate");
        Map<String, String> outputs = IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, output, false, () -> { });
        assertEquals(4, outputs.size());
        for (var entry : outputs.entrySet()) {
            assertEquals(entry.getValue(), Files.readString(output.resolve(entry.getKey())));
        }
        for (var input : inputs(false)) assertEquals(input.printAll(), Files.readString(source.resolve(input.getSourcePath())));
        assertThrows(IllegalStateException.class,
                () -> IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, output, false, () -> { }));
        assertThrows(IllegalStateException.class,
                () -> IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, source.resolve("out"), false, () -> { }));
    }
    @Test void outputSymlinkAncestorIntoSourceIsRefusedBeforeCreation() throws IOException {
        Path source = sourceRoot();
        Path alias = temporary.resolve("source-alias");
        Files.createSymbolicLink(alias, source);
        assertThrows(IllegalStateException.class,
                () -> IntegratedUtilitiesRunner.run(new FixtureRecipe(), source,
                        alias.resolve("candidate"), false, () -> { }));
        assertFalse(Files.exists(source.resolve("candidate")));
        for (var input : inputs(false)) {
            assertEquals(input.printAll(), Files.readString(source.resolve(input.getSourcePath())));
        }
    }
    @Test void runnerCancellationBeforeEmissionLeavesNoOutputDirectory() throws IOException {
        Path source = sourceRoot(), output = temporary.resolve("cancelled");
        assertThrows(CancellationException.class,
                () -> IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, output, false,
                        () -> { throw new CancellationException("preflight cancelled"); }));
        assertFalse(Files.exists(output));
    }
    @Test void runnerRefusalLeavesNoOutputDirectory() throws IOException {
        Path source = sourceRoot(), output = temporary.resolve("refused");
        Files.writeString(source.resolve("README.md"), "drift\n");
        assertThrows(IllegalStateException.class,
                () -> IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, output, false, () -> { }));
        assertFalse(Files.exists(output));
    }
    @Test void checkModeUsesActualSdkAndRequiresFixedPoint() throws IOException {
        Path source = sourceRoot();
        assertThrows(IllegalStateException.class,
                () -> IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, null, true, () -> { }));
        for (var input : inputs(true)) {
            Path file = source.resolve(input.getSourcePath()); Files.createDirectories(file.getParent());
            Files.writeString(file, input.printAll());
        }
        assertTrue(IntegratedUtilitiesRunner.run(new FixtureRecipe(), source, null, true, () -> { }).isEmpty());
    }
}
