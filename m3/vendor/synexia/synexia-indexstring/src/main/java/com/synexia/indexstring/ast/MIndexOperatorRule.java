// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import java.util.Objects;

/**
 * Frozen operator metadata.
 *
 * <p>Higher precedence binds more tightly. Precedence 0 and arity 0 identify a non-operator.
 * The table is a source-language lookup aid; javac remains the parser/semantic authority.</p>
 */
public record MIndexOperatorRule(
    String token,
    int precedence,
    MIndexAssociativity associativity,
    int arity,
    boolean shortCircuit,
    boolean assignment) {

  public static final MIndexOperatorRule NONE =
      new MIndexOperatorRule("", 0, MIndexAssociativity.NONE, 0, false, false);

  public MIndexOperatorRule {
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(associativity, "associativity");
    if (precedence < 0) throw new IllegalArgumentException("negative precedence");
    if (arity < 0 || arity > 3) throw new IllegalArgumentException("operator arity must be in [0,3]");
    if (arity == 0 && precedence != 0) {
      throw new IllegalArgumentException("non-operator must use precedence 0");
    }
  }

  public boolean isOperator() {
    return arity != 0;
  }
}
