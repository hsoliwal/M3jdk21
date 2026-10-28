// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Typed Java facade over the native SQLite-backed M3FS.
 *
 * <p>Filesystem semantics remain native: this class owns lifetime, Java shapes and buffer
 * admission only. It issues no SQL and does not duplicate inode/block transaction logic.</p>
 */
public final class M3FS implements AutoCloseable {
  private static final Cleaner CLEANER = Cleaner.create();

  /** Maximum entries admitted by one bounded native directory-page request. */
  public static final int MAX_PAGE_ENTRIES = M3FsNative.MAX_PAGE_ENTRIES;

  public static final int WRITE_APPEND = 1;
  public static final int WRITE_TRUNCATE = 2;
  private static final byte[] EMPTY_BYTES = new byte[0];

  /** Immutable array-backed page; row objects are materialized only through the API view. */
  public static final class DirectoryPage {
    private final M3DirectoryTable table;
    private final boolean hasMore;

    public DirectoryPage(List<DirEntry> entries, boolean hasMore) {
      this(M3DirectoryTable.fromEntries(entries), hasMore);
    }

    DirectoryPage(M3DirectoryTable table, boolean hasMore) {
      this.table = Objects.requireNonNull(table, "table");
      this.hasMore = hasMore;
      if (table.size() > MAX_PAGE_ENTRIES || (table.size() == 0 && hasMore)) {
        throw new IllegalArgumentException("invalid directory page");
      }
    }

    public M3DirectoryTable table() { return table; }
    public List<DirEntry> entries() { return table.entries(); }
    public boolean hasMore() { return hasMore; }
    public String nextAfter() { return table.size() == 0 ? null : table.name(table.size() - 1); }
    @Override public boolean equals(Object other) {
      return other instanceof DirectoryPage page && hasMore == page.hasMore
          && table.equals(page.table);
    }
    @Override public int hashCode() { return 31 * table.entriesHashCode() + Boolean.hashCode(hasMore); }
    @Override public String toString() {
      return "DirectoryPage[entries=" + entries() + ", hasMore=" + hasMore + "]";
    }
  }

  public record AllocatedRange(long offset, long length) {
    public AllocatedRange {
      requireRange(offset, length);
      if (length == 0) throw new IllegalArgumentException("empty allocated range");
    }
  }

  public enum Kind {
    FILE,
    DIRECTORY,
    SYMLINK;

    static Kind fromNative(long value) {
      return switch (Math.toIntExact(value)) {
        case 1 -> FILE;
        case 2 -> DIRECTORY;
        case 3 -> SYMLINK;
        default -> throw new IllegalStateException("unknown native inode kind " + value);
      };
    }
  }

  public record Stat(
      long inode,
      long size,
      long generation,
      long modifiedTimeNanos,
      Kind kind,
      int mode,
      int linkCount,
      int blockSize) {
    public Stat {
      if (inode <= 0L || size < 0L || generation < 1L || modifiedTimeNanos < 0L
          || mode < 0 || linkCount < 0 || blockSize <= 0) {
        throw new IllegalArgumentException("invalid M3FS stat");
      }
      kind = Objects.requireNonNull(kind, "kind");
    }
  }

  public record DirEntry(String name, Stat stat) {
    public DirEntry {
      name = Objects.requireNonNull(name, "name");
      stat = Objects.requireNonNull(stat, "stat");
      if (name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf('\0') >= 0) {
        throw new IllegalArgumentException("invalid M3FS directory entry name");
      }
    }
  }

  public record Stats(
      long inodeCount,
      long direntCount,
      long logicalBytes,
      long uniqueBlocks,
      long blockReferences,
      long physicalBlockBytes) {
    public Stats {
      if (inodeCount < 0L || direntCount < 0L || logicalBytes < 0L || uniqueBlocks < 0L
          || blockReferences < 0L || physicalBlockBytes < 0L) {
        throw new IllegalArgumentException("negative M3FS statistics");
      }
    }

    public long deduplicatedBlockBytes() {
      long referenced = Math.multiplyExact(blockReferences, 65_536L);
      return Math.max(0L, referenced - physicalBlockBytes);
    }
  }

  /** Persistent CRC32C integrity coverage for the unique deduplicated block table. */
  public record ScrubReport(
      long blocksScanned,
      long checksumsVerified,
      long checksumsMissing,
      long checksumsMismatched,
      long checksumsBackfilled) {
    public ScrubReport {
      long classified =
          Math.addExact(
              Math.addExact(checksumsVerified, checksumsMissing),
              checksumsMismatched);
      if (blocksScanned < 0L || checksumsVerified < 0L || checksumsMissing < 0L
          || checksumsMismatched < 0L || checksumsBackfilled < 0L
          || classified != blocksScanned
          || checksumsBackfilled > checksumsMissing) {
        throw new IllegalArgumentException("invalid M3FS scrub report");
      }
    }

    /** True when every scanned block had a checksum and every stored checksum matched. */
    public boolean cleanAndComplete() {
      return checksumsMissing == 0L && checksumsMismatched == 0L;
    }

    /** True when no persisted checksum disagreed with the current block bytes. */
    public boolean noMismatch() {
      return checksumsMismatched == 0L;
    }

    /** Coverage after an optional backfill operation. */
    public long checksumsCoveredAfterRun() {
      return Math.addExact(checksumsVerified, checksumsBackfilled);
    }
  }

  public record MIndexInfo(
      int valueCount,
      int tokenCount,
      int utf16Count,
      int imageBytes) {
    public MIndexInfo {
      if (valueCount < 0 || tokenCount < 0 || utf16Count < 0 || imageBytes < 64) {
        throw new IllegalArgumentException("invalid native MIndex image info");
      }
    }
  }

  public record MIndexValueInfo(
      int tokenCount,
      int utf16Length,
      int javaHash,
      long signal64) {
    public MIndexValueInfo {
      if (tokenCount < 0 || utf16Length < 0) {
        throw new IllegalArgumentException("invalid native MIndex value info");
      }
    }
  }

  public enum RegexMode {
    MATCHES(0),
    FIND(1),
    LOOKING_AT(2);

    private final int nativeCode;

    RegexMode(int nativeCode) {
      this.nativeCode = nativeCode;
    }
  }

  public record NativeRegexResult(
      int valueCount,
      long[] matches,
      long[] unsupported) {
    public NativeRegexResult {
      if (valueCount < 0) throw new IllegalArgumentException("valueCount");
      matches = Objects.requireNonNull(matches, "matches").clone();
      unsupported = Objects.requireNonNull(unsupported, "unsupported").clone();
      int words = (valueCount + 63) >>> 6;
      if (matches.length != words || unsupported.length != words) {
        throw new IllegalArgumentException("native regex bitmap geometry");
      }
    }

    @Override
    public long[] matches() {
      return matches.clone();
    }

    @Override
    public long[] unsupported() {
      return unsupported.clone();
    }

    public boolean matches(int ordinal) {
      int checked = Objects.checkIndex(ordinal, valueCount);
      return (matches[checked >>> 6] & (1L << (checked & 63))) != 0L;
    }

    public boolean requiresFallback(int ordinal) {
      int checked = Objects.checkIndex(ordinal, valueCount);
      return (unsupported[checked >>> 6] & (1L << (checked & 63))) != 0L;
    }

    public int matchCount() {
      return Math.toIntExact(Arrays.stream(matches).map(Long::bitCount).sum());
    }

    public int fallbackCount() {
      return Math.toIntExact(Arrays.stream(unsupported).map(Long::bitCount).sum());
    }
  }

  private static final class NativeState implements Runnable {
    private final AtomicLong handle;

    NativeState(long handle) {
      this.handle = new AtomicLong(handle);
    }

    long requireHandle() {
      long value = handle.get();
      if (value == 0L) throw new IllegalStateException("M3FS is closed");
      return value;
    }

    @Override
    public void run() {
      long closing = handle.getAndSet(0L);
      if (closing != 0L) M3FsNative.close(closing);
    }
  }

  private final Path database;
  private final boolean readOnly;
  private final boolean snapshot;
  private final NativeState state;
  private final Cleaner.Cleanable cleanable;

  private M3FS(Path database, boolean readOnly, long handle) {
    this(database, readOnly, false, handle);
  }

  private M3FS(Path database, boolean readOnly, boolean snapshot, long handle) {
    this.database = database.toAbsolutePath().normalize();
    this.readOnly = readOnly;
    this.snapshot = snapshot;
    this.state = new NativeState(handle);
    this.cleanable = CLEANER.register(this, state);
  }

  public static boolean nativeAvailable() {
    return M3FsNative.available();
  }

  /** Whether the loaded ABI-1 library supports explicit shared-block cloning. */
  public static boolean supportsSharedBlockClone() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_CLONE_FILE) != 0;
  }

  /** Whether the loaded ABI-1 library supports bounded directory pages. */
  public static boolean supportsDirectoryPaging() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_LIST_PAGE) != 0;
  }

  /** Whether the loaded ABI-1 library supports read views retained until close. */
  public static boolean supportsReadSnapshots() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_READ_SNAPSHOT) != 0;
  }

  /** Whether the loaded ABI-1 library supports one-transaction scatter/gather writes. */
  public static boolean supportsScatterGatherWrite() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_WRITEV) != 0;
  }

  /** Whether the loaded ABI-1 library supports scatter/gather direct-buffer reads. */
  public static boolean supportsScatterGatherRead() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_READV) != 0;
  }

  /** Whether the loaded ABI-1 library supports persistent block CRC32C scrub metadata. */
  public static boolean supportsBlockScrub() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_BLOCK_SCRUB) != 0;
  }

  /** Whether the native filesystem can interpret persisted MIndexString images directly. */
  public static boolean supportsNativeMIndex() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_NATIVE_MINDEX) != 0;
  }

  /** Whether the native filesystem can execute compiled M3FsRegexImage images. */
  public static boolean supportsNativeRegex() {
    return (M3FsNative.capabilities() & M3FsNative.CAP_NATIVE_REGEX) != 0;
  }

  public static M3FS create(Path database) throws IOException {
    Path path = Objects.requireNonNull(database, "database").toAbsolutePath().normalize();
    long handle = M3FsNative.open(path.toString(), M3FsNative.OPEN_CREATE);
    return new M3FS(path, false, handle);
  }

  public static M3FS open(Path database) throws IOException {
    Path path = Objects.requireNonNull(database, "database").toAbsolutePath().normalize();
    long handle = M3FsNative.open(path.toString(), 0);
    return new M3FS(path, false, handle);
  }

  public static M3FS openReadOnly(Path database) throws IOException {
    Path path = Objects.requireNonNull(database, "database").toAbsolutePath().normalize();
    long handle = M3FsNative.open(path.toString(), M3FsNative.OPEN_READ_ONLY);
    return new M3FS(path, true, handle);
  }

  /**
   * Open a read-only SQLite view pinned before this method returns, not on the first user read.
   *
   * <p>All reads, metadata and directory pages share that view until {@link #close()}. Other
   * handles may keep writing. Failed lookups and rejected mutations preserve the view; unexpected
   * transaction loss fails rather than silently refreshing. A long-lived view can retain WAL
   * history: close explicitly. This is not a durable backup or a mapped primitive-array image.
   * Never replace database/WAL/SHM files under open handles.</p>
   *
   * @throws UnsupportedOperationException if the loaded ABI-1 library lacks this capability
   * @throws IOException if the database cannot be opened or its read view cannot be established
   */
  public static M3FS openSnapshot(Path database) throws IOException {
    Path path = Objects.requireNonNull(database, "database").toAbsolutePath().normalize();
    long handle = M3FsNative.open(path.toString(),
        M3FsNative.OPEN_READ_ONLY | M3FsNative.OPEN_READ_SNAPSHOT);
    return new M3FS(path, true, true, handle);
  }

  public Path database() {
    return database;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  /** Whether this handle was opened with one retained read view rather than live reads. */
  public boolean isSnapshot() {
    return snapshot;
  }

  public synchronized boolean isOpen() {
    try {
      state.requireHandle();
      return true;
    } catch (IllegalStateException closed) {
      return false;
    }
  }

  public synchronized void mkdir(String path, int mode) throws IOException {
    requireMode(mode);
    M3FsNative.mkdir(state.requireHandle(), path(path), mode);
  }

  public synchronized void createFile(String path, int mode) throws IOException {
    requireMode(mode);
    M3FsNative.create(state.requireHandle(), path(path), mode);
  }

  public synchronized long write(String path, long offset, byte[] data) throws IOException {
    requireOffset(offset);
    byte[] checked = Objects.requireNonNull(data, "data");
    return M3FsNative.write(
        state.requireHandle(), path(path), offset, checked, 0, checked.length);
  }

  public synchronized long write(
      String path, long offset, byte[] data, int dataOffset, int length) throws IOException {
    requireOffset(offset);
    byte[] checked = Objects.requireNonNull(data, "data");
    Objects.checkFromIndexSize(dataOffset, length, checked.length);
    return M3FsNative.write(
        state.requireHandle(), path(path), offset, checked, dataOffset, length);
  }

  /**
   * Zero-copy Java/native boundary for direct buffers. The native store may still copy into its
   * fixed block scratch/SQLite pages.
   */
  public synchronized long write(String path, long offset, ByteBuffer source) throws IOException {
    requireOffset(offset);
    ByteBuffer checked = Objects.requireNonNull(source, "source");
    if (!checked.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
    int length = checked.remaining();
    long size =
        M3FsNative.writeDirect(
            state.requireHandle(), path(path), offset, checked, checked.position(), length);
    checked.position(Math.addExact(checked.position(), length));
    return size;
  }

  /**
   * Write the concatenation of direct-buffer remaining ranges as one filesystem transaction.
   *
   * <p>Caller buffer positions and limits are unchanged. Only descriptor/view objects are created;
   * payload bytes are not flattened at the Java/JNI boundary. Callers must not mutate source
   * contents until this synchronous method returns.</p>
   */
  public synchronized long writeSegments(String path, long offset, ByteBuffer... sources)
      throws IOException {
    requireOffset(offset);
    ByteBuffer[] checked = Objects.requireNonNull(sources, "sources");
    if (checked.length > M3FsNative.MAX_WRITE_SEGMENTS) {
      throw new IllegalArgumentException("scatter/gather segment budget");
    }
    ByteBuffer[] slices = new ByteBuffer[checked.length];
    for (int index = 0; index < checked.length; index++) {
      ByteBuffer source = Objects.requireNonNull(checked[index], "source");
      if (!source.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
      slices[index] = source.slice();
    }
    return M3FsNative.writeDirectSegments(
        state.requireHandle(), path(path), offset, slices);
  }

  public synchronized byte[] read(String path, long offset, int maximumBytes) throws IOException {
    requireOffset(offset);
    if (maximumBytes < 0) throw new IllegalArgumentException("maximumBytes");
    byte[] output = new byte[maximumBytes];
    int count =
        M3FsNative.read(
            state.requireHandle(), path(path), offset, output, 0, maximumBytes);
    return count == output.length ? output : Arrays.copyOf(output, count);
  }

  public synchronized int read(String path, long offset, byte[] output, int at, int length) throws IOException {
    requireOffset(offset);
    byte[] checked = Objects.requireNonNull(output, "output");
    Objects.checkFromIndexSize(at, length, checked.length);
    return M3FsNative.read(
        state.requireHandle(), path(path), offset, checked, at, length);
  }

  /** Read into a direct buffer and advance its position by the bytes actually read. */
  public synchronized int read(String path, long offset, ByteBuffer output) throws IOException {
    requireOffset(offset);
    ByteBuffer checked = Objects.requireNonNull(output, "output");
    if (!checked.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
    int count =
        M3FsNative.readDirect(
            state.requireHandle(),
            path(path),
            offset,
            checked,
            checked.position(),
            checked.remaining());
    checked.position(Math.addExact(checked.position(), count));
    return count;
  }

  /**
   * Fill the concatenation of direct-buffer remaining ranges under one native read view.
   *
   * <p>Caller positions and limits are unchanged. The return value is the total bytes filled.
   * A suffix after EOF is untouched. Payload bytes are never flattened at the Java/JNI boundary.</p>
   */
  public synchronized long readSegments(String path, long offset, ByteBuffer... outputs)
      throws IOException {
    requireOffset(offset);
    ByteBuffer[] checked = Objects.requireNonNull(outputs, "outputs");
    if (checked.length > M3FsNative.MAX_READ_SEGMENTS) {
      throw new IllegalArgumentException("scatter/gather segment budget");
    }
    ByteBuffer[] slices = new ByteBuffer[checked.length];
    for (int index = 0; index < checked.length; index++) {
      ByteBuffer output = Objects.requireNonNull(checked[index], "output");
      if (!output.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
      if (output.isReadOnly()) throw new java.nio.ReadOnlyBufferException();
      slices[index] = output.slice();
    }
    return M3FsNative.readDirectSegments(
        state.requireHandle(), path(path), offset, slices);
  }

  /**
   * Replace one existing regular file with an exact native MIndexString image.
   *
   * <p>The image remains an ordinary deduplicated M3FS file. Semantic identity is carried by
   * its token/UTF-16 lanes; no database schema extension or Java object pointer is persisted.</p>
   */
  public synchronized long writeMIndexImage(String path, M3FsIndexImage image)
      throws IOException {
    String checkedPath = path(path);
    M3FsIndexImage checked = Objects.requireNonNull(image, "image");
    return M3FsNative.replaceImage(state.requireHandle(), checkedPath, checked.image().slice());
  }

  /** Create a file and publish a native MIndex image; failed publication removes the new file. */
  public synchronized void createMIndexFile(
      String path,
      int mode,
      M3FsIndexImage image)
      throws IOException {
    String checkedPath = path(path);
    createFile(checkedPath, mode);
    try {
      writeMIndexImage(checkedPath, Objects.requireNonNull(image, "image"));
    } catch (IOException | RuntimeException failure) {
      try {
        unlink(checkedPath);
      } catch (IOException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  public synchronized MIndexInfo mindexInfo(String path) throws IOException {
    long[] values = M3FsNative.mindexInfo(state.requireHandle(), path(path));
    if (values.length != 4) throw new IllegalStateException("native MIndex info geometry");
    return new MIndexInfo(
        Math.toIntExact(values[0]),
        Math.toIntExact(values[1]),
        Math.toIntExact(values[2]),
        Math.toIntExact(values[3]));
  }

  public synchronized MIndexValueInfo mindexValueInfo(String path, int value) throws IOException {
    long[] values = M3FsNative.mindexValueInfo(state.requireHandle(), path(path), value);
    if (values.length != 4) throw new IllegalStateException("native MIndex value geometry");
    return new MIndexValueInfo(
        Math.toIntExact(values[0]),
        Math.toIntExact(values[1]),
        Math.toIntExact(values[2]),
        values[3]);
  }

  public synchronized long mindexToken(String path, int value, int ordinal) throws IOException {
    return M3FsNative.mindexToken(state.requireHandle(), path(path), value, ordinal);
  }

  public synchronized char mindexChar(String path, int value, int ordinal) throws IOException {
    return M3FsNative.mindexChar(state.requireHandle(), path(path), value, ordinal);
  }

  public synchronized int mindexCompare(String path, int left, int right) throws IOException {
    return M3FsNative.mindexCompare(state.requireHandle(), path(path), left, right);
  }

  public synchronized int mindexTokenIndexOf(
      String path,
      int value,
      long[] needle,
      int fromToken)
      throws IOException {
    return M3FsNative.mindexTokenIndexOf(
        state.requireHandle(), path(path), value, needle, fromToken);
  }

  /**
   * Execute one capture-free compiled MIndex regex over every value in a persisted image.
   *
   * <p>Unsupported surrogate-sensitive values are reported separately; native code never
   * approximates them.</p>
   */
  public synchronized NativeRegexResult nativeRegex(
      String path,
      M3FsRegexImage program,
      RegexMode mode)
      throws IOException {
    M3FsRegexImage checkedProgram = Objects.requireNonNull(program, "program");
    RegexMode checkedMode = Objects.requireNonNull(mode, "mode");
    if (checkedProgram.captureCount() != 0) {
      throw new IllegalArgumentException("native M3FS regex requires capture-free program");
    }
    long[] encoded =
        M3FsNative.regexMatchAll(
            state.requireHandle(),
            path(path),
            checkedMode.nativeCode,
            checkedProgram.image());
    if (encoded.length < 2) throw new IllegalStateException("native regex result geometry");
    int valueCount = Math.toIntExact(encoded[0]);
    int words = Math.toIntExact(encoded[1]);
    if (words != ((valueCount + 63) >>> 6) || encoded.length != 2 + words * 2) {
      throw new IllegalStateException("native regex result bitmap geometry");
    }
    long[] matches = Arrays.copyOfRange(encoded, 2, 2 + words);
    long[] unsupported = Arrays.copyOfRange(encoded, 2 + words, encoded.length);
    return new NativeRegexResult(valueCount, matches, unsupported);
  }

  /**
   * Resolves unsupported values with the matching JDK or RE2 semantics. The supplied image is
   * byte-for-byte bound to the persisted image in the same retained view used by native matching.
   * The selected fallback is compiled once, only after a value actually requires it.
   */
  public synchronized NativeRegexResult nativeRegex(
      String path, M3FsRegexImage program, RegexMode mode, M3FsIndexImage javaValues)
      throws IOException {
    Objects.requireNonNull(program, "program");
    return nativeRegex(path, program, mode, javaValues, new DefaultRegexFallback(program));
  }

  /** Call-local lazy compilation preserves image/native admission and exception ordering. */
  private static final class DefaultRegexFallback implements BiPredicate<CharSequence, RegexMode> {
    private final M3FsRegexImage program;
    private java.util.regex.Pattern jdk;
    private com.m3.fs.internal.re2j.Pattern re2;

    private DefaultRegexFallback(M3FsRegexImage program) { this.program = program; }

    @Override public boolean test(CharSequence value, RegexMode operation) {
      if (program.semantics() == M3FsRegexImage.Semantics.JDK) {
        if (jdk == null) jdk = java.util.regex.Pattern.compile(program.expression());
        java.util.regex.Matcher matcher = jdk.matcher(value);
        return switch (operation) {
          case MATCHES -> matcher.matches();
          case FIND -> matcher.find();
          case LOOKING_AT -> matcher.lookingAt();
        };
      }
      if (re2 == null) re2 = com.m3.fs.internal.re2j.Pattern.compile(program.expression());
      com.m3.fs.internal.re2j.Matcher matcher = re2.matcher(value);
      return switch (operation) {
        case MATCHES -> matcher.matches();
        case FIND -> matcher.find();
        case LOOKING_AT -> matcher.lookingAt();
      };
    }
  }

  /** Admits an authoritative fallback without installing a second regex compiler in M3FS. */
  public synchronized NativeRegexResult nativeRegex(
      String path, M3FsRegexImage program, RegexMode mode, M3FsIndexImage javaValues,
      BiPredicate<CharSequence, RegexMode> fallback) throws IOException {
    state.requireHandle();
    Objects.requireNonNull(fallback, "fallback");
    M3FsIndexImage values = Objects.requireNonNull(javaValues, "javaValues");
    if (snapshot) return boundRegex(path, program, mode, values, fallback);
    try (M3FS view = openSnapshot(database)) {
      return view.boundRegex(path, program, mode, values, fallback);
    }
  }

  private NativeRegexResult boundRegex(String path, M3FsRegexImage program, RegexMode mode,
      M3FsIndexImage values, BiPredicate<CharSequence, RegexMode> fallback) throws IOException {
    String checkedPath = path(path);
    ByteBuffer expected = values.image();
    if (stat(checkedPath).size() != expected.remaining()) {
      throw new IllegalArgumentException("fallback image differs from persisted image");
    }
    byte[] chunk = new byte[Math.min(65_536, expected.remaining())];
    long offset = 0;
    while (expected.hasRemaining()) {
      int length = Math.min(chunk.length, expected.remaining());
      int n = read(checkedPath, offset, chunk, 0, length);
      if (n != length) throw new IOException("persisted image changed within retained view");
      for (int i = 0; i < n; i++) {
        if (chunk[i] != expected.get()) {
          throw new IllegalArgumentException("fallback image differs from persisted image");
        }
      }
      offset += n;
    }
    NativeRegexResult result = nativeRegex(checkedPath, program, mode);
    if (result.valueCount() != values.valueCount()) throw new IllegalStateException("native image count");
    if (result.fallbackCount() == 0) return result;
    long[] matches = result.matches();
    for (int i = 0; i < result.valueCount(); i++) {
      if (!result.requiresFallback(i)) continue;
      long mask = 1L << (i & 63);
      if (fallback.test(values.value(i), mode)) matches[i >>> 6] |= mask;
      else matches[i >>> 6] &= ~mask;
    }
    return new NativeRegexResult(result.valueCount(), matches, new long[result.unsupported.length]);
  }

  public static long featureBits() { return M3FsNative.featureBits(); }
  public static Set<M3FsFeature> supportedFeatures() { return M3FsFeature.supportedFeatures(); }
  public static boolean supports(M3FsFeature feature) {
    return Objects.requireNonNull(feature, "feature").supported();
  }
  public static boolean supportsHolePunch() { return supportsCapability(M3FsNative.CAP_PUNCH_HOLE); }
  public static boolean supportsAllocatedRangeQuery() { return supportsCapability(M3FsNative.CAP_ALLOCATED_RANGES); }
  public static boolean supportsAllocatedRanges() { return supportsAllocatedRangeQuery(); }
  public static boolean supportsSetMetadata() { return supportsCapability(M3FsNative.CAP_SET_METADATA); }
  public static boolean supportsAtomicReplace() { return supportsCapability(M3FsNative.CAP_ATOMIC_REPLACE); }
  public static boolean supportsCheckedInodeIo() { return supportsCapability(M3FsNative.CAP_CHECKED_INODE_IO); }
  private static boolean supportsCapability(int capability) {
    return (M3FsNative.capabilities() & capability) != 0;
  }

  public synchronized void punchHole(String path, long offset, long length) throws IOException {
    requireRange(offset, length);
    M3FsNative.punchHole(state.requireHandle(), path(path), offset, length);
  }

  public synchronized List<AllocatedRange> allocatedRanges(String path, long offset, long length)
      throws IOException {
    requireRange(offset, length);
    long[] pairs = M3FsNative.allocatedRanges(state.requireHandle(), path(path), offset, length);
    if ((pairs.length & 1) != 0) throw new IllegalStateException("native allocated range geometry");
    long previousEnd = offset;
    for (int i = 0; i < pairs.length; i += 2) {
      long at = pairs[i], size = pairs[i + 1];
      if (at < previousEnd || size <= 0 || at > offset + length || size > offset + length - at) {
        throw new IllegalStateException("native allocated range bounds");
      }
      previousEnd = at + size;
    }
    return Collections.unmodifiableList(new AbstractList<AllocatedRange>() {
      @Override public int size() { return pairs.length / 2; }
      @Override public AllocatedRange get(int index) {
        int lane = Objects.checkIndex(index, size()) * 2;
        return new AllocatedRange(pairs[lane], pairs[lane + 1]);
      }
    });
  }

  public synchronized void setMetadata(String path, int mode, long modifiedTimeNanos,
      boolean setMode, boolean setTime) throws IOException {
    requireMode(mode);
    if (modifiedTimeNanos < 0) throw new IllegalArgumentException("modifiedTimeNanos");
    M3FsNative.setMetadata(state.requireHandle(), path(path), mode, modifiedTimeNanos, setMode, setTime);
  }

  /** Resolves and checks the inode in the same native read transaction as the byte transfer. */
  public synchronized int readIfInode(String path, long expectedInode, long offset,
      byte[] output, int at, int length) throws IOException {
    requireInode(expectedInode);
    requireOffset(offset);
    Objects.checkFromIndexSize(at, length, Objects.requireNonNull(output, "output").length);
    return M3FsNative.readIfInode(state.requireHandle(), path(path), expectedInode, offset,
        output, at, length);
  }

  public synchronized long writeIfInode(String path, long expectedInode, long offset,
      byte[] data, int at, int length) throws IOException {
    return writeIfInode(path, expectedInode, offset, data, at, length, 0);
  }

  /**
   * Checks identity and writes within one transaction. APPEND selects current EOF; TRUNCATE
   * first sets the size to offset, then writes the optional payload there. Flags are exclusive.
   */
  public synchronized long writeIfInode(String path, long expectedInode, long offset,
      byte[] data, int at, int length, int flags) throws IOException {
    requireInode(expectedInode);
    requireRange(offset, length);
    Objects.checkFromIndexSize(at, length, Objects.requireNonNull(data, "data").length);
    if (flags < 0 || flags > WRITE_TRUNCATE) throw new IllegalArgumentException("checked write flags");
    return M3FsNative.writeIfInode(state.requireHandle(), path(path), expectedInode, offset,
        data, at, length, flags);
  }

  public synchronized long appendIfInode(String path, long expectedInode,
      byte[] data, int at, int length) throws IOException {
    return writeIfInode(path, expectedInode, 0, data, at, length, WRITE_APPEND);
  }

  public synchronized void truncateIfInode(String path, long expectedInode, long size)
      throws IOException {
    writeIfInode(path, expectedInode, size, EMPTY_BYTES, 0, 0, WRITE_TRUNCATE);
  }

  public synchronized void truncate(String path, long size) throws IOException {
    if (size < 0L) throw new IllegalArgumentException("size");
    M3FsNative.truncate(state.requireHandle(), path(path), size);
  }

  public synchronized void unlink(String path) throws IOException {
    M3FsNative.unlink(state.requireHandle(), path(path));
  }

  public synchronized void rmdir(String path) throws IOException {
    M3FsNative.rmdir(state.requireHandle(), path(path));
  }

  public synchronized void rename(String from, String to) throws IOException {
    M3FsNative.rename(state.requireHandle(), path(from), path(to));
  }

  /** Moves atomically only when the destination is absent, including across independent handles. */
  public synchronized void renameNoReplace(String from, String to) throws IOException {
    M3FsNative.renameNoReplace(state.requireHandle(), path(from), path(to));
  }

  public static boolean supportsRenameNoReplace() {
    return supportsCapability(M3FsNative.CAP_RENAME_NOREPLACE);
  }

  public synchronized void link(String existing, String newPath) throws IOException {
    M3FsNative.link(state.requireHandle(), path(existing), path(newPath));
  }

  /**
   * Atomically create a distinct regular-file inode sharing the source's current block references.
   * Copies mode and logical size; link count/generation start at one with a fresh modification time.
   * The destination must not exist. Subsequent writes/truncations are independent, unlike link().
   * Returns the new inode after commit; does not decode or renumber any stored MIndex IDs.
   */
  public synchronized long cloneFile(String existing, String newPath) throws IOException {
    return M3FsNative.cloneFile(state.requireHandle(), path(existing), path(newPath));
  }

  /**
   * Read up to limit entries strictly after a name in unsigned UTF-8 byte order. A null or empty
   * afterName starts the traversal. limit is 1..MAX_PAGE_ENTRIES. An {@link #openSnapshot(Path)}
   * handle retains one view across pages. Other handles read a fresh view per call, so concurrent
   * modifications/renames can change their traversal.
   * Cursor names need not still exist. Do not compare cursors using Java UTF-16 lexical order.
   */
  public synchronized DirectoryPage listPage(String path, String afterName, int limit) throws IOException {
    if (limit < 1 || limit > MAX_PAGE_ENTRIES) throw new IllegalArgumentException("page limit");
    byte[] encoded = M3FsNative.listPage(state.requireHandle(), path(path), afterName, limit);
    if (encoded.length < 8 || encoded.length > 8 + limit * 307) {
      throw new IllegalStateException("invalid native directory page size");
    }
    ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
    int count = data.getInt();
    int more = data.getInt();
    if (count < 0 || count > limit || (more != 0 && more != 1)
        || (more == 1 && count != limit)) {
      throw new IllegalStateException("invalid native directory page header");
    }
    M3DirectoryTable table = M3DirectoryTable.decode(data);
    if (table.size() != count) throw new IllegalStateException("directory page count mismatch");
    return new DirectoryPage(table, more != 0);
  }

  public synchronized void symlink(String target, String linkPath) throws IOException {
    M3FsNative.symlink(
        state.requireHandle(),
        Objects.requireNonNull(target, "target"),
        path(linkPath));
  }

  public synchronized String readlink(String path) throws IOException {
    return decodeUtf8(M3FsNative.readlink(state.requireHandle(), path(path)));
  }

  public synchronized Stat stat(String path) throws IOException {
    return decodeStat(M3FsNative.stat(state.requireHandle(), path(path)));
  }

  public synchronized List<DirEntry> list(String path) throws IOException {
    return listTable(path).entries();
  }

  public synchronized M3DirectoryTable listTable(String path) throws IOException {
    return M3DirectoryTable.decode(ByteBuffer.wrap(M3FsNative.list(state.requireHandle(), path(path))));
  }

  public synchronized Stats stats() throws IOException {
    long[] values = M3FsNative.stats(state.requireHandle());
    if (values.length != 6) throw new IllegalStateException("native stats geometry");
    return new Stats(values[0], values[1], values[2], values[3], values[4], values[5]);
  }

  /** Verify stored block checksums without mutating legacy blocks that lack an integrity row. */
  public synchronized ScrubReport scrub() throws IOException {
    return scrub(false);
  }

  /**
   * Verify every unique deduplicated block and optionally backfill missing checksum rows.
   *
   * <p>Backfill never overwrites an existing checksum and therefore cannot bless a mismatch.
   * This API detects corruption; M3FS has no redundant replica repair claim.</p>
   */
  public synchronized ScrubReport scrub(boolean backfillMissing) throws IOException {
    if (backfillMissing && readOnly) {
      throw new java.nio.file.ReadOnlyFileSystemException();
    }
    long[] values = M3FsNative.scrub(state.requireHandle(), backfillMissing);
    if (values.length != 5) throw new IllegalStateException("native scrub geometry");
    return new ScrubReport(values[0], values[1], values[2], values[3], values[4]);
  }

  public synchronized void verify() throws IOException {
    M3FsNative.verify(state.requireHandle());
  }

  public synchronized void checkpoint() throws IOException {
    M3FsNative.checkpoint(state.requireHandle());
  }

  @Override
  public synchronized void close() {
    cleanable.clean();
  }

  private static Stat decodeStat(long[] values) {
    if (values.length != 8) throw new IllegalStateException("native stat geometry");
    return new Stat(
        values[0],
        values[1],
        values[2],
        values[3],
        Kind.fromNative(values[4]),
        Math.toIntExact(values[5]),
        Math.toIntExact(values[6]),
        Math.toIntExact(values[7]));
  }

  private static String decodeUtf8(byte[] encoded) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(Objects.requireNonNull(encoded, "encoded")))
          .toString();
    } catch (CharacterCodingException invalid) {
      throw new IllegalStateException("native M3FS emitted invalid UTF-8", invalid);
    }
  }

  private static String path(String path) {
    return Objects.requireNonNull(path, "path");
  }

  private static void requireOffset(long offset) {
    if (offset < 0L) throw new IllegalArgumentException("offset");
  }

  private static void requireInode(long inode) {
    if (inode <= 0) throw new IllegalArgumentException("expectedInode");
  }

  private static void requireRange(long offset, long length) {
    if (offset < 0 || length < 0 || length > Long.MAX_VALUE - offset) {
      throw new IllegalArgumentException("offset/length");
    }
  }

  private static void requireMode(int mode) {
    if (mode < 0 || mode > 07777) throw new IllegalArgumentException("mode");
  }
}
