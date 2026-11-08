// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import jdk.m3.collections.M3ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/** Deterministic backward may-liveness; preserves the compact API and adds width-independent analysis. */
public final class Liveness {
  private Liveness() {}

  /** Defensive copies prevent post-analysis mutation of a proof input or result. */
  public record Result(long[] liveIn, long[] liveOut, long transfers) {
    public Result {
      liveIn = liveIn.clone();
      liveOut = liveOut.clone();
      if (liveIn.length != liveOut.length || transfers < 0) throw new IllegalArgumentException();
    }
    @Override public long[] liveIn() { return liveIn.clone(); }
    @Override public long[] liveOut() { return liveOut.clone(); }
  }

  /**
   * The caller must supply ALL normal and exceptional CFG edges and conservative USE/DEF.
   * A dead local is not a dead object: aliases, JNI/global roots, identity, synchronization,
   * weak-reference semantics and ownership remain separate proof obligations.
   */
  public static Result analyze(long[] use, long[] def, int[][] successors, M3ProgressMonitor monitor) {
    if (monitor != null) monitor.checkCanceled();
    Objects.requireNonNull(use, "use");
    Objects.requireNonNull(def, "def");
    Objects.requireNonNull(successors, "successors");
    int n = use.length;
    if (def.length != n || successors.length != n) throw new IllegalArgumentException("CFG dimensions");
    use = use.clone();
    def = def.clone();
    int[][] edges = new int[n][];
    List<List<Integer>> predecessors = new ArrayList<>(n);
    for (int i = 0; i < n; i++) predecessors.add(new ArrayList<>());
    for (int i = 0; i < n; i++) {
      if (monitor != null) monitor.checkCanceled();
      edges[i] = Arrays.stream(Objects.requireNonNull(successors[i], "successors row"))
          .sorted().distinct().toArray();
      for (int s : edges[i]) {
        if (s < 0 || s >= n) throw new IllegalArgumentException("CFG edge outside node range");
        predecessors.get(s).add(i);
      }
    }
    long[] in = new long[n], out = new long[n];
    boolean[] queued = new boolean[n];
    ArrayDeque<Integer> work = new ArrayDeque<>();
    for (int i = n - 1; i >= 0; i--) { work.addLast(i); queued[i] = true; }
    long transfers = 0;
    while (!work.isEmpty()) {
      if (monitor != null) monitor.checkCanceled();
      int node = work.removeFirst();
      queued[node] = false;
      long nextOut = 0;
      for (int successor : edges[node]) nextOut |= in[successor];
      long nextIn = use[node] | (nextOut & ~def[node]);
      out[node] = nextOut;
      transfers = Math.incrementExact(transfers);
      if (nextIn != in[node]) {
        in[node] = nextIn;
        for (int predecessor : predecessors.get(node)) {
          if (!queued[predecessor]) { work.addLast(predecessor); queued[predecessor] = true; }
        }
      }
    }
    return new Result(in, out, transfers);
  }

  /** Immutable analysis of local bindings, not a reference count or object-reclamation proof. */
  public static final class WideResult {
    private final BitSet[] in;
    private final BitSet[] out;
    private final int slots;
    private final long transfers;
    private WideResult(BitSet[] in, BitSet[] out, int slots, long transfers) {
      this.in = copySets(in, slots); this.out = copySets(out, slots);
      this.slots = slots; this.transfers = transfers;
    }
    public int nodeCount() { return in.length; }
    public int slotCount() { return slots; }
    public long transfers() { return transfers; }
    public BitSet liveIn(int node) { return (BitSet) in[node].clone(); }
    public BitSet liveOut(int node) { return (BitSet) out[node].clone(); }
    public boolean isLiveIn(int node, int slot) { return in[node].get(Objects.checkIndex(slot, slots)); }
    public boolean isLiveOut(int node, int slot) { return out[node].get(Objects.checkIndex(slot, slots)); }
    public boolean authorizesReclamation() { return false; }
  }

  /**
   * Arbitrary-width variant. Input arrays/sets must not be concurrently modified during admission.
   * All normal and exceptional successors are required. Inputs/results are defensively copied.
   * Nodes are queued at most once; predecessor propagation uses the existing monotone equation.
   */
  public static WideResult analyze(BitSet[] use, BitSet[] def, int[][] successors,
                                    int slotCount, M3ProgressMonitor monitor) {
    check(monitor);
    Objects.requireNonNull(use, "use"); Objects.requireNonNull(def, "def");
    Objects.requireNonNull(successors, "successors");
    if (slotCount < 0 || use.length != def.length || use.length != successors.length) {
      throw new IllegalArgumentException("CFG dimensions");
    }
    use = copySets(use, slotCount); def = copySets(def, slotCount);
    int n = use.length;
    int[][] edges = new int[n][], predecessors = new int[n][];
    int[] counts = new int[n];
    for (int i = 0; i < n; i++) {
      check(monitor);
      edges[i] = Arrays.stream(Objects.requireNonNull(successors[i], "successors row"))
          .sorted().distinct().toArray();
      for (int successor : edges[i]) {
        if (successor < 0 || successor >= n) throw new IllegalArgumentException("CFG edge outside node range");
        counts[successor] = Math.incrementExact(counts[successor]);
      }
    }
    for (int i = 0; i < n; i++) predecessors[i] = new int[counts[i]];
    Arrays.fill(counts, 0);
    for (int i = 0; i < n; i++) for (int successor : edges[i]) predecessors[successor][counts[successor]++] = i;
    BitSet[] in = new BitSet[n], out = new BitSet[n];
    int[] queue = new int[n];
    boolean[] queued = new boolean[n];
    for (int i = 0; i < n; i++) {
      check(monitor);
      in[i] = new BitSet(slotCount); out[i] = new BitSet(slotCount);
      queue[i] = n - 1 - i; queued[i] = true;
    }
    int head = 0, tail = 0, size = n;
    long transfers = 0;
    BitSet next = new BitSet(slotCount);
    while (size != 0) {
      check(monitor);
      int node = queue[head]; head = head + 1 == n ? 0 : head + 1;
      size--; queued[node] = false;
      next.clear();
      for (int i = 0; i < edges[node].length; i++) {
        if ((i & 255) == 0) check(monitor);
        next.or(in[edges[node][i]]);
      }
      out[node].clear(); out[node].or(next);
      next.andNot(def[node]); next.or(use[node]);
      transfers = Math.incrementExact(transfers);
      if (!next.equals(in[node])) {
        in[node].clear(); in[node].or(next);
        for (int predecessor : predecessors[node]) if (!queued[predecessor]) {
          queue[tail] = predecessor; tail = tail + 1 == n ? 0 : tail + 1;
          size++; queued[predecessor] = true;
        }
      }
    }
    check(monitor);
    return new WideResult(in, out, slotCount, transfers);
  }

  private static BitSet[] copySets(BitSet[] values, int slots) {
    BitSet[] copy = new BitSet[values.length];
    for (int i = 0; i < values.length; i++) {
      BitSet value = Objects.requireNonNull(values[i], "null bit set");
      if (value.length() > slots) throw new IllegalArgumentException("slot outside declared width");
      copy[i] = (BitSet) value.clone();
    }
    return copy;
  }
  private static void check(M3ProgressMonitor monitor) { if (monitor != null) monitor.checkCanceled(); }

  /** Batched lattice atom IN = USE | (OUT & ~DEF). Arrays must remain stable during the call. */
  public static long[] transferBatch(long[] use, long[] def, long[] out, M3ProgressMonitor monitor) {
    check(monitor); dimensions(use, def, out);
    long[] result = new long[use.length];
    for (int i = 0; i < result.length; i++) {
      if ((i & 1023) == 0) check(monitor);
      result[i] = use[i] | (out[i] & ~def[i]);
    }
    check(monitor); return result;
  }

  /** Opt-in JNI lattice atom using the same reviewed compiler library; never a per-node default. */
  public static long[] nativeTransferBatch(long[] use, long[] def, long[] out, M3ProgressMonitor monitor) {
    check(monitor); dimensions(use, def, out);
    if (livenessAbi0() != 1) throw new LinkageError("M3 liveness JNI ABI mismatch");
    long[] result = new long[use.length];
    for (int offset = 0; offset < result.length;) {
      check(monitor);
      int length = Math.min(1024, result.length - offset);
      transfer0(use, def, out, result, offset, length);
      offset += length;
    }
    check(monitor); return result;
  }
  private static void dimensions(long[] use, long[] def, long[] out) {
    Objects.requireNonNull(use, "use"); Objects.requireNonNull(def, "def"); Objects.requireNonNull(out, "out");
    if (use.length != def.length || use.length != out.length) throw new IllegalArgumentException("lane dimensions");
  }
  private static native int livenessAbi0();
  private static native void transfer0(long[] use, long[] def, long[] out, long[] result, int offset, int length);

  /** Difference of local-slot requirement masks; does not identify expired heap objects. */
  public static long[] endingBatch(long[] before, long[] after, M3ProgressMonitor monitor) {
    check(monitor); endingDimensions(before, after);
    long[] result = new long[before.length];
    for (int i = 0; i < result.length; i++) {
      if ((i & 1023) == 0) check(monitor);
      result[i] = before[i] & ~after[i];
    }
    check(monitor); return result;
  }

  /** Explicit native batch; no library loading or silent fallback and no runtime object releases. */
  public static long[] nativeEndingBatch(long[] before, long[] after, M3ProgressMonitor monitor) {
    check(monitor); endingDimensions(before, after);
    if (endingAbi0() != 1) throw new LinkageError("M3 ending JNI ABI mismatch");
    long[] result = new long[before.length];
    for (int offset = 0; offset < result.length;) {
      check(monitor);
      int length = Math.min(1024, result.length - offset);
      ending0(before, after, result, offset, length);
      offset += length;
    }
    check(monitor); return result;
  }
  private static void endingDimensions(long[] before, long[] after) {
    Objects.requireNonNull(before, "before"); Objects.requireNonNull(after, "after");
    if (before.length != after.length) throw new IllegalArgumentException("lane dimensions");
  }
  private static native int endingAbi0();
  private static native void ending0(long[] before, long[] after, long[] result, int offset, int length);

}
