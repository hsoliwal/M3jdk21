// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import jdk.m3.text.M3RegexRequiredSegments;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Additive host-approved Atom using M3 primitive native necessary-literal filtering.
 * This does not change existing Unix4j Command<?> semantics or RegexGrepAtom.
 */
public final class RegexRequiredSegmentsAtom implements Atom {
    private final M3RegexRequiredSegments plan;
    private final Limits limits;
    private final M3RegexRequiredSegments.Backend backend;

    private RegexRequiredSegmentsAtom(M3RegexRequiredSegments plan, Limits limits,
                                       M3RegexRequiredSegments.Backend backend) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public static RegexRequiredSegmentsAtom compile(String regex, int flags,
                                                     Limits limits,
                                                     M3RegexRequiredSegments.Backend backend) {
        return new RegexRequiredSegmentsAtom(M3RegexRequiredSegments.compile(regex, flags), limits, backend);
    }

    public static RegexRequiredSegmentsAtom fromPattern(Pattern pattern, Limits limits,
                                                         M3RegexRequiredSegments.Backend backend) {
        return new RegexRequiredSegmentsAtom(M3RegexRequiredSegments.fromPattern(pattern), limits, backend);
    }

    public M3RegexRequiredSegments.Facts precomputedFacts() { return plan.facts(); }

    @Override public void execute(TextFrame input, Consumer<TextLine> output) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        limits.check(input);
        ArrayList<String> contents = new ArrayList<>(input.lines().size());
        input.lines().forEach(line -> contents.add(line.content()));
        int[] indices = plan.select(contents, backend);
        FrameCollector staged = new FrameCollector(limits);
        for (int index : indices) staged.accept(input.lines().get(index));
        // Callbacks only after all native, Java and output-budget checks have passed.
        staged.frame().lines().forEach(output);
    }
}
