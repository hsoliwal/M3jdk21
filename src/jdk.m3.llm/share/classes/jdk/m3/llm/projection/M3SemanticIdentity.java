// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Versioned identity for exact segmented context and its immutable semantic authorities.
 *
 * <p>Only hashes, counts and coordinates survive construction. Source text is consumed through
 * {@link CharSequence#charAt(int)} without concatenation, UTF-8 replacement or normalization.
 * Callers must keep a supplied CharSequence stable while it is consumed. A root is an identity
 * claim, not proof of model quality, equality of arbitrary payloads, or publication authority.
 */
public final class M3SemanticIdentity {
  public static final int FORMAT_VERSION = 1;

  /** Exact artifact identities; names alone cannot identify learned or configured behavior. */
  public record Model(
      String weightsRoot,
      String tokenizerRoot,
      String configurationRoot,
      String backendVersionRoot) {
    public Model {
      weightsRoot = requireRoot(weightsRoot, "weightsRoot");
      tokenizerRoot = requireRoot(tokenizerRoot, "tokenizerRoot");
      configurationRoot = requireRoot(configurationRoot, "configurationRoot");
      backendVersionRoot = requireRoot(backendVersionRoot, "backendVersionRoot");
    }
  }

  /** Half-open UTF-16 coordinates in one immutable owner/snapshot context. */
  public record Scope(
      String ownerRoot,
      String snapshotRoot,
      String contextRoot,
      long sourceStart,
      long sourceEnd,
      long contextStart,
      long contextEnd) {
    public Scope {
      ownerRoot = requireRoot(ownerRoot, "ownerRoot");
      snapshotRoot = requireRoot(snapshotRoot, "snapshotRoot");
      contextRoot = requireRoot(contextRoot, "contextRoot");
      if (contextStart < 0 || contextEnd < contextStart
          || sourceStart < contextStart || sourceEnd < sourceStart || sourceEnd > contextEnd) {
        throw new IllegalArgumentException("source range must be contained in the context range");
      }
    }

    /** A whole context with no separately supplied owner/snapshot metadata. */
    public static Scope whole(String contentRoot, long utf16Length) {
      return new Scope(contentRoot, contentRoot, contentRoot, 0, utf16Length, 0, utf16Length);
    }

    public void requireContextLength(long utf16Length) {
      if (utf16Length < 0 || contextEnd - contextStart != utf16Length) {
        throw new IllegalArgumentException("context range does not match the complete evaluated text");
      }
    }
  }

  private final String rootHash;
  private final String stateRoot;
  private final String schemaRoot;
  private final Model model;
  private final Scope scope;
  private final int segmentCount;
  private final long utf16Length;

  private M3SemanticIdentity(
      String rootHash, String stateRoot, String schemaRoot, Model model, Scope scope,
      int segmentCount, long utf16Length) {
    this.rootHash = rootHash;
    this.stateRoot = stateRoot;
    this.schemaRoot = schemaRoot;
    this.model = model;
    this.scope = scope;
    this.segmentCount = segmentCount;
    this.utf16Length = utf16Length;
  }

  public String rootHash() { return rootHash; }
  public String stateRoot() { return stateRoot; }
  public String schemaRoot() { return schemaRoot; }
  public Model model() { return model; }
  public Scope scope() { return scope; }
  public int segmentCount() { return segmentCount; }
  public long utf16Length() { return utf16Length; }

  /** Value equality compares the complete versioned identity root. */
  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof M3SemanticIdentity value
        && rootHash.equals(value.rootHash);
  }

  @Override
  public int hashCode() { return rootHash.hashCode(); }

  public static Builder builder(
      CharSequence modelName, Model model, CharSequence backendId,
      String schemaRoot, int segmentCount) {
    return new Builder(modelName, model, backendId, schemaRoot, segmentCount, null);
  }

  public static Builder builder(
      CharSequence modelName, Model model, CharSequence backendId,
      String schemaRoot, int segmentCount, Scope scope) {
    return new Builder(modelName, model, backendId, schemaRoot, segmentCount,
        Objects.requireNonNull(scope, "scope"));
  }

  /** Streaming, single-use builder. A supplied context root never replaces the actual text root. */
  public static final class Builder {
    private final String modelNameRoot;
    private final String backendIdRoot;
    private final Model model;
    private final String schemaRoot;
    private final int expectedSegments;
    private final Scope suppliedScope;
    private final Hasher state;
    private int segments;
    private long units;
    private boolean closed;

    private Builder(
        CharSequence modelName, Model model, CharSequence backendId,
        String schemaRoot, int segmentCount, Scope scope) {
      requireName(modelName, "modelName");
      requireName(backendId, "backendId");
      this.model = Objects.requireNonNull(model, "model");
      this.schemaRoot = requireRoot(schemaRoot, "schemaRoot");
      if (segmentCount < 0) throw new IllegalArgumentException("negative segment count");
      modelNameRoot = hasher("SYNEXIA_SEMANTIC_MODEL_NAME_V1").text(modelName).finish();
      backendIdRoot = hasher("SYNEXIA_SEMANTIC_BACKEND_ID_V1").text(backendId).finish();
      expectedSegments = segmentCount;
      suppliedScope = scope;
      state = hasher("SYNEXIA_SEMANTIC_SEGMENTS_V1").integer(segmentCount);
    }

    public Builder segment(CharSequence source) {
      Objects.requireNonNull(source, "source");
      return segment(source, 0, source.length());
    }

    /** Bind both the selected code units and the source slice coordinates. */
    public Builder segment(CharSequence source, int start, int end) {
      return addSegment(source, start, end, null);
    }

    /** Also bind a caller-framed digest of immutable metadata visible to the evaluator. */
    public Builder segment(CharSequence source, int start, int end, String metadataRoot) {
      return addSegment(source, start, end, requireRoot(metadataRoot, "segment metadata root"));
    }

    private Builder addSegment(CharSequence source, int start, int end, String metadataRoot) {
      requireOpen();
      Objects.requireNonNull(source, "source");
      Objects.checkFromToIndex(start, end, source.length());
      if (segments >= expectedSegments) throw new IllegalStateException("too many context segments");
      try {
        state.integer(start).integer(end).integer(metadataRoot == null ? 0 : 1);
        if (metadataRoot != null) state.root(metadataRoot);
        state.text(source, start, end);
        units = Math.addExact(units, (long) end - start);
        segments++;
        return this;
      } catch (RuntimeException | Error failure) {
        closed = true;
        throw failure;
      }
    }

    public M3SemanticIdentity build() {
      requireOpen();
      closed = true;
      if (segments != expectedSegments) throw new IllegalStateException("missing context segments");
      String stateRoot = state.finish();
      Scope scope = suppliedScope == null ? Scope.whole(stateRoot, units) : suppliedScope;
      scope.requireContextLength(units);
      String root = hasher("SYNEXIA_SEMANTIC_IDENTITY_V1")
          .integer(FORMAT_VERSION)
          .root(modelNameRoot).root(backendIdRoot)
          .root(model.weightsRoot()).root(model.tokenizerRoot())
          .root(model.configurationRoot()).root(model.backendVersionRoot())
          .root(schemaRoot).root(stateRoot)
          .root(scope.ownerRoot()).root(scope.snapshotRoot()).root(scope.contextRoot())
          .longValue(scope.sourceStart()).longValue(scope.sourceEnd())
          .longValue(scope.contextStart()).longValue(scope.contextEnd())
          .integer(segments).longValue(units).finish();
      return new M3SemanticIdentity(root, stateRoot, schemaRoot, model, scope, segments, units);
    }

    private void requireOpen() {
      if (closed) throw new IllegalStateException("semantic identity builder is closed");
    }
  }

  public static Hasher hasher(String domain) { return new Hasher(domain); }

  /**
   * Typed, length-framed SHA-256 stream. Integers use big-endian bytes; text uses exact unsigned
   * UTF-16 code units. Null, empty, field boundaries and numeric field types remain distinct.
   */
  public static final class Hasher {
    private final MessageDigest digest;
    private final byte[] scratch = new byte[Long.BYTES];
    private boolean closed;

    private Hasher(String domain) {
      requireName(domain, "domain");
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException impossible) {
        throw new IllegalStateException("SHA-256 unavailable", impossible);
      }
      digest.update(new byte[] {'M', '3', 'S', 'I', 1});
      text(domain);
    }

    public Hasher integer(int value) {
      requireOpen();
      digest.update((byte) 1);
      number(value, Integer.BYTES);
      return this;
    }

    public Hasher longValue(long value) {
      requireOpen();
      digest.update((byte) 2);
      number(value, Long.BYTES);
      return this;
    }

    public Hasher text(CharSequence source) {
      Objects.requireNonNull(source, "source");
      return text(source, 0, source.length());
    }

    public Hasher text(CharSequence source, int start, int end) {
      requireOpen();
      Objects.requireNonNull(source, "source");
      int originalLength = source.length();
      Objects.checkFromToIndex(start, end, originalLength);
      digest.update((byte) 3);
      number(end - start, Integer.BYTES);
      try {
        for (int index = start; index < end; index++) {
          char unit = source.charAt(index);
          scratch[0] = (byte) (unit >>> 8);
          scratch[1] = (byte) unit;
          digest.update(scratch, 0, Character.BYTES);
        }
        if (source.length() != originalLength) {
          throw new IllegalArgumentException("source length changed while hashing");
        }
        return this;
      } catch (RuntimeException | Error failure) {
        closed = true;
        throw failure;
      }
    }

    public Hasher nullableText(CharSequence source) {
      requireOpen();
      try {
        digest.update((byte) 4);
        digest.update((byte) (source == null ? 0 : 1));
        return source == null ? this : text(source);
      } catch (RuntimeException | Error failure) {
        closed = true;
        throw failure;
      }
    }

    public Hasher root(String root) {
      requireOpen();
      String checked = requireRoot(root, "root");
      digest.update((byte) 5);
      digest.update(HexFormat.of().parseHex(checked));
      return this;
    }

    public String finish() {
      requireOpen();
      closed = true;
      return HexFormat.of().formatHex(digest.digest());
    }

    private void number(long value, int bytes) {
      for (int index = bytes - 1; index >= 0; index--) {
        scratch[index] = (byte) value;
        value >>>= Byte.SIZE;
      }
      digest.update(scratch, 0, bytes);
    }

    private void requireOpen() {
      if (closed) throw new IllegalStateException("semantic identity digest is closed");
    }
  }

  private static String requireRoot(String value, String field) {
    if (value == null || !value.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(field + " must be lowercase SHA-256");
    }
    return value;
  }

  private static void requireName(CharSequence value, String field) {
    Objects.requireNonNull(value, field);
    for (int index = 0; index < value.length(); index++) {
      if (!Character.isWhitespace(value.charAt(index))) return;
    }
    throw new IllegalArgumentException(field + " must not be blank");
  }
}
