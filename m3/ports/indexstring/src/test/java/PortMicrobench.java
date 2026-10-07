// SPDX-License-Identifier: Apache-2.0
import com.m3.text.compat.M3Text;
import com.synexia.indexstring.FrozenChars;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

/** Exploratory allocation/latency probe, not JMH or a production throughput acceptance test. */
public final class PortMicrobench {
  private static volatile Object sink;
  private static final int WARMUP=2000, ITERATIONS=10000;
  private PortMicrobench() { }
  private static void measure(String shape, String operation, Supplier<?> action) {
    com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    if (!bean.isThreadAllocatedMemorySupported())throw new IllegalStateException("allocation counters unavailable");
    if (!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
    for (int i=0;i<WARMUP;i++)sink=action.get();
    long thread=Thread.currentThread().threadId();
    long allocated=bean.getThreadAllocatedBytes(thread), start=System.nanoTime();
    for (int i=0;i<ITERATIONS;i++)sink=action.get();
    long elapsed=System.nanoTime()-start, bytes=bean.getThreadAllocatedBytes(thread)-allocated;
    System.out.printf("{\"shape\":\"%s\",\"operation\":\"%s\",\"iterations\":%d,\"warmup\":%d,\"elapsed_ns\":%d,\"allocated_bytes\":%d}%n",
        shape,operation,ITERATIONS,WARMUP,elapsed,bytes);
  }
  public static void main(String[] args) {
    for (int size:new int[]{8,4096}) {
      String a="a".repeat(size), b="b".repeat(size);String shape="ascii-"+size+"+"+size;
      M3Text left=M3Text.fromFrozen(FrozenChars.copyOf(a.toCharArray()));
      M3Text right=M3Text.fromFrozen(FrozenChars.copyOf(b.toCharArray()));
      M3Text warm=left.concat(right);sink=warm;
      if (!warm.asString().equals(a+b))throw new AssertionError();
      measure(shape,"stock-concat",()->a.concat(b));
      measure(shape,"warm-retained-concat",()->left.concat(right));
      measure(shape,"cold-frozen-admission",()->M3Text.fromFrozen(FrozenChars.copyOf(a.toCharArray())));
      measure(shape,"materialize-char-output",warm::toCharArray);
      measure(shape,"retained-search",()->warm.indexOf("ab"));
      String flat=a+b;measure(shape,"stock-search",()->flat.indexOf("ab"));
    }
  }
}
