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

final class M3Jep510KdfBackportRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk25-jep510-kdf/";

    private static final Map<String, String> PREIMAGES = Map.of(
            "src/java.base/share/classes/com/sun/crypto/provider/SunJCE.java",
            "pre-SunJCE.java.before.txt",
            "src/java.base/share/classes/java/security/Provider.java",
            "pre-Provider.java.before.txt",
            "src/java.base/share/classes/sun/security/util/Debug.java",
            "pre-Debug.java.before.txt");

    @Test
    void compositePinsCorrectedJep510LineageAndExplicitLibraryApiAuthority() {
        var recipe = new M3Jep510KdfBackportRecipe();

        assertEquals(
                "2a1ae0ff89a8ac364206b09059d9dc884adcc5ac",
                M3Jep510KdfBackportRecipe.PREVIEW_IMPLEMENTATION);
        assertEquals(
                "2c7bea1cb2acd768e57f460440228fee914255a6",
                M3Jep510KdfBackportRecipe.DELAYED_PROVIDER_FIX);
        assertEquals(
                "db7fa6a2c65d11e5bd790073d345f37b5ec356b6",
                M3Jep510KdfBackportRecipe.NON_EXTRACTABLE_PRK_FIX);
        assertEquals(
                "079fccfa9a03b890e698c52c689dea0f19f8fbee",
                M3Jep510KdfBackportRecipe.FINAL_JEP);
        assertEquals(
                "012b4eb6cea6e1756a589a6c17a805867ed60686",
                M3Jep510KdfBackportRecipe.SECRET_CLEANUP_FIX);
        assertEquals(
                "79456110fb6dd11ef19e9637c6f40ee7ce329481",
                M3Jep510KdfBackportRecipe.DOC_FIX);

        assertEquals(1, recipe.getRecipeList().size());
        var crate =
                assertInstanceOf(
                        M3Jdk21HashPinnedSnapshotRecipe.class,
                        recipe.getRecipeList().getFirst());
        assertEquals("jdk25-jep510-kdf", crate.getCrateName());

        var policy = M3RecipeScopeRegistry.require(M3Jep510KdfBackportRecipe.class);
        assertEquals(M3EditScope.LIBRARY_API, policy.minimumScope());
        assertEquals(M3ContractMode.EXPLICIT_CONTRACT_CHANGE, policy.contractMode());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("security"));
    }

    @Test
    void namedRecipeIsDiscoverable() {
        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.Jep510Kdf");
        assertEquals(
                "com.m3.jdk21.Jep510Kdf",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void exactCurrentMasterPreimagesReplayEightTargetsThenReachFixedPoint()
            throws Exception {
        var recipe = new M3Jep510KdfBackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(8, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            SourceFile source = change.getAfter();
            if (source != null) after.put(path(source), source);
        }

        for (ManifestRow row : manifest()) {
            assertEquals(resource(ROOT + row.resource()), after.get(row.path()).printAll());
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
    void driftedProviderPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = baseline();
        String target = "src/java.base/share/classes/java/security/Provider.java";
        int index = indexOf(sources, target);
        SourceFile current = sources.get(index);
        sources.set(index, parse(target, current.printAll() + "\n// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jep510KdfBackportRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void receiverPostimagesDoNotImportUnrelatedMlKemOrPreviewCleanup() throws Exception {
        String sunJce = resource(ROOT + "01-SunJCE.java.after.txt");
        assertTrue(sunJce.contains("ChaCha20, and HKDF"));
        assertTrue(!sunJce.contains("ML-KEM"));

        String provider = resource(ROOT + "02-Provider.java.after.txt");
        assertTrue(provider.contains("addEngine(\"KDF\""));
        String debug = resource(ROOT + "07-Debug.java.after.txt");
        assertTrue(debug.contains("Cipher, KDF, KeyAgreement"));
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

    private static List<ManifestRow> manifest() throws IOException {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(ROOT + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            rows.add(new ManifestRow(cells[0], cells[1], cells[2], cells[3]));
        }
        assertEquals(8, rows.size());
        return List.copyOf(rows);
    }

    private record ManifestRow(String path, String before, String after, String resource) {}

    private static String resource(String name) throws IOException {
        try (var input = M3Jep510KdfBackportRecipeTest.class.getResourceAsStream(name)) {
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
