// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable, spec-versioned exact-lexeme DFA for an existing M3String or CharSequence view.
 *
 * <p>Grammar literals are precomputed once into sorted, primitive-array trie transitions.
 * Per-source lookup allocates no objects, does not copy the source, does not parse regex and
 * does not retain per-lexeme Entry/Node objects. Temporary construction nodes are discarded.
 * Lexical input must already have passed the language's Unicode-escape translation stage;
 * the caller enforces identifier boundaries, trivia and contextual-keyword rules.</p>
 *
 * <p>This matches literal lexemes only, not a complete regular-expression or syntax grammar.
 * Literal searches across chunk boundaries remain owned by M3CursorLiteralSearch and token
 * sequence patterns remain owned by M3TokenPattern.</p>
 */
public final class M3StaticGrammarLexicon {
    /** A literal spelling with a stable positive token ID; contextual is parser metadata. */
    public record Literal(String spelling, int tokenId, boolean contextual) { }

    private static final class BuildNode {
        final TreeMap<Character, BuildNode> edges = new TreeMap<>();
        int id = -1;
        int terminal = -1;
    }

    // Final canonical storage is primitive lanes; no TreeMap or BuildNode survives compilation.
    private final int[] edgeStart;
    private final char[] edgeUnit;
    private final int[] edgeTarget;
    private final int[] terminalId;
    private final int[] contextualIds;
    // Optional 512-byte root accelerator; absent for small or non-ASCII grammars.
    private final int[] asciiRootTarget;
    private final String language;
    private final String revision;
    private final String grammarSha256;
    private final int literalCount;

    private M3StaticGrammarLexicon(int[] edgeStart, char[] edgeUnit, int[] edgeTarget,
            int[] terminalId, int[] contextualIds, int[] asciiRootTarget,
            String language, String revision, String grammarSha256, int literalCount) {
        this.edgeStart = edgeStart;
        this.edgeUnit = edgeUnit;
        this.edgeTarget = edgeTarget;
        this.terminalId = terminalId;
        this.contextualIds = contextualIds;
        this.asciiRootTarget = asciiRootTarget;
        this.language = language;
        this.revision = revision;
        this.grammarSha256 = grammarSha256;
        this.literalCount = literalCount;
    }

    /** Compile once per known language specification version; the input is never retained. */
    public static M3StaticGrammarLexicon compile(String language, String revision,
            List<Literal> definitions) {
        if (Objects.requireNonNull(language, "language").isBlank()
                || Objects.requireNonNull(revision, "revision").isBlank()) {
            throw new IllegalArgumentException("language and revision must be nonblank");
        }
        List<Literal> rows = new ArrayList<>(Objects.requireNonNull(definitions, "definitions"));
        rows.sort(Comparator.comparing(row -> Objects.requireNonNull(row, "definition").spelling(),
                Comparator.nullsFirst(String::compareTo)));
        HashSet<Integer> ids = new HashSet<>();
        String prior = null;
        BuildNode root = new BuildNode();
        List<Integer> contextual = new ArrayList<>();
        for (Literal row : rows) {
            String spelling = Objects.requireNonNull(row.spelling(), "spelling");
            if (spelling.isEmpty() || row.tokenId() <= 0 || spelling.equals(prior)
                    || !ids.add(row.tokenId())) {
                throw new IllegalArgumentException("empty/duplicate spelling or token ID");
            }
            prior = spelling;
            BuildNode at = root;
            for (int i = 0; i < spelling.length(); i++) {
                at = at.edges.computeIfAbsent(spelling.charAt(i), ignored -> new BuildNode());
            }
            at.terminal = row.tokenId();
            if (row.contextual()) contextual.add(row.tokenId());
        }
        int[] contextualIds = new int[contextual.size()];
        for (int i = 0; i < contextual.size(); i++) contextualIds[i] = contextual.get(i);
        Arrays.sort(contextualIds);

        List<BuildNode> nodes = new ArrayList<>();
        ArrayDeque<BuildNode> pending = new ArrayDeque<>();
        root.id = 0;
        pending.add(root);
        int edgeCount = 0;
        while (!pending.isEmpty()) {
            BuildNode node = pending.removeFirst();
            nodes.add(node);
            edgeCount = Math.addExact(edgeCount, node.edges.size());
            for (BuildNode child : node.edges.values()) {
                child.id = nodes.size() + pending.size();
                pending.addLast(child);
            }
        }
        int[] starts = new int[nodes.size() + 1];
        char[] units = new char[edgeCount];
        int[] targets = new int[edgeCount];
        int[] terminals = new int[nodes.size()];
        int cursor = 0;
        for (int i = 0; i < nodes.size(); i++) {
            BuildNode node = nodes.get(i);
            starts[i] = cursor;
            terminals[i] = node.terminal;
            for (var edge : node.edges.entrySet()) {
                units[cursor] = edge.getKey();
                targets[cursor] = edge.getValue().id;
                cursor++;
            }
        }
        starts[nodes.size()] = cursor;
        if (cursor != edgeCount) throw new IllegalStateException("inconsistent grammar trie");
        // Root edges are already sorted UTF-16 units. A direct-address ASCII root is
        // beneficial only above the bounded eight-edge crossover; other grammars retain
        // the original binary search with no additional retained 512-byte allocation.
        int asciiEdges = 0;
        for (int i = starts[0]; i < starts[1]; i++) {
            if (units[i] < 128) asciiEdges++;
        }
        int[] asciiRoot = null;
        if (asciiEdges >= 8) {
            asciiRoot = new int[128];
            Arrays.fill(asciiRoot, -1);
            for (int i = starts[0]; i < starts[1]; i++) {
                if (units[i] < 128) asciiRoot[units[i]] = targets[i];
            }
        }
        return new M3StaticGrammarLexicon(starts, units, targets, terminals,
                contextualIds, asciiRoot, language, revision,
                digest(language, revision, rows), rows.size());
    }

    /**
     * Return a matching token ID only when the entire half-open UTF-16 range is one literal.
     * Returns -1 for no match; does not infer Java identifier boundaries or syntactic context.
     */
    public int exact(CharSequence source, int from, int to) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(source, "source").length());
        int state = 0;
        for (int i = from; i < to; i++) {
            state = transition(state, source.charAt(i));
            if (state < 0) return -1;
        }
        return terminalId[state];
    }

    /**
     * Maximal-munch literal prefix, packed as (tokenId high 32 bits, endExclusive low 32 bits).
     * Returns -1L when none; use {@link #tokenId(long)} and {@link #endExclusive(long)}.
     * This is not a language identifier/keyword boundary check.
     */
    public long longest(CharSequence source, int from, int to) {
        Objects.checkFromToIndex(from, to, Objects.requireNonNull(source, "source").length());
        long best = -1L;
        int state = 0;
        for (int i = from; i < to; i++) {
            state = transition(state, source.charAt(i));
            if (state < 0) break;
            int id = terminalId[state];
            if (id > 0) best = ((long) id << 32) | ((i + 1L) & 0xffff_ffffL);
        }
        return best;
    }

    public static int tokenId(long packed) {
        return packed == -1L ? -1 : (int) (packed >>> 32);
    }

    public static int endExclusive(long packed) {
        return packed == -1L ? -1 : (int) packed;
    }

    /** Contextual role is advisory; the parser must decide its grammatical meaning. */
    public boolean contextual(int tokenId) {
        return Arrays.binarySearch(contextualIds, tokenId) >= 0;
    }

    public String language() { return language; }
    public String revision() { return revision; }
    public String grammarSha256() { return grammarSha256; }
    public int literalCount() { return literalCount; }
    public int stateCount() { return terminalId.length; }

    private int transition(int state, char codeUnit) {
        if (state == 0 && asciiRootTarget != null && codeUnit < 128) {
            return asciiRootTarget[codeUnit];
        }
        int at = Arrays.binarySearch(edgeUnit, edgeStart[state], edgeStart[state + 1], codeUnit);
        return at < 0 ? -1 : edgeTarget[at];
    }

    private static String digest(String language, String revision, List<Literal> rows) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            updateString(md, language);
            updateString(md, revision);
            updateInt(md, rows.size());
            for (Literal row : rows) {
                updateString(md, row.spelling());
                updateInt(md, row.tokenId());
                md.update((byte) (row.contextual() ? 1 : 0));
            }
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException impossibleOnJdk21) {
            throw new IllegalStateException("SHA-256 unavailable", impossibleOnJdk21);
        }
    }

    private static void updateString(MessageDigest md, String text) {
        updateInt(md, text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            md.update((byte) (c >>> 8));
            md.update((byte) c);
        }
    }

    private static void updateInt(MessageDigest md, int value) {
        md.update((byte) (value >>> 24));
        md.update((byte) (value >>> 16));
        md.update((byte) (value >>> 8));
        md.update((byte) value);
    }
}
