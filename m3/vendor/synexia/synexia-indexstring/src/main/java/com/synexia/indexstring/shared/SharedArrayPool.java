// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.shared;

import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.zip.CRC32C;

/**
 * Bounded, append-only byte/UTF-16 interning arena shared by cooperating local processes.
 * No mutable payload array is retained. Joins store literal-range descriptors, not joined bytes.
 * Input arrays/CharSequences must not be mutated DURING admission; mutation after return is safe.
 * The directory must be trusted and the arena must never be externally replaced or truncated.
 */
public final class SharedArrayPool implements AutoCloseable {
  private static final Object JVM_GATE = new Object();
  private static final long MAGIC = 0x53594e4152523031L;
  private static final long COMMIT = 0x434f4d4d49543031L;
  private static final int RECORD_MAGIC = 0x41525231;
  private static final int HEADER = 80;
  private static final int MAX_PAYLOAD = 64 * 1024 * 1024;
  private final FileChannel data;
  private final FileChannel gate;
  private final long maxBytes;
  private final int maxEntries;
  private final int maxCachedViews;
  private final UUID namespace;
  private final LinkedHashMap<Integer, SharedArray> cache = new LinkedHashMap<>(16, .75f, true);
  private final ByteBuffer scratch = ByteBuffer.allocate(8192);
  private final ByteBuffer descriptorScratch = ByteBuffer.allocate(16);
  // Optional residency metadata: record IDs remain the primitive positions lane.
  private Mapping[] mappings;
  private ReferenceQueue<ByteBuffer> mappingQueue = new ReferenceQueue<>();
  private long[] positions = new long[16], signatures = new long[16];
  private long[] descriptorSignatures = new long[16];
  private int[] kinds = new int[16], languages = new int[16], lengths = new int[16];
  private int[] hashes = new int[16], parts = new int[16], slots = new int[32];
  private int[] descriptorSlots = new int[32];
  private int count, descriptorCount;
  private long scanned = 64, literalBytes, descriptorBytes, descriptorIndexHits;
  private boolean closed;

  public record Stats(int entries, long literalPayloadBytes, long descriptorBytes,
                      long fileBytes, int cachedViews) {}
  private record Fingerprint(byte[] sha, long signature, int javaHash) {}
  private static final class Mapping extends WeakReference<ByteBuffer> {
    final int row;
    Mapping(int row, ByteBuffer owner, ReferenceQueue<ByteBuffer> queue) {
      super(owner, queue); this.row = row;
    }
  }
  private interface Input {
    int width(); int language(); int length(); int at(int index);
    default SharedSegments segments() { return null; }
  }
  @FunctionalInterface private interface IoAction<T> { T run() throws IOException; }

  public static SharedArrayPool open(Path directory, long maxBytes, int maxEntries, int maxCachedViews)
      throws IOException {
    Objects.requireNonNull(directory, "directory");
    if (maxBytes < 64 || maxEntries < 0 || maxEntries > 16_777_216 || maxCachedViews < 0) {
      throw new IllegalArgumentException("invalid storage budget");
    }
    Files.createDirectories(directory);
    Path root = directory.toRealPath();
    synchronized (JVM_GATE) {
      FileChannel lock = FileChannel.open(root.resolve("arena.lock"),
          StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      FileChannel content = null;
      try {
        content = FileChannel.open(root.resolve("arena.bin"), StandardOpenOption.CREATE,
            StandardOpenOption.READ, StandardOpenOption.WRITE);
        return new SharedArrayPool(content, lock, maxBytes, maxEntries, maxCachedViews);
      } catch (IOException | RuntimeException | Error failure) {
        if (content != null) try { content.close(); } catch (IOException e) { failure.addSuppressed(e); }
        try { lock.close(); } catch (IOException e) { failure.addSuppressed(e); }
        throw failure;
      }
    }
  }
  private SharedArrayPool(FileChannel data, FileChannel gate, long maxBytes, int maxEntries,
                          int maxCachedViews) throws IOException {
    this.data = data; this.gate = gate; this.maxBytes = maxBytes;
    this.maxEntries = maxEntries; this.maxCachedViews = maxCachedViews;
    this.mappings = new Mapping[Math.min(16, maxEntries)];
    try (FileLock lock = gate.lock()) {
      if (!lock.isValid()) throw new IOException("invalid lock");
      ByteBuffer h = ByteBuffer.allocate(64);
      if (data.size() == 0) {
        UUID id = UUID.randomUUID();
        h.putLong(0, MAGIC).putInt(8, 1).putLong(16, id.getMostSignificantBits())
            .putLong(24, id.getLeastSignificantBits()).putLong(32, maxBytes).putInt(40, maxEntries);
        CRC32C crc = new CRC32C(); crc.update(h.array(), 0, 56); h.putLong(56, crc.getValue());
        write(h, 0); data.force(true);
      }
      h.clear(); read(h, 0);
      CRC32C crc = new CRC32C(); crc.update(h.array(), 0, 56);
      if (h.getLong(0) != MAGIC || h.getInt(8) != 1 || h.getLong(56) != crc.getValue()
          || h.getLong(32) != maxBytes || h.getInt(40) != maxEntries) {
        throw new IOException("arena header/version/budget mismatch");
      }
      namespace = new UUID(h.getLong(16), h.getLong(24));
      refresh();
    }
  }
  public UUID namespace() { return namespace; }

  /**
   * Copies bounded literal-record geometry without touching payload bytes.
   *
   * <p>The returned long packs the next source row in the high 32 bits and the number of emitted
   * literal rows in the low 32 bits. Descriptor rows are skipped. Offsets are 64-bit file
   * coordinates into arena.bin, not Java-array offsets or native pointers.</p>
   */
  public long copyLiteralGeometryPage(
      int startRow,
      int maxRows,
      long[] payloadOffsets,
      int[] payloadByteLengths,
      int[] widths,
      int[] languageIds,
      int[] sourceRows)
      throws IOException {
    Objects.requireNonNull(payloadOffsets, "payloadOffsets");
    Objects.requireNonNull(payloadByteLengths, "payloadByteLengths");
    Objects.requireNonNull(widths, "widths");
    Objects.requireNonNull(languageIds, "languageIds");
    Objects.requireNonNull(sourceRows, "sourceRows");
    if (maxRows < 1
        || maxRows > payloadOffsets.length
        || maxRows > payloadByteLengths.length
        || maxRows > widths.length
        || maxRows > languageIds.length
        || maxRows > sourceRows.length) {
      throw new IllegalArgumentException("literal geometry output capacity");
    }

    return locked(
        () -> {
          if (startRow < 0 || startRow > count) {
            throw new IndexOutOfBoundsException("startRow=" + startRow + " count=" + count);
          }
          int row = startRow;
          int written = 0;
          while (row < count && written < maxRows) {
            if (parts[row] < 0) {
              payloadOffsets[written] = Math.addExact(positions[row], HEADER);
              payloadByteLengths[written] = Math.multiplyExact(kinds[row], lengths[row]);
              widths[written] = kinds[row];
              languageIds[written] = this.languages[row];
              sourceRows[written] = row;
              written++;
            }
            row++;
          }
          return packGeometryPage(row, written);
        });
  }

  public static int geometryNextRow(long packedPage) {
    return (int) (packedPage >>> Integer.SIZE);
  }

  public static int geometryCount(long packedPage) {
    return (int) packedPage;
  }

  private static long packGeometryPage(int nextRow, int count) {
    return ((long) nextRow << Integer.SIZE) | Integer.toUnsignedLong(count);
  }
  private <T> T locked(IoAction<T> action) throws IOException {
    synchronized (JVM_GATE) {
      ensureOpen();
      drainMappings();
      try (FileLock lock = gate.lock()) {
        if (!lock.isValid()) throw new IOException("invalid lock");
        refresh();
        return action.run();
      }
    }
  }
  private void ensureOpen() { if (closed) throw new IllegalStateException("pool closed"); }
  /** Called under JVM_GATE; release cleared optional wrappers without a whole-row sweep. */
  private void drainMappings() {
    Mapping cleared;
    while ((cleared = (Mapping) mappingQueue.poll()) != null) {
      // A delayed queue entry must not erase a more recently mapped owner for this row.
      if (cleared.row < mappings.length && mappings[cleared.row] == cleared) mappings[cleared.row] = null;
    }
  }

  public SharedBytes internBytes(byte[] source) throws IOException {
    Objects.requireNonNull(source, "source");
    return (SharedBytes) intern(new Input() {
      public int width() { return 1; } public int language() { return 0; }
      public int length() { return source.length; } public int at(int i) { return source[i] & 255; }
    });
  }
  /**
   * Admits the remaining range of a heap or direct buffer, including JNI-created buffers.
   * Neither the source cursor nor its mark is changed. The source must remain valid and stable
   * until this method returns. A miss copies once to the shared arena, never to a heap byte array.
   */
  public SharedBytes internBytes(ByteBuffer source) throws IOException {
    ByteBuffer input = Objects.requireNonNull(source, "source").asReadOnlyBuffer();
    int start = input.position(), size = input.remaining();
    return (SharedBytes) intern(new Input() {
      public int width() { return 1; } public int language() { return 0; }
      public int length() { return size; } public int at(int i) { return input.get(start + i) & 255; }
    });
  }
  /** Same-namespace immutable ranges are attached by descriptor; foreign content is imported. */
  public SharedBytes internBytes(SharedBytes source) throws IOException {
    Objects.requireNonNull(source, "source");
    if (namespace.equals(source.handle.namespace())) return (SharedBytes) join(1, 0, new SharedArray[] {source});
    return (SharedBytes) intern(new Input() {
      public int width() { return 1; } public int language() { return 0; }
      public int length() { return source.length(); } public int at(int i) { return source.byteAt(i) & 255; }
    });
  }
  public SharedChars internChars(int languageId, char[] source) throws IOException {
    return internChars(languageId, CharBuffer.wrap(Objects.requireNonNull(source, "source")));
  }
  public SharedChars internChars(int languageId, CharSequence source) throws IOException {
    if (source instanceof SharedChars shared && namespace.equals(shared.handle.namespace())) {
      return (SharedChars) join(2, languageId, new SharedArray[] {shared});
    }
    return (SharedChars) intern(charInput(languageId, source));
  }
  private static Input charInput(int languageId, CharSequence source) {
    Objects.requireNonNull(source, "source");
    return new Input() {
      public int width() { return 2; } public int language() { return languageId; }
      public int length() { return source.length(); } public int at(int i) { return source.charAt(i); }
    };
  }
  public OptionalLong lookupChars(int languageId, CharSequence source) throws IOException {
    Input input = charInput(languageId, source);
    return locked(() -> {
      int row = find(input, fingerprint(input));
      return row < 0 ? OptionalLong.empty() : OptionalLong.of(positions[row]);
    });
  }
  public SharedChars concatChars(int languageId, SharedChars... values) throws IOException {
    return (SharedChars) join(2, languageId, values);
  }
  public SharedBytes concatBytes(SharedBytes... values) throws IOException {
    return (SharedBytes) join(1, 0, values);
  }
  private SharedArray join(int width, int languageId, SharedArray[] values) throws IOException {
    Objects.requireNonNull(values, "values");
    for (SharedArray value : values) {
      Objects.requireNonNull(value, "value");
      if (!namespace.equals(value.handle.namespace())) throw new IllegalArgumentException("foreign namespace");
    }
    SharedSegments joined = SharedSegments.join(width, values);
    return intern(new Input() {
      public int width() { return width; } public int language() { return languageId; }
      public int length() { return joined.length(); } public int at(int i) { return joined.unitAt(i); }
      public SharedSegments segments() { return joined; }
    });
  }
  /** Charset boundary; JDK encoder sees ONE logical sequence, never separate surrogate halves. */
  public SharedBytes encode(SharedChars source, Charset charset) throws IOException {
    Objects.requireNonNull(source, "source"); Objects.requireNonNull(charset, "charset");
    ByteBuffer encoded = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE).encode(CharBuffer.wrap(source));
    return (SharedBytes) intern(new Input() {
      public int width() { return 1; } public int language() { return 0; }
      public int length() { return encoded.remaining(); }
      public int at(int i) { return encoded.get(encoded.position() + i) & 255; }
    });
  }
  /** Explicit decoding boundary; flattening here is intentional and never confused with joining. */
  public SharedChars decode(int languageId, SharedBytes source, Charset charset) throws IOException {
    Objects.requireNonNull(source, "source"); Objects.requireNonNull(charset, "charset");
    return internChars(languageId, charset.decode(ByteBuffer.wrap(source.copy())));
  }
  public SharedChars chars(long recordOffset) throws IOException { return (SharedChars) get(recordOffset, 2); }
  public SharedBytes bytes(long recordOffset) throws IOException { return (SharedBytes) get(recordOffset, 1); }
  public SharedChars chars(ArrayHandle handle) throws IOException {
    requireNamespace(handle);
    return chars(handle.recordOffset()).subSequence(handle.start(), Math.addExact(handle.start(), handle.length()));
  }
  public SharedBytes bytes(ArrayHandle handle) throws IOException {
    requireNamespace(handle);
    return bytes(handle.recordOffset()).slice(handle.start(), Math.addExact(handle.start(), handle.length()));
  }
  private void requireNamespace(ArrayHandle handle) {
    Objects.requireNonNull(handle, "handle");
    if (!namespace.equals(handle.namespace())) throw new IllegalArgumentException("foreign namespace");
  }
  private SharedArray get(long position, int width) throws IOException {
    synchronized (JVM_GATE) {
      ensureOpen();
      drainMappings();
      int row = Arrays.binarySearch(positions, 0, count, position);
      if (row < 0) return locked(() -> getKnown(position, width));
      if (kinds[row] != width) throw new IllegalArgumentException("wrong array kind");
      return view(row);
    }
  }
  private SharedArray getKnown(long position, int width) throws IOException {
    int row = Arrays.binarySearch(positions, 0, count, position);
    if (row < 0 || kinds[row] != width) throw new IllegalArgumentException("unknown record or wrong kind");
    return view(row);
  }
  public Stats stats() throws IOException {
    return locked(() -> new Stats(count, literalBytes, descriptorBytes, scanned, cache.size()));
  }
  /** Number of admissions resolved by exact descriptor-tuple lookup before content scanning. */
  public long descriptorIndexHits() throws IOException {
    return locked(() -> descriptorIndexHits);
  }
  private SharedArray intern(Input input) throws IOException {
    return locked(() -> {
      SharedSegments segments = input.segments();
      if (segments != null) {
        int descriptorRow = findDescriptor(input.width(), input.language(), segments);
        if (descriptorRow >= 0) {
          descriptorIndexHits++;
          return view(descriptorRow);
        }
      }

      Fingerprint fingerprint = fingerprint(input);
      int found = find(input, fingerprint);
      if (found >= 0) return view(found);
      int n = segments == null ? -1 : segments.data.length;
      long payloadSize = n < 0 ? (long) input.width() * input.length() : (long) n * 16;
      long total = HEADER + payloadSize + 8;
      if (payloadSize > MAX_PAYLOAD) throw new IllegalArgumentException("single-record payload limit exceeded");
      if (count >= maxEntries || total > maxBytes - scanned) throw new IllegalStateException("arena budget exhausted");
      ByteBuffer header = ByteBuffer.allocate(HEADER);
      header.putInt(0, RECORD_MAGIC).putInt(4, (int) total).putInt(8, input.width())
          .putInt(12, input.language()).putInt(16, input.length()).putInt(20, n)
          .putInt(24, fingerprint.javaHash()).putInt(28, (int) payloadSize);
      header.position(32); header.put(fingerprint.sha()); header.clear();
      CRC32C crc = new CRC32C(); crc.update(header.array(), 0, 64);
      long position = scanned;
      write(header, position);
      long at = position + HEADER;
      scratch.clear();
      if (n < 0) {
        for (int i = 0; i < input.length(); i++) {
          if (scratch.remaining() < input.width()) at = flushPayload(at, crc);
          int unit = input.at(i);
          if (input.width() == 2) scratch.put((byte) (unit >>> 8));
          scratch.put((byte) unit);
        }
      } else {
        for (int i = 0; i < n; i++) {
          if (scratch.remaining() < 16) at = flushPayload(at, crc);
          scratch.putLong(segments.ids[i]).putInt(segments.offsets[i]).putInt(segments.lengths[i]);
        }
      }
      flushPayload(at, crc);
      ByteBuffer checksum = ByteBuffer.allocate(8).putLong(crc.getValue()); checksum.flip();
      write(checksum, position + 64);
      data.force(false);
      ByteBuffer trailer = ByteBuffer.allocate(8).putLong(COMMIT ^ position ^ total); trailer.flip();
      write(trailer, position + total - 8);
      data.force(true);
      refresh();
      return view(count - 1);
    });
  }
  private long flushPayload(long position, CRC32C crc) throws IOException {
    int bytes = scratch.position();
    crc.update(scratch.array(), 0, bytes); scratch.flip(); write(scratch, position); scratch.clear();
    return position + bytes;
  }
  private static Fingerprint fingerprint(Input input) {
    MessageDigest digest;
    try { digest = MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    ByteBuffer domain = ByteBuffer.allocate(8).putInt(input.width()).putInt(input.language());
    digest.update(domain.array());
    int hash = input.width() == 1 ? 1 : 0;
    for (int i = 0; i < input.length(); i++) {
      int unit = input.at(i);
      if (input.width() == 2) digest.update((byte) (unit >>> 8));
      digest.update((byte) unit);
      hash = 31 * hash + (input.width() == 1 ? (byte) unit : unit);
    }
    byte[] sha = digest.digest();
    return new Fingerprint(sha, ByteBuffer.wrap(sha).getLong(), hash);
  }
  private int find(Input input, Fingerprint f) throws IOException {
    int slot = slot(f.signature(), slots.length);
    while (slots[slot] != 0) {
      int row = slots[slot] - 1;
      if (signatures[row] == f.signature() && kinds[row] == input.width()
          && languages[row] == input.language() && lengths[row] == input.length()
          && hashes[row] == f.javaHash()) {
        SharedArray candidate = view(row);
        boolean equal = true;
        for (int i = 0; i < input.length(); i++) {
          if (input.at(i) != candidate.segments.unitAt(i)) { equal = false; break; }
        }
        if (equal) return row;
      }
      slot = (slot + 1) & (slots.length - 1);
    }
    return -1;
  }
  private int findDescriptor(
      int width, int language, SharedSegments segments) throws IOException {
    if (descriptorCount == 0) return -1;

    long signature = descriptorSignature(width, language, segments);
    int slot = slot(signature, descriptorSlots.length);
    while (descriptorSlots[slot] != 0) {
      int row = descriptorSlots[slot] - 1;
      if (descriptorSignatures[row] == signature
          && kinds[row] == width
          && languages[row] == language
          && parts[row] == segments.data.length
          && descriptorEquals(row, segments)) {
        return row;
      }
      slot = (slot + 1) & (descriptorSlots.length - 1);
    }
    return -1;
  }

  private boolean descriptorEquals(int row, SharedSegments segments) throws IOException {
    long position = positions[row] + HEADER;
    for (int index = 0; index < segments.data.length; index++) {
      descriptorScratch.clear();
      read(descriptorScratch, position + (long) index * 16);
      if (descriptorScratch.getLong(0) != segments.ids[index]
          || descriptorScratch.getInt(8) != segments.offsets[index]
          || descriptorScratch.getInt(12) != segments.lengths[index]) {
        return false;
      }
    }
    return true;
  }

  private static long descriptorSignature(
      int width, int language, SharedSegments segments) {
    long signature = descriptorSignatureStart(width, language);
    for (int index = 0; index < segments.data.length; index++) {
      signature =
          descriptorSignatureAdd(
              signature,
              segments.ids[index],
              segments.offsets[index],
              segments.lengths[index]);
    }
    return signature;
  }

  private static long descriptorSignatureStart(int width, int language) {
    return mix64(
        0x9e3779b97f4a7c15L
            ^ ((long) width << Integer.SIZE)
            ^ Integer.toUnsignedLong(language));
  }

  private static long descriptorSignatureAdd(
      long signature, long record, int offset, int length) {
    long mixed = mix64(record);
    mixed ^= Long.rotateLeft(Integer.toUnsignedLong(offset), 17);
    mixed ^= Long.rotateLeft(Integer.toUnsignedLong(length), 41);
    return mix64(signature ^ mixed);
  }

  private static long mix64(long value) {
    long mixed = value;
    mixed ^= mixed >>> 30;
    mixed *= 0xbf58476d1ce4e5b9L;
    mixed ^= mixed >>> 27;
    mixed *= 0x94d049bb133111ebL;
    return mixed ^ (mixed >>> 31);
  }
  private static int slot(long hash, int capacity) {
    hash ^= hash >>> 33; hash *= 0xff51afd7ed558ccdL; hash ^= hash >>> 33;
    return (int) hash & (capacity - 1);
  }
  private void add(long position, ByteBuffer h, long descriptorSignature) {
    if (count == positions.length) {
      int next = Math.multiplyExact(count, 2);
      positions = Arrays.copyOf(positions, next);
      mappings = Arrays.copyOf(mappings, Math.min(next, maxEntries));
      signatures = Arrays.copyOf(signatures, next);
      descriptorSignatures = Arrays.copyOf(descriptorSignatures, next);
      kinds = Arrays.copyOf(kinds, next);
      languages = Arrays.copyOf(languages, next);
      lengths = Arrays.copyOf(lengths, next);
      hashes = Arrays.copyOf(hashes, next);
      parts = Arrays.copyOf(parts, next);
    }

    int row = count;
    positions[row] = position;
    signatures[row] = h.getLong(32);
    descriptorSignatures[row] = descriptorSignature;
    kinds[row] = h.getInt(8);
    languages[row] = h.getInt(12);
    lengths[row] = h.getInt(16);
    parts[row] = h.getInt(20);
    hashes[row] = h.getInt(24);
    count++;

    if (count * 2 >= slots.length) {
      slots = new int[Math.multiplyExact(slots.length, 2)];
      for (int index = 0; index < count; index++) insertSlot(index);
    } else {
      insertSlot(row);
    }

    if (parts[row] < 0) {
      literalBytes += h.getInt(28);
      return;
    }

    descriptorBytes += h.getInt(28);
    descriptorCount++;
    if (descriptorCount * 2 >= descriptorSlots.length) {
      descriptorSlots = new int[Math.multiplyExact(descriptorSlots.length, 2)];
      for (int index = 0; index < count; index++) {
        if (parts[index] >= 0) insertDescriptorSlot(index);
      }
    } else {
      insertDescriptorSlot(row);
    }
  }

  private void insertSlot(int row) {
    int slot = slot(signatures[row], slots.length);
    while (slots[slot] != 0) slot = (slot + 1) & (slots.length - 1);
    slots[slot] = row + 1;
  }

  private void insertDescriptorSlot(int row) {
    int slot = slot(descriptorSignatures[row], descriptorSlots.length);
    while (descriptorSlots[slot] != 0) {
      slot = (slot + 1) & (descriptorSlots.length - 1);
    }
    descriptorSlots[slot] = row + 1;
  }
  /** Called only with both JVM and process writer locks held. */
  private void refresh() throws IOException {
    long end = data.size();
    if (end < scanned || end > maxBytes) throw new IOException("arena size/ownership violation");
    while (scanned < end) {
      if (end - scanned < HEADER) { data.truncate(scanned); data.force(true); return; }
      ByteBuffer h = ByteBuffer.allocate(HEADER); read(h, scanned);
      int total = h.getInt(4), width = h.getInt(8), units = h.getInt(16);
      int n = h.getInt(20), payload = h.getInt(28);
      long expected = n < 0 ? (long) width * units : (long) n * 16;
      if (h.getInt(0) != RECORD_MAGIC || (width != 1 && width != 2) || units < 0
          || n < -1 || n > SharedSegments.MAX_SEGMENTS || payload < 0 || payload > MAX_PAYLOAD
          || expected != payload || (long) HEADER + payload + 8 != total || h.getLong(72) != 0
          || (width == 1 && h.getInt(12) != 0)) throw new IOException("invalid arena record at " + scanned);
      if (end - scanned < total) { data.truncate(scanned); data.force(true); return; }
      ByteBuffer trailer = ByteBuffer.allocate(8); read(trailer, scanned + total - 8);
      if (trailer.getLong(0) != (COMMIT ^ scanned ^ total)) throw new IOException("invalid commit trailer");
      CRC32C crc = new CRC32C(); crc.update(h.array(), 0, 64);
      long at = scanned + HEADER;
      int remaining = payload;
      while (remaining > 0) {
        int bytes = Math.min(remaining, scratch.capacity()); scratch.clear().limit(bytes);
        read(scratch, at); crc.update(scratch.array(), 0, bytes); at += bytes; remaining -= bytes;
      }
      if (crc.getValue() != h.getLong(64)) throw new IOException("record checksum mismatch");
      long descriptorSignature =
          n >= 0
              ? validateDescriptors(
                  scanned + HEADER, n, width, h.getInt(12), units)
              : 0L;
      if (count >= maxEntries) throw new IOException("entry budget violation");
      add(scanned, h, descriptorSignature);
      scanned += total;
    }
  }

  private long validateDescriptors(
      long position, int n, int width, int language, int units) throws IOException {
    long sum = 0;
    long signature = descriptorSignatureStart(width, language);
    for (int i = 0; i < n; i++) {
      descriptorScratch.clear();
      read(descriptorScratch, position + (long) i * 16);
      long record = descriptorScratch.getLong(0);
      int row = Arrays.binarySearch(positions, 0, count, record);
      int from = descriptorScratch.getInt(8);
      int length = descriptorScratch.getInt(12);
      if (row < 0 || kinds[row] != width || parts[row] != -1 || from < 0 || length <= 0
          || (long) from + length > lengths[row]) {
        throw new IOException("invalid literal reference");
      }
      sum += length;
      signature = descriptorSignatureAdd(signature, record, from, length);
    }
    if (sum != units) throw new IOException("joined length mismatch");
    return signature;
  }
  private SharedArray view(int row) throws IOException {
    SharedArray hit = cache.get(row);
    if (hit != null) return hit;
    int width = kinds[row], n = parts[row];
    SharedSegments segments;
    if (n < 0) {
      int size = lengths[row];
      ByteBuffer[] buffers;
      if (size == 0) buffers = new ByteBuffer[0];
      else {
        Mapping reference = mappings[row];
        // Promote once to a strong local; a weak hit alone is not a lifetime guarantee.
        ByteBuffer owner = reference == null ? null : reference.get();
        if (owner == null) {
          owner = data.map(FileChannel.MapMode.READ_ONLY, positions[row] + HEADER, (long) size * width);
          mappings[row] = new Mapping(row, owner, mappingQueue);
        }
        buffers = new ByteBuffer[] {owner};
      }
      segments = new SharedSegments(width, buffers, size == 0 ? new long[0] : new long[] {positions[row]},
          size == 0 ? new int[0] : new int[] {0}, size == 0 ? new int[0] : new int[] {size});
    } else {
      ByteBuffer[] buffers = new ByteBuffer[n]; long[] ids = new long[n];
      int[] offsets = new int[n], sizes = new int[n]; ByteBuffer descriptor = ByteBuffer.allocate(16);
      for (int i = 0; i < n; i++) {
        descriptor.clear(); read(descriptor, positions[row] + HEADER + (long) i * 16);
        ids[i] = descriptor.getLong(0); offsets[i] = descriptor.getInt(8); sizes[i] = descriptor.getInt(12);
        int leaf = Arrays.binarySearch(positions, 0, count, ids[i]);
        buffers[i] = view(leaf).segments.data[0];
      }
      segments = new SharedSegments(width, buffers, ids, offsets, sizes);
    }
    ArrayHandle handle = new ArrayHandle(namespace, positions[row], 0, lengths[row]);
    SharedArray value = width == 1 ? new SharedBytes(handle, segments, hashes[row])
        : new SharedChars(handle, segments, languages[row], hashes[row]);
    if (maxCachedViews > 0) {
      cache.put(row, value);
      while (cache.size() > maxCachedViews) cache.pollFirstEntry();
    }
    return value;
  }
  private void read(ByteBuffer target, long position) throws IOException {
    while (target.hasRemaining()) {
      int n = data.read(target, position);
      if (n < 0) throw new EOFException("truncated arena");
      if (n == 0) throw new IOException("no progress reading arena");
      position += n;
    }
    target.flip();
  }
  private void write(ByteBuffer source, long position) throws IOException {
    while (source.hasRemaining()) {
      int n = data.write(source, position);
      if (n <= 0) throw new IOException("no progress writing arena");
      position += n;
    }
  }
  @Override public void close() throws IOException {
    synchronized (JVM_GATE) {
      if (closed) return;
      closed = true; cache.clear();
      mappings = null; mappingQueue = null;
      try { data.close(); } finally { gate.close(); }
    }
  }
}
