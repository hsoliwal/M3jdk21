/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.util.regex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import jdk.internal.mindex.M3TQ;

/**
 * Derives the conservative {@link M3TQ} trigram requirement of a compiled pattern from its
 * node tree, for the {@code Matcher} absence gate that previously served only {@code LITERAL}
 * patterns.
 *
 * <p>Rules (adapted from the Synexia {@code MIndexRegexProgram} trigram planner onto the JDK
 * node tree, which stays the only parser): a case-sensitive literal run ({@code Slice},
 * {@code SliceS}) is required; empty-width sequencing nodes keep adjacent runs together;
 * alternation is the OR of its alternatives and an empty alternative requires nothing;
 * a group or atom repeated at least once requires its body; anything repeated zero or more
 * times, every character class, back reference, anchor and lookaround requires nothing. The
 * result is a necessary condition only: when it evaluates false over the exact UTF-16 trigram
 * facts of the search region, no match exists in that region.</p>
 *
 * <p>Admission keeps a pruned search observably identical to a failed search: patterns whose
 * root is not a {@code Start} or {@code BnM} scan (so {@code hitEnd} would not be set), patterns
 * containing a node that can set {@code requireEnd} ({@code Dollar}, {@code UnixDollar},
 * {@code Bound}, {@code GraphemeBound}, {@code Neg}), {@code CANON_EQ} patterns and any node
 * class this walker does not know yield no query at all.</p>
 */
final class M3PatternQuery {
    private static final int MAX_NODES = 4_096;

    private M3PatternQuery() {}

    /**
     * Returns the requirement every {@code find} match of the pattern satisfies, or {@code null}
     * when the pattern is not admitted or carries no constraint.
     */
    static M3TQ derive(Pattern.Node matchRoot, Pattern.Node root) {
        if (!(root instanceof Pattern.Start) && !(root instanceof Pattern.BnM)) {
            return null;
        }
        if (!admits(matchRoot)) {
            return null;
        }
        M3TQ query = new Walker().sequence(matchRoot, null);
        return query != null && query.hasConstraints() ? query : null;
    }

    /**
     * Bounded Boolean full-match plan over the JDK's compiled nodes, not another parser.
     * Only straight-line character atoms, exact slices and greedy zero-or-more character
     * atoms are admitted. Matcher remains responsible for every observable match object.
     */
    static BooleanPlan deriveBoolean(Pattern.Node start) {
        final int maxAtoms = 256;
        int count = 0;
        boolean hasRepeat = false;
        Pattern.Node node = start;
        while (node != Pattern.lastAccept) {
            if (node instanceof Pattern.CharPropertyGreedy greedy && greedy.cmin == 0) {
                count++;
                hasRepeat = true;
            } else if (node instanceof Pattern.CharProperty) {
                count++;
            } else if (node instanceof Pattern.Slice slice) {
                if (slice.buffer.length == 0 || slice.buffer.length > maxAtoms - count) {
                    return null;
                }
                for (int unit : slice.buffer) {
                    if (unit > Character.MAX_VALUE || Character.isSurrogate((char) unit)) {
                        return null;
                    }
                }
                count += slice.buffer.length;
            } else {
                return null;
            }
            if (count > maxAtoms) return null;
            node = node.next;
        }
        if (!hasRepeat) return null;

        Pattern.CharPredicate[] atoms = new Pattern.CharPredicate[count];
        boolean[] repeated = new boolean[count];
        boolean[] empty = new boolean[count + 1];
        empty[0] = true;
        int index = 0;
        for (node = start; node != Pattern.lastAccept; node = node.next) {
            if (node instanceof Pattern.CharPropertyGreedy greedy) {
                atoms[index] = greedy.predicate;
                repeated[index] = true;
                empty[index + 1] = empty[index];
                index++;
            } else if (node instanceof Pattern.CharProperty property) {
                atoms[index++] = property.predicate;
            } else if (node instanceof Pattern.Slice slice) {
                for (int unit : slice.buffer) atoms[index++] = Pattern.Single(unit);
            }
        }
        return new BooleanPlan(atoms, repeated, empty);
    }

    /** Immutable pattern facts; each invocation owns a scalar frontier or a rolling DP row. */
    static final class BooleanPlan {
        private final Pattern.CharPredicate[] atoms;
        private final boolean[] repeated;
        private final boolean[] empty;
        // Optional derived metadata belongs to this plan, never to M3String or its backing.
        // 128 character masks + six epsilon jumps + initial/accepting state = 1,088 bytes.
        private final long[] ascii;
        private final long repeatedStates;

        private BooleanPlan(Pattern.CharPredicate[] atoms, boolean[] repeated, boolean[] empty) {
            this.atoms = atoms;
            this.repeated = repeated;
            this.empty = empty;
            long stars = 0L;
            if (atoms.length <= 63) {
                for (int i = 0; i < atoms.length; i++) {
                    if (repeated[i]) stars |= 1L << (i + 1);
                }
            }
            this.repeatedStates = stars;
            this.ascii = prepareAscii(atoms, empty, stars);
        }

        /**
         * Reuses JDK predicates as semantic authority. Bit j represents the prefix of j atoms;
         * a starred atom adds an epsilon edge from j-1 to j. Doubling masks summarize runs
         * of 1, 2, 4, 8, 16 and 32 such edges without an input-length-sized DP allocation.
         * Programs beyond 63 atoms retain the original rolling-row implementation.
         */
        private static long[] prepareAscii(Pattern.CharPredicate[] atoms, boolean[] empty,
                                          long stars) {
            if (atoms.length > 63) return null;
            try {
                long[] masks = new long[136];
                for (int unit = 0; unit < 128; unit++) {
                    long accepted = 0L;
                    for (int atom = 0; atom < atoms.length; atom++) {
                        if (atoms[atom].is(unit)) accepted |= 1L << (atom + 1);
                    }
                    masks[unit] = accepted;
                }
                long jump = stars;
                for (int level = 0, shift = 1; level < 6; level++, shift <<= 1) {
                    masks[128 + level] = jump;
                    jump &= jump << shift;
                }
                for (int state = 0; state < empty.length; state++) {
                    if (empty[state]) masks[134] |= 1L << state;
                }
                masks[135] = 1L << atoms.length;
                return masks;
            } catch (OutOfMemoryError optionalMetadataUnavailable) {
                return null;
            }
        }

        /** Returns 0/1, or -1 to defer surrogate semantics to the original Matcher. */
        int matches(String input) {
            long[] masks = ascii;
            if (masks == null) return matchesRolling(input);
            long state = masks[134];
            for (int offset = 0; offset < input.length(); offset++) {
                char unit = input.charAt(offset);
                // Convert the exact prefix frontier once, then continue the original DP.
                // Do not stop at a dead frontier: a later surrogate must still produce -1.
                if (unit >= 128) {
                    boolean[] row = new boolean[atoms.length + 1];
                    for (int atom = 0; atom <= atoms.length; atom++) {
                        row[atom] = ((state >>> atom) & 1L) != 0L;
                    }
                    return matchesRolling(input, row, offset);
                }
                state = (((state << 1) & ~repeatedStates) | (state & repeatedStates))
                        & masks[unit];
                state |= (state << 1) & masks[128];
                state |= (state << 2) & masks[129];
                state |= (state << 4) & masks[130];
                state |= (state << 8) & masks[131];
                state |= (state << 16) & masks[132];
                state |= (state << 32) & masks[133];
            }
            return (state & masks[135]) != 0L ? 1 : 0;
        }

        /** Original one-row DP kept as the non-ASCII and larger-program authority. */
        private int matchesRolling(String input) {
            return matchesRolling(input, empty.clone(), 0);
        }

        /** Continue from a proved prefix frontier without rereading the input prefix. */
        private int matchesRolling(String input, boolean[] row, int start) {
            for (int offset = start; offset < input.length(); offset++) {
                char unit = input.charAt(offset);
                if (Character.isSurrogate(unit)) return -1;
                boolean diagonal = row[0];
                row[0] = false;
                for (int atom = 0; atom < atoms.length; atom++) {
                    boolean above = row[atom + 1];
                    row[atom + 1] = repeated[atom]
                            ? row[atom] || (above && atoms[atom].is(unit))
                            : diagonal && atoms[atom].is(unit);
                    diagonal = above;
                }
            }
            return row[atoms.length] ? 1 : 0;
        }
    }

    private static boolean admits(Pattern.Node start) {
        IdentityHashMap<Pattern.Node, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Pattern.Node> work = new ArrayDeque<>();
        push(work, start);
        while (!work.isEmpty()) {
            Pattern.Node node = work.pop();
            if (node == null || seen.put(node, Boolean.TRUE) != null) {
                continue;
            }
            if (seen.size() > MAX_NODES || writesRequireEnd(node) || !known(node)) {
                return false;
            }
            push(work, node.next);
            if (node instanceof Pattern.Branch branch) {
                for (int n = 0; n < branch.size; n++) {
                    push(work, branch.atoms[n]);
                }
                push(work, branch.conn);
            } else if (node instanceof Pattern.Ques ques) {
                push(work, ques.atom);
            } else if (node instanceof Pattern.Curly curly) {
                push(work, curly.atom);
            } else if (node instanceof Pattern.GroupCurly curly) {
                push(work, curly.atom);
            } else if (node instanceof Pattern.Prolog prolog) {
                push(work, prolog.loop);
            } else if (node instanceof Pattern.Loop loop) {
                push(work, loop.body);
            } else if (node instanceof Pattern.Pos pos) {
                push(work, pos.cond);
            } else if (node instanceof Pattern.Behind behind) {
                push(work, behind.cond);
            } else if (node instanceof Pattern.NotBehind notBehind) {
                push(work, notBehind.cond);
            }
        }
        return true;
    }

    private static void push(ArrayDeque<Pattern.Node> work, Pattern.Node node) {
        if (node != null) {
            work.push(node);
        }
    }

    private static boolean writesRequireEnd(Pattern.Node node) {
        return node instanceof Pattern.Dollar
                || node instanceof Pattern.UnixDollar
                || node instanceof Pattern.Bound
                || node instanceof Pattern.GraphemeBound
                || node instanceof Pattern.Neg;
    }

    private static boolean known(Pattern.Node node) {
        return node.getClass() == Pattern.Node.class
                || node instanceof Pattern.LastNode
                || node instanceof Pattern.Start
                || node instanceof Pattern.Begin
                || node instanceof Pattern.End
                || node instanceof Pattern.Caret
                || node instanceof Pattern.UnixCaret
                || node instanceof Pattern.LastMatch
                || node instanceof Pattern.LineEnding
                || node instanceof Pattern.CharProperty
                || node instanceof Pattern.XGrapheme
                || node instanceof Pattern.SliceNode
                || node instanceof Pattern.Ques
                || node instanceof Pattern.CharPropertyGreedy
                || node instanceof Pattern.Curly
                || node instanceof Pattern.GroupCurly
                || node instanceof Pattern.BranchConn
                || node instanceof Pattern.Branch
                || node instanceof Pattern.GroupHead
                || node instanceof Pattern.GroupTail
                || node instanceof Pattern.Prolog
                || node instanceof Pattern.Loop
                || node instanceof Pattern.BackRef
                || node instanceof Pattern.CIBackRef
                || node instanceof Pattern.First
                || node instanceof Pattern.Pos
                || node instanceof Pattern.Behind
                || node instanceof Pattern.NotBehind
                || node instanceof Pattern.LookBehindEndNode
                || node instanceof Pattern.BnM;
    }

    /** Bounded walk over one sequencing chain; sub-chains recurse with their own stop node. */
    private static final class Walker {
        private int budget = MAX_NODES;

        M3TQ sequence(Pattern.Node start, Pattern.Node stop) {
            List<M3TQ> requirements = new ArrayList<>();
            StringBuilder run = new StringBuilder();
            Pattern.Node node = start;
            while (node != null
                    && node != stop
                    && node != Pattern.accept
                    && !(node instanceof Pattern.LastNode)
                    && !(node instanceof Pattern.LookBehindEndNode)) {
                if (--budget < 0) {
                    return M3TQ.all();
                }
                if (node instanceof Pattern.Slice slice) {
                    // Slice carries UTF-16 units, SliceS code points; both are exact text.
                    for (int unit : slice.buffer) {
                        run.appendCodePoint(unit);
                    }
                    node = node.next;
                    continue;
                }
                if (node instanceof Pattern.GroupHead
                        || node instanceof Pattern.GroupTail
                        || node instanceof Pattern.BranchConn) {
                    node = node.next;
                    continue;
                }
                flush(requirements, run);
                if (node instanceof Pattern.Branch branch) {
                    requirements.add(alternation(branch));
                    node = branch.conn.next;
                } else if (node instanceof Pattern.Prolog prolog) {
                    Pattern.Loop loop = prolog.loop;
                    if (loop.cmin >= 1) {
                        requirements.add(sequence(loop.body, loop));
                    }
                    node = loop.next;
                } else if (node instanceof Pattern.GroupCurly curly) {
                    if (curly.cmin >= 1) {
                        requirements.add(sequence(curly.atom, null));
                    }
                    node = curly.next;
                } else if (node instanceof Pattern.Curly curly) {
                    if (curly.cmin >= 1) {
                        requirements.add(sequence(curly.atom, null));
                    }
                    node = curly.next;
                } else if (node instanceof Pattern.Loop) {
                    return M3TQ.all();
                } else {
                    node = node.next;
                }
            }
            flush(requirements, run);
            return M3TQ.and(requirements);
        }

        private M3TQ alternation(Pattern.Branch branch) {
            List<M3TQ> alternatives = new ArrayList<>(branch.size);
            for (int n = 0; n < branch.size; n++) {
                Pattern.Node atom = branch.atoms[n];
                if (atom == null) {
                    return M3TQ.all();
                }
                alternatives.add(sequence(atom, branch.conn));
            }
            return M3TQ.or(alternatives);
        }

        private static void flush(List<M3TQ> requirements, StringBuilder run) {
            if (run.length() >= 3) {
                requirements.add(M3TQ.fromExact(List.of(run.toString())));
            }
            run.setLength(0);
        }
    }
}
