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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ContextGrail v9 heuristic and v10 probabilistic sense selection. Scores are not calibrated probabilities of truth.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/LexicalEngine.java}
 * blob {@code 078204322e7caefd87e26c6984fb2954aff194f7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LexicalEngine {
    public record Sense(String id, String word, String domain, String meaning, double weight, ContextGuard evidence) {
        public Sense {
            Objects.requireNonNull(id); Objects.requireNonNull(word); Objects.requireNonNull(domain);
            Objects.requireNonNull(meaning); Objects.requireNonNull(evidence);
            if (!Double.isFinite(weight)) throw new IllegalArgumentException("Non-finite weight");
            word = normalizeCase(word);
        }
    }
    public record Candidate(Sense sense, double score, double probability) {}
    public record Resolution(String token, List<Candidate> candidates, Optional<Candidate> accepted) {
        public Resolution { candidates = List.copyOf(candidates); Objects.requireNonNull(accepted); }
    }
    /** Packs are explicitly selected by the host; unrelated locales are never silently loaded together. */
    public record Pack(String id, Map<String, String> normalizers, Map<String, ContextGuard> phrases) {
        public Pack {
            Objects.requireNonNull(id);
            normalizers = Map.copyOf(normalizers); phrases = Map.copyOf(phrases);
        }
    }
    private final Map<String, String> normalizers;
    private final Map<String, ContextGuard> phrases;
    private final Map<String, List<String>> synsets;
    private final List<Sense> senses;

    public LexicalEngine(List<Pack> packs, Map<String, List<String>> synsets, List<Sense> senses) {
        normalizers = packs.stream().flatMap(pack -> pack.normalizers().entrySet().stream()).collect(
                Collectors.toUnmodifiableMap(entry -> normalizeCase(entry.getKey()), Map.Entry::getValue,
                        LexicalEngine::sameOrReject));
        phrases = packs.stream().flatMap(pack -> pack.phrases().entrySet().stream()).collect(
                Collectors.toUnmodifiableMap(entry -> normalizeCase(entry.getKey()), Map.Entry::getValue,
                        (left, right) -> { throw new IllegalArgumentException("Duplicate phrase across packs"); }));
        this.synsets = synsets.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                entry -> normalizeCase(entry.getKey()), entry -> entry.getValue().stream().map(LexicalEngine::normalizeCase).distinct().sorted().toList()));
        this.senses = List.copyOf(senses);
        if (senses.stream().map(Sense::id).distinct().count() != senses.size())
            throw new IllegalArgumentException("Duplicate sense ID");
    }
    private static String sameOrReject(String left, String right) {
        if (!left.equals(right)) throw new IllegalArgumentException("Conflicting lexical pack normalization");
        return left;
    }
    private static String normalizeCase(String word) { return Objects.requireNonNull(word).strip().toLowerCase(Locale.ROOT); }
    public String normalize(String token) { return normalizers.getOrDefault(normalizeCase(token), normalizeCase(token)); }
    public List<String> canonicalForms(String word) {
        var normalized = normalizeCase(word);
        return synsets.entrySet().stream().filter(entry -> entry.getKey().equals(normalized) || entry.getValue().contains(normalized))
                .map(Map.Entry::getKey).sorted().toList();
    }
    /** Unknown phrases fail closed; callers must explicitly choose an unconstrained guard. */
    public ContextGuard phraseGuard(String phrase) {
        ContextGuard guard = phrases.get(normalizeCase(phrase));
        if (guard == null) throw new IllegalArgumentException("Unknown lexical phrase: " + phrase);
        return guard;
    }

    public Resolution resolve(String word, ContextVector context, double minimumConfidence) {
        if (!Double.isFinite(minimumConfidence) || minimumConfidence < 0 || minimumConfidence > 1)
            throw new IllegalArgumentException("Confidence must be in [0, 1]");
        String base = normalizeCase(normalize(word));
        var rules = senses.stream().filter(sense -> sense.word().equals(base)).sorted(Comparator.comparing(Sense::id)).toList();
        if (rules.isEmpty()) return new Resolution(base, List.of(), Optional.empty());
        List<Double> scores = rules.stream().map(sense -> Math.max(0.01, sense.weight() + sense.evidence().atoms().stream()
                .mapToDouble(atom -> atom.matches(context) ? 0.2 : -0.2).sum())).toList();
        double maximum = scores.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        List<Double> exponentials = scores.stream().map(score -> Math.exp(score - maximum)).toList();
        double sum = exponentials.stream().mapToDouble(Double::doubleValue).sum();
        List<Candidate> candidates = java.util.stream.IntStream.range(0, rules.size())
                .mapToObj(i -> new Candidate(rules.get(i), scores.get(i), exponentials.get(i) / sum))
                .sorted(Comparator.comparingDouble(Candidate::probability).reversed().thenComparing(candidate -> candidate.sense().id())).toList();
        return new Resolution(base, candidates, candidates.stream().findFirst().filter(candidate -> candidate.probability() >= minimumConfidence));
    }

    /** Deterministic v9 behavior is kept separate, not replaced by the probabilistic interpretation. */
    public String resolveHeuristic(String word, ContextVector context) {
        String base = normalizeCase(normalize(word));
        Function<String, String> text = key -> normalizeCase(Objects.toString(context.values().get(key), ""));
        String location = text.apply("Location");
        String speaker = text.apply("Role.Speaker");
        if (speaker.isEmpty()) speaker = text.apply("SpeakerRole");
        if (base.equals("drive")) {
            if (List.of("road", "commute", "highway").contains(location) || truthy(context.values().get("Transport"))) return "operate vehicle";
            if (location.equals("office") && List.of("manager", "lead").contains(speaker)) return "motivate team";
            if (location.equals("home") && List.of("mother", "father", "parent").contains(speaker)) return "encourage child";
            return "propel/lead";
        }
        if (base.equals("map")) return List.of("road", "commute").contains(location) || text.apply("Intent").equals("gotooffice")
                ? "navigate route" : "spatial representation";
        return List.of("office", "home", "hospital").contains(base) ? "place:" + base : "generic";
    }
    private static boolean truthy(Object value) {
        return value != null && !Boolean.FALSE.equals(value) && !"".equals(value)
                && (!(value instanceof Number n) || n.doubleValue() != 0.0);
    }
    public static List<Sense> defaultSenses() {
        return List.of(
                new Sense("drive.traffic", "drive", "Traffic", "operate vehicle", .6, ContextGuard.of(
                        ContextGuard.anyOf("Location", "Road", "Highway", "Commute"),
                        new ContextGuard.Atom("Transport", "Transport present", (value, context) -> value != null))),
                new Sense("drive.office", "drive", "Office", "motivate team", .25, ContextGuard.of(
                        ContextGuard.eq("Location", "Office"), ContextGuard.anyOf("Role.Speaker", "Manager", "Lead"))),
                new Sense("drive.home", "drive", "Home", "encourage child", .15, ContextGuard.of(
                        ContextGuard.eq("Location", "Home"), ContextGuard.anyOf("Role.Speaker", "Mother", "Father", "Parent"))),
                new Sense("map.navigation", "map", "Navigation", "navigate route", .7, ContextGuard.of(
                        ContextGuard.eq("Intent", "GoToOffice"), ContextGuard.anyOf("Location", "Road", "Commute"))),
                new Sense("map.generic", "map", "Generic", "spatial representation", .3, ContextGuard.always()));
    }
}
