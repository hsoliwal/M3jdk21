// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/** Dependency-free contract proof; arbitrary DFA oracle and JDK semantic fixtures are independent. */
public final class M3RegexDfaEffectsProof {
  private static long checks;
  private M3RegexDfaEffectsProof() { }

  static final class Table implements M3RegexDfaEffects.Dfa {
    final int[][] rows;
    final boolean[] accepting;
    Table(int[][] rows, boolean[] accepting) { this.rows = rows; this.accepting = accepting; }
    @Override public int stateCount() { return rows.length; }
    @Override public int startState() { return 0; }
    @Override public int next(int state, int ascii) { return rows[state][ascii]; }
    @Override public boolean accepting(int state) { return accepting[state]; }
  }

  private static Table abc() {
    int[][] rows = blank(3);
    rows[0]['a'] = 1; rows[1]['b'] = 1; rows[1]['c'] = 2;
    return new Table(rows, new boolean[] {false, false, true});
  }

  private static Table evenA() {
    int[][] rows = blank(2);
    rows[0]['a'] = 1; rows[1]['a'] = 0;
    return new Table(rows, new boolean[] {true, false});
  }

  private static int[][] blank(int states) {
    int[][] rows = new int[states][128];
    for (int[] row : rows) Arrays.fill(row, -1);
    return rows;
  }

  private static M3RegexDfaEffects.Plan plan(Table table) {
    return M3RegexDfaEffects.prepare(table, M3RegexDfaEffects.DEFAULT_LIMITS, null);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("no target JNI provider admitted");
    run(null);
    System.out.println("M3_REGEX_DP_PROOF checks=" + checks + " native=false PASS");
  }

  static void run(M3RegexDfaEffects.NativeJoin nativeJoin) throws Exception {
    checks = 0;
    for (var fixture : new Object[][] {{abc(), "ab*c"}, {evenA(), "(?:aa)*"}}) {
      Table table = (Table) fixture[0];
      Pattern oracle = Pattern.compile((String) fixture[1]);
      var plan = plan(table);
      for (int length = 0; length <= 6; length++) {
        int count = 1 << (2 * length);
        for (int code = 0; code < count; code++) {
          String text = text(code, length);
          var whole = plan.summarize(text, null);
          equal(plan.matches(whole), oracle.matcher(text).matches(), "JDK Boolean oracle");
          equal(plan.tryMatch(text.length(), text::charAt, null) == M3RegexDfaEffects.MATCH,
              oracle.matcher(text).matches(), "cold streaming oracle");
          for (int cut = 0; cut <= text.length(); cut++) {
            var left = plan.summarize(text.substring(0, cut), null);
            var right = plan.summarize(text.substring(cut), null);
            same(plan.compose(left, right, null), whole, table.stateCount());
            if (nativeJoin != null && (code & 127) == 0) {
              same(plan.composeNative(left, right, nativeJoin, null), whole, table.stateCount());
            }
          }
        }
      }
    }
    Random random = new Random(0x4d334450L);
    for (int trial = 0; trial < 1000; trial++) {
      int states = 1 + random.nextInt(64);
      int[][] rows = blank(states);
      boolean[] accept = new boolean[states];
      for (int s = 0; s < states; s++) {
        accept[s] = random.nextBoolean();
        for (int c = 0; c < 128; c++) rows[s][c] = random.nextInt(states + 1) - 1;
      }
      Table table = new Table(rows, accept);
      var p = plan(table);
      String a = text(random.nextInt(), random.nextInt(9));
      String b = text(random.nextInt(), random.nextInt(9));
      String c = text(random.nextInt(), random.nextInt(9));
      var x = p.summarize(a, null); var y = p.summarize(b, null); var z = p.summarize(c, null);
      var whole = p.summarize(a + b + c, null);
      same(p.compose(p.compose(x, y, null), z, null), whole, states);
      same(p.compose(x, p.compose(y, z, null), null), whole, states);
      same(p.composeAll(new M3RegexDfaEffects.Effect[] {x, p.identity(), y, z}, null), whole, states);
      for (int entry = 0; entry < states; entry++) {
        int expected = entry;
        for (char unit : (a + b + c).toCharArray()) {
          if (expected >= 0) expected = rows[expected][unit];
        }
        equal(whole.next(entry), expected, "scalar incoming-state oracle");
      }
      int times = random.nextInt(10);
      same(p.repeat(x, times, null), p.summarize(a.repeat(times), null), states);
      if (nativeJoin != null) {
        same(p.composeNative(x, y, nativeJoin, null), p.summarize(a + b, null), states);
        same(p.repeat(x, times, nativeJoin, null), p.summarize(a.repeat(times), null), states);
      }
    }
    boundaries(nativeJoin);
    // Immutable prepared metadata can be shared; operation-local cursors/vectors are not shared.
    var p = plan(abc()); var x = p.summarize("ab", null); var y = p.summarize("bc", null);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Boolean>>();
      for (int n = 0; n < 64; n++) tasks.add(() -> p.matches(p.compose(x, y, null)));
      for (var future : pool.invokeAll(tasks)) require(future.get(), "concurrent immutable effects");
    }
  }

  private static void boundaries(M3RegexDfaEffects.NativeJoin nativeJoin) {
    var p = plan(abc()); var a = p.summarize("a", null); var b = p.summarize("bbb", null);
    var c = p.summarize("c", null);
    require(!p.matches(a) && !p.matches(b) && !p.matches(c), "fragments individually fail");
    require(p.matches(p.composeAll(new M3RegexDfaEffects.Effect[] {a, b, c}, null)), "cross-seam whole match");
    require(p.summarize("a\ud83d\ude00c", null) == null, "supplementary fallback");
    require(p.summarize("\ud83d", null) == null, "split high surrogate fallback");
    require(p.summarize("\ude00", null) == null, "split low surrogate fallback");
    equal(p.tryMatch(2, i -> i == 0 ? 'z' : 0xd83d, null), M3RegexDfaEffects.UNSUPPORTED,
        "dead frontier must not hide unsupported suffix");
    require(p.summarize(1, i -> -1, null) == null, "invalid cursor unit refusal");
    expect(IllegalArgumentException.class, () -> p.summarize(-1, i -> 'a', null));
    expect(IllegalArgumentException.class, () -> p.compose(a, plan(abc()).summarize("a", null), null));
    expect(IllegalArgumentException.class, () -> p.repeat(a, -1, null));
    var huge = p.repeat(a, Long.MAX_VALUE, null);
    equal(huge.length(), Long.MAX_VALUE, "checked huge repetition");
    expect(ArithmeticException.class, () -> p.compose(huge, a, null));
    expect(ArithmeticException.class, () -> p.repeat(p.summarize("aa", null), Long.MAX_VALUE, null));
    expect(CancellationException.class, () -> p.summarize("abc", () -> { throw new CancellationException(); }));
    Thread.currentThread().interrupt();
    try { expect(CancellationException.class, () -> p.compose(a, b, null)); }
    finally { Thread.interrupted(); }
    AtomicInteger reads = new AtomicInteger();
    var small = M3RegexDfaEffects.prepare(abc(), new M3RegexDfaEffects.Limits(3, 5, 12), null);
    require(small.summarize(2, i -> { reads.incrementAndGet(); return 'a'; }, null) == null, "work limit");
    equal(reads.get(), 0, "refuse before text reads");
    require(M3RegexDfaEffects.prepare(abc(), new M3RegexDfaEffects.Limits(3, 10, 11), null) == null, "byte limit");
    var counted = p.summarize(4, i -> { reads.incrementAndGet(); return "abbb".charAt(i); }, null);
    equal(reads.get(), 4, "one source read per unit");
    p.compose(counted, c, null); p.repeat(counted, 1024, null);
    equal(reads.get(), 4, "warm composition never re-reads source");
    require(small.composeAll(new M3RegexDfaEffects.Effect[] {small.identity(), small.identity()}, null) == null,
        "fan-in work limit");
    expect(IllegalStateException.class, () -> p.composeNative(a, b, (x, y, n) -> null, null));
    if (nativeJoin != null) {
      same(p.composeNative(p.identity(), a, nativeJoin, null), a, 3);
      expect(NullPointerException.class, () -> nativeJoin.compose(null, null, 1));
      expect(IllegalArgumentException.class, () -> nativeJoin.compose(new long[0], new long[0], 0));
      expect(IllegalArgumentException.class, () -> nativeJoin.compose(new long[0], new long[0], 1));
      expect(IllegalArgumentException.class, () -> nativeJoin.compose(new long[] {1, 5, 0, -1, -1},
          new long[] {1, 0, 0, -1, -1}, 1));
      expect(ArithmeticException.class, () -> nativeJoin.compose(new long[] {Long.MAX_VALUE, 0, 0, -1, -1},
          new long[] {1, 0, 0, -1, -1}, 1));
    }
  }

  private static String text(int value, int length) {
    StringBuilder text = new StringBuilder(length);
    for (int i = 0; i < length; i++) { text.append("abcd".charAt(value & 3)); value >>>= 2; }
    return text.toString();
  }
  private static void same(M3RegexDfaEffects.Effect actual, M3RegexDfaEffects.Effect expected, int states) {
    equal(actual.length(), expected.length(), "length");
    for (int state = 0; state < states; state++) equal(actual.next(state), expected.next(state), "effect");
  }
  private static void expect(Class<? extends Throwable> type, Runnable action) {
    try { action.run(); } catch (Throwable failure) {
      if (type.isInstance(failure)) { checks++; return; }
      throw new AssertionError("wrong exception", failure);
    }
    throw new AssertionError("missing " + type.getSimpleName());
  }
  private static void equal(boolean a, boolean b, String message) { require(a == b, message); }
  private static void equal(long a, long b, String message) { require(a == b, message); }
  private static void require(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }
}
