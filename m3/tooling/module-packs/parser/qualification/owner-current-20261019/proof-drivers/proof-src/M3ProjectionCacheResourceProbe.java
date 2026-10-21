/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Public-API allocation experiment; timings are observations, never test assertions. */
public final class M3ProjectionCacheResourceProbe {
    private static final Object VALUE = new Object();
    private static final Exception FAILURE = new Exception("preallocated failure");
    private static volatile Object sink;

    public static void main(String[] args) throws Exception {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unavailable");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        var compiler = ToolProvider.getSystemJavaCompiler();
        for (boolean failing : new boolean[] {false, true}) {
            for (int count : new int[] {1, 8, 64, 1024, 8192}) {
                List<M3AST.Projection<Object>> operations = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    operations.add(new M3AST.Projection<>("same-name", unit -> {
                        if (failing) throw FAILURE;
                        return VALUE;
                    }));
                }
                long[] measurements = new long[3];
                for (int iteration = 0; iteration < 12; iteration++) {
                    boolean measured = iteration >= 4;
                    JavaFileObject source = new SimpleJavaFileObject(
                            URI.create("string:///CacheInput.java"), JavaFileObject.Kind.SOURCE) {
                        @Override public CharSequence getCharContent(boolean ignore) {
                            return "class CacheInput {}";
                        }
                    };
                    try (var files = compiler.getStandardFileManager(null, null, null)) {
                        JavacTask task = (JavacTask) compiler.getTask(null, files, null,
                                List.of("-proc:none"), null, List.of(source));
                        try (M3AST ast = M3AST.open(task)) {
                            ast.addConsumer(new M3AST.Consumer() {
                                @Override public void parsed(M3AST.Unit unit) throws Exception {
                                    long beforeBytes = bean.getThreadAllocatedBytes(thread);
                                    long beforeTime = System.nanoTime();
                                    for (var operation : operations) {
                                        try {
                                            Object actual = unit.project(operation);
                                            if (failing || actual != VALUE) throw new AssertionError();
                                            sink = actual;
                                        } catch (Exception actual) {
                                            if (!failing || actual != FAILURE) throw actual;
                                            sink = actual;
                                        }
                                    }
                                    long elapsed = System.nanoTime() - beforeTime;
                                    long bytes = bean.getThreadAllocatedBytes(thread) - beforeBytes;
                                    if (measured) {
                                        measurements[0] += bytes;
                                        measurements[1] += elapsed;
                                        measurements[2]++;
                                    }
                                }
                            });
                            task.parse();
                            if (ast.summary().projectionComputations() != count
                                    || ast.summary().projectionHits() != 0) throw new AssertionError();
                        }
                    }
                }
                if (measurements[2] != 8) throw new AssertionError();
                System.out.printf("M3_CACHE_RESOURCE failure=%s count=%d samples=%d bytesPerEvent=%.1f nsPerEvent=%.1f%n",
                        failing, count, measurements[2], (double) measurements[0] / measurements[2],
                        (double) measurements[1] / measurements[2]);
            }
        }
    }
}
