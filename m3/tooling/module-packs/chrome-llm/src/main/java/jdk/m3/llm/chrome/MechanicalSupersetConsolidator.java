/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/com/synexia/cpullm/chrome/MechanicalSupersetConsolidator.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Lossless exact-dedupe, conflict retention, integer entropy and mechanical-root owner. */
public final class MechanicalSupersetConsolidator {
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*|\\d+(?:\\.\\d+)?");
    private final MechanicalPatternCatalog catalog;

    public MechanicalSupersetConsolidator(MechanicalPatternCatalog catalog) {
        this.catalog = catalog;
    }

    public SupersetBundle consolidate(List<SourceRecord> input) {
        List<SourceRecord> records = input.stream()
                .sorted(Comparator.comparing(SourceRecord::sourceRef))
                .toList();
        List<EvidenceNode> evidence = records.stream().flatMap(r -> catalog.extract(r).stream()).toList();

        Map<String, List<EvidenceNode>> exact = new TreeMap<>();
        for (EvidenceNode node : evidence) exact.computeIfAbsent(exactKey(node), k -> new ArrayList<>()).add(node);

        List<MechanicalClaim> claims = new ArrayList<>();
        for (List<EvidenceNode> group : exact.values()) {
            EvidenceNode first = group.getFirst();
            List<String> evidenceIds = group.stream().map(EvidenceNode::evidenceId).sorted().toList();
            List<String> sourceRefs = group.stream().map(EvidenceNode::sourceRef).distinct().sorted().toList();
            String claimId = Hashing.hash("CPULLM_JAVA_MECHANICAL_CLAIM_V1",
                    first.structuralKey(), first.kind(), first.text(), first.boundary(), first.authority(),
                    String.join(",", evidenceIds), String.join(",", sourceRefs));
            claims.add(new MechanicalClaim(claimId, first.structuralKey(), first.kind(), first.text(),
                    first.boundary(), first.authority(), evidenceIds, sourceRefs));
        }
        claims.sort(Comparator.comparing(MechanicalClaim::claimId));

        Map<String, List<MechanicalClaim>> byKey = new TreeMap<>();
        for (MechanicalClaim claim : claims) byKey.computeIfAbsent(claim.structuralKey(), k -> new ArrayList<>()).add(claim);
        List<ConflictSet> conflicts = new ArrayList<>();
        for (var entry : byKey.entrySet()) {
            List<MechanicalClaim> alternatives = entry.getValue();
            if (alternatives.stream().map(MechanicalClaim::text).distinct().count() < 2) continue;
            int top = alternatives.stream().mapToInt(c -> c.authority().rank()).max().orElseThrow();
            List<String> ids = alternatives.stream().filter(c -> c.authority().rank() == top)
                    .map(MechanicalClaim::claimId).sorted().toList();
            if (ids.size() > 1) {
                conflicts.add(new ConflictSet(Hashing.hashList("CPULLM_JAVA_CONFLICT_V1",
                        concat(List.of(entry.getKey()), ids)), entry.getKey(), ids));
            }
        }

        byte[] raw = Hashing.utf8(records.stream().map(SourceRecord::text)
                .collect(Collectors.joining()));
        List<String> tokens = new ArrayList<>();
        for (SourceRecord record : records) {
            var matcher = TOKEN.matcher(record.text());
            while (matcher.find()) tokens.add(matcher.group());
        }

        int duplicateOccurrences = Math.max(0, evidence.size() - claims.size());
        int uniqueStructures = (int) claims.stream().map(MechanicalClaim::structuralKey).distinct().count();
        String semanticRoot = Hashing.hash("CPULLM_JAVA_STRUCTURED_ENTROPY_V1",
                claims.stream().map(MechanicalClaim::claimId).toList(),
                conflicts.stream().map(ConflictSet::conflictId).toList());
        StructuredCodeEntropy entropy = new StructuredCodeEntropy(
                records.size(), raw.length, evidence.size(), claims.size(), uniqueStructures,
                duplicateOccurrences, ppm(duplicateOccurrences, evidence.size()), ppm(claims.size(), evidence.size()),
                ppm(uniqueStructures, claims.size()), conflicts.size(), entropyBytes(raw), entropyTokens(tokens), semanticRoot);

        List<String> sourceRoots = records.stream()
                .map(r -> Hashing.hash("CPULLM_JAVA_SOURCE_RECORD_V1", r.sourceRef(), r.scope(), r.kind(),
                        r.boundary(), r.text(), r.authority()))
                .toList();
        String mechanicalRoot = Hashing.hash("CPULLM_JAVA_MECHANICAL_SUPERSET_V1",
                sourceRoots, claims.stream().map(MechanicalClaim::claimId).toList(),
                conflicts.stream().map(ConflictSet::conflictId).toList(), semanticRoot);

        return new SupersetBundle(evidence, claims, conflicts, entropy, mechanicalRoot,
                true, false, false, false);
    }

    private static String exactKey(EvidenceNode n) {
        return String.join("\u001f", n.structuralKey(), n.kind().name(), n.text(), n.boundary(), n.authority().name());
    }

    private static int ppm(int numerator, int denominator) {
        return denominator == 0 ? 0 : (int) (((long) numerator * 1_000_000L) / denominator);
    }

    private static int entropyBytes(byte[] values) {
        if (values.length == 0) return 0;
        int[] counts = new int[256];
        for (byte value : values) counts[value & 0xff]++;
        double entropy = 0.0;
        for (int count : counts) if (count > 0) {
            double p = (double) count / values.length;
            entropy -= p * (Math.log(p) / Math.log(2.0));
        }
        return (int) Math.round(entropy * 1000.0);
    }

    private static int entropyTokens(List<String> values) {
        if (values.isEmpty()) return 0;
        Map<String, Integer> counts = new HashMap<>();
        for (String value : values) counts.merge(value, 1, Integer::sum);
        double entropy = 0.0;
        for (int count : counts.values()) {
            double p = (double) count / values.size();
            entropy -= p * (Math.log(p) / Math.log(2.0));
        }
        return (int) Math.round(entropy * 1000.0);
    }

    private static List<String> concat(List<String> left, List<String> right) {
        LinkedHashSet<String> all = new LinkedHashSet<>(left);
        all.addAll(right);
        return List.copyOf(all);
    }
}
