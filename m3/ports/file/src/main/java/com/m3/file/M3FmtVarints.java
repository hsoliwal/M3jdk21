// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

/** Unsigned canonical LEB128 ID lane. Native acceleration is opt-in; Java is the oracle. */
public final class M3FmtVarints {
  private static final boolean NATIVE_AVAILABLE;
  static {
    boolean loaded = false;
    if (Boolean.getBoolean("m3.file.fmt.native")) {
      try {
        System.loadLibrary("m3fmt_varints");
        loaded = true;
      } catch (UnsatisfiedLinkError unavailable) {
        if (Boolean.getBoolean("m3.file.fmt.native.required")) throw unavailable;
      }
    }
    NATIVE_AVAILABLE = loaded;
  }

  private M3FmtVarints() {}

  public static boolean nativeAvailable() { return NATIVE_AVAILABLE; }

  /** Pack every 64-bit ID bit pattern, treating the ID as unsigned. */
  public static byte[] pack(long[] ids) {
    Objects.requireNonNull(ids, "ids");
    if (NATIVE_AVAILABLE && ids.length >= 64) return packNative(ids.clone());
    ByteArrayOutputStream out = new ByteArrayOutputStream(ids.length);
    for (long value : ids) {
      while (Long.compareUnsigned(value, 0x80L) >= 0) {
        out.write(((int) value & 0x7f) | 0x80);
        value >>>= 7;
      }
      out.write((int) value);
    }
    return out.toByteArray();
  }

  /** Reject truncation, non-minimal encodings, overflow and surplus bytes. */
  public static long[] unpack(byte[] packed, int count) {
    Objects.requireNonNull(packed, "packed");
    if (count < 0 || count > packed.length) throw new IllegalArgumentException("invalid ID count");
    long[] out = new long[count];
    int offset = 0;
    for (int row = 0; row < count; row++) {
      long value = 0;
      boolean ended = false;
      for (int digit = 0; digit < 10; digit++) {
        if (offset >= packed.length) throw new IllegalArgumentException("truncated ID lane");
        int next = Byte.toUnsignedInt(packed[offset++]);
        if (digit == 9 && (next & 0xfe) != 0) {
          throw new IllegalArgumentException("unsigned varint overflow");
        }
        value |= (long) (next & 0x7f) << (digit * 7);
        if ((next & 0x80) == 0) {
          if (digit > 0 && (next & 0x7f) == 0) {
            throw new IllegalArgumentException("non-minimal unsigned varint");
          }
          ended = true;
          break;
        }
      }
      if (!ended) throw new IllegalArgumentException("oversized unsigned varint");
      out[row] = value;
    }
    if (offset != packed.length) throw new IllegalArgumentException("trailing ID bytes");
    return out;
  }

  private static native byte[] packNative(long[] ids);
}
