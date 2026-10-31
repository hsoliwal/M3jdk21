// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.Random;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Executable Java21 contract: one captured source, one javac parse, no path reopen. */
public final class M3CapturedSourceHandoffContract {
    private static int checks;

    private M3CapturedSourceHandoffContract() { }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }

    @FunctionalInterface private interface Throwing { void run() throws Exception; }

    private static void rejects(Class<? extends Throwable> type, Throwing action, String why) {
        checks++;
        try { action.run(); }
        catch (Exception e) {
            if (type.isInstance(e)) return;
            throw new AssertionError("unexpected " + why, e);
        }
        throw new AssertionError("expected rejection: " + why);
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static M3File admit(URI uri, ByteBuffer b, byte[] bytes, int budget) throws Exception {
        return M3File.fromCaptured(uri, b, sha(bytes), StandardCharsets.UTF_8, budget);
    }

    private static void boundsAndSeals() throws Exception {
        URI uri = URI.create("string:///demo/Test.java");
        byte[] bytes = "class Test {}".getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[bytes.length + 4];
        System.arraycopy(bytes, 0, padded, 2, bytes.length);
        ByteBuffer view = ByteBuffer.wrap(padded);
        view.position(2); view.limit(2 + bytes.length);
        int pos = view.position(), limit = view.limit();
        M3File file = admit(uri, view.asReadOnlyBuffer(), bytes, bytes.length);
        check(file.toUri().equals(uri), "source URI retained");
        check(file.source().equals("class Test {}"), "captured source decoded");
        check(file.byteSha256().equals(sha(bytes)), "original byte fingerprint retained");
        check(file.sourceSha256().matches("[0-9a-f]{64}"), "UTF-16 source identity present");
        check(view.position() == pos && view.limit() == limit, "borrowed buffer position and limit unchanged");
        padded[2] = 'X';
        check(file.source().equals("class Test {}"), "caller mutation does not change frozen source");
        check(file.openReader(false).read() == 'c', "reader uses captured text");
        check(M3File.of(uri, new StringBuilder("test")).byteSha256() == null,
                "legacy character-only factory retains contract");
        rejects(IOException.class, () -> admit(uri, ByteBuffer.wrap(bytes), bytes, bytes.length - 1),
                "byte budget");
        rejects(IllegalArgumentException.class,
                () -> M3File.fromCaptured(uri, ByteBuffer.wrap(bytes), sha(bytes), StandardCharsets.UTF_8, -1),
                "negative budget");
        rejects(IllegalArgumentException.class,
                () -> M3File.fromCaptured(uri, ByteBuffer.wrap(bytes), "BAD", StandardCharsets.UTF_8, bytes.length),
                "invalid fingerprint format");
        rejects(IOException.class,
                () -> M3File.fromCaptured(uri, ByteBuffer.wrap(bytes), "0".repeat(64), StandardCharsets.UTF_8, bytes.length),
                "wrong fingerprint");
        rejects(IOException.class,
                () -> M3File.fromCaptured(uri, ByteBuffer.wrap(new byte[] {(byte) 0xc3, (byte) 0x28}),
                        sha(new byte[] {(byte) 0xc3, (byte) 0x28}), StandardCharsets.UTF_8, 10),
                "malformed UTF-8 must not be replaced");
        rejects(NullPointerException.class,
                () -> M3File.fromCaptured(uri, null, sha(bytes), StandardCharsets.UTF_8, bytes.length),
                "null captured bytes");
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 2);
        direct.put((byte) 0); direct.put(bytes); direct.put((byte) 0);
        direct.flip(); direct.position(1); direct.limit(1 + bytes.length);
        check(admit(uri, direct, bytes, bytes.length).source().equals("class Test {}"),
                "non-array direct byte buffer accepted");
    }

    private static byte[] compile(Path out, M3File a, M3File b) throws Exception {
        Files.createDirectories(out);
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "real javac available");
        var errors = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(errors, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, errors,
                    List.of("--release", "21", "-proc:none", "-g:none", "-d", out.toString()),
                    null, List.of(a, b));
            int units = 0;
            for (CompilationUnitTree unit : task.parse()) {
                units++;
                if (unit.getSourceFile().toUri().equals(a.toUri())) {
                    check(unit.getSourceFile().getCharContent(false) == a.source(),
                            "parser retained exact immutable String instance");
                }
            }
            check(units == 2, "exactly two original source units");
            task.analyze();
            check(errors.getDiagnostics().stream().noneMatch(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR),
                    "javac attribution did not report failures");
            task.generate();
            check(errors.getDiagnostics().stream().noneMatch(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR),
                    "generation compiles exact original source");
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[] {out.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> bClass = Class.forName("demo.B", true, loader);
            check(((Integer) bClass.getMethod("value").invoke(null)) == 2,
                    "compiled class reflects captured value despite subsequent disk edits");
        }
        return Files.readAllBytes(out.resolve("demo/A.class"));
    }

    private static void compilerCapture() throws Exception {
        Path root = Files.createTempDirectory("m3-captured-ast-");
        Path physical = Files.createDirectories(root.resolve("demo")).resolve("A.java");
        String source = "package demo; public class A { public static final int VALUE = 2; }";
        String dependent = "package demo; public class B { public static int value(){ return A.VALUE; } }";
        Files.writeString(physical, source);
        FileTime originalTime = Files.getLastModifiedTime(physical);
        long originalSize = Files.size(physical);
        byte[] snapshot = Files.readAllBytes(physical);
        M3File original = admit(physical.toUri(), ByteBuffer.wrap(snapshot).asReadOnlyBuffer(), snapshot, 4096);
        M3File b = M3File.of(URI.create("string:///demo/B.java"), dependent);
        // Reopening the physical path now is incorrect: it belongs to a newer generation.
        Files.writeString(physical, source.replace("= 2", "= 9"));
        Files.setLastModifiedTime(physical, originalTime);
        check(Files.size(physical) == originalSize
                && Files.getLastModifiedTime(physical).equals(originalTime),
                "same length and restored timestamp cannot hide edit");
        Files.delete(physical);
        byte[] first = compile(root.resolve("captured"), original, b);
        byte[] oracle = compile(root.resolve("oracle"), M3File.of(original.toUri(), source), b);
        check(Arrays.equals(first, oracle), "captured class bytes equal independent clean oracle");
        check(original.source().equals(source), "source generation survives physical deletion");
        check(original.byteSha256().equals(sha(snapshot)), "compiled bytes bound to original digest");
        check(!Files.exists(physical), "source file remained deleted throughout parse/compile");
    }

    private static void replayAndPermutations() throws Exception {
        String[] samples = {"", "hello", "naïve", "日本語", "😀", "e\u0301", "line\r\nend", "\u0000abc"};
        URI uri = URI.create("string:///demo/Encoded.java");
        for (int seed = 0; seed < 96; seed++) {
            Random random = new Random(seed * 311L + 7);
            String original = samples[random.nextInt(samples.length)];
            byte[] utf8 = original.getBytes(StandardCharsets.UTF_8);
            int padding = random.nextInt(11);
            byte[] arena = new byte[utf8.length + padding + 3];
            System.arraycopy(utf8, 0, arena, padding, utf8.length);
            ByteBuffer slice = ByteBuffer.wrap(arena);
            slice.position(padding);
            slice.limit(padding + utf8.length);
            M3File parsed = admit(uri, slice, utf8, utf8.length);
            check(parsed.source().equals(original), "seeded exact decode " + seed);
            check(parsed.byteSha256().equals(sha(utf8)), "seeded byte digest " + seed);
            check(slice.position() == padding && slice.remaining() == utf8.length,
                    "seeded cursor unchanged " + seed);
            check(parsed.sourceSha256().equals(M3FileSourceFacts.sourceSha256(original)),
                    "seeded UTF-16 source digest " + seed);
        }
        byte[] utf16 = "κόσμε😀".getBytes(StandardCharsets.UTF_16LE);
        M3File decoded = M3File.fromCaptured(uri, ByteBuffer.wrap(utf16), sha(utf16),
                StandardCharsets.UTF_16LE, utf16.length);
        check(decoded.source().equals("κόσμε😀"), "UTF-16LE source decoded without replacement");
        check(decoded.byteSha256().equals(sha(utf16)), "original UTF-16LE byte hash preserved");
    }

    public static void main(String[] args) throws Exception {
        boundsAndSeals();
        replayAndPermutations();
        compilerCapture();
        System.out.println("PASS M3CapturedSourceHandoffContract checks=" + checks);
    }
}
