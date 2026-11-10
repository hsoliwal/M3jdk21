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
