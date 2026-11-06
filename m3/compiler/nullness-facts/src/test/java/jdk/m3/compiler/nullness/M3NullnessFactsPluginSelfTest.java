// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.nullness;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The donor plugin test (the donor crate's qualification/verify.sh), as a Java self-test over the relocated plugin:
 * the real javac of this JDK is invoked on the donor fixture Sample.java with and without {@code -Xplugin:M3NullnessFacts}
 * (the plugin on the processor path with its service descriptor), the two class files per compilation must be
 * byte-identical, the stderr stream must carry exactly three {@code M3_NULLNESS_FACT} rows whose offset, method,
 * positive-edge and negative-edge columns equal the rows the donor plugin produced (fixtures/Sample.facts.expected.tsv,
 * recorded by recipe/differential.sh) and that satisfy the donor's own two row checks, and a second compilation under
 * {@code javac -J-Xint} must repeat the stream byte for byte. Prints one PASS line; fails with an AssertionError.
 */
public final class M3NullnessFactsPluginSelfTest {
    private static final String FIXTURE = "/fixtures/Sample.java";
    private static final String EXPECTED = "/fixtures/Sample.facts.expected.tsv";
    private static final String FACT = "M3_NULLNESS_FACT";
    private static final String PLUGIN = "-Xplugin:M3NullnessFacts";
    private static final List<String> CLASSES = List.of("Sample", "Sibling");
    private static final int EXPECTED_FACTS = 3;
    private static final String SAMPLE_ROW = "sample\t0:NON_NULL,1:NULL";
    private static final String SIBLING_ROW = "sibling\t0:NULL\t0:NON_NULL";
    private static int checks;

    private M3NullnessFactsPluginSelfTest() {
    }

    /**
     * Runs the donor plugin test against the relocated plugin.
     *
     * @param args unused
     * @throws Exception when javac cannot be started or the scratch directory cannot be written
     */
    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-nullness-facts");
        Path fixture = work.resolve("Sample.java");
        Files.write(fixture, resource(FIXTURE));
        List<String> expected = Arrays.stream(new String(resource(EXPECTED), StandardCharsets.UTF_8).split("\n"))
                .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        Path normal = Files.createDirectory(work.resolve("normal"));
        Path instrumented = Files.createDirectory(work.resolve("instrumented"));
        Path repeated = Files.createDirectory(work.resolve("repeated"));
        Path facts = work.resolve("facts.txt");
        Path repeat = work.resolve("repeat.txt");
        String pluginPath = pluginPath();
        javac(List.of(), pluginPath, normal, fixture, work.resolve("normal.txt"));
        javac(List.of(PLUGIN), pluginPath, instrumented, fixture, facts);
        sameClassFiles(normal, instrumented);
        List<String> rows = factRows(facts);
        check(rows.size() == EXPECTED_FACTS, "facts " + rows.size() + " != " + EXPECTED_FACTS + ": " + rows);
        check(rows.stream().anyMatch(row -> row.contains(SAMPLE_ROW)), "sample row missing: " + rows);
        check(rows.stream().anyMatch(row -> row.contains(SIBLING_ROW)), "sibling row missing: " + rows);
        check(expected.size() == EXPECTED_FACTS, "expected fixture rows " + expected.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] columns = rows.get(i).split("\t", -1);
            check(columns.length == 6, "fact columns " + columns.length + ": " + rows.get(i));
            check(columns[0].equals(FACT) && columns[1].endsWith("Sample.java"), "fact head " + rows.get(i));
            String tail = String.join("\t", Arrays.copyOfRange(columns, 2, columns.length));
            check(tail.equals(expected.get(i)), "fact " + i + " [" + tail + "] != donor [" + expected.get(i) + "]");
        }
        javac(List.of("-J-Xint", PLUGIN), pluginPath, repeated, fixture, repeat);
        check(Arrays.equals(Files.readAllBytes(facts), Files.readAllBytes(repeat)), "-J-Xint facts differ");
        sameClassFiles(normal, repeated);
        System.out.println("M3_NULLNESS_FACTS_PLUGIN_SELFTEST PASS immutable_bytecode=" + CLASSES.size()
                + " facts=" + rows.size() + " modes=default,Xint checks=" + checks);
    }

    private static void javac(List<String> leading, String pluginPath, Path out, Path source, Path stderr)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javacLauncher());
        command.addAll(leading);
        command.addAll(List.of("--release", "21", "-Xlint:all", "-Werror", "-proc:none", "-processorpath",
                pluginPath, "-d", out.toString(), source.toString()));
        Process process = new ProcessBuilder(command).redirectErrorStream(false)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(stderr.toFile()).start();
        int exit = process.waitFor();
        check(exit == 0, "javac " + command + " exit " + exit + ": " + Files.readString(stderr));
    }

    private static String javacLauncher() {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path exe = bin.resolve("javac.exe");
        return (Files.exists(exe) ? exe : bin.resolve("javac")).toString();
    }

    private static String pluginPath() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(M3JavaNullnessCompilerPlugin.class.getProtectionDomain()
                .getCodeSource(), "plugin code source").getLocation().toURI()).toString();
    }

    private static void sameClassFiles(Path left, Path right) throws IOException {
        for (String name : CLASSES) {
            byte[] a = Files.readAllBytes(left.resolve(name + ".class"));
            byte[] b = Files.readAllBytes(right.resolve(name + ".class"));
            check(Arrays.equals(a, b), name + ".class differs between " + left + " and " + right);
        }
    }

    private static List<String> factRows(Path stderr) throws IOException {
        try (Stream<String> lines = Files.lines(stderr, StandardCharsets.UTF_8)) {
            return lines.filter(line -> line.startsWith(FACT)).toList();
        }
    }

    private static byte[] resource(String name) {
        try (InputStream in = M3NullnessFactsPluginSelfTest.class.getResourceAsStream(name)) {
            return Objects.requireNonNull(in, name).readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void check(boolean condition, String failure) {
        checks++;
        if (!condition) {
            throw new AssertionError("M3_NULLNESS_FACTS_PLUGIN_SELFTEST FAIL " + failure);
        }
    }
}
