// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.atom.M3PureIntConvergenceRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

class M3JdkA3RecipeExportTest {
    private static final Path MANIFEST =
            Path.of("src/main/resources/META-INF/m3/jdk-a3-recipe-export.tsv");

    @Test
    void manifestPinsEveryCanonicalExportSourceToItsGitBlob() throws Exception {
        Map<String, Row> rows = rows();
        assertEquals(8, rows.size());
        for (Row row : rows.values()) {
            Path source = Path.of(row.path());
            assertTrue(Files.isRegularFile(source), row.path());
            assertEquals(row.gitBlobSha1(), gitBlob(Files.readAllBytes(source)), row.path());
        }
    }

    @Test
    void portableMasteryVerifierIsPartOfTheSlimExport() throws Exception {
        Map<String, Row> rows = rows();
        Row row = rows.get("com.synexia.rewrite.M3RecipeMasteryPortableReceipt");
        assertTrue(row != null);
        assertEquals(
                "src/main/java/com/synexia/rewrite/M3RecipeMasteryPortableReceipt.java",
                row.path());
        assertEquals(
                row.gitBlobSha1(),
                gitBlob(Files.readAllBytes(Path.of(row.path()))));
    }

    @Test
    void exportedConvergenceRecipeIsTheCanonicalFileLocalDag() {
        Recipe recipe = new M3PureIntConvergenceRecipe();
        assertTrue(recipe.getTags().contains("synexia"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
        assertEquals(3, recipe.getRecipeList().size());
    }

    private static Map<String, Row> rows() throws IOException {
        List<String> lines = Files.readAllLines(MANIFEST, StandardCharsets.UTF_8);
        Map<String, Row> rows = new LinkedHashMap<>();
        boolean headerSeen = false;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            if (!headerSeen) {
                assertEquals("class\tpath\tgitBlobSha1", line);
                headerSeen = true;
                continue;
            }
            String[] cells = line.split("\\t", -1);
            assertEquals(3, cells.length);
            Row row = new Row(cells[0], cells[1], cells[2]);
            assertEquals(null, rows.putIfAbsent(row.className(), row), row.className());
        }
        assertTrue(headerSeen);
        return rows;
    }

    private static String gitBlob(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private record Row(String className, String path, String gitBlobSha1) {}
}
