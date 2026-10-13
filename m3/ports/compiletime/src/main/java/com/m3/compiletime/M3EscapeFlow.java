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
 * Forward def-use escape classification of one obtaining site over {@code SourceInterpreter}
 * frames (pattern: the C2 escape lattice and Error Prone's ReferenceEquality, no code copied).
 * Copies ({@code ALOAD}/{@code ASTORE}, {@code DUP*}, {@code CHECKCAST}, {@code AALOAD} of a
 * tracked array) extend the tracked set to a fixpoint; every consuming instruction then adds its
 * escape kind. A receiver call on the handle is behaviour.
 */
final class M3EscapeFlow {
    private M3EscapeFlow() {
    }

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
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted);
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X1:
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted) || top(insns, f, 2, tainted);
            case Opcodes.DUP2_X2:
                return top(insns, f, 0, tainted) || top(insns, f, 1, tainted) || top(insns, f, 2, tainted)
                        || top(insns, f, 3, tainted);
            case Opcodes.AALOAD:
                return top(insns, f, 1, tainted);
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
        if (scan.retainingMethods.contains(key) || RETAINING_CALLEES.contains(key)) return RETAINS_CALLEE;
        if (scan.behaviourMethods.contains(key) || BEHAVIOUR_CALLEES.contains(key)) return 0;
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
