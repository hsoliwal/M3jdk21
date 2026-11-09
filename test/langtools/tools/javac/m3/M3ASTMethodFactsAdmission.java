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
 * @summary The original javac ANALYZED event admits the additive method-facts projection
 * @modules jdk.compiler/com.sun.source.util
 * @run main M3ASTMethodFactsAdmission
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** First red witness: this exercises an actual ANALYZED event, without a mock Unit. */
public class M3ASTMethodFactsAdmission {
    public static void main(String[] arguments) throws Exception {
        var input = new SimpleJavaFileObject(URI.create("memory:///FactsAdmission.java"),
                                            JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) {
                return "class FactsAdmission { int plus(int x) { return x + 1; } }";
            }
        };
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, null,
                    List.of("-proc:none"), null, List.of(input));
            int[] observations = { 0 };
            try (M3AST session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        Method method;
                        try {
                            method = M3AST.class.getMethod("methodFacts", M3AST.Unit.class);
                        } catch (NoSuchMethodException absent) {
                            throw new AssertionError("M3_AST_METHOD_FACTS_ADMISSION_MISSING", absent);
                        }
                        Object first = method.invoke(null, unit);
                        Object second = method.invoke(null, unit);
                        if (first == null || first != second)
                            throw new AssertionError("method facts must memoize one detached result");
                        observations[0]++;
                    }
                });
                task.parse();
                task.analyze();
            }
            if (observations[0] != 1) throw new AssertionError("one actual ANALYZED event required");
        }
        System.out.println("M3_AST_METHOD_FACTS_ADMISSION_PASS events=1");
    }
}
