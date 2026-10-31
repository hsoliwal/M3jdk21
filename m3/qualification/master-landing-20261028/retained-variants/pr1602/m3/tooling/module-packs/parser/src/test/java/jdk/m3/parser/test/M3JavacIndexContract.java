// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.net.URI;
import java.util.List;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3JavacIndexIngress;

public final class M3JavacIndexContract {
    private static int checks;
    private M3JavacIndexContract() { }
    private static void ok(boolean yes, String msg) { checks++; if (!yes) throw new AssertionError(msg); }
    private static final class Source extends SimpleJavaFileObject {
        final String text; int reads;
        Source(String name, String text) { super(URI.create("memory:/" + name + ".java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignoreErrors) { reads++; return text; }
    }
    private static M3ASTIndex capture(Source input, M3ASTIngress.Limits limits) throws Exception {
        var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
        try (manager) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, manager, null,
                    List.of("-proc:none", "--release", "21"), null, List.of(input));
            CompilationUnitTree unit = task.parse().iterator().next();
            M3ASTIndex index = M3JavacIndexIngress.javac(
                    M3File.of(input.toUri(), input.text), unit, Trees.instance(task), limits);
            ok(input.reads == 1, "existing javac source is not re-read for indexing");
            return index;
        }
    }
    private static M3ASTIngress.Limits limits() { return new M3ASTIngress.Limits(300000, 30000, 200); }
    private static void rejects(Runnable call) {
        checks++; try { call.run(); throw new AssertionError("expected refusal"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        String text = "class Demo {\r\n static final int N = 42;\r\n interface Inner {}\r\n int plus(int x) { return x + N; }\r\n}\r\n";
        M3ASTIndex index = capture(new Source("Demo", text), limits());
        ok(index.domain() == M3ASTIndex.Domain.JAVAC, "original javac domain");
        ok(index.nodeCount() > 15 && index.parent(0) == -1, "original preorder and parents");
        boolean cls = false, constant = false, inner = false, literal = false, method = false;
        for (int i = 0; i < index.nodeCount(); i++) {
            if ("Demo".equals(index.simpleName(i))) cls = true;
            if ("N".equals(index.simpleName(i)) && "JAVAC_VARIABLE".equals(index.kind(i))) {
                constant = (index.flags(i) & (M3ASTIndex.STATIC | M3ASTIndex.FINAL))
                        == (M3ASTIndex.STATIC | M3ASTIndex.FINAL);
                ok(index.line(i) == 2 && index.spanLines(i).last() == 2, "javac line map for constant");
            }
            if ("Inner".equals(index.simpleName(i))) inner = (index.flags(i) & M3ASTIndex.INTERFACE) != 0;
            if ("42".equals(index.literal(i))) literal = true;
            if ("plus".equals(index.simpleName(i))) method = true;
            if ("x".equals(index.simpleName(i)) && "JAVAC_IDENTIFIER".equals(index.kind(i))) {
                int enclosing = index.firstEnclosing(i, "JAVAC_METHOD");
                ok(enclosing >= 0 && "plus".equals(index.simpleName(enclosing)), "ancestor from parent lane");
            }
            ok(index.parent(i) < i && index.parent(i) >= -1, "parent ordered");
            ok(index.structuralHash64(i) != 0, "shape hash available for every index row");
            if (index.hasExactRange(i)) {
                String original = text.substring(index.start(i), index.end(i));
                ok(original.equals(index.sourceText(i)), "verified half-open javac UTF16 range");
                ok(index.exactHash64(i) == index.exactHash64(i), "exact source hash deterministic");
            }
        }
        ok(cls && constant && inner && literal && method, "names, values and modifiers are indexed");
        M3ASTIndex another = capture(new Source("Other", text.replace("Demo", "Other").replace("N", "Q")
                .replace("42", "77").replace("Inner", "Nested")), limits());
        ok(index.structuralHash64(0) == another.structuralHash64(0), "same shape masks names and literals");
        ok(index.exactHash64(0) != another.exactHash64(0), "exact source differs");
        rejects(() -> {
            try { capture(new Source("TooBig", "class TooBig {}"), new M3ASTIngress.Limits(3, 100, 20)); }
            catch (IllegalArgumentException ex) { throw ex; }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
        rejects(() -> {
            try { capture(new Source("Tiny", "class Tiny {}"), new M3ASTIngress.Limits(200, 1, 20)); }
            catch (IllegalArgumentException ex) { throw ex; }
            catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("M3_JAVAC_INDEX_PASS checks=" + checks);
    }
}
