/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/com/synexia/cpullm/chrome/MechanicalPatternCatalog.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory + strategy chain for all mechanical line patterns carried by the existing
 * Python multipass lane: headings, declarations, imports, constraints, failures,
 * decisions, causal statements, list items, branches, code/contract/tool/text fallback,
 * plus fenced code blocks.
 */
public final class MechanicalPatternCatalog {
    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s+(.+?)\\s*$");
    private static final Pattern DECL = Pattern.compile("^\\s*(?:async\\s+)?(?:def|class|function|interface|type|record|enum|struct|trait)\\s+([A-Za-z_$][\\w$]*)\\b");
    private static final Pattern IMPORT = Pattern.compile("^\\s*(?:import\\b|from\\s+\\S+\\s+import\\b|#include\\b|using\\b)");
    private static final Pattern CONSTRAINT = Pattern.compile("\\b(?:MUST|SHALL|NEVER|REQUIRED|FORBIDDEN|INVARIANT|CONTRACT)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAILURE = Pattern.compile("\\b(?:failed|failure|error|cannot|blocked|rejected|residue)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DECISION = Pattern.compile("\\b(?:decision|decided|chosen|choose|selected|adopt|reject)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAUSAL = Pattern.compile("\\b(?:because|due to|therefore|result|hence)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST = Pattern.compile("^\\s*(?:[-*+] |\\d+[.)]\\s+)");
    private static final Pattern BRANCH = Pattern.compile("\\b(?:if|elif|else|for|while|match|case|try|except|catch|switch)\\b");

    private final List<EvidencePattern> patterns;

    public MechanicalPatternCatalog(List<EvidencePattern> patterns) {
        this.patterns = List.copyOf(patterns);
    }

    public static MechanicalPatternCatalog defaultCatalog() {
        List<EvidencePattern> p = new ArrayList<>();
        p.add(regex(HEADING, PatternKind.HEADING, "heading", Authority.OBSERVED, 1));
        p.add(regex(DECL, PatternKind.DECLARATION, "declaration", Authority.OBSERVED, 1));
        p.add(regex(IMPORT, PatternKind.IMPORT, "import", Authority.OBSERVED, 0));
        p.add(regex(CONSTRAINT, PatternKind.CONSTRAINT, "constraint", Authority.OBSERVED, 0));
        p.add(regex(FAILURE, PatternKind.FAILURE, "failure", Authority.OBSERVED, 0));
        p.add(regex(DECISION, PatternKind.DECISION, "decision", Authority.OBSERVED, 0));
        p.add(regex(CAUSAL, PatternKind.CAUSAL, "causal", Authority.OBSERVED, 0));
        p.add(regex(LIST, PatternKind.LIST_ITEM, "list-item", Authority.OBSERVED, 0));
        p.add(regex(BRANCH, PatternKind.BRANCH, "branch", Authority.OBSERVED, 0));
        return new MechanicalPatternCatalog(p);
    }

    public List<EvidenceNode> extract(SourceRecord source) {
        List<EvidenceNode> nodes = new ArrayList<>();
        String[] lines = source.text().split("\\R", -1);
        List<String> fenced = new ArrayList<>();
        int fenceStart = -1;
        for (int i = 0; i < lines.length; i++) {
            int ordinal = i + 1;
            String line = lines[i].replace("\r", "");
            if (line.stripLeading().startsWith("```")) {
                if (fenced.isEmpty()) {
                    fenceStart = ordinal;
                    fenced.add(line);
                } else {
                    fenced.add(line);
                    nodes.add(node(source, fenceStart, PatternKind.CODE_BLOCK, String.join("\n", fenced),
                            "code-block:" + source.boundary() + ":" + Hashing.hash("code-block", String.join("\n", fenced)),
                            Authority.OBSERVED));
                    fenced.clear();
                    fenceStart = -1;
                }
                continue;
            }
            if (!fenced.isEmpty()) {
                fenced.add(line);
                continue;
            }
            if (line.isBlank()) continue;
            EvidencePattern.PatternMatch match = classify(source, ordinal, line);
            nodes.add(node(source, ordinal, match.kind(), line, match.structuralKey(), match.authority()));
        }
        if (!fenced.isEmpty()) {
            String block = String.join("\n", fenced);
            nodes.add(node(source, fenceStart, PatternKind.CODE_BLOCK, block,
                    "code-block:" + source.boundary() + ":" + Hashing.hash("code-block", block), Authority.OBSERVED));
        }
        return List.copyOf(nodes);
    }

    private EvidencePattern.PatternMatch classify(SourceRecord source, int ordinal, String line) {
        for (EvidencePattern pattern : patterns) {
            Optional<EvidencePattern.PatternMatch> match = pattern.match(source, ordinal, line);
            if (match.isPresent()) return match.get();
        }
        PatternKind kind = switch (source.kind().toLowerCase(Locale.ROOT)) {
            case "code" -> PatternKind.CODE;
            case "contract" -> PatternKind.CONTRACT;
            case "tool" -> PatternKind.TOOL;
            default -> PatternKind.TEXT;
        };
        Authority base = kind == PatternKind.TEXT ? Authority.DESCRIPTION_CANDIDATE : Authority.OBSERVED;
        return new EvidencePattern.PatternMatch(kind,
                kind.name().toLowerCase(Locale.ROOT) + ":" + source.boundary() + ":" + Hashing.hash(kind.name(), normalize(line)),
                MechanicalModel.Authority.max(source.authority(), base));
    }

    private static EvidencePattern regex(
            Pattern pattern, PatternKind kind, String prefix, Authority baseAuthority, int captureGroup) {
        return (source, ordinal, line) -> {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.find()) return Optional.empty();
            String discriminator = captureGroup > 0 && matcher.groupCount() >= captureGroup
                    ? normalize(matcher.group(captureGroup))
                    : Hashing.hash(prefix, normalize(line));
            return Optional.of(new EvidencePattern.PatternMatch(
                    kind,
                    prefix + ":" + source.boundary() + ":" + discriminator,
                    MechanicalModel.Authority.max(source.authority(), baseAuthority)));
        };
    }

    private static EvidenceNode node(
            SourceRecord source, int ordinal, PatternKind kind, String text, String key, Authority authority) {
        String id = Hashing.hash("CPULLM_JAVA_EVIDENCE_V1", source.sourceRef(), ordinal, kind, text, key,
                source.scope(), source.boundary(), authority);
        return new EvidenceNode(id, source.sourceRef(), ordinal, kind, text, key,
                source.scope(), source.boundary(), authority);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
