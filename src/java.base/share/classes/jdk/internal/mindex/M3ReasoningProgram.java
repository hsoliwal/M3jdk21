/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Synexia-owned compact reasoning language compiled to deterministic primitive rules.
 *
 * <p>Grammar, one statement per line (optional trailing period):</p>
 * <ul>
 *   <li>{@code fact alpha}</li>
 *   <li>{@code fact !alpha}</li>
 *   <li>{@code rule alpha & beta -> gamma}</li>
 *   <li>{@code rule alpha & !beta -> !gamma}</li>
 *   <li>{@code support alpha -> beta}</li>
 *   <li>{@code attack alpha -> beta}</li>
 * </ul>
 *
 * <p>The implementation is intentionally monotone and finite. It owns its parser and fixed-point
 * semantics; no external reasoning framework is linked or exposed.</p>
 */
public final class M3ReasoningProgram {
  static final int RELATION_SUPPORT = 1;
  static final int RELATION_ATTACK = 2;
  private static final int MAX_SYMBOLS = 65_536;
  private static final int MAX_RULES = 262_144;
  private static final int MAX_ANTECEDENTS = 64;

  private final String[] symbols;
  private final Map<String, Integer> symbolRows;
  private final BitSet positiveFacts;
  private final BitSet negativeFacts;
  private final Rule[] rules;
  private final Relation[] relations;
  private final String canonicalText;
  private final String rootHash;

  private M3ReasoningProgram(
      String[] symbols,
      BitSet positiveFacts,
      BitSet negativeFacts,
      Rule[] rules,
      Relation[] relations,
      String canonicalText) {
    this.symbols = symbols;
    HashMap<String, Integer> rows = new HashMap<>();
    for (int i = 0; i < symbols.length; i++) rows.put(symbols[i], i);
    this.symbolRows = Map.copyOf(rows);
    this.positiveFacts = (BitSet) positiveFacts.clone();
    this.negativeFacts = (BitSet) negativeFacts.clone();
    this.rules = rules.clone();
    this.relations = relations.clone();
    this.canonicalText = canonicalText;
    this.rootHash = M3PrecomputeHash.sha256("SYNEXIA_REASONING_PROGRAM_V1", canonicalText);
  }

  public static M3ReasoningProgram compile(String source) {
    Objects.requireNonNull(source, "source");
    ArrayList<String> factTexts = new ArrayList<>();
    ArrayList<RawRule> rawRules = new ArrayList<>();
    ArrayList<RawRelation> rawRelations = new ArrayList<>();
    TreeSet<String> symbols = new TreeSet<>();

    String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
      String line = stripComment(lines[lineNumber]).trim();
      if (line.isEmpty()) continue;
      if (line.endsWith(".")) line = line.substring(0, line.length() - 1).trim();

      if (line.startsWith("fact ")) {
        Literal literal = parseLiteral(line.substring(5).trim(), lineNumber + 1);
        symbols.add(literal.symbol);
        factTexts.add(literal.canonical());
      } else if (line.startsWith("rule ")) {
        String body = line.substring(5).trim();
        int arrow = body.indexOf("->");
        if (arrow < 0 || body.indexOf("->", arrow + 2) >= 0) {
          throw syntax(lineNumber + 1, "rule requires one ->");
        }
        String left = body.substring(0, arrow).trim();
        String right = body.substring(arrow + 2).trim();
        if (left.isEmpty() || right.isEmpty()) throw syntax(lineNumber + 1, "empty rule side");

        String[] terms = left.split("\\s*&\\s*");
        if (terms.length > MAX_ANTECEDENTS) {
          throw syntax(lineNumber + 1, "rule antecedent bound exceeded");
        }
        ArrayList<Literal> antecedents = new ArrayList<>(terms.length);
        for (String term : terms) {
          Literal literal = parseLiteral(term.trim(), lineNumber + 1);
          antecedents.add(literal);
          symbols.add(literal.symbol);
        }
        Literal conclusion = parseLiteral(right, lineNumber + 1);
        symbols.add(conclusion.symbol);
        rawRules.add(new RawRule(List.copyOf(antecedents), conclusion));
      } else if (line.startsWith("support ") || line.startsWith("attack ")) {
        boolean support = line.startsWith("support ");
        String body = line.substring(support ? 8 : 7).trim();
        int arrow = body.indexOf("->");
        if (arrow < 0 || body.indexOf("->", arrow + 2) >= 0) {
          throw syntax(lineNumber + 1, "relation requires one ->");
        }
        String left = body.substring(0, arrow).trim();
        String right = body.substring(arrow + 2).trim();
        String sourceSymbol = parsePositiveSymbol(left, lineNumber + 1);
        String targetSymbol = parsePositiveSymbol(right, lineNumber + 1);
        symbols.add(sourceSymbol);
        symbols.add(targetSymbol);
        rawRelations.add(
            new RawRelation(
                sourceSymbol,
                targetSymbol,
                support ? RELATION_SUPPORT : RELATION_ATTACK));
      } else {
        throw syntax(lineNumber + 1, "unknown statement");
      }
    }

    if (symbols.size() > MAX_SYMBOLS) {
      throw new IllegalArgumentException("reasoning symbol bound exceeded");
    }
    if (rawRules.size() > MAX_RULES) {
      throw new IllegalArgumentException("reasoning rule bound exceeded");
    }

    String[] symbolArray = symbols.toArray(String[]::new);
    HashMap<String, Integer> rows = new HashMap<>();
    for (int i = 0; i < symbolArray.length; i++) rows.put(symbolArray[i], i);

    factTexts.sort(String::compareTo);
    BitSet positiveFacts = new BitSet(symbolArray.length);
    BitSet negativeFacts = new BitSet(symbolArray.length);
    for (String text : factTexts) {
      Literal literal = parseLiteral(text, 0);
      int row = rows.get(literal.symbol);
      (literal.negative ? negativeFacts : positiveFacts).set(row);
    }

    rawRules.sort(Comparator.comparing(RawRule::canonical));
    Rule[] rules = new Rule[rawRules.size()];
    for (int i = 0; i < rules.length; i++) {
      RawRule raw = rawRules.get(i);
      int[] antecedents = raw.antecedents.stream()
          .mapToInt(literal -> encodeLiteral(rows.get(literal.symbol), literal.negative))
          .toArray();
      Arrays.sort(antecedents);
      rules[i] =
          new Rule(
              antecedents,
              encodeLiteral(rows.get(raw.conclusion.symbol), raw.conclusion.negative));
    }

    rawRelations.sort(Comparator.comparing(RawRelation::canonical));
    Relation[] relations = new Relation[rawRelations.size()];
    for (int i = 0; i < relations.length; i++) {
      RawRelation raw = rawRelations.get(i);
      relations[i] = new Relation(rows.get(raw.source), rows.get(raw.target), raw.kind);
    }

    StringBuilder canonical = new StringBuilder(1024);
    for (String symbol : symbolArray) canonical.append("symbol\t").append(symbol).append('\n');
    for (String fact : factTexts) canonical.append("fact\t").append(fact).append('\n');
    for (RawRule rule : rawRules) canonical.append("rule\t").append(rule.canonical()).append('\n');
    for (RawRelation relation : rawRelations) {
      canonical.append(relation.kind == RELATION_SUPPORT ? "support\t" : "attack\t")
          .append(relation.source).append("->").append(relation.target).append('\n');
    }

    return new M3ReasoningProgram(
        symbolArray,
        positiveFacts,
        negativeFacts,
        rules,
        relations,
        canonical.toString());
  }

  public M3ReasoningClosure evaluate() {
    return M3ReasoningClosure.evaluate(this);
  }

  public int symbolCount() { return symbols.length; }
  public int ruleCount() { return rules.length; }
  public int relationCount() { return relations.length; }
  public String rootHash() { return rootHash; }
  public String canonicalText() { return canonicalText; }

  public long retainedBytes() {
    long bytes = 0L;
    for (String symbol : symbols) bytes += (long) symbol.length() * Character.BYTES;
    bytes += positiveFacts.toLongArray().length * (long) Long.BYTES;
    bytes += negativeFacts.toLongArray().length * (long) Long.BYTES;
    for (Rule rule : rules) bytes += (long) rule.antecedents.length * Integer.BYTES + Integer.BYTES;
    bytes += (long) relations.length * 3L * Integer.BYTES;
    return bytes;
  }

  int row(String symbol) {
    Integer row = symbolRows.get(Objects.requireNonNull(symbol, "symbol"));
    return row == null ? -1 : row;
  }

  String symbol(int row) { return symbols[row]; }
  BitSet positiveFacts() { return (BitSet) positiveFacts.clone(); }
  BitSet negativeFacts() { return (BitSet) negativeFacts.clone(); }
  Rule[] rules() { return rules.clone(); }
  Relation[] relations() { return relations.clone(); }

  static boolean literalSatisfied(int encoded, BitSet positive, BitSet negative) {
    int row = literalRow(encoded);
    return encoded < 0 ? negative.get(row) : positive.get(row);
  }

  static int literalRow(int encoded) {
    return Math.abs(encoded) - 1;
  }

  static boolean literalNegative(int encoded) {
    return encoded < 0;
  }

  private static int encodeLiteral(int row, boolean negative) {
    int value = Math.addExact(row, 1);
    return negative ? -value : value;
  }

  private static Literal parseLiteral(String text, int line) {
    String value = text.trim();
    boolean negative = value.startsWith("!");
    String symbol = negative ? value.substring(1).trim() : value;
    validateSymbol(symbol, line);
    return new Literal(symbol, negative);
  }

  private static String parsePositiveSymbol(String text, int line) {
    String value = text.trim();
    if (value.startsWith("!")) throw syntax(line, "support/attack endpoints must be positive symbols");
    validateSymbol(value, line);
    return value;
  }

  private static void validateSymbol(String symbol, int line) {
    if (!symbol.matches("[A-Za-z_][A-Za-z0-9_.:-]*")) {
      throw syntax(line, "invalid symbol: " + symbol);
    }
  }

  private static String stripComment(String line) {
    int marker = line.indexOf('#');
    return marker < 0 ? line : line.substring(0, marker);
  }

  private static IllegalArgumentException syntax(int line, String message) {
    return new IllegalArgumentException(
        (line > 0 ? "reasoning line " + line + ": " : "reasoning: ") + message);
  }

  static final class Rule {
    private final int[] antecedents;
    private final int conclusion;

    Rule(int[] antecedents, int conclusion) {
      this.antecedents = antecedents.clone();
      this.conclusion = conclusion;
    }

    int[] antecedents() { return antecedents; }
    int conclusion() { return conclusion; }
  }

  record Relation(int source, int target, int kind) {}

  private record Literal(String symbol, boolean negative) {
    String canonical() { return negative ? "!" + symbol : symbol; }
  }

  private record RawRule(List<Literal> antecedents, Literal conclusion) {
    String canonical() {
      ArrayList<String> terms = new ArrayList<>(antecedents.size());
      for (Literal literal : antecedents) terms.add(literal.canonical());
      terms.sort(String::compareTo);
      return String.join("&", terms) + "->" + conclusion.canonical();
    }
  }

  private record RawRelation(String source, String target, int kind) {
    String canonical() {
      return (kind == RELATION_SUPPORT ? "support:" : "attack:") + source + "->" + target;
    }
  }
}
