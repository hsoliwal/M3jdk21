// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/**
 * Immutable per-input parser refusal bounds; work counts deterministic operations, not time.
 *
 * <p>Grammar compilation and its one-time immutable decision-index construction are separate
 * caller configuration operations. Source offsets are UTF-16 units. The recursive retained
 * tree builder has a hard depth ceiling of 256.</p>
 */
public record M3GrammarLimits(
    int maxSourceChars,
    int maxTokens,
    int maxStates,
    int maxTreeNodes,
    int maxTreeDepth,
    long maxWork) {
  public M3GrammarLimits {
    if (maxSourceChars < 0 || maxSourceChars > 0x00ff_ffff) {
      throw new IllegalArgumentException("maxSourceChars must be in [0, 16777215]");
    }
    if (maxTokens < 0 || maxTokens > 0x00ff_ffff) {
      throw new IllegalArgumentException("maxTokens must be in [0, 16777215]");
    }
    if (maxStates < 1 || maxStates > 0x0400_0000) {
      throw new IllegalArgumentException("maxStates must be in [1, 67108864]");
    }
    if (maxTreeNodes < 1 || maxTreeNodes > 0x0400_0000) {
      throw new IllegalArgumentException("maxTreeNodes must be in [1, 67108864]");
    }
    if (maxTreeDepth < 1 || maxTreeDepth > 256) {
      throw new IllegalArgumentException("maxTreeDepth must be in [1, 256]");
    }
    if (maxWork < 1) throw new IllegalArgumentException("maxWork must be positive");
  }

  /** Bounded defaults suitable for a reusable local parser service. */
  public static M3GrammarLimits defaults() {
    return new M3GrammarLimits(1_048_576, 10_000, 250_000, 100_000, 256, 10_000_000L);
  }
}
