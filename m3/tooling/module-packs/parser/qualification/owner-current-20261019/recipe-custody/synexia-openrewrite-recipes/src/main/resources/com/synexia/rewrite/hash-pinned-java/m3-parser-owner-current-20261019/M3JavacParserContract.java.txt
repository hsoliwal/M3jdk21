/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3JavacParser;
import jdk.m3.parser.M3JavacTree;

/** Direct existing-M3AST contracts; no plugin descriptor or service-loader claim. */
public final class M3JavacParserContract {
    private static int checks;

    private M3JavacParserContract() { }

    public static void main(String[] arguments) throws Exception {
        sharedProjectionAndRanges();
        distinctProjectionIdentities();
        nodeBudgetAndCleanup();
        parsedPhaseOnly();
        traversalShapes();
        System.out.println("M3_JAVAC_PARSER_PASS checks=" + checks);
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        private int reads;
        Source(String name, String text) {
            super(URI.create("memory:/" + name + ".java"), Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignored) {
            reads++;
            return new StringBuilder(text);
        }
    }

    private static JavacTask task(Source source, StandardJavaFileManager manager,
                                  DiagnosticCollector<JavaFileObject> diagnostics) {
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, manager, diagnostics,
                List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(source));
    }

    private record Row(Tree.Kind kind, int parent, int start, int end) { }

    private static void sharedProjectionAndRanges() throws Exception {
        String text = "/* \ud83d\ude00 UTF-16 prefix */\r\nclass Demo {\r\n"
                + "  String value = \"a\" + \"b\";\r\n  int f(int n) { return n + 1; }\r\n}\r\n";
        Source input = new Source("Demo", text);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        M3JavacTree[] captured = new M3JavacTree[2];
        M3AST.Source[] sealed = new M3AST.Source[1];
        M3AST.Unit[] live = new M3AST.Unit[1];
        List<Row> oracle = new ArrayList<>();
        AtomicInteger parseEvents = new AtomicInteger();
        M3AST.Summary summary;
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(input, manager, diagnostics);
            task.addTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.PARSE) parseEvents.incrementAndGet();
                }
            });
            var projection = M3JavacParser.projection(4096);
            try (M3AST session = M3JavacParser.attach(task, projection, tree -> captured[0] = tree)) {
                check(M3JavacParser.attach(task, projection, tree -> captured[1] = tree) == session,
                        "adapters share the existing task-owned M3AST session");
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        sealed[0] = unit.source();
                        live[0] = unit;
                        var positions = unit.trees().getSourcePositions();
                        new TreeScanner<Void, Integer>() {
                            @Override public Void scan(Tree tree, Integer parent) {
                                if (tree == null) return null;
                                int row = oracle.size();
                                oracle.add(new Row(tree.getKind(), parent,
                                        (int) positions.getStartPosition(unit.tree(), tree),
                                        (int) positions.getEndPosition(unit.tree(), tree)));
                                return super.scan(tree, row);
                            }
                        }.scan(unit.tree(), -1);
                        check(CompletableFuture.supplyAsync(() -> {
                            try { unit.tree(); return false; }
                            catch (IllegalStateException expected) { return true; }
                        }).join(), "existing M3AST live view refuses another thread");
                    }
                });
                int units = 0;
                for (CompilationUnitTree ignored : task.parse()) units++;
                check(units == 1, "caller ran one actual javac parse");
                summary = session.summary();
                check(summary.parsedUnits() == 1 && summary.capturedSources() == 1
                        && summary.projectionComputations() == 1 && summary.projectionHits() == 1,
                        "two consumers reuse one source-bound projection");
                check(summary.sourceReads() == 0 && input.reads == 1, "projection uses exact parser input without rereading");
            }
        }
        check(parseEvents.get() == 1 && diagnostics.getDiagnostics().stream()
                .noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR), "single successful frontend parse event");
        check(captured[0] != null && captured[0] == captured[1], "same-key consumers receive identical detached projection");
        M3JavacTree tree = captured[0];
        check(tree.source().text() == sealed[0].text(), "detached source retains existing immutable String by identity");
        check(tree.source().uri().equals(input.toUri()) && tree.source().utf16Sha256().equals(sealed[0].utf16Sha256()),
                "detached projection preserves URI and exact UTF-16 seal");
        check(tree.source().text().equals(text), "CRLF and supplementary source remain unchanged after close");
        expect(IllegalStateException.class, live[0]::tree);
        check(tree.nodeCount() == oracle.size(), "iterative projection agrees with recursive TreeScanner node count");
        check(tree.primitivePayloadBytes() == 16L * tree.nodeCount(), "four primitive node lanes");
        boolean stringPlus = false;
        int unavailable = 0;
        for (int row = 0; row < tree.nodeCount(); row++) {
            Row expected = oracle.get(row);
            check(tree.kind(row) == expected.kind() && tree.parent(row) == expected.parent(), "preorder kinds/parents match javac oracle");
            check(tree.start(row) == expected.start() && tree.end(row) == expected.end(), "exact original UTF-16 range matches javac oracle");
            check(tree.parent(row) < row && tree.parent(row) >= -1, "parent precedes detached child");
            if (tree.hasSourceRange(row)) {
                check(tree.sourceText(row).equals(text.substring(expected.start(), expected.end())), "range projection remains valid after compiler closes");
                if (tree.kind(row) == Tree.Kind.PLUS && tree.sourceText(row).equals("\"a\" + \"b\"")) stringPlus = true;
            } else {
                int current = row;
                expect(IllegalStateException.class, () -> tree.sourceText(current));
                unavailable++;
            }
        }
        check(stringPlus, "existing retainSyntax policy preserves literal concatenation syntax");
        check(unavailable > 0, "javac unavailable positions remain explicit");
        check(tree.kind(0) == Tree.Kind.COMPILATION_UNIT && tree.parent(0) == -1, "detached compilation root");
        expect(IndexOutOfBoundsException.class, () -> tree.kind(-1));
        expect(IndexOutOfBoundsException.class, () -> tree.end(tree.nodeCount()));
        check(!summary.completed(), "parse-only caller does not invent a full compilation event");
    }

    private static void distinctProjectionIdentities() throws Exception {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var outputs = new ArrayList<M3JavacTree>();
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(new Source("Independent", "class Independent {}"), manager, diagnostics);
            try (M3AST session = M3JavacParser.attach(task, M3JavacParser.projection(128), outputs::add)) {
                M3JavacParser.attach(task, M3JavacParser.projection(128), outputs::add);
                task.parse();
                check(session.summary().projectionComputations() == 2 && session.summary().projectionHits() == 0,
                        "equal projection names do not share distinct operation identities");
            }
        }
        check(outputs.size() == 2 && outputs.get(0) != outputs.get(1), "distinct projections produce separate detached values");
        check(outputs.get(0).source().text() == outputs.get(1).source().text(), "distinct projections still share the frozen text payload");
        check(M3ParserFixtures.javacDigest(outputs.get(0)).equals(M3ParserFixtures.javacDigest(outputs.get(1))),
                "independent projections agree in semantic content");
    }

    private static void nodeBudgetAndCleanup() throws Exception {
        expect(IllegalArgumentException.class, () -> M3JavacParser.projection(0));
        String text = "class Budget { int a; int b; }";
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        AtomicInteger expectedNodes = new AtomicInteger();
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(new Source("Budget", text), manager, diagnostics);
            for (CompilationUnitTree unit : task.parse()) {
                new TreeScanner<Void, Void>() {
                    @Override public Void scan(Tree tree, Void ignored) {
                        if (tree != null) expectedNodes.incrementAndGet();
                        return super.scan(tree, ignored);
                    }
                }.scan(unit, null);
            }
        }
        int count = expectedNodes.get();
        check(count > 1, "independent scanner supplies a nontrivial exact node budget");
        for (int budget : new int[] {count, count - 1}) {
            AtomicInteger callbacks = new AtomicInteger();
            try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                    diagnostics, null, StandardCharsets.UTF_8)) {
                JavacTask task = task(new Source("Budget", text), manager, diagnostics);
                try (M3AST session = M3JavacParser.attach(task, M3JavacParser.projection(budget), tree -> {
                    check(tree.nodeCount() == count, "exact budget retains every oracle node");
                    callbacks.incrementAndGet();
                })) {
                    if (budget == count) {
                        task.parse();
                        check(callbacks.get() == 1 && !session.summary().consumerFailed(),
                                "maxNodes=N accepts the complete N-node projection");
                    } else {
                        RuntimeException failure = expect(RuntimeException.class, task::parse);
                        check(hasCause(failure, IllegalStateException.class, "maxNodes"),
                                "maxNodes=N-1 reaches the explicit budget failure path");
                        check(callbacks.get() == 0, "partial projection is not published");
                        check(session.summary().closed() && session.summary().consumerFailed(),
                                "budget failure releases existing compiler session");
                    }
                }
            }
        }
    }

    private static void parsedPhaseOnly() throws Exception {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        AtomicInteger refusals = new AtomicInteger();
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(new Source("Phase", "class Phase { int n = 1; }"), manager, diagnostics);
            var projection = M3JavacParser.projection(128);
            try (M3AST session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) {
                        IllegalArgumentException first = expect(IllegalArgumentException.class, () -> unit.project(projection));
                        check(expect(IllegalArgumentException.class, () -> unit.project(projection)) == first,
                                "wrong-phase projection failure is memoized by existing M3AST");
                        refusals.incrementAndGet();
                    }
                });
                task.parse();
                task.analyze();
                check(!session.summary().consumerFailed(), "handled wrong-phase refusal stays local");
            }
        }
        check(refusals.get() == 1, "adapter refuses attributed-event capture");
    }

    private static void traversalShapes() throws Exception {
        StringBuilder wide = new StringBuilder("class Wide {");
        for (int index = 0; index < 513; index++) wide.append("int value").append(index).append(" = ").append(index).append(';');
        wide.append("void visit() { for (int i = 0; i < 9; i++) { if (i > 3) continue; } }");
        wide.append('}');
        StringBuilder deep = new StringBuilder("class Deep { int value = ");
        deep.append("(".repeat(80)).append('1');
        for (int index = 0; index < 80; index++) deep.append(" + ").append(index).append(')');
        deep.append("; }");
        String[] names = {"Empty", "Wide", "Deep", "Patterns", "module-info", "Broken"};
        String[] texts = {"", wide.toString(), deep.toString(),
                "sealed interface Expr permits Lit {} record Lit(int value) implements Expr {} "
                        + "final class Patterns { int eval(Expr e) { return switch (e) { "
                        + "case Lit(int value) when value > 0 -> value; default -> 0; }; } }",
                "module sample { requires java.base; exports p; opens p; uses java.lang.Runnable; "
                        + "provides java.lang.Runnable with p.Worker; }",
                "class Broken { void m( { if (true) return; }"};
        for (int fixture = 0; fixture < texts.length; fixture++) {
            ArrayList<Row> oracle = new ArrayList<>();
            M3JavacTree tree = captureShape(names[fixture], texts[fixture], 100_000, oracle);
            check(tree.nodeCount() == oracle.size(), "shape node count " + names[fixture]);
            for (int row = 0; row < oracle.size(); row++) {
                Row expected = oracle.get(row);
                check(tree.kind(row) == expected.kind() && tree.parent(row) == expected.parent()
                                && tree.start(row) == expected.start() && tree.end(row) == expected.end(),
                        "scanner order/parent/range " + names[fixture] + " row=" + row);
            }
            M3JavacTree exact = captureShape(names[fixture], texts[fixture], oracle.size(), new ArrayList<>());
            check(M3ParserFixtures.javacDigest(tree).equals(M3ParserFixtures.javacDigest(exact)),
                    "exact budget keeps deterministic result " + names[fixture]);
            if (oracle.size() > 1) {
                String name = names[fixture];
                String text = texts[fixture];
                RuntimeException failure = expect(RuntimeException.class,
                        () -> captureShape(name, text, oracle.size() - 1, new ArrayList<>()));
                check(hasCause(failure, IllegalStateException.class, "maxNodes"), "shape budget refuses " + name);
            }
        }
    }

    private static M3JavacTree captureShape(String name, String text, int limit, List<Row> oracle) throws Exception {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        M3JavacTree[] captured = new M3JavacTree[1];
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            Source input = new Source(name, text);
            JavacTask task = task(input, manager, diagnostics);
            try (M3AST session = M3JavacParser.attach(task, M3JavacParser.projection(limit), tree -> captured[0] = tree)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) {
                        var positions = unit.trees().getSourcePositions();
                        new TreeScanner<Void, Integer>() {
                            @Override public Void scan(Tree tree, Integer parent) {
                                if (tree == null) return null;
                                int row = oracle.size();
                                oracle.add(new Row(tree.getKind(), parent,
                                        (int) positions.getStartPosition(unit.tree(), tree),
                                        (int) positions.getEndPosition(unit.tree(), tree)));
                                return super.scan(tree, row);
                            }
                        }.scan(unit.tree(), -1);
                    }
                });
                task.parse();
                check(input.reads == 1 && session.summary().capturedSources() == 1, "single retained source " + name);
            }
        }
        check(captured[0] != null, "complete projection published " + name);
        return captured[0];
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type, String message) {
        for (int depth = 0; failure != null && depth < 16; depth++, failure = failure.getCause()) {
            if (type.isInstance(failure) && failure.getMessage() != null && failure.getMessage().contains(message)) return true;
        }
        return false;
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Operation action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
