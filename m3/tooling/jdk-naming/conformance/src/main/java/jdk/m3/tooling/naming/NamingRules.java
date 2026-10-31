/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.naming;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The M3JDK21 naming rules of {@code NAMING_RULES.tsv}: ordered text rules, kept lines, kept spans and kept paths.
 * This is the Java executor of the same table the rename recipe {@code jdk_naming.py} runs; a line is conformant when
 * {@link #rewrite} leaves it unchanged.
 */
public final class NamingRules {

    /** One ordered text rule: the first matching rule rewrites a token. */
    public record Rule(String name, Pattern pattern, String replacement) {
    }

    private static final Pattern GROUP_REFERENCE = Pattern.compile("\\\\(\\d)");
    private static final Pattern SPAN_TOKEN = Pattern.compile("\u0000(\\d+)\u0000");

    private final List<Rule> textRules;
    private final Pattern keepLine;
    private final Pattern keepSpan;
    private final Pattern keepPath;

    private NamingRules(List<Rule> textRules, Pattern keepLine, Pattern keepSpan, Pattern keepPath) {
        this.textRules = textRules;
        this.keepLine = keepLine;
        this.keepSpan = keepSpan;
        this.keepPath = keepPath;
    }

    /** Reads the table ({@code kind name pattern replacement note}, one header line). */
    public static NamingRules load(Path tsv) throws IOException {
        List<Rule> text = new ArrayList<>();
        List<String> keepLine = new ArrayList<>();
        List<String> keepSpan = new ArrayList<>();
        List<String> keepPath = new ArrayList<>();
        List<String> lines = Files.readAllLines(tsv, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] field = line.split("\t", -1);
            switch (field[0]) {
                case "TEXT" -> text.add(new Rule(field[1], Pattern.compile(field[2]), replacement(field[3])));
                case "KEEP_LINE" -> keepLine.add(field[2]);
                case "KEEP_SPAN" -> keepSpan.add(field[2]);
                case "KEEP_PATH" -> keepPath.add(field[2]);
                default -> throw new IllegalArgumentException("unknown rule kind " + field[0]);
            }
        }
        return new NamingRules(List.copyOf(text), join(keepLine), join(keepSpan), join(keepPath));
    }

    /** A Python {@code \1} group reference becomes {@code $1}; any other replacement is literal. */
    static String replacement(String python) {
        Matcher reference = GROUP_REFERENCE.matcher(python);
        return reference.find() ? reference.replaceAll("\\$$1") : Matcher.quoteReplacement(python);
    }

    private static Pattern join(List<String> alternatives) {
        return Pattern.compile(String.join("|", alternatives));
    }

    public List<Rule> textRules() {
        return textRules;
    }

    /** True for a legal or attribution file that keeps its name. */
    public boolean keepsPath(String path) {
        return keepPath.matcher(path).find();
    }

    /** The path renamed by the text rules (paths and references to them always agree). */
    public String renamedPath(String path) {
        return rewrite(path, new java.util.HashMap<>());
    }

    /**
     * The line with every rule applied outside the kept spans; {@code counts} receives one entry per rule that
     * matched. A kept line is returned as it is.
     */
    public String rewrite(String line, Map<String, Integer> counts) {
        if (keepLine.matcher(line).find()) {
            return line;
        }
        List<String> spans = new ArrayList<>();
        String text = mask(line, spans);
        for (Rule rule : textRules) {
            text = apply(rule, text, counts);
        }
        return restore(text, spans);
    }

    private String mask(String line, List<String> spans) {
        Matcher stash = keepSpan.matcher(line);
        StringBuilder masked = new StringBuilder();
        while (stash.find()) {
            spans.add(stash.group());
            stash.appendReplacement(masked, Matcher.quoteReplacement("\u0000" + (spans.size() - 1) + "\u0000"));
        }
        stash.appendTail(masked);
        return masked.toString();
    }

    private static String apply(Rule rule, String text, Map<String, Integer> counts) {
        Matcher matcher = rule.pattern().matcher(text);
        if (!matcher.find()) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        int matches = 0;
        do {
            matcher.appendReplacement(out, rule.replacement());
            matches++;
        } while (matcher.find());
        matcher.appendTail(out);
        counts.merge(rule.name(), matches, Integer::sum);
        return out.toString();
    }

    private static String restore(String text, List<String> spans) {
        Matcher token = SPAN_TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (token.find()) {
            token.appendReplacement(out, Matcher.quoteReplacement(spans.get(Integer.parseInt(token.group(1)))));
        }
        token.appendTail(out);
        return out.toString();
    }
}
