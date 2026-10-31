// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/** Primitive ordering and API projections shared by immutable metadata columns. */
final class M3Columns {
  private M3Columns() {}

  static byte[] digest(byte[] bytes) {
    try { return java.security.MessageDigest.getInstance("SHA-256").digest(bytes); }
    catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }

  static int[] order(int count, IntBinaryOperator comparison) {
    int[] rows = new int[count];
    int[] scratch = new int[count];
    for (int i = 0; i < count; i++) rows[i] = i;
    for (int width = 1; width < count; width = Math.multiplyExact(width, 2)) {
      for (int start = 0; start < count; start += width * 2) {
        int middle = Math.min(count, start + width), end = Math.min(count, middle + width);
        int a = start, b = middle, out = start;
        while (a < middle || b < end) {
          scratch[out++] = b == end || (a < middle && comparison.applyAsInt(rows[a], rows[b]) <= 0)
              ? rows[a++] : rows[b++];
        }
      }
      int[] swap = rows; rows = scratch; scratch = swap;
    }
    return rows;
  }

  static int[] order(String[] names) {
    return order(names.length, (a, b) -> names[a].compareTo(names[b]));
  }

  static byte[] readId(DataInputStream in) throws IOException {
    byte[] id = new byte[M3ContentId.BYTES]; in.readFully(id); return id;
  }

  static String readString(DataInputStream in, boolean empty) throws IOException {
    int size = in.readInt();
    if (size < (empty ? 0 : 1) || size > 1024 * 1024 || size > in.available()) {
      throw new IOException("invalid metadata string length");
    }
    byte[] bytes = in.readNBytes(size);
    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
  }

  static void writeString(DataOutputStream out, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
  }

  static int readCount(DataInputStream in, int max, int minimumBytes) throws IOException {
    int n = in.readInt();
    if (n < 0 || n > max || (long) n * minimumBytes > in.available()) {
      throw new IOException("invalid metadata row count");
    }
    return n;
  }

  /** Called only by public collection getters; row objects are created by API access. */
  static <K, V> Map<K, V> view(int size, IntFunction<K> keys, IntFunction<V> values,
      ToIntFunction<Object> find) {
    return Collections.unmodifiableMap(new AbstractMap<>() {
      @Override public int size() { return size; }
      @Override public boolean containsKey(Object key) { return find.applyAsInt(key) >= 0; }
      @Override public V get(Object key) {
        int row = find.applyAsInt(key); return row < 0 ? null : values.apply(row);
      }
      @Override public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
          @Override public int size() { return size; }
          @Override public Iterator<Entry<K, V>> iterator() {
            return new Iterator<>() {
              private int row;
              @Override public boolean hasNext() { return row < size; }
              @Override public Entry<K, V> next() {
                if (!hasNext()) throw new NoSuchElementException();
                int at = row++; return new SimpleImmutableEntry<>(keys.apply(at), values.apply(at));
              }
            };
          }
        };
      }
    });
  }
}
