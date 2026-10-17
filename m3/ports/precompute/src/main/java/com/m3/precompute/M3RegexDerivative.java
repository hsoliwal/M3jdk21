// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import com.m3.precompute.M3RegexPredicateAlphabet.Shadow;
import com.m3.precompute.M3RegexPredicateAlphabet.UnitSet;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Canonical Brzozowski-derivative regex algebra over {@link M3RegexPredicateAlphabet} minterms.
 *
 * <p>Compilation hash-conses immutable algebraic nodes, memoizes derivatives, closes the reachable
 * derivative graph, minimizes equivalent states, quotients behaviorally identical alphabet
 * minterms, computes nullable/live/dead facts, and precomputes powers of equal-class transitions.
 * Runtime matching over an MIndex shadow therefore consumes RLE predicate runs with integer table
 * lookups and no expression construction or backtracking. Intersection and complement are
 * first-class. Captures, lookarounds and backreferences remain with their existing owners.
 */
public final class M3RegexDerivative {
  public sealed interface Expr permits Empty, Epsilon, Unit, Alternative, Concatenation, Star,
      Intersection, Complement {}
  private enum Empty implements Expr { INSTANCE }
  private enum Epsilon implements Expr { INSTANCE }
  private record Unit(UnitSet predicate) implements Expr {}
  private record Alternative(List<Expr> children) implements Expr {}
  private record Concatenation(List<Expr> children) implements Expr {}
  private record Star(Expr child) implements Expr {}
  private record Intersection(List<Expr> children) implements Expr {}
  private record Complement(Expr child) implements Expr {}

  public enum Backend { AUTO, JAVA, JNI }

  public record Limits(
      int maxExpressionNodes,
      int maxDepth,
      int maxCanonicalNodes,
      int maxDerivativeStates,
      long maxDerivativeCells,
      int maxBatchUnits,
      long maxRunPowerCells) {
    public static final Limits DEFAULT =
        new Limits(4096, 64, 65_536, 16_384, 8_000_000L, 16_000_000, 32_000_000L);
    public Limits {
      if (maxExpressionNodes < 1 || maxDepth < 1 || maxDepth > 256 || maxCanonicalNodes < 2
          || maxDerivativeStates < 1 || maxDerivativeCells < 1 || maxBatchUnits < 0
          || maxRunPowerCells < 1) {
        throw new IllegalArgumentException("invalid derivative regex limits");
      }
    }
  }

  public static final class BatchResult {
    private final boolean[] matches;
    private final Backend backend;
    private final int nativeCalls;
    private BatchResult(boolean[] matches, Backend backend, int nativeCalls) {
      this.matches = matches.clone();
      this.backend = Objects.requireNonNull(backend, "backend");
      this.nativeCalls = nativeCalls;
    }
    public boolean[] matches() { return matches.clone(); }
    public Backend backend() { return backend; }
    public int nativeCalls() { return nativeCalls; }
  }

  public record Statistics(
      int canonicalNodes,
      int reachableStates,
      int minimizedStates,
      int alphabetClasses,
      int executionClasses,
      long derivativeComputations,
      long simplifications,
      int liveStates,
      int deadStates,
      int runPowerBits,
      long runPowerCells) {}

  private final M3RegexPredicateAlphabet alphabet;
  private final Limits limits;
  private final int startState;
  private final int executionClasses;
  private final int[] alphabetToExecutionClass;
  private final int[] transitions;
  private final byte[] accepting;
  private final byte[] live;
  private final long[] viableClasses;
  private final int viableWordsPerState;
  private final int[] runPowers;
  private final int runPowerBits;
  private final Statistics statistics;
  private final String programSha256;

  private M3RegexDerivative(
      M3RegexPredicateAlphabet alphabet,
      Limits limits,
      int startState,
      int executionClasses,
      int[] alphabetToExecutionClass,
      int[] transitions,
      byte[] accepting,
      byte[] live,
      long[] viableClasses,
      int viableWordsPerState,
      int[] runPowers,
      int runPowerBits,
      Statistics statistics,
      String programSha256) {
    this.alphabet = alphabet;
    this.limits = limits;
    this.startState = startState;
    this.executionClasses = executionClasses;
    this.alphabetToExecutionClass = alphabetToExecutionClass;
    this.transitions = transitions;
    this.accepting = accepting;
    this.live = live;
    this.viableClasses = viableClasses;
    this.viableWordsPerState = viableWordsPerState;
    this.runPowers = runPowers;
    this.runPowerBits = runPowerBits;
    this.statistics = statistics;
    this.programSha256 = programSha256;
  }

  public static Expr empty() { return Empty.INSTANCE; }
  public static Expr epsilon() { return Epsilon.INSTANCE; }
  public static Expr unit(UnitSet predicate) {
    return new Unit(Objects.requireNonNull(predicate, "predicate"));
  }
  public static Expr literal(CharSequence text) {
    Objects.requireNonNull(text, "text");
    ArrayList<Expr> units = new ArrayList<>(text.length());
    for (int index = 0; index < text.length(); index++) {
      char value = text.charAt(index);
      if (Character.isSurrogate(value)) {
        throw new IllegalArgumentException("surrogate literal at " + index);
      }
      units.add(unit(UnitSet.literal(value)));
    }
    return sequence(units);
  }
  public static Expr sequence(Expr... children) { return sequence(List.of(children)); }
  public static Expr sequence(Collection<? extends Expr> children) {
    List<Expr> copy = copy(children);
    return copy.isEmpty() ? epsilon() : new Concatenation(copy);
  }
  public static Expr either(Expr... children) { return either(List.of(children)); }
  public static Expr either(Collection<? extends Expr> children) {
    List<Expr> copy = copy(children);
    return copy.isEmpty() ? empty() : new Alternative(copy);
  }
  public static Expr star(Expr child) { return new Star(Objects.requireNonNull(child, "child")); }
  public static Expr plus(Expr child) {
    Expr checked = Objects.requireNonNull(child, "child");
    return sequence(checked, star(checked));
  }
  public static Expr optional(Expr child) { return either(epsilon(), child); }
  public static Expr intersect(Expr... children) { return intersect(List.of(children)); }
  public static Expr intersect(Collection<? extends Expr> children) {
    List<Expr> copy = copy(children);
    return copy.isEmpty() ? complement(empty()) : new Intersection(copy);
  }
  public static Expr complement(Expr child) {
    return new Complement(Objects.requireNonNull(child, "child"));
  }
  public static Expr anyString() { return star(unit(UnitSet.any())); }

  private static List<Expr> copy(Collection<? extends Expr> source) {
    Objects.requireNonNull(source, "children");
    ArrayList<Expr> result = new ArrayList<>(source.size());
    for (Expr expression : source) result.add(Objects.requireNonNull(expression, "child"));
    return List.copyOf(result);
  }

  public static M3RegexPredicateAlphabet alphabetFor(
      Collection<? extends Expr> expressions, M3Progress monitor) {
    Objects.requireNonNull(expressions, "expressions");
    LinkedHashMap<UnitSet, Integer> predicates = new LinkedHashMap<>();
    for (Expr expression : expressions) {
      for (UnitSet predicate : inspect(expression, Limits.DEFAULT, monitor).predicates()) {
        predicates.putIfAbsent(predicate, predicates.size());
      }
    }
    return M3RegexPredicateAlphabet.compile(predicates.keySet(), monitor);
  }

  public static M3RegexDerivative compile(
      Expr expression, M3RegexPredicateAlphabet alphabet, M3Progress monitor) {
    return compile(expression, alphabet, Limits.DEFAULT, monitor);
  }

  public static M3RegexDerivative compile(
      Expr expression,
      M3RegexPredicateAlphabet alphabet,
      Limits limits,
      M3Progress monitor) {
    Objects.requireNonNull(alphabet, "alphabet");
    Objects.requireNonNull(limits, "limits");
    Inventory inventory = inspect(expression, limits, monitor);
    LinkedHashMap<UnitSet, Integer> predicateIds = new LinkedHashMap<>();
    for (UnitSet predicate : inventory.predicates()) {
      predicateIds.putIfAbsent(predicate, predicateIds.size());
    }
    boolean[][] classesByPredicate = new boolean[predicateIds.size()][];
    for (Map.Entry<UnitSet, Integer> entry : predicateIds.entrySet()) {
      classesByPredicate[entry.getValue()] = alphabet.lower(entry.getKey(), monitor);
    }

    Arena arena = new Arena(predicateIds, classesByPredicate, limits);
    int root = arena.lower(Objects.requireNonNull(expression, "expression"));
    Frozen reachable =
        freeze(arena, root, alphabet.mintermCount(), limits, monitor);
    Frozen minimized = minimize(reachable, monitor);
    Quotient quotient = quotientClasses(minimized, alphabet.mintermCount(), monitor);
    byte[] live = computeLive(quotient.transitions(), minimized.accepting(), quotient.classes(), monitor);
    int liveStates = 0;
    for (byte value : live) if (value != 0) liveStates++;
    int viableWords = (quotient.classes() + Long.SIZE - 1) / Long.SIZE;
    long[] viable =
        computeViableClasses(quotient.transitions(), live, quotient.classes(), viableWords);
    PowerTable powers =
        buildRunPowers(
            quotient.transitions(),
            minimized.accepting().length,
            quotient.classes(),
            limits.maxRunPowerCells(),
            monitor);

    Statistics stats =
        new Statistics(
            arena.nodes.size(),
            reachable.accepting().length,
            minimized.accepting().length,
            alphabet.mintermCount(),
            quotient.classes(),
            arena.derivativeComputations,
            arena.simplifications,
            liveStates,
            live.length - liveStates,
            powers.bits(),
            powers.cells().length);

    String programSha =
        sha256(
            minimized.startState(),
            quotient.classMap(),
            quotient.transitions(),
            minimized.accepting(),
            live,
            powers.bits(),
            powers.cells(),
            inventory.predicates());

    return new M3RegexDerivative(
        alphabet,
        limits,
        minimized.startState(),
        quotient.classes(),
        quotient.classMap(),
        quotient.transitions(),
        minimized.accepting(),
        live,
        viable,
        viableWords,
        powers.cells(),
        powers.bits(),
        stats,
        programSha);
  }

  /**
   * Reuses the existing bounded M3RegexDfaEffects execution/optional JNI composition substrate
   * for ASCII-only fragments. Non-ASCII input is refused by that owner; use matches() instead.
   * This is an additive proving-ground bridge, not java.util.regex.Pattern replacement.
   */
  M3RegexDfaEffects.Plan asciiEffects(
      M3RegexDfaEffects.Limits effectLimits, Runnable checkpoint) {
    Objects.requireNonNull(effectLimits, "effectLimits");
    return M3RegexDfaEffects.prepare(new M3RegexDfaEffects.Dfa() {
      @Override public int stateCount() { return accepting.length; }
      @Override public int startState() { return startState; }
      @Override public int next(int state, int ascii) {
        Objects.checkIndex(state, accepting.length);
        if (ascii < 0 || ascii >= 128) throw new IllegalArgumentException("not ASCII");
        int minterm = alphabet.classForUnit((char) ascii);
        return transitions[state * executionClasses + alphabetToExecutionClass[minterm]];
      }
      @Override public boolean accepting(int state) {
        return M3RegexDerivative.this.isAcceptingState(state);
      }
    }, effectLimits, checkpoint);
  }

  public M3RegexPredicateAlphabet alphabet() { return alphabet; }
  public int stateCount() { return accepting.length; }
  public int alphabetClassCount() { return alphabetToExecutionClass.length; }
  public int executionClassCount() { return executionClasses; }
  public int runPowerBits() { return runPowerBits; }
  public Statistics statistics() { return statistics; }
  public String programSha256() { return programSha256; }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * (long)
                (alphabetToExecutionClass.length + transitions.length + runPowers.length)
        + accepting.length
        + live.length
        + Long.BYTES * (long) viableClasses.length;
  }

  public boolean isAcceptingState(int state) {
    return accepting[Objects.checkIndex(state, accepting.length)] != 0;
  }

  public boolean isLiveState(int state) {
    return live[Objects.checkIndex(state, live.length)] != 0;
  }

  public boolean mayConsume(int state, int executionClass) {
    Objects.checkIndex(state, stateCount());
    Objects.checkIndex(executionClass, executionClasses);
    int word = executionClass >>> 6;
    long bit = 1L << (executionClass & 63);
    return (viableClasses[state * viableWordsPerState + word] & bit) != 0L;
  }

  public boolean matches(CharSequence source, M3Progress monitor) {
    return matches(alphabet.project(Objects.requireNonNull(source, "source"), monitor), monitor);
  }

  public boolean matches(Shadow shadow, M3Progress monitor) {
    requireShadow(shadow);
    int state = startState;
    for (int run = 0; run < shadow.runCount(); run++) {
      if ((run & 255) == 0) M3RegexPredicateAlphabet.check(monitor);
      int executionClass = alphabetToExecutionClass[shadow.typeAtRun(run)];
      if (!mayConsume(state, executionClass)) return false;
      state = advanceRun(state, executionClass, shadow.lengthAtRun(run));
      if (live[state] == 0) return false;
    }
    M3RegexPredicateAlphabet.check(monitor);
    return accepting[state] != 0;
  }

  public BatchResult matchesBatch(
      Collection<? extends CharSequence> sources, Backend backend, M3Progress monitor) {
    Objects.requireNonNull(sources, "sources");
    Backend requested = Objects.requireNonNull(backend, "backend");
    ArrayList<Shadow> shadows = new ArrayList<>(sources.size());
    long totalUnits = 0;
    int totalRuns = 0;
    for (CharSequence source : sources) {
      Shadow shadow = alphabet.project(Objects.requireNonNull(source, "source"), monitor);
      shadows.add(shadow);
      totalUnits += shadow.length();
      if (totalUnits > limits.maxBatchUnits()) {
        throw new IllegalArgumentException("batch unit budget exceeded");
      }
      totalRuns = Math.addExact(totalRuns, shadow.runCount());
    }

    boolean explicitNative = requested == Backend.JNI;
    boolean wantsNative =
        explicitNative
            || (requested == Backend.AUTO
                && M3RegexDerivativeNative.shouldUse(shadows.size()));
    boolean nativeAvailable = wantsNative && M3RegexDerivativeNative.available();
    if (explicitNative && !nativeAvailable) {
      throw new IllegalStateException("JNI backend unavailable");
    }

    if (nativeAvailable) {
      ProjectedRuns batch = flattenRuns(shadows, totalRuns, monitor);
      try {
        byte[] raw =
            M3RegexDerivativeNative.matchRunsBatch(
                executionClasses,
                stateCount(),
                startState,
                runPowers,
                runPowerBits,
                accepting,
                live,
                batch.classes(),
                batch.lengths(),
                batch.offsets());
        if (raw.length != shadows.size()) {
          throw new IllegalStateException("JNI row count mismatch");
        }
        boolean[] result = new boolean[raw.length];
        for (int index = 0; index < raw.length; index++) {
          if (raw[index] != 0 && raw[index] != 1) {
            throw new IllegalStateException("JNI non-boolean lane");
          }
          result[index] = raw[index] != 0;
        }
        return new BatchResult(result, Backend.JNI, 1);
      } catch (UnsatisfiedLinkError missingSymbol) {
        if (explicitNative) {
          throw new IllegalStateException("JNI symbol unavailable", missingSymbol);
        }
      }
    }

    boolean[] result = new boolean[shadows.size()];
    for (int index = 0; index < shadows.size(); index++) {
      result[index] = matches(shadows.get(index), monitor);
    }
    return new BatchResult(result, Backend.JAVA, 0);
  }

  /**
   * Caller-owned, program-bound predicate runs. Retains no input spelling or source object.
   * Its digest identifies projected behavior, never exact source-text identity.
   */
  public static final class PreparedBatch {
    private final M3RegexDerivative owner;
    private final ProjectedRuns runs;
    private final long units;
    private final String fingerprint;

    private PreparedBatch(M3RegexDerivative owner, ProjectedRuns runs, long units) {
      this.owner = owner;
      this.runs = runs;
      this.units = units;
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update("MIndexPreparedRuns/v1".getBytes(StandardCharsets.US_ASCII));
        digest.update(owner.programSha256.getBytes(StandardCharsets.US_ASCII));
        ByteBuffer word = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);
        digest.update(word.putLong(units).array());
        for (int[] lane : List.of(runs.classes(), runs.lengths(), runs.offsets())) {
          digest.update(word.clear().putLong(lane.length).array());
          for (int value : lane) digest.update(word.clear().putLong(value).array());
        }
        fingerprint = java.util.HexFormat.of().formatHex(digest.digest());
      } catch (NoSuchAlgorithmException impossible) {
        throw new AssertionError(impossible);
      }
    }

    public int size() { return runs.offsets().length - 1; }
    public int runCount() { return runs.classes().length; }
    public long inputUnits() { return units; }
    public String fingerprint() { return fingerprint; }
    public long primitivePayloadBytes() {
      return Integer.BYTES * (2L * runCount() + runs.offsets().length);
    }
  }

  /**
   * Project and pack once for repeated Java/JNI evaluation. Row count and worst-case packed
   * primitive bytes are admitted before reading any input character. Per-atom projection limits
   * still apply. Caller-owned mutable inputs must remain stable during preparation.
   */
  public PreparedBatch prepareBatch(Collection<? extends CharSequence> sources,
      int maxRows, long maxPrimitiveBytes, M3Progress monitor) {
    Objects.requireNonNull(sources, "sources");
    if (maxRows < 0 || maxRows > 1_000_000 || maxPrimitiveBytes < Integer.BYTES) {
      throw new IllegalArgumentException("prepared batch budget");
    }
    M3RegexPredicateAlphabet.check(monitor);
    ArrayList<CharSequence> inputs = new ArrayList<>();
    long totalUnits = 0;
    for (CharSequence source : sources) {
      M3RegexPredicateAlphabet.check(monitor);
      if (inputs.size() == maxRows) throw new IllegalArgumentException("batch row budget exceeded");
      CharSequence input = Objects.requireNonNull(source, "source");
      int length = input.length();
      if (length < 0) throw new IllegalArgumentException("negative source length");
      totalUnits = Math.addExact(totalUnits, length);
      if (totalUnits > limits.maxBatchUnits()) throw new IllegalArgumentException("batch unit budget exceeded");
      inputs.add(input);
    }
    long worstCaseBytes = Integer.BYTES * (2L * totalUnits + inputs.size() + 1L);
    if (worstCaseBytes > maxPrimitiveBytes) throw new IllegalArgumentException("batch primitive byte budget exceeded");
    ArrayList<Shadow> shadows = new ArrayList<>(inputs.size());
    int totalRuns = 0;
    long observedUnits = 0;
    for (CharSequence input : inputs) {
      Shadow shadow = alphabet.project(input, monitor);
      observedUnits = Math.addExact(observedUnits, shadow.length());
      if (observedUnits > totalUnits) throw new IllegalArgumentException("input changed during preparation");
      totalRuns = Math.addExact(totalRuns, shadow.runCount());
      shadows.add(shadow);
    }
    if (observedUnits != totalUnits) throw new IllegalArgumentException("input changed during preparation");
    ProjectedRuns runs = flattenRuns(shadows, totalRuns, monitor);
    M3RegexPredicateAlphabet.check(monitor);
    PreparedBatch result = new PreparedBatch(this, runs, totalUnits);
    M3RegexPredicateAlphabet.check(monitor);
    return result;
  }

  /** Evaluate frozen runs without re-reading or retaining any input text. */
  public BatchResult matchesPrepared(PreparedBatch prepared, Backend backend, M3Progress monitor) {
    Objects.requireNonNull(prepared, "prepared");
    Backend requested = Objects.requireNonNull(backend, "backend");
    M3RegexPredicateAlphabet.check(monitor);
    if (prepared.owner != this) throw new IllegalArgumentException("foreign prepared program");
    ProjectedRuns batch = prepared.runs;
    boolean explicitNative = requested == Backend.JNI;
    boolean wantsNative = explicitNative || (requested == Backend.AUTO
        && M3RegexDerivativeNative.shouldUse(prepared.size()));
    boolean available = wantsNative && M3RegexDerivativeNative.available();
    if (explicitNative && !available) throw new IllegalStateException("JNI backend unavailable");
    if (available) {
      try {
        byte[] raw = M3RegexDerivativeNative.matchRunsBatch(executionClasses, stateCount(),
            startState, runPowers, runPowerBits, accepting, live,
            batch.classes(), batch.lengths(), batch.offsets());
        M3RegexPredicateAlphabet.check(monitor);
        if (raw.length != prepared.size()) throw new IllegalStateException("JNI row count mismatch");
        boolean[] result = new boolean[raw.length];
        for (int row = 0; row < raw.length; row++) {
          if (raw[row] != 0 && raw[row] != 1) throw new IllegalStateException("JNI non-boolean lane");
          result[row] = raw[row] != 0;
        }
        return new BatchResult(result, Backend.JNI, 1);
      } catch (UnsatisfiedLinkError missingSymbol) {
        if (explicitNative) throw new IllegalStateException("JNI symbol unavailable", missingSymbol);
      }
    }
    boolean[] result = new boolean[prepared.size()];
    for (int row = 0; row < result.length; row++) {
      M3RegexPredicateAlphabet.check(monitor);
      int state = startState;
      for (int at = batch.offsets()[row]; at < batch.offsets()[row + 1]; at++) {
        if ((at & 255) == 0) M3RegexPredicateAlphabet.check(monitor);
        state = advanceRun(state, batch.classes()[at], batch.lengths()[at]);
        if (live[state] == 0) break;
      }
      result[row] = accepting[state] != 0;
    }
    M3RegexPredicateAlphabet.check(monitor);
    return new BatchResult(result, Backend.JAVA, 0);
  }


  private void requireShadow(Shadow shadow) {
    Objects.requireNonNull(shadow, "shadow");
    if (shadow.alphabet() != alphabet) {
      throw new IllegalArgumentException("foreign predicate alphabet");
    }
  }

  private int advanceRun(int state, int executionClass, int length) {
    if (length < 0) throw new IllegalArgumentException("negative run length");
    if (length == 0) return state;

    int highestBit = runPowerBits - 1;
    int chunk = 1 << highestBit;
    int chunks = length / chunk;
    int current = state;
    for (int count = 0; count < chunks; count++) {
      current = power(highestBit, executionClass, current);
      if (live[current] == 0) return current;
    }

    int tail = length % chunk;
    int bit = 0;
    while (tail != 0) {
      if ((tail & 1) != 0) current = power(bit, executionClass, current);
      tail >>>= 1;
      bit++;
    }
    return current;
  }

  private int power(int bit, int executionClass, int state) {
    int plane = Math.multiplyExact(executionClasses, stateCount());
    int offset =
        Math.addExact(
            Math.multiplyExact(bit, plane),
            Math.addExact(Math.multiplyExact(executionClass, stateCount()), state));
    return runPowers[offset];
  }

  private ProjectedRuns flattenRuns(
      List<Shadow> shadows, int totalRuns, M3Progress monitor) {
    int[] classes = new int[totalRuns];
    int[] lengths = new int[totalRuns];
    int[] offsets = new int[shadows.size() + 1];
    int cursor = 0;
    for (int row = 0; row < shadows.size(); row++) {
      offsets[row] = cursor;
      Shadow shadow = shadows.get(row);
      for (int run = 0; run < shadow.runCount(); run++) {
        classes[cursor] = alphabetToExecutionClass[shadow.typeAtRun(run)];
        lengths[cursor] = shadow.lengthAtRun(run);
        cursor++;
      }
      M3RegexPredicateAlphabet.check(monitor);
    }
    offsets[shadows.size()] = cursor;
    return new ProjectedRuns(classes, lengths, offsets);
  }

  private record ProjectedRuns(int[] classes, int[] lengths, int[] offsets) {}
  private record Inventory(List<UnitSet> predicates) {}
  private record AtDepth(Expr expression, int depth) {}

  private static Inventory inspect(
      Expr expression, Limits limits, M3Progress monitor) {
    Objects.requireNonNull(expression, "expression");
    LinkedHashMap<UnitSet, Integer> predicates = new LinkedHashMap<>();
    ArrayDeque<AtDepth> work = new ArrayDeque<>();
    work.push(new AtDepth(expression, 1));
    int count = 0;
    while (!work.isEmpty()) {
      M3RegexPredicateAlphabet.check(monitor);
      AtDepth item = work.pop();
      if (++count > limits.maxExpressionNodes() || item.depth() > limits.maxDepth()) {
        throw new IllegalArgumentException("expression size/depth budget exceeded");
      }
      switch (item.expression()) {
        case Empty ignored -> {}
        case Epsilon ignored -> {}
        case Unit unit -> predicates.putIfAbsent(unit.predicate(), predicates.size());
        case Star star -> work.push(new AtDepth(star.child(), item.depth() + 1));
        case Complement not -> work.push(new AtDepth(not.child(), item.depth() + 1));
        case Alternative alternative -> push(work, alternative.children(), item.depth());
        case Concatenation concatenation -> push(work, concatenation.children(), item.depth());
        case Intersection intersection -> push(work, intersection.children(), item.depth());
      }
    }
    return new Inventory(List.copyOf(predicates.keySet()));
  }

  private static void push(ArrayDeque<AtDepth> work, List<Expr> children, int depth) {
    for (int index = children.size() - 1; index >= 0; index--) {
      work.push(new AtDepth(children.get(index), depth + 1));
    }
  }

  private enum Op { EMPTY, EPSILON, UNIT, ALT, CONCAT, STAR, INTERSECT, COMPLEMENT }
  private record Node(Op op, int left, int right, int predicate) {}
  private record Key(Op op, int left, int right, int predicate) {}

  private static final class Arena {
    private static final int EMPTY = 0;
    private static final int EPSILON = 1;
    private final Map<UnitSet, Integer> predicateIds;
    private final boolean[][] predicateClasses;
    private final Limits limits;
    private final ArrayList<Node> nodes = new ArrayList<>();
    private final ArrayList<Byte> nullability = new ArrayList<>();
    private final HashMap<Key, Integer> intern = new HashMap<>();
    private final HashMap<Long, Integer> derivatives = new HashMap<>();
    private long derivativeComputations;
    private long simplifications;

    Arena(
        Map<UnitSet, Integer> predicateIds,
        boolean[][] predicateClasses,
        Limits limits) {
      this.predicateIds = predicateIds;
      this.predicateClasses = predicateClasses;
      this.limits = limits;
      seed(new Node(Op.EMPTY, -1, -1, -1), false);
      seed(new Node(Op.EPSILON, -1, -1, -1), true);
    }

    private void seed(Node node, boolean nullableValue) {
      int id = nodes.size();
      nodes.add(node);
      nullability.add((byte) (nullableValue ? 1 : 0));
      intern.put(new Key(node.op(), node.left(), node.right(), node.predicate()), id);
    }

    int lower(Expr expression) {
      return switch (expression) {
        case Empty ignored -> EMPTY;
        case Epsilon ignored -> EPSILON;
        case Unit unit -> raw(Op.UNIT, -1, -1, predicateIds.get(unit.predicate()));
        case Star star -> star(lower(star.child()));
        case Complement not -> complement(lower(not.child()));
        case Alternative alternative -> foldAlt(alternative.children());
        case Concatenation concatenation -> foldConcat(concatenation.children());
        case Intersection intersection -> foldAnd(intersection.children());
      };
    }

    private int foldAlt(List<Expr> children) {
      int result = EMPTY;
      for (Expr child : children) result = alt(result, lower(child));
      return result;
    }

    private int foldAnd(List<Expr> children) {
      int result = complement(EMPTY);
      for (Expr child : children) result = and(result, lower(child));
      return result;
    }

    private int foldConcat(List<Expr> children) {
      int result = EPSILON;
      for (Expr child : children) result = concat(result, lower(child));
      return result;
    }

    private int star(int child) {
      if (child == EMPTY || child == EPSILON) {
        simplifications++;
        return EPSILON;
      }
      if (nodes.get(child).op() == Op.STAR) {
        simplifications++;
        return child;
      }
      return raw(Op.STAR, child, -1, -1);
    }

    private int complement(int child) {
      Node node = nodes.get(child);
      if (node.op() == Op.COMPLEMENT) {
        simplifications++;
        return node.left();
      }
      return raw(Op.COMPLEMENT, child, -1, -1);
    }

    private boolean universal(int id) {
      Node node = nodes.get(id);
      return node.op() == Op.COMPLEMENT && node.left() == EMPTY;
    }

    private int alt(int left, int right) {
      if (left == right) {
        simplifications++;
        return left;
      }
      if (left == EMPTY) {
        simplifications++;
        return right;
      }
      if (right == EMPTY) {
        simplifications++;
        return left;
      }
      if (universal(left)) {
        simplifications++;
        return left;
      }
      if (universal(right)) {
        simplifications++;
        return right;
      }
      return commutative(Op.ALT, left, right);
    }

    private int and(int left, int right) {
      if (left == right) {
        simplifications++;
        return left;
      }
      if (left == EMPTY || right == EMPTY) {
        simplifications++;
        return EMPTY;
      }
      if (universal(left)) {
        simplifications++;
        return right;
      }
      if (universal(right)) {
        simplifications++;
        return left;
      }
      return commutative(Op.INTERSECT, left, right);
    }

    private int commutative(Op op, int left, int right) {
      TreeSet<Integer> terms = new TreeSet<>();
      collect(op, left, terms);
      collect(op, right, terms);
      if (op == Op.INTERSECT && terms.contains(EMPTY)) return EMPTY;
      if (op == Op.ALT && terms.stream().anyMatch(this::universal)) {
        return complement(EMPTY);
      }
      for (int term : terms) {
        Node node = nodes.get(term);
        if (node.op() == Op.COMPLEMENT && terms.contains(node.left())) {
          simplifications++;
          return op == Op.INTERSECT ? EMPTY : complement(EMPTY);
        }
      }
      int result = -1;
      for (int term : terms) result = result < 0 ? term : raw(op, result, term, -1);
      return result;
    }

    private void collect(Op op, int id, TreeSet<Integer> terms) {
      Node node = nodes.get(id);
      if (node.op() == op) {
        collect(op, node.left(), terms);
        collect(op, node.right(), terms);
      } else {
        terms.add(id);
      }
    }

    private int concat(int left, int right) {
      if (left == EMPTY || right == EMPTY) {
        simplifications++;
        return EMPTY;
      }
      if (left == EPSILON) {
        simplifications++;
        return right;
      }
      if (right == EPSILON) {
        simplifications++;
        return left;
      }
      ArrayList<Integer> terms = new ArrayList<>();
      collectConcat(left, terms);
      collectConcat(right, terms);
      int result = EPSILON;
      for (int term : terms) {
        result = result == EPSILON ? term : raw(Op.CONCAT, result, term, -1);
      }
      return result;
    }

    private void collectConcat(int id, List<Integer> terms) {
      Node node = nodes.get(id);
      if (node.op() == Op.CONCAT) {
        collectConcat(node.left(), terms);
        collectConcat(node.right(), terms);
      } else if (id != EPSILON) {
        terms.add(id);
      }
    }

    private int raw(Op op, int left, int right, int predicate) {
      Key key = new Key(op, left, right, predicate);
      Integer found = intern.get(key);
      if (found != null) return found;
      if (nodes.size() >= limits.maxCanonicalNodes()) {
        throw new IllegalArgumentException("canonical node budget exceeded");
      }
      int id = nodes.size();
      nodes.add(new Node(op, left, right, predicate));
      nullability.add((byte) -1);
      intern.put(key, id);
      return id;
    }

    boolean nullable(int id) {
      byte cached = nullability.get(id);
      if (cached >= 0) return cached != 0;
      Node node = nodes.get(id);
      boolean result =
          switch (node.op()) {
            case EMPTY, UNIT -> false;
            case EPSILON, STAR -> true;
            case ALT -> nullable(node.left()) || nullable(node.right());
            case CONCAT, INTERSECT -> nullable(node.left()) && nullable(node.right());
            case COMPLEMENT -> !nullable(node.left());
          };
      nullability.set(id, (byte) (result ? 1 : 0));
      return result;
    }

    int derive(int id, int symbol) {
      long key = ((long) id << 32) | Integer.toUnsignedLong(symbol);
      Integer cached = derivatives.get(key);
      if (cached != null) return cached;
      derivativeComputations++;
      Node node = nodes.get(id);
      int result =
          switch (node.op()) {
            case EMPTY, EPSILON -> EMPTY;
            case UNIT -> predicateClasses[node.predicate()][symbol] ? EPSILON : EMPTY;
            case ALT -> alt(derive(node.left(), symbol), derive(node.right(), symbol));
            case INTERSECT -> and(derive(node.left(), symbol), derive(node.right(), symbol));
            case CONCAT -> {
              int first = concat(derive(node.left(), symbol), node.right());
              yield nullable(node.left())
                  ? alt(first, derive(node.right(), symbol))
                  : first;
            }
            case STAR -> concat(derive(node.left(), symbol), id);
            case COMPLEMENT -> complement(derive(node.left(), symbol));
          };
      derivatives.put(key, result);
      return result;
    }
  }

  private record Frozen(
      int startState, int classes, int[] transitions, byte[] accepting) {}

  private static Frozen freeze(
      Arena arena,
      int root,
      int classes,
      Limits limits,
      M3Progress monitor) {
    if (classes < 1) throw new IllegalArgumentException("empty predicate alphabet");
    ArrayList<Integer> expressions = new ArrayList<>();
    HashMap<Integer, Integer> states = new HashMap<>();
    ArrayList<int[]> rows = new ArrayList<>();
    expressions.add(root);
    states.put(root, 0);
    long cells = 0;
    for (int state = 0; state < expressions.size(); state++) {
      M3RegexPredicateAlphabet.check(monitor);
      if (expressions.size() > limits.maxDerivativeStates()) {
        throw new IllegalArgumentException("state budget exceeded");
      }
      int[] row = new int[classes];
      for (int symbol = 0; symbol < classes; symbol++) {
        if (++cells > limits.maxDerivativeCells()) {
          throw new IllegalArgumentException("transition budget exceeded");
        }
        int targetExpression = arena.derive(expressions.get(state), symbol);
        Integer target = states.get(targetExpression);
        if (target == null) {
          if (expressions.size() == limits.maxDerivativeStates()) {
            throw new IllegalArgumentException("state budget exceeded");
          }
          target = expressions.size();
          expressions.add(targetExpression);
          states.put(targetExpression, target);
        }
        row[symbol] = target;
      }
      rows.add(row);
    }
    int[] transitions = new int[Math.multiplyExact(expressions.size(), classes)];
    byte[] accepting = new byte[expressions.size()];
    for (int state = 0; state < expressions.size(); state++) {
      System.arraycopy(rows.get(state), 0, transitions, state * classes, classes);
      accepting[state] = (byte) (arena.nullable(expressions.get(state)) ? 1 : 0);
    }
    return new Frozen(0, classes, transitions, accepting);
  }

  private static Frozen minimize(Frozen input, M3Progress monitor) {
    int states = input.accepting().length;
    int classes = input.classes();
    int[] groups = new int[states];
    for (int state = 0; state < states; state++) {
      groups[state] = input.accepting()[state];
    }

    for (int pass = 0; pass < states; pass++) {
      M3RegexPredicateAlphabet.check(monitor);
      LinkedHashMap<IntVectorKey, Integer> ids = new LinkedHashMap<>();
      int[] next = new int[states];
      for (int state = 0; state < states; state++) {
        int[] signature = new int[classes + 1];
        signature[0] = input.accepting()[state];
        for (int symbol = 0; symbol < classes; symbol++) {
          signature[symbol + 1] =
              groups[input.transitions()[state * classes + symbol]];
        }
        next[state] =
            ids.computeIfAbsent(new IntVectorKey(signature), ignored -> ids.size());
      }
      if (Arrays.equals(groups, next)) break;
      groups = next;
    }

    int groupCount = Arrays.stream(groups).max().orElse(-1) + 1;
    int[] representative = new int[groupCount];
    Arrays.fill(representative, -1);
    for (int state = 0; state < states; state++) {
      if (representative[groups[state]] < 0) representative[groups[state]] = state;
    }

    int[] transitions = new int[Math.multiplyExact(groupCount, classes)];
    byte[] accepting = new byte[groupCount];
    for (int group = 0; group < groupCount; group++) {
      int state = representative[group];
      if (state < 0) throw new IllegalStateException("non-contiguous DFA partition");
      accepting[group] = input.accepting()[state];
      for (int symbol = 0; symbol < classes; symbol++) {
        transitions[group * classes + symbol] =
            groups[input.transitions()[state * classes + symbol]];
      }
    }
    return new Frozen(groups[input.startState()], classes, transitions, accepting);
  }

  private record Quotient(int classes, int[] classMap, int[] transitions) {}

  private static Quotient quotientClasses(
      Frozen input, int alphabetClasses, M3Progress monitor) {
    if (input.classes() != alphabetClasses) {
      throw new IllegalArgumentException("alphabet class drift");
    }

    LinkedHashMap<IntVectorKey, Integer> ids = new LinkedHashMap<>();
    int[] classMap = new int[alphabetClasses];
    ArrayList<Integer> representatives = new ArrayList<>();
    for (int symbol = 0; symbol < alphabetClasses; symbol++) {
      if ((symbol & 255) == 0) M3RegexPredicateAlphabet.check(monitor);
      int[] signature = new int[input.accepting().length];
      for (int state = 0; state < signature.length; state++) {
        signature[state] =
            input.transitions()[state * alphabetClasses + symbol];
      }
      IntVectorKey key = new IntVectorKey(signature);
      Integer id = ids.get(key);
      if (id == null) {
        id = ids.size();
        ids.put(key, id);
        representatives.add(symbol);
      }
      classMap[symbol] = id;
    }

    int classes = representatives.size();
    int[] transitions =
        new int[Math.multiplyExact(input.accepting().length, classes)];
    for (int state = 0; state < input.accepting().length; state++) {
      for (int symbol = 0; symbol < classes; symbol++) {
        transitions[state * classes + symbol] =
            input.transitions()[
                state * alphabetClasses + representatives.get(symbol)];
      }
    }
    return new Quotient(classes, classMap, transitions);
  }

  private static byte[] computeLive(
      int[] transitions,
      byte[] accepting,
      int classes,
      M3Progress monitor) {
    int states = accepting.length;
    int[] counts = new int[states];
    for (int target : transitions) counts[target]++;

    int[][] predecessors = new int[states][];
    for (int state = 0; state < states; state++) {
      predecessors[state] = new int[counts[state]];
    }
    Arrays.fill(counts, 0);
    for (int source = 0; source < states; source++) {
      for (int symbol = 0; symbol < classes; symbol++) {
        int target = transitions[source * classes + symbol];
        predecessors[target][counts[target]++] = source;
      }
    }

    byte[] live = new byte[states];
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    for (int state = 0; state < states; state++) {
      if (accepting[state] != 0) {
        live[state] = 1;
        queue.addLast(state);
      }
    }
    while (!queue.isEmpty()) {
      M3RegexPredicateAlphabet.check(monitor);
      int target = queue.removeFirst();
      for (int source : predecessors[target]) {
        if (live[source] == 0) {
          live[source] = 1;
          queue.addLast(source);
        }
      }
    }
    return live;
  }

  private static long[] computeViableClasses(
      int[] transitions, byte[] live, int classes, int wordsPerState) {
    int states = live.length;
    long[] result =
        new long[Math.multiplyExact(states, wordsPerState)];
    for (int state = 0; state < states; state++) {
      for (int symbol = 0; symbol < classes; symbol++) {
        if (live[transitions[state * classes + symbol]] != 0) {
          result[state * wordsPerState + (symbol >>> 6)] |=
              1L << (symbol & 63);
        }
      }
    }
    return result;
  }

  private record PowerTable(int bits, int[] cells) {}

  private static PowerTable buildRunPowers(
      int[] transitions,
      int states,
      int classes,
      long maxCells,
      M3Progress monitor) {
    long planeCells = Math.multiplyExact((long) states, classes);
    if (planeCells > maxCells) {
      throw new IllegalArgumentException(
          "run-power base exceeds primitive-cell budget");
    }
    int bits =
        (int) Math.min(31L, Math.max(1L, maxCells / planeCells));
    int plane = Math.multiplyExact(states, classes);
    int[] powers =
        new int[Math.toIntExact(Math.multiplyExact(planeCells, bits))];

    for (int executionClass = 0; executionClass < classes; executionClass++) {
      for (int state = 0; state < states; state++) {
        powers[executionClass * states + state] =
            transitions[state * classes + executionClass];
      }
    }

    for (int bit = 1; bit < bits; bit++) {
      M3RegexPredicateAlphabet.check(monitor);
      int previous = (bit - 1) * plane;
      int current = bit * plane;
      for (int executionClass = 0;
          executionClass < classes;
          executionClass++) {
        int classBase = executionClass * states;
        for (int state = 0; state < states; state++) {
          int middle = powers[previous + classBase + state];
          powers[current + classBase + state] =
              powers[previous + classBase + middle];
        }
      }
    }
    return new PowerTable(bits, powers);
  }

  private static final class IntVectorKey {
    private final int[] values;
    private final int hash;
    IntVectorKey(int[] values) {
      this.values = values;
      this.hash = Arrays.hashCode(values);
    }
    @Override public int hashCode() { return hash; }
    @Override public boolean equals(Object other) {
      return other instanceof IntVectorKey key
          && Arrays.equals(values, key.values);
    }
  }

  private static String sha256(
      int startState,
      int[] classMap,
      int[] transitions,
      byte[] accepting,
      byte[] live,
      int powerBits,
      int[] powers,
      List<UnitSet> predicates) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      updateInt(digest, startState);
      updateInt(digest, powerBits);
      updateInts(digest, classMap);
      updateInts(digest, transitions);
      digest.update(accepting);
      digest.update(live);
      updateInts(digest, powers);
      for (UnitSet predicate : predicates) {
        byte[] text =
            predicate.javaRegex().getBytes(StandardCharsets.UTF_8);
        updateInt(digest, text.length);
        digest.update(text);
      }
      return hex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void updateInt(MessageDigest digest, int value) {
    digest.update(
        ByteBuffer.allocate(Integer.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(value)
            .array());
  }

  private static void updateInts(MessageDigest digest, int[] values) {
    ByteBuffer buffer = ByteBuffer.allocate(4096).order(ByteOrder.BIG_ENDIAN);
    for (int value : values) {
      if (buffer.remaining() < Integer.BYTES) {
        digest.update(buffer.array(), 0, buffer.position());
        buffer.clear();
      }
      buffer.putInt(value);
    }
    if (buffer.position() > 0) {
      digest.update(buffer.array(), 0, buffer.position());
    }
  }

  private static String hex(byte[] input) {
    char[] digits = "0123456789abcdef".toCharArray();
    char[] output = new char[input.length * 2];
    for (int index = 0; index < input.length; index++) {
      int value = input[index] & 0xff;
      output[2 * index] = digits[value >>> 4];
      output[2 * index + 1] = digits[value & 15];
    }
    return new String(output);
  }
}
