// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Exact predicate projection for immutable indexed text. This owns no spelling dictionary.
 * Input characters are read at admission only. Shadows retain runs of predicate-equivalence IDs;
 * slices share those same primitive arrays. Coordinates belong to this exact alphabet instance.
 *
 * <p>The experimental domain is non-surrogate BMP UTF-16 units. Surrogates are rejected rather
 * than silently changing code-point or JDK semantics. Mutable inputs must not change during
 * admission; the resulting shadow retains neither the input nor its characters.
 */
public final class M3RegexPredicateAlphabet {
  private static final int UNITS = 1 << Character.SIZE;

  /** Exact immutable predicates; names are diagnostic, never the definition of membership. */
  public static final class UnitSet {
    public static final UnitSet ASCII_LOWER = range('a', 'z');
    public static final UnitSet ASCII_UPPER = range('A', 'Z');
    public static final UnitSet ASCII_DIGIT = range('0', '9');
    public static final UnitSet ASCII_WORD = ASCII_LOWER.union(ASCII_UPPER)
        .union(ASCII_DIGIT).union(literal('_'));
    public static final UnitSet ASCII_SPACE = characters(" \t\n\u000b\f\r");
    private final BitSet units;

    private UnitSet(BitSet owned) { units = owned; }

    public static UnitSet range(char first, char last) {
      if (first > last) throw new IllegalArgumentException("reversed unit range");
      if (first <= Character.MAX_SURROGATE && last >= Character.MIN_SURROGATE) {
        throw new IllegalArgumentException("surrogate ranges are outside this projection domain");
      }
      BitSet bits = new BitSet();
      bits.set(first, last + 1);
      return new UnitSet(bits);
    }

    public static UnitSet literal(char unit) { return range(unit, unit); }

    public static UnitSet characters(CharSequence text) { return characters(text, null); }

    public static UnitSet characters(CharSequence text, M3Progress monitor) {
      Objects.requireNonNull(text, "text");
      check(monitor);
      BitSet bits = new BitSet();
      for (int i = 0; i < text.length(); i++) {
        if ((i & 255) == 0) check(monitor);
        char unit = text.charAt(i);
        if (Character.isSurrogate(unit)) throw new IllegalArgumentException("surrogate literal");
        bits.set(unit);
      }
      return new UnitSet(bits);
    }

    public static UnitSet any() {
      BitSet bits = new BitSet(UNITS);
      bits.set(0, Character.MIN_SURROGATE);
      bits.set(Character.MAX_SURROGATE + 1, UNITS);
      return new UnitSet(bits);
    }

    public UnitSet union(UnitSet other) {
      BitSet result = (BitSet) units.clone();
      result.or(Objects.requireNonNull(other, "other").units);
      return new UnitSet(result);
    }

    public UnitSet intersect(UnitSet other) {
      BitSet result = (BitSet) units.clone();
      result.and(Objects.requireNonNull(other, "other").units);
      return new UnitSet(result);
    }

    public UnitSet without(UnitSet other) {
      BitSet result = (BitSet) units.clone();
      result.andNot(Objects.requireNonNull(other, "other").units);
      return new UnitSet(result);
    }

    /** Complement within the declared non-surrogate BMP domain. */
    public UnitSet complement() { return any().without(this); }

    boolean contains(int unit) { return units.get(unit); }

    /** Build-time reference syntax, not runtime interpretation of the shadow. */
    public String javaRegex() {
      if (units.isEmpty()) return "(?!)";
      StringBuilder out = new StringBuilder("[");
      for (int first = units.nextSetBit(0); first >= 0;) {
        int last = units.nextClearBit(first) - 1;
        appendUnit(out, first);
        if (last != first) { out.append('-'); appendUnit(out, last); }
        first = units.nextSetBit(last + 1);
      }
      return out.append(']').toString();
    }

    private static void appendUnit(StringBuilder out, int unit) {
      out.append("\\x{").append(Integer.toHexString(unit)).append('}');
    }

    @Override public boolean equals(Object other) {
      return other instanceof UnitSet set && units.equals(set.units);
    }
    @Override public int hashCode() { return units.hashCode(); }
  }

  /** Bounds apply before allocating atom scratch or admitting a new minterm. */
  public record Limits(int maxPredicates, int maxMinterms, int maxAtomUnits,
      long maxAdmissionPrimitiveBytes) {
    public static final Limits DEFAULT = new Limits(63, 4096, 4_000_000, 64L << 20);
    public Limits {
      if (maxPredicates < 0 || maxPredicates > 63 || maxMinterms < 1
          || maxAtomUnits < 0 || maxAdmissionPrimitiveBytes < 0) {
        throw new IllegalArgumentException("invalid projection alphabet limits");
      }
    }
  }

  private final int[] classByUnit;
  private final int classes;
  private final Limits limits;

  private M3RegexPredicateAlphabet(int[] classByUnit, int classes, Limits limits) {
    this.classByUnit = classByUnit;
    this.classes = classes;
    this.limits = limits;
  }

  public static M3RegexPredicateAlphabet compile(Collection<UnitSet> predicates,
      M3Progress monitor) {
    return compile(predicates, Limits.DEFAULT, monitor);
  }

  public static M3RegexPredicateAlphabet compile(Collection<UnitSet> predicates, Limits limits,
      M3Progress monitor) {
    Objects.requireNonNull(predicates, "predicates");
    Objects.requireNonNull(limits, "limits");
    check(monitor);
    List<UnitSet> distinct = new ArrayList<>();
    for (UnitSet predicate : predicates) {
      check(monitor);
      Objects.requireNonNull(predicate, "predicate");
      if (!distinct.contains(predicate)) {
        if (distinct.size() == limits.maxPredicates()) {
          throw new IllegalArgumentException("predicate budget exceeded");
        }
        distinct.add(predicate);
      }
    }
    int[] ids = new int[UNITS];
    Arrays.fill(ids, -1);
    Map<Long, Integer> minterms = new LinkedHashMap<>();
    for (int unit = 0; unit < UNITS; unit++) {
      if ((unit & 255) == 0) check(monitor);
      if (Character.isSurrogate((char) unit)) continue;
      long signature = 0;
      for (int p = 0; p < distinct.size(); p++) {
        if (distinct.get(p).contains(unit)) signature |= 1L << p;
      }
      Integer id = minterms.get(signature);
      if (id == null) {
        if (minterms.size() == limits.maxMinterms()) {
          throw new IllegalArgumentException("minterm budget exceeded");
        }
        id = minterms.size();
        minterms.put(signature, id);
      }
      ids[unit] = id;
    }
    check(monitor);
    return new M3RegexPredicateAlphabet(ids, minterms.size(), limits);
  }

  /** Exact class of a supported UTF-16 code unit; never reinterprets a surrogate. */
  int classForUnit(char unit) {
    int found = classByUnit[unit];
    if (found < 0) throw new IllegalArgumentException("surrogate outside predicate domain");
    return found;
  }

  public int mintermCount() { return classes; }
  public long primitivePayloadBytes() { return Integer.BYTES * (long) classByUnit.length; }

  /**
   * Lower an additional predicate only when it is an exact union of existing minterms. Thus a new
   * query may reuse a shadow, but cannot pretend an information-losing projection distinguishes
   * two literals it had grouped together. Refinement requires a new alphabet and new admission.
   */
  boolean[] lower(UnitSet predicate, M3Progress monitor) {
    Objects.requireNonNull(predicate, "predicate");
    byte[] observed = new byte[classes];
    for (int unit = 0; unit < UNITS; unit++) {
      if ((unit & 255) == 0) check(monitor);
      int id = classByUnit[unit];
      if (id < 0) continue;
      byte value = (byte) (predicate.contains(unit) ? 2 : 1);
      if (observed[id] != 0 && observed[id] != value) {
        throw new IllegalArgumentException("predicate requires projection refinement at minterm " + id);
      }
      observed[id] = value;
    }
    boolean[] accepted = new boolean[classes];
    for (int id = 0; id < classes; id++) accepted[id] = observed[id] == 2;
    check(monitor);
    return accepted;
  }

  public Shadow project(CharSequence source, M3Progress monitor) {
    Objects.requireNonNull(source, "source");
    check(monitor);
    int length = source.length();
    if (length < 0 || length > limits.maxAtomUnits()) {
      throw new IllegalArgumentException("atom unit budget exceeded");
    }
    // Two worst-case scratch lanes and their trimmed immutable copies; headers excluded.
    if (16L * length > limits.maxAdmissionPrimitiveBytes()) {
      throw new IllegalArgumentException("atom admission primitive-byte budget exceeded");
    }
    int[] types = new int[length];
    int[] ends = new int[length];
    int runs = 0;
    for (int i = 0; i < length; i++) {
      if ((i & 255) == 0) check(monitor);
      int type = classByUnit[source.charAt(i)];
      if (type < 0) {
        throw new IllegalArgumentException("surrogate at UTF-16 offset " + i + "; use existing backend");
      }
      if (runs == 0 || types[runs - 1] != type) types[runs++] = type;
      ends[runs - 1] = i + 1;
    }
    check(monitor);
    return new Shadow(this, Arrays.copyOf(types, runs), Arrays.copyOf(ends, runs), 0, length);
  }

  /** A read-only range over predicate runs, not another copy or interning pool of source spelling. */
  public static final class Shadow {
    private final M3RegexPredicateAlphabet alphabet;
    private final int[] types;
    private final int[] ends;
    private final int begin;
    private final int end;
    private final int firstRun;
    private final int limitRun;

    private Shadow(M3RegexPredicateAlphabet alphabet, int[] types, int[] ends, int begin, int end) {
      this.alphabet = alphabet;
      this.types = types;
      this.ends = ends;
      this.begin = begin;
      this.end = end;
      this.firstRun = upperBound(ends, begin);
      this.limitRun = begin == end ? firstRun : upperBound(ends, end - 1) + 1;
    }

    public M3RegexPredicateAlphabet alphabet() { return alphabet; }
    public int length() { return end - begin; }
    public int runCount() { return limitRun - firstRun; }
    /** Full backing allocation; slices share it and must not be summed as separate allocations. */
    public long backingPrimitiveBytes() { return 8L * types.length; }

    public Shadow slice(int start, int stop, M3Progress monitor) {
      Objects.checkFromToIndex(start, stop, length());
      check(monitor);
      return new Shadow(alphabet, types, ends, begin + start, begin + stop);
    }

    int typeAtRun(int run) { return types[firstRun + Objects.checkIndex(run, runCount())]; }
    int lengthAtRun(int run) {
      int actual = firstRun + Objects.checkIndex(run, runCount());
      int low = actual == 0 ? 0 : ends[actual - 1];
      return Math.min(end, ends[actual]) - Math.max(begin, low);
    }

    private static int upperBound(int[] sorted, int key) {
      int low = 0, high = sorted.length;
      while (low < high) {
        int middle = (low + high) >>> 1;
        if (sorted[middle] <= key) low = middle + 1;
        else high = middle;
      }
      return low;
    }
  }

  static void check(M3Progress monitor) {
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("projection interrupted");
    if (monitor != null) monitor.checkCanceled();
  }
}
