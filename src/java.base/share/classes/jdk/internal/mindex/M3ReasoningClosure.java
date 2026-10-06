/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/** Immutable least-fixed-point result for a {@link M3ReasoningProgram}. */
public final class M3ReasoningClosure {
  private static final int PROOF_UNDERIVED = -2;
  private static final int PROOF_FACT = -1;

  private final M3ReasoningProgram program;
  private final BitSet positive;
  private final BitSet negative;
  private final BitSet inconsistent;
  private final int[] positiveProofRule;
  private final int[] negativeProofRule;
  private final String rootHash;

  private M3ReasoningClosure(
      M3ReasoningProgram program,
      BitSet positive,
      BitSet negative,
      int[] positiveProofRule,
      int[] negativeProofRule) {
    this.program = program;
    this.positive = (BitSet) positive.clone();
    this.negative = (BitSet) negative.clone();
    this.inconsistent = (BitSet) positive.clone();
    this.inconsistent.and(negative);
    this.positiveProofRule = positiveProofRule.clone();
    this.negativeProofRule = negativeProofRule.clone();

    long[] lanes = new long[
        positive.toLongArray().length
            + negative.toLongArray().length
            + inconsistent.toLongArray().length];
    int cursor = 0;
    for (long value : positive.toLongArray()) lanes[cursor++] = value;
    for (long value : negative.toLongArray()) lanes[cursor++] = value;
    for (long value : inconsistent.toLongArray()) lanes[cursor++] = value;
    int[] proofs = new int[positiveProofRule.length + negativeProofRule.length];
    System.arraycopy(positiveProofRule, 0, proofs, 0, positiveProofRule.length);
    System.arraycopy(negativeProofRule, 0, proofs, positiveProofRule.length, negativeProofRule.length);
    this.rootHash =
        M3PrecomputeHash.sha256(
            "SYNEXIA_REASONING_CLOSURE_V1:" + program.rootHash(), lanes, proofs);
  }

  static M3ReasoningClosure evaluate(M3ReasoningProgram program) {
    Objects.requireNonNull(program, "program");
    BitSet positive = program.positiveFacts();
    BitSet negative = program.negativeFacts();
    int[] positiveProof = new int[program.symbolCount()];
    int[] negativeProof = new int[program.symbolCount()];
    Arrays.fill(positiveProof, PROOF_UNDERIVED);
    Arrays.fill(negativeProof, PROOF_UNDERIVED);
    for (int row = positive.nextSetBit(0); row >= 0; row = positive.nextSetBit(row + 1)) {
      positiveProof[row] = PROOF_FACT;
    }
    for (int row = negative.nextSetBit(0); row >= 0; row = negative.nextSetBit(row + 1)) {
      negativeProof[row] = PROOF_FACT;
    }

    M3ReasoningProgram.Rule[] rules = program.rules();
    boolean changed;
    int additions = positive.cardinality() + negative.cardinality();
    int maximumAdditions = Math.multiplyExact(program.symbolCount(), 2);
    do {
      changed = false;
      for (int ruleIndex = 0; ruleIndex < rules.length; ruleIndex++) {
        M3ReasoningProgram.Rule rule = rules[ruleIndex];
        boolean satisfied = true;
        for (int literal : rule.antecedents()) {
          if (!M3ReasoningProgram.literalSatisfied(literal, positive, negative)) {
            satisfied = false;
            break;
          }
        }
        if (!satisfied) continue;

        int conclusion = rule.conclusion();
        int row = M3ReasoningProgram.literalRow(conclusion);
        BitSet target = M3ReasoningProgram.literalNegative(conclusion) ? negative : positive;
        int[] proofs = M3ReasoningProgram.literalNegative(conclusion) ? negativeProof : positiveProof;
        if (!target.get(row)) {
          target.set(row);
          proofs[row] = ruleIndex;
          additions++;
          if (additions > maximumAdditions) {
            throw new IllegalStateException("monotone reasoning closure exceeded finite bound");
          }
          changed = true;
        }
      }
    } while (changed);

    return new M3ReasoningClosure(program, positive, negative, positiveProof, negativeProof);
  }

  public String rootHash() { return rootHash; }
  public int symbolCount() { return program.symbolCount(); }
  public int derivedPositiveCount() { return positive.cardinality(); }
  public int derivedNegativeCount() { return negative.cardinality(); }
  public int inconsistentCount() { return inconsistent.cardinality(); }

  public boolean entails(String symbol) {
    int row = requireRow(symbol);
    return positive.get(row);
  }

  public boolean refutes(String symbol) {
    int row = requireRow(symbol);
    return negative.get(row);
  }

  public boolean inconsistent(String symbol) {
    int row = requireRow(symbol);
    return inconsistent.get(row);
  }

  /**
   * First deterministic proof rule for a derived literal.
   *
   * @return -1 for an initial fact, >=0 for a rule, or -2 when the literal is underived
   */
  public int proofRule(String literal) {
    Objects.requireNonNull(literal, "literal");
    boolean negativeLiteral = literal.startsWith("!");
    String symbol = negativeLiteral ? literal.substring(1) : literal;
    int row = requireRow(symbol);
    return negativeLiteral ? negativeProofRule[row] : positiveProofRule[row];
  }

  public boolean supports(String source, String target) {
    return hasRelation(source, target, M3ReasoningProgram.RELATION_SUPPORT);
  }

  public boolean attacks(String source, String target) {
    return hasRelation(source, target, M3ReasoningProgram.RELATION_ATTACK);
  }

  public long retainedBytes() {
    return program.retainedBytes()
        + (long) positive.toLongArray().length * Long.BYTES
        + (long) negative.toLongArray().length * Long.BYTES
        + (long) inconsistent.toLongArray().length * Long.BYTES
        + (long) (positiveProofRule.length + negativeProofRule.length) * Integer.BYTES;
  }

  private boolean hasRelation(String source, String target, int kind) {
    int sourceRow = requireRow(source);
    int targetRow = requireRow(target);
    for (M3ReasoningProgram.Relation relation : program.relations()) {
      if (relation.source() == sourceRow
          && relation.target() == targetRow
          && relation.kind() == kind) {
        return true;
      }
    }
    return false;
  }

  private int requireRow(String symbol) {
    int row = program.row(Objects.requireNonNull(symbol, "symbol"));
    if (row < 0) throw new IllegalArgumentException("unknown reasoning symbol: " + symbol);
    return row;
  }
}
