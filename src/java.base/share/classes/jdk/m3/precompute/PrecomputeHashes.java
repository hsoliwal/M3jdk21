// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/PrecomputeHashes.java blob 98f62383c93e; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
final class PrecomputeHashes {
  private PrecomputeHashes() {}

  static long mix64(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  static int slot(long hash, int mask) {
    return (int) hash & mask;
  }

  static String sha256(String domain, String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] domainBytes = domain.getBytes(StandardCharsets.UTF_8);
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(domainBytes.length).array());
      digest.update(domainBytes);
      for (String value : values) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  static String sha256(String domain, long[] longs, int[] ints) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(domain.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
      for (long value : longs) {
        buffer.clear();
        buffer.putLong(value);
        digest.update(buffer.array());
      }
      ByteBuffer ib = ByteBuffer.allocate(Integer.BYTES);
      for (int value : ints) {
        ib.clear();
        ib.putInt(value);
        digest.update(ib.array());
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }
}
