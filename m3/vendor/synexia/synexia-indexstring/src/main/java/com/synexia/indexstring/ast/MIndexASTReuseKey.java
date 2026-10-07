// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import java.util.Objects;

/**
 * Primitive candidate key for reusing compiled work across immutable AST atoms.
 *
 * <p>The key deliberately exposes hashes and shape metrics as candidates only. A matching key is
 * not semantic proof: callers must still use {@link MIndexASTDiff#compare(MIndexAST, MIndexAST)}
 * or an equivalent structural/contract check before publishing a reused result.</p>
 */
public record MIndexASTReuseKey(
    long exactHash64,
    long structuralHash64,
    long logicHash64,
    long simHash64,
    int expandedNodeCount,
    int depth,
    int expandedLeafCount) {

  public MIndexASTReuseKey {
    if (expandedNodeCount < 0) throw new IllegalArgumentException("expandedNodeCount < 0");
    if (depth < -1) throw new IllegalArgumentException("depth < -1");
    if (expandedLeafCount < -1) throw new IllegalArgumentException("expandedLeafCount < -1");
  }

  /** Candidate class ordered from strongest hash evidence to weakest proximity evidence. */
  public enum Candidate {
    EXACT,
    NORMALIZED_LOGIC,
    STRUCTURE,
    APPROXIMATE,
    NONE
  }

  /** Which side of an {@link MIndexASTDiff.Result} should become a key. */
  public enum Side {
    LEFT,
    RIGHT
  }

  public static MIndexASTReuseKey from(MIndexAST ast) {
    Objects.requireNonNull(ast, "ast");
    return new MIndexASTReuseKey(
        ast.exactHash64(),
        ast.structuralHash64(),
        ast.logicHash64(),
        ast.simHash64(),
        ast.expandedNodeCount(),
        ast.depth(),
        ast.expandedLeafCount());
  }

  public static MIndexASTReuseKey from(MIndexASTDiff.Result result, Side side) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(side, "side");
    return side == Side.LEFT
        ? new MIndexASTReuseKey(
            result.leftExactHash(),
            result.leftStructuralHash(),
            result.leftLogicHash(),
            0L,
            result.leftExpandedNodes(),
            -1,
            -1)
        : new MIndexASTReuseKey(
            result.rightExactHash(),
            result.rightStructuralHash(),
            result.rightLogicHash(),
            0L,
            result.rightExpandedNodes(),
            -1,
            -1);
  }

  /** Hash/shape candidate for identical canonical content. */
  public boolean exactCandidate(MIndexASTReuseKey other) {
    return sameShape(other)
        && exactHash64 == other.exactHash64
        && structuralHash64 == other.structuralHash64;
  }

  /** Hash/shape candidate for identifier-renamed or otherwise normalized logic reuse. */
  public boolean normalizedLogicCandidate(MIndexASTReuseKey other) {
    return sameShape(other)
        && structuralHash64 == other.structuralHash64
        && logicHash64 == other.logicHash64;
  }

  /** Hash/shape candidate for shape reuse when literal/logic labels differ. */
  public boolean structuralCandidate(MIndexASTReuseKey other) {
    return sameShape(other) && structuralHash64 == other.structuralHash64;
  }

  /** Approximate candidate only; never use this result as a proof of reuse. */
  public boolean approximateCandidate(MIndexASTReuseKey other, int maximumHammingDistance) {
    Objects.requireNonNull(other, "other");
    if (maximumHammingDistance < 0 || maximumHammingDistance > Long.SIZE) {
      throw new IllegalArgumentException("maximumHammingDistance outside [0,64]");
    }
    return hammingDistance(other) <= maximumHammingDistance;
  }

  public int hammingDistance(MIndexASTReuseKey other) {
    Objects.requireNonNull(other, "other");
    return Long.bitCount(simHash64 ^ other.simHash64);
  }

  public Candidate classify(MIndexASTReuseKey other, int maximumHammingDistance) {
    Objects.requireNonNull(other, "other");
    if (exactCandidate(other)) return Candidate.EXACT;
    if (normalizedLogicCandidate(other)) return Candidate.NORMALIZED_LOGIC;
    if (structuralCandidate(other)) return Candidate.STRUCTURE;
    if (approximateCandidate(other, maximumHammingDistance)) return Candidate.APPROXIMATE;
    return Candidate.NONE;
  }

  /**
   * Classifies with a provider-produced Hamming distance for the approximate lane.
   *
   * <p>The provider contract is deliberately narrow: exact, normalized-logic and structural
   * checks remain local hash/shape checks, while only the approximate distance is supplied by the
   * selected batch provider. This keeps provider output candidate-only and leaves admission to the
   * structural proof layer.</p>
   */
  Candidate classifyWithHammingDistance(
      MIndexASTReuseKey other, int maximumHammingDistance, int hammingDistance) {
    Objects.requireNonNull(other, "other");
    if (maximumHammingDistance < 0 || maximumHammingDistance > Long.SIZE) {
      throw new IllegalArgumentException("maximumHammingDistance outside [0,64]");
    }
    if (hammingDistance < 0 || hammingDistance > Long.SIZE) {
      throw new IllegalArgumentException("hammingDistance outside [0,64]");
    }
    if (exactCandidate(other)) return Candidate.EXACT;
    if (normalizedLogicCandidate(other)) return Candidate.NORMALIZED_LOGIC;
    if (structuralCandidate(other)) return Candidate.STRUCTURE;
    if (hammingDistance <= maximumHammingDistance) return Candidate.APPROXIMATE;
    return Candidate.NONE;
  }

  private boolean sameShape(MIndexASTReuseKey other) {
    Objects.requireNonNull(other, "other");
    return expandedNodeCount == other.expandedNodeCount
        && (depth < 0 || other.depth < 0 || depth == other.depth)
        && (expandedLeafCount < 0
            || other.expandedLeafCount < 0
            || expandedLeafCount == other.expandedLeafCount);
  }
}
