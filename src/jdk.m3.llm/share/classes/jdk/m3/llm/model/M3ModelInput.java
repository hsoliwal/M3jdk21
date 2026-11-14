// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
// M3JDK adaptation: immutable String boundary; no additional text storage or exported String internals.
package jdk.m3.llm.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * An exact immutable String input with explicit model, tokenizer, schema and context identities.
 *
 * <p>The original String is retained directly. On M3JDK21 it continues to use the ordinary String
 * owner; this value creates no second text pool and exposes no internal M3String coordinate.
 * All UTF-16 code units, including unpaired surrogates, contribute to the digest. Hash screening
 * is always followed by exact equality when validating an input match.</p>
 *
 * <p>The context root must bind all caller state affecting the operation, including source or
 * policy generations when relevant. A cached model result never acquires source-substitution
 * authority merely because its input matches.</p>
 */
public final class M3ModelInput {
  private final String text;
  private final Namespace namespace;
  private final String textRoot;
  private final String rootHash;

  /** Exact dependency versions; callers must supply content roots, never floating model aliases. */
  public record Namespace(
      String modelRoot, String tokenizerRoot, String schemaRoot, String contextRoot) {
    public Namespace {
      modelRoot = requireRoot(modelRoot, "modelRoot");
      tokenizerRoot = requireRoot(tokenizerRoot, "tokenizerRoot");
      schemaRoot = requireRoot(schemaRoot, "schemaRoot");
      contextRoot = requireRoot(contextRoot, "contextRoot");
    }
  }

  private M3ModelInput(String text, Namespace namespace) {
    this.text = Objects.requireNonNull(text, "text");
    this.namespace = Objects.requireNonNull(namespace, "namespace");
    ExactHash content = new ExactHash("SYNEXIA_MINDEX_MODEL_TEXT_UTF16_V1");
    content.text(text);
    textRoot = content.finish();
    // Deliberately distinct from the Synexia indexed-input domain: internal ownership lanes
    // are not exported through the ordinary String boundary.
    ExactHash identity = new ExactHash("M3JDK_MODEL_INPUT_V1");
    identity.text(namespace.modelRoot());
    identity.text(namespace.tokenizerRoot());
    identity.text(namespace.schemaRoot());
    identity.text(namespace.contextRoot());
    identity.text(textRoot);
    rootHash = identity.finish();
  }

  public static M3ModelInput of(String text, Namespace namespace) {
    return new M3ModelInput(text, namespace);
  }

  public static M3ModelInput of(
      String text, String modelRoot, String tokenizerRoot, String schemaRoot, String contextRoot) {
    return of(text, new Namespace(modelRoot, tokenizerRoot, schemaRoot, contextRoot));
  }

  public String text() { return text; }
  public Namespace namespace() { return namespace; }
  public String textRoot() { return textRoot; }
  public String rootHash() { return rootHash; }

  /** Owned primitive metadata arrays: none. Excludes text, digest Strings and object headers. */
  public long retainedBytes() { return 0L; }

  /** Exact text equality, independent of model configuration. */
  public boolean sameText(M3ModelInput other) {
    return other != null && textRoot.equals(other.textRoot) && text.equals(other.text);
  }

  /** Digest screening followed by exact namespace and UTF-16 equality. */
  public boolean matches(M3ModelInput other) {
    return other != null && rootHash.equals(other.rootHash)
        && namespace.equals(other.namespace) && sameText(other);
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof M3ModelInput input && matches(input);
  }

  @Override
  public int hashCode() { return rootHash.hashCode(); }

  static String requireRoot(String value, String field) {
    if (value == null || value.length() != 64) {
      throw new IllegalArgumentException(field + " must be a lower-case SHA-256 root");
    }
    for (int index = 0; index < value.length(); index++) {
      char digit = value.charAt(index);
      if (!(digit >= '0' && digit <= '9' || digit >= 'a' && digit <= 'f')) {
        throw new IllegalArgumentException(field + " must be a lower-case SHA-256 root");
      }
    }
    return value;
  }

  /** Framed, big-endian UTF-16 hashing; package-private so legacy hash domains stay unchanged. */
  static final class ExactHash {
    private final MessageDigest digest;

    ExactHash(String domain) {
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException impossible) {
        throw new IllegalStateException("SHA-256 unavailable", impossible);
      }
      text(domain);
    }

    void integer(int value) {
      digest.update((byte) (value >>> 24));
      digest.update((byte) (value >>> 16));
      digest.update((byte) (value >>> 8));
      digest.update((byte) value);
    }

    void longValue(long value) {
      integer((int) (value >>> 32));
      integer((int) value);
    }

    void character(char value) {
      digest.update((byte) (value >>> 8));
      digest.update((byte) value);
    }

    void text(CharSequence value) {
      if (value == null) {
        integer(-1);
      } else {
        integer(value.length());
        for (int index = 0; index < value.length(); index++) character(value.charAt(index));
      }
    }

    String finish() { return HexFormat.of().formatHex(digest.digest()); }
  }
}
