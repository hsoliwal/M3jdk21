// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/** Hash-bound resource adaptation using existing XML and exact-text OpenRewrite owners. */
public final class M3CollectionsPackageRenameResourcesCli {
    private M3CollectionsPackageRenameResourcesCli() { }

    record Replacement(String oldValue, String newValue) { }
    record Result(String text, int oldOccurrences, int postimageOccurrences) { }
    private record Candidate(String relativePath, Path path, String before, Result result) { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4
                || args.length == 4 && !"--apply".equals(args[3])) {
            throw new IllegalArgumentException("ROOT MANIFEST_JSON RECEIPT_TSV [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath();
        JsonNode manifest = new ObjectMapper().readTree(Path.of(args[1]).toFile());
        if (!manifest.isArray() || manifest.isEmpty()) {
            throw new IllegalArgumentException("Nonempty manifest array required");
        }
        Map<String, List<Replacement>> files = new LinkedHashMap<>();
        Map<String, String> hashes = new LinkedHashMap<>();
        for (JsonNode row : manifest) {
            String path = required(row, "path");
            if (!path.replace('\\', '/').equals(path) || path.startsWith("/")
                    || path.chars().anyMatch(Character::isISOControl)
                    || !Path.of(path).normalize().toString().replace('\\', '/').equals(path)) {
                throw new IllegalArgumentException("Noncanonical resource path: " + path);
            }
            requireKind(path);
            M3CollectionsPackageRenameCli.source(root, path);
            String hash = required(row, "expected_sha256");
            if (!hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid source hash");
            String prior = hashes.putIfAbsent(path, hash);
            if (prior != null && !prior.equals(hash)) throw new IllegalArgumentException("Conflicting hashes");
            Replacement rule = new Replacement(required(row, "old"), required(row, "new"));
            if (rule.oldValue().equals(rule.newValue())) throw new IllegalArgumentException("No-op manifest rule");
            List<Replacement> rules = files.computeIfAbsent(path, ignored -> new ArrayList<>());
            if (rules.stream().anyMatch(r -> r.oldValue().equals(rule.oldValue()))) {
                throw new IllegalArgumentException("Duplicate resource rule: " + path);
            }
            rules.add(rule);
        }
        List<Candidate> candidates = new ArrayList<>();
        StringBuilder receipt = new StringBuilder(
                "path\tbefore_sha256\tafter_sha256\told_occurrences\tpostimage_occurrences\tchanged\tfixedpoint\n");
        for (String path : files.keySet().stream().sorted().toList()) {
            Path target = M3CollectionsPackageRenameCli.source(root, path);
            String before = Files.readString(target);
            String hash = M3CollectionsPackageRenameMetadataRecipe.sha256(before);
            if (!hash.equals(hashes.get(path))) throw new IllegalArgumentException("Resource source hash drift: " + path);
            Result result = rewrite(path, before, files.get(path));
            Result fixed = rewrite(path, result.text(), files.get(path));
            if (!fixed.text().equals(result.text()) || fixed.oldOccurrences() != 0) {
                throw new IllegalStateException("Resource fixedpoint refused: " + path);
            }
            candidates.add(new Candidate(path, target, before, result));
            receipt.append(path).append('\t').append(hash).append('\t')
                    .append(M3CollectionsPackageRenameMetadataRecipe.sha256(result.text())).append('\t')
                    .append(result.oldOccurrences()).append('\t').append(fixed.postimageOccurrences())
                    .append('\t').append(!before.equals(result.text())).append("\tPASS\n");
        }
        // Every source is checked before any candidate is allowed to mutate a target.
        for (Candidate candidate : candidates) {
            if (!Files.readString(candidate.path()).equals(candidate.before())) {
                throw new IllegalStateException("Concurrent resource drift: " + candidate.relativePath());
            }
        }
        Path receiptPath = Path.of(args[2]).toAbsolutePath();
        Files.createDirectories(receiptPath.getParent());
        Path output = receiptPath.getParent().resolve("resource-candidates");
        for (Candidate candidate : candidates) {
            Path destination = output.resolve(candidate.relativePath());
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, candidate.result().text());
        }
        Files.writeString(receiptPath, receipt.toString());
        if (args.length == 4) {
            for (Candidate candidate : candidates) {
                if (!candidate.before().equals(candidate.result().text())) {
                    Files.writeString(candidate.path(), candidate.result().text());
                }
            }
        }
        System.out.println("resource_files=" + candidates.size() + " changed="
                + candidates.stream().filter(c -> !c.before().equals(c.result().text())).count()
                + " apply=" + (args.length == 4));
    }

    static Result rewrite(String path, String source, List<Replacement> rules) {
        requireKind(path);
        if (path.endsWith(".xml")) return xml(path, source, rules);
        String current = source;
        int oldCount = 0, postCount = 0;
        for (Replacement rule : rules) {
            int old = count(current, rule.oldValue()), post = count(current, rule.newValue());
            if (old + post == 0) throw new IllegalArgumentException("Resource token absent: " + path);
            oldCount += old;
            postCount += post;
            var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
            SourceFile text = PlainText.builder().sourcePath(Path.of(path)).text(current).build();
            var changes = exactTextRecipe(rule, path)
                    .run(new InMemoryLargeSourceSet(List.of(text)), context).getChangeset().getAllResults();
            if (!changes.isEmpty()) current = changes.getFirst().getAfter().printAll();
        }
        return new Result(current, oldCount, postCount);
    }

    // Retain source compatibility with the canonical 8.17.1 API while a separately
    // qualified newer provider adds its optional plaintextOnly constructor argument.
    // Quoted regex/replacement tokens avoid the newer literal fast path escaping a literal dollar.
    private static Recipe exactTextRecipe(Replacement rule, String path) {
        try {
            try {
                return FindAndReplace.class.getConstructor(String.class, String.class, Boolean.class,
                        Boolean.class, Boolean.class, Boolean.class, String.class)
                        .newInstance(java.util.regex.Pattern.quote(rule.oldValue()), java.util.regex.Matcher.quoteReplacement(rule.newValue()), true, true, false, false, path);
            } catch (NoSuchMethodException newerProvider) {
                return FindAndReplace.class.getConstructor(String.class, String.class, Boolean.class,
                        Boolean.class, Boolean.class, Boolean.class, String.class, Boolean.class)
                        .newInstance(java.util.regex.Pattern.quote(rule.oldValue()), java.util.regex.Matcher.quoteReplacement(rule.newValue()), true, true, false, false, path, true);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported existing FindAndReplace provider API", failure);
        }
    }

    private static Result xml(String path, String source, List<Replacement> rules) {
        for (Replacement rule : rules) {
            if (rule.oldValue().chars().anyMatch(c -> c == '<' || c == '>' || c == '&')
                    || rule.newValue().chars().anyMatch(c -> c == '<' || c == '>' || c == '&')) {
                throw new IllegalArgumentException("XML namespace metadata cannot inject markup/entities: " + path);
            }
        }
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        List<SourceFile> parsed = XmlParser.builder().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), source)), null, context).toList();
        if (parsed.size() != 1 || !(parsed.getFirst() instanceof Xml.Document document)
                || !document.printAll().equals(source)) {
            throw new IllegalArgumentException("Lossless XML parse refused: " + path);
        }
        int[] oldCounts = new int[rules.size()], postCounts = new int[rules.size()];
        Xml.Document after = (Xml.Document) new XmlIsoVisitor<Void>() {
            private String scalar(String value, boolean attribute) {
                String result = value;
                for (int i = 0; i < rules.size(); i++) {
                    Replacement rule = rules.get(i);
                    boolean quoted = quoted(rule.oldValue()) && quoted(rule.newValue());
                    if (quoted && !attribute) continue;
                    String old = quoted ? unquote(rule.oldValue()) : rule.oldValue();
                    String post = quoted ? unquote(rule.newValue()) : rule.newValue();
                    oldCounts[i] += count(result, old);
                    postCounts[i] += count(result, post);
                    result = result.replace(old, post);
                }
                return result;
            }
            @Override public Xml.CharData visitCharData(Xml.CharData data, Void unused) {
                if (data.isCdata()) return data;
                return data.withText(scalar(data.getText(), false));
            }
            @Override public Xml.Attribute visitAttribute(Xml.Attribute attribute, Void unused) {
                return attribute.withValue(attribute.getValue().withValue(
                        scalar(attribute.getValue().getValue(), true)));
            }
        }.visit(document, null);
        int old = 0, post = 0;
        for (int i = 0; i < rules.size(); i++) {
            if (oldCounts[i] + postCounts[i] == 0) {
                throw new IllegalArgumentException("XML scalar token absent (comments/CDATA are opaque): " + path);
            }
            old += oldCounts[i]; post += postCounts[i];
        }
        return new Result(after.printAll(), old, post);
    }

    private static boolean quoted(String token) {
        return token.length() >= 2 && (token.charAt(0) == '"' || token.charAt(0) == '\'')
                && token.charAt(token.length() - 1) == token.charAt(0);
    }
    private static String unquote(String token) { return token.substring(1, token.length() - 1); }
    private static int count(String text, String token) {
        if (token.isEmpty()) throw new IllegalArgumentException("Empty resource token");
        int result = 0, index = 0;
        while ((index = text.indexOf(token, index)) >= 0) { result++; index += token.length(); }
        return result;
    }
    private static void requireKind(String path) {
        if (!(path.endsWith(".xml") || path.endsWith(".pro") || path.endsWith(".stg")
                || path.endsWith(".bnd") || path.endsWith(".scala")
                || path.equals("src/main/javadoc/overview.html"))) {
            throw new IllegalArgumentException("Unsupported metadata source kind: " + path);
        }
    }
    private static String required(JsonNode row, String name) {
        if (!row.hasNonNull(name) || !row.get(name).isTextual() || row.get(name).asText().isEmpty()) {
            throw new IllegalArgumentException("Missing resource text field: " + name);
        }
        return row.get(name).asText();
    }
}
