// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.parser.M3ScopedCompilation;

/** Real javac source closure, no compiler API substitutes. */
public final class M3ScopedCompilationTest {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static void check(boolean ok) {
        CHECKS.incrementAndGet();
        if (!ok) throw new AssertionError("scoped javac contract check=" + CHECKS.get());
    }
    private static M3ScopedCompilation.Unit source(String name, String text) {
        return new M3ScopedCompilation.Unit(URI.create("string:///" + name), text);
    }
    private static M3ScopedCompilation.Request request(M3ScopedCompilation.Scope scope,
            M3ScopedCompilation.Phase phase, List<M3ScopedCompilation.Unit> sources,
            List<Path> binaries, Path output) {
        return new M3ScopedCompilation.Request(scope, phase, sources, binaries,
                List.of(), output, List.of());
    }
    public static void main(String[] unused) throws Exception {
        Path tmp = Files.createTempDirectory("m3-scope-test");
        var dep = source("dep/External.java",
                "package dep; public class External { public static int value(){return 42;} }");
        Path depOut = tmp.resolve("deps");
        var depResult = M3ScopedCompilation.execute(request(M3ScopedCompilation.Scope.MODULE,
                M3ScopedCompilation.Phase.GENERATE, List.of(dep), List.of(), depOut));
        check(depResult.success() && depResult.parsedUnits() == 1 && depResult.generatedFiles() >= 1);
        check(Files.isRegularFile(depOut.resolve("dep/External.class")));
        var unit = source("use/One.java",
                "package use; public class One { public int f() { return dep.External.value(); } }");
        var file = M3ScopedCompilation.execute(request(M3ScopedCompilation.Scope.FILE,
                M3ScopedCompilation.Phase.ANALYZE, List.of(unit), List.of(depOut), null));
        check(file.success() && file.parsedUnits() == 1 && file.generatedFiles() == 0);
        var missing = M3ScopedCompilation.execute(request(M3ScopedCompilation.Scope.FILE,
                M3ScopedCompilation.Phase.ANALYZE, List.of(unit), List.of(), null));
        check(!missing.success() && missing.parsedUnits() == 1);
        var syntax = M3ScopedCompilation.execute(request(M3ScopedCompilation.Scope.FILE,
                M3ScopedCompilation.Phase.SYNTAX, List.of(unit), List.of(), null));
        check(syntax.success() && syntax.parsedUnits() == 1);
        var another = source("use/Two.java", "package use; public class Two { private One one; }");
        Path packageOut = tmp.resolve("package");
        var packageResult = M3ScopedCompilation.execute(request(M3ScopedCompilation.Scope.PACKAGE,
                M3ScopedCompilation.Phase.GENERATE, List.of(unit, another), List.of(depOut), packageOut));
        check(packageResult.success() && packageResult.parsedUnits() == 2);
        check(Files.isRegularFile(packageOut.resolve("use/One.class")));
        check(Files.isRegularFile(packageOut.resolve("use/Two.class")));
        try {
            request(M3ScopedCompilation.Scope.FILE, M3ScopedCompilation.Phase.SYNTAX,
                    List.of(unit, another), List.of(), null);
            throw new AssertionError("multi source FILE scope admitted");
        } catch (IllegalArgumentException expected) { check(true); }
        System.out.println("M3_SCOPED_COMPILATION_PASS checks=" + CHECKS.get());
    }
}
