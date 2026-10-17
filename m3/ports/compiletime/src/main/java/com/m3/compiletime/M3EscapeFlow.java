// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import static com.m3.compiletime.M3ItemReferenceScan.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

/**
 * Def-use escape classification over {@code SourceInterpreter}
 * frames (pattern: the C2 escape lattice and Error Prone's ReferenceEquality, no code copied).
 * Copies ({@code ALOAD}/{@code ASTORE}, {@code DUP*}/{@code SWAP}, {@code CHECKCAST}, {@code AALOAD} of a
 * tracked array) extend the tracked set to a fixpoint; every consuming instruction then adds its
 * escape kind. A receiver call on the handle is behaviour. The prepared path solves this
 * same conservative classifier for every instruction once; it does not prove general method
 * ownership, runtime reference counts, callee effects or GC eligibility.
 */
final class M3EscapeFlow {
    static final int MAX_PREPARED_EDGES = 1 << 20;
    static final long MAX_PREPARED_BYTES = 32L << 20;
    static final long MAX_PREPARED_SOURCE_VISITS = 4_000_000L;

    private M3EscapeFlow() {
    }

    /**
     * Method-local answers indexed by the unchanged ASM instruction coordinates, or null when
     * preparation exceeds its budget. Callers then use {@link #escape} for the same answer.
     * Frames, instruction identities and the scan's exact callee table must stay fixed during
     * this call. Only the returned primitive answers escape preparation; there is no cache.
     *
     * <p>For a copy q with source p, the forward classifier propagates taint p to q. Its
     * distributive equivalent is mask[p] |= mask[q], seeded by every consuming operand's
     * unchanged use mask. The least fixed point includes loops and every SourceValue merge.
     * Each of the eleven escape bits can be added to a node once: closure is O(11*(N+E));
     * each obtaining-site query afterwards is O(1). No N-by-site reachability table is built.
     * The bounded CSR/worklist pattern follows the reviewed Synexia Horn/CSR owners; no rule
     * engine, canonical labels or runtime dependency is imported.
     */
    static int[] prepare(M3ItemReferenceScan scan, MethodNode m, Frame<SourceValue>[] frames) {
        return prepare(scan, m, frames, MAX_PREPARED_EDGES, MAX_PREPARED_BYTES,
                MAX_PREPARED_SOURCE_VISITS);
    }

    /** Package-local budget seam; bytes bound primitive array payload, not ASM or VM headers. */
    static int[] prepare(M3ItemReferenceScan scan, MethodNode m, Frame<SourceValue>[] frames,
            int maxEdges, long maxBytes, long maxSourceVisits) {
        if (maxEdges < 0 || maxBytes < 0 || maxSourceVisits < 0) {
            throw new IllegalArgumentException("negative escape preparation budget");
        }
        InsnList insns = m.instructions;
        int n = insns.size();
        // offsets[n+1], masks[n], queue[n], queued[n], then sources[edgeCount].
        long baseBytes = 13L * n + Integer.BYTES;
        if (n == Integer.MAX_VALUE || baseBytes > maxBytes) return null;
        int[] offsets = new int[n + 1];
        for (int i = 0; i < n; i++) {
            long edges = offsets[i];
            if (frames[i] != null) edges += copySourceCount(insns.get(i), frames[i]);
            if (edges > maxEdges || edges > maxSourceVisits
                    || baseBytes + Integer.BYTES * edges > maxBytes) return null;
            offsets[i + 1] = (int) edges;
        }
        int[] sources = new int[offsets[n]];
        for (int i = 0; i < n; i++) {
            if (frames[i] != null) copySources(insns, insns.get(i), frames[i], sources, offsets[i]);
        }
        int[] masks = new int[n];
        long remaining = maxSourceVisits - sources.length;
        for (int i = 0; i < n; i++) {
            if (frames[i] != null) {
                remaining = seedUse(scan, insns, insns.get(i), frames[i], masks, remaining);
                if (remaining < 0) return null;
            }
        }
        close(offsets, sources, masks);
        return masks;
    }

    /** -1 denotes ALOAD's local; other bits denote conservative input stack depths. */
    private static int copyInputs(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case Opcodes.ALOAD -> -1;
            case Opcodes.ASTORE, Opcodes.CHECKCAST, Opcodes.DUP -> 1;
            case Opcodes.DUP_X1, Opcodes.DUP2, Opcodes.SWAP -> 3;
            case Opcodes.DUP_X2, Opcodes.DUP2_X1 -> 7;
            case Opcodes.DUP2_X2 -> 15;
            case Opcodes.AALOAD -> 2;
            case Opcodes.INVOKESTATIC -> {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals("java/util/Objects") || !call.name.equals("requireNonNull")) yield 0;
                yield switch (call.desc) {
                    case "(Ljava/lang/Object;)Ljava/lang/Object;" -> 1;
                    case "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                            "(Ljava/lang/Object;Ljava/util/function/Supplier;)Ljava/lang/Object;" -> 2;
                    default -> 0;
                };
            }
            default -> 0;
        };
    }

    private static SourceValue topValue(Frame<SourceValue> frame, int depth) {
        int size = frame.getStackSize();
        return size > depth ? frame.getStack(size - 1 - depth) : null;
    }

    private static long copySourceCount(AbstractInsnNode insn, Frame<SourceValue> frame) {
        int inputs = copyInputs(insn);
        if (inputs == -1) return frame.getLocal(((VarInsnNode) insn).var).insns.size();
        long count = 0;
        for (int depth = 0; depth < 4; depth++) {
            if ((inputs & (1 << depth)) != 0) {
                SourceValue value = topValue(frame, depth);
                if (value != null) count += value.insns.size();
            }
        }
        return count;
    }

    private static void copySources(InsnList insns, AbstractInsnNode insn, Frame<SourceValue> frame,
            int[] sources, int position) {
        int inputs = copyInputs(insn);
        if (inputs == -1) {
            appendSources(insns, frame.getLocal(((VarInsnNode) insn).var), sources, position);
        } else {
            for (int depth = 0; depth < 4; depth++) {
                if ((inputs & (1 << depth)) != 0) {
                    position = appendSources(insns, topValue(frame, depth), sources, position);
                }
            }
        }
    }

    private static int appendSources(InsnList insns, SourceValue value, int[] sources, int position) {
        if (value != null) {
            for (AbstractInsnNode source : value.insns) sources[position++] = insns.indexOf(source);
        }
        return position;
    }

    private static long seedTop(InsnList insns, Frame<SourceValue> frame, int depth, int mask,
            int[] masks, long remaining) {
        if (mask == 0 || remaining < 0) return remaining;
        SourceValue value = topValue(frame, depth);
        if (value == null) return remaining;
        if (value.insns.size() > remaining) return -1;
        for (AbstractInsnNode source : value.insns) masks[insns.indexOf(source)] |= mask;
        return remaining - value.insns.size();
    }

    private static long seedUse(M3ItemReferenceScan scan, InsnList insns, AbstractInsnNode insn,
            Frame<SourceValue> frame, int[] masks, long remaining) {
        switch (insn.getOpcode()) {
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
                return seedTop(insns, frame, 0, FIELD, masks, remaining);
            case Opcodes.AASTORE:
                return seedTop(insns, frame, 0, ARRAY, masks, remaining);
            case Opcodes.ARETURN:
                return seedTop(insns, frame, 0, RETURN, masks, remaining);
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
                remaining = seedTop(insns, frame, 0, IDENTITY_COMPARE, masks, remaining);
                return seedTop(insns, frame, 1, IDENTITY_COMPARE, masks, remaining);
            case Opcodes.MONITORENTER:
                return seedTop(insns, frame, 0, MONITOR, masks, remaining);
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEINTERFACE: {
                MethodInsnNode call = (MethodInsnNode) insn;
                int args = Type.getArgumentTypes(call.desc).length;
                if (call.getOpcode() != Opcodes.INVOKESTATIC) {
                    int receiver = call.name.equals("hashCode") && call.desc.equals("()I") ? IDENTITY_HASH
                            : call.name.equals("equals") && call.desc.equals("(Ljava/lang/Object;)Z")
                                    ? IDENTITY_COMPARE : 0;
                    remaining = seedTop(insns, frame, args, receiver, masks, remaining);
                }
                int mask = args == 0 ? 0 : callee(scan, call);
                for (int depth = 0; depth < args && remaining >= 0; depth++) {
                    remaining = seedTop(insns, frame, depth, mask, masks, remaining);
                }
                return remaining;
            }
            case Opcodes.INVOKEDYNAMIC: {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                if (indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")) return remaining;
                int args = Type.getArgumentTypes(indy.desc).length;
                for (int depth = 0; depth < args && remaining >= 0; depth++) {
                    remaining = seedTop(insns, frame, depth, LAMBDA, masks, remaining);
                }
                return remaining;
            }
            default:
                return remaining;
        }
    }

    private static void close(int[] offsets, int[] sources, int[] masks) {
        int n = masks.length;
        int[] queue = new int[n];
        boolean[] queued = new boolean[n];
        int head = 0, tail = 0, size = 0;
        for (int i = 0; i < n; i++) {
            if (masks[i] != 0) {
                queue[tail++] = i;
                if (tail == n) tail = 0;
                queued[i] = true;
                size++;
            }
        }
        while (size != 0) {
            int current = queue[head++];
            if (head == n) head = 0;
            size--;
            queued[current] = false;
            int mask = masks[current];
            for (int edge = offsets[current]; edge < offsets[current + 1]; edge++) {
                int source = sources[edge];
                int merged = masks[source] | mask;
                if (merged != masks[source]) {
                    masks[source] = merged;
                    if (!queued[source]) {
                        queue[tail++] = source;
                        if (tail == n) tail = 0;
                        queued[source] = true;
                        size++;
                    }
                }
            }
        }
    }

    /** Corrected original per-site fixed point, retained as the budget fallback. */
    static int escape(M3ItemReferenceScan scan, MethodNode m, Frame<SourceValue>[] frames, AbstractInsnNode site) {
        InsnList insns = m.instructions;
        int n = insns.size();
        boolean[] tainted = new boolean[n];
        tainted[insns.indexOf(site)] = true;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < n; i++) {
                if (tainted[i] || frames[i] == null) continue;
                if (propagates(insns, insns.get(i), frames[i], tainted)) {
                    tainted[i] = true;
                    changed = true;
                }
            }
        }
        int mask = 0;
        for (int i = 0; i < n; i++) {
            if (frames[i] != null) mask |= use(scan, insns, insns.get(i), frames[i], tainted);
        }
        return mask;
    }

    private static boolean tainted(InsnList insns, SourceValue v, boolean[] tainted) {
        for (AbstractInsnNode source : v.insns) {
            if (tainted[insns.indexOf(source)]) return true;
        }
        return false;
    }

    private static boolean top(InsnList insns, Frame<SourceValue> f, int depth, boolean[] tainted) {
        int size = f.getStackSize();
        return size > depth && tainted(insns, f.getStack(size - 1 - depth), tainted);
    }

    private static boolean propagates(InsnList insns, AbstractInsnNode insn, Frame<SourceValue> f, boolean[] tainted) {
        switch (insn.getOpcode()) {
            case Opcodes.ALOAD:
                return tainted(insns, f.getLocal(((VarInsnNode) insn).var), tainted);
            case Opcodes.ASTORE:
            case Opcodes.CHECKCAST:
            case Opcodes.DUP:
                return top(insns, f, 0, tainted);
            case Opcodes.DUP_X1:
            case Opcodes.DUP2:
            case Opcodes.SWAP:
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted);
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X1:
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted) || top(insns, f, 2, tainted);
            case Opcodes.DUP2_X2:
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted) || top(insns, f, 2, tainted)
                        || top(insns, f, 3, tainted);
            case Opcodes.AALOAD:
                return top(insns, f, 1, tainted);
            case Opcodes.INVOKESTATIC: {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals("java/util/Objects") || !call.name.equals("requireNonNull")) {
                    return false;
                }
                // Each exact JDK overload returns its first argument on normal completion.
                int depth = switch (call.desc) {
                    case "(Ljava/lang/Object;)Ljava/lang/Object;" -> 0;
                    case "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                            "(Ljava/lang/Object;Ljava/util/function/Supplier;)Ljava/lang/Object;" -> 1;
                    default -> -1;
                };
                return depth >= 0 && top(insns, f, depth, tainted);
            }
            default:
                return false;
        }
    }

    private static int use(M3ItemReferenceScan scan, InsnList insns, AbstractInsnNode insn, Frame<SourceValue> f,
            boolean[] tainted) {
        switch (insn.getOpcode()) {
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
                return top(insns, f, 0, tainted) ? FIELD : 0;
            case Opcodes.AASTORE:
                return top(insns, f, 0, tainted) ? ARRAY : 0;
            case Opcodes.ARETURN:
                return top(insns, f, 0, tainted) ? RETURN : 0;
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted) ? IDENTITY_COMPARE : 0;
            case Opcodes.MONITORENTER:
                return top(insns, f, 0, tainted) ? MONITOR : 0;
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEINTERFACE:
                return call(scan, insns, (MethodInsnNode) insn, f, tainted);
            case Opcodes.INVOKEDYNAMIC:
                return indy(insns, (InvokeDynamicInsnNode) insn, f, tainted);
            default:
                return 0;
        }
    }

    private static int indy(InsnList insns, InvokeDynamicInsnNode indy, Frame<SourceValue> f, boolean[] tainted) {
        int args = Type.getArgumentTypes(indy.desc).length;
        for (int j = 0; j < args; j++) {
            if (top(insns, f, j, tainted)) {
                return indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory") ? 0 : LAMBDA;
            }
        }
        return 0;
    }

    private static int call(M3ItemReferenceScan scan, InsnList insns, MethodInsnNode call, Frame<SourceValue> f,
            boolean[] tainted) {
        int args = Type.getArgumentTypes(call.desc).length;
        boolean receiver = call.getOpcode() != Opcodes.INVOKESTATIC;
        int mask = 0;
        if (receiver && top(insns, f, args, tainted)) {
            if (call.name.equals("hashCode") && call.desc.equals("()I")) mask |= IDENTITY_HASH;
            else if (call.name.equals("equals") && call.desc.equals("(Ljava/lang/Object;)Z")) mask |= IDENTITY_COMPARE;
        }
        for (int j = 0; j < args; j++) {
            if (top(insns, f, args - 1 - j, tainted)) mask |= callee(scan, call);
        }
        return mask;
    }

    /** Escape kind of passing a tracked handle as a non-receiver argument to {@code call}. */
    static int callee(M3ItemReferenceScan scan, MethodInsnNode call) {
        String key = call.owner + "." + call.name;
        String annotationKey = key + call.desc;
        if (scan.retainingMethods.contains(annotationKey) || RETAINING_CALLEES.contains(key)) return RETAINS_CALLEE;
        if (scan.behaviourMethods.contains(annotationKey) || BEHAVIOUR_CALLEES.contains(key)) return 0;
        if ((call.owner.startsWith("org/eclipse/swt/") || call.owner.startsWith("org/eclipse/nebula/"))
                && BEHAVIOUR_NAMES_ON_SWT.contains(call.name)) return 0;
        if (call.owner.equals("java/lang/System") && call.name.equals("identityHashCode")) return IDENTITY_HASH;
        if (call.owner.equals("java/util/Objects")) {
            if (call.name.equals("equals")) return IDENTITY_COMPARE;
            if (call.name.startsWith("hash")) return IDENTITY_HASH;
        }
        if (call.owner.startsWith("java/util/")) {
            if (COLLECTION_NAMES.contains(call.name) || call.name.equals("asList")) return COLLECTION;
            if (call.name.equals("contains") || call.name.equals("indexOf") || call.name.equals("lastIndexOf")) {
                return IDENTITY_COMPARE;
            }
            if (KEY_LOOKUP_NAMES.contains(call.name)) return IDENTITY_HASH;
        }
        return ARG_UNKNOWN;
    }
}
