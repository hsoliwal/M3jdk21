// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A bounded importer for the documented M3 EBNF subset into normalized grammar
 * productions consumed by {@link GrammarSpecification}. This is a build-time
 * authoring tool, never a replacement for an authoritative language lexer.
 *
 * <p>Accepted syntax: {@code name = expression ;} or {@code name ::= expression ;},
 * {@code |} alternatives, comma-separated or adjacent concatenation, {@code ()}
 * grouping, {@code []} optionality, {@code {}} zero-or-more repetition,
 * and postfix {@code ?}, {@code *}, {@code +} quantifiers.
 * Terminal names and quoted literals must already be in the caller's lexicon
 * mapping. The importer does not infer keyword/identifier boundaries, lexical
 * modes, case-folding, Unicode transformations, or ABNF-specific semantics.</p>
 */
public final class EbnfGrammar {
    private static final int MAX_RULES = 4_096;
    private static final int MAX_NESTING = 96;
    private static final int MAX_SOURCE_CHARS = 1_048_576;
    private static final int MAX_PRODUCTIONS = 32_768;

    public record Normalized(int startSymbol,
            List<GrammarSpecification.Production> productions,
            Map<String, Integer> nonterminals) {
        public Normalized {
            productions = List.copyOf(Objects.requireNonNull(productions));
            nonterminals = Map.copyOf(Objects.requireNonNull(nonterminals));
        }
    }

    private sealed interface Expr permits Sequence, Alternative, Optional, Repeated, Symbol { }
    private record Sequence(List<Expr> members) implements Expr { }
    private record Alternative(List<Expr> branches) implements Expr { }
    private record Optional(Expr contents) implements Expr { }
    private record Repeated(Expr contents, boolean atLeastOne) implements Expr { }
    private record Symbol(String name) implements Expr { }

    private final String source;
    private final Map<String, Integer> terminals;
    private final LinkedHashMap<String, Expr> definitions = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> nonterminals = new LinkedHashMap<>();
    private final List<GrammarSpecification.Production> productions = new ArrayList<>();
    private int position;
    private int nextNonterminal = -1;
    private int depth;

    private EbnfGrammar(String source, Map<String, Integer> terminals) {
        this.source = source;
        this.terminals = Map.copyOf(terminals);
        if (source.length() > MAX_SOURCE_CHARS) {
            throw new IllegalArgumentException("EBNF source limit exceeded");
        }
        for (var terminal : this.terminals.entrySet()) {
            if (terminal.getKey().isEmpty() || terminal.getValue() <= 0) {
                throw new IllegalArgumentException("invalid terminal mapping " + terminal);
            }
        }
    }

    /** Parse EBNF into deterministic, array-backed normalized BNF production rows. */
    public static Normalized normalize(String specification, String startRule,
            Map<String, Integer> terminalSymbols) {
        Objects.requireNonNull(startRule, "startRule");
        EbnfGrammar parser = new EbnfGrammar(Objects.requireNonNull(specification, "specification"),
                Objects.requireNonNull(terminalSymbols, "terminalSymbols"));
        parser.readDefinitions();
        if (!parser.definitions.containsKey(startRule)) {
            throw new IllegalArgumentException("undefined start rule: " + startRule);
        }
        // Discover every named nonterminal first, then resolve all references.
        for (String name : new TreeMap<>(parser.definitions).keySet()) {
            if (parser.terminals.containsKey(name)) {
                throw new IllegalArgumentException("name is both terminal and rule: " + name);
            }
            parser.nonterminals.put(name, parser.allocate());
        }
        for (var definition : new TreeMap<>(parser.definitions).entrySet()) {
            parser.emit(definition.getValue(), parser.nonterminals.get(definition.getKey()));
        }
        return new Normalized(parser.nonterminals.get(startRule), parser.productions,
                parser.nonterminals);
    }

    private int allocate() {
        if (-nextNonterminal >= MAX_PRODUCTIONS * 2) {
            throw new IllegalArgumentException("EBNF synthetic rule limit exceeded");
        }
        return nextNonterminal--;
    }

    private void emit(Expr expression, int lhs) {
        if (expression instanceof Alternative alt) {
            for (Expr branch : alt.branches) emit(branch, lhs);
        } else if (expression instanceof Sequence sequence) {
            int[] rhs = new int[sequence.members.size()];
            for (int i = 0; i < rhs.length; i++) rhs[i] = symbol(sequence.members.get(i));
            add(lhs, rhs);
        } else if (expression instanceof Optional optional) {
            add(lhs, new int[] {symbol(optional.contents)});
            add(lhs, new int[0]);
        } else if (expression instanceof Repeated repeated) {
            if (repeated.atLeastOne) {
                // Avoid FIRST/FIRST ambiguity from H -> Body Tail | Body.
                int tail = allocate();
                add(lhs, new int[] {symbol(repeated.contents), tail});
                add(tail, new int[] {symbol(repeated.contents), tail});
                add(tail, new int[0]);
            } else {
                add(lhs, new int[] {symbol(repeated.contents), lhs});
                add(lhs, new int[0]);
            }
        } else {
            add(lhs, new int[] {symbol(expression)});
        }
    }

    private int symbol(Expr expression) {
        if (expression instanceof Symbol ref) {
            Integer nonterminal = nonterminals.get(ref.name);
            if (nonterminal != null) return nonterminal;
            Integer terminal = terminals.get(ref.name);
            if (terminal != null) return terminal;
            throw new IllegalArgumentException("undefined EBNF symbol: " + ref.name);
        }
        int synthetic = allocate();
        emit(expression, synthetic);
        return synthetic;
    }

    private void add(int lhs, int[] rhs) {
        if (productions.size() >= MAX_PRODUCTIONS) {
            throw new IllegalArgumentException("EBNF production limit exceeded");
        }
        productions.add(new GrammarSpecification.Production(lhs, rhs));
    }

    private void readDefinitions() {
        skip();
        while (position < source.length()) {
            if (definitions.size() == MAX_RULES) throw bad("EBNF rule limit exceeded");
            String name = identifier();
            if (definitions.containsKey(name)) throw bad("duplicate rule: " + name);
            skip();
            if (consume("::=")) { /* BNF assignment spelling */ }
            else expect('=');
            Expr rhs = expression(';');
            expect(';');
            definitions.put(name, rhs);
            skip();
        }
        if (definitions.isEmpty()) throw bad("empty EBNF specification");
    }

    private Expr expression(char stop) {
        if (++depth > MAX_NESTING) throw bad("EBNF nesting limit exceeded");
        try {
            List<Expr> branches = new ArrayList<>();
            branches.add(sequence(stop));
            skip();
            while (consume('|')) {
                branches.add(sequence(stop));
                skip();
            }
            return branches.size() == 1 ? branches.get(0) : new Alternative(List.copyOf(branches));
        } finally {
            depth--;
        }
    }

    private Expr sequence(char stop) {
        List<Expr> members = new ArrayList<>();
        skip();
        while (position < source.length()) {
            char next = source.charAt(position);
            if (next == stop || next == '|' || next == ')' || next == ']' || next == '}') break;
            if (next == ',') {
                if (members.isEmpty()) throw bad("unexpected comma");
                position++;
                skip();
                if (position == source.length() || ";|)]},".indexOf(source.charAt(position)) >= 0) {
                    throw bad("trailing comma");
                }
            }
            if (position < source.length() && source.charAt(position) == ',') {
                throw bad("repeated comma");
            }
            members.add(atom());
            skip();
        }
        return new Sequence(List.copyOf(members));
    }

    private Expr atom() {
        skip();
        if (position == source.length()) throw bad("unexpected end of EBNF expression");
        char c = source.charAt(position);
        Expr expression;
        if (c == '(' || c == '[' || c == '{') {
            position++;
            char end = c == '(' ? ')' : c == '[' ? ']' : '}';
            Expr contents = expression(end);
            expect(end);
            expression = c == '(' ? contents : c == '[' ? new Optional(contents)
                    : new Repeated(contents, false);
        } else if (c == '\'' || c == '"') expression = new Symbol(quoted());
        else expression = new Symbol(identifier());
        skip();
        if (consume('?')) return new Optional(expression);
        if (consume('*')) return new Repeated(expression, false);
        if (consume('+')) return new Repeated(expression, true);
        return expression;
    }

    private String quoted() {
        char quote = source.charAt(position++);
        StringBuilder spelling = new StringBuilder();
        while (position < source.length()) {
            char c = source.charAt(position++);
            if (c == quote) return spelling.toString();
            if (c == '\\' && position < source.length()) {
                char escaped = source.charAt(position++);
                c = switch (escaped) {
                    case '\\' -> '\\';
                    case '\'' -> '\'';
                    case '"' -> '"';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> throw bad("unknown quoted escape " + escaped);
                };
            }
            spelling.append(c);
        }
        throw bad("unterminated quoted terminal");
    }

    private String identifier() {
        skip();
        int from = position;
        if (position >= source.length() || !isFirst(source.charAt(position))) {
            throw bad("expected EBNF identifier");
        }
        position++;
        while (position < source.length() && isRest(source.charAt(position))) position++;
        return source.substring(from, position);
    }

    private static boolean isFirst(char c) {
        return c == '_' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
    }

    private static boolean isRest(char c) {
        return isFirst(c) || c >= '0' && c <= '9' || c == '-';
    }

    private boolean consume(char c) {
        skip();
        if (position < source.length() && source.charAt(position) == c) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consume(String spelling) {
        skip();
        if (source.startsWith(spelling, position)) {
            position += spelling.length();
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!consume(c)) throw bad("expected '" + c + "'");
    }

    private void skip() {
        while (position < source.length()) {
            char c = source.charAt(position);
            if (Character.isWhitespace(c)) {position++;continue;}
            if (source.startsWith("(*", position)) {
                int close = source.indexOf("*)", position + 2);
                if (close < 0) throw bad("unterminated EBNF comment");
                position = close + 2;
                continue;
            }
            break;
        }
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message + " at UTF-16 offset " + position);
    }
}
