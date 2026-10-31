// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import java.util.Objects;

/** Fast classification of two immutable AST roots using precomputed candidate hashes plus proof. */
public final class MIndexASTDiff {
  private MIndexASTDiff() {}

  public enum Classification {
    IDENTICAL,
    LABEL_ONLY,
    NORMALIZED_LOGIC_CHANGED,
    STRUCTURE_CHANGED
  }

  public record Result(
      Classification classification,
      long leftExactHash,
      long rightExactHash,
      long leftStructuralHash,
      long rightStructuralHash,
      long leftLogicHash,
      long rightLogicHash,
      int simHashDistance,
      int leftExpandedNodes,
      int rightExpandedNodes) {}

  public static Result compare(MIndexAST left, MIndexAST right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");

    Classification classification;
    if (left.contentEquals(right)) {
      classification = Classification.IDENTICAL;
    } else if (left.structurallyEquals(right)) {
      classification =
          left.normalizedLogicEquals(right)
              ? Classification.LABEL_ONLY
              : Classification.NORMALIZED_LOGIC_CHANGED;
    } else {
      classification = Classification.STRUCTURE_CHANGED;
    }

    return new Result(
        classification,
        left.exactHash64(),
        right.exactHash64(),
        left.structuralHash64(),
        right.structuralHash64(),
        left.logicHash64(),
        right.logicHash64(),
        MIndexASTPool.hamming64(left.simHash64(), right.simHash64()),
        left.expandedNodeCount(),
        right.expandedNodeCount());
  }
}
