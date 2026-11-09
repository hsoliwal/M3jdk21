/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.cds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Executable JDK21 fixture, differential runtime oracle, and negative-replay tests. */
public final class CdsTrainReplaySelfTest {
    private static int checks;

    private CdsTrainReplaySelfTest() { }

    /**
     * Compiles a real JAR and runs baseline/training/archive-replay against a real HotSpot JDK21.
     *
     * @param args one optional existing parent directory for isolated test outputs
     * @throws Exception on a failed source, compiler, archive, or runtime check
     */
    public static void main(String[] args) throws Exception {
        Path parent = args.length == 0 ? Path.of(".").toAbsolutePath() : Path.of(args[0]);
        if (!Files.isDirectory(parent)) throw new IllegalArgumentException("test parent must exist");
        Path root = Files.createTempDirectory(parent, "m3cds-selftest-");
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Path source = root.resolve("Main.java");
        Path build = Files.createDirectory(root.resolve("build"));
        String code = "package demo; public final class Main {"
                + " public static void main(String[] args) {"
                + " if(args.length > 0 && args[0].equals(\"FLAGS\")) {"
                + " System.out.print(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());"
                + " } else { byte[] output = (\"M3CDS:\" + java.util.Arrays.toString(args))"
                + ".getBytes(java.nio.charset.StandardCharsets.UTF_8);"
                + " System.out.write(output, 0, output.length); }"
                + " } }";
        Files.writeString(source, code, StandardCharsets.UTF_8);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "JDK21 compiler required");
        check(compiler.run(null, null, null, "--release", "21", "-Xlint:all", "-Werror", "-d",
                build.toString(), source.toString()) == 0, "compile fixture");
        Path jar = root.resolve("application.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            JarEntry entry = new JarEntry("demo/Main.class");
            entry.setTime(0L);
            out.putNextEntry(entry);
            Files.copy(build.resolve("demo/Main.class"), out);
            out.closeEntry();
        }
        check(Files.isRegularFile(jar), "fixture JAR materialized");

        List<String> params = List.of("M3", "", "with space", "संस्कृत", "\t-tab");
        CdsIdentity.Plan input = CdsIdentity.inspect(java, jar, "demo.Main", params);
        check(input.arguments().equals(params), "exact argument vector");
        check(input.fields().size() >= 13, "JDK image and source hashes recorded");
        check(input.fields().get("arg_000001").equals("~"), "canonical empty argument");
        check(CdsIdentity.recheck(input.fields()).fields().equals(input.fields()), "source fixed point");
        check(CdsIdentity.table(input.fields()).equals(CdsIdentity.table(input.fields())), "stable ordering");
        expect(IllegalArgumentException.class, () -> CdsIdentity.inspect(java, build, "demo.Main", params),
                "reject classpath directory");
        expect(IllegalArgumentException.class, () -> CdsIdentity.inspect(java, jar, "-agent", params),
                "reject invalid entrypoint");
        expect(IOException.class, () -> CdsIdentity.inspect(java, root.resolve("missing.jar"),
                "demo.Main", params), "missing JAR refusal");
        expect(IllegalArgumentException.class, () -> CdsIdentity.inspect(java, jar, "demo.Main",
                Arrays.asList((String) null)), "null argument refusal");

        var proof = CdsTrainReplay.train(java, jar, "demo.Main", root.resolve("qualified"), params);
        Path qualified = proof.directory();
        check(Files.isRegularFile(qualified.resolve("application.jsa")), "dynamic archive produced");
        check(Files.size(qualified.resolve("application.jsa")) > 0, "archive nonempty");
        check(Files.readString(qualified.resolve("baseline.stdout")).contains("संस्कृत"),
                "Unicode roundtrip");
        check(Files.mismatch(qualified.resolve("baseline.stdout"), qualified.resolve("training.stdout")) == -1,
                "training observable parity");
        check(Files.mismatch(qualified.resolve("baseline.stdout"), qualified.resolve("deployment.stdout")) == -1,
                "deployment observable parity");
        check(Files.mismatch(qualified.resolve("baseline.stderr"), qualified.resolve("deployment.stderr")) == -1,
                "stderr observable parity");
        check(Files.readString(qualified.resolve("classload.log")).contains("demo.Main source: shared objects file (top)"),
                "named app main shared from top archive");
        check(proof.archiveSha256().equals(CdsIdentity.sha(qualified.resolve("application.jsa"))),
                "archive SHA seal");
        Map<String, String> receipt = CdsIdentity.parse(qualified.resolve("RECEIPT.tsv"));
        check(receipt.get("status").equals("PASS_BOUNDED_HOST_JDK21"), "bounded status text");
        check(receipt.get("archive_sha256").equals(proof.archiveSha256()), "receipt archive identity");
        byte[] originalReceipt = Files.readAllBytes(qualified.resolve("RECEIPT.tsv"));
        check(CdsTrainReplay.verify(qualified).archiveSha256().equals(proof.archiveSha256()),
                "first replay exact archive");
        check(CdsTrainReplay.verify(qualified).planSha256().equals(proof.planSha256()),
                "second replay exact plan");
        check(Arrays.equals(originalReceipt, Files.readAllBytes(qualified.resolve("RECEIPT.tsv"))),
                "receipt replay fixed point");
        check(Files.mismatch(qualified.resolve("baseline.stdout"), qualified.resolve("replay.stdout")) == -1,
                "replay stdout parity");
        check(Files.mismatch(qualified.resolve("baseline.stderr"), qualified.resolve("replay.stderr")) == -1,
                "replay stderr parity");
        expect(IllegalArgumentException.class,
                () -> CdsTrainReplay.train(java, jar, "demo.Main", qualified, params),
                "occupied output refusal");

        // Each refusal changes exactly one artifact, then restores the original bytes.
        byte[] archived = Files.readAllBytes(qualified.resolve("application.jsa"));
        Files.write(qualified.resolve("application.jsa"), new byte[]{1, 2, 3, 4});
        expect(IllegalArgumentException.class, () -> CdsTrainReplay.verify(qualified),
                "corrupt archive refused");
        Files.write(qualified.resolve("application.jsa"), archived);
        byte[] originalJar = Files.readAllBytes(jar);
        java.nio.file.attribute.FileTime originalJarTimestamp = Files.getLastModifiedTime(jar);
        Files.write(jar, new byte[]{5, 6, 7});
        expect(IllegalArgumentException.class, () -> CdsTrainReplay.verify(qualified),
                "jar source drift refusal");
        Files.write(jar, originalJar);
        expect(IllegalArgumentException.class, () -> CdsTrainReplay.verify(qualified),
                "JAR metadata drift even with same bytes");
        Files.setLastModifiedTime(jar, originalJarTimestamp);
        Path stderr = qualified.resolve("baseline.stderr");
        byte[] originalStderr = Files.readAllBytes(stderr);
        Files.writeString(stderr, "tampered\n");
        expect(IllegalArgumentException.class, () -> CdsTrainReplay.verify(qualified),
                "retained log integrity refusal");
        Files.write(stderr, originalStderr);
        Path receiptPath = qualified.resolve("RECEIPT.tsv");
        Files.writeString(receiptPath, "key\tvalue\nstatus\tPASS_BOUNDED_HOST_JDK21\n");
        expect(IllegalArgumentException.class, () -> CdsTrainReplay.verify(qualified),
                "forged incomplete receipt refused");
        Files.write(receiptPath, originalReceipt);
        check(CdsTrainReplay.verify(qualified).archiveSha256().equals(proof.archiveSha256()),
                "all restored evidence still verifies");

        expect(IllegalStateException.class,
                () -> CdsTrainReplay.train(java, jar, "demo.Main", root.resolve("changed-visible-flags"),
                        List.of("FLAGS")), "observable runtime-option change rejected");
        check(!Files.exists(root.resolve("changed-visible-flags/RECEIPT.tsv")),
                "failed training has no successful receipt");

        var simple = CdsTrainReplay.train(java, jar, "demo.Main", root.resolve("second"), List.of());
        check(simple.archiveSha256().length() == 64, "zero arguments work");
        check(CdsTrainReplay.verify(simple.directory()).archiveSha256().equals(simple.archiveSha256()),
                "zero-argument replay");
        System.out.println("M3_CDS_SELF_TEST\tPASS\tchecks=" + checks);
    }

    private static void check(boolean value, String title) {
        if (!value) throw new AssertionError("FAILED: " + title);
        checks++;
    }

    private static void expect(Class<? extends Throwable> target, Throwing operation, String title)
            throws Exception {
        try {
            operation.run();
        } catch (Throwable exception) {
            if (target.isInstance(exception)) {
                checks++;
                return;
            }
            throw new AssertionError("wrong refusal type for " + title + ": " + exception, exception);
        }
        throw new AssertionError("missing refusal: " + title);
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
