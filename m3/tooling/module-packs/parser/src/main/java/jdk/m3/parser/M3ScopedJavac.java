// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Selected-scope javac orchestration, never a second Java parser or AST owner.
 *
 * <p>If the host already owns a JavacTask, use {@link #observeOriginal} and let
 * Maven, Gradle, NetBeans or Eclipse invoke the task exactly as before. Its
 * existing M3AST listeners/projections remain the sole compiler AST authority.
 *
 * <p>For an otherwise absent task, {@link #execute} creates exactly one javac
 * task for the explicitly selected units. FILE, PACKAGE, MODULE and PROJECT
 * are *selection scopes*, not instructions to scan/recompile their siblings.
 * All nonselected symbols must already exist on binary class/module paths.
 * Implicit source discovery and annotation processing are disabled. One task
 * parses each selected unit once; PARSE, ANALYZE and COMPILE are exclusive
 * terminal goals, not sequential reparses.
 */
public final class M3ScopedJavac {
    public enum Scope { FILE, PACKAGE, MODULE, PROJECT }
    public enum Goal { PARSE, ANALYZE, COMPILE }

    @FunctionalInterface
    public interface Parsed {
        /** The original compiler tree, borrowed only during this event. */
        void onParsed(JavacTask originalTask, CompilationUnitTree unit);
    }

    public record Plan(Scope scope, List<JavaFileObject> selected,
                       List<Path> givenClasses, List<Path> givenModules,
                       Path output, String contextIdentity) {
        public Plan {
            Objects.requireNonNull(scope, "scope");
            selected = List.copyOf(Objects.requireNonNull(selected, "selected"));
            givenClasses = List.copyOf(Objects.requireNonNull(givenClasses, "givenClasses"));
            givenModules = List.copyOf(Objects.requireNonNull(givenModules, "givenModules"));
            output = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
            if (contextIdentity == null || contextIdentity.isBlank()) {
                throw new IllegalArgumentException("explicit context identity required");
            }
            if (selected.isEmpty() || selected.size() > 100_000
                    || (scope == Scope.FILE && selected.size() != 1)) {
                throw new IllegalArgumentException("invalid selected compilation-unit count");
            }
            Set<URI> names = new HashSet<>();
            int descriptors = 0;
            for (JavaFileObject file : selected) {
                Objects.requireNonNull(file, "selected source");
                if (file.getKind() != JavaFileObject.Kind.SOURCE || !names.add(file.toUri())) {
                    throw new IllegalArgumentException("non-source or repeated selected Java unit");
                }
                if (file.getName().endsWith("module-info.java")) descriptors++;
            }
            if ((scope == Scope.MODULE) != (descriptors == 1) || descriptors > 1) {
                throw new IllegalArgumentException("module scope must include exactly one module-info.java");
            }
            // Context is admission metadata, not an assertion of cross-build
            // cache equivalence. Callers must separately seal complete bytes.
        }
    }

    public record Result(Scope scope, Goal goal, boolean succeeded,
                         int selectedUnits, int parsedEvents, int analyzedEvents,
                         int generatedEvents, List<String> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    /**
     * Borrow the task that the build tool already created. No compiler task,
     * file snapshot, source read, parser or projection is allocated here.
     * Register before that task starts; the caller owns invocation and close.
     */
    public static Observation observeOriginal(JavacTask task,
            Set<URI> selected, Parsed consumer) {
        return new Observation(task, selected, consumer);
    }

    /** A pure observer; closing never closes its caller-owned javac task. */
    public static final class Observation implements AutoCloseable {
        private final JavacTask task;
        private final Set<URI> selected;
        private final Parsed consumer;
        private final TaskListener listener;
        private final Thread owner = Thread.currentThread();
        private final AtomicInteger parsed = new AtomicInteger();
        private boolean closed;

        private Observation(JavacTask task, Set<URI> selected, Parsed consumer) {
            this.task = Objects.requireNonNull(task, "original JavacTask");
            this.selected = Set.copyOf(Objects.requireNonNull(selected, "selected"));
            this.consumer = Objects.requireNonNull(consumer, "consumer");
            if (this.selected.isEmpty()) throw new IllegalArgumentException("empty source selection");
            listener = new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.PARSE
                            && selectedSource(event) && !closed) {
                        parsed.incrementAndGet();
                        Observation.this.consumer.onParsed(Observation.this.task, event.getCompilationUnit());
                    }
                }
            };
            task.addTaskListener(listener);
        }

        private boolean selectedSource(TaskEvent event) {
            return event.getSourceFile() != null && selected.contains(event.getSourceFile().toUri());
        }

        public int observedParseEvents() { return parsed.get(); }

        @Override public void close() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("original javac task listener is thread-confined");
            }
            if (!closed) {
                closed = true;
                task.removeTaskListener(listener);
            }
        }
    }

    /**
     * Create one task only because no original task was supplied. Each input
     * is caller-selected; binary class/module paths are given, never compiled
     * from unselected source. The output must be absent to avoid stale classes.
     */
    public static Result execute(Plan plan, Goal goal, Parsed consumer) throws IOException {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(consumer, "consumer");
        if (Files.exists(plan.output())) throw new IOException("scope output must be fresh");
        for (Path binary : plan.givenClasses()) checkBinary(binary);
        for (Path binary : plan.givenModules()) checkBinary(binary);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("full JDK compiler required");
        Files.createDirectories(plan.output());
        Path empty = plan.output().resolve(".m3-no-implicit-source-discovery");
        Files.createDirectory(empty);
        List<String> options = new ArrayList<>(List.of(
                "--release", "21", "-proc:none", "-implicit:none",
                "-sourcepath", empty.toString(), "-d", plan.output().toString()));
        if (!plan.givenClasses().isEmpty()) {
            options.add("--class-path");
            options.add(pathList(plan.givenClasses()));
        }
        if (!plan.givenModules().isEmpty()) {
            options.add("--module-path");
            options.add(pathList(plan.givenModules()));
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        int[] parseEvents = {0};
        int[] analyzeEvents = {0};
        int[] generatedEvents = {0};
        Set<URI> expected = new HashSet<>();
        for (JavaFileObject source : plan.selected()) expected.add(source.toUri());
        Set<URI> observed = new HashSet<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    options, null, plan.selected());
            task.addTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.PARSE) {
                        URI uri = event.getSourceFile().toUri();
                        if (!expected.contains(uri) || !observed.add(uri)) {
                            throw new IllegalStateException("implicit, duplicate or foreign parse: " + uri);
                        }
                        parseEvents[0]++;
                        consumer.onParsed(task, event.getCompilationUnit());
                    } else if (event.getKind() == TaskEvent.Kind.ANALYZE) {
                        analyzeEvents[0]++;
                    } else if (event.getKind() == TaskEvent.Kind.GENERATE) {
                        generatedEvents[0]++;
                    }
                }
            });
            boolean success;
            switch (goal) {
                case PARSE -> {
                    for (CompilationUnitTree ignored : task.parse()) {
                        // Consume the already parsed units, never parse again.
                    }
                    success = true;
                }
                case ANALYZE -> {
                    task.parse();
                    task.analyze();
                    success = true;
                }
                case COMPILE -> success = Boolean.TRUE.equals(task.call());
                default -> throw new AssertionError(goal);
            }
            List<String> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                errors.add(d.getKind() + ":" + d.getCode());
                if (d.getKind() == Diagnostic.Kind.ERROR) success = false;
            }
            if (parseEvents[0] != plan.selected().size()) success = false;
            return new Result(plan.scope(), goal, success, plan.selected().size(),
                    parseEvents[0], analyzeEvents[0], generatedEvents[0], errors);
        }
    }

    /**
     * A project may contain several independently selected modules/packages.
     * Compile in the caller-supplied dependency order; feed previous class
     * outputs into later plans explicitly. No plan reparses another plan.
     */
    public static List<Result> executeProject(List<Plan> orderedPlans, Goal goal,
                                               Parsed observer) throws IOException {
        Objects.requireNonNull(orderedPlans, "orderedPlans");
        if (orderedPlans.isEmpty()) throw new IllegalArgumentException("empty project");
        List<Result> results = new ArrayList<>();
        Set<URI> all = new HashSet<>();
        for (Plan plan : orderedPlans) {
            for (JavaFileObject file : plan.selected()) {
                if (!all.add(file.toUri())) {
                    throw new IllegalArgumentException("project selects a source twice");
                }
            }
        }
        for (Plan plan : orderedPlans) {
            Result result = execute(plan, goal, observer);
            results.add(result);
            if (!result.succeeded()) break; // Never publish later scopes on failure.
        }
        return List.copyOf(results);
    }

    private static void checkBinary(Path input) throws IOException {
        Objects.requireNonNull(input, "dependency");
        if (!Files.exists(input) || !(Files.isDirectory(input) || Files.isRegularFile(input))) {
            throw new IOException("required precompiled dependency unavailable: " + input);
        }
        if (Files.isRegularFile(input) && !(input.toString().endsWith(".jar")
                || input.toString().endsWith(".jmod"))) {
            throw new IOException("dependency is not a binary class/module archive: " + input);
        }
    }

    private static String pathList(List<Path> inputs) {
        StringBuilder value = new StringBuilder();
        for (Path path : inputs) {
            if (!value.isEmpty()) value.append(java.io.File.pathSeparatorChar);
            value.append(path.toAbsolutePath().normalize());
        }
        return value.toString();
    }

    private M3ScopedJavac() { }
}
