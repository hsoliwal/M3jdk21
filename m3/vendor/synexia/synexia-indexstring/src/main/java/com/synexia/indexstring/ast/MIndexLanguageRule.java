// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import java.util.Objects;

/**
 * One immutable rule row in a language-spec image.
 *
 * <p>The rule index is the Tree.Kind ordinal, so an MIndexAST does not retain a separate rule ID.
 * Category and operator metadata are precomputed once when the process loads the spec singleton.</p>
 */
public record MIndexLanguageRule(
    int ruleIndex,
    Tree.Kind kind,
    long categoryMask,
    MIndexLabelPolicy labelPolicy,
    MIndexFlagPolicy flagPolicy,
    MIndexOperatorRule operator,
    int minimumRelease,
    boolean preview) {

  public MIndexLanguageRule {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(labelPolicy, "labelPolicy");
    Objects.requireNonNull(flagPolicy, "flagPolicy");
    Objects.requireNonNull(operator, "operator");
    if (ruleIndex != kind.ordinal()) {
      throw new IllegalArgumentException("rule index must equal Tree.Kind ordinal");
    }
    if (minimumRelease < 1) throw new IllegalArgumentException("minimum release must be positive");
  }

  public boolean is(MIndexSyntaxCategory category) {
    Objects.requireNonNull(category, "category");
    return (categoryMask & category.bit()) != 0L;
  }

  public boolean isOperator() {
    return operator.isOperator();
  }
}
