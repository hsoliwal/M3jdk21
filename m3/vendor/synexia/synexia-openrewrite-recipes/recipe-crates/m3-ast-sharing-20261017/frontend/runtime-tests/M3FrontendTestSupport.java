// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.synexia.mindex.ast.MIndexASTLimits;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Test-only ownership of one real compiler invocation and its temporary output. */
final class M3FrontendTestSupport {
    static final MIndexASTLimits LIMITS = MIndexASTLimits.defaults();

    private M3FrontendTestSupport() { }

    static M3File input(String name, String source) {
        return M3File.of(URI.create("memory:///" + name + ".java"), source);
    }

    static final class Compilation implements AutoCloseable {
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StringWriter messages = new StringWriter();
        final StandardJavaFileManager files;
        final JavacTask task;
        final List<String> options;
        final Path classes;

        Compilation(Path work, List<? extends JavaFileObject> sources) throws IOException {
            this(work, sources, 21, false);
        }

        Compilation(Path work, List<? extends JavaFileObject> sources, int release, boolean processing)
                throws IOException {
            classes = Files.createDirectories(work.resolve("classes"));
            Path generated = Files.createDirectories(work.resolve("generated"));
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("a real JDK compiler is required");
            files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
            List<String> settings = new ArrayList<>(List.of("--release", Integer.toString(release),
                    "-d", classes.toString(), "-s", generated.toString(), "-encoding", "UTF-8"));
            settings.add(processing ? "-proc:full" : "-proc:none");
            options = List.copyOf(settings);
            task = (JavacTask) compiler.getTask(messages, files, diagnostics, options, null, sources);
        }

        boolean call() { return Boolean.TRUE.equals(task.call()); }

        long errors() {
            return diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).count();
        }

        String diagnosticText() { return diagnostics.getDiagnostics() + "\n" + messages; }

        @Override public void close() throws IOException { files.close(); }
    }

    static M3AST capture(Path work, M3File source) throws Exception {
        AtomicReference<M3AST> result = new AtomicReference<>();
        try (var compile = new Compilation(work, List.of(source));
                var index = new M3JavacASTIndex(compile.task,
                        M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            index.on(M3JavacASTIndex.Phase.PARSE, "test-capture", view -> {
                try { result.set(view.ast()); }
                catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
            });
            if (!compile.call()) throw new AssertionError(compile.diagnosticText());
        }
        if (result.get() == null) throw new AssertionError("real compiler delivered no PARSE view");
        return result.get();
    }

    static String rawUtf16Sha256(String source) throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var data = new java.io.DataOutputStream(bytes)) {
            for (int i = 0; i < source.length(); i++) data.writeChar(source.charAt(i));
        }
        return sha256(bytes.toByteArray());
    }

    static String sha256(byte[] source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
    }

    static Observation observe(JavacTask task,
            BiConsumer<CompilationUnitTree, CharSequence> consumer) throws Exception {
        return new Observation((AutoCloseable) bridge("observe",
                new Class<?>[] {JavacTask.class, BiConsumer.class}, task, consumer));
    }

    static final class Observation implements AutoCloseable {
        private final AutoCloseable delegate;
        Observation(AutoCloseable delegate) { this.delegate = delegate; }
        @Override public void close() {
            try { delegate.close(); }
            catch (Exception failure) { throw new IllegalStateException("observer close failed", failure); }
        }
    }

    static int sourceRelease(JavacTask task) throws Exception {
        return (Integer) bridge("sourceRelease", new Class<?>[] {JavacTask.class}, task);
    }

    static int errorCount(JavacTask task) throws Exception {
        return (Integer) bridge("errorCount", new Class<?>[] {JavacTask.class}, task);
    }

    private static Object bridge(String method, Class<?>[] parameters, Object... arguments) throws Exception {
        try {
            Class<?> api = Class.forName("com.sun.tools.javac.m3.M3ASTPC");
            return api.getMethod(method, parameters).invoke(null, arguments);
        } catch (InvocationTargetException invocation) {
            if (invocation.getCause() instanceof Exception failure) throw failure;
            if (invocation.getCause() instanceof Error failure) throw failure;
            throw invocation;
        }
    }

    static boolean causedBy(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) return true;
        }
        return false;
    }
}
