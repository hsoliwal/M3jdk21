// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

/**
 * Minimal immutable occurrence/topology membrane consumed by file-local atomization.
 *
 * <p>The source may be a javac document, a mapped AST image, or a server-side parser result. It
 * exposes no parser or rewrite authority, only the stable rows required to partition and verify a
 * workset.
 */
public interface MIndexASTOccurrenceSource {
  /**
   * Stable, immutable atom-handle domain used for exact in-process partial syntax reuse.
   * Default isolation forbids cross-source reuse. An override may return an existing append-only
   * AST pool only when a handle always denotes the same immutable syntax for that owner's lifetime.
   * This identity must never be replaced by a hash, name or evicting cache generation.
   */
  default Object syntaxIdentityDomain() { return this; }

  String sourceUtf16Sha256Hex();

  long rootExactHash64();

  long rootStructuralHash64();

  long rootLogicHash64();

  int occurrenceCount();

  int atomHandleAtOccurrence(int occurrence);

  long startPosition(int occurrence);

  long endPosition(int occurrence);

  int parentOccurrence(int occurrence);

  int firstChildOccurrence(int occurrence);

  int nextSiblingOccurrence(int occurrence);

  int depth(int occurrence);
}
