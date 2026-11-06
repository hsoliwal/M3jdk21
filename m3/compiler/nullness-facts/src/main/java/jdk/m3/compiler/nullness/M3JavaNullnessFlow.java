// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.nullness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Conservative Java 21 flow-nullness facts, inspired by Kotlin's data-flow analysis.
 *
 * <p>This is a primitive-lane fact lattice, not a parser, language extension, Kotlin
 * dependency, Java type-system change, compiler certificate, or permission to change
 * bytecode. Callers must map slots to proven local-variable symbols from their
 * authoritative attributed AST. Fields, array elements and unknown aliases cannot
 * be treated as stable local slots. Mutations and unmodelled control flow must
 * invalidate facts; no optimistic inference is permitted.
 *
 * <p>Two bits per local variable: 00 is unreachable, 01 is null, 10 is non-null,
 * 11 is maybe-null. The bitwise OR is the least upper bound; a whole unreachable
 * state is represented separately. The source/compiler/graph generation tuple
 * scopes comparisons and prevents reuse of facts after invalidation.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/982689e16763e54b01632407d64846fbea5719b6/synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/M3JavaNullnessFlow.java blob 347ba86620ca; recipe m3/compiler/nullness-facts/recipe/relocate.py v1
public final class M3JavaNullnessFlow {
  public static final int MAX_SLOTS = 1_000_000;
  private static final byte NULL = 1;
  private static final byte NON_NULL = 2;
  private static final byte MAYBE_NULL = 3;
  private static final byte[] LABEL = "m3-java-nullness-facts/v1\u0000"
      .getBytes(StandardCharsets.US_ASCII);

  private M3JavaNullnessFlow() {}

  public enum Nullness { UNREACHABLE, NULL, NON_NULL, MAYBE_NULL }

  /** Context identity is an input from the authoritative compiler, not proof itself. */
  public record Epoch(String sourceSha256, String compilerContextSha256,
      String attributedGraphSha256, long symbolGeneration) {
    public Epoch {
      requireDigest(sourceSha256, "source");
      requireDigest(compilerContextSha256, "compiler context");
      requireDigest(attributedGraphSha256, "attributed graph");
      if (symbolGeneration < 0L) {
        throw new IllegalArgumentException("negative symbol generation");
      }
    }
  }

  /** Immutable snapshot: no per-variable Node, Entry, Map or facade is retained. */
  public static final class Facts {
    private final Epoch epoch;
    private final int slots;
    private final boolean reachable;
    private final long[] packed;

    private Facts(Epoch epoch, int slots, boolean reachable, long[] packed) {
      this.epoch = epoch;
      this.slots = slots;
      this.reachable = reachable;
      this.packed = packed;
    }

    public Epoch epoch() { return epoch; }
    public int slots() { return slots; }
    public boolean reachable() { return reachable; }

    public Nullness nullness(int slot) {
      Objects.checkIndex(slot, slots);
      if (!reachable) return Nullness.UNREACHABLE;
      return switch (get(packed, slot)) {
        case NULL -> Nullness.NULL;
        case NON_NULL -> Nullness.NON_NULL;
        case MAYBE_NULL -> Nullness.MAYBE_NULL;
        default -> throw new IllegalStateException("invalid reachable nullness fact");
      };
    }

    /** Use only after independent compiler attribution and flow-edge verification. */
    public boolean definitelyNonNull(int slot) {
      return nullness(slot) == Nullness.NON_NULL;
    }

    /** Detached primitive image for M3AtomSpace admission, never a mutable view. */
    public long[] packedCopy() { return packed.clone(); }

    /** Stable value identity for caches; SHA-256 is not compiler or semantic proof. */
    public String contentKey() {
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(LABEL);
        md.update(epoch.sourceSha256().getBytes(StandardCharsets.US_ASCII));
        md.update(epoch.compilerContextSha256().getBytes(StandardCharsets.US_ASCII));
        md.update(epoch.attributedGraphSha256().getBytes(StandardCharsets.US_ASCII));
        updateLong(md, epoch.symbolGeneration());
        updateLong(md, slots);
        md.update((byte) (reachable ? 1 : 0));
        for (long word : packed) updateLong(md, word);
        return HexFormat.of().formatHex(md.digest());
      } catch (NoSuchAlgorithmException impossible) {
        throw new IllegalStateException(impossible);
      }
    }

    /** A fact alone never authorizes changes to source, bytecode, GC or JNI state. */
    public boolean automaticMutationAuthority() { return false; }
  }

  public static Facts start(Epoch epoch, int slots) {
    Objects.requireNonNull(epoch, "epoch");
    checkSlots(slots);
    long[] words = new long[(slots + 31) >>> 5];
    Arrays.fill(words, -1L); // 11: MAYBE_NULL for each occupied slot.
    if (slots != 0 && (slots & 31) != 0) {
      words[words.length - 1] = (1L << ((slots & 31) << 1)) - 1L;
    }
    return new Facts(epoch, slots, true, words);
  }

  public static Facts unreachable(Epoch epoch, int slots) {
    Objects.requireNonNull(epoch, "epoch");
    checkSlots(slots);
    return new Facts(epoch, slots, false, new long[(slots + 31) >>> 5]);
  }

  /** A checked null condition on one attributed local symbol. */
  public static Facts assumeNull(Facts input, int slot) {
    return assume(input, slot, NULL);
  }

  /** A checked non-null condition on one attributed local symbol. */
  public static Facts assumeNonNull(Facts input, int slot) {
    return assume(input, slot, NON_NULL);
  }

  /** Transfer for an attributed assignment to a local symbol. */
  public static Facts assign(Facts input, int slot, Nullness value) {
    Objects.requireNonNull(value, "value");
    if (value == Nullness.UNREACHABLE) {
      throw new IllegalArgumentException("cannot assign unreachable as a value");
    }
    return change(input, slot, switch (value) {
      case NULL -> NULL;
      case NON_NULL -> NON_NULL;
      case MAYBE_NULL -> MAYBE_NULL;
      case UNREACHABLE -> throw new IllegalArgumentException("unreachable assignment");
    });
  }

  /** Fail closed for a symbol whose assignment/capture/callback is not modeled. */
  public static Facts invalidate(Facts input, int slot) {
    return change(input, slot, MAYBE_NULL);
  }

  /** Unknown side effects at a flow boundary invalidate every local fact. */
  public static Facts invalidateAll(Facts input) {
    Objects.requireNonNull(input, "input");
    return input.reachable ? start(input.epoch, input.slots) : input;
  }

  /** Lattice join of two paths, with exact source/toolchain/symbol scopes. */
  public static Facts join(Facts left, Facts right) {
    requireSameScope(left, right);
    if (!left.reachable) return right;
    if (!right.reachable) return left;
    long[] merged = new long[left.packed.length];
    boolean changedLeft = false;
    boolean changedRight = false;
    for (int i = 0; i < merged.length; i++) {
      merged[i] = left.packed[i] | right.packed[i];
      changedLeft |= merged[i] != left.packed[i];
      changedRight |= merged[i] != right.packed[i];
    }
    if (!changedLeft) return left;
    if (!changedRight) return right;
    return new Facts(left.epoch, left.slots, true, merged);
  }

  private static Facts assume(Facts input, int slot, byte expected) {
    Objects.requireNonNull(input, "input");
    Objects.checkIndex(slot, input.slots);
    if (!input.reachable) return input;
    byte found = get(input.packed, slot);
    if ((found & expected) == 0) return unreachable(input.epoch, input.slots);
    return found == expected ? input : change(input, slot, expected);
  }

  private static Facts change(Facts input, int slot, byte value) {
    Objects.requireNonNull(input, "input");
    Objects.checkIndex(slot, input.slots);
    if (!input.reachable || get(input.packed, slot) == value) return input;
    long[] copy = input.packed.clone();
    int shift = (slot & 31) << 1;
    int word = slot >>> 5;
    copy[word] = (copy[word] & ~(3L << shift)) | ((long) value << shift);
    return new Facts(input.epoch, input.slots, true, copy);
  }

  private static byte get(long[] packed, int slot) {
    return (byte) ((packed[slot >>> 5] >>> ((slot & 31) << 1)) & 3L);
  }

  private static void requireSameScope(Facts left, Facts right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    if (left.slots != right.slots || !left.epoch.equals(right.epoch)) {
      throw new IllegalArgumentException("M3 nullness fact scope changed");
    }
  }

  private static void checkSlots(int slots) {
    if (slots < 0 || slots > MAX_SLOTS) {
      throw new IllegalArgumentException("nullness slot budget");
    }
  }

  private static void requireDigest(String digest, String label) {
    if (digest == null || !digest.matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("invalid " + label + " SHA-256");
    }
  }

  private static void updateLong(MessageDigest digest, long value) {
    for (int shift = 56; shift >= 0; shift -= 8) {
      digest.update((byte) (value >>> shift));
    }
  }
}
