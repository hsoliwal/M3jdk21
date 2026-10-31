// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexGrammarSymbolKind;
import com.synexia.indexstring.grammar.MIndexLiteralCase;
import com.synexia.indexstring.grammar.MIndexTokenClass;
import java.util.Optional;

/**
 * Read-only compiled AST-spec image.
 *
 * <p>Heap and mmap implementations expose the same immutable language/spec facts. The image is
 * derived from a canonical MIndex grammar/spec and contains no source-instance state.</p>
 */
public interface MIndexASTSpecImageView {
  byte DISPATCH_PRESENT = 1;
  byte DISPATCH_AMBIGUOUS = 1 << 1;
  String identity();
  String language();
  String version();

  long languageFingerprint();
  long grammarFingerprint();
  long astSpecFingerprint();
  long productionPlanFingerprint();
  long contentFingerprint();

  int symbolCount();
  int productionCount();
  int operatorCount();
  int sourceRuleCount();

  MIndexGrammarSymbolKind symbolKind(int symbol);
  Optional<MIndexTokenClass> tokenClass(int symbol);
  MIndexLiteralCase literalCase(int symbol);
  boolean nullable(int symbol);
  boolean productive(int symbol);
  boolean reachable(int symbol);
  boolean leftRecursive(int symbol);
  boolean canEndAfter(int symbol);
  boolean operatorTerminal(int symbol);
  int minimumTokens(int symbol);
  long symbolHash64(int symbol);
  int[] firstSymbols(int symbol);
  int[] followSymbols(int symbol);
  int[] productionsUsingSymbol(int symbol);
  int operatorIndexForSymbol(int symbol);

  int productionLhs(int production);
  int productionRhsLength(int production);
  int productionRhsSymbol(int production, int ordinal);
  int productionMinimumTokens(int production);
  boolean productionCanEndAtEof(int production);
  long productionHash64(int production);
  int[] predictionSymbols(int production);

  MIndexASTChildRole childRole(int production, int ordinal);
  boolean childNullable(int production, int ordinal);
  boolean nullablePrefixBefore(int production, int ordinal);
  boolean nullableSuffixAfter(int production, int ordinal);
  int minimumTokensBefore(int production, int ordinal);
  int minimumTokensAfter(int production, int ordinal);
  boolean directOperator(int production, int ordinal);
  int operatorIndex(int production, int ordinal);
  int terminalChildCount(int production);
  int nonTerminalChildCount(int production);
  int directOperatorCount(int production);
  int[] directOperatorOrdinals(int production);
  long shapeHash64(int production);
  long planHash64(int production);
  int[] productionsForLhs(int nonTerminalSymbol);
  int[] productionsPredicting(int terminalSymbol);

  boolean derivesTerminal(int symbol, int terminalSymbol);
  int[] derivableTerminals(int symbol);
  boolean productionDerivesTerminal(int production, int terminalSymbol);
  int[] productionDerivableTerminals(int production);

  boolean derivesOperator(int symbol, int operatorIndex);
  int[] derivableOperators(int symbol);
  boolean productionDerivesOperator(int production, int operatorIndex);
  int[] productionDerivableOperators(int production);

  int sourceRuleOperatorIndex(int ruleIndex);

  long productionDispatchFingerprint();
  int productionDispatchEntryCount();
  int productionDispatchUniqueEntryCount();
  int productionDispatchAmbiguousEntryCount();
  int productionDispatchEntryCount(int nonTerminalSymbol);
  int[] predictedTerminals(int nonTerminalSymbol);
  boolean mayPredict(int nonTerminalSymbol, int terminalSymbol);
  int dispatchCandidateCount(int nonTerminalSymbol, int terminalSymbol);
  int[] candidateProductions(int nonTerminalSymbol, int terminalSymbol);
  int uniqueProductionOrMinusOne(int nonTerminalSymbol, int terminalSymbol);
  boolean dispatchAmbiguous(int nonTerminalSymbol, int terminalSymbol);
  int[] eofCandidateProductions(int nonTerminalSymbol);
  int uniqueEofProductionOrMinusOne(int nonTerminalSymbol);
  boolean eofAmbiguous(int nonTerminalSymbol);

  /**
   * Allocation-free batch predictive dispatch.
   *
   * <p>flags uses {@link #DISPATCH_PRESENT} and {@link #DISPATCH_AMBIGUOUS}. The default
   * implementation is the canonical scalar Java oracle; heap/mmap implementations may override
   * it with behavior-identical JNI acceleration.</p>
   */
  default void batchDispatch(
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    requireBatchLengths(
        nonTerminals, terminals, candidateCounts, uniqueProductions, flags);
    for (int index = 0; index < nonTerminals.length; index++) {
      int count = dispatchCandidateCount(nonTerminals[index], terminals[index]);
      candidateCounts[index] = count;
      uniqueProductions[index] =
          count == 1
              ? uniqueProductionOrMinusOne(nonTerminals[index], terminals[index])
              : -1;
      flags[index] =
          count == 0
              ? 0
              : (byte)
                  (DISPATCH_PRESENT
                      | (count > 1 ? DISPATCH_AMBIGUOUS : 0));
    }
  }

  /** Allocation-free batch EOF dispatch using the same result-lane contract as batchDispatch. */
  default void batchEofDispatch(
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    requireEofBatchLengths(nonTerminals, candidateCounts, uniqueProductions, flags);
    for (int index = 0; index < nonTerminals.length; index++) {
      int[] candidates = eofCandidateProductions(nonTerminals[index]);
      int count = candidates.length;
      candidateCounts[index] = count;
      uniqueProductions[index] = count == 1 ? candidates[0] : -1;
      flags[index] =
          count == 0
              ? 0
              : (byte)
                  (DISPATCH_PRESENT
                      | (count > 1 ? DISPATCH_AMBIGUOUS : 0));
    }
  }

  private static void requireBatchLengths(
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    java.util.Objects.requireNonNull(nonTerminals, "nonTerminals");
    java.util.Objects.requireNonNull(terminals, "terminals");
    java.util.Objects.requireNonNull(candidateCounts, "candidateCounts");
    java.util.Objects.requireNonNull(uniqueProductions, "uniqueProductions");
    java.util.Objects.requireNonNull(flags, "flags");
    int length = nonTerminals.length;
    if (terminals.length != length
        || candidateCounts.length != length
        || uniqueProductions.length != length
        || flags.length != length) {
      throw new IllegalArgumentException("batch dispatch lane lengths differ");
    }
  }

  private static void requireEofBatchLengths(
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    java.util.Objects.requireNonNull(nonTerminals, "nonTerminals");
    java.util.Objects.requireNonNull(candidateCounts, "candidateCounts");
    java.util.Objects.requireNonNull(uniqueProductions, "uniqueProductions");
    java.util.Objects.requireNonNull(flags, "flags");
    int length = nonTerminals.length;
    if (candidateCounts.length != length
        || uniqueProductions.length != length
        || flags.length != length) {
      throw new IllegalArgumentException("batch EOF dispatch lane lengths differ");
    }
  }

  int syntaxKindCodeForProduction(int production);
  int syntaxKindCodeForTerminal(int terminalSymbol);
  boolean terminalSyntaxKindCode(int kindCode);
  int terminalSymbolFromSyntaxKindCode(int kindCode);
  int productionFromSyntaxKindCode(int kindCode);

  long primitivePayloadBytes();
}
