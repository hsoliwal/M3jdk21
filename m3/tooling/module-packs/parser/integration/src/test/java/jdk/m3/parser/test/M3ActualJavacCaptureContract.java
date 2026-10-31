// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Utf8SourceMap;

/**
 * Real JDK javac parser over ONE original NIO M3File capture. Source content is
 * served only from the immutable parser M3File generation: no filesystem reread.
 * This is an external compiler projection, not a proof of semantic binding.
 */
public final class M3ActualJavacCaptureContract {
    private static int checks;

    private M3ActualJavacCaptureContract() { }

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void refuses(Class<? extends Throwable> kind, Checked call, String why) {
        checks++;
        try { call.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("unexpected failure: " + why, failure);
        }
        throw new AssertionError("missing failure: " + why);
    }

    private static final class OriginalInput extends SimpleJavaFileObject {
        private final M3File source;
        private int reads;

        OriginalInput(M3File source) {
            super(source.uri(), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            if (ignoreEncodingErrors) throw new AssertionError("UTF-8 input already decoded strictly");
            return source.text();
        }

        @Override public InputStream openInputStream() {
            throw new AssertionError("javac must not reopen the mutable physical source");
        }
    }

    private record Parsed(M3ASTIndex index, int contentRequests) { }

    private static Parsed parse(M3File source) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "real JDK javac provider present");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var input = new OriginalInput(source);
        try (StandardJavaFileManager files = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none"), null, List.of(input));
            var iterator = task.parse().iterator();
            check(iterator.hasNext(), "javac parsed original snapshot");
            CompilationUnitTree compilationUnit = iterator.next();
            check(!iterator.hasNext(), "one compilation unit for one captured source");
            var limits = new M3ASTIngress.Limits(Math.max(1, source.length()), 4096, 128);
            M3ASTIndex index = M3ASTIngress.javac(source, compilationUnit,
                    Trees.instance(task), limits);
            check(input.reads > 0, "javac sourced content from immutable captured snapshot");
            check(index.source() == source && index.domain() == M3ASTIndex.Domain.JAVAC,
                    "the original AST index owns the exact captured source instance");
            check(index.nodeCount() > 4 && index.nodeCount() < 4096,
                    "the real parsed AST has bounded occurrences");
            check((index.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                    "public detached JavacTask ingress cannot silently attest source binding");
            for (int row = 0; row < index.nodeCount(); row++) {
                if (index.hasExactRange(row)) {
                    check(index.start(row) >= 0 && index.end(row) <= source.length()
                            && index.end(row) >= index.start(row),
                            "every exact half-open AST occurrence stays inside the source");
                    check(index.sourceText(row).equals(source.text().substring(
                            index.start(row), index.end(row))),
                            "source-backed AST span preserves every UTF-16 code unit");
                }
            }
            var hashes = index.structuralHashes(4096, 1_000_000);
            check(hashes.nodeCount() == index.nodeCount()
                    && hashes.schema().equals(M3ASTIndex.STRUCTURAL_HASH_SCHEMA),
                    "original structural digest schema and occurrence count");
            return new Parsed(index, input.reads);
        }
    }

    private static void exercise(Path dir, String type, String literal, int index) throws Exception {
        String code = "package portable;\r\n"
                + "// immutable source: café, € and 😀\r\n"
                + "public final class " + type + " {\r\n"
                + "  private final String value = \"" + literal + "\";\r\n"
                + "  public int café(int argument) { return argument + 1; }\r\n"
                + "}\r\n";
        Path path = dir.resolve(type + ".java");
        byte[] expected = code.getBytes(StandardCharsets.UTF_8);
        Files.write(path, expected);
        // Exactly one source-provider byte capture before javac is invoked.
        byte[] bytes = com.m3.file.M3File.of(path).readAllBytes(expected.length);
        check(Arrays.equals(bytes, expected), "provider returns exact captured input bytes");
        M3File source = M3File.fromCapturedBytes(path.toUri(), bytes,
                StandardCharsets.UTF_8, expected.length);
        M3Utf8SourceMap map = M3Utf8SourceMap.verified(source,
                ByteBuffer.wrap(bytes).asReadOnlyBuffer(), bytes.length);
        check(map.source() == source && map.byteLength() == bytes.length
                && source.text().equals(code), "source and UTF-8 map share one generation");
        for (int offset = 0; offset < source.length();) {
            int byteOffset = map.byteOffset(offset);
            check(map.utf16Offset(byteOffset) == offset,
                    "real Java source UTF-8/UTF-16 coordinate round trip");
            offset += Character.charCount(source.text().codePointAt(offset));
        }
        // Physical file changes, but javac MUST read captured immutable source.
        Files.writeString(path, "package tampered; class Replaced" + index + " {}\n",
                StandardCharsets.UTF_8);
        Parsed first = parse(source);
        Parsed second = parse(source);
        check(first.index.nodeCount() == second.index.nodeCount(),
                "two independent javac tasks preserve original AST occurrence geometry");
        check(first.index.structuralHashes(4096, 1_000_000).sha256(0)
                .equals(second.index.structuralHashes(4096, 1_000_000).sha256(0)),
                "same captured generation gives stable structural SHA-256");
        check(first.contentRequests > 0 && second.contentRequests > 0,
                "both javac tasks consumed the immutable source rather than provider state");
        boolean foundClass = false, foundMethod = false, foundLiteral = false;
        String exactLiteral = "\"" + literal + "\"";
        for (int row = 0; row < first.index.nodeCount(); row++) {
            String kind = first.index.kind(row);
            if (kind.equals("CLASS") && first.index.name(row).orElse("").equals(type)) {
                foundClass = true;
            }
            if (kind.equals("METHOD") && first.index.name(row).orElse("").equals("café")) {
                foundMethod = true;
            }
            if (kind.equals("STRING_LITERAL") && first.index.hasExactRange(row)
                    && first.index.sourceText(row).equals(exactLiteral)) {
                foundLiteral = true;
                check(map.byteOffset(first.index.start(row)) < map.byteOffset(first.index.end(row)),
                        "literal AST span maps to exact original UTF-8 byte region");
            }
        }
        check(foundClass && foundMethod && foundLiteral,
                "actual javac AST kinds and names match the earlier captured Unicode source");
        check(source.byteSha256().isPresent()
                && source.text().contains(type) && !source.text().contains("tampered"),
                "physical mutation cannot change the original parser source generation");
        byte[] later = com.m3.file.M3File.of(path).readAllBytes(4096);
        check(!Arrays.equals(bytes, later), "explicit new provider read sees a new generation");
        refuses(IllegalArgumentException.class,
                () -> M3Utf8SourceMap.verified(source, later, later.length),
                "same URI but another source generation cannot masquerade as captured bytes");
        Arrays.fill(bytes, (byte) 0);
        check(map.byteOffset(source.length()) == expected.length,
                "caller may discard or reuse borrowed byte[] after admission");
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3-real-javac-capture-");
        try {
            exercise(dir, "Packet", "hello", 0);
            exercise(dir, "Unicode", "é café 😀", 1);
            exercise(dir, "Currencies", "€ ₹ ø", 2);
            System.out.println("M3_ACTUAL_JAVAC_CAPTURE_PASS checks=" + checks);
        } finally {
            try (var entries = Files.list(dir)) {
                for (Path file : entries.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(dir);
        }
    }
}
