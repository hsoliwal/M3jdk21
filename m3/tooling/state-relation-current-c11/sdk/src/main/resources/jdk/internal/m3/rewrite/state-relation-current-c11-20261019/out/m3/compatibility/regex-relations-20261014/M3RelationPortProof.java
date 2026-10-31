// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

/** Executed on actual mapped numeric Java/JNI owners; graph objects below are test fixtures. */
public final class M3RelationPortProof {
  private static long checks;
  private static long cases;
  private static boolean nativeMode;
  private static final Random RANDOM = new Random(0x4d33524cL);

  private M3RelationPortProof() { }

  private static final class Node {
    final String text;
    final View left;
    final View right;
    final int length;
    Node(String text) { this.text = text; left = right = null; length = text.length(); }
    Node(View left, View right) {
      text = null; this.left = left; this.right = right;
      length = Math.addExact(left.length, right.length);
    }
  }
  private record View(Node node, int start, int length) { }
  private static View leaf(String text) { Node node = new Node(text); return new View(node, 0, node.length); }
  private static View join(View a, View b) { Node node = new Node(a, b); return new View(node, 0, node.length); }
  private static View slice(View v, int start, int end) {
    java.util.Objects.checkFromToIndex(start, end, v.length);
    return new View(v.node, v.start + start, end - start);
  }
  private static int unit(View view, int index) {
    java.util.Objects.checkIndex(index, view.length);
    int at = view.start + index;
    Node node = view.node;
    if (node.text != null) return node.text.charAt(at);
    return at < node.left.length ? unit(node.left, at) : unit(node.right, at - node.left.length);
  }
  private static String flat(View v) {
    StringBuilder b = new StringBuilder(v.length);
    for (int i = 0; i < v.length; i++) b.append((char) unit(v, i));
    return b.toString();
  }
  private static final M3DotStarRelations.Access<View> ACCESS = new M3DotStarRelations.Access<>() {
    @Override public Object owner(View v) { return v.node; }
    @Override public long coordinate(View v) { return ((long) v.start << 32) | v.length; }
    @Override public int length(View v) { return v.length; }
    @Override public int codeUnitAt(View v, int i) { return unit(v, i); }
    @Override public View first(View v) {
      if (v.node.text != null) return null;
      int split = v.node.left.length;
      int end = v.start + v.length;
      return v.start >= split ? slice(v.node.right, v.start - split, end - split)
          : slice(v.node.left, v.start, Math.min(end, split));
    }
    @Override public View second(View v) {
      if (v.node.text != null) return null;
      int split = v.node.left.length;
      return v.start < split && v.start + v.length > split
          ? slice(v.node.right, 0, v.start + v.length - split) : null;
    }
  };

  private static M3DotStarRelations.Result evaluate(View v, int[] atoms, int nodes, long budget,
      M3DotStarRelations.Join provider, Runnable checkpoint) {
    return M3DotStarRelations.evaluate(v, ACCESS, atoms, atoms.length, nodes, budget, provider, checkpoint);
  }
  private static int[] atoms(String regex) {
    int[] a = new int[regex.length()]; int count = 0;
    for (int i = 0; i < regex.length(); i++) {
      char c = regex.charAt(i);
      if (c == '*') a[count - 1] |= 1 << 16;
      else a[count++] = c;
    }
    return Arrays.copyOf(a, count);
  }
  private static String regex(int[] atoms) {
    StringBuilder b = new StringBuilder();
    for (int a : atoms) { b.append((char) a); if ((a & (1 << 16)) != 0) b.append('*'); }
    return b.toString();
  }
  private static void check(boolean valid, String reason) { checks++; if (!valid) throw new AssertionError(reason); }
  private static void refusal(Class<? extends Throwable> type, Runnable action) {
    try { action.run(); } catch (Throwable failure) {
      check(type.isInstance(failure), "wrong refusal: " + failure); return;
    }
    throw new AssertionError("missing refusal " + type.getName());
  }
  private static void oracle(View v, int[] a) {
    cases++;
    String text = flat(v);
    int expected = text.chars().anyMatch(c -> Character.isSurrogate((char) c)) ? -1
        : Pattern.matches(regex(a), text) ? 1 : 0;
    var java = evaluate(v, a, 256, 100_000_000L, null, null);
    check(java.decision() == expected, "Java regex mismatch: " + regex(a));
    if (nativeMode) {
      var jni = evaluate(v, a, 256, 100_000_000L, M3StateRelation::composeNative, null);
      check(jni.equals(java), "JNI regex/counter mismatch");
    }
  }

  private static long[] slow(long[] a, long[] b) {
    long[] result = new long[a.length];
    for (int i = 0; i < a.length; i++) for (int j = 0; j < a.length; j++)
      if ((a[i] & (1L << j)) != 0) result[i] |= b[j];
    return result;
  }
  private static void relations() {
    for (int q = 1; q <= 64; q++) for (int trial = 0; trial < 24; trial++) {
      long mask = q == 64 ? -1L : (1L << q) - 1;
      long[] a = new long[q], b = new long[q];
      for (int i = 0; i < q; i++) {
        a[i] = RANDOM.nextLong() & mask;
        b[i] = RANDOM.nextLong() & mask;
      }
      if ((trial & 3) == 0) Arrays.fill(a, a[0]);
      long[] expected = slow(a, b), result = new long[q];
      long[] originalA = a.clone(), originalB = b.clone();
      M3StateRelation.compose(a, 0, b, 0, result, 0, q);
      check(Arrays.equals(result, expected), "numeric Java compose");
      if (nativeMode) {
        check(Arrays.equals(M3StateRelation.composeNative(a, b), expected), "numeric JNI compose");
        int repeat = trial % 12;
        long[] power = new long[q];
        for (int i = 0; i < q; i++) power[i] = 1L << i;
        for (int i = 0; i < repeat; i++) power = slow(power, a);
        check(Arrays.equals(M3StateRelation.repeatNative(a, repeat), power), "numeric JNI repeat");
      }
      check(Arrays.equals(a, originalA) && Arrays.equals(b, originalB), "input mutation");
    }
    if (nativeMode) {
      refusal(NullPointerException.class, () -> M3StateRelation.composeNative(null, new long[]{1}));
      refusal(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[0], new long[0]));
      refusal(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[]{2}, new long[]{1}));
      refusal(IllegalArgumentException.class, () -> M3StateRelation.repeatNative(new long[]{1}, -1));
      check(M3StateRelation.repeatNative(new long[]{1}, Long.MAX_VALUE)[0] == 1, "long power identity");
      check(M3StateRelation.repeatNative(new long[]{0}, Long.MAX_VALUE)[0] == 0, "long power zero");
    }
  }

  private static void corpus() {
    for (int len = 0; len <= 5; len++) for (int bits = 0; bits < 1 << len; bits++) {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < len; i++) text.append((bits & (1 << i)) == 0 ? 'a' : 'b');
      View whole = leaf(text.toString());
      for (int cut = 0; cut <= len; cut++) {
        View composed = join(slice(whole, 0, cut), slice(whole, cut, len));
        for (int code = 0; code < 216; code++) {
          int n = code; int[] a = new int[3];
          for (int k = 0; k < 3; k++) {
            int x = n % 6; n /= 6;
            a[k] = "ab.".charAt(x / 2) | ((x & 1) == 0 ? 0 : 1 << 16);
          }
          oracle(composed, a);
        }
      }
    }
    char[] alphabet = {'a','b','Z','0','\n','\r','\u0085','\u2028','\u2029','\u0100','\ud800','\udc00',0};
    for (int trial = 0; trial < 1200; trial++) {
      int len = RANDOM.nextInt(25); StringBuilder b = new StringBuilder();
      for (int i = 0; i < len; i++) b.append(alphabet[RANDOM.nextInt(alphabet.length)]);
      View base = leaf(b.toString()); int cut = RANDOM.nextInt(len + 1);
      View root = join(slice(base, 0, cut), slice(base, cut, len));
      root = join(root, root);
      int start = RANDOM.nextInt(root.length + 1), end = start + RANDOM.nextInt(root.length - start + 1);
      oracle(slice(root, start, end), atoms(trial % 2 == 0 ? ".*a*b*." : "a*b*.*"));
      // Selection can create new logical adjacency without copying the original payload.
      oracle(join(slice(base, 0, cut), slice(base, Math.min(len, cut + 1), len)), atoms("a*b*.*"));
    }
    for (int count : new int[]{0, 1, 15, 31, 62, 63}) {
      int[] a = new int[count]; Arrays.fill(a, 'a' | (1 << 16));
      oracle(join(leaf("a"), leaf("aa")), a);
      oracle(join(leaf(""), leaf("")), a);
    }
  }

  private static void guards() {
    View block = leaf("ab"); View root = block;
    for (int i = 0; i < 20; i++) root = join(root, root);
    var large = evaluate(root, atoms(".*"), 128, 1_000_000, null, null);
    check(large.decision() == 1 && large.codeUnitReads() == 2 && large.reusedSummaries() == 20,
        "virtual repeated DAG should reuse leaves");
    System.out.printf("REUSE logicalUnits=%d reads=%d summaries=%d joins=%d hits=%d%n",
        root.length, large.codeUnitReads(), large.summaries(), large.compositions(), large.reusedSummaries());
    if (nativeMode) check(evaluate(root, atoms(".*"), 128, 1_000_000,
        M3StateRelation::composeNative, null).equals(large), "large JNI reuse");
    View ab = join(leaf("a"), leaf("b"));
    check(evaluate(ab, atoms("ab*"), 128, 10000, null, null).decision() == 1, "seam match");
    check(evaluate(join(ab.node.right, ab.node.left), atoms("ab*"), 128, 10000, null, null).decision() == 0,
        "noncommutative order");
    check(evaluate(ab, atoms("ab*"), 1, 10000, null, null).decision() == -1, "node refusal");
    check(evaluate(ab, atoms("ab*"), 128, 0, null, null).decision() == -1, "work refusal");
    check(evaluate(ab, new int[64], 128, 1000, null, null).decision() == -1, "wide fallback");
    refusal(CancellationException.class, () -> evaluate(ab, atoms(".*"), 128, 10000, null,
        () -> { throw new CancellationException("cancel"); }));
    Thread.currentThread().interrupt();
    try {
      check(evaluate(ab, atoms(".*"), 128, 10000, null, null).decision() == 1,
          "String path must not acquire interrupt semantics");
      check(Thread.currentThread().isInterrupted(), "interrupt must remain set");
    } finally { Thread.interrupted(); }
    if (nativeMode) {
      var normal = evaluate(join(ab, ab), atoms(".*"), 128, 10000, null, null);
      var guarded = evaluate(join(ab, ab), atoms(".*"), 128, 10000, (a,b) -> {
        long[] result = M3StateRelation.composeNative(a,b); Arrays.fill(a,0); Arrays.fill(b,0); return result;
      }, null);
      check(normal.equals(guarded), "native provider must not mutate memoized children");
      refusal(IllegalStateException.class, () -> evaluate(ab, atoms(".*"), 128, 10000,
          (a,b) -> new long[]{1}, null));
    }
  }

  public static void main(String[] args) {
    nativeMode = args.length == 1;
    if (nativeMode) System.load(args[0]);
    relations(); corpus(); guards();
    System.out.printf("M3_RELATION_PORT cases=%d checks=%d native=%s PASS%n", cases, checks, nativeMode);
  }
}
