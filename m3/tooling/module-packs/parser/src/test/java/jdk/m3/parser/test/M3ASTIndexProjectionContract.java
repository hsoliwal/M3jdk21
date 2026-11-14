/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3JavacParser;

/** Real original-task events qualify the new detached index projection. */
public final class M3ASTIndexProjectionContract {
    private static int checks;
    private M3ASTIndexProjectionContract() { }
    public static void main(String[] arguments) throws Exception {
        sharedSourceSeal();
        independentGenerations();
        failureMemoization();
        analyzedRefusal();
        System.out.println("M3_AST_INDEX_PROJECTION_PASS checks=" + checks + " groups=4");
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        private int reads;
        Source(String text) { super(URI.create("memory:/Demo.java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignored) { reads++; return new StringBuilder(text); }
    }
    private static JavacTask task(Source source, StandardJavaFileManager files,
                                  DiagnosticCollector<JavaFileObject> diagnostics) {
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files, diagnostics,
                List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(source));
    }

    private static void sharedSourceSeal() throws Exception {
        Source source = new Source("/* \ud83d\ude00 */\r\nclass Demo { static final int answer = 42; String text = \"a\" + \"b\"; }\r\n");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        M3ASTIndex[] values = new M3ASTIndex[2];
        M3AST.Source[] sealed = new M3AST.Source[1];
        M3AST.Unit[] escaped = new M3AST.Unit[1];
        M3AST.Summary summary;
        try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(source, files, diagnostics);
            M3AST.Projection<?> key = projection(M3ASTIngress.Limits.defaults());
            try (M3AST session = attach(task, key, index -> values[0] = index)) {
                check(attach(task, key, index -> values[1] = index) == session, "same actual task reuses existing event owner");
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        sealed[0] = unit.source(); escaped[0] = unit;
                    }
                });
                task.parse();
                summary = session.summary();
                check(summary.parsedUnits() == 1 && summary.capturedSources() == 1
                        && summary.projectionComputations() == 1 && summary.projectionHits() == 1,
                        "same identity has exactly one index computation and one memoized hit");
                check(summary.sourceReads() == 0 && source.reads == 1, "new projection does not reread or reparse source");
            }
        }
        check(values[0] != null && values[0] == values[1], "both consumers receive the identical detached index");
        M3ASTIndex index = values[0];
        check(index.source().text() == sealed[0].text() && index.source().utf16Sha256().equals(sealed[0].utf16Sha256()),
                "exact compiler source seal is preserved by payload identity and hash");
        check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR), "valid original javac parse");
        boolean plus = false, answer = false;
        for (int row = 0; row < index.nodeCount(); row++) {
            check((index.flags(row) & M3ASTIndex.SOURCE_UNVERIFIED) == 0,
                    "only sealed event ingress certifies parser-input binding");
            if (index.kind(row).equals("PLUS")) plus = true;
            Object value = M3ASTIndex.class.getMethod("name", int.class).invoke(index, row);
            if (value instanceof Optional<?> name && name.filter("answer"::equals).isPresent()) answer = true;
        }
        check(plus && answer, "retainSyntax and detached name facts survive owner close");
        expect(IllegalStateException.class, escaped[0]::tree);
        check(index.primitiveBytes() == 32L * index.nodeCount(), "legacy base payload accounting remains independent");
        System.out.println("M3_AST_INDEX_COST nodes=" + index.nodeCount() + " primitive_bytes=" + index.primitiveBytes()
                + " source_units=" + index.source().length() + " source_reads=" + source.reads
                + " projections=" + summary.projectionComputations() + " projection_hits=" + summary.projectionHits());
    }

    private static void independentGenerations() throws Exception {
        M3AST.Projection<?> sharedOperation = projection(M3ASTIngress.Limits.defaults());
        var outputs = new ArrayList<M3ASTIndex>();
        for (String text : List.of("class Demo { int n = 1; }", "class Demo { int n = 2; }")) {
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
                JavacTask task = task(new Source(text), files, diagnostics);
                try (M3AST session = attach(task, sharedOperation, outputs::add)) {
                    task.parse();
                    check(session.summary().projectionComputations() == 1, "new task computes its own detached generation");
                }
            }
        }
        check(outputs.size() == 2 && outputs.get(0) != outputs.get(1), "projection identity is scoped by existing event owner");
        check(outputs.get(0).source().uri().equals(outputs.get(1).source().uri())
                && !outputs.get(0).source().utf16Sha256().equals(outputs.get(1).source().utf16Sha256()),
                "same URI does not collapse different exact source generations");
    }

    private static void failureMemoization() throws Exception {
        M3AST.Projection<?> key = projection(new M3ASTIngress.Limits(1024, 1, 20));
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        AtomicInteger failures = new AtomicInteger();
        try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(new Source("class Demo { int n; }"), files, diagnostics);
            try (M3AST session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        IllegalArgumentException first = expect(IllegalArgumentException.class, () -> unit.project(key));
                        check(expect(IllegalArgumentException.class, () -> unit.project(key)) == first,
                                "failed bounded index is not recomputed or partially published");
                        failures.incrementAndGet();
                    }
                });
                task.parse();
                check(session.summary().projectionComputations() == 1 && session.summary().projectionHits() == 1,
                        "existing event cache owns failure memoization");
                check(!session.summary().consumerFailed(), "handled budget refusal stays local to the consumer");
            }
        }
        check(failures.get() == 1, "real parse event exercised the exact budget failure");
    }

    private static void analyzedRefusal() throws Exception {
        M3AST.Projection<?> key = projection(M3ASTIngress.Limits.defaults());
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        AtomicInteger refusals = new AtomicInteger();
        try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(new Source("class Demo { int n; }"), files, diagnostics);
            try (M3AST session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        IllegalArgumentException first = expect(IllegalArgumentException.class, () -> unit.project(key));
                        check(expect(IllegalArgumentException.class, () -> unit.project(key)) == first,
                                "wrong phase refusal remains memoized");
                        refusals.incrementAndGet();
                    }
                });
                task.parse(); task.analyze();
                check(!session.summary().consumerFailed(), "handled wrong-phase request does not poison compilation");
            }
        }
        check(refusals.get() == 1, "syntax projection refuses an analyzed event");
    }

    private static M3AST.Projection<?> projection(M3ASTIngress.Limits limits) throws Exception {
        return (M3AST.Projection<?>) invoke("indexProjection", new Class<?>[]{M3ASTIngress.Limits.class}, limits);
    }
    private static M3AST attach(JavacTask task, M3AST.Projection<?> key, Consumer<? super M3ASTIndex> consumer) throws Exception {
        return (M3AST) invoke("attachIndex", new Class<?>[]{JavacTask.class, M3AST.Projection.class, Consumer.class}, task, key, consumer);
    }
    private static Object invoke(String name, Class<?>[] parameters, Object... values) throws Exception {
        try { return M3JavacParser.class.getMethod(name, parameters).invoke(null, values); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Error error) throw error;
            if (failure.getCause() instanceof Exception exception) throw exception;
            throw new AssertionError(failure.getCause());
        }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("expected " + type.getName() + " but got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}
