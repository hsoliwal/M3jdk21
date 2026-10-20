// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTSourceSession;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3ScopedJavac;

/** Real Java 21 compiler oracle: selected sources parse once, given binaries never reparse. */
public final class M3ScopedJavacContract {
    private static int assertions;
    private M3ScopedJavacContract() { }

    private static final class Source extends SimpleJavaFileObject {
        private final String content;
        private int reads;
        Source(String file, String content) {
            super(URI.create("memory:/" + file), Kind.SOURCE);
            this.content = content;
        }
        @Override public CharSequence getCharContent(boolean ignored) {
            reads++;
            return content;
        }
        @Override public String getName() { return toUri().getPath(); }
    }

    private static void ok(boolean yes, String reason) {
        assertions++;
        if (!yes) throw new AssertionError(reason);
    }
    private static void same(Object expected, Object found, String reason) {
        assertions++;
        if (!Objects.equals(expected, found)) {
            throw new AssertionError(reason + ": " + expected + " versus " + found);
        }
    }
    private static M3ScopedJavac.Plan plan(M3ScopedJavac.Scope scope, Path output,
            List<Path> classes, List<Path> modules, Source... sources) {
        return new M3ScopedJavac.Plan(scope, List.<JavaFileObject>of(sources),
                classes, modules, output, "isolated-original-compiler-context");
    }
    private static void rejected(Class<? extends Throwable> expected,
                                 Checked action, String reason) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(reason + ": wrong error " + failure, failure);
        }
        throw new AssertionError("expected refusal: " + reason);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void originalTask() throws Exception {
        Source file = new Source("p/Original.java", "package p; class Original { int n; }");
        AtomicInteger first = new AtomicInteger(), second = new AtomicInteger();
        CompilationUnitTree[] a = new CompilationUnitTree[1], b = new CompilationUnitTree[1];
        try (StandardJavaFileManager manager =
                ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                        null, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(
                    null, manager, null, List.of("-proc:none", "--release", "21"), null, List.of(file));
            try (var one = M3ScopedJavac.observeOriginal(task, Set.of(file.toUri()), (t, unit) -> {
                ok(t == task, "reuse same original javac task");
                a[0] = unit;
                first.incrementAndGet();
            }); var two = M3ScopedJavac.observeOriginal(task, Set.of(file.toUri()), (t, unit) -> {
                b[0] = unit;
                second.incrementAndGet();
            })) {
                task.parse(); // Exactly one caller-owned parser invocation.
                same(1, one.observedParseEvents(), "one parse observed");
                same(1, two.observedParseEvents(), "same parse observed by second tool");
            }
        }
        same(1, file.reads, "original source was read only once");
        same(1, first.get(), "first callback");
        same(1, second.get(), "second callback");
        ok(a[0] == b[0], "consumers borrow identical CompilationUnitTree");
    }

    private static void scopedCompiles(Path root) throws Exception {
        Source given = new Source("ext/Given.java",
                "package ext; public class Given { public int value() { return 42; } }");
        Path givenClasses = root.resolve("given");
        var base = M3ScopedJavac.execute(plan(M3ScopedJavac.Scope.FILE,
                givenClasses, List.of(), List.of(), given),
                M3ScopedJavac.Goal.COMPILE, (t, u) -> { });
        ok(base.succeeded() && base.parsedEvents() == 1
                && Files.isRegularFile(givenClasses.resolve("ext/Given.class")),
                "produce one selected external binary dependency");
        same(1, given.reads, "one source read to compile supplied symbols");

        Source use = new Source("user/Only.java",
                "package user; public class Only { public int f() { return new ext.Given().value(); } }");
        Path single = root.resolve("single");
        var one = M3ScopedJavac.execute(plan(M3ScopedJavac.Scope.FILE,
                single, List.of(givenClasses), List.of(), use),
                M3ScopedJavac.Goal.COMPILE, (t, u) -> { });
        ok(one.succeeded() && one.parsedEvents() == 1
                && Files.isRegularFile(single.resolve("user/Only.class")),
                "compile one file with other package symbols GIVEN");
        same(1, given.reads, "given dependency was never reparsed");
        same(1, use.reads, "selected source parsed once");

        Source missing = new Source("user/Missing.java",
                "package user; class Missing { ext.Given value; }");
        var failure = M3ScopedJavac.execute(plan(M3ScopedJavac.Scope.FILE,
                root.resolve("no-dependency"), List.of(), List.of(), missing),
                M3ScopedJavac.Goal.COMPILE, (t, u) -> { });
        ok(!failure.succeeded() && failure.parsedEvents() == 1,
                "unprovided symbol must fail instead of searching/reparsing source");

        Source pkgA = new Source("pkg/First.java",
                "package pkg; public class First { ext.Given object = new ext.Given(); }");
        Source pkgB = new Source("pkg/Second.java",
                "package pkg; public class Second { First item = new First(); }");
        var pkg = M3ScopedJavac.execute(plan(M3ScopedJavac.Scope.PACKAGE,
                root.resolve("package"), List.of(givenClasses), List.of(), pkgA, pkgB),
                M3ScopedJavac.Goal.COMPILE, (t, u) -> { });
        ok(pkg.succeeded() && pkg.parsedEvents() == 2
                && Files.isRegularFile(root.resolve("package/pkg/Second.class")),
                "compile just two supplied package units");
        same(1, pkgA.reads, "selected package A read once");
        same(1, pkgB.reads, "selected package B read once");

        Source parseOnly = new Source("parse/Only.java", "package parse; class Only { int x; }");
        var parse = M3ScopedJavac.execute(plan(M3ScopedJavac.Scope.FILE,
                root.resolve("parse-only"), List.of(), List.of(), parseOnly),
                M3ScopedJavac.Goal.PARSE, (t, u) -> { });
        ok(parse.succeeded() && parse.parsedEvents() == 1
                && parse.generatedEvents() == 0, "parse scope avoids compilation work");
        same(1, parseOnly.reads, "parse input read only once");
    }

    private static void modulesAndProject(Path root) throws Exception {
        Path modules = root.resolve("modules");
        Files.createDirectory(modules);
        Source infoA = new Source("m.one/module-info.java", "module m.one { exports base; }");
        Source modA = new Source("base/Api.java",
                "package base; public class Api { public static int answer() { return 42; } }");
        Source infoB = new Source("m.two/module-info.java", "module m.two { requires m.one; }");
        Source modB = new Source("consumer/Client.java",
                "package consumer; public class Client { int n = base.Api.answer(); }");
        M3ScopedJavac.Plan first = plan(M3ScopedJavac.Scope.MODULE,
                modules.resolve("m.one"), List.of(), List.of(), infoA, modA);
        M3ScopedJavac.Plan second = plan(M3ScopedJavac.Scope.MODULE,
                modules.resolve("m.two"), List.of(), List.of(modules), infoB, modB);
        var parts = M3ScopedJavac.executeProject(List.of(first, second),
                M3ScopedJavac.Goal.COMPILE, (t, u) -> { });
        ok(parts.size() == 2 && parts.get(0).succeeded() && parts.get(1).succeeded(),
                "compile selected project modules in dependency order");
        same(2, parts.get(0).parsedEvents(), "first module exactly two units");
        same(2, parts.get(1).parsedEvents(), "second module exactly two units");
        ok(Files.isRegularFile(modules.resolve("m.one/base/Api.class"))
                && Files.isRegularFile(modules.resolve("m.two/consumer/Client.class")),
                "two module outputs from GIVEN compiled-module dependencies");
        same(1, infoA.reads, "module declaration A parsed once");
        same(1, modA.reads, "module source A parsed once");
        same(1, infoB.reads, "module declaration B parsed once");
        same(1, modB.reads, "module source B parsed once");
        rejected(IllegalArgumentException.class,
                () -> M3ScopedJavac.executeProject(List.of(first, first),
                        M3ScopedJavac.Goal.PARSE, (t, u) -> { }),
                "duplicate file across project scopes refuses before parse");
    }

    private static void lazyReuse() {
        String text = "class A {}";
        M3File source = M3File.of(URI.create("memory:/A.java"), text);
        AtomicInteger projected = new AtomicInteger();
        M3ASTIndex result;
        try (M3ASTSourceSession<String> session = M3ASTSourceSession.fromParsed(
                source, "generation-with-supplied-symbol-context", "ORIGINAL_AST",
                (file, original) -> {
                    same("ORIGINAL_AST", original, "use original parsed tree");
                    projected.incrementAndGet();
                    return singleRoot(file);
                })) {
            same(0, session.projectionComputations(), "no preemptive AST/index materialization");
            ok(!session.projected(), "index is lazy");
            result = session.index();
            ok(result == session.index(), "two tools share same detached AST projection identity");
            same(1, session.projectionComputations(), "projection computed only once");
            same(1, projected.get(), "one original AST projection, zero reparses");
        }
        same("ROOT", result.kind(0), "published immutable index survives session closure");
        rejected(IllegalArgumentException.class, () -> {
            try (M3ASTSourceSession<String> wrong = M3ASTSourceSession.fromParsed(
                    source, "wrong-owner", "TREE",
                    (file, original) -> singleRoot(M3File.of(URI.create("memory:/other"), "x")))) {
                wrong.index();
            }
        }, "foreign source owner refuses");
    }

    private static M3ASTIndex singleRoot(M3File source) {
        M3ASTIndex.Builder builder = M3ASTIndex.builder(source,
                M3ASTIndex.Domain.ECLIPSE_JDT, 4);
        builder.add(-1, "ROOT", 0, source.length(), M3ASTIndex.EXACT_RANGE);
        return builder.seal();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one absent work directory expected");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(root)) throw new IOException("work directory must not preexist");
        Files.createDirectories(root);
        originalTask();
        scopedCompiles(root);
        modulesAndProject(root);
        lazyReuse();
        System.out.println("M3_AST_SCOPE_PARSE_ONCE_PASS assertions=" + assertions);
    }
}
