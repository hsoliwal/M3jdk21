// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3LongLookup;
import com.m3.file.M3FileResolver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Frozen resolver-token lexicon and global occurrence postings for one Universe snapshot. */
public final class M3FileUniverseTokenIndex {
  private final List<M3FileUniverseSnapshot.File> files;
  private final M3FileResolver resolver;
  private final int[] tokenStarts;
  private final long[] resolverTokenIds;
  private final int[] postingStarts;
  private final int[] globalTokenOrdinals;
  private final M3LongLookup.Geometry lookup;

  private M3FileUniverseTokenIndex(
      List<M3FileUniverseSnapshot.File> files,
      int[] tokenStarts,
      long[] resolverTokenIds,
      int[] postingStarts,
      int[] globalTokenOrdinals,
      M3FileResolver resolver) {
    this(files, tokenStarts, resolverTokenIds, postingStarts, globalTokenOrdinals, null, resolver);
  }

  private M3FileUniverseTokenIndex(
      List<M3FileUniverseSnapshot.File> files,
      int[] tokenStarts,
      long[] resolverTokenIds,
      int[] postingStarts,
      int[] globalTokenOrdinals,
      M3LongLookup.Geometry persistedLookup,
      M3FileResolver resolver) {
    this.files = List.copyOf(Objects.requireNonNull(files, "files"));
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.tokenStarts = Objects.requireNonNull(tokenStarts, "tokenStarts").clone();
    this.resolverTokenIds = Objects.requireNonNull(resolverTokenIds, "resolverTokenIds").clone();
    this.postingStarts = Objects.requireNonNull(postingStarts, "postingStarts").clone();
    this.globalTokenOrdinals =
        Objects.requireNonNull(globalTokenOrdinals, "globalTokenOrdinals").clone();
    if (this.tokenStarts.length != this.files.size() + 1
        || this.tokenStarts[0] != 0
        || this.tokenStarts[this.files.size()] < 0
        || this.postingStarts.length != this.resolverTokenIds.length + 1
        || this.postingStarts[0] != 0
        || this.postingStarts[this.resolverTokenIds.length] != this.globalTokenOrdinals.length) {
      throw new IllegalArgumentException("invalid Universe token index shape");
    }
    for (int ordinal = 1; ordinal < this.tokenStarts.length; ordinal++) {
      if (this.tokenStarts[ordinal] < this.tokenStarts[ordinal - 1]) {
        throw new IllegalArgumentException("Universe token boundaries are not ordered");
      }
    }
    for (int row = 1; row < this.postingStarts.length; row++) {
      if (this.postingStarts[row] < this.postingStarts[row - 1]) {
        throw new IllegalArgumentException("Universe token postings are not ordered");
      }
    }
    int totalTokens = this.tokenStarts[this.files.size()];
    for (int global : this.globalTokenOrdinals) {
      if (global < 0 || global >= totalTokens) {
        throw new IllegalArgumentException("Universe token posting is out of bounds");
      }
    }
    this.lookup =
        persistedLookup == null
            ? M3LongLookup.build(this.resolverTokenIds)
            : validateLookup(this.resolverTokenIds, persistedLookup);
  }

  static M3FileUniverseTokenIndex freeze(
      List<M3FileUniverseSnapshot.File> files, int[] tokenStarts, M3FileResolver resolver) {
    Objects.requireNonNull(files, "files");
    Objects.requireNonNull(resolver, "resolver");
    Objects.requireNonNull(tokenStarts, "tokenStarts");
    if (tokenStarts.length != files.size() + 1) {
      throw new IllegalArgumentException("Universe token boundary shape mismatch");
    }

    Map<Long, IntAccumulator> rows = new HashMap<>();
    for (int fileOrdinal = 0; fileOrdinal < files.size(); fileOrdinal++) {
      M3FileUniverseSnapshot.File file = files.get(fileOrdinal);
      M3FilePrimitiveSnapshot primitive = file.snapshot().primitiveSnapshot();
      M3FileLexiconSnapshot lexicon = file.snapshot().lexiconSnapshot();
      int globalStart = tokenStarts[fileOrdinal];
      int globalEnd = tokenStarts[fileOrdinal + 1];
      if (primitive.tokenCount() != globalEnd - globalStart) {
        throw new IllegalArgumentException("Universe token boundary does not match snapshot");
      }
      for (int localToken = 0; localToken < primitive.tokenCount(); localToken++) {
        int handle = primitive.localHandleAt(localToken);
        int resolverTokenCount = lexicon.tokenCount(handle);
        int globalToken = Math.addExact(globalStart, localToken);
        for (int token = 0; token < resolverTokenCount; token++) {
          long resolverTokenId = lexicon.tokenIdAt(handle, token);
          rows.computeIfAbsent(resolverTokenId, ignored -> new IntAccumulator())
              .addAscendingUnique(globalToken);
        }
      }
    }

    long[] keys = new long[rows.size()];
    int keyIndex = 0;
    for (long key : rows.keySet()) keys[keyIndex++] = key;
    Arrays.sort(keys);
    int postingCount = 0;
    for (long key : keys) postingCount = Math.addExact(postingCount, rows.get(key).size());

    int[] starts = new int[keys.length + 1];
    int[] postings = new int[postingCount];
    int cursor = 0;
    for (int row = 0; row < keys.length; row++) {
      starts[row] = cursor;
      IntAccumulator values = rows.get(keys[row]);
      values.copyTo(postings, cursor);
      cursor = Math.addExact(cursor, values.size());
    }
    starts[keys.length] = cursor;
    return new M3FileUniverseTokenIndex(files, tokenStarts, keys, starts, postings, resolver);
  }

  static M3FileUniverseTokenIndex restore(
      List<M3FileUniverseSnapshot.File> files,
      int[] tokenStarts,
      long[] resolverTokenIds,
      int[] postingStarts,
      int[] globalTokenOrdinals,
      M3LongLookup.Geometry lookup,
      M3FileResolver resolver) {
    return new M3FileUniverseTokenIndex(
        files, tokenStarts, resolverTokenIds, postingStarts, globalTokenOrdinals, lookup, resolver);
  }

  int fileCount() {
    return files.size();
  }

  M3FileResolver resolver() {
    return resolver;
  }

  M3FileSnapshot snapshotAt(int ordinal) {
    return files.get(Objects.checkIndex(ordinal, files.size())).snapshot();
  }

  int tokenBoundaryAt(int ordinal) {
    return tokenStarts[Objects.checkIndex(ordinal, tokenStarts.length)];
  }

  long resolverTokenIdAt(int row) {
    return resolverTokenIds[Objects.checkIndex(row, resolverTokenIds.length)];
  }

  M3LongLookup.Geometry lookupGeometry() {
    return lookup;
  }

  int postingBoundaryAt(int row) {
    return postingStarts[Objects.checkIndex(row, postingStarts.length)];
  }

  int globalTokenOrdinalAt(int posting) {
    return globalTokenOrdinals[Objects.checkIndex(posting, globalTokenOrdinals.length)];
  }

  public int uniqueResolverTokenCount() {
    return resolverTokenIds.length;
  }

  public int postingCount() {
    return globalTokenOrdinals.length;
  }

  public boolean contains(long resolverTokenId) {
    return rowOf(resolverTokenId) >= 0;
  }

  public int occurrenceCount(long resolverTokenId) {
    int row = rowOf(resolverTokenId);
    return row < 0 ? 0 : postingStarts[row + 1] - postingStarts[row];
  }

  /** Global token ordinals in ascending Universe order for one resolver atom. */
  public int[] globalTokenOrdinals(long resolverTokenId) {
    int row = requireRow(resolverTokenId);
    return Arrays.copyOfRange(globalTokenOrdinals, postingStarts[row], postingStarts[row + 1]);
  }

  /** Resolve one posting to its immutable file and UTF-16 coordinates. */
  public Occurrence occurrenceAt(long resolverTokenId, int postingOrdinal) {
    int row = requireRow(resolverTokenId);
    int start = postingStarts[row];
    int globalToken = globalTokenOrdinals[start + Objects.checkIndex(
        postingOrdinal, postingStarts[row + 1] - start)];
    int fileOrdinal = fileOrdinalFor(globalToken);
    int fileTokenOrdinal = globalToken - tokenStarts[fileOrdinal];
    M3FileSnapshot snapshot = files.get(fileOrdinal).snapshot();
    M3FilePrimitiveSnapshot primitive = snapshot.primitiveSnapshot();
    return new Occurrence(
        resolverTokenId,
        globalToken,
        fileOrdinal,
        fileTokenOrdinal,
        snapshot,
        primitive.utf16StartAt(fileTokenOrdinal),
        primitive.utf16EndAt(fileTokenOrdinal));
  }

  private int rowOf(long resolverTokenId) {
    return M3LongLookup.find(resolverTokenId, resolverTokenIds, lookup);
  }

  private static M3LongLookup.Geometry validateLookup(
      long[] keys, M3LongLookup.Geometry candidate) {
    if (keys.length == 0) {
      if (candidate.bucketMask() != 0 || candidate.slotMask() != 1) {
        throw new IllegalArgumentException("invalid empty Universe lookup geometry");
      }
      return candidate;
    }
    for (int row = 1; row < keys.length; row++) {
      if (keys[row] <= keys[row - 1]) {
        throw new IllegalArgumentException("Universe resolver token IDs are not strictly ordered");
      }
    }
    for (int row = 0; row < keys.length; row++) {
      if (M3LongLookup.find(keys[row], keys, candidate) != row) {
        throw new IllegalArgumentException("Universe lookup geometry does not resolve its key row");
      }
    }
    return candidate;
  }

  private int requireRow(long resolverTokenId) {
    int row = rowOf(resolverTokenId);
    if (row < 0) throw new IllegalArgumentException("resolver token is absent from Universe");
    return row;
  }

  private int fileOrdinalFor(int globalToken) {
    int low = 0;
    int high = files.size();
    while (low + 1 < high) {
      int middle = (low + high) >>> 1;
      if (tokenStarts[middle] <= globalToken) low = middle;
      else high = middle;
    }
    return low;
  }

  /** One immutable resolver-token occurrence in the Universe coordinate space. */
  public record Occurrence(
      long resolverTokenId,
      int globalTokenOrdinal,
      int fileOrdinal,
      int fileTokenOrdinal,
      M3FileSnapshot snapshot,
      int utf16Start,
      int utf16End) {
    public Occurrence {
      if (globalTokenOrdinal < 0 || fileOrdinal < 0 || fileTokenOrdinal < 0) {
        throw new IllegalArgumentException("negative Universe token coordinate");
      }
      snapshot = Objects.requireNonNull(snapshot, "snapshot");
      if (utf16Start < 0 || utf16End < utf16Start) {
        throw new IllegalArgumentException("invalid Universe token UTF-16 span");
      }
    }
  }

  private static final class IntAccumulator {
    private int[] values = new int[4];
    private int size;

    int size() {
      return size;
    }

    void addAscendingUnique(int value) {
      if (size != 0 && values[size - 1] == value) return;
      if (size == values.length) values = Arrays.copyOf(values, Math.multiplyExact(size, 2));
      values[size++] = value;
    }

    void copyTo(int[] destination, int offset) {
      System.arraycopy(values, 0, destination, offset, size);
    }
  }
}
