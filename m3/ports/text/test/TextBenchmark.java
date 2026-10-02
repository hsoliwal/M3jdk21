/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
import com.m3.indexstring.*;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

/** Diagnostic microbenchmark, not a substitute for JMH or application workloads. */
public final class TextBenchmark {
    static volatile Object sink;
    static final com.sun.management.ThreadMXBean ALLOC =
        (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    static void measure(String name, Supplier<?> operation, int iterations) {
        for (int i = 0; i < iterations; i++) sink = operation.get();
        long thread = Thread.currentThread().threadId();
        long bytes = ALLOC.getThreadAllocatedBytes(thread), start = System.nanoTime();
        for (int i = 0; i < iterations; i++) sink = operation.get();
        long nanos = System.nanoTime() - start;
        bytes = ALLOC.getThreadAllocatedBytes(thread) - bytes;
        System.out.printf("%s\t%.1f\t%.1f%n", name, (double)nanos / iterations, (double)bytes / iterations);
    }
    public static void main(String[] args) {
        int iterations = Integer.parseInt(args[0]);
        String left = "a".repeat(4096), right = "\u0100".repeat(4096);
        M3StringArena arena = new M3StringArena(64, 1024 * 1024);
        M3String a = arena.fromString(left), b = arena.fromString(right);
        M3String joined = a.concat(b); sink = joined;
        String stockJoined = left + right;
        if (!joined.contentEquals(stockJoined)) throw new AssertionError("benchmark oracle");
        System.out.println("operation\tns_per_op\tallocated_bytes_per_op");
        measure("stock_concat", () -> left + right, iterations);
        measure("m3_concat", () -> a.concat(b), iterations);
        measure("m3_warm_admission", () -> arena.fromString(left), iterations);
        M3StringArena cold = new M3StringArena(0, 0);
        measure("m3_cold_admission", () -> cold.fromString(left), iterations);
        measure("stock_slice", () -> stockJoined.substring(4090, 4102), iterations);
        measure("m3_slice", () -> joined.substring(4090, 4102), iterations);
        measure("m3_copy_output", joined::toCharArray, iterations);
        System.out.println("joined_segments\t" + joined.segmentCount());
        System.out.println("joined_owner_payload_bytes\t" + joined.retainedPayloadBytes());
        System.out.println("tiny_slice_owner_payload_bytes\t" + joined.substring(1, 2).retainedPayloadBytes());
        System.out.println("tiny_compact_owner_payload_bytes\t" + joined.substring(1, 2).compact().retainedPayloadBytes());
        System.out.println("cache_payload_bytes\t" + arena.retainedPayloadBytes());
        System.out.println("native_payload_bytes\t0\nmapped_payload_bytes\t0");
    }
}
