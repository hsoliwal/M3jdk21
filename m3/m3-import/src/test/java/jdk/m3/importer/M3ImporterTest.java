// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3ImporterTest {
    @TempDir
    Path temp;

    @Test
    void exactSourceMaterializesAndSecondPassIsIdempotent() throws Exception {
        Path synexia = temp.resolve("synexia");
        Path m3jdk = temp.resolve("m3jdk");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath =
                "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/M3EditScope.java";
        String targetPath =
                "m3/vendor/m3/m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/M3EditScope.java";
        byte[] bytes =
                ("// SPDX-License-Identifier: Apache-2.0\n"
                                + "package jdk.internal.m3.rewrite; enum M3EditScope { FILE }\n")
                        .getBytes(StandardCharsets.UTF_8);
        write(m3.resolve(sourcePath), bytes);

        M3ImportManifest manifest = manifest(sourcePath, targetPath, bytes);
        M3Importer.verify(synexia, m3jdk, manifest);
        assertFalse(Files.exists(m3jdk.resolve(targetPath)));

        M3Importer.materialize(synexia, m3jdk, manifest);
        M3Importer.verifyTargetSnapshot(m3jdk, manifest);
        assertTrue(Files.isRegularFile(m3jdk.resolve(targetPath)));
        assertEquals(sha256(bytes), sha256(Files.readAllBytes(m3jdk.resolve(targetPath))));

        M3Importer.materialize(synexia, m3jdk, manifest);
        assertEquals(sha256(bytes), sha256(Files.readAllBytes(m3jdk.resolve(targetPath))));
    }

    @Test
    void sourceDriftAndTargetDriftFailClosed() throws Exception {
        Path synexia = temp.resolve("m3-drift");
        Path m3jdk = temp.resolve("m3jdk-drift");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath = "module/src/main/java/p/A.java";
        String targetPath = "m3/vendor/m3/module/src/main/java/p/A.java";
        byte[] before = apache("package p; final class A {}\n");
        write(m3.resolve(sourcePath), before);
        M3ImportManifest manifest = manifest(sourcePath, targetPath, before);

        write(m3.resolve(sourcePath), apache("package p; final class A { int x; }\n"));
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verify(synexia, m3jdk, manifest));

        write(m3.resolve(sourcePath), before);
        write(m3jdk.resolve(targetPath), apache("package p; final class A { int y; }\n"));
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verifyTargetSnapshot(m3jdk, manifest));
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verify(synexia, m3jdk, manifest));
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.materialize(synexia, m3jdk, manifest));
    }

    @Test
    void missingSourceAndUnsafeRootsFailClosed() throws Exception {
        Path synexia = temp.resolve("m3-missing");
        Path m3jdk = temp.resolve("m3jdk-missing");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath = "module/src/main/java/p/A.java";
        String targetPath = "m3/vendor/m3/module/src/main/java/p/A.java";
        byte[] bytes = apache("package p; final class A {}\n");
        M3ImportManifest manifest = manifest(sourcePath, targetPath, bytes);

        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verify(synexia, m3jdk, manifest));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3Importer.verify(temp.resolve("absent"), m3jdk, manifest));
        write(m3.resolve(sourcePath), bytes);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3Importer.verify(synexia, temp.resolve("absent-target"), manifest));
    }

    @Test
    void materializeRollsBackEarlierAddsWhenALaterWriteFails() throws Exception {
        Path synexia = temp.resolve("m3-rollback");
        Path m3jdk = temp.resolve("m3jdk-rollback");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        byte[] a = apache("package p; final class A {}\n");
        byte[] b = apache("package p; final class B {}\n");
        String sourceA = "module/src/main/java/p/A.java";
        String sourceB = "module/src/main/java/p/B.java";
        String targetA = "m3/vendor/m3/module/src/main/java/p/A.java";
        String targetB = "m3/vendor/m3/module/src/main/java/p/B.java";
        write(m3.resolve(sourceA), a);
        write(m3.resolve(sourceB), b);
        M3ImportManifest manifest =
                new M3ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(
                                new M3ImportManifest.Entry(
                                        "seed", sourceA, targetA, sha256(a), "Apache-2.0",
                                        M3ImportManifest.Mode.APACHE_SOURCE),
                                new M3ImportManifest.Entry(
                                        "seed", sourceB, targetB, sha256(b), "Apache-2.0",
                                        M3ImportManifest.Mode.APACHE_SOURCE)),
                        "");

        assertThrows(
                IOException.class,
                () ->
                        M3Importer.materialize(
                                synexia,
                                m3jdk,
                                manifest,
                                (index, entry) -> {
                                    if (index == 1) throw new IOException("injected second-write failure");
                                }));

        assertFalse(Files.exists(m3jdk.resolve(targetA)));
        assertFalse(Files.exists(m3jdk.resolve(targetB)));
        assertFalse(
                Files.exists(m3jdk.resolve("m3")),
                "rollback must remove only the empty directory chain created by this invocation");
    }

    @Test
    void strictCliRequiresManifestRevisionAndCleanTrackedCheckout() throws Exception {
        Path synexia = temp.resolve("m3-git");
        Path m3jdk = temp.resolve("m3jdk-git");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String sourcePath = "module/src/main/java/p/A.java";
        String targetPath = "m3/vendor/m3/module/src/main/java/p/A.java";
        byte[] bytes = apache("package p; final class A {}\n");
        write(m3.resolve(sourcePath), bytes);
        Files.writeString(m3.resolve("README.md"), "clean\n");
        git(synexia, "init");
        git(synexia, "config", "user.email", "m3@example.invalid");
        git(synexia, "config", "user.name", "M3 Test");
        git(synexia, "add", sourcePath, "README.md");
        git(synexia, "commit", "-m", "fixture");
        String head = git(synexia, "rev-parse", "HEAD").trim();

        M3ImportManifest manifest =
                new M3ImportManifest(
                        head,
                        "m3jdk21",
                        List.of(new M3ImportManifest.Entry(
                                "seed",
                                sourcePath,
                                targetPath,
                                sha256(bytes),
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE)),
                        "");
        Path manifestFile = temp.resolve("strict-manifest.tsv");
        Files.writeString(manifestFile, manifest.toTsv());

        M3ImportCli.main(
                new String[] {
                    "verify-strict",
                    manifestFile.toString(),
                    m3.toString(),
                    m3jdk.toString()
                });

        M3ImportManifest wrongRevision =
                new M3ImportManifest(
                        "0".repeat(40), "m3jdk21", manifest.entries(), "");
        Path wrongFile = temp.resolve("wrong-revision.tsv");
        Files.writeString(wrongFile, wrongRevision.toTsv());
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3ImportCli.main(
                                new String[] {
                                    "verify-strict",
                                    wrongFile.toString(),
                                    m3.toString(),
                                    m3jdk.toString()
                                }));

        M3ImportManifest wrongSha256Revision =
                new M3ImportManifest(
                        "0".repeat(64), "m3jdk21", manifest.entries(), "");
        Path wrongSha256File = temp.resolve("wrong-sha256-revision.tsv");
        Files.writeString(wrongSha256File, wrongSha256Revision.toTsv());
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3ImportCli.main(
                                new String[] {
                                    "verify-strict",
                                    wrongSha256File.toString(),
                                    m3.toString(),
                                    m3jdk.toString()
                                }));

        Files.writeString(m3.resolve("README.md"), "tracked drift\n");
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3ImportCli.main(
                                new String[] {
                                    "materialize-strict",
                                    manifestFile.toString(),
                                    m3.toString(),
                                    m3jdk.toString()
                                }));
        assertFalse(Files.exists(m3jdk.resolve(targetPath)));

        git(synexia, "checkout", "--", "README.md");
        String untrackedSource = "module/src/main/java/p/B.java";
        String untrackedTarget = "m3/vendor/m3/" + untrackedSource;
        write(
                m3.resolve(untrackedSource),
                apache("package p; final class B {}\n"));
        M3ImportManifest untrackedManifest =
                new M3ImportManifest(
                        head,
                        "m3jdk21",
                        List.of(new M3ImportManifest.Entry(
                                "seed",
                                untrackedSource,
                                untrackedTarget,
                                "2140213c926ce6d65e25fe23c8fb82f7d75906172aaebee0a1aa8cf3d133c57a",
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE)),
                        "");
        Path untrackedFile = temp.resolve("untracked-source.tsv");
        Files.writeString(untrackedFile, untrackedManifest.toTsv());
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3ImportCli.main(
                                new String[] {
                                    "verify-strict",
                                    untrackedFile.toString(),
                                    m3.toString(),
                                    m3jdk.toString()
                                }));
        assertFalse(Files.exists(m3jdk.resolve(untrackedTarget)));

        String wildcardSource = "module/src/main/java/p/[AB].java";
        String wildcardTarget = "m3/vendor/m3/" + wildcardSource;
        write(
                m3.resolve(wildcardSource),
                apache("package p; final class Bracket {}\n"));
        M3ImportManifest wildcardManifest =
                new M3ImportManifest(
                        head,
                        "m3jdk21",
                        List.of(new M3ImportManifest.Entry(
                                "seed",
                                wildcardSource,
                                wildcardTarget,
                                "9c2c5f1bfda66d7c0c69c3cbb3bddbfc029e0c838ca26e662799903e5976db78",
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE)),
                        "");
        Path wildcardFile = temp.resolve("wildcard-source.tsv");
        Files.writeString(wildcardFile, wildcardManifest.toTsv());
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3ImportCli.main(
                                new String[] {
                                    "verify-strict",
                                    wildcardFile.toString(),
                                    m3.toString(),
                                    m3jdk.toString()
                                }));
        assertFalse(Files.exists(m3jdk.resolve(wildcardTarget)));
    }

    @Test
    void intermediateSymlinksFailClosedWhenSupported() throws Exception {
        Path synexia = temp.resolve("m3-symlink");
        Path m3jdk = temp.resolve("m3jdk-symlink");
        Path outside = temp.resolve("outside");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);
        Files.createDirectories(outside.resolve("src"));
        byte[] bytes = apache("package p; final class A {}\n");
        write(outside.resolve("src/A.java"), bytes);

        Path link = m3.resolve("module");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException unsupported) {
            return;
        }

        M3ImportManifest manifest =
                manifest(
                        "module/src/A.java",
                        "m3/vendor/m3/module/src/A.java",
                        bytes);
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verifySources(synexia, manifest));
    }

    private static String git(Path root, String... args) throws Exception {
        String[] command = new String[args.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = root.toString();
        System.arraycopy(args, 0, command, 3, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        return output;
    }

    @Test
    void manifestRoundTripAndValidationAreDeterministic() {
        byte[] bytes = apache("package p; final class A {}\n");
        M3ImportManifest original = manifest(
                "module/src/main/java/p/A.java",
                "m3/vendor/m3/module/src/main/java/p/A.java",
                bytes);

        M3ImportManifest parsed = M3ImportManifest.parse(original.toTsv());
        assertEquals(original, parsed);
        assertEquals(64, parsed.root().length());
        assertEquals("m3jdk21", parsed.targetId());
        assertEquals(1, parsed.entries().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportManifest.parse("bad\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportManifest.parse(
                        M3ImportManifest.HEADER
                                + "\n"
                                + "a".repeat(40)
                                + "\tm3jdk21\tjava\tA.java\tm3/vendor/m3/A.java\t"
                                + "1".repeat(64)
                                + "\tMIT\tAPACHE_SOURCE\n# root\t"
                                + "2".repeat(64)
                                + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportManifest.Entry(
                        "java",
                        "A.java",
                        "../A.java",
                        "1".repeat(64),
                        "Apache-2.0",
                        M3ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportManifest.Entry(
                        "java",
                        "A.java",
                        "outside/A.java",
                        "1".repeat(64),
                        "Apache-2.0",
                        M3ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportManifest(
                        "a".repeat(40),
                        "other",
                        List.of(new M3ImportManifest.Entry(
                                "java",
                                "A.java",
                                "m3/vendor/m3/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE)),
                        ""));
    }

    @Test
    void producerAndReceiverShareKnownDeliveryRootVector() {
        M3ImportManifest.Entry entry =
                new M3ImportManifest.Entry(
                        "java",
                        "A.java",
                        "m3/vendor/m3/A.java",
                        "1".repeat(64),
                        "Apache-2.0",
                        M3ImportManifest.Mode.APACHE_SOURCE);
        M3ImportManifest manifest =
                new M3ImportManifest(
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
        M3ImportManifest.Entry entry = new M3ImportManifest.Entry(
                "java",
                "A.java",
                "m3/vendor/m3/A.java",
                "1".repeat(64),
                "Apache-2.0",
                M3ImportManifest.Mode.APACHE_SOURCE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportManifest(
                        "a".repeat(40), "m3jdk21", List.of(entry, entry), ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportManifest(
                        "a".repeat(40), "m3jdk21", List.of(entry), "2".repeat(64)));
    }

    @Test
    void cliRejectsUnknownActionAndBadArity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportCli.main(new String[0]));
        assertThrows(
                Exception.class,
                () -> M3ImportCli.main(new String[] {
                    "other",
                    temp.resolve("manifest.tsv").toString(),
                    temp.resolve("synexia").toString(),
                    temp.resolve("m3jdk").toString()
                }));
    }

    private static M3ImportManifest manifest(
            String sourcePath,
            String targetPath,
            byte[] bytes) {
        return new M3ImportManifest(
                "a".repeat(40),
                "m3jdk21",
                List.of(new M3ImportManifest.Entry(
                        "seed",
                        sourcePath,
                        targetPath,
                        sha256(bytes),
                        "Apache-2.0",
                        M3ImportManifest.Mode.APACHE_SOURCE)),
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
