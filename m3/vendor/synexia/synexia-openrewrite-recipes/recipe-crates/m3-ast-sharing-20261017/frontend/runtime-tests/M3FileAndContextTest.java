// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import static com.synexia.mindex.compiler.ast.M3FrontendTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FileAndContextTest {
    @TempDir Path temporary;

    @Test void characterIngressIsImmutableAndPreservesExactUtf16() throws Exception {
        String original = "\ufeff// CRLF\r\nclass Unicode { String s = \"\ud83d\ude80\ud800\"; }\r\n";
        StringBuilder changing = new StringBuilder(original);
        M3File file = M3File.of(URI.create("memory:///Unicode.java"), changing);
        changing.replace(0, changing.length(), "changed after ingress");
        assertEquals(original, file.source());
        assertSame(file.source(), file.getCharContent(false));
        assertNull(file.byteSha256());
        assertEquals(M3FileSourceFacts.sourceSha256(original), file.sourceSha256());
        try (var reader = file.openReader(false)) {
            StringBuilder fromReader = new StringBuilder();
            for (int c; (c = reader.read()) != -1;) fromReader.append((char) c);
            assertEquals(original, fromReader.toString());
        }
    }

    @Test void byteSnapshotHasIndependentDigestAndStrictBoundedDecoding() throws Exception {
        Path path = temporary.resolve("Bytes.java");
        String source = "\ufeff// exact bytes\r\nclass Bytes { String text = \"λ\"; }\r\n";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);
        M3File file = M3File.read(path, StandardCharsets.UTF_8, bytes.length);
        assertEquals(source, file.source());
        assertEquals(sha256(bytes), file.byteSha256());
        assertEquals(M3FileSourceFacts.sourceSha256(source), file.sourceSha256());
        assertNotEquals(file.byteSha256(), file.sourceSha256());
        Files.writeString(path, "class Changed {}", StandardCharsets.UTF_8);
        assertEquals(source, file.source());
        Files.write(path, bytes);
        assertThrows(IOException.class, () -> M3File.read(path, StandardCharsets.UTF_8, bytes.length - 1));
        assertThrows(IllegalArgumentException.class, () -> M3File.read(path, StandardCharsets.UTF_8, -1));
        Files.write(path, new byte[] {(byte) 0xc3, 0x28});
        assertThrows(IOException.class, () -> M3File.read(path, StandardCharsets.UTF_8, 2));
    }

    @Test void stockJavacWrapperStillResolvesTheAdmittedImmutableInput() throws Exception {
        M3File file = input("Wrapped", "class Wrapped { int answer = 42; }\n");
        AtomicBoolean observed = new AtomicBoolean();
        try (var compile = new Compilation(temporary, List.of(file));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(file)), LIMITS, 21)) {
            index.on(M3JavacASTIndex.Phase.PARSE, "wrapped-input", view -> {
                assertFalse(view.unit().getSourceFile() instanceof M3File,
                        "JavacTool must exercise its actual ClientCodeWrapper route");
                assertSame(file.source(), view.source());
                assertSame(compile.task, view.task());
                observed.set(true);
            });
            assertTrue(compile.call(), compile::diagnosticText);
            assertTrue(observed.get());
            assertEquals(0, index.statistics().syntaxCaptures());
        }
    }

    @Test void duplicateAndReplacedFrozenInputsAreRefused() throws Exception {
        M3File admitted = input("Bound", "class Bound {}\n");
        M3File duplicate = M3File.of(admitted.toUri(), admitted.source());
        assertThrows(IllegalArgumentException.class,
                () -> M3JavacASTIndex.frozenInputs(List.of(admitted, duplicate)));
        JavaFileObject replacement = new SimpleJavaFileObject(admitted.toUri(), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return new String(admitted.source());
            }
        };
        try (var compile = new Compilation(temporary, List.of(replacement));
                var index = new M3JavacASTIndex(compile.task, M3CompilationContext.isolated(compile.options),
                        M3JavacASTIndex.frozenInputs(List.of(admitted)), LIMITS, 21)) {
            index.on(M3JavacASTIndex.Phase.PARSE, "verify-admitted-source", view -> { });
            RuntimeException refusal = assertThrows(RuntimeException.class, compile::call);
            assertTrue(causedBy(refusal, IOException.class));
            assertTrue(index.statistics().closed());
            assertEquals(0, index.statistics().syntaxCaptures());
        }
    }

    @Test void sealedContextIncludesOrderContentAndGenerationAndDefendsItsDigest() {
        List<String> options = new ArrayList<>(List.of("--release", "21", "-proc:none"));
        String generation = "a".repeat(64);
        String first = "b".repeat(64);
        String second = "c".repeat(64);
        M3CompilationContext context = M3CompilationContext.sealed(options, generation, List.of(first, second));
        assertTrue(context.reusable());
        assertEquals(context, M3CompilationContext.sealed(options, generation, List.of(first, second)));
        options.set(1, "17");
        assertNotEquals(context.sha256(), M3CompilationContext.sealed(options, generation, List.of(first, second)).sha256());
        assertNotEquals(context.sha256(), M3CompilationContext.sealed(List.of("--release", "21", "-proc:none"),
                generation, List.of(second, first)).sha256());
        assertNotEquals(context.sha256(), M3CompilationContext.sealed(List.of("--release", "21", "-proc:none"),
                "d".repeat(64), List.of(first, second)).sha256());
        assertThrows(UnsupportedOperationException.class, () -> context.facts().add("drift"));
        assertThrows(IllegalArgumentException.class,
                () -> new M3CompilationContext("f".repeat(64), context.facts(), true));
        assertThrows(IllegalArgumentException.class,
                () -> M3CompilationContext.sealed(List.of(), "path-not-content", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> M3CompilationContext.sealed(List.of(), generation, List.of("A".repeat(64))));
    }

    @Test void incompleteEnvironmentUsesFreshSessionAuthority() {
        M3CompilationContext first = M3CompilationContext.isolated(List.of("--release", "21"));
        M3CompilationContext second = M3CompilationContext.isolated(List.of("--release", "21"));
        assertFalse(first.reusable());
        assertFalse(second.reusable());
        assertNotEquals(first.sha256(), second.sha256());
        assertEquals(M3FileSourceFacts.contextSha256(first.facts()), first.sha256());
    }
}
