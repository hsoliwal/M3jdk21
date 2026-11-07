// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.staticuse;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

/**
 * Bounded, opt-in javac analysis atom for M3StaticUsePlan, not a replacement compiler.
 * Reads real syntax trees; source payloads are never searched for code with regex.
 * Only literal-start, constant-step int for-loops with stable int/array parameters
 * or literal bounds are admitted. Escape, runtime counts and dependencies remain
 * explicitly UNPROVEN. No source/bytecode rewrite or reclamation is performed here.
 */
@SupportedAnnotationTypes({"jdk.m3.compiler.staticuse.M3StaticUsePlan.Analyze",
        "jdk.m3.compiler.staticuse.M3StaticUsePlan.CompilerSummary"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-mindex/runtime/src/main/java/com/synexia/mindex/virtual/M3StaticFactsProcessor.java blob b59ae8925b0b; recipe m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py v1
public final class M3StaticFactsProcessor extends AbstractProcessor {
    public static final String ANALYZER = "m3-static-facts-v2";
    private final Runnable progress;
    private final Set<String> emitted = new HashSet<>();
    private final Set<String> ledger = new TreeSet<>();
    private final Map<String, String> sourceHashes = new HashMap<>();

    public M3StaticFactsProcessor() { this(null); }
    public M3StaticFactsProcessor(Runnable progress) {
        this.progress = progress == null ? () -> { } : progress;
    }

    private void checkpoint() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("M3 facts interrupted");
        progress.run();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("M3 facts interrupted");
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        checkpoint();
        if (round.processingOver()) {
            if (!round.errorRaised() && !ledger.isEmpty()) {
                try (Writer out = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                        "", "META-INF/m3/compiler-facts.tsv").openWriter()) {
                    out.write("analyzer\tsource_sha256\tmethod\tcompanion\n");
                    for (String line : ledger) { checkpoint(); out.write(line + "\n"); }
                } catch (IOException failure) { error(null, failure); }
            }
            sourceHashes.clear();
            return true;
        }
        Trees trees = Trees.instance(processingEnv);
        for (Element element : round.getElementsAnnotatedWith(M3StaticUsePlan.Analyze.class)) {
            checkpoint();
            if (!(element instanceof ExecutableElement method)) continue;
            try { emit(trees, method); }
            catch (IOException | IllegalArgumentException failure) { error(element, failure); }
        }
        return true;
    }

    private void error(Element element, Exception failure) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "M3 compiler facts: " + failure.getMessage(), element);
    }

    private void emit(Trees trees, ExecutableElement method) throws IOException {
        TreePath path = trees.getPath(method);
        if (path == null || !(path.getLeaf() instanceof MethodTree tree))
            throw new IllegalArgumentException("source tree unavailable");
        TypeElement owner = (TypeElement) method.getEnclosingElement();
        String key = processingEnv.getElementUtils().getBinaryName(owner) + "#"
                + method.getSimpleName() + method.asType();
        if (!emitted.add(key)) return;
        CompilationUnitTree unit = path.getCompilationUnit();
        String uri = unit.getSourceFile().toUri().toString();
        String sourceHash = sourceHashes.get(uri);
        if (sourceHash == null) {
            CharSequence source = unit.getSourceFile().getCharContent(true);
            if (source.length() > 1_048_576) throw new IllegalArgumentException("source character budget");
            sourceHash = digest(source.toString());
            sourceHashes.put(uri, sourceHash);
        }
        Collector collector = new Collector(trees, path, method);
        collector.scan(path, null);
        String packageName = processingEnv.getElementUtils().getPackageOf(owner).getQualifiedName().toString();
        String simpleName = "M3Facts_" + digest(key);
        String qualified = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        try (Writer out = processingEnv.getFiler().createSourceFile(qualified, method).openWriter()) {
            if (!packageName.isEmpty()) out.write("package " + packageName + ";\n");
            out.write("@jdk.m3.compiler.staticuse.M3StaticUsePlan.CompilerSummary(\n"
                    + "analyzer=" + quoted(ANALYZER) + ",\nsourceSha256=" + quoted(sourceHash)
                    + ",\nmethodKey=" + quoted(key) + ",\nloopBounds=" + array(collector.loops)
                    + ",\ndynamicInputs=" + array(collector.dynamic)
                    + ",\nunresolved=" + array(collector.unresolved) + ")\n"
                    + "final class " + simpleName + " { private " + simpleName + "() {} }\n");
        }
        ledger.add(ANALYZER + "\t" + sourceHash + "\t" + key + "\t" + qualified);
    }

    private final class Collector extends TreePathScanner<Void, Void> {
        private final Trees trees;
        private final TreePath methodPath;
        private final Map<String, TypeKind> parameters = new HashMap<>();
        private final Set<String> mutated = new HashSet<>();
        private final List<String> loops = new ArrayList<>();
        private final Set<String> dynamic = new TreeSet<>();
        private final Set<String> unresolved = new TreeSet<>(Set.of(
                "ESCAPE_AND_OWNER_LIFETIME_UNPROVEN", "RUNTIME_REFERENCE_COUNTS_UNPROVEN",
                "CALLEE_AND_DEPENDENCY_EFFECTS_UNPROVEN"));
        private int nodes;

        Collector(Trees trees, TreePath methodPath, ExecutableElement method) {
            this.trees = trees;
            this.methodPath = methodPath;
            method.getParameters().forEach(p -> parameters.put(p.getSimpleName().toString(), p.asType().getKind()));
            new TreeScanner<Void, Void>() {
                private int visits;
                @Override public Void scan(Tree node, Void unused) {
                    checkpoint();
                    if (++visits > 100_000) throw new IllegalArgumentException("mutation node budget");
                    return super.scan(node, unused);
                }
                private void mark(ExpressionTree expression) {
                    expression = unparenthesized(expression);
                    if (expression instanceof IdentifierTree id) mutated.add(id.getName().toString());
                }
                @Override public Void visitAssignment(AssignmentTree n, Void p) { mark(n.getVariable()); return super.visitAssignment(n,p); }
                @Override public Void visitCompoundAssignment(CompoundAssignmentTree n, Void p) { mark(n.getVariable()); return super.visitCompoundAssignment(n,p); }
                @Override public Void visitUnary(UnaryTree n, Void p) {
                    switch (n.getKind()) {
                        case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> mark(n.getExpression());
                        default -> { }
                    }
                    return super.visitUnary(n,p);
                }
            }.scan(methodPath.getLeaf(), null);
        }

        @Override public Void scan(Tree node, Void unused) {
            checkpoint();
            if (++nodes > 100_000) throw new IllegalArgumentException("analysis node budget");
            return super.scan(node, unused);
        }
        @Override public Void visitClass(ClassTree tree, Void unused) { unresolved.add("DEFERRED_CLASS_BODY"); return null; }
        @Override public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) { unresolved.add("DEFERRED_LAMBDA_BODY"); return null; }
        @Override public Void visitWhileLoop(WhileLoopTree tree, Void unused) { unknown(tree); return super.visitWhileLoop(tree,unused); }
        @Override public Void visitDoWhileLoop(DoWhileLoopTree tree, Void unused) { unknown(tree); return super.visitDoWhileLoop(tree,unused); }
        @Override public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void unused) { unknown(tree); return super.visitEnhancedForLoop(tree,unused); }
        private long position(Tree tree) { return trees.getSourcePositions().getStartPosition(methodPath.getCompilationUnit(), tree); }
        private void add(String row) {
            if (loops.size() == 128) throw new IllegalArgumentException("loop metadata budget");
            loops.add(row);
        }
        private void unknown(Tree tree) { add(position(tree) + ":UNKNOWN"); }

        @Override public Void visitForLoop(ForLoopTree loop, Void unused) {
            String shape = shape(loop);
            if (shape == null) unknown(loop); else add(position(loop) + ":" + shape);
            return super.visitForLoop(loop, unused);
        }

        private String shape(ForLoopTree loop) {
            if (loop.getInitializer().size() != 1 || loop.getUpdate().size() != 1
                    || !(loop.getInitializer().getFirst() instanceof VariableTree variable)
                    || !(variable.getType() instanceof PrimitiveTypeTree primitive)
                    || primitive.getPrimitiveTypeKind() != TypeKind.INT
                    || !(loop.getCondition() instanceof BinaryTree condition)) return null;
            String index = variable.getName().toString();
            Integer start = integer(variable.getInitializer());
            if (start == null || !named(condition.getLeftOperand(), index)) return null;
            M3StaticUsePlan.Relation relation = switch (condition.getKind()) {
                case LESS_THAN -> M3StaticUsePlan.Relation.LT;
                case LESS_THAN_EQUAL -> M3StaticUsePlan.Relation.LE;
                case GREATER_THAN -> M3StaticUsePlan.Relation.GT;
                case GREATER_THAN_EQUAL -> M3StaticUsePlan.Relation.GE;
                default -> null;
            };
            Integer step = step(loop.getUpdate().getFirst().getExpression(), index);
            if (relation == null || step == null || writes(loop.getStatement(), index)) return null;
            M3StaticUsePlan.IntLoop plan;
            try { plan = new M3StaticUsePlan.IntLoop(start, step, relation); }
            catch (IllegalArgumentException failure) { return null; }
            ExpressionTree right = condition.getRightOperand();
            Integer bound = integer(right);
            String binding;
            String guard = "INT_UPDATE_NO_WRAP";
            if (bound != null) {
                try { binding = "CONST(" + bound + ");conditionBound=" + plan.conditionBound(bound); }
                catch (ArithmeticException wraps) { return null; }
            } else if (right instanceof IdentifierTree id && parameters.get(id.getName().toString()) == TypeKind.INT
                    && !mutated.contains(id.getName().toString())) {
                binding = "PARAM(" + id.getName() + ")";
                dynamic.add(id.getName().toString());
            } else if (right instanceof MemberSelectTree select && select.getIdentifier().contentEquals("length")
                    && select.getExpression() instanceof IdentifierTree id
                    && parameters.get(id.getName().toString()) == TypeKind.ARRAY
                    && !mutated.contains(id.getName().toString())) {
                binding = "ARRAY_LENGTH(" + id.getName() + ")";
                guard += ",ARRAY_NON_NULL(" + id.getName() + ")";
                dynamic.add(id.getName() + ".length");
            } else return null;
            return "CONDITION_BOUND;start=" + start + ";step=" + step + ";relation=" + relation
                    + ";bound=" + binding + ";guards=" + guard + ";NOT_EXACT_EXECUTIONS";
        }

        private boolean writes(Tree body, String name) {
            boolean[] found = {false};
            new TreeScanner<Void, Void>() {
                private int visits;
                @Override public Void scan(Tree node, Void p) {
                    checkpoint();
                    if (++visits > 100_000) throw new IllegalArgumentException("body node budget");
                    return super.scan(node,p);
                }
                @Override public Void visitAssignment(AssignmentTree n, Void p) { found[0] |= named(n.getVariable(),name); return super.visitAssignment(n,p); }
                @Override public Void visitCompoundAssignment(CompoundAssignmentTree n, Void p) { found[0] |= named(n.getVariable(),name); return super.visitCompoundAssignment(n,p); }
                @Override public Void visitUnary(UnaryTree n, Void p) {
                    switch (n.getKind()) {
                        case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> found[0] |= named(n.getExpression(),name);
                        default -> { }
                    }
                    return super.visitUnary(n,p);
                }
            }.scan(body,null);
            return found[0];
        }
    }

    /** JLS 15.8.5: parentheses preserve variable identity, including assignment targets. */
    private static ExpressionTree unparenthesized(ExpressionTree expression) {
        while (expression instanceof ParenthesizedTree parentheses) expression = parentheses.getExpression();
        return expression;
    }

    private static boolean named(ExpressionTree expression, String name) {
        expression = unparenthesized(expression);
        return expression instanceof IdentifierTree id && id.getName().contentEquals(name);
    }
    private static Integer step(ExpressionTree expression, String name) {
        if (expression instanceof UnaryTree u && named(u.getExpression(),name)) return switch (u.getKind()) {
            case PREFIX_INCREMENT, POSTFIX_INCREMENT -> 1;
            case PREFIX_DECREMENT, POSTFIX_DECREMENT -> -1;
            default -> null;
        };
        if (expression instanceof CompoundAssignmentTree a && named(a.getVariable(),name)) {
            Integer value = integer(a.getExpression());
            if (value == null) return null;
            return switch (a.getKind()) {
                case PLUS_ASSIGNMENT -> value;
                case MINUS_ASSIGNMENT -> value == Integer.MIN_VALUE ? null : -value;
                default -> null;
            };
        }
        return null;
    }
    private static Integer integer(ExpressionTree expression) {
        if (expression instanceof LiteralTree literal && literal.getValue() instanceof Integer value) return value;
        if (expression instanceof UnaryTree unary) {
            Integer value = integer(unary.getExpression());
            if (value == null) return null;
            return switch (unary.getKind()) {
                case UNARY_PLUS -> value;
                case UNARY_MINUS -> value == Integer.MIN_VALUE ? null : -value;
                default -> null;
            };
        }
        return null;
    }
    private static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static String quoted(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
    private static String array(Iterable<String> values) {
        var pieces = new ArrayList<String>();
        for (String value : values) pieces.add(quoted(value));
        return "{" + String.join(",", pieces) + "}";
    }
}
