// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * M3FMT transport of immutable, catalog-scoped token IDs and lossless literal spans.
 * The receiver must independently verify its mapped immutable catalog before binding a Lexicon.
 * A frame holds primitive lanes, never a per-token Java object or an independent dictionary.
 */
public final class M3FmtLexiconFrame {
  private static final long MAGIC = 0x4d33464d544c3031L; // M3FMTL01
  private static final int VERSION = 1;
  public static final int TOKEN = 0;
  public static final int LITERAL = 1;
  private static final int SHA_LENGTH = 32;
  private static final int MAX_FRAME = 64 * 1024 * 1024;
  private static final int MAX_SEGMENTS = 1_000_000;
  private static final int MAX_LITERAL_BYTES = 32 * 1024 * 1024;
  private static final int MAX_METADATA = 1024;
  private static final int MAX_TEXT = 8 * 1024;
  private M3FmtLexiconFrame() {}

  /** Adapter to the existing immutable catalog owner; not a second lexicon. */
  public interface Lexicon {
    /** Lowercase SHA-256 of the authoritative catalog manifest and word-order identity. */
    String catalogRoot();
    /** Writes the exact canonical wire bytes of one frozen ID; refuse overlays/unknown IDs. */
    void writeFrozenUtf8(long tokenId, OutputStream destination) throws IOException;
  }

  /** Immutable primitive representation; offset[i..i+1] is nonempty for literal spans only. */
  public static final class Frame {
    private final String catalogRoot;
    private final String contentRoot;
    private final Map<String, String> metadata;
    private final byte[] kinds;
    private final long[] ids;
    private final int[] offsets;
    private final byte[] literalBytes;

    private Frame(String catalogRoot, String contentRoot, Map<String, String> metadata,
                  byte[] kinds, long[] ids, int[] offsets, byte[] literalBytes) {
      this.catalogRoot = catalogRoot;
      this.contentRoot = contentRoot;
      this.metadata = Map.copyOf(metadata);
      this.kinds = kinds.clone();
      this.ids = ids.clone();
      this.offsets = offsets.clone();
      this.literalBytes = literalBytes.clone();
    }

    public String catalogRoot() { return catalogRoot; }
    public String contentRoot() { return contentRoot; }
    public Map<String, String> metadata() { return metadata; }
    public int segmentCount() { return kinds.length; }
    public int kind(int position) { return Byte.toUnsignedInt(kinds[Objects.checkIndex(position, kinds.length)]); }
    public long tokenId(int position) { return ids[Objects.checkIndex(position, ids.length)]; }
    public int literalByteCount() { return literalBytes.length; }
    public byte[] literalBytes() { return literalBytes.clone(); }
  }

  /** Seal a lossless segment image to the authoritative catalog and its rendered byte digest. */
  public static Frame seal(Lexicon lexicon, Map<String, String> metadata,
                           byte[] kinds, long[] ids, int[] offsets, byte[] literals)
      throws IOException {
    Objects.requireNonNull(lexicon, "lexicon");
    String root = root(lexicon.catalogRoot());
    if (metadata == null || kinds == null || ids == null || offsets == null || literals == null) {
      throw new NullPointerException("frame lane");
    }
    if (kinds.length > MAX_SEGMENTS || ids.length != kinds.length
        || offsets.length != kinds.length + 1 || literals.length > MAX_LITERAL_BYTES
        || offsets[0] != 0 || offsets[offsets.length - 1] != literals.length) {
      throw new IllegalArgumentException("invalid frame lanes");
    }
    TreeMap<String, String> sorted = metadata(metadata);
    MessageDigest digest = sha256();
    OutputStream hashing = new OutputStream() {
      @Override public void write(int b) { digest.update((byte) b); }
      @Override public void write(byte[] b, int off, int length) {
        digest.update(b, off, length);
      }
    };
    for (int i = 0; i < kinds.length; i++) {
      int a = offsets[i], b = offsets[i + 1];
      if (a < 0 || b < a || b > literals.length) {
        throw new IllegalArgumentException("invalid literal offsets");
      }
      if (kinds[i] == TOKEN) {
        if (a != b) throw new IllegalArgumentException("token with literal payload");
        lexicon.writeFrozenUtf8(ids[i], hashing);
      } else if (kinds[i] == LITERAL) {
        if (ids[i] != 0 || a == b) {
          throw new IllegalArgumentException("noncanonical literal segment");
        }
        hashing.write(literals, a, b - a);
      } else throw new IllegalArgumentException("unknown segment kind");
    }
    return new Frame(root, HexFormat.of().formatHex(digest.digest()),
                     sorted, kinds, ids, offsets, literals);
  }

  /** Validate catalog identity and reconstruct physical bytes only when requested. */
  public static byte[] materialize(Frame frame, Lexicon lexicon) throws IOException {
    Objects.requireNonNull(frame, "frame");
    requireCatalog(frame.catalogRoot, lexicon);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = 0; i < frame.kinds.length; i++) {
      if (frame.kinds[i] == TOKEN) lexicon.writeFrozenUtf8(frame.ids[i], out);
      else out.write(frame.literalBytes, frame.offsets[i], frame.offsets[i + 1] - frame.offsets[i]);
      if (out.size() > MAX_FRAME) throw new IOException("materialized file exceeds budget");
    }
    byte[] body = out.toByteArray();
    if (!frame.contentRoot.equals(HexFormat.of().formatHex(sha256().digest(body)))) {
      throw new IOException("file content hash mismatch");
    }
    return body;
  }

  /** Fixed, deterministic v1 binary representation; includes no repeated dictionary strings. */
  public static byte[] write(Frame frame) throws IOException {
    Objects.requireNonNull(frame, "frame");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataOutputStream data = new DataOutputStream(out);
    data.writeLong(MAGIC);
    data.writeInt(VERSION);
    data.write(HexFormat.of().parseHex(frame.catalogRoot));
    data.write(HexFormat.of().parseHex(frame.contentRoot));
    TreeMap<String, String> sorted = metadata(frame.metadata);
    data.writeInt(sorted.size());
    for (var entry : sorted.entrySet()) {
      writeText(data, entry.getKey());
      writeText(data, entry.getValue());
    }
    data.writeInt(frame.kinds.length);
    data.write(frame.kinds);
    byte[] packed = M3FmtVarints.pack(frame.ids);
    data.writeInt(packed.length);
    data.write(packed);
    for (int offset : frame.offsets) data.writeInt(offset);
    data.writeInt(frame.literalBytes.length);
    data.write(frame.literalBytes);
    data.flush();
    byte[] withoutTrailer = out.toByteArray();
    if (withoutTrailer.length > MAX_FRAME - SHA_LENGTH) {
      throw new IOException("M3FMT frame exceeds budget");
    }
    data.write(sha256().digest(withoutTrailer));
    data.flush();
    return out.toByteArray();
  }

  /** Authenticate entire wire image before allocating or resolving dictionary content. */
  public static Frame read(byte[] encoded, Lexicon lexicon) throws IOException {
    Objects.requireNonNull(encoded, "encoded");
    Objects.requireNonNull(lexicon, "lexicon");
    if (encoded.length < 8 + 4 + SHA_LENGTH * 3 + 4 * 4 || encoded.length > MAX_FRAME) {
      throw new IOException("invalid M3FMT frame size");
    }
    int prefix = encoded.length - SHA_LENGTH;
    byte[] expected = Arrays.copyOfRange(encoded, prefix, encoded.length);
    MessageDigest envelopeDigest = sha256();
    envelopeDigest.update(encoded, 0, prefix);
    if (!MessageDigest.isEqual(expected, envelopeDigest.digest())) {
      throw new IOException("M3FMT envelope checksum mismatch");
    }
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(encoded, 0, prefix));
    try {
      if (input.readLong() != MAGIC || input.readInt() != VERSION) {
        throw new IOException("unsupported M3FMT frame");
      }
      byte[] catalogBytes = input.readNBytes(SHA_LENGTH);
      byte[] contentBytes = input.readNBytes(SHA_LENGTH);
      String catalog = HexFormat.of().formatHex(catalogBytes);
      requireCatalog(catalog, lexicon);
      String expectedContent = HexFormat.of().formatHex(contentBytes);
      int metadataCount = checkedCount(input.readInt(), MAX_METADATA);
      TreeMap<String, String> metadata = new TreeMap<>();
      String previous = null;
      for (int i = 0; i < metadataCount; i++) {
        String key = readText(input);
        String value = readText(input);
        if (previous != null && previous.compareTo(key) >= 0) {
          throw new IOException("duplicate or unordered metadata");
        }
        metadata.put(key, value);
        previous = key;
      }
      int count = checkedCount(input.readInt(), MAX_SEGMENTS);
      if ((long) count > input.available()) throw new IOException("short kind lane");
      byte[] kinds = input.readNBytes(count);
      int packedLength = checkedCount(input.readInt(), Math.min(input.available(), MAX_FRAME));
      byte[] packed = input.readNBytes(packedLength);
      long[] ids = M3FmtVarints.unpack(packed, count);
      if ((long) count + 1L > (long) input.available() / 4L) {
        throw new IOException("short offset lane");
      }
      int[] offsets = new int[count + 1];
      for (int i = 0; i < offsets.length; i++) offsets[i] = input.readInt();
      int literalsLength = checkedCount(input.readInt(), MAX_LITERAL_BYTES);
      if (literalsLength != input.available()) throw new IOException("literal length mismatch");
      byte[] literals = input.readNBytes(literalsLength);
      Frame result = seal(lexicon, metadata, kinds, ids, offsets, literals);
      if (!expectedContent.equals(result.contentRoot)) {
        throw new IOException("decoded file content hash mismatch");
      }
      return result;
    } catch (IllegalArgumentException malformed) {
      throw new IOException("malformed M3FMT frame", malformed);
    }
  }

  private static int checkedCount(int count, int maximum) throws IOException {
    if (count < 0 || count > maximum) throw new IOException("out-of-range frame count");
    return count;
  }
  private static void requireCatalog(String expected, Lexicon lexicon) throws IOException {
    if (!expected.equals(root(lexicon.catalogRoot()))) {
      throw new IOException("shared lexicon catalog identity mismatch");
    }
  }
  private static String root(String hex) {
    if (hex == null || !hex.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("catalog root must be lowercase SHA-256");
    }
    return hex;
  }
  private static TreeMap<String, String> metadata(Map<String, String> source) {
    if (source.size() > MAX_METADATA) throw new IllegalArgumentException("too much metadata");
    TreeMap<String, String> sorted = new TreeMap<>();
    source.forEach((key, value) -> {
      if (key == null || value == null || key.isEmpty()) {
        throw new IllegalArgumentException("invalid metadata");
      }
      try {
        if (utf8(key).length > MAX_TEXT || utf8(value).length > MAX_TEXT) {
          throw new IllegalArgumentException("oversized metadata text");
        }
      } catch (CharacterCodingException invalid) {
        throw new IllegalArgumentException("malformed UTF-16 metadata", invalid);
      }
      sorted.put(key, value);
    });
    return sorted;
  }
  private static byte[] utf8(String value) throws CharacterCodingException {
    ByteBuffer bytes = StandardCharsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .encode(CharBuffer.wrap(value));
    byte[] copy = new byte[bytes.remaining()];
    bytes.get(copy);
    return copy;
  }
  private static void writeText(DataOutputStream out, String text) throws IOException {
    byte[] bytes = utf8(text);
    out.writeInt(bytes.length);
    out.write(bytes);
  }
  private static String readText(DataInputStream input) throws IOException {
    int bytes = checkedCount(input.readInt(), MAX_TEXT);
    if (bytes > input.available()) throw new IOException("truncated metadata");
    try {
      return StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(input.readNBytes(bytes))).toString();
    } catch (CharacterCodingException invalid) {
      throw new IOException("malformed metadata UTF-8", invalid);
    }
  }
  private static MessageDigest sha256() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
}
