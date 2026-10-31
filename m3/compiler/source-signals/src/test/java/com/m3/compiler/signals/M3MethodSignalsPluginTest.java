/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.compiler.signals;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/** Use the already-registered original javac Plugin ServiceLoader and compare both artifacts. */
final class M3MethodSignalsPluginTest {
    private static final String SOURCE = """
            package sample;
            class Alpha {
                static int twice(int number) { return number * 2; }
                int compute(int value) { if (value > 0) return twice(value) + new Beta().get(); return 0; }
                String codeLike = "fakeCall(new RuntimeException())";
            }
            class Beta { int get() { return 3; } }
            """;

    @Test
    void unchangedV1AndOptionalMethodFactsFromSameCompilerTree() throws Exception {
        Path workspace = Files.createTempDirectory("m3-method-plugin-");
        try {
            Path sample = workspace.resolve("sample/Alpha.java");
            Files.createDirectories(sample.getParent());
            Files.writeString(sample, SOURCE);
            Path baseline = workspace.resolve("old/type.tsv");
            Path newType = workspace.resolve("new/type.tsv");
            Path method = workspace.resolve("new/method.tsv");
            compile(sample, baseline, null);
            compile(sample, newType, method);
            assertArrayEquals(Files.readAllBytes(baseline), Files.readAllBytes(newType),
                    "opt-in method output must not modify any original S/T/E byte");
            String facts = Files.readString(method, StandardCharsets.UTF_8);
            assertTrue(facts.startsWith("# M3JDK21-METHOD-SIGNALS/1\n"));
            assertTrue(facts.contains("M\tsample.Alpha#compute(int;):int\t"));
            assertTrue(facts.contains("call:DIRECT:sample.Alpha#twice(int;):int"));
            assertTrue(facts.contains("call:DISPATCH_CANDIDATE:sample.Beta#get():int"));
            assertFalse(facts.contains("fakeCall"));
            validateDigest(facts);
            compile(sample, newType, method);
            assertEquals(facts, Files.readString(method), "second compiler run must be idempotent");
            assertThrows(IllegalArgumentException.class, () -> compile(sample, newType, newType),
                    "original type and optional method outputs must never alias");
        } finally {
            try (var stream = Files.walk(workspace)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void compile(Path source, Path type, Path method) throws IOException {
        Files.createDirectories(type.getParent());
        Path classes = type.getParent().resolve("classes");
        Files.createDirectories(classes);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        Path plugin = Path.of(M3SourceSignalPlugin.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());
        try (var fm = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            String option = "-Xplugin:M3SourceSignals out=" + type
                    + (method == null ? "" : " methodOut=" + method) + " include=all";
            boolean success = compiler.getTask(null, fm, diagnostics, List.of(
                    "--release", "21", "-proc:none", "-processorpath", plugin.toString(),
                    option, "-d", classes.toString()),
                    null, fm.getJavaFileObjects(source.toFile())).call();
            if (!success) throw new AssertionError("real javac plugin failure: " + diagnostics.getDiagnostics());
        }
    }

    private static void validateDigest(String source) throws Exception {
        int start = source.lastIndexOf("# sha256\t");
        assertTrue(start > 0);
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.substring(0, start).getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, source.substring(start + 9).trim());
    }
}
