// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.objectweb.asm.Type;

/**
 * One-class scanner: ldc constants, catalogued call-site operands (via ASM
 * {@link SourceInterpreter} provenance), indy concat recipes, string switches, annotation
 * strings, enum names and ConstantValue strings. Every count is exact over the class file.
 */
final class M3ClassScan {
    /** Analyzer frame budget (instructions x (locals + stack)); larger methods are counted. */
    private static final long MAX_FRAME_CELLS = 4_000_000L;
    private static final String CONCAT_FACTORY = "java/lang/invoke/StringConcatFactory";
    private static final String SWITCH_BOOTSTRAPS = "java/lang/runtime/SwitchBootstraps";

    private final M3InventoryState state;
    private final M3SiteRecorder recorder;

    M3ClassScan(M3InventoryState state, boolean regexOnly) {
        this.state = state;
        this.recorder = new M3SiteRecorder(state, regexOnly);
    }

    void scan(byte[] bytes) {
        state.classSerial++;
        ClassReader reader = new ClassReader(bytes);
        state.inc("all", "classes");
        constantPool(reader);
        ClassNode cn = new ClassNode();
        reader.accept(cn, ClassReader.SKIP_FRAMES);
        annotations(cn);
        fields(cn);
        cn.methods.forEach(m -> method(cn.name, m));
    }

    private void constantPool(ClassReader reader) {
        long strings = 0;
        for (int i = 1; i < reader.getItemCount(); i++) {
            int offset = reader.getItem(i);
            if (offset > 0 && reader.readByte(offset - 1) == 8) {
                strings++;
            }
        }
        state.add("a", "constant_pool_string_entries", strings);
    }

    private void annotations(ClassNode cn) {
        List<AnnotationNode> all = new ArrayList<>();
        addAll(all, cn.visibleAnnotations, cn.invisibleAnnotations);
        addAll(all, cn.visibleTypeAnnotations, cn.invisibleTypeAnnotations);
        for (FieldNode f : cn.fields) {
            addAll(all, f.visibleAnnotations, f.invisibleAnnotations);
            addAll(all, f.visibleTypeAnnotations, f.invisibleTypeAnnotations);
        }
        for (MethodNode m : cn.methods) {
            addAll(all, m.visibleAnnotations, m.invisibleAnnotations);
            addAll(all, m.visibleTypeAnnotations, m.invisibleTypeAnnotations);
            params(all, m.visibleParameterAnnotations);
            params(all, m.invisibleParameterAnnotations);
            if (m.annotationDefault != null) {
                annotationValue(m.annotationDefault);
            }
        }
        state.add("f", "annotations", all.size());
        all.forEach(a -> {
            if (a.values != null) {
                for (int i = 1; i < a.values.size(); i += 2) {
                    annotationValue(a.values.get(i));
                }
            }
        });
    }

    private static void addAll(List<AnnotationNode> out, List<? extends AnnotationNode> a,
            List<? extends AnnotationNode> b) {
        if (a != null) {
            out.addAll(a);
        }
        if (b != null) {
            out.addAll(b);
        }
    }

    private static void params(List<AnnotationNode> out, List<AnnotationNode>[] lists) {
        if (lists != null) {
            for (List<AnnotationNode> list : lists) {
                addAll(out, list, null);
            }
        }
    }

    private void annotationValue(Object value) {
        if (value instanceof String s) {
            state.inc("f", "annotation_string_values");
            state.add("f", "annotation_string_units", s.length());
        } else if (value instanceof String[] enumValue) {
            state.inc("f", "annotation_enum_values");
            state.add("f", "annotation_enum_name_units", enumValue[1].length());
        } else if (value instanceof Type) {
            state.inc("f", "annotation_class_values");
        } else if (value instanceof AnnotationNode nested) {
            state.inc("f", "annotations_nested");
            if (nested.values != null) {
                for (int i = 1; i < nested.values.size(); i += 2) {
                    annotationValue(nested.values.get(i));
                }
            }
        } else if (value instanceof List<?> list) {
            list.forEach(this::annotationValue);
        }
    }

    private void fields(ClassNode cn) {
        boolean isEnum = (cn.access & Opcodes.ACC_ENUM) != 0;
        for (FieldNode f : cn.fields) {
            if (isEnum && (f.access & Opcodes.ACC_ENUM) != 0) {
                state.inc("f", "enum_constant_names");
                state.add("f", "enum_constant_name_units", f.name.length());
            }
            if (f.value instanceof String s) {
                state.inc("g", "field_constant_value_strings");
                state.add("g", "field_constant_value_units", s.length());
            }
        }
        if (isEnum) {
            state.inc("f", "enum_classes");
        }
    }

    private void method(String owner, MethodNode m) {
        List<AbstractInsnNode> sites = new ArrayList<>();
        Set<AbstractInsnNode> switchLabels = new HashSet<>();
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                ldcString(s);
            } else if (insn instanceof InvokeDynamicInsnNode indy) {
                indy(indy);
            } else if (insn instanceof MethodInsnNode call) {
                if (M3CallSiteCatalog.lookup(call.owner, call.name, call.desc).isPresent()
                        || isStringEquals(call)) {
                    sites.add(call);
                }
                if (isHashCode(call)) {
                    stringSwitch(call, switchLabels);
                }
            }
        }
        if (!sites.isEmpty()) {
            operands(owner, m, sites, switchLabels);
        }
    }

    private void ldcString(String s) {
        state.inc("a", "ldc_string_sites");
        state.add("a", "ldc_string_units", s.length());
        state.inc("a", M3InventoryState.bucket(s.length()));
        if (state.seen(state.ldc, s)) {
            state.inc("a", "ldc_class_string_pairs");
        }
    }

    private void indy(InvokeDynamicInsnNode indy) {
        Handle bsm = indy.bsm;
        if (CONCAT_FACTORY.equals(bsm.getOwner())) {
            if ("makeConcatWithConstants".equals(bsm.getName()) && indy.bsmArgs.length > 0
                    && indy.bsmArgs[0] instanceof String recipe) {
                concat(recipe, indy);
            } else {
                state.inc("d", "indy_make_concat_sites");
            }
        } else if (SWITCH_BOOTSTRAPS.equals(bsm.getOwner())) {
            state.inc("e", "indy_switch_bootstrap_sites");
            for (Object arg : indy.bsmArgs) {
                if (arg instanceof String) {
                    state.inc("e", "indy_switch_string_labels");
                }
            }
        }
    }

    private void concat(String recipe, InvokeDynamicInsnNode indy) {
        long args = recipe.chars().filter(c -> c == 1).count();
        long tags = recipe.chars().filter(c -> c == 2).count();
        state.inc("d", "indy_concat_sites");
        state.add("d", "indy_concat_dynamic_args", args);
        state.add("d", "indy_concat_constant_tags", tags);
        state.add("d", "indy_concat_recipe_literal_units", recipe.length() - args - tags);
        state.inc("d", "recipe_" + M3InventoryState.bucket((int) (recipe.length() - args - tags)));
        for (int i = 1; i < indy.bsmArgs.length; i++) {
            if (indy.bsmArgs[i] instanceof String s) {
                state.add("d", "indy_concat_constant_tag_units", s.length());
            }
        }
        if (state.seen(state.concatRecipes, recipe)) {
            state.inc("d", "indy_concat_class_recipe_pairs");
        }
    }

    static boolean isStringEquals(MethodInsnNode call) {
        return "java/lang/String".equals(call.owner) && "equals".equals(call.name)
                && "(Ljava/lang/Object;)Z".equals(call.desc);
    }

    private static boolean isHashCode(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKEVIRTUAL && "java/lang/String".equals(call.owner)
                && "hashCode".equals(call.name) && "()I".equals(call.desc);
    }

    /** javac/ECJ string switch: {@code aload t; String.hashCode(); lookupswitch|tableswitch}. */
    private void stringSwitch(MethodInsnNode hash, Set<AbstractInsnNode> labels) {
        AbstractInsnNode sw = realNext(hash);
        AbstractInsnNode load = realPrevious(hash);
        if (!(sw instanceof LookupSwitchInsnNode) && !(sw instanceof TableSwitchInsnNode)) {
            return;
        }
        state.inc("e", "string_switch_sites");
        int keys = sw instanceof LookupSwitchInsnNode l ? l.keys.size()
                : ((TableSwitchInsnNode) sw).max - ((TableSwitchInsnNode) sw).min + 1;
        state.add("e", "string_switch_hash_keys", keys);
        if (!(load instanceof VarInsnNode var)) {
            return;
        }
        for (AbstractInsnNode i = sw.getNext(); i != null; i = i.getNext()) {
            if (i instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD && v.var == var.var
                    && realNext(v) instanceof LdcInsnNode ldc && ldc.cst instanceof String
                    && realNext(ldc) instanceof MethodInsnNode eq && isStringEquals(eq)) {
                if (labels.add(eq)) {
                    state.inc("e", "string_switch_case_labels");
                }
            }
        }
    }

    static AbstractInsnNode realNext(AbstractInsnNode insn) {
        AbstractInsnNode i = insn.getNext();
        while (i != null && i.getOpcode() < 0) {
            i = i.getNext();
        }
        return i;
    }

    private static AbstractInsnNode realPrevious(AbstractInsnNode insn) {
        AbstractInsnNode i = insn.getPrevious();
        while (i != null && i.getOpcode() < 0) {
            i = i.getPrevious();
        }
        return i;
    }

    private void operands(String owner, MethodNode m, List<AbstractInsnNode> sites,
            Set<AbstractInsnNode> switchLabels) {
        long cells = (long) m.instructions.size() * (m.maxLocals + m.maxStack + 1);
        Frame<SourceValue>[] frames = null;
        if (cells <= MAX_FRAME_CELLS) {
            try {
                frames = new Analyzer<>(new SourceInterpreter()).analyze(owner, m);
            } catch (AnalyzerException | RuntimeException failed) {
                state.inc("all", "methods_analysis_failed");
            }
        } else {
            state.inc("all", "methods_over_frame_budget");
        }
        for (AbstractInsnNode site : sites) {
            MethodInsnNode call = (MethodInsnNode) site;
            Frame<SourceValue> frame = frames == null ? null : frames[m.instructions.indexOf(call)];
            if (isStringEquals(call)) {
                if (!switchLabels.contains(call)) {
                    equalsSite(call, frame);
                }
                continue;
            }
            M3CallSiteCatalog.Site s = M3CallSiteCatalog.lookup(call.owner, call.name, call.desc).orElseThrow();
            recorder.record(s, frame == null ? Optional.empty() : operand(call, frame, s.arg()),
                    frame == null, frame == null ? -1 : intOperand(call, frame, 1));
        }
    }

    private void equalsSite(MethodInsnNode call, Frame<SourceValue> frame) {
        state.inc("g", "string_equals_sites");
        if (frame == null) {
            return;
        }
        operand(call, frame, -1).ifPresent(s -> {
            state.inc("g", "string_equals_receiver_const");
            state.add("g", "string_equals_receiver_const_units", s.length());
        });
        operand(call, frame, 0).ifPresent(s -> {
            state.inc("g", "string_equals_arg_const");
            state.add("g", "string_equals_arg_const_units", s.length());
        });
    }

    /** The single-ldc String operand at {@code arg} (-1 = receiver), if provably constant. */
    static Optional<String> operand(MethodInsnNode call, Frame<SourceValue> frame, int arg) {
        SourceValue v = stackValue(call, frame, arg);
        if (v == null || v.insns.size() != 1) {
            return Optional.empty();
        }
        AbstractInsnNode source = v.insns.iterator().next();
        return source instanceof LdcInsnNode ldc && ldc.cst instanceof String s
                ? Optional.of(s) : Optional.empty();
    }

    /** The constant int operand at {@code arg}, or -1 when absent or not constant. */
    static int intOperand(MethodInsnNode call, Frame<SourceValue> frame, int arg) {
        Type[] types = Type.getArgumentTypes(call.desc);
        if (arg >= types.length || types[arg].getSort() != Type.INT) {
            return -1;
        }
        SourceValue v = stackValue(call, frame, arg);
        if (v == null || v.insns.size() != 1) {
            return -1;
        }
        AbstractInsnNode source = v.insns.iterator().next();
        int op = source.getOpcode();
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
            return op - Opcodes.ICONST_0;
        }
        if (source instanceof IntInsnNode in && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
            return in.operand;
        }
        return source instanceof LdcInsnNode ldc && ldc.cst instanceof Integer i ? i : -1;
    }

    private static SourceValue stackValue(MethodInsnNode call, Frame<SourceValue> frame, int arg) {
        if (frame == null) {
            return null;
        }
        int nargs = Type.getArgumentTypes(call.desc).length;
        int index = frame.getStackSize() - nargs + arg;
        return index < 0 || index >= frame.getStackSize() ? null : frame.getStack(index);
    }
}
