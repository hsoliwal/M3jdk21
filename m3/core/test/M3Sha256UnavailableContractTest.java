/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3CorpusProjectionDigest;
import jdk.m3.text.M3CorpusSourceReceipt;
import jdk.m3.text.M3LangDexSourceEnvelope;
import jdk.m3.text.M3LanguageCorpusManifest;
import jdk.m3.text.SharedLexiconFamilySidecarCatalog;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Integrity owners fail closed when the platform offers no SHA-256 provider.
 *
 * <p>The parent JVM starts one child JVM whose {@code java.security} file is replaced by a
 * provider-free configuration (a real deployment misconfiguration, injected without reflection).
 * The child proves that digest framing, corpus manifest identity and LangDex intake refuse to run
 * instead of skipping their SHA-256 checks; a well-formed family sidecar bundle is refused before
 * its first family file is trusted. The child inherits JAVA_TOOL_OPTIONS, the module path and the
 * class path of the parent.
 */
public final class M3Sha256UnavailableContractTest {
    private static final String CHILD = "child";
    private static int checks;

    private M3Sha256UnavailableContractTest() {}

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)
                    && (reason == null || reason.equals(thrown.getMessage()))
                    && thrown.getCause() instanceof NoSuchAlgorithmException) {
                return;
            }
            throw new AssertionError(
                    "expected " + type.getSimpleName() + " caused by missing SHA-256", thrown);
        }
        throw new AssertionError("accepted without SHA-256: " + reason);
    }

    /** Minimal well-formed family index; every family file exists and is empty. */
    private static void familyBundle(Path dir) throws java.io.IOException {
        String[][] families = {
            {"prefix-counts", "m3.prefix-counts.tsv"}, {"spell-index", "m3.spell.tsv"},
            {"token-frequency", "m3.token-frequency.tsv"},
            {"token-hash-precompute", "m3.token-hash.tsv"},
            {"translation-projection", "m3.translation.tsv"}
        };
        StringBuilder index = new StringBuilder("schema_version\tfamily\tfile\trows\tsha256\n");
        for (String[] family : families) {
            index.append(SharedLexiconFamilySidecarCatalog.SCHEMA_VERSION)
                    .append('\t')
                    .append(family[0])
                    .append('\t')
                    .append(family[1])
                    .append("\t0\t")
                    .append("e".repeat(64))
                    .append('\n');
            Files.write(dir.resolve(family[1]), new byte[0]);
        }
        Files.writeString(
                dir.resolve(SharedLexiconFamilySidecarCatalog.INDEX_FILE),
                index,
                StandardCharsets.US_ASCII);
    }

    private static void child(String familyDirectory) throws Exception {
        check(Security.getProviders().length == 0, "provider-free platform was injected");
        refused(AssertionError.class, null, M3CorpusProjectionDigest::new);
        refused(AssertionError.class, null, () -> M3CorpusProjectionDigest.bytes(new byte[] {1}));
        String sha = "a".repeat(64);
        var artifact =
                new M3LanguageCorpusManifest.Artifact(
                        M3LanguageCorpusManifest.Plane.DICTIONARY,
                        "hi",
                        "fixture",
                        "word",
                        "word-v1",
                        "nfc-v1",
                        1L,
                        2L,
                        sha,
                        sha,
                        new M3CorpusSourceReceipt(
                                "FIXTURE", URI.create("https://example.org/data"), "1", "cc0", sha),
                        "synthetic",
                        URI.create("https://example.org/terms"),
                        0,
                        "2026");
        refused(
                IllegalStateException.class,
                "SHA-256 unavailable",
                () -> M3LanguageCorpusManifest.of(List.of(artifact)));
        var receipt = new M3LangDexSourceEnvelope.Receipt("0".repeat(40), 0, 0, sha);
        refused(
                AssertionError.class,
                "SHA-256 is required by the Java platform",
                () ->
                        M3LangDexSourceEnvelope.verify(
                                new ByteArrayInputStream(new byte[0]), receipt));
        // The parent owns the directory: temporary names need SecureRandom, which needs SHA-1.
        Path bundle = Files.createDirectories(Path.of(familyDirectory));
        familyBundle(bundle);
        refused(AssertionError.class, null, () -> SharedLexiconFamilySidecarCatalog.open(bundle));
        System.out.println("M3_SHA256_UNAVAILABLE_CHILD_PASS checks=" + checks);
    }

    private static void parent() throws Exception {
        Path dir = Files.createTempDirectory("m3-no-sha256-");
        Path security = dir.resolve("provider-free.security");
        Path family = dir.resolve("family");
        try {
            Files.writeString(
                    security,
                    "# no security.provider.N entries: no MessageDigest SHA-256\n",
                    StandardCharsets.US_ASCII);
            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            command.add("-ea");
            command.add("-esa");
            command.add("-Djava.security.properties==" + security.toAbsolutePath());
            String modules = System.getProperty("jdk.module.path");
            if (modules != null) {
                command.add("--module-path");
                command.add(modules);
            }
            command.add("--add-modules");
            command.add("jdk.m3.text");
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(M3Sha256UnavailableContractTest.class.getName());
            command.add(CHILD);
            command.add(family.toString());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("provider-free child did not exit");
            }
            System.out.print(output);
            check(process.exitValue() == 0, "provider-free child exit status");
            check(
                    output.contains("M3_SHA256_UNAVAILABLE_CHILD_PASS checks=6"),
                    "child refusal proof");
        } finally {
            if (Files.isDirectory(family)) {
                try (var files = Files.list(family)) {
                    for (Path file : files.toList()) Files.delete(file);
                }
                Files.delete(family);
            }
            Files.deleteIfExists(security);
            Files.delete(dir);
        }
        System.out.println("M3_SHA256_UNAVAILABLE_PASS checks=" + checks);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && CHILD.equals(args[0])) child(args[1]);
        else parent();
    }
}
