// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3JavacFactImage;
import jdk.m3.parser.M3JavacParser;
import jdk.m3.parser.M3JavacTree;

/**
 * Consolidation contract: the geometry projection and the detailed index projection of
 * the same original parse event agree row by row, the geometry view over the index
 * yields the same fact image bytes, and foreign domains or kind names are refused.
 */
public final class M3JavacTreeViewContract {
    private static final String TEXT = """
            package demo;
            /** Doc. */
            public final class Demo {
                static final int LIMIT = 42;
                private final String name = "a\\tb";
                int sum(int[] values) {
                    int total = 0;
                    for (int value : values) { total += value; }
                    Runnable r = () -> System.out.println(values.length);
                    return total > LIMIT ? total : -1;
                }
            }
            """;
    private static final byte[] CONTEXT = new byte[32];
    private static final int MAX_IMAGE_BYTES = 1 << 20;
    private static final int MAX_NODES = 100_000;
    private static int checks;

    private M3JavacTreeViewContract() { }

    public static void main(String[] arguments) throws Exception {
        sameRowsAndSameImage();
        refusals();
        System.out.println("M3_JAVAC_TREE_VIEW_PASS checks=" + checks + " groups=2");
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        Source(String text) { super(URI.create("memory:/Demo.java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignored) { return text; }
    }

    private static JavacTask task(Source source, StandardJavaFileManager files,
                                  DiagnosticCollector<JavaFileObject> diagnostics) {
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files, diagnostics,
                List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(source));
    }

    private static void sameRowsAndSameImage() throws Exception {
        M3AST.Projection<M3JavacTree> geometry = M3JavacParser.projection(MAX_NODES);
        M3AST.Projection<M3ASTIndex> details = M3JavacParser.indexProjection(M3ASTIngress.Limits.defaults());
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        AtomicInteger units = new AtomicInteger();
        try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = task(new Source(TEXT), files, diagnostics);
            try (M3AST session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        compare(unit.project(geometry), unit.project(details));
                        units.incrementAndGet();
                    }
                });
                task.parse();
                check(!session.summary().consumerFailed(), "consolidation consumer completes on the original event");
            }
        }
        check(units.get() == 1, "exactly one parsed unit was compared");
    }

    private static void compare(M3JavacTree projected, M3ASTIndex index) throws Exception {
        M3JavacTree view = M3JavacTree.of(index);
        check(view.index().orElse(null) == index, "the view reports its owning index");
        check(view.nodeCount() == projected.nodeCount() && view.nodeCount() == index.nodeCount(),
                "same occurrence count from both projections");
        check(view.source() == index.source(), "the view borrows the index source owner");
        boolean complete = true;
        for (int row = 0; row < projected.nodeCount(); row++) {
            check(view.kind(row) == projected.kind(row), "same Tree.Kind at row " + row);
            check(view.parent(row) == projected.parent(row), "same parent at row " + row);
            if (view.hasSourceRange(row)) {
                check(view.start(row) == projected.start(row) && view.end(row) == projected.end(row),
                        "same exact range at row " + row);
            } else {
                check(projected.start(row) < 0 || projected.end(row) < 0, "view drops only half-known ranges at row " + row);
                complete = false;
            }
        }
        check(view.primitivePayloadBytes() < projected.primitivePayloadBytes(),
                "the view retains less than the four detached lanes");
        if (complete) {
            byte[] fromTree = M3JavacFactImage.capture(projected, CONTEXT, MAX_IMAGE_BYTES);
            byte[] fromIndex = M3JavacFactImage.capture(index, CONTEXT, MAX_IMAGE_BYTES);
            check(Arrays.equals(fromTree, fromIndex), "identical fact image bytes from both captures");
            check(M3JavacFactImage.read(fromIndex, index.source(), CONTEXT, MAX_IMAGE_BYTES, MAX_NODES).size()
                    == index.nodeCount(), "image from the index reads back with the same row count");
        }
    }

    private static void refusals() {
        M3File file = M3File.of(URI.create("memory:/build.gradle"), "plugins { }");
        M3ASTIndex gradle = M3ASTIngress.gradleScript(file, false, M3ASTIngress.Limits.defaults());
        expect(IllegalArgumentException.class, () -> M3JavacTree.of(gradle));
        check(true, "a foreign domain is refused");
        M3File java = M3File.of(URI.create("memory:/Foreign.java"), "class Foreign {}");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(java, M3ASTIndex.Domain.JAVAC, 4);
        builder.add(-1, "NOT_A_TREE_KIND", 0, java.length(), M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        M3ASTIndex foreignKind = builder.seal();
        expect(IllegalArgumentException.class, () -> M3JavacTree.of(foreignKind));
        check(true, "a kind outside this compiler's Tree.Kind is refused");
        expect(NullPointerException.class, () -> M3JavacTree.of(null));
        check(true, "a missing index is refused");
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
