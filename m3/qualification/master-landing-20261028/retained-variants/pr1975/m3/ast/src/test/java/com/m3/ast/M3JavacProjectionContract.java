// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Original Java 21 jdk.compiler -> indexed-source projection contract. */
public final class M3JavacProjectionContract {
    private M3JavacProjectionContract() { }
    private static int checks;
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
        checks++;
    }
    private static void reject(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (!expected.isInstance(error)) throw new AssertionError("wrong refusal", error);
            checks++; return;
        }
        throw new AssertionError("invalid source accepted");
    }
    private static M3JavacProjection.Result parse(String text) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///T.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) { return text; }
        };
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(
                null, null, null, List.of("-proc:none"), null, List.of(source));
        CompilationUnitTree compilation = task.parse().iterator().next();
        IntUnaryOperator mapper = p -> {
            if (p < 0 || p > text.length()) throw new IndexOutOfBoundsException(p);
            if (p > 0 && p < text.length() && Character.isHighSurrogate(text.charAt(p - 1))
                    && Character.isLowSurrogate(text.charAt(p)))
                throw new IllegalArgumentException("split source surrogate");
            return text.substring(0, p).getBytes(StandardCharsets.UTF_8).length;
        };
        return M3JavacProjection.project(compilation,
                Trees.instance(task).getSourcePositions(), bytes, mapper);
    }
    public static void main(String[] args) throws Exception {
        String[] programs = {
                "class T { void f() {} }",
                "// leading α\r\nclass T { String s = \"😀\"; int f(int x) { return x + 1; } }",
                "package pkg; import java.util.List; record T(List<String> xs) { }",
                "class T { int f() { if (true) return 1; return 2; } }",
                "class T { String s = \"broken\"; /* error? */ }",
                "// only header\n"
        };
        for (String code : programs) {
            M3JavacProjection.Result result = parse(code);
            M3SyntaxTree tree = result.tree();
            check(result.visited() == result.projected() + result.unpositioned()
                    + result.crossing() + result.invalid(), "complete visitation accounting");
            check(tree.size() == result.projected() + 1, "synthetic root and admitted nodes");
            check(tree.kind(0).equals("m3.source_image") && tree.startByte(0) == 0
                    && tree.endByte(0) == code.getBytes(StandardCharsets.UTF_8).length,
                    "complete byte-image boundary");
            check(Arrays.equals(tree.sourceBytes(), code.getBytes(StandardCharsets.UTF_8)),
                    "comments, Unicode and errors preserved in byte image");
            check(tree.projectionSha256().matches("[0-9a-f]{64}"), "source-sealed syntax");
            var index = new M3SyntaxIndex(tree);
            var pool = new M3SyntaxGreenPool(10000, 400000);
            check(Arrays.equals(pool.render(pool.intern(tree).rootGreen(), 400000),
                    code.getBytes(StandardCharsets.UTF_8)), "real parser tree survives green roundtrip");
            for (int node = 1; node < tree.size(); node++) {
                int parent = tree.parent(node);
                check(tree.startByte(parent) <= tree.startByte(node)
                        && tree.endByte(node) <= tree.endByte(parent), "nested mapped spans");
            }
            check(index.rootSha256().length() == 64, "syntax fingerprint");
        }
        String code = "class T { int x; }";
        byte[] raw = code.getBytes(StandardCharsets.UTF_8);
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///T.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) { return code; }
        };
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null,
                null, null, List.of("-proc:none"), null, List.of(file));
        var unit = task.parse().iterator().next();
        var positions = Trees.instance(task).getSourcePositions();
        check(M3JavacProjection.project(unit, positions, raw, at -> -1).invalid() > 0,
                "bad source coordinate recorded and retained as unparsed bytes");
        reject(IllegalStateException.class, () -> M3JavacProjection.project(
                unit, positions, raw, at -> at, 1));
        M3JavacProjection.Result first = parse(code), second = parse(code);
        check(first.tree().projectionSha256().equals(second.tree().projectionSha256()),
                "deterministic byte/grammar identity");
        System.out.println("M3_JAVAC_PROJECTION_PASS checks=" + checks);
    }
}
