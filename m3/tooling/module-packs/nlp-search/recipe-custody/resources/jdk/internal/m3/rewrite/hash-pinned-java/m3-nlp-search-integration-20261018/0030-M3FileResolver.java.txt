// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * File-local coordinate membrane over existing immutable {@link String} owners.
 * This interface owns no character storage. IDs use {@link M3FileId}'s packed
 * language/base/overlay format and are meaningful only in one resolver instance.
 * A resolver must never change the spelling attached to a published ID.
 * Ordinary String remains the public surface of the runtime's internal M3String.
 */
public interface M3FileResolver {
  OptionalLong lookup(int languageId, CharSequence term);
  long intern(int languageId, CharSequence term);
  String resolve(long id);

  default OptionalLong lookupPrepared(int languageId, CharSequence term,
      int javaHash, long bitSignal64) {
    return lookup(languageId, term);
  }

  default int languageId(long id) { return M3FileId.language(id); }
  default long coordinateEntryId(long id) { return M3FileId.local(id); }

  default void appendTo(long id, StringBuilder out) {
    Objects.requireNonNull(out, "out").append(resolve(id));
  }

  default int compareIds(long left, long right) { return Long.compareUnsigned(left, right); }

  /** Empty for live dictionaries. Only a sealed immutable generation may supply this proof. */
  default OptionalLong coordinateFingerprint(int languageId) { return OptionalLong.empty(); }

  default M3TextMetrics metrics(long id) { return M3TextMetrics.of(resolve(id)); }
  default M3TextFacts facts(long id) { return M3TextFacts.of(resolve(id)); }
}
