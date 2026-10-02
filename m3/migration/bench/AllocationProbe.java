// SPDX-License-Identifier: Apache-2.0
import com.m3.indexstring.M3Text;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.function.Supplier;

/** Narrow exploratory warm-allocation/latency probe; not JMH or full-workload acceptance. */
final class AllocationProbe {
  private static volatile Object sink;
  private AllocationProbe() {}
  public static void main(String[] args) {
    String left = "ab\u0100x".repeat(1024), right = "cd\u0200y".repeat(1024);
    M3Text a = M3Text.fromString(left), b = M3Text.fromString(right);
    String joined = left.concat(right); M3Text view = a.concat(b);
    if (!view.toString().equals(joined)) throw new AssertionError();
    measure("string_concat_8192_utf16", () -> left.concat(right));
    measure("m3_concat_2_retained_segments", () -> a.concat(b));
    measure("string_toCharArray_8192", joined::toCharArray);
    measure("m3_toCharArray_8192", view::toCharArray);
    measure("string_slice_1", () -> joined.substring(4095, 4096));
    measure("m3_slice_1_retains_full_body", () -> view.substring(4095, 4096));
    // Keep canonical inputs alive across each measured loop.
    if (view.length() != 8192 || a.length() != 4096) throw new AssertionError();
  }
  private static void measure(String name, Supplier<Object> operation) {
    var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    if (!bean.isThreadAllocatedMemorySupported()) throw new UnsupportedOperationException("allocation counter unavailable");
    bean.setThreadAllocatedMemoryEnabled(true);
    int count = 20000;
    for (int round = 0; round < 3; round++) for (int i = 0; i < count; i++) sink = operation.get();
    long thread = Thread.currentThread().threadId();
    long allocated = bean.getThreadAllocatedBytes(thread), start = System.nanoTime();
    for (int i = 0; i < count; i++) sink = operation.get();
    long elapsed = System.nanoTime() - start, bytes = bean.getThreadAllocatedBytes(thread) - allocated;
    System.out.printf(Locale.ROOT, "%s,%d,%.3f,%.3f%n", name, count, (double) elapsed / count, (double) bytes / count);
  }
}
