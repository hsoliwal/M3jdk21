// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

/** Orthogonal precomputed categories for Java compiler-tree rules. */
public enum MIndexSyntaxCategory {
  COMPILATION,
  DECLARATION,
  EXPRESSION,
  STATEMENT,
  TYPE,
  PATTERN,
  DIRECTIVE,
  LITERAL,
  OPERATOR,
  CONTROL_FLOW,
  NAME,
  ANNOTATION,
  MODULE,
  ERROR,
  OTHER;

  public long bit() {
    return 1L << ordinal();
  }
}
