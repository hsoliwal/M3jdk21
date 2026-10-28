/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3JavacParser;
import jdk.m3.parser.M3JavacTree;
import jdk.m3.parser.M3Parser;

/** Precompiled external consumer; two image JVMs must emit the same semantic receipt. */
public final class M3ParserImageSmoke {
    private M3ParserImageSmoke() { }

    public static void main(String[] arguments) throws Exception {
        M3File source = M3File.of(URI.create("memory:/image.expr"), "12 + 3 * (4 + 5)");
        var tree = new M3Parser(M3ParserFixtures.expression()).parse(source).requireTree();
        if (M3ParserFixtures.evaluate(tree, tree.root()) != 39 || tree.tokens().source() != source.text()) {
            throw new AssertionError("linked grammar/source contract failed");
        }
        String program = "class ImageInput { int value() { return 39; } }";
        var input = new SimpleJavaFileObject(URI.create("memory:/ImageInput.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) { return program; }
        };
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        M3JavacTree[] captured = new M3JavacTree[1];
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, manager, diagnostics,
                    List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(input));
            try (M3AST session = M3JavacParser.attach(task, M3JavacParser.projection(512), value -> captured[0] = value)) {
                task.parse();
                if (session.summary().parsedUnits() != 1 || session.summary().projectionComputations() != 2
                        || session.summary().capturedSources() != 1 || session.summary().sourceReads() != 0) {
                    throw new AssertionError("linked existing-compiler projection contract failed");
                }
            }
        }
        if (captured[0] == null || !captured[0].source().text().equals(program)) {
            throw new AssertionError("linked detached compiler source missing");
        }
        System.out.println("M3_PARSER_IMAGE_PASS grammar=" + M3ParserFixtures.grammarDigest(tree)
                + " javac=" + M3ParserFixtures.javacDigest(captured[0]));
    }
}
