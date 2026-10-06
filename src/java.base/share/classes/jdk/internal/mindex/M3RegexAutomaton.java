/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Synexia-owned bounded UTF-16 regular-expression automaton.
 *
 * <p>This implementation is intentionally dependency-free. It uses the classical Thompson-NFA to
 * deterministic-automaton pipeline and supports the regular subset needed by precompute:
 * concatenation, alternation, grouping, dot, character classes, {@code * + ?}, anchors and common
 * escapes. Backreferences, lookaround and other non-regular constructs are rejected.</p>
 */
public final class M3RegexAutomaton {
  private static final int MAX_PATTERN_UNITS = 8192;
  private static final int MAX_NFA_STATES = 4096;
  private static final int MAX_DFA_STATES = 4096;

  private final String pattern;
  private final boolean anchoredStart;
  private final boolean anchoredEnd;
  private final int startState;
  private final int[] offsets;
  private final char[] minimum;
  private final char[] maximum;
  private final int[] targets;
  private final boolean[] accepting;
  private final boolean[] canReachAccept;
  private final String rootHash;

  private M3RegexAutomaton(
      String pattern,
      boolean anchoredStart,
      boolean anchoredEnd,
      int startState,
      int[] offsets,
      char[] minimum,
      char[] maximum,
      int[] targets,
      boolean[] accepting) {
    this.pattern = pattern;
    this.anchoredStart = anchoredStart;
    this.anchoredEnd = anchoredEnd;
    this.startState = startState;
    this.offsets = offsets;
    this.minimum = minimum;
    this.maximum = maximum;
    this.targets = targets;
    this.accepting = accepting;
    this.canReachAccept = computeCanReachAccept(offsets, targets, accepting);

    long laneCount =
        (long) offsets.length + minimum.length + maximum.length + targets.length + 3L;
    if (laneCount > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("automaton image too large");
    }
    long[] lanes = new long[(int) laneCount];
    int cursor = 0;
    lanes[cursor++] = Integer.toUnsignedLong(startState);
    lanes[cursor++] = anchoredStart ? 1L : 0L;
    lanes[cursor++] = anchoredEnd ? 1L : 0L;
    for (int value : offsets) lanes[cursor++] = Integer.toUnsignedLong(value);
    for (char value : minimum) lanes[cursor++] = value;
    for (char value : maximum) lanes[cursor++] = value;
    for (int value : targets) lanes[cursor++] = Integer.toUnsignedLong(value);
    int[] flags = new int[accepting.length];
    for (int i = 0; i < accepting.length; i++) flags[i] = accepting[i] ? 1 : 0;
    this.rootHash =
        M3PrecomputeHash.sha256("SYNEXIA_REGEX_AUTOMATON_V1:" + pattern, lanes, flags);
  }

  public static M3RegexAutomaton compile(String source) {
    String pattern = Objects.requireNonNull(source, "source");
    if (pattern.length() > MAX_PATTERN_UNITS) {
      throw new IllegalArgumentException("regex exceeds UTF-16 unit bound: " + pattern.length());
    }

    boolean anchoredStart = pattern.startsWith("^");
    int from = anchoredStart ? 1 : 0;
    boolean anchoredEnd = hasTerminalUnescapedDollar(pattern, from);
    int to = anchoredEnd ? pattern.length() - 1 : pattern.length();
    String body = pattern.substring(from, to);

    Nfa nfa = new Nfa();
    Parser parser = new Parser(body, nfa);
    Fragment fragment = parser.parse();
    nfa.states.get(fragment.end).accepting = true;
    DfaData dfa = determinize(nfa, fragment.start);

    return new M3RegexAutomaton(
        pattern,
        anchoredStart,
        anchoredEnd,
        dfa.startState,
        dfa.offsets,
        dfa.minimum,
        dfa.maximum,
        dfa.targets,
        dfa.accepting);
  }

  public String pattern() { return pattern; }
  public String rootHash() { return rootHash; }
  public int stateCount() { return accepting.length; }
  public int transitionCount() { return targets.length; }
  public boolean anchoredStart() { return anchoredStart; }
  public boolean anchoredEnd() { return anchoredEnd; }

  public long retainedBytes() {
    return (long) offsets.length * Integer.BYTES
        + (long) minimum.length * Character.BYTES
        + (long) maximum.length * Character.BYTES
        + (long) targets.length * Integer.BYTES
        + (long) accepting.length
        + (long) canReachAccept.length;
  }

  /** Equivalent to a whole-input regular match. */
  public boolean matches(CharSequence input) {
    Objects.requireNonNull(input, "input");
    int state = startState;
    for (int index = 0; index < input.length(); index++) {
      state = transition(state, input.charAt(index));
      if (state < 0) return false;
    }
    return accepting[state];
  }

  /**
   * Search for any matching substring. The execution keeps the active deterministic states for all
   * possible starts, which remains input-linear for a bounded automaton.
   */
  public boolean find(CharSequence input) {
    Objects.requireNonNull(input, "input");
    int length = input.length();

    if (anchoredStart) {
      int state = startState;
      if (accepting[state] && (!anchoredEnd || length == 0)) return true;
      for (int index = 0; index < length; index++) {
        state = transition(state, input.charAt(index));
        if (state < 0) return false;
        if (accepting[state] && (!anchoredEnd || index + 1 == length)) return true;
      }
      return false;
    }

    if (accepting[startState]) {
      if (!anchoredEnd || length >= 0) return true;
    }

    BitSet active = new BitSet(accepting.length);
    for (int index = 0; index < length; index++) {
      char unit = input.charAt(index);
      BitSet next = new BitSet(accepting.length);

      int fresh = transition(startState, unit);
      if (fresh >= 0) next.set(fresh);

      for (int state = active.nextSetBit(0);
          state >= 0;
          state = active.nextSetBit(state + 1)) {
        int target = transition(state, unit);
        if (target >= 0) next.set(target);
      }

      if (!anchoredEnd || index + 1 == length) {
        for (int state = next.nextSetBit(0);
            state >= 0;
            state = next.nextSetBit(state + 1)) {
          if (accepting[state]) return true;
        }
      }
      active = next;
    }

    return anchoredEnd && accepting[startState];
  }

  /**
   * True when the supplied prefix can still be extended to some full regex match.
   *
   * <p>This is a safe precompute rejection fact: false proves impossibility; true is candidate-only.</p>
   */
  public boolean prefixCanStillMatch(CharSequence prefix) {
    Objects.requireNonNull(prefix, "prefix");
    int state = startState;
    for (int index = 0; index < prefix.length(); index++) {
      state = transition(state, prefix.charAt(index));
      if (state < 0) return false;
    }
    return canReachAccept[state];
  }

  /** UTF-16 length of the longest accepted prefix, or -1 when none is accepted. */
  public int longestAcceptedPrefix(CharSequence input) {
    Objects.requireNonNull(input, "input");
    int state = startState;
    int best = accepting[state] ? 0 : -1;
    for (int index = 0; index < input.length(); index++) {
      state = transition(state, input.charAt(index));
      if (state < 0) break;
      if (accepting[state]) best = index + 1;
    }
    return best;
  }

  private int transition(int state, char unit) {
    int low = offsets[state];
    int high = offsets[state + 1] - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      if (unit < minimum[middle]) {
        high = middle - 1;
      } else if (unit > maximum[middle]) {
        low = middle + 1;
      } else {
        return targets[middle];
      }
    }
    return -1;
  }

  private static DfaData determinize(Nfa nfa, int nfaStart) {
    BitSet seed = new BitSet(nfa.states.size());
    seed.set(nfaStart);
    BitSet startClosure = epsilonClosure(nfa, seed);

    HashMap<BitSet, Integer> ids = new HashMap<>();
    ArrayList<BitSet> closures = new ArrayList<>();
    ArrayList<DfaBuildState> states = new ArrayList<>();

    BitSet startKey = (BitSet) startClosure.clone();
    ids.put(startKey, 0);
    closures.add(startKey);
    states.add(new DfaBuildState());

    for (int cursor = 0; cursor < closures.size(); cursor++) {
      BitSet closure = closures.get(cursor);
      DfaBuildState current = states.get(cursor);
      current.accepting = containsAccepting(nfa, closure);

      TreeSet<Integer> boundaries = new TreeSet<>();
      for (int state = closure.nextSetBit(0);
          state >= 0;
          state = closure.nextSetBit(state + 1)) {
        for (RangeEdge edge : nfa.states.get(state).ranges) {
          boundaries.add(edge.minimum);
          boundaries.add(edge.maximum == Character.MAX_VALUE ? 65536 : edge.maximum + 1);
        }
      }
      if (boundaries.isEmpty()) continue;

      ArrayList<Integer> points = new ArrayList<>(boundaries);
      for (int i = 0; i + 1 < points.size(); i++) {
        int low = points.get(i);
        int highExclusive = points.get(i + 1);
        if (low >= highExclusive || low > Character.MAX_VALUE) continue;
        int high = Math.min(Character.MAX_VALUE, highExclusive - 1);

        BitSet destination = new BitSet(nfa.states.size());
        for (int state = closure.nextSetBit(0);
            state >= 0;
            state = closure.nextSetBit(state + 1)) {
          for (RangeEdge edge : nfa.states.get(state).ranges) {
            if (low >= edge.minimum && low <= edge.maximum) destination.set(edge.target);
          }
        }
        if (destination.isEmpty()) continue;
        destination = epsilonClosure(nfa, destination);

        Integer target = ids.get(destination);
        if (target == null) {
          if (closures.size() >= MAX_DFA_STATES) {
            throw new IllegalArgumentException("regex DFA state bound exceeded");
          }
          BitSet key = (BitSet) destination.clone();
          target = closures.size();
          ids.put(key, target);
          closures.add(key);
          states.add(new DfaBuildState());
        }
        current.addRange((char) low, (char) high, target);
      }
    }

    int transitions = states.stream().mapToInt(state -> state.ranges.size()).sum();
    int[] offsets = new int[states.size() + 1];
    char[] minimum = new char[transitions];
    char[] maximum = new char[transitions];
    int[] targets = new int[transitions];
    boolean[] accepting = new boolean[states.size()];

    int edge = 0;
    for (int state = 0; state < states.size(); state++) {
      DfaBuildState build = states.get(state);
      offsets[state] = edge;
      accepting[state] = build.accepting;
      for (DfaRange range : build.ranges) {
        minimum[edge] = range.minimum;
        maximum[edge] = range.maximum;
        targets[edge] = range.target;
        edge++;
      }
    }
    offsets[states.size()] = edge;
    return new DfaData(0, offsets, minimum, maximum, targets, accepting);
  }

  private static BitSet epsilonClosure(Nfa nfa, BitSet seed) {
    BitSet closure = (BitSet) seed.clone();
    ArrayDeque<Integer> pending = new ArrayDeque<>();
    for (int state = seed.nextSetBit(0); state >= 0; state = seed.nextSetBit(state + 1)) {
      pending.push(state);
    }
    while (!pending.isEmpty()) {
      int state = pending.pop();
      for (int target : nfa.states.get(state).epsilon) {
        if (!closure.get(target)) {
          closure.set(target);
          pending.push(target);
        }
      }
    }
    return closure;
  }

  private static boolean containsAccepting(Nfa nfa, BitSet closure) {
    for (int state = closure.nextSetBit(0); state >= 0; state = closure.nextSetBit(state + 1)) {
      if (nfa.states.get(state).accepting) return true;
    }
    return false;
  }

  private static boolean[] computeCanReachAccept(
      int[] offsets, int[] targets, boolean[] accepting) {
    ArrayList<ArrayList<Integer>> incoming = new ArrayList<>(accepting.length);
    for (int i = 0; i < accepting.length; i++) incoming.add(new ArrayList<>());
    for (int state = 0; state < accepting.length; state++) {
      for (int edge = offsets[state]; edge < offsets[state + 1]; edge++) {
        incoming.get(targets[edge]).add(state);
      }
    }

    boolean[] reachable = accepting.clone();
    ArrayDeque<Integer> pending = new ArrayDeque<>();
    for (int state = 0; state < accepting.length; state++) {
      if (accepting[state]) pending.add(state);
    }
    while (!pending.isEmpty()) {
      int state = pending.removeFirst();
      for (int source : incoming.get(state)) {
        if (!reachable[source]) {
          reachable[source] = true;
          pending.addLast(source);
        }
      }
    }
    return reachable;
  }

  private static boolean hasTerminalUnescapedDollar(String pattern, int from) {
    if (pattern.length() <= from || pattern.charAt(pattern.length() - 1) != '$') return false;
    int slashes = 0;
    for (int index = pattern.length() - 2; index >= from && pattern.charAt(index) == '\\'; index--) {
      slashes++;
    }
    return (slashes & 1) == 0;
  }

  private static final class Parser {
    private final String source;
    private final Nfa nfa;
    private int index;

    Parser(String source, Nfa nfa) {
      this.source = source;
      this.nfa = nfa;
    }

    Fragment parse() {
      Fragment result = parseAlternation();
      if (index != source.length()) {
        throw syntax("unexpected token '" + source.charAt(index) + "'");
      }
      return result;
    }

    private Fragment parseAlternation() {
      Fragment result = parseConcatenation();
      while (peek('|')) {
        index++;
        result = alternate(result, parseConcatenation());
      }
      return result;
    }

    private Fragment parseConcatenation() {
      Fragment result = null;
      while (index < source.length() && source.charAt(index) != ')' && source.charAt(index) != '|') {
        Fragment next = parseRepeat();
        result = result == null ? next : concatenate(result, next);
      }
      return result == null ? epsilon() : result;
    }

    private Fragment parseRepeat() {
      Fragment value = parseAtom();
      if (index >= source.length()) return value;
      char token = source.charAt(index);
      if (token == '*' || token == '+' || token == '?') {
        index++;
        value = switch (token) {
          case '*' -> star(value);
          case '+' -> plus(value);
          case '?' -> optional(value);
          default -> throw new AssertionError(token);
        };
        if (index < source.length()) {
          char next = source.charAt(index);
          if (next == '*' || next == '+' || next == '?') {
            throw syntax("stacked quantifier");
          }
        }
      }
      return value;
    }

    private Fragment parseAtom() {
      if (index >= source.length()) return epsilon();
      char token = source.charAt(index++);
      return switch (token) {
        case '(' -> {
          Fragment inside = parseAlternation();
          if (index >= source.length() || source.charAt(index++) != ')') {
            throw syntax("unclosed group");
          }
          yield inside;
        }
        case '.' -> ranges(List.of(new CharRange((char) 0, Character.MAX_VALUE)));
        case '[' -> ranges(parseClass());
        case '\\' -> ranges(parseEscape(false));
        case ')', '|', '*', '+', '?' -> throw syntax("unexpected metacharacter '" + token + "'");
        case '{', '}' -> throw syntax("counted quantifiers are not in the bounded precompute subset");
        default -> literal(token);
      };
    }

    private List<CharRange> parseClass() {
      boolean negate = index < source.length() && source.charAt(index) == '^';
      if (negate) index++;

      ArrayList<CharRange> ranges = new ArrayList<>();
      boolean first = true;
      while (index < source.length()) {
        if (source.charAt(index) == ']' && !first) {
          index++;
          return negate ? complement(normalize(ranges)) : normalize(ranges);
        }
        first = false;

        List<CharRange> left =
            source.charAt(index) == '\\'
                ? consumeEscapeInClass()
                : List.of(new CharRange(source.charAt(index++), source.charAt(index - 1)));

        if (left.size() == 1
            && index < source.length()
            && source.charAt(index) == '-'
            && index + 1 < source.length()
            && source.charAt(index + 1) != ']') {
          index++;
          List<CharRange> right =
              source.charAt(index) == '\\'
                  ? consumeEscapeInClass()
                  : List.of(new CharRange(source.charAt(index++), source.charAt(index - 1)));
          if (right.size() != 1
              || left.get(0).minimum != left.get(0).maximum
              || right.get(0).minimum != right.get(0).maximum) {
            throw syntax("range endpoints must be single UTF-16 units");
          }
          char minimum = left.get(0).minimum;
          char maximum = right.get(0).minimum;
          if (minimum > maximum) throw syntax("descending character range");
          ranges.add(new CharRange(minimum, maximum));
        } else {
          ranges.addAll(left);
        }
      }
      throw syntax("unclosed character class");
    }

    private List<CharRange> consumeEscapeInClass() {
      index++;
      return parseEscape(true);
    }

    private List<CharRange> parseEscape(boolean inClass) {
      if (index >= source.length()) throw syntax("dangling escape");
      char escape = source.charAt(index++);
      return switch (escape) {
        case 'd' -> List.of(new CharRange('0', '9'));
        case 'w' -> List.of(
            new CharRange('0', '9'),
            new CharRange('A', 'Z'),
            new CharRange('_', '_'),
            new CharRange('a', 'z'));
        case 's' -> List.of(
            new CharRange('\t', '\t'),
            new CharRange('\n', '\n'),
            new CharRange('\f', '\f'),
            new CharRange('\r', '\r'),
            new CharRange(' ', ' '));
        case 't' -> List.of(new CharRange('\t', '\t'));
        case 'n' -> List.of(new CharRange('\n', '\n'));
        case 'r' -> List.of(new CharRange('\r', '\r'));
        case 'f' -> List.of(new CharRange('\f', '\f'));
        case 'u' -> {
          char value = parseUnicode();
          yield List.of(new CharRange(value, value));
        }
        case 'b' -> {
          if (!inClass) throw syntax("word-boundary escape is not a finite character transition");
          yield List.of(new CharRange('\b', '\b'));
        }
        case '1', '2', '3', '4', '5', '6', '7', '8', '9' ->
            throw syntax("backreferences are not supported");
        default -> List.of(new CharRange(escape, escape));
      };
    }

    private char parseUnicode() {
      if (index + 4 > source.length()) throw syntax("short unicode escape");
      int value = 0;
      for (int i = 0; i < 4; i++) {
        int digit = Character.digit(source.charAt(index++), 16);
        if (digit < 0) throw syntax("invalid unicode escape");
        value = (value << 4) | digit;
      }
      return (char) value;
    }

    private Fragment literal(char unit) {
      return ranges(List.of(new CharRange(unit, unit)));
    }

    private Fragment ranges(List<CharRange> ranges) {
      int start = nfa.newState();
      int end = nfa.newState();
      for (CharRange range : normalize(ranges)) {
        nfa.states.get(start).ranges.add(new RangeEdge(range.minimum, range.maximum, end));
      }
      return new Fragment(start, end);
    }

    private Fragment epsilon() {
      int start = nfa.newState();
      int end = nfa.newState();
      nfa.states.get(start).epsilon.add(end);
      return new Fragment(start, end);
    }

    private Fragment concatenate(Fragment left, Fragment right) {
      nfa.states.get(left.end).epsilon.add(right.start);
      return new Fragment(left.start, right.end);
    }

    private Fragment alternate(Fragment left, Fragment right) {
      int start = nfa.newState();
      int end = nfa.newState();
      nfa.states.get(start).epsilon.add(left.start);
      nfa.states.get(start).epsilon.add(right.start);
      nfa.states.get(left.end).epsilon.add(end);
      nfa.states.get(right.end).epsilon.add(end);
      return new Fragment(start, end);
    }

    private Fragment star(Fragment value) {
      int start = nfa.newState();
      int end = nfa.newState();
      nfa.states.get(start).epsilon.add(value.start);
      nfa.states.get(start).epsilon.add(end);
      nfa.states.get(value.end).epsilon.add(value.start);
      nfa.states.get(value.end).epsilon.add(end);
      return new Fragment(start, end);
    }

    private Fragment plus(Fragment value) {
      int start = nfa.newState();
      int end = nfa.newState();
      nfa.states.get(start).epsilon.add(value.start);
      nfa.states.get(value.end).epsilon.add(value.start);
      nfa.states.get(value.end).epsilon.add(end);
      return new Fragment(start, end);
    }

    private Fragment optional(Fragment value) {
      int start = nfa.newState();
      int end = nfa.newState();
      nfa.states.get(start).epsilon.add(value.start);
      nfa.states.get(start).epsilon.add(end);
      nfa.states.get(value.end).epsilon.add(end);
      return new Fragment(start, end);
    }

    private boolean peek(char value) {
      return index < source.length() && source.charAt(index) == value;
    }

    private IllegalArgumentException syntax(String message) {
      return new IllegalArgumentException(
          "regex syntax at UTF-16 index " + index + ": " + message + " in " + source);
    }
  }

  private static List<CharRange> normalize(List<CharRange> source) {
    ArrayList<CharRange> sorted = new ArrayList<>(source);
    sorted.sort((left, right) -> {
      int compared = Character.compare(left.minimum, right.minimum);
      return compared != 0 ? compared : Character.compare(left.maximum, right.maximum);
    });
    ArrayList<CharRange> result = new ArrayList<>();
    for (CharRange range : sorted) {
      if (result.isEmpty()) {
        result.add(range);
        continue;
      }
      CharRange previous = result.get(result.size() - 1);
      if ((int) range.minimum <= (int) previous.maximum + 1) {
        result.set(
            result.size() - 1,
            new CharRange(previous.minimum, (char) Math.max(previous.maximum, range.maximum)));
      } else {
        result.add(range);
      }
    }
    return List.copyOf(result);
  }

  private static List<CharRange> complement(List<CharRange> normalized) {
    ArrayList<CharRange> result = new ArrayList<>();
    int cursor = 0;
    for (CharRange range : normalized) {
      if (cursor < range.minimum) result.add(new CharRange((char) cursor, (char) (range.minimum - 1)));
      cursor = range.maximum == Character.MAX_VALUE ? 65536 : range.maximum + 1;
    }
    if (cursor <= Character.MAX_VALUE) {
      result.add(new CharRange((char) cursor, Character.MAX_VALUE));
    }
    return List.copyOf(result);
  }

  private static final class Nfa {
    final ArrayList<NfaState> states = new ArrayList<>();

    int newState() {
      if (states.size() >= MAX_NFA_STATES) {
        throw new IllegalArgumentException("regex NFA state bound exceeded");
      }
      states.add(new NfaState());
      return states.size() - 1;
    }
  }

  private static final class NfaState {
    final ArrayList<Integer> epsilon = new ArrayList<>();
    final ArrayList<RangeEdge> ranges = new ArrayList<>();
    boolean accepting;
  }

  private static final class DfaBuildState {
    final ArrayList<DfaRange> ranges = new ArrayList<>();
    boolean accepting;

    void addRange(char minimum, char maximum, int target) {
      if (!ranges.isEmpty()) {
        DfaRange previous = ranges.get(ranges.size() - 1);
        if (previous.target == target
            && previous.maximum != Character.MAX_VALUE
            && previous.maximum + 1 == minimum) {
          ranges.set(
              ranges.size() - 1,
              new DfaRange(previous.minimum, maximum, target));
          return;
        }
      }
      ranges.add(new DfaRange(minimum, maximum, target));
    }
  }

  private record Fragment(int start, int end) {}
  private record CharRange(char minimum, char maximum) {}
  private record RangeEdge(char minimum, char maximum, int target) {}
  private record DfaRange(char minimum, char maximum, int target) {}

  private record DfaData(
      int startState,
      int[] offsets,
      char[] minimum,
      char[] maximum,
      int[] targets,
      boolean[] accepting) {}
}
