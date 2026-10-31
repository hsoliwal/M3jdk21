// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file.json;

import com.m3.file.M3PageableTree;
import com.m3.file.M3JsonStructuralNative;
import com.m3.file.M3ProgressMonitor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

/**
 * Long-offset, primitive structural index for very large UTF-8 JSON and whitespace-separated JSON
 * streams.
 *
 * <p>The source file remains the value authority. This index retains only node topology, kind,
 * sibling ordinal, and byte ranges. Object/property names and scalar values are decoded only when
 * explicitly requested by a viewport/query. No JObject/JToken-style object tree or full-file
 * String is retained.</p>
 *
 * <p>A synthetic DOCUMENT node is always coordinate zero. Ordinary JSON has one child; NDJSON or
 * another whitespace-separated multi-root stream has multiple children. Direct children retain
 * source order. All source coordinates are signed 64-bit byte offsets.</p>
 */
public final class M3JsonStructuralIndex implements AutoCloseable {
  private static final int WINDOW_BYTES = 64 * 1024;
  private static final int PROGRESS_QUANTUM = 1024 * 1024;
  private static final int MAX_SIGNAL_PAGE_BYTES = 1024 * 1024;
  private static final long MAX_NODES = Integer.MAX_VALUE - 8L;

  public enum Kind {
    DOCUMENT,
    OBJECT,
    ARRAY,
    STRING,
    NUMBER,
    TRUE,
    FALSE,
    NULL
  }

  /**
   * Prepared exact raw UTF-8 KMP pattern over the source file.
   *
   * <p>The failure table is computed once and reused across calls. Matching is byte-exact and may
   * cross internal I/O windows. It is deliberately independent of JSON semantics: callers that
   * need property/value semantics first use the structural index to constrain byte ranges.</p>
   */
  public static final class RawSearch {
    private static final int MAX_PATTERN_BYTES = 1024 * 1024;
    private final M3JsonStructuralIndex owner;
    private final byte[] pattern;
    private final int[] failure;

    private RawSearch(M3JsonStructuralIndex owner, byte[] pattern) {
      this.owner = Objects.requireNonNull(owner, "owner");
      this.pattern = Objects.requireNonNull(pattern, "pattern").clone();
      if (this.pattern.length > MAX_PATTERN_BYTES) {
        throw new IllegalArgumentException("raw JSON search pattern exceeds byte budget");
      }
      this.failure = failure(this.pattern);
    }

    public int byteLength() {
      return pattern.length;
    }

    public long indexOf(long fromByte) throws IOException {
      return indexOf(fromByte, M3ProgressMonitor.noop());
    }

    public long indexOf(long fromByte, M3ProgressMonitor supplied) throws IOException {
      owner.ensureOpen();
      owner.requireCurrentSource();
      if (fromByte < 0L || fromByte > owner.fileSize) {
        throw new IndexOutOfBoundsException("raw JSON search start outside source");
      }
      if (pattern.length == 0) return fromByte;
      if ((long) pattern.length > owner.fileSize - fromByte) return -1L;

      M3ProgressMonitor monitor = supplied == null ? M3ProgressMonitor.noop() : supplied;
      ByteBuffer page = ByteBuffer.allocateDirect(WINDOW_BYTES);
      int matched = 0;
      long position = fromByte;
      long reported = fromByte;
      while (position < owner.fileSize) {
        monitor.checkCanceled();
        page.clear();
        int amount = Math.toIntExact(Math.min((long) page.capacity(), owner.fileSize - position));
        page.limit(amount);
        long readAt = position;
        while (page.hasRemaining()) {
          int count = owner.channel.read(page, readAt);
          if (count < 0) throw new IOException("unexpected EOF while searching JSON source");
          if (count == 0) {
            Thread.onSpinWait();
            continue;
          }
          readAt += count;
        }
        page.flip();
        for (int index = 0; index < amount; index++) {
          byte value = page.get(index);
          while (matched > 0 && value != pattern[matched]) {
            matched = failure[matched - 1];
          }
          if (value == pattern[matched]) matched++;
          if (matched == pattern.length) {
            long found = position + index - pattern.length + 1L;
            owner.requireCurrentSource();
            return found;
          }
        }
        position += amount;
        long delta = position - reported;
        if (delta >= PROGRESS_QUANTUM) {
          monitor.worked(delta);
          reported = position;
        }
      }
      if (position > reported) monitor.worked(position - reported);
      monitor.checkCanceled();
      owner.requireCurrentSource();
      return -1L;
    }

    private static int[] failure(byte[] pattern) {
      int[] failure = new int[pattern.length];
      int matched = 0;
      for (int index = 1; index < pattern.length; index++) {
        while (matched > 0 && pattern[index] != pattern[matched]) {
          matched = failure[matched - 1];
        }
        if (pattern[index] == pattern[matched]) matched++;
        failure[index] = matched;
      }
      return failure;
    }
  }

  /** Bounded per-byte static-signal page; never semantic JSON authority. */
  public static final class SignalPage {
    private final long sourceOffset;
    private final ByteBuffer flags;
    private final M3JsonStructuralNative.Backend backend;

    private SignalPage(
        long sourceOffset,
        ByteBuffer flags,
        M3JsonStructuralNative.Backend backend) {
      this.sourceOffset = sourceOffset;
      this.flags = flags.asReadOnlyBuffer();
      this.backend = Objects.requireNonNull(backend, "backend");
    }

    public long sourceOffset() { return sourceOffset; }
    public int length() { return flags.limit(); }
    public M3JsonStructuralNative.Backend backend() { return backend; }
    public int flagsAt(int index) {
      return Byte.toUnsignedInt(flags.get(Objects.checkIndex(index, flags.limit())));
    }
    public ByteBuffer flags() {
      ByteBuffer copy = flags.asReadOnlyBuffer();
      copy.position(0);
      return copy;
    }
  }

  /** Bounded display page; only these rows have decoded names/previews. */
  public static final class ViewportPage {
    private final int parentNode;
    private final int offset;
    private final int total;
    private final int[] nodes;
    private final String[] names;
    private final String[] previews;

    private ViewportPage(
        int parentNode,
        int offset,
        int total,
        int[] nodes,
        String[] names,
        String[] previews) {
      this.parentNode = parentNode;
      this.offset = offset;
      this.total = total;
      this.nodes = nodes;
      this.names = names;
      this.previews = previews;
    }

    public int parentNode() { return parentNode; }
    public int offset() { return offset; }
    public int total() { return total; }
    public int size() { return nodes.length; }
    public boolean hasNext() { return offset + nodes.length < total; }
    public int nextOffset() { return offset + nodes.length; }
    public int nodeAt(int index) { return nodes[Objects.checkIndex(index, nodes.length)]; }
    public String nameAt(int index) { return names[Objects.checkIndex(index, names.length)]; }
    public String previewAt(int index) {
      return previews[Objects.checkIndex(index, previews.length)];
    }
  }

  private final Path path;
  private final FileChannel channel;
  private final long fileSize;
  private final SourceSeal sourceSeal;
  private final int[] parent;
  private final int[] ordinal;
  private final byte[] kind;
  private final long[] start;
  private final long[] end;
  private final long[] nameStart;
  private final long[] nameEnd;
  private final int[] childOffsets;
  private final int[] children;
  private final boolean multipleRoots;
  private volatile boolean closed;

  private M3JsonStructuralIndex(
      Path path,
      FileChannel channel,
      long fileSize,
      SourceSeal sourceSeal,
      int[] parent,
      int[] ordinal,
      byte[] kind,
      long[] start,
      long[] end,
      long[] nameStart,
      long[] nameEnd,
      int[] childOffsets,
      int[] children,
      boolean multipleRoots) {
    this.path = path;
    this.channel = channel;
    this.fileSize = fileSize;
    this.sourceSeal = Objects.requireNonNull(sourceSeal, "sourceSeal");
    this.parent = parent;
    this.ordinal = ordinal;
    this.kind = kind;
    this.start = start;
    this.end = end;
    this.nameStart = nameStart;
    this.nameEnd = nameEnd;
    this.childOffsets = childOffsets;
    this.children = children;
    this.multipleRoots = multipleRoots;
  }

  public static M3JsonStructuralIndex open(Path source) throws IOException {
    return open(source, M3ProgressMonitor.noop());
  }

  public static M3JsonStructuralIndex open(Path source, M3ProgressMonitor supplied)
      throws IOException {
    Path path = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
    if (!java.nio.file.Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || java.nio.file.Files.isSymbolicLink(path)) {
      throw new IllegalArgumentException("JSON source must be a real regular file");
    }
    M3ProgressMonitor monitor = supplied == null ? M3ProgressMonitor.noop() : supplied;
    SourceSeal before = SourceSeal.capture(path);
    FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
    boolean keep = false;
    monitor.beginTask("Index JSON structure", M3ProgressMonitor.UNKNOWN);
    try {
      long size = channel.size();
      if (size != before.size()) {
        throw new IllegalArgumentException("JSON source changed while opening");
      }
      Builder builder = new Builder();
      builder.add(Kind.DOCUMENT, M3PageableTree.NO_PARENT, 0, 0L, size, -1L, -1L);
      Cursor cursor = new Cursor(channel, size, monitor);
      cursor.skipWhitespace();
      int roots = 0;
      while (!cursor.eof()) {
        if (roots == Integer.MAX_VALUE) {
          throw cursor.error("too many JSON roots");
        }
        parseValue(cursor, builder, 0, -1L, -1L, roots++);
        long beforeWhitespace = cursor.position();
        cursor.skipWhitespace();
        if (!cursor.eof() && cursor.position() == beforeWhitespace) {
          throw cursor.error("multiple JSON roots must be whitespace-separated");
        }
      }
      if (roots == 0) throw cursor.error("empty JSON source");
      cursor.finishProgress();

      Frozen frozen = builder.freeze();
      SourceSeal after = SourceSeal.capture(path);
      if (!before.sameContent(after) || after.size() != size) {
        throw new IllegalArgumentException("JSON source changed while indexing");
      }
      keep = true;
      return new M3JsonStructuralIndex(
          path,
          channel,
          size,
          after,
          frozen.parent,
          frozen.ordinal,
          frozen.kind,
          frozen.start,
          frozen.end,
          frozen.nameStart,
          frozen.nameEnd,
          frozen.childOffsets,
          frozen.children,
          roots > 1);
    } finally {
      monitor.done();
      if (!keep) channel.close();
    }
  }

  public Path path() {
    return path;
  }

  public long fileSize() {
    return fileSize;
  }

  public boolean multipleRoots() {
    return multipleRoots;
  }

  /** Prepare one reusable exact UTF-8 search pattern. */
  public RawSearch prepareRawSearch(CharSequence query) {
    Objects.requireNonNull(query, "query");
    return prepareRawSearch(query.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** Prepare one reusable exact byte search pattern. Caller bytes are defensively copied. */
  public RawSearch prepareRawSearch(byte[] pattern) {
    return new RawSearch(this, Objects.requireNonNull(pattern, "pattern"));
  }

  public int nodeCount() {
    return parent.length;
  }

  public Kind kind(int node) {
    return Kind.values()[Byte.toUnsignedInt(kind[check(node)])];
  }

  public int parent(int node) {
    return parent[check(node)];
  }

  public int ordinal(int node) {
    return ordinal[check(node)];
  }

  public long startByte(int node) {
    return start[check(node)];
  }

  public long endByte(int node) {
    return end[check(node)];
  }

  public long byteLength(int node) {
    int checked = check(node);
    return end[checked] - start[checked];
  }

  public boolean hasName(int node) {
    return nameStart[check(node)] >= 0L;
  }

  public long nameStartByte(int node) {
    int checked = check(node);
    if (nameStart[checked] < 0L) throw new IllegalStateException("node has no property name");
    return nameStart[checked];
  }

  public long nameEndByte(int node) {
    int checked = check(node);
    if (nameEnd[checked] < 0L) throw new IllegalStateException("node has no property name");
    return nameEnd[checked];
  }

  public int childCount(int node) {
    int checked = check(node);
    return childOffsets[checked + 1] - childOffsets[checked];
  }

  public int childAt(int node, int childOrdinal) {
    int checked = check(node);
    int count = childOffsets[checked + 1] - childOffsets[checked];
    return children[childOffsets[checked] + Objects.checkIndex(childOrdinal, count)];
  }

  /**
   * Lazy hierarchy view with bulk pages over the retained primitive child lane.
   *
   * <p>Construction and page reads do not decode JSON names or values.</p>
   */
  public M3PageableTree.Source source() {
    return new M3PageableTree.Source() {
      @Override
      public int rootCount() {
        return 1;
      }

      @Override
      public int rootAt(int ordinal) {
        if (ordinal != 0) throw new IndexOutOfBoundsException("document has one synthetic root");
        return 0;
      }

      @Override
      public int childCount(int parentNode) {
        return M3JsonStructuralIndex.this.childCount(parentNode);
      }

      @Override
      public int childAt(int parentNode, int ordinal) {
        return M3JsonStructuralIndex.this.childAt(parentNode, ordinal);
      }

      @Override
      public void readRoots(
          int firstOrdinal,
          int count,
          int[] target,
          int targetOffset,
          M3ProgressMonitor monitor) {
        Objects.requireNonNull(target, "target");
        if (firstOrdinal != 0 || count < 0 || count > 1
            || targetOffset < 0 || (long) targetOffset + count > target.length) {
          throw new IndexOutOfBoundsException("invalid document-root page");
        }
        M3ProgressMonitor active = monitor == null ? M3ProgressMonitor.noop() : monitor;
        active.checkCanceled();
        if (count == 1) target[targetOffset] = 0;
        active.worked(count);
      }

      @Override
      public void readChildren(
          int parentNode,
          int firstOrdinal,
          int count,
          int[] target,
          int targetOffset,
          M3ProgressMonitor monitor) {
        Objects.requireNonNull(target, "target");
        int checked = check(parentNode);
        int total = childOffsets[checked + 1] - childOffsets[checked];
        if (firstOrdinal < 0 || count < 0 || (long) firstOrdinal + count > total
            || targetOffset < 0 || (long) targetOffset + count > target.length) {
          throw new IndexOutOfBoundsException("invalid JSON child page");
        }
        M3ProgressMonitor active = monitor == null ? M3ProgressMonitor.noop() : monitor;
        active.checkCanceled();
        System.arraycopy(
            children, childOffsets[checked] + firstOrdinal, target, targetOffset, count);
        active.worked(count);
      }
    };
  }

  /** Explicit full-topology conversion for algorithms; viewport use should prefer {@link #source()}. */
  public M3PageableTree freezePagingTree() {
    return M3PageableTree.freezePaging(parent);
  }

  /**
   * Classifies one bounded source-byte page for static precompute, syntax highlighting or candidate
   * filtering. Java and JNI produce the same local flags; the parser does not consume these flags
   * as proof.
   */
  public SignalPage signalPage(long sourceOffset, int length) throws IOException {
    ensureOpen();
    requireCurrentSource();
    if (sourceOffset < 0L
        || length < 0
        || length > MAX_SIGNAL_PAGE_BYTES
        || sourceOffset > fileSize
        || length > fileSize - sourceOffset) {
      throw new IndexOutOfBoundsException("invalid JSON signal page");
    }
    ByteBuffer source = ByteBuffer.allocateDirect(length);
    long at = sourceOffset;
    while (source.hasRemaining()) {
      int count = channel.read(source, at);
      if (count < 0) throw new IOException("unexpected EOF while loading JSON signal page");
      if (count == 0) {
        Thread.onSpinWait();
        continue;
      }
      at += count;
    }
    source.flip();
    ByteBuffer flags = ByteBuffer.allocateDirect(length);
    M3JsonStructuralNative.Backend backend =
        M3JsonStructuralNative.classify(source, 0, length, flags, 0);
    flags.position(0);
    flags.limit(length);
    requireCurrentSource();
    return new SignalPage(sourceOffset, flags, backend);
  }

  /**
   * Materializes one bounded direct-child viewport page.
   *
   * <p>Only names/previews for the requested rows are decoded. The complete hierarchy and source
   * payload remain represented by primitive coordinates/ranges.</p>
   */
  public ViewportPage viewportChildren(int parentNode, int offset, int limit, int maxChars)
      throws IOException {
    requireCurrentSource();
    int parent = check(parentNode);
    if (offset < 0 || limit < 0 || maxChars < 1) {
      throw new IllegalArgumentException("invalid JSON viewport request");
    }
    int total = childOffsets[parent + 1] - childOffsets[parent];
    if (offset > total) throw new IndexOutOfBoundsException("JSON viewport offset");
    int size = Math.min(limit, total - offset);
    int[] nodes = new int[size];
    String[] names = new String[size];
    String[] previews = new String[size];
    int base = childOffsets[parent] + offset;
    for (int index = 0; index < size; index++) {
      int node = children[base + index];
      nodes[index] = node;
      names[index] = displayNameUnchecked(node, maxChars);
      previews[index] = previewUnchecked(node, maxChars);
    }
    requireCurrentSource();
    return new ViewportPage(parent, offset, total, nodes, names, previews);
  }

  /**
   * Human-facing node label without retaining decoded names.
   *
   * <p>Property names are unescaped lazily. Array members use their source ordinal. Top-level roots
   * use {@code $} or {@code $[n]} for multi-root streams.</p>
   */
  public String displayName(int node, int maxChars) throws IOException {
    requireCurrentSource();
    String value = displayNameUnchecked(node, maxChars);
    requireCurrentSource();
    return value;
  }

  private String displayNameUnchecked(int node, int maxChars) throws IOException {
    if (maxChars < 1) throw new IllegalArgumentException("maxChars");
    int checked = check(node);
    if (checked == 0) return "$document";
    if (nameStart[checked] >= 0L) {
      return decodeJsonString(nameStart[checked], nameEnd[checked], maxChars);
    }
    int p = parent[checked];
    if (p == 0) return multipleRoots ? "$[" + ordinal[checked] + "]" : "$";
    if (kind(p) == Kind.ARRAY) return "[" + ordinal[checked] + "]";
    return "$";
  }

  /**
   * Bounded lazy value preview. Containers never materialize descendants.
   */
  public String preview(int node, int maxChars) throws IOException {
    requireCurrentSource();
    String value = previewUnchecked(node, maxChars);
    requireCurrentSource();
    return value;
  }

  private String previewUnchecked(int node, int maxChars) throws IOException {
    if (maxChars < 1) throw new IllegalArgumentException("maxChars");
    int checked = check(node);
    return switch (kind(checked)) {
      case DOCUMENT ->
          multipleRoots
              ? "JSON stream, roots " + childCount(checked)
              : "JSON document";
      case OBJECT -> "{} object, children " + childCount(checked);
      case ARRAY -> "[] array, size " + childCount(checked);
      case STRING -> decodeJsonString(start[checked], end[checked], maxChars);
      case NUMBER, TRUE, FALSE, NULL ->
          rawUtf8Preview(start[checked], end[checked], maxChars);
    };
  }

  /** Explicit bounded raw UTF-8 slice for a node's original JSON lexeme. */
  public String rawValue(int node, int maxBytes) throws IOException {
    requireCurrentSource();
    if (maxBytes < 0) throw new IllegalArgumentException("maxBytes");
    int checked = check(node);
    long length = end[checked] - start[checked];
    if (length > maxBytes) {
      throw new IllegalArgumentException(
          "JSON lexeme exceeds caller materialization bound: " + length + " > " + maxBytes);
    }
    String value = decodeUtf8(read(start[checked], Math.toIntExact(length)));
    requireCurrentSource();
    return value;
  }

  /** Primitive retained payload only; excludes JVM array/object headers and the open FileChannel. */
  public long retainedPrimitivePayloadBytes() {
    return (long) parent.length * Integer.BYTES
        + (long) ordinal.length * Integer.BYTES
        + kind.length
        + (long) start.length * Long.BYTES
        + (long) end.length * Long.BYTES
        + (long) nameStart.length * Long.BYTES
        + (long) nameEnd.length * Long.BYTES
        + (long) childOffsets.length * Integer.BYTES
        + (long) children.length * Integer.BYTES;
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      channel.close();
    }
  }

  private int check(int node) {
    return Objects.checkIndex(node, parent.length);
  }

  private byte[] read(long offset, int length) throws IOException {
    ensureOpen();
    if (offset < 0 || length < 0 || offset > fileSize || length > fileSize - offset) {
      throw new IndexOutOfBoundsException("JSON byte range outside source");
    }
    byte[] bytes = new byte[length];
    ByteBuffer target = ByteBuffer.wrap(bytes);
    long position = offset;
    while (target.hasRemaining()) {
      int count = channel.read(target, position);
      if (count < 0) throw new IOException("unexpected EOF while materializing JSON slice");
      if (count == 0) {
        Thread.onSpinWait();
        continue;
      }
      position += count;
    }
    return bytes;
  }

  private void ensureOpen() {
    if (closed) throw new IllegalStateException("JSON structural index is closed");
  }

  private void requireCurrentSource() throws IOException {
    ensureOpen();
    SourceSeal current = SourceSeal.capture(path);
    if (!sourceSeal.sameContent(current) || current.size() != fileSize) {
      throw new IllegalStateException("JSON source changed after structural indexing");
    }
  }

  private String rawUtf8Preview(long from, long to, int maxChars) throws IOException {
    long length = to - from;
    int budget = Math.toIntExact(Math.min(length, Math.max(32L, (long) maxChars * 4L)));
    String text = decodeUtf8(read(from, budget));
    if (text.length() <= maxChars && budget == length) return text;
    int end = Math.min(maxChars, text.length());
    return text.substring(0, end) + "…";
  }

  private String decodeJsonString(long from, long to, int maxChars) throws IOException {
    if (to - from < 2L) throw new IllegalStateException("invalid retained JSON string range");
    ensureOpen();
    Cursor cursor = new Cursor(channel, fileSize, M3ProgressMonitor.noop());
    cursor.position(from);
    if (cursor.read() != '"') throw new IllegalStateException("retained JSON string lost opening quote");
    StringBuilder out = new StringBuilder(Math.min(maxChars, 64));
    boolean truncated = false;
    while (cursor.position() < to - 1L) {
      int b = cursor.read();
      if (b < 0) throw new IOException("unexpected EOF in retained JSON string");
      if (b == '\\') {
        int escaped = cursor.read();
        if (escaped < 0) throw new IOException("unexpected EOF in retained JSON escape");
        char value;
        switch (escaped) {
          case '"' -> value = '"';
          case '\\' -> value = '\\';
          case '/' -> value = '/';
          case 'b' -> value = '\b';
          case 'f' -> value = '\f';
          case 'n' -> value = '\n';
          case 'r' -> value = '\r';
          case 't' -> value = '\t';
          case 'u' -> value = (char) readHex4(cursor);
          default -> throw new IllegalStateException("retained JSON escape is invalid");
        }
        if (out.length() < maxChars) out.append(value);
        else truncated = true;
        continue;
      }
      if (b < 0x80) {
        if (out.length() < maxChars) out.append((char) b);
        else truncated = true;
        continue;
      }
      int codePoint = readUtf8CodePoint(cursor, b);
      int chars = Character.charCount(codePoint);
      if (out.length() + chars <= maxChars) out.appendCodePoint(codePoint);
      else truncated = true;
    }
    if (cursor.read() != '"' || cursor.position() != to) {
      throw new IllegalStateException("retained JSON string range drift");
    }
    return truncated ? out + "…" : out.toString();
  }

  private static String decodeUtf8(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException invalid) {
      throw new IllegalStateException("retained JSON UTF-8 is invalid", invalid);
    }
  }

  private static int parseValue(
      Cursor cursor,
      Builder builder,
      int parent,
      long nameStart,
      long nameEnd,
      int ordinal)
      throws IOException {
    cursor.skipWhitespace();
    long start = cursor.position();
    int b = cursor.peek();
    if (b < 0) throw cursor.error("expected JSON value");
    return switch (b) {
      case '{' -> parseObject(cursor, builder, parent, nameStart, nameEnd, ordinal, start);
      case '[' -> parseArray(cursor, builder, parent, nameStart, nameEnd, ordinal, start);
      case '"' -> {
        long end = parseStringToken(cursor);
        yield builder.add(Kind.STRING, parent, ordinal, start, end, nameStart, nameEnd);
      }
      case 't' -> {
        parseLiteral(cursor, "true");
        yield builder.add(
            Kind.TRUE, parent, ordinal, start, cursor.position(), nameStart, nameEnd);
      }
      case 'f' -> {
        parseLiteral(cursor, "false");
        yield builder.add(
            Kind.FALSE, parent, ordinal, start, cursor.position(), nameStart, nameEnd);
      }
      case 'n' -> {
        parseLiteral(cursor, "null");
        yield builder.add(
            Kind.NULL, parent, ordinal, start, cursor.position(), nameStart, nameEnd);
      }
      default -> {
        if (b != '-' && (b < '0' || b > '9')) throw cursor.error("expected JSON value");
        parseNumber(cursor);
        yield builder.add(
            Kind.NUMBER, parent, ordinal, start, cursor.position(), nameStart, nameEnd);
      }
    };
  }

  private static int parseObject(
      Cursor cursor,
      Builder builder,
      int parent,
      long nameStart,
      long nameEnd,
      int ordinal,
      long start)
      throws IOException {
    int node = builder.add(Kind.OBJECT, parent, ordinal, start, -1L, nameStart, nameEnd);
    expect(cursor, '{');
    cursor.skipWhitespace();
    if (cursor.peek() == '}') {
      cursor.read();
      builder.end(node, cursor.position());
      return node;
    }
    int childOrdinal = 0;
    while (true) {
      if (cursor.peek() != '"') throw cursor.error("object member name must be a JSON string");
      long keyStart = cursor.position();
      long keyEnd = parseStringToken(cursor);
      cursor.skipWhitespace();
      expect(cursor, ':');
      parseValue(cursor, builder, node, keyStart, keyEnd, childOrdinal++);
      cursor.skipWhitespace();
      int separator = cursor.read();
      if (separator == '}') {
        builder.end(node, cursor.position());
        return node;
      }
      if (separator != ',') throw cursor.error("expected ',' or '}' after object member");
      cursor.skipWhitespace();
    }
  }

  private static int parseArray(
      Cursor cursor,
      Builder builder,
      int parent,
      long nameStart,
      long nameEnd,
      int ordinal,
      long start)
      throws IOException {
    int node = builder.add(Kind.ARRAY, parent, ordinal, start, -1L, nameStart, nameEnd);
    expect(cursor, '[');
    cursor.skipWhitespace();
    if (cursor.peek() == ']') {
      cursor.read();
      builder.end(node, cursor.position());
      return node;
    }
    int childOrdinal = 0;
    while (true) {
      parseValue(cursor, builder, node, -1L, -1L, childOrdinal++);
      cursor.skipWhitespace();
      int separator = cursor.read();
      if (separator == ']') {
        builder.end(node, cursor.position());
        return node;
      }
      if (separator != ',') throw cursor.error("expected ',' or ']' after array element");
      cursor.skipWhitespace();
    }
  }

  private static long parseStringToken(Cursor cursor) throws IOException {
    expect(cursor, '"');
    while (true) {
      int b = cursor.read();
      if (b < 0) throw cursor.error("unterminated JSON string");
      if (b == '"') return cursor.position();
      if (b < 0x20) throw cursor.error("unescaped control byte in JSON string");
      if (b == '\\') {
        int escaped = cursor.read();
        if (escaped < 0) throw cursor.error("unterminated JSON escape");
        if (escaped == 'u') {
          readHex4(cursor);
        } else if (escaped != '"' && escaped != '\\' && escaped != '/'
            && escaped != 'b' && escaped != 'f' && escaped != 'n'
            && escaped != 'r' && escaped != 't') {
          throw cursor.error("invalid JSON escape");
        }
      } else if (b >= 0x80) {
        readUtf8CodePoint(cursor, b);
      }
    }
  }

  private static void parseLiteral(Cursor cursor, String literal) throws IOException {
    for (int i = 0; i < literal.length(); i++) {
      if (cursor.read() != literal.charAt(i)) {
        throw cursor.error("invalid JSON literal");
      }
    }
  }

  private static void parseNumber(Cursor cursor) throws IOException {
    if (cursor.peek() == '-') cursor.read();
    int first = cursor.peek();
    if (first == '0') {
      cursor.read();
      int next = cursor.peek();
      if (next >= '0' && next <= '9') throw cursor.error("leading zero in JSON number");
    } else if (first >= '1' && first <= '9') {
      do {
        cursor.read();
        first = cursor.peek();
      } while (first >= '0' && first <= '9');
    } else {
      throw cursor.error("JSON number requires an integer part");
    }

    if (cursor.peek() == '.') {
      cursor.read();
      int digit = cursor.peek();
      if (digit < '0' || digit > '9') throw cursor.error("JSON fraction requires a digit");
      do {
        cursor.read();
        digit = cursor.peek();
      } while (digit >= '0' && digit <= '9');
    }

    int exponent = cursor.peek();
    if (exponent == 'e' || exponent == 'E') {
      cursor.read();
      int sign = cursor.peek();
      if (sign == '+' || sign == '-') cursor.read();
      int digit = cursor.peek();
      if (digit < '0' || digit > '9') throw cursor.error("JSON exponent requires a digit");
      do {
        cursor.read();
        digit = cursor.peek();
      } while (digit >= '0' && digit <= '9');
    }
  }

  private static int readHex4(Cursor cursor) throws IOException {
    int value = 0;
    for (int i = 0; i < 4; i++) {
      int b = cursor.read();
      int hex =
          b >= '0' && b <= '9'
              ? b - '0'
              : b >= 'a' && b <= 'f'
                  ? b - 'a' + 10
                  : b >= 'A' && b <= 'F' ? b - 'A' + 10 : -1;
      if (hex < 0) throw cursor.error("invalid JSON unicode escape");
      value = (value << 4) | hex;
    }
    return value;
  }

  private static int readUtf8CodePoint(Cursor cursor, int first) throws IOException {
    if (first >= 0xC2 && first <= 0xDF) {
      int b2 = continuation(cursor);
      return ((first & 0x1F) << 6) | (b2 & 0x3F);
    }
    if (first >= 0xE0 && first <= 0xEF) {
      int b2 = continuation(cursor);
      int b3 = continuation(cursor);
      if (first == 0xE0 && b2 < 0xA0) throw cursor.error("overlong UTF-8 in JSON string");
      if (first == 0xED && b2 >= 0xA0) throw cursor.error("UTF-8 surrogate in JSON string");
      return ((first & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
    }
    if (first >= 0xF0 && first <= 0xF4) {
      int b2 = continuation(cursor);
      int b3 = continuation(cursor);
      int b4 = continuation(cursor);
      if (first == 0xF0 && b2 < 0x90) throw cursor.error("overlong UTF-8 in JSON string");
      if (first == 0xF4 && b2 > 0x8F) throw cursor.error("UTF-8 code point outside Unicode");
      return ((first & 0x07) << 18)
          | ((b2 & 0x3F) << 12)
          | ((b3 & 0x3F) << 6)
          | (b4 & 0x3F);
    }
    throw cursor.error("invalid UTF-8 lead byte in JSON string");
  }

  private static int continuation(Cursor cursor) throws IOException {
    int value = cursor.read();
    if (value < 0x80 || value > 0xBF) throw cursor.error("invalid UTF-8 continuation byte");
    return value;
  }

  private static void expect(Cursor cursor, int expected) throws IOException {
    if (cursor.read() != expected) throw cursor.error("expected '" + (char) expected + "'");
  }

  private static final class Cursor {
    private final FileChannel channel;
    private final long size;
    private final M3ProgressMonitor monitor;
    private final ByteBuffer window = ByteBuffer.allocateDirect(WINDOW_BYTES);
    private long windowStart = -1L;
    private int windowLength;
    private long position;
    private long reported;

    Cursor(FileChannel channel, long size, M3ProgressMonitor monitor) {
      this.channel = channel;
      this.size = size;
      this.monitor = monitor;
    }

    long position() {
      return position;
    }

    void position(long value) {
      if (value < 0 || value > size) throw new IndexOutOfBoundsException("JSON byte position");
      position = value;
    }

    boolean eof() {
      return position >= size;
    }

    int peek() throws IOException {
      return byteAt(position);
    }

    int read() throws IOException {
      int value = byteAt(position);
      if (value >= 0) {
        position++;
        progress();
      }
      return value;
    }

    void skipWhitespace() throws IOException {
      while (true) {
        int value = peek();
        if (value != ' ' && value != '\t' && value != '\r' && value != '\n') return;
        read();
      }
    }

    IllegalArgumentException error(String message) {
      return new IllegalArgumentException(message + " at byte " + position);
    }

    void finishProgress() {
      long remaining = position - reported;
      if (remaining > 0L) {
        monitor.worked(remaining);
        reported = position;
      }
      monitor.checkCanceled();
    }

    private void progress() {
      if ((position & 0xFFFFL) == 0L) monitor.checkCanceled();
      long delta = position - reported;
      if (delta >= PROGRESS_QUANTUM) {
        monitor.worked(delta);
        reported = position;
      }
    }

    private int byteAt(long absolute) throws IOException {
      if (absolute >= size) return -1;
      if (absolute < 0) throw new IndexOutOfBoundsException("negative JSON byte position");
      if (windowStart < 0L
          || absolute < windowStart
          || absolute >= windowStart + windowLength) {
        load(absolute);
      }
      return Byte.toUnsignedInt(window.get(Math.toIntExact(absolute - windowStart)));
    }

    private void load(long absolute) throws IOException {
      long base = absolute - absolute % WINDOW_BYTES;
      window.clear();
      int requested = Math.toIntExact(Math.min((long) WINDOW_BYTES, size - base));
      window.limit(requested);
      long at = base;
      while (window.hasRemaining()) {
        int count = channel.read(window, at);
        if (count < 0) throw new IOException("unexpected EOF while indexing JSON");
        if (count == 0) {
          Thread.onSpinWait();
          continue;
        }
        at += count;
      }
      window.flip();
      windowStart = base;
      windowLength = requested;
    }
  }

  private record SourceSeal(
      long size,
      FileTime creationTime,
      FileTime lastModifiedTime,
      String fileKey) {
    static SourceSeal capture(Path path) throws IOException {
      BasicFileAttributes attributes =
          Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isRegularFile() || Files.isSymbolicLink(path)) {
        throw new IllegalArgumentException("JSON source is no longer a real regular file");
      }
      Object key = attributes.fileKey();
      return new SourceSeal(
          attributes.size(),
          attributes.creationTime(),
          attributes.lastModifiedTime(),
          key == null ? null : key.toString());
    }

    boolean sameContent(SourceSeal other) {
      return other != null
          && size == other.size
          && creationTime.equals(other.creationTime)
          && lastModifiedTime.equals(other.lastModifiedTime)
          && Objects.equals(fileKey, other.fileKey);
    }
  }

  private static final class Builder {
    private int size;
    private int[] parent = new int[64];
    private int[] ordinal = new int[64];
    private byte[] kind = new byte[64];
    private long[] start = new long[64];
    private long[] end = new long[64];
    private long[] nameStart = new long[64];
    private long[] nameEnd = new long[64];
    private int[] childCount = new int[64];

    int add(
        Kind nodeKind,
        int parentNode,
        int siblingOrdinal,
        long from,
        long to,
        long propertyNameStart,
        long propertyNameEnd) {
      if (size >= MAX_NODES) throw new IllegalArgumentException("JSON node count exceeds int space");
      ensure(size + 1);
      int node = size++;
      parent[node] = parentNode;
      ordinal[node] = siblingOrdinal;
      kind[node] = (byte) nodeKind.ordinal();
      start[node] = from;
      end[node] = to;
      nameStart[node] = propertyNameStart;
      nameEnd[node] = propertyNameEnd;
      if (parentNode >= 0) childCount[parentNode] = Math.addExact(childCount[parentNode], 1);
      return node;
    }

    void end(int node, long value) {
      if (node < 0 || node >= size || value < start[node]) {
        throw new IllegalArgumentException("invalid JSON node end");
      }
      end[node] = value;
    }

    Frozen freeze() {
      int[] parents = Arrays.copyOf(parent, size);
      int[] ordinals = Arrays.copyOf(ordinal, size);
      byte[] kinds = Arrays.copyOf(kind, size);
      long[] starts = Arrays.copyOf(start, size);
      long[] ends = Arrays.copyOf(end, size);
      long[] namesStart = Arrays.copyOf(nameStart, size);
      long[] namesEnd = Arrays.copyOf(nameEnd, size);
      int[] counts = Arrays.copyOf(childCount, size);
      int[] offsets = new int[size + 1];
      for (int node = 0; node < size; node++) {
        offsets[node + 1] = Math.addExact(offsets[node], counts[node]);
      }
      int[] children = new int[Math.max(0, size - 1)];
      int[] fill = Arrays.copyOf(offsets, size);
      for (int node = 1; node < size; node++) {
        int p = parents[node];
        if (p < 0 || p >= node) {
          throw new IllegalStateException("JSON parser produced invalid parent topology");
        }
        children[fill[p]++] = node;
      }
      return new Frozen(
          parents, ordinals, kinds, starts, ends, namesStart, namesEnd, offsets, children);
    }

    private void ensure(int required) {
      if (required <= parent.length) return;
      int next = Math.max(required, parent.length + (parent.length >>> 1));
      parent = Arrays.copyOf(parent, next);
      ordinal = Arrays.copyOf(ordinal, next);
      kind = Arrays.copyOf(kind, next);
      start = Arrays.copyOf(start, next);
      end = Arrays.copyOf(end, next);
      nameStart = Arrays.copyOf(nameStart, next);
      nameEnd = Arrays.copyOf(nameEnd, next);
      childCount = Arrays.copyOf(childCount, next);
    }
  }

  private record Frozen(
      int[] parent,
      int[] ordinal,
      byte[] kind,
      long[] start,
      long[] end,
      long[] nameStart,
      long[] nameEnd,
      int[] childOffsets,
      int[] children) {}
}
