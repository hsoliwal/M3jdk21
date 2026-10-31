// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/** Primitive-ID construction buffer for M3FileText. */
public final class M3FileTextBuilder {
  private final M3FileResolver resolver;
  private long[] ids;
  private int size;

  public M3FileTextBuilder(M3FileResolver resolver) { this(resolver, 16); }

  public M3FileTextBuilder(M3FileResolver resolver, int expectedTokens) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    if (expectedTokens < 0) throw new IllegalArgumentException("expectedTokens");
    ids = new long[Math.max(1, expectedTokens)];
  }

  public M3FileTextBuilder append(M3FileText value) {
    Objects.requireNonNull(value, "value");
    if (value.resolver() != resolver) throw new IllegalArgumentException("resolver mismatch");
    int count = value.tokenCount();
    ensure(Math.addExact(size, count));
    for (int index = 0; index < count; index++) ids[size++] = value.tokenIdAt(index);
    return this;
  }

  public M3FileTextBuilder appendId(long id) {
    ensure(Math.addExact(size, 1));
    ids[size++] = id;
    return this;
  }

  /**
   * Interns the supplied value as one resolver entry.
   *
   * <p>Use {@link #appendTokenizedText(int, CharSequence)} for ordinary text ingestion.
   */
  public M3FileTextBuilder appendText(int languageId, CharSequence text) {
    return appendId(resolver.intern(languageId, Objects.requireNonNull(text, "text")));
  }

  /** Tokenizes exact text and appends each resolver coordinate without a temporary token list. */
  public M3FileTextBuilder appendTokenizedText(int languageId, CharSequence text) {
    M3FileTextTokenizer.forEachToken(
        Objects.requireNonNull(text, "text"),
        token -> appendId(resolver.intern(languageId, token)));
    return this;
  }

  public int tokenCount() { return size; }

  public M3FileTextBuilder clear() { size = 0; return this; }

  public M3FileText build() {
    return new M3FileText(resolver, Arrays.copyOf(ids, size));
  }

  private void ensure(int required) {
    if (required <= ids.length) return;
    ids = Arrays.copyOf(ids, Math.max(required, ids.length + (ids.length >>> 1) + 1));
  }
}
