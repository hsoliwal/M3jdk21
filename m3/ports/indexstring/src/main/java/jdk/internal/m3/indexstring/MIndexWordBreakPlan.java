// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Frozen trie-guided dictionary segmentation for arbitrary UTF-16 CharSequence values (a String included).
 *
 * <p>The vocabulary trie is prepared once by the existing
 * {@link MIndexVocabularySearchTree} owner. Each query walks only reachable
 * positions and stores primitive boundary/row coordinates. No substring String, second dictionary
 * or canonical spelling is created. Inputs must remain stable for the duration of a query; result
 * views retain the source's existing ownership and lifetime contract.</p>
 *
 * <p>{@link #segment(CharSequence)} preserves the historical first-predecessor result.
 * {@link #minimumSegments(CharSequence)} explicitly selects a shortest path through the same
 * reachable-position DAG. Both interpret positions as UTF-16 units, including isolated surrogates.
 * Neither operation applies normalization, case folding or an implicit literal fallback.</p>
 */
public final class MIndexWordBreakPlan {
  /**
   * Per-query bounds, excluding the existing vocabulary/trie and array/object headers.
   *
   * <p>Workspace admission conservatively includes the DP arrays and the maximum primitive result
   * arrays retained simultaneously during reconstruction. It is checked before position-array
   * allocation. Every attempted trie edge, including a dead transition, consumes one transition.
   * Exhaustion throws; it never returns an incomplete or unproved segmentation.</p>
   */
  public record Limits(long maxWorkspaceBytes, long maxTransitions) {
    public static final Limits DEFAULT = new Limits(64L << 20, 100_000_000L);

    public Limits {
      if (maxWorkspaceBytes < 0L || maxTransitions < 0L) {
        throw new IllegalArgumentException("negative word-break limit");
      }
    }
  }

  // Existing overloads do not acquire a new arbitrary refusal threshold.
  private static final Limits LEGACY_LIMITS = new Limits(Long.MAX_VALUE, Long.MAX_VALUE);

  public static final class Result {
    private final CharSequence source;
    private final MIndexVocabularySearchTree vocabulary;
    private final int[] boundaries;
    private final int[] vocabularyRows;

    private Result(
        CharSequence source,
        MIndexVocabularySearchTree vocabulary,
        int[] boundaries,
        int[] vocabularyRows) {
      this.source = source;
      this.vocabulary = vocabulary;
      this.boundaries = boundaries;
      this.vocabularyRows = vocabularyRows;
    }

    public boolean segmentable() { return boundaries.length != 0; }
    public int segmentCount() { return vocabularyRows.length; }
    public int[] boundaries() { return boundaries.clone(); }
    public int[] vocabularyRows() { return vocabularyRows.clone(); }

    public CharSequence segmentView(int segment) {
      Objects.checkIndex(segment, segmentCount());
      return source.subSequence(boundaries[segment], boundaries[segment + 1]);
    }

    public CharSequence vocabularyValue(int segment) {
      return vocabulary.valueAt(vocabularyRows[Objects.checkIndex(segment, segmentCount())]);
    }

    public List<CharSequence> segmentViews() {
      ArrayList<CharSequence> result = new ArrayList<>(segmentCount());
      for (int index = 0; index < segmentCount(); index++) result.add(segmentView(index));
      return List.copyOf(result);
    }

    @Override
    public String toString() {
      return segmentable()
          ? "MIndexWordBreakResult[segments=" + segmentCount() + "]"
          : "MIndexWordBreakResult[unsegmentable]";
    }
  }

  private final MIndexVocabularySearchTree vocabulary;
  private final MIndexTrieNavigation trie;
  private final int maximumWordLength;

  private MIndexWordBreakPlan(
      MIndexVocabularySearchTree vocabulary,
      MIndexTrieNavigation trie,
      int maximumWordLength) {
    this.vocabulary = vocabulary;
    this.trie = trie;
    this.maximumWordLength = maximumWordLength;
  }

  public static MIndexWordBreakPlan compile(MIndexVocabularySearchTree vocabulary) {
    return compile(vocabulary, new DefaultProgressMonitor());
  }

  public static MIndexWordBreakPlan compile(
      MIndexVocabularySearchTree vocabulary, ProgressMonitor monitor) {
    MIndexVocabularySearchTree words = Objects.requireNonNull(vocabulary, "vocabulary");
    ProgressMonitor progress = monitor == null ? new DefaultProgressMonitor() : monitor;
    progress.beginTask("compile MIndex word-break plan", words.size());
    try {
      MIndexTrieNavigation.check(progress);
      MIndexTrieNavigation trie =
          words
              .precomputeTries(MIndexTrieNavigation.Limits.DEFAULT, progress)
              .forward();
      int maximum = trie.maximumLength(MIndexTrieNavigation.ROOT);
      MIndexTrieNavigation.check(progress);
      return new MIndexWordBreakPlan(words, trie, Math.max(0, maximum));
    } finally {
      progress.done();
    }
  }

  public MIndexVocabularySearchTree vocabulary() { return vocabulary; }
  public int maximumWordLength() { return maximumWordLength; }

  public boolean canSegment(CharSequence input) {
    return segment(input).segmentable();
  }

  public Result segment(CharSequence input) {
    return segment(input, new DefaultProgressMonitor());
  }

  /**
   * Returns the historical deterministic segmentation.
   *
   * <p>Starts and their terminal ends are visited in increasing order. The first predecessor
   * retained for each position wins, and discovering the final position finishes this feasibility
   * query immediately. This result need not contain the fewest segments. Empty dictionary words
   * never create transitions.</p>
   */
  public Result segment(CharSequence input, ProgressMonitor monitor) {
    return evaluate(input, LEGACY_LIMITS, monitor, false);
  }

  /** Historical segmentation policy with explicit per-query resource bounds. */
  public Result segment(CharSequence input, Limits limits, ProgressMonitor monitor) {
    return evaluate(input, limits, monitor, false);
  }

  /** Minimum dictionary-segment count under the default explicit workspace/transition bounds. */
  public Result minimumSegments(CharSequence input) {
    return minimumSegments(input, Limits.DEFAULT, null);
  }

  /**
   * Selects a shortest path through reachable UTF-16 positions.
   *
   * <p>A position's best count is final when that start is visited, because every word edge
   * advances to a greater position. A candidate replaces a predecessor only when its count is
   * strictly smaller. Equal-cost ties therefore keep the first candidate discovered in increasing
   * start/end order, independently of vocabulary insertion order. This method does not return at
   * the first complete path; later reachable starts may produce a shorter path.</p>
   */
  public Result minimumSegments(CharSequence input, Limits limits, ProgressMonitor monitor) {
    return evaluate(input, limits, monitor, true);
  }

  private Result evaluate(
      CharSequence input, Limits limits, ProgressMonitor monitor, boolean minimum) {
    CharSequence source = Objects.requireNonNull(input, "input");
    Objects.requireNonNull(limits, "limits");
    ProgressMonitor progress = monitor == null ? new DefaultProgressMonitor() : monitor;
    MIndexTrieNavigation.check(progress);
    int length = source.length();
    MIndexTrieNavigation.check(progress);
    if (length == 0) {
      if (limits.maxWorkspaceBytes() < Integer.BYTES) {
        throw new IllegalArgumentException("word-break workspace budget exceeded");
      }
      return new Result(source, vocabulary, new int[] {0}, new int[0]);
    }
    if (maximumWordLength <= 0) {
      return new Result(source, vocabulary, new int[0], new int[0]);
    }

    long positions = (long) length + 1L;
    long workspace = Integer.BYTES
        * ((minimum ? 3L : 2L) * positions + 2L * length + 1L);
    if (positions > Integer.MAX_VALUE || workspace > limits.maxWorkspaceBytes()) {
      throw new IllegalArgumentException("word-break workspace budget exceeded");
    }

    progress.beginTask(
        minimum ? "minimize MIndex word-break segments" : "segment CharSequence by vocabulary",
        length);
    try {
      progress.subTask("prepare word-break positions");
      MIndexTrieNavigation.check(progress);
      int[] predecessor = new int[(int) positions];
      int[] wordRow = new int[(int) positions];
      int[] best = minimum ? new int[(int) positions] : null;
      initialize(predecessor, wordRow, best, progress);
      predecessor[0] = 0;
      if (minimum) best[0] = 0;

      long transitions = 0L;
      for (int start = 0; start < length; start++) {
        MIndexTrieNavigation.check(progress);
        if (predecessor[start] >= 0) {
          int state = MIndexTrieNavigation.ROOT;
          int limit = (int) Math.min(length, (long) start + maximumWordLength);
          int candidateCount = minimum ? best[start] + 1 : 0;
          for (int end = start; end < limit; end++) {
            if (transitions == limits.maxTransitions()) {
              throw new IllegalStateException("word-break transition budget exceeded");
            }
            transitions++;
            state = trie.step(state, source.charAt(end), progress);
            if (state == MIndexTrieNavigation.DEAD) break;
            int row = trie.terminalPayload(state);
            int next = end + 1;
            if (row >= 0
                && (predecessor[next] < 0 || (minimum && candidateCount < best[next]))) {
              predecessor[next] = start;
              wordRow[next] = row;
              if (minimum) best[next] = candidateCount;
              if (!minimum && next == length) {
                return reconstruct(source, length, predecessor, wordRow, progress);
              }
            }
          }
        }
        progress.worked(1L);
      }
      MIndexTrieNavigation.check(progress);
      return predecessor[length] < 0
          ? new Result(source, vocabulary, new int[0], new int[0])
          : reconstruct(source, length, predecessor, wordRow, progress);
    } finally {
      progress.done();
    }
  }

  private static void initialize(
      int[] predecessor, int[] wordRow, int[] best, ProgressMonitor monitor) {
    for (int start = 0; start < predecessor.length; ) {
      MIndexTrieNavigation.check(monitor);
      int end = (int) Math.min(predecessor.length, (long) start + 1024L);
      Arrays.fill(predecessor, start, end, -1);
      Arrays.fill(wordRow, start, end, -1);
      if (best != null) Arrays.fill(best, start, end, Integer.MAX_VALUE);
      start = end;
    }
    MIndexTrieNavigation.check(monitor);
  }

  private Result reconstruct(
      CharSequence source, int length, int[] predecessor, int[] wordRow,
      ProgressMonitor monitor) {
    monitor.subTask("reconstruct word-break segments");
    MIndexTrieNavigation.check(monitor);
    int segments = 0;
    for (int at = length; at > 0; at = predecessor[at]) {
      if ((segments & 1023) == 0) MIndexTrieNavigation.check(monitor);
      if (predecessor[at] < 0 || predecessor[at] >= at || wordRow[at] < 0) {
        throw new IllegalStateException("word-break predecessor chain");
      }
      segments++;
    }

    MIndexTrieNavigation.check(monitor);
    int[] boundaries = new int[segments + 1];
    int[] rows = new int[segments];
    int at = length;
    boundaries[segments] = at;
    for (int segment = segments - 1; segment >= 0; segment--) {
      if ((segment & 1023) == 0) MIndexTrieNavigation.check(monitor);
      rows[segment] = wordRow[at];
      at = predecessor[at];
      boundaries[segment] = at;
    }
    if (at != 0) throw new IllegalStateException("word-break did not reach source origin");
    MIndexTrieNavigation.check(monitor);
    return new Result(source, vocabulary, boundaries, rows);
  }
}
