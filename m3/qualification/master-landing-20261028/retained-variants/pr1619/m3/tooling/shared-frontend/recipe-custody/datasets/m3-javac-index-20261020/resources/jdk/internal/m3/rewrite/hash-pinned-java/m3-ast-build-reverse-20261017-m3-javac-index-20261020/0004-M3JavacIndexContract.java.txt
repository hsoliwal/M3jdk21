// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;

/** Original javac tree -> existing primitive M3ASTIndex, without a second parse. */
public final class M3JavacIndexContract {
    private static long checks;
    private M3JavacIndexContract() { }

    private static void ok(boolean valid, String reason) {
        checks++;
        if (!valid) throw new AssertionError(reason);
    }

    private static void equal(Object a, Object b, String reason) {
        ok(Objects.equals(a, b), reason + " expected=" + a + " actual=" + b);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void rejects(Class<? extends Throwable> expected, Checked work)
            throws Exception {
        checks++;
        try { work.run(); }
        catch (Throwable problem) {
            if (expected.isInstance(problem)) return;
            throw new AssertionError("wrong refusal: " + problem, problem);
        }
        throw new AssertionError("did not refuse " + expected.getName());
    }

    private static final class Memory extends SimpleJavaFileObject {
        private final String text;
        private int reads;
        Memory(String path, String content) {
            super(URI.create("memory:///" + path), JavaFileObject.Kind.SOURCE);
            text = content;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return text;
        }
    }

    private static M3ASTIndex indexed(CompilationUnitTree unit, Trees trees,
            Memory owner, String text) {
        return M3ASTIngress.javac(M3File.of(owner.toUri(), text), unit, trees,
                text, new M3ASTIngress.Limits(300_000, 40_000, 256));
    }

    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler());
        Path output = Files.createTempDirectory("m3-original-javac");
        Path classes = Files.createDirectory(output.resolve("classes"));
        Path emptySource = Files.createDirectory(output.resolve("sourcepath"));
        String dep = "package dep; public class B { public static final int CONST = 19; }";
        String program = "package app;\r\n"
                + "import dep.B;\n"
                + "interface Marker {}\n"
                + "public final class A implements Marker {\n"
                + "  static final int N = B.CONST;\n"
                + "  public String text = \"\\u00E9\";\n"
                + "  public int method() { int local = 2; return N + local; }\n"
                + "}\n";
        Memory dependency = new Memory("dep/B.java", dep);
        Memory selected = new Memory("app/A.java", program);
        var problems = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                problems, null, StandardCharsets.UTF_8)) {
            boolean compiled = compiler.getTask((Writer) null, manager, problems,
                    List.of("-proc:none", "-d", classes.toString()),
                    null, List.of(dependency)).call();
            ok(compiled, "precompile dependency only once: " + problems.getDiagnostics());
            byte[] untouched = Files.readAllBytes(classes.resolve("dep/B.class"));
            int previousDependencyReads = dependency.reads;
            JavacTask task = (JavacTask) compiler.getTask(null, manager, problems,
                    List.of("-proc:none", "-implicit:none",
                            "-classpath", classes.toString(),
                            "-sourcepath", emptySource.toString(),
                            "-d", classes.toString()), null, List.of(selected));
            CompilationUnitTree unit = task.parse().iterator().next();
            int originalSourceReads = selected.reads;
            Trees trees = Trees.instance(task);
            M3ASTIndex index = indexed(unit, trees, selected, program);
            equal(M3ASTIndex.Domain.JAVAC, index.domain(), "original javac domain");
            ok(index.source().uri().equals(selected.toUri()), "same file owner");
            ok(index.nodeCount() > 15, "complete javac preorder");
            equal(program, index.sourceText(0), "same exact UTF-16 source");
            equal(1, index.line(0), "one-based root line");
            ok(selected.reads == originalSourceReads && originalSourceReads > 0,
                    "index never requests a second source parse/read");
            ok(dependency.reads == previousDependencyReads,
                    "binary dependency is never re-read from its source");

            int cls = -1, inter = -1, field = -1, method = -1;
            int str = -1, number = -1;
            for (int row = 0; row < index.nodeCount(); row++) {
                if ("CLASS".equals(index.kind(row)) && "A".equals(index.simpleName(row))) cls = row;
                if ("INTERFACE".equals(index.kind(row))
                        && "Marker".equals(index.simpleName(row))) inter = row;
                if ("VARIABLE".equals(index.kind(row)) && "N".equals(index.simpleName(row))) field = row;
                if ("METHOD".equals(index.kind(row))
                        && "method".equals(index.simpleName(row))) method = row;
                if ("STRING_LITERAL".equals(index.kind(row))) str = row;
                if ("INT_LITERAL".equals(index.kind(row))) number = row;
                if (index.hasExactRange(row)) {
                    ok(index.start(row) <= index.end(row), "bounded UTF-16 source range");
                    ok(index.line(row) >= 1, "line derived only from exact source");
                    ok(index.spanLines(row) >>> Integer.SIZE >= 1, "packed start line");
                    equal(index.exactSha256(row), index.exactSha256(row),
                            "stable lazy exact SHA");
                }
                equal(index.structuralSha256(row), index.structuralSha256(row),
                        "stable structural SHA, no serialization of compiler objects");
            }
            ok(cls >= 0 && inter >= 0 && field >= 0 && method >= 0
                    && str >= 0 && number >= 0, "all required javac occurrences");
            ok((index.flags(cls) & M3ASTIndex.FINAL) != 0, "class FINAL");
            ok((index.flags(inter) & M3ASTIndex.INTERFACE) != 0, "INTERFACE");
            ok((index.flags(field) & (M3ASTIndex.STATIC | M3ASTIndex.FINAL))
                    == (M3ASTIndex.STATIC | M3ASTIndex.FINAL), "STATIC FINAL field");
            equal("é", index.literalValue(str), "javac-decoded Unicode string literal");
            equal("2", index.literalValue(number), "javac numeric constant");
            equal(method, index.firstEnclosing(number, "METHOD"),
                    "parent-lane enclosing method search");
            equal("N", index.simpleName(field), "interned identifier");
            equal(4, index.line(cls), "CRLF-aware source line");
            ok(index.primitiveBytes() >= index.nodeCount() * 8L * Integer.BYTES,
                    "primitive geometry retained");

            M3File drift = M3File.of(selected.toUri(), program + " ");
            rejects(IllegalArgumentException.class, () -> M3ASTIngress.javac(drift,
                    unit, trees, program, M3ASTIngress.Limits.defaults()));
            rejects(IllegalArgumentException.class, () -> M3ASTIngress.javac(
                    M3File.of(URI.create("memory:///app/wrong.java"), program), unit,
                    trees, M3ASTIngress.Limits.defaults()));
            rejects(IllegalArgumentException.class, () -> M3ASTIngress.javac(
                    M3File.of(selected.toUri(), program), unit, trees,
                    new M3ASTIngress.Limits(300_000, 5, 256)));

            task.analyze();
            task.generate();
            ok(Files.isRegularFile(classes.resolve("app/A.class")),
                    "compile only selected A against existing B.class");
            ok(Arrays.equals(untouched, Files.readAllBytes(classes.resolve("dep/B.class"))),
                    "unchanged B.class bytes never rebuilt");
            ok(dependency.reads == previousDependencyReads,
                    "dependency symbols supplied by existing binary");
            ok(selected.reads >= originalSourceReads,
                    "compiler retains original source read authority");
        }
        System.out.println("M3_JAVAC_INDEX_PASS checks=" + checks);
    }
}
