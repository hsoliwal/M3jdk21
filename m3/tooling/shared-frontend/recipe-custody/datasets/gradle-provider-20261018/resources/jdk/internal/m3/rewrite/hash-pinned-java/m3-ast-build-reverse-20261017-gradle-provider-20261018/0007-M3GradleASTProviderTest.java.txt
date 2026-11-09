/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.gradle.internal.m3.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.gradle.api.internal.tasks.compile.M3JavacASTProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Behavioral qualification against the real compiler, actual SPI and discovered consumers. */
final class M3GradleASTProviderTest {
    @TempDir Path output;

    @Test
    void twoConsumersShareOriginalTreesAndOneProjectionPerParse() throws Exception {
        var ordinary = compile("ordinary", false, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
        assertEquals(0, selected.stringConcatenations(), "provider keeps the compiler's default folding policy");
    }

    @Test
    void longerPluginNameDoesNotSuppressProviderConsumers() throws Exception {
        List<String> options = List.of("-Xplugin:M3ASTIndexProbe");
        var ordinary = compile("ordinary", false, options, M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, options, M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
    }

    @Test
    void builtinPluginAndProviderDiscoverConsumersOnce() throws Exception {
        var ordinary = compile("ordinary", false, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, List.of("-Xplugin:M3AST"),
                M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
    }

    @ParameterizedTest
    @ValueSource(chars = { ' ', '\t', '\n', 11, '\f', '\r' })
    void builtinPluginArgumentsUseJavacsExactWhitespace(char delimiter) throws Exception {
        List<String> options = List.of("-Xplugin:M3AST" + delimiter + "lossless");
        var ordinary = compile("ordinary", false, options,
                M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, options,
                M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
        assertEquals(2, ordinary.instances(), "the ordinary plugin also discovers each consumer once");
        assertEquals(1, ordinary.stringConcatenations());
        assertEquals(ordinary.stringConcatenations(), selected.stringConcatenations(),
                "only the explicit lossless plugin option retains the string concatenation");
    }

    @Test
    void programmaticNulSeparatedPluginNamesKeepOneDiscoveryOwner() throws Exception {
        List<String> options = List.of("-Xplugin:M3ASTIndexProbe" + (char) 0 + "M3AST\tlossless");
        var ordinary = compile("ordinary", false, options, M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, options, M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
        assertEquals(2, ordinary.instances());
        assertEquals(1, selected.stringConcatenations());
    }

    @Test
    void optionLookingClasspathOperandDoesNotSuppressProviderConsumers() throws Exception {
        List<String> options = List.of("--class-path", "-Xplugin:M3AST");
        var ordinary = compile("ordinary", false, options, M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, options, M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
        assertEquals(0, ordinary.instances(), "the original compiler has no builtin plugin selection");
    }

    @Test
    void compilerOptionTableEnablingBuiltinPluginKeepsOneDiscoveryOwner() throws Exception {
        List<String> options = List.of("-XD-Xplugin:=M3AST");
        var ordinary = compile("ordinary", false, options, M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, options, M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
        assertEquals(2, ordinary.instances(), "the original effective option table selects the builtin plugin");
    }

    @Test
    void laterCompilerOptionOverrideKeepsProviderConsumersWhenBuiltinIsRemoved() throws Exception {
        List<String> options = List.of("-Xplugin:M3AST", "-XD-Xplugin:=M3ASTIndexProbe");
        var ordinary = compile("ordinary", false, options, M3GradleASTProviderProbe.SOURCE, false, false);
        var selected = compile("selected", true, options, M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, selected);
        assertEquals(0, ordinary.instances(), "the later original option-table value removes builtin selection");
    }

    @Test
    void compilerErrorsKeepOriginalDiagnosticsAndCloseBothConsumers() throws Exception {
        String broken = "public class Sample { void broken() { int value = ; } }";
        var ordinary = compile("ordinary", false, List.of(), broken, false, false);
        var selected = compile("selected", true, List.of(), broken, false, false);
        assertFalse(ordinary.success());
        assertFalse(selected.success());
        assertEquals(ordinary.diagnostics(), selected.diagnostics());
        assertTrue(selected.diagnostics().stream().anyMatch(fact -> fact.kind() == Diagnostic.Kind.ERROR));
        assertTrue(selected.diagnostics().stream().filter(fact -> fact.kind() == Diagnostic.Kind.ERROR)
                .allMatch(M3GradleASTProviderProbe.DiagnosticFact::originalSource));
        // Javac's DiagnosticSource can read content to calculate line/column
        // positions. The provider must preserve that original diagnostic work.
        assertTrue(ordinary.inputReads() >= 1);
        assertEquals(ordinary.inputReads(), selected.inputReads());
        assertEquals(1, selected.compilations());
        assertEquals(2, selected.summaries().size());
        for (M3AST.Summary summary : selected.summaries()) {
            assertTrue(summary.closed());
            assertTrue(summary.compilerErrors() > 0);
            assertEquals(0, summary.sourceReads());
            assertFalse(summary.consumerFailed());
        }
    }

    @Test
    void parsedFailureClosesBothConsumersAndDoesNotLeakIntoNextInvocation() throws Exception {
        var failed = compile("failed", true, List.of(), M3GradleASTProviderProbe.SOURCE, true, false);
        assertNotNull(failed.failure());
        assertFalse(failed.success());
        assertEquals(1, failed.firstParsed());
        assertEquals(0, failed.secondParsed());
        assertEquals(2, failed.summaries().size());
        for (M3AST.Summary summary : failed.summaries()) {
            assertTrue(summary.closed());
            assertTrue(summary.consumerFailed());
            assertFalse(summary.completed());
        }
        var ordinary = compile("ordinary", false, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        var next = compile("next", true, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, next);
    }

    @Test
    void bothFinishedFailuresPreservePrimaryAndSuppressedErrors() throws Exception {
        var failed = compile("failed", true, List.of(), M3GradleASTProviderProbe.SOURCE, false, true);
        assertNotNull(failed.failure());
        assertFalse(failed.success());
        assertEquals(2, failed.summaries().size());
        assertTrue(failed.summaries().stream().allMatch(M3AST.Summary::closed));
        assertTrue(hasSuppressed(failed.failure()), "second finished failure must remain available");
        var ordinary = compile("ordinary", false, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        var next = compile("next", true, List.of(), M3GradleASTProviderProbe.SOURCE, false, false);
        M3GradleASTProviderProbe.verifyPair(ordinary, next);
    }

    @Test
    void preparePreservesDeferredInputsAndExistingManagerWithoutReading() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        AtomicInteger enumerations = new AtomicInteger();
        AtomicInteger diagnosticSnapshots = new AtomicInteger();
        var source = new M3GradleASTProviderProbe.Input(M3GradleASTProviderProbe.SOURCE);
        Iterable<? extends JavaFileObject> sources = () -> {
            enumerations.incrementAndGet();
            return List.<JavaFileObject>of(source).iterator();
        };
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            var owned = new M3GradleASTProvider().prepare(sources, StandardCharsets.UTF_8, List.of(), manager, () -> {
                diagnosticSnapshots.incrementAndGet();
                return List.of();
            });
            assertSame(sources, owned.sources());
            assertSame(manager, owned.fileManager(manager));
            assertEquals(0, enumerations.get());
            assertEquals(0, source.reads.get());
            assertEquals(0, diagnosticSnapshots.get());
            owned.close();
            owned.close();
            assertThrows(IllegalStateException.class, owned::sources);
            assertEquals(0, enumerations.get());
            assertEquals(0, source.reads.get());
        }
    }

    @Test
    void originalSessionHasOneAttachmentAndExplicitCloseReleasesTask() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var state = new M3GradleASTProviderProbe.State(false, false);
        var source = new M3GradleASTProviderProbe.Input(M3GradleASTProviderProbe.SOURCE);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        assertEquals(null, M3GradleASTProviderProbe.ACTIVE.get());
        M3GradleASTProviderProbe.ACTIVE.set(state);
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = List.of("--release", "21", "-proc:none");
            M3JavacASTProvider.M3Compilation owned = new M3GradleASTProvider().prepare(List.of(source),
                    StandardCharsets.UTF_8, options, manager, () -> List.copyOf(diagnostics.getDiagnostics()));
            JavacTask original = (JavacTask) compiler.getTask(null, manager, diagnostics, options, null, owned.sources());
            M3AST session = M3AST.open(original);
            JavaCompiler.CompilationTask execution = owned.attach(original, () -> 0);
            assertSame(session, M3AST.open(original));
            assertThrows(IllegalStateException.class, () -> owned.attach(original, () -> 0));
            owned.close();
            owned.close();
            assertEquals(0, source.reads.get());
            assertEquals(2, state.summaries.size());
            assertTrue(session.summary().closed());
            assertFalse(session.summary().completed());
            assertThrows(IllegalStateException.class, execution::call);
            assertThrows(IllegalStateException.class, () -> execution.setLocale(Locale.ROOT));
            assertThrows(IllegalStateException.class, () -> execution.setProcessors(List.of()));
            assertThrows(IllegalStateException.class, () -> execution.addModules(List.of("java.logging")));
        } finally {
            M3GradleASTProviderProbe.ACTIVE.remove();
            state.release();
        }
    }

    @Test
    void failedServiceConstructionClosesOriginalSessionBeforeCompilation() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertEquals(null, M3GradleASTProviderProbe.ACTIVE.get());
        var source = new M3GradleASTProviderProbe.Input(M3GradleASTProviderProbe.SOURCE);
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = List.of("--release", "21", "-proc:none");
            var owned = new M3GradleASTProvider().prepare(List.of(source), StandardCharsets.UTF_8, options,
                    manager, List::of);
            JavacTask original = (JavacTask) compiler.getTask(null, manager, null, options, null, owned.sources());
            M3AST session = M3AST.open(original);
            assertThrows(java.util.ServiceConfigurationError.class, () -> owned.attach(original, () -> 0));
            assertTrue(session.summary().closed());
            assertFalse(session.summary().completed());
            assertEquals(0, source.reads.get());
            assertThrows(IllegalStateException.class, owned::sources);
            owned.close();
        }
    }

    @Test
    void parentLoadedConsumerIsRejectedBeforeConstructionAndKeepsApiIdentity() throws Exception {
        String implementation = M3GradleASTProvider.class.getName();
        ClassLoader sharedApiParent = new ClassLoader(M3JavacASTProvider.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(implementation) || name.startsWith(implementation + "$")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        URL providerAsset = M3GradleASTProvider.class.getProtectionDomain().getCodeSource().getLocation();
        var state = new M3GradleASTProviderProbe.State(false, false);
        assertEquals(null, M3GradleASTProviderProbe.ACTIVE.get());
        M3GradleASTProviderProbe.ACTIVE.set(state);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var source = new M3GradleASTProviderProbe.Input(M3GradleASTProviderProbe.SOURCE);
        try (URLClassLoader assets = new URLClassLoader(new URL[] { providerAsset }, sharedApiParent);
             StandardJavaFileManager manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            Class<? extends M3JavacASTProvider> implementationClass = Class.forName(implementation, true, assets)
                    .asSubclass(M3JavacASTProvider.class);
            assertSame(assets, implementationClass.getClassLoader());
            assertSame(M3JavacASTProvider.class, implementationClass.getInterfaces()[0]);
            M3JavacASTProvider provider = implementationClass.getConstructor().newInstance();
            List<String> options = List.of("--release", "21", "-proc:none");
            var owned = provider.prepare(List.of(source), StandardCharsets.UTF_8, options, manager, List::of);
            JavacTask original = (JavacTask) compiler.getTask(null, manager, null, options, null, owned.sources());
            M3AST session = M3AST.open(original);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> owned.attach(original, () -> 0));
            assertTrue(failure.getMessage().contains("outside the explicit provider assets"));
            assertEquals(0, state.instances, "parent-resolved consumer constructor must not execute");
            assertEquals(0, source.reads.get());
            assertTrue(session.summary().closed());
            owned.close();
        } finally {
            M3GradleASTProviderProbe.ACTIVE.remove();
            state.release();
        }
    }

    private M3GradleASTProviderProbe.Run compile(String name, boolean selected, List<String> options,
                                                String source, boolean failParsed, boolean failFinished) throws Exception {
        return M3GradleASTProviderProbe.compile(output.resolve(name), selected, options, source, failParsed, failFinished);
    }

    private static boolean hasSuppressed(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getSuppressed().length != 0) {
                return true;
            }
        }
        return false;
    }
}
