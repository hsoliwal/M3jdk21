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
 * @summary M3 projection state preserves identity, reentrant growth, null and failure memoization
 * @modules jdk.compiler/com.sun.source.util
 * @run main/othervm M3ASTProjectionStateTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class M3ASTProjectionStateTest {
    private static int checks;

    public static void main(String[] arguments) throws Exception {
        for (int count : new int[] {1, 5, 6, 21, 22, 128, 8192}) exercise(count);
        System.out.println("M3_AST_PROJECTION_STATE_PASS checks=" + checks);
    }

    private static void exercise(int count) throws Exception {
        var source = new SimpleJavaFileObject(URI.create("memory:/State.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) { return "class State {}"; }
        };
        int[] calls = new int[count];
        Object[] values = new Object[count];
        List<M3AST.Projection<Object>> keys = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            final int id = index;
            values[index] = switch (id % 6) {
                case 0 -> null;
                case 1 -> new Object();
                case 2, 5 -> new IOException("checked-" + id);
                case 3 -> new IllegalArgumentException("runtime-" + id);
                default -> new AssertionError("error-" + id);
            };
            // Equal operation names deliberately have distinct object identities.
            keys.add(new M3AST.Projection<>("same-name", unit -> {
                calls[id]++;
                if (id % 6 == 2 || id % 6 == 3) throw (Exception) values[id];
                if (id % 6 == 4) throw (Error) values[id];
                return values[id];
            }));
        }
        Object outerValue = new Object();
        M3AST.Projection<Object> outer = new M3AST.Projection<>("outer", unit -> {
            // Growth takes place while the outer key is still computing.
            for (int index = 0; index < count; index++) observe(unit, keys.get(index), values[index], index % 6);
            return outerValue;
        });
        List<M3AST.Projection<Object>> recursive = new ArrayList<>();
        recursive.add(new M3AST.Projection<>("recursive", unit -> unit.project(recursive.get(0))));
        List<M3AST.Projection<Object>> mutual = new ArrayList<>();
        mutual.add(new M3AST.Projection<>("mutual-a", unit -> unit.project(mutual.get(1))));
        mutual.add(new M3AST.Projection<>("mutual-b", unit -> unit.project(mutual.get(0))));
        M3AST.Unit[] escaped = new M3AST.Unit[1];
        try (var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, files, null,
                    List.of("-proc:none"), null, List.of(source));
            try (M3AST session = M3AST.open(task)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        escaped[0] = unit;
                        check(unit.project(outer) == outerValue && unit.project(outer) == outerValue,
                                "outer survives nested insertion and keeps identity");
                        for (int round = 0; round < 3; round++) {
                            // Revisit every identity without assuming any private hash layout.
                            for (int index = count - 1; index >= 0; index--)
                                observe(unit, keys.get(index), values[index], index % 6);
                        }
                        Throwable loop = failure(() -> unit.project(recursive.get(0)));
                        check(loop instanceof IllegalStateException && loop.getMessage().contains("recursive M3AST projection"),
                                "self recursion has the original refusal");
                        check(failure(() -> unit.project(recursive.get(0))) == loop, "self-recursive exception memoized by identity");
                        Throwable cycle = failure(() -> unit.project(mutual.get(0)));
                        check(cycle instanceof IllegalStateException, "mutual recursion refused");
                        check(failure(() -> unit.project(mutual.get(0))) == cycle
                                        && failure(() -> unit.project(mutual.get(1))) == cycle,
                                "mutual factories memoize the same propagated failure");
                        check(CompletableFuture.supplyAsync(() -> failure(() -> unit.project(outer))).join()
                                instanceof IllegalStateException, "cross-thread cache access refused");
                        for (int callsForKey : calls) check(callsForKey == 1, "each identity computes once");
                        check(session.summary().projectionComputations() == count + 4L, "exact computation accounting");
                        check(session.summary().projectionHits() == 3L * count + 4, "exact hit accounting including caught failures");
                    }
                });
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        check(unit == escaped[0], "second consumer sees original event view");
                        check(unit.project(outer) == outerValue, "second consumer shares original memoized value");
                    }
                });
                task.parse();
                check(!session.summary().consumerFailed(), "handled projection errors stay local");
            }
        }
        check(failure(() -> escaped[0].project(outer)) instanceof IllegalStateException, "released event refuses access");
    }

    private static void observe(M3AST.Unit unit, M3AST.Projection<Object> key, Object expected, int mode) throws Exception {
        if (mode >= 2 && mode <= 4) check(failure(() -> unit.project(key)) == expected, "original throwable identity");
        else check(unit.project(key) == expected, "null/object/returned-Throwable success identity");
    }

    private static Throwable failure(Operation operation) {
        try { operation.run(); }
        catch (Throwable failure) { return failure; }
        throw new AssertionError("expected operation failure");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }
}
