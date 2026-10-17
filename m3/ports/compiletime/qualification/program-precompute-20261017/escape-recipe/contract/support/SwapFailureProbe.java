// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.SourceInterpreter;

/** Permanent same-failure witness, compiled independently against old and emitted owners. */
public final class SwapFailureProbe {
    private SwapFailureProbe() { }

    public static void main(String[] args) throws Exception {
        String fixture = "com/m3/compiletime/fixture/";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "swap", "(L" + fixture + "FakeTable;)V", null, null);
        method.maxStack = 2;
        method.maxLocals = 1;
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        MethodInsnNode obtain = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fixture + "FakeTable",
                "getItem", "(I)L" + fixture + "FakeItem;", false);
        method.instructions.add(obtain);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.SWAP));
        method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, fixture + "Witness", "kept",
                "L" + fixture + "FakeItem;"));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        var frames = new Analyzer<>(new SourceInterpreter()).analyze(fixture + "Witness", method);
        int actual = M3EscapeFlow.escape(new M3ItemReferenceScan(), method, frames, obtain);
        if (actual != M3ItemReferenceScan.FIELD) {
            throw new AssertionError("SWAP_REFERENCE_ESCAPE_LOST expected=1 actual=" + actual);
        }
        System.out.println("M3_SWAP_WITNESS_PASS mask=" + actual + " instructions=" + method.instructions.size());
    }
}
