// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.internal.m3.indexstring;

import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.precompute.EditDistancePlan;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

/**
 * Precomputed navigation metadata over an existing immutable UTF-16 literal trie.
 *
 * <p>This is not another dictionary or trie builder. The owning literal tree supplies its existing
 * edge/terminal arrays and adaptive transition operator; these are shared, never copied here.
 * Only parent/depth, lexical intervals, descendant lengths and terminal-rank coordinates are added.
 * No vocabulary spelling, Java String, resolver or lexicon is consulted by the execution methods.
 * Coordinates belong to this exact tree, not to a global dictionary or another tree generation.
 *
 * <p>Compilation is iterative O(states + edges). Runtime traversal consumes UTF-16 units, not code
 * points, graphemes or physical tokens. Callers own monitor beginTask/done; library operations only
 * report work and check cancellation. Null selects no monitor, but interruption is always honored.
 */
public final class MIndexTrieNavigation {
  public static final int ROOT = 0;
  public static final int DEAD = -1;

  /** Reverse means reverse UTF-16-unit traversal, including isolated surrogate units. */
  public enum Direction { FORWARD, REVERSE }

  /** Added primitive storage, including compilation scratch but excluding the shared source trie. */
  public record Limits(int maxStates, long maxPeakPrimitiveBytes) {
    public static final Limits DEFAULT = new Limits(8_000_000, 512L << 20);
    public Limits {
      if (maxStates < 1 || maxPeakPrimitiveBytes < 0) {
        throw new IllegalArgumentException("invalid trie-navigation limits");
      }
    }
  }

  /** Exhaustion throws; neither a partial match set nor an implicit fallback is returned. */
  public record WorkLimits(long maxTransitions, int maxMatches, long maxWorkspaceBytes) {
    public static final WorkLimits DEFAULT = new WorkLimits(100_000_000L, 8_000_000, 512L << 20);
    public WorkLimits {
      if (maxTransitions < 0 || maxMatches < 0 || maxWorkspaceBytes < 0) {
        throw new IllegalArgumentException("invalid trie-walk limits");
      }
    }
  }

  /** Immutable/pure, UTF-16-consuming compiled machine. No parser or regex dialect is implied. */
  public interface Utf16Machine {
    long initialState();
    long transition(long state, char unit);
    boolean isAccepting(long state);
    /** A dead state must have no accepting continuation, including the empty continuation. */
    boolean isDead(long state);

    /**
     * Whether any complete literal length in this subtree can still be accepted.
     *
     * <p>The default preserves existing machine behavior. Specialized machines may use the
     * navigation owner's precomputed descendant-length range as an exact-safe pruning signal.
     */
    default boolean mayAcceptLengthRange(int minimumLength, int maximumLength) {
      return true;
    }
  }

  /**
   * Compile-once bounded UTF-16 Levenshtein machine for shared-prefix trie traversal.
   *
   * <p>The query is owned by the existing immutable {@link M3EditDistancePlan}; passing
   * an already compiled plan shares it without another spelling copy. Other stable CharSequences
   * enter through that plan's bounded UTF-16 snapshot operation, never through {@code toString()}.
   * The machine adds only scalar configuration and one packed diagonal band per traversal state. For
   * edit radii up to {@value #MAX_EDIT_DISTANCE}, every state fits in one {@code long}; no dynamic
   * programming row, candidate String, or per-node object is allocated while walking a trie.
   * Values outside the active band are exactly greater than the cutoff because their length
   * difference alone exceeds the cutoff.
   *
   * <p>A dead packed state is absorbing. Therefore {@link #walk(Utf16Machine, IntConsumer,
   * WorkLimits, M3ProgressMonitor)} can reject an entire descendant subtree as soon as every band
   * cell is above the cutoff. Shared prefixes are transitioned once by the existing trie walker,
   * rather than once for every vocabulary spelling below that prefix. States are opaque and belong
   * to the machine that produced them; they are not transferable between queries or cutoffs.
   * Compilation reports work and polls cancellation every 1024 input units. The existing plan's
   * query-size limit applies; traversal workspace is separately governed by {@link WorkLimits}.
   */
  public static final class BoundedLevenshteinMachine implements Utf16Machine {
    public static final int MAX_EDIT_DISTANCE = 4;
    private static final long DEAD_STATE = -1L;

    private final EditDistancePlan query;
    private final int maxEditDistance;
    private final int infinity;
    private final int width;
    private final int bitsPerCell;
    private final int payloadBits;
    private final long cellMask;
    private final long initialState;
    private final int minimumAcceptedLength;
    private final long maximumAcceptedLength;

    public BoundedLevenshteinMachine(CharSequence query, int maxEditDistance) {
      this(query, maxEditDistance, null);
    }

    /** The caller keeps the input stable during admission and owns monitor beginTask/done. */
    public BoundedLevenshteinMachine(CharSequence query, int maxEditDistance,
        M3ProgressMonitor monitor) {
      Objects.requireNonNull(query, "query");
      if (maxEditDistance < 0 || maxEditDistance > MAX_EDIT_DISTANCE) {
        throw new IllegalArgumentException(
            "maxEditDistance must be between 0 and " + MAX_EDIT_DISTANCE);
      }
      check(monitor);
      this.query = compileQuery(query, monitor);
      this.maxEditDistance = maxEditDistance;
      this.infinity = maxEditDistance + 1;
      this.width = Math.addExact(Math.multiplyExact(maxEditDistance, 2), 1);
      this.bitsPerCell = Integer.SIZE - Integer.numberOfLeadingZeros(infinity);
      this.payloadBits = Math.multiplyExact(width, bitsPerCell);
      this.cellMask = (1L << bitsPerCell) - 1L;
      this.minimumAcceptedLength = Math.max(0, queryLength() - maxEditDistance);
      this.maximumAcceptedLength = (long) queryLength() + maxEditDistance;
      this.initialState = compileInitialState();
      check(monitor);
    }

    /** The existing immutable pattern owner; its storage is not copied by this machine. */
    public EditDistancePlan queryPlan() { return query; }

    public int queryLength() {
      return query.length();
    }

    public int maxEditDistance() {
      return maxEditDistance;
    }

    /** Packed band bits retained in every machine state; depth is encoded above this payload. */
    public int packedBandBits() {
      return payloadBits;
    }

    @Override
    public long initialState() {
      return initialState;
    }

    @Override
    public long transition(long state, char unit) {
      if (state == DEAD_STATE) return DEAD_STATE;
      int previousDepth = checkedDepth(state);
      if (previousDepth == Integer.MAX_VALUE) return DEAD_STATE;
      int nextDepth = previousDepth + 1;
      if ((long) nextDepth > maximumAcceptedLength) return DEAD_STATE;

      long packed = 0L;
      int minimum = infinity;
      int left = infinity;
      for (int offset = -maxEditDistance; offset <= maxEditDistance; offset++) {
        int queryIndex = nextDepth + offset;
        int value;
        if (queryIndex < 0 || queryIndex > query.length()) {
          value = infinity;
        } else if (queryIndex == 0) {
          value = Math.min(nextDepth, infinity);
        } else {
          int deletion = increment(cell(state, offset + 1));
          int insertion = increment(left);
          int diagonal =
              cell(state, offset)
                  + (query.charAt(queryIndex - 1) == unit ? 0 : 1);
          value = Math.min(infinity, Math.min(diagonal, Math.min(deletion, insertion)));
        }
        left = value;
        minimum = Math.min(minimum, value);
        packed |= (long) value << laneShift(offset);
      }
      return minimum > maxEditDistance ? DEAD_STATE : encode(nextDepth, packed);
    }

    @Override
    public boolean isAccepting(long state) {
      if (state == DEAD_STATE) return false;
      int offset = query.length() - checkedDepth(state);
      return offset >= -maxEditDistance
          && offset <= maxEditDistance
          && cell(state, offset) <= maxEditDistance;
    }

    @Override
    public boolean isDead(long state) {
      return state == DEAD_STATE;
    }

    @Override
    public boolean mayAcceptLengthRange(int minimumLength, int maximumLength) {
      return minimumLength >= 0
          && maximumLength >= minimumLength
          && (long) maximumLength >= minimumAcceptedLength
          && (long) minimumLength <= maximumAcceptedLength;
    }

    private long compileInitialState() {
      long packed = 0L;
      for (int offset = -maxEditDistance; offset <= maxEditDistance; offset++) {
        int value =
            offset >= 0 && offset <= query.length() ? Math.min(offset, infinity) : infinity;
        packed |= (long) value << laneShift(offset);
      }
      return encode(0, packed);
    }

    private int checkedDepth(long state) {
      long depth = state >>> payloadBits;
      if (state < 0L || depth > maximumAcceptedLength || depth > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("invalid Levenshtein machine state");
      }
      return (int) depth;
    }

    private int cell(long state, int offset) {
      if (offset < -maxEditDistance || offset > maxEditDistance) return infinity;
      return (int) ((state >>> laneShift(offset)) & cellMask);
    }

    private int laneShift(int offset) {
      return Math.multiplyExact(offset + maxEditDistance, bitsPerCell);
    }

    private int increment(int value) {
      return value >= infinity ? infinity : value + 1;
    }

    private long encode(int depth, long packed) {
      return ((long) depth << payloadBits) | packed;
    }

    private static EditDistancePlan compileQuery(CharSequence query,
        M3ProgressMonitor monitor) {
      if (query instanceof EditDistancePlan prepared) return prepared;
      final int length = query.length();
      // Invocation-local adapter only: the existing plan is the sole retained query snapshot.
      CharSequence checked = new CharSequence() {
        @Override public int length() { return length; }
        @Override public char charAt(int index) {
          if ((index & 1023) == 0) check(monitor);
          return query.charAt(index);
        }
        @Override public CharSequence subSequence(int start, int end) {
          Objects.checkFromToIndex(start, end, length);
          return query.subSequence(start, end);
        }
      };
      EditDistancePlan prepared = EditDistancePlan.compile(checked);
      check(monitor);
      if (monitor != null) monitor.worked((long) length);
      check(monitor);
      return prepared;
    }
  }

  /** Factory form for callers that do not need the concrete machine type. */
  public static Utf16Machine boundedLevenshtein(CharSequence query, int maxEditDistance) {
    return new BoundedLevenshteinMachine(query, maxEditDistance);
  }

  public static Utf16Machine boundedLevenshtein(CharSequence query, int maxEditDistance,
      M3ProgressMonitor monitor) {
    return new BoundedLevenshteinMachine(query, maxEditDistance, monitor);
  }

  public record RankRange(int fromInclusive, int toExclusive) {
    public RankRange {
      if (fromInclusive < 0 || toExclusive < fromInclusive) {
        throw new IllegalArgumentException("invalid lexical range");
      }
    }
    public int size() { return toExclusive - fromInclusive; }
    public boolean isEmpty() { return fromInclusive == toExclusive; }
  }

  public record WalkStats(long transitions, int visitedStates, int matches, int prunedSubtrees) {}

  /** Payloads remain in lexical order. Distinct literals may intentionally have equal payloads. */
  public static final class Selection {
    private final MIndexTrieNavigation owner;
    private final int[] payloads;
    private final WalkStats stats;
    private Selection(MIndexTrieNavigation owner, int[] payloads, WalkStats stats) {
      this.owner = owner;
      this.payloads = payloads;
      this.stats = stats;
    }
    public MIndexTrieNavigation owner() { return owner; }
    public int size() { return payloads.length; }
    public int payloadAt(int index) { return payloads[Objects.checkIndex(index, size())]; }
    public int[] payloads() { return payloads.clone(); }
    public WalkStats stats() { return stats; }
  }

  /**
   * A completed build-time pattern evaluation. The machine is not retained. Runtime membership is
   * one bit test at a tree-local terminal rank; result paging is indexed, not another traversal.
   * Equal payloads at distinct terminals remain distinct results.
   */
  public static final class PreparedMatches {
    private final MIndexTrieNavigation owner;
    private final long[] accepted;
    private final int[] matchingRanks;
    private final WalkStats stats;

    private PreparedMatches(MIndexTrieNavigation owner, long[] accepted, int[] matchingRanks,
        WalkStats stats) {
      this.owner = owner;
      this.accepted = accepted;
      this.matchingRanks = matchingRanks;
      this.stats = stats;
    }

    public MIndexTrieNavigation owner() { return owner; }
    public int size() { return matchingRanks.length; }
    public WalkStats stats() { return stats; }
    public long primitivePayloadBytes() {
      return Long.BYTES * (long) accepted.length + Integer.BYTES * (long) matchingRanks.length;
    }
    public boolean matchesRank(int rank) {
      Objects.checkIndex(rank, owner.terminalCount());
      return (accepted[rank >>> 6] & (1L << (rank & 63))) != 0;
    }
    public boolean matchesState(int state) {
      return owner.terminalPayload(state) >= 0 && matchesRank(owner.firstRank(state));
    }
    public int rankAt(int ordinal) {
      return matchingRanks[Objects.checkIndex(ordinal, size())];
    }
    public int payloadAt(int ordinal) { return owner.payloadAtRank(rankAt(ordinal)); }
    public int[] page(int offset, int length, M3ProgressMonitor monitor) {
      Objects.checkFromIndexSize(offset, length, size());
      check(monitor);
      int[] result = new int[length];
      for (int i = 0; i < length; i++) {
        result[i] = payloadAt(offset + i);
        if ((i & 1023) == 0) check(monitor);
      }
      check(monitor);
      return result;
    }
  }

  private final int[] firstEdge;
  private final int[] edgeCount;
  private final int[] terminalPayload;
  private final char[] edgeChar;
  private final int[] edgeTarget;
  private final IntBinaryOperator transition;
  private final int[] parent;
  private final int[] depth;
  private final int[] firstRank;
  private final int[] endRank;
  private final int[] minimumLength;
  private final int[] maximumLength;
  private final int[] rankState;
  private final int maximumDepth;

  private MIndexTrieNavigation(
      int[] firstEdge, int[] edgeCount, int[] terminalPayload, char[] edgeChar, int[] edgeTarget,
      IntBinaryOperator transition, int[] parent, int[] depth, int[] firstRank, int[] endRank,
      int[] minimumLength, int[] maximumLength, int[] rankState, int maximumDepth) {
    this.firstEdge = firstEdge;
    this.edgeCount = edgeCount;
    this.terminalPayload = terminalPayload;
    this.edgeChar = edgeChar;
    this.edgeTarget = edgeTarget;
    this.transition = transition;
    this.parent = parent;
    this.depth = depth;
    this.firstRank = firstRank;
    this.endRank = endRank;
    this.minimumLength = minimumLength;
    this.maximumLength = maximumLength;
    this.rankState = rankState;
    this.maximumDepth = maximumDepth;
  }

  /** Package-private ownership boundary: all supplied lanes must already be permanently frozen. */
  static MIndexTrieNavigation compileShared(
      int[] starts, int[] counts, int[] terminals, char[] labels, int[] targets,
      IntBinaryOperator transition, Limits limits, M3ProgressMonitor monitor) {
    Objects.requireNonNull(starts, "starts");
    Objects.requireNonNull(counts, "counts");
    Objects.requireNonNull(terminals, "terminals");
    Objects.requireNonNull(labels, "labels");
    Objects.requireNonNull(targets, "targets");
    Objects.requireNonNull(transition, "transition");
    Objects.requireNonNull(limits, "limits");
    check(monitor);
    int states = starts.length;
    if (states < 1 || counts.length != states || terminals.length != states
        || labels.length != targets.length || targets.length != states - 1) {
      throw new IllegalArgumentException("navigation requires one rooted trie, not a shared-state DAG");
    }
    if (states > limits.maxStates()) throw new IllegalArgumentException("trie state budget exceeded");
    int terminalCount = 0;
    for (int state = 0; state < states; state++) {
      if (terminals[state] < -1) throw new IllegalArgumentException("invalid terminal payload");
      if (terminals[state] >= 0) terminalCount++;
      if ((state & 1023) == 0) check(monitor);
    }
    // Six retained state lanes, one retained terminal lane, and two temporary DFS stack lanes.
    long peakBytes = Integer.BYTES * (8L * states + terminalCount);
    if (peakBytes > limits.maxPeakPrimitiveBytes()) {
      throw new IllegalArgumentException("trie navigation primitive-byte budget exceeded");
    }
    int[] parent = new int[states];
    Arrays.fill(parent, -2);
    parent[ROOT] = DEAD;
    int expectedEdge = 0;
    for (int state = 0; state < states; state++) {
      int count = counts[state];
      if (starts[state] != expectedEdge || count < 0 || count > targets.length - expectedEdge) {
        throw new IllegalArgumentException("non-partitioned trie edge ranges");
      }
      for (int offset = 0; offset < count; offset++) {
        int edge = expectedEdge + offset;
        if (offset > 0 && labels[edge - 1] >= labels[edge]) {
          throw new IllegalArgumentException("trie labels must be strictly sorted");
        }
        int target = targets[edge];
        if (target <= ROOT || target >= states || parent[target] != -2) {
          throw new IllegalArgumentException("trie target has invalid or multiple parents");
        }
        parent[target] = state;
        if ((edge & 1023) == 0) check(monitor);
      }
      expectedEdge += count;
    }
    if (expectedEdge != targets.length) throw new IllegalArgumentException("unowned trie edges");
    int[] depth = new int[states];
    int[] first = new int[states];
    int[] end = new int[states];
    int[] minimum = new int[states];
    int[] maximum = new int[states];
    Arrays.fill(minimum, Integer.MAX_VALUE);
    Arrays.fill(maximum, -1);
    int[] ranks = new int[terminalCount];
    int[] stack = new int[states];
    int[] next = new int[states];
    int top = 0;
    int rank = 0;
    int visited = 1;
    int maximumDepth = 0;
    if (terminals[ROOT] >= 0) {
      ranks[rank++] = ROOT;
      minimum[ROOT] = maximum[ROOT] = 0;
    }
    while (top >= 0) {
      int state = stack[top];
      int ordinal = next[top];
      if (ordinal < counts[state]) {
        next[top] = ordinal + 1;
        int child = targets[starts[state] + ordinal];
        depth[child] = Math.addExact(depth[state], 1);
        maximumDepth = Math.max(maximumDepth, depth[child]);
        first[child] = rank;
        if (terminals[child] >= 0) {
          ranks[rank++] = child;
          minimum[child] = maximum[child] = depth[child];
        }
        stack[++top] = child;
        next[top] = 0;
        visited++;
      } else {
        end[state] = rank;
        if (top > 0) {
          int p = stack[top - 1];
          minimum[p] = Math.min(minimum[p], minimum[state]);
          maximum[p] = Math.max(maximum[p], maximum[state]);
        }
        top--;
        if (monitor != null) monitor.worked(1L);
      }
      check(monitor);
    }
    if (visited != states || rank != terminalCount) {
      throw new IllegalArgumentException("trie has disconnected states or cycles");
    }
    check(monitor);
    return new MIndexTrieNavigation(starts, counts, terminals, labels, targets, transition,
        parent, depth, first, end, minimum, maximum, ranks, maximumDepth);
  }

  public int stateCount() { return parent.length; }
  public int terminalCount() { return rankState.length; }
  /** Does not count any of the shared owner's existing primitive lanes a second time. */
  public long primitivePayloadBytes() { return Integer.BYTES * (6L * stateCount() + terminalCount()); }
  public int parentState(int state) { return parent[checkedState(state)]; }
  public int depth(int state) { return depth[checkedState(state)]; }
  public int terminalPayload(int state) { return terminalPayload[checkedState(state)]; }
  public int firstRank(int state) { return firstRank[checkedState(state)]; }
  public int endRank(int state) { return endRank[checkedState(state)]; }
  public int descendantCount(int state) { return endRank(state) - firstRank(state); }
  /** Returns -1 for a subtree without a terminal. Lengths are complete-path UTF-16 lengths. */
  public int minimumLength(int state) {
    int value = minimumLength[checkedState(state)];
    return value == Integer.MAX_VALUE ? -1 : value;
  }
  public int maximumLength(int state) { return maximumLength[checkedState(state)]; }
  public int stateAtRank(int rank) { return rankState[Objects.checkIndex(rank, terminalCount())]; }
  public int payloadAtRank(int rank) { return terminalPayload[stateAtRank(rank)]; }
  public RankRange range(int state) { return new RankRange(firstRank(state), endRank(state)); }

  public int step(int state, char unit) { return step(state, unit, null); }
  public int step(int state, char unit, M3ProgressMonitor monitor) {
    checkedState(state);
    check(monitor);
    int result = transition.applyAsInt(state, unit);
    if (result < DEAD || result >= stateCount()) {
      throw new IllegalStateException("owner returned an invalid trie state");
    }
    check(monitor);
    return result;
  }

  public int stateFor(CharSequence query) {
    Objects.requireNonNull(query, "query");
    return stateFor(query, 0, query.length(), Direction.FORWARD, null);
  }

  /** Consumes only the supplied range; a String slice is accepted as a CharSequence. */
  public int stateFor(CharSequence query, int begin, int end, Direction direction,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(query, "query");
    Objects.requireNonNull(direction, "direction");
    Objects.checkFromToIndex(begin, end, query.length());
    check(monitor);
    int state = ROOT;
    for (int i = 0; i < end - begin; i++) {
      int offset = direction == Direction.FORWARD ? begin + i : end - 1 - i;
      state = step(state, query.charAt(offset), monitor);
      if (state == DEAD) return DEAD;
    }
    check(monitor);
    return state;
  }

  public int longestPrefixPayload(CharSequence query, int begin, int end, Direction direction,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(query, "query");
    Objects.requireNonNull(direction, "direction");
    Objects.checkFromToIndex(begin, end, query.length());
    check(monitor);
    int state = ROOT;
    int best = terminalPayload[ROOT];
    for (int i = 0; i < end - begin; i++) {
      int offset = direction == Direction.FORWARD ? begin + i : end - 1 - i;
      state = step(state, query.charAt(offset), monitor);
      if (state == DEAD) break;
      if (terminalPayload[state] >= 0) best = terminalPayload[state];
    }
    check(monitor);
    return best;
  }

  /** Exact bounded page within a subtree. No rank sorting or spelling tests occur here. */
  public int[] payloads(int state, int offset, int length, M3ProgressMonitor monitor) {
    int start = firstRank(state);
    Objects.checkFromIndexSize(offset, length, descendantCount(state));
    check(monitor);
    int[] result = new int[length];
    for (int i = 0; i < length; i++) {
      result[i] = payloadAtRank(start + offset + i);
      if ((i & 1023) == 0) check(monitor);
    }
    check(monitor);
    return result;
  }

  /**
   * Streams full-literal matches in lexical order. A shared prefix is transitioned once, not once
   * per descendant spelling. Source spelling storage is never accessed. The callback may already
   * have received earlier matches if it, the machine, cancellation or a work limit throws.
   */
  public WalkStats walk(Utf16Machine machine, IntConsumer onMatch, WorkLimits limits,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(onMatch, "onMatch");
    return walkStates(machine, state -> onMatch.accept(terminalPayload[state]), limits, monitor);
  }

  private WalkStats walkStates(Utf16Machine machine, IntConsumer onMatch, WorkLimits limits,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(machine, "machine");
    Objects.requireNonNull(limits, "limits");
    check(monitor);
    long workspace = 16L * (maximumDepth + 1L);
    if (workspace > limits.maxWorkspaceBytes()) {
      throw new IllegalArgumentException("trie walk workspace budget exceeded");
    }
    int capacity = Math.addExact(maximumDepth, 1);
    int[] states = new int[capacity];
    int[] next = new int[capacity];
    long[] machineStates = new long[capacity];
    machineStates[0] = machine.initialState();
    check(monitor);
    long transitions = 0;
    int matches = 0;
    int visited = 0;
    int pruned = 0;
    int top = 0;
    while (top >= 0) {
      int state = states[top];
      long machineState = machineStates[top];
      if (next[top] == 0) {
        visited++;
        int minimumTerminalLength =
            minimumLength[state] == Integer.MAX_VALUE ? -1 : minimumLength[state];
        if (machine.isDead(machineState)
            || !machine.mayAcceptLengthRange(minimumTerminalLength, maximumLength[state])) {
          pruned++;
          top--;
          check(monitor);
          continue;
        }
        if (terminalPayload[state] >= 0 && machine.isAccepting(machineState)) {
          if (matches == limits.maxMatches()) throw new IllegalStateException("trie match budget exceeded");
          onMatch.accept(state);
          matches++;
          check(monitor);
        }
      }
      int ordinal = next[top];
      if (ordinal < edgeCount[state]) {
        if (transitions == limits.maxTransitions()) {
          throw new IllegalStateException("trie transition budget exceeded");
        }
        int edge = firstEdge[state] + ordinal;
        next[top] = ordinal + 1;
        long after = machine.transition(machineState, edgeChar[edge]);
        transitions++;
        check(monitor);
        states[++top] = edgeTarget[edge];
        next[top] = 0;
        machineStates[top] = after;
      } else {
        top--;
        if (monitor != null) monitor.worked(1L);
        check(monitor);
      }
    }
    check(monitor);
    return new WalkStats(transitions, visited, matches, pruned);
  }

  public Selection select(Utf16Machine machine) { return select(machine, WorkLimits.DEFAULT, null); }

  /** Materialized result with no partial publication on failure; use walk for streaming delivery. */
  public Selection select(Utf16Machine machine, WorkLimits limits, M3ProgressMonitor monitor) {
    Objects.requireNonNull(limits, "limits");
    check(monitor);
    int possible = Math.min(terminalCount(), limits.maxMatches());
    long peak = 16L * (maximumDepth + 1L) + 8L * possible + Integer.BYTES;
    if (peak > limits.maxWorkspaceBytes()) {
      throw new IllegalArgumentException("trie selection workspace budget exceeded");
    }
    int[] scratch = new int[possible];
    int[] count = {0};
    WalkStats stats = walk(machine, payload -> scratch[count[0]++] = payload, limits, monitor);
    int[] result = Arrays.copyOf(scratch, count[0]);
    check(monitor);
    return new Selection(this, result, stats);
  }

  public PreparedMatches precompute(Utf16Machine machine) {
    return precompute(machine, WorkLimits.DEFAULT, null);
  }

  /**
   * Run the supplied compiled machine once per shared trie edge and freeze the answers. This
   * method belongs in dictionary-pack generation or admission, not in the repeated match loop.
   * A failure publishes no result. No parser, machine or query spelling is retained by the result.
   */
  public PreparedMatches precompute(Utf16Machine machine, WorkLimits limits,
      M3ProgressMonitor monitor) {
    Objects.requireNonNull(machine, "machine");
    Objects.requireNonNull(limits, "limits");
    check(monitor);
    int possible = Math.min(terminalCount(), limits.maxMatches());
    int words = (int) ((terminalCount() + 63L) >>> 6);
    long peak = 16L * (maximumDepth + 1L) + 8L * possible + 8L * words + Integer.BYTES;
    if (peak > limits.maxWorkspaceBytes()) {
      throw new IllegalArgumentException("prepared match workspace budget exceeded");
    }
    long[] accepted = new long[words];
    int[] scratch = new int[possible];
    int[] count = {0};
    WalkStats stats = walkStates(machine, state -> {
      int rank = firstRank[state];
      accepted[rank >>> 6] |= 1L << (rank & 63);
      scratch[count[0]++] = rank;
    }, limits, monitor);
    int[] matchingRanks = Arrays.copyOf(scratch, count[0]);
    check(monitor);
    return new PreparedMatches(this, accepted, matchingRanks, stats);
  }

  private int checkedState(int state) { return Objects.checkIndex(state, stateCount()); }

  static void check(M3ProgressMonitor monitor) {
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("trie operation interrupted");
    if (monitor != null) monitor.checkCanceled();
  }
}

