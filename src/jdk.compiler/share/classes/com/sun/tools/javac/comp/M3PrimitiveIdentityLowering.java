/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */
package com.sun.tools.javac.comp;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Opt-in javac21 launcher for the matched M3PrimitiveIdentity v1 kernel.
 * Uses existing attributed javac trees and operator resolution, not a second AST.
 * Standard javac, reference comparisons, numeric promotions and raw FP keys are untouched.
 */
public final class M3PrimitiveIdentityLowering {
    private M3PrimitiveIdentityLowering() { }

    public static void main(String[] args) {
        Context context = new Context();
        JavacFileManager.preRegister(context);
        int[] changes = install(context);
        Main.Result result = new Main("m3-identity-javac").compile(args, context);
        System.err.println("m3.identity.lowered=" + changes[0]);
        if (!result.isOK()) System.exit(result.exitCode);
    }

    public static int[] install(Context context) {
        int[] changes = {0};
        Set<JCTree> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        MultiTaskListener.instance(context).add(new TaskListener() {
            @Override public void finished(TaskEvent event) {
                if (event.getKind() != TaskEvent.Kind.ANALYZE || event.getTypeElement() == null) return;
                JCTree root = JavacTrees.instance(context).getTree(event.getTypeElement());
                if (root != null && visited.add(root)) new Lowering(context, changes).translate(root);
            }
        });
        return changes;
    }

    private static final class Lowering extends TreeTranslator {
        private final TreeMaker make;
        private final Operators operators;
        private final Symtab syms;
        private final int[] changes;
        Lowering(Context context, int[] changes) {
            this.make = TreeMaker.instance(context); this.operators = Operators.instance(context);
            this.syms = Symtab.instance(context); this.changes = changes;
        }

        private TypeTag admitted(JCMethodInvocation call, String prefix, int arity) {
            Symbol symbol = TreeInfo.symbol(call.meth);
            if (!(symbol instanceof Symbol.MethodSymbol method) || call.args.size() != arity
                    || (method.flags() & Flags.STATIC) == 0
                    || !method.owner.getQualifiedName().contentEquals("jdk.internal.mindex.M3PrimitiveIdentity")
                    || method.owner.packge().modle != syms.java_base
                    || !(call.meth instanceof JCIdent || call.meth instanceof JCFieldAccess
                        && TreeInfo.symbol(((JCFieldAccess) call.meth).selected) instanceof Symbol.ClassSymbol))
                return null;
            String name = method.name.toString();
            if (!name.startsWith(prefix)) return null;
            TypeTag tag = switch (name.substring(prefix.length())) {
                case "Boolean" -> TypeTag.BOOLEAN; case "Byte" -> TypeTag.BYTE;
                case "Short" -> TypeTag.SHORT; case "Char" -> TypeTag.CHAR;
                case "Int" -> TypeTag.INT; case "Long" -> TypeTag.LONG;
                case "Float" -> TypeTag.FLOAT; case "Double" -> TypeTag.DOUBLE;
                default -> null;
            };
            if (tag == null || method.type.getParameterTypes().size() != arity) return null;
            if (!method.getReturnType().hasTag(prefix.equals("of") ? TypeTag.LONG : TypeTag.BOOLEAN))
                return null;
            for (var parameter : method.type.getParameterTypes()) if (!parameter.hasTag(tag)) return null;
            for (JCExpression argument : call.args) if (argument.type == null || !argument.type.hasTag(tag))
                return null; // Refuse boxing/unboxing and implicit widening instead of guessing.
            return tag;
        }

        private JCExpression comparison(JCTree original, JCTree.Tag tag, JCExpression a, JCExpression b) {
            JCBinary binary = make.at(original.pos).Binary(tag, a, b);
            binary.operator = operators.resolveBinary(original, tag, a.type, b.type);
            binary.type = syms.booleanType; changes[0]++; return binary;
        }

        @Override public void visitBinary(JCBinary tree) {
            // Injective integral/boolean encoders only. Never equate raw float/double keys with ==.
            if ((tree.hasTag(JCTree.Tag.EQ) || tree.hasTag(JCTree.Tag.NE))
                    && TreeInfo.skipParens(tree.lhs) instanceof JCMethodInvocation left
                    && TreeInfo.skipParens(tree.rhs) instanceof JCMethodInvocation right) {
                TypeTag tag = admitted(left, "of", 1);
                if (tag != null && tag != TypeTag.FLOAT && tag != TypeTag.DOUBLE
                        && admitted(right, "of", 1) == tag) {
                    result = comparison(tree, tree.getTag(), translate(left.args.head), translate(right.args.head));
                    return;
                }
            }
            super.visitBinary(tree);
        }

        @Override public void visitApply(JCMethodInvocation tree) {
            TypeTag tag = admitted(tree, "same", 2);
            if (tag != null) {
                result = comparison(tree, JCTree.Tag.EQ, translate(tree.args.head), translate(tree.args.tail.head));
                return;
            }
            super.visitApply(tree);
        }
    }
}
