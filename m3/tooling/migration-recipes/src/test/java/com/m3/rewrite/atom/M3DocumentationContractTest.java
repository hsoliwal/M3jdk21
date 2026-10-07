// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3DocumentationContractTest {
    @Test
    void existingUserJavadocRemainsTheCompilerAssociatedContract() throws Exception {
        String before = """
                class T {
                    /**
                     * User summary remains authoritative.
                     * @return retained user contract
                     */
                    private static int f(int a, int b) {
                        int m3$pureIntAtom = a + b;
                        return m3$pureIntAtom;
                    }
                }
                """;

        String after = apply(before);
        assertTrue(after.indexOf("M3-ATOM: m3$pureIntAtom")
                < after.indexOf("User summary remains authoritative."));
        assertTrue(after.contains("/* M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */"));
        assertFalse(after.contains("/** M3-ATOM: m3$pureIntAtom"));

        String doc = effectiveJavadoc(after, "f");
        assertTrue(doc.contains("User summary remains authoritative."));
        assertTrue(doc.contains("@return retained user contract"));
        assertFalse(doc.contains("M3-ATOM"));
        assertEquals(after, apply(after));
    }

    @Test
    void undocumentedAtomRetainsGeneratedM3JavadocAndFixedPoint() throws Exception {
        String before = """
                class T {
                    private static int f(int a, int b) {
                        int m3$pureIntAtom = a + b;
                        return m3$pureIntAtom;
                    }
                }
                """;

        String after = apply(before);
        assertTrue(after.contains("/** M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */"));
        String doc = effectiveJavadoc(after, "f");
        assertTrue(doc.contains("M3-ATOM: m3$pureIntAtom"));
        assertEquals(after, apply(after));
    }

    private static String apply(String source) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        SourceFile parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(
                                Path.of("src/main/java/T.java"),
                                source)),
                        null,
                        context)
                .findFirst()
                .orElseThrow();
        var results = new M3DocumentPureIntAtomRecipe()
                .run(new InMemoryLargeSourceSet(List.of(parsed)), context, 1)
                .getChangeset()
                .getAllResults();
        if (results.isEmpty()) {
            return source;
        }
        assertEquals(1, results.size());
        assertNotNull(results.getFirst().getAfter());
        return results.getFirst().getAfter().printAll();
    }

    private static String effectiveJavadoc(String source, String methodName) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "a full JDK is required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject input = new SimpleJavaFileObject(
                URI.create("string:///T.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };

        try (var manager = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    manager,
                    diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror"),
                    null,
                    List.of(input));
            Iterable<? extends CompilationUnitTree> units = task.parse();
            task.analyze();
            assertTrue(
                    diagnostics.getDiagnostics().stream()
                            .noneMatch(item -> item.getKind() == Diagnostic.Kind.ERROR),
                    diagnostics.getDiagnostics().toString());

            DocTrees docs = DocTrees.instance(task);
            AtomicReference<String> result = new AtomicReference<>();
            for (CompilationUnitTree unit : units) {
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree method, Void unused) {
                        if (method.getName().contentEquals(methodName)) {
                            DocCommentTree doc = docs.getDocCommentTree(getCurrentPath());
                            result.set(doc == null ? null : doc.toString());
                        }
                        return super.visitMethod(method, unused);
                    }
                }.scan(unit, null);
            }
            String value = result.get();
            assertNotNull(value, "expected compiler-associated Javadoc");
            return value;
        }
    }
}
