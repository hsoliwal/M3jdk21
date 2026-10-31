// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/** Sorted packed SHA-256 key/value columns. */
final class M3IdPairs {
  final byte[] keys;
  final byte[] values;
  M3IdPairs(Map<M3ContentId, M3ContentId> input, int max) {
    Objects.requireNonNull(input, "precomputes");
    if (input.size() > max) throw new IllegalArgumentException("too many precomputes");
    byte[] k = new byte[Math.multiplyExact(input.size(), 32)], v = new byte[k.length]; int row = 0;
    for (Map.Entry<M3ContentId, M3ContentId> e : input.entrySet()) {
      System.arraycopy(Objects.requireNonNull(e.getKey()).bytes(), 0, k, row * 32, 32);
      System.arraycopy(Objects.requireNonNull(e.getValue()).bytes(), 0, v, row * 32, 32); row++;
    }
    int[] order = M3Columns.order(input.size(), (a, b) -> compare(k, a * 32, k, b * 32));
    keys = new byte[k.length]; values = new byte[v.length];
    for (int i = 0; i < order.length; i++) {
      System.arraycopy(k, order[i] * 32, keys, i * 32, 32);
      System.arraycopy(v, order[i] * 32, values, i * 32, 32);
    }
  }
  M3IdPairs(byte[] k, byte[] v) {
    if (k.length % 32 != 0 || k.length != v.length) throw new IllegalArgumentException("ID pair geometry");
    for (int i = 32; i < k.length; i += 32) if (compare(k, i - 32, k, i) >= 0) throw new IllegalArgumentException("unordered ID keys");
    keys = k; values = v;
  }
  static int compare(byte[] a, int ao, byte[] b, int bo) { return Arrays.compareUnsigned(a, ao, ao + 32, b, bo, bo + 32); }
  int index(byte[] key) {
    int lo = 0, hi = keys.length / 32 - 1;
    while (lo <= hi) { int mid = (lo + hi) >>> 1, c = compare(keys, mid * 32, key, 0);
      if (c < 0) lo = mid + 1; else if (c > 0) hi = mid - 1; else return mid; }
    return -lo - 1;
  }
  Map<M3ContentId, M3ContentId> view() {
    return M3Columns.view(keys.length / 32,
        i -> M3ContentId.of(Arrays.copyOfRange(keys, i * 32, i * 32 + 32)),
        i -> M3ContentId.of(Arrays.copyOfRange(values, i * 32, i * 32 + 32)),
        key -> key instanceof M3ContentId id ? index(id.bytes()) : -1);
  }
  M3IdPairs put(byte[] key, byte[] value) {
    if (key.length != 32 || value.length != 32) throw new IllegalArgumentException("ID pair geometry");
    int found = index(key), at = found >= 0 ? found : -found - 1, n = keys.length / 32 + (found < 0 ? 1 : 0);
    byte[] k = new byte[Math.multiplyExact(n, 32)], v = new byte[k.length];
    System.arraycopy(keys, 0, k, 0, at * 32); System.arraycopy(values, 0, v, 0, at * 32);
    System.arraycopy(key, 0, k, at * 32, 32); System.arraycopy(value, 0, v, at * 32, 32);
    int source = (at + (found >= 0 ? 1 : 0)) * 32;
    System.arraycopy(keys, source, k, (at + 1) * 32, keys.length - source);
    System.arraycopy(values, source, v, (at + 1) * 32, values.length - source);
    return new M3IdPairs(k, v);
  }
}
