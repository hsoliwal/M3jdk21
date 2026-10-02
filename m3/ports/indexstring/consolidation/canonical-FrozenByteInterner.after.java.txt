// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Objects;

/** Caller-owned exact immutable byte interner; bounded retained payloads, no global pool. */
public final class FrozenByteInterner {
  private final HashMap<Key, FrozenBytes> unique = new HashMap<>();
  private final int maxEntries;
  private final long maxPayloadBytes;
  private long payloadBytes;

  FrozenByteInterner() { this(Integer.MAX_VALUE, Long.MAX_VALUE); }
  public FrozenByteInterner(int maxEntries, long maxPayloadBytes) {
    if (maxEntries < 0 || maxPayloadBytes < 0) throw new IllegalArgumentException("negative pool limit");
    this.maxEntries = maxEntries; this.maxPayloadBytes = maxPayloadBytes;
  }
  /** Input must not be modified during the call; modifications after return are safe. */
  public synchronized FrozenBytes intern(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    FrozenBytes hit = unique.get(new Key(bytes));
    return hit != null ? hit : retain(FrozenBytes.copyOf(bytes));
  }
  /** Reuses immutable full payloads; snapshots partial/mapped backing only on an admission miss. */
  public synchronized FrozenBytes internFrozen(FrozenBytes bytes) {
    Objects.requireNonNull(bytes, "bytes");
    FrozenBytes hit = unique.get(new Key(bytes));
    return hit != null ? hit : retain(bytes.compact());
  }
  public synchronized FrozenBytes intern(String text, Charset encoding) {
    return retain(FrozenBytes.owned(Objects.requireNonNull(text, "text")
        .getBytes(Objects.requireNonNull(encoding, "encoding"))));
  }
  public synchronized long payloadBytes() { return payloadBytes; }
  public synchronized int size() { return unique.size(); }
  /** Exact raw UTF16BE admission: String probes allocate backing only on a miss. */
  public synchronized FrozenBytes internUtf16(String text) {
    Key probe = new Key(Objects.requireNonNull(text, "text"));
    FrozenBytes hit = unique.get(probe);
    if (hit != null) return hit;
    byte[] bytes = new byte[probe.length()];
    for (int i = 0; i < bytes.length; i++) bytes[i] = probe.at(i);
    return retain(FrozenBytes.owned(bytes));
  }
  /** Drop lookup retention only. Independently owned live values stay valid. */
  public synchronized void clear() { unique.clear(); payloadBytes = 0; }
  private FrozenBytes retain(FrozenBytes candidate) {
    Key key = new Key(candidate);
    FrozenBytes hit = unique.get(key);
    if (hit != null) return hit;
    if (unique.size() >= maxEntries || candidate.length() > maxPayloadBytes - payloadBytes) return candidate;
    unique.put(key, candidate);
    payloadBytes += candidate.length();
    return candidate;
  }
  private static final class Key {
    final FrozenBytes frozen;
    final byte[] probe;
    final String utf16;
    final int hash;
    Key(FrozenBytes bytes) { frozen = bytes; probe = null; utf16 = null; hash = bytes.hash32(); }
    Key(byte[] bytes) { frozen = null; probe = bytes; utf16 = null; hash = FrozenBytes.hash(bytes); }
    Key(String text) {
      frozen = null; probe = null; utf16 = text;
      Math.multiplyExact(text.length(), 2);
      int h = 0x811c9dc5;
      for (int i = 0; i < text.length(); i++) {
        char unit = text.charAt(i);
        h = (h ^ (unit >>> 8)) * 0x01000193;
        h = (h ^ (unit & 255)) * 0x01000193;
      }
      hash = h;
    }
    int length() { return utf16 != null ? utf16.length() * 2 : frozen == null ? probe.length : frozen.length(); }
    byte at(int index) {
      if (utf16 != null) return (byte)(utf16.charAt(index >>> 1) >>> ((index & 1) == 0 ? 8 : 0));
      return frozen == null ? probe[index] : frozen.byteAt(index);
    }
    @Override public int hashCode() { return hash; }
    @Override public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof Key that) || hash != that.hash || length() != that.length()) return false;
      for (int i = 0; i < length(); i++) if (at(i) != that.at(i)) return false;
      return true;
    }
  }
}
