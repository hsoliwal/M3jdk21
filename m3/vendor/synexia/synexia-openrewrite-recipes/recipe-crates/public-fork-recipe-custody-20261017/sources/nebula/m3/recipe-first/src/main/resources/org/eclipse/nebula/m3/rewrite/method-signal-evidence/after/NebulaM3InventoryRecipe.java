// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Read-only, file-local structural inventory for applying the Synexia M3 invariant to Nebula.
 *
 * <p>The recipe never changes a source tree. It records evidence that can later select a dedicated
 * contract-preserving source-changing recipe.</p>
 */
public final class NebulaM3InventoryRecipe extends Recipe {
    private final transient NebulaM3InventoryTable inventory = new NebulaM3InventoryTable(this);
    private final transient NebulaM3MethodInventoryTable methods =
            new NebulaM3MethodInventoryTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory Eclipse Nebula for M3 recipe-first work";
    }

    @Override
    public String getDescription() {
        return "Records file-local API density, native declarations, loops and fast-search signals "
                + "without modifying Nebula source.";
    }

    @Override
    public java.util.Set<String> getTags() {
        return java.util.Set.of(
                "nebula",
                "m3",
                "inventory-first",
                "recipe-first",
                "fast-search",
                "contract-lock");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext context) {
                J.CompilationUnit result =
                        super.visitCompilationUnit(compilationUnit, context);
                inventory.insertRow(context, new NebulaM3InventoryTable.Row(analyze(result)));
                analyzeMethods(result)
                        .forEach(facts ->
                                methods.insertRow(
                                        context,
                                        new NebulaM3MethodInventoryTable.Row(facts)));
                return result;
            }
        };
    }

    static SourceFacts analyze(J.CompilationUnit compilationUnit) {
        MutableFacts facts = new MutableFacts();
        new JavaIsoVisitor<MutableFacts>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration classDecl, MutableFacts p) {
                p.typeCount++;
                if (classDecl.hasModifier(J.Modifier.Type.Public)
                        || classDecl.hasModifier(J.Modifier.Type.Protected)) {
                    p.publicProtectedTypeCount++;
                }
                return super.visitClassDeclaration(classDecl, p);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, MutableFacts p) {
                p.methodCount++;
                if (method.hasModifier(J.Modifier.Type.Public)
                        || method.hasModifier(J.Modifier.Type.Protected)) {
                    p.publicProtectedMethodCount++;
                }
                if (method.hasModifier(J.Modifier.Type.Native)) {
                    p.nativeMethodCount++;
                }
                return super.visitMethodDeclaration(method, p);
            }

            @Override
            public J.ForLoop visitForLoop(J.ForLoop loop, MutableFacts p) {
                p.loopCount++;
                return super.visitForLoop(loop, p);
            }

            @Override
            public J.ForEachLoop visitForEachLoop(J.ForEachLoop loop, MutableFacts p) {
                p.loopCount++;
                return super.visitForEachLoop(loop, p);
            }

            @Override
            public J.WhileLoop visitWhileLoop(J.WhileLoop loop, MutableFacts p) {
                p.loopCount++;
                return super.visitWhileLoop(loop, p);
            }

            @Override
            public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop loop, MutableFacts p) {
                p.loopCount++;
                return super.visitDoWhileLoop(loop, p);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation method, MutableFacts p) {
                switch (method.getSimpleName()) {
                    case "indexOf", "lastIndexOf" -> p.indexOfCalls++;
                    case "contains", "containsKey", "containsValue" -> p.containsCalls++;
                    case "sort" -> p.sortCalls++;
                    case "binarySearch" -> p.binarySearchCalls++;
                    default -> {
                        // Inventory only the first bounded search/sort signal family.
                    }
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(compilationUnit, facts);

        String source = compilationUnit.printAll();
        facts.todoMarkers = count(source, "TODO") + count(source, "FIXME");
        int lineCount = source.isEmpty() ? 0 : source.split("\\R", -1).length;
        String path = compilationUnit.getSourcePath().toString().replace('\\', '/');
        String module = module(path);
        boolean testSource =
                path.contains("/test/")
                        || path.contains("/tests/")
                        || path.contains(".tests/")
                        || path.contains(".test/")
                        || path.contains("/examples/")
                        || path.startsWith("examples/");

        int methodDensity =
                Math.toIntExact(
                        Math.min(
                                Integer.MAX_VALUE,
                                1000L * lineCount / Math.max(1, facts.methodCount)));
        int visible = facts.publicProtectedMethodCount + facts.publicProtectedTypeCount;
        int declared = facts.methodCount + facts.typeCount;
        int surfaceDensity =
                Math.toIntExact(
                        Math.min(
                                Integer.MAX_VALUE,
                                1000L * visible / Math.max(1, declared)));

        String signal = fastSearchSignal(facts);
        return new SourceFacts(
                path,
                module,
                testSource,
                lineCount,
                facts.typeCount,
                facts.publicProtectedTypeCount,
                facts.methodCount,
                facts.publicProtectedMethodCount,
                facts.nativeMethodCount,
                facts.loopCount,
                facts.indexOfCalls,
                facts.containsCalls,
                facts.sortCalls,
                facts.binarySearchCalls,
                facts.todoMarkers,
                methodDensity,
                surfaceDensity,
                signal,
                nextPass(facts));
    }

    static List<MethodFacts> analyzeMethods(J.CompilationUnit compilationUnit) {
        String path = compilationUnit.getSourcePath().toString().replace('\\', '/');
        ArrayList<MethodFacts> rows = new ArrayList<>();
        new JavaIsoVisitor<List<MethodFacts>>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method,
                    List<MethodFacts> result) {
                J.ClassDeclaration owner =
                        getCursor()
                                .getPathAsStream()
                                .skip(1)
                                .filter(J.ClassDeclaration.class::isInstance)
                                .map(J.ClassDeclaration.class::cast)
                                .findFirst()
                                .orElse(null);
                String ownerType = ownerType(owner);
                MethodMutableFacts facts = new MethodMutableFacts();
                if (method.getBody() != null) {
                    facts.directStatementCount = method.getBody().getStatements().size();
                    new JavaIsoVisitor<MethodMutableFacts>() {
                        @Override
                        public J.MethodDeclaration visitMethodDeclaration(
                                J.MethodDeclaration nested,
                                MethodMutableFacts p) {
                            return nested;
                        }

                        @Override
                        public J.ForLoop visitForLoop(J.ForLoop loop, MethodMutableFacts p) {
                            p.loopCount++;
                            return super.visitForLoop(loop, p);
                        }

                        @Override
                        public J.ForEachLoop visitForEachLoop(
                                J.ForEachLoop loop,
                                MethodMutableFacts p) {
                            p.loopCount++;
                            return super.visitForEachLoop(loop, p);
                        }

                        @Override
                        public J.WhileLoop visitWhileLoop(
                                J.WhileLoop loop,
                                MethodMutableFacts p) {
                            p.loopCount++;
                            return super.visitWhileLoop(loop, p);
                        }

                        @Override
                        public J.DoWhileLoop visitDoWhileLoop(
                                J.DoWhileLoop loop,
                                MethodMutableFacts p) {
                            p.loopCount++;
                            return super.visitDoWhileLoop(loop, p);
                        }

                        @Override
                        public J.If visitIf(J.If condition, MethodMutableFacts p) {
                            p.branchCount++;
                            return super.visitIf(condition, p);
                        }

                        @Override
                        public J.Switch visitSwitch(
                                J.Switch switchStatement,
                                MethodMutableFacts p) {
                            p.branchCount++;
                            return super.visitSwitch(switchStatement, p);
                        }

                        @Override
                        public J.Try visitTry(J.Try tryStatement, MethodMutableFacts p) {
                            p.exceptionBoundaryCount++;
                            return super.visitTry(tryStatement, p);
                        }

                        @Override
                        public J.Throw visitThrow(J.Throw thrown, MethodMutableFacts p) {
                            p.exceptionBoundaryCount++;
                            return super.visitThrow(thrown, p);
                        }

                        @Override
                        public J.Synchronized visitSynchronized(
                                J.Synchronized synchronizedStatement,
                                MethodMutableFacts p) {
                            p.synchronizedCount++;
                            return super.visitSynchronized(synchronizedStatement, p);
                        }

                        @Override
                        public J.Lambda visitLambda(J.Lambda lambda, MethodMutableFacts p) {
                            p.lambdaCount++;
                            return super.visitLambda(lambda, p);
                        }

                        @Override
                        public J.MethodInvocation visitMethodInvocation(
                                J.MethodInvocation invocation,
                                MethodMutableFacts p) {
                            p.invocationCount++;
                            switch (invocation.getSimpleName()) {
                                case "indexOf", "lastIndexOf" -> p.indexOfCalls++;
                                case "contains", "containsKey", "containsValue" ->
                                        p.containsCalls++;
                                case "sort" -> p.sortCalls++;
                                case "binarySearch" -> p.binarySearchCalls++;
                                default -> {
                                    // Method-local evidence only.
                                }
                            }
                            return super.visitMethodInvocation(invocation, p);
                        }
                    }.visit(method.getBody(), facts);
                }

                boolean nativeMethod = method.hasModifier(J.Modifier.Type.Native);
                TreeSet<String> patterns = new TreeSet<>();
                if (facts.loopCount > 0) patterns.add("ITERATION");
                if (facts.branchCount > 0) patterns.add("BRANCHING");
                if (facts.exceptionBoundaryCount > 0) patterns.add("EXCEPTION_BOUNDARY");
                if (facts.synchronizedCount > 0) patterns.add("SYNCHRONIZATION");
                if (facts.lambdaCount > 0) patterns.add("LAMBDA");
                if (nativeMethod) patterns.add("JNI_BOUNDARY");
                if (facts.invocationCount > 0) patterns.add("INVOCATION");
                if (patterns.isEmpty()) patterns.add("STRAIGHT_LINE");

                result.add(
                        new MethodFacts(
                                path,
                                ownerType,
                                methodKey(ownerType, method),
                                method.getSimpleName(),
                                contractSurface(method),
                                nativeMethod,
                                facts.directStatementCount,
                                facts.loopCount,
                                facts.branchCount,
                                facts.exceptionBoundaryCount,
                                facts.synchronizedCount,
                                facts.lambdaCount,
                                facts.invocationCount,
                                facts.indexOfCalls,
                                facts.containsCalls,
                                facts.sortCalls,
                                facts.binarySearchCalls,
                                String.join(";", patterns),
                                fastSearchSignal(
                                        facts.loopCount,
                                        facts.indexOfCalls,
                                        facts.containsCalls,
                                        facts.sortCalls,
                                        facts.binarySearchCalls),
                                nextPass(
                                        facts.loopCount,
                                        facts.indexOfCalls,
                                        facts.containsCalls,
                                        facts.sortCalls,
                                        facts.binarySearchCalls)));
                return super.visitMethodDeclaration(method, result);
            }
        }.visit(compilationUnit, rows);
        return List.copyOf(rows);
    }

    private static String ownerType(J.ClassDeclaration owner) {
        if (owner == null) return "<top-level>";
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(owner.getType());
        return type == null ? "<unresolved>:" + owner.getSimpleName() : type.getFullyQualifiedName();
    }

    private static String methodKey(String ownerType, J.MethodDeclaration method) {
        JavaType.Method type = method.getMethodType();
        if (type != null && TypeUtils.isWellFormedType(type)) {
            return type.toString();
        }
        long arity =
                method.getParameters().stream()
                        .filter(parameter -> !(parameter instanceof J.Empty))
                        .count();
        return ownerType + "#" + method.getSimpleName() + "/arity=" + arity;
    }

    private static String contractSurface(J.MethodDeclaration method) {
        if (method.hasModifier(J.Modifier.Type.Native)) return "JNI_CONTRACT";
        if (method.hasModifier(J.Modifier.Type.Public)) return "PUBLIC_API";
        if (method.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED_API";
        return "INTERNAL_OR_PACKAGE";
    }

    private static String fastSearchSignal(MutableFacts facts) {
        return fastSearchSignal(
                facts.loopCount,
                facts.indexOfCalls,
                facts.containsCalls,
                facts.sortCalls,
                facts.binarySearchCalls);
    }

    private static String fastSearchSignal(
            int loopCount,
            int indexOfCalls,
            int containsCalls,
            int sortCalls,
            int binarySearchCalls) {
        List<String> signals = new ArrayList<>();
        if (binarySearchCalls > 0) signals.add("BINARY_SEARCH");
        if (indexOfCalls > 0) signals.add("LINEAR_INDEX_SEARCH");
        if (containsCalls > 0) signals.add("MEMBERSHIP_SEARCH");
        if (sortCalls > 0) signals.add("SORT");
        if (loopCount > 0 && (indexOfCalls > 0 || containsCalls > 0)) {
            signals.add("REPEATED_SCAN_CANDIDATE");
        }
        return signals.isEmpty() ? "NONE" : String.join(";", signals);
    }

    private static String nextPass(MutableFacts facts) {
        return nextPass(
                facts.loopCount,
                facts.indexOfCalls,
                facts.containsCalls,
                facts.sortCalls,
                facts.binarySearchCalls);
    }

    private static String nextPass(
            int loopCount,
            int indexOfCalls,
            int containsCalls,
            int sortCalls,
            int binarySearchCalls) {
        if (binarySearchCalls > 0) {
            return "VERIFY_EXISTING_ORDERED_SEARCH";
        }
        if (loopCount > 0 && indexOfCalls > 0) {
            return "REVIEW_LINEAR_SEARCH_FOR_PRECOMPUTED_INDEX";
        }
        if (loopCount > 0 && containsCalls > 0) {
            return "REVIEW_REPEATED_MEMBERSHIP_FOR_INDEX";
        }
        if (indexOfCalls > 0) {
            return "REVIEW_LINEAR_SEARCH_CONTRACT";
        }
        if (sortCalls > 0) {
            return "REVIEW_SORT_AND_BOUNDED_RESULT_SHAPE";
        }
        return "NO_FAST_SEARCH_ACTION";
    }

    private static int count(String source, String token) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(token, from)) >= 0) {
            count++;
            from += token.length();
        }
        return count;
    }

    private static String module(String path) {
        int source = path.indexOf("/src/");
        if (source >= 0) return path.substring(0, source);
        int sourceRoot = path.indexOf("/src-");
        if (sourceRoot >= 0) return path.substring(0, sourceRoot);
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "." : path.substring(0, slash);
    }

    static record SourceFacts(
            String sourcePath,
            String module,
            boolean testSource,
            int lineCount,
            int typeCount,
            int publicProtectedTypeCount,
            int methodCount,
            int publicProtectedMethodCount,
            int nativeMethodCount,
            int loopCount,
            int indexOfCalls,
            int containsCalls,
            int sortCalls,
            int binarySearchCalls,
            int todoMarkers,
            int methodDensityX1000,
            int publicSurfaceDensityX1000,
            String fastSearchSignal,
            String recommendedNextPass) {}

    static record MethodFacts(
            String sourcePath,
            String ownerType,
            String methodKey,
            String methodName,
            String contractSurface,
            boolean nativeMethod,
            int directStatementCount,
            int loopCount,
            int branchCount,
            int exceptionBoundaryCount,
            int synchronizedCount,
            int lambdaCount,
            int invocationCount,
            int indexOfCalls,
            int containsCalls,
            int sortCalls,
            int binarySearchCalls,
            String structuralPatterns,
            String fastSearchSignal,
            String recommendedNextPass) {}

    private static final class MethodMutableFacts {
        int directStatementCount;
        int loopCount;
        int branchCount;
        int exceptionBoundaryCount;
        int synchronizedCount;
        int lambdaCount;
        int invocationCount;
        int indexOfCalls;
        int containsCalls;
        int sortCalls;
        int binarySearchCalls;
    }

    private static final class MutableFacts {
        int typeCount;
        int publicProtectedTypeCount;
        int methodCount;
        int publicProtectedMethodCount;
        int nativeMethodCount;
        int loopCount;
        int indexOfCalls;
        int containsCalls;
        int sortCalls;
        int binarySearchCalls;
        int todoMarkers;
    }
}
