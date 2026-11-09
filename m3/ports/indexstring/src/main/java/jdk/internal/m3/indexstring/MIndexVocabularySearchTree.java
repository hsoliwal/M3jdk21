// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Frozen forward/reverse lexical geometry for one immutable precomputed vocabulary.
 *
 * <p>This is intentionally distinct from an all-suffix search tree, which indexes all suffixes
 * inside one string. Here each row is one complete vocabulary spelling. Prefix subtrees are
 * contiguous forward-rank intervals and suffix subtrees are contiguous reverse-rank intervals, so
 * no per-node Java trie objects are retained.</p>
 */
public final class MIndexVocabularySearchTree {
  private final FrozenChars[] spellings;
  private final int[] forwardRows;
  private final int[] reverseRows;
  private final int[] forwardRanks;
  private final int[] reverseRanks;
  private volatile PreparedTries preparedTries;
  private boolean preparingTries;

  private MIndexVocabularySearchTree(FrozenChars[] spellings) {
    this.spellings = spellings;
    forwardRows = ordered(false);
    reverseRows = ordered(true);
    forwardRanks = ranks(forwardRows);
    reverseRanks = ranks(reverseRows);
  }

  /**
   * Compiles the vocabulary tree over spellings. Rows are distinct spellings in first-occurrence order;
   * each row is frozen once (UTF-16 units, isolated surrogates kept) and never re-read from the caller.
   */
  public static MIndexVocabularySearchTree compile(Collection<? extends CharSequence> vocabulary) {
    Objects.requireNonNull(vocabulary, "vocabulary");
    Map<String, Integer> positions = new LinkedHashMap<>();
    for (CharSequence value : vocabulary) {
      Objects.requireNonNull(value, "vocabulary element");
      positions.putIfAbsent(value.toString(), positions.size());
    }
    FrozenChars[] frozen = new FrozenChars[positions.size()];
    int row = 0;
    for (String spelling : positions.keySet()) frozen[row++] = FrozenChars.copyOf(spelling.toCharArray());
    return new MIndexVocabularySearchTree(frozen);
  }

  /** The frozen spelling of one row. */
  public FrozenChars valueAt(int row) {
    return spellings[Objects.checkIndex(row, spellings.length)];
  }

  /** Existing ranked-array behavior remains available until this explicit preparation is requested. */
  public PreparedTries precomputeTries() {
    return precomputeTries(MIndexTrieNavigation.Limits.DEFAULT, null);
  }

  /**
   * Compile forward and reverse tries once, using the existing literal-tree owner. No new interner
   * is created: payloads are this snapshot's rows, including previously admitted local values.
   * Publication is atomic; failure leaves the ranked-array fallback intact.
   */
  public synchronized PreparedTries precomputeTries(
      MIndexTrieNavigation.Limits limits, ProgressMonitor monitor) {
    Objects.requireNonNull(limits, "limits");
    MIndexTrieNavigation.check(monitor);
    if (preparedTries != null) {
      preparedTries.forwardTree.navigation(limits, monitor);
      preparedTries.reverseTree.navigation(limits, monitor);
      return preparedTries;
    }
    if (preparingTries) throw new IllegalStateException("reentrant vocabulary trie preparation");
    preparingTries = true;
    try {
      MIndexLiteralSearchTree forwardTree = compileDirection(false, limits, monitor);
      MIndexLiteralSearchTree reverseTree = compileDirection(true, limits, monitor);
      MIndexTrieNavigation forward = forwardTree.navigation(limits, monitor);
      MIndexTrieNavigation reverse = reverseTree.navigation(limits, monitor);
      if (forward.terminalCount() != size() || reverse.terminalCount() != size()) {
        throw new IllegalStateException("vocabulary rows are not unique spelling identities");
      }
      for (int rank = 0; rank < size(); rank++) {
        if (forward.payloadAtRank(rank) != forwardRows[rank]
            || reverse.payloadAtRank(rank) != reverseRows[rank]) {
          throw new IllegalStateException("prepared trie disagrees with retained lexical ranks");
        }
        if ((rank & 1023) == 0) MIndexTrieNavigation.check(monitor);
      }
      PreparedTries result = new PreparedTries(forwardTree, reverseTree, forward, reverse);
      MIndexTrieNavigation.check(monitor);
      preparedTries = result;
      return result;
    } finally {
      preparingTries = false;
    }
  }

  private MIndexLiteralSearchTree compileDirection(boolean reverse,
      MIndexTrieNavigation.Limits limits, ProgressMonitor monitor) {
    MIndexLiteralSearchTree.Builder builder = MIndexLiteralSearchTree.builder(limits.maxStates());
    for (int row = 0; row < size(); row++) {
      MIndexTrieNavigation.check(monitor);
      FrozenChars text = chars(row);
      builder.add(reverse ? new ReverseUnits(text, 0, text.length()) : text, row, monitor);
    }
    return builder.freeze(monitor);
  }

  /** Evaluate a pure compiled machine during preparation; runtime answers are rank-indexed bits. */
  public MIndexTrieNavigation.PreparedMatches precomputeMatches(
      MIndexTrieNavigation.Utf16Machine machine, MIndexTrieNavigation.WorkLimits limits,
      ProgressMonitor monitor) {
    return requirePrepared().forward.precompute(machine, limits, monitor);
  }

  public boolean hasPrecomputedTries() { return preparedTries != null; }

  public static final class PreparedTries {
    private final MIndexLiteralSearchTree forwardTree;
    private final MIndexLiteralSearchTree reverseTree;
    private final MIndexTrieNavigation forward;
    private final MIndexTrieNavigation reverse;
    private PreparedTries(MIndexLiteralSearchTree forwardTree, MIndexLiteralSearchTree reverseTree,
        MIndexTrieNavigation forward, MIndexTrieNavigation reverse) {
      this.forwardTree = forwardTree;
      this.reverseTree = reverseTree;
      this.forward = forward;
      this.reverse = reverse;
    }
    public MIndexTrieNavigation forward() { return forward; }
    public MIndexTrieNavigation reverse() { return reverse; }
    /** Both source tries plus their shared-array navigation metadata, counted once. */
    public long primitivePayloadBytes() {
      return Math.addExact(forwardTree.primitivePayloadBytes(), reverseTree.primitivePayloadBytes());
    }
  }

  /** Build-only reversed UTF-16 view; it never allocates a reversed spelling for the builder. */
  private record ReverseUnits(CharSequence source, int begin, int end) implements CharSequence {
    private ReverseUnits {
      Objects.requireNonNull(source, "source");
      Objects.checkFromToIndex(begin, end, source.length());
    }
    @Override public int length() { return end - begin; }
    @Override public char charAt(int index) {
      return source.charAt(end - 1 - Objects.checkIndex(index, length()));
    }
    @Override public CharSequence subSequence(int start, int stop) {
      Objects.checkFromToIndex(start, stop, length());
      return new ReverseUnits(source, end - stop, end - start);
    }
    @Override public String toString() {
      char[] chars = new char[length()];
      for (int i = 0; i < chars.length; i++) chars[i] = charAt(i);
      return new String(chars);
    }
  }

  public int size() {
    return forwardRows.length;
  }

  public long primitivePayloadBytes() {
    long ranks = 4L * Integer.BYTES * size();
    PreparedTries prepared = preparedTries;
    return prepared == null ? ranks : Math.addExact(ranks, prepared.primitivePayloadBytes());
  }

  public int rowAtLexicalRank(int rank) {
    return forwardRows[Objects.checkIndex(rank, size())];
  }

  public int rowAtReverseRank(int rank) {
    return reverseRows[Objects.checkIndex(rank, size())];
  }

  public int lexicalRankOf(int row) {
    return forwardRanks[Objects.checkIndex(row, size())];
  }

  public int reverseRankOf(int row) {
    return reverseRanks[Objects.checkIndex(row, size())];
  }

  /** Exact lookup: ranked comparisons before preparation, trie transitions after preparation. */
  public OptionalInt exactRow(CharSequence text) {
    Objects.requireNonNull(text, "text");
    PreparedTries prepared = preparedTries;
    if (prepared != null) {
      int state = prepared.forward.stateFor(text);
      return optionalPayload(prepared.forward, state);
    }
    int low = 0;
    int high = size() - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int row = forwardRows[mid];
      int cmp = compare(chars(row), text, false);
      if (cmp < 0) low = mid + 1;
      else if (cmp > 0) high = mid - 1;
      else return OptionalInt.of(row);
    }
    return OptionalInt.empty();
  }

  /** Rows beginning with prefix, in lexical rank order; prepared queries read no vocabulary spelling. */
  public int[] prefixRows(CharSequence prefix) {
    Objects.requireNonNull(prefix, "prefix");
    PreparedTries prepared = preparedTries;
    if (prepared != null) {
      int state = prepared.forward.stateFor(prefix);
      return allPayloads(prepared.forward, state);
    }
    int start = lowerBound(prefix, false);
    int[] scratch = new int[size() - start];
    int count = 0;
    for (int rank = start; rank < size(); rank++) {
      int row = forwardRows[rank];
      if (!startsWith(chars(row), prefix)) break;
      scratch[count++] = row;
    }
    return Arrays.copyOf(scratch, count);
  }

  /** Rows ending with suffix, in reverse-lexical rank order; prepared queries read no vocabulary spelling. */
  public int[] suffixRows(CharSequence suffix) {
    Objects.requireNonNull(suffix, "suffix");
    PreparedTries prepared = preparedTries;
    if (prepared != null) {
      int state = prepared.reverse.stateFor(suffix, 0, suffix.length(),
          MIndexTrieNavigation.Direction.REVERSE, null);
      return allPayloads(prepared.reverse, state);
    }
    int start = lowerBound(suffix, true);
    int[] scratch = new int[size() - start];
    int count = 0;
    for (int rank = start; rank < size(); rank++) {
      int row = reverseRows[rank];
      if (!endsWith(chars(row), suffix)) break;
      scratch[count++] = row;
    }
    return Arrays.copyOf(scratch, count);
  }

  /** Longest complete vocabulary spelling that is a prefix of input. */
  public OptionalInt longestPrefixRow(CharSequence input) {
    Objects.requireNonNull(input, "input");
    PreparedTries prepared = preparedTries;
    if (prepared != null) {
      int payload = prepared.forward.longestPrefixPayload(input, 0, input.length(),
          MIndexTrieNavigation.Direction.FORWARD, null);
      return payload < 0 ? OptionalInt.empty() : OptionalInt.of(payload);
    }
    for (int length = input.length(); length >= 0; length--) {
      OptionalInt row = exactSlice(input, 0, length);
      if (row.isPresent()) return row;
    }
    return OptionalInt.empty();
  }

  /** Longest complete vocabulary spelling that is a suffix of input. */
  public OptionalInt longestSuffixRow(CharSequence input) {
    Objects.requireNonNull(input, "input");
    PreparedTries prepared = preparedTries;
    if (prepared != null) {
      int payload = prepared.reverse.longestPrefixPayload(input, 0, input.length(),
          MIndexTrieNavigation.Direction.REVERSE, null);
      return payload < 0 ? OptionalInt.empty() : OptionalInt.of(payload);
    }
    for (int length = input.length(); length >= 0; length--) {
      OptionalInt row = exactSlice(input, input.length() - length, length);
      if (row.isPresent()) return row;
    }
    return OptionalInt.empty();
  }

  OptionalInt exactPrefixRow(CharSequence input, int length) {
    Objects.requireNonNull(input, "input");
    Objects.checkFromIndexSize(0, length, input.length());
    return exactSlice(input, 0, length);
  }

  OptionalInt exactSuffixRow(CharSequence input, int length) {
    Objects.requireNonNull(input, "input");
    Objects.checkFromIndexSize(0, length, input.length());
    return exactSlice(input, input.length() - length, length);
  }

  private OptionalInt exactSlice(CharSequence input, int offset, int length) {
    PreparedTries prepared = preparedTries;
    if (prepared != null) {
      int state = prepared.forward.stateFor(input, offset, Math.addExact(offset, length),
          MIndexTrieNavigation.Direction.FORWARD, null);
      return optionalPayload(prepared.forward, state);
    }
    int low = 0;
    int high = size() - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int row = forwardRows[mid];
      int cmp = compareSlice(chars(row), input, offset, length);
      if (cmp < 0) low = mid + 1;
      else if (cmp > 0) high = mid - 1;
      else return OptionalInt.of(row);
    }
    return OptionalInt.empty();
  }

  /** Range coordinates agree with the retained forward ranks. Explicit preparation is required. */
  public MIndexTrieNavigation.RankRange prefixRange(CharSequence prefix, ProgressMonitor monitor) {
    Objects.requireNonNull(prefix, "prefix");
    MIndexTrieNavigation navigation = requirePrepared().forward;
    int state = navigation.stateFor(prefix, 0, prefix.length(), MIndexTrieNavigation.Direction.FORWARD, monitor);
    return state == MIndexTrieNavigation.DEAD ? new MIndexTrieNavigation.RankRange(0, 0) : navigation.range(state);
  }

  /** Range coordinates agree with the retained reverse ranks; units, not code points, are reversed. */
  public MIndexTrieNavigation.RankRange suffixRange(CharSequence suffix, ProgressMonitor monitor) {
    Objects.requireNonNull(suffix, "suffix");
    MIndexTrieNavigation navigation = requirePrepared().reverse;
    int state = navigation.stateFor(suffix, 0, suffix.length(), MIndexTrieNavigation.Direction.REVERSE, monitor);
    return state == MIndexTrieNavigation.DEAD ? new MIndexTrieNavigation.RankRange(0, 0) : navigation.range(state);
  }

  /**
   * Exact prefix/suffix/length intersection, in forward lexical order. Iterates the smaller
   * population using inverse ranks and prepared lengths; never re-tests vocabulary spellings.
   * For the single-star expression prefix*suffix, use a minimum length of prefix.length()+suffix.length()
   * to prohibit overlap. This method itself deliberately permits overlapping affix constraints.
   */
  public int[] affixRows(CharSequence prefix, CharSequence suffix, int minimumLength, int maximumLength,
      ProgressMonitor monitor) {
    if (minimumLength < 0 || maximumLength < minimumLength) throw new IllegalArgumentException("invalid length bounds");
    MIndexTrieNavigation.RankRange left = prefixRange(prefix, monitor);
    MIndexTrieNavigation.RankRange right = suffixRange(suffix, monitor);
    MIndexTrieNavigation navigation = requirePrepared().forward;
    boolean scanForward = left.size() <= right.size();
    MIndexTrieNavigation.RankRange scan = scanForward ? left : right;
    MIndexTrieNavigation.RankRange other = scanForward ? right : left;
    int[] order = scanForward ? forwardRows : reverseRows;
    int[] inverse = scanForward ? reverseRanks : forwardRanks;
    int[] ranks = new int[scan.size()];
    int count = 0;
    for (int rank = scan.fromInclusive(); rank < scan.toExclusive(); rank++) {
      int row = order[rank];
      int otherRank = inverse[row];
      if (otherRank >= other.fromInclusive() && otherRank < other.toExclusive()) {
        int length = navigation.depth(navigation.stateAtRank(forwardRanks[row]));
        if (length >= minimumLength && length <= maximumLength) ranks[count++] = forwardRanks[row];
      }
      if ((rank & 1023) == 0) MIndexTrieNavigation.check(monitor);
    }
    Arrays.sort(ranks, 0, count);
    int[] result = new int[count];
    for (int i = 0; i < count; i++) {
      result[i] = forwardRows[ranks[i]];
      if ((i & 1023) == 0) MIndexTrieNavigation.check(monitor);
    }
    MIndexTrieNavigation.check(monitor);
    return result;
  }

  private PreparedTries requirePrepared() {
    PreparedTries result = preparedTries;
    if (result == null) throw new IllegalStateException("call precomputeTries before indexed range queries");
    return result;
  }

  private static OptionalInt optionalPayload(MIndexTrieNavigation navigation, int state) {
    int row = state == MIndexTrieNavigation.DEAD ? -1 : navigation.terminalPayload(state);
    return row < 0 ? OptionalInt.empty() : OptionalInt.of(row);
  }

  private static int[] allPayloads(MIndexTrieNavigation navigation, int state) {
    return state == MIndexTrieNavigation.DEAD ? new int[0]
        : navigation.payloads(state, 0, navigation.descendantCount(state), null);
  }

  private int lowerBound(CharSequence key, boolean reversed) {
    int[] order = reversed ? reverseRows : forwardRows;
    int low = 0;
    int high = order.length;
    while (low < high) {
      int mid = (low + high) >>> 1;
      if (prefixCompare(chars(order[mid]), key, reversed) < 0) low = mid + 1;
      else high = mid;
    }
    return low;
  }

  private int[] ordered(boolean reversed) {
    int[] rows = new int[spellings.length];
    int[] scratch = new int[rows.length];
    for (int row = 0; row < rows.length; row++) rows[row] = row;
    mergeSort(rows, scratch, reversed);
    return rows;
  }

  private void mergeSort(int[] rows, int[] scratch, boolean reversed) {
    for (int width = 1; width < rows.length; width <<= 1) {
      for (int start = 0; start < rows.length; start += width << 1) {
        int middle = Math.min(start + width, rows.length);
        int end = Math.min(start + (width << 1), rows.length);
        int left = start;
        int right = middle;
        int out = start;
        while (left < middle && right < end) {
          int cmp = compare(chars(rows[left]), chars(rows[right]), reversed);
          scratch[out++] = cmp <= 0 ? rows[left++] : rows[right++];
        }
        while (left < middle) scratch[out++] = rows[left++];
        while (right < end) scratch[out++] = rows[right++];
      }
      System.arraycopy(scratch, 0, rows, 0, rows.length);
      if (width > rows.length / 2) break;
    }
  }

  private static int[] ranks(int[] order) {
    int[] result = new int[order.length];
    for (int rank = 0; rank < order.length; rank++) result[order[rank]] = rank;
    return result;
  }

  private FrozenChars chars(int row) {
    return spellings[row];
  }

  private static boolean startsWith(CharSequence text, CharSequence prefix) {
    return prefix.length() <= text.length()
        && regionEquals(text, 0, prefix, 0, prefix.length());
  }

  private static boolean endsWith(CharSequence text, CharSequence suffix) {
    return suffix.length() <= text.length()
        && regionEquals(
            text, text.length() - suffix.length(), suffix, 0, suffix.length());
  }

  private static int compareSlice(CharSequence left, CharSequence right, int offset, int length) {
    int common = Math.min(left.length(), length);
    for (int i = 0; i < common; i++) {
      int delta = left.charAt(i) - right.charAt(offset + i);
      if (delta != 0) return delta;
    }
    return Integer.compare(left.length(), length);
  }

  private static int compare(CharSequence left, CharSequence right, boolean reversed) {
    int cmp = prefixCompare(left, right, reversed);
    return cmp != 0 ? cmp : Integer.compare(left.length(), right.length());
  }

  private static int prefixCompare(CharSequence left, CharSequence right, boolean reversed) {
    int common = Math.min(left.length(), right.length());
    for (int i = 0; i < common; i++) {
      char a = left.charAt(reversed ? left.length() - 1 - i : i);
      char b = right.charAt(reversed ? right.length() - 1 - i : i);
      int delta = a - b;
      if (delta != 0) return delta;
    }
    return left.length() < right.length() ? -1 : 0;
  }
  /** UTF-16 unit equality of two ranges (the donor's packed four-unit form gives the same answer). */
  private static boolean regionEquals(
      CharSequence left, int leftOffset, CharSequence right, int rightOffset, int length) {
    for (int i = 0; i < length; i++) {
      if (left.charAt(leftOffset + i) != right.charAt(rightOffset + i)) return false;
    }
    return true;
  }
}
