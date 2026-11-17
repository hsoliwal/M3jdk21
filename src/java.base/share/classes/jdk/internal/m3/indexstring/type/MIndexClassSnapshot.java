// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable hierarchy snapshot over one prefix of an {@link MIndexClassPool}.
 *
 * <p>Reverse subtype edges are retained as primitive CSR. Kahn topological scheduling and BFS
 * reachability/shortest-distance kernels are the same general algorithm families commonly taught
 * by LeetCode/HackerRank; donor custody is documented separately and no judge solution source is
 * copied.</p>
 */
public final class MIndexClassSnapshot {
  private final MIndexClassPool pool;
  private final int classCount;
  private final int[] childStarts;
  private final int[] childHandles;
  private final int[] topologicalOrder;
  private final int[] depths;
  private final long[] ancestorSignals;
  private final String topologySha256;

  private MIndexClassSnapshot(
      MIndexClassPool pool,
      int classCount,
      int[] childStarts,
      int[] childHandles,
      int[] topologicalOrder,
      int[] depths,
      long[] ancestorSignals,
      String topologySha256) {
    this.pool = pool;
    this.classCount = classCount;
    this.childStarts = childStarts;
    this.childHandles = childHandles;
    this.topologicalOrder = topologicalOrder;
    this.depths = depths;
    this.ancestorSignals = ancestorSignals;
    this.topologySha256 = topologySha256;
  }

  static MIndexClassSnapshot of(MIndexClassPool pool) {
    Objects.requireNonNull(pool, "pool");
    int count = pool.size();
    int[] childCounts = new int[count + 1];
    int edgeCount = 0;
    for (int child = 1; child <= count; child++) {
      int parents = pool.directParentCount(child);
      edgeCount = Math.addExact(edgeCount, parents);
      for (int ordinal = 0; ordinal < parents; ordinal++) {
        int parent = pool.directParentHandle(child, ordinal);
        if (parent > count) throw new IllegalStateException("snapshot contains future parent");
        childCounts[parent] = Math.addExact(childCounts[parent], 1);
      }
    }

    int[] starts = new int[count + 2];
    for (int handle = 1; handle <= count; handle++) {
      starts[handle + 1] = Math.addExact(starts[handle], childCounts[handle]);
    }
    int[] children = new int[edgeCount];
    int[] cursor = starts.clone();
    for (int child = 1; child <= count; child++) {
      int parents = pool.directParentCount(child);
      for (int ordinal = 0; ordinal < parents; ordinal++) {
        int parent = pool.directParentHandle(child, ordinal);
        children[cursor[parent]++] = child;
      }
    }

    int[] indegree = new int[count + 1];
    for (int handle = 1; handle <= count; handle++) {
      indegree[handle] = pool.directParentCount(handle);
    }
    int[] queue = new int[Math.max(1, count)];
    int head = 0;
    int tail = 0;
    for (int handle = 1; handle <= count; handle++) {
      if (indegree[handle] == 0) queue[tail++] = handle;
    }
    int[] order = new int[count];
    int out = 0;
    while (head < tail) {
      int parent = queue[head++];
      order[out++] = parent;
      for (int p = starts[parent]; p < starts[parent + 1]; p++) {
        int child = children[p];
        int remaining = --indegree[child];
        if (remaining == 0) queue[tail++] = child;
      }
    }
    if (out != count) throw new IllegalStateException("MIndexClass hierarchy cycle");

    int[] depths = new int[count + 1];
    long[] signals = new long[count + 1];
    for (int ordinal = 0; ordinal < order.length; ordinal++) {
      int handle = order[ordinal];
      int maxDepth = 0;
      long signal = bit(handle);
      int parents = pool.directParentCount(handle);
      for (int p = 0; p < parents; p++) {
        int parent = pool.directParentHandle(handle, p);
        maxDepth = Math.max(maxDepth, Math.addExact(depths[parent], 1));
        signal |= signals[parent] | bit(parent);
      }
      depths[handle] = maxDepth;
      signals[handle] = signal;
    }

    return new MIndexClassSnapshot(
        pool, count, starts, children, order, depths, signals, topologyDigest(pool, count));
  }

  public MIndexClassPool pool() { return pool; }
  public int classCount() { return classCount; }
  public int edgeCount() { return childHandles.length; }
  public String topologySha256() { return topologySha256; }

  public int directSubtypeCount(int handle) {
    checkHandle(handle);
    return childStarts[handle + 1] - childStarts[handle];
  }

  public MIndexClass directSubtypeAt(int handle, int ordinal) {
    int count = directSubtypeCount(handle);
    Objects.checkIndex(ordinal, count);
    return pool.value(childHandles[childStarts[handle] + ordinal]);
  }

  public int[] directSubtypeHandles(int handle) {
    checkHandle(handle);
    return Arrays.copyOfRange(childHandles, childStarts[handle], childStarts[handle + 1]);
  }

  public int depth(int handle) {
    checkHandle(handle);
    return depths[handle];
  }

  public MIndexClass topologicalClassAt(int ordinal) {
    return pool.value(topologicalOrder[Objects.checkIndex(ordinal, topologicalOrder.length)]);
  }

  public int[] topologicalHandles() {
    return topologicalOrder.clone();
  }

  /** JVM-style reflexive assignability over the retained superclass/interface graph. */
  public boolean isAssignableFrom(int targetSuperHandle, int candidateSubHandle) {
    checkHandle(targetSuperHandle);
    checkHandle(candidateSubHandle);
    if (targetSuperHandle == candidateSubHandle) return true;
    if ((ancestorSignals[candidateSubHandle] & bit(targetSuperHandle)) == 0L) return false;
    return inheritanceDistance(targetSuperHandle, candidateSubHandle) >= 0;
  }

  /**
   * Shortest superclass/interface edge count, or -1 when {@code candidateSubHandle} is unrelated.
   */
  public int inheritanceDistance(int targetSuperHandle, int candidateSubHandle) {
    checkHandle(targetSuperHandle);
    checkHandle(candidateSubHandle);
    if (targetSuperHandle == candidateSubHandle) return 0;
    if ((ancestorSignals[candidateSubHandle] & bit(targetSuperHandle)) == 0L) return -1;

    boolean[] seen = new boolean[classCount + 1];
    int[] queue = new int[Math.max(1, classCount)];
    int[] distance = new int[classCount + 1];
    int head = 0;
    int tail = 0;
    queue[tail++] = targetSuperHandle;
    seen[targetSuperHandle] = true;
    while (head < tail) {
      int current = queue[head++];
      int nextDistance = Math.addExact(distance[current], 1);
      for (int p = childStarts[current]; p < childStarts[current + 1]; p++) {
        int child = childHandles[p];
        if (seen[child]) continue;
        if (child == candidateSubHandle) return nextDistance;
        seen[child] = true;
        distance[child] = nextDistance;
        queue[tail++] = child;
      }
    }
    return -1;
  }

  /** Sorted transitive ancestors, excluding the subject itself. */
  public int[] ancestors(int handle) {
    checkHandle(handle);
    boolean[] seen = new boolean[classCount + 1];
    int[] queue = new int[Math.max(1, classCount)];
    int head = 0;
    int tail = 0;
    int parents = pool.directParentCount(handle);
    for (int ordinal = 0; ordinal < parents; ordinal++) {
      int parent = pool.directParentHandle(handle, ordinal);
      if (!seen[parent]) {
        seen[parent] = true;
        queue[tail++] = parent;
      }
    }
    while (head < tail) {
      int current = queue[head++];
      int count = pool.directParentCount(current);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        int parent = pool.directParentHandle(current, ordinal);
        if (!seen[parent]) {
          seen[parent] = true;
          queue[tail++] = parent;
        }
      }
    }
    return sortedSeen(seen);
  }

  /** Sorted transitive descendants, excluding the subject itself. */
  public int[] descendants(int handle) {
    checkHandle(handle);
    boolean[] seen = new boolean[classCount + 1];
    int[] queue = new int[Math.max(1, classCount)];
    int head = 0;
    int tail = 0;
    for (int p = childStarts[handle]; p < childStarts[handle + 1]; p++) {
      int child = childHandles[p];
      if (!seen[child]) {
        seen[child] = true;
        queue[tail++] = child;
      }
    }
    while (head < tail) {
      int current = queue[head++];
      for (int p = childStarts[current]; p < childStarts[current + 1]; p++) {
        int child = childHandles[p];
        if (!seen[child]) {
          seen[child] = true;
          queue[tail++] = child;
        }
      }
    }
    return sortedSeen(seen);
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * ((long) childStarts.length
                + childHandles.length
                + topologicalOrder.length
                + depths.length)
        + Long.BYTES * (long) ancestorSignals.length;
  }

  private int[] sortedSeen(boolean[] seen) {
    int count = 0;
    for (int handle = 1; handle <= classCount; handle++) if (seen[handle]) count++;
    int[] result = new int[count];
    int out = 0;
    for (int handle = 1; handle <= classCount; handle++) {
      if (seen[handle]) result[out++] = handle;
    }
    return result;
  }

  private void checkHandle(int handle) {
    if (handle < 1 || handle > classCount) {
      throw new IndexOutOfBoundsException("snapshot class handle " + handle);
    }
  }

  private static long bit(int handle) {
    int mixed = handle * 0x9e3779b9;
    mixed ^= mixed >>> 16;
    return 1L << (mixed & 63);
  }

  private static String topologyDigest(MIndexClassPool pool, int count) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(count).array());
      for (int handle = 1; handle <= count; handle++) {
        putLong(digest, pool.identityHash64(handle));
        int parents = pool.directParentCount(handle);
        putInt(digest, parents);
        for (int ordinal = 0; ordinal < parents; ordinal++) {
          putLong(digest, pool.identityHash64(pool.directParentHandle(handle, ordinal)));
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static void putInt(MessageDigest digest, int value) {
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
  }

  private static void putLong(MessageDigest digest, long value) {
    digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }
}
