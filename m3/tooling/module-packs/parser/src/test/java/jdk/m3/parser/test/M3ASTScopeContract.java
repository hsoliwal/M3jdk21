/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.Plugin;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Actual original-task compilation; no parser doubles or replacement symbol table. */
public final class M3ASTScopeContract {
    private static int checks;
    private static int compilations;
    private static long scopedParses;
    private static int outsideRefusals;

    private M3ASTScopeContract() { }

    public static void main(String[] arguments) throws Exception {
        Path work = Files.createTempDirectory("m3-ast-scope-");
        scopeValues();
        unchangedBinaries(work);
        implicitSourceBoundary(work);
        declarationBoundaries(work);
        modulesAndProject(work);
        missingBinary(work);
        lifecycle(work);
        lateScopeAdmission(work);
        pluginScopes(work);
        System.out.println("M3_AST_SCOPE_PASS checks=" + checks + " compilations="
                + compilations + " scopedParses=" + scopedParses
                + " outsideRefusals=" + outsideRefusals
                + " unchangedDependencySourceReads=0 unchangedDependencyParses=0");
    }

    private static void scopeValues() {
        URI a = URI.create("string:///A.java");
        URI b = URI.create("string:///B.java");
        ArrayList<URI> mutable = new ArrayList<>(List.of(b, a));
        M3AST.Scope scope = M3AST.Scope.packageSources("p", mutable);
        mutable.clear();
        check(scope.sourceCount() == 2 && scope.contains(a) && scope.contains(b), "scope owns its array");
        check(scope.sourceUris().equals(List.of(a, b)), "sorted URI membership");
        expect(UnsupportedOperationException.class, () -> scope.sourceUris().clear());
        check(scope.equals(M3AST.Scope.packageSources("p", List.of(a, b))), "scope value equality");
        check(scope.hashCode() == M3AST.Scope.packageSources("p", List.of(b, a)).hashCode(), "scope hash equality");
        check(!scope.equals(M3AST.Scope.projectSources(List.of(a, b))), "scope kind participates in identity");
        check(M3AST.Scope.file(a).kind() == M3AST.ScopeKind.FILE, "single-file scope");
        expect(IllegalArgumentException.class, () -> M3AST.Scope.projectSources(List.of()));
        expect(IllegalArgumentException.class, () -> M3AST.Scope.projectSources(List.of(a, a)));
        expect(IllegalArgumentException.class, () -> M3AST.Scope.file(URI.create("A.java")));
        expect(IllegalArgumentException.class, () -> M3AST.Scope.moduleSources("", List.of(a)));
        expect(NullPointerException.class, () -> M3AST.Scope.file(null));
        expect(NullPointerException.class, () -> M3AST.Scope.packageSources(null, List.of(a)));
        expect(NullPointerException.class, () -> M3AST.Scope.projectSources(Arrays.asList(a, null)));
    }

    private static void unchangedBinaries(Path work) throws Exception {
        Path dependencies = Files.createDirectory(work.resolve("dependencies"));
        Source b = source("dep/B", "package dep; public class B { public static int value() { return 40; } }");
        Source c = source("dep/C", "package dep; public class C { public static int value() { return 2; } }");
        compile(dependencies, List.of(), null, List.of(b, c));
        Map<Path, byte[]> binaryBefore = classBytes(dependencies);
        check(binaryBefore.size() == 2, "two actual dependency class files");
        int bReads = b.reads;
        int cReads = c.reads;
        for (int generation = 0; generation < 2; generation++) {
            Path output = Files.createDirectory(work.resolve("generation-" + generation));
            Source a = source("A", "public class A { public static int value() { return dep.B.value() + dep.C.value() + "
                    + generation + "; } }");
            M3AST.Summary summary = compile(output, List.of("-classpath", dependencies.toString(), "-sourcepath", ""),
                    M3AST.Scope.file(a.toUri()), List.of(a));
            check(a.reads == 1 && summary.parsedUnits() == 1, "one source read and parse per selected generation");
            check(summary.completed() && summary.closed() && !summary.consumerFailed(), "scope call lifecycle");
            check(b.reads == bReads && c.reads == cReads, "unchanged dependency sources are untouched");
            check(sameClassBytes(binaryBefore, classBytes(dependencies)), "unchanged dependency class bytes are untouched");
            try (URLClassLoader loader = new URLClassLoader(new URL[] {
                    output.toUri().toURL(), dependencies.toUri().toURL()}, null)) {
                check(loader.loadClass("A").getMethod("value").invoke(null).equals(42 + generation),
                        "new selected bytecode executes against unchanged dependency bytecode");
            }
        }
        Source lint = source("LintOnly", "class LintOnly { int value() { return dep.B.value(); } }");
        Path lintOutput = Files.createDirectory(work.resolve("lint-output"));
        try (FilesOwner files = new FilesOwner()) {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = task(files, lintOutput, List.of("-classpath", dependencies.toString(), "-sourcepath", ""),
                    List.of(lint), diagnostics);
            M3AST session = M3AST.open(task, true, M3AST.Scope.file(lint.toUri()));
            try {
                task.analyze();
                compilations++;
                check(noErrors(diagnostics), "single-file lint resolves original binary symbols");
                check(session.summary().parsedUnits() == 1 && lint.reads == 1, "lint performs one parse/read");
                scopedParses += session.summary().parsedUnits();
            } finally { session.close(); }
            check(!session.summary().completed() && session.summary().closed(), "explicit phase caller closes ownership");
        }
        check(classBytes(lintOutput).isEmpty(), "lint produces no class file");
        Source p = source("selected/P", "package selected; public class P { public int value() { return dep.C.value(); } }");
        Source q = source("selected/Q", "package selected; class Q extends P { }");
        M3AST.Summary packageSummary = compile(Files.createDirectory(work.resolve("package-output")),
                List.of("-classpath", dependencies.toString(), "-sourcepath", ""),
                M3AST.Scope.packageSources("selected", List.of(q.toUri(), p.toUri())), List.of(p, q));
        check(packageSummary.parsedUnits() == 2 && p.reads == 1 && q.reads == 1, "selected package parses only its two sources");
        check(b.reads == bReads && c.reads == cReads && sameClassBytes(binaryBefore, classBytes(dependencies)),
                "lint and package compilation also preserve unchanged dependencies");
    }

    private static void implicitSourceBoundary(Path work) throws Exception {
        Path sourcePath = Files.createDirectory(work.resolve("outside-source"));
        Path outside = sourcePath.resolve("Outside.java");
        Files.writeString(outside, "class Outside { int value() { return 7; } }");
        for (boolean scoped : new boolean[] {false, true}) {
            Source input = source("Input", "class Input extends Outside { }");
            Path output = Files.createDirectory(work.resolve(scoped ? "scoped-implicit" : "unscoped-implicit"));
            try (FilesOwner files = new FilesOwner()) {
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                JavacTask task = task(files, output,
                        List.of("-sourcepath", sourcePath.toString(), "-Xprefer:source", "-implicit:none"), List.of(input), diagnostics);
                M3AST session = scoped ? M3AST.open(task, true, M3AST.Scope.file(input.toUri())) : M3AST.open(task);
                if (scoped) {
                    int[] published = {0};
                    session.addConsumer(new M3AST.Consumer() {
                        @Override public void finished(M3AST.Summary summary) {
                            if (summary.completed() && !summary.consumerFailed()) published[0]++;
                        }
                    });
                    expectMessage("M3AST source outside FILE scope", task::call);
                    check(files.reads(outside.toUri()) == 0, "implicit source is refused before character I/O");
                    check(session.summary().parsedUnits() == 1, "refused dependency has no PARSE event");
                    check(session.summary().closed() && session.summary().consumerFailed()
                            && !session.summary().completed() && published[0] == 0, "refusal closes without success publication");
                    outsideRefusals++;
                    scopedParses += session.summary().parsedUnits();
                } else {
                    check(task.call(), "unscoped original javac still completes an implicit source");
                    check(files.reads(outside.toUri()) > 0 && session.summary().parsedUnits() == 2,
                            "implicit:none suppresses generation, not dependency source parsing");
                    check(!Files.exists(output.resolve("Outside.class")), "implicit:none class-generation contract unchanged");
                }
                compilations++;
            }
        }
        Source selected = source("Selected", "class Selected { }");
        Source excluded = source("Excluded", "class Excluded { }");
        try (FilesOwner files = new FilesOwner()) {
            JavacTask task = task(files, Files.createDirectory(work.resolve("excluded-explicit")), List.of(),
                    List.of(selected, excluded), new DiagnosticCollector<>());
            M3AST session = M3AST.open(task, false, M3AST.Scope.file(selected.toUri()));
            expectMessage("M3AST source outside FILE scope", task::call);
            check(excluded.reads == 0 && session.summary().parsedUnits() == 1, "explicit extra source also refused before opening");
            outsideRefusals++;
            compilations++;
        }
        Path moduleSourcePath = Files.createDirectory(work.resolve("outside-module-source"));
        Path moduleInfo = moduleSourcePath.resolve("module-info.java");
        Files.writeString(moduleInfo, "module outside.module { }");
        Source probe = source("probe/Probe", "package probe; class Probe { }");
        try (FilesOwner files = new FilesOwner()) {
            JavacTask task = task(files, Files.createDirectory(work.resolve("excluded-module-probe")),
                    List.of("-sourcepath", moduleSourcePath.toString()), List.of(probe), new DiagnosticCollector<>());
            M3AST session = M3AST.open(task, false, M3AST.Scope.file(probe.toUri()));
            expectMessage("M3AST source outside FILE scope", task::call);
            check(files.reads(moduleInfo.toUri()) == 0, "silent module-name probe is refused before character I/O");
            check(session.summary().closed() && session.summary().consumerFailed(), "silent probe refusal closes ownership");
            outsideRefusals++;
            compilations++;
        }
    }

    private static void declarationBoundaries(Path work) throws Exception {
        Source wrong = source("wrong/Wrong", "package wrong; class Wrong { }");
        refusal(work.resolve("wrong-package"), M3AST.Scope.packageSources("expected", List.of(wrong.toUri())),
                List.of(wrong), "declaration outside PACKAGE scope");
        Source unnamed = source("Unnamed", "class Unnamed { }");
        compile(Files.createDirectory(work.resolve("unnamed-package")), List.of(),
                M3AST.Scope.packageSources("", List.of(unnamed.toUri())), List.of(unnamed));
        Source module = source("module-info", "module actual.module { }");
        refusal(work.resolve("wrong-module"), M3AST.Scope.moduleSources("expected.module", List.of(module.toUri())),
                List.of(module), "declaration outside MODULE scope");
        Source noModule = source("p/NoModule", "package p; class NoModule { }");
        refusal(work.resolve("unnamed-module"), M3AST.Scope.moduleSources("expected.module", List.of(noModule.toUri())),
                List.of(noModule), "declaration outside MODULE scope");
    }

    private static void modulesAndProject(Path work) throws Exception {
        Source descriptor = source("module-info", "module dependency.module { exports api; }");
        Source api = source("api/Api", "package api; public class Api { public static int value() { return 9; } }");
        Path dependency = Files.createDirectory(work.resolve("dependency-module"));
        compile(dependency, List.of(), null, List.of(descriptor, api));
        Map<Path, byte[]> before = classBytes(dependency);
        int descriptorReads = descriptor.reads;
        int apiReads = api.reads;
        Source selectedDescriptor = source("module-info", "module selected.module { requires dependency.module; }");
        Source implementation = source("selected/Use", "package selected; public class Use { int value() { return api.Api.value(); } }");
        M3AST.Summary summary = compile(Files.createDirectory(work.resolve("selected-module")),
                List.of("--module-path", dependency.toString()),
                M3AST.Scope.moduleSources("selected.module", List.of(selectedDescriptor.toUri(), implementation.toUri())),
                List.of(selectedDescriptor, implementation));
        check(summary.parsedUnits() == 2 && selectedDescriptor.reads == 1 && implementation.reads == 1,
                "one selected module uses its original two parsed units");
        check(descriptor.reads == descriptorReads && api.reads == apiReads && sameClassBytes(before, classBytes(dependency)),
                "required module descriptor and API remain unchanged binary inputs");
        Source first = source("first/First", "package first; public class First { }");
        Source second = source("second/Second", "package second; class Second extends first.First { }");
        M3AST.Summary project = compile(Files.createDirectory(work.resolve("project")), List.of(),
                M3AST.Scope.projectSources(List.of(second.toUri(), first.toUri())), List.of(first, second));
        check(project.parsedUnits() == 2 && first.reads == 1 && second.reads == 1, "project scope shares cross-package symbols");
    }

    private static void missingBinary(Path work) throws Exception {
        Source input = source("Missing", "class Missing extends unavailable.Dependency { }");
        try (FilesOwner files = new FilesOwner()) {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = task(files, Files.createDirectory(work.resolve("missing-binary")),
                    List.of("-classpath", "", "-sourcepath", ""), List.of(input), diagnostics);
            M3AST session = M3AST.open(task, false, M3AST.Scope.file(input.toUri()));
            check(!task.call() && !noErrors(diagnostics), "missing binaries retain real javac errors");
            check(session.summary().compilerErrors() > 0 && session.summary().parsedUnits() == 1,
                    "scope manufactures no placeholder symbol success");
            compilations++;
        }
    }

    private static void lifecycle(Path work) throws Exception {
        Source input = source("Phases", "class Phases { int value() { return 3; } }");
        try (FilesOwner files = new FilesOwner()) {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = task(files, Files.createDirectory(work.resolve("phases")), List.of(), List.of(input), diagnostics);
            M3AST.Scope scope = M3AST.Scope.file(input.toUri());
            M3AST session = M3AST.open(task, true, scope);
            check(M3AST.open(task, false, M3AST.Scope.file(input.toUri())) == session, "equivalent scope reuses single task handle");
            expect(IllegalArgumentException.class, () -> M3AST.open(task, false, M3AST.Scope.projectSources(List.of(input.toUri()))));
            CompilationUnitTree[] parsed = {null};
            Object[] projection = {null};
            M3AST.Projection<Object> key = new M3AST.Projection<>("actual-tree", unit -> new Object());
            session.addConsumer(new M3AST.Consumer() {
                @Override public void parsed(M3AST.Unit unit) throws Exception {
                    parsed[0] = unit.tree();
                    projection[0] = unit.project(key);
                    check(unit.source().text() == input.text, "PARSE borrows the authoritative source String");
                }
                @Override public void analyzed(M3AST.Unit unit) {
                    check(unit.tree() == parsed[0], "analysis uses the exact parsed compilation unit");
                }
            });
            session.addConsumer(new M3AST.Consumer() {
                @Override public void parsed(M3AST.Unit unit) throws Exception {
                    check(unit.tree() == parsed[0] && unit.project(key) == projection[0], "consumers share original tree and one projection");
                }
            });
            try {
                check(task.parse().iterator().next() == parsed[0], "parse API returns the same tree");
                expect(IllegalStateException.class, () -> M3AST.open(task, false, scope));
                task.analyze();
                task.generate();
                check(noErrors(diagnostics), "split phases compile successfully");
                check(input.reads == 1 && session.summary().parsedUnits() == 1, "parse/analyze/generate never reparse selected input");
                check(session.summary().projectionComputations() == 1 && session.summary().projectionHits() == 1,
                        "shared projection is computed exactly once in the original event");
                scopedParses += session.summary().parsedUnits();
                compilations++;
            } finally { session.close(); }
            check(session.summary().closed(), "split-phase owner releases compiler references");
        }
    }

    /** A first scope cannot retroactively admit source already read by javac. */
    private static void lateScopeAdmission(Path work) throws Exception {
        Source input = source("AlreadyParsed", "class AlreadyParsed { }");
        try (FilesOwner files = new FilesOwner()) {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = task(files, Files.createDirectory(work.resolve("late-scope")),
                    List.of(), List.of(input), diagnostics);
            CompilationUnitTree tree = task.parse().iterator().next();
            check(input.reads == 1, "original task parsed before any M3AST handle existed");
            expect(IllegalStateException.class, () -> M3AST.open(task, true, M3AST.Scope.file(input.toUri())));
            expect(IllegalStateException.class, () -> M3AST.open(task, false,
                    M3AST.Scope.file(URI.create("string:///NeverSelected.java"))));
            expect(IllegalStateException.class, () -> M3AST.open(task, true,
                    M3AST.Scope.projectSources(List.of(input.toUri()))));
            try (M3AST session = M3AST.open(task)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        check(unit.tree() == tree, "late refusal preserves the original parsed tree");
                        check(!((Boolean) plugin().getClass().getMethod("retainsSyntax", M3AST.Unit.class)
                                .invoke(null, unit)), "late refusal did not enable syntax retention");
                    }
                });
                task.analyze();
                check(session.summary().parsedUnits() == 0 && session.summary().analyzedTypes() == 1,
                        "existing unscoped late-open behavior remains available");
                task.generate();
            }
            check(noErrors(diagnostics) && input.reads == 1, "refusal leaves original phased compilation usable");
            compilations++;
        }
        Source reading = source("ReadingSource", "class ReadingSource { }");
        try (FilesOwner files = new FilesOwner()) {
            JavacTask task = task(files, Files.createDirectory(work.resolve("reading-scope")),
                    List.of(), List.of(reading), new DiagnosticCollector<>());
            reading.beforeRead = () -> expect(IllegalStateException.class,
                    () -> M3AST.open(task, false, M3AST.Scope.file(reading.toUri())));
            task.parse();
            task.analyze();
            task.generate();
            check(reading.reads == 1, "source admission closes before character I/O and PARSE events");
            compilations++;
        }
    }

    private static void refusal(Path output, M3AST.Scope scope, List<Source> sources, String message) throws Exception {
        Files.createDirectory(output);
        try (FilesOwner files = new FilesOwner()) {
            JavacTask task = task(files, output, List.of(), sources, new DiagnosticCollector<>());
            M3AST session = M3AST.open(task, false, scope);
            expectMessage(message, task::call);
            check(session.summary().closed() && session.summary().consumerFailed() && !session.summary().completed(),
                    "scope declaration refusal closes the original session");
            compilations++;
        }
    }

    /** Exercise the exact plugin init selected by existing build-tool compiler arguments. */
    private static void pluginScopes(Path work) throws Exception {
        Module compiler = M3AST.class.getModule();
        if (!compiler.canUse(M3AST.Consumer.class)) {
            // Test-only: grant the uses declaration in M3JDK21's real compiler
            // descriptor when this contract runs on the supplied stock JDK.
            Class.forName("jdk.internal.module.Modules").getMethod("addUses", Module.class, Class.class)
                    .invoke(null, compiler, M3AST.Consumer.class);
        }
        check(compiler.canUse(M3AST.Consumer.class), "actual plugin has its product consumer-service declaration");
        for (String selection : List.of("file", "package:p", "project", "module:selected.module")) {
            Source input = selection.startsWith("module:")
                    ? source("module-info", "module selected.module { }")
                    : source("p/PluginInput", "package p; class PluginInput { }");
            try (FilesOwner files = new FilesOwner()) {
                JavacTask task = task(files, Files.createTempDirectory(work, "plugin-"), List.of(),
                        List.of(input), new DiagnosticCollector<>());
                plugin().init(task, "lossless", "scope=" + selection);
                check(input.reads == 0, "plugin derives workset from original arguments without source I/O");
                M3AST session = M3AST.open(task);
                check(task.call() && session.summary().parsedUnits() == 1 && input.reads == 1,
                        "original plugin scope compiles its original task: " + selection);
                compilations++;
                scopedParses += session.summary().parsedUnits();
            }
        }
        Source a = source("PluginA", "class PluginA { }");
        Source b = source("PluginB", "class PluginB { }");
        try (FilesOwner files = new FilesOwner()) {
            JavacTask task = task(files, Files.createTempDirectory(work, "plugin-refusal-"), List.of(),
                    List.of(a, b), new DiagnosticCollector<>());
            expect(IllegalArgumentException.class, () -> plugin().init(task, "scope=file"));
            expect(IllegalArgumentException.class, () -> plugin().init(task, "scope=unknown"));
            expect(IllegalArgumentException.class, () -> plugin().init(task, "scope=project", "scope=project"));
            check(a.reads == 0 && b.reads == 0, "plugin option refusals do not open either source");
        }
        Path outsidePath = Files.createDirectory(work.resolve("plugin-outside-source"));
        Path outside = outsidePath.resolve("Outside.java");
        Files.writeString(outside, "class Outside { }");
        Source selected = source("PluginSelected", "class PluginSelected extends Outside { }");
        try (FilesOwner files = new FilesOwner()) {
            JavacTask task = task(files, Files.createTempDirectory(work, "plugin-implicit-"),
                    List.of("-sourcepath", outsidePath.toString(), "-Xprefer:source"),
                    List.of(selected), new DiagnosticCollector<>());
            plugin().init(task, "scope=file");
            M3AST session = M3AST.open(task);
            expectMessage("M3AST source outside FILE scope", task::call);
            check(files.reads(outside.toUri()) == 0 && session.summary().parsedUnits() == 1,
                    "plugin path enforces the same source boundary before I/O");
            outsideRefusals++;
            compilations++;
        }
    }

    private static Plugin plugin() throws ReflectiveOperationException {
        return (Plugin) Class.forName("com.sun.tools.javac.m3.M3ASTPC").getConstructor().newInstance();
    }

    private static M3AST.Summary compile(Path output, List<String> options, M3AST.Scope scope, List<Source> sources) throws Exception {
        try (FilesOwner files = new FilesOwner()) {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = task(files, output, options, sources, diagnostics);
            M3AST session = scope == null ? M3AST.open(task) : M3AST.open(task, true, scope);
            check(task.call(), "original task compiles: " + diagnostics.getDiagnostics());
            compilations++;
            if (scope != null) scopedParses += session.summary().parsedUnits();
            return session.summary();
        }
    }

    private static JavacTask task(JavaFileManager files, Path output, List<String> options,
            List<Source> sources, DiagnosticCollector<JavaFileObject> diagnostics) {
        ArrayList<String> all = new ArrayList<>(List.of("-proc:none", "-d", output.toString()));
        all.addAll(options);
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(new StringWriter(), files, diagnostics, all, null, sources);
    }

    private static Source source(String name, String text) { return new Source(name, text); }

    private static final class Source extends SimpleJavaFileObject {
        final String text;
        int reads;
        Runnable beforeRead;
        Source(String name, String text) { super(URI.create("string:///" + name + ".java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            if (beforeRead != null) beforeRead.run();
            return text;
        }
    }

    /** Instrument source I/O discovered by the real standard file manager. */
    private static final class FilesOwner extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<URI, Integer> reads = new HashMap<>();
        FilesOwner() { super(ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)); }
        int reads(URI source) { return reads.getOrDefault(source, 0); }
        private JavaFileObject wrap(JavaFileObject source) {
            return source == null || source.getKind() != JavaFileObject.Kind.SOURCE ? source : new Counted(source);
        }
        private final class Counted extends ForwardingJavaFileObject<JavaFileObject> {
            Counted(JavaFileObject source) { super(source); }
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                reads.merge(toUri(), 1, Integer::sum);
                return super.getCharContent(ignoreEncodingErrors);
            }
            JavaFileObject original() { return fileObject; }
        }
        private FileObject unwrap(FileObject source) { return source instanceof Counted counted ? counted.original() : source; }
        @Override public Iterable<JavaFileObject> list(Location location, String packageName,
                Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
            ArrayList<JavaFileObject> result = new ArrayList<>();
            for (JavaFileObject source : super.list(location, packageName, kinds, recurse)) result.add(wrap(source));
            return result;
        }
        @Override public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
            return wrap(super.getJavaFileForInput(location, className, kind));
        }
        @Override public String inferBinaryName(Location location, JavaFileObject source) {
            return super.inferBinaryName(location, (JavaFileObject) unwrap(source));
        }
        @Override public boolean isSameFile(FileObject first, FileObject second) { return super.isSameFile(unwrap(first), unwrap(second)); }
        @Override public boolean contains(Location location, FileObject source) throws IOException { return super.contains(location, unwrap(source)); }
    }

    private static Map<Path, byte[]> classBytes(Path directory) throws IOException {
        Map<Path, byte[]> result = new HashMap<>();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".class")).toList()) {
                result.put(directory.relativize(path), Files.readAllBytes(path));
            }
        }
        return result;
    }

    private static boolean sameClassBytes(Map<Path, byte[]> first, Map<Path, byte[]> second) {
        if (!first.keySet().equals(second.keySet())) return false;
        for (Path path : first.keySet()) if (!Arrays.equals(first.get(path), second.get(path))) return false;
        return true;
    }

    private static boolean noErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics().stream().noneMatch(item -> item.getKind() == Diagnostic.Kind.ERROR);
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> kind, Action action) {
        try { action.run(); }
        catch (Throwable failure) { if (kind.isInstance(failure)) { checks++; return; } throw new AssertionError("expected " + kind, failure); }
        throw new AssertionError("expected " + kind);
    }

    private static void expectMessage(String message, Action action) {
        try { action.run(); }
        catch (Throwable failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof IllegalArgumentException && cause.getMessage().contains(message)) { checks++; return; }
            }
            throw new AssertionError("expected refusal " + message, failure);
        }
        throw new AssertionError("expected refusal " + message);
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
