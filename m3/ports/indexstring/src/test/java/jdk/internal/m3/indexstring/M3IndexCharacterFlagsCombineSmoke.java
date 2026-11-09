// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Executable Java 17 oracle for the exact production candidate.
package jdk.internal.m3.indexstring;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ac5feec00a6d6206194b3ba954c6f738aae07126/synexia-openrewrite-recipes/crates/m3-character-flags-combine-recovery-20261104/proof/FlagCombineSmoke.java blob 05e59df0ea7d; recipe m3/ports/indexstring/recipe/character-flags-20261106/relocate.py v1
final class M3IndexCharacterFlagsCombineSmoke {
  public static void main(String[] args) {
    String[] cases = {
        "", " ", "\t\n", "\r\n", "A9_", "alpha", "é", "Ā", "Ω", "😀",
        "a😀b", " e\u0301 ", String.valueOf(Character.MIN_HIGH_SURROGATE),
        String.valueOf(Character.MIN_LOW_SURROGATE), "\u00A0", "\u2003"
    };
    int pairs = 0;
    int triples = 0;
    for (String a : cases) {
      for (String b : cases) {
        int actual = M3IndexCharacterFlags.combine(
            M3IndexCharacterFlags.of(a), M3IndexCharacterFlags.of(b));
        if (actual != M3IndexCharacterFlags.of(a + b)) {
          throw new AssertionError("pair mismatch " + pairs);
        }
        pairs++;
        for (String c : cases) {
          int lhs = M3IndexCharacterFlags.combine(actual, M3IndexCharacterFlags.of(c));
          int rhs = M3IndexCharacterFlags.combine(
              M3IndexCharacterFlags.of(a),
              M3IndexCharacterFlags.combine(
                  M3IndexCharacterFlags.of(b), M3IndexCharacterFlags.of(c)));
          if (lhs != rhs || lhs != M3IndexCharacterFlags.of(a + b + c)) {
            throw new AssertionError("triple mismatch " + triples);
          }
          triples++;
        }
      }
    }
    int invalids = 0;
    for (int invalid : new int[] {-1, Integer.MIN_VALUE, 1 << 10, 1 << 20}) {
      try {
        M3IndexCharacterFlags.combine(invalid, M3IndexCharacterFlags.of("Alpha"));
        throw new AssertionError("invalid left accepted");
      } catch (IllegalArgumentException expected) {
        invalids++;
      }
      try {
        M3IndexCharacterFlags.combine(M3IndexCharacterFlags.of("Alpha"), invalid);
        throw new AssertionError("invalid right accepted");
      } catch (IllegalArgumentException expected) {
        invalids++;
      }
    }
    System.out.println("PASS actual M3IndexCharacterFlags.java; pairs="+pairs+
        " triples="+triples+" rejectedInvalids="+invalids);
  }
}
