// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import com.synexia.indexstring.grammar.MIndexFormalLanguageSpec;
import com.synexia.indexstring.grammar.MIndexFormalLanguageSpecs;
import com.synexia.indexstring.grammar.MIndexLang;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

/**
 * Immutable process-level source-language specification exposed to every MIndexAST.
 *
 * <p>Implementations are frozen images. Callers query them; they are never loaded per file or per
 * node.</p>
 */
public interface MIndexLanguageSpec {
  /** Stable cache/debug identity such as java-21. */
  String identity();
  String language();
  int release();
  SourceVersion sourceVersion();

  int ruleCount();
  MIndexLanguageRule rule(Tree.Kind kind);
  MIndexLanguageRule rule(int ruleIndex);

  int keywordCount();
  String keywordAt(int index);
  boolean isKeyword(CharSequence token);

  int contextualKeywordCount();
  String contextualKeywordAt(int index);
  boolean isContextualKeyword(CharSequence token);

  int literalTokenCount();
  String literalTokenAt(int index);
  boolean isLiteralToken(CharSequence token);

  int primitiveTypeCount();
  String primitiveTypeAt(int index);
  boolean isPrimitiveType(CharSequence token);

  int modifierCount();
  String modifierAt(int index);
  boolean isModifierKeyword(CharSequence token);
  long modifierMask();

  /** Universal process-level AST spec derived from this source language and formal grammar. */
  default MIndexASTSpecPrecompute astSpec() {
    return MIndexASTSpecs.forLang(MIndexLang.source(this, formalExpressionSpec()));
  }

  /** Frozen grammar-production AST construction plans for this source language. */
  default MIndexASTProductionPlans astProductionPlans() {
    return astSpec().productionPlans();
  }

  default MIndexASTProductionDispatch astProductionDispatch() {
    return astSpec().productionDispatch();
  }

  /**
   * Immutable formal grammar for Java-style expressions.
   *
   * <p>The full javac Tree.Kind rule image remains the authority for complete Java AST nodes;
   * this grammar is the direct expression-conformance surface shared with other formal languages.</p>
   */
  default MIndexFormalLanguageSpec formalExpressionSpec() {
    return MIndexFormalLanguageSpecs.programmingExpression();
  }

  default boolean isReservedToken(CharSequence token) {
    return isKeyword(token) || isLiteralToken(token);
  }

  default long modifierBit(Modifier modifier) {
    if (modifier.ordinal() >= Long.SIZE) {
      throw new IllegalArgumentException("modifier ordinal exceeds 64-bit flag lane");
    }
    return 1L << modifier.ordinal();
  }

  default boolean hasModifier(long flags, Modifier modifier) {
    return (flags & modifierBit(modifier)) != 0L;
  }
}
