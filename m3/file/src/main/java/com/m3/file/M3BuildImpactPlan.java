// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

// Modified 2026-10-21: Add explicit selection policy and file/source counts through the existing impact plan.

import java.util.Objects;

/**
 * Native M3JDK21 incremental build-impact propagation over the original
 * M3FileDeltaLedger physical slots. The source/class SHA-256 ledger owns file
 * identities; javac/M3AST/JDT supply the reverse dependency graph and semantic
 * evidence. Maven/Gradle consume the resulting impacted slot sequence.
 *
 * <p>All per-slot state lives in primitive arrays: a byte reason mask, int[]
 * reverse-dependency offsets/targets and a bounded int[] work queue. This class
 * does not parse Java/POMs, own an independent filesystem or duplicate any file
 * hashes. An uncertified graph, build-input delta or missing graph deliberately
 * forces a conservative recompile decision. ABI-safe hints are allowed only
 * from a compiler-proved generation-specific dependency result, never from
 * file hash or file-name similarity alone.
 *
 * <p>Every graph coordinate refers to an immutable snapshot physical slot;
 * removed entries remain as tombstones for output cleanup. The dependency
 * graph MUST be complete for the same source/class generation and include all
 * Java processor/generated-source edges before verified=true is used.
 */
public final class M3BuildImpactPlan {
    /** Exact changed source bytes; the compiler still has to examine that source. */
    public static final int SOURCE_DELTA = 1;
    /** Exact changed class bytes, not proof of API/ABI changes. */
    public static final int CLASS_DELTA = 1 << 1;
    /** Changed build input; options, processors and reactor models may differ. */
    public static final int BUILD_INPUT_DELTA = 1 << 2;
    /** Impact propagated through a compiler-authoritative reverse dependency edge. */
    public static final int DEPENDENT = 1 << 3;
    /** Missing proof requires conservative source/class compilation. */
    public static final int CONSERVATIVE = 1 << 4;

    /** Caller policy; an explicit incremental request cannot waive correctness. */
    public enum Mode { AUTO, FULL, INCREMENTAL }

    /** Executable result, distinct from the requested policy. */
    public enum Decision { NO_OP, INCREMENTAL, FULL }

    /** First applicable rule, in the precedence documented by {@link #select}. */
    public enum SelectionReason {
        REQUESTED_FULL, NO_CHANGES, BUILD_INPUT_CHANGE, UNVERIFIED_GRAPH,
        REQUESTED_INCREMENTAL, AFFECTED_FILE_THRESHOLD,
        AFFECTED_PERCENT_THRESHOLD, BELOW_THRESHOLDS
    }

    /**
     * Immutable build decision over the ledger's original physical slots.
     * Counts describe this delta generation, not accumulated historical slots.
     * Removed slots are retained separately even when no live source is selected.
     * This value does not execute a build or publish a successful ledger baseline.
     */
    public static final class Selection {
        private final Mode requestedMode;
        private final Decision decision;
        private final SelectionReason reason;
        private final int totalLiveFiles;
        private final int totalLiveSources;
        private final int createdFiles;
        private final int modifiedFiles;
        private final int removedFiles;
        private final int[] affectedSourceSlots;
        private final int[] selectedSourceSlots;
        private final int[] removedSlots;

        private Selection(Mode requestedMode, Decision decision, SelectionReason reason,
                int totalLiveFiles, int totalLiveSources, int createdFiles,
                int modifiedFiles, int removedFiles, int[] affectedSourceSlots,
                int[] selectedSourceSlots, int[] removedSlots) {
            this.requestedMode = requestedMode;
            this.decision = decision;
            this.reason = reason;
            this.totalLiveFiles = totalLiveFiles;
            this.totalLiveSources = totalLiveSources;
            this.createdFiles = createdFiles;
            this.modifiedFiles = modifiedFiles;
            this.removedFiles = removedFiles;
            this.affectedSourceSlots = affectedSourceSlots;
            this.selectedSourceSlots = selectedSourceSlots;
            this.removedSlots = removedSlots;
        }

        public Mode requestedMode() { return requestedMode; }
        public Decision decision() { return decision; }
        public SelectionReason reason() { return reason; }
        /** True for cleanup-only work and for an explicit full request as well. */
        public boolean hasWork() { return decision != Decision.NO_OP; }
        /** All live SOURCE, CLASS and BUILD_INPUT files; tombstones are excluded. */
        public int totalLiveFiles() { return totalLiveFiles; }
        public int totalLiveSources() { return totalLiveSources; }
        /** Direct created files of every tracked kind, without dependent fanout. */
        public int createdFiles() { return createdFiles; }
        /** Direct modified files of every tracked kind, without dependent fanout. */
        public int modifiedFiles() { return modifiedFiles; }
        /** Direct removals of every tracked kind in this generation only. */
        public int removedFiles() { return removedFiles; }
        public int directChangedFiles() { return createdFiles + modifiedFiles + removedFiles; }
        public int affectedSourceCount() { return affectedSourceSlots.length; }
        public int selectedSourceCount() { return selectedSourceSlots.length; }
        public double affectedSourcePercent() {
            return totalLiveSources == 0 ? 0.0
                    : 100.0 * affectedSourceSlots.length / totalLiveSources;
        }
        /** Live source closure already computed by this impact plan. */
        public int[] affectedSourceSlots() { return affectedSourceSlots.clone(); }
        /** Ascending physical slots to compile; empty does not imply no work. */
        public int[] selectedSourceSlots() { return selectedSourceSlots.clone(); }
        /** Original physical slots whose outputs may need deletion or cleanup. */
        public int[] removedSlots() { return removedSlots.clone(); }
    }

    private final M3FileDeltaLedger.Snapshot sourceSnapshot;
    private final byte[] reasons;
    private final int[] impactedSlots;
    private final boolean fullRebuild;

    private M3BuildImpactPlan(M3FileDeltaLedger.Snapshot snapshot,
            byte[] reasons, int[] impactedSlots, boolean fullRebuild) {
        this.sourceSnapshot = snapshot;
        this.reasons = reasons;
        this.impactedSlots = impactedSlots;
        this.fullRebuild = fullRebuild;
    }

    /** Original physical file slot indexes in ascending order, never logical ordinals. */
    public int[] impactedSlots() { return impactedSlots.clone(); }
    public int size() { return impactedSlots.length; }
    public boolean isEmpty() { return impactedSlots.length == 0; }
    public boolean requiresConservativeBuild() { return fullRebuild; }
    public int reasonAt(int physicalSlot) {
        return Byte.toUnsignedInt(reasons[Objects.checkIndex(physicalSlot, reasons.length)]);
    }

    /**
     * Select work without introducing another graph or persisted state store.
     * The caller must supply the exact immutable snapshot instance used by
     * {@link #compute}; the plan retains one reference, not a copy of file payloads.
     * Identity, slot dimensions and direct delta/kind masks are checked before
     * selection. These checks do not certify the caller's semantic graph or
     * toolchain proof.
     *
     * <p>FULL selects every live SOURCE even with no deltas. Otherwise an empty
     * impact plan is NO_OP. A conservative plan then forces FULL, including for
     * requested INCREMENTAL; a changed build input takes reason precedence over
     * an unverified graph. Requested INCREMENTAL bypasses only the cost policy.
     *
     * <p>In AUTO, either inclusive threshold can choose FULL. The file threshold
     * counts affected live SOURCE files; the percentage uses all live SOURCE
     * files as its denominator, never historical slots or generated class files.
     * A value of -1 disables that threshold. The file threshold is otherwise
     * nonnegative; the percentage must be finite and in [0, 100]. If both are
     * reached, the file threshold supplies the reason. Thresholds are considered
     * only when at least one live source is affected; deletion-only work still
     * yields INCREMENTAL with removal slots, not NO_OP.
     */
    public Selection select(M3FileDeltaLedger.Snapshot snapshot, Mode mode,
            int affectedFileThreshold, double affectedPercentThreshold) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(mode, "mode");
        if (affectedFileThreshold < -1 || !Double.isFinite(affectedPercentThreshold)
                || (affectedPercentThreshold != -1.0
                    && (affectedPercentThreshold < 0.0 || affectedPercentThreshold > 100.0))) {
            throw new IllegalArgumentException("invalid M3 affected-source thresholds");
        }
        if (snapshot.slots() != reasons.length) {
            throw new IllegalArgumentException("file plan/snapshot slot dimensions differ");
        }
        if (snapshot != sourceSnapshot) {
            throw new IllegalArgumentException("file plan belongs to different snapshot instance");
        }
        int liveFiles = 0, liveSources = 0, created = 0, modified = 0, removed = 0;
        boolean buildInputChange = false;
        for (int slot = 0; slot < reasons.length; slot++) {
            int direct = reasons[slot] & (SOURCE_DELTA | CLASS_DELTA | BUILD_INPUT_DELTA);
            int expected = snapshot.changedAt(slot) ? switch (snapshot.kindAt(slot)) {
                case SOURCE -> SOURCE_DELTA;
                case CLASS -> CLASS_DELTA;
                case BUILD_INPUT -> BUILD_INPUT_DELTA;
            } : 0;
            if (direct != expected) {
                throw new IllegalArgumentException("file plan belongs to different delta generation");
            }
            if (direct == BUILD_INPUT_DELTA) buildInputChange = true;
            long state = snapshot.stateAt(slot);
            if ((state & M3FileDeltaLedger.LIVE) != 0) {
                liveFiles++;
                if (snapshot.kindAt(slot) == M3FileDeltaLedger.Kind.SOURCE) liveSources++;
            }
            if ((state & M3FileDeltaLedger.CREATED) != 0) created++;
            if ((state & M3FileDeltaLedger.MODIFIED) != 0) modified++;
            if ((state & M3FileDeltaLedger.REMOVED) != 0) removed++;
        }
        int affected = 0;
        for (int slot : impactedSlots) {
            if (snapshot.liveAt(slot) && snapshot.kindAt(slot) == M3FileDeltaLedger.Kind.SOURCE) {
                affected++;
            }
        }
        int[] affectedSources = new int[affected];
        int affectedAt = 0;
        for (int slot : impactedSlots) {
            if (snapshot.liveAt(slot) && snapshot.kindAt(slot) == M3FileDeltaLedger.Kind.SOURCE) {
                affectedSources[affectedAt++] = slot;
            }
        }
        Decision decision;
        SelectionReason reason;
        if (mode == Mode.FULL) {
            decision = Decision.FULL;
            reason = SelectionReason.REQUESTED_FULL;
        } else if (isEmpty()) {
            decision = Decision.NO_OP;
            reason = SelectionReason.NO_CHANGES;
        } else if (requiresConservativeBuild()) {
            decision = Decision.FULL;
            reason = buildInputChange ? SelectionReason.BUILD_INPUT_CHANGE
                    : SelectionReason.UNVERIFIED_GRAPH;
        } else if (mode == Mode.INCREMENTAL) {
            decision = Decision.INCREMENTAL;
            reason = SelectionReason.REQUESTED_INCREMENTAL;
        } else if (affectedSources.length > 0 && affectedFileThreshold >= 0
                && affectedSources.length >= affectedFileThreshold) {
            decision = Decision.FULL;
            reason = SelectionReason.AFFECTED_FILE_THRESHOLD;
        } else if (affectedSources.length > 0 && affectedPercentThreshold >= 0.0
                && 100.0 * affectedSources.length >= affectedPercentThreshold * liveSources) {
            decision = Decision.FULL;
            reason = SelectionReason.AFFECTED_PERCENT_THRESHOLD;
        } else {
            decision = Decision.INCREMENTAL;
            reason = SelectionReason.BELOW_THRESHOLDS;
        }
        int[] selectedSources = affectedSources;
        if (decision == Decision.FULL) {
            selectedSources = new int[liveSources];
            for (int slot = 0, at = 0; slot < reasons.length; slot++) {
                if (snapshot.liveAt(slot) && snapshot.kindAt(slot) == M3FileDeltaLedger.Kind.SOURCE) {
                    selectedSources[at++] = slot;
                }
            }
        }
        int[] removedSlots = new int[removed];
        for (int slot = 0, at = 0; slot < reasons.length; slot++) {
            if ((snapshot.stateAt(slot) & M3FileDeltaLedger.REMOVED) != 0) removedSlots[at++] = slot;
        }
        return new Selection(mode, decision, reason, liveFiles, liveSources, created,
                modified, removed, affectedSources, selectedSources, removedSlots);
    }

    /**
     * Build one deterministic impact set. In verified mode offsets are CSR
     * boundaries (length=slots+1) and targets are downstream physical slots
     * (dependency changes flow towards consumers). A byte value of 1 in
     * {@code propagationSafe} means javac/JDT proved this exact changed slot
     * cannot invalidate any dependent compilation. A 0 requires propagation.
     * All other values are rejected. Unknown processors or missing ABI evidence
     * MUST be represented as 0. A build-input delta always overrides this hint.
     *
     * <p>When verified=false, offsets/targets/hints must be null, and any
     * changed input conservatively invalidates every live SOURCE or CLASS slot.
     * An incomplete graph must never be passed with verified=true.
     */
    public static M3BuildImpactPlan compute(M3FileDeltaLedger.Snapshot snapshot,
            boolean verified, int[] dependentOffsets, int[] dependentTargets,
            byte[] propagationSafe) {
        Objects.requireNonNull(snapshot, "snapshot");
        int count = snapshot.slots();
        if (verified) validate(count, dependentOffsets, dependentTargets, propagationSafe);
        else if (dependentOffsets != null || dependentTargets != null || propagationSafe != null) {
            throw new IllegalArgumentException("unverified dependencies must not be supplied");
        }
        byte[] bits = new byte[count];
        int[] queue = new int[count];
        byte[] queued = new byte[count];
        int write = 0;
        boolean changed = false;
        boolean buildInputChange = false;
        for (int slot = 0; slot < count; slot++) {
            if (!snapshot.changedAt(slot)) continue;
            changed = true;
            int reason = switch (snapshot.kindAt(slot)) {
                case SOURCE -> SOURCE_DELTA;
                case CLASS -> CLASS_DELTA;
                case BUILD_INPUT -> BUILD_INPUT_DELTA;
            };
            bits[slot] = (byte) reason;
            if (reason == BUILD_INPUT_DELTA) buildInputChange = true;
        }
        if (!changed) return new M3BuildImpactPlan(snapshot, bits, new int[0], false);

        boolean conservative = !verified || buildInputChange;
        if (conservative) {
            for (int slot = 0; slot < count; slot++) {
                if (snapshot.liveAt(slot) && snapshot.kindAt(slot) != M3FileDeltaLedger.Kind.BUILD_INPUT) {
                    bits[slot] |= (byte) CONSERVATIVE;
                }
            }
        } else {
            // Only semantically unsafe direct deltas seed propagation; a stable
            // ABI source still recompiles itself but does not invalidate users.
            for (int slot = 0; slot < count; slot++) {
                if (bits[slot] != 0 && propagationSafe[slot] == 0) {
                    queued[slot] = 1;
                    queue[write++] = slot;
                }
            }
            // BFS consumes each dependent slot at most once, even across cycles.
            // The row mask avoids retaining per-edge or per-object node wrappers.
            for (int read = 0; read < write; read++) {
                int current = queue[read];
                for (int edge = dependentOffsets[current]; edge < dependentOffsets[current + 1]; edge++) {
                    int next = dependentTargets[edge];
                    if ((bits[next] & DEPENDENT) == 0) {
                        bits[next] |= (byte) DEPENDENT;
                        // A directly changed slot with safe ABI may still be
                        // indirectly invalidated through another changed owner.
                        // Subsequent propagation must not be skipped in that case.
                        if (queued[next] == 0) {
                            queued[next] = 1;
                            queue[write++] = next;
                        }
                    }
                }
            }
        }
        int impacted = 0;
        for (byte bit : bits) if (bit != 0) impacted++;
        int[] selected = new int[impacted];
        for (int slot = 0, out = 0; slot < count; slot++) {
            if (bits[slot] != 0) selected[out++] = slot;
        }
        return new M3BuildImpactPlan(snapshot, bits, selected, conservative);
    }

    private static void validate(int count, int[] offsets, int[] targets, byte[] safe) {
        Objects.requireNonNull(offsets, "dependentOffsets");
        Objects.requireNonNull(targets, "dependentTargets");
        Objects.requireNonNull(safe, "propagationSafe");
        if (offsets.length != count + 1 || safe.length != count
                || offsets[0] != 0 || offsets[count] != targets.length) {
            throw new IllegalArgumentException("mismatched M3 dependency-array dimensions");
        }
        for (int i = 0; i < count; i++) {
            if (offsets[i] > offsets[i + 1] || offsets[i] < 0
                    || offsets[i + 1] > targets.length || (safe[i] != 0 && safe[i] != 1)) {
                throw new IllegalArgumentException("invalid M3 dependency offsets or ABI flags");
            }
        }
        for (int target : targets) {
            if (target < 0 || target >= count) {
                throw new IllegalArgumentException("invalid dependent physical slot");
            }
        }
    }
}
