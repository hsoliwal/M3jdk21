// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
/** Mapped-file token search over borrowed buffers; native addresses never escape a call. */
public final class M3FileNative {
  private static final M3NativeLibrarySpec LIBRARY =
      M3NativeLibrarySpec.fromProperties("m3.file.native", "m3file");
  private static final int NATIVE_THRESHOLD = Integer.getInteger("m3.file.native.threshold", 256);
  private static volatile Boolean available;
  private M3FileNative() { }
  public static boolean available() {
    if (!Boolean.parseBoolean(System.getProperty("m3.file.native", "true"))) return false;
    Boolean value = available;
    if (value != null) return value;
    synchronized (M3FileNative.class) {
      value = available;
      if (value == null) {
        try { M3JniLibraryLoader.load(LIBRARY); value = Boolean.TRUE; }
        catch (RuntimeException | LinkageError failure) { value = Boolean.FALSE; }
        available = value;
      }
      return value;
    }
  }
  /**
   * KMP search directly over one big-endian mapped M3File local-handle lane.
   *
   * <p>Large direct buffers cross JNI once. Token offsets are absolute from the buffer base;
   * position is ignored and limit bounds the accessible lane. Java remains the semantic oracle
   * and fallback; native code never retains the mapped address after the call.</p>
   */
  public static int mappedFileTokenIndexOf(
      ByteBuffer data,
      int tokenBase,
      int tokenWidth,
      int tokenCount,
      int[] pattern,
      int fromToken) {
    ByteBuffer source = Objects.requireNonNull(data, "data");
    int[] needle = Objects.requireNonNull(pattern, "pattern");
    if (tokenBase < 0
        || tokenCount < 0
        || fromToken < 0
        || fromToken > tokenCount
        || (tokenWidth != 1 && tokenWidth != 2 && tokenWidth != 4)) {
      throw new IllegalArgumentException("invalid mapped M3File token geometry");
    }
    int laneBytes = Math.toIntExact(Math.multiplyExact((long) tokenCount, tokenWidth));
    Objects.checkFromIndexSize(tokenBase, laneBytes, source.limit());
    for (int handle : needle) {
      if (handle < 0) throw new IllegalArgumentException("negative local handle");
    }
    if (needle.length == 0) return fromToken;
    if (fromToken == tokenCount || needle.length > tokenCount - fromToken) return -1;

    if (source.isDirect() && tokenCount >= NATIVE_THRESHOLD && available()) {
      try {
        int result =
            nativeMappedFileTokenIndexOf(
                source, tokenBase, tokenWidth, tokenCount, needle, fromToken);
        if (result != -2) return result;
      } catch (UnsatisfiedLinkError incompatibleLibrary) {
        available = Boolean.FALSE;
      }
    }
    return javaMappedFileTokenIndexOf(
        source, tokenBase, tokenWidth, tokenCount, needle, fromToken);
  }

  private static int javaMappedFileTokenIndexOf(
      ByteBuffer source,
      int tokenBase,
      int tokenWidth,
      int tokenCount,
      int[] pattern,
      int fromToken) {
    ByteBuffer data = source.duplicate().order(ByteOrder.BIG_ENDIAN);
    int[] failure = new int[pattern.length];
    for (int i = 1, matched = 0; i < pattern.length; ) {
      if (pattern[i] == pattern[matched]) {
        failure[i++] = ++matched;
      } else if (matched > 0) {
        matched = failure[matched - 1];
      } else {
        i++;
      }
    }
    int matched = 0;
    for (int token = fromToken; token < tokenCount; token++) {
      int value = mappedFileHandleAt(data, tokenBase, tokenWidth, token);
      while (matched > 0 && value != pattern[matched]) matched = failure[matched - 1];
      if (value == pattern[matched]) matched++;
      if (matched == pattern.length) return token - pattern.length + 1;
    }
    return -1;
  }

  private static int mappedFileHandleAt(
      ByteBuffer data, int tokenBase, int tokenWidth, int tokenIndex) {
    int at = tokenBase + Math.multiplyExact(tokenIndex, tokenWidth);
    return switch (tokenWidth) {
      case 1 -> Byte.toUnsignedInt(data.get(at));
      case 2 -> Short.toUnsignedInt(data.getShort(at));
      case 4 -> data.getInt(at);
      default -> throw new IllegalArgumentException("tokenWidth");
    };
  }


  private static native int nativeMappedFileTokenIndexOf(
      ByteBuffer data, int tokenBase, int tokenWidth, int tokenCount,
      int[] pattern, int fromToken);
}
