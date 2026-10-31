// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SynexiaImporterTest {
    @TempDir
    Path temp;

    @Test
    void exactSourceMaterializesAndSecondPassIsIdempotent() throws Exception {
        Path synexia = temp.resolve("synexia");
        Path m3jdk = temp.resolve("m3jdk");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath =
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3EditScope.java";
        String targetPath =
                "m3/vendor/synexia/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3EditScope.java";
        byte[] bytes =
                ("// SPDX-License-Identifier: Apache-2.0\n"
                                + "package com.synexia.rewrite; enum M3EditScope { FILE }\n")
                        .getBytes(StandardCharsets.UTF_8);
        write(synexia.resolve(sourcePath), bytes);

        SynexiaImportManifest manifest = manifest(sourcePath, targetPath, bytes);
        SynexiaImporter.verify(synexia, m3jdk, manifest);
        assertFalse(Files.exists(m3jdk.resolve(targetPath)));

        SynexiaImporter.materialize(synexia, m3jdk, manifest);
        SynexiaImporter.verifyTargetSnapshot(m3jdk, manifest);
        assertTrue(Files.isRegularFile(m3jdk.resolve(targetPath)));
        assertEquals(sha256(bytes), sha256(Files.readAllBytes(m3jdk.resolve(targetPath))));

        SynexiaImporter.materialize(synexia, m3jdk, manifest);
        assertEquals(sha256(bytes), sha256(Files.readAllBytes(m3jdk.resolve(targetPath))));
    }

    @Test
    void sourceDriftAndTargetDriftFailClosed() throws Exception {
        Path synexia = temp.resolve("synexia-drift");
        Path m3jdk = temp.resolve("m3jdk-drift");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath = "module/src/main/java/p/A.java";
        String targetPath = "m3/vendor/synexia/module/src/main/java/p/A.java";
        byte[] before = apache("package p; final class A {}\n");
        write(synexia.resolve(sourcePath), before);
        SynexiaImportManifest manifest = manifest(sourcePath, targetPath, before);

        write(synexia.resolve(sourcePath), apache("package p; final class A { int x; }\n"));
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verify(synexia, m3jdk, manifest));

        write(synexia.resolve(sourcePath), before);
        write(m3jdk.resolve(targetPath), apache("package p; final class A { int y; }\n"));
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verifyTargetSnapshot(m3jdk, manifest));
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verify(synexia, m3jdk, manifest));
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.materialize(synexia, m3jdk, manifest));
    }

    @Test
    void missingSourceAndUnsafeRootsFailClosed() throws Exception {
        Path synexia = temp.resolve("synexia-missing");
        Path m3jdk = temp.resolve("m3jdk-missing");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath = "module/src/main/java/p/A.java";
        String targetPath = "m3/vendor/synexia/module/src/main/java/p/A.java";
        byte[] bytes = apache("package p; final class A {}\n");
        SynexiaImportManifest manifest = manifest(sourcePath, targetPath, bytes);

        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verify(synexia, m3jdk, manifest));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImporter.verify(temp.resolve("absent"), m3jdk, manifest));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImporter.verify(synexia, temp.resolve("absent-target"), manifest));
    }

    @Test
    void manifestRoundTripAndValidationAreDeterministic() {
        byte[] bytes = apache("package p; final class A {}\n");
        SynexiaImportManifest original = manifest(
                "module/src/main/java/p/A.java",
                "m3/vendor/synexia/module/src/main/java/p/A.java",
                bytes);

        SynexiaImportManifest parsed = SynexiaImportManifest.parse(original.toTsv());
        assertEquals(original, parsed);
        assertEquals(64, parsed.root().length());
        assertEquals("m3jdk21", parsed.targetId());
        assertEquals(1, parsed.entries().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportManifest.parse("bad\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportManifest.parse(
                        SynexiaImportManifest.HEADER
                                + "\n"
                                + "a".repeat(40)
                                + "\tm3jdk21\tjava\tA.java\tm3/vendor/synexia/A.java\t"
                                + "1".repeat(64)
                                + "\tMIT\tAPACHE_SOURCE\n# root\t"
                                + "2".repeat(64)
                                + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportManifest.Entry(
                        "java",
                        "A.java",
                        "../A.java",
                        "1".repeat(64),
                        "Apache-2.0",
                        SynexiaImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportManifest.Entry(
                        "java",
                        "A.java",
                        "outside/A.java",
                        "1".repeat(64),
                        "Apache-2.0",
                        SynexiaImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportManifest(
                        "a".repeat(40),
                        "other",
                        List.of(new SynexiaImportManifest.Entry(
                                "java",
                                "A.java",
                                "m3/vendor/synexia/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                SynexiaImportManifest.Mode.APACHE_SOURCE)),
                        ""));
    }

    @Test
    void producerAndReceiverShareKnownDeliveryRootVector() {
        SynexiaImportManifest.Entry entry =
                new SynexiaImportManifest.Entry(
                        "java",
                        "A.java",
                        "m3/vendor/synexia/A.java",
                        "1".repeat(64),
                        "Apache-2.0",
                        SynexiaImportManifest.Mode.APACHE_SOURCE);
        SynexiaImportManifest manifest =
                new SynexiaImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry),
                        "");
        assertEquals(
                "01b4e1f8804e2699c9133c3ca7dbf3f2841f2ff3991096778a6131cbc9c3fc55",
                manifest.root());
    }

    @Test
    void duplicateTargetAndRootMismatchFailClosed() {
        SynexiaImportManifest.Entry entry = new SynexiaImportManifest.Entry(
                "java",
                "A.java",
                "m3/vendor/synexia/A.java",
                "1".repeat(64),
                "Apache-2.0",
                SynexiaImportManifest.Mode.APACHE_SOURCE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportManifest(
                        "a".repeat(40), "m3jdk21", List.of(entry, entry), ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportManifest(
                        "a".repeat(40), "m3jdk21", List.of(entry), "2".repeat(64)));
    }

    @Test
    void cliRejectsUnknownActionAndBadArity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportCli.main(new String[0]));
        assertThrows(
                Exception.class,
                () -> SynexiaImportCli.main(new String[] {
                    "other",
                    temp.resolve("manifest.tsv").toString(),
                    temp.resolve("synexia").toString(),
                    temp.resolve("m3jdk").toString()
                }));
    }

    private static SynexiaImportManifest manifest(
            String sourcePath,
            String targetPath,
            byte[] bytes) {
        return new SynexiaImportManifest(
                "a".repeat(40),
                "m3jdk21",
                List.of(new SynexiaImportManifest.Entry(
                        "seed",
                        sourcePath,
                        targetPath,
                        sha256(bytes),
                        "Apache-2.0",
                        SynexiaImportManifest.Mode.APACHE_SOURCE)),
                "");
    }

    private static byte[] apache(String body) {
        return ("// SPDX-License-Identifier: Apache-2.0\n" + body)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
// deliberate input drift
