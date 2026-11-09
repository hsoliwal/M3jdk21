// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

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

final class ImportCoverageTest {
    @TempDir
    Path temp;

    @Test
    void importCliExecutesVerifyMaterializeAndVerifyTargetBranches() throws Exception {
        Fixture fixture = fixture("cli");
        Path manifest = temp.resolve("manifest.tsv");
        Files.writeString(manifest, fixture.manifest().toTsv());

        ImportCli.main(
                new String[] {
                    "verify",
                    manifest.toString(),
                    fixture.m3().toString(),
                    fixture.m3jdk().toString()
                });
        ImportCli.main(
                new String[] {
                    "materialize",
                    manifest.toString(),
                    fixture.m3().toString(),
                    fixture.m3jdk().toString()
                });
        assertTrue(Files.isRegularFile(fixture.m3jdk().resolve(fixture.targetPath())));

        ImportCli.main(
                new String[] {
                    "verify-target",
                    manifest.toString(),
                    fixture.m3().toString(),
                    fixture.m3jdk().toString()
                });

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ImportCli.main(
                                new String[] {
                                    "unknown",
                                    manifest.toString(),
                                    fixture.m3().toString(),
                                    fixture.m3jdk().toString()
                                }));
    }

    @Test
    void manifestParserCoversCommentsDeclaredRootAndEveryStructuralRefusal() throws Exception {
        Fixture fixture = fixture("manifest");
        String valid = fixture.manifest().toTsv();
        String withComment =
                valid.replace(
                        ImportManifest.HEADER + "\n",
                        ImportManifest.HEADER + "\n# evidence only\n");
        assertEquals(fixture.manifest(), ImportManifest.parse(withComment));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ImportManifest.parse(
                                valid + "# root\t" + fixture.manifest().root() + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ImportManifest.parse(
                                ImportManifest.HEADER
                                        + "\n"
                                        + "a".repeat(40)
                                        + "\tm3jdk21\tseed\ttoo\tfew\tcells\n"
                                        + "# root\t"
                                        + "1".repeat(64)
                                        + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ImportManifest.parse(
                                valid.replaceFirst(
                                        "a{40}\\tm3jdk21",
                                        "a".repeat(40) + "\tother")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ImportManifest.parse(
                                valid.replace(
                                        "\tAPACHE_SOURCE\n",
                                        "\tUNKNOWN_MODE\n")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ImportManifest.parse(
                                ImportManifest.HEADER
                                        + "\n# comment only\n# root\t"
                                        + "1".repeat(64)
                                        + "\n"));
    }

    @Test
    void manifestValueObjectsRejectEmptyBadCommitBadHashNullModeAndControlCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest(
                                "short",
                                "m3jdk21",
                                List.of(validEntry()),
                                ""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest(
                                "a".repeat(40),
                                "m3jdk21",
                                List.of(),
                                ""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest.Entry(
                                "seed",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "bad",
                                "Apache-2.0",
                                ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                NullPointerException.class,
                () ->
                        new ImportManifest.Entry(
                                "seed",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest.Entry(
                                "bad\tcategory",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest.Entry(
                                "seed",
                                "../A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                ImportManifest.Mode.APACHE_SOURCE));
    }

    @Test
    void importerRejectsMissingTargetSnapshotAndNonRegularTargets() throws Exception {
        Fixture fixture = fixture("targets");

        assertThrows(
                IllegalStateException.class,
                () -> Importer.verifyTargetSnapshot(fixture.m3jdk(), fixture.manifest()));

        Path target = fixture.m3jdk().resolve(fixture.targetPath());
        Files.createDirectories(target);
        assertThrows(
                IllegalStateException.class,
                () -> Importer.verify(fixture.m3(), fixture.m3jdk(), fixture.manifest()));
        assertThrows(
                IllegalStateException.class,
                () -> Importer.verifyTargetSnapshot(fixture.m3jdk(), fixture.manifest()));
    }

    @Test
    void borrowingLedgerLoadRejectsMissingAndDirectoryInputs() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> RecipeOwnershipPolicy.load(temp.resolve("missing.tsv")));

        Path directory = temp.resolve("ledger-directory");
        Files.createDirectories(directory);
        assertThrows(
                IllegalArgumentException.class,
                () -> RecipeOwnershipPolicy.load(directory));
    }

    private Fixture fixture(String name) throws Exception {
        Path m3 = temp.resolve(name + "-synexia");
        Path m3jdk = temp.resolve(name + "-m3jdk");
        Files.createDirectories(m3);
        Files.createDirectories(m3jdk);

        String sourcePath =
                "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/A.java";
        String targetPath = "m3/vendor/m3/" + sourcePath;
        byte[] bytes =
                ("// SPDX-License-Identifier: Apache-2.0\n"
                                + "package jdk.internal.m3.rewrite; final class A {}\n")
                        .getBytes(StandardCharsets.UTF_8);
        Path source = m3.resolve(sourcePath);
        Files.createDirectories(source.getParent());
        Files.write(source, bytes);

        ImportManifest.Entry entry =
                new ImportManifest.Entry(
                        "openrewrite-java",
                        sourcePath,
                        targetPath,
                        sha256(bytes),
                        "Apache-2.0",
                        ImportManifest.Mode.APACHE_SOURCE);
        ImportManifest manifest =
                new ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry),
                        "");
        return new Fixture(m3, m3jdk, targetPath, manifest);
    }

    private static ImportManifest.Entry validEntry() {
        return new ImportManifest.Entry(
                "seed",
                "A.java",
                "m3/vendor/m3/A.java",
                "1".repeat(64),
                "Apache-2.0",
                ImportManifest.Mode.APACHE_SOURCE);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record Fixture(
            Path m3,
            Path m3jdk,
            String targetPath,
            ImportManifest manifest) {}
}
