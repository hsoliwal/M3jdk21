// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarBuilder.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One-time builder that compiles named grammar symbols into an immutable primitive image. */
public final class M3GrammarBuilder {
  private final String identity;
  private final M3GrammarFamily family;
  private final M3GrammarStrictness strictness;
  private final M3LexerProfile lexerProfile;
  private final Map<SymbolKey,Integer> symbols = new LinkedHashMap<>();
  private final List<M3GrammarRef> symbolRefs = new ArrayList<>();
  private final List<ProductionSpec> productions = new ArrayList<>();
  private final List<M3GrammarSource> sources = new ArrayList<>();
  private String start;

  public M3GrammarBuilder(String identity, M3GrammarFamily family, M3GrammarStrictness strictness, M3LexerProfile lexerProfile) {
    this.identity = Objects.requireNonNull(identity, "identity");
    this.family = Objects.requireNonNull(family, "family");
    this.strictness = Objects.requireNonNull(strictness, "strictness");
    this.lexerProfile = Objects.requireNonNull(lexerProfile, "lexerProfile");
  }

  public M3GrammarBuilder source(M3GrammarSource source) { sources.add(Objects.requireNonNull(source, "source")); return this; }
  public M3GrammarBuilder start(String nonTerminal) { start = Objects.requireNonNull(nonTerminal, "nonTerminal"); nonTerminal(nonTerminal); return this; }
  public M3GrammarBuilder nonTerminal(String name) { intern(M3GrammarRef.nonTerminal(name)); return this; }
  public M3GrammarBuilder literal(String text) { intern(M3GrammarRef.literal(text)); return this; }
  public M3GrammarBuilder token(M3TokenClass tokenClass) { intern(M3GrammarRef.token(tokenClass)); return this; }

  public M3GrammarBuilder production(String lhs, M3GrammarRef... rhs) {
    Objects.requireNonNull(lhs, "lhs"); Objects.requireNonNull(rhs, "rhs");
    nonTerminal(lhs);
    M3GrammarRef[] copy = rhs.clone();
    for (M3GrammarRef symbol : copy) intern(Objects.requireNonNull(symbol, "rhs symbol"));
    productions.add(new ProductionSpec(lhs, copy));
    return this;
  }

  public M3GrammarBuilder alternatives(String lhs, M3GrammarRef[]... alternatives) {
    Objects.requireNonNull(alternatives, "alternatives");
    for (M3GrammarRef[] alternative : alternatives) production(lhs, alternative);
    return this;
  }

  public M3Grammar build() {
    if (start == null) throw new M3GrammarException("grammar has no start symbol");
    if (productions.isEmpty() && strictness != M3GrammarStrictness.FREE) throw new M3GrammarException("grammar has no productions");

    int symbolCount = symbolRefs.size();
    String[] names = new String[symbolCount];
    byte[] kinds = new byte[symbolCount];
    byte[] tokenClasses = new byte[symbolCount];
    byte[] literalCases = new byte[symbolCount];
    java.util.Arrays.fill(tokenClasses, (byte) -1);
    for (int id = 0; id < symbolCount; id++) {
      M3GrammarRef symbol = symbolRefs.get(id);
      names[id] = symbol.value();
      kinds[id] = (byte) symbol.kind().ordinal();
      literalCases[id] = (byte) symbol.literalCase().ordinal();
      if (symbol.tokenClass() != null) tokenClasses[id] = (byte) symbol.tokenClass().ordinal();
    }

    int productionCount = productions.size();
    int[] lhs = new int[productionCount];
    int[] rhsStart = new int[productionCount + 1];
    int rhsTotal = 0;
    for (ProductionSpec p : productions) rhsTotal = Math.addExact(rhsTotal, p.rhs().length);
    int[] rhs = new int[rhsTotal];
    int cursor = 0;
    for (int production = 0; production < productionCount; production++) {
      ProductionSpec p = productions.get(production);
      lhs[production] = id(M3GrammarRef.nonTerminal(p.lhs()));
      rhsStart[production] = cursor;
      for (M3GrammarRef symbol : p.rhs()) rhs[cursor++] = id(symbol);
    }
    rhsStart[productionCount] = cursor;

    int[] lhsCounts = new int[symbolCount];
    for (int value : lhs) lhsCounts[value]++;
    for (int symbol = 0; symbol < symbolCount; symbol++) {
      if (Byte.toUnsignedInt(kinds[symbol]) == M3GrammarSymbolKind.NON_TERMINAL.ordinal()
          && lhsCounts[symbol] == 0) {
        throw new M3GrammarException(
            "non-terminal has no production: " + names[symbol]);
      }
    }

    int[] lhsProductionStart = new int[symbolCount + 1];
    for (int i = 0; i < symbolCount; i++) lhsProductionStart[i + 1] = lhsProductionStart[i] + lhsCounts[i];
    int[] productionByLhs = new int[productionCount];
    int[] fill = lhsProductionStart.clone();
    for (int p = 0; p < productionCount; p++) productionByLhs[fill[lhs[p]]++] = p;

    return M3Grammar.compile(
        identity, family, strictness, lexerProfile, List.copyOf(sources), id(M3GrammarRef.nonTerminal(start)),
        names, kinds, tokenClasses, literalCases, lhs, rhsStart, rhs, lhsProductionStart, productionByLhs);
  }

  private int intern(M3GrammarRef ref) {
    SymbolKey key =
        new SymbolKey(ref.kind(), ref.value(), ref.tokenClass(), ref.literalCase());
    Integer existing = symbols.get(key);
    if (existing != null) return existing;
    int id = symbolRefs.size(); symbols.put(key, id); symbolRefs.add(ref); return id;
  }
  private int id(M3GrammarRef ref) {
    Integer value =
        symbols.get(new SymbolKey(ref.kind(), ref.value(), ref.tokenClass(), ref.literalCase()));
    if (value == null) throw new M3GrammarException("unknown symbol " + ref);
    return value;
  }
  private record SymbolKey(
      M3GrammarSymbolKind kind,
      String value,
      M3TokenClass tokenClass,
      M3LiteralCase literalCase) {}
  private record ProductionSpec(String lhs, M3GrammarRef[] rhs) {}
}
