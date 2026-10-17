// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.precompute.M3RegexDerivative.Backend;
import com.m3.precompute.M3RegexDerivative.Expr;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

final class M3RegexDerivativeTest {
  @Test
  void literalStarSequenceMatchesJdkOracleAndPrecomputesRunPowers() {
    Expr expression =
        M3RegexDerivative.sequence(
            M3RegexDerivative.literal("a"),
            M3RegexDerivative.star(M3RegexDerivative.literal("b")),
            M3RegexDerivative.literal("c"));
    M3RegexPredicateAlphabet alphabet =
        M3RegexDerivative.alphabetFor(List.of(expression), null);
    M3RegexDerivative compiled =
        M3RegexDerivative.compile(expression, alphabet, null);
    Pattern oracle = Pattern.compile("ab*c");

    for (String value :
        List.of("", "ac", "abc", "abbbc", "abb", "cabbbc", "abbbcc")) {
      assertEquals(oracle.matcher(value).matches(), compiled.matches(value, null), value);
    }

    String longRun = "a" + "b".repeat(1_000_000) + "c";
    assertTrue(compiled.matches(longRun, null));
    assertTrue(compiled.statistics().derivativeComputations() > 0);
    assertTrue(compiled.statistics().runPowerBits() > 1);
    assertTrue(compiled.statistics().minimizedStates() <= compiled.statistics().reachableStates());
    assertTrue(compiled.statistics().executionClasses() <= compiled.statistics().alphabetClasses());
    assertEquals(
        compiled.statistics().minimizedStates(),
        compiled.statistics().liveStates() + compiled.statistics().deadStates());
    assertEquals(64, compiled.programSha256().length());
  }

  @Test
  void intersectionAndComplementAreFirstClassBooleanAlgebra() {
    Expr any = M3RegexDerivative.anyString();
    Expr containsA =
        M3RegexDerivative.sequence(
            any, M3RegexDerivative.literal("a"), any);
    Expr containsB =
        M3RegexDerivative.sequence(
            any, M3RegexDerivative.literal("b"), any);
    Expr expression =
        M3RegexDerivative.intersect(
            containsA, M3RegexDerivative.complement(containsB));
    M3RegexPredicateAlphabet alphabet =
        M3RegexDerivative.alphabetFor(List.of(expression), null);
    M3RegexDerivative compiled =
        M3RegexDerivative.compile(expression, alphabet, null);

    assertTrue(compiled.matches("cat", null));
    assertTrue(compiled.matches("a", null));
    assertFalse(compiled.matches("cab", null));
    assertFalse(compiled.matches("bbb", null));
    assertFalse(compiled.matches("", null));
  }

  @Test
  void compileIsDeterministicAndJavaBatchUsesFrozenRleProgram() {
    Expr expression =
        M3RegexDerivative.either(
            M3RegexDerivative.literal("dog"),
            M3RegexDerivative.literal("dogma"),
            M3RegexDerivative.literal("cat"));
    M3RegexPredicateAlphabet alphabet =
        M3RegexDerivative.alphabetFor(List.of(expression), null);
    M3RegexDerivative first =
        M3RegexDerivative.compile(expression, alphabet, null);
    M3RegexDerivative second =
        M3RegexDerivative.compile(expression, alphabet, null);

    assertEquals(first.programSha256(), second.programSha256());
    assertEquals(first.statistics(), second.statistics());

    M3RegexDerivative.BatchResult result =
        first.matchesBatch(
            List.of("dog", "dogma", "cat", "dogmatic", ""), Backend.JAVA, null);
    assertEquals(Backend.JAVA, result.backend());
    assertEquals(0, result.nativeCalls());
    assertArrayEquals(
        new boolean[] {true, true, true, false, false}, result.matches());
  }

  @Test
  void explicitBudgetsFailClosed() {
    Expr expression =
        M3RegexDerivative.sequence(
            M3RegexDerivative.literal("a"),
            M3RegexDerivative.star(M3RegexDerivative.literal("b")),
            M3RegexDerivative.literal("c"));
    M3RegexPredicateAlphabet alphabet =
        M3RegexDerivative.alphabetFor(List.of(expression), null);
    M3RegexDerivative.Limits limits =
        new M3RegexDerivative.Limits(64, 16, 64, 1, 64, 64, 64);

    assertThrows(
        IllegalArgumentException.class,
        () -> M3RegexDerivative.compile(expression, alphabet, limits, null));
  }

  @Test
  void nativeBatchMatchesJavaOracleWhenCompatibleLibraryIsPresent() {
    Assumptions.assumeTrue(M3RegexDerivativeNative.available());
    Expr expression =
        M3RegexDerivative.either(
            M3RegexDerivative.literal("alpha"),
            M3RegexDerivative.literal("beta"));
    M3RegexPredicateAlphabet alphabet =
        M3RegexDerivative.alphabetFor(List.of(expression), null);
    M3RegexDerivative compiled =
        M3RegexDerivative.compile(expression, alphabet, null);
    List<String> values = List.of("alpha", "beta", "gamma", "", "alphabet");

    boolean[] javaResult =
        compiled.matchesBatch(values, Backend.JAVA, null).matches();
    M3RegexDerivative.BatchResult nativeResult =
        compiled.matchesBatch(values, Backend.JNI, null);

    assertEquals(Backend.JNI, nativeResult.backend());
    assertEquals(1, nativeResult.nativeCalls());
    assertArrayEquals(javaResult, nativeResult.matches());
  }
}
