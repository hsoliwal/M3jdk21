// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.IntSupplier;
import java.util.function.LongToIntFunction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TokenIndex;

/**
 * A versioned, immutable, prepared lexical-and-predictive grammar specification.
 * <p>
 * Literal spellings are delegated to the existing {@link StaticGrammarLexicon};
 * regular lexical validation is compiled exactly once into JDK 21 {@link Pattern}
 * programs. The source lexer supplies token spans; this class never attempts to
 * reinterpret comments, Unicode preprocessing, lexical modes, or contextual
 * keywords. A session reuses one {@link Matcher} per pattern across all token
 * spans and creates no substrings or per-token matchers.
 * </p>
 * <p>
 * Syntactic productions are normalized BNF, not text parsed from an arbitrary
 * BNF/EBNF/ABNF dialect. Positive integers denote terminal token IDs, negative
 * integers nonterminal IDs, and zero is reserved for EOF. Empty RHS means epsilon.
 * FIRST/FOLLOW, nullable-prefix recursion guards, and a conflict-free LL(1)
 * prediction table are computed once. Grammars requiring LR/GLR/generalized
 * parsing must use their authoritative parser; no incorrect DFA substitution.
 * </p>
 * <p>
 * This isolated candidate does not modify java.util.regex.Pattern or Matcher,
 * JDK public behavior, M3String ownership, or the authoritative language lexer.
 * </p>
 */
public final class GrammarSpecification {
    /** One lexical regexp with a stable positive token ID and precedence (lower wins). */
    public record RegexRule(int tokenId, String expression, int flags, int precedence) { }

    /** One normalized BNF alternative. A negative LHS and zero-or-more nonzero RHS symbols. */
    public record Production(int lhs, int[] rhs) {
        public Production {
            Objects.requireNonNull(rhs, "rhs");
            rhs = rhs.clone();
        }

        @Override
        public int[] rhs() {
            return rhs.clone();
        }
    }

    private static final int MAX_PRODUCTIONS = 32_768;
    private static final int MAX_RHS_UNITS = 1_048_576;
    private static final int MAX_PARSE_STACK = 1_048_576;
    private static final int MAX_IMAGE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_LEXICAL_RULES = 32_768;
    private static final int MAX_REGEX_CHARS = 1_048_576;
    private static final int MAX_LITERAL_CHARS = 4_194_304;
    private static final int IMAGE_MAGIC = 0x4d334752; // M3GR
    // Version 2 removes DataOutputStream.writeUTF's 64KiB per-string limit.
    // All strings are encoded as length-prefixed UTF-16 code units, not UTF-8,
    // preserving even unpaired surrogates present in grammar literal names.
    // Version 1 remains readable for existing pinned artifacts.
    private static final int IMAGE_VERSION = 2;
    private static final int MAX_SYNTAX_IMAGE = 16 * 1024 * 1024;
    private static final int SYNTAX_MAGIC = 0x4d334731; // M3G1
    private final StaticGrammarLexicon literals;
    private final RegexRule[] rules;
    private final Pattern[] patterns;
    private final int[] terminals;      // sorted IDs, excludes EOF
    private final int[] nonterminals;   // sorted negative IDs
    private final int startSymbol;
    // Runtime production storage is CSR: two primitive lanes, never one array per rule.
    private final int[] rhsOffsets;
    private final int[] rhsSymbols;
    private final long[] predictKeys;  // (nonterminal-index << 32) | terminal ID; EOF=0
    private final int[] predictValues;
    private final int[] productionLhs;
    private final String[] spellings; // frozen specification words, not source text
    private final int[] spellingIds;
    private final boolean[] spellingContext;
    private final String sha256;

    private GrammarSpecification(StaticGrammarLexicon literals, RegexRule[] rules,
            Pattern[] patterns, int[] terminals, int[] nonterminals, int startSymbol,
            int[][] rhs, long[] predictKeys, int[] predictValues, int[] productionLhs,
            String[] spellings, int[] spellingIds, boolean[] spellingContext, String sha256) {
        this.literals = literals;
        this.rules = rules;
        this.patterns = patterns;
        this.terminals = terminals;
        this.nonterminals = nonterminals;
        this.startSymbol = startSymbol;
        this.rhsOffsets = new int[rhs.length + 1];
        int length = 0;
        for (int i = 0; i < rhs.length; i++) {
            length = Math.addExact(length, rhs[i].length);
            if (length > MAX_RHS_UNITS) {
                throw new IllegalArgumentException("grammar RHS budget exceeded");
            }
            this.rhsOffsets[i + 1] = length;
        }
        this.rhsSymbols = new int[length];
        for (int i = 0; i < rhs.length; i++) {
            System.arraycopy(rhs[i], 0, rhsSymbols, rhsOffsets[i], rhs[i].length);
        }
        this.predictKeys = predictKeys;
        this.predictValues = predictValues;
        this.productionLhs = productionLhs;
        this.spellings = spellings;
        this.spellingIds = spellingIds;
        this.spellingContext = spellingContext;
        this.sha256 = sha256;
    }

    /**
     * Compile a normalized specification. Syntax is optional: use startSymbol=0
     * and an empty production list for lexical-only specifications.
     * All input collections/arrays are copied or reduced to immutable snapshots.
     * Ambiguous/non-LL(1) productions fail closed rather than misparse source.
     */
    public static GrammarSpecification compile(String language, String revision,
            List<StaticGrammarLexicon.Literal> fixed,
            List<RegexRule> regex,
            int startSymbol,
            List<Production> grammar) {
        return compileWithSyntaxImage(language, revision, fixed, regex, startSymbol, grammar, null);
    }

    /**
     * Reuse a trusted, hash-checked syntax-table build artifact, avoiding runtime
     * FIRST/FOLLOW and prediction-table construction. The input grammar, token
     * semantics and version must exactly match the image or loading fails closed.
     * The fixed-literal trie and JDK regex programs are still prepared separately.
     */
    public static GrammarSpecification compileWithSyntaxImage(String language, String revision,
            List<StaticGrammarLexicon.Literal> fixed,
            List<RegexRule> regex,
            int startSymbol,
            List<Production> grammar, byte[] syntaxImage) {
        List<StaticGrammarLexicon.Literal> fixedRows =
                List.copyOf(Objects.requireNonNull(fixed, "fixed"));
        List<RegexRule> regexRows = new ArrayList<>(Objects.requireNonNull(regex, "regex"));
        List<Production> grammarRows = List.copyOf(Objects.requireNonNull(grammar, "grammar"));
        if (fixedRows.size() > MAX_LEXICAL_RULES || regexRows.size() > MAX_LEXICAL_RULES) {
            throw new IllegalArgumentException("lexical rule limit exceeded");
        }
        if (grammarRows.size() > MAX_PRODUCTIONS) {
            throw new IllegalArgumentException("grammar production limit exceeded");
        }
        StaticGrammarLexicon lexicon = StaticGrammarLexicon.compile(language, revision, fixedRows);
        HashSet<Integer> ids = new HashSet<>();
        long literalChars = 0;
        for (StaticGrammarLexicon.Literal l : fixedRows) {
            // Lexicon validates spelling, empty/duplicate lexical IDs and contextual role.
            literalChars += l.spelling().length();
            if (literalChars > MAX_LITERAL_CHARS) {
                throw new IllegalArgumentException("fixed-lexeme budget exceeded");
            }
            ids.add(l.tokenId());
        }
        regexRows.sort(Comparator.comparingInt(RegexRule::precedence)
                .thenComparingInt(RegexRule::tokenId));
        Pattern[] patterns = new Pattern[regexRows.size()];
        long regexChars = 0;
        for (int i = 0; i < regexRows.size(); i++) {
            RegexRule rule = Objects.requireNonNull(regexRows.get(i), "regex rule");
            if (rule.tokenId() <= 0 || !ids.add(rule.tokenId())) {
                throw new IllegalArgumentException("duplicate/invalid lexical token ID "
                        + rule.tokenId());
            }
            String expression = Objects.requireNonNull(rule.expression(), "expression");
            regexChars += expression.length();
            if (regexChars > MAX_REGEX_CHARS) {
                throw new IllegalArgumentException("regex specification budget exceeded");
            }
            Pattern compiled = Pattern.compile(expression, rule.flags());
            if (compiled.matcher("").matches()) {
                throw new IllegalArgumentException("empty-token rule ID " + rule.tokenId());
            }
            patterns[i] = compiled;
        }
        int[] terminals = ids.stream().mapToInt(Integer::intValue).sorted().toArray();
        int[] nonterminals;
        int[][] rhs;
        int[] productionLhs;
        long[] keys;
        int[] values;
        if (grammarRows.isEmpty()) {
            if (startSymbol != 0)
                throw new IllegalArgumentException("start symbol without grammar");
            if (syntaxImage != null)
                throw new IllegalArgumentException("syntax image without grammar");
            nonterminals = new int[0];
            rhs = new int[0][];
            productionLhs = new int[0];
            keys = new long[0];
            values = new int[0];
        } else {
            if (startSymbol >= 0)
                throw new IllegalArgumentException("start nonterminal must be negative");
            TreeMap<Integer, Integer> nonterminalIndex = new TreeMap<>();
            for (Production p : grammarRows) {
                if (p.lhs() >= 0)
                    throw new IllegalArgumentException("nonterminal lhs must be negative");
                nonterminalIndex.putIfAbsent(p.lhs(), nonterminalIndex.size());
            }
            nonterminals = nonterminalIndex.keySet().stream().mapToInt(Integer::intValue).toArray();
            if (Arrays.binarySearch(nonterminals, startSymbol) < 0) {
                throw new IllegalArgumentException("undefined start nonterminal");
            }
            rhs = new int[grammarRows.size()][];
            productionLhs = new int[grammarRows.size()];
            long rhsUnits = 0;
            for (int i = 0; i < grammarRows.size(); i++) {
                Production production = grammarRows.get(i);
                int[] sequence = production.rhs();
                rhsUnits += sequence.length;
                if (rhsUnits > MAX_RHS_UNITS)
                    throw new IllegalArgumentException("grammar RHS limit exceeded");
                for (int symbol : sequence) {
                    if (symbol == 0 || (symbol > 0 && Arrays.binarySearch(terminals, symbol) < 0)
                            || (symbol < 0 && Arrays.binarySearch(nonterminals, symbol) < 0)) {
                        throw new IllegalArgumentException("undefined/reserved grammar symbol "
                                + symbol);
                    }
                }
                rhs[i] = sequence;
                productionLhs[i] = production.lhs();
            }
            String fingerprint = fingerprint(lexicon.grammarSha256(), regexRows,
                    startSymbol, grammarRows, rhs);
            Table table = syntaxImage == null
                    ? buildTable(startSymbol, grammarRows, rhs, nonterminals, terminals)
                    : readSyntaxImage(syntaxImage, fingerprint, startSymbol,
                            nonterminals, terminals, rhs.length);
            keys = table.keys;
            values = table.values;
        }
        String signature =
                fingerprint(lexicon.grammarSha256(), regexRows, startSymbol, grammarRows, rhs);
        List<StaticGrammarLexicon.Literal> sorted = new ArrayList<>(fixedRows);
        sorted.sort(Comparator.comparing(StaticGrammarLexicon.Literal::spelling));
        String[] spellings = new String[sorted.size()];
        int[] spellingIds = new int[sorted.size()];
        boolean[] contexts = new boolean[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            var row = sorted.get(i);
            spellings[i] = row.spelling();
            spellingIds[i] = row.tokenId();
            contexts[i] = row.contextual();
        }
        return new GrammarSpecification(lexicon, regexRows.toArray(RegexRule[]::new), patterns,
                terminals, nonterminals, startSymbol, rhs, keys, values, productionLhs,
                spellings, spellingIds, contexts, signature);
    }

    public String language() { return literals.language(); }
    public String revision() { return literals.revision(); }
    public String grammarSha256() { return sha256; }
    // Original-owner projection bridge; the fixed-lexeme trie is never copied.
    StaticGrammarLexicon literalLexicon() { return literals; }
    public int terminalCount() { return terminals.length; }
    public int productionCount() { return productionLhs.length; }

    /** Number of packed RHS elements; independent of number of source files. */
    public int rhsSymbolCount() { return rhsSymbols.length; }
    public boolean hasSyntax() { return startSymbol != 0; }
    public boolean contextual(int tokenId) { return literals.contextual(tokenId); }

    /**
     * Build a deterministic, content-addressable grammar image. LL(1) prediction
     * tables are serialized as primitive lanes, not regenerated on image load.
     * The immutable fixed-lexeme trie is reconstructed by its existing owner;
     * java.util.regex.Pattern programs are recompiled once on image admission,
     * because stock Java regex has no portable serialized bytecode format.
     */
    public byte[] image() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(IMAGE_MAGIC);
            output.writeInt(IMAGE_VERSION);
            writeText(output, language());
            writeText(output, revision());
            output.writeInt(spellings.length);
            for (int i = 0; i < spellings.length; i++) {
                writeText(output, spellings[i]);
                output.writeInt(spellingIds[i]);
                output.writeBoolean(spellingContext[i]);
            }
            output.writeInt(rules.length);
            for (RegexRule rule : rules) {
                output.writeInt(rule.tokenId());
                writeText(output, rule.expression());
                output.writeInt(rule.flags());
                output.writeInt(rule.precedence());
            }
            output.writeInt(startSymbol);
            output.writeInt(productionLhs.length);
            for (int i = 0; i < productionLhs.length; i++) {
                output.writeInt(productionLhs[i]);
                int from = rhsOffsets[i];
                int to = rhsOffsets[i + 1];
                output.writeInt(to - from);
                for (int at = from; at < to; at++) output.writeInt(rhsSymbols[at]);
            }
            output.writeInt(predictKeys.length);
            for (int i = 0; i < predictKeys.length; i++) {
                output.writeLong(predictKeys[i]);
                output.writeInt(predictValues[i]);
            }
            writeText(output, sha256);
            output.flush();
            byte[] payload = bytes.toByteArray();
            if ((long) payload.length + 32 > MAX_IMAGE_BYTES) {
                throw new IllegalStateException("grammar image size limit exceeded");
            }
            byte[] sealed = Arrays.copyOf(payload, payload.length + 32);
            System.arraycopy(digestBytes(payload), 0, sealed, payload.length, 32);
            return sealed;
        } catch (IOException encodingFailure) {
            throw new IllegalArgumentException("unsupported grammar image encoding",
                    encodingFailure);
        }
    }

    /**
     * Restore previously computed prediction tables from a build-authored image.
     * The expected SHA-256 must be pinned by the build manifest, not obtained
     * from the untrusted image itself. Never silently load an unsealed grammar.
     */
    /**
     * Load an authenticated build-prepared grammar directly from an existing heap,
     * direct, read-only, or mapped byte view. The caller's position, mark and limit
     * are unchanged. Only the remaining range is authenticated; no writable alias
     * or mutable buffer is retained after this method returns.
     */
    public static GrammarSpecification fromImage(ByteBuffer sealed, String expectedSha256) {
        Objects.requireNonNull(sealed, "sealed");
        Objects.requireNonNull(expectedSha256, "expectedSha256");
        int size = sealed.remaining();
        if (size < 40 || size > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("invalid grammar image length");
        }
        ByteBuffer borrowed = sealed.asReadOnlyBuffer();
        byte[] snapshot = new byte[size];
        borrowed.get(snapshot);
        return fromImage(snapshot, expectedSha256);
    }

    public static GrammarSpecification fromImage(byte[] sealed, String expectedSha256) {
        Objects.requireNonNull(sealed, "sealed");
        Objects.requireNonNull(expectedSha256, "expectedSha256");
        if (sealed.length < 40 || sealed.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("invalid grammar image length");
        }
        // Pin one coherent byte snapshot; never hash caller-owned mutable data
        // and later decode a different generation of that same buffer.
        byte[] image = sealed.clone();
        byte[] payload = Arrays.copyOf(image, image.length - 32);
        byte[] digest = digestBytes(payload);
        if (!MessageDigest.isEqual(digest,
                Arrays.copyOfRange(image, image.length - 32, image.length))
                || !HexFormat.of().formatHex(digestBytes(image)).equals(expectedSha256)) {
            throw new IllegalArgumentException("grammar image seal mismatch");
        }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            if (input.readInt() != IMAGE_MAGIC) {
                throw new IllegalArgumentException("invalid grammar image magic");
            }
            int version = input.readInt();
            if (version != 1 && version != IMAGE_VERSION) {
                throw new IllegalArgumentException("unsupported grammar image schema " + version);
            }
            String language = readText(input, version, 16_384);
            String revision = readText(input, version, 16_384);
            int count = bounded(input.readInt(), MAX_LEXICAL_RULES);
            List<StaticGrammarLexicon.Literal> fixed = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                fixed.add(new StaticGrammarLexicon.Literal(
                        readText(input, version, MAX_LITERAL_CHARS), input.readInt(),
                        input.readBoolean()));
            }
            count = bounded(input.readInt(), MAX_LEXICAL_RULES);
            List<RegexRule> regex = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                regex.add(new RegexRule(input.readInt(),
                        readText(input, version, MAX_REGEX_CHARS), input.readInt(),
                        input.readInt()));
            }
            int start = input.readInt();
            count = bounded(input.readInt(), MAX_PRODUCTIONS);
            List<Production> productions = new ArrayList<>(count);
            long units = 0;
            for (int i = 0; i < count; i++) {
                int lhs = input.readInt();
                int length = bounded(input.readInt(), MAX_RHS_UNITS);
                units += length;
                if (units > MAX_RHS_UNITS) throw new IllegalArgumentException("grammar RHS limit");
                int[] sequence = new int[length];
                for (int j = 0; j < length; j++) sequence[j] = input.readInt();
                productions.add(new Production(lhs, sequence));
            }
            count = bounded(input.readInt(), MAX_RHS_UNITS);
            long[] keys = new long[count];
            int[] values = new int[count];
            for (int i = 0; i < count; i++) {
                keys[i] = input.readLong();
                values[i] = input.readInt();
                if (i > 0 && keys[i] <= keys[i-1]) {
                    throw new IllegalArgumentException("unsorted prediction keys");
                }
                if (values[i] < 0 || values[i] >= productions.size()) {
                    throw new IllegalArgumentException("invalid prediction value");
                }
            }
            String fingerprint = readText(input, version, 64);
            if (input.available() != 0) {
                throw new IllegalArgumentException("trailing grammar image payload");
            }
            // Compile only the lexical owners; prediction tables have already
            // been compiled in the build and are supplied unchanged below.
            GrammarSpecification lexical = compile(language, revision, fixed, regex, 0,
                    List.of());
            List<RegexRule> sortedRegex = Arrays.asList(lexical.rules);
            int[][] rhs = new int[productions.size()][];
            int[] lhs = new int[rhs.length];
            TreeMap<Integer, Boolean> seen = new TreeMap<>();
            for (int i = 0; i < rhs.length; i++) {
                Production production = productions.get(i);
                lhs[i] = production.lhs();
                rhs[i] = production.rhs();
                if (lhs[i] >= 0) throw new IllegalArgumentException("bad nonterminal");
                seen.put(lhs[i], Boolean.TRUE);
                for (int symbol : rhs[i]) {
                    if (symbol == 0 || (symbol > 0
                            && Arrays.binarySearch(lexical.terminals, symbol) < 0)) {
                        throw new IllegalArgumentException("undefined grammar terminal");
                    }
                }
            }
            int[] nonterminals = seen.keySet().stream().mapToInt(Integer::intValue).toArray();
            for (int[] seq : rhs) for (int symbol : seq) {
                if (symbol < 0 && Arrays.binarySearch(nonterminals, symbol) < 0) {
                    throw new IllegalArgumentException("undefined nonterminal");
                }
            }
            validateImageLeftRecursion(nonterminals, lhs, rhs);
            if (start == 0 ? !productions.isEmpty() || keys.length != 0
                    : Arrays.binarySearch(nonterminals, start) < 0) {
                throw new IllegalArgumentException("invalid grammar start");
            }
            for (int i = 0; i < keys.length; i++) {
                int nt = (int) (keys[i] >>> 32);
                int lookahead = (int) keys[i];
                if (nt < 0 || nt >= nonterminals.length
                        || (lookahead != 0 && Arrays.binarySearch(lexical.terminals, lookahead) < 0)
                        || lhs[values[i]] != nonterminals[nt]) {
                    throw new IllegalArgumentException("invalid predictive transition");
                }
            }
            String computed = fingerprint(lexical.literals.grammarSha256(),
                    sortedRegex, start, productions, rhs);
            if (!computed.equals(fingerprint)) {
                throw new IllegalArgumentException("grammar fingerprint mismatch");
            }
            return new GrammarSpecification(lexical.literals, lexical.rules, lexical.patterns,
                    lexical.terminals, nonterminals, start, rhs, keys, values, lhs,
                    lexical.spellings, lexical.spellingIds, lexical.spellingContext,
                    computed);
        } catch (IOException | RuntimeException invalidImage) {
            if (invalidImage instanceof IllegalArgumentException argument) throw argument;
            throw new IllegalArgumentException("invalid grammar image", invalidImage);
        }
    }

    /**
     * In an externally signed grammar image, even a sorted prediction table can
     * encode a non-consuming recursion cycle. Admit only acyclic nullable-prefix
     * left-corner graphs. The worklist consumes O(nonterminals + RHS units) primitive
     * storage, with no recursive Java stack and no per-production object graph.
     */
    private static void validateImageLeftRecursion(int[] symbols, int[] lhs, int[][] rhs) {
        int n = symbols.length;
        if (n == 0) return;
        int[] remaining = new int[lhs.length];
        int[] nullableUsers = new int[n];
        for (int p = 0; p < lhs.length; p++) {
            int pending = 0;
            boolean hasTerminal = false;
            for (int symbol : rhs[p]) {
                if (symbol > 0) hasTerminal = true;
                else pending++;
            }
            remaining[p] = hasTerminal ? -1 : pending;
            if (!hasTerminal) for (int symbol : rhs[p]) {
                nullableUsers[Arrays.binarySearch(symbols, symbol)]++;
            }
        }
        int[] usersStart = new int[n + 1];
        for (int i = 0; i < n; i++) usersStart[i + 1] = usersStart[i] + nullableUsers[i];
        int[] userWrite = usersStart.clone();
        int[] userProductions = new int[usersStart[n]];
        for (int p = 0; p < lhs.length; p++) {
            if (remaining[p] < 0) continue;
            for (int symbol : rhs[p]) {
                int at = Arrays.binarySearch(symbols, symbol);
                userProductions[userWrite[at]++] = p;
            }
        }
        boolean[] nullable = new boolean[n];
        int[] queue = new int[n];
        int tail = 0;
        for (int p = 0; p < lhs.length; p++) if (remaining[p] == 0) {
            int at = Arrays.binarySearch(symbols, lhs[p]);
            if (!nullable[at]) { nullable[at] = true; queue[tail++] = at; }
        }
        for (int head = 0; head < tail; head++) {
            int nt = queue[head];
            for (int k = usersStart[nt]; k < usersStart[nt + 1]; k++) {
                int p = userProductions[k];
                if (--remaining[p] == 0) {
                    int left = Arrays.binarySearch(symbols, lhs[p]);
                    if (!nullable[left]) { nullable[left] = true; queue[tail++] = left; }
                }
            }
        }
        int[] degrees = new int[n];
        for (int p = 0; p < lhs.length; p++) {
            int left = Arrays.binarySearch(symbols, lhs[p]);
            for (int unit : rhs[p]) {
                if (unit > 0) break;
                int nt = Arrays.binarySearch(symbols, unit);
                degrees[left]++;
                if (!nullable[nt]) break;
            }
        }
        int[] edgeStart = new int[n + 1];
        for (int i = 0; i < n; i++) edgeStart[i + 1] = edgeStart[i] + degrees[i];
        int[] write = edgeStart.clone();
        int[] edges = new int[edgeStart[n]];
        int[] incoming = new int[n];
        for (int p = 0; p < lhs.length; p++) {
            int left = Arrays.binarySearch(symbols, lhs[p]);
            for (int unit : rhs[p]) {
                if (unit > 0) break;
                int nt = Arrays.binarySearch(symbols, unit);
                edges[write[left]++] = nt;
                incoming[nt]++;
                if (!nullable[nt]) break;
            }
        }
        tail = 0;
        for (int i = 0; i < n; i++) if (incoming[i] == 0) queue[tail++] = i;
        int consumed = 0;
        for (int head = 0; head < tail; head++) {
            int at = queue[head];
            consumed++;
            for (int k = edgeStart[at]; k < edgeStart[at + 1]; k++) {
                if (--incoming[edges[k]] == 0) queue[tail++] = edges[k];
            }
        }
        if (consumed != n) throw new IllegalArgumentException("left-recursive grammar image");
    }

    /** Exact UTF-16 image encoding: preserves isolated surrogate code units. */
    private static void writeText(DataOutputStream output, String text) throws IOException {
        output.writeInt(text.length());
        for (int i = 0; i < text.length(); i++) output.writeChar(text.charAt(i));
    }

    private static String readText(DataInputStream input, int version, int maxUnits)
            throws IOException {
        if (version == 1) return input.readUTF();
        int units = input.readInt();
        if (units < 0 || units > maxUnits || (long) units * 2 > input.available()) {
            throw new IllegalArgumentException("invalid UTF-16 image text length");
        }
        char[] chars = new char[units];
        for (int i = 0; i < units; i++) chars[i] = input.readChar();
        return new String(chars);
    }

    /** SHA-256 of the complete sealed artifact for build-manifest pinning. */
    public static String imageSha256(byte[] image) {
        return HexFormat.of().formatHex(digestBytes(Objects.requireNonNull(image)));
    }

    private static byte[] digestBytes(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossibleOnJdk21) {
            throw new IllegalStateException("SHA-256 missing", impossibleOnJdk21);
        }
    }

    private static int bounded(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("image count limit");
        return value;
    }

    /** Bind a generation-owned immutable source. Sessions are thread-confined. */
    public Session bind(CharSequence source) {
        return new Session(Objects.requireNonNull(source, "source"));
    }

    /** A reusable, non-thread-safe source session; never stored by the grammar image. */
    public final class Session {
        private CharSequence source;
        private final Matcher[] matchers = new Matcher[patterns.length];

        private Session(CharSequence source) {
            rebind(source);
        }

        /** Reuse precompiled programs and matcher workspaces for another source generation. */
        public Session rebind(CharSequence next) {
            source = Objects.requireNonNull(next, "source");
            for (Matcher matcher : matchers) {
                if (matcher != null) matcher.reset(source);
            }
            return this;
        }

        /**
         * Classify precisely one authoritative, nonempty [from,to) token span.
         * Exact lexemes beat regexp validators; the latter use full-region JDK21
         * matching (including declared flags), then precedence/token-ID order.
         * Zero means unknown and must not be placed in a complete token cursor.
         */
        public int classify(int from, int to) {
            Objects.checkFromToIndex(from, to, source.length());
            if (from == to) throw new IllegalArgumentException("empty token");
            int literalId = literals.exact(source, from, to);
            if (literalId > 0) return literalId;
            return classifyRegex(from, to);
        }

        // Original GrammarTokenProjection calls this only for unclassified admitted
        // spans; avoid a redundant trie lookup and retain the original geometry gate.
        int classifyRegex(int from, int to) {
            Objects.checkFromToIndex(from, to, source.length());
            if (from == to) throw new IllegalArgumentException("empty token");
            for (int i = 0; i < rules.length; i++) {
                Matcher matcher = matchers[i];
                if (matcher == null) matcher = matchers[i] = patterns[i].matcher(source);
                if (matcher.region(from, to).matches()) return rules[i].tokenId();
            }
            return 0;
        }

        /**
         * Reuses this session's already-prepared matcher workspaces while applying
         * the authoritative lexer's exact token spans and code-token admission mask.
         * This does not retain input arrays or attempt to re-lex comments/trivia.
         */
        public int[] project(int[] starts, int[] ends, byte[] admission, boolean complete) {
            int[] ids = GrammarTokenProjection.project(literals, source, starts, ends, admission);
            for (int i = 0; i < ids.length; i++) {
                if (admission[i] == GrammarTokenProjection.FIXED_CANDIDATE && ids[i] == 0) {
                    ids[i] = classifyRegex(starts[i], ends[i]);
                }
                if (complete && ids[i] == 0) {
                    throw new IllegalArgumentException("incomplete grammar token at index " + i);
                }
            }
            return ids;
        }

        /** Complete projected tokens are owned by the existing immutable-facts cursor. */
        public TokenCursor projectCompleteCursor(int[] starts, int[] ends, byte[] admission) {
            return TokenCursor.ownedProjection(project(starts, ends, admission, true));
        }
    }

    /**
     * Parse an already-complete token-ID lane with the precomputed LL(1) table.
     * The authoritative lexer must already have interpreted whitespace, comments,
     * escapes, modes and contextual identifiers. This recognition produces no AST.
     */
    public boolean accepts(int[] tokenIds) {
        Objects.requireNonNull(tokenIds, "tokenIds");
        // Preserve the original eager admission of every caller-owned array token.
        for (int tokenId : tokenIds) {
            if (Arrays.binarySearch(terminals, tokenId) < 0) return false;
        }
        return predict(tokenIds.length, null, tokenIds);
    }

    /**
     * Predictive recognition over a borrowed read-only/mapped primitive IntBuffer.
     * Does not change its position, limit or content and does not materialize token IDs.
     * The supplier's storage must be stable for the duration of this invocation.
     */
    public boolean accepts(IntBuffer tokenIds) {
        Objects.requireNonNull(tokenIds, "tokenIds");
        int offset = tokenIds.position();
        long count = tokenIds.remaining();
        return predict(count, index -> tokenIds.get(offset + (int) index), null);
    }

    /**
     * Recognize a canonical immutable M3TokenIndex directly. The grammar's terminal
     * alphabet is the token kind, never the separate canonical spelling/lexeme ID.
     * Significant-only mode skips TRIVIA without building the index's cached
     * significant-position int[] or any additional token-sized lane.
     */
    public boolean accepts(M3TokenIndex index, boolean significantOnly) {
        Objects.requireNonNull(index, "index");
        if (!significantOnly) {
            return accepts((long) index.count(), at -> index.kindAt((int) at));
        }
        int significant = 0;
        for (int physical = 0; physical < index.count(); physical++) {
            if ((index.flagsAt(physical) & M3TokenIndex.TRIVIA) == 0) significant++;
        }
        return accepts((long) significant, new LongToIntFunction() {
            private int physical;
            private long logical;
            @Override public int applyAsInt(long requested) {
                if (logical != requested) {
                    throw new IllegalStateException("nonsequential significant-token access");
                }
                while (physical < index.count()) {
                    int raw = physical++;
                    if ((index.flagsAt(raw) & M3TokenIndex.TRIVIA) == 0) {
                        logical++;
                        return index.kindAt(raw);
                    }
                }
                throw new IllegalStateException("inconsistent immutable token index");
            }
        });
    }

    /** Reject a stale token index from another canonical source generation. */
    public boolean accepts(M3TokenIndex index, long generation, boolean significantOnly) {
        Objects.requireNonNull(index, "index");
        if (index.generation() != generation) {
            throw new IllegalArgumentException("stale M3 token source generation");
        }
        return accepts(index, significantOnly);
    }

    /**
     * Long-index token stream over the existing owner. This is a read-only accessor,
     * not a copied token lane. The supplier must return the same ID for any position
     * while this method runs. It is called at most once per consumed token.
     * This API supports long logical source/token indices without changing JDK int APIs.
     */
    public boolean accepts(long tokenCount, LongToIntFunction tokenAt) {
        Objects.requireNonNull(tokenAt, "tokenAt");
        if (tokenCount < 0) throw new IllegalArgumentException("negative token count");
        return predict(tokenCount, tokenAt, null);
    }

    /**
     * Single-pass streaming LL(1) recognition. Tokens are positive IDs; zero
     * means permanent EOF. No input token lane, copy, AST or per-token object
     * is required. The source remains owned by its lexer/CharSequence cursor.
     * The supplier is called once for lookahead and once after each consumed
     * token, with no speculative lookahead or backtracking.
     */
    public boolean accepts(IntSupplier nextToken) {
        Objects.requireNonNull(nextToken, "nextToken");
        if (startSymbol == 0) throw new IllegalStateException("no syntax grammar");
        int[] stack = new int[16];
        int top = 0;
        stack[top++] = 0;
        stack[top++] = startSymbol;
        int lookahead = nextToken.getAsInt();
        // Prevent endless no-input grammar expansion even with an adversarial
        // supplier. Legal LL(1) recursion must eventually consume a token.
        long stepsWithoutInput = 0;
        long stepBudget = Math.max(4096L,
                ((long) productionLhs.length + nonterminals.length + 8L) * 128L);
        while (top != 0) {
            if (++stepsWithoutInput > stepBudget) {
                throw new IllegalStateException("non-consuming syntax expansion budget exceeded");
            }
            int symbol = stack[--top];
            if (symbol >= 0) {
                if (symbol != lookahead) return false;
                if (symbol != 0) {
                    lookahead = nextToken.getAsInt();
                    stepsWithoutInput = 0;
                }
                continue;
            }
            int nt = Arrays.binarySearch(nonterminals, symbol);
            long key = ((long) nt << 32) | (lookahead & 0xffff_ffffL);
            int choice = Arrays.binarySearch(predictKeys, key);
            if (choice < 0) return false;
            int production = predictValues[choice];
            int from = rhsOffsets[production];
            int to = rhsOffsets[production + 1];
            int count = to - from;
            if ((long) top + count > MAX_PARSE_STACK) {
                throw new IllegalStateException("predictive parse stack limit exceeded");
            }
            if (top + count > stack.length) {
                int capacity = stack.length;
                while (capacity < top + count) {
                    capacity = Math.min(MAX_PARSE_STACK, capacity * 2);
                }
                stack = Arrays.copyOf(stack, capacity);
            }
            for (int i = to - 1; i >= from; i--) stack[top++] = rhsSymbols[i];
        }
        return lookahead == 0;
    }

    private boolean predict(long tokenCount, LongToIntFunction tokenAt, int[] array) {
        if (startSymbol == 0) throw new IllegalStateException("no syntax grammar");
        int[] stack = new int[16];
        int top = 0;
        stack[top++] = 0;
        stack[top++] = startSymbol;
        long input = 0;
        int lookahead = tokenCount == 0 ? 0 :
                array != null ? array[0] : tokenAt.applyAsInt(0);
        // A capped work budget protects against malformed recursive source while
        // accepting multi-GB logical token views without arithmetic overflow.
        long weight = ((long) productionLhs.length + nonterminals.length + 8L) * 4L;
        long remainingSteps = tokenCount >= (Long.MAX_VALUE / Math.max(1L, weight)) - 1L
                ? Long.MAX_VALUE : Math.max(64L, (tokenCount + 1L) * weight);
        while (top != 0) {
            if (--remainingSteps < 0)
                throw new IllegalStateException("predictive parse budget exhausted");
            int symbol = stack[--top];
            if (symbol >= 0) {
                if (symbol != lookahead) return false;
                if (symbol != 0) {
                    input++;
                    lookahead = input == tokenCount ? 0 :
                            array != null ? array[(int) input] : tokenAt.applyAsInt(input);
                }
                continue;
            }
            int nt = Arrays.binarySearch(nonterminals, symbol);
            long key = ((long) nt << 32) | (lookahead & 0xffff_ffffL);
            int selected = Arrays.binarySearch(predictKeys, key);
            if (selected < 0) return false;
            int production = predictValues[selected];
            int from = rhsOffsets[production];
            int to = rhsOffsets[production + 1];
            int push = to - from;
            if ((long) top + push > MAX_PARSE_STACK) {
                throw new IllegalStateException("predictive parse stack limit exceeded");
            }
            if (top + push > stack.length) {
                int capacity = stack.length;
                while (capacity < top + push) {
                    capacity = Math.min(MAX_PARSE_STACK, capacity * 2);
                }
                stack = Arrays.copyOf(stack, capacity);
            }
            for (int i = to - 1; i >= from; i--) stack[top++] = rhsSymbols[i];
        }
        return input == tokenCount;
    }

    /**
     * Persist the deterministic build-time LL(1) decisions as a byte image.
     * A SHA-256 trailer protects against torn/corrupt image bytes; the embedded
     * grammar fingerprint binds the image to exact lexical and syntax sources.
     * The image is an optimization only, not a signed trust boundary.
     */
    public byte[] syntaxImage() {
        if (!hasSyntax()) throw new IllegalStateException("no syntax grammar");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(SYNTAX_MAGIC);
            out.writeInt(1);
            for (int i = 0; i < sha256.length(); i++) out.writeByte((byte) sha256.charAt(i));
            out.writeInt(startSymbol);
            writeInts(out, nonterminals);
            writeInts(out, terminals);
            out.writeInt(predictKeys.length);
            for (int i = 0; i < predictKeys.length; i++) {
                out.writeLong(predictKeys[i]);
                out.writeInt(predictValues[i]);
            }
            out.flush();
            byte[] body = bytes.toByteArray();
            if (body.length + 32 > MAX_SYNTAX_IMAGE) {
                throw new IllegalArgumentException("syntax image size limit exceeded");
            }
            byte[] image = Arrays.copyOf(body, body.length + 32);
            byte[] digest = sha256(body, 0, body.length);
            System.arraycopy(digest, 0, image, body.length, digest.length);
            return image;
        } catch (IOException impossibleForByteArray) {
            throw new IllegalStateException("in-memory syntax image write failed",
                    impossibleForByteArray);
        }
    }

    private static Table readSyntaxImage(byte[] image, String fingerprint, int start,
            int[] nonterminals, int[] terminals, int productionCount) {
        Objects.requireNonNull(image, "syntaxImage");
        if (image.length < 8 + 64 + 4 + 4 + 4 + 4 + 32
                || image.length > MAX_SYNTAX_IMAGE) {
            throw new IllegalArgumentException("syntax image length invalid");
        }
        int bodyLength = image.length - 32;
        byte[] computed = sha256(image, 0, bodyLength);
        for (int i = 0; i < computed.length; i++) {
            if (computed[i] != image[bodyLength + i]) {
                throw new IllegalArgumentException("syntax image SHA-256 mismatch");
            }
        }
        try (DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(image, 0, bodyLength))) {
            if (in.readInt() != SYNTAX_MAGIC || in.readInt() != 1) {
                throw new IllegalArgumentException("unsupported syntax image version");
            }
            for (int i = 0; i < fingerprint.length(); i++) {
                if (in.readUnsignedByte() != fingerprint.charAt(i)) {
                    throw new IllegalArgumentException("syntax image grammar fingerprint mismatch");
                }
            }
            if (in.readInt() != start
                    || !Arrays.equals(readInts(in, nonterminals.length), nonterminals)
                    || !Arrays.equals(readInts(in, terminals.length), terminals)) {
                throw new IllegalArgumentException("syntax image symbol identity mismatch");
            }
            int count = in.readInt();
            if (count < 0 || count > (bodyLength - (8 + 64 + 4 + 4 + nonterminals.length * 4
                    + 4 + terminals.length * 4 + 4)) / 12) {
                throw new IllegalArgumentException("syntax image decision count invalid");
            }
            long[] keys = new long[count];
            int[] values = new int[count];
            long lastKey = -1;
            for (int i = 0; i < count; i++) {
                long key = in.readLong();
                int value = in.readInt();
                int nonterminalIndex = (int) (key >>> 32);
                int tokenId = (int) key;
                if (key <= lastKey || nonterminalIndex < 0
                        || nonterminalIndex >= nonterminals.length || tokenId < 0
                        || (tokenId != 0 && Arrays.binarySearch(terminals, tokenId) < 0)
                        || value < 0 || value >= productionCount) {
                    throw new IllegalArgumentException("syntax image invalid predictive entry");
                }
                keys[i] = key;
                values[i] = value;
                lastKey = key;
            }
            if (in.available() != 0)
                throw new IllegalArgumentException("syntax image trailing bytes");
            return new Table(keys, values);
        } catch (IOException malformed) {
            throw new IllegalArgumentException("truncated/invalid syntax image", malformed);
        }
    }

    private static void writeInts(DataOutputStream out, int[] numbers) throws IOException {
        out.writeInt(numbers.length);
        for (int number : numbers) out.writeInt(number);
    }

    private static int[] readInts(DataInputStream in, int expectedLength) throws IOException {
        int size = in.readInt();
        if (size != expectedLength)
            throw new IllegalArgumentException("syntax image vector length mismatch");
        int[] result = new int[size];
        for (int i = 0; i < size; i++) result[i] = in.readInt();
        return result;
    }

    private static byte[] sha256(byte[] bytes, int offset, int length) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, offset, length);
            return md.digest();
        } catch (NoSuchAlgorithmException missingJdkProvider) {
            throw new IllegalStateException("SHA-256 unavailable", missingJdkProvider);
        }
    }

    private record Table(long[] keys, int[] values) { }

    private static Table buildTable(int start, List<Production> productions,
            int[][] rhs, int[] nonterminals, int[] terminals) {
        int n = nonterminals.length;
        boolean[] nullable = new boolean[n];
        BitSet[] first = bitSets(n);
        BitSet[] follow = bitSets(n);
        int[] lefts = new int[rhs.length];
        for (int p = 0; p < rhs.length; p++) {
            lefts[p] = Arrays.binarySearch(nonterminals, productions.get(p).lhs());
        }
        boolean changed;
        do {
            changed = false;
            for (int p = 0; p < rhs.length; p++) {
                int lhs = lefts[p];
                int before = first[lhs].cardinality();
                boolean sequenceNullable = true;
                for (int sym : rhs[p]) {
                    if (sym > 0) {
                        first[lhs].set(terminalIndex(terminals, sym));
                        sequenceNullable = false;
                        break;
                    }
                    int at = Arrays.binarySearch(nonterminals, sym);
                    first[lhs].or(first[at]);
                    if (!nullable[at]) { sequenceNullable = false; break; }
                }
                if (before != first[lhs].cardinality()) changed = true;
                if (sequenceNullable && !nullable[lhs]) { nullable[lhs] = true; changed = true; }
            }
        } while (changed);
        refuseLeftRecursion(lefts, rhs, nonterminals, nullable);
        follow[Arrays.binarySearch(nonterminals, start)].set(0);
        do {
            changed = false;
            for (int p = 0; p < rhs.length; p++) {
                BitSet trailer = (BitSet) follow[lefts[p]].clone();
                int[] sequence = rhs[p];
                for (int i = sequence.length - 1; i >= 0; i--) {
                    int sym = sequence[i];
                    if (sym > 0) {
                        trailer.clear();
                        trailer.set(terminalIndex(terminals, sym));
                    } else {
                        int at = Arrays.binarySearch(nonterminals, sym);
                        int before = follow[at].cardinality();
                        follow[at].or(trailer);
                        if (before != follow[at].cardinality()) changed = true;
                        if (nullable[at]) trailer.or(first[at]);
                        else trailer = (BitSet) first[at].clone();
                    }
                }
            }
        } while (changed);
        TreeMap<Long, Integer> decisions = new TreeMap<>();
        for (int p = 0; p < rhs.length; p++) {
            BitSet look = new BitSet();
            boolean empty = true;
            for (int sym : rhs[p]) {
                if (sym > 0) {
                    look.set(terminalIndex(terminals, sym));
                    empty = false;
                    break;
                }
                int at = Arrays.binarySearch(nonterminals, sym);
                look.or(first[at]);
                if (!nullable[at]) { empty = false; break; }
            }
            if (empty) look.or(follow[lefts[p]]);
            for (int bit = look.nextSetBit(0); bit >= 0; bit = look.nextSetBit(bit + 1)) {
                int terminal = bit == 0 ? 0 : terminals[bit - 1];
                long key = ((long) lefts[p] << 32) | (terminal & 0xffff_ffffL);
                Integer previous = decisions.putIfAbsent(key, p);
                if (previous != null) {
                    throw new IllegalArgumentException("non-LL(1) grammar conflict: "
                            + productions.get(previous).lhs() + " / lookahead " + terminal);
                }
            }
        }
        long[] keys = new long[decisions.size()];
        int[] values = new int[keys.length];
        int i = 0;
        for (Map.Entry<Long, Integer> entry : decisions.entrySet()) {
            keys[i] = entry.getKey();
            values[i++] = entry.getValue();
        }
        return new Table(keys, values);
    }

    private static BitSet[] bitSets(int size) {
        BitSet[] sets = new BitSet[size];
        for (int i = 0; i < size; i++) sets[i] = new BitSet();
        return sets;
    }

    private static int terminalIndex(int[] terminals, int id) {
        return Arrays.binarySearch(terminals, id) + 1;
    }

    private static void refuseLeftRecursion(int[] lefts, int[][] productions,
            int[] nonterminals, boolean[] nullable) {
        // Left-corner edges include nullable prefixes. Ordinary right-recursion
        // remains valid; direct/indirect non-consuming recursion is refused.
        List<BitSet> edges = new ArrayList<>(nonterminals.length);
        for (int i = 0; i < nonterminals.length; i++) edges.add(new BitSet());
        for (int p = 0; p < productions.length; p++) {
            for (int symbol : productions[p]) {
                if (symbol > 0) break;
                int at = Arrays.binarySearch(nonterminals, symbol);
                edges.get(lefts[p]).set(at);
                if (!nullable[at]) break;
            }
        }
        byte[] state = new byte[nonterminals.length];
        for (int nt = 0; nt < state.length; nt++) {
            if (state[nt] == 0) visitIteratively(nt, edges, state);
        }
    }

    private static void visitIteratively(int initial, List<BitSet> edges, byte[] state) {
        int[] frames = new int[state.length];
        int[] next = new int[state.length];
        int top = 0;
        frames[top] = initial;
        next[top] = edges.get(initial).nextSetBit(0);
        state[initial] = 1;
        top++;
        while (top > 0) {
            int at = frames[top - 1];
            int neighbor = next[top - 1];
            if (neighbor < 0) {
                state[at] = 2;
                top--;
                continue;
            }
            next[top - 1] = edges.get(at).nextSetBit(neighbor + 1);
            if (state[neighbor] == 1) {
                throw new IllegalArgumentException("left/non-consuming recursion at " + at);
            }
            if (state[neighbor] == 0) {
                frames[top] = neighbor;
                next[top] = edges.get(neighbor).nextSetBit(0);
                state[neighbor] = 1;
                top++;
            }
        }
    }

    private static String fingerprint(String lexicalDigest, List<RegexRule> rules,
            int start, List<Production> productions, int[][] rhs) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            hashText(md, "M3-GRAMMAR-SPEC-V1");
            hashText(md, lexicalDigest);
            hashInt(md, rules.size());
            for (RegexRule rule : rules) {
                hashInt(md, rule.tokenId());
                hashText(md, rule.expression());
                hashInt(md, rule.flags());
                hashInt(md, rule.precedence());
            }
            hashInt(md, start);
            hashInt(md, productions.size());
            for (int i = 0; i < rhs.length; i++) {
                hashInt(md, productions.get(i).lhs());
                hashInt(md, rhs[i].length);
                for (int symbol : rhs[i]) hashInt(md, symbol);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException impossibleOnJdk21) {
            throw new IllegalStateException("SHA-256 unavailable", impossibleOnJdk21);
        }
    }

    private static void hashText(MessageDigest md, String text) {
        hashInt(md, text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            md.update((byte) (c >>> 8));
            md.update((byte) c);
        }
    }

    private static void hashInt(MessageDigest md, int number) {
        md.update((byte) (number >>> 24));
        md.update((byte) (number >>> 16));
        md.update((byte) (number >>> 8));
        md.update((byte) number);
    }
}
