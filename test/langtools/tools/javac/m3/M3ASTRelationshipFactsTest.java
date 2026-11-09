/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

/*
 * @test
 * @summary Original javac events project attributed relationships without a second frontend
 * @modules jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.m3
 * @run main M3ASTRelationshipFactsTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class M3ASTRelationshipFactsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Method projection;
        try {
            projection = M3AST.class.getMethod("relationshipFacts", M3AST.Unit.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("original javac ANALYZED events need relationshipFacts", missing);
        }
        var source = new Source("""
                package specimen;
                interface Strategy { int run(int x); }
                class Concrete implements Strategy { public int run(int x) { return x; } }
                class Context {
                    Strategy algorithm;
                    String decoy = "implements Strategy { fake.run(42); }";
                    int execute(int x) { return algorithm.run(x); }
                    int execute(String x) { return x.length(); }
                    Runnable deferred() { return () -> algorithm.run(99); }
                    class Nested { int execute(int x) { return algorithm.run(x); } }
                }
                """);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var compiler = ToolProvider.getSystemJavaCompiler();
        var output = Files.createTempDirectory("m3-relations-test-");
        var retained = new AtomicReference<M3AST.Unit>();
        var facts = new ArrayList<Object>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "-d", output.toString()), null, List.of(source));
            try (var session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    public void parsed(M3AST.Unit unit) throws Exception {
                        expectFailure(IllegalStateException.class, () -> invoke(projection, unit));
                    }
                    public void analyzed(M3AST.Unit unit) throws Exception {
                        retained.set(unit);
                        Object value = invoke(projection, unit);
                        check(value == invoke(projection, unit), "same event must share its projection");
                        facts.add(value);
                        var otherThread = new AtomicReference<Throwable>();
                        Thread thread = new Thread(() -> {
                            try { invoke(projection, unit); }
                            catch (Throwable failure) { otherThread.set(failure); }
                        });
                        thread.start();
                        thread.join();
                        check(otherThread.get() instanceof IllegalStateException,
                                "relationship projection must be callback-thread confined");
                    }
                });
                task.parse();
                task.analyze();
                check(diagnostics.getDiagnostics().stream().noneMatch(d ->
                        d.getKind() == javax.tools.Diagnostic.Kind.ERROR), "fixture must attribute");
                check(facts.size() == 3, "one projection per original top-level analysis event");
                check(session.summary().capturedSources() == 1, "capture exact parser input once");
                check(session.summary().sourceReads() == 0, "relationships must not reread source");
                check(source.reads == 1, "no additional parser or source read");
            }
        }
        expectFailure(IllegalStateException.class, () -> invoke(projection, retained.get()));
        var observations = new ArrayList<String>();
        var identities = new ArrayList<String>();
        for (Object value : facts) {
            check(!(Boolean) value.getClass().getMethod("completeGraph").invoke(value),
                    "positive relationships never certify graph completeness");
            check((Integer) value.getClass().getMethod("sourceLength").invoke(value) == source.text.length(),
                    "source extent is exact UTF16 length");
            Object[] symbols = (Object[]) value.getClass().getMethod("symbols").invoke(value);
            for (Object symbol : symbols) identities.add((String) symbol.getClass().getMethod("identity").invoke(symbol));
            if (symbols.length != 0) {
                symbols[0] = null;
                Object[] again = (Object[]) value.getClass().getMethod("symbols").invoke(value);
                check(again[0] != null, "detached symbol lane must be defensive");
            }
            Object[] relations = (Object[]) value.getClass().getMethod("relations").invoke(value);
            for (Object relation : relations) {
                Class<?> type = relation.getClass();
                observations.add(type.getMethod("relation").invoke(relation) + "\t"
                        + type.getMethod("from").invoke(relation) + "\t"
                        + type.getMethod("to").invoke(relation));
                int start = (Integer) type.getMethod("start").invoke(relation);
                int end = (Integer) type.getMethod("end").invoke(relation);
                check(start >= 0 && end > start && end <= source.text.length(),
                        "relationship evidence must be a real half-open source range");
            }
        }
        check(observations.contains("IMPLEMENTS\tspecimen.Concrete\tspecimen.Strategy"),
                "attributed direct interface relation");
        check(observations.contains("ABSTRACT_ROLE\tspecimen.Strategy\tspecimen.Strategy"),
                "actual interface abstract role");
        check(observations.contains("DECLARES_FIELD\tspecimen.Context\tspecimen.Context#algorithm:Lspecimen/Strategy;"),
                "field owner identity");
        check(observations.contains("FIELD_TYPE\tspecimen.Context#algorithm:Lspecimen/Strategy;\tspecimen.Strategy"),
                "resolved declared reference type");
        check(observations.contains("INVOKES\tspecimen.Context#execute(I)I\tspecimen.Strategy#run(I)I"),
                "actual call resolves overloaded JVM member identity");
        check(observations.contains("INVOKES\tspecimen.Context#execute(Ljava/lang/String;)I\tjava.lang.String#length()I"),
                "external called method preserves its binary owner");
        check(observations.contains("DECLARES_METHOD\tjava.lang.String\tjava.lang.String#length()I"),
                "referenced member includes attributed declaring ownership");
        check(observations.stream().noneMatch(s -> s.startsWith("INVOKES\tspecimen.Context#deferred()")),
                "lambda body must not masquerade as an enclosing method invocation");
        check(identities.stream().noneMatch(s -> s.contains("fake")), "string decoys are not facts");
        System.out.println("M3_AST_RELATIONSHIP_FACTS checks=" + checks);
    }

    private static Object invoke(Method method, M3AST.Unit unit) throws Exception {
        try { return method.invoke(null, unit); }
        catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private static void expectFailure(Class<? extends Throwable> expected, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            check(expected.isInstance(failure), "wrong refusal: " + failure);
            return;
        }
        throw new AssertionError("missing refusal: " + expected.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static final class Source extends SimpleJavaFileObject {
        final String text;
        int reads;
        Source(String text) { super(URI.create("string:///specimen/Context.java"), Kind.SOURCE); this.text = text; }
        @Override public CharSequence getCharContent(boolean ignored) { reads++; return text; }
    }
}
