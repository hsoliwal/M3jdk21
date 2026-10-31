// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SynexiaImportCoverageTest {
    @TempDir
    Path temp;

    @Test
    void importCliExecutesVerifyMaterializeAndVerifyTargetBranches() throws Exception {
        Fixture fixture = fixture("cli");
        Path manifest = temp.resolve("manifest.tsv");
        Files.writeString(manifest, fixture.manifest().toTsv());

        SynexiaImportCli.main(
                new String[] {
                    "verify",
                    manifest.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString()
                });
        SynexiaImportCli.main(
                new String[] {
                    "materialize",
                    manifest.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString()
                });
        assertTrue(Files.isRegularFile(fixture.m3jdk().resolve(fixture.targetPath())));

        SynexiaImportCli.main(
                new String[] {
                    "verify-target",
                    manifest.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString()
                });

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaImportCli.main(
                                new String[] {
                                    "unknown",
                                    manifest.toString(),
                                    fixture.synexia().toString(),
                                    fixture.m3jdk().toString()
                                }));
    }

    @Test
    void manifestParserCoversCommentsDeclaredRootAndEveryStructuralRefusal() throws Exception {
        Fixture fixture = fixture("manifest");
        String valid = fixture.manifest().toTsv();
        String withComment =
                valid.replace(
                        SynexiaImportManifest.HEADER + "\n",
                        SynexiaImportManifest.HEADER + "\n# evidence only\n");
        assertEquals(fixture.manifest(), SynexiaImportManifest.parse(withComment));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaImportManifest.parse(
                                valid + "# root\t" + fixture.manifest().root() + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaImportManifest.parse(
                                SynexiaImportManifest.HEADER
                                        + "\n"
                                        + "a".repeat(40)
                                        + "\tm3jdk21\tseed\ttoo\tfew\tcells\n"
                                        + "# root\t"
                                        + "1".repeat(64)
                                        + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaImportManifest.parse(
                                valid.replaceFirst(
                                        "a{40}\\tm3jdk21",
                                        "a".repeat(40) + "\tother")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaImportManifest.parse(
                                valid.replace(
                                        "\tAPACHE_SOURCE\n",
                                        "\tUNKNOWN_MODE\n")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaImportManifest.parse(
                                SynexiaImportManifest.HEADER
                                        + "\n# comment only\n# root\t"
                                        + "1".repeat(64)
                                        + "\n"));
    }

    @Test
    void manifestValueObjectsRejectEmptyBadCommitBadHashNullModeAndControlCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest(
                                "short",
                                "m3jdk21",
                                List.of(validEntry()),
                                ""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest(
                                "a".repeat(40),
                                "m3jdk21",
                                List.of(),
                                ""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "seed",
                                "A.java",
                                "m3/vendor/synexia/A.java",
                                "bad",
                                "Apache-2.0",
                                SynexiaImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                NullPointerException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "seed",
                                "A.java",
                                "m3/vendor/synexia/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "bad\tcategory",
                                "A.java",
                                "m3/vendor/synexia/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                SynexiaImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "seed",
                                "../A.java",
                                "m3/vendor/synexia/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                SynexiaImportManifest.Mode.APACHE_SOURCE));
    }

    @Test
    void importerRejectsMissingTargetSnapshotAndNonRegularTargets() throws Exception {
        Fixture fixture = fixture("targets");

        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verifyTargetSnapshot(fixture.m3jdk(), fixture.manifest()));

        Path target = fixture.m3jdk().resolve(fixture.targetPath());
        Files.createDirectories(target);
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verify(fixture.synexia(), fixture.m3jdk(), fixture.manifest()));
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verifyTargetSnapshot(fixture.m3jdk(), fixture.manifest()));
    }

    @Test
    void borrowingLedgerLoadRejectsMissingAndDirectoryInputs() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.load(temp.resolve("missing.tsv")));

        Path directory = temp.resolve("ledger-directory");
        Files.createDirectories(directory);
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.load(directory));
    }

    private Fixture fixture(String name) throws Exception {
        Path synexia = temp.resolve(name + "-synexia");
        Path m3jdk = temp.resolve(name + "-m3jdk");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath =
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/A.java";
        String targetPath = "m3/vendor/synexia/" + sourcePath;
        byte[] bytes =
                ("// SPDX-License-Identifier: Apache-2.0\n"
                                + "package com.synexia.rewrite; final class A {}\n")
                        .getBytes(StandardCharsets.UTF_8);
        Path source = synexia.resolve(sourcePath);
        Files.createDirectories(source.getParent());
        Files.write(source, bytes);

        SynexiaImportManifest.Entry entry =
                new SynexiaImportManifest.Entry(
                        "openrewrite-java",
                        sourcePath,
                        targetPath,
                        sha256(bytes),
                        "Apache-2.0",
                        SynexiaImportManifest.Mode.APACHE_SOURCE);
        SynexiaImportManifest manifest =
                new SynexiaImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry),
                        "");
        return new Fixture(synexia, m3jdk, targetPath, manifest);
    }

    private static SynexiaImportManifest.Entry validEntry() {
        return new SynexiaImportManifest.Entry(
                "seed",
                "A.java",
                "m3/vendor/synexia/A.java",
                "1".repeat(64),
                "Apache-2.0",
                SynexiaImportManifest.Mode.APACHE_SOURCE);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record Fixture(
            Path synexia,
            Path m3jdk,
            String targetPath,
            SynexiaImportManifest manifest) {}
}
