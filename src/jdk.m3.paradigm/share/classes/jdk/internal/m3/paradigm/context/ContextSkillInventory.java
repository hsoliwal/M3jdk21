/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Deterministic fuzzy inventory for context-selected skills.
 *
 * <p>Selection considers semantic tags and hard operational constraints such as cost, latency,
 * task size, memory, reliability, parallelism and mutability.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dag/ContextSkillInventory.java}
 * blob {@code a7e1e2139912f8059f73c50a7adbb6b9ec09c61d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ContextSkillInventory {
    private static final Pattern SPLIT = Pattern.compile("[^a-z0-9]+");

    public record Skill(
            String id,
            String capability,
            String routeUri,
            Set<String> tags,
            int efficiencyBasisPoints,
            long unitCost,
            long latencyMicros,
            int minTaskBytes,
            int maxTaskBytes,
            long memoryBytes,
            int reliabilityBasisPoints,
            int maxParallelism,
            boolean deterministic,
            boolean readOnly) {
        public Skill {
            id = required(id, "id");
            capability = required(capability, "capability");
            routeUri = required(routeUri, "routeUri");
            tags = immutableTags(tags);
            if (efficiencyBasisPoints < 0 || efficiencyBasisPoints > 10_000
                    || unitCost < 0 || latencyMicros < 0
                    || minTaskBytes < 0 || maxTaskBytes < minTaskBytes
                    || memoryBytes < 0
                    || reliabilityBasisPoints < 0 || reliabilityBasisPoints > 10_000
                    || maxParallelism < 1) {
                throw new IllegalArgumentException("SKILL_METRICS_INVALID:" + id);
            }
        }
    }

    public record Query(
            String taskText,
            Set<String> requiredTags,
            long maxUnitCost,
            long maxLatencyMicros,
            int taskBytes,
            long maxMemoryBytes,
            int minReliabilityBasisPoints,
            int minEfficiencyBasisPoints,
            int minParallelism,
            boolean requireDeterministic,
            boolean requireReadOnly,
            int limit) {
        public Query {
            taskText = Objects.requireNonNullElse(taskText, "");
            requiredTags = immutableTags(requiredTags);
            if (maxUnitCost < 0 || maxLatencyMicros < 0 || taskBytes < 0 || maxMemoryBytes < 0
                    || minReliabilityBasisPoints < 0 || minReliabilityBasisPoints > 10_000
                    || minEfficiencyBasisPoints < 0 || minEfficiencyBasisPoints > 10_000
                    || minParallelism < 1 || limit < 1 || limit > 1024) {
                throw new IllegalArgumentException("SKILL_QUERY_INVALID");
            }
        }
    }

    public record Match(Skill skill, int fuzzyScore, long operationalPenalty, long rank) {}

    private final Map<String, Skill> skills;

    public ContextSkillInventory(Collection<Skill> skills) {
        Objects.requireNonNull(skills, "skills");
        TreeMap<String, Skill> ordered = new TreeMap<>();
        skills.forEach(skill -> {
            Skill value = Objects.requireNonNull(skill, "skill");
            if (ordered.putIfAbsent(value.id(), value) != null) {
                throw new IllegalArgumentException("DUPLICATE_SKILL:" + value.id());
            }
        });
        this.skills = Map.copyOf(ordered);
    }

    public Map<String, Skill> skills() {
        return skills;
    }

    public Skill require(String id) {
        Skill skill = skills.get(id);
        if (skill == null) {
            throw new IllegalArgumentException("UNKNOWN_SKILL:" + id);
        }
        return skill;
    }

    public List<Match> rank(Query query) {
        Objects.requireNonNull(query, "query");
        List<String> needles = tokens(query.taskText());
        return skills.values().stream()
                .filter(skill -> skill.unitCost() <= query.maxUnitCost())
                .filter(skill -> skill.latencyMicros() <= query.maxLatencyMicros())
                .filter(skill -> query.taskBytes() >= skill.minTaskBytes()
                        && query.taskBytes() <= skill.maxTaskBytes())
                .filter(skill -> skill.memoryBytes() <= query.maxMemoryBytes())
                .filter(skill -> skill.reliabilityBasisPoints() >= query.minReliabilityBasisPoints())
                .filter(skill -> skill.efficiencyBasisPoints() >= query.minEfficiencyBasisPoints())
                .filter(skill -> skill.maxParallelism() >= query.minParallelism())
                .filter(skill -> !query.requireDeterministic() || skill.deterministic())
                .filter(skill -> !query.requireReadOnly() || skill.readOnly())
                .filter(skill -> skill.tags().containsAll(query.requiredTags()))
                .map(skill -> match(skill, needles))
                .sorted(Comparator.comparingLong(Match::rank).reversed()
                        .thenComparing(match -> match.skill().unitCost())
                        .thenComparing(match -> match.skill().latencyMicros())
                        .thenComparing(match -> match.skill().id()))
                .limit(query.limit())
                .toList();
    }

    private static Match match(Skill skill, List<String> needles) {
        Set<String> haystack = searchable(skill);
        int fuzzy = needles.stream().mapToInt(needle -> bestTokenScore(needle, haystack)).sum();
        long penalty = skill.unitCost() * 16L
                + skill.latencyMicros()
                + skill.memoryBytes() / 1024L;
        long quality = (long) skill.efficiencyBasisPoints() * 32L
                + (long) skill.reliabilityBasisPoints() * 16L
                + (skill.deterministic() ? 20_000L : 0L)
                + (skill.readOnly() ? 5_000L : 0L)
                + (long) fuzzy * 1_000L;
        return new Match(skill, fuzzy, penalty, quality - penalty);
    }

    private static Set<String> searchable(Skill skill) {
        TreeSet<String> values = new TreeSet<>();
        values.addAll(tokens(skill.id()));
        values.addAll(tokens(skill.capability()));
        skill.tags().forEach(tag -> values.addAll(tokens(tag)));
        return values;
    }

    private static int bestTokenScore(String needle, Set<String> haystack) {
        int best = 0;
        for (String candidate : haystack) {
            if (needle.equals(candidate)) {
                best = Math.max(best, 100);
            } else if (candidate.startsWith(needle) || needle.startsWith(candidate)) {
                best = Math.max(best, 70);
            } else if (needle.length() >= 4 && candidate.length() >= 4) {
                int distance = editDistanceBounded(needle, candidate, 2);
                if (distance == 1) best = Math.max(best, 50);
                else if (distance == 2) best = Math.max(best, 30);
            } else if (candidate.contains(needle) || needle.contains(candidate)) {
                best = Math.max(best, 20);
            }
        }
        return best;
    }

    private static int editDistanceBounded(String left, String right, int max) {
        if (Math.abs(left.length() - right.length()) > max) return max + 1;
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            int rowMin = current[0];
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
                rowMin = Math.min(rowMin, current[j]);
            }
            if (rowMin > max) return max + 1;
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private static List<String> tokens(String input) {
        if (input == null || input.isBlank()) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String token : SPLIT.split(input.toLowerCase(Locale.ROOT))) {
            if (!token.isBlank()) result.add(token);
        }
        return new ArrayList<>(result);
    }

    private static Set<String> immutableTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return Set.of();
        TreeSet<String> result = new TreeSet<>();
        tags.forEach(tag -> result.add(required(tag, "tag").toLowerCase(Locale.ROOT)));
        return Set.copyOf(result);
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > 256) {
            throw new IllegalArgumentException("SKILL_" + field.toUpperCase(Locale.ROOT) + "_INVALID");
        }
        return normalized;
    }
}
