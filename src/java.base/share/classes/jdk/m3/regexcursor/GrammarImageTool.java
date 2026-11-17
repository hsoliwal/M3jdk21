// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Offline build-time compiler for trusted M3 tabular lexical specifications and
 * bounded EBNF grammars. Emits the versioned, hash-checked LL(1) .m3grammar
 * binary table; the runtime still uses its existing literal and regex owners.
 * This is an authoring tool and must not be installed into java.base.
 */
public final class GrammarImageTool {
    private static final int MAX_SOURCE_SIZE = 1_048_576;

    private GrammarImageTool() { }

    /** Compile a language specification: lexicon.tsv, grammar.ebnf, start rule, output image. */
    public static void main(String[] args) throws IOException {
        if (args.length != 4 && args.length != 5) {
            throw new IllegalArgumentException("Usage: GrammarImageTool lexicon.tsv grammar.ebnf "
                    + "startRule syntax.m3grammar [complete.m3image]");
        }
        Path input = Path.of(args[0]);
        Path ebnf = Path.of(args[1]);
        String start = args[2];
        Path target = Path.of(args[3]);
        if (Files.size(input) > MAX_SOURCE_SIZE || Files.size(ebnf) > MAX_SOURCE_SIZE) {
            throw new IllegalArgumentException("language specification file exceeds size limit");
        }
        String language = null;
        String version = null;
        List<StaticGrammarLexicon.Literal> fixed = new ArrayList<>();
        List<GrammarSpecification.RegexRule> regex = new ArrayList<>();
        Map<String, Integer> symbols = new HashMap<>();
        HashSet<Integer> lexicalIds = new HashSet<>();
        for (String line : Files.readAllLines(input, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length < 2) throw new IllegalArgumentException("malformed lexicon row");
            switch (parts[0]) {
                case "language" -> {
                    if (parts.length != 2 || language != null)
                        throw new IllegalArgumentException("duplicate language metadata");
                    language = parts[1];
                }
                case "revision" -> {
                    if (parts.length != 2 || version != null)
                        throw new IllegalArgumentException("duplicate revision metadata");
                    version = parts[1];
                }
                case "literal" -> {
                    if (parts.length != 4)
                        throw new IllegalArgumentException("literal row must have four columns");
                    int id = positive(parts[1]);
                    boolean contextual = switch(parts[2]) {
                        case "true" -> true;
                        case "false" -> false;
                        default -> throw new IllegalArgumentException("invalid contextual role");
                    };
                    fixed.add(new StaticGrammarLexicon.Literal(parts[3], id, contextual));
                    lexicalIds.add(id);
                    declare(symbols, parts[3], id);
                }
                case "regex" -> {
                    if (parts.length != 5)
                        throw new IllegalArgumentException("regex row must have five columns");
                    int id = positive(parts[1]);
                    regex.add(new GrammarSpecification.RegexRule(id,parts[4],
                            Integer.parseInt(parts[2]),Integer.parseInt(parts[3])));
                    lexicalIds.add(id);
                }
                case "symbol" -> {
                    if (parts.length != 3)
                        throw new IllegalArgumentException("symbol row must have three columns");
                    declare(symbols,parts[1],positive(parts[2]));
                }
                default -> throw new IllegalArgumentException("unknown lexicon row " + parts[0]);
            }
        }
        if (language == null || version == null) {
            throw new IllegalArgumentException("language and revision metadata required");
        }
        for (int id : symbols.values()) {
            if (!lexicalIds.contains(id))
                throw new IllegalArgumentException("symbol references unknown token " + id);
        }
        EbnfGrammar.Normalized normalized=EbnfGrammar.normalize(
                Files.readString(ebnf,StandardCharsets.UTF_8),start,symbols);
        GrammarSpecification compiled=GrammarSpecification.compile(language,version,
                fixed,regex,normalized.startSymbol(),normalized.productions());
        byte[] image=compiled.syntaxImage();
        Path parent=target.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(target,image);
        System.out.printf("M3 grammar image: %s, language=%s, revision=%s, tokenCount=%d, "
                + "productions=%d, bytes=%d, sha256=%s%n",
                target,compiled.language(),compiled.revision(),compiled.terminalCount(),
                compiled.productionCount(),image.length,compiled.grammarSha256());
        if (args.length == 5) {
            Path complete=Path.of(args[4]);
            Path fullParent=complete.toAbsolutePath().getParent();
            if (fullParent != null) Files.createDirectories(fullParent);
            byte[] sealed=compiled.image();
            Files.write(complete,sealed);
            System.out.printf("Complete grammar image: %s, bytes=%d, artifactSha256=%s%n",
                    complete,sealed.length,GrammarSpecification.imageSha256(sealed));
        }
    }

    private static int positive(String text) {
        int id=Integer.parseInt(text);
        if (id<=0) throw new IllegalArgumentException("token IDs must be positive");
        return id;
    }

    private static void declare(Map<String,Integer> names,String name,int id) {
        if (name.isEmpty()) throw new IllegalArgumentException("empty symbol name");
        Integer previous=names.putIfAbsent(name,id);
        if (previous != null && previous != id) {
            throw new IllegalArgumentException("duplicate symbol name with conflicting IDs "
                    + name);
        }
    }
}
