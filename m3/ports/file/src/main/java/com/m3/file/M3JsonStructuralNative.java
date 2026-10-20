// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3JniLibraryLoader;
import com.m3.file.M3NativeLibrarySpec;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Optional JNI byte classifier for large JSON/text pages.
 *
 * <p>The output is one flag byte per input byte. It is candidate/precompute evidence only; callers
 * must not use it as proof that a JSON document is valid or that a structural byte is outside a
 * string. No native address or pointer is retained beyond the call.</p>
 */
public final class M3JsonStructuralNative {
  public enum Backend { JAVA, JNI }

  public static final int STRUCTURAL = 1;
  public static final int QUOTE = 1 << 1;
  public static final int BACKSLASH = 1 << 2;
  public static final int WHITESPACE = 1 << 3;
  public static final int NEWLINE = 1 << 4;
  public static final int HIGH_BIT = 1 << 5;
  public static final int CONTROL = 1 << 6;

  private static final M3NativeLibrarySpec LIBRARY =
      M3NativeLibrarySpec.fromProperties("m3.file.native", "m3file");
  private static volatile Boolean available;

  private M3JsonStructuralNative() {}

  public static boolean available() {
    if (!Boolean.parseBoolean(System.getProperty("m3.file.native", "true"))) return false;
    Boolean current = available;
    if (current != null) return current;
    synchronized (M3JsonStructuralNative.class) {
      current = available;
      if (current == null) {
        try {
          M3JniLibraryLoader.load(LIBRARY);
          current = nativeJsonSignalAbiVersion() == 1;
        } catch (RuntimeException | LinkageError unavailable) {
          current = Boolean.FALSE;
        }
        available = current;
      }
      return current;
    }
  }

  /**
   * Classifies {@code length} bytes without changing either buffer's position or limit.
   *
   * <p>JNI is selected only when both buffers are direct and ABI-compatible. Otherwise the exact
   * Java classifier is used.</p>
   */
  public static Backend classify(
      ByteBuffer source,
      int sourceOffset,
      int length,
      ByteBuffer target,
      int targetOffset) {
    range(source, sourceOffset, length, "source");
    range(target, targetOffset, length, "target");
    if (source == target && overlaps(sourceOffset, targetOffset, length)) {
      throw new IllegalArgumentException("JSON signal source and target ranges overlap");
    }
    // JNI direct addresses must obey the same writability boundary as ByteBuffer.put.
    if (length != 0 && target.isReadOnly()) {
      throw new java.nio.ReadOnlyBufferException();
    }

    if (source.isDirect() && target.isDirect() && available()) {
      try {
        int status =
            nativeClassifyJsonBytes(source, sourceOffset, length, target, targetOffset);
        if (status == 0) return Backend.JNI;
        if (status == -2) {
          throw new IllegalArgumentException("JSON signal source and target native ranges overlap");
        }
        throw new IllegalArgumentException("native JSON signal geometry rejected: " + status);
      } catch (UnsatisfiedLinkError incompatible) {
        available = Boolean.FALSE;
      }
    }

    classifyJava(source, sourceOffset, length, target, targetOffset);
    return Backend.JAVA;
  }

  static void classifyJava(
      ByteBuffer source,
      int sourceOffset,
      int length,
      ByteBuffer target,
      int targetOffset) {
    // Snapshot first so distinct ByteBuffer views over one heap/direct region cannot self-corrupt
    // while flags are written. Signal pages are bounded by their owner; public callers own their
    // requested allocation cost.
    byte[] input = new byte[length];
    for (int index = 0; index < length; index++) {
      input[index] = source.get(sourceOffset + index);
    }
    for (int index = 0; index < length; index++) {
      int value = Byte.toUnsignedInt(input[index]);
      int flags = 0;
      if (value == '{'
          || value == '}'
          || value == '['
          || value == ']'
          || value == ':'
          || value == ',') {
        flags |= STRUCTURAL;
      }
      if (value == '"') flags |= QUOTE;
      if (value == '\\') flags |= BACKSLASH;
      if (value == ' ' || value == '\t' || value == '\r' || value == '\n') {
        flags |= WHITESPACE;
      }
      if (value == '\n') flags |= NEWLINE;
      if ((value & 0x80) != 0) flags |= HIGH_BIT;
      if (value < 0x20) flags |= CONTROL;
      target.put(targetOffset + index, (byte) flags);
    }
  }

  private static void range(ByteBuffer buffer, int offset, int length, String label) {
    Objects.requireNonNull(buffer, label);
    if (offset < 0 || length < 0 || (long) offset + length > buffer.limit()) {
      throw new IndexOutOfBoundsException("invalid JSON signal " + label + " range");
    }
  }

  private static boolean overlaps(int left, int right, int length) {
    return length != 0 && left < right + (long) length && right < left + (long) length;
  }

  private static native int nativeJsonSignalAbiVersion();

  private static native int nativeClassifyJsonBytes(
      ByteBuffer source,
      int sourceOffset,
      int length,
      ByteBuffer target,
      int targetOffset);
}
