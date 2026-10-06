// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                "m3/indexdb/**")) {
            String[] row = rows.get(surface);
            assertEquals("MIGRATION_RESIDUE_NOT_CANONICAL", row[4], surface);
            assertEquals("true", row[7], surface);
        }

        assertEquals(
                "com.synexia:synexia-m3index-db",
                rows.get("m3/indexdb/**")[5]);
        assertEquals(
                "com.synexia:synexia-m3index-jdk-bridge",
                rows.get("m3/ports/**")[5]);
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
}
