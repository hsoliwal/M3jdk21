// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

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

    private final byte[] reasons;
    private final int[] impactedSlots;
    private final boolean fullRebuild;

    private M3BuildImpactPlan(byte[] reasons, int[] impactedSlots, boolean fullRebuild) {
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
        if (!changed) return new M3BuildImpactPlan(bits, new int[0], false);

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
        return new M3BuildImpactPlan(bits, selected, conservative);
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
