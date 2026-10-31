// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

public final class M3FileId {
  public static final int MAX_LANGUAGE_ID = 0xFFFF;
  public static final long OVERLAY_BIT = 1L << 47;
  public static final long MAX_LOCAL_ID = OVERLAY_BIT - 1L;
  private static final long LOCAL_FIELD_MASK = (1L << 48) - 1L;

  private M3FileId() {}

  public static long base(int languageId, long localId) {
    validate(languageId, localId);
    return ((long) languageId << 48) | localId;
  }

  /**
   * Packs coordinates already proven valid by a frozen lexicon boundary.
   *
   * <p>Package-private by design: public callers must use {@link #base(int, long)}. This removes
   * duplicate range validation from the known-token inner loop after both namespace and local row
   * have already been checked by the owning lexicon.</p>
   */
  static long baseKnownValid(int languageId, int localId) {
    return ((long) languageId << 48) | Integer.toUnsignedLong(localId);
  }

  public static long overlay(int languageId, long localId) {
    validate(languageId, localId);
    return ((long) languageId << 48) | OVERLAY_BIT | localId;
  }

  public static int language(long id) {
    return (int) ((id >>> 48) & 0xFFFFL);
  }

  public static long local(long id) {
    return id & MAX_LOCAL_ID;
  }

  public static boolean isOverlay(long id) {
    return (id & OVERLAY_BIT) != 0L;
  }

  public static long localField(long id) {
    return id & LOCAL_FIELD_MASK;
  }

  private static void validate(int languageId, long localId) {
    if (languageId < 0 || languageId > MAX_LANGUAGE_ID) {
      throw new IllegalArgumentException("languageId out of range: " + languageId);
    }
    if (localId < 0 || localId > MAX_LOCAL_ID) {
      throw new IllegalArgumentException("localId out of range: " + localId);
    }
  }
}
