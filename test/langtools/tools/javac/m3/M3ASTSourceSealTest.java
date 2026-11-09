/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

/* @test
 * @summary Exact M3 source seals preserve shared compiler work and refuse failed capture publication
 * @modules jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.m3:+open
 * @run main/othervm M3ASTSourceSealTest
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.tools.javac.m3.M3ASTPC;
import com.sun.tools.javac.m3.M3PrimitiveImageCompiler;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class M3ASTSourceSealTest {
    private static final URI HASH_URI = URI.create("string:///HashInput.java");
    private static int checks;
    private static int digestCases;

    public static void main(String[] args) throws Exception {
        hashCorpus();
        Path work = Files.createTempDirectory("m3-source-seal-");
        try (StandardJavaFileManager files = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            sharedSource(files, work);
            editedSource(files, work, false);
            editedSource(files, work, true);
            duplicateUri(files, work);
            captureFailure(files, work);
        }
        System.out.println("M3_AST_SOURCE_SEAL checks=" + checks
                + " digestCases=" + digestCases + " codeUnits=65536");
    }

    /** Calls the actual private digest owner; arbitrary UTF-16 need not be valid Java syntax. */
    private static void hashCorpus() throws Exception {
        Method retained = M3ASTPC.class.getDeclaredMethod("retained", URI.class, String.class);
        retained.setAccessible(true);
        for (String text : List.of("", "a", "ASCII\r\ntext", "\u03bb\u6f22\u5b57")) {
            checkDigest(retained, text);
        }
        char[] all = new char[Character.MAX_VALUE + 1];
        for (int index = 0; index < all.length; index++) {
            all[index] = (char) index;
        }
        checkDigest(retained, new String(all));
        for (int length : new int[] {1, 2, 4095, 4096, 4097, 8191, 8192, 8193,
                16383, 16384, 16385, 65535, 65536, 65537}) {
            char[] text = new char[length];
            for (int index = 0; index < text.length; index++) {
                text[index] = (char) ((index * 257 + length) & 0xffff);
            }
            checkDigest(retained, new String(text));
        }
        checkDigest(retained, new String(new char[] {0xd800}));
        checkDigest(retained, new String(new char[] {0xdc00}));
        checkDigest(retained, new String(new char[] {0xd800, 'x', 0xdc00, 0, 0xffff}));
        char[] split = new char[8193];
        Arrays.fill(split, 'a');
        split[4095] = 0xd83d;
        split[4096] = 0xde00;
        split[8191] = 0xd800;
        split[8192] = 0xdc00;
        checkDigest(retained, new String(split));
    }

    private static void checkDigest(Method retained, String text) throws Exception {
        M3AST.Source result;
        try {
            result = (M3AST.Source) retained.invoke(null, HASH_URI, text);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error error) throw error;
            if (cause instanceof Exception exception) throw exception;
            throw new AssertionError(cause);
        }
        check(result.uri().equals(HASH_URI), "digest keeps source URI");
        check(result.text() == text, "digest keeps the existing immutable String owner");
        check(result.utf16Sha256().equals(scalarDigest(text)), "exact scalar digest parity");
        digestCases++;
    }

    /** The unchanged scalar algorithm is the differential oracle for exact code-unit bytes. */
    private static String scalarDigest(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static final class Source extends SimpleJavaFileObject {
        String text;
        int reads;
        final RuntimeException captureFailure;

        Source(String name, String text) {
            this(name, text, null);
        }

        Source(String name, String text, RuntimeException captureFailure) {
            super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
            this.text = text;
            this.captureFailure = captureFailure;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            if (captureFailure == null) return new StringBuilder(text);
            return new CharSequence() {
                @Override public int length() { return text.length(); }
                @Override public char charAt(int index) { return text.charAt(index); }
                @Override public CharSequence subSequence(int start, int end) {
                    return text.subSequence(start, end);
                }
                @Override public String toString() { throw captureFailure; }
            };
        }
    }

    private static JavacTask task(StandardJavaFileManager files, Path work,
                                  Source... sources) {
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files,
                new DiagnosticCollector<>(), List.of("-proc:none", "-d", work.toString()),
                null, List.of(sources));
    }

    private static void sharedSource(StandardJavaFileManager files, Path work) throws Exception {
        Source input = new Source("SharedSeal", "/* \u03bb */\r\nclass SharedSeal {} class SharedPeer {}");
        JavacTask task = task(files, work, input);
        M3AST session = M3AST.open(task);
        CompilationUnitTree[] original = new CompilationUnitTree[1];
        M3AST.Source[] first = new M3AST.Source[1];
        M3AST.Projection<M3AST.Source> key = new M3AST.Projection<>("sealed-source", M3AST.Unit::source);
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                original[0] = unit.tree();
                first[0] = unit.project(key);
                check(first[0].text().equals(input.text), "PARSE retains the exact characters");
                check(first[0].utf16Sha256().equals(scalarDigest(input.text)), "pipeline digest matches oracle");
            }
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                first[0] = unit.project(key);
            }
        });
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                check(unit.tree() == original[0], "consumers share the actual parsed tree");
                check(unit.project(key) == first[0], "consumers share the PARSE source projection");
            }
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                check(unit.project(key) == first[0], "consumers share each ANALYZE source projection");
            }
        });
        check(task.call(), "shared source compiles");
        M3AST.Summary summary = session.summary();
        check(summary.parsedUnits() == 1 && summary.analyzedTypes() == 2, "one parse and two type events");
        check(summary.capturedSources() == 1 && summary.sourceReads() == 2 && input.reads == 3,
                "PARSE adds no read and ANALYZE keeps one validated read per event");
        check(summary.projectionComputations() == 3 && summary.projectionHits() == 3,
                "identity-key event-local memoization stays unchanged");
        check(summary.closed() && summary.completed() && !summary.consumerFailed(), "normal source cleanup");
    }

    private static void editedSource(StandardJavaFileManager files, Path work,
                                     boolean changeLength) throws Exception {
        String name = changeLength ? "LongEdit" : "EqualEdit";
        Source input = new Source(name, "class " + name + " { static final int VALUE = 42; }");
        JavacTask task = task(files, work, input);
        M3AST session = M3AST.open(task);
        int[] refusals = {0};
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                M3AST.Source frozen = unit.source();
                input.text = input.text.replace("42", changeLength ? "420" : "43");
                check(frozen.text().contains("42;"), "the parsed snapshot is immutable");
            }
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                IOException first = expect(IOException.class, unit::source);
                check(expect(IOException.class, unit::source) == first, "source refusal identity is memoized");
                check(first.getMessage().equals("M3AST source changed since parsing: " + input.toUri()),
                        "source edit retains the exact refusal message");
                refusals[0]++;
            }
        });
        check(task.call(), "handled source refusal preserves original compilation");
        M3AST.Summary summary = session.summary();
        check(refusals[0] == 1 && summary.sourceReads() == 1 && input.reads == 2,
                "edited source is read only once in the event");
        check(summary.completed() && summary.closed() && !summary.consumerFailed(),
                "a caught source request refusal stays local to its consumer");
    }

    private static void duplicateUri(StandardJavaFileManager files, Path work) throws Exception {
        Source first = new Source("Collision", "class Collision { static final int VALUE = 1; }");
        Source second = new Source("Collision", "class Collision { static final int VALUE = 2; }");
        check(first != second && !first.equals(second) && first.toUri().equals(second.toUri()),
                "distinct inputs exercise one URI with different equal-length characters");
        Path image = work.resolve("rejected-source.m3prim");
        check(!Files.exists(image), "image path starts absent");
        JavacTask task = task(files, work, first, second);
        M3AST session = M3AST.open(task);
        int[] finishes = {0};
        M3AST.Unit[] expired = new M3AST.Unit[1];
        session.addConsumer(M3PrimitiveImageCompiler.consumer(image, null, false));
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) { expired[0] = unit; }
            @Override public void finished(M3AST.Summary summary) {
                finishes[0]++;
                check(summary.consumerFailed() && summary.closed() && !summary.completed(),
                        "capture refusal closes before a later COMPILATION event");
            }
        });
        IllegalStateException failure = expect(IllegalStateException.class, task::call);
        check(failure.getMessage().equals("M3AST source changed within compilation: " + first.toUri()),
                "duplicate URI retains the original source-seal failure");
        M3AST.Summary summary = session.summary();
        check(summary.capturedSources() == 1 && summary.parsedUnits() == 1 && summary.analyzedTypes() == 0,
                "second capture is refused before a second parse or attribution");
        check(summary.compilerErrors() == 0 && summary.consumerFailed() && !summary.completed(),
                "zero compiler diagnostics cannot turn failed capture into success");
        check(!Files.exists(image), "failed capture cannot publish a partial primitive image");
        check(finishes[0] == 1, "capture refusal releases consumers once");
        expect(IllegalStateException.class, () -> expired[0].tree());
        session.close();
        check(finishes[0] == 1, "later close stays idempotent");
    }

    private static void captureFailure(StandardJavaFileManager files, Path work) throws Exception {
        IllegalStateException original = new IllegalStateException("fixture source freeze failure");
        IllegalArgumentException cleanup = new IllegalArgumentException("fixture first cleanup failure");
        IOException laterCleanup = new IOException("fixture later cleanup failure");
        Source input = new Source("BrokenCapture", "class BrokenCapture {}", original);
        Path image = work.resolve("rejected-freeze.m3prim");
        check(!Files.exists(image), "freeze-failure image path starts absent");
        JavacTask task = task(files, work, input);
        M3AST session = M3AST.open(task);
        int[] finishes = {0};
        session.addConsumer(M3PrimitiveImageCompiler.consumer(image, null, false));
        session.addConsumer(new M3AST.Consumer() {
            @Override public void finished(M3AST.Summary summary) {
                finishes[0]++;
                throw cleanup;
            }
        });
        session.addConsumer(new M3AST.Consumer() {
            @Override public void finished(M3AST.Summary summary) throws IOException {
                finishes[0]++;
                throw laterCleanup;
            }
        });
        session.addConsumer(new M3AST.Consumer() {
            @Override public void finished(M3AST.Summary summary) {
                finishes[0]++;
                check(summary.closed() && summary.consumerFailed(), "cleanup continues after failures");
            }
        });
        check(expect(IllegalStateException.class, task::call) == original,
                "capture rethrows the original failure object");
        check(original.getSuppressed().length == 1 && original.getSuppressed()[0] == cleanup,
                "first cleanup failure is suppressed on the original capture failure");
        check(cleanup.getSuppressed().length == 1 && cleanup.getSuppressed()[0] == laterCleanup,
                "later cleanup failure retains suppression order and identity");
        check(finishes[0] == 3 && session.summary().closed() && session.summary().consumerFailed(),
                "failed capture releases every consumer");
        check(session.summary().capturedSources() == 0 && session.summary().parsedUnits() == 0,
                "a failed freeze publishes no capture or parse event");
        check(!Files.exists(image), "source-freeze failure cannot publish an image");
        session.close();
        check(finishes[0] == 3, "failure cleanup remains idempotent");
    }

    @FunctionalInterface
    private interface Operation {
        void run() throws Exception;
    }

    private static <T extends Throwable> T expect(Class<T> type, Operation operation) throws Exception {
        try {
            operation.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type + " but got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
