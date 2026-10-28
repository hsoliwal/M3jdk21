/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3JavacParser;
import jdk.m3.parser.M3JavacTree;

/** Real compiler consumers of the existing event-owned source projection. */
public final class M3SharedSourceDeltaContract {
    private static int checks;

    private M3SharedSourceDeltaContract() { }

    public static void main(String[] arguments) throws Exception {
        String configured = System.getProperty("m3.parser.test.work");
        Path work = configured == null ? Files.createTempDirectory("m3-shared-source-delta-")
                : Path.of(configured);
        Files.createDirectories(work);
        check(M3JavacParser.sourceProjection() == M3JavacParser.sourceProjection(),
                "the public source operation has one reusable identity");
        compilationAndDeltaParity(work);
        separateParseEvents();
        System.out.println("M3_SHARED_SOURCE_DELTA_PASS checks=" + checks);
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

    private record DiagnosticFact(Diagnostic.Kind kind, String code, URI source,
                                  long position, long start, long end, long line,
                                  long column, String message) { }

    private record Compilation(boolean success, List<DiagnosticFact> diagnostics,
                               String output, Map<String, byte[]> classes,
                               int sourceReads, Observation observation) { }

    private static final class Observation {
        private final Source input;
        private final M3File previous;
        private final M3File.SourceDelta expected;
        private final boolean refuseAnalyzed;
        private M3AST session;
        private M3File first;
        private M3File second;
        private M3JavacTree tree;
        private M3AST.Unit live;
        private M3AST.Summary summary;
        private int refusals;

        Observation(Source input, String previous, M3File.SourceDelta expected,
                    boolean refuseAnalyzed) {
            this.input = input;
            this.previous = M3File.of(input.toUri(), previous);
            this.expected = expected;
            this.refuseAnalyzed = refuseAnalyzed;
        }

        void attach(JavacTask task, boolean withTree) {
            session = M3AST.open(task);
            session.addConsumer(new M3AST.Consumer() {
                @Override public void parsed(M3AST.Unit unit) throws Exception {
                    int reads = input.reads;
                    first = unit.project(M3JavacParser.sourceProjection());
                    live = unit;
                    check(first.text() == unit.source().text(),
                            "source consumer retains the compiler's frozen String by identity");
                    check(input.reads == reads && session.summary().sourceReads() == 0,
                            "first source request does not read the source again");
                }
                @Override public void analyzed(M3AST.Unit unit) {
                    if (!refuseAnalyzed) return;
                    int reads = input.reads;
                    IllegalArgumentException failure = expect(IllegalArgumentException.class,
                            () -> unit.project(M3JavacParser.sourceProjection()));
                    check(expect(IllegalArgumentException.class,
                            () -> M3JavacParser.changesFrom(unit, previous)) == failure,
                            "source and delta requests share the same wrong-phase refusal");
                    check(input.reads == reads && session.summary().sourceReads() == 0,
                            "wrong-phase refusal precedes a source read");
                    refusals++;
                }
            });
            session.addConsumer(new M3AST.Consumer() {
                @Override public void parsed(M3AST.Unit unit) throws Exception {
                    int reads = input.reads;
                    second = unit.project(M3JavacParser.sourceProjection());
                    check(first == second, "two source consumers share one M3File per event");
                    check(session.summary().projectionComputations() == 1
                                    && session.summary().projectionHits() == 1,
                            "two source requests compute exactly once before any tree request");
                    check(M3JavacParser.changesFrom(unit, previous).equals(expected),
                            "actual parsed source yields the exact expected UTF-16 edit window");
                    M3File.SourceDelta same = M3JavacParser.changesFrom(unit, first);
                    check(same.sameUri() && !same.charactersChanged() && same.start() == 0
                                    && same.previousEnd() == 0 && same.currentEnd() == 0
                                    && same.byteChange() == M3File.ByteChange.UNKNOWN,
                            "unchanged character input has an empty window and unknown byte seal");
                    M3File relocated = M3File.of(URI.create("memory:/PreviousLocation.java"), input.text);
                    M3File.SourceDelta moved = M3JavacParser.changesFrom(unit, relocated);
                    check(!moved.sameUri() && !moved.charactersChanged(),
                            "URI changes remain distinct from character changes");
                    check(input.reads == reads && session.summary().sourceReads() == 0,
                            "shared source and delta requests cause no additional source reads");
                }
            });
            if (withTree) {
                check(M3JavacParser.attach(task, M3JavacParser.projection(512), value -> tree = value)
                                == session,
                        "source and tree consumers join the existing compiler-owned session");
            }
        }

        void afterCall(boolean withTree) {
            summary = session.summary();
            check(summary.completed() && summary.closed() && !summary.consumerFailed(),
                    "the caller's compiler completion owns session cleanup");
            check(summary.parsedUnits() == 1 && summary.capturedSources() == 1
                            && summary.sourceReads() == 0,
                    "one compiler parse captures one source with no projection reread");
            if (!refuseAnalyzed) {
                check(summary.projectionComputations() == (withTree ? 2 : 1)
                                && summary.projectionHits() == (withTree ? 5 : 4),
                        "source-only consumers never request the tree projection");
            }
            if (withTree) {
                check(tree != null && tree.source() == first && first == second,
                        "tree and source consumers retain the identical detached M3File");
            }
            check(first.text().equals(input.text) && first.uri().equals(input.toUri()),
                    "detached source remains exact after compiler completion");
            expect(IllegalStateException.class,
                    () -> live.project(M3JavacParser.sourceProjection()));
            expect(IllegalStateException.class,
                    () -> M3JavacParser.changesFrom(live, previous));
        }
    }

    private static void compilationAndDeltaParity(Path work) throws Exception {
        String text = "/* \ud83d\ude00 UTF-16 prefix */\r\nclass DeltaFixture {\r\n"
                + "  int answer() { return 42; }\r\n}\r\nclass DeltaHelper { int value = 7; }\r\n";
        String previous = text.replace("return 42;", "return 41;");
        int edit = text.indexOf("return 42;") + "return 4".length();
        M3File.SourceDelta expected = new M3File.SourceDelta(true, edit, edit + 1,
                edit + 1, M3File.ByteChange.UNKNOWN);
        Compilation baseline = compile(work.resolve("baseline"), "DeltaFixture", text,
                previous, expected, false, false, false);
        Compilation sourceOnly = compile(work.resolve("source-only"), "DeltaFixture", text,
                previous, expected, true, false, false);
        Compilation mixed = compile(work.resolve("source-and-tree"), "DeltaFixture", text,
                previous, expected, true, true, false);
        Compilation wrongPhase = compile(work.resolve("wrong-phase"), "DeltaFixture", text,
                previous, expected, true, false, true);
        check(baseline.success() && baseline.classes().keySet().equals(
                        Set.of("DeltaFixture.class", "DeltaHelper.class")),
                "baseline javac emits both real top-level classes");
        assertParity(baseline, sourceOnly);
        assertParity(baseline, mixed);
        assertParity(baseline, wrongPhase);
        check(sourceOnly.observation().first != mixed.observation().first,
                "identical source in separate compiler tasks does not share projected objects");
        check(wrongPhase.observation().refusals == 2,
                "source projection refuses both actual ANALYZED events");

        String invalid = "class Broken { int answer() { return missing; } }";
        M3File.SourceDelta unchanged = new M3File.SourceDelta(true, 0, 0, 0,
                M3File.ByteChange.UNKNOWN);
        Compilation failedBaseline = compile(work.resolve("failed-baseline"), "Broken", invalid,
                invalid, unchanged, false, false, false);
        Compilation failedConsumer = compile(work.resolve("failed-consumer"), "Broken", invalid,
                invalid, unchanged, true, false, false);
        check(!failedBaseline.success() && failedBaseline.classes().isEmpty()
                        && failedBaseline.diagnostics().stream()
                        .anyMatch(diagnostic -> diagnostic.kind() == Diagnostic.Kind.ERROR),
                "baseline fixture reaches a real compiler diagnostic");
        assertParity(failedBaseline, failedConsumer);
        check(failedConsumer.observation().summary.compilerErrors() > 0,
                "source capture preserves the compiler's error disposition");
    }

    private static Compilation compile(Path output, String name, String text, String previous,
                                       M3File.SourceDelta expected, boolean observe,
                                       boolean withTree, boolean refuseAnalyzed) throws Exception {
        Files.createDirectory(output);
        Source input = new Source(name, text);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter compilerOutput = new StringWriter();
        Observation observation = observe ? new Observation(input, previous, expected, refuseAnalyzed) : null;
        boolean success;
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(
                    compilerOutput, manager, diagnostics,
                    List.of("-proc:none", "-source", "21", "-target", "21", "-g", "-d", output.toString()),
                    null, List.of(input));
            if (observation != null) observation.attach(task, withTree);
            try {
                success = Boolean.TRUE.equals(task.call());
                if (observation != null) observation.afterCall(withTree);
            } finally {
                if (observation != null) observation.session.close();
            }
        }
        List<DiagnosticFact> facts = diagnostics.getDiagnostics().stream().map(diagnostic ->
                new DiagnosticFact(diagnostic.getKind(), diagnostic.getCode(),
                        diagnostic.getSource() == null ? null : diagnostic.getSource().toUri(),
                        diagnostic.getPosition(), diagnostic.getStartPosition(), diagnostic.getEndPosition(),
                        diagnostic.getLineNumber(), diagnostic.getColumnNumber(), diagnostic.getMessage(Locale.ROOT)))
                .toList();
        Map<String, byte[]> classes = new TreeMap<>();
        try (var files = Files.walk(output)) {
            for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                check(path.getFileName().toString().endsWith(".class"), "fixture outputs are actual class files");
                classes.put(output.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
            }
        }
        return new Compilation(success, facts, compilerOutput.toString(), classes, input.reads, observation);
    }

    private static void assertParity(Compilation baseline, Compilation selected) {
        check(baseline.success() == selected.success()
                        && baseline.diagnostics().equals(selected.diagnostics())
                        && baseline.output().equals(selected.output()),
                "source consumers preserve compiler success and exact diagnostic facts");
        check(baseline.classes().keySet().equals(selected.classes().keySet()),
                "source consumers preserve emitted class ownership");
        for (String path : baseline.classes().keySet()) {
            check(Arrays.equals(baseline.classes().get(path), selected.classes().get(path)),
                    "source consumers preserve every generated class byte: " + path);
        }
        check(baseline.sourceReads() == selected.sourceReads(),
                "consumer selection adds no reads to the baseline compiler source count");
    }

    private static void separateParseEvents() throws Exception {
        Source first = new Source("EventOne", "class EventOne {}");
        Source second = new Source("EventTwo", "class EventTwo {}");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<M3File> sources = new ArrayList<>();
        M3AST.Unit[] previousEvent = new M3AST.Unit[1];
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, manager,
                    diagnostics, List.of("-proc:none", "-source", "21", "-target", "21"),
                    null, List.of(first, second));
            try (M3AST session = M3AST.open(task)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        if (previousEvent[0] != null) {
                            expect(IllegalStateException.class,
                                    () -> previousEvent[0].project(M3JavacParser.sourceProjection()));
                        }
                        M3File current = unit.project(M3JavacParser.sourceProjection());
                        check(unit.project(M3JavacParser.sourceProjection()) == current,
                                "each separate event memoizes its own source identity");
                        sources.add(current);
                        previousEvent[0] = unit;
                    }
                });
                task.parse();
                check(session.summary().parsedUnits() == 2 && session.summary().capturedSources() == 2
                                && session.summary().projectionComputations() == 2
                                && session.summary().projectionHits() == 2
                                && session.summary().sourceReads() == 0,
                        "one operation key has independent values for separate original parse events");
            }
        }
        check(sources.size() == 2 && sources.get(0) != sources.get(1)
                        && !sources.get(0).uri().equals(sources.get(1).uri()),
                "event-owned projections do not conflate different source inputs");
        check(first.reads == 1 && second.reads == 1 && diagnostics.getDiagnostics().isEmpty(),
                "each original source is read only once by its successful javac parse");
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Operation operation) {
        try { operation.run(); }
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
