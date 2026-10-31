// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Sorted names and packed SHA-256 values; no retained content-ID row objects. */
final class M3IdTable {
  final String[] names;
  final byte[] ids;

  M3IdTable(Map<String, M3ContentId> source, UnaryOperator<String> normalize, int max) {
    Objects.requireNonNull(source, "entries");
    if (source.size() > max) throw new IllegalArgumentException("too many entries");
    String[] keys = new String[source.size()]; byte[] values = new byte[Math.multiplyExact(keys.length, 32)];
    int row = 0;
    for (Map.Entry<String, M3ContentId> entry : source.entrySet()) {
      keys[row] = normalize.apply(entry.getKey());
      System.arraycopy(Objects.requireNonNull(entry.getValue(), "content ID").bytes(), 0, values, row * 32, 32);
      row++;
    }
    int[] order = M3Columns.order(keys); String[] sorted = new String[keys.length]; byte[] packed = new byte[values.length];
    int count = 0;
    for (int at : order) {
      if (count == 0 || !sorted[count - 1].equals(keys[at])) sorted[count++] = keys[at];
      System.arraycopy(values, at * 32, packed, (count - 1) * 32, 32);
    }
    names = Arrays.copyOf(sorted, count); ids = Arrays.copyOf(packed, count * 32);
  }

  M3IdTable(String[] sorted, byte[] packed) {
    if (packed.length != Math.multiplyExact(sorted.length, 32)) throw new IllegalArgumentException("ID geometry");
    for (int i = 0; i < sorted.length; i++) {
      Objects.requireNonNull(sorted[i]);
      if (i != 0 && sorted[i - 1].compareTo(sorted[i]) >= 0) throw new IllegalArgumentException("unordered names");
    }
    names = sorted; ids = packed;
  }

  int index(String name) { return Arrays.binarySearch(names, name); }
  M3ContentId idAt(int row) { return M3ContentId.of(Arrays.copyOfRange(ids, row * 32, row * 32 + 32)); }
  Map<String, M3ContentId> view() {
    return M3Columns.view(names.length, i -> names[i], this::idAt, k -> k instanceof String s ? index(s) : -1);
  }
  M3IdTable put(String name, byte[] id) {
    if (id.length != 32) throw new IllegalArgumentException("ID geometry");
    int found = index(name), at = found >= 0 ? found : -found - 1;
    int n = names.length + (found < 0 ? 1 : 0);
    String[] nextNames = new String[n]; byte[] nextIds = new byte[Math.multiplyExact(n, 32)];
    System.arraycopy(names, 0, nextNames, 0, at); System.arraycopy(ids, 0, nextIds, 0, at * 32);
    nextNames[at] = name; System.arraycopy(id, 0, nextIds, at * 32, 32);
    int source = at + (found >= 0 ? 1 : 0);
    System.arraycopy(names, source, nextNames, at + 1, names.length - source);
    System.arraycopy(ids, source * 32, nextIds, (at + 1) * 32, (names.length - source) * 32);
    return new M3IdTable(nextNames, nextIds);
  }
  M3IdTable remove(int at) {
    String[] keys = new String[names.length - 1]; byte[] values = new byte[keys.length * 32];
    System.arraycopy(names, 0, keys, 0, at); System.arraycopy(names, at + 1, keys, at, keys.length - at);
    System.arraycopy(ids, 0, values, 0, at * 32); System.arraycopy(ids, (at + 1) * 32, values, at * 32, (keys.length - at) * 32);
    return new M3IdTable(keys, values);
  }
}
