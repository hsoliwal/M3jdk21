// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Conservative boolean query over UTF-16 trigrams.
 *
 * <p>The query is a necessary condition only: a false result proves that a regex cannot match,
 * while a true result always requires the authoritative regex verifier. This is deliberately the
 * same candidate-only contract used by the rest of the MIndex search geometry.</p>
 */
public final class MIndexRegexTrigramQuery {
  public enum Op {
    ALL,
    NONE,
    AND,
    OR
  }

  private static final MIndexRegexTrigramQuery ALL =
      new MIndexRegexTrigramQuery(Op.ALL, new long[0], List.of());
  private static final MIndexRegexTrigramQuery NONE =
      new MIndexRegexTrigramQuery(Op.NONE, new long[0], List.of());

  private final Op op;
  private final long[] trigrams;
  private final List<MIndexRegexTrigramQuery> subqueries;

  private MIndexRegexTrigramQuery(
      Op op, long[] trigrams, List<MIndexRegexTrigramQuery> subqueries) {
    this.op = Objects.requireNonNull(op, "op");
    this.trigrams = Objects.requireNonNull(trigrams, "trigrams").clone();
    Arrays.sort(this.trigrams);
    this.subqueries = List.copyOf(Objects.requireNonNull(subqueries, "subqueries"));
  }

  static MIndexRegexTrigramQuery all() {
    return ALL;
  }

  static MIndexRegexTrigramQuery none() {
    return NONE;
  }

  static MIndexRegexTrigramQuery fromExact(List<String> exact) {
    Objects.requireNonNull(exact, "exact");
    if (exact.isEmpty()) return NONE;

    ArrayList<MIndexRegexTrigramQuery> alternatives = new ArrayList<>(exact.size());
    for (String value : exact) {
      String checked = Objects.requireNonNull(value, "exact value");
      if (checked.length() < 3) {
        // One legal branch can match without any trigram, so no trigram is globally required.
        return ALL;
      }
      long[] grams = uniqueTrigrams(checked);
      alternatives.add(new MIndexRegexTrigramQuery(Op.AND, grams, List.of()));
    }
    return combine(Op.OR, alternatives);
  }

  static MIndexRegexTrigramQuery and(List<MIndexRegexTrigramQuery> terms) {
    return combine(Op.AND, terms);
  }

  static MIndexRegexTrigramQuery or(List<MIndexRegexTrigramQuery> terms) {
    return combine(Op.OR, terms);
  }

  private static MIndexRegexTrigramQuery combine(
      Op op, List<MIndexRegexTrigramQuery> input) {
    if (op != Op.AND && op != Op.OR) throw new IllegalArgumentException("combine op");
    Objects.requireNonNull(input, "input");

    ArrayList<MIndexRegexTrigramQuery> flat = new ArrayList<>();
    ArrayList<Long> grams = new ArrayList<>();
    for (MIndexRegexTrigramQuery term : input) {
      MIndexRegexTrigramQuery checked = Objects.requireNonNull(term, "term");
      if (op == Op.AND) {
        if (checked.op == Op.NONE) return NONE;
        if (checked.op == Op.ALL) continue;
      } else {
        if (checked.op == Op.ALL) return ALL;
        if (checked.op == Op.NONE) continue;
      }

      if (checked.op == op) {
        for (long gram : checked.trigrams) grams.add(gram);
        flat.addAll(checked.subqueries);
      } else if (checked.subqueries.isEmpty() && checked.trigrams.length == 1) {
        grams.add(checked.trigrams[0]);
      } else {
        flat.add(checked);
      }
    }

    if (grams.isEmpty() && flat.isEmpty()) return op == Op.AND ? ALL : NONE;
    if (grams.isEmpty() && flat.size() == 1) return flat.getFirst();

    long[] gramArray = new long[grams.size()];
    for (int i = 0; i < gramArray.length; i++) gramArray[i] = grams.get(i);
    if (gramArray.length > 1) {
      Arrays.sort(gramArray);
      int unique = 1;
      for (int i = 1; i < gramArray.length; i++) {
        if (gramArray[i] != gramArray[unique - 1]) gramArray[unique++] = gramArray[i];
      }
      gramArray = Arrays.copyOf(gramArray, unique);
    }
    return new MIndexRegexTrigramQuery(op, gramArray, flat);
  }

  public Op op() {
    return op;
  }

  public long[] trigrams() {
    return trigrams.clone();
  }

  public List<MIndexRegexTrigramQuery> subqueries() {
    return subqueries;
  }

  long[] trigramsForReadOnlyKernel() {
    return trigrams;
  }

  List<MIndexRegexTrigramQuery> subqueriesForReadOnlyKernel() {
    return subqueries;
  }

  public boolean hasConstraints() {
    return op != Op.ALL;
  }

  public int termCount() {
    int count = trigrams.length;
    for (MIndexRegexTrigramQuery subquery : subqueries) {
      count = Math.addExact(count, subquery.termCount());
    }
    return count;
  }

  /** Evaluate only the conservative trigram condition. */
  public boolean test(LongPredicate containsTrigram) {
    Objects.requireNonNull(containsTrigram, "containsTrigram");
    return switch (op) {
      case ALL -> true;
      case NONE -> false;
      case AND -> {
        for (long gram : trigrams) {
          if (!containsTrigram.test(gram)) yield false;
        }
        for (MIndexRegexTrigramQuery subquery : subqueries) {
          if (!subquery.test(containsTrigram)) yield false;
        }
        yield true;
      }
      case OR -> {
        for (long gram : trigrams) {
          if (containsTrigram.test(gram)) yield true;
        }
        for (MIndexRegexTrigramQuery subquery : subqueries) {
          if (subquery.test(containsTrigram)) yield true;
        }
        yield false;
      }
    };
  }

  /**
   * Precompute exact UTF-16 trigram membership without retaining or materializing the input.
   *
   * <p>The caller must keep the input stable throughout this bounded scan and associate the
   * result with that exact immutable value/range. This is not a persistent resolver identity.
   * Preparation reads each code unit once, sorts at most {@code length - 2} keys and retains
   * only unique keys and two boundary units at each end. Budgets count UTF-16 units, not bytes.
   *
   * @param value stable text, including an indexed or sliced CharSequence
   * @param maxUtf16Units nonnegative maximum length admitted before any character read
   * @return caller-owned immutable membership facts
   * @throws IllegalArgumentException if the length or budget is invalid or exceeds the budget
   */
  public static Facts precompute(CharSequence value, int maxUtf16Units) {
    Objects.requireNonNull(value, "value");
    int length = value.length();
    requireFactBudget(length, maxUtf16Units);
    return scanFacts(value, 0, length);
  }

  /**
   * Prepare facts for a half-open UTF-16 range without slicing or materializing the source.
   *
   * <p>Null, range and range-length budget checks precede character reads and key allocation.
   * Each admitted unit is read once; units outside the range are never read. The caller must
   * keep the source stable during preparation and bind the result to that exact source/range.
   * The budget applies to the range, not the backing sequence's length or total resident bytes.
   * These facts describe only the isolated range: transparent regex lookarounds may observe
   * outside it, so constraints derived from such context require separately justified scope.
   *
   * @param value stable indexed, joined, sliced or ordinary character sequence
   * @param from inclusive UTF-16 start
   * @param to exclusive UTF-16 end
   * @param maxUtf16Units nonnegative maximum range length
   * @return immutable membership facts for the isolated range
   * @throws NullPointerException if value is null
   * @throws IndexOutOfBoundsException if the range is invalid
   * @throws IllegalArgumentException if the budget is negative or smaller than the range
   */
  public static Facts precompute(CharSequence value, int from, int to, int maxUtf16Units) {
    Objects.requireNonNull(value, "value");
    Objects.checkFromToIndex(from, to, value.length());
    int length = to - from;
    requireFactBudget(length, maxUtf16Units);
    return scanFacts(value, from, length);
  }

  private static Facts scanFacts(CharSequence value, int from, int length) {
    long[] keys = new long[Math.max(0, length - 2)];
    long window = 0L;
    int prefix = 0;
    for (int index = 0; index < length; index++) {
      char unit = value.charAt(from + index);
      if (index < 2) prefix = (prefix << 16) | unit;
      window = ((window << 16) | unit) & 0xffffffffffffL;
      if (index >= 2) keys[index - 2] = window;
    }
    Arrays.sort(keys);
    return new Facts(length, prefix, (int) window, compactFactKeys(keys));
  }

  /**
   * Evaluate only the existing necessary-condition query against prepared membership facts.
   * A true result is not an exact regex match, position, count or equality proof.
   */
  public boolean testPrecomputed(Facts facts) {
    return test(Objects.requireNonNull(facts, "facts"));
  }

  /**
   * Immutable derived search metadata; never a second canonical spelling store.
   *
   * <p>Composition merges exact key sets and at most two seam keys. It does not read either
   * original payload. Sorted membership discards multiplicity and order: these facts cannot
   * establish a match or recover arbitrary slices. Array exports are defensive copies.
   */
  public static final class Facts implements LongPredicate {
    private final int utf16Length;
    private final int prefix;
    private final int suffix;
    private final long[] keys;

    private Facts(int utf16Length, int prefix, int suffix, long[] ownedKeys) {
      this.utf16Length = utf16Length;
      this.prefix = prefix;
      this.suffix = suffix;
      this.keys = ownedKeys;
    }

    /** Number of UTF-16 units in the exact value represented by these facts. */
    public int utf16Length() {
      return utf16Length;
    }

    /** Number of distinct packed 48-bit keys, not the number of occurrences. */
    public int keyCount() {
      return keys.length;
    }

    /** Sorted unique membership keys; changing the export cannot change these facts. */
    public long[] keys() {
      return keys.clone();
    }

    /** Exact membership in this UTF-16 trigram set; not a regex acceptance predicate. */
    @Override
    public boolean test(long key) {
      return Arrays.binarySearch(keys, key) >= 0;
    }

    /**
     * Prepare facts for the ordered concatenation using metadata only.
     *
     * <p>With k retained keys this uses O(k) merge work and O(k) temporary/output space.
     * The budget is checked before allocation, including for empty identities. No global
     * cache or all-pairs precomputation is performed. Callers own retention and reuse.
     *
     * @param right facts for the immediately following value
     * @param maxUtf16Units nonnegative maximum combined length
     * @return facts observationally equal to preparing the concatenated text
     * @throws ArithmeticException if combined length exceeds the Java int coordinate domain
     * @throws IllegalArgumentException if the budget is invalid or exceeded
     */
    public Facts concat(Facts right, int maxUtf16Units) {
      Objects.requireNonNull(right, "right");
      int length = Math.addExact(utf16Length, right.utf16Length);
      requireFactBudget(length, maxUtf16Units);
      if (utf16Length == 0) return right;
      if (right.utf16Length == 0) return this;
      int nextPrefix = utf16Length >= 2 ? prefix : (suffix << 16) | right.first();
      int nextSuffix = right.utf16Length >= 2
          ? right.suffix : ((int) (char) suffix << 16) | right.suffix;
      long[] seam = seamKeys(right);
      return new Facts(length, nextPrefix, nextSuffix, mergeFactKeys(keys, right.keys, seam));
    }

    private char first() {
      return (char) (utf16Length == 1 ? prefix : prefix >>> 16);
    }

    private long[] seamKeys(Facts right) {
      int count = (utf16Length >= 2 ? 1 : 0) + (right.utf16Length >= 2 ? 1 : 0);
      long[] seam = new long[count];
      int index = 0;
      if (utf16Length >= 2) {
        seam[index++] = trigram((char) (suffix >>> 16), (char) suffix, right.first());
      }
      if (right.utf16Length >= 2) {
        seam[index] = trigram((char) suffix, right.first(), (char) right.prefix);
      }
      Arrays.sort(seam);
      return seam;
    }
  }

  private static void requireFactBudget(int length, int budget) {
    if (length < 0 || budget < 0 || length > budget) {
      throw new IllegalArgumentException("UTF-16 precompute budget exceeded or invalid");
    }
  }

  private static long[] compactFactKeys(long[] keys) {
    if (keys.length < 2) return keys;
    int count = 1;
    for (int index = 1; index < keys.length; index++) {
      if (keys[index] != keys[count - 1]) keys[count++] = keys[index];
    }
    return count == keys.length ? keys : Arrays.copyOf(keys, count);
  }

  private static long[] mergeFactKeys(long[] left, long[] right, long[] seam) {
    int capacity = Math.addExact(Math.addExact(left.length, right.length), seam.length);
    long[] merged = new long[capacity];
    int a = 0;
    int b = 0;
    int c = 0;
    int count = 0;
    while (a < left.length || b < right.length || c < seam.length) {
      long key = Math.min(a < left.length ? left[a] : Long.MAX_VALUE,
          Math.min(b < right.length ? right[b] : Long.MAX_VALUE,
              c < seam.length ? seam[c] : Long.MAX_VALUE));
      if (a < left.length && left[a] == key) a++;
      if (b < right.length && right[b] == key) b++;
      if (c < seam.length && seam[c] == key) c++;
      if (count == 0 || merged[count - 1] != key) merged[count++] = key;
    }
    return count == capacity ? merged : Arrays.copyOf(merged, count);
  }

  /** Packs three UTF-16 units into one unsigned 48-bit key. */
  public static long trigram(char first, char second, char third) {
    return ((long) first << 32) | ((long) second << 16) | third;
  }

  static long[] uniqueTrigrams(CharSequence value) {
    Objects.requireNonNull(value, "value");
    if (value.length() < 3) return new long[0];
    long[] keys = new long[value.length() - 2];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = trigram(value.charAt(i), value.charAt(i + 1), value.charAt(i + 2));
    }
    Arrays.sort(keys);
    int unique = 1;
    for (int i = 1; i < keys.length; i++) {
      if (keys[i] != keys[unique - 1]) keys[unique++] = keys[i];
    }
    return Arrays.copyOf(keys, unique);
  }

  @Override
  public String toString() {
    return switch (op) {
      case ALL -> "ALL";
      case NONE -> "NONE";
      case AND, OR -> render();
    };
  }

  private String render() {
    String separator = op == Op.AND ? " & " : " | ";
    ArrayList<String> terms = new ArrayList<>(trigrams.length + subqueries.size());
    for (long gram : trigrams) terms.add(quote(gram));
    for (MIndexRegexTrigramQuery subquery : subqueries) {
      terms.add("(" + subquery + ")");
    }
    return String.join(separator, terms);
  }

  private static String quote(long gram) {
    char first = (char) (gram >>> 32);
    char second = (char) (gram >>> 16);
    char third = (char) gram;
    return "'" + new String(new char[] {first, second, third}) + "'";
  }
}