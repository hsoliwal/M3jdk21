// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Read-only reflective view of the stock {@code java.util.regex.Pattern} node tree.
 *
 * <p>Build-time only: requires {@code --add-opens java.base/java.util.regex=ALL-UNNAMED}.
 * When the package is not opened, {@link #instance()} returns {@code null} and every analysis
 * refuses. Node class membership is closed: an unknown node class refuses the pattern.</p>
 */
final class M3PatternNodes {
    /** Sentinel returned by a walk step that must give up on the whole chain. */
    static final Object ABORT = new Object();

    /** Admission of a whole tree. */
    record Admission(boolean known, boolean matcherStateSafe, String reason) {}

    private static final String[] KNOWN = {
        "LastNode", "Start", "Begin", "End", "Caret", "UnixCaret", "LastMatch", "LineEnding",
        "CharProperty", "XGrapheme", "SliceNode", "Ques", "CharPropertyGreedy", "Curly",
        "GroupCurly", "BranchConn", "Branch", "GroupHead", "GroupTail", "Prolog", "Loop",
        "BackRef", "CIBackRef", "First", "Pos", "Behind", "NotBehind", "LookBehindEndNode", "BnM",
        "Dollar", "UnixDollar", "Bound", "GraphemeBound", "Neg"
    };
    private static final List<String> REQUIRE_END = List.of(
            "Dollar", "UnixDollar", "Bound", "GraphemeBound", "Neg");

    private static final M3PatternNodes INSTANCE = create();

    private final Class<?> node;
    private final Class<?>[] known;
    private final Class<?>[] requireEnd;
    private final Class<?> lastNode;
    private final Class<?> lookBehindEnd;
    private final Class<?> slice;
    private final Class<?> branch;
    private final Class<?> prolog;
    private final Class<?> loop;
    private final Class<?> curly;
    private final Class<?> groupCurly;
    private final Class<?> ques;
    private final Class<?> pos;
    private final Class<?> neg;
    private final Class<?> behind;
    private final Class<?> notBehind;
    private final Class<?> start;
    private final Class<?> bnm;
    private final Class<?>[] connectors;
    private final Object accept;
    private final Object lastAccept;
    private final VarHandle next;
    private final VarHandle buffer;
    private final VarHandle branchAtoms;
    private final VarHandle branchSize;
    private final VarHandle branchConn;
    private final VarHandle prologLoop;
    private final VarHandle loopBody;
    private final VarHandle loopMin;
    private final VarHandle curlyAtom;
    private final VarHandle curlyMin;
    private final VarHandle groupCurlyAtom;
    private final VarHandle groupCurlyMin;
    private final VarHandle quesAtom;
    private final VarHandle posCond;
    private final VarHandle negCond;
    private final VarHandle behindCond;
    private final VarHandle notBehindCond;
    private final VarHandle root;
    private final VarHandle matchRoot;

    private M3PatternNodes(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        node = type("Node");
        known = Arrays.stream(KNOWN).map(M3PatternNodes::typeUnchecked).toArray(Class<?>[]::new);
        requireEnd = REQUIRE_END.stream().map(M3PatternNodes::typeUnchecked).toArray(Class<?>[]::new);
        lastNode = type("LastNode");
        lookBehindEnd = type("LookBehindEndNode");
        slice = type("Slice");
        branch = type("Branch");
        prolog = type("Prolog");
        loop = type("Loop");
        curly = type("Curly");
        groupCurly = type("GroupCurly");
        ques = type("Ques");
        pos = type("Pos");
        neg = type("Neg");
        behind = type("Behind");
        notBehind = type("NotBehind");
        start = type("Start");
        bnm = type("BnM");
        connectors = new Class<?>[] {type("GroupHead"), type("GroupTail"), type("BranchConn")};
        MethodHandles.Lookup p = MethodHandles.privateLookupIn(Pattern.class, lookup);
        accept = p.findStaticVarHandle(Pattern.class, "accept", node).get();
        lastAccept = p.findStaticVarHandle(Pattern.class, "lastAccept", node).get();
        next = field(lookup, node, "next", node);
        buffer = field(lookup, type("SliceNode"), "buffer", int[].class);
        branchAtoms = field(lookup, branch, "atoms", node.arrayType());
        branchSize = field(lookup, branch, "size", int.class);
        branchConn = field(lookup, branch, "conn", node);
        prologLoop = field(lookup, prolog, "loop", loop);
        loopBody = field(lookup, loop, "body", node);
        loopMin = field(lookup, loop, "cmin", int.class);
        curlyAtom = field(lookup, curly, "atom", node);
        curlyMin = field(lookup, curly, "cmin", int.class);
        groupCurlyAtom = field(lookup, groupCurly, "atom", node);
        groupCurlyMin = field(lookup, groupCurly, "cmin", int.class);
        quesAtom = field(lookup, ques, "atom", node);
        posCond = field(lookup, pos, "cond", node);
        negCond = field(lookup, neg, "cond", node);
        behindCond = field(lookup, behind, "cond", node);
        notBehindCond = field(lookup, notBehind, "cond", node);
        root = p.findVarHandle(Pattern.class, "root", node);
        matchRoot = p.findVarHandle(Pattern.class, "matchRoot", node);
    }

    /** The shared view, or {@code null} when {@code java.util.regex} is not opened. */
    static M3PatternNodes instance() {
        return INSTANCE;
    }

    private static M3PatternNodes create() {
        try {
            return new M3PatternNodes(MethodHandles.lookup());
        } catch (ReflectiveOperationException | RuntimeException closed) {
            return null;
        }
    }

    private static Class<?> type(String simple) throws ClassNotFoundException {
        return Class.forName("java.util.regex.Pattern$" + simple, false, Pattern.class.getClassLoader());
    }

    private static Class<?> typeUnchecked(String simple) {
        try {
            return type(simple);
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static VarHandle field(MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> type)
            throws ReflectiveOperationException {
        return MethodHandles.privateLookupIn(owner, lookup).findVarHandle(owner, name, type);
    }

    Object root(Pattern pattern) {
        return root.get(pattern);
    }

    Object matchRoot(Pattern pattern) {
        return matchRoot.get(pattern);
    }

    Object next(Object n) {
        return next.get(n);
    }

    boolean terminal(Object n) {
        return n == accept || lastNode.isInstance(n) || lookBehindEnd.isInstance(n);
    }

    boolean isStartOrBnM(Object n) {
        return start.isInstance(n) || bnm.isInstance(n);
    }

    /** Exact UTF-16 units / code points of a case-sensitive Slice or SliceS, else null. */
    int[] caseSensitiveSlice(Object n) {
        return slice.isInstance(n) ? (int[]) buffer.get(n) : null;
    }

    boolean emptyWidthConnector(Object n) {
        return Arrays.stream(connectors).anyMatch(type -> type.isInstance(n));
    }

    boolean isBranch(Object n) {
        return branch.isInstance(n);
    }

    Object[] branchAtoms(Object n) {
        return Arrays.copyOf((Object[]) branchAtoms.get(n), (int) branchSize.get(n));
    }

    Object branchConn(Object n) {
        return branchConn.get(n);
    }

    boolean isProlog(Object n) {
        return prolog.isInstance(n);
    }

    Object prologLoop(Object n) {
        return prologLoop.get(n);
    }

    boolean isLoop(Object n) {
        return loop.isInstance(n);
    }

    Object loopBody(Object n) {
        return loopBody.get(n);
    }

    int loopMin(Object n) {
        return (int) loopMin.get(n);
    }

    boolean isCurlyFamily(Object n) {
        return curly.isInstance(n) || groupCurly.isInstance(n);
    }

    Object curlyAtom(Object n) {
        return curly.isInstance(n) ? curlyAtom.get(n) : groupCurlyAtom.get(n);
    }

    int curlyMin(Object n) {
        return curly.isInstance(n) ? (int) curlyMin.get(n) : (int) groupCurlyMin.get(n);
    }

    /** The literal of a tree that is exactly one case-sensitive slice, else null. */
    String pureLiteral(Object matchRoot) {
        int[] units = caseSensitiveSlice(matchRoot);
        if (units == null || next(matchRoot) != lastAccept) {
            return null;
        }
        StringBuilder text = new StringBuilder(units.length);
        Arrays.stream(units).forEach(text::appendCodePoint);
        return text.toString();
    }

    /** Closed-class admission over every reachable node, bounded by {@code maxNodes}. */
    Admission admit(Object startNode, int maxNodes) {
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Object> work = new ArrayDeque<>();
        work.push(startNode);
        boolean matcherState = true;
        while (!work.isEmpty()) {
            Object n = work.pop();
            if (seen.put(n, Boolean.TRUE) != null) {
                continue;
            }
            if (seen.size() > maxNodes) {
                return new Admission(false, false, "node budget");
            }
            if (n.getClass() != node && Arrays.stream(known).noneMatch(type -> type.isInstance(n))) {
                return new Admission(false, false, "unknown node " + n.getClass().getSimpleName());
            }
            matcherState &= Arrays.stream(requireEnd).noneMatch(type -> type.isInstance(n));
            children(n).forEach(child -> {
                if (child != null) {
                    work.push(child);
                }
            });
        }
        return new Admission(true, matcherState, "known");
    }

    private List<Object> children(Object n) {
        Object nx = next(n);
        if (branch.isInstance(n)) {
            List<Object> out = new java.util.ArrayList<>(Arrays.asList(branchAtoms(n)));
            out.add(branchConn(n));
            out.add(nx);
            return out;
        }
        Object inner = ques.isInstance(n) ? quesAtom.get(n)
                : curly.isInstance(n) ? curlyAtom.get(n)
                : groupCurly.isInstance(n) ? groupCurlyAtom.get(n)
                : prolog.isInstance(n) ? prologLoop.get(n)
                : loop.isInstance(n) ? loopBody.get(n)
                : pos.isInstance(n) ? posCond.get(n)
                : neg.isInstance(n) ? negCond.get(n)
                : behind.isInstance(n) ? behindCond.get(n)
                : notBehind.isInstance(n) ? notBehindCond.get(n)
                : null;
        return Arrays.asList(nx, inner);
    }
}
