// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Frozen dependency graph of precompute passes.
 *
 * <p>Pass rows are sorted by typed artifact key. Dependencies are retained in one primitive CSR
 * lane and execution order is a deterministic topological ordering. Builder collections are
 * transient and discarded after freeze.</p>
 */
public final class MIndexPrecomputePlan {
  private final MIndexPrecomputeKey<?>[] keys;
  private final MIndexPrecomputePass<?>[] passes;
  private final int[] dependencyStart;
  private final int[] dependencyRows;
  private final int[] executionOrder;
  private final String fingerprint;

  private MIndexPrecomputePlan(
      MIndexPrecomputeKey<?>[] keys,
      MIndexPrecomputePass<?>[] passes,
      int[] dependencyStart,
      int[] dependencyRows,
      int[] executionOrder) {
    this.keys = keys;
    this.passes = passes;
    this.dependencyStart = dependencyStart;
    this.dependencyRows = dependencyRows;
    this.executionOrder = executionOrder;
    this.fingerprint = fingerprint(keys, passes, dependencyStart, dependencyRows);
  }

  public static Builder builder() {
    return new Builder();
  }

  public int size() {
    return keys.length;
  }

  public String fingerprint() {
    return fingerprint;
  }

  public MIndexPrecomputeKey<?> keyAt(int row) {
    return keys[Objects.checkIndex(row, keys.length)];
  }

  public MIndexPrecomputePass<?> passAt(int row) {
    return passes[Objects.checkIndex(row, passes.length)];
  }

  public int[] executionOrder() {
    return executionOrder.clone();
  }

  public int[] dependencyRows(int row) {
    int checked = Objects.checkIndex(row, keys.length);
    return Arrays.copyOfRange(
        dependencyRows, dependencyStart[checked], dependencyStart[checked + 1]);
  }

  int rowOf(MIndexPrecomputeKey<?> key) {
    Objects.requireNonNull(key, "key");
    int low = 0;
    int high = keys.length - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int compared = keys[mid].compareTo(key);
      if (compared < 0) low = mid + 1;
      else if (compared > 0) high = mid - 1;
      else return keys[mid].equals(key) ? mid : -1;
    }
    return -1;
  }

  int dependencyStart(int row) {
    return dependencyStart[Objects.checkIndex(row, keys.length)];
  }

  int dependencyEnd(int row) {
    int checked = Objects.checkIndex(row, keys.length);
    return dependencyStart[checked + 1];
  }

  int dependencyRowAt(int absoluteIndex) {
    return dependencyRows[Objects.checkIndex(absoluteIndex, dependencyRows.length)];
  }

  public static final class Builder {
    private final List<MIndexPrecomputePass<?>> passes = new ArrayList<>();

    public Builder add(MIndexPrecomputePass<?> pass) {
      passes.add(Objects.requireNonNull(pass, "pass"));
      return this;
    }

    public MIndexPrecomputePlan build() {
      MIndexPrecomputePass<?>[] sorted = passes.toArray(MIndexPrecomputePass<?>[]::new);
      Arrays.sort(sorted, Comparator.comparing(MIndexPrecomputePass::output));
      MIndexPrecomputeKey<?>[] keys = new MIndexPrecomputeKey<?>[sorted.length];
      for (int row = 0; row < sorted.length; row++) {
        MIndexPrecomputeKey<?> key = Objects.requireNonNull(sorted[row].output(), "output");
        MIndexPrecomputeScope.required(sorted[row].implementationRevision());
        keys[row] = key;
        if (row > 0 && keys[row - 1].equals(key)) {
          throw new IllegalArgumentException("duplicate precompute output: " + key);
        }
      }

      int[] dependencyStart = new int[sorted.length + 1];
      int dependencyCount = 0;
      for (int row = 0; row < sorted.length; row++) {
        dependencyStart[row] = dependencyCount;
        List<MIndexPrecomputeKey<?>> dependencies =
            List.copyOf(Objects.requireNonNull(sorted[row].dependencies(), "dependencies"));
        dependencyCount = Math.addExact(dependencyCount, dependencies.size());
      }
      dependencyStart[sorted.length] = dependencyCount;

      int[] dependencyRows = new int[dependencyCount];
      int cursor = 0;
      for (int row = 0; row < sorted.length; row++) {
        List<MIndexPrecomputeKey<?>> dependencies = sorted[row].dependencies();
        int[] rows = new int[dependencies.size()];
        for (int index = 0; index < dependencies.size(); index++) {
          MIndexPrecomputeKey<?> dependency =
              Objects.requireNonNull(dependencies.get(index), "dependency");
          int dependencyRow = binarySearch(keys, dependency);
          if (dependencyRow < 0) {
            throw new IllegalArgumentException(
                "missing dependency " + dependency + " for " + keys[row]);
          }
          if (dependencyRow == row) {
            throw new IllegalArgumentException("pass depends on itself: " + keys[row]);
          }
          rows[index] = dependencyRow;
        }
        Arrays.sort(rows);
        for (int index = 0; index < rows.length; index++) {
          if (index > 0 && rows[index - 1] == rows[index]) {
            throw new IllegalArgumentException("duplicate dependency for " + keys[row]);
          }
          dependencyRows[cursor++] = rows[index];
        }
      }

      int[] order = topologicalOrder(keys.length, dependencyStart, dependencyRows);
      return new MIndexPrecomputePlan(
          keys, sorted, dependencyStart, dependencyRows, order);
    }

    private static int[] topologicalOrder(
        int size, int[] dependencyStart, int[] dependencyRows) {
      int[] indegree = new int[size];
      int[] dependentStart = new int[size + 1];
      for (int row = 0; row < size; row++) {
        indegree[row] = dependencyStart[row + 1] - dependencyStart[row];
        for (int at = dependencyStart[row]; at < dependencyStart[row + 1]; at++) {
          dependentStart[dependencyRows[at] + 1]++;
        }
      }
      for (int row = 0; row < size; row++) {
        dependentStart[row + 1] = Math.addExact(dependentStart[row + 1], dependentStart[row]);
      }
      int[] dependentRows = new int[dependencyRows.length];
      int[] cursor = Arrays.copyOf(dependentStart, size);
      for (int row = 0; row < size; row++) {
        for (int at = dependencyStart[row]; at < dependencyStart[row + 1]; at++) {
          int dependency = dependencyRows[at];
          dependentRows[cursor[dependency]++] = row;
        }
      }

      // Primitive min-heap preserves the original smallest-ready-key execution order.
      int[] ready = new int[size];
      int readySize = 0;
      for (int row = 0; row < size; row++) {
        if (indegree[row] == 0) offer(ready, readySize++, row);
      }
      int[] order = new int[size];
      for (int output = 0; output < size; output++) {
        if (readySize == 0) throw new IllegalArgumentException("precompute dependency cycle");
        int next = ready[0];
        ready[0] = ready[--readySize];
        if (readySize > 0) sink(ready, readySize);
        order[output] = next;
        for (int at = dependentStart[next]; at < dependentStart[next + 1]; at++) {
          int dependent = dependentRows[at];
          if (--indegree[dependent] == 0) {
            offer(ready, readySize++, dependent);
          }
        }
      }
      return order;
    }

    private static void offer(int[] heap, int size, int value) {
      while (size > 0) {
        int parent = (size - 1) >>> 1;
        if (heap[parent] <= value) break;
        heap[size] = heap[parent];
        size = parent;
      }
      heap[size] = value;
    }

    private static void sink(int[] heap, int size) {
      int value = heap[0];
      int parent = 0;
      while (parent < (size >>> 1)) {
        int child = (parent << 1) + 1;
        if (child + 1 < size && heap[child + 1] < heap[child]) child++;
        if (value <= heap[child]) break;
        heap[parent] = heap[child];
        parent = child;
      }
      heap[parent] = value;
    }

    private static int binarySearch(
        MIndexPrecomputeKey<?>[] keys, MIndexPrecomputeKey<?> key) {
      int low = 0;
      int high = keys.length - 1;
      while (low <= high) {
        int mid = (low + high) >>> 1;
        int compared = keys[mid].compareTo(key);
        if (compared < 0) low = mid + 1;
        else if (compared > 0) high = mid - 1;
        else return keys[mid].equals(key) ? mid : -1;
      }
      return -1;
    }
  }

  private static String fingerprint(
      MIndexPrecomputeKey<?>[] keys,
      MIndexPrecomputePass<?>[] passes,
      int[] dependencyStart,
      int[] dependencyRows) {
    MessageDigest digest = MIndexPrecomputeScope.digest();
    update(digest, "MIndexPrecomputePlan/v1");
    MIndexPrecomputeScope.putLong(digest, keys.length);
    for (int row = 0; row < keys.length; row++) {
      update(digest, keys[row].canonicalName());
      update(digest, passes[row].implementationRevision());
      int count = dependencyStart[row + 1] - dependencyStart[row];
      MIndexPrecomputeScope.putLong(digest, count);
      for (int at = dependencyStart[row]; at < dependencyStart[row + 1]; at++) {
        update(digest, keys[dependencyRows[at]].canonicalName());
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void update(MessageDigest digest, String value) {
    MIndexPrecomputeScope.putLong(digest, value.length());
    for (int index = 0; index < value.length(); index++) {
      char unit = value.charAt(index);
      digest.update((byte) (unit >>> 8));
      digest.update((byte) unit);
    }
  }
}
