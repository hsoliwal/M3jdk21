// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.synexia.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PublicA3ExportMirrorTest {
    private static final String EXPORT_BLOB =
            "8a3e3d6e802e95dbcc7b0bf83a347887b02d6717";

    @Test
    void mirrorMatchesCanonicalSynexiaExportExactly() throws Exception {
        Path repository = repositoryRoot();
        Path mirror = repository.resolve(
                "m3/vendor/synexia/synexia-openrewrite-recipes");
        Path manifest = mirror.resolve(
                "src/main/resources/META-INF/m3/jdk-a3-recipe-export.tsv");

        assertTrue(Files.isRegularFile(manifest));
        assertFalse(Files.isSymbolicLink(manifest));
        assertEquals(EXPORT_BLOB, gitBlob(Files.readAllBytes(manifest)));

        List<String> rows = Files.readAllLines(manifest, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
        assertEquals("class\tpath\tgitBlobSha1", rows.getFirst());
        assertEquals(9, rows.size());

        ArrayList<String> compiledSources = new ArrayList<>();
        for (String row : rows.subList(1, rows.size())) {
            String[] cells = row.split("\t", -1);
            assertEquals(3, cells.length);
            Path source = mirror.resolve(cells[1]).normalize();
            assertTrue(source.startsWith(mirror));
            assertTrue(Files.isRegularFile(source), cells[1]);
            assertFalse(Files.isSymbolicLink(source), cells[1]);
            assertEquals(cells[2], gitBlob(Files.readAllBytes(source)), cells[1]);
            compiledSources.add(cells[1]);
        }

        String pom = Files.readString(
                repository.resolve("m3/tooling/synexia-jdk-a3-recipes/pom.xml"),
                StandardCharsets.UTF_8);
        for (String source : compiledSources) {
            String relative = source.substring("src/main/java/".length());
            assertTrue(pom.contains("<include>" + relative + "</include>"), relative);
        }
        assertFalse(pom.contains("<include>com/synexia/rewrite/**/*.java</include>"));
        assertTrue(pom.contains("<artifactId>synexia-jdk-a3-recipes</artifactId>"));
        assertTrue(pom.contains("<version>1.0.0-SNAPSHOT</version>"));
    }

    private static Path repositoryRoot() {
        Path current = Path.of(
                        System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            if ("m3".equals(cursor.getFileName() == null ? "" : cursor.getFileName().toString())
                    && Files.isRegularFile(cursor.resolve("pom.xml"))) {
                return cursor.getParent();
            }
        }
        throw new IllegalStateException("M3JDK21 repository root not found");
    }

    private static String gitBlob(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }
}
