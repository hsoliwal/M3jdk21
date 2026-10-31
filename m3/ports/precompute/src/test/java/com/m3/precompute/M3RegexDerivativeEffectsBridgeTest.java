// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.regex.Pattern;

/** The existing M3 DFA effects engine is the only fragment-composition owner. */
final class M3RegexDerivativeEffectsBridgeTest {
  @Test void existingDfaEffectBackendMatchesFullInputOracle() {
    var expr = M3RegexDerivative.sequence(
        M3RegexDerivative.literal("a"),
        M3RegexDerivative.star(M3RegexDerivative.literal("b")),
        M3RegexDerivative.literal("c"));
    var alphabet = M3RegexDerivative.alphabetFor(List.of(expr), null);
    var program = M3RegexDerivative.compile(expr, alphabet, null);
    var plan = program.asciiEffects(M3RegexDfaEffects.DEFAULT_LIMITS, null);
    assertNotNull(plan);
    for (String value : List.of("", "ac", "abc", "abbbc", "abb", "cab", "abbbcc")) {
      var whole = plan.summarize(value, null);
      assertNotNull(whole);
      assertEquals(Pattern.matches("ab*c", value), plan.matches(whole), value);
      assertEquals(program.matches(value, null), plan.matches(whole), value);
      for (int at = 0; at <= value.length(); at++) {
        var a = plan.summarize(value.substring(0, at), null);
        var b = plan.summarize(value.substring(at), null);
        var combined = plan.compose(a, b, null);
        assertEquals(plan.matches(whole), plan.matches(combined), value);
      }
    }
    assertNull(plan.summarize("a\u00e9c", null));
    assertThrows(IllegalArgumentException.class, () -> program.matches("a\ud800c", null));
  }
}
