// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.attribute.FileTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable change description derived only from already-retained M3File facts.
 *
 * <p>No source is opened or rescanned. Metadata signals cheaply identify likely change while the
 * retained SHA-256 remains the exact content decision for downstream delta consumers.</p>
 */
public record M3FileDelta(
    Kind kind,
    Set<Signal> signals,
    M3Hash256 beforeSha256,
    M3Hash256 afterSha256,
    M3FileAttributes beforeAttributes,
    M3FileAttributes afterAttributes,
    int beforeTokenCount,
    int afterTokenCount) {

  public enum Kind {
    UNCHANGED,
    METADATA_ONLY,
    CONTENT_CHANGED,
    CREATED,
    DELETED
  }

  public enum Signal {
    PATH,
    SIZE,
    CREATION_TIME,
    MODIFIED_TIME,
    FILE_KEY,
    CONTENT_SHA256,
    TOKEN_COUNT,
    UNIQUE_TOKEN_COUNT
  }

  public M3FileDelta {
    kind = Objects.requireNonNull(kind, "kind");
    signals = Set.copyOf(Objects.requireNonNull(signals, "signals"));
    if (beforeTokenCount < 0 || afterTokenCount < 0) {
      throw new IllegalArgumentException("negative token count");
    }
  }

  public static M3FileDelta between(M3FileSnapshot before, M3FileSnapshot after) {
    return between(
        Objects.requireNonNull(before, "before").sourceFactsSnapshot(),
        Objects.requireNonNull(after, "after").sourceFactsSnapshot());
  }

  public static M3FileDelta between(
      M3FileSourceFactsSnapshot before, M3FileSourceFactsSnapshot after) {
    M3FileSourceFactsSnapshot left = Objects.requireNonNull(before, "before");
    M3FileSourceFactsSnapshot right = Objects.requireNonNull(after, "after");
    M3FileAttributes a = left.attributesAfterRead();
    M3FileAttributes b = right.attributesAfterRead();
    EnumSet<Signal> signals = metadataSignals(a, b);

    if (!left.contentSha256().equals(right.contentSha256())) signals.add(Signal.CONTENT_SHA256);
    if (left.tokenCount() != right.tokenCount()) signals.add(Signal.TOKEN_COUNT);
    if (left.uniqueTokenCount() != right.uniqueTokenCount()) signals.add(Signal.UNIQUE_TOKEN_COUNT);

    Kind kind =
        signals.contains(Signal.CONTENT_SHA256)
            ? Kind.CONTENT_CHANGED
            : signals.isEmpty() ? Kind.UNCHANGED : Kind.METADATA_ONLY;
    return new M3FileDelta(
        kind,
        signals,
        left.contentSha256(),
        right.contentSha256(),
        a,
        b,
        left.tokenCount(),
        right.tokenCount());
  }

  public static M3FileDelta created(M3FileSnapshot after) {
    M3FileSourceFactsSnapshot facts =
        Objects.requireNonNull(after, "after").sourceFactsSnapshot();
    return new M3FileDelta(
        Kind.CREATED,
        Set.of(Signal.CONTENT_SHA256),
        null,
        facts.contentSha256(),
        null,
        facts.attributesAfterRead(),
        0,
        facts.tokenCount());
  }

  public static M3FileDelta deleted(M3FileSnapshot before) {
    M3FileSourceFactsSnapshot facts =
        Objects.requireNonNull(before, "before").sourceFactsSnapshot();
    return new M3FileDelta(
        Kind.DELETED,
        Set.of(Signal.CONTENT_SHA256),
        facts.contentSha256(),
        null,
        facts.attributesAfterRead(),
        null,
        facts.tokenCount(),
        0);
  }

  public boolean contentChanged() {
    return kind == Kind.CONTENT_CHANGED || kind == Kind.CREATED || kind == Kind.DELETED;
  }

  private static EnumSet<Signal> metadataSignals(
      M3FileAttributes left, M3FileAttributes right) {
    EnumSet<Signal> signals = EnumSet.noneOf(Signal.class);
    if (!left.normalizedPath().equals(right.normalizedPath())) signals.add(Signal.PATH);
    if (left.size() != right.size()) signals.add(Signal.SIZE);
    if (!same(left.creationTime(), right.creationTime())) signals.add(Signal.CREATION_TIME);
    if (!same(left.lastModifiedTime(), right.lastModifiedTime())) signals.add(Signal.MODIFIED_TIME);
    if (!Objects.equals(left.fileKey(), right.fileKey())) signals.add(Signal.FILE_KEY);
    return signals;
  }

  private static boolean same(FileTime left, FileTime right) {
    return Objects.equals(left, right);
  }
}
