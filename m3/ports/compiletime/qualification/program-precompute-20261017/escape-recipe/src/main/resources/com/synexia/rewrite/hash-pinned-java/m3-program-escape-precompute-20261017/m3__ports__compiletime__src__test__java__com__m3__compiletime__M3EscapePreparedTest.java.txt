// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import static com.m3.compiletime.M3ItemReferenceScan.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

/** Frozen forward oracle versus every prepared instruction, plus executable SWAP witnesses. */
public final class M3EscapePreparedTest {
    private static final String FIXTURE = "com/m3/compiletime/fixture/";
    private static final String ITEM = FIXTURE + "FakeItem";
    private static final String TABLE = FIXTURE + "FakeTable";
    private static final String GENERATED = FIXTURE + "PreparedSwapWitness";
    private static final String OBJECT = "Ljava/lang/Object;";

    static Frame<SourceValue>[] frames(MethodNode method) throws AnalyzerException {
        return new Analyzer<>(new SourceInterpreter()).analyze(GENERATED, method);
    }

    static MethodNode method(String name, String descriptor, int locals) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                name, descriptor, null, null);
        method.maxLocals = locals;
        method.maxStack = 16;
        return method;
    }

    private static int[] everyInstruction(M3ItemReferenceScan scan, MethodNode method)
            throws AnalyzerException {
        Frame<SourceValue>[] frames = frames(method);
        int[] actual = M3EscapeFlow.prepare(scan, method, frames);
        assertNotNull(actual, method.name);
        assertEquals(method.instructions.size(), actual.length);
        for (int i = 0; i < actual.length; i++) {
            int expected = M3EscapeFlow.escape(scan, method, frames, method.instructions.get(i));
            assertEquals(expected, actual[i], method.name + " instruction=" + i
                    + " opcode=" + method.instructions.get(i).getOpcode());
        }
        assertArrayEquals(actual, M3EscapeFlow.prepare(scan, method, frames), "deterministic preparation");
        return actual;
    }

    private static int[] everyInstruction(MethodNode method) throws AnalyzerException {
        return everyInstruction(new M3ItemReferenceScan(), method);
    }

    private static void obtain(MethodNode method, int tableLocal, int index) {
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, tableLocal));
        method.instructions.add(new LdcInsnNode(index));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TABLE,
                "getItem", "(I)L" + ITEM + ";", false));
    }

    static MethodNode swapWitness(boolean itemFirst, boolean localCopy) {
        MethodNode method = method("swap" + itemFirst + localCopy, "(L" + TABLE + ";)V", 2);
        if (!itemFirst) method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        obtain(method, 0, 1);
        if (itemFirst) method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.SWAP));
        if (!itemFirst) method.instructions.add(new InsnNode(Opcodes.POP));
        if (localCopy) {
            method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, ITEM));
        }
        method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, GENERATED, "kept", "L" + ITEM + ";"));
        if (itemFirst) method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    static byte[] classBytes(MethodNode... methods) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, GENERATED, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "kept", "L" + ITEM + ";", null, null).visitEnd();
        for (MethodNode method : methods) method.accept(writer);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] bytes(String name) throws IOException {
        try (InputStream stream = M3EscapePreparedTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream, name);
            return stream.readAllBytes();
        }
    }

    private static M3ItemReferenceScan policy() throws IOException {
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        for (String name : new String[] {"FakeItem", "FakeTable", "ItemUsers"}) scan.hierarchy(bytes(FIXTURE + name));
        scan.closeTransientSet();
        return scan;
    }

    @Test void swapsKeepBothInputPositionsAndRealFieldEscape() throws Exception {
        MethodNode[] methods = new MethodNode[4];
        int count = 0;
        for (boolean itemFirst : new boolean[] {false, true}) {
            for (boolean localCopy : new boolean[] {false, true}) {
                MethodNode method = swapWitness(itemFirst, localCopy);
                int[] masks = everyInstruction(method);
                for (int i = 0; i < masks.length; i++) {
                    if (method.instructions.get(i) instanceof MethodInsnNode call && call.name.equals("getItem")) {
                        assertEquals(FIELD, masks[i], "SWAP must preserve the acquired item");
                    }
                }
                methods[count++] = method;
            }
        }
        byte[] code = classBytes(methods);
        M3ItemReferenceScan scan = policy();
        scan.sites("prepared", "swap", code);
        assertEquals(4, scan.rows.size());
        for (String[] row : scan.rows) {
            assertEquals("RETAINS", row[9]);
            assertEquals("FIELD", row[10]);
            assertTrue(Integer.parseInt(row[5]) >= 0, "exact BCI");
            assertEquals(Integer.toHexString(siteKey(row[2], row[3], row[4], Integer.parseInt(row[5]))), row[12]);
        }
        Class<?> witness = new ClassLoader(M3EscapePreparedTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(GENERATED.replace('/', '.'), code, 0, code.length); }
        }.define();
        Object table = Class.forName(TABLE.replace('/', '.')).getConstructor().newInstance();
        for (MethodNode method : methods) {
            witness.getField("kept").set(null, null);
            witness.getMethod(method.name, table.getClass()).invoke(null, table);
            assertNotNull(witness.getField("kept").get(null), "the reference really escaped at runtime");
        }
    }

    @Test void everyLegalCopyShapeRetainsTheOldConservativeAliasSet() throws AnalyzerException {
        int[][] cases = {
            {Opcodes.DUP, 1}, {Opcodes.DUP_X1, 1, 1}, {Opcodes.DUP_X2, 1, 1, 1},
            {Opcodes.DUP_X2, 2, 1}, {Opcodes.DUP2, 1, 1}, {Opcodes.DUP2, 2},
            {Opcodes.DUP2_X1, 1, 1, 1}, {Opcodes.DUP2_X1, 1, 2},
            {Opcodes.DUP2_X2, 1, 1, 1, 1}, {Opcodes.DUP2_X2, 2, 1, 1},
            {Opcodes.DUP2_X2, 1, 1, 2}, {Opcodes.DUP2_X2, 2, 2}, {Opcodes.SWAP, 1, 1}
        };
        for (int[] shape : cases) {
            MethodNode method = method("copy" + Arrays.toString(shape), "()V", 0);
            for (int i = 1; i < shape.length; i++) {
                method.instructions.add(new InsnNode(shape[i] == 1 ? Opcodes.ACONST_NULL : Opcodes.LCONST_0));
            }
            method.instructions.add(new InsnNode(shape[0]));
            InsnNode marker = new InsnNode(Opcodes.NOP);
            InsnNode end = new InsnNode(Opcodes.RETURN);
            method.instructions.add(marker);
            method.instructions.add(end);
            Frame<SourceValue> afterCopy = frames(method)[method.instructions.indexOf(marker)];
            method.instructions.remove(marker);
            method.instructions.remove(end);
            for (int i = afterCopy.getStackSize() - 1; i >= 0; i--) {
                method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, "fixture/Fields", "kept",
                        afterCopy.getStack(i).getSize() == 1 ? OBJECT : "J"));
            }
            method.instructions.add(end);
            everyInstruction(method);
        }
    }

    /** Bounded hostile method with multiple producers, backward loop edges and merge-source sets. */
    static MethodNode loopProgram(int sites, long seed) {
        MethodNode method = method("loop" + sites + "_" + seed, "(L" + TABLE + ";IZ)V", sites + 3);
        Random random = new Random(seed);
        for (int i = 0; i < sites; i++) {
            obtain(method, 0, i);
            method.instructions.add(new VarInsnNode(Opcodes.ASTORE, i + 3));
        }
        LabelNode loop = new LabelNode(), done = new LabelNode();
        method.instructions.add(loop);
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new JumpInsnNode(Opcodes.IFLE, done));
        for (int i = 0; i < sites; i++) {
            LabelNode other = new LabelNode(), merge = new LabelNode();
            method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
            method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, other));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3 + random.nextInt(sites)));
            method.instructions.add(new JumpInsnNode(Opcodes.GOTO, merge));
            method.instructions.add(other);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3 + random.nextInt(sites)));
            method.instructions.add(merge);
            method.instructions.add(new VarInsnNode(Opcodes.ASTORE, i + 3));
        }
        method.instructions.add(new LdcInsnNode("SWAP; putstatic kept; @M3BehaviourOnly void leak(Object x) {}"));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new IincInsnNode(1, -1));
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, loop));
        method.instructions.add(done);
        for (int i = 0; i < sites; i++) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, i + 3));
            if ((i & 1) == 0) method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC,
                    GENERATED, "kept", "L" + ITEM + ";"));
            else method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "fixture/Unknown", "consume", "(" + OBJECT + ")V", false));
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    @Test void branchesLoopsAndMultipleProducersAgreeAtEveryInstruction() throws AnalyzerException {
        for (int sites : new int[] {1, 2, 3, 8, 16}) {
            for (long seed = 0; seed < 12; seed++) everyInstruction(loopProgram(sites, seed));
        }
    }

    @Test void independentPreparedCallsReplayInParallelAndSerialOrder() throws Exception {
        MethodNode method = loopProgram(16, 37);
        Frame<SourceValue>[] frames = frames(method);
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        int[] expected = everyInstruction(scan, method);
        for (int width : new int[] {1, 2, 4}) {
            try (var workers = Executors.newFixedThreadPool(width)) {
                var calls = new ArrayList<Callable<int[]>>();
                for (int i = 0; i < 12; i++) calls.add(() -> M3EscapeFlow.prepare(scan, method, frames));
                for (var result : workers.invokeAll(calls)) assertArrayEquals(expected, result.get());
            }
        }
        assertArrayEquals(expected, M3EscapeFlow.prepare(scan, method, frames));
    }

    @Test void existingCompiledFixturesPreserveAllBitsDescriptorsAndConcatRules() throws Exception {
        M3ItemReferenceScan scan = policy();
        for (String name : new String[] {"FakeItem", "FakeTable", "ItemUsers"}) {
            ClassNode node = new ClassNode();
            new ClassReader(bytes(FIXTURE + name)).accept(node, ClassReader.SKIP_FRAMES);
            for (MethodNode method : node.methods) {
                if (method.instructions.size() != 0) everyInstruction(scan, method);
            }
        }
    }

    @Test void oneProducerAccumulatesEveryConsumerBitIncludingWideArgumentDescriptors() throws AnalyzerException {
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        scan.retainingMethods.add("fixture/Policy.retain(" + OBJECT + ")V");
        MethodNode method = method("allMasks", "()" + OBJECT, 1);
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, "fixture/Fields", "kept", OBJECT));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.AASTORE));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add",
                "(" + OBJECT + ")Z", true));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InvokeDynamicInsnNode("capture", "(" + OBJECT + ")" + OBJECT,
                new Handle(Opcodes.H_INVOKESTATIC, "fixture/Bootstrap", "capture", "()V", false)));
        method.instructions.add(new InsnNode(Opcodes.POP));
        LabelNode same = new LabelNode();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, same));
        method.instructions.add(same);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "identityHashCode",
                "(" + OBJECT + ")I", false));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.MONITORENTER));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.MONITOREXIT));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "fixture/Policy", "retain",
                "(" + OBJECT + ")V", false));
        method.instructions.add(new InsnNode(Opcodes.LCONST_0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.DCONST_0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "fixture/Unknown", "wide",
                "(J" + OBJECT + "D)V", false));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        assertEquals(RETAINING | ARG_UNKNOWN, everyInstruction(scan, method)[0]);
    }

    @Test void calleePolicyIsExactAndPreparedAnswersDoNotChangeWithLaterPolicy() throws AnalyzerException {
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        MethodNode method = method("policy", "()V", 0);
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "fixture/Policy", "use",
                "(" + OBJECT + ")V", false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        String exact = "fixture/Policy.use(" + OBJECT + ")V";
        scan.behaviourMethods.add("fixture/Policy.use(Ljava/lang/String;)V");
        scan.retainingMethods.add("fixture/Policy.use(I)V");
        int[] before = everyInstruction(scan, method);
        assertEquals(ARG_UNKNOWN, before[0]);
        scan.behaviourMethods.add(exact);
        int[] after = everyInstruction(scan, method);
        assertEquals(0, after[0]);
        assertEquals(ARG_UNKNOWN, before[0], "no stale shared mutable answer owner");
        scan.retainingMethods.add(exact);
        assertEquals(RETAINS_CALLEE, everyInstruction(scan, method)[0], "retain precedence is preserved");
    }

    @Test void unreachableInstructionsAndEmptyProgramsHaveNoInventedEscapes() throws AnalyzerException {
        MethodNode method = method("dead", "()V", 0);
        LabelNode done = new LabelNode();
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, done));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, "fixture/Fields", "kept", OBJECT));
        method.instructions.add(done);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        int[] masks = everyInstruction(method);
        assertNull(frames(method)[1]);
        assertEquals(0, masks[1]);
        MethodNode empty = method("empty", "()V", 0);
        @SuppressWarnings("unchecked") Frame<SourceValue>[] noFrames = (Frame<SourceValue>[]) new Frame<?>[0];
        assertArrayEquals(new int[0], M3EscapeFlow.prepare(new M3ItemReferenceScan(), empty, noFrames));
    }

    @Test void exactPreparationBudgetsRefuseWithoutPublishingPartialMasks() throws AnalyzerException {
        MethodNode method = method("budget", "()V", 1);
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, "fixture/Fields", "kept", OBJECT));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        Frame<SourceValue>[] frames = frames(method);
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        // Two copy edges and one direct-use source; 3 int lanes + offsets extra + byte queue flags.
        long bytes = 13L * method.instructions.size() + Integer.BYTES + 2L * Integer.BYTES;
        assertNotNull(M3EscapeFlow.prepare(scan, method, frames, 2, bytes, 3));
        assertNull(M3EscapeFlow.prepare(scan, method, frames, 1, bytes, 3));
        assertNull(M3EscapeFlow.prepare(scan, method, frames, 2, bytes - 1, 3));
        assertNull(M3EscapeFlow.prepare(scan, method, frames, 2, bytes, 2));
        assertNull(M3EscapeFlow.prepare(scan, method, frames, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> M3EscapeFlow.prepare(scan, method, frames, -1, bytes, 3));
        assertThrows(IllegalArgumentException.class, () -> M3EscapeFlow.prepare(scan, method, frames, 2, -1, 3));
        assertThrows(IllegalArgumentException.class, () -> M3EscapeFlow.prepare(scan, method, frames, 2, bytes, -1));
        assertEquals(FIELD, M3EscapeFlow.escape(scan, method, frames, method.instructions.getFirst()),
                "the retained forward fallback has the complete answer");
        everyInstruction(method);
    }

    @Test void analyzerAndFrameBudgetFailuresRemainUnknown() throws Exception {
        MethodNode invalid = method("invalidSwap", "()V", 0);
        invalid.instructions.add(new TypeInsnNode(Opcodes.NEW, ITEM));
        invalid.instructions.add(new InsnNode(Opcodes.LCONST_0));
        invalid.instructions.add(new InsnNode(Opcodes.SWAP));
        invalid.instructions.add(new InsnNode(Opcodes.RETURN));
        assertThrows(AnalyzerException.class, () -> frames(invalid), "SWAP cannot exchange a category-two value");
        M3ItemReferenceScan scan = policy();
        scan.sites("prepared", "invalid", classBytes(invalid));
        assertEquals(1, scan.rows.size());
        assertEquals("UNKNOWN", scan.rows.getFirst()[9]);
        assertEquals("ANALYSIS_FAILED", scan.rows.getFirst()[10]);

        MethodNode over = method("frameBudget", "()V", 65_535);
        over.maxStack = 65_535;
        over.instructions.add(new TypeInsnNode(Opcodes.NEW, ITEM));
        for (int i = 0; i < 40; i++) over.instructions.add(new InsnNode(Opcodes.NOP));
        over.instructions.add(new InsnNode(Opcodes.POP));
        over.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, GENERATED, null, "java/lang/Object", null);
        over.accept(writer);
        writer.visitEnd();
        scan = policy();
        scan.sites("prepared", "frame-budget", writer.toByteArray());
        assertEquals(1, scan.methodsOverBudget);
        assertEquals("UNKNOWN", scan.rows.getFirst()[9]);
        assertEquals("ANALYSIS_FAILED", scan.rows.getFirst()[10]);
    }
}
