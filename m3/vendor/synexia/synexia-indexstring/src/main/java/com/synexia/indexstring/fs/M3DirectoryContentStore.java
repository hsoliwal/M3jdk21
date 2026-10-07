// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
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
    private final Set<PrecomputeRole> roles;

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
      EnumSet<PrecomputeRole> required = EnumSet.of(first, rest);
      roles = Set.copyOf(required);
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
      return roles;
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

  public record PrecomputeBundle(
      String sourceRoot,
      String configRoot,
      List<PrecomputeLane> lanes) {
    public PrecomputeBundle {
      sourceRoot = requireSha256(sourceRoot, "sourceRoot");
      configRoot = requireSha256(configRoot, "configRoot");
      ArrayList<PrecomputeLane> ordered =
          new ArrayList<>(Objects.requireNonNull(lanes, "lanes"));
      ordered.sort(Comparator.comparingInt(lane -> lane.role().ordinal()));
      HashSet<PrecomputeRole> roles = new HashSet<>();
      for (PrecomputeLane lane : ordered) {
        if (!roles.add(lane.role())) {
          throw new IllegalArgumentException("duplicate precompute role " + lane.role());
        }
      }
      if (ordered.isEmpty()) {
        throw new IllegalArgumentException("precompute bundle must contain lanes");
      }
      lanes = List.copyOf(ordered);
    }

    public boolean supports(PrecomputeProfile profile) {
      PrecomputeProfile checked = Objects.requireNonNull(profile, "profile");
      EnumSet<PrecomputeRole> present = EnumSet.noneOf(PrecomputeRole.class);
      for (PrecomputeLane lane : lanes) present.add(lane.role());
      return present.containsAll(checked.roles());
    }

    public List<PrecomputeRole> missing(PrecomputeProfile profile) {
      PrecomputeProfile checked = Objects.requireNonNull(profile, "profile");
      EnumSet<PrecomputeRole> missing = EnumSet.copyOf(checked.roles());
      for (PrecomputeLane lane : lanes) missing.remove(lane.role());
      return List.copyOf(missing);
    }

    public void requireProfile(PrecomputeProfile profile) {
      List<PrecomputeRole> absent = missing(profile);
      if (!absent.isEmpty()) {
        throw new IllegalArgumentException("precompute profile missing lanes " + absent);
      }
    }

    public PrecomputeLane require(PrecomputeRole role) {
      PrecomputeRole wanted = Objects.requireNonNull(role, "role");
      return lanes.stream()
          .filter(lane -> lane.role() == wanted)
          .findFirst()
          .orElseThrow(
              () -> new IllegalArgumentException("missing precompute role " + wanted));
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
    Files.createDirectories(objects);
    Files.createDirectories(staging);
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
    String encoded =
        new String(readSmall(checked, maxBytes), StandardCharsets.UTF_8);
    PrecomputeBundle parsed = parsePrecomputeBundle(encoded);
    if (!encodePrecomputeBundle(parsed).equals(encoded)) {
      throw new IOException("non-canonical precompute bundle descriptor");
    }
    return parsed;
  }

  public M3MappedObject openPrecomputeLane(
      PrecomputeBundle bundle, PrecomputeRole role, int maxBytes)
      throws IOException {
    return openMapped(
        Objects.requireNonNull(bundle, "bundle").require(role).object(),
        maxBytes);
  }

  public M3ObjectRef put(byte[] bytes, M3ObjectKind kind) throws IOException {
    Objects.requireNonNull(bytes, "bytes");
    // Hash exactly the buffered bytes written, not a second observation of caller memory.
    return put(new ByteArrayInputStream(bytes), kind);
  }

  public M3ObjectRef put(Path source, M3ObjectKind kind) throws IOException {
    Path input = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
    if (!Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(input)) {
      throw new IOException("M3 content source must be a non-symlink regular file: " + input);
    }
    try (InputStream in = Files.newInputStream(input, StandardOpenOption.READ)) {
      return put(in, kind);
    }
  }

  public M3ObjectRef put(InputStream source, M3ObjectKind kind) throws IOException {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(kind, "kind");
    Path temp = newTemp();
    MessageDigest digest = M3ContentId.newDigest();
    long size = 0L;
    byte[] buffer = new byte[COPY_BUFFER_BYTES];
    try {
      try (OutputStream out =
          Files.newOutputStream(
              temp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        int read;
        while ((read = source.read(buffer)) >= 0) {
          if (read == 0) continue;
          out.write(buffer, 0, read);
          digest.update(buffer, 0, read);
          size = Math.addExact(size, read);
        }
      }
      return publishTemp(temp, M3ContentId.of(digest.digest()), size, kind);
    } catch (ArithmeticException overflow) {
      throw new IOException("M3 object size overflow", overflow);
    } finally {
      Files.deleteIfExists(temp);
    }
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
    Path path = objectPath(checked.id());
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(path)) {
      throw new IOException("M3 object missing: " + checked.id());
    }
    if (Files.size(path) != checked.size()) {
      throw new IOException("M3 object size mismatch");
    }
    return Files.newInputStream(path, StandardOpenOption.READ);
  }

  public byte[] readSmall(M3ObjectRef ref, int maxBytes) throws IOException {
    if (maxBytes < 0) throw new IllegalArgumentException("negative byte limit");
    M3ObjectRef checked = Objects.requireNonNull(ref, "ref");
    if (checked.size() > maxBytes) throw new IOException("M3 object exceeds read limit");
    byte[] data;
    try (InputStream in = open(checked)) {
      data = in.readNBytes(Math.toIntExact(checked.size()));
      if (in.read() != -1) {
        throw new IOException("M3 object length changed while reading");
      }
    }
    if (data.length != checked.size() || data.length > maxBytes) {
      throw new IOException("M3 object length changed while reading");
    }
    if (!M3ContentId.digest(data).equals(checked.id())) {
      throw new IOException("M3 object digest mismatch");
    }
    return data;
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

  Path objectPath(M3ContentId id) {
    String hex = id.hex();
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
    for (PrecomputeLane lane : bundle.lanes()) {
      M3ObjectRef object = lane.object();
      out.append(lane.role().name())
          .append('\t')
          .append(lane.encoding().name())
          .append('\t')
          .append(lane.elementCount())
          .append('\t')
          .append(object.id().hex())
          .append('\t')
          .append(object.size())
          .append('\t')
          .append(object.kind().name())
          .append('\n');
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
    ArrayList<PrecomputeLane> lanes = new ArrayList<>(lines.length - 2);
    try {
      for (int index = 1; index < lines.length - 1; index++) {
        String[] fields = lines[index].split("\t", -1);
        if (fields.length != 6) {
          throw new IOException("invalid precompute lane record");
        }
        lanes.add(
            new PrecomputeLane(
                PrecomputeRole.valueOf(fields[0]),
                LaneEncoding.valueOf(fields[1]),
                Long.parseLong(fields[2]),
                new M3ObjectRef(
                    M3ContentId.parse(fields[3]),
                    Long.parseLong(fields[4]),
                    M3ObjectKind.valueOf(fields[5]))));
      }
      return new PrecomputeBundle(header[1], header[2], lanes);
    } catch (IllegalArgumentException invalid) {
      throw new IOException("invalid precompute bundle value", invalid);
    }
  }

  private static String requireSha256(String value, String field) {
    String checked = Objects.requireNonNull(value, field);
    M3ContentId.parse(checked);
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

  private M3ObjectRef publishTemp(
      Path temp, M3ContentId id, long size, M3ObjectKind kind) throws IOException {
    Objects.requireNonNull(kind, "kind");
    Path target = objectPath(id);
    Files.createDirectories(target.getParent());
    rejectSymlink(target.getParent());
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      requireExactExisting(temp, target, size, id);
      return new M3ObjectRef(id, size, kind);
    }
    try {
      moveCreateOnly(temp, target);
    } catch (FileAlreadyExistsException race) {
      requireExactExisting(temp, target, size, id);
    }
    return new M3ObjectRef(id, size, kind);
  }

  private static void requireExactExisting(
      Path candidate, Path existing, long size, M3ContentId id) throws IOException {
    if (!Files.isRegularFile(existing, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(existing)) {
      throw new IOException("M3 object target is not a regular file: " + existing);
    }
    if (Files.size(existing) != size
        || !M3NativeContent.filesEqual(candidate, existing)) {
      throw new IOException("SHA-256 collision or corrupt M3 object for " + id);
    }
  }

  private static void moveCreateOnly(Path source, Path target) throws IOException {
    // ATOMIC_MOVE may replace an existing target. A new hard link cannot do so.
    // Keep source until the caller's finally block, including after a lost publication race.
    try (FileChannel channel = FileChannel.open(source, StandardOpenOption.WRITE)) {
      channel.force(true);
    }
    try {
      Files.createLink(target, source);
    } catch (UnsupportedOperationException unsupported) {
      throw new IOException("M3 create-only publication requires hard-link support", unsupported);
    }
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
