// SPDX-License-Identifier: Apache-2.0
package jdk.m3.regexcursor;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/** Hot-loop allocations, caller mutation, and immutable multi-reader proof. */
public final class AsciiRootAllocationProof {
    private static volatile long sink;
    private static final int WARMUP = 300_000;
    private static final int MEASURE = 1_000_000;
    private AsciiRootAllocationProof() { }

    private static StaticGrammarLexicon grammar() {
        List<StaticGrammarLexicon.Literal> definitions = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            String root = Character.toString((char) ('a' + i));
            definitions.add(new StaticGrammarLexicon.Literal(root, i + 1, false));
            definitions.add(new StaticGrammarLexicon.Literal(root + "_suffix", i + 27, false));
        }
        definitions.add(new StaticGrammarLexicon.Literal("₹", 53, false));
        definitions.add(new StaticGrammarLexicon.Literal("😀", 54, true));
        StaticGrammarLexicon fixed = StaticGrammarLexicon.compile("java", "21", definitions);
        definitions.clear();
        if (fixed.literalCount() != 54) throw new AssertionError("caller mutation changed image");
        return fixed;
    }

    private static String[] inputs() {
        String[] data = new String[1024];
        Random random = new Random(0x4d33414c4c4f4341L);
        for (int i = 0; i < data.length; i++) {
            int n = random.nextInt(26);
            String word = Character.toString((char) ('a' + n));
            data[i] = switch (i % 5) {
                case 0 -> word;
                case 1 -> word + "_suffix";
                case 2 -> word + "_suffixed";
                case 3 -> "📦";
                default -> "!";
            };
        }
        return data;
    }

    private static long run(StaticGrammarLexicon image, String[] data, int iterations) {
        long sum = 0;
        for (int i = 0; i < iterations; i++) {
            String text = data[(i * 751 + 19) & (data.length - 1)];
            sum += image.exact(text, 0, text.length());
            sum ^= image.longest(text, 0, text.length());
        }
        sink = sum;
        return sum;
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        StaticGrammarLexicon image = grammar();
        String[] data = inputs();
        StringBuilder sb = new StringBuilder("a_suffix");
        if (image.exact(sb, 0, sb.length()) != 27) throw new AssertionError("initial StringBuilder");
        sb.setCharAt(0, '#');
        if (image.exact(sb, 0, sb.length()) != -1) throw new AssertionError("stale source result");
        sb.setCharAt(0, 'a');
        if (image.exact(sb, 0, sb.length()) != 27) throw new AssertionError("mutable restore");

        run(image, data, WARMUP);
        var factory = ManagementFactory.getThreadMXBean();
        if (!(factory instanceof ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation instrumentation not available");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long threadId = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(threadId);
        long checksum = run(image, data, MEASURE);
        long after = bean.getThreadAllocatedBytes(threadId);
        if (before < 0 || after < before) throw new AssertionError("invalid allocation counter");
        long bytes = after - before;
        if (bytes > 8192L) throw new AssertionError("hot path allocated " + bytes + " bytes");

        final int workers = 4;
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicLongArray sums = new AtomicLongArray(workers);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[workers];
        for (int i = 0; i < workers; i++) {
            final int slot = i;
            threads[i] = new Thread(() -> {
                try {
                    start.await();
                    sums.set(slot, run(image, data, 100_000));
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                }
            });
            threads[i].start();
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        if (error.get() != null) throw new AssertionError("parallel reader failed", error.get());
        for (int i = 1; i < workers; i++) {
            if (sums.get(i) != sums.get(0)) throw new AssertionError("immutable readers diverged");
        }
        System.out.printf("M3_ASCII_ROOT_ALLOCATION_PASS calls=%d allocated_bytes=%d "
                + "readers=%d concurrent_checksums_equal=true checksum=%d digest=%s%n",
                MEASURE, bytes, workers, checksum, image.grammarSha256());
    }
}
