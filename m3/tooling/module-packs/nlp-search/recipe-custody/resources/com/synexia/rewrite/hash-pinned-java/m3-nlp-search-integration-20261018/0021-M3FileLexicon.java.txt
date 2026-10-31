// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import com.m3.file.M3FileText;
import com.m3.file.M3FileTextPool;
import java.util.Objects;

/**
 * File-local dictionary of unique M3FileText values.
 *
 * <p>The resolver remains the canonical text owner. This lexicon stores each unique token sequence
 * once and gives it a compact file-local integer handle used by M3File occurrences.</p>
 */
public final class M3FileLexicon {
  private final M3FileTextPool pool;
  private boolean frozen;

  public M3FileLexicon(M3FileResolver resolver) {
    pool = new M3FileTextPool(Objects.requireNonNull(resolver, "resolver"));
  }

  public M3FileResolver resolver() {
    return pool.resolver();
  }

  public int size() {
    return pool.size();
  }

  public int intern(M3FileText value) {
    if (frozen) throw new IllegalStateException("M3FileLexicon is frozen");
    Objects.requireNonNull(value, "value");
    if (value.resolver() != resolver()) {
      throw new IllegalArgumentException("M3FileText belongs to a different resolver");
    }
    return pool.internHandle(value);
  }

  /** Admits a scalar resolver token directly without a transient M3FileText wrapper. */
  public int intern(int languageId, CharSequence text) {
    if (frozen) throw new IllegalStateException("M3FileLexicon is frozen");
    return pool.internHandle(languageId, Objects.requireNonNull(text, "text"));
  }

  public int find(M3FileText value) {
    Objects.requireNonNull(value, "value");
    if (value.resolver() != resolver()) return -1;
    return pool.findHandle(value);
  }

  /** Looks up an already-admitted scalar token without growing the resolver or local lexicon. */
  public int find(int languageId, CharSequence text) {
    return pool.findHandle(languageId, Objects.requireNonNull(text, "text"));
  }

  public M3FileText valueAt(int localHandle) {
    return pool.valueAt(localHandle);
  }

  public int tokenCount(int localHandle) {
    return pool.tokenCount(localHandle);
  }

  public long tokenIdAt(int localHandle, int ordinal) {
    return pool.tokenIdAt(localHandle, ordinal);
  }

  /** Returns the resolver-native sequence for a local handle as a defensive copy. */
  long[] tokenIds(int localHandle) {
    int count = pool.tokenCount(localHandle);
    long[] ids = new long[count];
    for (int ordinal = 0; ordinal < count; ordinal++) {
      ids[ordinal] = pool.tokenIdAt(localHandle, ordinal);
    }
    return ids;
  }

  /** Returns one defensive flattened lexicon snapshot for JNI and structural serializers. */
  public M3FileLexiconSnapshot snapshot() {
    return new M3FileLexiconSnapshot(pool.tokenIdsSnapshot(), pool.tokenStartsSnapshot());
  }

  public void appendTo(int localHandle, StringBuilder out) {
    Objects.requireNonNull(out, "out");
    int count = pool.tokenCount(localHandle);
    for (int i = 0; i < count; i++) {
      resolver().appendTo(pool.tokenIdAt(localHandle, i), out);
    }
  }

  public boolean isFrozen() {
    return frozen;
  }

  void freeze() {
    frozen = true;
  }

  public long retainedPayloadBytes() {
    return pool.retainedPayloadBytes();
  }
}
