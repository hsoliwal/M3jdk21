// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

final class M3SynexiaRecipeHomePolicyTest {
    @Test
    void reusableM3Jdk21RecipeAndM3IndexSurfacesAreConsumerOnly() throws Exception {
        Path root = repositoryRoot();
        Path policy = root.resolve("m3/compatibility/synexia-recipe-home-policy.tsv");
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
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/**",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/**",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/**",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/**",
                "m3/indexdb/**",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java")) {
            String[] row = rows.get(surface);
            assertNotNull(row, surface);
            assertEquals("MIGRATION_RESIDUE_NOT_CANONICAL", row[4], surface);
            assertEquals("true", row[7], surface);
        }

        assertEquals(
                "com.synexia:synexia-m3index-db",
                rows.get("m3/indexdb/**")[5]);
        assertEquals(
                "com.synexia:synexia-m3index-jdk-bridge",
                rows.get("m3/ports/**")[5]);
        assertEquals(
                "RECEIVER_ADAPTER_ONLY",
                rows.get("m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/InstallIndexStringCompatibility.java")[4]);
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
                root.resolve("m3/tooling/migration-recipes/src/main/java/com/m3/rewrite");

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
        Path manifest =
                root.resolve("m3/compatibility/synexia-canonical-residue-gitblobs.tsv");
        assertTrue(Files.isRegularFile(manifest));

        List<String> lines = Files.readAllLines(manifest);
        assertEquals(
                "path\tgit_blob_sha1\tdisposition\tcanonical_synexia_owner\tlicense",
                lines.getFirst());

        Map<String, FrozenRow> frozen = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            assertEquals(5, columns.length);
            assertTrue(columns[1].matches("[0-9a-f]{40}"), columns[0]);
            assertEquals("MIGRATION_RESIDUE_NOT_CANONICAL", columns[2], columns[0]);
            assertTrue(
                    columns[3].startsWith("com.synexia")
                            || columns[3].startsWith("synexia-"),
                    columns[0]);
            assertEquals("Apache-2.0", columns[4], columns[0]);
            frozen.put(columns[0], new FrozenRow(columns[1], columns[3]));
        }

        Set<String> actual = reusableResiduePaths(root);
        assertEquals(new TreeSet<>(frozen.keySet()), actual);

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

    private static Set<String> reusableResiduePaths(Path root) throws Exception {
        TreeSet<String> result = new TreeSet<>();
        for (String exact : List.of(
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java")) {
            if (Files.isRegularFile(root.resolve(exact))) {
                result.add(exact);
            }
        }

        for (String directory : List.of(
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic",
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3",
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
                repositoryRoot().resolve("m3/compatibility/synexia-recipe-home-policy.tsv"));
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
                repositoryRoot().resolve("m3/compatibility/synexia-recipe-home-policy.tsv"));
        Map<String, String[]> result = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\t", -1);
            result.put(columns[3], columns);
        }
        return result;
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
                return path.startsWith(surface.substring(0, surface.length() - 2));
            }
            return surface.equals(path);
        }
    }

    private record FrozenRow(String gitBlobSha1, String canonicalOwner) {}
}
