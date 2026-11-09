// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A single-writer, source-bound store backed by immutable content-addressed files.
 *
 * <p>Each put verifies the exact {@link M3ContentExtent} first, writes and fsyncs an
 * immutable blob, then publishes a checksummed complete index via an atomic rename.
 * Interrupted writes may leave an unreachable blob, never a partially published
 * index. Reads are exact-length and verify SHA-256 before returning a fresh array.
 * Blob reads map the exact-length blob READ_ONLY through {@link FileChannel#map} and copy it out.
 * No raw Java heap pointer or mapping escapes this class.
 *
 * <p>This requires atomic rename on the index filesystem. Directory fsync is
 * attempted where the platform permits it; no portable Java API can guarantee
 * power-loss ordering on every filesystem. Recovery makes no claim of stronger
 * durability than the underlying filesystem provides.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-storage-sdk/src/main/java/com/synexia/storage/DurableExtentStore.java blob 804457b2228f; recipe m3/file/recipe/durable-extent-store-20261107/relocate.py v1
public final class M3DurableExtentStore implements AutoCloseable {
  private static final String MAGIC = "M3EXTIDX1\n";
  private static final String SEAL = "#sha256:";
  private static final int MAX_INDEX_BYTES = 64 * 1024 * 1024;
  private static final int MAX_ENTRIES = 100_000;
  private static final int MAX_KEY_BYTES = 4 * 1024;

  @FunctionalInterface
  interface BeforePublish {
    void run() throws IOException;
  }

  private final Path root;
  private final Path blobs;
  private final Path indexFile;
  private final BeforePublish beforePublish;
  private final FileChannel lockChannel;
  private final FileLock writerLock;
  private final TreeMap<String, M3ContentExtent> index = new TreeMap<>();
  private boolean closed;

  public static M3DurableExtentStore open(Path directory) throws IOException {
    return new M3DurableExtentStore(directory, () -> { });
  }

  static M3DurableExtentStore open(Path directory, BeforePublish hook) throws IOException {
    return new M3DurableExtentStore(directory, hook);
  }

  private M3DurableExtentStore(Path directory, BeforePublish hook)
      throws IOException {
    root = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    beforePublish = Objects.requireNonNull(hook, "hook");
    blobs = root.resolve("blobs");
    indexFile = root.resolve("index.m3");
    Files.createDirectories(blobs);
    FileChannel channel = FileChannel.open(root.resolve("writer.lock"),
        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    FileLock acquired;
    try {
      acquired = channel.tryLock();
    } catch (OverlappingFileLockException overlap) {
      channel.close();
      throw new IOException("extent store already has a writer", overlap);
    } catch (IOException | RuntimeException failure) {
      channel.close();
      throw failure;
    }
    if (acquired == null) {
      channel.close();
      throw new IOException("extent store already has a writer");
    }
    lockChannel = channel;
    writerLock = acquired;
    try {
      if (Files.exists(indexFile)) {
        index.putAll(readSnapshot());
      } else {
        publish(new TreeMap<>(), false);
      }
    } catch (IOException | RuntimeException failure) {
      try { acquired.release(); } finally { channel.close(); }
      throw failure;
    }
  }

  /** Immutable write. A repeated key is rejected even if it has identical bytes. */
  public synchronized M3ContentExtent write(String key, M3ContentExtent extent, byte[] content)
      throws IOException {
    requireOpen();
    key(key);
    if (index.containsKey(key)) throw new IllegalStateException("extent key is immutable: " + key);
    return install(key, extent, content);
  }

  /** Replace only an existing logical key, after the new data is fully persisted. */
  public synchronized M3ContentExtent supersede(String key, M3ContentExtent extent, byte[] content)
      throws IOException {
    requireOpen();
    key(key);
    if (!index.containsKey(key)) throw new IllegalStateException("missing key to supersede: " + key);
    return install(key, extent, content);
  }

  private M3ContentExtent install(String key, M3ContentExtent extent, byte[] content)
      throws IOException {
    M3ContentExtent.validateContent(Objects.requireNonNull(extent, "extent"), content);
    if (extent.byteLength() > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("extent exceeds Java array limit");
    }
    persistBlob(extent, content);
    TreeMap<String, M3ContentExtent> staged = new TreeMap<>(index);
    staged.put(key, extent);
    if (staged.size() > MAX_ENTRIES) throw new IOException("extent index entry limit exceeded");
    publish(staged, true);
    return extent;
  }

  public synchronized M3ContentExtent metadata(String key) throws IOException {
    requireOpen();
    key(key);
    return index.get(key);
  }

  /** Returns an independently mutable snapshot, never an alias of mapped native storage. */
  public synchronized byte[] read(String key) throws IOException {
    requireOpen();
    key(key);
    M3ContentExtent extent = index.get(key);
    if (extent == null) return null;
    return loadBlob(extent);
  }

  public synchronized List<String> keys(String prefix, int limit) throws IOException {
    requireOpen();
    Objects.requireNonNull(prefix, "prefix");
    if (limit < 0) throw new IllegalArgumentException("limit must not be negative");
    ArrayList<String> keys = new ArrayList<>();
    for (String item : index.tailMap(prefix).keySet()) {
      if (!item.startsWith(prefix)) break;
      if (limit != 0 && keys.size() >= limit) break;
      keys.add(item);
    }
    return List.copyOf(keys);
  }

  public synchronized M3ContentExtent.ContentExtentManifest manifest() throws IOException {
    requireOpen();
    return M3ContentExtent.buildManifest(new LinkedHashSet<>(index.values()));
  }

  public synchronized void verifyAll() throws IOException {
    requireOpen();
    for (M3ContentExtent extent : new LinkedHashSet<>(index.values())) loadBlob(extent);
  }

  private byte[] loadBlob(M3ContentExtent extent) throws IOException {
    Path file = blobPath(extent);
    if (!Files.isRegularFile(file)) throw new IOException("missing content blob: " + extent.contentSha256());
    int length = Math.toIntExact(extent.byteLength());
    byte[] bytes = new byte[length];
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      if (channel.size() != length) throw new IOException("truncated or extended extent blob");
      if (length != 0) channel.map(FileChannel.MapMode.READ_ONLY, 0, length).get(bytes);
    }
    try {
      M3ContentExtent.validateContent(extent, bytes);
    } catch (M3ContentExtent.ContentTamperedException invalid) {
      throw new IOException("corrupt content blob " + extent.contentSha256(), invalid);
    }
    return bytes;
  }

  private Path blobPath(M3ContentExtent extent) {
    return blobs.resolve(extent.contentSha256() + ".bin");
  }

  private void persistBlob(M3ContentExtent extent, byte[] content) throws IOException {
    Path destination = blobPath(extent);
    if (Files.exists(destination)) {
      loadBlob(extent); // Do not overwrite a corrupt canonical object.
      return;
    }
    Path pending = Files.createTempFile(blobs, ".m3-staged-", ".blob");
    try {
      try (FileChannel channel = FileChannel.open(pending, StandardOpenOption.WRITE)) {
        ByteBuffer source = ByteBuffer.wrap(content);
        while (source.hasRemaining()) channel.write(source);
        channel.force(true);
      }
      moveAtomic(pending, destination);
      syncDirectory(blobs);
    } finally {
      Files.deleteIfExists(pending);
    }
  }

  private void publish(TreeMap<String, M3ContentExtent> next, boolean invokeHook)
      throws IOException {
    byte[] bytes = snapshot(next);
    Path pending = Files.createTempFile(root, ".m3-index-", ".new");
    try {
      try (FileChannel channel = FileChannel.open(pending, StandardOpenOption.WRITE)) {
        ByteBuffer source = ByteBuffer.wrap(bytes);
        while (source.hasRemaining()) channel.write(source);
        channel.force(true);
      }
      if (invokeHook) beforePublish.run();
      moveAtomic(pending, indexFile);
      index.clear();
      index.putAll(next);
      syncDirectory(root);
    } finally {
      Files.deleteIfExists(pending);
    }
  }

  private static void moveAtomic(Path from, Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("atomic extent publication is unsupported on this filesystem", unsupported);
    }
  }

  private static void syncDirectory(Path path) {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      channel.force(true);
    } catch (IOException | UnsupportedOperationException unsupported) {
      // Java cannot portably fsync directory handles. Atomic rename is mandatory;
      // power-loss durability needs platform-specific qualification.
    }
  }

  private static byte[] snapshot(TreeMap<String, M3ContentExtent> values) throws IOException {
    StringBuilder body = new StringBuilder(MAGIC);
    for (Map.Entry<String, M3ContentExtent> entry : values.entrySet()) {
      M3ContentExtent extent = entry.getValue();
      body.append(encode(entry.getKey())).append('\t')
          .append(encode(extent.sourceId())).append('\t')
          .append(extent.sourceSha256()).append('\t')
          .append(extent.byteStart()).append('\t')
          .append(extent.byteEnd()).append('\t')
          .append(extent.contentSha256()).append('\t')
          .append(encode(extent.logicalKind())).append('\n');
    }
    String content = body + SEAL + M3ContentExtent.sha256(body.toString().getBytes(StandardCharsets.UTF_8)) + "\n";
    byte[] result = content.getBytes(StandardCharsets.UTF_8);
    if (result.length > MAX_INDEX_BYTES) throw new IOException("extent index byte limit exceeded");
    return result;
  }

  private TreeMap<String, M3ContentExtent> readSnapshot() throws IOException {
    if (Files.size(indexFile) > MAX_INDEX_BYTES) throw new IOException("extent index too large");
    byte[] data = Files.readAllBytes(indexFile);
    String sealed;
    try {
      sealed = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(data)).toString();
    } catch (CharacterCodingException invalid) {
      throw new IOException("extent index is not valid UTF-8", invalid);
    }
    int marker = sealed.lastIndexOf(SEAL);
    if (!sealed.startsWith(MAGIC) || marker < MAGIC.length() ||
        !sealed.endsWith("\n") || marker + SEAL.length() + 64 != sealed.length() - 1) {
      throw new IOException("malformed extent index header or seal");
    }
    String body = sealed.substring(0, marker);
    String checksum = sealed.substring(marker + SEAL.length(), sealed.length() - 1);
    if (!M3ContentExtent.sha256(body.getBytes(StandardCharsets.UTF_8)).equals(checksum)) {
      throw new IOException("extent index checksum mismatch");
    }
    TreeMap<String, M3ContentExtent> values = new TreeMap<>();
    String last = null;
    String[] rows = body.substring(MAGIC.length()).split("\n", -1);
    if (!rows[rows.length - 1].isEmpty()) throw new IOException("unterminated extent index row");
    try {
      for (int i = 0; i < rows.length - 1; i++) {
        String[] fields = rows[i].split("\t", -1);
        if (fields.length != 7 || values.size() >= MAX_ENTRIES) {
          throw new IOException("malformed extent index row");
        }
        String key = decode(fields[0]);
        key(key);
        if (last != null && last.compareTo(key) >= 0) throw new IOException("index keys out of order");
        long start = Long.parseLong(fields[3]);
        long end = Long.parseLong(fields[4]);
        if (!Long.toString(start).equals(fields[3]) || !Long.toString(end).equals(fields[4])) {
          throw new IOException("noncanonical index offsets");
        }
        M3ContentExtent extent = new M3ContentExtent(decode(fields[1]), fields[2],
            start, end, fields[5], decode(fields[6]));
        if (extent.byteLength() > Integer.MAX_VALUE) throw new IOException("extent too large to read");
        values.put(key, extent);
        last = key;
      }
    } catch (IllegalArgumentException invalid) {
      throw new IOException("invalid extent index metadata", invalid);
    }
    return values;
  }

  private static String encode(String text) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(String encoded) {
    String text = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    if (!encode(text).equals(encoded)) throw new IllegalArgumentException("noncanonical UTF-8 index field");
    return text;
  }

  private static void key(String key) {
    Objects.requireNonNull(key, "key");
    if (key.isEmpty() || key.indexOf('\0') >= 0 ||
        key.getBytes(StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
      throw new IllegalArgumentException("invalid extent key");
    }
  }

  private void requireOpen() throws IOException {
    if (closed) throw new IOException("extent store is closed");
  }

  @Override public synchronized void close() throws IOException {
    if (closed) return;
    closed = true;
    try { writerLock.release(); } finally { lockChannel.close(); }
  }
}
