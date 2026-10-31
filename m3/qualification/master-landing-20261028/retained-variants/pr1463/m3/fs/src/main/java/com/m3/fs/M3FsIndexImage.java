// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Frozen filesystem image containing exact token coordinates and packed UTF-16 values.
 * Token identity is supplied by the owning index. This codec neither invents IDs nor owns a
 * runtime string representation. Value views are materialized only when requested by the API.
 */
public final class M3FsIndexImage {
  public static final long MAGIC = 0x4d334d4944583031L;
  public static final int FORMAT = 1;
  public static final int HEADER_BYTES = 64;

  private final ByteBuffer image;
  private final int valueCount;
  private final int tokenCount;
  private final int utf16Count;

  private M3FsIndexImage(ByteBuffer image, int values, int tokens, int units) {
    this.image = image.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
    this.valueCount = values;
    this.tokenCount = tokens;
    this.utf16Count = units;
  }

  /** Creates text-only values, with empty token spans rather than invented index identities. */
  public static M3FsIndexImage of(CharSequence... values) {
    return of(Arrays.asList(Objects.requireNonNull(values, "values").clone()));
  }

  /** Creates text-only values, with empty token spans rather than invented index identities. */
  public static M3FsIndexImage of(List<? extends CharSequence> values) {
    Objects.requireNonNull(values, "values");
    return of(values, new int[Math.addExact(values.size(), 1)], new long[0]);
  }

  /** Admits an API list and explicit CSR token lanes; all caller data is copied into the image. */
  public static M3FsIndexImage of(
      List<? extends CharSequence> values, int[] tokenStarts, long[] tokenIds) {
    Objects.requireNonNull(values, "values");
    int count = values.size();
    int[] starts = new int[Math.addExact(count, 1)];
    for (int i = 0; i < count; i++) {
      CharSequence value = Objects.requireNonNull(values.get(i), "value");
      starts[i + 1] = Math.addExact(starts[i], value.length());
    }
    char[] units = new char[starts[count]];
    for (int i = 0; i < count; i++) {
      CharSequence value = Objects.requireNonNull(values.get(i), "value");
      if (value.length() != starts[i + 1] - starts[i]) {
        throw new IllegalArgumentException("value changed during image admission");
      }
      for (int j = 0; j < value.length(); j++) units[starts[i] + j] = value.charAt(j);
    }
    return of(tokenStarts, tokenIds, starts, units);
  }

  /** Admits exact primitive CSR lanes. Each value has an independent token and UTF-16 span. */
  public static M3FsIndexImage of(
      int[] tokenStarts, long[] tokenIds, int[] utf16Starts, char[] utf16Units) {
    int[] ts = Objects.requireNonNull(tokenStarts, "tokenStarts").clone();
    long[] ids = Objects.requireNonNull(tokenIds, "tokenIds").clone();
    int[] us = Objects.requireNonNull(utf16Starts, "utf16Starts").clone();
    char[] units = Objects.requireNonNull(utf16Units, "utf16Units").clone();
    if (ts.length < 1 || ts.length != us.length) {
      throw new IllegalArgumentException("value CSR geometry");
    }
    validateStarts(ts, ids.length);
    validateStarts(us, units.length);
    int count = ts.length - 1;
    long tokenStartOffset = HEADER_BYTES;
    long utf16StartOffset = tokenStartOffset + 4L * ts.length;
    long tokenOffset = utf16StartOffset + 4L * us.length;
    long utf16Offset = tokenOffset + 8L * ids.length;
    long hashOffset = utf16Offset + 2L * units.length;
    long signalOffset = hashOffset + 4L * count;
    long end = signalOffset + 8L * count;
    if (end > Integer.MAX_VALUE) throw new IllegalArgumentException("M3FS image exceeds 2 GiB");
    ByteBuffer out = ByteBuffer.allocateDirect((int) end).order(ByteOrder.BIG_ENDIAN);
    out.putLong(MAGIC).putInt(FORMAT).putInt(count).putInt(ids.length).putInt(units.length);
    out.putInt((int) tokenStartOffset).putInt((int) utf16StartOffset).putInt((int) tokenOffset);
    out.putInt((int) utf16Offset).putInt((int) hashOffset).putInt((int) signalOffset);
    out.putInt((int) end).putInt(0).putLong(0L);
    for (int start : ts) out.putInt(start);
    for (int start : us) out.putInt(start);
    for (long id : ids) out.putLong(id);
    for (char unit : units) out.putChar(unit);
    for (int i = 0; i < count; i++) {
      int hash = 0;
      for (int j = us[i]; j < us[i + 1]; j++) hash = 31 * hash + units[j];
      out.putInt(hash);
    }
    for (int i = 0; i < count; i++) {
      long signal = 0L;
      for (int j = us[i]; j < us[i + 1]; j++) {
        int c = units[j];
        if (c >= 'A' && c <= 'Z') c |= 0x20;
        signal |= 1L << (mix(c) & 63);
        signal |= 1L << (mix(c ^ 0x9e3779b9) & 63);
      }
      out.putLong(signal);
    }
    out.flip();
    return new M3FsIndexImage(out, count, ids.length, units.length);
  }

  private static void validateStarts(int[] starts, int length) {
    if (starts[0] != 0 || starts[starts.length - 1] != length) {
      throw new IllegalArgumentException("CSR terminal offset");
    }
    for (int i = 1; i < starts.length; i++) {
      if (starts[i] < starts[i - 1]) throw new IllegalArgumentException("CSR offset order");
    }
  }

  private static int mix(int x) {
    x ^= x >>> 16;
    x *= 0x7feb352d;
    x ^= x >>> 15;
    x *= 0x846ca68b;
    return x ^ (x >>> 16);
  }

  public int valueCount() { return valueCount; }
  public int tokenCount() { return tokenCount; }
  public int utf16Count() { return utf16Count; }

  /** Returns a read-only API view preserving every UTF-16 code unit, including lone surrogates. */
  public CharBuffer value(int ordinal) {
    Objects.checkIndex(ordinal, valueCount);
    int offsets = image.getInt(28);
    int start = image.getInt(offsets + ordinal * Integer.BYTES);
    int end = image.getInt(offsets + (ordinal + 1) * Integer.BYTES);
    int units = image.getInt(36);
    return image.slice(units + start * Character.BYTES, (end - start) * Character.BYTES)
        .order(ByteOrder.BIG_ENDIAN).asCharBuffer().asReadOnlyBuffer();
  }

  public ByteBuffer image() { return image.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN); }
  public long byteSize() { return image.capacity(); }
}
