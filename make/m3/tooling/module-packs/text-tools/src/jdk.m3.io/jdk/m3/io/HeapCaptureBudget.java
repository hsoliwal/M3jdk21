// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/** Automatic small hot-buffer budget derived from Xmx, current heap use and stream fan-out. */
final class HeapCaptureBudget {
  private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
  private static final long KIB = 1024L;
  private static final long MIB = 1024L * KIB;

  private HeapCaptureBudget() { }

  static int perStream(int streamCount) {
    if (streamCount < 1 || streamCount > 65) {
      throw new IllegalArgumentException("invalid capture stream count");
    }
    long max = Runtime.getRuntime().maxMemory();
    long used = MEMORY.getHeapMemoryUsage().getUsed();
    long headroom = Math.max(0, max - Math.max(0, used));
    long total = Math.min(4 * MIB, Math.min(max / 64, headroom / 8));
    return (int) Math.max(4 * KIB, total / streamCount);
  }
}
