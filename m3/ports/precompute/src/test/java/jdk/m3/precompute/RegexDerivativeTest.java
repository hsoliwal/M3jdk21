// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.precompute.RegexDerivative.Backend;
import jdk.m3.precompute.RegexDerivative.Expr;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

final class RegexDerivativeTest {
  @Test
  void literalStarSequenceMatchesJdkOracleAndPrecomputesRunPowers() {
    Expr expression =
        RegexDerivative.sequence(
            RegexDerivative.literal("a"),
            RegexDerivative.star(RegexDerivative.literal("b")),
            RegexDerivative.literal("c"));
    RegexPredicateAlphabet alphabet =
        RegexDerivative.alphabetFor(List.of(expression), null);
    RegexDerivative compiled =
        RegexDerivative.compile(expression, alphabet, null);
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
    Expr any = RegexDerivative.anyString();
    Expr containsA =
        RegexDerivative.sequence(
            any, RegexDerivative.literal("a"), any);
    Expr containsB =
        RegexDerivative.sequence(
            any, RegexDerivative.literal("b"), any);
    Expr expression =
        RegexDerivative.intersect(
            containsA, RegexDerivative.complement(containsB));
    RegexPredicateAlphabet alphabet =
        RegexDerivative.alphabetFor(List.of(expression), null);
    RegexDerivative compiled =
        RegexDerivative.compile(expression, alphabet, null);

    assertTrue(compiled.matches("cat", null));
    assertTrue(compiled.matches("a", null));
    assertFalse(compiled.matches("cab", null));
    assertFalse(compiled.matches("bbb", null));
    assertFalse(compiled.matches("", null));
  }

  @Test
  void compileIsDeterministicAndJavaBatchUsesFrozenRleProgram() {
    Expr expression =
        RegexDerivative.either(
            RegexDerivative.literal("dog"),
            RegexDerivative.literal("dogma"),
            RegexDerivative.literal("cat"));
    RegexPredicateAlphabet alphabet =
        RegexDerivative.alphabetFor(List.of(expression), null);
    RegexDerivative first =
        RegexDerivative.compile(expression, alphabet, null);
    RegexDerivative second =
        RegexDerivative.compile(expression, alphabet, null);

    assertEquals(first.programSha256(), second.programSha256());
    assertEquals(first.statistics(), second.statistics());

    RegexDerivative.BatchResult result =
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
        RegexDerivative.sequence(
            RegexDerivative.literal("a"),
            RegexDerivative.star(RegexDerivative.literal("b")),
            RegexDerivative.literal("c"));
    RegexPredicateAlphabet alphabet =
        RegexDerivative.alphabetFor(List.of(expression), null);
    RegexDerivative.Limits limits =
        new RegexDerivative.Limits(64, 16, 64, 1, 64, 64, 64);

    assertThrows(
        IllegalArgumentException.class,
        () -> RegexDerivative.compile(expression, alphabet, limits, null));
  }

  @Test
  void nativeBatchMatchesJavaOracleWhenCompatibleLibraryIsPresent() {
    Assumptions.assumeTrue(RegexDerivativeNative.available());
    Expr expression =
        RegexDerivative.either(
            RegexDerivative.literal("alpha"),
            RegexDerivative.literal("beta"));
    RegexPredicateAlphabet alphabet =
        RegexDerivative.alphabetFor(List.of(expression), null);
    RegexDerivative compiled =
        RegexDerivative.compile(expression, alphabet, null);
    List<String> values = List.of("alpha", "beta", "gamma", "", "alphabet");

    boolean[] javaResult =
        compiled.matchesBatch(values, Backend.JAVA, null).matches();
    RegexDerivative.BatchResult nativeResult =
        compiled.matchesBatch(values, Backend.JNI, null);

    assertEquals(Backend.JNI, nativeResult.backend());
    assertEquals(1, nativeResult.nativeCalls());
    assertArrayEquals(javaResult, nativeResult.matches());
  }
}
