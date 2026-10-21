// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.AbstractList;
import java.util.Collections;
import java.util.function.IntToLongFunction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent computation-memory substrate for MIndex files and derived structures.
 *
 * <p>Raw bytes remain canonical. Every derived artifact is an immutable content-addressed object;
 * generations publish atomically through a compare-and-swap HEAD protected by a process/file lock.</p>
 */
public final class M3FileSystemStore {
  public static final String MINDEX_SEMANTIC_STRUCTURE = "mindex.semantic";
  public static final String MINDEX_SOURCE_FACTS_STRUCTURE = "mindex.source-facts";
  private static final int MAX_TOKEN_RANGE_BATCH = 65_536;
  private static final int RANGE_DIGEST_BUFFER_BYTES = 64 * 1024;
  private static final int MAX_MANIFEST_BYTES =
      64 * 1024 * 1024;
  private static final int MAX_GENERATION_BYTES =
      512 * 1024 * 1024;

  private final M3DirectoryContentStore objects;
  private final M3PrecomputeCache precomputes;
  private final Path head;
  private final Path headLock;
  private static final Object HEAD_PROMOTION_LOCK = new Object();
  private final AtomicHeadMove headMove;

  @FunctionalInterface
  interface AtomicHeadMove {
    void move(Path from, Path to) throws IOException;
  }
  private volatile HeadGenerationView headGenerationView;

  private record HeadGenerationView(byte[] id, M3GenerationRoot root) {
    private HeadGenerationView {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(root, "root");
    }
  }

  public M3FileSystemStore(Path root) throws IOException {
    this(root, (from, to) -> Files.move(from, to,
        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING));
  }

  M3FileSystemStore(Path root, AtomicHeadMove headMove) throws IOException {
    this.headMove = Objects.requireNonNull(headMove, "headMove");
    this.objects = new M3DirectoryContentStore(root);
    this.precomputes = new M3PrecomputeCache(objects);
    this.head = objects.root().resolve("HEAD");
    this.headLock = objects.root().resolve("HEAD.lock");
  }

  public M3DirectoryContentStore objects() {
    return objects;
  }

  public M3PrecomputeCache precomputes() {
    return precomputes;
  }

  public M3ObjectRef putRaw(byte[] bytes)
      throws IOException {
    return objects.put(bytes, M3ObjectKind.RAW_BYTES);
  }

  public M3ObjectRef putRaw(Path path)
      throws IOException {
    return objects.put(path, M3ObjectKind.RAW_BYTES);
  }

  /** Publishes an already-validated MIndexFileImage artifact without rebuilding it. */
  public M3ObjectRef putMIndexFileImage(Path image)
      throws IOException {
    return objects.put(
        image, M3ObjectKind.MINDEX_FILE_IMAGE);
  }

  /** Publishes the existing authenticated source-facts image without rebuilding it. */
  public M3ObjectRef putMIndexFileSourceFactsImage(Path image) throws IOException {
    return objects.put(image, M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE);
  }

  public M3ObjectRef putPrecomputeOutput(byte[] bytes)
      throws IOException {
    return objects.put(
        bytes, M3ObjectKind.PRECOMPUTE_OUTPUT);
  }

  public M3ObjectRef publishPrecompute(
      M3PrecomputeResult result) throws IOException {
    return precomputes.publish(result);
  }

  public M3ObjectRef publishManifest(
      M3FileManifest manifest) throws IOException {
    M3FileManifest checked =
        Objects.requireNonNull(manifest, "manifest");
    M3ObjectRef ref =
        objects.put(
            checked.canonicalBytes(),
            M3ObjectKind.FILE_MANIFEST);
    if (!ref.id().equals(checked.id())) {
      throw new IOException("M3 manifest identity mismatch");
    }
    return ref;
  }

  public M3FileManifest readManifest(M3ObjectRef ref)
      throws IOException {
    requireKind(ref, M3ObjectKind.FILE_MANIFEST);
    return M3FileManifest.decode(
        objects.readSmall(ref, MAX_MANIFEST_BYTES));
  }

  public M3ObjectRef publishGeneration(
      M3GenerationRoot root) throws IOException {
    M3GenerationRoot checked =
        Objects.requireNonNull(root, "root");
    M3ObjectRef ref =
        objects.put(
            checked.canonicalBytes(),
            M3ObjectKind.GENERATION_ROOT);
    if (!ref.id().equals(checked.id())) {
      throw new IOException("M3 generation identity mismatch");
    }
    return ref;
  }

  public M3GenerationRoot readGeneration(M3ObjectRef ref)
      throws IOException {
    requireKind(ref, M3ObjectKind.GENERATION_ROOT);
    return M3GenerationRoot.decode(
        objects.readSmall(ref, MAX_GENERATION_BYTES));
  }

  public Optional<M3ObjectRef> head() throws IOException {
    byte[] id = readHeadId();
    return id == null ? Optional.empty()
        : Optional.of(objects.ref(M3ContentId.of(id), M3ObjectKind.GENERATION_ROOT));
  }

  private byte[] readHeadId() throws IOException {
    if (!Files.exists(head, LinkOption.NOFOLLOW_LINKS)) return null;
    if (!Files.isRegularFile(head, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("M3 HEAD must be a non-symlink regular file");
    }
    try (FileChannel channel = FileChannel.open(head, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      long size = channel.size();
      if (size < 1 || size > 1024) throw new IOException("invalid M3 HEAD size");
      byte[] bytes = new byte[(int) size];
      ByteBuffer target = ByteBuffer.wrap(bytes);
      while (target.hasRemaining()) {
        if (channel.read(target) < 0) throw new IOException("truncated M3 HEAD");
      }
      if (channel.size() != size) throw new IOException("M3 HEAD changed during read");
      int start = 0, end = bytes.length;
      while (start < end && Byte.toUnsignedInt(bytes[start]) <= 32) start++;
      while (end > start && Byte.toUnsignedInt(bytes[end - 1]) <= 32) end--;
      if (end - start != 64) throw new IOException("invalid M3 HEAD identity length");
      byte[] id = new byte[32];
      for (int i = 0; i < id.length; i++) {
        int high = hex(bytes[start + i * 2]), low = hex(bytes[start + i * 2 + 1]);
        if (high < 0 || low < 0) throw new IOException("invalid M3 HEAD identity");
        id[i] = (byte) ((high << 4) | low);
      }
      return id;
    } catch (NoSuchFileException absent) {
      return null;
    }
  }

  private static int hex(byte value) {
    int c = Byte.toUnsignedInt(value);
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
  }

  /** Current generation, decoded at most once per observed HEAD identity in this store. */
  public Optional<M3GenerationRoot> headGeneration() throws IOException {
    byte[] id = readHeadId();
    if (id == null) {
      headGenerationView = null;
      return Optional.empty();
    }
    HeadGenerationView cached = headGenerationView;
    if (cached != null && Arrays.equals(cached.id(), id)) return Optional.of(cached.root());
    M3GenerationRoot decoded = M3GenerationRoot.decode(objects.readSmallId(id, 0, -1, MAX_GENERATION_BYTES));
    headGenerationView = new HeadGenerationView(id, decoded);
    return Optional.of(decoded);
  }

  /** Exact manifest object reference for one logical path in the currently observed HEAD. */
  public Optional<M3ObjectRef> headManifestRef(String path) throws IOException {
    Optional<M3GenerationRoot> generation = headGeneration();
    if (generation.isEmpty()) return Optional.empty();
    Optional<M3ContentId> manifest = generation.get().entry(path);
    return manifest.isEmpty()
        ? Optional.empty()
        : Optional.of(objects.ref(manifest.get(), M3ObjectKind.FILE_MANIFEST));
  }

  /** Exact decoded manifest for one logical path in the currently observed HEAD. */
  public Optional<M3FileManifest> headManifest(String path) throws IOException {
    Optional<M3ObjectRef> ref = headManifestRef(path);
    return ref.isEmpty() ? Optional.empty() : Optional.of(readManifest(ref.get()));
  }

  /** Bounded immediate-child page from the currently observed HEAD generation. */
  public Optional<M3GenerationRoot.DirectoryPage> headDirectoryPage(
      String directory, String afterName, int limit) throws IOException {
    Optional<M3GenerationRoot> generation = headGeneration();
    return generation.isEmpty()
        ? Optional.empty()
        : Optional.of(generation.get().directoryPage(directory, afterName, limit));
  }

  /**
   * Serial canonical promotion. The current HEAD must equal {@code expected}; otherwise no state is
   * changed. The target generation object must already exist and verify before publication.
   */
  public void promote(Optional<M3ContentId> expected, M3ObjectRef next) throws IOException {
    Objects.requireNonNull(expected, "expected");
    requireKind(next, M3ObjectKind.GENERATION_ROOT);
    if (!objects.verify(next)) throw new IOException("next M3 generation failed object verification");
    byte[] expectedId = expected.isEmpty() ? null : expected.get().bytes();
    // POSIX record locks are process-scoped: serialize before opening another descriptor.
    synchronized (HEAD_PROMOTION_LOCK) {
      if (Files.exists(headLock, LinkOption.NOFOLLOW_LINKS)
          && !Files.isRegularFile(headLock, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("M3 HEAD lock must be a non-symlink regular file");
      }
      try (FileChannel channel = FileChannel.open(headLock, StandardOpenOption.CREATE,
              StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
          java.nio.channels.FileLock lock = channel.lock()) {
        if (!lock.isValid()) throw new IOException("M3 HEAD lock is not valid");
        if (!Arrays.equals(readHeadId(), expectedId)) throw new IOException("M3 HEAD compare-and-swap mismatch");
        M3DirectoryContentStore.syncDirectory(objects.root());
        Path temp = objects.root().resolve(".head-" + UUID.randomUUID() + ".tmp");
        Throwable failure = null;
        try {
          try (FileChannel staged = FileChannel.open(temp, StandardOpenOption.CREATE_NEW,
              StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer bytes = StandardCharsets.US_ASCII.encode(next.id().hex() + "\n");
            while (bytes.hasRemaining()) staged.write(bytes);
            staged.force(true);
          }
          headMove.move(temp, head);
          headGenerationView = null;
          try {
            M3DirectoryContentStore.syncDirectory(objects.root());
          } catch (IOException uncertainty) {
            throw new IOException("M3 HEAD rename completed but directory sync failed; re-read HEAD before retry", uncertainty);
          }
        } catch (IOException | RuntimeException | Error problem) {
          failure = problem;
          throw problem;
        } finally {
          try { Files.deleteIfExists(temp); }
          catch (IOException cleanup) {
            if (failure != null) failure.addSuppressed(cleanup);
            else throw cleanup;
          }
        }
      }
    }
  }

  public M3ObjectRef createFileManifest(
      M3ObjectRef raw,
      Map<String, M3ObjectRef> structures,
      Map<String, M3ObjectRef> relations,
      Map<M3ContentId, M3ContentId>
          precomputeDescriptors)
      throws IOException {
    return publishManifest(
        new M3FileManifest(
            raw,
            structures,
            relations,
            precomputeDescriptors));
  }

  /** Compose the three existing objects into the stable MIndexFile roles of a format-1 manifest. */
  public M3ObjectRef createMIndexFileManifest(
      M3ObjectRef raw, M3ObjectRef semanticImage, M3ObjectRef sourceFactsImage)
      throws IOException {
    requireKind(raw, M3ObjectKind.RAW_BYTES);
    requireKind(semanticImage, M3ObjectKind.MINDEX_FILE_IMAGE);
    requireKind(sourceFactsImage, M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE);
    return createFileManifest(
        raw,
        Map.of(
            MINDEX_SEMANTIC_STRUCTURE, semanticImage,
            MINDEX_SOURCE_FACTS_STRUCTURE, sourceFactsImage),
        Map.of(),
        Map.of());
  }

  private static final String QUALIFICATION_RELATION_PREFIX = "mindex.unicodex.";

  /**
   * Binds full trusted UnicodeX qualification-descriptor SHA-256 seals to the existing
   * content-addressed manifest. The caller obtains these from admitted sender catalogs.
   * This records compatibility pins, not provenance authenticity. No SMFILE01 change occurs.
   * Existing pins may be repeated identically; conflicting replacements are refused.
   */
  public M3ObjectRef withMIndexLexiconQualifications(
      M3ObjectRef manifestRef, Map<Integer, String> qualificationSeals) throws IOException {
    Map<Integer, String> pins = Map.copyOf(Objects.requireNonNull(qualificationSeals, "qualificationSeals"));
    for (var entry : pins.entrySet()) {
      if (entry.getKey() < 0 || entry.getKey() > 65535
          || !entry.getValue().matches("[0-9a-f]{64}"))
        throw new IllegalArgumentException("invalid UnicodeX qualification language/seal");
    }
    M3FileManifest manifest = readManifest(manifestRef);
    requireStructure(manifest, MINDEX_SEMANTIC_STRUCTURE, M3ObjectKind.MINDEX_FILE_IMAGE);
    Map<Integer, String> existing = readLexiconQualifications(manifest);
    for (var entry : existing.entrySet()) {
      if (!entry.getValue().equals(pins.get(entry.getKey())))
        throw new IOException("conflicting or removed UnicodeX qualification");
    }
    for (var entry : pins.entrySet()) {
      M3ObjectRef seal = putRaw(java.util.HexFormat.of().parseHex(entry.getValue()));
      manifest = manifest.withRelation(QUALIFICATION_RELATION_PREFIX + entry.getKey(), seal);
    }
    return publishManifest(manifest);
  }

  /**
   * Qualified SMFILE01 admission. Every declared language must have exactly one manifest pin
   * equal to the admitted receiver descriptor's full SHA-256, followed by the existing
   * mandatory generation and structural checks. Receiver callbacks must use trusted catalogs;
   * the 64-bit generation callback alone is never a substitute for qualification.
   */
  public List<M3FileTokenRangeSha256> sha256MIndexFileTokenRangesQualified(
      M3ObjectRef manifestRef, IntToLongFunction languageFingerprint,
      java.util.function.IntFunction<String> receiverQualificationSeal,
      int[] startTokens, int[] endTokens) throws IOException {
    Objects.requireNonNull(languageFingerprint, "languageFingerprint");
    Objects.requireNonNull(receiverQualificationSeal, "receiverQualificationSeal");
    Map<Integer, String> pins = readLexiconQualifications(readManifest(manifestRef));
    return sha256MIndexFileTokenRanges(manifestRef, languageFingerprint, startTokens,
        endTokens, pins, receiverQualificationSeal);
  }

  private Map<Integer, String> readLexiconQualifications(M3FileManifest manifest) throws IOException {
    Map<Integer, String> result = new java.util.HashMap<>();
    for (var entry : manifest.relations().entrySet()) {
      if (!entry.getKey().startsWith(QUALIFICATION_RELATION_PREFIX)) continue;
      String suffix = entry.getKey().substring(QUALIFICATION_RELATION_PREFIX.length());
      if (!suffix.matches("0|[1-9][0-9]{0,4}"))
        throw new IOException("invalid UnicodeX qualification relation");
      int language = Integer.parseInt(suffix);
      M3ObjectRef ref = entry.getValue();
      if (language > 65535 || ref.kind() != M3ObjectKind.RAW_BYTES || ref.size() != 32)
        throw new IOException("invalid UnicodeX qualification object");
      byte[] seal = objects.readSmall(ref, 32);
      if (seal.length != 32) throw new IOException("invalid qualification seal length");
      result.put(language, java.util.HexFormat.of().formatHex(seal));
    }
    return Map.copyOf(result);
  }


  /** Exact SHA-256 of the original raw UTF-8 bytes between two token boundaries. */
  public M3FileTokenRangeSha256 sha256MIndexFileTokenRange(
      M3ObjectRef manifestRef, int startToken, int endToken) throws IOException {
    return sha256MIndexFileTokenRange(manifestRef, null, startToken, endToken);
  }

  /** Optionally binds each persisted language generation to its current owning index. */
  public M3FileTokenRangeSha256 sha256MIndexFileTokenRange(
      M3ObjectRef manifestRef, IntToLongFunction languageFingerprint, int startToken, int endToken)
      throws IOException {
    if (startToken < 0 || endToken < startToken) throw new IndexOutOfBoundsException("invalid token range");
    return sha256MIndexFileTokenRanges(manifestRef, languageFingerprint,
        new int[] { startToken }, new int[] { endToken }).getFirst();
  }

  public List<M3FileTokenRangeSha256> sha256MIndexFileTokenRanges(
      M3ObjectRef manifestRef, List<M3FileTokenRange> ranges) throws IOException {
    return sha256MIndexFileTokenRanges(manifestRef, null, ranges);
  }

  public List<M3FileTokenRangeSha256> sha256MIndexFileTokenRanges(
      M3ObjectRef manifestRef, IntToLongFunction languageFingerprint, List<M3FileTokenRange> ranges)
      throws IOException {
    Objects.requireNonNull(ranges, "ranges");
    int count = ranges.size();
    if (count > MAX_TOKEN_RANGE_BATCH) throw new IllegalArgumentException("token range batch exceeds 65536 requests");
    int[] starts = new int[count], ends = new int[count];
    for (int i = 0; i < count; i++) {
      M3FileTokenRange range = Objects.requireNonNull(ranges.get(i), "range");
      starts[i] = range.startToken();
      ends[i] = range.endToken();
    }
    return sha256MIndexFileTokenRanges(manifestRef, languageFingerprint, starts, ends);
  }

  public List<M3FileTokenRangeSha256> sha256MIndexFileTokenRanges(
      M3ObjectRef manifestRef, int[] startTokens, int[] endTokens) throws IOException {
    return sha256MIndexFileTokenRanges(manifestRef, null, startTokens, endTokens);
  }

  /** Hashes a bounded batch in one verified raw traversal; coordinates and results use arrays. */
  public List<M3FileTokenRangeSha256> sha256MIndexFileTokenRanges(
      M3ObjectRef manifestRef, IntToLongFunction languageFingerprint, int[] startTokens, int[] endTokens)
      throws IOException {
    return sha256MIndexFileTokenRanges(manifestRef, languageFingerprint, startTokens,
        endTokens, null, null);
  }

  private List<M3FileTokenRangeSha256> sha256MIndexFileTokenRanges(
      M3ObjectRef manifestRef, IntToLongFunction languageFingerprint, int[] startTokens, int[] endTokens,
      Map<Integer, String> qualificationSeals,
      java.util.function.IntFunction<String> receiverQualificationSeal) throws IOException {
    int[] starts = Objects.requireNonNull(startTokens, "startTokens").clone();
    int[] ends = Objects.requireNonNull(endTokens, "endTokens").clone();
    if (starts.length != ends.length || starts.length > MAX_TOKEN_RANGE_BATCH) {
      throw new IllegalArgumentException("token range batch geometry");
    }
    M3FileManifest manifest = readManifest(manifestRef);
    byte[] ids = new byte[96];
    manifest.copyRawIdTo(ids, 0);
    long rawSize = manifest.rawSizeValue();
    int semantic = requireStructure(manifest, MINDEX_SEMANTIC_STRUCTURE, M3ObjectKind.MINDEX_FILE_IMAGE);
    int source = requireStructure(manifest, MINDEX_SOURCE_FACTS_STRUCTURE, M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE);
    manifest.copyStructureIdTo(semantic, ids, 32);
    manifest.copyStructureIdTo(source, ids, 64);
    M3MappedObject mapped = objects.openMappedId(ids, 32, manifest.structureSize(semantic),
        M3ObjectKind.MINDEX_FILE_IMAGE, MAX_GENERATION_BYTES);
    long[] shape = qualificationSeals == null
        ? M3ColdFileImage.validate(mapped, languageFingerprint)
        : M3ColdFileImage.validateQualified(mapped, languageFingerprint,
            qualificationSeals, receiverQualificationSeal);
    M3MappedObject facts = objects.openMappedId(ids, 64, manifest.structureSize(source),
        M3ObjectKind.MINDEX_SOURCE_FACTS_IMAGE, M3FileSourceFactsImage.MAX_IMAGE_BYTES);
    long[] offsets = M3FileSourceFactsImage.read(facts, shape, ids, 0, rawSize);
    verifyMapping(mapped, ids, 32);
    verifyMapping(facts, ids, 64);
    long[] byteStarts = new long[starts.length], byteEnds = new long[starts.length];
    for (int i = 0; i < starts.length; i++) {
      Objects.checkFromToIndex(starts[i], ends[i], Math.toIntExact(shape[1]));
      byteStarts[i] = offsets[starts[i]];
      byteEnds[i] = offsets[ends[i]];
    }
    byte[] digests = sha256RawRanges(ids, rawSize, byteStarts, byteEnds);
    return Collections.unmodifiableList(new TokenRangeResults(starts, ends, byteStarts, byteEnds, digests));
  }

  private static void verifyMapping(M3MappedObject mapped, byte[] ids, int offset) throws IOException {
    MessageDigest digest = M3ContentId.newDigest();
    digest.update(mapped.bytes(0, mapped.size()));
    byte[] actual = digest.digest();
    if (!Arrays.equals(actual, 0, 32, ids, offset, offset + 32)) {
      throw new IOException("M3 source image changed during admission");
    }
  }

  private static int requireStructure(
      M3FileManifest manifest, String name, M3ObjectKind expectedKind) throws IOException {
    int row = manifest.structureIndex(name);
    if (row < 0 || manifest.structureKindOrdinal(row) != expectedKind.ordinal()) {
      throw new IOException("M3 MIndexFile manifest requires " + name + " as " + expectedKind);
    }
    return row;
  }

  private byte[] sha256RawRanges(byte[] rawId, long rawSize, long[] starts, long[] ends) throws IOException {
    int count = starts.length;
    MessageDigest[] digests = new MessageDigest[count];
    int[] order = new int[count], active = new int[count];
    int nonempty = 0;
    for (int i = 0; i < count; i++) {
      digests[i] = M3ContentId.newDigest();
      if (starts[i] < ends[i]) order[nonempty++] = i;
    }
    for (int i = nonempty / 2 - 1; i >= 0; i--) sift(order, i, nonempty, starts, ends);
    for (int i = nonempty - 1; i > 0; i--) {
      int t = order[0]; order[0] = order[i]; order[i] = t;
      sift(order, 0, i, starts, ends);
    }
    MessageDigest full = M3ContentId.newDigest();
    byte[] buffer = new byte[RANGE_DIGEST_BUFFER_BYTES];
    long offset = 0;
    int next = 0, live = 0;
    try (InputStream input = objects.openId(rawId, 0, rawSize)) {
      int n;
      while ((n = input.read(buffer)) >= 0) {
        if (n == 0) continue;
        long chunkEnd = Math.addExact(offset, n);
        if (chunkEnd > rawSize) throw new IOException("M3 raw object grew during range hashing");
        full.update(buffer, 0, n);
        while (next < nonempty && starts[order[next]] < chunkEnd) active[live++] = order[next++];
        int retained = 0;
        for (int i = 0; i < live; i++) {
          int row = active[i];
          long from = Math.max(offset, starts[row]), to = Math.min(chunkEnd, ends[row]);
          if (from < to) digests[row].update(buffer, Math.toIntExact(from - offset), Math.toIntExact(to - from));
          if (ends[row] > chunkEnd) active[retained++] = row;
        }
        live = retained;
        offset = chunkEnd;
      }
    }
    byte[] actual = full.digest();
    if (offset != rawSize || !Arrays.equals(actual, 0, 32, rawId, 0, 32)) {
      throw new IOException("M3 raw object size/digest changed during range hashing");
    }
    byte[] results = new byte[Math.multiplyExact(count, M3ContentId.BYTES)];
    for (int i = 0; i < count; i++) System.arraycopy(digests[i].digest(), 0, results, i * 32, 32);
    return results;
  }

  private static void sift(int[] order, int root, int size, long[] starts, long[] ends) {
    while (root < size / 2) {
      int child = root * 2 + 1;
      if (child + 1 < size && compare(order[child], order[child + 1], starts, ends) < 0) child++;
      if (compare(order[root], order[child], starts, ends) >= 0) return;
      int t = order[root]; order[root] = order[child]; order[child] = t;
      root = child;
    }
  }

  private static int compare(int a, int b, long[] starts, long[] ends) {
    int c = Long.compare(starts[a], starts[b]);
    if (c == 0) c = Long.compare(ends[a], ends[b]);
    return c == 0 ? Integer.compare(a, b) : c;
  }

  private static final class TokenRangeResults extends AbstractList<M3FileTokenRangeSha256> {
    private final int[] starts, ends;
    private final long[] byteStarts, byteEnds;
    private final byte[] digests;
    TokenRangeResults(int[] starts, int[] ends, long[] byteStarts, long[] byteEnds, byte[] digests) {
      this.starts = starts; this.ends = ends; this.byteStarts = byteStarts; this.byteEnds = byteEnds;
      this.digests = digests;
    }
    @Override public int size() { return starts.length; }
    @Override public M3FileTokenRangeSha256 get(int index) {
      Objects.checkIndex(index, size());
      return new M3FileTokenRangeSha256(starts[index], ends[index], byteStarts[index], byteEnds[index],
          M3ContentId.of(Arrays.copyOfRange(digests, index * 32, (index + 1) * 32)));
    }
  }

  private static void requireKind(
      M3ObjectRef ref, M3ObjectKind kind) {
    M3ObjectRef checked =
        Objects.requireNonNull(ref, "ref");
    if (checked.kind() != kind) {
      throw new IllegalArgumentException(
          "expected "
              + kind
              + " but was "
              + checked.kind());
    }
  }
}
