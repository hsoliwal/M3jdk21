// SPDX-License-Identifier: Apache-2.0
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LongArrayDeque;
import com.m3.collections.M3LongArrayList;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Finite matched JVM measurements; this resource probe is not a JMH benchmark. */
public final class MaskZeroCostProbe {
    private static volatile int mask;
    private static volatile long sink;
    private static volatile Object result;
    private MaskZeroCostProbe() { }
    @FunctionalInterface private interface Operation { long run(); }
    private record Case(String name, Operation operation) { }

    public static void main(String[] args) throws Exception {
        boolean nativeRun = List.of(args).contains("--native");
        boolean interpreted = List.of(args).contains("--interpreted");
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported() || !bean.isCurrentThreadCpuTimeSupported()) {
            throw new AssertionError("allocation and CPU accounting required");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        bean.setThreadCpuTimeEnabled(true);
        long tid = Thread.currentThread().threadId();
        bean.getThreadAllocatedBytes(tid);
        bean.getCurrentThreadCpuTime();
        if (SizeAgent.instrumentation == null) throw new AssertionError("size agent required");
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) throw new AssertionError("actual JNI required");
        System.out.println("operation,length,round,iterations,cpu_ns,wall_ns,allocated_bytes,retained_bytes_before,retained_bytes_after,rss_kb_before,rss_kb_after");
        for (int length : new int[] {0, 257, 65536}) {
            byte[] states = new byte[length];
            int[] rows = new int[length];
            long[] values = new long[length];
            for (int i = 0; i < length; i++) { states[i] = (byte) (197 * i); rows[i] = i ^ -17; values[i] = i ^ Long.MIN_VALUE; }
            M3LongArrayList list = M3LongArrayList.copyOf(values);
            M3LongArrayDeque deque = new M3LongArrayDeque(length + 1);
            for (int i = length - 1; i >= 0; i--) deque.addFirst(values[i]);
            Object listStorage = storage(list), dequeStorage = storage(deque);
            List<Case> cases = new ArrayList<>();
            cases.add(new Case("selectedCount", () -> M3IndexMaskCompactor.selectedCount(states, mask)));
            cases.add(new Case("listRetain", () -> list.retainByStateMask(states, mask)));
            cases.add(new Case("dequeRetain", () -> deque.retainByStateMask(states, mask)));
            cases.add(new Case("javaIntSelect", () -> { int[] copy = M3IndexMaskCompactor.select(rows, states, mask, M3IndexMaskCompactor.Backend.JAVA); result = copy; return copy.length; }));
            cases.add(new Case("javaLongSelect", () -> { long[] copy = M3IndexMaskCompactor.selectLong(values, states, mask); result = copy; return copy.length; }));
            if (nativeRun) cases.add(new Case("nativeIntSelect", () -> { int[] copy = M3IndexMaskCompactor.select(rows, states, mask, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED); result = copy; return copy.length; }));
            int iterations = interpreted ? (length > 1000 ? 4 : 128) : (length > 1000 ? 64 : 4096);
            int warmup = interpreted ? 2 : length > 1000 ? 64 : 2048;
            for (Case item : cases) {
                for (int i = 0; i < warmup; i++) sink += item.operation.run();
                for (int round = 0; round < 5; round++) {
                    result = null;
                    System.gc();
                    long retainedBefore = retained(list) + retained(deque);
                    long rssBefore = rss();
                    long allocatedBefore = bean.getThreadAllocatedBytes(tid);
                    long cpuBefore = bean.getCurrentThreadCpuTime();
                    long wallBefore = System.nanoTime();
                    long count = 0;
                    for (int i = 0; i < iterations; i++) count += item.operation.run();
                    long wall = System.nanoTime() - wallBefore;
                    long cpu = bean.getCurrentThreadCpuTime() - cpuBefore;
                    long allocated = bean.getThreadAllocatedBytes(tid) - allocatedBefore;
                    sink = count;
                    long retainedAfter = retained(list) + retained(deque);
                    if (storage(list) != listStorage || storage(deque) != dequeStorage || retainedBefore != retainedAfter) {
                        throw new AssertionError("resident storage changed");
                    }
                    if (allocated < 0) throw new AssertionError("allocation counter regressed");
                    System.out.println(item.name + "," + length + "," + round + "," + iterations + "," + cpu + "," + wall + "," + allocated
                            + "," + retainedBefore + "," + retainedAfter + "," + rssBefore + "," + rss());
                }
            }
        }
    }
    private static Object storage(Object owner) throws ReflectiveOperationException {
        var field = owner.getClass().getDeclaredField("elements");
        field.setAccessible(true);
        return field.get(owner);
    }
    private static long retained(Object owner) throws ReflectiveOperationException {
        return SizeAgent.instrumentation.getObjectSize(owner) + SizeAgent.instrumentation.getObjectSize(storage(owner));
    }
    private static long rss() throws java.io.IOException {
        for (String line : Files.readAllLines(Path.of("/proc/self/status"))) {
            if (line.startsWith("VmRSS:")) return Long.parseLong(line.substring(6).trim().split("\\s+")[0]);
        }
        throw new AssertionError("Linux process RSS unavailable");
    }
}
