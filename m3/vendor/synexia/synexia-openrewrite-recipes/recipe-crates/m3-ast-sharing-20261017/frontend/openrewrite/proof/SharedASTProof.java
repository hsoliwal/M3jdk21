// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.compiler.ast.M3CompilationContext;
import com.synexia.mindex.compiler.ast.M3File;
import com.synexia.mindex.compiler.ast.M3JavacASTIndex;
import com.synexia.mindex.compiler.ast.M3OpenRewriteSharedAST;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public final class SharedASTProof {
    private static int checks;
    private static int tasksCreated;
    private static final List<String> REQUIRED = List.of("-proc:none", "-XDcompilePolicy=simple", "-XDallowStringFolding=false");
    private static final String ALPHA = """
            package sample;
            import java.util.List;
            /** Frozen documentation, café, and {@link String}. */
            class Alpha<T extends Number> {
                String text = "a" /* retain the addition */ + "b";
                String decoy = "class Fake { } // + import missing.Type;";
                List<String> values = List.of("first");
                int size(Beta beta) { return beta.value().size(); }
                Runnable work() { return () -> values.add(text); }
            }
            record Beta(List<String> value) { }
            enum Gamma {
                ONE;
                int count() { return new Alpha<Integer>().size(new Beta(List.of("z"))); }
            }
            """.replace("\n", "\r\n");
    private static final String DELTA = """
            package sample;
            import java.util.List;
            class Delta {
                String first(Beta beta) { return beta.value().get(0).strip(); }
                Beta fresh() { return new Beta(List.of("d")); }
            }
            """;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void refusal(String fragment, Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (String.valueOf(cause.getMessage()).contains(fragment)) {
                    checks++;
                    return;
                }
            }
            throw new AssertionError("unexpected refusal: expected " + fragment, failure);
        }
        throw new AssertionError("missing refusal: " + fragment);
    }

    private static final class CountingInput extends ForwardingJavaFileObject<M3File> {
        private int reads;
        CountingInput(String path, String source) {
            super(M3File.of(URI.create("mem:///" + path), source));
        }
        String source() { return fileObject.source(); }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return fileObject.source();
        }
    }

    private static final class Run implements AutoCloseable {
        final StandardJavaFileManager manager;
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StringWriter compilerOutput = new StringWriter();
        final List<CountingInput> inputs;
        final Map<URI, String> frozenSources = new LinkedHashMap<>();
        final JavacTask task;
        final M3JavacASTIndex index;
        final List<String> events = new ArrayList<>();
        final Map<CompilationUnitTree, Integer> analyzed = new IdentityHashMap<>();
        final Map<CompilationUnitTree, CompilationUnitTree> parsed = new IdentityHashMap<>();
        final List<M3OpenRewriteSharedAST.Snapshot> snapshots = new ArrayList<>();
        int generated;
        M3OpenRewriteSharedAST adapter;

        Run(Path output, Map<String, String> sources, List<String> requested) throws IOException {
            Files.createDirectories(output);
            inputs = sources.entrySet().stream().map(e -> new CountingInput(e.getKey(), e.getValue())).toList();
            for (CountingInput input : inputs) frozenSources.put(input.toUri(), input.source());
            var compiler = ToolProvider.getSystemJavaCompiler();
            manager = compiler.getStandardFileManager(diagnostics, null, null);
            List<String> options = new ArrayList<>(requested);
            options.addAll(List.of("-g:none", "-d", output.toString()));
            task = (JavacTask) compiler.getTask(compilerOutput, manager, diagnostics, options, null, inputs);
            tasksCreated++;
            task.addTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    events.add(event.getKind() + ":" + event.getTypeElement());
                    if (event.getKind() == TaskEvent.Kind.PARSE) {
                        parsed.put(event.getCompilationUnit(), event.getCompilationUnit());
                    }
                    if (event.getKind() == TaskEvent.Kind.ANALYZE) {
                        check(parsed.get(event.getCompilationUnit()) == event.getCompilationUnit(), "same original javac tree");
                        analyzed.merge(event.getCompilationUnit(), 1, Integer::sum);
                    }
                    if (event.getKind() == TaskEvent.Kind.GENERATE) generated++;
                }
            });
            index = new M3JavacASTIndex(task, M3CompilationContext.isolated(options), unit -> {
                String frozen = frozenSources.get(unit.getSourceFile().toUri());
                if (frozen != null) return frozen;
                throw new IOException("source outside this frozen input generation");
            }, MIndexASTLimits.defaults(), 21);
            index.on(M3JavacASTIndex.Phase.PARSE, "proof-source-identity", view -> {
                check(view.source() == frozenSources.get(view.unit().getSourceFile().toUri()), "identical frozen source String");
            });
        }

        CountingInput input(URI uri) {
            return inputs.stream().filter(input -> input.toUri().equals(uri)).findFirst().orElseThrow();
        }

        void attach() {
            adapter = M3OpenRewriteSharedAST.attach(index, task,
                    new InMemoryExecutionContext(t -> { throw new IllegalStateException(t); }), List.of(), snapshot -> {
                        check(generated == 0, "all conversion precedes generation under SIMPLE");
                        check(snapshot.tree().printAll().equals(input(URI.create(snapshot.sourceUri())).source()), "lossless exact characters");
                        check(snapshot.compilerContextSha256().equals(index.context().sha256()), "same sealed compiler context");
                        check(snapshot.topLevelTypes() == snapshot.tree().getClasses().size(), "whole-unit type count");
                        check(events.getLast().startsWith("ANALYZE:"), "conversion inside finished ANALYZE callback");
                        for (var unit : analyzed.entrySet()) {
                            if (unit.getKey().getSourceFile().toUri().toString().equals(snapshot.sourceUri())) {
                                check(unit.getValue() == snapshot.topLevelTypes(), "all top-level ANALYZE completions observed");
                            }
                        }
                        snapshots.add(snapshot);
                    });
        }

        void call() {
            check(Boolean.TRUE.equals(task.call()), "compiler succeeded: " + diagnostics.getDiagnostics() + compilerOutput);
        }

        @Override public void close() throws IOException {
            if (adapter != null) adapter.close();
            index.close();
            manager.close();
        }
    }

    private record RawTree(String text, List<Tree> nodes, List<Object> types) {
        static RawTree capture(CompilationUnitTree unit) {
            List<Tree> nodes = new ArrayList<>();
            List<Object> types = new ArrayList<>();
            new TreeScanner<Void, Void>() {
                @Override public Void scan(Tree tree, Void ignored) {
                    if (tree != null) {
                        nodes.add(tree);
                        types.add(((JCTree) tree).type);
                    }
                    return super.scan(tree, ignored);
                }
            }.scan(unit, null);
            return new RawTree(unit.toString(), nodes, types);
        }

        void unchanged(CompilationUnitTree unit) {
            RawTree after = capture(unit);
            check(text.equals(after.text), "javac syntax unchanged by projection");
            check(nodes.size() == after.nodes.size(), "javac node count unchanged");
            for (int i = 0; i < nodes.size(); i++) {
                check(nodes.get(i) == after.nodes.get(i), "javac node identity unchanged");
                check(types.get(i) == after.types.get(i), "javac type identity unchanged");
            }
        }
    }

    private static Map<String, String> sources() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("sample/Alpha.java", ALPHA);
        result.put("sample/Delta.java", DELTA);
        return result;
    }

    private static void wholeUnit(Path root) throws Exception {
        Path before = root.resolve("reference");
        Path after = root.resolve("observed");
        try (Run baseline = new Run(before, sources(), REQUIRED)) {
            baseline.call();
        }
        int createdBefore = tasksCreated;
        try (Run run = new Run(after, sources(), REQUIRED)) {
            Map<CompilationUnitTree, RawTree> raw = new IdentityHashMap<>();
            run.index.on(M3JavacASTIndex.Phase.ANALYZE, "proof-before-visitor", view -> raw.put(view.unit(), RawTree.capture(view.unit())));
            run.attach();
            run.index.on(M3JavacASTIndex.Phase.ANALYZE, "proof-after-visitor", view -> raw.get(view.unit()).unchanged(view.unit()));
            run.call();
            check(tasksCreated - createdBefore == 1, "one measured compiler task");
            check(run.snapshots.size() == 2, "one LST per original compilation unit");
            check(run.index.statistics().parseEvents() == 2, "one parse per input");
            check(run.index.statistics().syntaxCaptures() == 0, "no separate MIndex syntax projection demanded");
            check(run.adapter.statistics().parsedUnits() == 2, "adapter parse count");
            check(run.adapter.statistics().analyzedTypes() == 4, "all four top-level types analyzed");
            check(run.adapter.statistics().convertedUnits() == 2, "two guarded conversions");
            check(run.adapter.statistics().incompleteUnits() == 0, "no partial unit published");
            check(run.adapter.statistics().closed(), "borrowed compiler state released");
            for (CountingInput input : run.inputs) check(input.reads == 1, "frozen source read once by javac");
            int[] operations = new int[2];
            JavaType.FullyQualified[] betaType = new JavaType.FullyQualified[1];
            List<JavaType.FullyQualified> betaMethodOwners = new ArrayList<>();
            for (var snapshot : run.snapshots) {
                new JavaIsoVisitor<Void>() {
                    @Override public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration node, Void p) {
                        check(node.getType() != null, "class declaration attributed");
                        if (node.getSimpleName().equals("Beta")) betaType[0] = node.getType();
                        return super.visitClassDeclaration(node, p);
                    }
                    @Override public J.Binary visitBinary(J.Binary node, Void p) {
                        operations[0]++;
                        check(node.getType() != null, "string addition type retained");
                        return super.visitBinary(node, p);
                    }
                    @Override public J.MethodInvocation visitMethodInvocation(J.MethodInvocation node, Void p) {
                        operations[1]++;
                        check(node.getMethodType() != null, "method invocation attributed: " + node.getSimpleName());
                        if (node.getSimpleName().equals("value")) betaMethodOwners.add(node.getMethodType().getDeclaringType());
                        return super.visitMethodInvocation(node, p);
                    }
                }.visit(snapshot.tree(), null);
            }
            check(operations[0] == 1, "string concatenation remains a binary LST expression");
            check(operations[1] >= 8, "method attribution checked across both files");
            check(betaMethodOwners.size() == 2, "record access in both source units");
            for (var owner : betaMethodOwners) check(owner == betaType[0], "shared JavaType identity across projections");
            System.out.println("SHARED_AST_PASS parses=2 analyzedTypes=4 conversions=2 frozenReads=2 stringAdditions=1");
        }
        try (var paths = Files.walk(before)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Path counterpart = after.resolve(before.relativize(path));
                check(Arrays.equals(Files.readAllBytes(path), Files.readAllBytes(counterpart)), "unchanged generated bytecode: " + path.getFileName());
            }
        }
        System.out.println("BYTECODE_EQUAL_PASS classes=4");
    }

    private static void defaultPolicyHazard(Path root) throws Exception {
        try (Run run = new Run(root.resolve("bytodo"), Map.of("sample/Alpha.java", ALPHA),
                List.of("-proc:none", "-XDallowStringFolding=false"))) {
            run.call();
            int firstGenerate = -1;
            int lastAnalyze = -1;
            for (int i = 0; i < run.events.size(); i++) {
                if (run.events.get(i).startsWith("GENERATE:") && firstGenerate < 0) firstGenerate = i;
                if (run.events.get(i).startsWith("ANALYZE:")) lastAnalyze = i;
            }
            check(firstGenerate >= 0 && firstGenerate < lastAnalyze, "default BY_TODO interleaves generation before last ANALYZE");
            System.out.println("DEFAULT_BYTODO_HAZARD_REPRODUCED " + run.events);
        }
    }

    private static void rejections(Path root) throws Exception {
        Map<String, String> one = Map.of("A.java", "class A {} class B {}\n");
        try (Run run = new Run(root.resolve("missing-policy"), one, List.of("-proc:none", "-XDallowStringFolding=false"))) {
            refusal("compilePolicy=simple", run::attach);
            check(run.index.statistics().parseEvents() == 0, "missing policy refused before parsing");
        }
        try (Run run = new Run(root.resolve("missing-folding"), one, List.of("-proc:none", "-XDcompilePolicy=simple"))) {
            refusal("allowStringFolding=false", run::attach);
        }
        try (Run run = new Run(root.resolve("late"), one, REQUIRED)) {
            run.task.parse();
            refusal("before parsing", run::attach);
        }
        try (Run run = new Run(root.resolve("partial"), one,
                List.of("-proc:only", "-XDcompilePolicy=simple", "-XDallowStringFolding=false"))) {
            run.attach();
            refusal("incomplete whole-unit attribution", run::call);
            check(run.snapshots.isEmpty(), "partial run emits no LST");
            check(run.adapter.statistics().incompleteUnits() == 1, "partial run records incomplete unit");
            check(run.adapter.statistics().closed(), "partial run releases compiler state");
        }
        try (Run run = new Run(root.resolve("lowered"), one, REQUIRED)) {
            run.index.on(M3JavacASTIndex.Phase.ANALYZE, "proof-lowered-control", view -> {
                if (run.analyzed.get(view.unit()) == 2) {
                    var context = ((BasicJavacTask) run.task).getContext();
                    var first = (JCClassDecl) view.unit().getTypeDecls().getFirst();
                    CompileStates.instance(context).put(Enter.instance(context).getEnv(first.sym), CompileState.LOWER);
                }
            });
            run.attach();
            refusal("partial or lowered compiler tree", run::call);
            check(run.snapshots.isEmpty(), "lowered control emits no LST");
        }
        try (Run run = new Run(root.resolve("missing-end-position"), one, REQUIRED)) {
            run.index.on(M3JavacASTIndex.Phase.PARSE, "proof-end-position-control", view -> ((JCCompilationUnit) view.unit()).endPositions = null);
            run.attach();
            refusal("source positions and comments", run::call);
            check(run.snapshots.isEmpty(), "missing source positions emit no LST");
        }
        try (Run run = new Run(root.resolve("module"), Map.of("module-info.java", "module proof.shared {}\n"), REQUIRED)) {
            run.attach();
            refusal("class-bearing compilation unit", run::call);
            check(run.snapshots.isEmpty(), "module descriptor is not falsely represented as class LST");
        }
        try (Run run = new Run(root.resolve("foreign-one"), one, REQUIRED);
             Run foreign = new Run(root.resolve("foreign-two"), one, REQUIRED)) {
            run.adapter = M3OpenRewriteSharedAST.attach(run.index, foreign.task, new InMemoryExecutionContext(), List.of(), run.snapshots::add);
            refusal("foreign compiler view or phase", run::call);
            check(run.snapshots.isEmpty(), "foreign task emits no LST");
        }
        System.out.println("SHARED_AST_REFUSAL_PASS cases=8");
    }

    private static void lazySourceCompletion(Path root) throws Exception {
        Path library = root.resolve("lazy/library");
        String dependency = "package dep; public class Dependency {}\n";
        Map<String, String> librarySources = Map.of(
                "dep/Dependency.java", dependency,
                "dep/Lib.java", "package dep; public class Lib { public static int used() { return 1; } public static Dependency unused() { return null; } }\n");
        try (Run run = new Run(library, librarySources, REQUIRED)) { run.call(); }
        Files.delete(library.resolve("dep/Dependency.class"));
        Path source = root.resolve("lazy/source/dep/Dependency.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, dependency);
        List<String> options = new ArrayList<>(REQUIRED);
        options.addAll(List.of("-classpath", library.toString(), "-sourcepath", source.getParent().getParent().toString(), "-implicit:none"));
        Map<String, String> main = Map.of("Main.java", "import dep.Lib; class Main { int n = Lib.used(); }\n");
        try (Run run = new Run(root.resolve("lazy/reference"), main, options)) {
            run.frozenSources.put(source.toUri(), dependency);
            run.call();
            check(run.index.statistics().parseEvents() == 1, "ordinary compiler does not parse unused dependency");
        }
        try (Run run = new Run(root.resolve("lazy/observed"), main, options)) {
            run.frozenSources.put(source.toUri(), dependency);
            run.attach();
            refusal("compiler advanced during LST projection", run::call);
            check(run.index.statistics().parseEvents() == 2, "stock mapper's lazy source completion was observed");
            check(run.snapshots.isEmpty(), "extra dependency parse cannot publish a sharing result");
            check(run.adapter.statistics().closed(), "reentrant refusal releases compiler state");
        }
        System.out.println("LAZY_SOURCE_COMPLETION_REFUSAL_PASS baselineParses=1 visitorParses=2 published=0");
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        wholeUnit(root);
        defaultPolicyHazard(root);
        rejections(root);
        lazySourceCompletion(root);
        System.out.println("OPENREWRITE_SAME_JAVAC_TASK_PASS checks=" + checks);
    }
}
