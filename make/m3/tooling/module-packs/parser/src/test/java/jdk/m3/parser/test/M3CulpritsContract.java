// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3Culprits;
import jdk.m3.parser.M3File;

/**
 * Culprit lanes contract: one real javac parse, one JAVAC-domain index, exact (line, culprit) findings for the
 * M3 hard limits, constant and message positions excluded, limits as options, foreign domains refused.
 */
public final class M3CulpritsContract {
    private static final String TEXT = """
            package demo;
            public class Demo {
                static final int LIMIT = 42;
                interface Codes { int MAX = 99; }
                Integer sum(int[] values) {
                    int total = 0;
                    for (int value : values) {
                        total += value * 3;
                    }
                    System.out.println("done");
                    String label = "x";
                    if (total > 10) {
                        return null;
                    }
                    throw new IllegalStateException("too small " + label);
                }
                @SuppressWarnings("unused")
                public String getDescription() {
                    return "metadata text";
                }
            }
            """;
    private static final Set<String> EXPECTED = new TreeSet<>(List.of(
            "7:LOOP", "8:MAGIC_NUMBER", "10:SYSTEM_OUT", "11:FLOATING_STRING", "12:MAGIC_NUMBER", "13:RETURN_NULL"));
    /** javac's method range starts at its modifiers, so the annotated getDescription spans four lines. */
    private static final int SHORT_METHOD = 4;
    private static final int SHORT_CLASS = 5;
    private static int checks;

    private M3CulpritsContract() { }

    public static void main(String[] arguments) throws Exception {
        M3ASTIndex index = index(TEXT);
        defaults(index);
        limits(index);
        refusals();
        System.out.println("M3_CULPRITS_PASS checks=" + checks + " groups=3");
    }

    private static void defaults(M3ASTIndex index) {
        M3Culprits.Report report = M3Culprits.scan(index, M3Culprits.Limits.defaults());
        Set<String> found = new TreeSet<>();
        for (int i = 0; i < report.size(); i++) found.add(report.line(i) + ":" + report.culprit(i));
        check(found.equals(EXPECTED), "exact findings, found " + found);
        check(report.count(M3Culprits.Culprit.MAGIC_NUMBER) == 2, "two magic numbers; 42 and 99 are constants");
        check(report.count(M3Culprits.Culprit.FLOATING_STRING) == 1,
                "println argument, throw message and getDescription text are not floating");
        check(report.sealed() && report.primitiveBytes() == report.size() * 5L, "two primitive lanes per finding");
        check(report.tsv().lines().count() == report.size(), "one TSV line per finding");
        check(report.index() == index, "the report borrows the original index");
    }

    private static void limits(M3ASTIndex index) {
        M3Culprits.Report report = M3Culprits.scan(index, new M3Culprits.Limits(SHORT_METHOD, SHORT_CLASS));
        check(report.count(M3Culprits.Culprit.METHOD_LENGTH) == 1, "sum exceeds a 4-line method limit; the annotated getDescription does not");
        check(report.count(M3Culprits.Culprit.CLASS_LENGTH) == 1, "only the top-level class is measured against the class limit");
        check(report.size() == EXPECTED.size() + 2, "limit findings add to the default findings");
        expect(IllegalArgumentException.class, () -> new M3Culprits.Limits(0, SHORT_CLASS));
        check(true, "zero line limits are refused");
    }

    private static void refusals() {
        M3File gradle = M3File.of(URI.create("memory:/build.gradle"), "plugins { }");
        M3ASTIndex foreign = M3ASTIngress.gradleScript(gradle, false, M3ASTIngress.Limits.defaults());
        expect(IllegalArgumentException.class, () -> M3Culprits.scan(foreign, M3Culprits.Limits.defaults()));
        check(true, "a foreign domain is refused");
        expect(NullPointerException.class, () -> M3Culprits.scan(null, M3Culprits.Limits.defaults()));
        check(true, "a missing index is refused");
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        Source(String text) { super(URI.create("memory:/Demo.java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignored) { return text; }
    }

    private static M3ASTIndex index(String text) throws Exception {
        Source input = new Source(text);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files, diagnostics,
                    List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(input));
            CompilationUnitTree tree = task.parse().iterator().next();
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            return M3ASTIngress.javac(M3File.of(input.toUri(), text), tree, positions, M3ASTIngress.Limits.defaults());
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + thrown, thrown);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
