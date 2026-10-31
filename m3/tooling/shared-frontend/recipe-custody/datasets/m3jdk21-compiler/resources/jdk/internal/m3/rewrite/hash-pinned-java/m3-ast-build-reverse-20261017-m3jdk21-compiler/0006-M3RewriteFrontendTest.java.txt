/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package org.m3jdk.tooling.rewrite;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import static org.junit.jupiter.api.Assertions.*;

class M3RewriteFrontendTest {
    @TempDir Path directory;

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        private int reads;
        Source(String name, String text) {
            super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return text;
        }
    }

    private JavacTask task(Source source, String... extra) {
        List<String> options = new ArrayList<>(List.of("-proc:none", "-d", directory.toString()));
        options.addAll(List.of(extra));
        return (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null,
                new DiagnosticCollector<>(), options, null, List.of(source));
    }

    private static ExecutionContext execution() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    @Test void realCompilerTreeHasLosslessStructureTypesAndRecipeParity() throws Exception {
        String text = "import java.util.List;\r\n/** α documentation */\r\n"
                + "class Demo<T extends Number> {\r\n"
                + "  String joined = \"a\" /* keep */ + \"b\";\r\n"
                + "  List<String> names = List.of(\"x\");\r\n"
                + "  String clean(String s) { return s.trim(); }\r\n"
                + "  record Pair(int left, int right) {}\r\n"
                + "}\r\nclass Auxiliary {}\r\n";
        Source source = new Source("Demo", text);
        JavacTask task = task(source, "-XDcompilePolicy=byfile");
        M3AST shared = M3AST.open(task, true);
        var projection = M3RewriteFrontend.projection(execution());
        J.CompilationUnit[] first = new J.CompilationUnit[1];
        J.CompilationUnit[] second = new J.CompilationUnit[1];
        M3AST.Unit[] event = new M3AST.Unit[1];
        shared.addConsumer(new M3AST.Consumer() {
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                if (first[0] == null) { first[0] = unit.project(projection); event[0] = unit; }
            }
        });
        shared.addConsumer(new M3AST.Consumer() {
            @Override public void analyzed(M3AST.Unit unit) throws Exception {
                if (unit == event[0]) second[0] = unit.project(projection);
            }
        });
        assertTrue(task.call());
        assertSame(first[0], second[0]);
        assertEquals(text, first[0].printAll());
        assertEquals(1, shared.summary().parsedUnits());
        assertEquals(2, shared.summary().analyzedTypes());
        assertEquals(1, shared.summary().projectionComputations());
        assertEquals(1, shared.summary().projectionHits());
        assertEquals(2, source.reads); // Original javac read, one sealed analysis request.
        assertTrue(shared.summary().completed());
        assertTrue(Files.isRegularFile(directory.resolve("Demo.class")));

        // The independent parse below is solely the differential test oracle.
        J.CompilationUnit baseline = (J.CompilationUnit) JavaParser.fromJavaVersion()
                .build().parse(execution(), text).findFirst().orElseThrow();
        assertEquals(structure(baseline), structure(first[0]));
        assertEquals(List.of(1, 1, 1), structure(first[0]));
        String adapted = rename(first[0]);
        assertEquals(rename(baseline), adapted);
        assertEquals(text.replace("s.trim()", "s.strip()"), adapted);
        // Changed recipe output is a new generation. Compile that generation.
        assertTrue(task(new Source("Demo", adapted)).call());
    }

    private static List<Integer> structure(J.CompilationUnit unit) {
        AtomicInteger binaries = new AtomicInteger();
        AtomicInteger methods = new AtomicInteger();
        AtomicInteger parameterized = new AtomicInteger();
        new JavaIsoVisitor<Void>() {
            @Override public J.Binary visitBinary(J.Binary binary, Void unused) {
                if (binary.getOperator() == J.Binary.Type.Addition
                        && binary.getLeft() instanceof J.Literal left
                        && binary.getRight() instanceof J.Literal right
                        && "a".equals(left.getValue()) && "b".equals(right.getValue()))
                    binaries.incrementAndGet();
                return super.visitBinary(binary, unused);
            }
            @Override public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Void unused) {
                if (method.getSimpleName().equals("trim")) {
                    assertNotNull(method.getMethodType());
                    assertEquals("java.lang.String", method.getMethodType().getDeclaringType().getFullyQualifiedName());
                    methods.incrementAndGet();
                }
                return super.visitMethodInvocation(method, unused);
            }
            @Override public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, Void unused) {
                if (type.getType() instanceof JavaType.Parameterized mapped
                        && mapped.getFullyQualifiedName().equals("java.util.List")) parameterized.incrementAndGet();
                return super.visitParameterizedType(type, unused);
            }
        }.visit(unit, null);
        return List.of(binaries.get(), methods.get(), parameterized.get());
    }

    private static String rename(J.CompilationUnit unit) {
        var changes = new ChangeMethodName("java.lang.String trim()", "strip", false, false)
                .run(new InMemoryLargeSourceSet(List.of(unit)), execution()).getChangeset().getAllResults();
        assertEquals(1, changes.size());
        return changes.get(0).getAfter().printAll();
    }

    @Test void missingSyntaxRetentionIsRefused() {
        JavacTask task = task(new Source("Plain", "class Plain { String s=\"a\"+\"b\"; }"));
        M3AST session = M3AST.open(task);
        var projection = M3RewriteFrontend.projection(execution());
        session.addConsumer(new M3AST.Consumer() {
            @Override public void analyzed(M3AST.Unit unit) throws Exception { unit.project(projection); }
        });
        assertTrue(assertThrows(RuntimeException.class, task::call).toString().contains("lossless"));
        assertTrue(session.summary().closed());
    }

    @Test void defaultPerTypeCompilationRefusesIncompleteWholeUnit() {
        JavacTask task = task(new Source("Multiple", "class Multiple {} class Another {}"));
        M3AST session = M3AST.open(task, true);
        var projection = M3RewriteFrontend.projection(execution());
        session.addConsumer(new M3AST.Consumer() {
            @Override public void analyzed(M3AST.Unit unit) throws Exception { unit.project(projection); }
        });
        RuntimeException error = assertThrows(RuntimeException.class, task::call);
        assertTrue(error.toString().contains("not fully attributed"));
        assertTrue(session.summary().consumerFailed());
    }

    @Test void projectionCannotShareTypesAcrossCompilerContexts() throws Exception {
        var projection = M3RewriteFrontend.projection(execution());
        for (int index = 0; index < 2; index++) {
            JavacTask task = task(new Source("Isolated", "class Isolated {}"));
            M3AST session = M3AST.open(task, true);
            session.addConsumer(new M3AST.Consumer() {
                @Override public void analyzed(M3AST.Unit unit) throws Exception { unit.project(projection); }
            });
            if (index == 0) assertTrue(task.call());
            else assertTrue(assertThrows(RuntimeException.class, task::call).toString().contains("different compilation"));
        }
    }

    @Test void parsePhaseCannotPretendToHaveSemanticTypes() throws Exception {
        JavacTask task = task(new Source("SyntaxOnly", "class SyntaxOnly {}"));
        M3AST session = M3AST.open(task, true);
        var projection = M3RewriteFrontend.projection(execution());
        session.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) {
                assertThrows(IllegalStateException.class, () -> unit.project(projection));
            }
        });
        assertTrue(task.call());
    }
}
