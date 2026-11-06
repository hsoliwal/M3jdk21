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

final class M3ImportCoverageTest {
    @TempDir
    Path temp;

    @Test
    void importCliExecutesVerifyMaterializeAndVerifyTargetBranches() throws Exception {
        Fixture fixture = fixture("cli");
        Path manifest = temp.resolve("manifest.tsv");
        Files.writeString(manifest, fixture.manifest().toTsv());

        M3ImportCli.main(
                new String[] {
                    "verify",
                    manifest.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString()
                });
        M3ImportCli.main(
                new String[] {
                    "materialize",
                    manifest.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString()
                });
        assertTrue(Files.isRegularFile(fixture.m3jdk().resolve(fixture.targetPath())));

        M3ImportCli.main(
                new String[] {
                    "verify-target",
                    manifest.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString()
                });

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3ImportCli.main(
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
                        M3ImportManifest.HEADER + "\n",
                        M3ImportManifest.HEADER + "\n# evidence only\n");
        assertEquals(fixture.manifest(), M3ImportManifest.parse(withComment));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3ImportManifest.parse(
                                valid + "# root\t" + fixture.manifest().root() + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3ImportManifest.parse(
                                M3ImportManifest.HEADER
                                        + "\n"
                                        + "a".repeat(40)
                                        + "\tm3jdk21\tseed\ttoo\tfew\tcells\n"
                                        + "# root\t"
                                        + "1".repeat(64)
                                        + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3ImportManifest.parse(
                                valid.replaceFirst(
                                        "a{40}\\tm3jdk21",
                                        "a".repeat(40) + "\tother")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3ImportManifest.parse(
                                valid.replace(
                                        "\tAPACHE_SOURCE\n",
                                        "\tUNKNOWN_MODE\n")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3ImportManifest.parse(
                                M3ImportManifest.HEADER
                                        + "\n# comment only\n# root\t"
                                        + "1".repeat(64)
                                        + "\n"));
    }

    @Test
    void manifestValueObjectsRejectEmptyBadCommitBadHashNullModeAndControlCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest(
                                "short",
                                "m3jdk21",
                                List.of(validEntry()),
                                ""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest(
                                "a".repeat(40),
                                "m3jdk21",
                                List.of(),
                                ""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "seed",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "bad",
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                NullPointerException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "seed",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "bad\tcategory",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "seed",
                                "../A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE));
    }

    @Test
    void importerRejectsMissingTargetSnapshotAndNonRegularTargets() throws Exception {
        Fixture fixture = fixture("targets");

        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verifyTargetSnapshot(fixture.m3jdk(), fixture.manifest()));

        Path target = fixture.m3jdk().resolve(fixture.targetPath());
        Files.createDirectories(target);
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verify(fixture.synexia(), fixture.m3jdk(), fixture.manifest()));
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verifyTargetSnapshot(fixture.m3jdk(), fixture.manifest()));
    }

    @Test
    void borrowingLedgerLoadRejectsMissingAndDirectoryInputs() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.load(temp.resolve("missing.tsv")));

        Path directory = temp.resolve("ledger-directory");
        Files.createDirectories(directory);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.load(directory));
    }

    private Fixture fixture(String name) throws Exception {
        Path synexia = temp.resolve(name + "-synexia");
        Path m3jdk = temp.resolve(name + "-m3jdk");
        Files.createDirectories(synexia);
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

        M3ImportManifest.Entry entry =
                new M3ImportManifest.Entry(
                        "openrewrite-java",
                        sourcePath,
                        targetPath,
                        sha256(bytes),
                        "Apache-2.0",
                        M3ImportManifest.Mode.APACHE_SOURCE);
        M3ImportManifest manifest =
                new M3ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry),
                        "");
        return new Fixture(synexia, m3jdk, targetPath, manifest);
    }

    private static M3ImportManifest.Entry validEntry() {
        return new M3ImportManifest.Entry(
                "seed",
                "A.java",
                "m3/vendor/m3/A.java",
                "1".repeat(64),
                "Apache-2.0",
                M3ImportManifest.Mode.APACHE_SOURCE);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record Fixture(
            Path synexia,
            Path m3jdk,
            String targetPath,
            M3ImportManifest manifest) {}
}
