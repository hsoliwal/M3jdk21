// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import jdk.m3.collections.M3ProgressMonitor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only compiler reference multiplicity from existing alias and source CFG facts.
 * Counts SOURCE-LOCAL SLOTS that MAY alias an allocation origin, not runtime
 * Java objects, strong roots, JNI globals, or actual owning references.
 * Unknown-origin slots are kept at index zero and conservatively apply to
 * every origin. The original reference solver, source liveness and proof
 * contexts remain authoritative. These signals never permit reclamation.
 */
public final class CompilerReferenceCensus {
    private CompilerReferenceCensus() { }

    // Bit-packed static optimization hints, never an ownership or GC permit.
    public static final int NO_VISIBLE_LOCAL = 1 << 0;
    public static final int SINGLE_POSSIBLE_LOCAL = 1 << 1;
    public static final int MULTIPLE_POSSIBLE_LOCALS = 1 << 2;
    public static final int UNKNOWN_ALIAS = 1 << 3;
    public static final int EXTERNAL_EXPOSURE = 1 << 4;
    public static final int LOOP_REPEATED_ORIGIN = 1 << 5;
    public static final int POSSIBLY_ENDING_LOCAL = 1 << 6;
    public static final int LIVE_ACROSS_EDGE = 1 << 7;
    public static final int LAST_LOCAL_CANDIDATE = 1 << 8;

    /** Each lane has siteCount+1 entries; index zero represents unknown origins. */
    public record Edge(String source, String signature, long methodPosition, int from, int to,
            int[] knownBefore, int[] knownAfter, int[] knownPossiblyEnding,
            BitSet exposedOrigins, BitSet repeatedOrigins) {
        public Edge {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(signature, "signature");
            knownBefore = Objects.requireNonNull(knownBefore, "knownBefore").clone();
            knownAfter = Objects.requireNonNull(knownAfter, "knownAfter").clone();
            knownPossiblyEnding = Objects.requireNonNull(knownPossiblyEnding, "knownPossiblyEnding").clone();
            exposedOrigins = (BitSet) Objects.requireNonNull(exposedOrigins, "exposedOrigins").clone();
            repeatedOrigins = (BitSet) Objects.requireNonNull(repeatedOrigins, "repeatedOrigins").clone();
            if (from < 0 || to < 0 || knownBefore.length == 0 || knownAfter.length != knownBefore.length
                    || knownPossiblyEnding.length != knownBefore.length
                    || exposedOrigins.length() > knownBefore.length
                    || repeatedOrigins.length() > knownBefore.length) {
                throw new IllegalArgumentException("invalid source-local count dimensions");
            }
            // Bit zero is unknown-source origin, never a real allocation site.
            if (exposedOrigins.get(0) || repeatedOrigins.get(0))
                throw new IllegalArgumentException("unknown origin is not an allocation site");
            for (int site = 0; site < knownBefore.length; site++) {
                if (knownBefore[site] < 0 || knownAfter[site] < 0
                        || knownPossiblyEnding[site] < 0) {
                    throw new IllegalArgumentException("negative source-local count");
                }
                if (knownPossiblyEnding[site] > knownBefore[site]) {
                    throw new IllegalArgumentException("ending aliases exceed preceding locals");
                }
            }
        }
        @Override public int[] knownBefore() { return knownBefore.clone(); }
        @Override public int[] knownAfter() { return knownAfter.clone(); }
        @Override public int[] knownPossiblyEnding() { return knownPossiblyEnding.clone(); }
        @Override public BitSet exposedOrigins() { return (BitSet) exposedOrigins.clone(); }
        @Override public BitSet repeatedOrigins() { return (BitSet) repeatedOrigins.clone(); }
        public int originCount() { return knownBefore.length - 1; }
        public int unknownBefore() { return knownBefore[0]; }
        public int unknownAfter() { return knownAfter[0]; }
        public int possibleBefore(int site) { return upper(knownBefore, site); }
        public int possibleAfter(int site) { return upper(knownAfter, site); }
        /** Possible disappearing source-local slots; NOT a number of released references. */
        public int possiblyEnding(int site) { return upper(knownPossiblyEnding, site); }
        private static int upper(int[] lane, int site) {
            Objects.checkIndex(site, lane.length);
            // Even a forged externally constructed diagnostic record must
            // never turn a large positive alias count into a negative one.
            long n = (long) lane[site] + (site == 0 ? 0L : lane[0]);
            return n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n;
        }
        /** Search signal only: no heap, JNI, concurrency or scalar-replacement proof. */
        public boolean possibleLastLocalAlias(int site) {
            // A TOP/unknown local might denote this origin even if its local
            // lifetime ends here. Such an edge is a possible end, never a
            // candidate for concluding the final known alias has ended.
            return site > 0 && site < knownBefore.length
                    && unknownBefore() == 0 && unknownAfter() == 0
                    && knownBefore[site] > 0 && knownAfter[site] == 0
                    && possiblyEnding(site) > 0 && !exposedOrigins.get(site)
                    && !repeatedOrigins.get(site);
        }
        /**
         * Compiler-produced diagnostic bits for one allocation origin.
         * SINGLE_POSSIBLE_LOCAL is about a MAY-alias graph and is NOT a
         * definite non-null reference, exclusive owner, or elision permit.
         */
        public int signalMask(int site) {
            if (site < 1 || site >= knownBefore.length)
                throw new IllegalArgumentException("allocation origin required");
            int before = possibleBefore(site), after = possibleAfter(site);
            int flags = 0;
            if (before == 0) flags |= NO_VISIBLE_LOCAL;
            if (before == 1 && unknownBefore() == 0 && !exposedOrigins.get(site)
                    && !repeatedOrigins.get(site)) flags |= SINGLE_POSSIBLE_LOCAL;
            if (before > 1) flags |= MULTIPLE_POSSIBLE_LOCALS;
            if (unknownBefore() > 0 || unknownAfter() > 0) flags |= UNKNOWN_ALIAS;
            if (exposedOrigins.get(site)) flags |= EXTERNAL_EXPOSURE;
            if (repeatedOrigins.get(site)) flags |= LOOP_REPEATED_ORIGIN;
            if (possiblyEnding(site) > 0) flags |= POSSIBLY_ENDING_LOCAL;
            if (before > 0 && after > 0) flags |= LIVE_ACROSS_EDGE;
            if (possibleLastLocalAlias(site)) flags |= LAST_LOCAL_CANDIDATE;
            return flags;
        }

        /**
         * Write all compile-time signal masks into a caller-owned primitive
         * sidecar, without allocating one signal/entry object per origin.
         *
         * <p>Lane zero is reserved for unknown-origin diagnostics, not a real
         * allocation site; it is always cleared. Exact width avoids stale
         * caller array tails being interpreted as additional origins.</p>
         *
         * <p>These remain MAY-alias hints, never exclusive ownership or
         * JNI/GC release authority.</p>
         */
        public void signalMasks(int[] destination) {
            Objects.requireNonNull(destination, "destination");
            if (destination.length != knownBefore.length)
                throw new IllegalArgumentException("source signal lane width mismatch");
            destination[0] = 0;
            for (int site = 1; site < destination.length; site++)
                destination[site] = signalMask(site);
        }

        /**
         * Copy all three compiler-derived local-alias count lanes into reusable,
         * caller-owned primitive arrays. No temporary arrays or per-site objects.
         *
         * <p>Buffers must be pairwise distinct and have exact originCount()+1
         * width, including the unknown origin at index zero. Validation occurs
         * before any write, so invalid inputs never partially overwrite output.
         * This operation copies diagnostic MAY-alias facts only: no ownership,
         * JNI borrow release or reclamation authority.</p>
         */
        public void copyCountLanes(int[] beforeOut, int[] afterOut, int[] endingOut) {
            Objects.requireNonNull(beforeOut, "beforeOut");
            Objects.requireNonNull(afterOut, "afterOut");
            Objects.requireNonNull(endingOut, "endingOut");
            if (beforeOut == afterOut || beforeOut == endingOut || afterOut == endingOut)
                throw new IllegalArgumentException("count lane buffers must not alias");
            int count = knownBefore.length;
            if (beforeOut.length != count || afterOut.length != count || endingOut.length != count)
                throw new IllegalArgumentException("source-local count lane width mismatch");
            System.arraycopy(knownBefore, 0, beforeOut, 0, count);
            System.arraycopy(knownAfter, 0, afterOut, 0, count);
            System.arraycopy(knownPossiblyEnding, 0, endingOut, 0, count);
        }

        public boolean authorizesReclamation() { return false; }
        public boolean authorizesRcElision() { return false; }
    }

    public record Result(String sourceDigest, String referenceProofKey, String flowProofKey,
            List<Edge> edges, List<String> unresolved) {
        public Result {
            Objects.requireNonNull(sourceDigest, "sourceDigest");
            Objects.requireNonNull(referenceProofKey, "referenceProofKey");
            Objects.requireNonNull(flowProofKey, "flowProofKey");
            edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
            unresolved = List.copyOf(Objects.requireNonNull(unresolved, "unresolved"));
        }
        public boolean complete() { return unresolved.isEmpty(); }
        public boolean authorizesReclamation() { return false; }
        public boolean authorizesSourceMutation() { return false; }
    }

    /** No parsing/compilation/solver: take existing source/compiler-proven inputs. */
    public static Result analyze(ReferenceFlow.Result references,
            SourceLiveness.Result flow, M3ProgressMonitor monitor) {
        Objects.requireNonNull(references, "references");
        Objects.requireNonNull(flow, "flow");
        check(monitor);
        // Canonical implementation validates source/compiler snapshot, CFG edges
        // and attributed local identities; it excludes unresolved methods.
        var projected = ReferenceFlow.project(references, flow, monitor);
        Map<String, ReferenceFlow.MethodReferences> refMethods = new HashMap<>();
        for (var method : references.methods()) {
            if (refMethods.putIfAbsent(key(method.source(), method.signature(), method.position()), method) != null) {
                throw new IllegalArgumentException("duplicate attributed method");
            }
        }
        Map<String, Joined> joined = new java.util.LinkedHashMap<>();
        for (var method : flow.methods()) {
            check(monitor);
            var refs = refMethods.remove(key(method.source(), method.signature(), method.position()));
            if (refs == null) throw new IllegalArgumentException("missing reference method");
            if (!method.complete() || !refs.complete()) continue;
            int siteCount = refs.sites().size();
            BitSet[] originBySlot = new BitSet[method.slots().size()];
            Map<String, BitSet> aliasBySymbol = new HashMap<>();
            for (var alias : refs.aliases()) {
                BitSet origins = alias.origins();
                if (origins.length() > siteCount + 1
                        || aliasBySymbol.putIfAbsent(key(alias.name(), alias.type(), alias.position()), origins) != null) {
                    throw new IllegalArgumentException("invalid allocation origin or duplicate alias");
                }
            }
            for (var slot : method.slots()) {
                Objects.checkIndex(slot.index(), originBySlot.length);
                originBySlot[slot.index()] = aliasBySymbol.remove(key(slot.name(), slot.type(), slot.position()));
            }
            if (!aliasBySymbol.isEmpty()) throw new IllegalArgumentException("reference/CFG local identities differ");
            BitSet escaped = new BitSet(siteCount + 1), repeated = new BitSet(siteCount + 1);
            for (int i = 0; i < siteCount; i++) {
                var site = refs.sites().get(i);
                if (site.origin() != i + 1) throw new IllegalArgumentException("allocation origin order differs");
                if (site.mayEscape()) escaped.set(site.origin());
                if (!site.enclosingLoops().isEmpty()) repeated.set(site.origin());
            }
            String identity = identity(method.source(), method.signature());
            if (joined.putIfAbsent(identity,
                    new Joined(method, originBySlot, siteCount, escaped, repeated)) != null) {
                throw new IllegalArgumentException("duplicate source method identity");
            }
        }
        if (!refMethods.isEmpty()) throw new IllegalArgumentException("reference/CFG inventories differ");
        // Enumerate ALL reachable source-CFG edges, not only last-use edges.
        // ReferenceFlow.project has already checked snapshot identities,
        // fixed-point liveness equations, source symbols and reachability.
        // A count on a still-live edge is often more useful to scalarization
        // than the last-reference boundary alone.
        List<Edge> edges = new ArrayList<>();
        // A single reached-node mask per method is sufficient: the validated
        // original CFG itself is the authoritative successor index. Avoid
        // allocating a dense destination mask for each source node.
        Map<String, BitSet> reachableByMethod = new HashMap<>();
        long emittedCells = 0;
        for (var entry : joined.entrySet()) {
            check(monitor);
            var owner = entry.getValue();
            if (owner.sites() == 0) continue;
            // Admit the source-site array width before any queue or edge lanes.
            reserveCells(0, owner.sites());
            var method = owner.method();
            var nodes = method.nodes();
            var live = method.liveness().orElseThrow();
            // One mutable scratch for the method, not one object per CFG edge.
            BitSet endingScratch = new BitSet(method.slots().size());
            // Edge freezes the lane, so reuse scratch counts for every edge.
            int[] endingCountsScratch = new int[owner.sites() + 1];
            // Cache post-edge local alias counts once per destination CFG
            // node. The Edge constructor still freezes independent arrays.
            // Cache retention is bounded; on exhaustion recompute normally.
            int[][] afterCache = new int[nodes.size()][];
            int afterCacheCells = 0;
            BitSet reachable = new BitSet(nodes.size());
            reachableByMethod.put(entry.getKey(), reachable);
            // Every node is enqueued at most once: primitive array, no
            // per-node Integer boxing or ArrayDeque growth.
            int[] queue = new int[nodes.size()];
            int head = 0, tail = 1;
            queue[0] = method.entry();
            reachable.set(method.entry());
            while (head < tail) {
                check(monitor);
                for (int next : nodes.get(queue[head++]).successors()) {
                    Objects.checkIndex(next, nodes.size());
                    if (!reachable.get(next)) {
                        reachable.set(next);
                        queue[tail++] = next;
                    }
                }
            }
            for (int from = reachable.nextSetBit(0); from >= 0;
                    from = reachable.nextSetBit(from + 1)) {
                check(monitor);
                int[] successors = nodes.get(from).successors();
                if (successors.length == 0) continue;
                java.util.Arrays.sort(successors);
                BitSet before = live.liveIn(from);
                before.or(nodes.get(from).def());
                int[] beforeLane = count(before, owner);
                int previous = -1;
                for (int to : successors) {
                    Objects.checkIndex(to, nodes.size());
                    if (to == previous) continue;
                    previous = to;
                    // Successors were sorted in a cloned array; equal IDs
                    // are already collapsed by the previous-value guard.
                    emittedCells = reserveCells(emittedCells, owner.sites());
                    if (edges.size() >= MAX_CENSUS_EDGES)
                        throw new IllegalArgumentException("reference census edge budget exceeded");
                    BitSet after = live.liveIn(to);
                    int[] afterLane = afterCache[to];
                    if (afterLane == null) {
                        afterLane = count(after, owner);
                        if (afterLane.length <= MAX_DESTINATION_CACHE_CELLS - afterCacheCells) {
                            afterCache[to] = afterLane;
                            afterCacheCells += afterLane.length;
                        }
                    }
                    endingSlots(endingScratch, before, after);
                    countInto(endingScratch, owner.origins(), endingCountsScratch);
                    edges.add(new Edge(method.source(), method.signature(), method.position(),
                            from, to, beforeLane, afterLane,
                            endingCountsScratch, owner.escaped(), owner.repeated()));
                }
            }
        }
        // The canonical reference projection independently proves ending
        // origins. Every such origin/edge must be present in the count index.
        for (var boundary : projected.boundaries()) {
            check(monitor);
            String ownerKey = identity(boundary.source(), boundary.signature());
            var owner = joined.get(ownerKey);
            if (owner == null) throw new IllegalArgumentException("boundary source not qualified");
            var method = owner.method();
            BitSet reachable = reachableByMethod.get(ownerKey);
            if (!hasReachableSuccessor(reachable, method.nodes().size(),
                    boundary.from(), boundary.to(),
                    boundary.from() >= 0 && boundary.from() < method.nodes().size()
                            ? method.nodes().get(boundary.from()).successors()
                            : new int[0]))
                throw new IllegalArgumentException("projected edge not counted");
            var live = method.liveness().orElseThrow();
            BitSet ending = live.liveIn(boundary.from());
            ending.or(method.nodes().get(boundary.from()).def());
            ending.andNot(live.liveIn(boundary.to()));
            int[] endingLane = count(ending, owner);
            BitSet origins = boundary.origins();
            if (origins.length() > owner.sites() + 1)
                throw new IllegalArgumentException("projected origin outside allocation sites");
            for (int site = origins.nextSetBit(1); site >= 0; site = origins.nextSetBit(site + 1))
                if (endingLane[site] + endingLane[0] == 0)
                    throw new IllegalArgumentException("projected last-use origin not counted");
        }
        check(monitor);
        return new Result(references.facts().sourceDigest(), references.proofKey(),
                flow.proofKey(), edges, projected.unresolved());
    }

    /** Reuse one per-method scratch mask without mutating input liveness snapshots. */
    static void endingSlots(BitSet scratch, BitSet before, BitSet after) {
        Objects.requireNonNull(scratch, "scratch");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (scratch == before || scratch == after)
            throw new IllegalArgumentException("scratch cannot alias compiler snapshots");
        scratch.clear();
        scratch.or(before);
        scratch.andNot(after);
    }

    /**
     * Validate a projected edge against original CFG successors and the
     * single per-method reachability mask. This never constructs a per-node
     * destination bitset or a string key per edge.
     */
    static boolean hasReachableSuccessor(BitSet reachable, int nodeCount,
            int from, int to, int[] successors) {
        Objects.requireNonNull(reachable, "reachable");
        Objects.requireNonNull(successors, "successors");
        if (nodeCount <= 0 || from < 0 || to < 0
                || from >= nodeCount || to >= nodeCount || !reachable.get(from))
            return false;
        for (int successor : successors) {
            if (successor == to) return true;
        }
        return false;
    }

    /** Bound the three primitive count lanes (~8 MiB) and edge records. */
    static final long MAX_CENSUS_CELLS = 2_097_152L;
    static final int MAX_CENSUS_EDGES = 65_536;
    /** Additional bounded per-method memoization; fallback is ordinary recount. */
    static final int MAX_DESTINATION_CACHE_CELLS = 131_072;

    static long reserveCells(long current, int sites) {
        if (current < 0 || current > MAX_CENSUS_CELLS || sites < 0)
            throw new IllegalArgumentException("invalid reference census budget");
        long requested = 3L * (sites + 1L);
        if (requested > MAX_CENSUS_CELLS - current)
            throw new IllegalArgumentException("reference census cell budget exceeded");
        return current + requested;
    }

    private record Joined(SourceLiveness.MethodFlow method, BitSet[] origins,
            int sites, BitSet escaped, BitSet repeated) { }

    /** Primitive sidecar lanes: unknown slots are counted once, without O(sites) fanout. */
    private static int[] count(BitSet liveSlots, Joined owner) {
        int[] lane = new int[owner.sites() + 1];
        countInto(liveSlots, owner.origins(), lane);
        return lane;
    }

    /**
     * Preallocated primitive alias lane. The lane belongs to the invoking
     * compiler method and never leaves the current edge: Edge snapshots
     * its values through a defensive clone.
     *
     * Unknown origin zero takes precedence over named possible origins,
     * matching the original canonical alias counting contract exactly.
     */
    static void countInto(BitSet liveSlots, BitSet[] originsBySlot, int[] lane) {
        Objects.requireNonNull(liveSlots, "liveSlots");
        Objects.requireNonNull(originsBySlot, "originsBySlot");
        Objects.requireNonNull(lane, "lane");
        if (lane.length == 0)
            throw new IllegalArgumentException("missing unknown-origin lane");
        java.util.Arrays.fill(lane, 0);
        for (int slot = liveSlots.nextSetBit(0); slot >= 0; slot = liveSlots.nextSetBit(slot + 1)) {
            BitSet origins = originsBySlot[Objects.checkIndex(slot, originsBySlot.length)];
            if (origins == null || origins.isEmpty()) continue;
            if (origins.length() > lane.length)
                throw new IllegalArgumentException("alias origin outside count lanes");
            if (origins.get(0)) lane[0]++;
            else for (int site = origins.nextSetBit(1); site >= 0; site = origins.nextSetBit(site + 1))
                lane[site]++;
        }
    }
    private static String identity(String source, String signature) {
        return source.length() + ":" + source + signature.length() + ":" + signature;
    }
    private static String key(String a, String b, long position) {
        return identity(a, b) + ":" + position;
    }
    private static void check(M3ProgressMonitor monitor) {
        if (monitor != null) monitor.checkCanceled();
    }
}
