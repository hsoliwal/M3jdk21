// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/** Names, content IDs, sizes and kind ordinals, with object projection only in API views. */
final class M3RefTable {
  final String[] names;
  final byte[] ids;
  final long[] sizes;
  final int[] kinds;
  private static final M3ObjectKind[] KINDS = M3ObjectKind.values();
  M3RefTable(Map<String, M3ObjectRef> input, int max) {
    Objects.requireNonNull(input, "references");
    if (input.size() > max) throw new IllegalArgumentException("too many references");
    names = input.keySet().toArray(String[]::new); Arrays.sort(names);
    ids = new byte[Math.multiplyExact(names.length, 32)]; sizes = new long[names.length]; kinds = new int[names.length];
    for (int i = 0; i < names.length; i++) {
      requireName(names[i]); M3ObjectRef ref = Objects.requireNonNull(input.get(names[i]), "reference");
      System.arraycopy(ref.id().bytes(), 0, ids, i * 32, 32); sizes[i] = ref.size(); kinds[i] = ref.kind().ordinal();
    }
  }
  private M3RefTable(String[] n, byte[] d, long[] s, int[] k) { names = n; ids = d; sizes = s; kinds = k; }
  static String requireName(String name) {
    Objects.requireNonNull(name, "name");
    if (name.isEmpty() || name.indexOf('\0') >= 0) throw new IllegalArgumentException("invalid reference name");
    return name;
  }
  M3ObjectRef ref(int row) { return new M3ObjectRef(M3ContentId.of(Arrays.copyOfRange(ids, row * 32, row * 32 + 32)), sizes[row], KINDS[kinds[row]]); }
  Map<String, M3ObjectRef> view() {
    return M3Columns.view(names.length, i -> names[i], this::ref, k -> k instanceof String s ? Arrays.binarySearch(names, s) : -1);
  }
  void write(DataOutputStream out) throws IOException {
    out.writeInt(names.length);
    for (int i = 0; i < names.length; i++) { M3Columns.writeString(out, names[i]); out.write(ids, i * 32, 32); out.writeLong(sizes[i]); out.writeInt(kinds[i]); }
  }
  static M3RefTable read(DataInputStream in, int max) throws IOException {
    int n = M3Columns.readCount(in, max, 49); String[] names = new String[n]; byte[] ids = new byte[n * 32]; long[] sizes = new long[n]; int[] kinds = new int[n];
    for (int i = 0; i < n; i++) {
      names[i] = requireName(M3Columns.readString(in, false)); in.readFully(ids, i * 32, 32); sizes[i] = in.readLong(); kinds[i] = in.readInt();
      if (sizes[i] < 0 || kinds[i] < 0 || kinds[i] >= KINDS.length || (i != 0 && names[i - 1].compareTo(names[i]) >= 0)) throw new IOException("invalid named reference");
    }
    return new M3RefTable(names, ids, sizes, kinds);
  }
  M3RefTable put(String name, M3ObjectRef ref) {
    requireName(name); Objects.requireNonNull(ref, "ref"); int found = Arrays.binarySearch(names, name), at = found < 0 ? -found - 1 : found;
    int n = names.length + (found < 0 ? 1 : 0), source = at + (found < 0 ? 0 : 1), tail = names.length - source;
    String[] nn = new String[n]; byte[] nd = new byte[Math.multiplyExact(n, 32)]; long[] ns = new long[n]; int[] nk = new int[n];
    System.arraycopy(names, 0, nn, 0, at); System.arraycopy(ids, 0, nd, 0, at * 32); System.arraycopy(sizes, 0, ns, 0, at); System.arraycopy(kinds, 0, nk, 0, at);
    nn[at] = name; System.arraycopy(ref.id().bytes(), 0, nd, at * 32, 32); ns[at] = ref.size(); nk[at] = ref.kind().ordinal();
    System.arraycopy(names, source, nn, at + 1, tail); System.arraycopy(ids, source * 32, nd, (at + 1) * 32, tail * 32); System.arraycopy(sizes, source, ns, at + 1, tail); System.arraycopy(kinds, source, nk, at + 1, tail);
    return new M3RefTable(nn, nd, ns, nk);
  }
}
