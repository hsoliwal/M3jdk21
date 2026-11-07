// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.matsortable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Portable JSR-269 generator for a Lombok-style @Sortable companion.
 * It never mutates the annotated class, loads it reflectively, or assumes runtime instance values.
 * The processor intentionally locates the MAT annotation by its canonical name so that its
 * canonical mat-value-processor module need not acquire a new dependency on mat-collections.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-mat-value-processor/src/main/java/com/synexia/matvalue/processor/MatSortableProcessor.java blob 095ac09526a5; recipe m3/compiler/mat-sortable/recipe/mat-sortable-processor-20261107/relocate.py v1
public final class M3MatSortableProcessor extends AbstractProcessor {
    private static final String SORTABLE = "jdk.m3.collections.Sortable";
    private static final String FIELD = "jdk.m3.collections.SortField";
    private static final String SIGNAL = "jdk.m3.collections.SortSignal";
    private static final String DISTANCE = "jdk.m3.collections.SortDistance";
    private static final String DECLARED_DEPENDENCY = "jdk.m3.collections.SortDependency";
    private static final Set<String> EXACT_TYPES = Set.of(
            "java.lang.String", "java.lang.Boolean", "java.lang.Byte", "java.lang.Short",
            "java.lang.Character", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
            "java.lang.Double", "java.math.BigDecimal", "java.math.BigInteger", "java.util.UUID",
            "java.time.Instant", "java.time.LocalDate", "java.time.LocalDateTime");
    private final Set<String> emitted = new HashSet<>();

    /** Instantiated by javac (opt-in through -processor or the Processors API); explicit for the exported module package. */
    public M3MatSortableProcessor() { }

    private record Component(String name, String access, TypeMirror type, int priority,
            boolean descending, Element origin) { }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(SORTABLE, FIELD, SIGNAL, DISTANCE, DECLARED_DEPENDENCY);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        if (round.processingOver()) return false;
        TypeElement marker = processingEnv.getElementUtils().getTypeElement(SORTABLE);
        if (marker == null) return false;
        for (Element element : round.getElementsAnnotatedWith(marker)) {
            if (!(element instanceof TypeElement type)) {
                error(element, "@Sortable must annotate a class or record");
                continue;
            }
            String qualified = type.getQualifiedName().toString();
            if (!emitted.add(qualified)) continue;
            if (!admittedType(type)) continue;
            List<Component> components = components(type);
            if (components == null) continue;
            ExecutableElement signal = candidateMethod(type, SIGNAL, 0);
            ExecutableElement distance = candidateMethod(type, DISTANCE, 1);
            if (hasAnnotatedInvalidMethod(type, SIGNAL, signal)
                    || hasAnnotatedInvalidMethod(type, DISTANCE, distance)) continue;
            if (signal != null && signal.getReturnType().getKind() != TypeKind.DOUBLE) {
                error(signal, "@SortSignal must return primitive double");
                continue;
            }
            if (distance != null && (distance.getReturnType().getKind() != TypeKind.DOUBLE
                    || !processingEnv.getTypeUtils().isSameType(
                            processingEnv.getTypeUtils().erasure(distance.getParameters().get(0).asType()),
                            processingEnv.getTypeUtils().erasure(type.asType())))) {
                error(distance, "@SortDistance must take this type and return primitive double");
                continue;
            }
            if (components.isEmpty() && signal == null) {
                error(type, "@Sortable requires @SortField, record components, or @SortSignal");
                continue;
            }
            try {
                generate(type, components, signal, distance);
            } catch (IOException exception) {
                error(type, "cannot write @Sortable companion: " + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                // Schema bounds are user-source diagnostics, not uncaught processor failures.
                error(type, "invalid @Sortable schema: " + exception.getMessage());
            }
        }
        // Let AnOP's independent behavior processors see their annotations too.
        return false;
    }

    private boolean admittedType(TypeElement type) {
        if (type.getKind() != ElementKind.CLASS && type.getKind() != ElementKind.RECORD) {
            error(type, "@Sortable requires a concrete class or record");
            return false;
        }
        if (type.getModifiers().contains(Modifier.ABSTRACT)
                || !type.getTypeParameters().isEmpty()) {
            error(type, "@Sortable requires a concrete non-generic type");
            return false;
        }
        Element cursor = type;
        while (cursor instanceof TypeElement owner) {
            if (owner.getModifiers().contains(Modifier.PRIVATE)
                    || !owner.getTypeParameters().isEmpty()) {
                error(type, "@Sortable type and enclosing classes must be accessible and non-generic");
                return false;
            }
            cursor = owner.getEnclosingElement();
        }
        if (type.getNestingKind() != NestingKind.TOP_LEVEL
                && type.getNestingKind() != NestingKind.MEMBER) {
            error(type, "@Sortable cannot annotate local or anonymous types");
            return false;
        }
        return true;
    }

    private List<Component> components(TypeElement type) {
        List<Component> selected = new ArrayList<>();
        Set<String> recordNames = new HashSet<>();
        for (RecordComponentElement component : type.getRecordComponents()) {
            recordNames.add(component.getSimpleName().toString());
            if (has(component, FIELD)) {
                selected.add(toComponent(component, component.getSimpleName().toString() + "()"));
            }
        }
        for (Element child : type.getEnclosedElements()) {
            if (!has(child, FIELD)) continue;
            if (recordNames.contains(child.getSimpleName().toString())) continue;
            if (child.getKind() == ElementKind.FIELD) {
                if (child.getModifiers().contains(Modifier.PRIVATE)
                        || child.getModifiers().contains(Modifier.STATIC)) {
                    error(child, "@SortField must be accessible and nonstatic");
                    return null;
                }
                selected.add(toComponent(child, child.getSimpleName().toString()));
            } else if (child instanceof ExecutableElement method
                    && method.getKind() == ElementKind.METHOD) {
                if (!validMethod(method, 0)) return null;
                selected.add(toComponent(child, child.getSimpleName().toString() + "()"));
            } else {
                error(child, "@SortField requires an instance field, accessor or record component");
                return null;
            }
        }
        if (selected.isEmpty() && type.getKind() == ElementKind.RECORD) {
            int index = 0;
            for (RecordComponentElement component : type.getRecordComponents()) {
                selected.add(new Component(component.getSimpleName().toString(),
                        component.getSimpleName().toString() + "()", component.asType(), index++,
                        false, component));
            }
        }
        Set<Integer> priorities = new HashSet<>();
        for (Component c : selected) {
            if (!priorities.add(c.priority())) {
                error(c.origin(), "duplicate @SortField priority " + c.priority());
                return null;
            }
            if (!exactType(c.type())) {
                error(c.origin(), "unsupported @SortField type: " + c.type());
                return null;
            }
        }
        selected.sort(Comparator.comparingInt(Component::priority).thenComparing(Component::name));
        return List.copyOf(selected);
    }

    private Component toComponent(Element element, String access) {
        int priority = (Integer) annotationValue(element, FIELD, "priority");
        boolean descending = (Boolean) annotationValue(element, FIELD, "descending");
        TypeMirror type = element instanceof ExecutableElement method
                ? method.getReturnType() : element.asType();
        return new Component(element.getSimpleName().toString(), access, type, priority,
                descending, element);
    }

    private ExecutableElement candidateMethod(TypeElement type, String annotation,
            int parameters) {
        ExecutableElement selected = null;
        for (Element child : type.getEnclosedElements()) {
            if (!has(child, annotation)) continue;
            if (!(child instanceof ExecutableElement method) || !validMethod(method, parameters)) {
                return null;
            }
            if (selected != null) {
                error(child, "multiple " + annotation + " methods are ambiguous");
                return null;
            }
            selected = method;
        }
        return selected;
    }

    private boolean hasAnnotatedInvalidMethod(TypeElement type, String annotation,
            ExecutableElement selected) {
        int count = 0;
        for (Element child : type.getEnclosedElements()) {
            if (has(child, annotation)) count++;
        }
        return count > 0 && (selected == null || count != 1);
    }

    private boolean validMethod(ExecutableElement method, int paramCount) {
        if (method.getKind() != ElementKind.METHOD
                || method.getModifiers().contains(Modifier.STATIC)
                || method.getModifiers().contains(Modifier.PRIVATE)
                || method.getModifiers().contains(Modifier.ABSTRACT)
                || method.getParameters().size() != paramCount) {
            error(method, "sortable method must be accessible, concrete, nonstatic, with "
                    + paramCount + " parameter(s)");
            return false;
        }
        // Companion methods do not declare checked exceptions. Reject such source methods
        // at the annotated declaration rather than generating uncompilable Java.
        TypeMirror unchecked = processingEnv.getElementUtils()
                .getTypeElement("java.lang.RuntimeException").asType();
        TypeMirror errors = processingEnv.getElementUtils()
                .getTypeElement("java.lang.Error").asType();
        for (TypeMirror thrown : method.getThrownTypes()) {
            if (!processingEnv.getTypeUtils().isSubtype(thrown, unchecked)
                    && !processingEnv.getTypeUtils().isSubtype(thrown, errors)) {
                error(method, "sortable method must not declare checked exceptions: " + thrown);
                return false;
            }
        }
        return true;
    }

    private boolean exactType(TypeMirror type) {
        if (type.getKind().isPrimitive()) return type.getKind() != TypeKind.VOID;
        if (type.getKind() != TypeKind.DECLARED) return false;
        Element element = processingEnv.getTypeUtils().asElement(type);
        return element instanceof TypeElement clazz
                && (clazz.getKind() == ElementKind.ENUM
                    || EXACT_TYPES.contains(clazz.getQualifiedName().toString()));
    }

    private void generate(TypeElement type, List<Component> columns,
            ExecutableElement signal, ExecutableElement distance) throws IOException {
        String qualified = type.getQualifiedName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(type)
                .getQualifiedName().toString();
        String simple = name(type) + "Sortable";
        String generatedName = packageName.isEmpty() ? simple : packageName + "." + simple;
        String descriptor = schema(type);
        String digest = sha256(descriptor);
        StructuralFeatures structure = structuralFeatures(descriptor);
        String policyDigest = orderPolicyFingerprint(digest, columns, signal, distance);
        JavaFileObject file = processingEnv.getFiler().createSourceFile(generatedName, type);
        try (Writer w = file.openWriter()) {
            w.write("// SPDX-License-Identifier: Apache-2.0\n");
            if (!packageName.isEmpty()) w.write("package " + packageName + ";\n\n");
            w.write("/** Deterministic @Sortable companion generated at source time. */\n");
            w.write("public final class " + simple + " {\n");
            w.write("    public static final String SCHEMA_SHA256 = \"" + digest + "\";\n");
            w.write("    public static final String ORDER_POLICY_SHA256 = \"" + policyDigest + "\";\n");
            w.write("    public static final String SCHEMA = \"" + quote(descriptor) + "\";\n");
            writeStructuralMetadata(w, structure);
            w.write("    private " + simple + "() {}\n");
            w.write("    public static int compareExact(" + qualified + " left, "
                    + qualified + " right) {\n");
            w.write("        if (left == right) return 0;\n");
            w.write("        if (left == null) return -1;\n");
            w.write("        if (right == null) return 1;\n");
            if (!columns.isEmpty()) {
                w.write("        int comparison;\n");
                for (Component column : columns) {
                    String expr = compareExpr(column);
                    w.write("        comparison = " + expr + ";\n");
                    if (column.descending()) w.write("        comparison = Integer.compare(0, comparison);\n");
                    w.write("        if (comparison != 0) return comparison;\n");
                }
            }
            if (columns.isEmpty() && signal != null) {
                // Signal-only classes have a numeric ordering. Returning zero
                // for every distinct pair silently destroyed the only rank fact.
                w.write("        return Double.compare(left." + signal.getSimpleName()
                        + "(), right." + signal.getSimpleName() + "());\n    }\n");
            } else {
                w.write("        return 0;\n    }\n");
            }
            w.write("    public static java.util.Comparator<" + qualified
                    + "> exactComparator() { return " + simple + "::compareExact; }\n");
            boolean nullableNeeded = columns.stream().anyMatch(c -> !c.type().getKind().isPrimitive());
            if (nullableNeeded) {
                w.write("    private static <T extends java.lang.Comparable<? super T>> "
                        + "int compareNullable(T left, T right) {\n"
                        + "        if (left == right) return 0;\n"
                        + "        if (left == null) return -1;\n"
                        + "        if (right == null) return 1;\n"
                        + "        return left.compareTo(right);\n    }\n");
            }
            if (signal != null) {
                w.write("    public static double primary(" + qualified + " value) {\n"
                        + "        return java.util.Objects.requireNonNull(value, \"value\")."
                        + signal.getSimpleName() + "();\n    }\n");
                w.write("    public static double[] signals(" + qualified + "[] values) {\n"
                        + "        java.util.Objects.requireNonNull(values, \"values\");\n"
                        + "        double[] result = new double[values.length];\n"
                        + "        for (int i = 0; i < values.length; i++) result[i] = primary(values[i]);\n"
                        + "        return result;\n    }\n");
            }
            if (distance != null) {
                w.write("    public static double distance(" + qualified + " left, "
                        + qualified + " right) {\n"
                        + "        java.util.Objects.requireNonNull(left, \"left\");\n"
                        + "        java.util.Objects.requireNonNull(right, \"right\");\n"
                        + "        double result = left." + distance.getSimpleName() + "(right);\n"
                        + "        if (!Double.isFinite(result) || result < 0.0) {\n"
                        + "            throw new IllegalArgumentException(\"invalid domain distance\");\n"
                        + "        }\n        return result;\n    }\n");
            }
            writeEvidence(w, qualified, !columns.isEmpty(), signal != null, distance != null);
            w.write("}\n");
        }
    }

    /**
     * The normal Comparator stays allocation-free. On request, this produces a single
     * immutable comparison receipt with explicit signal/domain distance availability.
     */
    private static void writeEvidence(Writer writer, String qualified, boolean exactColumns,
            boolean hasSignal, boolean hasDistance) throws IOException {
        writer.write("""
                /** Comparison evidence; numeric signal separation is not domain identity. */
                public record ComparisonEvidence(
                        int order, boolean exactOrderCertified,
                        boolean signalSeparationAvailable, double signalSeparation,
                        boolean domainDistanceAvailable, double domainDistance) {
                    public ComparisonEvidence {
                        if (order < -1 || order > 1
                                || (signalSeparationAvailable
                                        && (!Double.isFinite(signalSeparation)
                                                || signalSeparation < 0.0))
                                || (!signalSeparationAvailable && signalSeparation != 0.0)
                                || (domainDistanceAvailable
                                        && (!Double.isFinite(domainDistance)
                                                || domainDistance < 0.0))
                                || (!domainDistanceAvailable && domainDistance != 0.0)) {
                            throw new IllegalArgumentException("invalid comparison evidence");
                        }
                    }
                }
                """);
        writer.write("    /** Computes each available dimension once; no all-pairs cache. */\n");
        writer.write("    public static ComparisonEvidence compareWithEvidence("
                + qualified + " left, " + qualified + " right) {\n");
        writer.write("        if (left == null || right == null) {\n"
                + "            return new ComparisonEvidence("
                + "Integer.signum(compareExact(left, right)), "
                + exactColumns + ", false, 0.0, false, 0.0);\n"
                + "        }\n");
        if (hasSignal) {
            writer.write("        double leftSignal = primary(left);\n"
                    + "        double rightSignal = left == right ? leftSignal : primary(right);\n");
            writer.write("        int order = Integer.signum("
                    + (exactColumns ? "compareExact(left, right)" :
                            "Double.compare(leftSignal, rightSignal)") + ");\n");
            writer.write("        double rawGap = Math.abs(leftSignal - rightSignal);\n"
                    + "        boolean knownGap = Double.isFinite(leftSignal)"
                    + " && Double.isFinite(rightSignal) && Double.isFinite(rawGap);\n"
                    + "        double gap = knownGap ? rawGap : 0.0;\n");
        } else {
            writer.write("        int order = Integer.signum(compareExact(left, right));\n"
                    + "        boolean knownGap = false;\n"
                    + "        double gap = 0.0;\n");
        }
        if (hasDistance) {
            writer.write("        double domain = distance(left, right);\n");
        } else {
            writer.write("        double domain = 0.0;\n");
        }
        writer.write("        return new ComparisonEvidence(order, " + exactColumns
                + ", knownGap, gap, " + hasDistance + ", domain);\n"
                + "    }\n");
    }

    private String compareExpr(Component component) {
        String left = "left." + component.access();
        String right = "right." + component.access();
        if (!component.type().getKind().isPrimitive()) {
            return "compareNullable(" + left + ", " + right + ")";
        }
        String wrapper = switch (component.type().getKind()) {
            case BOOLEAN -> "Boolean";
            case BYTE -> "Byte";
            case SHORT -> "Short";
            case CHAR -> "Character";
            case INT -> "Integer";
            case LONG -> "Long";
            case FLOAT -> "Float";
            case DOUBLE -> "Double";
            default -> throw new IllegalArgumentException("unsupported primitive type");
        };
        return "java.lang." + wrapper + ".compare(" + left + ", " + right + ")";
    }


    /**
     * Declaration-only policy identity, distinct from the existing structural schema identity.
     * No method-body or instance-value equivalence is claimed by this fingerprint.
     */
    private static String orderPolicyFingerprint(String schemaDigest,
            List<Component> columns, ExecutableElement signal, ExecutableElement distance) {
        List<String> facts = new ArrayList<>();
        facts.add("mat-sort-order-policy/v1");
        facts.add("schema:" + schemaDigest);
        facts.add("ordering:" + (columns.isEmpty() ? "signal-only" : "exact-columns"));
        for (Component column : columns) {
            facts.add("priority:" + column.priority());
            facts.add("member:" + column.name());
            facts.add("access:" + column.access());
            facts.add("type:" + column.type());
            facts.add("descending:" + column.descending());
        }
        facts.add("signal:" + (signal == null ? "-"
                : signal.getSimpleName() + "():" + signal.getReturnType()));
        facts.add("distance:" + (distance == null ? "-"
                : distance.getSimpleName() + "("
                    + distance.getParameters().get(0).asType() + "):"
                    + distance.getReturnType()));
        return sha256(String.join("\n", facts) + "\n");
    }

    private String schema(TypeElement type) {
        List<String> lines = new ArrayList<>();
        TreeSet<String> deps = new TreeSet<>();
        lines.add("schema/v1");
        lines.add("type:" + type.getQualifiedName());
        lines.add("kind:" + type.getKind());
        lines.add("package:" + processingEnv.getElementUtils().getPackageOf(type).getQualifiedName());
        lines.add("super:" + type.getSuperclass());
        deps.add(type.getSuperclass().toString());
        for (TypeMirror parent : type.getInterfaces()) deps.add(parent.toString());
        List<String> members = new ArrayList<>();
        for (Element child : type.getEnclosedElements()) {
            if (child.getKind() == ElementKind.FIELD || child.getKind() == ElementKind.RECORD_COMPONENT) {
                members.add("field:" + child.getSimpleName() + ":" + child.asType()
                        + ":" + child.getModifiers());
                deps.add(child.asType().toString());
            } else if (child instanceof ExecutableElement method
                    && (child.getKind() == ElementKind.METHOD || child.getKind() == ElementKind.CONSTRUCTOR)) {
                List<String> params = method.getParameters().stream()
                        .map(x -> x.asType().toString()).toList();
                members.add("method:" + method.getSimpleName() + "(" + String.join(",", params)
                        + "):" + method.getReturnType() + ":" + method.getModifiers());
                deps.add(method.getReturnType().toString());
                deps.addAll(params);
            }
        }
        members.sort(String::compareTo);
        if (members.size() > 128) throw new IllegalArgumentException("@Sortable schema budget exceeded");
        lines.addAll(members);
        deps.remove("none");
        deps.remove("void");
        deps.remove("java.lang.Object");
        for (String dep : deps) lines.add("dependency:" + dep);
        List<String> declarations = new ArrayList<>();
        declarations.addAll(declaredDependencies(type, "type:" + type.getQualifiedName()));
        for (Element child : type.getEnclosedElements()) {
            if (child.getKind() == ElementKind.FIELD) {
                declarations.addAll(
                        declaredDependencies(child, "field:" + child.getSimpleName()));
            } else if (child instanceof ExecutableElement method
                    && (child.getKind() == ElementKind.METHOD
                        || child.getKind() == ElementKind.CONSTRUCTOR)) {
                List<String> parameters = method.getParameters().stream()
                        .map(value -> value.asType().toString()).toList();
                declarations.addAll(declaredDependencies(
                        child, "method:" + method.getSimpleName()
                                + "(" + String.join(",", parameters) + ")"));
            }
        }
        if (declarations.size() > 128) {
            throw new IllegalArgumentException("@SortDependency total edge budget exceeded");
        }
        declarations.sort(String::compareTo);
        lines.addAll(declarations);
        String result = String.join("\n", lines) + "\n";
        if (result.length() > 16_384) throw new IllegalArgumentException("@Sortable schema size exceeded");
        return result;
    }

    /** Source-declaration features; method bodies and dynamic calls remain outside this proof. */
    private record StructuralFeatures(
            List<String> keys, String sha256, int fields, int methods,
            int dependencies, int declaredEdges, int packageDepth, double magnitude) { }

    /** Convert the compiler-attributed schema into deterministic, named structural dimensions. */
    private static StructuralFeatures structuralFeatures(String descriptor) {
        TreeSet<String> keys = new TreeSet<>();
        int fields = 0;
        int methods = 0;
        int dependencies = 0;
        int declaredEdges = 0;
        int packageDepth = 0;
        for (String line : descriptor.lines().toList()) {
            if (line.startsWith("type:")) {
                keys.add("class:" + line.substring("type:".length()));
            } else if (line.startsWith("package:")) {
                keys.add(line);
                String pkg = line.substring("package:".length());
                if (!pkg.isEmpty()) {
                    packageDepth = 1;
                    for (int i = 0; i < pkg.length(); i++) {
                        if (pkg.charAt(i) == '.') packageDepth++;
                    }
                }
            } else if (line.startsWith("field:")) {
                keys.add(line);
                fields++;
            } else if (line.startsWith("method:")) {
                keys.add(line);
                methods++;
            } else if (line.startsWith("dependency:")) {
                keys.add(line);
                dependencies++;
            } else if (line.startsWith("declared-edge:")) {
                keys.add(line);
                declaredEdges++;
            } else if (line.startsWith("kind:") || line.startsWith("super:")) {
                keys.add(line);
            }
        }
        List<String> ordered = List.copyOf(keys);
        String exact = "mat-structural-features/v1\n" + String.join("\n", ordered) + "\n";
        double magnitude = fields + methods * 0.5 + dependencies * 0.25
                + declaredEdges * 0.125 + packageDepth * 0.125;
        return new StructuralFeatures(
                ordered, sha256(exact), fields, methods, dependencies, declaredEdges,
                packageDepth, magnitude);
    }

    /** Source-time constants plus an immutable index that is initialized only when queried. */
    private static void writeStructuralMetadata(Writer writer, StructuralFeatures features)
            throws IOException {
        writer.write("    public static final String STRUCTURAL_FEATURES_SHA256 = \""
                + features.sha256() + "\";\n");
        writer.write("    public static final int STRUCTURAL_FIELD_COUNT = "
                + features.fields() + ";\n");
        writer.write("    public static final int STRUCTURAL_METHOD_COUNT = "
                + features.methods() + ";\n");
        writer.write("    public static final int STRUCTURAL_DEPENDENCY_COUNT = "
                + features.dependencies() + ";\n");
        writer.write("    public static final int STRUCTURAL_DECLARED_EDGE_COUNT = "
                + features.declaredEdges() + ";\n");
        writer.write("    public static final int STRUCTURAL_PACKAGE_DEPTH = "
                + features.packageDepth() + ";\n");
        writer.write("    /** Heuristic complexity rank, not lexical or exact domain order. */\n");
        writer.write("    public static final double STRUCTURAL_MAGNITUDE = "
                + Double.toString(features.magnitude()) + ";\n");
        writer.write("    public static double structuralSignal() { return STRUCTURAL_MAGNITUDE; }\n");
        writer.write("    /** Exact declaration keys; class-level metadata only, no per-object payload. */\n");
        writer.write("    public static java.util.List<String> structuralFeatures() {\n"
                + "        return StructuralFeatureHolder.VALUES;\n    }\n");
        writer.write("    private static final class StructuralFeatureHolder {\n"
                + "        private static final java.util.List<String> VALUES = java.util.List.of(\n");
        for (int index = 0; index < features.keys().size(); index++) {
            writer.write("                \"" + quote(features.keys().get(index)) + "\""
                    + (index + 1 == features.keys().size() ? "" : ",") + "\n");
        }
        writer.write("        );\n    }\n");
    }

    /**
     * A declared dependency is a source annotation, not a resolved compiler call edge.
     * Preserve only finite, normalized, unambiguous metadata with bounded retention.
     */
    private List<String> declaredDependencies(Element element, String owner) {
        if (!has(element, DECLARED_DEPENDENCY)) return List.of();
        Object raw = annotationValue(element, DECLARED_DEPENDENCY, "value");
        if (!(raw instanceof List<?> entries) || entries.isEmpty() || entries.size() > 64) {
            throw new IllegalArgumentException("@SortDependency requires 1..64 descriptors");
        }
        TreeSet<String> edges = new TreeSet<>();
        for (Object item : entries) {
            if (!(item instanceof AnnotationValue value)
                    || !(value.getValue() instanceof String descriptor)
                    || descriptor.isBlank() || descriptor.length() > 256
                    || !descriptor.equals(descriptor.strip())
                    || descriptor.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("invalid @SortDependency descriptor");
            }
            if (!edges.add("declared-edge:" + owner + "->" + descriptor)) {
                throw new IllegalArgumentException("duplicate @SortDependency descriptor");
            }
        }
        return List.copyOf(edges);
    }

    private static String name(TypeElement type) {
        List<String> parts = new ArrayList<>();
        Element current = type;
        while (current instanceof TypeElement member) {
            parts.add(0, member.getSimpleName().toString());
            current = member.getEnclosingElement();
        }
        return String.join("_", parts);
    }

    private static String quote(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static boolean has(Element element, String annotationName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationName)) return true;
        }
        return false;
    }

    private Object annotationValue(Element element, String annotationName, String key) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (!mirror.getAnnotationType().toString().equals(annotationName)) continue;
            for (var entry : processingEnv.getElementUtils()
                    .getElementValuesWithDefaults(mirror).entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals(key)) {
                    return entry.getValue().getValue();
                }
            }
        }
        throw new IllegalArgumentException("missing annotation value: " + key);
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
