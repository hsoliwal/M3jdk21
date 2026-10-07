// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jep496MlKemBackportRecipeTest {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk24-jep496-mlkem/";
    private static final String VECTOR_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk24-jep496-mlkem-vectors/";

    private static final Map<String, String> PREIMAGES = Map.of(
            "src/java.base/share/classes/com/sun/crypto/provider/SunJCE.java",
            "pre-SunJCE.java.before.txt",
            "src/java.base/share/classes/java/security/spec/NamedParameterSpec.java",
            "pre-NamedParameterSpec.java.before.txt",
            "src/java.base/share/classes/sun/security/util/KnownOIDs.java",
            "pre-KnownOIDs.java.before.txt");

    @Test
    void bindsExactUpstreamChainAndExplicitLibraryApiAuthority() {
        var recipe = new M3Jep496MlKemBackportRecipe();

        assertEquals(
                "3f53d571343792341481f4d15970cdc0bcd76a5e",
                M3Jep496MlKemBackportRecipe.NAMED_KEY_PREREQUISITE);
        assertEquals(
                "c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c",
                M3Jep496MlKemBackportRecipe.SHAKE_XOF_PREREQUISITE);
        assertEquals(
                "13987b4244614d594dc8f94c288eddb6239a066f",
                M3Jep496MlKemBackportRecipe.FEATURE_COMMIT);
        assertEquals(
                "f400896822c2704d8e7c66afc1efa8a4fa91acb6",
                M3Jep496MlKemBackportRecipe.ACVP_MECHANICS_REFERENCE);

        assertEquals(2, recipe.getRecipeList().size());
        assertEquals(
                "jdk24-jep496-mlkem",
                assertInstanceOf(
                                M3Jdk21HashPinnedSnapshotRecipe.class,
                                recipe.getRecipeList().get(0))
                        .getCrateName());
        assertEquals(
                "jdk24-jep496-mlkem-vectors",
                assertInstanceOf(
                                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                                recipe.getRecipeList().get(1))
                        .getCrateName());

        var policy = M3RecipeScopeRegistry.require(M3Jep496MlKemBackportRecipe.class);
        assertEquals(M3EditScope.LIBRARY_API, policy.minimumScope());
        assertEquals(M3ContractMode.EXPLICIT_CONTRACT_CHANGE, policy.contractMode());
        assertTrue(recipe.getTags().contains("security"));
        assertTrue(recipe.getTags().contains("candidate-only"));
    }

    @Test
    void namedRecipeIsDiscoverable() {
        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.Jep496MlKem");
        assertEquals(
                "com.m3.jdk21.Jep496MlKem",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void exactShakeBranchPreimagesReplayNineTargetsThenReachFixedPoint()
            throws Exception {
        var recipe = new M3Jep496MlKemBackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(10, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            SourceFile source = change.getAfter();
            if (source != null) {
                after.put(path(source), source);
            }
        }

        for (ManifestRow row : javaManifest()) {
            assertEquals(resource(JAVA_ROOT + row.resource()), after.get(row.path()).printAll());
        }
        for (ManifestRow row : textManifest()) {
            assertEquals(resource(VECTOR_ROOT + row.resource()), after.get(row.path()).printAll());
        }

        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(new ArrayList<>(after.values())),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void staleProviderPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = baseline();
        String target = "src/java.base/share/classes/com/sun/crypto/provider/SunJCE.java";
        int index = indexOf(sources, target);
        SourceFile current = sources.get(index);
        sources.set(index, parse(target, current.printAll() + "\n// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jep496MlKemBackportRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void postimagesKeepTheJava21FocusedDependencyBoundary() throws Exception {
        String named = resource(JAVA_ROOT + "03-NamedParameterSpec.java.after.txt");
        assertTrue(named.contains("ML_KEM_512"));
        assertTrue(named.contains("ML_KEM_768"));
        assertTrue(named.contains("ML_KEM_1024"));

        String provider = resource(JAVA_ROOT + "02-SunJCE.java.after.txt");
        assertTrue(provider.contains("\"KEM\", \"ML-KEM\""));
        assertTrue(provider.contains("\"KeyPairGenerator\", \"ML-KEM\""));
        assertTrue(provider.contains("\"KeyFactory\", \"ML-KEM\""));

        String smoke = resource(JAVA_ROOT + "07-MLKEMProviderSmoke.java.after.txt");
        assertTrue(smoke.contains("NamedParameterSpec.ML_KEM_512"));
        assertTrue(smoke.contains("2.16.840.1.101.3.4.4.1"));

        String test = resource(JAVA_ROOT + "06-MLKEMKnownAnswer.java.after.txt");
        assertTrue(test.contains("class MLKEMKnownAnswer"));
        assertTrue(test.contains("private static final class FixedSecureRandom"));
        assertTrue(!test.contains("jdk.test.lib.security.FixedSecureRandom"));
        assertTrue(!test.contains("SeededSecureRandom"));

        assertEquals(2, textManifest().size());
        assertTrue(textManifest().stream()
                .allMatch(row -> row.path().contains("FIPS203/internalProjection.json")));
    }

    private static List<SourceFile> baseline() throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(PREIMAGES).entrySet()) {
            result.add(parse(entry.getKey(), resource(JAVA_ROOT + entry.getValue())));
        }
        return result;
    }

    private static SourceFile parse(String path, String source) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), source)),
                                null,
                                context())
                        .toList();
        if (parsed.size() != 1) {
            throw new IllegalStateException("unexpected Java parse count: " + path);
        }
        return parsed.getFirst();
    }

    private static int indexOf(List<SourceFile> sources, String target) {
        for (int index = 0; index < sources.size(); index++) {
            if (path(sources.get(index)).equals(target)) return index;
        }
        throw new IllegalArgumentException(target);
    }

    private static List<ManifestRow> javaManifest() throws IOException {
        return manifest(JAVA_ROOT, 8);
    }

    private static List<ManifestRow> textManifest() throws IOException {
        return manifest(VECTOR_ROOT, 2);
    }

    private static List<ManifestRow> manifest(String root, int expected) throws IOException {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            rows.add(new ManifestRow(cells[0], cells[1], cells[2], cells[3]));
        }
        assertEquals(expected, rows.size());
        return List.copyOf(rows);
    }

    private record ManifestRow(String path, String before, String after, String resource) {}

    private static String resource(String name) throws IOException {
        try (var input = M3Jep496MlKemBackportRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}
