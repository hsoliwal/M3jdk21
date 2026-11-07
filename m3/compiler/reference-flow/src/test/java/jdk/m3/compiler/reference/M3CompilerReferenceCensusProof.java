// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.reference;

import java.util.Map;

/** Strict Java21 compiler-source alias multiplicity and provenance smoke proof. */
public final class M3CompilerReferenceCensusProof {
    private static long checks;
    private M3CompilerReferenceCensusProof() { }
    private static void check(boolean yes, String reason) {
        checks++;
        if (!yes) throw new AssertionError(reason);
    }
    private static M3CompilerReferenceCensus.Result count(String source) throws Exception {
        Map<String,String> units = Map.of("F.java", source);
        var refs = M3ReferenceFlow.analyze(units, null);
        var flow = M3SourceLiveness.analyzeExtended(units, null);
        check(refs.facts().baselineCompiles() && flow.facts().baselineCompiles(), "javac attribution");
        return M3CompilerReferenceCensus.analyze(refs, flow, null);
    }
    public static void main(String[] args) throws Exception {
        checks = 0;
        String code = "class F {static int f(){int[] a=new int[2];int[] b=a;b[0]=7;return b[0];}}";
        var refs = M3ReferenceFlow.analyze(Map.of("F.java", code), null);
        var flow = M3SourceLiveness.analyzeExtended(Map.of("F.java", code), null);
        var a = M3CompilerReferenceCensus.analyze(refs, flow, null);
        check(a.complete() && !a.edges().isEmpty(), "local last-reference edge exists");
        check(!a.authorizesReclamation() && !a.authorizesSourceMutation(), "no release authority");
        check(a.edges().stream().anyMatch(e -> e.possiblyEnding(1) > 0), "origin 1 can end");
        var originalLastUses = M3ReferenceFlow.project(refs, flow, null).boundaries();
        check(a.edges().size() > originalLastUses.size(),
                "count non-ending reachable edges, not just last-use projection");
        var flowMethod = flow.methods().stream()
                .filter(m -> m.signature().contains("#f(")).findFirst().orElseThrow();
        java.util.BitSet seen = new java.util.BitSet(flowMethod.nodes().size());
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        queue.add(flowMethod.entry());
        seen.set(flowMethod.entry());
        int expectedEdges = 0;
        while (!queue.isEmpty()) {
            int from = queue.removeFirst();
            int[] successors = flowMethod.nodes().get(from).successors();
            expectedEdges += java.util.Arrays.stream(successors).distinct().count();
            for (int to : successors) if (!seen.get(to)) { seen.set(to); queue.add(to); }
        }
        check(a.edges().size() == expectedEdges, "full reachable CFG edge coverage");
        // Compiler-built diamond: a destination's possible aliases are
        // independent of which predecessor reached it. The memoized path
        // must retain exactly the original non-memoized output.
        var diamond = count("class F {static int f(boolean x){int[] a=new int[3];"
                + "int[] b=a;if(x){b[0]=1;}else{b[0]=2;}return b[0];}}");
        check(diamond.complete(), "compiler-qualified diamond");
        java.util.Map<Integer, int[]> destinationCounts = new java.util.HashMap<>();
        java.util.Set<Integer> repeatedDestinations = new java.util.HashSet<>();
        for (var edge : diamond.edges()) {
            int[] first = destinationCounts.putIfAbsent(edge.to(), edge.knownAfter());
            if (first != null) {
                repeatedDestinations.add(edge.to());
                check(java.util.Arrays.equals(first, edge.knownAfter()),
                        "converging CFG edges must have identical post-liveness alias counts");
            }
        }
        check(!repeatedDestinations.isEmpty(), "diamond has a repeated destination");
        check(M3CompilerReferenceCensus.MAX_DESTINATION_CACHE_CELLS == 131_072,
                "bounded memoization is explicit");
        check(M3CompilerReferenceCensus.MAX_DESTINATION_CACHE_CELLS
                < M3CompilerReferenceCensus.MAX_CENSUS_CELLS,
                "destination cache smaller than cumulative census budget");
        // Exact Java21 differential of reusable scratch against clone+andNot.
        java.util.Random masks = new java.util.Random(0x4d33534352415443L);
        java.util.BitSet scratch = new java.util.BitSet(4096);
        for (int trial = 0; trial < 20_000; trial++) {
            java.util.BitSet before = new java.util.BitSet(4096);
            java.util.BitSet after = new java.util.BitSet(4096);
            int width = trial % 11 == 0 ? 0 : trial % 11 == 1 ? 4096 : 128;
            for (int k = 0; k < width; k++) {
                before.set(masks.nextInt(4096));
                if ((k & 1) == 0) after.set(masks.nextInt(4096));
            }
            java.util.BitSet expected = (java.util.BitSet) before.clone();
            java.util.BitSet unmodified = (java.util.BitSet) before.clone();
            expected.andNot(after);
            M3CompilerReferenceCensus.endingSlots(scratch, before, after);
            check(scratch.equals(expected), "scratch equals original clone-and-subtract");
            check(before.equals(unmodified), "compiler before mask unchanged");
        }
        boolean refusedScratch = false;
        try {
            M3CompilerReferenceCensus.endingSlots(scratch, scratch, new java.util.BitSet());
        } catch (IllegalArgumentException expected) {
            refusedScratch = true;
        }
        check(refusedScratch, "before/scratch alias refused");
        refusedScratch = false;
        try {
            M3CompilerReferenceCensus.endingSlots(scratch, new java.util.BitSet(), scratch);
        } catch (IllegalArgumentException expected) {
            refusedScratch = true;
        }
        check(refusedScratch, "after/scratch alias refused");
        // Reference-count multiplicity: 20,000 seeded slot/origin masks.
        // The expected counts use an independent original-style scan.
        // Reusing the same int[] must zero every stale count and Edge must
        // freeze the result before the scratch is reused on the next edge.
        java.util.Random aliasRandom = new java.util.Random(0x4d3343524546414cL);
        for (int trial = 0; trial < 20_000; trial++) {
            int siteCount = 1 + trial % 29;
            int slotCount = 1 + trial % 77;
            java.util.BitSet[] originBySlot = new java.util.BitSet[slotCount];
            java.util.BitSet liveSlots = new java.util.BitSet(slotCount);
            for (int slot = 0; slot < slotCount; slot++) {
                if (aliasRandom.nextInt(5) != 0) liveSlots.set(slot);
                if (aliasRandom.nextInt(9) == 0) continue;
                java.util.BitSet origins = new java.util.BitSet(siteCount + 1);
                if (aliasRandom.nextInt(8) == 0) origins.set(0);
                for (int site = 1; site <= siteCount; site++)
                    if (aliasRandom.nextInt(13) == 0) origins.set(site);
                originBySlot[slot] = origins;
            }
            int[] expected = new int[siteCount + 1];
            for (int slot = liveSlots.nextSetBit(0); slot >= 0;
                    slot = liveSlots.nextSetBit(slot + 1)) {
                java.util.BitSet origins = originBySlot[slot];
                if (origins == null || origins.isEmpty()) continue;
                if (origins.get(0)) expected[0]++;
                else for (int site = origins.nextSetBit(1); site >= 0;
                        site = origins.nextSetBit(site + 1)) expected[site]++;
            }
            int[] endingLane = new int[siteCount + 1];
            java.util.Arrays.fill(endingLane, -1);
            M3CompilerReferenceCensus.countInto(liveSlots, originBySlot, endingLane);
            check(java.util.Arrays.equals(expected, endingLane), "canonical alias lane parity");
            // The strict first-party Edge requires possibly ending aliases
            // to be a subset of preceding aliases. Use the exact count lane
            // as feasible before-state, then mutate only the scratch.
            var snapshot = new M3CompilerReferenceCensus.Edge("F.java", "F#f", 1L, 0, 1,
                    endingLane.clone(), new int[siteCount + 1], endingLane,
                    new java.util.BitSet(), new java.util.BitSet());
            M3CompilerReferenceCensus.countInto(new java.util.BitSet(), originBySlot, endingLane);
            check(java.util.Arrays.equals(endingLane, new int[siteCount + 1]),
                    "reuse clears stale ending aliases");
            check(java.util.Arrays.equals(snapshot.knownPossiblyEnding(), expected),
                    "Edge snapshots reused compiler scratch before next transition");
        }
        boolean badAliasLane = false;
        try {
            java.util.BitSet invalidSlot = new java.util.BitSet();
            invalidSlot.set(7);
            M3CompilerReferenceCensus.countInto(invalidSlot,
                    new java.util.BitSet[3], new int[2]);
        } catch (IndexOutOfBoundsException expected) { badAliasLane = true; }
        check(badAliasLane, "invalid compiler local index refused");
        badAliasLane = false;
        try {
            java.util.BitSet[] invalidOrigin = {new java.util.BitSet()};
            invalidOrigin[0].set(4);
            java.util.BitSet oneSlot = new java.util.BitSet();
            oneSlot.set(0);
            M3CompilerReferenceCensus.countInto(oneSlot, invalidOrigin, new int[2]);
        } catch (IllegalArgumentException expected) { badAliasLane = true; }
        check(badAliasLane, "invalid compiler allocation origin refused");


        // Sparse high-ID CFG regression. A single reached-node mask must
        // validate original edge membership without an O(nodes^2) index.
        java.util.BitSet sparse = new java.util.BitSet();
        sparse.set(0);
        check(M3CompilerReferenceCensus.hasReachableSuccessor(sparse, 1_000_001,
                0, 1_000_000, new int[] {1_000_000}), "sparse far edge");
        check(!M3CompilerReferenceCensus.hasReachableSuccessor(sparse, 1_000_001,
                1, 1_000_000, new int[] {1_000_000}), "unreachable source refused");
        check(!M3CompilerReferenceCensus.hasReachableSuccessor(sparse, 1_000_001,
                0, 999_999, new int[] {1_000_000}), "absent target refused");
        check(!M3CompilerReferenceCensus.hasReachableSuccessor(sparse, 1_000_001,
                -1, 1_000_000, new int[] {1_000_000}), "negative node refused");
        check(!M3CompilerReferenceCensus.hasReachableSuccessor(sparse, 1_000_001,
                0, 1_000_001, new int[] {1_000_001}), "out of range target refused");
        check(!M3CompilerReferenceCensus.hasReachableSuccessor(sparse, 1_000_001,
                0, 1_000_000, new int[0]), "absent edge refused");

        check(M3CompilerReferenceCensus.reserveCells(0, 0) == 3,
                "zero-site three-lane reservation");
        check(M3CompilerReferenceCensus.reserveCells(3, 1) == 9,
                "single-site three-lane reservation");
        check(M3CompilerReferenceCensus.reserveCells(
                M3CompilerReferenceCensus.MAX_CENSUS_CELLS - 3, 0)
                == M3CompilerReferenceCensus.MAX_CENSUS_CELLS,
                "exact memory boundary accepted");
        for (long previous : new long[] {
                -1L, M3CompilerReferenceCensus.MAX_CENSUS_CELLS + 1L}) {
            boolean refused = false;
            try { M3CompilerReferenceCensus.reserveCells(previous, 0); }
            catch (IllegalArgumentException expected) { refused = true; }
            check(refused, "invalid memory accounting rejected");
        }
        for (int siteCount : new int[] {-1, 1, Integer.MAX_VALUE}) {
            boolean refused = false;
            try { M3CompilerReferenceCensus.reserveCells(
                    M3CompilerReferenceCensus.MAX_CENSUS_CELLS - 3, siteCount); }
            catch (IllegalArgumentException expected) { refused = true; }
            check(refused, "out-of-budget site lanes rejected");
        }
        check(a.edges().stream().map(e -> e.from() + ":" + e.to()).distinct().count()
                == a.edges().size(), "unique primitive-indexed edges");
        check(a.edges().stream().allMatch(e -> e.possibleBefore(1) >= 0
                && e.possibleAfter(1) >= 0 && e.possiblyEnding(1) >= 0),
                "counts remain bounded nonnegative local-slot facts");

        for (var ended : originalLastUses) {
            check(a.edges().stream().anyMatch(e -> e.from() == ended.from()
                    && e.to() == ended.to()), "projection-to-count edge consistency");
        }
        check(a.edges().stream().anyMatch(e -> e.possibleBefore(1) > 0
                && e.possibleAfter(1) > 0 && e.possiblyEnding(1) == 0),
                "count sustained aliases on edges with no ending");
        for (var edge : a.edges()) {
            check(edge.originCount() == 1, "site identity");
            check(!edge.exposedOrigins().get(1), "source-local nonescaping example");
            check(!edge.authorizesReclamation() && !edge.authorizesRcElision(), "no runtime RC substitution");
            check(edge.possibleBefore(1) >= edge.possiblyEnding(1), "ending alias is a before alias");
            int[] mutable = edge.knownBefore();
            mutable[1] = Integer.MAX_VALUE;
            check(edge.possibleBefore(1) != Integer.MAX_VALUE, "defensive primitive lanes");
            var exposed = edge.exposedOrigins();
            exposed.set(1);
            check(!edge.exposedOrigins().get(1), "defensive exposure mask");
        }
        String escaping = "class F {static int[] f(){int[] a=new int[2];int[] b=a;return b;}}";
        var b = count(escaping);
        check(b.complete() && !b.edges().isEmpty(), "escape still yields local slot count");
        check(b.edges().stream().anyMatch(e -> e.exposedOrigins().get(1)), "return marks external escape");
        check(b.edges().stream().noneMatch(e -> e.possibleLastLocalAlias(1)), "escape forbids local last-use shortcut");
        var changed = M3SourceLiveness.analyzeExtended(Map.of("F.java",
                code.replace("new int[2]", "new int[3]")), null);
        boolean rejected = false;
        try { M3CompilerReferenceCensus.analyze(refs, changed, null); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "source/compiler drift refused");
        var again = M3CompilerReferenceCensus.analyze(refs, flow, null);
        check(again.edges().size() == a.edges().size()
                && again.sourceDigest().equals(a.sourceDigest())
                && again.referenceProofKey().equals(a.referenceProofKey())
                && again.flowProofKey().equals(a.flowProofKey()), "deterministic compiler replay");
        // Pure signal-lattice permutation proof: no Java allocation-site or
        // runtime reference-count inference can sneak into the signal masks.
        java.util.Random permutations = new java.util.Random(0x4d33435245464cL);
        for (int i = 0; i < 30_000; i++) {
            int beforeKnown = permutations.nextInt(5);
            int afterKnown = permutations.nextInt(5);
            int beforeUnknown = permutations.nextInt(3);
            int afterUnknown = permutations.nextInt(3);
            int endingKnown = permutations.nextInt(beforeKnown + 1);
            int endingUnknown = permutations.nextInt(beforeUnknown + 1);
            java.util.BitSet exposed = new java.util.BitSet();
            java.util.BitSet repeated = new java.util.BitSet();
            if (permutations.nextBoolean()) exposed.set(1);
            if (permutations.nextBoolean()) repeated.set(1);
            var edge = new M3CompilerReferenceCensus.Edge("F.java", "F#f", 1L, 0, 1,
                    new int[] {beforeUnknown, beforeKnown},
                    new int[] {afterUnknown, afterKnown},
                    new int[] {endingUnknown, endingKnown}, exposed, repeated);
            int signal = edge.signalMask(1);
            // All 30,000 permutations must yield identical scalar and bulk
            // signals, with no accidental unknown-origin lane pollution.
            int[] lanes = new int[2];
            java.util.Arrays.fill(lanes, 0x7a7a7a7a);
            edge.signalMasks(lanes);
            check(lanes[0] == 0 && lanes[1] == signal,
                    "bulk primitive signal sidecar must match scalar");
            boolean uniqueHint = (signal & M3CompilerReferenceCensus.SINGLE_POSSIBLE_LOCAL) != 0;
            check(uniqueHint == (beforeUnknown + beforeKnown == 1 && beforeUnknown == 0
                    && !exposed.get(1) && !repeated.get(1)), "single-local mask is a hint only");
            check(((signal & M3CompilerReferenceCensus.MULTIPLE_POSSIBLE_LOCALS) != 0)
                    == (beforeUnknown + beforeKnown > 1), "multiple-local mask");
            check(((signal & M3CompilerReferenceCensus.UNKNOWN_ALIAS) != 0)
                    == (beforeUnknown > 0 || afterUnknown > 0), "unknown-local mask");
            check(((signal & M3CompilerReferenceCensus.EXTERNAL_EXPOSURE) != 0)
                    == exposed.get(1), "escape classification mask");
            check(((signal & M3CompilerReferenceCensus.LOOP_REPEATED_ORIGIN) != 0)
                    == repeated.get(1), "loop repetition mask");
            check(((signal & M3CompilerReferenceCensus.POSSIBLY_ENDING_LOCAL) != 0)
                    == (endingUnknown + endingKnown > 0), "possibly ending mask");
            check(((signal & M3CompilerReferenceCensus.LIVE_ACROSS_EDGE) != 0)
                    == (beforeUnknown + beforeKnown > 0 && afterUnknown + afterKnown > 0),
                    "liveness-across mask");
            check(((signal & M3CompilerReferenceCensus.NO_VISIBLE_LOCAL) != 0)
                    == (beforeUnknown + beforeKnown == 0), "no-visible-local mask");
            check(!edge.authorizesReclamation() && !edge.authorizesRcElision(),
                    "zero runtime authority for every mask combination");
            check(((signal & M3CompilerReferenceCensus.LAST_LOCAL_CANDIDATE) != 0)
                    == edge.possibleLastLocalAlias(1), "last-local signal remains diagnostic");
        }

        // Multi-origin, caller-owned array fanout: exact primitive lane
        // width, full overwrite and no fresh allocation inside exporter.
        java.util.Random bulkRandom = new java.util.Random(0x4d334352424c4bL);
        for (int round = 0; round < 8_192; round++) {
            int origins = 1 + bulkRandom.nextInt(48);
            int[] before = new int[origins + 1];
            int[] after = new int[origins + 1];
            int[] ending = new int[origins + 1];
            java.util.BitSet exposures = new java.util.BitSet();
            java.util.BitSet repeats = new java.util.BitSet();
            for (int site = 0; site <= origins; site++) {
                before[site] = bulkRandom.nextInt(3);
                after[site] = bulkRandom.nextInt(3);
                ending[site] = bulkRandom.nextInt(before[site] + 1);
                if (site > 0) {
                    if (bulkRandom.nextBoolean()) exposures.set(site);
                    if (bulkRandom.nextBoolean()) repeats.set(site);
                }
            }
            var candidate = new M3CompilerReferenceCensus.Edge(
                    "F.java", "F#f", 1, 0, 1, before, after, ending, exposures, repeats);
            int[] output = new int[origins + 1];
            java.util.Arrays.fill(output, Integer.MIN_VALUE);
            candidate.signalMasks(output);
            // CRef count lanes are immutable internally and reusable externally.
            int[] copiedBefore = new int[origins + 1];
            int[] copiedAfter = new int[origins + 1];
            int[] copiedEnding = new int[origins + 1];
            java.util.Arrays.fill(copiedBefore, -1);
            java.util.Arrays.fill(copiedAfter, -1);
            java.util.Arrays.fill(copiedEnding, -1);
            candidate.copyCountLanes(copiedBefore, copiedAfter, copiedEnding);
            check(java.util.Arrays.equals(before, copiedBefore), "before lane parity");
            check(java.util.Arrays.equals(after, copiedAfter), "after lane parity");
            check(java.util.Arrays.equals(ending, copiedEnding), "ending lane parity");
            // Caller-owned arrays cannot alias each other: otherwise one
            // lane overwrites another and an optimization could misread it.
            boolean invalidCopy = false;
            try { candidate.copyCountLanes(copiedBefore, copiedBefore, copiedEnding); }
            catch (IllegalArgumentException expected) { invalidCopy = true; }
            check(invalidCopy, "overlapping count lanes refused");
            invalidCopy = false;
            try { candidate.copyCountLanes(copiedBefore, new int[origins + 2], copiedEnding); }
            catch (IllegalArgumentException expected) { invalidCopy = true; }
            check(invalidCopy, "oversized count lane refused");
            invalidCopy = false;
            try { candidate.copyCountLanes(null, copiedAfter, copiedEnding); }
            catch (NullPointerException expected) { invalidCopy = true; }
            check(invalidCopy, "null count lane refused");
            // A readback must always return the original immutable facts.
            java.util.Arrays.fill(copiedBefore, Integer.MIN_VALUE);
            candidate.copyCountLanes(copiedBefore, copiedAfter, copiedEnding);
            check(java.util.Arrays.equals(before, copiedBefore), "source facts not mutated");
            check(output[0] == 0, "unknown origin zero cleared");
            for (int site = 1; site <= origins; site++)
                check(output[site] == candidate.signalMask(site),
                        "multi-origin bulk/scalar equivalence");
            boolean badLane = false;
            try { candidate.signalMasks(new int[origins]); }
            catch (IllegalArgumentException expected) { badLane = true; }
            check(badLane, "undersized output buffer refused");
            badLane = false;
            try { candidate.signalMasks(new int[origins + 2]); }
            catch (IllegalArgumentException expected) { badLane = true; }
            check(badLane, "oversized output buffer refused");
        }

        var overflow = new M3CompilerReferenceCensus.Edge("F.java", "F#f", 1, 0, 1,
                new int[] {Integer.MAX_VALUE, 1},
                new int[] {Integer.MAX_VALUE, 1},
                new int[] {Integer.MAX_VALUE, 0},
                new java.util.BitSet(), new java.util.BitSet());
        check(overflow.possibleBefore(1) == Integer.MAX_VALUE
                && overflow.possibleAfter(1) == Integer.MAX_VALUE
                && overflow.possiblyEnding(1) == Integer.MAX_VALUE,
                "saturating source-local upper counts refuse integer wraparound");
        check((overflow.signalMask(1)
                & M3CompilerReferenceCensus.MULTIPLE_POSSIBLE_LOCALS) != 0
                && (overflow.signalMask(1)
                & M3CompilerReferenceCensus.SINGLE_POSSIBLE_LOCAL) == 0,
                "overflow must not downgrade an ambiguous alias to singleton");
        // Unknown-origin source locals must never create a last-known-alias
        // candidate, even when this edge drops the only visible local slot.
        var onlyUnknown = new M3CompilerReferenceCensus.Edge("F.java", "F#f", 1, 0, 1,
                new int[] {1, 0}, new int[] {0, 0}, new int[] {1, 0},
                new java.util.BitSet(), new java.util.BitSet());
        check(onlyUnknown.possibleBefore(1) == 1
                && onlyUnknown.possiblyEnding(1) == 1
                && !onlyUnknown.possibleLastLocalAlias(1),
                "unknown source origin cannot prove last-known-local candidate");
        check((onlyUnknown.signalMask(1)
                & M3CompilerReferenceCensus.LAST_LOCAL_CANDIDATE) == 0,
                "unknown-origin end never gains last-local signal");
        boolean invalidSignal = false;
        try {
            a.edges().getFirst().signalMask(0);
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "reserved unknown-origin zero must never be a candidate");
        invalidSignal = false;
        try {
            new M3CompilerReferenceCensus.Edge("F.java", "F#f", 0, 0, 1,
                    new int[] {0, -1}, new int[] {0, 0}, new int[] {0, 0},
                    new java.util.BitSet(), new java.util.BitSet());
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "negative injected count refused");
        invalidSignal = false;
        java.util.BitSet invalidExposure = new java.util.BitSet();
        invalidExposure.set(3);
        try {
            new M3CompilerReferenceCensus.Edge("F.java", "F#f", 0, 0, 1,
                    new int[] {0, 0}, new int[] {0, 0}, new int[] {0, 0},
                    invalidExposure, new java.util.BitSet());
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "out-of-range exposure refused");
        invalidSignal = false;
        try {
            new M3CompilerReferenceCensus.Edge("F.java", "F#f", 0, 0, 1,
                    new int[] {0, 1}, new int[] {0, 0}, new int[] {0, 2},
                    new java.util.BitSet(), new java.util.BitSet());
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "ending known aliases cannot exceed before counts");
        invalidSignal = false;
        try {
            new M3CompilerReferenceCensus.Edge("F.java", "F#f", 0, 0, 1,
                    new int[] {0, 1}, new int[] {0, 0}, new int[] {1, 0},
                    new java.util.BitSet(), new java.util.BitSet());
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "ending unknown aliases cannot exceed before counts");
        java.util.BitSet originZero = new java.util.BitSet();
        originZero.set(0);
        invalidSignal = false;
        try {
            new M3CompilerReferenceCensus.Edge("F.java", "F#f", 0, 0, 1,
                    new int[] {0, 1}, new int[] {0, 0}, new int[] {0, 1},
                    originZero, new java.util.BitSet());
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "unknown origin exposure refused");
        invalidSignal = false;
        try {
            new M3CompilerReferenceCensus.Edge("F.java", "F#f", 0, 0, 1,
                    new int[] {0, 1}, new int[] {0, 0}, new int[] {0, 1},
                    new java.util.BitSet(), originZero);
        } catch (IllegalArgumentException expected) { invalidSignal = true; }
        check(invalidSignal, "unknown origin repetition refused");
        System.out.println("M3_COMPILER_REFERENCE_CENSUS_PASS checks=" + checks
                + " edges=" + a.edges().size());
    }
}
