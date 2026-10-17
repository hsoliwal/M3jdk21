// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import static com.synexia.mindex.compiler.ast.M3FrontendTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePathScanner;
import com.synexia.mindex.swap.SqliteSwapStore;
import com.synexia.mindex.swap.TieredSwapCache;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.lang.model.element.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3JavacASTIndexTest {
    @TempDir Path temporary;

    @Test void upstreamPublicSessionAndImmutableIndexShareTheSameActualParse() throws Exception {
        // The test sources still compile on an ordinary Java 21 compiler. The qualified
        // runtime patch provides the current upstream public API and existing plugin.
        Class<?> api = Class.forName("com.sun.source.util.M3AST");
        Class<?> consumerType = Class.forName("com.sun.source.util.M3AST$Consumer");
        Class<?> unitType = Class.forName("com.sun.source.util.M3AST$Unit");
        Class<?> sourceType = Class.forName("com.sun.source.util.M3AST$Source");
        M3File source = input("Coexisting", "class Coexisting { int value = 42; }\n");
        List<CompilationUnitTree> publicUnits = new ArrayList<>();
        Map<CompilationUnitTree, String> frozen = new java.util.IdentityHashMap<>();
        try (var compile = new Compilation(temporary, List.of(source))) {
            Object session = api.getMethod("open", com.sun.source.util.JavacTask.class)
                    .invoke(null, compile.task);
            Object consumer = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{consumerType}, (proxy, method, arguments) -> {
                        if (method.getName().equals("parsed")) {
                            Object unit = arguments[0];
                            publicUnits.add((CompilationUnitTree) unitType.getMethod("tree").invoke(unit));
                            Object retained = unitType.getMethod("source").invoke(unit);
                            assertSame(source.source(), sourceType.getMethod("text").invoke(retained));
                        }
                        return null;
                    });
            api.getMethod("addConsumer", consumerType).invoke(session, consumer);
            try (var observation = observe(compile.task, (unit, text) -> frozen.put(unit, text.toString()));
                    var index = new M3JavacASTIndex(compile.task,
                            M3CompilationContext.isolated(compile.options), frozen::get, LIMITS, 21)) {
                assertNotNull(observation, "compiler supplied an independently closeable observer registration");
                index.on(M3JavacASTIndex.Phase.PARSE, "same-upstream-unit", view -> {
                    assertEquals(1, publicUnits.size());
                    assertSame(publicUnits.getFirst(), view.unit());
                    assertSame(source.source(), view.source());
                    try { assertEquals(source.source(), view.ast().source()); }
                    catch (IOException failure) { throw new UncheckedIOException(failure); }
                });
                assertTrue(compile.call(), compile::diagnosticText);
                assertEquals(1, index.statistics().parseEvents());
                assertEquals(1, index.statistics().syntaxCaptures());
                Object summary = api.getMethod("summary").invoke(session);
                assertEquals(1L, summary.getClass().getMethod("parsedUnits").invoke(summary));
                assertEquals(1L, summary.getClass().getMethod("capturedSources").invoke(summary));
                assertEquals(true, summary.getClass().getMethod("completed").invoke(summary));
                assertEquals(true, summary.getClass().getMethod("closed").invoke(summary));
            } finally {
                api.getMethod("close").invoke(session);
                frozen.clear();
            }
        }
        assertEquals("M3ASTIndex", new M3ASTPlugin().getName());
        var builtin = (com.sun.source.util.Plugin) Class.forName("com.sun.tools.javac.m3.M3ASTPC")
                .getConstructor().newInstance();
        assertEquals("M3AST", builtin.getName());
    }

    @Test void noConsumerPerformsNoSyntaxCaptureAndLeavesCompilerOutputExact() throws Exception {
        M3File source = input("Unused", "class Unused { static int twice(int x) { return x * 2; } }\n");
        byte[] baseline;
        try (var compile = new Compilation(temporary.resolve("baseline"), List.of(source))) {
            assertTrue(compile.call(), compile::diagnosticText);
            baseline = Files.readAllBytes(compile.classes.resolve("Unused.class"));
        }
        try (var compile = new Compilation(temporary.resolve("indexed"), List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            assertTrue(compile.call(), compile::diagnosticText);
            assertEquals(1, index.statistics().parseEvents());
            assertEquals(0, index.statistics().syntaxCaptures());
            assertEquals(0, index.statistics().consumerCalls());
            assertTrue(index.statistics().closed());
            assertArrayEquals(baseline, Files.readAllBytes(compile.classes.resolve("Unused.class")));
        }
    }

    @Test void twoConsumersShareOneCaptureFromTheOwningCompilerUnit() throws Exception {
        M3File source = input("Shared", "// original CRLF\r\nclass Shared { int a = 1; int b = a + 2; }\r\n");
        List<M3AST> values = new ArrayList<>();
        List<CompilationUnitTree> units = new ArrayList<>();
        try (var compile = new Compilation(temporary, List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            for (String consumer : List.of("first", "second")) {
                index.on(M3JavacASTIndex.Phase.PARSE, consumer, view -> {
                    assertSame(compile.task, view.task());
                    assertSame(source.source(), view.source());
                    units.add(view.unit());
                    try { values.add(view.ast()); }
                    catch (IOException failure) { throw new UncheckedIOException(failure); }
                });
            }
            assertTrue(compile.call(), compile::diagnosticText);
            assertEquals(1, index.statistics().parseEvents());
            assertEquals(1, index.statistics().syntaxCaptures());
            assertEquals(2, index.statistics().consumerCalls());
        }
        assertEquals(2, values.size());
        assertSame(values.getFirst(), values.getLast());
        assertSame(units.getFirst(), units.getLast());
        assertEquals(source.source(), values.getFirst().source());
        assertEquals(rawUtf16Sha256(source.source()), values.getFirst().sourceSha256());
    }

    @Test void callbackViewsRejectAnotherThreadAndAllEscapedAccess() throws Exception {
        M3File source = input("Lifetime", "class Lifetime {}\n");
        AtomicReference<M3JavacASTIndex.View> escaped = new AtomicReference<>();
        try (var worker = Executors.newSingleThreadExecutor();
                var compile = new Compilation(temporary, List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            index.on(M3JavacASTIndex.Phase.PARSE, "lifetime", view -> {
                escaped.set(view);
                try {
                    assertTrue(worker.submit(() -> {
                        assertThrows(IllegalStateException.class, view::unit);
                        assertThrows(IllegalStateException.class, view::source);
                        assertThrows(IllegalStateException.class, view::ast);
                        return true;
                    }).get(5, TimeUnit.SECONDS));
                } catch (Exception failure) { throw new IllegalStateException(failure); }
                assertNotNull(view.unit());
            });
            assertTrue(compile.call(), compile::diagnosticText);
            assertThrows(IllegalStateException.class, escaped.get()::unit);
            assertThrows(IllegalStateException.class, escaped.get()::source);
            assertThrows(IllegalStateException.class, escaped.get()::phase);
            assertThrows(IllegalStateException.class, escaped.get()::trees);
            assertThrows(IllegalStateException.class, escaped.get()::task);
            assertThrows(IllegalStateException.class, escaped.get()::analyzedType);
            assertThrows(IllegalStateException.class, escaped.get()::ast);
            assertThrows(IllegalStateException.class, () -> escaped.get().facts("late", 1));
            assertDoesNotThrow(index::close);
        }
    }

    @Test void analysisUsesActualAttributedTypesAndKeepsParseSyntaxFrozen() throws Exception {
        M3File source = input("First", "class First { static int value = 7; }\n"
                + "class Second { int result() { return First.value; } }\n");
        AtomicReference<M3AST> frozen = new AtomicReference<>();
        AtomicReference<String> frozenFingerprint = new AtomicReference<>();
        AtomicReference<CompilationUnitTree> unit = new AtomicReference<>();
        Set<String> types = new HashSet<>();
        Set<M3FileSourceFacts> identities = new HashSet<>();
        AtomicReference<String> resolvedField = new AtomicReference<>();
        try (var compile = new Compilation(temporary, List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            index.on(M3JavacASTIndex.Phase.PARSE, "freeze", view -> {
                unit.set(view.unit());
                try { frozen.set(view.ast()); frozenFingerprint.set(view.ast().fingerprint()); }
                catch (IOException failure) { throw new UncheckedIOException(failure); }
            });
            index.on(M3JavacASTIndex.Phase.ANALYZE, "bindings", view -> {
                assertSame(unit.get(), view.unit());
                assertNotNull(view.analyzedType());
                String type = view.analyzedType().getQualifiedName().toString();
                types.add(type);
                identities.add(view.facts("resolved-fields", 1));
                assertThrows(IllegalStateException.class, view::ast);
                if (type.equals("Second")) {
                    new TreePathScanner<Void, Void>() {
                        @Override public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
                            if (tree.toString().equals("First.value")) {
                                Element field = view.trees().getElement(getCurrentPath());
                                assertNotNull(field);
                                resolvedField.set(field.getEnclosingElement() + "#" + field.getSimpleName());
                            }
                            return super.visitMemberSelect(tree, unused);
                        }
                    }.scan(view.unit(), null);
                }
            });
            index.on(M3JavacASTIndex.Phase.GENERATE, "generation-phase", view -> {
                assertThrows(IllegalStateException.class, view::ast);
                assertEquals("GENERATE", view.facts("generation", 1).phase());
            });
            assertTrue(compile.call(), compile::diagnosticText);
            assertEquals(Set.of("First", "Second"), types);
            assertEquals(2, identities.size());
            assertEquals(2, index.statistics().analyzeEvents());
            assertEquals(2, index.statistics().generateEvents());
            assertEquals(1, index.statistics().syntaxCaptures());
            assertEquals("First#value", resolvedField.get());
            assertEquals(frozenFingerprint.get(), frozen.get().fingerprint());
        }
    }

    @Test void failedConsumerClosesTheIndexAndInvalidatesItsView() throws Exception {
        M3File source = input("Refused", "class Refused {}\n");
        AtomicReference<M3JavacASTIndex.View> escaped = new AtomicReference<>();
        try (var compile = new Compilation(temporary, List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            index.on(M3JavacASTIndex.Phase.PARSE, "failure", view -> {
                escaped.set(view);
                throw new IllegalStateException("consumer rejected projection");
            });
            RuntimeException refusal = assertThrows(RuntimeException.class, compile::call);
            assertTrue(causedBy(refusal, IllegalStateException.class));
            assertTrue(index.statistics().closed());
            assertThrows(IllegalStateException.class, escaped.get()::unit);
            assertThrows(IllegalStateException.class,
                    () -> index.on(M3JavacASTIndex.Phase.PARSE, "after-failure", view -> { }));
        }
    }

    @Test void consumerRegistrationIsFixedBeforeCompilationStarts() throws Exception {
        M3File source = input("Registration", "class Registration {}\n");
        AtomicInteger calls = new AtomicInteger();
        try (var compile = new Compilation(temporary, List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            assertThrows(IllegalArgumentException.class,
                    () -> index.on(M3JavacASTIndex.Phase.PARSE, " ", view -> { }));
            index.on(M3JavacASTIndex.Phase.PARSE, "same-id", view -> {
                calls.incrementAndGet();
                assertThrows(IllegalStateException.class,
                        () -> index.on(M3JavacASTIndex.Phase.ANALYZE, "late", ignored -> { }));
            });
            assertThrows(IllegalArgumentException.class,
                    () -> index.on(M3JavacASTIndex.Phase.PARSE, "same-id", view -> { }));
            index.on(M3JavacASTIndex.Phase.ANALYZE, "same-id", view -> calls.incrementAndGet());
            assertTrue(compile.call(), compile::diagnosticText);
            assertEquals(2, calls.get());
        }
    }

    @Test void malformedSourceNeverBecomesSuccessfulCompilation() throws Exception {
        M3File source = input("Malformed", "class Malformed { int missing = ; }\n");
        try (var compile = new Compilation(temporary, List.of(source));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21)) {
            assertFalse(compile.call());
            assertTrue(compile.errors() > 0);
            assertTrue(index.statistics().closed());
            assertFalse(Files.exists(compile.classes.resolve("Malformed.class")));
            assertEquals(0, index.statistics().syntaxCaptures());
        }
    }

    @Test void parseOnlyInvocationHasAnExplicitCloseAndNoAttributionOrGeneration() throws Exception {
        M3File source = input("ParseOnly", "class ParseOnly { Unknown unresolved; }\n");
        AtomicReference<M3AST> snapshot = new AtomicReference<>();
        try (var compile = new Compilation(temporary, List.of(source))) {
            var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                    M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21);
            try (index) {
                index.on(M3JavacASTIndex.Phase.PARSE, "parse-only", view -> {
                    try { snapshot.set(view.ast()); }
                    catch (IOException failure) { throw new UncheckedIOException(failure); }
                });
                List<CompilationUnitTree> units = new ArrayList<>();
                compile.task.parse().forEach(units::add);
                assertEquals(1, units.size());
                assertEquals(0, compile.errors(), "unresolved names are not a parse error");
                assertEquals(1, index.statistics().syntaxCaptures());
                assertEquals(0, index.statistics().analyzeEvents());
                assertEquals(0, index.statistics().generateEvents());
                assertFalse(index.statistics().closed());
                assertFalse(Files.exists(compile.classes.resolve("ParseOnly.class")));
            }
            assertTrue(index.statistics().closed());
            assertEquals(source.source(), snapshot.get().source());
        }
    }

    @Test void analyzeOnlyInvocationDeliversBindingsAndClosesWithoutGeneratingClasses() throws Exception {
        M3File source = input("AnalyzeOnly", "class AnalyzeOnly { int value = 7; }\n");
        List<String> analyzed = new ArrayList<>();
        try (var compile = new Compilation(temporary, List.of(source))) {
            var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                    M3JavacASTIndex.frozenInputs(List.of(source)), LIMITS, 21);
            try (index) {
                index.on(M3JavacASTIndex.Phase.ANALYZE, "analyze-only", view -> {
                    analyzed.add(view.analyzedType().getQualifiedName().toString());
                    assertThrows(IllegalStateException.class, view::ast);
                });
                List<Element> elements = new ArrayList<>();
                compile.task.analyze().forEach(elements::add);
                assertEquals(1, elements.size());
                assertEquals(0, compile.errors());
                assertEquals(List.of("AnalyzeOnly"), analyzed);
                assertEquals(1, index.statistics().parseEvents());
                assertEquals(1, index.statistics().analyzeEvents());
                assertEquals(0, index.statistics().generateEvents());
                assertEquals(0, index.statistics().syntaxCaptures());
                assertFalse(Files.exists(compile.classes.resolve("AnalyzeOnly.class")));
            }
            assertTrue(index.statistics().closed());
        }
    }

    @Test void compilerProjectionUsesTheExistingDurableCacheAndReloadsAfterCallback() throws Exception {
        Path library = Path.of(System.getProperty("m3.cache.native", "missing-native-library")).toAbsolutePath();
        assertTrue(Files.isRegularFile(library), "the actual SQLite donor library is required for integration");
        SqliteSwapStore.loadLibrary(library);
        M3File first = input("CachedFirst", "class CachedFirst { int value = 1; }\n");
        M3File second = input("CachedSecond", "class CachedSecond { int value = 2; }\n");
        List<M3File> inputs = List.of(first, second);
        Map<String, Long> ordinals = Map.of(first.toUri().toString(), 0L, second.toUri().toString(), 1L);
        Map<Long, M3FileSourceFacts> keys = new HashMap<>();
        Map<Long, String> fingerprints = new HashMap<>();
        try (var compile = new Compilation(temporary, inputs)) {
            M3CompilationContext context = M3CompilationContext.isolated(compile.options);
            try (var store = SqliteSwapStore.open(temporary.resolve("compiler-cache.sqlite"), context.sha256(),
                        "m3-frontend-test-v1", SqliteSwapStore.Options.defaults());
                    var cache = new M3ASTWorkspaceCache<>(context.sha256(), store, new M3ASTCodec(LIMITS),
                            new TieredSwapCache.Budget(1 << 20, 1, 0), false);
                    var index = new M3JavacASTIndex(compile.task, context,
                            M3JavacASTIndex.frozenInputs(inputs), LIMITS, 21)) {
                for (String consumer : List.of("consumer-a", "consumer-b")) {
                    index.on(M3JavacASTIndex.Phase.PARSE, consumer, view -> {
                        long id = ordinals.get(view.unit().getSourceFile().toUri().toString());
                        try (var lease = view.acquireSyntax(cache, id)) {
                            keys.put(id, view.facts("javac-public-tree", 1));
                            assertEquals(keys.get(id), lease.key());
                            assertEquals(view.source(), lease.value().source());
                            String old = fingerprints.putIfAbsent(id, lease.value().fingerprint());
                            if (old != null) assertEquals(old, lease.value().fingerprint());
                        } catch (IOException failure) { throw new UncheckedIOException(failure); }
                    });
                }
                assertTrue(compile.call(), compile::diagnosticText);
                assertEquals(2, index.statistics().parseEvents());
                assertEquals(2, index.statistics().syntaxCaptures());
                assertEquals(4, index.statistics().consumerCalls());
                assertEquals(2, store.count());
                assertEquals(1, cache.stats().entries());
                assertFalse(cache.residentIdsByHeat(2).contains(0L));
                try (var reloaded = cache.acquire(0, keys.get(0L))) {
                    assertEquals(first.source(), reloaded.value().source());
                    assertEquals(fingerprints.get(0L), reloaded.value().fingerprint());
                    assertEquals(keys.get(0L), reloaded.key());
                }
                assertEquals(0, cache.stats().pins());
                assertEquals(2, store.count());
            }
        }
    }
}
