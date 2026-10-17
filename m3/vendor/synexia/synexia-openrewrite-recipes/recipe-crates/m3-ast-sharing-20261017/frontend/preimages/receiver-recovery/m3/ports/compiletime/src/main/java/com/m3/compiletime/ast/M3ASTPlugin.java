// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.synexia.mindex.compiler.ast.MIndexJavaAST;
import com.synexia.mindex.compiler.ast.MIndexASTDocument;
import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.synexia.mindex.ast.MIndexASTLimits;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Optional build-tool adapter: {@code -Xplugin:"M3AST out=/absolute/path"}. */
public final class M3ASTPlugin implements Plugin {
    @Override public String getName() { return "M3AST"; }

    @Override public void init(JavacTask task, String... args) {
        if (args.length != 1 || !args[0].startsWith("out=") || args[0].length() == 4) {
            throw new IllegalArgumentException("M3AST requires exactly out=/absolute/output-directory");
        }
        Path output = Path.of(args[0].substring(4));
        if (!output.isAbsolute()) throw new IllegalArgumentException("M3AST output must be absolute");
        int release = sourceRelease(task);
        try {
            Files.createDirectories(output);
            Path session = Files.createTempDirectory(output, "compilation-");
            M3CompilationContext context = M3CompilationContext.isolated(List.of(args));
            Map<CompilationUnitTree, String> inputs = new IdentityHashMap<>();
            AutoCloseable observer = observe(task, (unit, source) -> inputs.put(unit, source.toString()));
            try {
            MIndexASTLimits limits = MIndexASTLimits.defaults();
            M3ASTCodec codec = new M3ASTCodec(limits);
            M3JavacASTIndex index = new M3JavacASTIndex(task, context, unit -> {
                String exact = inputs.remove(unit);
                if (exact != null) return exact;
                throw new IOException("owning compiler did not publish exact parser input");
            }, limits, release);
            boolean installed = false;
            try {
            index.on(M3JavacASTIndex.Phase.PARSE, "immutable-syntax-export", view -> {
                try {
                    M3FileSourceFacts facts = view.facts("javac-public-tree", 1);
                    String id = M3FileSourceFacts.contextSha256(List.of(facts.sourceUri(), facts.sourceSha256()));
                    Files.write(session.resolve(id + ".m3ast"), codec.encode(view.ast()),
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
            });
            task.addTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() != TaskEvent.Kind.COMPILATION) return;
                    try {
                        M3JavacASTIndex.Statistics statistics = index.statistics();
                        String receipt = "{\n  \"schema\": \"M3_AST_BUILD_RECEIPT_V1\",\n"
                                + "  \"contextSha256\": \"" + context.sha256() + "\",\n"
                                + "  \"crossProcessSemanticReuse\": false,\n"
                                + "  \"parseEvents\": " + statistics.parseEvents() + ",\n"
                                + "  \"analyzeEvents\": " + statistics.analyzeEvents() + ",\n"
                                + "  \"syntaxCaptures\": " + statistics.syntaxCaptures() + ",\n"
                                + "  \"compilerErrors\": " + errors(task) + "\n}\n";
                        Files.writeString(session.resolve("receipt.json"), receipt, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    } catch (IOException failure) { throw new UncheckedIOException(failure); }
                    finally {
                        index.close();
                        inputs.clear();
                        closeObserver(observer);
                        task.removeTaskListener(this);
                    }
                }
            });
            installed = true;
            } finally {
                if (!installed) index.close();
            }
            } catch (RuntimeException | Error failure) {
                inputs.clear();
                try { closeObserver(observer); }
                catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }

    private static AutoCloseable observe(JavacTask task,
            BiConsumer<CompilationUnitTree, CharSequence> consumer) {
        try {
            Class<?> bridge = Class.forName("com.sun.tools.javac.m3.M3ASTPC", false,
                    task.getClass().getClassLoader());
            Method method = bridge.getMethod("observe", JavacTask.class, BiConsumer.class);
            return (AutoCloseable) method.invoke(null, task, consumer);
        } catch (ClassNotFoundException unavailable) {
            throw new IllegalStateException("M3AST requires the qualified M3 compiler bridge");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("M3AST requires the qualified jdk.compiler M3 export", unwrap(failure));
        }
    }

    private static int sourceRelease(JavacTask task) {
        try {
            Class<?> bridge = Class.forName("com.sun.tools.javac.m3.M3ASTPC", false,
                    task.getClass().getClassLoader());
            return (Integer) bridge.getMethod("sourceRelease", JavacTask.class).invoke(null, task);
        } catch (ClassNotFoundException unavailable) {
            throw new IllegalStateException("plugin source release requires the qualified M3 compiler bridge");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("cannot obtain compiler source release", unwrap(failure));
        }
    }

    private static int errors(JavacTask task) {
        try {
            Class<?> bridge = Class.forName("com.sun.tools.javac.m3.M3ASTPC", false,
                    task.getClass().getClassLoader());
            return (Integer) bridge.getMethod("errorCount", JavacTask.class).invoke(null, task);
        } catch (ClassNotFoundException unavailable) {
            return -1; // Unknown is never reported as successful compilation.
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("cannot obtain compiler error count", unwrap(failure));
        }
    }

    private static Throwable unwrap(ReflectiveOperationException failure) {
        return failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
    }

    private static void closeObserver(AutoCloseable observer) {
        try { observer.close(); }
        catch (Exception failure) { throw new IllegalStateException("cannot release compiler observer", failure); }
    }
}
