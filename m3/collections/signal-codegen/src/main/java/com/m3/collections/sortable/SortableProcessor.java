// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import com.sun.source.tree.MethodInvocationTree;
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
            for (VariableElement p : method.getParameters()) {
                signature.append(p.asType()).append(';');
                dependencies(p.asType(), lanes[4]);
            }
            signature.append("):").append(method.getReturnType());
            add(lanes[3], signature.toString());
            dependencies(method.getReturnType(), lanes[4]);
            for (TypeMirror ex : method.getThrownTypes()) dependencies(ex, lanes[4]);
        }
        // javac Trees resolves receiver/callee symbols; non-javac processors still emit the
        // package/class/field/method/type dimensions and an empty explicit calls dimension.
        try {
            Trees trees = Trees.instance(processingEnv);
            TreePath path = trees.getPath(type);
            if (path != null) {
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                        Element target = trees.getElement(getCurrentPath());
                        if (target instanceof ExecutableElement method) {
                            Element owner = method.getEnclosingElement();
                            StringBuilder signature = new StringBuilder("call:")
                                    .append(owner).append('#').append(method.getSimpleName()).append('(');
                            for (VariableElement p : method.getParameters()) {
                                signature.append(p.asType()).append(';');
                            }
                            add(lanes[5], signature.append(')').toString());
                        }
                        return super.visitMethodInvocation(tree, unused);
                    }
                }.scan(path, null);
            }
        } catch (IllegalArgumentException unavailable) {
            // Model-level fields, methods and declared types remain available.
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
