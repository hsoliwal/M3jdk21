// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable precomputed search geometry for one canonical AST-pool snapshot.
 *
 * <p>This index works on canonical DAG atoms rather than source occurrences. It freezes kind,
 * label, reverse-parent and exact/structural/logic-hash postings. All retained search lanes are
 * primitive arrays.</p>
 */
public final class MIndexASTPoolPrecompute {
  private static final int SOURCE_ALL = 0;
  private static final int SOURCE_KIND = 1;
  private static final int SOURCE_LABEL = 2;
  private static final int SOURCE_EXACT = 3;
  private static final int SOURCE_STRUCTURAL = 4;
  private static final int SOURCE_LOGIC = 5;

  private final MIndexASTPool pool;
  private final int limit;
  private final int labelLimit;
  private final int[] kindOffsets;
  private final int[] handlesByKind;
  private final int[] labelOffsets;
  private final int[] handlesByLabel;
  private final int[] parentOffsets;
  private final int[] parentsByChild;
  private final long[] exactKeys;
  private final int[] exactHandles;
  private final long[] structuralKeys;
  private final int[] structuralHandles;
  private final long[] logicKeys;
  private final int[] logicHandles;

  /** Package-private immutable wire snapshot. Arrays are cloned by the public snapshot method. */
  static record Snapshot(
      int limit,
      int labelLimit,
      int[] kindOffsets,
      int[] handlesByKind,
      int[] labelOffsets,
      int[] handlesByLabel,
      int[] parentOffsets,
      int[] parentsByChild,
      long[] exactKeys,
      int[] exactHandles,
      long[] structuralKeys,
      int[] structuralHandles,
      long[] logicKeys,
      int[] logicHandles,
      String[] labelTexts,
      byte[] atomIdentities) {}

  private MIndexASTPoolPrecompute(
      MIndexASTPool pool,
      int limit,
      int labelLimit,
      int[] kindOffsets,
      int[] handlesByKind,
      int[] labelOffsets,
      int[] handlesByLabel,
      int[] parentOffsets,
      int[] parentsByChild,
      long[] exactKeys,
      int[] exactHandles,
      long[] structuralKeys,
      int[] structuralHandles,
      long[] logicKeys,
      int[] logicHandles) {
    this.pool = pool;
    this.limit = limit;
    this.labelLimit = labelLimit;
    this.kindOffsets = kindOffsets;
    this.handlesByKind = handlesByKind;
    this.labelOffsets = labelOffsets;
    this.handlesByLabel = handlesByLabel;
    this.parentOffsets = parentOffsets;
    this.parentsByChild = parentsByChild;
    this.exactKeys = exactKeys;
    this.exactHandles = exactHandles;
    this.structuralKeys = structuralKeys;
    this.structuralHandles = structuralHandles;
    this.logicKeys = logicKeys;
    this.logicHandles = logicHandles;
  }

  public static MIndexASTPoolPrecompute snapshot(MIndexASTPool pool) {
    Objects.requireNonNull(pool, "pool");
    int limit = pool.size();
    int kinds = Tree.Kind.values().length;
    int labelLimit = pool.labels().size();

    int[] kindCounts = new int[kinds];
    int[] labelCounts = new int[labelLimit + 1];
    int[] parentCounts = new int[limit + 1];
    long edgeCountLong = 0L;

    for (int handle = 1; handle <= limit; handle++) {
      kindCounts[pool.kind(handle).ordinal()]++;
      int label = pool.labelHandle(handle);
      if (label > labelLimit) {
        throw new IllegalStateException("node label was admitted after snapshot label limit");
      }
      labelCounts[label]++;

      int children = pool.childCount(handle);
      edgeCountLong = Math.addExact(edgeCountLong, children);
      if (edgeCountLong > Integer.MAX_VALUE) {
        throw new IllegalStateException("AST reverse-parent postings exceed int arena");
      }
      for (int ordinal = 0; ordinal < children; ordinal++) {
        parentCounts[pool.childHandle(handle, ordinal)]++;
      }
    }

    int[] kindOffsets = offsets(kindCounts);
    int[] handlesByKind = new int[limit];
    int[] kindCursor = Arrays.copyOf(kindOffsets, kinds);

    int[] labelOffsets = offsets(labelCounts);
    int[] handlesByLabel = new int[limit];
    int[] labelCursor = Arrays.copyOf(labelOffsets, labelCounts.length);

    int[] parentOffsets = offsets(parentCounts);
    int[] parentsByChild = new int[(int) edgeCountLong];
    int[] parentCursor = Arrays.copyOf(parentOffsets, parentCounts.length);

    long[] exactKeys = new long[limit];
    int[] exactHandles = new int[limit];
    long[] structuralKeys = new long[limit];
    int[] structuralHandles = new int[limit];
    long[] logicKeys = new long[limit];
    int[] logicHandles = new int[limit];

    for (int handle = 1; handle <= limit; handle++) {
      int index = handle - 1;
      int kind = pool.kind(handle).ordinal();
      handlesByKind[kindCursor[kind]++] = handle;

      int label = pool.labelHandle(handle);
      handlesByLabel[labelCursor[label]++] = handle;

      int children = pool.childCount(handle);
      for (int ordinal = 0; ordinal < children; ordinal++) {
        int child = pool.childHandle(handle, ordinal);
        parentsByChild[parentCursor[child]++] = handle;
      }

      exactKeys[index] = pool.exactHash64(handle);
      exactHandles[index] = handle;
      structuralKeys[index] = pool.structuralHash64(handle);
      structuralHandles[index] = handle;
      logicKeys[index] = pool.logicHash64(handle);
      logicHandles[index] = handle;
    }

    sortPairs(exactKeys, exactHandles);
    sortPairs(structuralKeys, structuralHandles);
    sortPairs(logicKeys, logicHandles);

    return new MIndexASTPoolPrecompute(
        pool,
        limit,
        labelLimit,
        kindOffsets,
        handlesByKind,
        labelOffsets,
        handlesByLabel,
        parentOffsets,
        parentsByChild,
        exactKeys,
        exactHandles,
        structuralKeys,
        structuralHandles,
        logicKeys,
        logicHandles);
  }

  /** Captures this precompute with stable atom identities and label text for cold restoration. */
  Snapshot snapshot() {
    String[] labelTexts = new String[labelLimit];
    for (int handle = 1; handle <= labelLimit; handle++) {
      labelTexts[handle - 1] = pool.labels().value(handle).materialize();
    }

    byte[] atomIdentities = new byte[Math.multiplyExact(limit, 32)];
    for (int handle = 1; handle <= limit; handle++) {
      byte[] identity = pool.sha256(handle);
      if (identity.length != 32) throw new IllegalStateException("unexpected atom identity size");
      System.arraycopy(identity, 0, atomIdentities, Math.multiplyExact(handle - 1, 32), 32);
    }

    return new Snapshot(
        limit,
        labelLimit,
        kindOffsets.clone(),
        handlesByKind.clone(),
        labelOffsets.clone(),
        handlesByLabel.clone(),
        parentOffsets.clone(),
        parentsByChild.clone(),
        exactKeys.clone(),
        exactHandles.clone(),
        structuralKeys.clone(),
        structuralHandles.clone(),
        logicKeys.clone(),
        logicHandles.clone(),
        labelTexts,
        atomIdentities);
  }

  /**
   * Restores a cold snapshot against a live pool.
   *
   * <p>Atom handles are remapped through stable subtree identities and label handles through their
   * materialized text. Every posting is then checked against the live pool. A mismatch fails closed
   * so the caller can deterministically rebuild instead of exposing stale process-local handles.</p>
   */
  static MIndexASTPoolPrecompute restore(MIndexASTPool pool, Snapshot payload) {
    Objects.requireNonNull(pool, "pool");
    Objects.requireNonNull(payload, "payload");
    int limit = payload.limit();
    int labelLimit = payload.labelLimit();
    if (limit < 0 || limit > 16_000_000 || labelLimit < 0 || labelLimit > 16_000_000) {
      throw new IllegalArgumentException("pool precompute bounds exceeded");
    }
    if (pool.size() != limit || pool.labels().size() != labelLimit) {
      throw new IllegalArgumentException("pool precompute pool size mismatch");
    }
    if (payload.kindOffsets().length != Tree.Kind.values().length + 1
        || payload.handlesByKind().length != limit
        || payload.labelOffsets().length != labelLimit + 2
        || payload.handlesByLabel().length != limit
        || payload.parentOffsets().length != limit + 2
        || payload.exactKeys().length != limit
        || payload.exactHandles().length != limit
        || payload.structuralKeys().length != limit
        || payload.structuralHandles().length != limit
        || payload.logicKeys().length != limit
        || payload.logicHandles().length != limit
        || payload.labelTexts().length != labelLimit
        || payload.atomIdentities().length != Math.multiplyExact(limit, 32)) {
      throw new IllegalArgumentException("pool precompute snapshot shape mismatch");
    }

    validateOffsets(payload.kindOffsets(), limit, "kind");
    validateOffsets(payload.labelOffsets(), limit, "label");
    validateOffsets(payload.parentOffsets(), payload.parentsByChild().length, "parent");

    int[] atomMap = remapAtoms(pool, payload.atomIdentities(), limit);
    int[] labelMap = remapLabels(pool, payload.labelTexts(), labelLimit);
    int[] kindOffsets = payload.kindOffsets().clone();
    int[] handlesByKind = remapKindPosting(pool, payload.handlesByKind(), kindOffsets, atomMap, limit);
    int[] labelOffsets;
    int[] handlesByLabel;
    {
      int[] counts = new int[labelLimit + 1];
      int[] oldHandles = payload.handlesByLabel();
      for (int oldLabel = 0; oldLabel <= labelLimit; oldLabel++) {
        int currentLabel = oldLabel == 0 ? 0 : labelMap[oldLabel];
        counts[currentLabel] =
            Math.addExact(counts[currentLabel],
                payload.labelOffsets()[oldLabel + 1] - payload.labelOffsets()[oldLabel]);
      }
      labelOffsets = offsets(counts);
      handlesByLabel = new int[limit];
      int[] cursor = Arrays.copyOf(labelOffsets, labelOffsets.length - 1);
      boolean[] seen = new boolean[limit + 1];
      for (int oldLabel = 0; oldLabel <= labelLimit; oldLabel++) {
        int currentLabel = oldLabel == 0 ? 0 : labelMap[oldLabel];
        for (int index = payload.labelOffsets()[oldLabel];
            index < payload.labelOffsets()[oldLabel + 1];
            index++) {
          int oldHandle = oldHandles[index];
          int currentHandle = mapHandle(atomMap, oldHandle, limit);
          if (pool.labelHandle(currentHandle) != currentLabel || seen[currentHandle]) {
            throw new IllegalArgumentException("pool label posting mismatch");
          }
          seen[currentHandle] = true;
          handlesByLabel[cursor[currentLabel]++] = currentHandle;
        }
      }
      for (int label = 0; label <= labelLimit; label++) {
        Arrays.sort(handlesByLabel, labelOffsets[label], labelOffsets[label + 1]);
      }
    }

    int[] parentOffsets;
    int[] parentsByChild;
    {
      int[] counts = new int[limit + 1];
      for (int oldChild = 1; oldChild <= limit; oldChild++) {
        int currentChild = atomMap[oldChild];
        counts[currentChild] =
            Math.addExact(
                counts[currentChild],
                payload.parentOffsets()[oldChild + 1] - payload.parentOffsets()[oldChild]);
      }
      parentOffsets = offsets(counts);
      parentsByChild = new int[payload.parentsByChild().length];
      int[] cursor = Arrays.copyOf(parentOffsets, parentOffsets.length - 1);
      for (int oldChild = 1; oldChild <= limit; oldChild++) {
        int currentChild = atomMap[oldChild];
        for (int index = payload.parentOffsets()[oldChild];
            index < payload.parentOffsets()[oldChild + 1];
            index++) {
          parentsByChild[cursor[currentChild]++] =
              mapHandle(atomMap, payload.parentsByChild()[index], limit);
        }
      }
      for (int child = 1; child <= limit; child++) {
        Arrays.sort(parentsByChild, parentOffsets[child], parentOffsets[child + 1]);
      }
      int[] expectedCounts = new int[limit + 1];
      for (int parent = 1; parent <= limit; parent++) {
        for (int ordinal = 0; ordinal < pool.childCount(parent); ordinal++) {
          expectedCounts[pool.childHandle(parent, ordinal)]++;
        }
      }
      if (!Arrays.equals(expectedCounts, counts)) {
        throw new IllegalArgumentException("pool reverse-parent counts mismatch");
      }
      int[] expected = new int[parentsByChild.length];
      int[] expectedCursor = Arrays.copyOf(parentOffsets, parentOffsets.length - 1);
      for (int parent = 1; parent <= limit; parent++) {
        for (int ordinal = 0; ordinal < pool.childCount(parent); ordinal++) {
          int child = pool.childHandle(parent, ordinal);
          expected[expectedCursor[child]++] = parent;
        }
      }
      if (!Arrays.equals(expected, parentsByChild)) {
        throw new IllegalArgumentException("pool reverse-parent postings mismatch");
      }
    }

    long[] exactKeys = payload.exactKeys().clone();
    int[] exactHandles = remapPairs(payload.exactHandles(), atomMap, limit);
    sortPairs(exactKeys, exactHandles);
    validatePairs(pool, exactKeys, exactHandles, HashKind.EXACT);

    long[] structuralKeys = payload.structuralKeys().clone();
    int[] structuralHandles = remapPairs(payload.structuralHandles(), atomMap, limit);
    sortPairs(structuralKeys, structuralHandles);
    validatePairs(pool, structuralKeys, structuralHandles, HashKind.STRUCTURAL);

    long[] logicKeys = payload.logicKeys().clone();
    int[] logicHandles = remapPairs(payload.logicHandles(), atomMap, limit);
    sortPairs(logicKeys, logicHandles);
    validatePairs(pool, logicKeys, logicHandles, HashKind.LOGIC);

    return new MIndexASTPoolPrecompute(
        pool,
        limit,
        labelLimit,
        kindOffsets,
        handlesByKind,
        labelOffsets,
        handlesByLabel,
        parentOffsets,
        parentsByChild,
        exactKeys,
        exactHandles,
        structuralKeys,
        structuralHandles,
        logicKeys,
        logicHandles);
  }

  private enum HashKind {
    EXACT,
    STRUCTURAL,
    LOGIC
  }

  private static int[] remapAtoms(MIndexASTPool pool, byte[] identities, int limit) {
    Map<IdentityKey, Integer> current = new HashMap<>();
    for (int handle = 1; handle <= limit; handle++) {
      Integer previous = current.put(new IdentityKey(pool.sha256(handle)), handle);
      if (previous != null) throw new IllegalArgumentException("duplicate live atom identity");
    }
    int[] map = new int[limit + 1];
    for (int oldHandle = 1; oldHandle <= limit; oldHandle++) {
      byte[] identity =
          Arrays.copyOfRange(identities, Math.multiplyExact(oldHandle - 1, 32), oldHandle * 32);
      Integer currentHandle = current.get(new IdentityKey(identity));
      if (currentHandle == null) throw new IllegalArgumentException("stale atom identity");
      map[oldHandle] = currentHandle;
    }
    return map;
  }

  private static int[] remapLabels(MIndexASTPool pool, String[] texts, int labelLimit) {
    Map<String, Integer> current = new HashMap<>();
    for (int handle = 1; handle <= labelLimit; handle++) {
      String text = pool.labels().value(handle).materialize();
      Integer previous = current.put(text, handle);
      if (previous != null) throw new IllegalArgumentException("duplicate live label identity");
    }
    int[] map = new int[labelLimit + 1];
    for (int oldHandle = 1; oldHandle <= labelLimit; oldHandle++) {
      String text = texts[oldHandle - 1];
      if (text == null) throw new IllegalArgumentException("null persisted label");
      Integer currentHandle = current.get(text);
      if (currentHandle == null) throw new IllegalArgumentException("stale label identity");
      map[oldHandle] = currentHandle;
    }
    return map;
  }

  private static int[] remapKindPosting(
      MIndexASTPool pool, int[] old, int[] offsets, int[] atomMap, int limit) {
    int[] result = old.clone();
    boolean[] seen = new boolean[limit + 1];
    for (int kind = 0; kind < offsets.length - 1; kind++) {
      for (int index = offsets[kind]; index < offsets[kind + 1]; index++) {
        int current = mapHandle(atomMap, result[index], limit);
        if (pool.kind(current).ordinal() != kind || seen[current]) {
          throw new IllegalArgumentException("pool kind posting mismatch");
        }
        seen[current] = true;
        result[index] = current;
      }
      Arrays.sort(result, offsets[kind], offsets[kind + 1]);
    }
    return result;
  }

  private static int[] remapPairs(int[] old, int[] atomMap, int limit) {
    int[] result = new int[old.length];
    for (int index = 0; index < old.length; index++) {
      result[index] = mapHandle(atomMap, old[index], limit);
    }
    return result;
  }

  private static int mapHandle(int[] map, int handle, int limit) {
    if (handle < 1 || handle > limit || map[handle] < 1) {
      throw new IllegalArgumentException("invalid pool atom handle");
    }
    return map[handle];
  }

  private static void validatePairs(
      MIndexASTPool pool, long[] keys, int[] handles, HashKind kind) {
    boolean[] seen = new boolean[handles.length + 1];
    for (int index = 0; index < handles.length; index++) {
      int handle = handles[index];
      if (handle < 1 || handle >= seen.length || seen[handle]) {
        throw new IllegalArgumentException("pool hash posting handle mismatch");
      }
      long expected =
          switch (kind) {
            case EXACT -> pool.exactHash64(handle);
            case STRUCTURAL -> pool.structuralHash64(handle);
            case LOGIC -> pool.logicHash64(handle);
          };
      if (keys[index] != expected) throw new IllegalArgumentException("pool hash posting key mismatch");
      seen[handle] = true;
      if (index > 0 && compare(keys[index - 1], handles[index - 1], keys[index], handle) > 0) {
        throw new IllegalArgumentException("pool hash postings are not sorted");
      }
    }
  }

  private static void validateOffsets(int[] offsets, int expectedTotal, String name) {
    if (offsets.length == 0 || offsets[0] != 0 || offsets[offsets.length - 1] != expectedTotal) {
      throw new IllegalArgumentException("pool " + name + " offsets total mismatch");
    }
    for (int index = 1; index < offsets.length; index++) {
      if (offsets[index] < offsets[index - 1]) {
        throw new IllegalArgumentException("pool " + name + " offsets are not monotonic");
      }
    }
  }

  private record IdentityKey(byte[] bytes) {
    @Override
    public boolean equals(Object other) {
      return other instanceof IdentityKey that && Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }

  public MIndexASTPool pool() {
    return pool;
  }

  public int atomCount() {
    return limit;
  }

  public int labelCount() {
    return labelLimit;
  }

  public int[] handles(Tree.Kind kind) {
    Objects.requireNonNull(kind, "kind");
    int ordinal = kind.ordinal();
    return Arrays.copyOfRange(handlesByKind, kindOffsets[ordinal], kindOffsets[ordinal + 1]);
  }

  public int[] handlesByLabelHandle(int labelHandle) {
    if (labelHandle < 0 || labelHandle > labelLimit) return new int[0];
    return Arrays.copyOfRange(
        handlesByLabel, labelOffsets[labelHandle], labelOffsets[labelHandle + 1]);
  }

  public int[] parents(int handle) {
    int checked = checkHandle(handle);
    return Arrays.copyOfRange(
        parentsByChild, parentOffsets[checked], parentOffsets[checked + 1]);
  }

  public int incomingParentCount(int handle) {
    int checked = checkHandle(handle);
    return parentOffsets[checked + 1] - parentOffsets[checked];
  }

  public int[] handlesByExactHash(long hash) {
    return lookup(exactKeys, exactHandles, hash);
  }

  public int[] handlesByStructuralHash(long hash) {
    return lookup(structuralKeys, structuralHandles, hash);
  }

  public int[] handlesByLogicHash(long hash) {
    return lookup(logicKeys, logicHandles, hash);
  }

  /**
   * Hash-candidate groups with at least {@code minimumCount} atoms.
   *
   * <p>Hash equality is not proof. Use {@link MIndexAST#structurallyEquals(MIndexAST)} before
   * treating two members as structurally identical.</p>
   */
  public int[][] structuralHashGroups(int minimumCount) {
    return groups(structuralKeys, structuralHandles, minimumCount);
  }

  /** Candidate groups for exact-content equality; exact comparison remains the proof boundary. */
  public int[][] exactHashGroups(int minimumCount) {
    return groups(exactKeys, exactHandles, minimumCount);
  }

  public int[][] logicHashGroups(int minimumCount) {
    return groups(logicKeys, logicHandles, minimumCount);
  }

  /**
   * Executes a compiled query using the narrowest available precomputed posting list.
   *
   * <p>All query predicates are still verified by {@link MIndexASTQuery#matchesHandle(int)}.</p>
   */
  public int[] find(MIndexASTQuery query) {
    Objects.requireNonNull(query, "query");
    if (query.pool() != pool) throw new IllegalArgumentException("query belongs to different pool");

    Source source = bestSource(query);
    int[] scratch = new int[source.length()];
    int count = 0;
    for (int index = source.from(); index < source.to(); index++) {
      int handle =
          switch (source.kind()) {
            case SOURCE_ALL -> index + 1;
            case SOURCE_KIND -> handlesByKind[index];
            case SOURCE_LABEL -> handlesByLabel[index];
            case SOURCE_EXACT -> exactHandles[index];
            case SOURCE_STRUCTURAL -> structuralHandles[index];
            case SOURCE_LOGIC -> logicHandles[index];
            default -> throw new AssertionError(source.kind());
          };
      if (query.matchesHandle(handle)) scratch[count++] = handle;
    }
    return Arrays.copyOf(scratch, count);
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * (long)
                (kindOffsets.length
                    + handlesByKind.length
                    + labelOffsets.length
                    + handlesByLabel.length
                    + parentOffsets.length
                    + parentsByChild.length
                    + exactHandles.length
                    + structuralHandles.length
                    + logicHandles.length)
        + Long.BYTES * (long) (exactKeys.length + structuralKeys.length + logicKeys.length);
  }

  private Source bestSource(MIndexASTQuery query) {
    Source best = new Source(SOURCE_ALL, 0, limit);

    Tree.Kind rootKind = query.rootKindOrNull();
    if (rootKind != null) {
      int ordinal = rootKind.ordinal();
      best = narrower(best, new Source(SOURCE_KIND, kindOffsets[ordinal], kindOffsets[ordinal + 1]));
    }

    if (query.labelSpecifiedForIndex()) {
      int label = query.labelHandleForIndex();
      if (label < 0 || label > labelLimit) return new Source(SOURCE_ALL, 0, 0);
      best = narrower(best, new Source(SOURCE_LABEL, labelOffsets[label], labelOffsets[label + 1]));
    }

    if (query.exactHashSpecifiedForIndex()) {
      int from = lowerBound(exactKeys, query.exactHashForIndex());
      int to = upperBound(exactKeys, query.exactHashForIndex());
      best = narrower(best, new Source(SOURCE_EXACT, from, to));
    }

    if (query.structuralHashSpecifiedForIndex()) {
      int from = lowerBound(structuralKeys, query.structuralHashForIndex());
      int to = upperBound(structuralKeys, query.structuralHashForIndex());
      best = narrower(best, new Source(SOURCE_STRUCTURAL, from, to));
    }

    if (query.logicHashSpecifiedForIndex()) {
      int from = lowerBound(logicKeys, query.logicHashForIndex());
      int to = upperBound(logicKeys, query.logicHashForIndex());
      best = narrower(best, new Source(SOURCE_LOGIC, from, to));
    }
    return best;
  }

  private static Source narrower(Source left, Source right) {
    return right.length() < left.length() ? right : left;
  }

  private int checkHandle(int handle) {
    if (handle < 1 || handle > limit) {
      throw new IllegalArgumentException("handle was not present in snapshot: " + handle);
    }
    return handle;
  }

  private static int[] lookup(long[] keys, int[] handles, long hash) {
    int from = lowerBound(keys, hash);
    int to = upperBound(keys, hash);
    return Arrays.copyOfRange(handles, from, to);
  }

  private static int[][] groups(long[] keys, int[] handles, int minimumCount) {
    if (minimumCount < 2) throw new IllegalArgumentException("minimumCount must be >= 2");
    List<int[]> groups = new ArrayList<>();
    int start = 0;
    while (start < keys.length) {
      int end = start + 1;
      while (end < keys.length && keys[end] == keys[start]) end++;
      if (end - start >= minimumCount) groups.add(Arrays.copyOfRange(handles, start, end));
      start = end;
    }
    return groups.toArray(int[][]::new);
  }

  private static int[] offsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static int lowerBound(long[] values, long target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int mid = (low + high) >>> 1;
      if (Long.compareUnsigned(values[mid], target) < 0) low = mid + 1;
      else high = mid;
    }
    return low;
  }

  private static int upperBound(long[] values, long target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int mid = (low + high) >>> 1;
      if (Long.compareUnsigned(values[mid], target) <= 0) low = mid + 1;
      else high = mid;
    }
    return low;
  }

  private static void sortPairs(long[] keys, int[] values) {
    int length = keys.length;
    if (length < 2) return;

    long[] tempKeys = new long[length];
    int[] tempValues = new int[length];
    long[] sourceKeys = keys;
    int[] sourceValues = values;
    long[] destinationKeys = tempKeys;
    int[] destinationValues = tempValues;

    int width = 1;
    while (width < length) {
      int block = width > length / 2 ? length : width << 1;
      for (int start = 0; start < length; start += block) {
        int middle = Math.min(length, start + width);
        int end = Math.min(length, start + block);
        int left = start;
        int right = middle;
        int out = start;
        while (left < middle || right < end) {
          if (right >= end
              || left < middle
                  && compare(
                          sourceKeys[left],
                          sourceValues[left],
                          sourceKeys[right],
                          sourceValues[right])
                      <= 0) {
            destinationKeys[out] = sourceKeys[left];
            destinationValues[out++] = sourceValues[left++];
          } else {
            destinationKeys[out] = sourceKeys[right];
            destinationValues[out++] = sourceValues[right++];
          }
        }
      }

      long[] keySwap = sourceKeys;
      sourceKeys = destinationKeys;
      destinationKeys = keySwap;
      int[] valueSwap = sourceValues;
      sourceValues = destinationValues;
      destinationValues = valueSwap;
      width = block;
    }

    if (sourceKeys != keys) {
      System.arraycopy(sourceKeys, 0, keys, 0, length);
      System.arraycopy(sourceValues, 0, values, 0, length);
    }
  }

  private static int compare(long leftKey, int leftHandle, long rightKey, int rightHandle) {
    int key = Long.compareUnsigned(leftKey, rightKey);
    return key != 0 ? key : Integer.compare(leftHandle, rightHandle);
  }

  private record Source(int kind, int from, int to) {
    int length() {
      return to - from;
    }
  }
}
