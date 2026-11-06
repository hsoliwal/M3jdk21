// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Real Java 21 annotation-processing, diagnostic and Maven receipt oracle. */
public final class M3CompilerHintEmitterSelfTest {
    private static int checks;
    private static final String SOURCE = """
            package demo;
            import com.m3.collections.sortable.Sortable;
            @Sortable public class Candidate implements Runnable {
                int value;
                public void run() {
                    if (value > 0) {
                        for (int i = 0; i < 2; i++) {
                            tick();
                            synchronized (this) { Object ignored = new Object(); }
                        }
                    }
                }
                private void tick() { value--; }
            }
            """;

    private M3CompilerHintEmitterSelfTest() { }

    public static void main(String[] args) throws Exception {
        try (Run off = compile(null); Run notes = compile("notes");
                Run receipts = compile("receipts"); Run both = compile("both");
                Run invalid = compile("unverified")) {
            check(off.ok && notes.ok && receipts.ok && both.ok, "valid javac modes");
            check(!invalid.ok && invalid.diagnostics.contains("must be off, notes, receipts or both"),
                    "invalid mode fails javac and cannot silently downgrade");
            check(off.companion.equals(notes.companion)
                            && off.companion.equals(receipts.companion)
                            && off.companion.equals(both.companion),
                    "byte-identical preexisting generated companion in every hint mode");
            check(off.receipt == null && notes.receipt == null,
                    "default/notes cannot create unintended Maven receipts");
            check(receipts.receipt != null && receipts.receipt.equals(both.receipt),
                    "per-type receipts deterministic across notes and no-notes modes");
            check(!off.diagnostics.contains("[M3-MOP:CANDIDATE]")
                            && !receipts.diagnostics.contains("[M3-MOP:CANDIDATE]"),
                    "default and receipts-only never change javac notes");
            check(notes.diagnostics.contains("[M3-MOP:CANDIDATE]")
                            && both.diagnostics.contains("[M3-MOP:CANDIDATE]"),
                    "javac actually emits candidate notes for compiled attributed sources");
            check(!notes.diagnostics.contains("warning:")
                            && !both.diagnostics.contains("warning:"),
                    "hint notes do not trigger -Werror");
            String actual = both.receipt;
            check(actual.startsWith(M3CompilerHintEmitter.SCHEMA
                            + "\tSOURCE_TIME_ONLY\tCANDIDATE_ONLY\n"), "versioned review-only contract");
            check(actual.contains("H\t3\tALLOCATION_FACTORY_REVIEW\tCANDIDATE\n"),
                    "compiler-observed construction/factory review");
            check(actual.contains("H\t2\tITERATIVE_INVOCATION\tCANDIDATE\n"),
                    "compiler-observed loop plus invocation review");
            int footer = actual.lastIndexOf("SHA256\t");
            check(footer > 0 && sha256(actual.substring(0, footer))
                            .equals(actual.substring(footer + 7).strip()),
                    "receipt body hash verifies exactly");

            int[] unknown = new int[M3CompilerFacts.COUNT];
            long modelKnown = (1L << M3CompilerFacts.MODEL_FACTS) - 1L;
            var projected = M3MachineHintRules.fromFacts(unknown, new long[] {modelKnown});
            String partial = M3CompilerHintEmitter.receipt("demo.Partial", unknown, modelKnown, projected);
            check(partial.contains("H\t3\tALLOCATION_FACTORY_REVIEW\tUNKNOWN\n"),
                    "unknown syntax cannot become NOT_OBSERVED");
            check(partial.contains("F\t14\tUNKNOWN\n"), "unknown raw compiler fact retained");
            var wrong = new M3MachineHintRules(0L, 0L);
            expectReject(() -> M3CompilerHintEmitter.receipt("demo.Partial", unknown, modelKnown, wrong),
                    "injected hint masks refused");
            expectReject(() -> M3CompilerHintEmitter.receipt(" ", unknown, modelKnown, projected),
                    "empty owner refused");
        }
        System.out.println("M3_COMPILER_HINT_EMITTER_PASS checks=" + checks);
    }

    private static Run compile(String mode) throws IOException {
        Path root = Files.createTempDirectory("m3-compiler-hints-");
        Path source = root.resolve("src/demo/Candidate.java");
        Path classes = root.resolve("classes");
        Path generated = root.resolve("generated");
        Files.createDirectories(source.getParent());
        Files.createDirectories(classes);
        Files.createDirectories(generated);
        Files.writeString(source, SOURCE, StandardCharsets.UTF_8);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java 21 compiler unavailable");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>(List.of(
                "--release", "21", "-Xlint:all", "-Werror",
                "-classpath", System.getProperty("java.class.path"),
                "-processor", SortableProcessor.class.getName(),
                "-s", generated.toString(), "-d", classes.toString()));
        if (mode != null) options.add("-A" + M3CompilerHintEmitter.OPTION + "=" + mode);
        boolean success;
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            success = Boolean.TRUE.equals(compiler.getTask(new StringWriter(), files, diagnostics,
                    options, null, files.getJavaFileObjects(source.toFile())).call());
        }
        StringBuilder reported = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            reported.append(diagnostic.getKind()).append(": ")
                    .append(diagnostic.getMessage(null)).append('\n');
        }
        Path companion = generated.resolve("demo/CandidateM3Sortable.java");
        Path hints = classes.resolve(M3CompilerHintEmitter.RECEIPT_DIRECTORY + "demo/Candidate.tsv");
        return new Run(root, success, Files.exists(companion) ? Files.readString(companion) : "",
                Files.exists(hints) ? Files.readString(hints) : null, reported.toString());
    }

    private record Run(Path root, boolean ok, String companion, String receipt,
                       String diagnostics) implements AutoCloseable {
        @Override public void close() throws IOException {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expectReject(Runnable action, String message) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }
}
