// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

/** Meaning of the canonical MIndexString label lane for one AST kind. */
public enum MIndexLabelPolicy {
  NONE(false),
  IDENTIFIER(true),
  DECLARATION_NAME(true),
  LITERAL_VALUE(true),
  PRIMITIVE_TYPE(true),
  SOURCE_LABEL(true);

  private final boolean labelled;

  MIndexLabelPolicy(boolean labelled) {
    this.labelled = labelled;
  }

  public boolean isLabelled() {
    return labelled;
  }
}
