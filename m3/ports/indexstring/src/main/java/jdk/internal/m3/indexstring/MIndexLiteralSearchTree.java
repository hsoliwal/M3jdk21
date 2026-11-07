// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Frozen UTF-16 literal trie with Aho-Corasick failure links.
 *
 * <p>This is the character-domain companion to {@link MIndexTokenTrie}. It serves precomputed
 * literal alternatives and keyword sets without regex execution. Runtime state is entirely
 * primitive arrays; compilation-only maps are discarded by {@link Builder#freeze()}.</p>
 */
public final class MIndexLiteralSearchTree {
  public static final int ROOT_STATE = 0;
  public static final int DEAD_STATE = -1;

  private static final byte LOOKUP_LINEAR = 0;
  private static final byte LOOKUP_BINARY = 1;
  private static final byte LOOKUP_DIRECT = 2;
  private static final int LINEAR_MAX_EDGES = 4;
  private static final int DIRECT_MIN_EDGES = 8;
  private static final int DIRECT_MAX_SPAN = 1024;

  private final int[] firstEdge;
  private final int[] edgeCount;
  private final int[] terminalPayload;
  private final int[] failure;
  private final int[] outputPayload;
  private final int[] outputLength;
  private final char[] edgeChar;
  private final int[] edgeTarget;
  private final byte[] lookupMode;
  private final int[] fastStart;
  private final int[] fastSpan;
  private final char[] directBase;
  private final int[] directTargets;
  private volatile MIndexTrieNavigation navigation;
  private boolean buildingNavigation;

  /** Portable primitive image for build-time/generated literal search trees. */
  public record Image(
      int[] firstEdge,
      int[] edgeCount,
      int[] terminalPayload,
      int[] failure,
      int[] outputPayload,
      int[] outputLength,
      char[] edgeChar,
      int[] edgeTarget) {

    public Image {
      firstEdge = Objects.requireNonNull(firstEdge, "firstEdge").clone();
      edgeCount = Objects.requireNonNull(edgeCount, "edgeCount").clone();
      terminalPayload = Objects.requireNonNull(terminalPayload, "terminalPayload").clone();
      failure = Objects.requireNonNull(failure, "failure").clone();
      outputPayload = Objects.requireNonNull(outputPayload, "outputPayload").clone();
      outputLength = Objects.requireNonNull(outputLength, "outputLength").clone();
      edgeChar = Objects.requireNonNull(edgeChar, "edgeChar").clone();
      edgeTarget = Objects.requireNonNull(edgeTarget, "edgeTarget").clone();

      int states = firstEdge.length;
      if (states == 0
          || edgeCount.length != states
          || terminalPayload.length != states
          || failure.length != states
          || outputPayload.length != states
          || outputLength.length != states) {
        throw new IllegalArgumentException("literal search image state lanes differ in length");
      }
      if (edgeChar.length != edgeTarget.length) {
        throw new IllegalArgumentException("literal search image edge lanes differ in length");
      }
      for (int state = 0; state < states; state++) {
        int start = firstEdge[state];
        int count = edgeCount[state];
        if (start < 0 || count < 0 || start > edgeChar.length || count > edgeChar.length - start) {
          throw new IllegalArgumentException("literal search image edge range");
        }
        if (failure[state] < 0 || failure[state] >= states) {
          throw new IllegalArgumentException("literal search image failure state");
        }
        if (terminalPayload[state] < -1
            || outputPayload[state] < -1
            || outputLength[state] < 0) {
          throw new IllegalArgumentException("literal search image payload metadata");
        }
        for (int edge = start + 1; edge < start + count; edge++) {
          if (edgeChar[edge - 1] >= edgeChar[edge]) {
            throw new IllegalArgumentException("literal search image edges are not sorted");
          }
        }
      }
      for (int target : edgeTarget) {
        if (target < 0 || target >= states) {
          throw new IllegalArgumentException("literal search image edge target");
        }
      }
    }

    @Override public int[] firstEdge() { return firstEdge.clone(); }
    @Override public int[] edgeCount() { return edgeCount.clone(); }
    @Override public int[] terminalPayload() { return terminalPayload.clone(); }
    @Override public int[] failure() { return failure.clone(); }
    @Override public int[] outputPayload() { return outputPayload.clone(); }
    @Override public int[] outputLength() { return outputLength.clone(); }
    @Override public char[] edgeChar() { return edgeChar.clone(); }
    @Override public int[] edgeTarget() { return edgeTarget.clone(); }
  }

  private MIndexLiteralSearchTree(
      int[] firstEdge,
      int[] edgeCount,
      int[] terminalPayload,
      int[] failure,
      int[] outputPayload,
      int[] outputLength,
      char[] edgeChar,
      int[] edgeTarget) {
    this.firstEdge = firstEdge;
    this.edgeCount = edgeCount;
    this.terminalPayload = terminalPayload;
    this.failure = failure;
    this.outputPayload = outputPayload;
    this.outputLength = outputLength;
    this.edgeChar = edgeChar;
    this.edgeTarget = edgeTarget;
    FastTransitions fast = buildFastTransitions(firstEdge, edgeCount, edgeChar, edgeTarget);
    this.lookupMode = fast.mode();
    this.fastStart = fast.start();
    this.fastSpan = fast.span();
    this.directBase = fast.base();
    this.directTargets = fast.targets();
  }

  public Image image() {
    return new Image(
        firstEdge,
        edgeCount,
        terminalPayload,
        failure,
        outputPayload,
        outputLength,
        edgeChar,
        edgeTarget);
  }

  public static MIndexLiteralSearchTree fromImage(Image image) {
    Image checked = Objects.requireNonNull(image, "image");
    return new MIndexLiteralSearchTree(
        checked.firstEdge(),
        checked.edgeCount(),
        checked.terminalPayload(),
        checked.failure(),
        checked.outputPayload(),
        checked.outputLength(),
        checked.edgeChar(),
        checked.edgeTarget());
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Bounded builder for callers preparing large dictionary packs. */
  public static Builder builder(int maxStates) {
    return new Builder(maxStates);
  }

  public MIndexTrieNavigation navigation() {
    return navigation(MIndexTrieNavigation.Limits.DEFAULT, null);
  }

  public MIndexTrieNavigation navigation(M3ProgressMonitor monitor) {
    return navigation(MIndexTrieNavigation.Limits.DEFAULT, monitor);
  }

  /**
   * Adds immutable rank/subtree metadata over these exact arrays. Existing adaptive transitions,
   * spelling storage, IDs, search behavior and image format are not replaced or copied.
   */
  public synchronized MIndexTrieNavigation navigation(
      MIndexTrieNavigation.Limits limits, M3ProgressMonitor monitor) {
    Objects.requireNonNull(limits, "limits");
    MIndexTrieNavigation.check(monitor);
    if (navigation != null) {
      long peak = Integer.BYTES * (8L * stateCount() + navigation.terminalCount());
      if (stateCount() > limits.maxStates() || peak > limits.maxPeakPrimitiveBytes()) {
        throw new IllegalArgumentException("cached navigation exceeds requested limits");
      }
      return navigation;
    }
    if (buildingNavigation) throw new IllegalStateException("reentrant trie navigation preparation");
    buildingNavigation = true;
    try {
      MIndexTrieNavigation prepared = MIndexTrieNavigation.compileShared(
          firstEdge, edgeCount, terminalPayload, edgeChar, edgeTarget,
          (state, unit) -> direct(state, (char) unit), limits, monitor);
      MIndexTrieNavigation.check(monitor);
      navigation = prepared;
      return prepared;
    } finally {
      buildingNavigation = false;
    }
  }

  public int stateCount() {
    return firstEdge.length;
  }

  public int edgeCount() {
    return edgeChar.length;
  }

  public int directAddressStateCount() {
    int count = 0;
    for (byte mode : lookupMode) if (mode == LOOKUP_DIRECT) count++;
    return count;
  }

  public int binaryStateCount() {
    int count = 0;
    for (byte mode : lookupMode) if (mode == LOOKUP_BINARY) count++;
    return count;
  }

  public int linearStateCount() {
    int count = 0;
    for (byte mode : lookupMode) if (mode == LOOKUP_LINEAR) count++;
    return count;
  }

  public long primitivePayloadBytes() {
    long sourceBytes = Integer.BYTES
            * ((long) firstEdge.length
                    + edgeCount.length
                    + terminalPayload.length
                    + failure.length
                    + outputPayload.length
                    + outputLength.length
                    + edgeTarget.length
                    + fastStart.length
                    + fastSpan.length
                    + directTargets.length)
        + Character.BYTES * ((long) edgeChar.length + directBase.length)
        + Byte.BYTES * (long) lookupMode.length;
    MIndexTrieNavigation prepared = navigation;
    return prepared == null ? sourceBytes : Math.addExact(sourceBytes, prepared.primitivePayloadBytes());
  }

  public int exact(CharSequence input) {
    Objects.requireNonNull(input, "input");
    int state = ROOT_STATE;
    for (int i = 0; i < input.length(); i++) {
      state = direct(state, input.charAt(i));
      if (state == DEAD_STATE) return -1;
    }
    return terminalPayload[state];
  }

  public Match findFirst(CharSequence input) {
    Objects.requireNonNull(input, "input");
    int state = ROOT_STATE;
    if (outputPayload[state] >= 0) {
      return new Match(outputPayload[state], 0, 0);
    }
    for (int index = 0; index < input.length(); index++) {
      state = searchTransition(state, input.charAt(index));
      int payload = outputPayload[state];
      if (payload >= 0) {
        int end = index + 1;
        return new Match(payload, end - outputLength[state], end);
      }
    }
    return Match.NONE;
  }

  /**
   * Visits every overlapping literal occurrence without materializing a result list.
   *
   * <p>Occurrences are delivered by increasing UTF-16 end boundary. When several patterns end at
   * the same boundary, the current automaton state is delivered first, followed by progressively
   * shorter failure-suffix terminals; an empty-root literal is delivered last at that boundary.
   * Returning {@code false} from the consumer stops scanning immediately. The returned count is
   * the number of occurrences already delivered.</p>
   */
  public long visitMatches(
      CharSequence input, MatchConsumer consumer, M3ProgressMonitor monitor) {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(consumer, "consumer");
    M3ProgressMonitor progress = monitor == null ? new M3DefaultProgressMonitor() : monitor;
    progress.beginTask("Visit literal matches", input.length());
    try {
      progress.checkCanceled();
      long delivered = 0L;
      int rootPayload = terminalPayload[ROOT_STATE];
      if (rootPayload >= 0) {
        delivered = Math.addExact(delivered, 1L);
        if (!consumer.accept(rootPayload, 0, 0)) return delivered;
      }

      int state = ROOT_STATE;
      for (int index = 0; index < input.length(); index++) {
        progress.checkCanceled();
        state = searchTransition(state, input.charAt(index));
        int end = index + 1;
        for (int linked = state; linked != ROOT_STATE; linked = failure[linked]) {
          int payload = terminalPayload[linked];
          if (payload < 0) continue;
          delivered = Math.addExact(delivered, 1L);
          if (!consumer.accept(payload, end - outputLength[linked], end)) return delivered;
        }
        if (rootPayload >= 0) {
          delivered = Math.addExact(delivered, 1L);
          if (!consumer.accept(rootPayload, end, end)) return delivered;
        }
        progress.worked(1);
      }
      progress.checkCanceled();
      return delivered;
    } finally {
      progress.done();
    }
  }

  public long visitMatches(CharSequence input, MatchConsumer consumer) {
    return visitMatches(input, consumer, null);
  }

  private int searchTransition(int state, char unit) {
    int next = direct(state, unit);
    while (next == DEAD_STATE && state != ROOT_STATE) {
      state = failure[state];
      next = direct(state, unit);
    }
    return next == DEAD_STATE ? ROOT_STATE : next;
  }

  private int direct(int state, char unit) {
    return switch (lookupMode[state]) {
      case LOOKUP_LINEAR -> linearDirect(state, unit);
      case LOOKUP_BINARY -> binaryDirect(state, unit);
      case LOOKUP_DIRECT -> directAddress(state, unit);
      default -> throw new IllegalStateException("unknown literal-tree lookup mode");
    };
  }

  private int linearDirect(int state, char unit) {
    int start = firstEdge[state];
    int end = start + edgeCount[state];
    for (int edge = start; edge < end; edge++) {
      if (edgeChar[edge] == unit) return edgeTarget[edge];
    }
    return DEAD_STATE;
  }

  private int binaryDirect(int state, char unit) {
    int low = firstEdge[state];
    int high = low + edgeCount[state] - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      char actual = edgeChar[mid];
      if (actual < unit) {
        low = mid + 1;
      } else if (actual > unit) {
        high = mid - 1;
      } else {
        return edgeTarget[mid];
      }
    }
    return DEAD_STATE;
  }

  private int directAddress(int state, char unit) {
    int offset = unit - directBase[state];
    if (offset < 0 || offset >= fastSpan[state]) return DEAD_STATE;
    int encoded = directTargets[fastStart[state] + offset];
    return encoded == 0 ? DEAD_STATE : encoded - 1;
  }

  private static FastTransitions buildFastTransitions(
      int[] firstEdge, int[] edgeCount, char[] edgeChar, int[] edgeTarget) {
    int states = firstEdge.length;
    byte[] mode = new byte[states];
    int[] start = new int[states];
    int[] span = new int[states];
    char[] base = new char[states];

    int slots = 0;
    for (int state = 0; state < states; state++) {
      int count = edgeCount[state];
      if (count <= LINEAR_MAX_EDGES) {
        mode[state] = LOOKUP_LINEAR;
        continue;
      }
      int edgeStart = firstEdge[state];
      int first = edgeChar[edgeStart];
      int last = edgeChar[edgeStart + count - 1];
      int width = last - first + 1;
      int denseLimit = Math.max(32, Math.min(DIRECT_MAX_SPAN, count << 2));
      if (count >= DIRECT_MIN_EDGES && width <= denseLimit) {
        mode[state] = LOOKUP_DIRECT;
        start[state] = slots;
        span[state] = width;
        base[state] = (char) first;
        slots = Math.addExact(slots, width);
      } else {
        mode[state] = LOOKUP_BINARY;
      }
    }

    int[] targets = new int[slots];
    for (int state = 0; state < states; state++) {
      if (mode[state] != LOOKUP_DIRECT) continue;
      int edgeStart = firstEdge[state];
      int baseOffset = start[state];
      int baseChar = base[state];
      for (int i = 0; i < edgeCount[state]; i++) {
        int edge = edgeStart + i;
        targets[baseOffset + edgeChar[edge] - baseChar] = edgeTarget[edge] + 1;
      }
    }
    return new FastTransitions(mode, start, span, base, targets);
  }

  private record FastTransitions(
      byte[] mode, int[] start, int[] span, char[] base, int[] targets) {}

  /** Allocation-free callback for one overlapping literal occurrence. */
  @FunctionalInterface
  public interface MatchConsumer {
    /**
     * @return true to continue scanning; false to stop after this delivered match
     */
    boolean accept(int payload, int startUtf16, int endUtf16);
  }

  public record Match(int payload, int startUtf16, int endUtf16) {
    public static final Match NONE = new Match(-1, -1, -1);
    public boolean found() { return payload >= 0; }
  }

  public static final class Builder {
    private final List<MutableNode> nodes = new ArrayList<>();
    private final int maxStates;

    private Builder() {
      this(Integer.MAX_VALUE - 8);
    }

    private Builder(int maxStates) {
      if (maxStates < 1) throw new IllegalArgumentException("maxStates must include the root");
      this.maxStates = maxStates;
      nodes.add(new MutableNode(0));
    }

    public Builder add(CharSequence literal, int payload) {
      return add(literal, payload, null);
    }

    /** Discard this mutable builder after cancellation or budget failure during admission. */
    public Builder add(CharSequence literal, int payload, M3ProgressMonitor monitor) {
      MIndexTrieNavigation.check(monitor);
      Objects.requireNonNull(literal, "literal");
      if (payload < 0) throw new IllegalArgumentException("payload");
      int state = ROOT_STATE;
      for (int i = 0; i < literal.length(); i++) {
        if ((i & 1023) == 0) MIndexTrieNavigation.check(monitor);
        char unit = literal.charAt(i);
        MutableNode node = nodes.get(state);
        Integer next = node.children.get(unit);
        if (next == null) {
          if (nodes.size() == maxStates) throw new IllegalArgumentException("literal trie state budget exceeded");
          next = nodes.size();
          node.children.put(unit, next);
          nodes.add(new MutableNode(node.depth + 1));
        }
        state = next;
      }
      MutableNode terminal = nodes.get(state);
      if (terminal.payload >= 0 && terminal.payload != payload) {
        throw new IllegalArgumentException("duplicate literal with different payload");
      }
      terminal.payload = payload;
      MIndexTrieNavigation.check(monitor);
      return this;
    }

    public MIndexLiteralSearchTree freeze() {
      return freeze(null);
    }

    public MIndexLiteralSearchTree freeze(M3ProgressMonitor monitor) {
      MIndexTrieNavigation.check(monitor);
      int stateCount = nodes.size();
      int totalEdges = 0;
      for (MutableNode node : nodes) {
        totalEdges = Math.addExact(totalEdges, node.children.size());
        MIndexTrieNavigation.check(monitor);
      }

      int[] firstEdge = new int[stateCount];
      int[] edgeCount = new int[stateCount];
      int[] terminalPayload = new int[stateCount];
      int[] failure = new int[stateCount];
      int[] outputPayload = new int[stateCount];
      int[] outputLength = new int[stateCount];
      char[] edgeChar = new char[totalEdges];
      int[] edgeTarget = new int[totalEdges];

      int cursor = 0;
      for (int state = 0; state < stateCount; state++) {
        MIndexTrieNavigation.check(monitor);
        MutableNode node = nodes.get(state);
        firstEdge[state] = cursor;
        edgeCount[state] = node.children.size();
        terminalPayload[state] = node.payload;
        outputPayload[state] = node.payload;
        outputLength[state] = node.payload >= 0 ? node.depth : 0;
        for (var edge : node.children.entrySet()) {
          edgeChar[cursor] = edge.getKey();
          edgeTarget[cursor] = edge.getValue();
          cursor++;
        }
      }

      ArrayDeque<Integer> queue = new ArrayDeque<>();
      for (int child : nodes.get(ROOT_STATE).children.values()) {
        failure[child] = ROOT_STATE;
        queue.addLast(child);
      }

      while (!queue.isEmpty()) {
        MIndexTrieNavigation.check(monitor);
        int state = queue.removeFirst();
        MutableNode node = nodes.get(state);
        for (var edge : node.children.entrySet()) {
          char unit = edge.getKey();
          int target = edge.getValue();
          int fallback = failure[state];
          Integer candidate = nodes.get(fallback).children.get(unit);
          while (candidate == null && fallback != ROOT_STATE) {
            fallback = failure[fallback];
            candidate = nodes.get(fallback).children.get(unit);
          }
          failure[target] = candidate == null ? ROOT_STATE : candidate;
          if (outputPayload[target] < 0) {
            int inherited = outputPayload[failure[target]];
            if (inherited >= 0) {
              outputPayload[target] = inherited;
              outputLength[target] = outputLength[failure[target]];
            }
          }
          queue.addLast(target);
        }
      }

      MIndexTrieNavigation.check(monitor);
      return new MIndexLiteralSearchTree(
          firstEdge,
          edgeCount,
          terminalPayload,
          failure,
          outputPayload,
          outputLength,
          edgeChar,
          edgeTarget);
    }

    private static final class MutableNode {
      private final int depth;
      private final TreeMap<Character, Integer> children = new TreeMap<>();
      private int payload = -1;

      MutableNode(int depth) {
        this.depth = depth;
      }
    }
  }
}
