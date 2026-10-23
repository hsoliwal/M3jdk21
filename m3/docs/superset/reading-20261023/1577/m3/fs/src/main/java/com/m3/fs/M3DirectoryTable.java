// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/** Immutable directory metadata columns. Names, stats and entry objects exist only at API access. */
public final class M3DirectoryTable {
  /** Kind flags address the original native 1=file, 2=directory, 3=symlink lane. */
  public static final int KIND_FILE = 1;
  public static final int KIND_DIRECTORY = 2;
  public static final int KIND_SYMLINK = 4;
  public static final int KIND_ALL = KIND_FILE | KIND_DIRECTORY | KIND_SYMLINK;
  private final char[] names;
  private final int[] offsets;
  private final int[] state;
  private final long[] scalar;

  private M3DirectoryTable(char[] names, int[] offsets, int[] state, long[] scalar) {
    this.names = names;
    this.offsets = offsets;
    this.state = state;
    this.scalar = scalar;
  }

  static M3DirectoryTable decode(ByteBuffer input) {
    ByteBuffer scan = Objects.requireNonNull(input, "input").slice().order(ByteOrder.LITTLE_ENDIAN);
    int count = 0;
    int nameCapacity = 0;
    while (scan.hasRemaining()) {
      if (scan.remaining() < 52) throw malformed("truncated row");
      int n = scan.getInt();
      if (n < 1 || n > 255 || scan.remaining() < n + 48) throw malformed("name length");
      nameCapacity = Math.addExact(nameCapacity, n);
      count = Math.addExact(count, 1);
      scan.position(scan.position() + n + 48);
    }
    char[] names = new char[nameCapacity];
    int[] offsets = new int[Math.addExact(count, 1)];
    int[] state = new int[Math.multiplyExact(count, 4)];
    long[] scalar = new long[Math.multiplyExact(count, 4)];
    ByteBuffer data = input.slice().order(ByteOrder.LITTLE_ENDIAN);
    int cursor = 0;
    for (int row = 0; row < count; row++) {
      offsets[row] = cursor;
      int bytes = data.getInt();
      int end = data.position() + bytes;
      while (data.position() < end) {
        int b = Byte.toUnsignedInt(data.get());
        int cp;
        if (b < 0x80) cp = b;
        else {
          int continuation;
          int minimum;
          if (b >= 0xc2 && b <= 0xdf) { cp = b & 31; continuation = 1; minimum = 0x80; }
          else if (b >= 0xe0 && b <= 0xef) { cp = b & 15; continuation = 2; minimum = 0x800; }
          else if (b >= 0xf0 && b <= 0xf4) { cp = b & 7; continuation = 3; minimum = 0x10000; }
          else throw malformed("invalid UTF-8 lead");
          if (end - data.position() < continuation) throw malformed("truncated UTF-8");
          for (int k = 0; k < continuation; k++) {
            int next = Byte.toUnsignedInt(data.get());
            if ((next & 0xc0) != 0x80) throw malformed("invalid UTF-8 continuation");
            cp = (cp << 6) | (next & 63);
          }
          if (cp < minimum || cp > 0x10ffff || (cp >= 0xd800 && cp <= 0xdfff)) {
            throw malformed("invalid UTF-8 scalar");
          }
        }
        if (cp == 0 || cp == '/') throw malformed("invalid name");
        if (cp <= 0xffff) names[cursor++] = (char) cp;
        else {
          names[cursor++] = Character.highSurrogate(cp);
          names[cursor++] = Character.lowSurrogate(cp);
        }
      }
      int lane = row * 4;
      for (int j = 0; j < 4; j++) state[lane + j] = data.getInt();
      for (int j = 0; j < 4; j++) scalar[lane + j] = data.getLong();
      if (state[lane] < 1 || state[lane] > 3 || state[lane + 1] < 0
          || state[lane + 2] < 0 || state[lane + 3] <= 0
          || scalar[lane] <= 0 || scalar[lane + 1] < 0
          || scalar[lane + 2] < 1 || scalar[lane + 3] < 0) throw malformed("stat columns");
    }
    offsets[count] = cursor;
    return new M3DirectoryTable(Arrays.copyOf(names, cursor), offsets, state, scalar);
  }

  /** Copies an API list without imposing the native codec's UTF-8 name limit on Java values. */
  static M3DirectoryTable fromEntries(List<M3FS.DirEntry> entries) {
    List<M3FS.DirEntry> checked = Objects.requireNonNull(entries, "entries");
    int count = checked.size();
    int[] offsets = new int[Math.addExact(count, 1)];
    for (int i = 0; i < count; i++) offsets[i + 1] =
        Math.addExact(offsets[i], Objects.requireNonNull(checked.get(i), "entry").name().length());
    char[] names = new char[offsets[count]];
    int[] state = new int[Math.multiplyExact(count, 4)];
    long[] scalar = new long[Math.multiplyExact(count, 4)];
    for (int i = 0; i < count; i++) {
      M3FS.DirEntry entry = Objects.requireNonNull(checked.get(i), "entry");
      if (entry.name().length() != offsets[i + 1] - offsets[i]) {
        throw new IllegalArgumentException("entries changed during admission");
      }
      entry.name().getChars(0, entry.name().length(), names, offsets[i]);
      M3FS.Stat stat = entry.stat();
      int lane = i * 4;
      state[lane] = stat.kind().ordinal() + 1;
      state[lane + 1] = stat.mode();
      state[lane + 2] = stat.linkCount();
      state[lane + 3] = stat.blockSize();
      scalar[lane] = stat.inode();
      scalar[lane + 1] = stat.size();
      scalar[lane + 2] = stat.generation();
      scalar[lane + 3] = stat.modifiedTimeNanos();
    }
    return new M3DirectoryTable(names, offsets, state, scalar);
  }

  private static IllegalStateException malformed(String reason) {
    return new IllegalStateException("invalid native M3FS directory " + reason);
  }

  private int lane(int row) { return Objects.checkIndex(row, size()) * 4; }
  public int size() { return offsets.length - 1; }
  public String name(int row) {
    Objects.checkIndex(row, size());
    return new String(names, offsets[row], offsets[row + 1] - offsets[row]);
  }
  public long inode(int row) { return scalar[lane(row)]; }
  public long size(int row) { return scalar[lane(row) + 1]; }
  public long fileSize(int row) { return size(row); }
  public long generation(int row) { return scalar[lane(row) + 2]; }
  public long modifiedTimeNanos(int row) { return scalar[lane(row) + 3]; }
  public M3FS.Kind kind(int row) { return M3FS.Kind.fromNative(state[lane(row)]); }
  public int mode(int row) { return state[lane(row) + 1]; }
  public int linkCount(int row) { return state[lane(row) + 2]; }
  public int blockSize(int row) { return state[lane(row) + 3]; }
  public M3FS.Stat stat(int row) {
    return new M3FS.Stat(inode(row), size(row), generation(row), modifiedTimeNanos(row),
        kind(row), mode(row), linkCount(row), blockSize(row));
  }
  public M3FS.DirEntry entry(int row) { return new M3FS.DirEntry(name(row), stat(row)); }
  public List<M3FS.DirEntry> entries() { return Collections.unmodifiableList(new Entries()); }
  private final class Entries extends AbstractList<M3FS.DirEntry> implements RandomAccess {
    @Override public int size() { return M3DirectoryTable.this.size(); }
    @Override public M3FS.DirEntry get(int index) { return entry(index); }
  }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
  /**
   * Select physical row ordinals without materializing DirEntry/Stat objects.
   * A row bit represents its existing native state-kind column, never its name.
   * Returned coordinates are detached and in the original directory encounter order.
   */
  public int[] selectedRows(int allowedKinds) {
    return selectedRows(0, size(), allowedKinds);
  }

  /** Half-open physical window selection with absolute returned row indexes. */
  public int[] selectedRows(int first, int length, int allowedKinds) {
    validateSelection(first, length, allowedKinds);
    int matches = 0;
    for (int row = first, end = first + length; row < end; row++) {
      if (kindAllowed(row, allowedKinds)) matches++;
    }
    int[] selected = new int[matches];
    int written = 0;
    for (int row = first, end = first + length; row < end; row++) {
      if (kindAllowed(row, allowedKinds)) selected[written++] = row;
    }
    return selected;
  }

  /** A detached bit mask of physical source row indexes, not a second payload owner. */
  public long[] selectedMaskWords(int allowedKinds) {
    return selectedMaskWords(0, size(), allowedKinds);
  }

  /** Keep the full parent coordinate space even for a bounded sub-window. */
  public long[] selectedMaskWords(int first, int length, int allowedKinds) {
    validateSelection(first, length, allowedKinds);
    long[] words = new long[(size() >>> 6) + ((size() & 63) == 0 ? 0 : 1)];
    for (int row = first, end = first + length; row < end; row++) {
      if (kindAllowed(row, allowedKinds)) words[row >>> 6] |= 1L << (row & 63);
    }
    return words;
  }

  /**
   * Stable primitive-index sort by original 64-bit file sizes. Comparator ties
   * retain physical directory order, including when descending is requested.
   * The source's char[], offsets and metadata lanes are never reordered/copied.
   */
  public int[] rowsBySize(int allowedKinds, boolean descending) {
    return rowsBySize(0, size(), allowedKinds, descending);
  }

  /** Stable sorted physical indexes drawn only from the selected physical window. */
  public int[] rowsBySize(int first, int length, int allowedKinds, boolean descending) {
    int[] selected = selectedRows(first, length, allowedKinds);
    int count = selected.length;
    if (count < 2) return selected;
    int[] scratch = new int[count];
    int[] input = selected;
    int[] output = scratch;
    for (long width = 1; width < count; width <<= 1) {
      for (long start = 0; start < count; start += width * 2) {
        int left = (int) start;
        int middle = (int) Math.min(count, start + width);
        int right = middle;
        int end = (int) Math.min(count, start + width * 2);
        int written = left;
        while (left < middle && right < end) {
          long leftSize = scalar[input[left] * 4 + 1];
          long rightSize = scalar[input[right] * 4 + 1];
          int compare = Long.compare(leftSize, rightSize);
          if (descending) compare = -compare;
          output[written++] = compare <= 0 ? input[left++] : input[right++];
        }
        while (left < middle) output[written++] = input[left++];
        while (right < end) output[written++] = input[right++];
      }
      int[] swap = input;
      input = output;
      output = swap;
    }
    if (input != selected) System.arraycopy(input, 0, selected, 0, count);
    return selected;
  }

  /**
   * Project already selected/sorted physical rows as a strict read-only List.
   * Clone and validate caller indexes before publication; duplicates are allowed.
   * Row objects materialize only on get(), iterator access or other public demand.
   */
  public List<M3FS.DirEntry> entriesByRows(int[] physicalRows) {
    int[] indexes = Objects.requireNonNull(physicalRows, "physicalRows").clone();
    for (int index : indexes) Objects.checkIndex(index, size());
    return ownedRowsView(indexes);
  }

  /** On-demand filtered entries; parent native columns remain the sole owner. */
  public List<M3FS.DirEntry> entriesOfKind(int allowedKinds) {
    return ownedRowsView(selectedRows(allowedKinds));
  }

  /** On-demand size-sorted entries, preserving original directory order on ties. */
  public List<M3FS.DirEntry> entriesBySize(int allowedKinds, boolean descending) {
    return ownedRowsView(rowsBySize(allowedKinds, descending));
  }

  private List<M3FS.DirEntry> ownedRowsView(int[] ownedIndexes) {
    return Collections.unmodifiableList(new IndexEntries(ownedIndexes));
  }

  private final class IndexEntries extends AbstractList<M3FS.DirEntry> implements RandomAccess {
    private final int[] indexes;
    private IndexEntries(int[] ownedIndexes) { this.indexes = ownedIndexes; }
    @Override public int size() { return indexes.length; }
    @Override public M3FS.DirEntry get(int index) {
      return entry(indexes[Objects.checkIndex(index, indexes.length)]);
    }
  }

  private void validateSelection(int first, int length, int allowedKinds) {
    Objects.checkFromIndexSize(first, length, size());
    if ((allowedKinds & ~KIND_ALL) != 0) {
      throw new IllegalArgumentException("unknown directory kind mask");
    }
  }

  private boolean kindAllowed(int row, int allowedKinds) {
    return (allowedKinds & (1 << (state[row * 4] - 1))) != 0;
=======
  /** Preserves the API entry-list hash using the canonical UTF-16 and stat columns. */
  int entriesHashCode() {
    int hash = 1;
    for (int row = 0; row < size(); row++) {
      int nameHash = 0;
      for (int i = offsets[row]; i < offsets[row + 1]; i++) {
        nameHash = 31 * nameHash + names[i];
      }
      int lane = row * 4;
      // Keep Stat's record component order, including the enum instance hash.
      int statHash = Long.hashCode(scalar[lane]);
      statHash = 31 * statHash + Long.hashCode(scalar[lane + 1]);
      statHash = 31 * statHash + Long.hashCode(scalar[lane + 2]);
      statHash = 31 * statHash + Long.hashCode(scalar[lane + 3]);
      statHash = 31 * statHash + M3FS.Kind.fromNative(state[lane]).hashCode();
      statHash = 31 * statHash + state[lane + 1];
      statHash = 31 * statHash + state[lane + 2];
      statHash = 31 * statHash + state[lane + 3];
      int entryHash = 31 * nameHash + statHash;
      hash = 31 * hash + entryHash;
    }
    return hash;
>>>>>>> 17507be40ecc1a30a46cb2c6134e99e5fe0aaa3e
  }

  @Override public boolean equals(Object other) {
    return this == other || other instanceof M3DirectoryTable table
        && Arrays.equals(names, table.names) && Arrays.equals(offsets, table.offsets)
        && Arrays.equals(state, table.state) && Arrays.equals(scalar, table.scalar);
  }

  @Override public int hashCode() {
    int hash = Arrays.hashCode(names);
    hash = 31 * hash + Arrays.hashCode(offsets);
    hash = 31 * hash + Arrays.hashCode(state);
    return 31 * hash + Arrays.hashCode(scalar);
  }
}
