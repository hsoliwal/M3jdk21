// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import com.synexia.job.IProgressMonitor;

/** Primitive-array admission limits; byte estimates exclude JVM headers and caller-owned data. */
public record MIndexPrecomputeLimits(
    int maxNodes, int maxEdges, long maxCells, long maxWork, long maxBytes) {
  public static final MIndexPrecomputeLimits DEFAULT =
      new MIndexPrecomputeLimits(1_000_000, 8_000_000, 4_000_000, 100_000_000, 268_435_456);

  public MIndexPrecomputeLimits {
    if (maxNodes < 1 || maxEdges < 0 || maxCells < 1 || maxWork < 1 || maxBytes < 1) {
      throw new IllegalArgumentException("invalid precompute limits");
    }
  }

  void bytes(long bytes) {
    if (bytes < 0 || bytes > maxBytes) throw new IllegalArgumentException("primitive byte budget");
  }

  static int arrayLength(long length) {
    if (length < 0 || length > Integer.MAX_VALUE - 8L) {
      throw new IllegalArgumentException("array length exceeds Java limit");
    }
    return (int) length;
  }

  static final class Work implements AutoCloseable {
    private final IProgressMonitor monitor;
    private final long maximum;
    private long used;

    Work(MIndexPrecomputeLimits limits, IProgressMonitor monitor, String name) {
      this.monitor = monitor == null ? IProgressMonitor.noop() : monitor;
      maximum = limits.maxWork();
      this.monitor.checkCanceled();
      this.monitor.beginTask(name, IProgressMonitor.UNKNOWN);
    }

    void charge(long amount) {
      monitor.checkCanceled();
      if (amount < 0 || amount > maximum - used) {
        throw new IllegalArgumentException("precompute work budget exhausted");
      }
      used += amount;
      monitor.worked(amount);
    }

    @Override public void close() {
      try { monitor.checkCanceled(); } finally { monitor.done(); }
    }
  }
}
