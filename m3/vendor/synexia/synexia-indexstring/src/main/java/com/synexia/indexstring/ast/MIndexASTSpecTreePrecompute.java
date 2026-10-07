// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexGrammarParseTree;
import com.synexia.indexstring.grammar.MIndexGrammarSymbolKind;
import com.synexia.indexstring.grammar.MIndexTokenClass;
import com.synexia.indexstring.grammar.MIndexTokenStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Language-neutral AST precompute over one formal grammar parse tree.
 *
 * <p>Non-terminal syntax-kind codes are production IDs. Terminal syntax-kind codes have the sign
 * bit set and carry the grammar symbol ID in the low 31 bits. This gives SQL, NoSQL, JSON query,
 * programming-expression and custom DSL trees the same stable integer AST coordinate model without
 * depending on Java Tree.Kind.</p>
 */
public final class MIndexASTSpecTreePrecompute {
  private static final int VALUE_MASK = Integer.MAX_VALUE;
  private static final long HASH_SEED = 0x4d494e4153545452L;
  private static final long STRUCTURAL_SEED = 0x5354525543545552L;
  private static final long LOGIC_SEED = 0x4c4f474943415354L;
  private static final int MAX_SNAPSHOT_NODES = 16_000_000;
  private static final int MAX_SNAPSHOT_SYMBOLS = 16_000_000;
  private static final int MAX_SNAPSHOT_PRODUCTIONS = 16_000_000;

  private final MIndexASTSpecPrecompute spec;
  private final MIndexGrammarParseTree tree;
  private final int nodeCount;
  private final int root;
  private final int[] parents;
  private final int[] childOrdinals;
  private final int[] preorderToNode;
  private final int[] nodeToPreorder;
  private final int[] subtreeSizes;
  private final int[] depths;
  private final int[] leafCounts;
  private final int levels;
  private final int[] ancestors;
  private final int[] kindCodes;
  private final long[] exactHashes;
  private final long[] structuralHashes;
  private final long[] logicHashes;
  private final long[] symbolSignals;
  private final int[] symbolOffsets;
  private final int[] nodesBySymbol;
  private final int[] preordersBySymbol;
  private final int[] productionOffsets;
  private final int[] nodesByProduction;
  private final int[] preordersByProduction;

  private MIndexASTSpecTreePrecompute(
      MIndexASTSpecPrecompute spec,
      MIndexGrammarParseTree tree,
      int root,
      int[] parents,
      int[] childOrdinals,
      int[] preorderToNode,
      int[] nodeToPreorder,
      int[] subtreeSizes,
      int[] depths,
      int[] leafCounts,
      int levels,
      int[] ancestors,
      int[] kindCodes,
      long[] exactHashes,
      long[] structuralHashes,
      long[] logicHashes,
      long[] symbolSignals,
      int[] symbolOffsets,
      int[] nodesBySymbol,
      int[] preordersBySymbol,
      int[] productionOffsets,
      int[] nodesByProduction,
      int[] preordersByProduction) {
    this.spec = spec;
    this.tree = tree;
    this.nodeCount = tree.nodeCount();
    this.root = root;
    this.parents = parents;
    this.childOrdinals = childOrdinals;
    this.preorderToNode = preorderToNode;
    this.nodeToPreorder = nodeToPreorder;
    this.subtreeSizes = subtreeSizes;
    this.depths = depths;
    this.leafCounts = leafCounts;
    this.levels = levels;
    this.ancestors = ancestors;
    this.kindCodes = kindCodes;
    this.exactHashes = exactHashes;
    this.structuralHashes = structuralHashes;
    this.logicHashes = logicHashes;
    this.symbolSignals = symbolSignals;
    this.symbolOffsets = symbolOffsets;
    this.nodesBySymbol = nodesBySymbol;
    this.preordersBySymbol = preordersBySymbol;
    this.productionOffsets = productionOffsets;
    this.nodesByProduction = nodesByProduction;
    this.preordersByProduction = preordersByProduction;
  }

  /**
   * Detached primitive state for this parse-tree precompute.
   *
   * <p>The packet is a transfer boundary, not a serialized grammar or token-stream image. Its
   * arrays are package-private so the owner can validate hostile restore inputs without exposing
   * mutable state as a public API.</p>
   */
  public static final class Snapshot {
    private final int nodeCount;
    private final int root;
    private final int[] parents;
    private final int[] childOrdinals;
    private final int[] preorderToNode;
    private final int[] nodeToPreorder;
    private final int[] subtreeSizes;
    private final int[] depths;
    private final int[] leafCounts;
    private final int levels;
    private final int[] ancestors;
    private final int[] kindCodes;
    private final long[] exactHashes;
    private final long[] structuralHashes;
    private final long[] logicHashes;
    private final long[] symbolSignals;
    private final int[] symbolOffsets;
    private final int[] nodesBySymbol;
    private final int[] preordersBySymbol;
    private final int[] productionOffsets;
    private final int[] nodesByProduction;
    private final int[] preordersByProduction;

    Snapshot(
        int nodeCount,
        int root,
        int[] parents,
        int[] childOrdinals,
        int[] preorderToNode,
        int[] nodeToPreorder,
        int[] subtreeSizes,
        int[] depths,
        int[] leafCounts,
        int levels,
        int[] ancestors,
        int[] kindCodes,
        long[] exactHashes,
        long[] structuralHashes,
        long[] logicHashes,
        long[] symbolSignals,
        int[] symbolOffsets,
        int[] nodesBySymbol,
        int[] preordersBySymbol,
        int[] productionOffsets,
        int[] nodesByProduction,
        int[] preordersByProduction) {
      this.nodeCount = nodeCount;
      this.root = root;
      this.parents = parents;
      this.childOrdinals = childOrdinals;
      this.preorderToNode = preorderToNode;
      this.nodeToPreorder = nodeToPreorder;
      this.subtreeSizes = subtreeSizes;
      this.depths = depths;
      this.leafCounts = leafCounts;
      this.levels = levels;
      this.ancestors = ancestors;
      this.kindCodes = kindCodes;
      this.exactHashes = exactHashes;
      this.structuralHashes = structuralHashes;
      this.logicHashes = logicHashes;
      this.symbolSignals = symbolSignals;
      this.symbolOffsets = symbolOffsets;
      this.nodesBySymbol = nodesBySymbol;
      this.preordersBySymbol = preordersBySymbol;
      this.productionOffsets = productionOffsets;
      this.nodesByProduction = nodesByProduction;
      this.preordersByProduction = preordersByProduction;
    }

    int nodeCount() { return nodeCount; }
    int root() { return root; }
    int[] parents() { return parents; }
    int[] childOrdinals() { return childOrdinals; }
    int[] preorderToNode() { return preorderToNode; }
    int[] nodeToPreorder() { return nodeToPreorder; }
    int[] subtreeSizes() { return subtreeSizes; }
    int[] depths() { return depths; }
    int[] leafCounts() { return leafCounts; }
    int levels() { return levels; }
    int[] ancestors() { return ancestors; }
    int[] kindCodes() { return kindCodes; }
    long[] exactHashes() { return exactHashes; }
    long[] structuralHashes() { return structuralHashes; }
    long[] logicHashes() { return logicHashes; }
    long[] symbolSignals() { return symbolSignals; }
    int[] symbolOffsets() { return symbolOffsets; }
    int[] nodesBySymbol() { return nodesBySymbol; }
    int[] preordersBySymbol() { return preordersBySymbol; }
    int[] productionOffsets() { return productionOffsets; }
    int[] nodesByProduction() { return nodesByProduction; }
    int[] preordersByProduction() { return preordersByProduction; }
  }

  /** Returns a detached copy of all primitive query geometry. */
  public Snapshot snapshot() {
    return new Snapshot(
        nodeCount,
        root,
        parents.clone(),
        childOrdinals.clone(),
        preorderToNode.clone(),
        nodeToPreorder.clone(),
        subtreeSizes.clone(),
        depths.clone(),
        leafCounts.clone(),
        levels,
        ancestors.clone(),
        kindCodes.clone(),
        exactHashes.clone(),
        structuralHashes.clone(),
        logicHashes.clone(),
        symbolSignals.clone(),
        symbolOffsets.clone(),
        nodesBySymbol.clone(),
        preordersBySymbol.clone(),
        productionOffsets.clone(),
        nodesByProduction.clone(),
        preordersByProduction.clone());
  }

  /**
   * Restores a snapshot against the supplied immutable grammar and parse tree.
   *
   * <p>Validation derives geometry from the existing tree and compares every scalar and primitive
   * lane before cloning the packet into a live precompute. It never reparses source text.</p>
   */
  public static MIndexASTSpecTreePrecompute restore(
      MIndexASTSpecPrecompute spec,
      MIndexGrammarParseTree tree,
      Snapshot payload) {
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(tree, "tree");
    Objects.requireNonNull(payload, "payload");
    validateSnapshot(spec, tree, payload);
    return new MIndexASTSpecTreePrecompute(
        spec,
        tree,
        payload.root,
        payload.parents.clone(),
        payload.childOrdinals.clone(),
        payload.preorderToNode.clone(),
        payload.nodeToPreorder.clone(),
        payload.subtreeSizes.clone(),
        payload.depths.clone(),
        payload.leafCounts.clone(),
        payload.levels,
        payload.ancestors.clone(),
        payload.kindCodes.clone(),
        payload.exactHashes.clone(),
        payload.structuralHashes.clone(),
        payload.logicHashes.clone(),
        payload.symbolSignals.clone(),
        payload.symbolOffsets.clone(),
        payload.nodesBySymbol.clone(),
        payload.preordersBySymbol.clone(),
        payload.productionOffsets.clone(),
        payload.nodesByProduction.clone(),
        payload.preordersByProduction.clone());
  }

  private static void validateSnapshot(
      MIndexASTSpecPrecompute spec,
      MIndexGrammarParseTree tree,
      Snapshot payload) {
    int n = tree.nodeCount();
    if (n == 0 || n > MAX_SNAPSHOT_NODES) {
      throw new IllegalArgumentException("grammar parse tree node count is out of bounds");
    }
    if (spec.symbolCount() > MAX_SNAPSHOT_SYMBOLS
        || spec.productionCount() > MAX_SNAPSHOT_PRODUCTIONS) {
      throw new IllegalArgumentException("grammar spec dimensions are out of bounds");
    }
    int expectedLevels = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(n));
    int nonTerminalNodes = 0;
    for (int node = 0; node < n; node++) if (!tree.terminal(node)) nonTerminalNodes++;

    if (payload.nodeCount != n
        || payload.root != tree.root()
        || payload.levels != expectedLevels
        || payload.parents == null
        || payload.parents.length != n
        || payload.childOrdinals == null
        || payload.childOrdinals.length != n
        || payload.preorderToNode == null
        || payload.preorderToNode.length != n
        || payload.nodeToPreorder == null
        || payload.nodeToPreorder.length != n
        || payload.subtreeSizes == null
        || payload.subtreeSizes.length != n
        || payload.depths == null
        || payload.depths.length != n
        || payload.leafCounts == null
        || payload.leafCounts.length != n
        || payload.ancestors == null
        || payload.ancestors.length != Math.multiplyExact(expectedLevels, n)
        || payload.kindCodes == null
        || payload.kindCodes.length != n
        || payload.exactHashes == null
        || payload.exactHashes.length != n
        || payload.structuralHashes == null
        || payload.structuralHashes.length != n
        || payload.logicHashes == null
        || payload.logicHashes.length != n
        || payload.symbolSignals == null
        || payload.symbolSignals.length != n
        || payload.symbolOffsets == null
        || payload.symbolOffsets.length != spec.symbolCount() + 1
        || payload.nodesBySymbol == null
        || payload.nodesBySymbol.length != n
        || payload.preordersBySymbol == null
        || payload.preordersBySymbol.length != n
        || payload.productionOffsets == null
        || payload.productionOffsets.length != spec.productionCount() + 1
        || payload.nodesByProduction == null
        || payload.nodesByProduction.length != nonTerminalNodes
        || payload.preordersByProduction == null
        || payload.preordersByProduction.length != nonTerminalNodes) {
      throw new IllegalArgumentException("AST spec-tree snapshot shape mismatch");
    }

    int root = tree.root();
    int[] parents = new int[n];
    int[] childOrdinals = new int[n];
    Arrays.fill(parents, -1);
    Arrays.fill(childOrdinals, -1);
    for (int parent = 0; parent < n; parent++) {
      for (int ordinal = 0; ordinal < tree.childCount(parent); ordinal++) {
        int child = tree.child(parent, ordinal);
        if (parents[child] != -1) {
          throw new IllegalArgumentException("parse tree parent is not unique");
        }
        parents[child] = parent;
        childOrdinals[child] = ordinal;
      }
    }
    if (parents[root] != -1) throw new IllegalArgumentException("parse tree root has a parent");
    for (int node = 0; node < n; node++) {
      if (node != root && parents[node] < 0) {
        throw new IllegalArgumentException("parse tree node is disconnected");
      }
      if (payload.parents[node] != parents[node]
          || payload.childOrdinals[node] != childOrdinals[node]) {
        throw new IllegalArgumentException("AST spec-tree parent geometry mismatch");
      }
    }

    boolean[] seen = new boolean[n];
    for (int preorder = 0; preorder < n; preorder++) {
      int node = payload.preorderToNode[preorder];
      if (node < 0 || node >= n || seen[node]) {
        throw new IllegalArgumentException("AST spec-tree preorder is not a permutation");
      }
      seen[node] = true;
      if (payload.nodeToPreorder[node] != preorder) {
        throw new IllegalArgumentException("AST spec-tree preorder inverse mismatch");
      }
    }
    if (payload.preorderToNode[0] != root) {
      throw new IllegalArgumentException("AST spec-tree root preorder mismatch");
    }

    int[] depths = new int[n];
    for (int preorder = 0; preorder < n; preorder++) {
      int node = payload.preorderToNode[preorder];
      int parent = parents[node];
      depths[node] = parent < 0 ? 0 : Math.addExact(depths[parent], 1);
      if (payload.depths[node] != depths[node]) {
        throw new IllegalArgumentException("AST spec-tree depth mismatch");
      }
    }

    int[] symbolCounts = new int[spec.symbolCount()];
    int[] productionCounts = new int[spec.productionCount()];
    MIndexTokenStream tokens = tree.tokens();
    int[] subtreeSizes = new int[n];
    int[] leafCounts = new int[n];
    long[] symbolSignals = new long[n];
    for (int preorder = n - 1; preorder >= 0; preorder--) {
      int node = payload.preorderToNode[preorder];
      int symbol = tree.symbol(node);
      symbolCounts[symbol]++;
      int production = tree.production(node);
      boolean terminal = tree.terminal(node);
      int expectedKind;
      if (terminal) {
        if (production >= 0) throw new IllegalArgumentException("terminal production mismatch");
        expectedKind = spec.productionPlans().syntaxKindCodeForTerminal(symbol);
      } else {
        if (production < 0 || spec.productionLhs(production) != symbol) {
          throw new IllegalArgumentException("non-terminal production mismatch");
        }
        productionCounts[production]++;
        expectedKind = spec.productionPlans().syntaxKindCodeForProduction(production);
      }
      if (payload.kindCodes[node] != expectedKind) {
        throw new IllegalArgumentException("AST spec-tree kind mismatch");
      }

      int children = tree.childCount(node);
      subtreeSizes[node] = 1;
      leafCounts[node] = children == 0 ? 1 : 0;
      long exact =
          terminal
              ? mix(HASH_SEED, spec.symbolHash64(symbol))
              : mix(HASH_SEED, spec.productionHash64(production));
      long structural =
          terminal
              ? mix(STRUCTURAL_SEED, spec.symbolHash64(symbol))
              : mix(STRUCTURAL_SEED, spec.productionHash64(production));
      long logic =
          terminal
              ? mix(LOGIC_SEED, spec.symbolHash64(symbol))
              : mix(LOGIC_SEED, spec.productionHash64(production));
      long signal = symbolBit(symbol);
      if (terminal) {
        long tokenHash = stableTokenHash(tokens, tree.tokenIndex(node));
        exact = mix(exact, tokenHash);
        if (logicTokenContentRelevant(spec, symbol)) logic = mix(logic, tokenHash);
      }
      for (int ordinal = 0; ordinal < children; ordinal++) {
        int child = tree.child(node, ordinal);
        subtreeSizes[node] = Math.addExact(subtreeSizes[node], subtreeSizes[child]);
        leafCounts[node] = Math.addExact(leafCounts[node], leafCounts[child]);
        exact = mix(exact, payload.exactHashes[child]);
        structural = mix(structural, payload.structuralHashes[child]);
        logic = mix(logic, payload.logicHashes[child]);
        signal |= payload.symbolSignals[child];
      }
      if (payload.subtreeSizes[node] != subtreeSizes[node]
          || payload.leafCounts[node] != leafCounts[node]
          || payload.exactHashes[node] != avalanche(exact ^ children)
          || payload.structuralHashes[node] != avalanche(structural ^ children)
          || payload.logicHashes[node] != avalanche(logic ^ children)
          || payload.symbolSignals[node] != signal) {
        throw new IllegalArgumentException("AST spec-tree derived geometry mismatch");
      }
    }

    for (int node = 0; node < n; node++) {
      if (payload.ancestors[node] != parents[node]) {
        throw new IllegalArgumentException("AST spec-tree ancestor level zero mismatch");
      }
    }
    for (int level = 1; level < expectedLevels; level++) {
      int previous = (level - 1) * n;
      int base = level * n;
      for (int node = 0; node < n; node++) {
        int half = payload.ancestors[previous + node];
        if (half < -1 || half >= n) {
          throw new IllegalArgumentException("AST spec-tree ancestor is out of range");
        }
        int expected = half < 0 ? -1 : payload.ancestors[previous + half];
        if (payload.ancestors[base + node] != expected) {
          throw new IllegalArgumentException("AST spec-tree ancestor table mismatch");
        }
      }
    }

    int[] symbolOffsets = offsets(symbolCounts);
    int[] productionOffsets = offsets(productionCounts);
    if (!Arrays.equals(payload.symbolOffsets, symbolOffsets)
        || !Arrays.equals(payload.productionOffsets, productionOffsets)) {
      throw new IllegalArgumentException("AST spec-tree posting offsets mismatch");
    }
    int[] symbolCursor = Arrays.copyOf(symbolOffsets, symbolCounts.length);
    int[] symbolPreorderCursor = Arrays.copyOf(symbolOffsets, symbolCounts.length);
    int[] productionCursor = Arrays.copyOf(productionOffsets, productionCounts.length);
    int[] productionPreorderCursor = Arrays.copyOf(productionOffsets, productionCounts.length);
    for (int node = 0; node < n; node++) {
      int symbol = tree.symbol(node);
      if (payload.nodesBySymbol[symbolCursor[symbol]++] != node) {
        throw new IllegalArgumentException("AST spec-tree symbol postings mismatch");
      }
      int production = tree.production(node);
      if (production >= 0 && payload.nodesByProduction[productionCursor[production]++] != node) {
        throw new IllegalArgumentException("AST spec-tree production postings mismatch");
      }
    }
    for (int preorder = 0; preorder < n; preorder++) {
      int node = payload.preorderToNode[preorder];
      int symbol = tree.symbol(node);
      if (payload.preordersBySymbol[symbolPreorderCursor[symbol]++] != preorder) {
        throw new IllegalArgumentException("AST spec-tree symbol preorder postings mismatch");
      }
      int production = tree.production(node);
      if (production >= 0
          && payload.preordersByProduction[productionPreorderCursor[production]++] != preorder) {
        throw new IllegalArgumentException("AST spec-tree production preorder postings mismatch");
      }
    }
  }

  static MIndexASTSpecTreePrecompute build(
      MIndexASTSpecPrecompute spec, MIndexGrammarParseTree tree) {
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(tree, "tree");
    int n = tree.nodeCount();
    if (n == 0) throw new IllegalArgumentException("grammar parse tree has no nodes");

    int root = tree.root();
    int[] parents = new int[n];
    int[] childOrdinals = new int[n];
    Arrays.fill(parents, -1);
    Arrays.fill(childOrdinals, -1);

    for (int parent = 0; parent < n; parent++) {
      int children = tree.childCount(parent);
      for (int ordinal = 0; ordinal < children; ordinal++) {
        int child = tree.child(parent, ordinal);
        if (parents[child] != -1) {
          throw new IllegalArgumentException("grammar parse structure is not a tree");
        }
        parents[child] = parent;
        childOrdinals[child] = ordinal;
      }
    }
    if (parents[root] != -1) throw new IllegalArgumentException("parse-tree root has a parent");
    for (int node = 0; node < n; node++) {
      if (node != root && parents[node] < 0) {
        throw new IllegalArgumentException("parse-tree node is disconnected: " + node);
      }
    }

    int[] preorderToNode = new int[n];
    int[] nodeToPreorder = new int[n];
    int[] depths = new int[n];
    int[] stack = new int[n];
    int top = 0;
    int preorder = 0;
    stack[top++] = root;
    while (top != 0) {
      int node = stack[--top];
      preorderToNode[preorder] = node;
      nodeToPreorder[node] = preorder++;
      int children = tree.childCount(node);
      for (int ordinal = children - 1; ordinal >= 0; ordinal--) {
        int child = tree.child(node, ordinal);
        depths[child] = Math.addExact(depths[node], 1);
        stack[top++] = child;
      }
    }
    if (preorder != n) throw new IllegalArgumentException("parse tree traversal did not cover all nodes");

    int[] subtreeSizes = new int[n];
    int[] leafCounts = new int[n];
    int[] kindCodes = new int[n];
    long[] exactHashes = new long[n];
    long[] structuralHashes = new long[n];
    long[] logicHashes = new long[n];
    long[] symbolSignals = new long[n];

    int[] symbolCounts = new int[spec.symbolCount()];
    int[] productionCounts = new int[spec.productionCount()];
    MIndexTokenStream tokens = tree.tokens();

    for (int preorderIndex = n - 1; preorderIndex >= 0; preorderIndex--) {
      int node = preorderToNode[preorderIndex];
      int symbol = tree.symbol(node);
      symbolCounts[symbol]++;

      int production = tree.production(node);
      boolean terminal = tree.terminal(node);
      if (terminal) {
        if (production >= 0) throw new IllegalArgumentException("terminal node has a production");
        kindCodes[node] = spec.productionPlans().syntaxKindCodeForTerminal(symbol);
      } else {
        if (production < 0) throw new IllegalArgumentException("non-terminal node has no production");
        if (spec.productionLhs(production) != symbol) {
          throw new IllegalArgumentException("node production LHS does not match node symbol");
        }
        productionCounts[production]++;
        kindCodes[node] =
            spec.productionPlans().syntaxKindCodeForProduction(production);
      }

      int children = tree.childCount(node);
      subtreeSizes[node] = 1;
      leafCounts[node] = children == 0 ? 1 : 0;

      long exact =
          terminal
              ? mix(HASH_SEED, spec.symbolHash64(symbol))
              : mix(HASH_SEED, spec.productionHash64(production));
      long structural =
          terminal
              ? mix(STRUCTURAL_SEED, spec.symbolHash64(symbol))
              : mix(STRUCTURAL_SEED, spec.productionHash64(production));
      long logic =
          terminal
              ? mix(LOGIC_SEED, spec.symbolHash64(symbol))
              : mix(LOGIC_SEED, spec.productionHash64(production));
      long signal = symbolBit(symbol);

      if (terminal) {
        int tokenIndex = tree.tokenIndex(node);
        long tokenHash = stableTokenHash(tokens, tokenIndex);
        exact = mix(exact, tokenHash);
        if (logicTokenContentRelevant(spec, symbol)) logic = mix(logic, tokenHash);
      }

      for (int ordinal = 0; ordinal < children; ordinal++) {
        int child = tree.child(node, ordinal);
        subtreeSizes[node] = Math.addExact(subtreeSizes[node], subtreeSizes[child]);
        leafCounts[node] = Math.addExact(leafCounts[node], leafCounts[child]);
        exact = mix(exact, exactHashes[child]);
        structural = mix(structural, structuralHashes[child]);
        logic = mix(logic, logicHashes[child]);
        signal |= symbolSignals[child];
      }

      exactHashes[node] = avalanche(exact ^ children);
      structuralHashes[node] = avalanche(structural ^ children);
      logicHashes[node] = avalanche(logic ^ children);
      symbolSignals[node] = signal;
    }

    int levels = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(n));
    int[] ancestors = new int[Math.multiplyExact(levels, n)];
    Arrays.fill(ancestors, -1);
    System.arraycopy(parents, 0, ancestors, 0, n);
    for (int level = 1; level < levels; level++) {
      int previous = (level - 1) * n;
      int base = level * n;
      for (int node = 0; node < n; node++) {
        int half = ancestors[previous + node];
        ancestors[base + node] = half < 0 ? -1 : ancestors[previous + half];
      }
    }

    int[] symbolOffsets = offsets(symbolCounts);
    int[] nodesBySymbol = new int[n];
    int[] preordersBySymbol = new int[n];
    int[] symbolCursor = Arrays.copyOf(symbolOffsets, symbolCounts.length);
    int[] symbolPreorderCursor = Arrays.copyOf(symbolOffsets, symbolCounts.length);
    int[] productionOffsets = offsets(productionCounts);
    int nonTerminalNodes = n - terminalCount(tree);
    int[] nodesByProduction = new int[nonTerminalNodes];
    int[] preordersByProduction = new int[nonTerminalNodes];
    int[] productionCursor = Arrays.copyOf(productionOffsets, productionCounts.length);
    int[] productionPreorderCursor =
        Arrays.copyOf(productionOffsets, productionCounts.length);

    for (int node = 0; node < n; node++) {
      int symbol = tree.symbol(node);
      nodesBySymbol[symbolCursor[symbol]++] = node;
      int production = tree.production(node);
      if (production >= 0) nodesByProduction[productionCursor[production]++] = node;
    }

    for (int preorderIndex = 0; preorderIndex < n; preorderIndex++) {
      int node = preorderToNode[preorderIndex];
      int symbol = tree.symbol(node);
      preordersBySymbol[symbolPreorderCursor[symbol]++] = preorderIndex;
      int production = tree.production(node);
      if (production >= 0) {
        preordersByProduction[productionPreorderCursor[production]++] = preorderIndex;
      }
    }

    return new MIndexASTSpecTreePrecompute(
        spec,
        tree,
        root,
        parents,
        childOrdinals,
        preorderToNode,
        nodeToPreorder,
        subtreeSizes,
        depths,
        leafCounts,
        levels,
        ancestors,
        kindCodes,
        exactHashes,
        structuralHashes,
        logicHashes,
        symbolSignals,
        symbolOffsets,
        nodesBySymbol,
        preordersBySymbol,
        productionOffsets,
        nodesByProduction,
        preordersByProduction);
  }

  public MIndexASTSpecPrecompute spec() {
    return spec;
  }

  public MIndexGrammarParseTree tree() {
    return tree;
  }

  public int root() {
    return root;
  }

  public int nodeCount() {
    return nodeCount;
  }

  public int parent(int node) {
    return parents[checkNode(node)];
  }

  public int childOrdinal(int node) {
    return childOrdinals[checkNode(node)];
  }

  public int depth(int node) {
    return depths[checkNode(node)];
  }

  public int subtreeSize(int node) {
    return subtreeSizes[checkNode(node)];
  }

  public int expandedLeafCount(int node) {
    return leafCounts[checkNode(node)];
  }

  public int preorderIndex(int node) {
    return nodeToPreorder[checkNode(node)];
  }

  public int preorderNode(int preorderIndex) {
    return preorderToNode[Objects.checkIndex(preorderIndex, nodeCount)];
  }

  public int subtreeEndPreorderExclusive(int node) {
    int checked = checkNode(node);
    return nodeToPreorder[checked] + subtreeSizes[checked];
  }

  public boolean isAncestor(int ancestor, int descendant) {
    int checkedAncestor = checkNode(ancestor);
    int checkedDescendant = checkNode(descendant);
    int start = nodeToPreorder[checkedAncestor];
    int candidate = nodeToPreorder[checkedDescendant];
    return candidate >= start && candidate < start + subtreeSizes[checkedAncestor];
  }

  public int kthAncestor(int node, int distance) {
    int current = checkNode(node);
    if (distance < 0) throw new IllegalArgumentException("negative ancestor distance");
    int remaining = distance;
    int level = 0;
    while (remaining != 0 && current >= 0) {
      if ((remaining & 1) != 0) {
        if (level >= levels) return -1;
        current = ancestors[level * nodeCount + current];
      }
      remaining >>>= 1;
      level++;
    }
    return current;
  }

  public int lowestCommonAncestor(int left, int right) {
    int a = checkNode(left);
    int b = checkNode(right);
    if (isAncestor(a, b)) return a;
    if (isAncestor(b, a)) return b;
    int current = a;
    for (int level = levels - 1; level >= 0; level--) {
      int candidate = ancestors[level * nodeCount + current];
      if (candidate >= 0 && !isAncestor(candidate, b)) current = candidate;
    }
    return parents[current];
  }

  /**
   * Stable language-neutral syntax kind.
   *
   * <p>Non-negative values are production IDs. Negative values encode terminal grammar symbols.</p>
   */
  public int syntaxKindCode(int node) {
    return kindCodes[checkNode(node)];
  }

  public boolean terminalKind(int node) {
    return isTerminalKindCode(syntaxKindCode(node));
  }

  public long exactHash64(int node) {
    return exactHashes[checkNode(node)];
  }

  public long structuralHash64(int node) {
    return structuralHashes[checkNode(node)];
  }

  /**
   * Identifier/name-insensitive generic logic hash. Literal/value content remains relevant.
   *
   * <p>This is a candidate key, not semantic equivalence proof.</p>
   */
  public long logicHash64(int node) {
    return logicHashes[checkNode(node)];
  }

  public boolean mayContainSymbol(int node, int symbol) {
    int checkedNode = checkNode(node);
    Objects.checkIndex(symbol, spec.symbolCount());
    long bit = symbolBit(symbol);
    return (symbolSignals[checkedNode] & bit) == bit;
  }

  public int[] nodesForSymbol(int symbol) {
    int checked = Objects.checkIndex(symbol, spec.symbolCount());
    return Arrays.copyOfRange(
        nodesBySymbol, symbolOffsets[checked], symbolOffsets[checked + 1]);
  }

  public int[] nodesForProduction(int production) {
    int checked = Objects.checkIndex(production, spec.productionCount());
    return Arrays.copyOfRange(
        nodesByProduction,
        productionOffsets[checked],
        productionOffsets[checked + 1]);
  }

  /** Exact symbol occurrence count inside this node's subtree in O(log n). */
  public int subtreeSymbolCount(int node, int symbol) {
    int checkedNode = checkNode(node);
    int checkedSymbol = Objects.checkIndex(symbol, spec.symbolCount());
    int fromPreorder = nodeToPreorder[checkedNode];
    int toPreorder = fromPreorder + subtreeSizes[checkedNode];
    int from =
        lowerBound(
            preordersBySymbol,
            symbolOffsets[checkedSymbol],
            symbolOffsets[checkedSymbol + 1],
            fromPreorder);
    int to =
        lowerBound(
            preordersBySymbol,
            symbolOffsets[checkedSymbol],
            symbolOffsets[checkedSymbol + 1],
            toPreorder);
    return to - from;
  }

  /** Exact production occurrence count inside this node's subtree in O(log n). */
  public int subtreeProductionCount(int node, int production) {
    int checkedNode = checkNode(node);
    int checkedProduction = Objects.checkIndex(production, spec.productionCount());
    int fromPreorder = nodeToPreorder[checkedNode];
    int toPreorder = fromPreorder + subtreeSizes[checkedNode];
    int from =
        lowerBound(
            preordersByProduction,
            productionOffsets[checkedProduction],
            productionOffsets[checkedProduction + 1],
            fromPreorder);
    int to =
        lowerBound(
            preordersByProduction,
            productionOffsets[checkedProduction],
            productionOffsets[checkedProduction + 1],
            toPreorder);
    return to - from;
  }

  /** Exact counterpart of the Bloom-like mayContainSymbol fast filter. */
  public boolean containsSymbol(int node, int symbol) {
    return subtreeSymbolCount(node, symbol) != 0;
  }

  /** Precomputed grammar role for one direct parse-tree child. */
  public MIndexASTChildRole childRole(int node, int childOrdinal) {
    int checkedNode = checkNode(node);
    if (tree.terminal(checkedNode)) {
      throw new IllegalArgumentException("terminal node has no grammar children");
    }
    int production = tree.production(checkedNode);
    if (tree.childCount(checkedNode) != spec.productionPlans().productionRhsLength(production)) {
      throw new IllegalStateException("parse-tree child count diverges from production plan");
    }
    return spec.productionPlans().childRole(production, childOrdinal);
  }

  public int childGrammarSymbol(int node, int childOrdinal) {
    int checkedNode = checkNode(node);
    if (tree.terminal(checkedNode)) {
      throw new IllegalArgumentException("terminal node has no grammar children");
    }
    int production = tree.production(checkedNode);
    return spec.productionPlans().rhsSymbol(production, childOrdinal);
  }

  public long productionShapeHash64(int node) {
    int checkedNode = checkNode(node);
    if (tree.terminal(checkedNode)) {
      throw new IllegalArgumentException("terminal node has no production shape");
    }
    return spec.productionPlans().shapeHash64(tree.production(checkedNode));
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * (long)
                (parents.length
                    + childOrdinals.length
                    + preorderToNode.length
                    + nodeToPreorder.length
                    + subtreeSizes.length
                    + depths.length
                    + leafCounts.length
                    + ancestors.length
                    + kindCodes.length
                    + symbolOffsets.length
                    + nodesBySymbol.length
                    + preordersBySymbol.length
                    + productionOffsets.length
                    + nodesByProduction.length
                    + preordersByProduction.length)
        + Long.BYTES
            * (long)
                (exactHashes.length
                    + structuralHashes.length
                    + logicHashes.length
                    + symbolSignals.length);
  }

  public static boolean isTerminalKindCode(int kindCode) {
    return kindCode < 0;
  }

  public static int terminalSymbolFromKindCode(int kindCode) {
    if (!isTerminalKindCode(kindCode)) {
      throw new IllegalArgumentException("kind code is not a terminal symbol");
    }
    return kindCode & VALUE_MASK;
  }

  public static int productionFromKindCode(int kindCode) {
    if (isTerminalKindCode(kindCode)) {
      throw new IllegalArgumentException("kind code is not a production");
    }
    return kindCode;
  }

  private int checkNode(int node) {
    return Objects.checkIndex(node, nodeCount);
  }

  private static int terminalCount(MIndexGrammarParseTree tree) {
    int count = 0;
    for (int node = 0; node < tree.nodeCount(); node++) if (tree.terminal(node)) count++;
    return count;
  }

  private static boolean logicTokenContentRelevant(
      MIndexASTSpecPrecompute spec, int symbol) {
    Optional<MIndexTokenClass> tokenClass = spec.tokenClass(symbol);
    if (tokenClass.isEmpty()) return true;
    return switch (tokenClass.orElseThrow()) {
      case IDENTIFIER, QUOTED_IDENTIFIER, NAME_PLACEHOLDER, VALUE_PLACEHOLDER -> false;
      default -> true;
    };
  }

  private static long stableTokenHash(MIndexTokenStream tokens, int tokenIndex) {
    long hash = 0xcbf29ce484222325L;
    int length = tokens.length(tokenIndex);
    for (int index = 0; index < length; index++) {
      char value = tokens.charAt(tokenIndex, index);
      hash ^= value & 0xffL;
      hash *= 0x100000001b3L;
      hash ^= value >>> 8;
      hash *= 0x100000001b3L;
    }
    return avalanche(hash ^ length);
  }

  private static long symbolBit(int symbol) {
    long mixed = avalanche(Integer.toUnsignedLong(symbol) ^ 0x53594d424f4c4249L);
    return (1L << (mixed & 63)) | (1L << ((mixed >>> 6) & 63));
  }

  private static int lowerBound(
      int[] values, int from, int to, int target) {
    int low = from;
    int high = to;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] < target) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static int[] offsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static long mix(long left, long right) {
    return avalanche(left ^ Long.rotateLeft(right + 0x9e3779b97f4a7c15L, 19));
  }

  private static long avalanche(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
