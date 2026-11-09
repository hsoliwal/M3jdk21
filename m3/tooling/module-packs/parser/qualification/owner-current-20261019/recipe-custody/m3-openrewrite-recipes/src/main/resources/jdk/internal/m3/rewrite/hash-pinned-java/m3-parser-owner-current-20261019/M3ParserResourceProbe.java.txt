/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.management.ThreadMXBean;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3JavacParser;
import jdk.m3.parser.M3JavacTree;

/** Resource observations over actual source owners; timings are not pass/fail assertions. */
public final class M3ParserResourceProbe {
    private static volatile Object sink;
    private static final ThreadMXBean ALLOCATIONS = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().threadId();

    private M3ParserResourceProbe() { }

    public static void main(String[] arguments) throws Exception {
        if (!ALLOCATIONS.isThreadAllocatedMemorySupported()) throw new IllegalStateException("thread allocation measurement unsupported");
        ALLOCATIONS.setThreadAllocatedMemoryEnabled(true);
        StringBuilder text = new StringBuilder("class Resources {");
        for (int index = 0; index < 2048; index++) text.append("int f").append(index).append(" = ").append(index).append(';');
        text.append('}');
        URI uri = URI.create("memory:/Resources.java");
        String frozen = text.toString();
        var source = new SimpleJavaFileObject(uri, javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) { return frozen; }
        };
        try (var manager = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, manager, null,
                    List.of("-proc:none", "-source", "21", "-target", "21"), null, List.of(source));
            try (M3AST session = M3AST.open(task, true)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        M3JavacTree tree = unit.project(M3JavacParser.projection(100_000));
                        observe("kind-lookup", 20_000, 200_000, () -> sink = tree.kind(0));
                        observe("source-seal", 8, 32, () -> sink = M3File.of(uri, frozen));
                        observe("javac-projection", 4, 16,
                                () -> sink = unit.project(M3JavacParser.projection(100_000)));
                        System.out.println("M3_PARSER_RESOURCE_SHAPE nodes=" + tree.nodeCount()
                                + " utf16Units=" + frozen.length() + " retainedPrimitiveBytes=" + tree.primitivePayloadBytes());
                    }
                });
                task.parse();
            }
        }
    }

    private static void observe(String name, int warmup, int repeats, Operation operation) throws Exception {
        for (int index = 0; index < warmup; index++) operation.run();
        long allocated = ALLOCATIONS.getThreadAllocatedBytes(THREAD);
        long started = System.nanoTime();
        for (int index = 0; index < repeats; index++) operation.run();
        long nanos = System.nanoTime() - started;
        allocated = ALLOCATIONS.getThreadAllocatedBytes(THREAD) - allocated;
        System.out.println("M3_PARSER_RESOURCE subject=" + name + " repeats=" + repeats
                + " bytesPerOperation=" + (allocated / repeats) + " nanosPerOperation=" + (nanos / repeats));
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }
}
