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
 * @summary M3 consumers share the real compiler task, phase-local work and sealed source
 * @modules jdk.compiler/com.sun.source.util jdk.compiler/com.sun.tools.javac.m3
 * @run main M3ASTSharedFrontendTest
 */

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.m3.M3PrimitiveImageCompiler;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

public class M3ASTSharedFrontendTest {
    private static int checks;
    private static Path work;

    public static void main(String[] args) throws Exception {
        work = Files.createTempDirectory("m3-ast-proof-");
        sharing();
        lazyAndFailureMemoization();
        editedSource();
        separatePhases();
        callbackFailure();
        explicitCallbackClose();
        cleanupFailure();
        pluginAndImage();
        providerClassLoader();
        generatedSources();
        System.out.println("M3_AST_SHARED_FRONTEND checks=" + checks);
    }

    private static final class Source extends SimpleJavaFileObject {
        String text;
        int reads;
        Source(String name, String text) {
            super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignored) {
            reads++;
            return new StringBuilder(text);
        }
    }

    private static JavacTask task(Source source, String... extra) throws IOException {
        List<String> options = new ArrayList<>(List.of("-proc:none", "-d", work.toString()));
        options.addAll(List.of(extra));
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null,
                new DiagnosticCollector<>(), options, null, List.of(source));
    }

    private static void sharing() throws Exception {
        Source input = new Source("Shared", "class Shared { static final int X=6*7; String s=\"a\" + \"b\"; } class Second {}");
        JavacTask task = task(input);
        AtomicInteger parses = new AtomicInteger();
        task.addTaskListener(new TaskListener() {
            @Override public void finished(TaskEvent event) {
                if (event.getKind() == TaskEvent.Kind.PARSE) parses.incrementAndGet();
            }
        });
        M3AST session = M3AST.open(task, true);
        check(M3AST.open(task) == session, "one task owner");
        AtomicInteger computations = new AtomicInteger();
        IdentityHashMap<M3AST.Unit, M3AST.Source> observed = new IdentityHashMap<>();
        M3AST.Unit[] escaped = new M3AST.Unit[1];
        M3AST.Projection<M3AST.Source> key = new M3AST.Projection<>("source", unit -> {
            computations.incrementAndGet();
            return unit.source();
        });
        M3AST.Consumer first = new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                check(unit.phase() == M3AST.Phase.PARSED && unit.analyzedType() == null, "parse phase");
                observed.put(unit, unit.project(key));
                check(unit.source().text().equals(input.text), "actual frozen parser input");
                AtomicInteger concatenations = new AtomicInteger();
                new TreeScanner<Void, Void>() {
                    @Override public Void visitBinary(BinaryTree tree, Void unused) {
                        if (tree.getKind() == Tree.Kind.PLUS && tree.getLeftOperand() instanceof LiteralTree left
                                && tree.getRightOperand() instanceof LiteralTree right
                                && "a".equals(left.getValue()) && "b".equals(right.getValue()))
                            concatenations.incrementAndGet();
                        return super.visitBinary(tree, unused);
                    }
                }.scan(unit.tree(), null);
                check(concatenations.get() == 1, "string expression structure retained");
                check(CompletableFuture.supplyAsync(() -> {
                    try { unit.tree(); return false; }
                    catch (IllegalStateException expected) { return true; }
                }).join(), "cross-thread live view refused");
                escaped[0] = unit;
            }
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                check(unit.phase() == M3AST.Phase.ANALYZED && unit.analyzedType() != null, "per-type attribution");
                observed.put(unit, unit.project(key));
            }
        };
        session.addConsumer(first);
        expect(IllegalArgumentException.class, () -> session.addConsumer(first));
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                check(unit.project(key) == observed.get(unit), "same parse projection reused");
            }
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                check(unit.project(key) == observed.get(unit), "same analysis projection reused");
            }
        });
        check(task.call(), "real class compilation");
        M3AST.Summary result = session.summary();
        check(parses.get() == 1 && result.parsedUnits() == 1, "one parse, multiple consumers");
        check(result.analyzedTypes() == 2 && result.capturedSources() == 1, "one source, two type events");
        check(result.projectionComputations() == 3 && result.projectionHits() == 3, "event-local work sharing");
        check(computations.get() == 3 && result.sourceReads() == 2 && input.reads == 3, "no source reread at parse");
        check(result.closed() && result.completed() && !result.consumerFailed(), "complete cleanup");
        expect(IllegalStateException.class, () -> escaped[0].tree());
        expect(IllegalStateException.class, () -> session.addConsumer(new M3AST.Consumer() { }));
        session.close();
    }

    private static void lazyAndFailureMemoization() throws Exception {
        Source input = new Source("Lazy", "class Lazy {}");
        JavacTask task = task(input);
        M3AST session = M3AST.open(task);
        AtomicInteger calls = new AtomicInteger();
        M3AST.Projection<String> unused = new M3AST.Projection<>("unused", u -> { calls.incrementAndGet(); return "x"; });
        M3AST.Projection<String> nil = new M3AST.Projection<>("nil", u -> { calls.incrementAndGet(); return null; });
        IOException original = new IOException("fixture");
        M3AST.Projection<String> failed = new M3AST.Projection<>("failure", u -> { calls.incrementAndGet(); throw original; });
        List<M3AST.Projection<String>> recursive = new ArrayList<>();
        recursive.add(new M3AST.Projection<>("recursive", u -> u.project(recursive.get(0))));
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                check(unit.project(nil) == null && unit.project(nil) == null, "null result memoized");
                for (int i = 0; i < 2; i++) {
                    try { unit.project(failed); throw new AssertionError("failure lost"); }
                    catch (IOException failure) { check(failure == original, "exception identity memoized"); }
                }
                expect(IllegalStateException.class, () -> unit.project(recursive.get(0)));
                expect(IllegalStateException.class, () -> unit.project(recursive.get(0)));
            }
        });
        check(task.call(), "caught projection failures preserve compile");
        check(calls.get() == 2 && unused.name().equals("unused"), "unused factory not called");
        check(session.summary().sourceReads() == 0 && input.reads == 1, "unused source is not reread");
        check(!session.summary().consumerFailed(), "handled projection failure stays local");
    }

    private static void editedSource() throws Exception {
        Source source = new Source("Edited", "class Edited { static final int X = 42; }");
        JavacTask task = task(source);
        M3AST session = M3AST.open(task);
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                M3AST.Source frozen = unit.source();
                source.text = source.text.replace("42", "43");
                check(frozen.text().contains("42"), "source snapshot remains immutable");
            }
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                IOException first = expect(IOException.class, unit::source);
                check(expect(IOException.class, unit::source) == first, "stale source refusal memoized");
                check(first.getMessage().contains("changed since parsing"), "source seal controls admission");
            }
        });
        check(task.call(), "compile retains original parsed input");
    }

    private static void separatePhases() throws Exception {
        JavacTask task = task(new Source("Phases", "class Phases { int f() { return 3; } }"));
        M3AST session = M3AST.open(task);
        AtomicInteger finishes = new AtomicInteger();
        session.addConsumer(new M3AST.Consumer() {
            @Override public void finished(M3AST.Summary summary) { finishes.incrementAndGet(); }
        });
        task.parse(); task.analyze(); task.generate(); session.close(); session.close();
        check(session.summary().parsedUnits() == 1 && session.summary().analyzedTypes() == 1, "split API lifecycle");
        check(!session.summary().completed() && session.summary().closed(), "no invented compilation event");
        check(finishes.get() == 1, "one explicit cleanup");
    }

    private static void callbackFailure() throws Exception {
        JavacTask task = task(new Source("Failure", "class Failure {}"));
        M3AST session = M3AST.open(task);
        M3AST.Unit[] view = new M3AST.Unit[1];
        AtomicInteger finish = new AtomicInteger();
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) { view[0] = unit; throw new IllegalArgumentException("fixture consumer"); }
            @Override public void finished(M3AST.Summary summary) { finish.incrementAndGet(); }
        });
        expect(RuntimeException.class, task::call);
        check(session.summary().closed() && session.summary().consumerFailed(), "failure closes session");
        check(finish.get() == 1, "failure closes consumer once");
        expect(IllegalStateException.class, () -> view[0].tree());
    }

    private static void explicitCallbackClose() throws Exception {
        JavacTask task = task(new Source("ClosedInside", "class ClosedInside {}"));
        M3AST session = M3AST.open(task);
        AtomicInteger later = new AtomicInteger();
        AtomicInteger finishes = new AtomicInteger();
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                session.close();
                expect(IllegalStateException.class, unit::tree);
            }
        });
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) { later.incrementAndGet(); }
            @Override public void finished(M3AST.Summary summary) { finishes.incrementAndGet(); }
        });
        check(task.call(), "explicit callback close preserves compiler completion");
        check(later.get() == 0 && finishes.get() == 1, "close stops dispatch and releases every consumer");
        check(session.summary().closed() && !session.summary().completed(), "partial close is explicit");
        expect(IllegalStateException.class, () -> M3AST.open(task));
    }

    private static void cleanupFailure() throws Exception {
        JavacTask task = task(new Source("Cleanup", "class Cleanup {}"));
        M3AST session = M3AST.open(task);
        IllegalStateException first = new IllegalStateException("first cleanup failure");
        IOException second = new IOException("second cleanup failure");
        session.addConsumer(new M3AST.Consumer() {
            @Override public void finished(M3AST.Summary summary) { throw first; }
        });
        session.addConsumer(new M3AST.Consumer() {
            @Override public void finished(M3AST.Summary summary) throws IOException {
                check(summary.consumerFailed(), "later cleanup sees preceding consumer failure");
                throw second;
            }
        });
        check(expect(IllegalStateException.class, session::close) == first, "first failure preserved");
        check(first.getSuppressed().length == 1 && first.getSuppressed()[0] == second,
                "later cleanup failure suppressed");
        check(session.summary().closed() && session.summary().consumerFailed(), "cleanup failure releases owner");
        session.close();
    }

    private static void pluginAndImage() throws Exception {
        String text = "class Image { static final int V = 6 * 7; static final double D = -0.0; int f(int n) { return n + 9; } } class Other { static final long L = 4L; }";
        Path source = work.resolve("Image.java");
        Files.writeString(source, text);
        for (boolean fold : new boolean[] { false, true }) {
            Path legacy = work.resolve("legacy-" + fold + ".m3prim");
            Path shared = work.resolve("shared-" + fold + ".m3prim");
            Path trace = work.resolve("trace-" + fold + ".tsv");
            List<String> cli = new ArrayList<>(List.of("--out", legacy.toString()));
            if (fold) cli.add("--fold-expressions");
            cli.add(source.toString());
            M3PrimitiveImageCompiler.main(cli.toArray(String[]::new));
            JavacTask task = task(new Source("Image", text), "-Xplugin:M3AST trace=" + trace
                    + " image=" + shared + (fold ? " fold-expressions" : ""));
            check(task.call(), "service-loaded product plugin");
            check(Arrays.equals(Files.readAllBytes(legacy), Files.readAllBytes(shared)), "same primitive image without independent task");
            String events = Files.readString(trace);
            check(events.contains("parsed=1 analyzed=2") && events.contains("completed=true"), "actual task trace");
        }
        Path rejected = work.resolve("rejected.m3prim");
        Path trace = work.resolve("failed.tsv");
        check(!task(new Source("Bad", "class Bad { int n = missing; }"),
                "-Xplugin:M3AST image=" + rejected + " trace=" + trace).call(), "compiler errors retained");
        check(!Files.exists(rejected) && Files.readString(trace).contains("errors=1"), "no image on rejected compilation");
        Path partial = work.resolve("partial.m3prim");
        JavacTask manual = task(new Source("Manual", "class Manual { static final int X = 1; }"));
        M3AST session = M3AST.open(manual);
        session.addConsumer(M3PrimitiveImageCompiler.consumer(partial, null, false));
        manual.parse(); manual.analyze(); session.close();
        check(!Files.exists(partial), "partial API close cannot publish incomplete image");
        Path collision = work.resolve("collision.tsv");
        expect(RuntimeException.class, () -> task(new Source("Collision", "class Collision {}"),
                "-Xplugin:M3AST image=" + collision + " trace=" + collision).call());
        check(!Files.exists(collision), "trace/image collision refused before opening files");
    }

    private static void providerClassLoader() throws Exception {
        Path provider = Files.createDirectory(work.resolve("provider"));
        String property = "m3.ast.provider." + System.nanoTime();
        String implementation = "import com.sun.source.util.M3AST; public class Provider implements M3AST.Consumer {"
                + "public void analyzed(M3AST.Unit u) { System.setProperty(\"" + property + "\", u.analyzedType().getSimpleName().toString()); }}";
        JavacTask compile = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null,
                new DiagnosticCollector<>(), List.of("-proc:none", "-d", provider.toString()), null,
                List.of(new Source("Provider", implementation)));
        check(compile.call(), "real external provider compilation");
        Path descriptor = provider.resolve("META-INF/services/com.sun.source.util.M3AST$Consumer");
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, "Provider\n");
        try {
            check(task(new Source("ServiceInput", "class ServiceInput {}"),
                    "-processorpath", provider.toString(), "-Xplugin:M3AST").call(), "actual plugin loader compilation");
            check("ServiceInput".equals(System.getProperty(property)), "consumer found on unchanged processor path");
        } finally { System.clearProperty(property); }
    }

    private static void generatedSources() throws Exception {
        AtomicInteger generations = new AtomicInteger();
        AbstractProcessor processor = new AbstractProcessor() {
            @Override public Set<String> getSupportedAnnotationTypes() { return Set.of("*"); }
            @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }
            @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
                if (!round.processingOver() && generations.get() == 0) {
                    try (var writer = processingEnv.getFiler().createSourceFile("GeneratedPeer").openWriter()) {
                        writer.write("class GeneratedPeer { static final int V = 42; }");
                        generations.incrementAndGet();
                    } catch (IOException failure) { throw new RuntimeException(failure); }
                }
                return false;
            }
        };
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null,
                new DiagnosticCollector<>(), List.of("-d", work.toString(), "-s", work.toString()), null,
                List.of(new Source("ProcessorInput", "class ProcessorInput { int value = GeneratedPeer.V; }")));
        task.setProcessors(List.of(processor));
        M3AST session = M3AST.open(task);
        AtomicInteger retained = new AtomicInteger();
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws IOException {
                check(unit.source().text().contains("class "), "generated and input source bound to real parse");
                retained.incrementAndGet();
            }
        });
        check(task.call(), "actual annotation processor and generated source compile");
        check(generations.get() == 1 && retained.get() == 2, "no additional processor run or source parse");
        check(session.summary().parsedUnits() == 2 && session.summary().capturedSources() == 2
                && session.summary().sourceReads() == 0, "generated sources participate in normal capture");
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }
    private static <T extends Throwable> T expect(Class<T> type, Operation operation) throws Exception {
        try { operation.run(); }
        catch (Throwable failure) {
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
