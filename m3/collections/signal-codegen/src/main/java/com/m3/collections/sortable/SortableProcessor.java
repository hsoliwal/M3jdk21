// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Java 21 source-time structural signal manufacturer; never mutates the annotated type.
 * Emits a separate source companion with fixed primitive feature lanes, avoiding reflection,
 * retained entry objects, runtime AST scans or Java object addresses in JNI.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes("com.m3.collections.sortable.Sortable")
public final class SortableProcessor extends AbstractProcessor {
    private static final Comparator<Long> UNSIGNED = Long::compareUnsigned;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        if (round.processingOver()) return false;
        List<TypeElement> types = new ArrayList<>();
        for (Element annotated : round.getElementsAnnotatedWith(Sortable.class)) {
            if (!(annotated instanceof TypeElement type)) {
                error(annotated, "@Sortable requires a class or record");
            } else {
                types.add(type);
            }
        }
        types.sort(Comparator.comparing(t -> t.getQualifiedName().toString()));
        for (TypeElement type : types) {
            generate(type);
        }
        return true;
    }

    private void generate(TypeElement type) {
        if (type.getEnclosingElement().getKind() != ElementKind.PACKAGE
                || (type.getKind() != ElementKind.CLASS && type.getKind() != ElementKind.RECORD)) {
            error(type, "@Sortable supports top-level classes and records only");
            return;
        }
        String accessor = accessor(type);
        if (accessor == null) return;

        String pkg = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        String name = type.getSimpleName().toString();
        @SuppressWarnings("unchecked")
        TreeSet<Long>[] lanes = (TreeSet<Long>[]) new TreeSet<?>[StructuralSignals.DIMENSIONS];
        for (int i = 0; i < lanes.length; i++) lanes[i] = new TreeSet<>(UNSIGNED);
        int[] facts = new int[M3CompilerFacts.COUNT];
        long validFacts = (1L << M3CompilerFacts.MODEL_FACTS) - 1L;
        facts[M3CompilerFacts.FIELDS] =
                ElementFilter.fieldsIn(type.getEnclosedElements()).size();
        facts[M3CompilerFacts.METHODS] =
                ElementFilter.methodsIn(type.getEnclosedElements()).size();
        facts[M3CompilerFacts.CONSTRUCTORS] =
                ElementFilter.constructorsIn(type.getEnclosedElements()).size();
        facts[M3CompilerFacts.IMPLEMENTED_INTERFACES] = type.getInterfaces().size();
        add(lanes[0], "package:" + pkg);
        add(lanes[1], "class:" + type.getQualifiedName());
        dependencies(type.getSuperclass(), lanes[4]);
        for (TypeMirror iface : type.getInterfaces()) dependencies(iface, lanes[4]);
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            add(lanes[2], "field:" + field.getSimpleName() + ":" + field.asType());
            dependencies(field.asType(), lanes[4]);
        }
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            StringBuilder signature = new StringBuilder("method:")
                    .append(method.getSimpleName()).append('(');
            facts[M3CompilerFacts.METHOD_PARAMETERS] += method.getParameters().size();
            facts[M3CompilerFacts.DECLARED_THROWS] += method.getThrownTypes().size();
            for (VariableElement p : method.getParameters()) {
                signature.append(p.asType()).append(';');
                dependencies(p.asType(), lanes[4]);
            }
            signature.append("):").append(method.getReturnType());
            add(lanes[3], signature.toString());
            dependencies(method.getReturnType(), lanes[4]);
            for (TypeMirror ex : method.getThrownTypes()) dependencies(ex, lanes[4]);
        }
        facts[M3CompilerFacts.DECLARED_TYPE_TOKENS] = lanes[4].size();
        // Source-path syntax counts are admitted only when Trees supplies the actual
        // annotated root. Any nested/local type is separately owned, not included
        // in this type's syntactic counts. No call-graph completeness is inferred.
        TreeSet<Long> localCallTokens = new TreeSet<>(UNSIGNED);
        try {
            Trees trees = Trees.instance(processingEnv);
            TreePath path = trees.getPath(type);
            if (path != null) {
                new TreePathScanner<Void, Void>() {
                    private int classDepth;
                    @Override public Void visitClass(ClassTree tree, Void unused) {
                        classDepth++;
                        try {
                            return super.visitClass(tree, unused);
                        } finally {
                            classDepth--;
                        }
                    }
                    @Override public Void visitMethodInvocation(
                            MethodInvocationTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.METHOD_INVOCATIONS]++;
                        Element target = trees.getElement(getCurrentPath());
                        if (target instanceof ExecutableElement method) {
                            Element owner = method.getEnclosingElement();
                            StringBuilder signature = new StringBuilder("call:")
                                    .append(owner).append('#').append(method.getSimpleName())
                                    .append('(');
                            for (VariableElement p : method.getParameters()) {
                                signature.append(p.asType()).append(';');
                            }
                            String token = signature.append(')').toString();
                            // Preserve all original structural tokens (including nested
                            // types). The new type-local fact excludes nested declarations.
                            add(lanes[5], token);
                            if (classDepth == 1) add(localCallTokens, token);
                        }
                        return super.visitMethodInvocation(tree, unused);
                    }
                    @Override public Void visitIf(IfTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.IF_BRANCHES]++;
                        return super.visitIf(tree, unused);
                    }
                    @Override public Void visitSwitch(SwitchTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.SWITCHES]++;
                        return super.visitSwitch(tree, unused);
                    }
                    @Override public Void visitSwitchExpression(
                            SwitchExpressionTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.SWITCHES]++;
                        return super.visitSwitchExpression(tree, unused);
                    }
                    @Override public Void visitForLoop(ForLoopTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.LOOPS]++;
                        return super.visitForLoop(tree, unused);
                    }
                    @Override public Void visitEnhancedForLoop(
                            EnhancedForLoopTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.LOOPS]++;
                        return super.visitEnhancedForLoop(tree, unused);
                    }
                    @Override public Void visitWhileLoop(WhileLoopTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.LOOPS]++;
                        return super.visitWhileLoop(tree, unused);
                    }
                    @Override public Void visitDoWhileLoop(DoWhileLoopTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.LOOPS]++;
                        return super.visitDoWhileLoop(tree, unused);
                    }
                    @Override public Void visitConditionalExpression(
                            ConditionalExpressionTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.CONDITIONAL_EXPRESSIONS]++;
                        return super.visitConditionalExpression(tree, unused);
                    }
                    @Override public Void visitLambdaExpression(
                            LambdaExpressionTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.LAMBDAS]++;
                        return super.visitLambdaExpression(tree, unused);
                    }
                    @Override public Void visitNewClass(NewClassTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.NEW_EXPRESSIONS]++;
                        return super.visitNewClass(tree, unused);
                    }
                    @Override public Void visitNewArray(NewArrayTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.NEW_EXPRESSIONS]++;
                        return super.visitNewArray(tree, unused);
                    }
                    @Override public Void visitSynchronized(
                            SynchronizedTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.SYNCHRONIZED_BLOCKS]++;
                        return super.visitSynchronized(tree, unused);
                    }
                    @Override public Void visitThrow(ThrowTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.THROW_STATEMENTS]++;
                        return super.visitThrow(tree, unused);
                    }
                    @Override public Void visitTry(TryTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.TRY_BLOCKS]++;
                        return super.visitTry(tree, unused);
                    }
                    @Override public Void visitCatch(CatchTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.CATCH_BLOCKS]++;
                        return super.visitCatch(tree, unused);
                    }
                    @Override public Void visitReturn(ReturnTree tree, Void unused) {
                        if (classDepth == 1) facts[M3CompilerFacts.RETURN_STATEMENTS]++;
                        return super.visitReturn(tree, unused);
                    }
                }.scan(path, null);
                facts[M3CompilerFacts.DISTINCT_RESOLVED_CALLEE_TOKENS] = localCallTokens.size();
                long modelKnown = (1L << M3CompilerFacts.MODEL_FACTS) - 1L;
                validFacts |= ((1L << M3CompilerFacts.AST_FACTS_END) - 1L) & ~modelKnown;
            }
        } catch (IllegalArgumentException unavailable) {
            // Only model-level coordinates are known without Trees.
        }

        String fqGenerated = (pkg.isEmpty() ? "" : pkg + ".") + name + "M3Sortable";
        try (Writer writer = processingEnv.getFiler().createSourceFile(fqGenerated, type).openWriter()) {
            if (!pkg.isEmpty()) writer.write("package " + pkg + ";\n\n");
            writer.write("// M3 generated from @Sortable; do not edit.\n");
            writer.write("public final class " + name + "M3Sortable {\n");
            writer.write("    private " + name + "M3Sortable() {}\n");
            writer.write("    public static final int VERSION = 1;\n");
            for (int d = 0; d < lanes.length; d++) {
                writer.write("    private static final long[] L" + d + " = new long[] {");
                for (long token : lanes[d]) writer.write("0x" + Long.toHexString(token) + "L,");
                writer.write("};\n");
            }
            writer.write("    public static long[][] structuralSignals() {\n");
            writer.write("        return new long[][] {L0.clone(), L1.clone(), L2.clone(), "
                    + "L3.clone(), L4.clone(), L5.clone()};\n    }\n");
            // The structural hashed-token API remains exactly as before. Numeric facts are
            // independent, typed int counts with an explicit validity mask.
            writer.write("    private static final int[] FACT_COUNTS = new int[] {");
            for (int feature : facts) writer.write(feature + ",");
            writer.write("};\n");
            writer.write("    private static final long[] FACT_KNOWN = new long[] {0x"
                    + Long.toHexString(validFacts) + "L};\n");
            writer.write("    public static com.m3.collections.sortable.M3CompilerFacts compilerFacts() {\n");
            writer.write("        return new com.m3.collections.sortable.M3CompilerFacts("
                    + "FACT_COUNTS, FACT_KNOWN);\n    }\n");
            if (!accessor.isEmpty()) {
                writer.write("    public static double absoluteSignal(" + name + " value) {\n");
                writer.write("        java.util.Objects.requireNonNull(value, \"value\");\n");
                writer.write("        return value." + accessor + ";\n    }\n");
                writer.write("    public static double[] absoluteSignals(" + name + "[] values) {\n");
                writer.write("        java.util.Objects.requireNonNull(values, \"values\");\n");
                writer.write("        double[] out = new double[values.length];\n");
                writer.write("        for (int i = 0; i < out.length; i++) out[i] = absoluteSignal(values[i]);\n");
                writer.write("        return out;\n    }\n");
            }
            writer.write("}\n");
        } catch (IOException problem) {
            error(type, "cannot write generated sortable source: " + problem.getMessage());
        }
    }

    private String accessor(TypeElement type) {
        String declared = type.getAnnotation(Sortable.class).signal().trim();
        if (declared.isEmpty()) return "";
        int hits = 0;
        String access = "";
        for (Element member : type.getEnclosedElements()) {
            if (!member.getSimpleName().contentEquals(declared)) continue;
            boolean field = member.getKind() == ElementKind.FIELD;
            boolean method = member.getKind() == ElementKind.METHOD
                    && ((ExecutableElement) member).getParameters().isEmpty();
            if (!field && !method) continue;
            if (member.getModifiers().contains(Modifier.PRIVATE)
                    || member.getModifiers().contains(Modifier.STATIC)) {
                error(member, "@Sortable signal must be an accessible instance member");
                return null;
            }
            TypeMirror valueType = method ? ((ExecutableElement) member).getReturnType() : member.asType();
            if (!isNumeric(valueType)) {
                error(member, "@Sortable signal must return a primitive numeric value");
                return null;
            }
            hits++;
            access = declared + (method ? "()" : "");
        }
        if (hits != 1) {
            error(type, "@Sortable signal requires exactly one accessible numeric member: " + declared);
            return null;
        }
        return access;
    }

    private static boolean isNumeric(TypeMirror type) {
        return switch (type.getKind()) {
            case BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

    private static void dependencies(TypeMirror type, TreeSet<Long> output) {
        if (type instanceof ArrayType array) {
            dependencies(array.getComponentType(), output);
        } else if (type instanceof DeclaredType declared) {
            Element element = declared.asElement();
            if (element instanceof TypeElement named) {
                add(output, "dep:" + named.getQualifiedName());
            }
            for (TypeMirror parameter : declared.getTypeArguments()) dependencies(parameter, output);
        }
    }

    private static void add(TreeSet<Long> sink, String token) {
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;
        for (byte b : bytes) {
            hash ^= b & 0xffL;
            hash *= 0x100000001b3L;
        }
        sink.add(hash);
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
