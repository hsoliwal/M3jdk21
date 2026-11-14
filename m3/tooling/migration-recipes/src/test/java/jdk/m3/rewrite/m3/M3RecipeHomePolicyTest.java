// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3RecipeHomePolicyTest {
    private static final String RESIDUE_MANIFEST = "m3/compatibility/m3-canonical-residue-gitblobs.tsv";

    /** Policy-ledger dispositions that keep a walked file out of the residue manifest: it is an adapter. */
    private static final Set<String> ADAPTER_DISPOSITIONS =
            Set.of("RECEIVER_ADAPTER_ONLY", "TARGET_PRODUCT_ADAPTER");

    @Test
    void reusableM3Jdk21RecipeAndM3IndexSurfacesAreConsumerOnly() throws Exception {
        Path root = repositoryRoot();
        Path policy = root.resolve("m3/compatibility/m3-recipe-home-policy.tsv");
        assertTrue(Files.isRegularFile(policy));

        List<String> lines = Files.readAllLines(policy);
        assertEquals(
                "schema\tcanonical_repository\ttarget_repository\tlocal_surface\tdisposition\tcanonical_owner\tlicense\thandoff_required",
                lines.getFirst());

        Map<String, String[]> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            assertEquals(8, columns.length);
            assertEquals("M3JDK21_SYNEXIA_RECIPE_HOME_V1", columns[0]);
            assertEquals("hsoliwal/com.synexia", columns[1]);
            assertEquals("hsoliwal/M3jdk21", columns[2]);
            assertEquals("Apache-2.0", columns[6]);
            rows.put(columns[3], columns);
        }

        for (String surface : List.of(
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/**",
                "m3/indexdb/**")) {
            String[] row = rows.get(surface);
            assertNotNull(row, surface);
            assertEquals("MIGRATION_RESIDUE_NOT_CANONICAL", row[4], surface);
            assertEquals("true", row[7], surface);
        }
        // The ledger records A3 and the convergence pair as Synexia receiver adapters since 2026-10-13.
        for (Map.Entry<String, String> adapter : Map.of(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/**",
                        "com.synexia.rewrite.M3Jdk21A3AlgorithmCatalogueRecipe;com.synexia.rewrite.M3Jdk21A3SerialFileWorkRecipe",
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/M3Java21ConvergenceCatalog.java",
                        "com.synexia.rewrite.M3Jdk21FileConvergenceRecipe",
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/M3Java21ConvergenceRecipe.java",
                        "com.synexia.rewrite.M3Jdk21FileConvergenceRecipe")
                .entrySet()) {
            String[] row = rows.get(adapter.getKey());
            assertNotNull(row, adapter.getKey());
            assertEquals("RECEIVER_ADAPTER_ONLY", row[4], adapter.getKey());
            assertEquals(adapter.getValue(), row[5], adapter.getKey());
            assertEquals("true", row[7], adapter.getKey());
        }

        assertScopeRow(
                rows,
                "M3EditScope.java",
                "RECEIVER_ADAPTER_ONLY",
                "com.synexia.rewrite.M3EditScope",
                "true");
        assertScopeRow(
                rows,
                "M3ScopeInference.java",
                "TARGET_PRODUCT_ADAPTER",
                "com.synexia.rewrite.M3ScopeInference",
                "true");
        assertScopeRow(
                rows,
                "M3ContractMode.java",
                "RECEIVER_ADAPTER_ONLY",
                "com.synexia.rewrite.M3ContractMode",
                "true");
        assertScopeRow(
                rows,
                "M3RecipeScopePolicy.java",
                "TARGET_PRODUCT_ADAPTER",
                "com.synexia.rewrite.M3RecipeScopePolicy",
                "true");
        assertScopeRow(
                rows,
                "M3RecipeScopeRegistry.java",
                "TARGET_PRODUCT_ADAPTER",
                "com.synexia.rewrite.M3RecipeScopeRegistry",
                "true");

        assertEquals(
                "com.synexia:synexia-m3index-db",
                rows.get("m3/indexdb/**")[5]);
        assertEquals(
                "com.synexia:synexia-m3index-jdk-bridge",
                rows.get("m3/ports/**")[5]);
        assertEquals(
                "RECEIVER_ADAPTER_ONLY",
                rows.get("m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/InstallIndexStringCompatibility.java")[4]);

        String[] pureIntReceiver = rows.get(
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-pure-int.yml");
        assertNotNull(pureIntReceiver);
        assertEquals("RECEIVER_ADAPTER_ONLY", pureIntReceiver[4]);
        assertEquals(
                "com.synexia.rewrite.M3PureIntConvergenceRecipe",
                pureIntReceiver[5]);
        assertEquals("true", pureIntReceiver[7]);
    }

    @Test
    void stringRecipeDescriptorsDeclareCanonicalM3Owners() throws Exception {
        Path root = repositoryRoot();
        Map<String, String[]> rows = rows();

        Map<String, String[]> expected = Map.of(
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-stream-traversal.yml",
                new String[] {"RECEIVER_ADAPTER_ONLY",
                    "com.synexia.rewrite.M3Jdk21StringDirectStreamTraversal",
                    "jdk.internal.m3.rewrite.M3Jdk21StringDirectStreamTraversal"},
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-canonical-dag-master-repair.yml",
                new String[] {"TARGET_PRODUCT_ADAPTER",
                    "synexia.m3jdk.string.canonical-dag.20261006",
                    "m3.m3jdk.string.canonical-dag.20261006"},
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-regex-string-precompute-v2.yml",
                new String[] {"MIGRATION_RESIDUE_NOT_CANONICAL",
                    "com.synexia.indexstring.M3InstallCodeTextRegexPrecompute",
                    "jdk.internal.m3.indexstring.M3InstallCodeTextRegexPrecompute"},
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-mechanical-donor-map.yml",
                new String[] {"MIGRATION_RESIDUE_NOT_CANONICAL",
                    "com.synexia.M3MechanicalDonorShapeReview;com.synexia.rewrite.M3MechanicalPrecomputeInvariant",
                    "jdk.internal.m3.M3MechanicalDonorShapeReview;jdk.internal.m3.rewrite.M3MechanicalPrecomputeInvariant"});

        for (Map.Entry<String, String[]> entry : expected.entrySet()) {
            String[] row = rows.get(entry.getKey());
            assertNotNull(row, entry.getKey());
            assertEquals(entry.getValue()[0], row[4], entry.getKey());
            assertEquals(entry.getValue()[1], row[5], entry.getKey());
            assertEquals("true", row[7], entry.getKey());

            // [1] is the canonical Synexia owner in the ledger; [2] is the JDK-named receiver the descriptor
            // references since the naming rename.
            String descriptor = Files.readString(root.resolve(entry.getKey()));
            for (String owner : entry.getValue()[2].split(";")) {
                assertTrue(descriptor.contains(owner), entry.getKey() + " -> " + owner);
            }
        }
    }

    @Test
    void jdkBackportNamespaceRemainsTargetSpecific() throws Exception {
        Map<String, String[]> rows = rows();
        String[] backport = rows.get(
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/**");
        assertEquals("JDK_TARGET_SPECIFIC", backport[4]);
        assertEquals("false", backport[7]);

        String[] receiver = rows.get(
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/synexia/**");
        assertEquals("RECEIVER_ADAPTER_ONLY", receiver[4]);
        assertEquals("true", receiver[7]);
        assertFalse(receiver[5].isBlank());
    }

    @Test
    void everyLocalRecipeClassHasExactlyOneCanonicalPolicyClassification() throws Exception {
        Path root = repositoryRoot();
        List<PolicyRow> policy = policyRows();
        Path recipeRoot =
                root.resolve("m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite");

        List<String> sourcePaths;
        try (var files = Files.walk(recipeRoot)) {
            sourcePaths =
                    files.filter(Files::isRegularFile)
                            .filter(file -> file.getFileName().toString().endsWith(".java"))
                            .map(root::relativize)
                            .map(Path::toString)
                            .map(value -> value.replace('\\', '/'))
                            .sorted()
                            .toList();
        }

        assertFalse(sourcePaths.isEmpty());
        for (String sourcePath : sourcePaths) {
            List<PolicyRow> matches =
                    policy.stream().filter(row -> row.matches(sourcePath)).toList();
            assertEquals(1, matches.size(), "policy classification: " + sourcePath);
            PolicyRow row = matches.getFirst();
            assertFalse(row.canonicalOwner().isBlank(), sourcePath);
            assertTrue(
                    Set.of(
                                    "MIGRATION_RESIDUE_NOT_CANONICAL",
                                    "RECEIVER_ADAPTER_ONLY",
                                    "JDK_TARGET_SPECIFIC",
                                    "TARGET_PRODUCT_ADAPTER")
                            .contains(row.disposition()),
                    sourcePath + " -> " + row.disposition());
        }
    }

    @Test
    void reusableResidueIsContentFrozenAgainstIndependentTargetEvolution() throws Exception {
        Path root = repositoryRoot();
        Path manifest = root.resolve(RESIDUE_MANIFEST);
        assertTrue(Files.isRegularFile(manifest));

        List<String> lines = Files.readAllLines(manifest);
        assertEquals(
                "path\tgit_blob_sha1\tdisposition\tcanonical_synexia_owner\tlicense\tprevious_git_blob_sha1\tprevious_pin_commit",
                lines.getFirst());

        Map<String, FrozenRow> frozen = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            assertEquals(7, columns.length);
            assertTrue(columns[1].matches("[0-9a-f]{40}"), columns[0]);
            assertEquals("MIGRATION_RESIDUE_NOT_CANONICAL", columns[2], columns[0]);
            assertTrue(
                    columns[3].startsWith("com.synexia")
                            || columns[3].startsWith("synexia-"),
                    columns[0]);
            assertEquals("Apache-2.0", columns[4], columns[0]);
            frozen.put(columns[0], new FrozenRow(columns[1], columns[3]));
        }

        assertEquals(lines.size() - 1, frozen.size(), "duplicate residue rows");
        assertResidueSetClosed(root, policyRows(), frozen.keySet());

        for (Map.Entry<String, FrozenRow> entry : frozen.entrySet()) {
            Path file = root.resolve(entry.getKey());
            assertTrue(Files.isRegularFile(file), entry.getKey());
            assertEquals(
                    entry.getValue().gitBlobSha1(),
                    gitBlobSha1(file),
                    entry.getKey()
                            + " is canonical Synexia residue; improve "
                            + entry.getValue().canonicalOwner()
                            + " and consume a new handoff instead");
        }
    }

    @Test
    void plantedUnlistedNonAdapterFileFailsTheResidueSet(@TempDir Path scratch) throws Exception {
        Path root = repositoryRoot();
        List<PolicyRow> policy = policyRows();
        List<String> lines = Files.readAllLines(root.resolve(RESIDUE_MANIFEST));
        Set<String> frozen = new TreeSet<>();
        for (String line : lines.subList(1, lines.size())) {
            frozen.add(line.split("\t", -1)[0]);
        }
        Set<String> walked = residueCandidatePaths(root);

        // One plant under a residue surface of the policy ledger, one under no policy row at all.
        for (String planted : List.of(
                "m3/indexdb/src/main/java/jdk/m3/indexdb/M3PlantedUnlistedResidue.java",
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/scope/M3PlantedUnlistedResidue.java")) {
            Path mirror = Files.createTempDirectory(scratch, "mirror");
            for (String path : walked) {
                touch(mirror.resolve(path));
            }
            assertResidueSetClosed(mirror, policy, frozen);

            touch(mirror.resolve(planted));
            AssertionError failure = assertThrows(
                    AssertionError.class, () -> assertResidueSetClosed(mirror, policy, frozen), planted);
            assertTrue(failure.getMessage().contains(planted), failure.getMessage());
        }
    }

    /**
     * The walked files the residue manifest must list: every candidate except those whose
     * m3-recipe-home-policy.tsv rows all carry an adapter disposition.
     */
    private static void assertResidueSetClosed(Path root, List<PolicyRow> policy, Set<String> frozen)
            throws Exception {
        TreeSet<String> actual = new TreeSet<>();
        for (String path : residueCandidatePaths(root)) {
            List<PolicyRow> matches = policy.stream().filter(row -> row.matches(path)).toList();
            boolean adapter = !matches.isEmpty()
                    && matches.stream().allMatch(row -> ADAPTER_DISPOSITIONS.contains(row.disposition()));
            if (!adapter) {
                actual.add(path);
            }
        }
        assertEquals(new TreeSet<>(frozen), actual, "unlisted or retired reusable residue");
    }

    private static void touch(Path file) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[0]);
    }

    /** Every file the closed residue check walks: the two convergence files and five directories. */
    private static Set<String> residueCandidatePaths(Path root) throws Exception {
        TreeSet<String> result = new TreeSet<>();
        for (String exact : List.of(
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/M3Java21ConvergenceCatalog.java",
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/M3Java21ConvergenceRecipe.java")) {
            if (Files.isRegularFile(root.resolve(exact))) {
                result.add(exact);
            }
        }

        for (String directory : List.of(
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/scope",
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/atom",
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/semantic",
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/a3",
                "m3/indexdb/src/main/java")) {
            Path start = root.resolve(directory);
            if (!Files.isDirectory(start)) {
                continue;
            }
            try (var files = Files.walk(start)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".java"))
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(value -> value.replace('\\', '/'))
                        .forEach(result::add);
            }
        }
        return result;
    }

    private static String gitBlobSha1(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static List<PolicyRow> policyRows() throws Exception {
        List<String> lines = Files.readAllLines(
                repositoryRoot().resolve("m3/compatibility/m3-recipe-home-policy.tsv"));
        List<PolicyRow> result = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            result.add(
                    new PolicyRow(
                            columns[3],
                            columns[4],
                            columns[5],
                            Boolean.parseBoolean(columns[7])));
        }
        return List.copyOf(result);
    }

    private static Map<String, String[]> rows() throws Exception {
        List<String> lines = Files.readAllLines(
                repositoryRoot().resolve("m3/compatibility/m3-recipe-home-policy.tsv"));
        Map<String, String[]> result = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            result.put(columns[3], columns);
        }
        return result;
    }

    private static void assertScopeRow(
            Map<String, String[]> rows,
            String file,
            String disposition,
            String owner,
            String handoff) {
        String surface =
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/scope/" + file;
        String[] row = rows.get(surface);
        assertNotNull(row, surface);
        assertEquals(disposition, row[4], surface);
        assertEquals(owner, row[5], surface);
        assertEquals(handoff, row[7], surface);
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("migration-recipes".equals(module.getFileName().toString())) {
            return module.getParent().getParent().getParent();
        }
        return Path.of(System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
    }

    private record PolicyRow(
            String surface,
            String disposition,
            String canonicalOwner,
            boolean handoffRequired) {
        boolean matches(String path) {
            if (surface.endsWith("/**")) {
                // The ledger keeps the surface it recorded before the naming rename (#2257); the directory moved.
                return path.startsWith(
                        surface.substring(0, surface.length() - 2)
                                .replace("/com/m3/rewrite/synexia/", "/jdk/m3/rewrite/m3/")
                                .replace("/com/m3/rewrite/", "/jdk/m3/rewrite/"));
            }
            return surface.equals(path);
        }
    }

    private record FrozenRow(String gitBlobSha1, String canonicalOwner) {}
}
