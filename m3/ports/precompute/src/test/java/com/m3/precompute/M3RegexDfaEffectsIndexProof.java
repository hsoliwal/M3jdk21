// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent scalar automaton oracle, not a second range-tree implementation. */
public final class M3RegexDfaEffectsIndexProof {
  private static long checks;
  static final M3RegexDfaEffects.IndexLimits ROOM = new M3RegexDfaEffects.IndexLimits(
      4096, 32L << 20, 32L << 20, 32L << 20, 32L << 20);
  private M3RegexDfaEffectsIndexProof() { }

  static final class Table implements M3RegexDfaEffects.Dfa {
    final int[][] next;
    int reads;
    Table(int[][] next) { this.next = next; }
    public int stateCount() { return next.length; }
    public int startState() { return 0; }
    public int next(int state, int unit) { reads++; return next[state][unit]; }
    public boolean accepting(int state) { return state == 0; }
  }

  static M3RegexDfaEffects.Plan plan(Table table) {
    return M3RegexDfaEffects.prepare(table, M3RegexDfaEffects.DEFAULT_LIMITS, null);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("no target JNI provider admitted");
    run(null);
  }

  static void run(M3RegexDfaEffects.NativeJoin nativeJoin) throws Exception {
    checks = 0;
    Random random = new Random(0x4d33494e444558L);
    for (int trial = 0; trial < 180; trial++) {
      int states = 1 + random.nextInt(24);
      int[][] rows = new int[states][128];
      for (int state = 0; state < states; state++) {
        for (int unit = 0; unit < 128; unit++) {
          rows[state][unit] = trial % 2 == 0 ? random.nextInt(states) : random.nextInt(states + 1) - 1;
        }
      }
      Table table = new Table(rows);
      var plan = plan(table);
      int count = random.nextInt(33);
      String[] texts = new String[count];
      M3RegexDfaEffects.Effect[] parts = new M3RegexDfaEffects.Effect[count];
      for (int i = 0; i < count; i++) {
        texts[i] = word(random);
        parts[i] = plan.summarize(texts[i], null);
      }
      var index = plan.index(parts, ROOM, null);
      require(index != null && index.fragments() == count, "index admission");
      int reads = table.reads;
      for (int from = 0; from <= count; from++) {
        for (int to = from; to <= count; to++) {
          String text = concatenate(texts, from, to);
          oracle(index.range(from, to, null), text, rows);
          if (nativeJoin != null && trial % 7 == 0) {
            oracle(index.range(from, to, nativeJoin, null), text, rows);
          }
        }
      }
      equal(table.reads, reads, "queries must not revisit automaton transitions");
      if (count != 0) {
        int at = random.nextInt(count);
        String replacement = word(random);
        var changed = index.replace(at, plan.summarize(replacement, null), null);
        require(changed != null, "snapshot admission");
        for (int from = 0; from <= count; from++) {
          for (int to = from; to <= count; to++) {
            String original = concatenate(texts, from, to);
            oracle(index.range(from, to, null), original, rows);
            String[] modified = texts.clone(); modified[at] = replacement;
            oracle(changed.range(from, to, null), concatenate(modified, from, to), rows);
          }
        }
      }
      String original = concatenate(texts, 0, count);
      Arrays.fill(parts, plan.identity());
      oracle(index.range(0, count, null), original, rows);
      equal(index.length(), original.length(), "checked total length");
    }
    boundaries(nativeJoin);
    System.out.println("M3_REGEX_INDEX_PROOF checks=" + checks + " native=" + (nativeJoin != null) + " PASS");
  }

  private static Table noncommutative() {
    int[][] rows = new int[3][128];
    for (int[] row : rows) Arrays.fill(row, -1);
    rows[0]['a'] = 1; rows[1]['a'] = 0; rows[2]['a'] = 2;
    rows[0]['b'] = 0; rows[1]['b'] = 2; rows[2]['b'] = 1;
    rows[0]['c'] = 2; rows[1]['c'] = 1; rows[2]['c'] = 0;
    return new Table(rows);
  }

  private static void boundaries(M3RegexDfaEffects.NativeJoin nativeJoin) throws Exception {
    Table table = noncommutative();
    var plan = plan(table);
    var a = plan.summarize("a", null); var b = plan.summarize("b", null);
    var parts = new M3RegexDfaEffects.Effect[] {a, b, a, b, b, a, b};
    var index = plan.index(parts, ROOM, null);
    oracle(index.range(1, 7, null), "babbab", table.next);
    long retained = index.retainedPrimitiveBytes();
    require(plan.index(parts, limits(retained, 45, Long.MAX_VALUE, Long.MAX_VALUE), null) != null,
        "exact retained/build bounds");
    require(plan.index(parts, limits(retained - 1, 45, Long.MAX_VALUE, Long.MAX_VALUE), null) == null,
        "one byte short");
    require(plan.index(parts, limits(retained, 44, Long.MAX_VALUE, Long.MAX_VALUE), null) == null,
        "one visit short");
    require(plan.index(parts, new M3RegexDfaEffects.IndexLimits(6, retained, 45, 99, 999), null) == null,
        "fragment cap");
    long work = index.queryStateVisits(1, 7);
    long bytes = index.queryPrimitiveBytes(1, 7, false);
    var exact = plan.index(parts, limits(retained, 45, work, bytes), null);
    require(exact.range(1, 7, null) != null, "exact query bounds");
    require(plan.index(parts, limits(retained, 45, work - 1, bytes), null).range(1, 7, null) == null,
        "query work preflight");
    require(plan.index(parts, limits(retained, 45, work, bytes - 1), null).range(1, 7, null) == null,
        "query byte preflight");
    require(exact.replace(0, b, null) == null, "snapshot copying charged to build work");
    AtomicInteger nativeCalls = new AtomicInteger();
    M3RegexDfaEffects.NativeJoin forbidden = (x, y, n) -> { nativeCalls.incrementAndGet(); throw new AssertionError("JNI"); };
    require(exact.range(1, 7, forbidden, null) == null, "native allocation preflight");
    equal(nativeCalls.get(), 0, "no JNI before admission");
    require(index.range(2, 2, forbidden, null).length() == 0, "empty query identity without JNI");
    equal(nativeCalls.get(), 0, "empty range must not cross JNI");
    expect(IndexOutOfBoundsException.class, () -> index.range(-1, 0, null));
    expect(IndexOutOfBoundsException.class, () -> index.range(3, 2, null));
    expect(IndexOutOfBoundsException.class, () -> index.range(0, 8, null));
    expect(IndexOutOfBoundsException.class, () -> index.replace(7, a, null));
    expect(NullPointerException.class, () -> plan.index(null, ROOM, null));
    expect(NullPointerException.class, () -> plan.index(new M3RegexDfaEffects.Effect[] {null}, ROOM, null));
    expect(IllegalArgumentException.class, () -> new M3RegexDfaEffects.IndexLimits(-1, 1, 1, 1, 1));
    var foreign = plan(noncommutative()).summarize("a", null);
    expect(IllegalArgumentException.class, () -> plan.index(new M3RegexDfaEffects.Effect[] {foreign}, ROOM, null));
    expect(IllegalArgumentException.class, () -> index.replace(0, foreign, null));
    var huge = plan.repeat(a, Long.MAX_VALUE, null);
    expect(ArithmeticException.class, () -> plan.index(new M3RegexDfaEffects.Effect[] {huge, a}, ROOM, null));
    var hugeIndex = plan.index(new M3RegexDfaEffects.Effect[] {huge, plan.identity()}, ROOM, null);
    equal(hugeIndex.range(0, 1, null).length(), Long.MAX_VALUE, "long logical length survives tree");
    expect(ArithmeticException.class, () -> hugeIndex.replace(1, a, null));
    expect(CancellationException.class, () -> index.range(0, 7, () -> { throw new CancellationException(); }));
    AtomicInteger polls = new AtomicInteger();
    expect(CancellationException.class, () -> index.replace(0, b,
        () -> { if (polls.incrementAndGet() == 3) throw new CancellationException(); }));
    oracle(index.range(0, 7, null), "ababbab", table.next);
    Thread.currentThread().interrupt();
    try {
      expect(CancellationException.class, () -> plan.index(parts, ROOM, null));
      require(Thread.currentThread().isInterrupted(), "interruption must remain set");
    } finally { Thread.interrupted(); }
    require(plan.summarize("\ud83d", null) == null && plan.summarize("\ude00", null) == null,
        "no accidental Unicode admission");
    require(plan.summarize("\u00e9", null) == null, "existing ASCII guard unchanged");
    // Child source is accessed only during initial preparation, not building/querying/replacing.
    AtomicInteger reads = new AtomicInteger();
    var counted = plan.summarize(2, i -> { reads.incrementAndGet(); return 'a'; }, null);
    var reusable = plan.index(new M3RegexDfaEffects.Effect[] {counted, counted}, ROOM, null);
    reusable.range(0, 2, null); reusable.replace(1, counted, null);
    equal(reads.get(), 2, "canonical input not reread or retained");
    if (nativeJoin != null) {
      long nativeBytes = index.queryPrimitiveBytes(1, 7, true);
      var admitted = plan.index(parts, limits(retained, 45, work, nativeBytes), null);
      oracle(admitted.range(1, 7, nativeJoin, null), "babbab", table.next);
      require(plan.index(parts, limits(retained, 45, work, nativeBytes - 1), null)
          .range(1, 7, forbidden, null) == null, "native one byte short");
      expect(IllegalStateException.class, () -> index.range(0, 1, (x, y, n) -> null, null));
      expect(IllegalStateException.class, () -> index.range(0, 1, (x, y, n) -> {
        long[] out = nativeJoin.compose(x, y, n); out[1 + 4 * (n - 1)] = 0; return out;
      }, null));
    }
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      var tasks = new java.util.ArrayList<java.util.concurrent.Callable<M3RegexDfaEffects.Effect>>();
      for (int n = 0; n < 128; n++) tasks.add(() -> index.range(1, 7, nativeJoin, null));
      for (var result : pool.invokeAll(tasks)) oracle(result.get(), "babbab", table.next);
    }
  }

  private static M3RegexDfaEffects.IndexLimits limits(long bytes, long build, long query, long allocation) {
    return new M3RegexDfaEffects.IndexLimits(4096, bytes, build, query, allocation);
  }
  private static String word(Random random) {
    StringBuilder text = new StringBuilder();
    for (int n = random.nextInt(5); n > 0; n--) text.append("abcd".charAt(random.nextInt(4)));
    return text.toString();
  }
  private static String concatenate(String[] texts, int from, int to) {
    StringBuilder text = new StringBuilder();
    for (int i = from; i < to; i++) text.append(texts[i]);
    return text.toString();
  }
  private static void oracle(M3RegexDfaEffects.Effect actual, String text, int[][] rows) {
    require(actual != null, "unexpected range refusal");
    equal(actual.length(), text.length(), "range logical length");
    for (int entry = 0; entry < rows.length; entry++) {
      int state = entry;
      for (int i = 0; i < text.length(); i++) if (state >= 0) state = rows[state][text.charAt(i)];
      equal(actual.next(entry), state, "ordered range effect");
    }
  }
  private static void expect(Class<? extends Throwable> type, Runnable action) {
    try { action.run(); } catch (Throwable error) {
      if (type.isInstance(error)) { checks++; return; }
      throw new AssertionError("wrong exception", error);
    }
    throw new AssertionError("missing " + type.getSimpleName());
  }
  private static void equal(long actual, long expected, String label) { require(actual == expected, label); }
  private static void require(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
}
/** [M3_JJDOC] Independent range/snapshot oracles cover all incoming states, order, dead states,
 * boundaries, budgets, cancellation, owner identity, source reuse and optional JNI. [M3_END_JJDOC] */
