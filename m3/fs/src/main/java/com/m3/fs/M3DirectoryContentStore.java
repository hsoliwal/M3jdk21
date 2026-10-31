// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

/**
 * Directory-backed content-addressed object store.
 *
 * <p>Publication is create-only. A digest hit is accepted only after size and exact-byte parity;
 * SHA-256 is the lookup identity, not permission to silently merge unequal bytes.</p>
 */
public final class M3DirectoryContentStore {
  private static final int COPY_BUFFER_BYTES = 128 * 1024;
  private static final String PRECOMPUTE_BUNDLE_MAGIC = "M3PRECOMPUTE1";

  /**
   * Backend-neutral immutable lanes worth retaining in the filesystem.
   *
   * <p>There is intentionally no GPU-resident/device-buffer role.</p>
   */
  public enum PrecomputeRole {
    TOKEN_IDS,
    TOKEN_ORDINALS,
    SORTED_KEYS,
    POSTING_OFFSETS,
    POSTING_ROWS,
    BITMAP_WORDS,
    NGRAM_KEYS,
    NGRAM_POSTING_OFFSETS,
    NGRAM_POSTING_ROWS,
    NGRAM_LOOKUP_DISPLACEMENTS,
    NGRAM_LOOKUP_SLOTS,
    UTF16_CORPUS,
    RECORD_LENGTHS,
    AUTOMATON_TRANSITIONS,
    AUTOMATON_ACCEPTS,
    CSR_OFFSETS,
    CSR_EDGES,
    REVERSE_CSR_OFFSETS,
    REVERSE_CSR_EDGES,
    TUPLE_ROWS,
    REVERSE_TUPLE_ROWS,
    INTERVAL_STARTS,
    INTERVAL_ENDS,
    WINDOW_STARTS,
    WINDOW_ENDS,
    PREFIX_SUM_I64,
    HASH64,
    SIMHASH64,
    MINHASH64,
    RANK_SELECT_BITS,
    RANK_SELECT_DIRECTORY,
    CHUNK_OFFSETS,
    CHUNK_LENGTHS,
    RECORD_OFFSETS,
    QUERY_TRIGRAM_IDS,
    QUERY_PROGRAM,
    AUTOMATON_START_STATE,
    AUTOMATON_ASCII_CLASSES,
    AUTOMATON_ALPHABET_CLASS_COUNT
  }

  /** Whether immutable precompute bytes derive from source content or a query. */
  public enum PrecomputeScope {
    SOURCE,
    QUERY
  }

  /** Durable source facts stay; query images are retained only when reuse justifies it. */
  public enum PrecomputeRetention {
    KEEP,
    KEEP_IF_REUSED
  }

  /**
   * Exact immutable lane sets consumed by higher-level Java/JNI/GPU execution plans.
   */
  public enum PrecomputeProfile {
    TOKEN_POSTINGS(
        PrecomputeRole.SORTED_KEYS,
        PrecomputeRole.POSTING_OFFSETS,
        PrecomputeRole.POSTING_ROWS,
        PrecomputeRole.TOKEN_ORDINALS),
    NGRAM_CANDIDATE(
        PrecomputeRole.NGRAM_KEYS,
        PrecomputeRole.NGRAM_POSTING_OFFSETS,
        PrecomputeRole.NGRAM_POSTING_ROWS,
        PrecomputeRole.NGRAM_LOOKUP_DISPLACEMENTS,
        PrecomputeRole.NGRAM_LOOKUP_SLOTS),
    UTF16_SCAN(
        PrecomputeRole.UTF16_CORPUS,
        PrecomputeRole.RECORD_OFFSETS,
        PrecomputeRole.RECORD_LENGTHS),
    GPU_GREP_CANDIDATE_SCAN(
        PrecomputeRole.NGRAM_KEYS,
        PrecomputeRole.NGRAM_POSTING_OFFSETS,
        PrecomputeRole.NGRAM_POSTING_ROWS,
        PrecomputeRole.NGRAM_LOOKUP_DISPLACEMENTS,
        PrecomputeRole.NGRAM_LOOKUP_SLOTS,
        PrecomputeRole.UTF16_CORPUS,
        PrecomputeRole.RECORD_OFFSETS,
        PrecomputeRole.RECORD_LENGTHS),
    GRAPH_FORWARD(
        PrecomputeRole.CSR_OFFSETS,
        PrecomputeRole.CSR_EDGES),
    GRAPH_BIDIRECTIONAL(
        PrecomputeRole.CSR_OFFSETS,
        PrecomputeRole.CSR_EDGES,
        PrecomputeRole.REVERSE_CSR_OFFSETS,
        PrecomputeRole.REVERSE_CSR_EDGES),
    TUPLE_FORWARD(PrecomputeRole.TUPLE_ROWS),
    TUPLE_BIDIRECTIONAL(
        PrecomputeRole.TUPLE_ROWS,
        PrecomputeRole.REVERSE_TUPLE_ROWS),
    BITMAP(PrecomputeRole.BITMAP_WORDS),
    BITMAP_RANK_SELECT(
        PrecomputeRole.BITMAP_WORDS,
        PrecomputeRole.RANK_SELECT_BITS,
        PrecomputeRole.RANK_SELECT_DIRECTORY),
    INTERVAL(
        PrecomputeRole.INTERVAL_STARTS,
        PrecomputeRole.INTERVAL_ENDS),
    WINDOW_SUM(
        PrecomputeRole.WINDOW_STARTS,
        PrecomputeRole.WINDOW_ENDS,
        PrecomputeRole.PREFIX_SUM_I64),
    HASH_BATCH(PrecomputeRole.HASH64),
    SIMILARITY(
        PrecomputeRole.SIMHASH64,
        PrecomputeRole.MINHASH64),
    CHUNKED_SOURCE(
        PrecomputeRole.CHUNK_OFFSETS,
        PrecomputeRole.CHUNK_LENGTHS,
        PrecomputeRole.RECORD_OFFSETS,
        PrecomputeRole.RECORD_LENGTHS),
    TRIGRAM_QUERY(
        PrecomputeScope.QUERY,
        PrecomputeRetention.KEEP_IF_REUSED,
        PrecomputeRole.QUERY_TRIGRAM_IDS,
        PrecomputeRole.QUERY_PROGRAM),
    DFA_QUERY(
        PrecomputeScope.QUERY,
        PrecomputeRetention.KEEP_IF_REUSED,
        PrecomputeRole.AUTOMATON_START_STATE,
        PrecomputeRole.AUTOMATON_TRANSITIONS,
        PrecomputeRole.AUTOMATON_ACCEPTS),
    DFA_COMPACT_QUERY(
        PrecomputeScope.QUERY,
        PrecomputeRetention.KEEP_IF_REUSED,
        PrecomputeRole.AUTOMATON_START_STATE,
        PrecomputeRole.AUTOMATON_ALPHABET_CLASS_COUNT,
        PrecomputeRole.AUTOMATON_ASCII_CLASSES,
        PrecomputeRole.AUTOMATON_TRANSITIONS,
        PrecomputeRole.AUTOMATON_ACCEPTS);

    private final PrecomputeScope scope;
    private final PrecomputeRetention retention;
    private final long[] roleWords;

    PrecomputeProfile(PrecomputeRole first, PrecomputeRole... rest) {
      this(PrecomputeScope.SOURCE, PrecomputeRetention.KEEP, first, rest);
    }

    PrecomputeProfile(
        PrecomputeScope scope,
        PrecomputeRetention retention,
        PrecomputeRole first,
        PrecomputeRole... rest) {
      this.scope = Objects.requireNonNull(scope, "scope");
      this.retention = Objects.requireNonNull(retention, "retention");
      long mask = 1L << Objects.requireNonNull(first, "first").ordinal();
      for (PrecomputeRole role : rest) mask |= 1L << Objects.requireNonNull(role, "role").ordinal();
      roleWords = new long[] {mask};
    }

    public PrecomputeScope scope() {
      return scope;
    }

    public PrecomputeRetention retention() {
      return retention;
    }

    public boolean fileManifestEligible() {
      return scope == PrecomputeScope.SOURCE;
    }

    public Set<PrecomputeRole> roles() {
      EnumSet<PrecomputeRole> result = EnumSet.noneOf(PrecomputeRole.class);
      long bits = roleWords[0];
      while (bits != 0) { result.add(PrecomputeRole.values()[Long.numberOfTrailingZeros(bits)]); bits &= bits - 1; }
      return Set.copyOf(result);
    }
  }

  public enum LaneEncoding {
    BYTE(1),
    UTF16_BE(Character.BYTES),
    UTF16_LE(Character.BYTES),
    INT32_BE(Integer.BYTES),
    INT32_LE(Integer.BYTES),
    INT64_BE(Long.BYTES),
    INT64_LE(Long.BYTES),
    INT16_BE(Short.BYTES),
    INT16_LE(Short.BYTES);

    private final int width;

    LaneEncoding(int width) {
      this.width = width;
    }

    public int width() {
      return width;
    }
  }

  public record PrecomputeLane(
      PrecomputeRole role,
      LaneEncoding encoding,
      long elementCount,
      M3ObjectRef object) {
    public PrecomputeLane {
      role = Objects.requireNonNull(role, "role");
      encoding = Objects.requireNonNull(encoding, "encoding");
      object = Objects.requireNonNull(object, "object");
      if (elementCount < 0) {
        throw new IllegalArgumentException("negative element count");
      }
      if (object.kind() != M3ObjectKind.PRECOMPUTE_OUTPUT
          && object.kind() != M3ObjectKind.MINDEX_FILE_IMAGE
          && object.kind() != M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE) {
        throw new IllegalArgumentException("invalid precompute lane object kind");
      }
      long expected = Math.multiplyExact(elementCount, encoding.width());
      if (expected != object.size()) {
        throw new IllegalArgumentException("precompute lane byte geometry mismatch");
      }
    }
  }

  private static final class PrecomputeLaneTable extends M3FsPrecomputeBundle.ArrayListView<PrecomputeLane> {
    private static final PrecomputeRole[] ROLES = PrecomputeRole.values();
    private static final LaneEncoding[] ENCODINGS = LaneEncoding.values();
    private static final M3ObjectKind[] KINDS = M3ObjectKind.values();
    private final byte[] roles;
    private final byte[] encodings;
    private final byte[] kinds;
    private final long[] elements;
    private final long[] sizes;
    private final byte[] ids;
    private final long roleMask;

    private PrecomputeLaneTable(byte[] roles, byte[] encodings, byte[] kinds,
        long[] elements, long[] sizes, byte[] ids) {
      int count = roles.length;
      if (count < 1 || count > ROLES.length || encodings.length != count || kinds.length != count
          || elements.length != count || sizes.length != count || ids.length != count * M3ContentId.BYTES) {
        throw new IllegalArgumentException("precompute lane table geometry");
      }
      long mask = 0;
      for (int i = 0; i < count; i++) {
        int role = Byte.toUnsignedInt(roles[i]), encoding = Byte.toUnsignedInt(encodings[i]);
        int kind = Byte.toUnsignedInt(kinds[i]);
        if (role >= ROLES.length || encoding >= ENCODINGS.length || kind >= KINDS.length
            || (i > 0 && Byte.toUnsignedInt(roles[i - 1]) >= role) || elements[i] < 0
            || Math.multiplyExact(elements[i], ENCODINGS[encoding].width()) != sizes[i]
            || (KINDS[kind] != M3ObjectKind.PRECOMPUTE_OUTPUT && KINDS[kind] != M3ObjectKind.MINDEX_FILE_IMAGE
                && KINDS[kind] != M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE)) {
          throw new IllegalArgumentException("invalid precompute lane geometry/order");
        }
        mask |= 1L << role;
      }
      this.roles = roles; this.encodings = encodings; this.kinds = kinds;
      this.elements = elements; this.sizes = sizes; this.ids = ids; this.roleMask = mask;
    }
    static PrecomputeLaneTable from(List<PrecomputeLane> supplied) {
      Objects.requireNonNull(supplied, "lanes");
      if (supplied instanceof PrecomputeLaneTable columns) return columns;
      int count = supplied.size();
      if (count < 1 || count > ROLES.length) throw new IllegalArgumentException("precompute lane count");
      int[] order = new int[count];
      for (int i = 0; i < count; i++) {
        Objects.requireNonNull(supplied.get(i), "lane");
        int at = i;
        while (at > 0 && supplied.get(order[at - 1]).role().ordinal() > supplied.get(i).role().ordinal()) {
          order[at] = order[at - 1]; at--;
        }
        order[at] = i;
      }
      byte[] roles = new byte[count], encodings = new byte[count], kinds = new byte[count];
      long[] elements = new long[count], sizes = new long[count]; byte[] ids = new byte[count * M3ContentId.BYTES];
      for (int i = 0; i < count; i++) {
        PrecomputeLane lane = supplied.get(order[i]);
        roles[i] = (byte) lane.role().ordinal(); encodings[i] = (byte) lane.encoding().ordinal();
        kinds[i] = (byte) lane.object().kind().ordinal(); elements[i] = lane.elementCount(); sizes[i] = lane.object().size();
        System.arraycopy(lane.object().id().bytes(), 0, ids, i * M3ContentId.BYTES, M3ContentId.BYTES);
      }
      return new PrecomputeLaneTable(roles, encodings, kinds, elements, sizes, ids);
    }
    @Override public int size() { return roles.length; }
    @Override public PrecomputeLane get(int index) {
      Objects.checkIndex(index, size());
      return new PrecomputeLane(ROLES[Byte.toUnsignedInt(roles[index])], ENCODINGS[Byte.toUnsignedInt(encodings[index])],
          elements[index], new M3ObjectRef(M3ContentId.of(java.util.Arrays.copyOfRange(ids,
              index * M3ContentId.BYTES, (index + 1) * M3ContentId.BYTES)), sizes[index], KINDS[Byte.toUnsignedInt(kinds[index])]));
    }
    int indexOf(PrecomputeRole role) {
      int wanted = Objects.requireNonNull(role, "role").ordinal();
      for (int i = 0; i < roles.length; i++) if (Byte.toUnsignedInt(roles[i]) == wanted) return i;
      throw new IllegalArgumentException("missing precompute role " + role);
    }
  }

  public record PrecomputeBundle(String sourceRoot, String configRoot, List<PrecomputeLane> lanes) {
    public PrecomputeBundle {
      sourceRoot = requireSha256(sourceRoot, "sourceRoot");
      configRoot = requireSha256(configRoot, "configRoot");
      lanes = PrecomputeLaneTable.from(lanes);
    }
    public boolean supports(PrecomputeProfile profile) {
      long required = Objects.requireNonNull(profile, "profile").roleWords[0];
      return (((PrecomputeLaneTable) lanes).roleMask & required) == required;
    }
    public List<PrecomputeRole> missing(PrecomputeProfile profile) {
      long absent = Objects.requireNonNull(profile, "profile").roleWords[0] & ~((PrecomputeLaneTable) lanes).roleMask;
      int[] roles = new int[Long.bitCount(absent)]; int at = 0;
      while (absent != 0) { roles[at++] = Long.numberOfTrailingZeros(absent); absent &= absent - 1; }
      return M3FsPrecomputeBundle.apiList(roles.length, index -> PrecomputeLaneTable.ROLES[roles[index]]);
    }
    public void requireProfile(PrecomputeProfile profile) {
      if (!supports(profile)) throw new IllegalArgumentException("precompute profile missing lanes " + missing(profile));
    }
    public PrecomputeLane require(PrecomputeRole role) {
      PrecomputeLaneTable table = (PrecomputeLaneTable) lanes; return table.get(table.indexOf(role));
    }
  }

  private final Path root;
  private final Path objects;
  private final Path staging;

  public M3DirectoryContentStore(Path root) throws IOException {
    this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    this.objects = this.root.resolve("objects");
    this.staging = this.root.resolve("staging");
    rejectSymlink(this.root);
    rejectSymlink(objects);
    rejectSymlink(staging);
    durableCreateDirs(objects);
    durableCreateDirs(staging);
    rejectSymlink(this.root);
    rejectSymlink(objects);
    rejectSymlink(staging);
  }

  public Path root() {
    return root;
  }

  public M3ObjectRef putIntLane(int[] values, LaneEncoding encoding)
      throws IOException {
    int[] checked = Objects.requireNonNull(values, "values");
    return putIntLane(checked.length, encoding, index -> checked[index]);
  }

  public M3ObjectRef putIntLane(
      int elementCount,
      LaneEncoding encoding,
      java.util.function.IntUnaryOperator valueAt)
      throws IOException {
    LaneEncoding checkedEncoding = Objects.requireNonNull(encoding, "encoding");
    if (elementCount < 0) {
      throw new IllegalArgumentException("negative int lane element count");
    }
    if (checkedEncoding != LaneEncoding.INT32_BE
        && checkedEncoding != LaneEncoding.INT32_LE) {
      throw new IllegalArgumentException("int lane encoding required");
    }
    java.util.function.IntUnaryOperator checkedValueAt =
        Objects.requireNonNull(valueAt, "valueAt");
    return put(
        new PrimitiveLaneInputStream(
            elementCount,
            Integer.BYTES,
            checkedEncoding == LaneEncoding.INT32_BE,
            index -> Integer.toUnsignedLong(checkedValueAt.applyAsInt(index))),
        M3ObjectKind.PRECOMPUTE_OUTPUT);
  }

  public M3ObjectRef putUtf16Lane(
      int elementCount,
      LaneEncoding encoding,
      java.util.function.IntUnaryOperator charAt)
      throws IOException {
    LaneEncoding checkedEncoding = Objects.requireNonNull(encoding, "encoding");
    if (elementCount < 0) {
      throw new IllegalArgumentException("negative UTF-16 lane element count");
    }
    if (checkedEncoding != LaneEncoding.UTF16_BE
        && checkedEncoding != LaneEncoding.UTF16_LE) {
      throw new IllegalArgumentException("UTF-16 lane encoding required");
    }
    java.util.function.IntUnaryOperator checkedCharAt =
        Objects.requireNonNull(charAt, "charAt");
    return put(
        new PrimitiveLaneInputStream(
            elementCount,
            Character.BYTES,
            checkedEncoding == LaneEncoding.UTF16_BE,
            index -> {
              int value = checkedCharAt.applyAsInt(index);
              if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
                throw new IllegalArgumentException("UTF-16 code unit out of range");
              }
              return value;
            }),
        M3ObjectKind.PRECOMPUTE_OUTPUT);
  }

  public M3ObjectRef putShortLane(short[] values, LaneEncoding encoding)
      throws IOException {
    short[] checked = Objects.requireNonNull(values, "values");
    return putShortLane(
        checked.length,
        encoding,
        index -> Short.toUnsignedInt(checked[index]));
  }

  public M3ObjectRef putShortLane(
      int elementCount,
      LaneEncoding encoding,
      java.util.function.IntUnaryOperator valueAt)
      throws IOException {
    LaneEncoding checkedEncoding =
        Objects.requireNonNull(encoding, "encoding");
    if (elementCount < 0) {
      throw new IllegalArgumentException("negative short lane element count");
    }
    if (checkedEncoding != LaneEncoding.INT16_BE
        && checkedEncoding != LaneEncoding.INT16_LE) {
      throw new IllegalArgumentException("short lane encoding required");
    }
    java.util.function.IntUnaryOperator checkedValueAt =
        Objects.requireNonNull(valueAt, "valueAt");
    return put(
        new PrimitiveLaneInputStream(
            elementCount,
            Short.BYTES,
            checkedEncoding == LaneEncoding.INT16_BE,
            index -> checkedValueAt.applyAsInt(index) & 0xffffL),
        M3ObjectKind.PRECOMPUTE_OUTPUT);
  }

  public M3ObjectRef putLongLane(long[] values, LaneEncoding encoding)
      throws IOException {
    long[] checked = Objects.requireNonNull(values, "values");
    return putLongLane(checked.length, encoding, index -> checked[index]);
  }

  public M3ObjectRef putLongLane(
      int elementCount,
      LaneEncoding encoding,
      java.util.function.IntToLongFunction valueAt)
      throws IOException {
    LaneEncoding checkedEncoding = Objects.requireNonNull(encoding, "encoding");
    if (elementCount < 0) {
      throw new IllegalArgumentException("negative long lane element count");
    }
    if (checkedEncoding != LaneEncoding.INT64_BE
        && checkedEncoding != LaneEncoding.INT64_LE) {
      throw new IllegalArgumentException("long lane encoding required");
    }
    return put(
        new PrimitiveLaneInputStream(
            elementCount,
            Long.BYTES,
            checkedEncoding == LaneEncoding.INT64_BE,
            Objects.requireNonNull(valueAt, "valueAt")),
        M3ObjectKind.PRECOMPUTE_OUTPUT);
  }

  public M3ObjectRef putPrecomputeBundle(PrecomputeBundle bundle)
      throws IOException {
    byte[] encoded =
        encodePrecomputeBundle(Objects.requireNonNull(bundle, "bundle"))
            .getBytes(StandardCharsets.UTF_8);
    return put(encoded, M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR);
  }

  public PrecomputeBundle readPrecomputeBundle(
      M3ObjectRef descriptor, int maxBytes) throws IOException {
    M3ObjectRef checked = Objects.requireNonNull(descriptor, "descriptor");
    if (checked.kind() != M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR) {
      throw new IllegalArgumentException("precompute descriptor object kind required");
    }
    String encoded;
    try {
      encoded = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
          .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
          .decode(java.nio.ByteBuffer.wrap(readSmall(checked, maxBytes))).toString();
    } catch (java.nio.charset.CharacterCodingException invalid) {
      throw new IOException("invalid UTF-8 precompute bundle", invalid);
    }
    PrecomputeBundle parsed = parsePrecomputeBundle(encoded);
    if (!encodePrecomputeBundle(parsed).equals(encoded)) {
      throw new IOException("non-canonical precompute bundle descriptor");
    }
    return parsed;
  }

  public M3MappedObject openPrecomputeLane(
      PrecomputeBundle bundle, PrecomputeRole role, int maxBytes)
      throws IOException {
    PrecomputeLaneTable table = (PrecomputeLaneTable) Objects.requireNonNull(bundle, "bundle").lanes();
    int row = table.indexOf(role);
    return openMappedId(table.ids, row * M3ContentId.BYTES, table.sizes[row],
        PrecomputeLaneTable.KINDS[Byte.toUnsignedInt(table.kinds[row])], maxBytes);
  }

  public M3ObjectRef put(byte[] bytes, M3ObjectKind kind) throws IOException {
    Objects.requireNonNull(bytes, "bytes");
    // Hash exactly the buffered bytes written, not a second observation of caller memory.
    return put(new ByteArrayInputStream(bytes), kind);
  }

  public M3ObjectRef put(Path source, M3ObjectKind kind) throws IOException {
    Objects.requireNonNull(kind, "kind"); byte[] id = new byte[M3ContentId.BYTES];
    long size = putId(source, id, 0); return new M3ObjectRef(M3ContentId.of(id), size, kind);
  }
  public M3ObjectRef put(InputStream source, M3ObjectKind kind) throws IOException {
    Objects.requireNonNull(kind, "kind"); byte[] id = new byte[M3ContentId.BYTES];
    long size = putId(source, id, 0); return new M3ObjectRef(M3ContentId.of(id), size, kind);
  }
  long putId(Path source, byte[] outputIds, int outputOffset) throws IOException {
    Path input = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
    if (!Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("M3 content source must be a non-symlink regular file: " + input);
    }
    try (InputStream in = Files.newInputStream(input, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      return putId(in, outputIds, outputOffset);
    }
  }
  long putId(InputStream source, byte[] outputIds, int outputOffset) throws IOException {
    Objects.requireNonNull(source, "source");
    Objects.checkFromIndexSize(outputOffset, M3ContentId.BYTES, Objects.requireNonNull(outputIds, "outputIds").length);
    Path temp = newTemp(); MessageDigest digest = M3ContentId.newDigest();
    long size = 0; byte[] buffer = new byte[COPY_BUFFER_BYTES];
    try {
      try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
        int read;
        while ((read = source.read(buffer)) >= 0) {
          if (read == 0) continue;
          out.write(buffer, 0, read); digest.update(buffer, 0, read); size = Math.addExact(size, read);
        }
      }
      byte[] id = digest.digest(); publishTemp(temp, id, size);
      System.arraycopy(id, 0, outputIds, outputOffset, M3ContentId.BYTES);
      return size;
    } catch (ArithmeticException overflow) {
      throw new IOException("M3 object size overflow", overflow);
    } finally { Files.deleteIfExists(temp); }
  }

  public boolean contains(M3ContentId id) {
    return Files.isRegularFile(
        objectPath(Objects.requireNonNull(id, "id")), LinkOption.NOFOLLOW_LINKS);
  }

  public M3ObjectRef ref(M3ContentId id, M3ObjectKind kind) throws IOException {
    Path path = objectPath(Objects.requireNonNull(id, "id"));
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(path)) {
      throw new IOException("M3 object missing: " + id);
    }
    return new M3ObjectRef(id, Files.size(path), Objects.requireNonNull(kind, "kind"));
  }

  public InputStream open(M3ObjectRef ref) throws IOException {
    M3ObjectRef checked = Objects.requireNonNull(ref, "ref");
    return openId(checked.id().bytes(), 0, checked.size());
  }

  InputStream openId(byte[] ids, int offset, long expectedSize) throws IOException {
    requireObject(ids, offset, expectedSize);
    FileChannel channel = FileChannel.open(objectPath(ids, offset), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    try {
      if (channel.size() != expectedSize) throw new IOException("M3 object size changed during open");
      return java.nio.channels.Channels.newInputStream(channel);
    } catch (IOException | RuntimeException | Error failure) {
      try { channel.close(); } catch (IOException close) { failure.addSuppressed(close); }
      throw failure;
    }
  }

  public byte[] readSmall(M3ObjectRef ref, int maxBytes) throws IOException {
    M3ObjectRef checked = Objects.requireNonNull(ref, "ref");
    return readSmallId(checked.id().bytes(), 0, checked.size(), maxBytes);
  }
  byte[] readSmallId(byte[] ids, int offset, long expectedSize, int maxBytes) throws IOException {
    if (maxBytes < 0 || expectedSize < -1) throw new IllegalArgumentException("read geometry");
    Path path = objectPath(ids, offset);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("M3 object missing: " + path);
    byte[] bytes;
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      long size = channel.size();
      if (size > maxBytes || (expectedSize >= 0 && size != expectedSize)) throw new IOException("M3 object exceeds read limit or size mismatch");
      bytes = new byte[Math.toIntExact(size)]; java.nio.ByteBuffer output = java.nio.ByteBuffer.wrap(bytes);
      while (output.hasRemaining()) if (channel.read(output) < 0) throw new IOException("M3 object truncated while reading");
      if (channel.size() != size || channel.read(java.nio.ByteBuffer.allocate(1)) != -1) throw new IOException("M3 object size changed while reading");
    }
    byte[] digest = M3ContentId.newDigest().digest(bytes);
    if (!java.util.Arrays.equals(digest, 0, M3ContentId.BYTES, ids, offset, offset + M3ContentId.BYTES)) throw new IOException("M3 object digest mismatch");
    return bytes;
  }
  void requireObject(byte[] ids, int offset, long size) throws IOException {
    if (size < 0) throw new IllegalArgumentException("negative object size");
    Path path = objectPath(ids, offset);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) != size) {
      throw new IOException("M3 object missing or size mismatch: " + path);
    }
  }
  M3MappedObject openMappedId(byte[] ids, int offset, long size, M3ObjectKind kind, int maxBytes)
      throws IOException {
    return M3MappedObject.openColumns(objectPath(ids, offset), ids, offset, size,
        Objects.requireNonNull(kind, "kind").ordinal(), maxBytes);
  }

  /**
   * Verifies and maps one immutable object, with an explicit per-mapping byte budget.
   *
   * <p>This is a physical view over the existing content ID, not a new dictionary or ID domain.
   * The store and its ancestors must not be modified by untrusted/concurrent external writers.</p>
   *
   * @param ref existing content-addressed object
   * @param maxBytes maximum admitted mapping size; must fit a Java ByteBuffer
   * @return read-only primitive access to the verified object
   * @throws IOException if admission, mapping or digest validation fails
   */
  public M3MappedObject openMapped(M3ObjectRef ref, int maxBytes) throws IOException {
    M3ObjectRef checked = Objects.requireNonNull(ref, "ref");
    return M3MappedObject.open(objectPath(checked.id()), checked, maxBytes);
  }

  public boolean verify(M3ObjectRef ref) throws IOException {
    M3ObjectRef checked = Objects.requireNonNull(ref, "ref");
    MessageDigest digest = M3ContentId.newDigest();
    long count = 0L;
    byte[] buffer = new byte[COPY_BUFFER_BYTES];
    try (InputStream in = open(checked)) {
      int read;
      while ((read = in.read(buffer)) >= 0) {
        if (read == 0) continue;
        digest.update(buffer, 0, read);
        count = Math.addExact(count, read);
      }
    }
    return count == checked.size()
        && M3ContentId.of(digest.digest()).equals(checked.id());
  }

  Path objectPath(M3ContentId id) { return objectPath(Objects.requireNonNull(id, "id").bytes(), 0); }
  private Path objectPath(byte[] ids, int offset) {
    Objects.checkFromIndexSize(offset, M3ContentId.BYTES, Objects.requireNonNull(ids, "ids").length);
    String hex = java.util.HexFormat.of().formatHex(ids, offset, offset + M3ContentId.BYTES);
    return objects.resolve(hex.substring(0, 2)).resolve(hex.substring(2));
  }

  private static String encodePrecomputeBundle(PrecomputeBundle bundle) {
    StringBuilder out = new StringBuilder(512);
    out.append(PRECOMPUTE_BUNDLE_MAGIC)
        .append('\t')
        .append(bundle.sourceRoot())
        .append('\t')
        .append(bundle.configRoot())
        .append('\n');
    PrecomputeLaneTable table = (PrecomputeLaneTable) bundle.lanes();
    for (int i = 0; i < table.size(); i++) {
      out.append(PrecomputeLaneTable.ROLES[Byte.toUnsignedInt(table.roles[i])].name()).append('\t')
          .append(PrecomputeLaneTable.ENCODINGS[Byte.toUnsignedInt(table.encodings[i])].name()).append('\t')
          .append(table.elements[i]).append('\t')
          .append(java.util.HexFormat.of().formatHex(table.ids, i * M3ContentId.BYTES, (i + 1) * M3ContentId.BYTES)).append('\t')
          .append(table.sizes[i]).append('\t')
          .append(PrecomputeLaneTable.KINDS[Byte.toUnsignedInt(table.kinds[i])].name()).append('\n');
    }
    return out.toString();
  }

  private static PrecomputeBundle parsePrecomputeBundle(String encoded)
      throws IOException {
    String[] lines = Objects.requireNonNull(encoded, "encoded").split("\n", -1);
    if (lines.length < 3 || !lines[lines.length - 1].isEmpty()) {
      throw new IOException("invalid precompute bundle framing");
    }
    String[] header = lines[0].split("\t", -1);
    if (header.length != 3 || !PRECOMPUTE_BUNDLE_MAGIC.equals(header[0])) {
      throw new IOException("invalid precompute bundle header");
    }
    int count = lines.length - 2;
    if (count > PrecomputeLaneTable.ROLES.length) throw new IOException("too many precompute lanes");
    byte[] roles = new byte[count], encodings = new byte[count], kinds = new byte[count];
    byte[] ids = new byte[count * M3ContentId.BYTES]; long[] elements = new long[count], sizes = new long[count];
    try {
      for (int i = 0; i < count; i++) {
        String[] fields = lines[i + 1].split("\t", -1);
        if (fields.length != 6) throw new IOException("invalid precompute lane record");
        roles[i] = (byte) PrecomputeRole.valueOf(fields[0]).ordinal();
        encodings[i] = (byte) LaneEncoding.valueOf(fields[1]).ordinal();
        kinds[i] = (byte) M3ObjectKind.valueOf(fields[5]).ordinal();
        elements[i] = Long.parseLong(fields[2]); sizes[i] = Long.parseLong(fields[4]);
        parseSha256(fields[3], ids, i * M3ContentId.BYTES);
      }
      return new PrecomputeBundle(header[1], header[2], new PrecomputeLaneTable(roles, encodings, kinds, elements, sizes, ids));
    } catch (IllegalArgumentException | ArithmeticException invalid) {
      throw new IOException("invalid precompute bundle value", invalid);
    }
  }
  private static void parseSha256(String hex, byte[] output, int offset) {
    if (hex.length() != M3ContentId.BYTES * 2) throw new IllegalArgumentException("SHA-256 length");
    for (int i = 0; i < M3ContentId.BYTES; i++) {
      char left = hex.charAt(i * 2), right = hex.charAt(i * 2 + 1);
      int high = left >= '0' && left <= '9' ? left - '0' : left >= 'a' && left <= 'f' ? left - 'a' + 10 : -1;
      int low = right >= '0' && right <= '9' ? right - '0' : right >= 'a' && right <= 'f' ? right - 'a' + 10 : -1;
      if (high < 0 || low < 0) throw new IllegalArgumentException("SHA-256 lowercase hex");
      output[offset + i] = (byte) ((high << 4) | low);
    }
  }

  private static String requireSha256(String value, String field) {
    String checked = Objects.requireNonNull(value, field);
    parseSha256(checked, new byte[M3ContentId.BYTES], 0);
    return checked;
  }

  private static final class PrimitiveLaneInputStream extends InputStream {
    private final int elements;
    private final int width;
    private final boolean bigEndian;
    private final java.util.function.IntToLongFunction valueAt;
    private long byteOffset;

    private PrimitiveLaneInputStream(
        int elements,
        int width,
        boolean bigEndian,
        java.util.function.IntToLongFunction valueAt) {
      this.elements = elements;
      this.width = width;
      this.bigEndian = bigEndian;
      this.valueAt = Objects.requireNonNull(valueAt, "valueAt");
    }

    @Override
    public int read() {
      if (byteOffset >= (long) elements * width) return -1;
      int result = encodedByte(byteOffset);
      byteOffset++;
      return result;
    }

    @Override
    public int read(byte[] target, int offset, int length) {
      Objects.checkFromIndexSize(offset, length, target.length);
      long total = (long) elements * width;
      if (byteOffset >= total) return -1;
      int count = (int) Math.min(length, total - byteOffset);
      for (int index = 0; index < count; index++) {
        target[offset + index] = (byte) encodedByte(byteOffset + index);
      }
      byteOffset += count;
      return count;
    }

    private int encodedByte(long absoluteByte) {
      int element = Math.toIntExact(absoluteByte / width);
      int within = (int) (absoluteByte % width);
      int byteIndex = bigEndian ? width - 1 - within : within;
      return (int) (valueAt.applyAsLong(element) >>> (byteIndex * Byte.SIZE))
          & 0xff;
    }
  }

  private void publishTemp(Path temp, byte[] id, long size) throws IOException {
    Path target = objectPath(id, 0);
    durableCreateDirs(target.getParent());
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      requireExactExisting(temp, target, size, id);
    } else {
      try { moveCreateOnly(temp, target); }
      catch (FileAlreadyExistsException race) { requireExactExisting(temp, target, size, id); }
    }
    forceFile(target); syncDirectory(target.getParent());
  }
  private static void moveCreateOnly(Path source, Path target) throws IOException {
    // Keep the staged source through any lost race so exact duplicate validation can still read it.
    forceFile(source);
    try { Files.createLink(target, source); }
    catch (UnsupportedOperationException unsupported) {
      throw new IOException("M3 create-only publication requires hard-link support", unsupported);
    }
    syncDirectory(target.getParent());
  }
  private static void requireExactExisting(Path candidate, Path existing, long size, byte[] id)
      throws IOException {
    if (!Files.isRegularFile(existing, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("M3 object target is not a regular file: " + existing);
    }
    if (Files.size(existing) != size || !M3NativeContent.filesEqual(candidate, existing)) {
      throw new IOException("SHA-256 collision or corrupt M3 object for " + java.util.HexFormat.of().formatHex(id));
    }
  }
  static void forceFile(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
      channel.force(true);
    } catch (UnsupportedOperationException unsupported) {
      throw new IOException("M3 durable publication requires file sync support: " + path, unsupported);
    }
  }
  static void syncDirectory(Path path) throws IOException {
    if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("M3 directory missing or symlink: " + path);
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      channel.force(true);
    } catch (UnsupportedOperationException unsupported) {
      throw new IOException("M3 durable publication requires directory sync support: " + path, unsupported);
    }
  }
  static void durableCreateDirs(Path path) throws IOException {
    Path directory = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    Path parent = directory.getParent();
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      if (parent != null) durableCreateDirs(parent);
      // Another publisher can create the directory after the probe above. Let mkdir settle
      // that race, then validate the existing entry without following symbolic links.
      try { Files.createDirectory(directory); }
      catch (FileAlreadyExistsException race) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("M3 directory creation conflict: " + directory, race);
      }
    }
    syncDirectory(directory);
    if (parent != null) syncDirectory(parent);
  }

  private Path newTemp() {
    return staging.resolve("object-" + UUID.randomUUID() + ".tmp");
  }

  private static void rejectSymlink(Path path) throws IOException {
    if (Files.isSymbolicLink(path)) {
      throw new IOException("M3 store path may not be a symlink: " + path);
    }
  }
}
