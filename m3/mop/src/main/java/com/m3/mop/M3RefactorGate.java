// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Refactor pillar: mechanical admission gate for one before/after pair of a Java source.
 *
 * <p>Order: baseline drift, CNPCDA anchors kept (multiset), both sides parse as Java 21, exposed
 * declaration contracts kept with none removed, narrowed, changed or newly exposed (type headers,
 * record components and enum constant order included), no hard-limit kind (METHOD_15, LOOP,
 * CONST_HOIST) grows, and the recipe output is a fixed point. Without the recipe's second
 * application the verdict is HOLD, never ADMITTED. Admission is projection evidence, not a proof
 * of runtime equivalence. Ported from Synexia M3RefactorGate (#13966, #14042, #14066).</p>
 */
//FLLDCIM: MOP — refactor admission: drift, anchors, parse, API, limit ratchet, fixed point
public final class M3RefactorGate {
    public static final List<String> LIMITS = List.of(M3Atom.METHOD_15, M3Atom.LOOP, M3Atom.CONST_HOIST);
    private static final int OBSERVED_LIMIT = 300;
    private static final Map<Verdict, String> LESSONS = Map.of(
            Verdict.REFUSED_DRIFT, "source changed after atomize; re-atomize, never refactor a stale snapshot",
            Verdict.REFUSED_NOT_IN_BASELINE, "file has no atomize identity; atomize first so every touched file is content-addressed",
            Verdict.REFUSED_ANCHOR_LOSS, "recipe must keep //FLLDCIM: anchors byte-for-byte; fix the recipe, not the output",
            Verdict.REFUSED_API_DRIFT, "recipe changed, narrowed, removed or newly exposed a declaration contract; needs API-scope proof",
            Verdict.REFUSED_LIMIT_RATCHET, "recipe output added METHOD_15/LOOP/CONST_HOIST violations; split or hoist inside the recipe",
            Verdict.REFUSED_NOT_FIXED_POINT, "second application still changes the output; the recipe must be idempotent",
            Verdict.REFUSED_PARSE, "recipe output does not parse as Java 21; reject at the leaf");

    public enum Verdict {
        ADMITTED, HOLD_FIXED_POINT_NOT_RUN, REFUSED_DRIFT, REFUSED_NOT_IN_BASELINE,
        REFUSED_ANCHOR_LOSS, REFUSED_PARSE, REFUSED_API_DRIFT, REFUSED_LIMIT_RATCHET,
        REFUSED_NOT_FIXED_POINT
    }

    /** One pair: {@code again} is the recipe applied to {@code after}; baseline is the atomize sha. */
    public record Input(String path, String before, String after, Optional<String> again,
            Optional<String> baselineSha256) {
        public Input {
            Stream.of(path, before, after, again, baselineSha256).forEach(Objects::requireNonNull);
        }
    }

    /** Gate decision for one file; {@code detail} is a short machine-readable reason. */
    public record Decision(String path, Verdict verdict, String detail, String beforeSha256,
            String afterSha256) {
        public boolean admitted() { return verdict == Verdict.ADMITTED; }

        public boolean refused() { return verdict.name().startsWith("REFUSED"); }

        public String status() {
            return admitted() ? M3MopReceipt.PASS : refused() ? M3MopReceipt.REFUSED : M3MopReceipt.HOLD;
        }
    }

    private M3RefactorGate() { }

    public static Decision check(Input input) {
        Function<Verdict, Function<String, Decision>> decide = verdict -> detail -> new Decision(
                input.path(), verdict, detail, M3MopReceipt.sha256(input.before()),
                M3MopReceipt.sha256(input.after()));
        Optional<Decision> early = drift(input, decide).or(() -> lostAnchors(input.before(), input.after())
                .map(lost -> decide.apply(Verdict.REFUSED_ANCHOR_LOSS).apply(lost)));
        return early.orElseGet(() -> structural(input, decide));
    }

    private static Decision structural(Input input, Function<Verdict, Function<String, Decision>> decide) {
        Optional<List<M3Atom>> before = atoms(input.path(), input.before());
        Optional<List<M3Atom>> after = atoms(input.path(), input.after());
        if (before.isEmpty() || after.isEmpty()) {
            return decide.apply(Verdict.REFUSED_PARSE).apply(before.isEmpty() ? "before" : "after");
        }
        return api(before.get(), after.get(), decide)
                .or(() -> limits(before.get(), after.get(), decide))
                .orElseGet(() -> fixedPoint(input, decide, ratchet(before.get(), after.get())));
    }

    /** Anchor lines (marker to end of line) missing after the change, or empty. */
    public static Optional<String> lostAnchors(String before, String after) {
        Map<String, Long> required = anchors(before);
        Map<String, Long> kept = anchors(after);
        String lost = required.entrySet().stream()
                .filter(entry -> kept.getOrDefault(entry.getKey(), 0L) < entry.getValue())
                .map(Map.Entry::getKey).sorted().collect(Collectors.joining(" | "));
        return lost.isEmpty() ? Optional.empty() : Optional.of(lost);
    }

    /** JSONL receipt line of one decision. */
    public static String receipt(String goal, Decision decision) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("path", decision.path());
        fields.put("verdict", decision.verdict().name());
        fields.put("beforeSha256", decision.beforeSha256());
        fields.put("afterSha256", decision.afterSha256());
        fields.put("detail", decision.detail());
        return M3MopReceipt.line(goal, "refactor-file", decision.status(), fields);
    }

    /** Appends one M3RecipeMemory candidate row per refusal; returns rows appended. */
    public static int remember(Path root, String ledger, String recipe, List<Decision> decisions)
            throws IOException {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        List<M3RecipeMemory.Entry> rows = decisions.stream().filter(Decision::refused)
                .map(decision -> entry(now, recipe, decision)).toList();
        try {
            return (int) rows.stream().map(row -> candidate(root, ledger, row)).toList().stream()
                    .filter(Boolean::booleanValue).count();
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
    }

    private static boolean candidate(Path root, String ledger, M3RecipeMemory.Entry row) {
        try {
            return M3RecipeMemory.appendCandidate(root, ledger, row);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    static M3RecipeMemory.Entry entry(Instant utc, String recipe, Decision decision) {
        String observed = M3Atom.clean(decision.path() + " " + decision.detail());
        return new M3RecipeMemory.Entry(utc, recipe, decision.verdict().name(),
                observed.substring(0, Math.min(observed.length(), OBSERVED_LIMIT)),
                "REFUSED@" + M3Atom.clean(decision.path()) + "#" + decision.afterSha256(),
                "com.m3.mop.M3RefactorGate", "M3RefactorGate." + decision.verdict().name(),
                LESSONS.get(decision.verdict()));
    }

    private static Optional<Decision> drift(Input input,
            Function<Verdict, Function<String, Decision>> decide) {
        String actual = M3MopReceipt.sha256(input.before());
        return input.baselineSha256().filter(expected -> !expected.equals(actual))
                .map(expected -> decide.apply(Verdict.REFUSED_DRIFT)
                        .apply("baseline=" + expected.substring(0, 12) + " current=" + actual.substring(0, 12)));
    }

    private static Optional<Decision> api(List<M3Atom> before, List<M3Atom> after,
            Function<Verdict, Function<String, Decision>> decide) {
        Set<String> exposedBefore = exposed(before);
        Set<String> exposedAfter = exposed(after);
        String missing = difference(exposedBefore, exposedAfter);
        String widened = difference(exposedAfter, exposedBefore);
        if (missing.isEmpty() && widened.isEmpty()) return Optional.empty();
        String detail = "missing=" + missing + " widened=" + widened;
        return Optional.of(decide.apply(Verdict.REFUSED_API_DRIFT)
                .apply(detail.substring(0, Math.min(detail.length(), 2 * OBSERVED_LIMIT))));
    }

    private static Optional<Decision> limits(List<M3Atom> before, List<M3Atom> after,
            Function<Verdict, Function<String, Decision>> decide) {
        String grown = LIMITS.stream().filter(kind -> count(after, kind) > count(before, kind))
                .map(kind -> kind + " " + count(before, kind) + "->" + count(after, kind))
                .collect(Collectors.joining(","));
        return grown.isEmpty() ? Optional.empty()
                : Optional.of(decide.apply(Verdict.REFUSED_LIMIT_RATCHET).apply(grown));
    }

    private static Decision fixedPoint(Input input, Function<Verdict, Function<String, Decision>> decide,
            String ratchet) {
        if (input.again().isEmpty()) {
            return decide.apply(Verdict.HOLD_FIXED_POINT_NOT_RUN).apply(ratchet + " fixedPoint=NOT_RUN");
        }
        return input.again().get().equals(input.after())
                ? decide.apply(Verdict.ADMITTED).apply(ratchet + " fixedPoint=PASS")
                : decide.apply(Verdict.REFUSED_NOT_FIXED_POINT).apply("second application changes output");
    }

    private static String ratchet(List<M3Atom> before, List<M3Atom> after) {
        return LIMITS.stream().map(kind -> kind + "=" + count(before, kind) + "->" + count(after, kind))
                .collect(Collectors.joining(","));
    }

    private static int count(List<M3Atom> atoms, String kind) {
        return atoms.stream().mapToInt(atom -> atom.limit(kind)).sum();
    }

    private static Set<String> exposed(List<M3Atom> atoms) {
        return atoms.stream().filter(M3Atom::exposed).map(M3Atom::signature).collect(Collectors.toSet());
    }

    private static String difference(Set<String> required, Set<String> present) {
        return required.stream().filter(value -> !present.contains(value)).sorted()
                .collect(Collectors.joining(","));
    }

    private static Optional<List<M3Atom>> atoms(String path, String source) {
        try {
            return Optional.of(M3Atomizer.parse(path, source).atoms());
        } catch (IOException | IllegalArgumentException unparsable) {
            return Optional.empty();
        }
    }

    private static Map<String, Long> anchors(String source) {
        return Stream.of(source.split("\\R")).filter(line -> line.contains(M3Atom.ANCHOR))
                .map(line -> line.substring(line.indexOf(M3Atom.ANCHOR)).strip())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
