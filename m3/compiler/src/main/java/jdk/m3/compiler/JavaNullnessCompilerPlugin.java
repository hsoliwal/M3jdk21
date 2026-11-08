// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.lang.model.element.Element;

/**
 * Opt-in read-only Java21 javac plugin for compiler-time nullness candidate facts.
 * Uses javac's real attributed trees; never rewrites Java syntax or bytecode.
 *
 * <p>Reports pure parameter-local null guards only. No inter-statement propagation,
 * heap/field stability assumptions, global compiler state, GC/JNI authority or
 * Kotlin compiler dependency. M3JDK 21 product integration is a separate gate.
 * The named command-line opt-in is {@code -Xplugin:M3NullnessFacts}.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/982689e16763e54b01632407d64846fbea5719b6/synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/M3JavaNullnessCompilerPlugin.java blob e06512091f03; recipe m3/compiler/nullness-facts/recipe/relocate.py v1
public final class JavaNullnessCompilerPlugin implements Plugin {
    private static final String CONTEXT = sha256("M3-JAVAC-PLUGIN-1\0" + Runtime.version());
    @Override public String getName() { return "M3NullnessFacts"; }

    @Override public void init(JavacTask task, String... args) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(args, "args");
        if (args.length != 0) throw new IllegalArgumentException("M3NullnessFacts takes no arguments");
        Trees trees = Trees.instance(task);
        task.addTaskListener(new TaskListener() {
            // Identity-keyed primitive-slot hash table; no per-file Map.Entry wrappers.
            private CompilationUnitTree[] reported = new CompilationUnitTree[16];
            private int count;
            @Override public void started(TaskEvent event) { /* No source mutation. */ }
            @Override public void finished(TaskEvent event) {
                if (event.getKind() != TaskEvent.Kind.ANALYZE) return;
                CompilationUnitTree unit = event.getCompilationUnit();
                if (unit == null || !admit(unit)) return;
                analyze(unit, trees);
            }
            private boolean admit(CompilationUnitTree unit) {
                if (count >= 1_000_000) return false; // Explicit bounded review, no compile failure.
                if ((count + 1L) * 2 >= reported.length) grow();
                int mask = reported.length - 1;
                int slot = System.identityHashCode(unit) & mask;
                while (reported[slot] != null) {
                    if (reported[slot] == unit) return false;
                    slot = (slot + 1) & mask;
                }
                reported[slot] = unit;
                count++;
                return true;
            }
            private void grow() {
                CompilationUnitTree[] prior = reported;
                reported = new CompilationUnitTree[prior.length << 1];
                int mask = reported.length - 1;
                for (CompilationUnitTree existing : prior) {
                    if (existing == null) continue;
                    int slot = System.identityHashCode(existing) & mask;
                    while (reported[slot] != null) slot = (slot + 1) & mask;
                    reported[slot] = existing;
                }
            }
        });
    }

    private static void analyze(CompilationUnitTree unit, Trees trees) {
        String source = sourceText(unit);
        if (source == null) return;
        String sourceDigest = sha256(source);
        String uri = unit.getSourceFile().toUri().toASCIIString();
        new TreePathScanner<Void, Void>() {
            private Element[] params = new Element[0];
            private String[] names = new String[0];
            private JavaNullnessFlow.Facts before;
            private String methodName;

            @Override public Void visitMethod(MethodTree method, Void ignored) {
                Element[] savedParams = params;
                String[] savedNames = names;
                JavaNullnessFlow.Facts savedBefore = before;
                String savedMethod = methodName;
                try {
                    int size = method.getParameters().size();
                    if (size == 0 || size > 1024) {
                        before = null;
                        params = new Element[0];
                        names = new String[0];
                        return super.visitMethod(method, ignored);
                    }
                    Element[] symbols = new Element[size];
                    String[] localNames = new String[size];
                    for (int i = 0; i < size; i++) {
                        VariableTree parameter = method.getParameters().get(i);
                        Element symbol = trees.getElement(new TreePath(getCurrentPath(), parameter));
                        if (symbol == null) {
                            before = null;
                            return super.visitMethod(method, ignored);
                        }
                        for (int prior = 0; prior < i; prior++) {
                            if (symbols[prior] == symbol) {
                                before = null;
                                return super.visitMethod(method, ignored);
                            }
                        }
                        symbols[i] = symbol;
                        localNames[i] = parameter.getName().toString();
                    }
                    params = symbols;
                    names = localNames;
                    methodName = method.getName().toString();
                    long offset = trees.getSourcePositions().getStartPosition(unit, method);
                    var epoch = new JavaNullnessFlow.Epoch(
                            sourceDigest, CONTEXT,
                            sha256(uri + "\0" + methodName + "\0" + offset),
                            0L);
                    before = JavaNullnessFlow.start(epoch, size);
                    return super.visitMethod(method, ignored);
                } finally {
                    before = savedBefore;
                    methodName = savedMethod;
                    names = savedNames;
                    params = savedParams;
                }
            }

            @Override public Void visitIf(IfTree node, Void ignored) {
                if (before != null) {
                    var condition = new TreePath(getCurrentPath(), node.getCondition());
                    var positive = JavaNullnessJavacBridge.refine(
                            before, condition, true, trees, this::slotOf);
                    var negative = JavaNullnessJavacBridge.refine(
                            before, condition, false, trees, this::slotOf);
                    if (positive != before || negative != before) {
                        long offset = trees.getSourcePositions().getStartPosition(unit, node);
                        // Evidence only. A hash and nullness edge are NOT compiler certificates.
                        System.err.println("M3_NULLNESS_FACT\t" + uri + "\t" + offset
                                + "\t" + methodName + "\t"
                                + render(positive, names) + "\t" + render(negative, names));
                    }
                }
                return super.visitIf(node, ignored);
            }
            private int slotOf(Element symbol) {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] == symbol) return i;
                }
                return -1;
            }
        }.scan(unit, null);
    }

    private static String render(JavaNullnessFlow.Facts facts, String[] names) {
        if (!facts.reachable()) return "UNREACHABLE";
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i != 0) output.append(',');
            output.append(i).append(':').append(facts.nullness(i).name());
        }
        return output.toString();
    }

    private static String sourceText(CompilationUnitTree source) {
        try {
            return source.getSourceFile().getCharContent(true).toString();
        } catch (java.io.IOException unavailable) {
            return null; // No facts without stable compiler-observed source.
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Encode UTF-16 code units losslessly: replacement UTF-8 encoding can
            // collapse distinct malformed source inputs to the same bytes.
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                digest.update((byte) (c >>> 8));
                digest.update((byte) c);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
