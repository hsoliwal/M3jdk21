/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/* @test
 * @summary Original javac plugin publishes bounded partial facts only for a successful current invocation
 * @modules jdk.compiler/com.sun.source.util jdk.compiler/com.sun.tools.javac.m3
 * @run main M3ASTBuildFactsTransportTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.tools.javac.m3.M3ASTPC;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public class M3ASTBuildFactsTransportTest {
    private static int checks;
    private static Path work;
    private static final String FIRST = "01234567-89ab-4cde-8123-0123456789ab";
    private static final String SECOND = "01234567-89ab-4cde-8123-0123456789ac";

    public static void main(String[] args) throws Exception {
        work = Files.createTempDirectory("m3-facts-transport-");
        successfulForkAndFailurePreservation();
        consumerFailureBeforePublication();
        nonClassCompilation();
        optionRefusal();
        System.out.println("M3_AST_BUILD_FACTS_TRANSPORT checks=" + checks);
    }

    private static void successfulForkAndFailurePreservation() throws Exception {
        String text = "import static java.lang.Integer.MAX_VALUE; class FactsInput { int value() { return MAX_VALUE; } }";
        Path source = work.resolve("FactsInput.java");
        Files.writeString(source, text);
        Path receipt = work.resolve("facts receipt.tsv");
        check(fork("first", source, options(receipt, FIRST)) == 0,
                "actual forked javac discovers the original builtin M3AST plugin");
        byte[] first = Files.readAllBytes(receipt);
        List<String> rows = Files.readAllLines(receipt);
        check(rows.get(0).equals("M3AST_BUILD_FACTS_V1\tPARTIAL\t" + FIRST),
                "receipt identifies the schema, partial coverage and caller nonce");
        check(rows.get(1).equals("OUTPUT\t" + encode(receipt.toAbsolutePath().normalize().toString())),
                "receipt binds the normalized output path including spaces");
        check(rows.contains("TYPE\t0\t" + encode(source.toUri().toASCIIString())
                        + "\t" + text.length() + "\t" + utf16Sha256(text)
                        + "\t" + encode("FactsInput") + "\tfalse"),
                "transport carries exact original source identity and analyzed type");
        check(rows.contains("DECLARE\t0\t" + encode("FactsInput"))
                        && rows.contains("REFERENCE\t0\t" + encode("java.lang.Integer")),
                "receipt includes declarations and the owner of an inlined constant");
        check(rows.contains("SUMMARY\t1\t1\t1\t0\t1\t0\t0\ttrue\ttrue\tfalse\t0"),
                "receipt records actual shared-frontend counters without source rereads");
        check(rows.get(rows.size() - 1).startsWith("END\t1\t1\t"),
                "terminal counts close the complete atomic receipt");
        check(fork("second", source, options(receipt, SECOND)) == 0, "second real compiler invocation succeeds");
        List<String> second = Files.readAllLines(receipt);
        check(second.get(0).equals("M3AST_BUILD_FACTS_V1\tPARTIAL\t" + SECOND)
                        && rows.subList(1, rows.size()).equals(second.subList(1, second.size())),
                "fact ordering is deterministic while invocation identity changes");
        byte[] successful = Files.readAllBytes(receipt);
        check(!Arrays.equals(first, successful), "an old nonce cannot match the new requested invocation");
        Files.writeString(source, "class FactsInput { Missing value; }");
        check(fork("failed", source, options(receipt, UUID.randomUUID().toString())) != 0,
                "compiler failure stays a failed invocation");
        check(Arrays.equals(successful, Files.readAllBytes(receipt)),
                "failed compilation cannot replace a previous receipt");
        Path absent = work.resolve("absent.tsv");
        check(fork("failed-absent", source, options(absent, UUID.randomUUID().toString())) != 0
                        && !Files.exists(absent),
                "failed compilation cannot create a current receipt");
        try (var paths = Files.list(work)) {
            check(paths.noneMatch(path -> path.getFileName().toString().startsWith(".m3ast-facts-")),
                    "no publication staging file remains");
        }
    }

    private static void consumerFailureBeforePublication() throws Exception {
        Path source = work.resolve("Late.java");
        Files.writeString(source, "class Late { }");
        Path receipt = work.resolve("late.tsv");
        byte[] stale = "prior invocation\n".getBytes(StandardCharsets.UTF_8);
        Files.write(receipt, stale);
        var compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "-d", Files.createDirectory(work.resolve("late-classes")).toString()),
                    null, files.getJavaFileObjects(source));
            // Initialize the same real plugin before adding a later consumer.
            new M3ASTPC().init(task, options(receipt, FIRST));
            M3AST session = M3AST.open(task);
            session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) throws IOException {
                    throw new IOException("late consumer fixture");
                }
            });
            boolean refused = false;
            try { task.call(); }
            catch (RuntimeException expected) { refused = true; }
            check(refused && session.summary().consumerFailed(), "later consumer failure is propagated");
            check(Arrays.equals(stale, Files.readAllBytes(receipt)),
                    "publication waits for every consumer, including a later registered one");
        }
    }

    private static void nonClassCompilation() throws Exception {
        Path directory = Files.createDirectory(work.resolve("module-input"));
        Path source = directory.resolve("module-info.java");
        Files.writeString(source, "module facts.fixture { }");
        Path receipt = work.resolve("module-only.tsv");
        check(fork("module-only", source, options(receipt, FIRST)) == 0,
                "module-only compilation is not broken by a class-fact projection");
        check(!Files.exists(receipt), "zero class slices cannot create a new success receipt");
    }

    private static void optionRefusal() throws Exception {
        Path source = work.resolve("Refusal.java");
        Files.writeString(source, "class Refusal { }");
        Path output = work.resolve("refused.tsv");
        String path = "facts64=" + encode(output.toString());
        check(fork("missing-nonce", source, new String[] { path }) != 0 && !Files.exists(output),
                "output path requires a caller-known nonce");
        check(fork("invalid-utf8", source, new String[] { "facts64=ww", "factsid=" + FIRST }) != 0,
                "invalid UTF-8 path is refused instead of replaced");
        check(fork("collision", source, new String[] { path, "factsid=" + FIRST,
                "trace=" + output }) != 0 && !Files.exists(output),
                "facts and trace cannot target the same path");
    }

    private static String[] options(Path output, String invocation) {
        return new String[] { "facts64=" + encode(output.toAbsolutePath().normalize().toString()),
                              "factsid=" + invocation };
    }

    private static int fork(String name, Path source, String[] pluginOptions) throws Exception {
        Path classes = Files.createDirectory(work.resolve(name + "-classes"));
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-m", "jdk.compiler/com.sun.tools.javac.Main", "-proc:none", "-d", classes.toString(),
                "-Xplugin:M3AST " + String.join(" ", pluginOptions), source.toString()));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        for (String variable : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "JDK_JAVAC_OPTIONS",
                "_JAVA_OPTIONS", "CLASSPATH")) builder.environment().remove(variable);
        Path log = work.resolve(name + ".log");
        Process process = builder.redirectOutput(log.toFile()).start();
        return process.waitFor();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String utf16Sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
}
