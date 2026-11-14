/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

/* @test
 * @summary Primitive images wait for all ordinary M3 consumers to finish successfully
 * @modules jdk.compiler/com.sun.source.util jdk.compiler/com.sun.tools.javac.m3
 * @run main/othervm M3PrimitiveImageCompletionTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.tools.javac.m3.M3ASTPC;
import com.sun.tools.javac.m3.M3PrimitiveImageCompiler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class M3PrimitiveImageCompletionTest {
    private static final String SOURCE = """
            class Program {
                static final int N = 6 * 7;
                static final long L = 9007199254740993L;
                static final double D = -0.0d;
                int value() { return N + 1; }
            }
            """;
    private static final byte[] OLD_IMAGE = { 11, 22, 33 };
    private static final byte[] OLD_SIDECAR = { 44, 55, 66 };
    private static Path work;
    private static int checks;

    public static void main(String[] args) throws Exception {
        work = Files.createTempDirectory("m3-image-completion-");
        lateFailure(false, false);
        lateFailure(true, false);
        lateFailure(false, true);
        lateFailure(true, true);
        earlierFailure();
        multipleImages();
        successParity(false, false);
        successParity(false, true);
        successParity(true, false);
        successParity(true, true);
        incompleteCompilation();
        rejectedCompilation();
        imagePublicationFailure();
        System.out.println("M3_PRIMITIVE_IMAGE_COMPLETION checks=" + checks);
    }

    private static void lateFailure(boolean plugin, boolean existing) throws Exception {
        String name = "late-" + plugin + "-" + existing;
        try (Fixture fixture = new Fixture(name, SOURCE)) {
            Path image = fixture.directory.resolve("constants.m3prim");
            Path sidecar = fixture.directory.resolve("constants.tsv");
            if (existing) {
                Files.write(image, OLD_IMAGE);
                Files.write(sidecar, OLD_SIDECAR);
            }
            List<String> order = new ArrayList<>();
            IllegalStateException first = new IllegalStateException("late completion failure");
            IOException second = new IOException("second completion failure");
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) {
                    order.add("before");
                    check(summary.completed() && summary.closed(), "completion state before image");
                    check(!summary.consumerFailed(), "no failure before late callback");
                }
            });
            register(fixture, image, sidecar, true, plugin);
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) {
                    order.add("first failure");
                    throw first;
                }
            });
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) throws IOException {
                    order.add("second failure");
                    check(summary.consumerFailed(), "late callback observes preceding failure");
                    throw second;
                }
            });
            Throwable thrown = expectFailure(fixture.task);
            check(hasCause(thrown, first), "original first failure retained");
            check(Arrays.equals(first.getSuppressed(), new Throwable[] { second }),
                    "later failure suppressed once in registration order");
            check(order.equals(List.of("before", "first failure", "second failure")),
                    "ordinary completion callback order");
            check(fixture.session.summary().consumerFailed(), "failed session recorded");
            if (existing) {
                check(Arrays.equals(Files.readAllBytes(image), OLD_IMAGE),
                        "late finished failure replaced a prior primitive image");
                check(Arrays.equals(Files.readAllBytes(sidecar), OLD_SIDECAR),
                        "late finished failure replaced a prior primitive sidecar");
            } else {
                check(!Files.exists(image), "late finished failure published a primitive image");
                check(!Files.exists(sidecar),
                        "late finished failure published a primitive sidecar");
            }
            fixture.session.close();
            check(order.size() == 3, "failed session close is idempotent");
        }
    }

    private static void earlierFailure() throws Exception {
        try (Fixture fixture = new Fixture("early", SOURCE)) {
            Path image = fixture.directory.resolve("constants.m3prim");
            IllegalStateException failure = new IllegalStateException("early completion failure");
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) { throw failure; }
            });
            register(fixture, image, null, false, false);
            check(hasCause(expectFailure(fixture.task), failure), "early failure identity");
            check(!Files.exists(image), "early failure suppresses image-only output");
        }
    }

    private static void multipleImages() throws Exception {
        try (Fixture fixture = new Fixture("multiple", SOURCE)) {
            Path first = fixture.directory.resolve("first.m3prim");
            Path second = fixture.directory.resolve("second.m3prim");
            register(fixture, first, null, false, false);
            register(fixture, second, null, true, false);
            IllegalStateException failure = new IllegalStateException("after both images");
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) { throw failure; }
            });
            check(hasCause(expectFailure(fixture.task), failure),
                    "multiple-image failure identity");
            check(!Files.exists(first) && !Files.exists(second),
                    "all built-in images await completion");
        }
    }

    private static void successParity(boolean fold, boolean plugin) throws Exception {
        try (Fixture fixture = new Fixture("success-" + fold + "-" + plugin, SOURCE)) {
            Path image = fixture.directory.resolve("constants.m3prim");
            Path sidecar = fixture.directory.resolve("constants.tsv");
            Path expectedImage = fixture.directory.resolve("expected.m3prim");
            Path expectedSidecar = fixture.directory.resolve("expected.tsv");
            List<String> arguments = new ArrayList<>(List.of("--out", expectedImage.toString(),
                    "--slots", expectedSidecar.toString()));
            if (fold) arguments.add("--fold-expressions");
            arguments.add(fixture.source.toString());
            M3PrimitiveImageCompiler.main(arguments.toArray(String[]::new));
            register(fixture, image, sidecar, fold, plugin);
            int[] completions = { 0 };
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) {
                    completions[0]++;
                    check(!summary.consumerFailed(), "successful callback state");
                    check(!Files.exists(image) && !Files.exists(sidecar),
                            "image is unpublished during ordinary completion callbacks");
                }
            });
            check(fixture.task.call(), "successful compilation");
            check(Arrays.equals(Files.readAllBytes(image), Files.readAllBytes(expectedImage)),
                    "successful image bytes preserve standalone compiler output");
            check(Arrays.equals(Files.readAllBytes(sidecar), Files.readAllBytes(expectedSidecar)),
                    "successful sidecar bytes preserve standalone compiler output");
            check(fixture.session.summary().parsedUnits() == 1
                    && fixture.session.summary().analyzedTypes() == 1,
                    "existing parse and attribution are reused");
            fixture.session.close();
            check(completions[0] == 1, "successful session closes once");
        }
    }

    private static void incompleteCompilation() throws Exception {
        try (Fixture fixture = new Fixture("incomplete", SOURCE)) {
            Path image = fixture.directory.resolve("constants.m3prim");
            register(fixture, image, null, true, false);
            fixture.task.parse();
            fixture.task.analyze();
            fixture.session.close();
            check(!fixture.session.summary().completed(), "explicit phases remain incomplete");
            check(!Files.exists(image), "incomplete compilation cannot publish");
        }
    }

    private static void rejectedCompilation() throws Exception {
        try (Fixture fixture = new Fixture("rejected",
                "class Program { int value() { return \"bad\"; } }")) {
            Path image = fixture.directory.resolve("constants.m3prim");
            register(fixture, image, null, false, true);
            check(!fixture.task.call(), "invalid source rejected");
            check(fixture.session.summary().compilerErrors() > 0, "compiler errors retained");
            check(!Files.exists(image), "compiler errors cannot publish");
        }
    }

    private static void imagePublicationFailure() throws Exception {
        try (Fixture fixture = new Fixture("publication-failure", SOURCE)) {
            Path image = fixture.directory.resolve("blocked/constants.m3prim");
            Path second = fixture.directory.resolve("second.m3prim");
            register(fixture, image, null, false, false);
            register(fixture, second, null, true, false);
            fixture.session.addConsumer(new M3AST.Consumer() {
                @Override public void finished(M3AST.Summary summary) throws IOException {
                    Files.writeString(image.getParent(), "not a directory");
                }
            });
            Throwable failure = expectFailure(fixture.task);
            check(hasType(failure, IOException.class),
                    "image publication IO failure remains visible");
            check(fixture.session.summary().consumerFailed(), "image publication marks failure");
            check(!Files.exists(image) && !Files.exists(second),
                    "image publication failure stops later images");
        }
    }

    private static void register(Fixture fixture, Path image, Path sidecar,
                                 boolean fold, boolean plugin) throws Exception {
        if (plugin) {
            List<String> options = new ArrayList<>(List.of("image=" + image));
            if (sidecar != null) options.add("slots=" + sidecar);
            if (fold) options.add("fold-expressions");
            new M3ASTPC().init(fixture.task, options.toArray(String[]::new));
        } else {
            fixture.session.addConsumer(M3PrimitiveImageCompiler.consumer(image, sidecar, fold));
        }
    }

    private static Throwable expectFailure(JavacTask task) {
        try {
            task.call();
        } catch (RuntimeException | Error failure) {
            return failure;
        }
        throw new AssertionError("consumer failure did not fail compilation");
    }

    private static boolean hasCause(Throwable thrown, Throwable expected) {
        for (Throwable at = thrown; at != null; at = at.getCause()) {
            if (at == expected) return true;
        }
        return false;
    }

    private static boolean hasType(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable at = thrown; at != null; at = at.getCause()) {
            if (type.isInstance(at)) return true;
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture implements AutoCloseable {
        final Path directory;
        final Path source;
        final StandardJavaFileManager files;
        final JavacTask task;
        final M3AST session;

        Fixture(String name, String text) throws IOException {
            directory = Files.createDirectory(work.resolve(name));
            Path classes = Files.createDirectory(directory.resolve("classes"));
            source = Files.writeString(directory.resolve("Program.java"), text);
            var compiler = ToolProvider.getSystemJavaCompiler();
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
            task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "-implicit:none", "-d", classes.toString()), null,
                    files.getJavaFileObjectsFromPaths(List.of(source)));
            session = M3AST.open(task);
        }

        @Override public void close() throws IOException {
            try { session.close(); }
            finally { files.close(); }
        }
    }
}
