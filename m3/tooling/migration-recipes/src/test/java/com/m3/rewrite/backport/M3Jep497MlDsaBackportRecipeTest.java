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

final class M3Jep497MlDsaBackportRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk24-jep497-mldsa/";
    private static final String VECTOR_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/jdk24-jep497-mldsa-vectors/";

    private static final Map<String, String> PREIMAGES = Map.of(
            "src/java.base/share/classes/java/security/spec/NamedParameterSpec.java",
            "pre-NamedParameterSpec.java.before.txt",
            "src/java.base/share/classes/sun/security/provider/SunEntries.java",
            "pre-SunEntries.java.before.txt",
            "src/java.base/share/classes/sun/security/util/KnownOIDs.java",
            "pre-KnownOIDs.java.before.txt");

    @Test
    void bindsReleasedFips204ChainAndExplicitLibraryApiAuthority() {
        var recipe = new M3Jep497MlDsaBackportRecipe();

        assertEquals(
                "3f53d571343792341481f4d15970cdc0bcd76a5e",
                M3Jep497MlDsaBackportRecipe.NAMED_KEY_PREREQUISITE);
        assertEquals(
                "c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c",
                M3Jep497MlDsaBackportRecipe.SHAKE_XOF_PREREQUISITE);
        assertEquals(
                "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7",
                M3Jep497MlDsaBackportRecipe.FEATURE_COMMIT);
        assertEquals(
                "8c2b4f62714f26ab3bc4808c734502af632a1eef",
                M3Jep497MlDsaBackportRecipe.ML_KEM_COEXISTENCE);
        assertEquals(
                "fb95a5394413dba7352a7ad2ebd39a3da42308a6",
                M3Jep497MlDsaBackportRecipe.FIPS_204_FINAL);
        assertEquals(
                "6705a9255d28f351950e7fbca9d05e73942a4e27",
                M3Jep497MlDsaBackportRecipe.RELEASED_STATE);

        assertEquals(2, recipe.getRecipeList().size());
        assertEquals(
                "jdk24-jep497-mldsa",
                assertInstanceOf(
                                M3Jdk21HashPinnedSnapshotRecipe.class,
                                recipe.getRecipeList().getFirst())
                        .getCrateName());
        assertEquals(
                "jdk24-jep497-mldsa-vectors",
                assertInstanceOf(
                                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                                recipe.getRecipeList().get(1))
                        .getCrateName());

        var policy = M3RecipeScopeRegistry.require(M3Jep497MlDsaBackportRecipe.class);
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
                        .activateRecipes("com.m3.jdk21.Jep497MlDsa");
        assertEquals(
                "com.m3.jdk21.Jep497MlDsa",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void exactMlKemStackPreimagesReplaySevenTargetsThenReachFixedPoint()
            throws Exception {
        var recipe = new M3Jep497MlDsaBackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(11, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            SourceFile source = change.getAfter();
            if (source != null) {
                after.put(path(source), source);
            }
        }

        for (ManifestRow row : javaManifest()) {
            assertEquals(resource(ROOT + row.resource()), after.get(row.path()).printAll());
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
    void staleSharedProviderPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = baseline();
        String target = "src/java.base/share/classes/sun/security/provider/SunEntries.java";
        int index = indexOf(sources, target);
        SourceFile current = sources.get(index);
        sources.set(index, parse(target, current.printAll() + "\n// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jep497MlDsaBackportRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void postimagesKeepTheBoundedJava21Fips204Surface() throws Exception {
        String named = resource(ROOT + "00-NamedParameterSpec.java.after.txt");
        assertTrue(named.contains("ML_DSA_44"));
        assertTrue(named.contains("ML_DSA_65"));
        assertTrue(named.contains("ML_DSA_87"));
        assertTrue(named.contains("ML_KEM_512"));

        String impl = resource(ROOT + "02-ML_DSA_Impls.java.after.txt");
        assertTrue(impl.contains("public static Version version = Version.FINAL"));
        assertTrue(impl.contains("extends NamedKeyPairGenerator"));
        assertTrue(impl.contains("extends NamedKeyFactory"));
        assertTrue(impl.contains("extends NamedSignature"));
        assertTrue(!impl.contains("AsymmetricKey"));

        String provider = resource(ROOT + "03-SunEntries.java.after.txt");
        assertTrue(provider.contains("\"Signature\", \"ML-DSA\""));
        assertTrue(provider.contains("\"KeyPairGenerator\", \"ML-DSA\""));
        assertTrue(provider.contains("\"KeyFactory\", \"ML-DSA\""));

        String oids = resource(ROOT + "04-KnownOIDs.java.after.txt");
        assertTrue(oids.contains("2.16.840.1.101.3.4.3.17"));
        assertTrue(oids.contains("2.16.840.1.101.3.4.3.18"));
        assertTrue(oids.contains("2.16.840.1.101.3.4.3.19"));
        assertTrue(oids.contains("ML_KEM_512"));

        String deterministic = resource(ROOT + "05-MLDSADeterministic.java.after.txt");
        assertTrue(deterministic.contains("changed message verified"));
        assertTrue(deterministic.contains("static final class FixedSecureRandom"));
        assertTrue(!deterministic.contains("MLDSAProviderSmoke.FixedSecureRandom"));

        String smoke = resource(ROOT + "06-MLDSAProviderSmoke.java.after.txt");
        assertTrue(smoke.contains("NamedParameterSpec.ML_DSA_44"));
        assertTrue(smoke.contains("2.16.840.1.101.3.4.3.17"));

        String kat = resource(ROOT + "07-MLDSAKnownAnswer.java.after.txt");
        assertTrue(kat.contains("Reduced pinned FIPS 204 ACVP"));
        assertTrue(kat.contains("JSONValue.parse"));
        assertEquals(8, javaManifest().size());
        assertEquals(3, textManifest().size());
    }

    private static List<SourceFile> baseline() throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(PREIMAGES).entrySet()) {
            result.add(parse(entry.getKey(), resource(ROOT + entry.getValue())));
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
        return manifest(ROOT, 8);
    }

    private static List<ManifestRow> textManifest() throws IOException {
        return manifest(VECTOR_ROOT, 3);
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
        try (var input = M3Jep497MlDsaBackportRecipeTest.class.getResourceAsStream(name)) {
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
