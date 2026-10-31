// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Caller-owned bounded LRU of conversion RESULT HANDLES over the existing canonical pool.
 * No converted payloads or source buffers are retained here. Standard charset singleton identity
 * is required for caching: a same-name custom/provider instance always bypasses the cache.
 * Misses retain the pool's existing JDK conversion semantics and allocation boundaries.
 * Closing this facade never closes the caller-owned pool. Operations are serialized per facade.
 */
public final class SharedArrayConversions implements AutoCloseable {
  private final SharedArrayPool pool;
  private final int maxEntries;
  private final LinkedHashMap<Key, ArrayHandle> retained = new LinkedHashMap<>(16, .75f, true);
  private long hits, misses, bypasses;
  private boolean closed;
  private record Key(ArrayHandle source, Charset charset, int targetLanguage, boolean encoding) {}
  /** Counters are requests: failures can increment misses/bypasses but are never cached. */
  public record Stats(int entries, long hits, long misses, long bypasses) {}

  public SharedArrayConversions(SharedArrayPool pool, int maxEntries) {
    this.pool = Objects.requireNonNull(pool, "pool");
    if (maxEntries < 0) throw new IllegalArgumentException("negative conversion cache entry budget");
    this.maxEntries = maxEntries;
  }

  public synchronized SharedBytes encode(SharedChars source, Charset charset) throws IOException {
    ensureOpen(); Objects.requireNonNull(source, "source"); Objects.requireNonNull(charset, "charset");
    if (maxEntries == 0 || !stableStandard(charset)) { bypasses++; return pool.encode(source, charset); }
    Key key = new Key(source.handle(), charset, 0, true);
    ArrayHandle hit = retained.get(key);
    if (hit != null) {
      SharedBytes result = pool.bytes(hit); // Enforce pool lifecycle on a cache hit too.
      hits++; return result;
    }
    misses++;
    SharedBytes result = pool.encode(source, charset);
    retain(key, result.handle()); return result;
  }

  public synchronized SharedChars decode(int languageId, SharedBytes source, Charset charset) throws IOException {
    ensureOpen(); Objects.requireNonNull(source, "source"); Objects.requireNonNull(charset, "charset");
    if (maxEntries == 0 || !stableStandard(charset)) { bypasses++; return pool.decode(languageId, source, charset); }
    Key key = new Key(source.handle(), charset, languageId, false);
    ArrayHandle hit = retained.get(key);
    if (hit != null) {
      SharedChars result = pool.chars(hit);
      hits++; return result;
    }
    misses++;
    SharedChars result = pool.decode(languageId, source, charset);
    retain(key, result.handle()); return result;
  }

  public synchronized Stats stats() { return new Stats(retained.size(), hits, misses, bypasses); }
  public synchronized void clear() { ensureOpen(); retained.clear(); }
  @Override public synchronized void close() { retained.clear(); closed = true; }
  private void ensureOpen() { if (closed) throw new IllegalStateException("conversion cache closed"); }
  private void retain(Key key, ArrayHandle result) {
    retained.put(key, result);
    while (retained.size() > maxEntries) retained.pollFirstEntry();
  }
  private static boolean stableStandard(Charset charset) {
    return charset == StandardCharsets.UTF_8 || charset == StandardCharsets.UTF_16
        || charset == StandardCharsets.UTF_16BE || charset == StandardCharsets.UTF_16LE
        || charset == StandardCharsets.ISO_8859_1 || charset == StandardCharsets.US_ASCII;
  }
}
