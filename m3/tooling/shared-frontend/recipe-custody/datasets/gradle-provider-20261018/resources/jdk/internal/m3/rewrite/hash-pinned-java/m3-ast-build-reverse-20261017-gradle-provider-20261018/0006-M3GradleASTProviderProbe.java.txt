/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.gradle.internal.m3.ast;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.gradle.api.internal.tasks.compile.M3JavacASTProvider;

/** Differential runtime probe using the real javac task, source objects and provider SPI. */
public final class M3GradleASTProviderProbe {
    static final String SOURCE = "public final class Sample { "
            + "static final String MARKER = \"a\" + \"b\"; "
            + "public static int answer() { return Generated.VALUE + Helper.ONE; } "
            + "public static void main(String[] args) { System.out.println(answer()); } } "
            + "final class Helper { static final int ONE = 1; }";
    static final ThreadLocal<State> ACTIVE = new ThreadLocal<>();

    private M3GradleASTProviderProbe() { }

    record DiagnosticFact(Diagnostic.Kind kind, String code, URI source, boolean originalSource,
                          long position, long start, long end, long line, long column, String message) { }

    record Run(boolean success, int inputReads, int compilations, int parsedEvents,
               int processorRounds, int generatedSources, int instances, int firstParsed,
               int secondParsed, int analyzedCallbacks, int conversions, int stringConcatenations,
               List<M3AST.Summary> summaries, List<DiagnosticFact> diagnostics,
               Map<String, String> classHashes, Path output, Throwable failure) { }

    /** Detached projection; no compiler-owned tree, element, type or task survives in it. */
    private record ProjectionFact(URI source, String sourceHash, int length) { }

    static final class State {
        final boolean failParsed;
        final boolean failFinished;
        int conversions;
        final M3AST.Projection<ProjectionFact> projection = new M3AST.Projection<>("probe-source", unit -> {
            conversions++;
            M3AST.Source source = unit.source();
            return new ProjectionFact(source.uri(), source.utf16Sha256(), source.text().length());
        });
        final List<M3AST.Summary> summaries = new ArrayList<>();
        Trees expectedTrees;
        CompilationUnitTree expectedTree;
        Object firstContext;
        ProjectionFact firstProjection;
        int instances;
        int firstParsed;
        int secondParsed;
        int analyzedCallbacks;
        int compilations;
        int parsedEvents;
        int stringConcatenations;

        State(boolean failParsed, boolean failFinished) {
            this.failParsed = failParsed;
            this.failFinished = failFinished;
        }

        void parsed(M3AST.Unit unit, boolean first) throws Exception {
            require(expectedTrees == unit.trees(), "consumer did not receive original compiler Trees");
            require(expectedTree == unit.tree(), "consumer did not receive original PARSE tree");
            if (first) {
                firstParsed++;
                if (failParsed) {
                    throw new IOException("probe parsed failure");
                }
                new TreeScanner<Void, Void>() {
                    @Override public Void visitBinary(BinaryTree tree, Void ignored) {
                        if (tree.getLeftOperand() instanceof LiteralTree left && "a".equals(left.getValue())
                                && tree.getRightOperand() instanceof LiteralTree right && "b".equals(right.getValue())) {
                            stringConcatenations++;
                        }
                        return super.visitBinary(tree, ignored);
                    }
                }.scan(unit.tree(), null);
                firstContext = unit.contextIdentity();
                firstProjection = unit.project(projection);
            } else {
                secondParsed++;
                require(firstContext == unit.contextIdentity(), "consumer compiler context changed");
                require(firstProjection == unit.project(projection), "shared projection was rebuilt");
                firstContext = null;
                firstProjection = null;
                expectedTree = null;
            }
        }

        void finished(M3AST.Summary summary) throws IOException {
            summaries.add(summary);
            if (failFinished) {
                throw new IOException("probe finished failure");
            }
        }

        void release() {
            expectedTrees = null;
            expectedTree = null;
            firstContext = null;
            firstProjection = null;
        }
    }

    /** First independently discovered consumer, retaining only the current probe's state. */
    public static final class FirstConsumer implements M3AST.Consumer {
        private final State state = current();

        public FirstConsumer() { state.instances++; }

        @Override public void parsed(M3AST.Unit unit) throws Exception { state.parsed(unit, true); }
        @Override public void analyzed(M3AST.Unit unit) { state.analyzedCallbacks++; }
        @Override public void finished(M3AST.Summary summary) throws IOException { state.finished(summary); }
    }

    /** Second independently discovered consumer, requesting the exact same projection identity. */
    public static final class SecondConsumer implements M3AST.Consumer {
        private final State state = current();

        public SecondConsumer() { state.instances++; }

        @Override public void parsed(M3AST.Unit unit) throws Exception { state.parsed(unit, false); }
        @Override public void analyzed(M3AST.Unit unit) { state.analyzedCallbacks++; }
        @Override public void finished(M3AST.Summary summary) throws IOException { state.finished(summary); }
    }

    /** A real unrelated javac plugin whose longer name must not select M3AST discovery. */
    public static final class UnrelatedPlugin implements Plugin {
        public UnrelatedPlugin() { }
        @Override public String getName() { return "M3ASTIndexProbe"; }
        @Override public void init(JavacTask task, String... arguments) { }
    }

    static final class Input extends SimpleJavaFileObject {
        final AtomicInteger reads = new AtomicInteger();
        private final String source;

        Input(String source) {
            super(URI.create("memory:///Sample.java"), Kind.SOURCE);
            this.source = source;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads.incrementAndGet();
            return source;
        }
    }

    @SupportedAnnotationTypes("*")
    @SupportedSourceVersion(SourceVersion.RELEASE_21)
    static final class Generator extends AbstractProcessor {
        int rounds;
        int generated;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
            rounds++;
            if (!environment.processingOver() && generated == 0) {
                generated++;
                try (Writer writer = processingEnv.getFiler().createSourceFile("Generated").openWriter()) {
                    writer.write("final class Generated { static final int VALUE = 41; }");
                } catch (IOException failure) {
                    throw new IllegalStateException(failure);
                }
            }
            return false;
        }
    }

    static Run compile(Path output, boolean selected, List<String> additional,
                       String sourceText, boolean failParsed, boolean failFinished) throws Exception {
        Files.createDirectories(output);
        State state = new State(failParsed, failFinished);
        require(ACTIVE.get() == null, "nested compiler probe");
        ACTIVE.set(state);
        Input source = new Input(sourceText);
        Generator processor = new Generator();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "JDK compiler unavailable");
        List<String> options = new ArrayList<>(List.of("--release", "21", "-g:none", "-d", output.toString(),
                "-classpath", System.getProperty("m3.gradle.provider.classpath", System.getProperty("java.class.path"))));
        options.addAll(additional);
        boolean success = false;
        Throwable failure = null;
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            M3JavacASTProvider.M3Compilation owned = selected
                    ? new M3GradleASTProvider().prepare(List.of(source), StandardCharsets.UTF_8,
                            options, manager, () -> List.copyOf(diagnostics.getDiagnostics())) : null;
            try {
                JavacTask original = (JavacTask) compiler.getTask(null, manager, diagnostics, options,
                        null, owned == null ? List.of(source) : owned.sources());
                state.expectedTrees = Trees.instance(original);
                original.addTaskListener(new TaskListener() {
                    @Override public void started(TaskEvent event) {
                        if (event.getKind() == TaskEvent.Kind.COMPILATION) {
                            state.compilations++;
                        }
                    }

                    @Override public void finished(TaskEvent event) {
                        if (event.getKind() == TaskEvent.Kind.PARSE) {
                            state.parsedEvents++;
                            state.expectedTree = event.getCompilationUnit();
                        }
                    }
                });
                M3AST existing = owned == null ? null : M3AST.open(original);
                JavaCompiler.CompilationTask execution = owned == null ? original
                        : owned.attach(original, () -> (int) diagnostics.getDiagnostics().stream()
                                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR).count());
                if (owned != null) {
                    require(existing == M3AST.open(original), "provider replaced the existing compiler session");
                }
                execution.setLocale(Locale.ROOT);
                execution.addModules(List.of("java.logging"));
                execution.setProcessors(List.of(processor));
                success = execution.call();
            } catch (RuntimeException | Error thrown) {
                failure = thrown;
            } finally {
                if (owned != null) {
                    owned.close();
                }
            }
        } finally {
            ACTIVE.remove();
            state.release();
        }
        List<DiagnosticFact> facts = diagnostics.getDiagnostics().stream().map(diagnostic ->
                new DiagnosticFact(diagnostic.getKind(), diagnostic.getCode(),
                        diagnostic.getSource() == null ? null : diagnostic.getSource().toUri(),
                        diagnostic.getSource() == source, diagnostic.getPosition(), diagnostic.getStartPosition(),
                        diagnostic.getEndPosition(), diagnostic.getLineNumber(), diagnostic.getColumnNumber(),
                        diagnostic.getMessage(Locale.ROOT))).toList();
        return new Run(success, source.reads.get(), state.compilations, state.parsedEvents,
                processor.rounds, processor.generated, state.instances, state.firstParsed,
                state.secondParsed, state.analyzedCallbacks, state.conversions, state.stringConcatenations,
                List.copyOf(state.summaries), facts, classHashes(output), output, failure);
    }

    static void verifyPair(Run baseline, Run selected) {
        require(baseline.success && baseline.failure == null, "ordinary compilation failed: " + baseline.failure);
        require(selected.success && selected.failure == null, "provider compilation failed: " + selected.failure);
        require(baseline.classHashes.equals(selected.classHashes), "compiler class bytes changed");
        require(baseline.diagnostics.equals(selected.diagnostics), "compiler diagnostics changed");
        require(baseline.inputReads == 1 && selected.inputReads == 1, "original source was read again");
        require(baseline.compilations == 1 && selected.compilations == 1, "compiler executed more than once");
        require(baseline.parsedEvents == 2 && selected.parsedEvents == 2, "original/generated source parse counts changed");
        require(baseline.processorRounds == selected.processorRounds && selected.generatedSources == 1,
                "annotation processing changed");
        require(selected.instances == 2, "consumer discovery did not have one owner");
        require(selected.firstParsed == 2 && selected.secondParsed == 2, "both consumers did not see both original trees");
        require(selected.conversions == 2, "projection was rebuilt for a second consumer");
        require(selected.analyzedCallbacks == 6, "both consumers did not see all three attributed types");
        require(selected.summaries.size() == 2, "consumers did not finish exactly once");
        for (M3AST.Summary summary : selected.summaries) {
            require(summary.parsedUnits() == 2 && summary.capturedSources() == 2, "compiler source capture mismatch");
            require(summary.sourceReads() == 0, "consumer requested extra source reads");
            require(summary.projectionComputations() == 2 && summary.projectionHits() == 2,
                    "shared projection counters mismatch");
            require(summary.completed() && summary.closed() && !summary.consumerFailed() && summary.compilerErrors() == 0,
                    "compiler session did not finish successfully");
        }
    }

    static Map<String, String> classHashes(Path directory) throws Exception {
        Map<String, String> hashes = new TreeMap<>();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".class")).sorted().toList()) {
                hashes.put(directory.relativize(path).toString(), HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }
        }
        return Map.copyOf(hashes);
    }

    static int executeAnswer(Path classes) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { classes.toUri().toURL() }, null)) {
            return (Integer) loader.loadClass("Sample").getMethod("answer").invoke(null);
        }
    }

    private static State current() {
        return java.util.Objects.requireNonNull(ACTIVE.get(), "consumer constructed outside compiler invocation");
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Executes a fresh ordinary/selected compilation pair and its unchanged output. */
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one fresh proof directory");
        }
        Path output = Path.of(arguments[0]).toAbsolutePath();
        Files.createDirectory(output);
        Run ordinary = compile(output.resolve("ordinary"), false, List.of(), SOURCE, false, false);
        Run selected = compile(output.resolve("selected"), true, List.of(), SOURCE, false, false);
        verifyPair(ordinary, selected);
        require(executeAnswer(ordinary.output) == 42 && executeAnswer(selected.output) == 42,
                "original/selected runtime results changed");
        String receipt = "{\"schema\":\"M3_GRADLE_PROVIDER_RUNTIME_V1\",\"status\":\"PASS\","
                + "\"compilerInvocations\":" + selected.compilations + ",\"parsedUnits\":" + selected.parsedEvents
                + ",\"consumers\":" + selected.instances + ",\"projectionComputations\":" + selected.conversions
                + ",\"projectionHits\":" + selected.summaries.getFirst().projectionHits()
                + ",\"extraSourceReads\":" + selected.summaries.getFirst().sourceReads() + ","
                + "\"classByteParity\":true,\"runtimeAnswer\":42,\"fullGradleDistribution\":false}\n";
        Files.writeString(output.resolve("RECEIPT.json"), receipt, StandardCharsets.UTF_8);
        System.out.print(receipt);
    }
}
