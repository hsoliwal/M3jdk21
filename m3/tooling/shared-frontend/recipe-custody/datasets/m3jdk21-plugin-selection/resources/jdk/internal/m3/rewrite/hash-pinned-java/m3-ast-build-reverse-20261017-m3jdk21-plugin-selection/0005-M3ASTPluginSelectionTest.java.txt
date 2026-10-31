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

/* @test
 * @summary M3 plugin selection reads original parsed options without creating compiler work
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.m3:+open
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main/othervm M3ASTPluginSelectionTest
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.m3.M3ASTPC;
import com.sun.tools.javac.util.Context;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class M3ASTPluginSelectionTest {
    private static int checks;
    private static int cases;
    private static Context.Key<?> sessionKey;

    public static void main(String[] args) throws Exception {
        Field field = M3ASTPC.class.getDeclaredField("KEY");
        field.setAccessible(true);
        sessionKey = (Context.Key<?>) field.get(null);
        Path work = Files.createTempDirectory("m3-plugin-selection-");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "actual JDK compiler is available");

        selection(compiler, work, List.of(), false, true);
        selection(compiler, work, List.of("-Xplugin:M3AST"), true, true);
        selection(compiler, work, List.of("-Xplugin:M3ASTIndex"), false, false);
        selection(compiler, work, List.of("-Xplugin:Unrelated M3AST"), false, false);
        selection(compiler, work, List.of("-Xplugin: M3AST"), false, false);
        selection(compiler, work, List.of("-Xplugin:M3AST\u00a0lossless"), false, false);
        for (char delimiter : new char[] {' ', '\t', '\n', 11, '\f', '\r'}) {
            selection(compiler, work, List.of("-Xplugin:M3AST" + delimiter + "lossless"), true, true);
        }
        selection(compiler, work, List.of("-Xplugin:M3AST" + (char) 0 + "M3AST"), true, true);
        selection(compiler, work,
                List.of("-Xplugin:Unrelated" + (char) 0 + "M3AST\tlossless"), true, false);

        // Each value below is consumed by javac as an operand, not another switch.
        for (String alias : List.of("--class-path", "-classpath", "-cp")) {
            selection(compiler, work, List.of(alias, "-Xplugin:M3AST"), false, true);
        }
        selection(compiler, work, List.of("--class-path=-Xplugin:M3AST"), false, true);
        selection(compiler, work,
                List.of("--class-path", "-Xplugin:M3AST", "-Xplugin:M3AST"), true, true);
        selection(compiler, work,
                List.of("--class-path=-Xplugin:M3AST", "-Xplugin:M3AST"), true, true);

        // Exercise the original option table's internal enable and replacement semantics.
        selection(compiler, work, List.of("-XD-Xplugin:=M3AST"), true, true);
        selection(compiler, work,
                List.of("-Xplugin:M3AST", "-XD-Xplugin:=M3ASTIndex"), false, false);
        selection(compiler, work,
                List.of("-Xplugin:M3ASTIndex", "-XD-Xplugin:=M3AST"), true, true);
        selection(compiler, work, List.of("--class-path", "-XD-Xplugin:=M3AST"), false, true);

        refusals();
        System.out.println("M3_AST_PLUGIN_SELECTION checks=" + checks + " cases=" + cases);
    }

    private static void selection(JavaCompiler compiler, Path work, List<String> extra,
                                  boolean selected, boolean compiles) throws Exception {
        Path output = Files.createDirectory(work.resolve("case-" + (++cases)));
        Path emptyClasspath = Files.createDirectory(output.resolve("empty-classpath"));
        Input input = new Input();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>(List.of("-proc:none", "-d", output.toString(),
                "--class-path", emptyClasspath.toString()));
        options.addAll(extra);
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    options, null, List.of(input));
            BasicJavacTask original = (BasicJavacTask) task;
            Context context = original.getContext();
            Arguments arguments = context.get(Arguments.argsKey);
            check(arguments != null, "getTask has already created the original arguments owner");
            var plugins = arguments.getPluginOpts();
            Object compilerOwner = context.get(com.sun.tools.javac.main.JavaCompiler.compilerKey);
            MultiTaskListener listenerOwner = context.get(MultiTaskListener.taskListenerKey);
            List<TaskListener> listeners = listenerOwner == null ? List.of()
                    : List.copyOf(listenerOwner.getTaskListeners());
            check(context.get(sessionKey) == null, "no M3 session exists before the query");
            check(input.reads == 0, "getTask has not read the source");

            check(M3AST.isPluginSelected(task) == selected, "selection follows original options: " + extra);
            check(M3AST.isPluginSelected(task) == selected, "repeated observation is unchanged");
            check(input.reads == 0, "the query does not read the source");
            check(original.getContext() == context, "the original context is unchanged");
            check(context.get(Arguments.argsKey) == arguments, "the original arguments owner is unchanged");
            check(plugins.equals(arguments.getPluginOpts()), "the original plugin options are unchanged");
            check(context.get(com.sun.tools.javac.main.JavaCompiler.compilerKey) == compilerOwner,
                    "the query creates no compiler owner");
            check(context.get(MultiTaskListener.taskListenerKey) == listenerOwner,
                    "the query creates no listener owner");
            check(listenerOwner == null || listeners.equals(List.copyOf(listenerOwner.getTaskListeners())),
                    "the query adds no listener");
            check(context.get(sessionKey) == null, "the query creates no M3 session");

            Boolean result = task.call();
            check(result != null && result == compiles,
                    "the original task retains its normal result: " + extra + " " + diagnostics.getDiagnostics());
            if (compiles) {
                check(input.reads == 1, "the original compiler reads the source once");
                check(Files.isRegularFile(output.resolve("SelectionProbe.class")),
                        "the original compiler emits the class");
            } else {
                check(diagnostics.getDiagnostics().stream().anyMatch(diagnostic ->
                        diagnostic.getCode().equals("compiler.err.plugin.not.found")),
                        "the original compiler reports the unresolved plugin");
            }
            check(original.getContext() == null, "normal task cleanup releases the context");
            expect(IllegalStateException.class, "closed javac context", () -> M3AST.isPluginSelected(task));
            // Plugin initialization can precede an error that prevents COMPILATION.
            if (context.get(sessionKey) instanceof M3AST session) session.close();
        }
    }

    private static void refusals() {
        expect(NullPointerException.class, "task", () -> M3AST.isPluginSelected(null));
        expect(IllegalArgumentException.class, "M3AST requires the active javac task",
                () -> M3AST.isPluginSelected(new ForeignTask()));

        Context context = new Context();
        BasicJavacTask missingArguments = new BasicJavacTask(context, false);
        check(context.get(Arguments.argsKey) == null, "the refusal fixture has no arguments owner");
        expect(IllegalStateException.class, "javac arguments not initialized",
                () -> M3AST.isPluginSelected(missingArguments));
        check(context.get(Arguments.argsKey) == null, "refusal does not create an arguments owner");
        check(context.get(com.sun.tools.javac.main.JavaCompiler.compilerKey) == null,
                "refusal does not create a compiler owner");
        check(context.get(MultiTaskListener.taskListenerKey) == null, "refusal creates no listener owner");
        check(context.get(sessionKey) == null, "refusal creates no M3 session");
    }

    private static final class Input extends SimpleJavaFileObject {
        int reads;

        Input() { super(URI.create("memory:///SelectionProbe.java"), Kind.SOURCE); }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return "class SelectionProbe { static String value() { return \"a\" + \"b\"; } }";
        }
    }

    /** A foreign task must be refused without invoking any of its operations. */
    private static final class ForeignTask extends JavacTask {
        private AssertionError unexpected() { return new AssertionError("foreign task was invoked"); }

        @Override public Iterable<? extends CompilationUnitTree> parse() { throw unexpected(); }
        @Override public Iterable<? extends Element> analyze() { throw unexpected(); }
        @Override public Iterable<? extends JavaFileObject> generate() { throw unexpected(); }
        @Override public void setTaskListener(TaskListener listener) { throw unexpected(); }
        @Override public void addTaskListener(TaskListener listener) { throw unexpected(); }
        @Override public void removeTaskListener(TaskListener listener) { throw unexpected(); }
        @Override public TypeMirror getTypeMirror(Iterable<? extends Tree> path) { throw unexpected(); }
        @Override public Elements getElements() { throw unexpected(); }
        @Override public Types getTypes() { throw unexpected(); }
        @Override public void setProcessors(Iterable<? extends Processor> processors) { throw unexpected(); }
        @Override public void setLocale(Locale locale) { throw unexpected(); }
        @Override public void addModules(Iterable<String> modules) { throw unexpected(); }
        @Override public Boolean call() { throw unexpected(); }
    }

    private static void expect(Class<? extends Throwable> type, String message, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "refusal has the documented exception type");
            check(message.equals(failure.getMessage()), "refusal has the documented message");
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
