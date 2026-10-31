// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarParseResult.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Objects;
import java.util.Optional;

/** Validation plus an optional deterministic parse tree. */
public final class M3GrammarParseResult {
  private final M3GrammarValidation validation;
  private final M3GrammarParseTree tree;

  M3GrammarParseResult(M3GrammarValidation validation, M3GrammarParseTree tree) {
    this.validation = Objects.requireNonNull(validation, "validation");
    this.tree = tree;
  }

  public M3GrammarValidation validation() { return validation; }
  public boolean accepted() { return validation.accepted(); }
  public boolean conforms() { return validation.conforms(); }
  public Optional<M3GrammarParseTree> tree() { return Optional.ofNullable(tree); }

  public M3GrammarParseTree requireTree() {
    if (tree == null) throw new M3GrammarException("no parse tree: " + validation);
    return tree;
  }
}
