// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.IntUnaryOperator;

/**
 * Bounded, operation-local Boolean DFA effects. Not a parser, payload owner or global cache.
 * A fragment maps every incoming DFA state to its outgoing state. Concatenation is function
 * composition; repetition uses binary lifting. No source text survives preparation.
 * Capture/region/search/assertion semantics must be admitted by the existing regex owner.
 */
final class M3RegexDfaEffects {
  static final int UNSUPPORTED = -1;
  static final int NO_MATCH = 0;
  static final int MATCH = 1;
  static final Limits DEFAULT_LIMITS = new Limits(4096, 4_000_000L, 16_384L);

  private M3RegexDfaEffects() { }

  record Limits(int maxStates, long maxPrepareSteps, long maxEffectBytes) {
    Limits {
      if (maxStates < 1 || maxStates > 4096 || maxPrepareSteps < 0 || maxEffectBytes < 0) {
        throw new IllegalArgumentException("invalid regex effect limits");
      }
    }
  }

  /** Borrow the exact immutable owner's DFA; do not clone its transition tables or expression. */
  interface Dfa {
    int stateCount();
    int startState();
    int next(int state, int ascii);
    boolean accepting(int state);
  }

  @FunctionalInterface
  interface NativeJoin {
    long[] compose(long[] left, long[] right, int states);
  }

  /** Call-local packed rows only; providers never receive an index's retained backing arrays. */
  @FunctionalInterface
  interface NativeBatch {
    int[] compose(int[] packed, int states, int rows);
  }

  static Plan prepare(Dfa dfa, Limits limits, Runnable checkpoint) {
    Objects.requireNonNull(dfa, "dfa");
    Objects.requireNonNull(limits, "limits");
    check(checkpoint);
    int states = dfa.stateCount();
    if (states < 1 || states > limits.maxStates() || (long) states * Integer.BYTES > limits.maxEffectBytes()) {
      return null;
    }
    if (dfa.startState() < 0 || dfa.startState() >= states) {
      throw new IllegalArgumentException("invalid DFA start");
    }
    return new Plan(dfa, states, limits);
  }

  static final class Plan {
    private final Dfa dfa;
    private final int states;
    private final Limits limits;

    private Plan(Dfa dfa, int states, Limits limits) {
      this.dfa = dfa;
      this.states = states;
      this.limits = limits;
    }

    Effect identity() { return new Effect(this, 0, identityStates()); }
    int states() { return states; }
    long effectBytes() { return (long) states * Integer.BYTES; }

    /**
     * Read each code unit once. Returns null on non-ASCII or work-budget refusal, never a false
     * negative. The source must remain stable during this call; it is not retained or cached.
     */
    Effect summarize(CharSequence text, Runnable checkpoint) {
      Objects.requireNonNull(text, "text");
      return summarize(text.length(), text::charAt, checkpoint);
    }

    Effect summarize(int length, IntUnaryOperator units, Runnable checkpoint) {
      Objects.requireNonNull(units, "units");
      check(checkpoint);
      if (length < 0) throw new IllegalArgumentException("negative logical length");
      if ((long) length * states > limits.maxPrepareSteps()) return null;
      int[] next = identityStates();
      for (int at = 0; at < length; at++) {
        if ((at & 255) == 0) check(checkpoint);
        int unit = units.applyAsInt(at);
        if (unit < 0 || unit >= 128) return null;
        for (int entry = 0; entry < states; entry++) {
          if (next[entry] >= 0) next[entry] = transition(next[entry], unit);
        }
      }
      check(checkpoint);
      return new Effect(this, length, next);
    }

    /** Baseline one-state streaming route: no O(states * text) summary cost for cold one-shot use. */
    int tryMatch(int length, IntUnaryOperator units, Runnable checkpoint) {
      Objects.requireNonNull(units, "units");
      check(checkpoint);
      if (length < 0) throw new IllegalArgumentException("negative logical length");
      int state = dfa.startState();
      for (int at = 0; at < length; at++) {
        if ((at & 255) == 0) check(checkpoint);
        int unit = units.applyAsInt(at);
        // Even a dead frontier must not disguise unsupported text as a proved non-match.
        if (unit < 0 || unit >= 128) return UNSUPPORTED;
        if (state >= 0) state = transition(state, unit);
      }
      check(checkpoint);
      return state >= 0 && dfa.accepting(state) ? MATCH : NO_MATCH;
    }

    boolean matches(Effect effect) {
      require(effect);
      int state = effect.next[dfa.startState()];
      return state >= 0 && dfa.accepting(state);
    }

    Effect compose(Effect left, Effect right, Runnable checkpoint) {
      require(left);
      require(right);
      check(checkpoint);
      long length = Math.addExact(left.length, right.length);
      int[] output = new int[states];
      composeInto(left.next, right.next, output, checkpoint);
      return new Effect(this, length, output);
    }

    /**
     * Reuse M3Utf16Spans.composeLiteralTables's existing ABI. The extra state represents dead;
     * counts/endpoints are neutral. Numeric ABI copies are explicit, never text copies.
     * Native selection is explicit until a measured crossover is established.
     */
    Effect composeNative(Effect left, Effect right, NativeJoin nativeJoin, Runnable checkpoint) {
      require(left);
      require(right);
      Objects.requireNonNull(nativeJoin, "nativeJoin");
      check(checkpoint);
      long length = Math.addExact(left.length, right.length);
      long[] output = nativeJoin.compose(nativeTable(left), nativeTable(right), states + 1);
      check(checkpoint);
      return decodeNative(output, length);
    }

    /** Linear, bounded fan-in with two reusable state vectors, not a retained chain of effects. */
    Effect composeAll(Effect[] parts, Runnable checkpoint) {
      Objects.requireNonNull(parts, "parts");
      check(checkpoint);
      if ((long) parts.length * states > limits.maxPrepareSteps()) return null;
      long length = 0;
      // Freeze shape/length before computing; caller must not mutate the parts array concurrently.
      for (Effect part : parts) { require(part); length = Math.addExact(length, part.length); }
      int[] current = identityStates();
      int[] next = new int[states];
      for (Effect part : parts) {
        check(checkpoint);
        composeInto(current, part.next, next, checkpoint);
        int[] swap = current; current = next; next = swap;
      }
      check(checkpoint);
      return new Effect(this, length, current);
    }

    /** Build a bounded numeric hierarchy over already-admitted effects, without rereading text. */
    Index index(Effect[] parts, IndexLimits indexLimits, Runnable checkpoint) {
      return Index.create(this, parts, indexLimits, checkpoint);
    }

    /** O(states * log repetitions) compositions, with checked logical length and no source reads. */
    Effect repeat(Effect block, long repetitions, Runnable checkpoint) {
      return repeat(block, repetitions, null, checkpoint);
    }

    Effect repeat(Effect block, long repetitions, NativeJoin nativeJoin, Runnable checkpoint) {
      require(block);
      check(checkpoint);
      if (repetitions < 0) throw new IllegalArgumentException("negative repetitions");
      Math.multiplyExact(block.length, repetitions);
      Effect result = identity();
      Effect power = block;
      for (long count = repetitions; count != 0; count >>>= 1) {
        check(checkpoint);
        if ((count & 1) != 0) result = join(result, power, nativeJoin, checkpoint);
        if (count > 1) power = join(power, power, nativeJoin, checkpoint);
      }
      return result;
    }

    private Effect join(Effect a, Effect b, NativeJoin nativeJoin, Runnable checkpoint) {
      return nativeJoin == null ? compose(a, b, checkpoint) : composeNative(a, b, nativeJoin, checkpoint);
    }

    private int transition(int state, int unit) {
      int next = dfa.next(state, unit);
      if (next < -1 || next >= states) throw new IllegalStateException("invalid owned DFA transition");
      return next;
    }

    private void composeInto(int[] left, int[] right, int[] output, Runnable checkpoint) {
      for (int entry = 0; entry < states; entry++) {
        if ((entry & 255) == 0) check(checkpoint);
        output[entry] = left[entry] < 0 ? -1 : right[left[entry]];
      }
      check(checkpoint);
    }

    private int[] identityStates() {
      int[] output = new int[states];
      for (int state = 0; state < states; state++) output[state] = state;
      return output;
    }

    private long[] nativeTable(Effect effect) {
      long[] table = new long[1 + 4 * (states + 1)];
      table[0] = effect.length;
      for (int state = 0; state <= states; state++) {
        int row = 1 + 4 * state;
        table[row] = state == states || effect.next[state] < 0 ? states : effect.next[state];
        table[row + 2] = -1;
        table[row + 3] = -1;
      }
      return table;
    }

    private Effect decodeNative(long[] table, long length) {
      if (table == null || table.length != 1 + 4 * (states + 1) || table[0] != length) {
        throw new IllegalStateException("native regex effect geometry");
      }
      int[] output = new int[states];
      for (int state = 0; state <= states; state++) {
        int row = 1 + 4 * state;
        long next = table[row];
        if (next < 0 || next > states || table[row + 1] != 0 || table[row + 2] != -1
            || table[row + 3] != -1 || (state == states && next != states)
            || (length == 0 && next != state)) {
          throw new IllegalStateException("native regex effect row");
        }
        if (state < states) output[state] = next == states ? -1 : (int) next;
      }
      return new Effect(this, length, output);
    }

    private void require(Effect effect) {
      if (Objects.requireNonNull(effect, "effect").owner != this) {
        throw new IllegalArgumentException("different regex execution owner or semantic context");
      }
    }
  }

  /** Primitive allocation and logical transition budgets; not a JVM heap reservation. */
  record IndexLimits(int maxFragments, long maxIndexPrimitiveBytes, long maxBuildStateVisits,
                     long maxQueryStateVisits, long maxQueryPrimitiveBytes) {
    IndexLimits {
      if (maxFragments < 0 || maxIndexPrimitiveBytes < 0 || maxBuildStateVisits < 0
          || maxQueryStateVisits < 0 || maxQueryPrimitiveBytes < 0) {
        throw new IllegalArgumentException("invalid regex index limits");
      }
    }
  }

  /**
   * Immutable, plan-owned, flat range-effect tree. Retains only numeric summaries, no source
   * objects, interner, cache registry or native address. The caller owns its residency.
   * Ordered cover traversal adapts AtCoder ac-library segtree::prod (CC0-1.0), pinned in
   * catalogue/m3-regex-dp/donors.tsv. Composition is associative, NOT commutative or invertible.
   * Build is O(states * paddedFragments); query is O(states * log fragments).
   * Budgets count primitive payloads, excluding object/array headers, allocator overhead and
   * unrelated allocations by callbacks. Null means admission refusal, never a proven non-match.
   * Published arrays never change: concurrent readers require no locks or native leases.
   */
  static final class Index {
    private final Plan owner;
    private final IndexLimits limits;
    private final int size;
    private final int base;
    private final int[] next;
    private final long[] lengths;

    private Index(Plan owner, IndexLimits limits, int size, int base, int[] next, long[] lengths) {
      this.owner = owner;
      this.limits = limits;
      this.size = size;
      this.base = base;
      this.next = next;
      this.lengths = lengths;
    }

    private static Index create(Plan owner, Effect[] parts, IndexLimits limits, Runnable checkpoint) {
      Objects.requireNonNull(parts, "parts");
      Objects.requireNonNull(limits, "limits");
      check(checkpoint);
      if (parts.length > limits.maxFragments()) return null;
      long padded = 1;
      while (padded < parts.length) padded <<= 1;
      long nodes = 2 * padded;
      long cells = nodes * owner.states;
      long bytes = cells * Integer.BYTES + nodes * Long.BYTES;
      long visits = (nodes - 1) * owner.states;
      if (nodes > Integer.MAX_VALUE - 8L || cells > Integer.MAX_VALUE - 8L
          || bytes > limits.maxIndexPrimitiveBytes() || visits > limits.maxBuildStateVisits()) {
        return null;
      }
      // Validate all ownership and logical lengths BEFORE allocating the retained tree.
      // Caller must not concurrently mutate parts during this operation.
      long total = 0;
      for (Effect part : parts) {
        check(checkpoint);
        owner.require(part);
        total = Math.addExact(total, part.length);
      }
      int base = (int) padded;
      int[] next = new int[(int) cells];
      long[] lengths = new long[(int) nodes];
      for (int leaf = 0; leaf < base; leaf++) {
        check(checkpoint);
        int offset = (base + leaf) * owner.states;
        if (leaf < parts.length) {
          System.arraycopy(parts[leaf].next, 0, next, offset, owner.states);
          lengths[base + leaf] = parts[leaf].length;
        } else {
          for (int state = 0; state < owner.states; state++) next[offset + state] = state;
        }
      }
      Index result = new Index(owner, limits, parts.length, base, next, lengths);
      for (int node = base - 1; node > 0; node--) result.rebuild(node, checkpoint);
      check(checkpoint);
      if (lengths[1] != total) throw new IllegalStateException("regex index length invariant");
      return result;
    }

    int fragments() { return size; }
    long length() { return lengths[1]; }
    long retainedPrimitiveBytes() { return (long) next.length * Integer.BYTES + (long) lengths.length * Long.BYTES; }

    /** Includes identity initialization plus ordered cover compositions; excludes ABI validation. */
    long queryStateVisits(int from, int to) {
      return (coverSize(from, to) + 1L) * owner.states;
    }

    /**
     * Cumulative primitive allocation volume, not just live Java heap. Native estimates include
     * the canonical JNI kernel's three malloc buffers per join, as well as Java ABI arrays.
     * An arbitrary NativeJoin callback is responsible for any additional allocations it makes.
     */
    long queryPrimitiveBytes(int from, int to, boolean nativePath) {
      int count = coverSize(from, to);
      long bytes = (long) count * Integer.BYTES + owner.effectBytes();
      if (nativePath) {
        long tableBytes = (1L + 4L * (owner.states + 1)) * Long.BYTES;
        bytes += count * (2 * owner.effectBytes() + 6 * tableBytes);
      }
      return bytes;
    }

    Effect range(int from, int to, Runnable checkpoint) {
      return range(from, to, null, checkpoint);
    }

    /** Native use is explicit; no automatic crossover or universal speed claim. */
    Effect range(int from, int to, NativeJoin nativeJoin, Runnable checkpoint) {
      check(checkpoint);
      int count = coverSize(from, to);
      if ((count + 1L) * owner.states > limits.maxQueryStateVisits()
          || queryPrimitiveBytes(from, to, nativeJoin != null) > limits.maxQueryPrimitiveBytes()) return null;
      int[] nodes = coverNodes(from, to, count);
      Effect result = owner.identity();
      long length = 0;
      for (int node : nodes) {
        check(checkpoint);
        int offset = node * owner.states;
        if (nativeJoin == null) {
          // Each cell reads only its own previous frontier state: in-place advancement is safe.
          for (int state = 0; state < owner.states; state++) {
            if ((state & 255) == 0) check(checkpoint);
            int middle = result.next[state];
            result.next[state] = middle < 0 ? -1 : next[offset + middle];
          }
        } else {
          int[] copy = new int[owner.states];
          System.arraycopy(next, offset, copy, 0, owner.states);
          result = owner.composeNative(result, new Effect(owner, lengths[node], copy), nativeJoin, checkpoint);
        }
        length = Math.addExact(length, lengths[node]);
      }
      check(checkpoint);
      return nativeJoin == null ? new Effect(owner, length, result.next) : result;
    }

    /**
     * Primitive allocation volume for the checked packed batch, including native scratch.
     * Nonempty: cover IDs + packed rows + JNI result + defensive result copy + two native
     * vectors. Excludes VM/array headers, JNI implementation internals and provider extras.
     * No retained tree, source text or DFA tables cross the ABI.
     */
    long batchPrimitiveBytes(int from, int to) {
      int count = coverSize(from, to);
      return count == 0 ? owner.effectBytes()
          : (long) count * Integer.BYTES + (count + 4L) * owner.effectBytes();
    }

    /** Packing, region copies, validation, advance, identity, result copies and validation. */
    long batchStateVisits(int from, int to) {
      int count = coverSize(from, to);
      return (count == 0 ? 1L : 4L * count + 4) * owner.states;
    }

    /**
     * At most one native invocation per nonempty range. Empty ranges use the Java identity.
     * The callback is an explicitly selected trusted computation provider, not a fallback.
     * Its result is copied before publication so provider-retained arrays cannot mutate effects.
     * Refusal returns null BEFORE invoking the provider. Cancellation never publishes a result.
     * The native call itself has no cancellation callbacks; its geometry is capped at
     * 4096 states by 64 rows. This is a work bound, not a wall-clock latency guarantee.
     */
    Effect rangeBatched(int from, int to, NativeBatch nativeBatch, Runnable checkpoint) {
      check(checkpoint);
      Objects.requireNonNull(nativeBatch, "nativeBatch");
      int count = coverSize(from, to);
      if (count > 64 || batchStateVisits(from, to) > limits.maxQueryStateVisits()
          || batchPrimitiveBytes(from, to) > limits.maxQueryPrimitiveBytes()) return null;
      if (count == 0) {
        Effect result = owner.identity();
        check(checkpoint);
        return result;
      }
      int[] nodes = coverNodes(from, to, count);
      long length = 0;
      for (int node : nodes) {
        check(checkpoint);
        length = Math.addExact(length, lengths[node]);
      }
      int[] packed = new int[Math.multiplyExact(count, owner.states)];
      for (int row = 0; row < count; row++) {
        check(checkpoint);
        System.arraycopy(next, nodes[row] * owner.states, packed, row * owner.states, owner.states);
      }
      check(checkpoint);
      int[] borrowed = nativeBatch.compose(packed, owner.states, count);
      check(checkpoint);
      if (borrowed == null || borrowed.length != owner.states) {
        throw new IllegalStateException("native regex batch geometry");
      }
      int[] result = borrowed.clone();
      for (int state = 0; state < owner.states; state++) {
        if ((state & 255) == 0) check(checkpoint);
        if (result[state] < -1 || result[state] >= owner.states
            || (length == 0 && result[state] != state)) {
          throw new IllegalStateException("native regex batch state");
        }
      }
      check(checkpoint);
      return new Effect(owner, length, result);
    }

    /** Shared ordered cover atom; right-hand nodes are prepended, never commuted. */
    private int[] coverNodes(int from, int to, int count) {
      int[] nodes = new int[count];
      int front = 0, back = count;
      for (int left = from + base, right = to + base; left < right; left >>= 1, right >>= 1) {
        if ((left & 1) != 0) nodes[front++] = left++;
        if ((right & 1) != 0) nodes[--back] = --right;
      }
      return nodes;
    }

    /**
     * Replace one leaf in a new immutable snapshot. Only its ancestor effects are recomputed,
     * but copying this flat representation is O(states * paddedFragments), NOT O(log n).
     * Both old/new snapshots can coexist; maxIndexPrimitiveBytes applies to each independently.
     */
    Index replace(int at, Effect replacement, Runnable checkpoint) {
      check(checkpoint);
      Objects.checkIndex(at, size);
      owner.require(replacement);
      Math.addExact(lengths[1] - lengths[base + at], replacement.length);
      int height = Integer.numberOfTrailingZeros(base);
      long visits = (long) next.length + (long) (height + 1) * owner.states;
      if (visits > limits.maxBuildStateVisits()) return null;
      int[] copied = new int[next.length];
      long[] copiedLengths = new long[lengths.length];
      // Bound uninterrupted copies and never publish a cancelled partial snapshot.
      for (int start = 0; start < next.length; start += Math.min(16384, next.length - start)) {
        check(checkpoint);
        System.arraycopy(next, start, copied, start, Math.min(16384, next.length - start));
      }
      for (int start = 0; start < lengths.length; start += Math.min(16384, lengths.length - start)) {
        check(checkpoint);
        System.arraycopy(lengths, start, copiedLengths, start, Math.min(16384, lengths.length - start));
      }
      int leaf = base + at;
      System.arraycopy(replacement.next, 0, copied, leaf * owner.states, owner.states);
      copiedLengths[leaf] = replacement.length;
      Index result = new Index(owner, limits, size, base, copied, copiedLengths);
      for (int node = leaf >> 1; node > 0; node >>= 1) result.rebuild(node, checkpoint);
      check(checkpoint);
      return result;
    }

    private int coverSize(int from, int to) {
      Objects.checkFromToIndex(from, to, size);
      int count = 0;
      for (int left = from + base, right = to + base; left < right; left >>= 1, right >>= 1) {
        if ((left & 1) != 0) { count++; left++; }
        if ((right & 1) != 0) { count++; right--; }
      }
      return count;
    }

    private void rebuild(int node, Runnable checkpoint) {
      int offset = node * owner.states;
      int left = 2 * offset, right = left + owner.states;
      for (int state = 0; state < owner.states; state++) {
        if ((state & 255) == 0) check(checkpoint);
        int middle = next[left + state];
        next[offset + state] = middle < 0 ? -1 : next[right + middle];
      }
      lengths[node] = Math.addExact(lengths[node << 1], lengths[(node << 1) + 1]);
    }
  }

  static final class Effect {
    private final Plan owner;
    private final long length;
    private final int[] next;
    private Effect(Plan owner, long length, int[] next) {
      this.owner = owner; this.length = length; this.next = next;
    }
    long length() { return length; }
    int next(int state) { return next[Objects.checkIndex(state, next.length)]; }
    long primitiveBytes() { return (long) next.length * Integer.BYTES; }
  }

  private static void check(Runnable checkpoint) {
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("regex effect interrupted");
    if (checkpoint != null) checkpoint.run();
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("regex effect interrupted");
  }
}
/** [M3_JJDOC] Plan#index builds immutable numeric range effects; Index#range preserves ordered
 * composition; Index#replace copies snapshots. Qualified by M3RegexDfaEffectsIndexProof, with explicit
 * refusal, JNI accounting, donor provenance and no public regex-routing change. [M3_END_JJDOC] */
