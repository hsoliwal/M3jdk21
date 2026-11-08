// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.util.Arrays;
import java.util.Random;

/** Dependency-free Java 21 executable differential and adversarial proof. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/982689e16763e54b01632407d64846fbea5719b6/synexia-m3-recipe/crates/m3-java-nullness-flow-20261031/qualification/NullnessProof.java blob 24a9d9804489; recipe m3/compiler/nullness-facts/recipe/relocate.py v1
public final class NullnessProof {
  private static final String ZERO = "0".repeat(64);
  private static final String ONE = "1".repeat(64);
  private static final String TWO = "2".repeat(64);
  private static int checks;

  private NullnessProof() { }

  public static void main(String[] args) {
    var epoch = new JavaNullnessFlow.Epoch(ZERO, ONE, TWO, 23);
    var base = JavaNullnessFlow.start(epoch, 100);
    require(base.reachable(), "start reachable");
    for (int slot = 0; slot < 100; slot++) {
      require(base.nullness(slot) == JavaNullnessFlow.Nullness.MAYBE_NULL, "unknown slot");
      var left = JavaNullnessFlow.assumeNull(base, slot);
      var right = JavaNullnessFlow.assumeNonNull(base, slot);
      require(left.nullness(slot) == JavaNullnessFlow.Nullness.NULL, "null refinement");
      require(right.definitelyNonNull(slot), "non-null refinement");
      require(JavaNullnessFlow.assumeNonNull(left, slot).reachable() == false,
          "contradictory guard unreachable");
      require(JavaNullnessFlow.join(left, right).nullness(slot)
          == JavaNullnessFlow.Nullness.MAYBE_NULL, "branch join");
      require(JavaNullnessFlow.invalidate(right, slot).nullness(slot)
          == JavaNullnessFlow.Nullness.MAYBE_NULL, "mutation invalidation");
      require(JavaNullnessFlow.assign(base, slot, JavaNullnessFlow.Nullness.NON_NULL)
          .definitelyNonNull(slot), "attributed assignment");
      require(base.nullness(slot) == JavaNullnessFlow.Nullness.MAYBE_NULL,
          "detached immutable snapshots");
    }
    long[] escaped = base.packedCopy();
    Arrays.fill(escaped, 0L);
    require(base.nullness(0) == JavaNullnessFlow.Nullness.MAYBE_NULL,
        "defensive primitive image");
    require(base.contentKey().equals(JavaNullnessFlow.start(epoch, 100).contentKey()),
        "stable content key");
    require(!base.automaticMutationAuthority(), "never automatic source authority");
    require(!JavaNullnessFlow.unreachable(epoch, 100).reachable(), "unreachable state");
    require(JavaNullnessFlow.join(base, JavaNullnessFlow.unreachable(epoch, 100)) == base,
        "unreachable join identity");
    require(JavaNullnessFlow.invalidateAll(JavaNullnessFlow.assumeNonNull(base, 12))
        .nullness(12) == JavaNullnessFlow.Nullness.MAYBE_NULL, "unknown side effects");

    var other = new JavaNullnessFlow.Epoch(ZERO, ONE, TWO, 24);
    expectFailure(() -> JavaNullnessFlow.join(base, JavaNullnessFlow.start(other, 100)));
    expectFailure(() -> JavaNullnessFlow.join(base, JavaNullnessFlow.start(epoch, 99)));
    expectFailure(() -> JavaNullnessFlow.start(epoch, -1));
    expectFailure(() -> JavaNullnessFlow.start(epoch, JavaNullnessFlow.MAX_SLOTS + 1));
    expectFailure(() -> JavaNullnessFlow.assumeNull(base, 100));
    expectFailure(() -> JavaNullnessFlow.assign(base, 0,
        JavaNullnessFlow.Nullness.UNREACHABLE));
    expectFailure(() -> new JavaNullnessFlow.Epoch(ZERO.toUpperCase(), ONE,
        "G".repeat(64), 0));

    // Exhaustively exercise every pair of null/nonnull/maybe values and both
    // transfer orders across 32-bit packing boundaries. join = lattice OR.
    JavaNullnessFlow.Nullness[] states = {
        JavaNullnessFlow.Nullness.NULL,
        JavaNullnessFlow.Nullness.NON_NULL,
        JavaNullnessFlow.Nullness.MAYBE_NULL};
    for (int slot : new int[] {0, 30, 31, 32, 63, 64, 99}) {
      for (int l = 0; l < 3; l++) for (int r = 0; r < 3; r++) {
        var a = JavaNullnessFlow.assign(base, slot, states[l]);
        var b = JavaNullnessFlow.assign(base, slot, states[r]);
        int expected = (l + 1) | (r + 1);
        require(JavaNullnessFlow.join(a, b).nullness(slot) == states[expected - 1],
            "lattice truth table");
        require(JavaNullnessFlow.join(a, b).contentKey()
            .equals(JavaNullnessFlow.join(b, a).contentKey()),
            "join commutativity");
        require(JavaNullnessFlow.join(a, a).contentKey().equals(a.contentKey()),
            "join idempotence");
      }
    }
    Random random = new Random(0x4d334e554c4cL);
    for (int i = 0; i < 4000; i++) {
      int slot = random.nextInt(100);
      var a = JavaNullnessFlow.assign(base, slot, states[random.nextInt(3)]);
      var b = JavaNullnessFlow.assign(base, slot, states[random.nextInt(3)]);
      var c = JavaNullnessFlow.assign(base, slot, states[random.nextInt(3)]);
      require(JavaNullnessFlow.join(JavaNullnessFlow.join(a, b), c).contentKey()
          .equals(JavaNullnessFlow.join(a, JavaNullnessFlow.join(b, c)).contentKey()),
          "associative join");
      require(JavaNullnessFlow.join(a, JavaNullnessFlow.unreachable(epoch, 100)) == a,
          "unreachable identity");
    }
    System.out.println("M3_JAVA_NULLNESS_PROOF_PASS checks=" + checks);
  }

  private static void require(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }

  private static void expectFailure(Runnable action) {
    checks++;
    try {
      action.run();
    } catch (IllegalArgumentException | IndexOutOfBoundsException expected) {
      return;
    }
    throw new AssertionError("fail-closed input accepted");
  }
}
