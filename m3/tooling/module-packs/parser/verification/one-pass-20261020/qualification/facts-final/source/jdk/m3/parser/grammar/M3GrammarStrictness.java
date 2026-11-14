// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarStrictness.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/**
 * Conformance policy layered over one immutable grammar.
 *
 * <p>STRICT and STRUCTURED reject syntax outside the grammar. TOLERANT reports syntax failure but
 * may be used by callers for editor/live-source workflows. FREE always accepts lexical text while
 * still reporting whether the optional grammar shape matched.</p>
 */
public enum M3GrammarStrictness {
  STRICT(true, false),
  STRUCTURED(true, false),
  TOLERANT(false, true),
  FREE(false, true);

  private final boolean rejecting;
  private final boolean acceptNonConforming;

  M3GrammarStrictness(boolean rejecting, boolean acceptNonConforming) {
    this.rejecting = rejecting;
    this.acceptNonConforming = acceptNonConforming;
  }

  public boolean rejecting() {
    return rejecting;
  }

  public boolean acceptNonConforming() {
    return acceptNonConforming;
  }
}
