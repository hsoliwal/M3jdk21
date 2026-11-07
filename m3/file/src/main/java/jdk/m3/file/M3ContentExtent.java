// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Immutable, source-bound logical content span. Placement and memory mapping do not
 * influence the stable identity; a caller may use the same identity on different hosts.
 * The canonical identity payload is sorted-key JSON and UTF-8 SHA-256, compatible
 * with the ContentExtent v1 identity rules; the schema ids follow JDK naming (m3.content-extent.v1).
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-storage-sdk/src/main/java/com/synexia/storage/ContentExtent.java blob c79f67413996; recipe m3/file/recipe/durable-extent-store-20261107/relocate.py v1
public final class M3ContentExtent {
  public static final String CONTENT_EXTENT_SCHEMA = "m3.content-extent.v1";
  public static final String CONTENT_SOURCE_SCHEMA = "m3.content-source.v1";
  public static final String CONTENT_EXTENT_MANIFEST_SCHEMA = "m3.content-extent-manifest.v1";
  public static final String CONTENT_VALIDATION_RECEIPT_SCHEMA = "m3.content-extent-validation.v1";

  private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");

  private final String sourceId;
  private final String sourceSha256;
  private final long byteStart;
  private final long byteEnd;
  private final String contentSha256;
  private final String logicalKind;
  private final String extentId;

  public M3ContentExtent(String sourceId, String sourceSha256, long byteStart, long byteEnd,
      String contentSha256, String logicalKind) {
    this.sourceId = text(sourceId, "sourceId");
    this.sourceSha256 = digest(sourceSha256, "sourceSha256");
    this.byteStart = offset(byteStart, "byteStart");
    this.byteEnd = offset(byteEnd, "byteEnd");
    if (this.byteEnd < this.byteStart) {
      throw new IllegalArgumentException("byteEnd precedes byteStart");
    }
    this.contentSha256 = digest(contentSha256, "contentSha256");
    this.logicalKind = text(logicalKind, "logicalKind");
    this.extentId = semanticHash(identityPayload());
  }

  public static M3ContentExtent fromBytes(String sourceId, String sourceSha256,
      long byteStart, byte[] content, String logicalKind) {
    Objects.requireNonNull(content, "content");
    long end;
    try {
      end = Math.addExact(offset(byteStart, "byteStart"), content.length);
    } catch (ArithmeticException overflow) {
      throw new IllegalArgumentException("byte range overflows", overflow);
    }
    return new M3ContentExtent(sourceId, sourceSha256, byteStart, end, sha256(content), logicalKind);
  }

  public static String contentSourceId(String sourceSha256, long byteSize) {
    return semanticHash(Map.of(
        "schema", CONTENT_SOURCE_SCHEMA,
        "content_sha256", digest(sourceSha256, "sourceSha256"),
        "byte_size", offset(byteSize, "byteSize")));
  }

  public String sourceId() { return sourceId; }
  public String sourceSha256() { return sourceSha256; }
  public long byteStart() { return byteStart; }
  public long byteEnd() { return byteEnd; }
  public long byteLength() { return byteEnd - byteStart; }
  public String contentSha256() { return contentSha256; }
  public String logicalKind() { return logicalKind; }
  public String schema() { return CONTENT_EXTENT_SCHEMA; }
  public String extentId() { return extentId; }

  public Map<String, Object> identityPayload() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("schema", CONTENT_EXTENT_SCHEMA);
    payload.put("source_id", sourceId);
    payload.put("source_sha256", sourceSha256);
    payload.put("byte_start", byteStart);
    payload.put("byte_end", byteEnd);
    payload.put("content_sha256", contentSha256);
    payload.put("logical_kind", logicalKind);
    return Map.copyOf(payload);
  }

  public Map<String, Object> evidenceRecord() {
    return Map.of("unit_id", extentId, "source_sha256", sourceSha256,
        "byte_start", byteStart, "byte_end", byteEnd, "raw_sha256", contentSha256);
  }

  public Map<String, Object> receipt() {
    Map<String, Object> record = new LinkedHashMap<>(identityPayload());
    record.put("extent_id", extentId);
    record.put("model_calls", 0L);
    record.put("network_calls", 0L);
    return Map.copyOf(record);
  }

  public static Map<String, Object> validateContent(M3ContentExtent extent, byte[] content) {
    Objects.requireNonNull(extent, "extent");
    Objects.requireNonNull(content, "content");
    if (extent.byteLength() != content.length) {
      throw new ContentTamperedException("extent length mismatch");
    }
    String actual = sha256(content);
    if (!actual.equals(extent.contentSha256)) {
      throw new ContentTamperedException("extent content SHA-256 mismatch");
    }
    return Map.of("schema", CONTENT_VALIDATION_RECEIPT_SCHEMA, "status", "VERIFIED",
        "extent_id", extent.extentId, "content_sha256", actual,
        "model_calls", 0L, "network_calls", 0L);
  }

  public static final class ContentExtentManifest {
    private final List<M3ContentExtent> extents;
    private final String root;

    public ContentExtentManifest(Collection<M3ContentExtent> values) {
      Objects.requireNonNull(values, "values");
      ArrayList<M3ContentExtent> copy = new ArrayList<>(values.size());
      for (M3ContentExtent extent : values) copy.add(Objects.requireNonNull(extent, "extent"));
      copy.sort(Comparator.comparing(M3ContentExtent::sourceId)
          .thenComparingLong(M3ContentExtent::byteStart)
          .thenComparingLong(M3ContentExtent::byteEnd)
          .thenComparing(M3ContentExtent::logicalKind)
          .thenComparing(M3ContentExtent::contentSha256)
          .thenComparing(M3ContentExtent::extentId));
      for (int i = 1; i < copy.size(); i++) {
        if (copy.get(i - 1).extentId.equals(copy.get(i).extentId)) {
          throw new IllegalArgumentException("duplicate extent identity in manifest");
        }
      }
      extents = List.copyOf(copy);
      root = semanticHash(Map.of("schema", CONTENT_EXTENT_MANIFEST_SCHEMA,
          "ordered_extent_ids", orderedExtentIds()));
    }

    public List<M3ContentExtent> extents() { return extents; }
    public List<String> orderedExtentIds() {
      return extents.stream().map(M3ContentExtent::extentId).toList();
    }
    public String manifestRoot() { return root; }
    public String root() { return root; }
    public Map<String, Object> receipt() {
      return Map.of("schema", CONTENT_EXTENT_MANIFEST_SCHEMA, "manifest_root", root,
          "extent_count", (long) extents.size(), "ordered_extent_ids", orderedExtentIds(),
          "model_calls", 0L, "network_calls", 0L);
    }
  }

  public static ContentExtentManifest buildManifest(Collection<M3ContentExtent> extents) {
    return new ContentExtentManifest(extents);
  }

  public static final class ContentTamperedException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    public ContentTamperedException(String message) { super(message); }
  }

  public static String sha256(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes");
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException failure) {
      throw new ExceptionInInitializerError(failure);
    }
  }

  private static String semanticHash(Object value) {
    return sha256(canonicalJson(value).getBytes(StandardCharsets.UTF_8));
  }

  private static String canonicalJson(Object value) {
    if (value instanceof String s) return jsonString(s);
    if (value instanceof Number || value instanceof Boolean) return value.toString();
    if (value instanceof Map<?, ?> map) {
      TreeMap<String, Object> sorted = new TreeMap<>();
      map.forEach((key, entry) -> sorted.put(String.valueOf(key), entry));
      StringBuilder result = new StringBuilder("{");
      for (Map.Entry<String, Object> entry : sorted.entrySet()) {
        if (result.length() > 1) result.append(',');
        result.append(jsonString(entry.getKey())).append(':').append(canonicalJson(entry.getValue()));
      }
      return result.append('}').toString();
    }
    if (value instanceof Collection<?> elements) {
      StringBuilder result = new StringBuilder("[");
      for (Object element : elements) {
        if (result.length() > 1) result.append(',');
        result.append(canonicalJson(element));
      }
      return result.append(']').toString();
    }
    throw new IllegalArgumentException("unsupported canonical identity value");
  }

  private static String jsonString(String value) {
    StringBuilder result = new StringBuilder(value.length() + 2).append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> result.append("\\\"");
        case '\\' -> result.append("\\\\");
        case '\b' -> result.append("\\b");
        case '\f' -> result.append("\\f");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        default -> {
          if (c < 0x20) {
            result.append("\\u").append(HexFormat.of().toHexDigits((short) c).substring(0, 4));
          } else {
            result.append(c);
          }
        }
      }
    }
    return result.append('"').toString();
  }

  private static String text(String value, String label) {
    if (value == null || value.isEmpty() || value.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(label + " must be nonempty text without NUL");
    }
    return value;
  }

  private static String digest(String value, String label) {
    if (value == null || !DIGEST.matcher(value).matches()) {
      throw new IllegalArgumentException(label + " must be a lowercase SHA-256 digest");
    }
    return value;
  }

  private static long offset(long value, String label) {
    if (value < 0) throw new IllegalArgumentException(label + " must not be negative");
    return value;
  }

  @Override public boolean equals(Object other) {
    if (!(other instanceof M3ContentExtent extent)) return false;
    return sourceId.equals(extent.sourceId) && sourceSha256.equals(extent.sourceSha256)
        && byteStart == extent.byteStart && byteEnd == extent.byteEnd
        && contentSha256.equals(extent.contentSha256) && logicalKind.equals(extent.logicalKind);
  }

  @Override public int hashCode() {
    return Objects.hash(sourceId, sourceSha256, byteStart, byteEnd, contentSha256, logicalKind);
  }

  @Override public String toString() {
    return "M3ContentExtent{" + extentId + ", " + byteStart + ".." + byteEnd + ", " + logicalKind + "}";
  }
}
