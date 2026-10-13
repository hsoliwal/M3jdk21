// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Build-time requirement analysis of one constant regex, read from the stock JDK node tree.
 *
 * <p>The JDK parser is the only parser: the pattern is compiled with {@link Pattern#compile}
 * and its private node tree is walked through {@link M3PatternNodes}. Rules (Synexia
 * {@code MIndexRegexProgram} planner rules, applied to the JDK tree with the same shape as the
 * GPL+CPE twin {@code java.util.regex.M3PatternQuery}): a case-sensitive literal run
 * ({@code Slice}/{@code SliceS}) is required; empty-width group/branch connectors keep adjacent
 * runs together; alternation is the OR of its alternatives and an empty alternative requires
 * nothing; a body repeated at least once is required; everything else requires nothing. The
 * result is a necessary condition for any match anywhere in the input.</p>
 */
public final class M3RegexRequirement {
    /** Classification of one (pattern, flags) key. */
    public enum Kind {
        /** The compiled tree is exactly one surrogate-free case-sensitive literal. */
        PURE_LITERAL,
        /** A non-trivial necessary literal formula exists. */
        REQUIRED,
        /** Admitted tree, but every match may avoid all literals. */
        UNCONSTRAINED,
        /** Tree contains an unknown node, CANON_EQ, or exceeds the walk budget. */
        REFUSED,
        /** {@link Pattern#compile} throws; the stock exception stays authoritative. */
        INVALID
    }

    /** Requirement formula: ALL, a literal atom, or an AND/OR of sub-formulas. */
    public record Req(Op op, String literal, List<Req> terms) {
        /** Formula operators. */
        public enum Op { ALL, LIT, AND, OR }

        public static final Req ALL = new Req(Op.ALL, "", List.of());

        public static Req lit(String text) {
            return new Req(Op.LIT, text, List.of());
        }

        public static Req and(List<Req> input) {
            return combine(Op.AND, input);
        }

        public static Req or(List<Req> input) {
            return combine(Op.OR, input);
        }

        private static Req combine(Op op, List<Req> input) {
            List<Req> flat = new ArrayList<>();
            for (Req term : input) {
                if (term.op == Op.ALL) {
                    if (op == Op.OR) {
                        return ALL;
                    }
                    continue;
                }
                if (term.op == op) {
                    flat.addAll(term.terms);
                } else {
                    flat.add(term);
                }
            }
            if (flat.isEmpty()) {
                return ALL;
            }
            return flat.size() == 1 ? flat.getFirst() : new Req(op, "", List.copyOf(flat));
        }

        /** Number of literal atoms in the formula. */
        public int atomCount() {
            return op == Op.LIT ? 1 : terms.stream().mapToInt(Req::atomCount).sum();
        }
    }

    /** Analysis result for one key. */
    public record Analysis(Kind kind, Req requirement, boolean matcherStateAdmitted, String reason) {}

    private static final int MAX_NODES = 4_096;

    private M3RegexRequirement() {}

    /** Analyzes {@code pattern} compiled with {@code flags}; never throws for bad input. */
    public static Analysis analyze(String pattern, int flags) {
        Pattern compiled;
        try {
            compiled = Pattern.compile(pattern, flags);
        } catch (IllegalArgumentException invalid) {
            return new Analysis(Kind.INVALID, Req.ALL, false, invalid.getClass().getSimpleName());
        }
        return analyze(compiled);
    }

    /** Analyzes an already compiled stock pattern. */
    public static Analysis analyze(Pattern compiled) {
        if ((compiled.flags() & Pattern.CANON_EQ) != 0) {
            return new Analysis(Kind.REFUSED, Req.ALL, false, "CANON_EQ");
        }
        M3PatternNodes nodes = M3PatternNodes.instance();
        if (nodes == null) {
            return new Analysis(Kind.REFUSED, Req.ALL, false, "node tree not opened");
        }
        Object matchRoot = nodes.matchRoot(compiled);
        Object root = nodes.root(compiled);
        M3PatternNodes.Admission admission = nodes.admit(matchRoot, MAX_NODES);
        if (!admission.known()) {
            return new Analysis(Kind.REFUSED, Req.ALL, false, admission.reason());
        }
        boolean matcherState = admission.matcherStateSafe() && nodes.isStartOrBnM(root);
        String pure = nodes.pureLiteral(matchRoot);
        if (pure != null && !pure.isEmpty() && !hasSurrogate(pure)) {
            return new Analysis(Kind.PURE_LITERAL, Req.lit(pure), matcherState, "slice");
        }
        Req requirement = new Walker(nodes).sequence(matchRoot, null);
        Kind kind = requirement.op() == Req.Op.ALL ? Kind.UNCONSTRAINED : Kind.REQUIRED;
        return new Analysis(kind, requirement, matcherState, "walk");
    }

    static boolean hasSurrogate(String text) {
        return text.chars().anyMatch(unit -> Character.isSurrogate((char) unit));
    }

    /** Bounded walk over one sequencing chain; sub-chains recurse with their own stop node. */
    private static final class Walker {
        private final M3PatternNodes nodes;
        private int budget = MAX_NODES;

        Walker(M3PatternNodes nodes) {
            this.nodes = nodes;
        }

        Req sequence(Object start, Object stop) {
            List<Req> requirements = new ArrayList<>();
            StringBuilder run = new StringBuilder();
            Object node = start;
            while (node != null && node != stop && !nodes.terminal(node)) {
                if (--budget < 0) {
                    return Req.ALL;
                }
                int[] slice = nodes.caseSensitiveSlice(node);
                if (slice != null) {
                    for (int unit : slice) {
                        run.appendCodePoint(unit);
                    }
                    node = nodes.next(node);
                    continue;
                }
                if (nodes.emptyWidthConnector(node)) {
                    node = nodes.next(node);
                    continue;
                }
                flush(requirements, run);
                node = step(node, requirements);
                if (node == M3PatternNodes.ABORT) {
                    return Req.ALL;
                }
            }
            flush(requirements, run);
            return Req.and(requirements);
        }

        private Object step(Object node, List<Req> requirements) {
            if (nodes.isBranch(node)) {
                requirements.add(alternation(node));
                return nodes.next(nodes.branchConn(node));
            }
            if (nodes.isProlog(node)) {
                Object loop = nodes.prologLoop(node);
                if (nodes.loopMin(loop) >= 1) {
                    requirements.add(sequence(nodes.loopBody(loop), loop));
                }
                return nodes.next(loop);
            }
            if (nodes.isCurlyFamily(node)) {
                if (nodes.curlyMin(node) >= 1) {
                    requirements.add(sequence(nodes.curlyAtom(node), null));
                }
                return nodes.next(node);
            }
            if (nodes.isLoop(node)) {
                return M3PatternNodes.ABORT;
            }
            return nodes.next(node);
        }

        private Req alternation(Object branch) {
            Object[] atoms = nodes.branchAtoms(branch);
            Object conn = nodes.branchConn(branch);
            List<Req> alternatives = new ArrayList<>(atoms.length);
            for (Object atom : atoms) {
                if (atom == null) {
                    return Req.ALL;
                }
                alternatives.add(sequence(atom, conn));
            }
            return Req.or(alternatives);
        }

        private static void flush(List<Req> requirements, StringBuilder run) {
            if (!run.isEmpty()) {
                requirements.add(Req.lit(run.toString()));
            }
            run.setLength(0);
        }
    }
}
