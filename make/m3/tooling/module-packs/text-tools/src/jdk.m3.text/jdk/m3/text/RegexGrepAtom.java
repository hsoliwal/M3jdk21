// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import jdk.m3.text.M3RegexLiteralPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Optional regex-aware selection atom for the existing M3 DAG.
 * Pattern/Matcher is authoritative; only a proven literal runs on the JNI selector.
 * Does NOT replace Unix4j's grep flags, command lifecycle, or filesystem semantics.
 */
public final class RegexGrepAtom implements Atom {
    private final M3RegexLiteralPlan plan;
    private final Limits limits;
    private final M3RegexLiteralPlan.Backend backend;

    private RegexGrepAtom(M3RegexLiteralPlan plan, Limits limits, M3RegexLiteralPlan.Backend backend) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public static RegexGrepAtom compile(String regex, int flags, Limits limits,
                                        M3RegexLiteralPlan.Backend backend) {
        return new RegexGrepAtom(M3RegexLiteralPlan.compile(regex, flags), limits, backend);
    }

    public static RegexGrepAtom fromPattern(Pattern pattern, Limits limits,
                                            M3RegexLiteralPlan.Backend backend) {
        return new RegexGrepAtom(M3RegexLiteralPlan.fromPattern(pattern), limits, backend);
    }

    public M3RegexLiteralPlan.Signals precomputedSignals() { return plan.signals(); }

    @Override
    public void execute(TextFrame input, Consumer<TextLine> output) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        limits.check(input);
        // Ordinal selection is computed before any output callback. Source strings and endings
        // remain owned by the existing TextFrame; only int[] ordinals cross the atom boundary.
        List<String> contents = new ArrayList<>(input.lines().size());
        for (TextLine line : input.lines()) contents.add(line.content());
        int[] positions = plan.select(contents, backend);
        for (int position : positions) output.accept(input.lines().get(position));
    }
}
