// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Real javac annotation-processing, generated-class, and refusal oracle. */
public final class SortableProcessorSelfTest {
    private static int checks;
    private SortableProcessorSelfTest() { }

    public static void main(String[] args) throws Exception {
        String header = "package demo; import com.m3.collections.sortable.Sortable; "
                + "@Sortable(signal=\"absoluteWeight\") public class Person {"
                + "public int age; public java.util.List<String> tags; "
                + "public double absoluteWeight(){return age*0.5;} ";
        String method = "public String describe(){return String.valueOf(age);}";
        String footer = "}";
        try (Compiled a = compile("demo.Person", header + method + footer, true);
                Compiled b = compile("demo.Person", header.replace("public double absoluteWeight(){return age*0.5;} ", "")
                        + method + "public double absoluteWeight(){return age*0.5;}" + footer, true)) {
            check(a.ok && b.ok, "valid compilations: " + a.messages + b.messages);
            check(a.generated.equals(b.generated), "source order must not change structural metadata");
            Class<?> person = a.loader.loadClass("demo.Person");
            Class<?> projection = a.loader.loadClass("demo.PersonM3Sortable");
            Object p = person.getConstructor().newInstance();
            Field age = person.getField("age");
            age.setInt(p, 17);
            Method signal = projection.getMethod("absoluteSignal", person);
            check(((Double) signal.invoke(null, p)) == 8.5, "exact domain-provided double accessor");
            Object values = java.lang.reflect.Array.newInstance(person, 2);
            java.lang.reflect.Array.set(values, 0, p);
            Object q = person.getConstructor().newInstance();
            age.setInt(q, 9);
            java.lang.reflect.Array.set(values, 1, q);
            Method signals = projection.getMethod("absoluteSignals", values.getClass());
            check(Arrays.equals(new double[]{8.5, 4.5}, (double[]) signals.invoke(null, values)),
                    "precompute each signal from the source array exactly once");
            long[][] lanes = (long[][]) projection.getMethod("structuralSignals").invoke(null);
            check(lanes.length == 6, "six source-time structural dimensions");
            check(lanes[0].length == 1 && lanes[1].length == 1, "package and class tokens");
            check(lanes[2].length >= 2 && lanes[3].length >= 2, "field and method tokens");
            check(lanes[4].length >= 2 && lanes[5].length >= 1, "dependency and resolved-call tokens");
            long[][] fresh = (long[][]) projection.getMethod("structuralSignals").invoke(null);
            check(StructuralSignals.weightedDistance(lanes, fresh, new double[]{1,1,1,1,1,1}) == 0.0,
                    "identical structure has zero projected distance");
            lanes[0][0] ^= 1;
            check(fresh[0][0] != lanes[0][0], "defensive source-time metadata export");
            check(StructuralSignals.weightedDistance(lanes, fresh, new double[]{1,1,1,1,1,1}) > 0.0,
                    "package change becomes distance evidence");
        }
        try (Compiled c = compile("demo.MetaOnly",
                "package demo; import com.m3.collections.sortable.Sortable;"
                + "@Sortable public class MetaOnly {public String name;} ", true)) {
            check(c.ok, "metadata only is accepted: " + c.messages);
            check(!c.generated.contains("absoluteSignal("), "no invented absolute weight");
        }
        String prefix = "package demo; import com.m3.collections.sortable.Sortable;";
        expectFail("demo.Missing", prefix + "@Sortable(signal=\"missing\") public class Missing {}",
                "requires exactly one accessible numeric member");
        expectFail("demo.Private", prefix + "@Sortable(signal=\"weight\") "
                + "public class Private {private double weight;}", "accessible instance member");
        expectFail("demo.TextWeight", prefix + "@Sortable(signal=\"weight\") "
                + "public class TextWeight {public String weight;}", "primitive numeric value");
        expectFail("demo.InterfaceWeight", prefix + "@Sortable public interface InterfaceWeight {}",
                "top-level classes and records only");
        check(StructuralSignals.jaccardDistance(new long[0], new long[0]) == 0.0, "empty metric");
        check(StructuralSignals.jaccardDistance(new long[]{1L}, new long[]{2L}) == 1.0,
                "disjoint metric");
        expectInvalid(() -> StructuralSignals.jaccardDistance(new long[]{2,1}, new long[]{1}),
                "unsorted input rejected");
        expectInvalid(() -> StructuralSignals.weightedDistance(new long[6][0], new long[6][0],
                new double[]{0,0,0,0,0,0}), "all-zero weights rejected");
        System.out.println("PASS SortableProcessorSelfTest checks=" + checks);
    }

    private static void expectInvalid(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }

    private static void expectFail(String type, String source, String expected) throws Exception {
        try (Compiled failure = compile(type, source, false)) {
            check(!failure.ok && failure.messages.contains(expected), expected + ": " + failure.messages);
        }
    }

    private static Compiled compile(String type, String code, boolean expectedSuccess) throws Exception {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) throw new IllegalStateException("JDK javac required");
        Path root = Files.createTempDirectory("m3-sortable-javac-");
        Path source = root.resolve("src/" + type.replace('.', '/') + ".java");
        Path generated = root.resolve("generated");
        Path classes = root.resolve("classes");
        Files.createDirectories(source.getParent());
        Files.createDirectories(generated);
        Files.createDirectories(classes);
        Files.writeString(source, code, StandardCharsets.UTF_8);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean ok;
        try (StandardJavaFileManager files = javac.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            String cp = System.getProperty("java.class.path");
            List<String> flags = List.of("--release", "21", "-Xlint:all", "-Werror",
                    "-classpath", cp, "-processorpath", cp,
                    "-processor", SortableProcessor.class.getName(),
                    "-s", generated.toString(), "-d", classes.toString());
            ok = Boolean.TRUE.equals(javac.getTask(null, files, diagnostics, flags, null,
                    files.getJavaFileObjects(source.toFile())).call());
        }
        StringBuilder messages = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            messages.append(d.getKind()).append(':').append(d.getMessage(null)).append('\n');
        }
        if (expectedSuccess && !ok) throw new AssertionError(messages.toString());
        Path gen = generated.resolve(type.replace('.', '/') + "M3Sortable.java");
        String generatedText = Files.exists(gen) ? Files.readString(gen) : "";
        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                SortableProcessorSelfTest.class.getClassLoader());
        return new Compiled(root, loader, ok, generatedText, messages.toString());
    }

    private record Compiled(Path root, URLClassLoader loader, boolean ok,
            String generated, String messages) implements AutoCloseable {
        @Override public void close() throws IOException {
            loader.close();
            try (var stream = Files.walk(root)) {
                for (Path file : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void check(boolean pass, String label) {
        checks++;
        if (!pass) throw new AssertionError(label);
    }
}
