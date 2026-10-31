/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.gradle.internal.m3.ast;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import org.gradle.api.internal.tasks.compile.M3JavacASTProvider;

/**
 * Connects the original Gradle compiler task to its existing public M3AST owner.
 * No compiler, source reader, parser, attribution pass or detached AST is created.
 * The host Gradle invocation retains file-manager and classloader ownership.
 */
public final class M3GradleASTProvider implements M3JavacASTProvider {
    /** Creates the explicitly selected, invocation-local Gradle provider. */
    public M3GradleASTProvider() { }

    @Override
    public M3Compilation prepare(
            Iterable<? extends JavaFileObject> sources,
            Charset encoding,
            List<String> actualOptions,
            StandardJavaFileManager fileManager,
            Supplier<List<Diagnostic<? extends JavaFileObject>>> diagnostics) {
        Objects.requireNonNull(encoding, "encoding");
        Objects.requireNonNull(fileManager, "fileManager");
        Objects.requireNonNull(diagnostics, "diagnostics");
        Objects.requireNonNull(actualOptions, "actualOptions");
        return new Compilation(Objects.requireNonNull(sources, "sources"),
                getClass().getClassLoader());
    }

    /** One host invocation, sharing the host's exact task and callback lifetime. */
    private static final class Compilation implements M3Compilation {
        private Iterable<? extends JavaFileObject> sources;
        private ClassLoader consumerLoader;
        private JavacTask task;
        private M3AST frontend;
        private boolean attached;
        private boolean closed;

        Compilation(Iterable<? extends JavaFileObject> sources, ClassLoader consumerLoader) {
            this.sources = sources;
            this.consumerLoader = consumerLoader;
        }

        @Override
        public Iterable<? extends JavaFileObject> sources() {
            requireOpen();
            return sources;
        }

        @Override
        public JavaCompiler.CompilationTask attach(JavacTask originalTask, IntSupplier errorCount) {
            requireOpen();
            Objects.requireNonNull(errorCount, "errorCount");
            if (attached) {
                throw new IllegalStateException("M3 Gradle provider is already attached");
            }
            task = Objects.requireNonNull(originalTask, "originalTask");
            attached = true;
            try {
                // The compiler has already resolved option operands and overrides.
                // Observe its decision before open() registers a session listener.
                boolean pluginOwnsConsumers = M3AST.isPluginSelected(originalTask);
                // open() returns the compiler-context-owned handle, including a
                // handle already opened by the builtin plugin or another client.
                frontend = M3AST.open(originalTask);
                if (!pluginOwnsConsumers) {
                    for (ServiceLoader.Provider<M3AST.Consumer> selected :
                            ServiceLoader.load(M3AST.Consumer.class, consumerLoader).stream().toList()) {
                        if (selected.type().getClassLoader() != consumerLoader) {
                            throw new IllegalArgumentException("M3 Gradle consumer is outside the explicit provider assets: "
                                    + selected.type().getName());
                        }
                        frontend.addConsumer(selected.get());
                    }
                }
                return new Execution();
            } catch (RuntimeException | Error failure) {
                closeAfterFailure(failure);
                throw failure;
            }
        }

        private void requireOpen() {
            if (closed) {
                throw new IllegalStateException("M3 Gradle compilation is closed");
            }
        }

        private JavacTask original() {
            requireOpen();
            return Objects.requireNonNull(task, "M3 Gradle provider is not attached");
        }

        private void closeAfterFailure(Throwable original) {
            try {
                close();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != original) {
                    original.addSuppressed(cleanup);
                }
            }
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                M3AST session = frontend;
                frontend = null;
                task = null;
                sources = null;
                consumerLoader = null;
                // Gradle closes its own file manager and provider loader. The
                // M3 owner alone closes consumers and releases compiler views.
                if (session != null) {
                    session.close();
                }
            }
        }

        /** Forwards the complete standard task contract to the original task. */
        private final class Execution implements JavaCompiler.CompilationTask {
            @Override
            public void setProcessors(Iterable<? extends Processor> processors) {
                original().setProcessors(processors);
            }

            @Override
            public void setLocale(Locale locale) {
                original().setLocale(locale);
            }

            @Override
            public void addModules(Iterable<String> moduleNames) {
                original().addModules(moduleNames);
            }

            @Override
            public Boolean call() {
                try {
                    Boolean result = original().call();
                    close();
                    return result;
                } catch (RuntimeException | Error failure) {
                    closeAfterFailure(failure);
                    throw failure;
                }
            }
        }
    }
}
