// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import static com.synexia.mindex.compiler.ast.M3FrontendTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The qualified compiler patch is required: missing bridge classes fail these actual integration tests. */
class M3ASTCompilerBridgeTest {
    @TempDir Path temporary;

    @Test void observersAreTaskScopedAndClosingOnePreservesTheOther() throws Exception {
        M3File firstInput = input("ObservedA", "class ObservedA {}\n");
        M3File secondInput = input("ObservedB", "class ObservedB {}\n");
        AtomicInteger removed = new AtomicInteger();
        AtomicInteger firstCount = new AtomicInteger();
        AtomicInteger secondCount = new AtomicInteger();
        try (var first = new Compilation(temporary.resolve("first"), List.of(firstInput));
                var second = new Compilation(temporary.resolve("second"), List.of(secondInput));
                var closed = observe(first.task, (unit, source) -> removed.incrementAndGet());
                var remaining = observe(first.task, (unit, source) -> {
                    assertSame(firstInput.source(), source);
                    firstCount.incrementAndGet();
                });
                var separate = observe(second.task, (unit, source) -> {
                    assertSame(secondInput.source(), source);
                    secondCount.incrementAndGet();
                })) {
            assertDoesNotThrow(closed::close);
            assertDoesNotThrow(closed::close);
            assertNotNull(remaining);
            assertNotNull(separate);
            assertTrue(first.call(), first::diagnosticText);
            assertEquals(0, removed.get());
            assertEquals(1, firstCount.get());
            assertEquals(0, secondCount.get());
            assertTrue(second.call(), second::diagnosticText);
            assertEquals(1, firstCount.get());
            assertEquals(1, secondCount.get());
        }
    }

    @Test void compilerBridgeReportsActualReleaseAndDiagnosticAuthority() throws Exception {
        M3File valid = input("Release17", "class Release17 {}\n");
        try (var compile = new Compilation(temporary.resolve("release"), List.of(valid), 17, false)) {
            assertEquals(17, sourceRelease(compile.task));
            assertTrue(compile.call(), compile::diagnosticText);
        }
        M3File invalid = input("Invalid", "class Invalid { int n = ; }\n");
        AtomicInteger compilerErrors = new AtomicInteger(-1);
        try (var compile = new Compilation(temporary.resolve("invalid"), List.of(invalid))) {
            compile.task.addTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.COMPILATION) {
                        try { compilerErrors.set(errorCount(compile.task)); }
                        catch (Exception failure) { throw new IllegalStateException(failure); }
                    }
                }
            });
            assertFalse(compile.call());
            assertTrue(compile.errors() > 0);
            assertTrue(compilerErrors.get() > 0);
        }
    }

    @Test void annotationGeneratedSourceUsesTheSameExactParserHook() throws Exception {
        M3File input = input("UsesGenerated", "class UsesGenerated { int value = Generated.VALUE; }\n");
        Map<CompilationUnitTree, String> originalInputs = new IdentityHashMap<>();
        List<M3AST> snapshots = new ArrayList<>();
        try (var compile = new Compilation(temporary, List.of(input), 21, true);
                var observer = observe(compile.task, (unit, source) -> {
                    assertNull(originalInputs.put(unit, source.toString()), "each parser result is published once");
                });
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options), unit -> {
                    String original = originalInputs.remove(unit);
                    if (original == null) throw new IOException("exact compiler source was not published before PARSE");
                    return original;
                }, LIMITS, 21)) {
            assertNotNull(observer);
            compile.task.setProcessors(List.of(new GeneratesSource()));
            index.on(M3JavacASTIndex.Phase.PARSE, "generated-source", view -> {
                try { snapshots.add(view.ast()); }
                catch (IOException failure) { throw new UncheckedIOException(failure); }
            });
            assertTrue(compile.call(), compile::diagnosticText);
            assertEquals(0, compile.errors());
            assertEquals(2, index.statistics().parseEvents());
            assertEquals(2, index.statistics().syntaxCaptures());
            assertEquals(2, snapshots.size());
            assertTrue(originalInputs.isEmpty());
            assertTrue(snapshots.stream().anyMatch(ast -> ast.source().equals(input.source())));
            assertTrue(snapshots.stream().anyMatch(ast -> ast.source().equals(GeneratesSource.SOURCE)));
            assertTrue(Files.isRegularFile(compile.classes.resolve("Generated.class")));
            assertTrue(Files.isRegularFile(compile.classes.resolve("UsesGenerated.class")));
        }
    }

    @Test void buildPluginExportsTheCapturedSourceAndHonestSuccessReceipt() throws Exception {
        M3File source = input("Exported", "// retained source\r\nclass Exported { int n = 5; }\r\n");
        Path output = temporary.resolve("ast-exports").toAbsolutePath();
        try (var compile = new Compilation(temporary.resolve("compiler"), List.of(source))) {
            new M3ASTPlugin().init(compile.task, "out=" + output);
            assertTrue(compile.call(), compile::diagnosticText);
        }
        Path session = onlySession(output);
        String receipt = Files.readString(session.resolve("receipt.json"));
        assertEquals(0, field(receipt, "compilerErrors"));
        assertEquals(1, field(receipt, "parseEvents"));
        assertEquals(1, field(receipt, "syntaxCaptures"));
        assertTrue(receipt.contains("\"crossProcessSemanticReuse\": false"));
        List<Path> images;
        try (var children = Files.list(session)) {
            images = children.filter(path -> path.getFileName().toString().endsWith(".m3ast")).toList();
        }
        assertEquals(1, images.size());
        M3AST recovered = new M3ASTCodec(LIMITS).decode(Files.readAllBytes(images.getFirst()));
        assertEquals(source.source(), recovered.source());
        assertEquals(source.toUri().toString(), recovered.sourceName());
        assertEquals(rawUtf16Sha256(source.source()), recovered.sourceSha256());
    }

    @Test void buildPluginNeverLabelsMalformedSourceAsSuccessful() throws Exception {
        M3File source = input("BadExport", "class BadExport { int n = ; }\n");
        Path output = temporary.resolve("bad-exports").toAbsolutePath();
        try (var compile = new Compilation(temporary.resolve("compiler"), List.of(source))) {
            new M3ASTPlugin().init(compile.task, "out=" + output);
            assertFalse(compile.call());
            assertTrue(compile.errors() > 0);
            assertFalse(Files.exists(compile.classes.resolve("BadExport.class")));
        }
        String receipt = Files.readString(onlySession(output).resolve("receipt.json"));
        assertTrue(field(receipt, "compilerErrors") > 0);
        assertTrue(receipt.contains("\"crossProcessSemanticReuse\": false"));
    }

    private static Path onlySession(Path output) throws IOException {
        try (var children = Files.list(output)) {
            List<Path> sessions = children.filter(Files::isDirectory).toList();
            assertEquals(1, sessions.size());
            return sessions.getFirst();
        }
    }

    private static int field(String json, String name) {
        var matcher = Pattern.compile("\"" + name + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        assertTrue(matcher.find(), "receipt field " + name + " was missing");
        return Integer.parseInt(matcher.group(1));
    }

    private static final class GeneratesSource extends AbstractProcessor {
        static final String SOURCE = "final class Generated { static final int VALUE = 7; }\n";
        private boolean generated;
        @Override public Set<String> getSupportedAnnotationTypes() { return Set.of("*"); }
        @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.RELEASE_21; }
        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
            if (!generated && !round.processingOver()) {
                generated = true;
                try (var writer = processingEnv.getFiler().createSourceFile("Generated").openWriter()) {
                    writer.write(SOURCE);
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
            }
            return false;
        }
    }
}
