// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

/**
 * Public launcher of the relocated smoke oracle {@link M3IndexCharacterFlagsCombineSmoke} (a package-private class,
 * kept byte for byte from the donor): exec-maven-plugin 3.3.0 resolves the main method through a method handle and
 * refuses a package-private class, so the test-phase execution {@code character-flags-combine} names this class.
 * The direct run is {@code java -cp target/classes;target/test-classes jdk.internal.m3.indexstring.M3IndexCharacterFlagsCombineSmoke}.
 */
//FLLDCIM: RELOCATE - launcher of recipe m3/ports/indexstring/recipe/character-flags-20261106 (gate 2); test scope, no production code
public final class M3IndexCharacterFlagsCombineSmokeMain {
  private M3IndexCharacterFlagsCombineSmokeMain() {}

  public static void main(String[] args) {
    M3IndexCharacterFlagsCombineSmoke.main(args);
  }
}
