// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TypeTree;

/**
 * Read-only, attributed Java semantic inventory for recipe-first code understanding.
 *
 * <p>This recipe complements structural atom/hash inventory. It records declarations and
 * compile-time semantic relationships from OpenRewrite type attribution. It deliberately does not
 * claim dynamic dispatch, points-to, alias, control-flow, or whole-program call-graph semantics.
 * Missing attribution is emitted as evidence instead of guessed.</p>
 */
public final class M3SemanticCodeInventoryRecipe
        extends ScanningRecipe<M3SemanticCodeInventoryRecipe.State> {

    private final transient M3SemanticInventoryTables.TypeTable typeTable =
            new M3SemanticInventoryTables.TypeTable(this);
    private final transient M3SemanticInventoryTables.MethodTable methodTable =
            new M3SemanticInventoryTables.MethodTable(this);
    private final transient M3SemanticInventoryTables.FieldTable fieldTable =
            new M3SemanticInventoryTables.FieldTable(this);
    private final transient M3SemanticInventoryTables.EdgeTable edgeTable =
            new M3SemanticInventoryTables.EdgeTable(this);
    private final transient M3SemanticInventoryTables.MissingAttributionTable missingTable =
            new M3SemanticInventoryTables.MissingAttributionTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory attributed Java semantics for M3";
    }

    @Override
    public String getDescription() {
        return "Emits deterministic read-only facts for types, methods, fields, direct type "
                + "relationships and statically attributed invocation/constructor targets.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "semantic-inventory",
                "type-attribution",
                "code-understanding",
                "inventory-first",
                "read-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    public boolean replacementAuthority() {
        return false;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration declaration, ExecutionContext context) {
                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                String path = portable(unit.getSourcePath().toString());
                JavaType.FullyQualified attributed =
                        M3SemanticAttribution.fullyQualified(declaration.getType());
                String qualifiedType = attributed == null
                        ? unresolvedType(path, declaration.getSimpleName())
                        : attributed.getFullyQualifiedName();
                String status = attributed == null ? "UNRESOLVED" : "RESOLVED";

                state.putType(
                        path + "\u0000" + qualifiedType,
                        new M3SemanticInventoryTables.TypeRow(
                                path,
                                qualifiedType,
                                declaration.getSimpleName(),
                                declaration.getKind().name(),
                                visibility(declaration.getModifiers()),
                                status,
                                "READ_ONLY_EVIDENCE"));

                J.ClassDeclaration enclosing =
                        getCursor()
                                .getParentTreeCursor()
                                .firstEnclosing(J.ClassDeclaration.class);
                JavaType.FullyQualified enclosingType = enclosing == null
                        ? null
                        : M3SemanticAttribution.fullyQualified(enclosing.getType());
                if (enclosingType != null && attributed != null) {
                    state.addEdge(
                            new EdgeKey(
                                    path,
                                    "TYPE",
                                    enclosingType.getFullyQualifiedName(),
                                    "DECLARES_TYPE",
                                    "TYPE",
                                    qualifiedType,
                                    "ATTRIBUTED_LST"));
                }

                if (attributed == null) {
                    state.addMissing(
                            new MissingKey(
                                    path,
                                    "TYPE",
                                    declaration.getSimpleName(),
                                    "MISSING_DECLARING_TYPE_ATTRIBUTION"));
                } else {
                    JavaType.FullyQualified supertype =
                            M3SemanticAttribution.fullyQualified(attributed.getSupertype());
                    if (supertype != null
                            && !"java.lang.Object".equals(supertype.getFullyQualifiedName())) {
                        state.addEdge(
                                new EdgeKey(
                                        path,
                                        "TYPE",
                                        qualifiedType,
                                        "DIRECT_SUPERTYPE",
                                        "TYPE",
                                        supertype.getFullyQualifiedName(),
                                        "ATTRIBUTED_LST"));
                    }
                    for (JavaType.FullyQualified candidate : attributed.getInterfaces()) {
                        JavaType.FullyQualified implemented =
                                M3SemanticAttribution.fullyQualified(candidate);
                        if (implemented == null) {
                            state.addMissing(
                                    new MissingKey(
                                            path,
                                            "DIRECT_INTERFACE",
                                            qualifiedType,
                                            "MISSING_INTERFACE_TYPE_ATTRIBUTION"));
                            continue;
                        }
                        state.addEdge(
                                new EdgeKey(
                                        path,
                                        "TYPE",
                                        qualifiedType,
                                        "DIRECT_INTERFACE",
                                        "TYPE",
                                        implemented.getFullyQualifiedName(),
                                        "ATTRIBUTED_LST"));
                    }
                }

                List<TypeTree> permits = declaration.getPermits();
                if (permits != null) {
                    for (TypeTree permitted : permits) {
                        String target = M3SemanticAttribution.typeName(permitted.getType());
                        if (attributed != null
                                && M3SemanticAttribution.isResolvedType(permitted.getType())) {
                            state.addEdge(
                                    new EdgeKey(
                                            path,
                                            "TYPE",
                                            qualifiedType,
                                            "PERMITS",
                                            "TYPE",
                                            target,
                                            "ATTRIBUTED_LST"));
                        } else {
                            state.addMissing(
                                    new MissingKey(
                                            path,
                                            "PERMITTED_TYPE",
                                            permitted.printTrimmed(new JavaPrinter<Integer>()),
                                            attributed == null
                                                    ? "MISSING_PERMITTED_TYPE_OWNER_ATTRIBUTION"
                                                    : "MISSING_PERMITTED_TYPE_ATTRIBUTION"));
                        }
                    }
                }
                return super.visitClassDeclaration(declaration, context);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                J.ClassDeclaration owner =
                        getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                String path = portable(unit.getSourcePath().toString());
                JavaType.Method declaredMethod = method.getMethodType();
                JavaType.FullyQualified ownerType = declaredMethod == null
                        ? null
                        : M3SemanticAttribution.fullyQualified(declaredMethod.getDeclaringType());
                String ownerName = ownerType == null
                        ? unresolvedType(path, owner.getSimpleName())
                        : ownerType.getFullyQualifiedName();
                JavaType.Method attributed =
                        M3SemanticAttribution.resolvedMethod(method.getMethodType());
                JavaType.Method overridden =
                        M3SemanticAttribution.overriddenMethod(attributed);
                String key = attributed == null
                        ? unresolvedMethod(ownerName, method)
                        : M3SemanticAttribution.methodKey(attributed);
                String returnType = attributed == null
                        ? unresolvedReturnType(method)
                        : M3SemanticAttribution.returnType(attributed);
                String parameters = attributed == null
                        ? unresolvedParameters(method)
                        : M3SemanticAttribution.parameterTypes(attributed);
                String status = attributed == null ? "UNRESOLVED" : "RESOLVED";

                state.putMethod(
                        path + "\u0000" + key,
                        new M3SemanticInventoryTables.MethodRow(
                                path,
                                ownerName,
                                key,
                                method.getSimpleName(),
                                returnType,
                                parameters,
                                visibility(method.getModifiers()),
                                method.hasModifier(J.Modifier.Type.Static),
                                method.hasModifier(J.Modifier.Type.Native),
                                method.hasModifier(J.Modifier.Type.Abstract),
                                method.hasModifier(J.Modifier.Type.Synchronized),
                                method.isConstructor(),
                                overridden != null,
                                status,
                                "READ_ONLY_EVIDENCE"));
                state.addEdge(
                        new EdgeKey(
                                path,
                                "TYPE",
                                ownerName,
                                "DECLARES_METHOD",
                                "METHOD",
                                key,
                                status.equals("RESOLVED")
                                        ? "ATTRIBUTED_LST"
                                        : "SYNTACTIC_DECLARATION_ONLY"));

                if (attributed != null) {
                    if (!attributed.isConstructor()) {
                        state.addEdge(
                                new EdgeKey(
                                        path,
                                        "METHOD",
                                        key,
                                        "RETURN_TYPE",
                                        "TYPE_SIGNATURE",
                                        M3SemanticAttribution.returnType(attributed),
                                        "OPENREWRITE_METHOD_TYPE"));
                    }
                    for (JavaType parameterType : attributed.getParameterTypes()) {
                        state.addEdge(
                                new EdgeKey(
                                        path,
                                        "METHOD",
                                        key,
                                        "PARAMETER_TYPE",
                                        "TYPE_SIGNATURE",
                                        M3SemanticAttribution.typeName(parameterType),
                                        "OPENREWRITE_METHOD_TYPE"));
                    }
                    for (JavaType.FullyQualified thrown : attributed.getThrownExceptions()) {
                        JavaType.FullyQualified thrownType =
                                M3SemanticAttribution.fullyQualified(thrown);
                        if (thrownType != null) {
                            state.addEdge(
                                    new EdgeKey(
                                            path,
                                            "METHOD",
                                            key,
                                            "THROWS_TYPE",
                                            "TYPE",
                                            thrownType.getFullyQualifiedName(),
                                            "OPENREWRITE_METHOD_TYPE"));
                        } else {
                            state.addMissing(
                                    new MissingKey(
                                            path,
                                            "THROWN_TYPE",
                                            key,
                                            "MISSING_THROWN_TYPE_ATTRIBUTION"));
                        }
                    }
                    if (overridden != null) {
                        state.addEdge(
                                new EdgeKey(
                                        path,
                                        "METHOD",
                                        key,
                                        "OVERRIDES_STATIC_DECLARATION",
                                        "METHOD",
                                        M3SemanticAttribution.methodKey(overridden),
                                        "OPENREWRITE_OVERRIDE_RESOLUTION"));
                    }
                }

                if (attributed == null) {
                    state.addMissing(
                            new MissingKey(
                                    path,
                                    "METHOD",
                                    ownerName + "#" + method.getSimpleName(),
                                    "MISSING_METHOD_TYPE_ATTRIBUTION"));
                }
                return super.visitMethodDeclaration(method, context);
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(
                    J.VariableDeclarations.NamedVariable variable,
                    ExecutionContext context) {
                if (!isFieldDeclaration(variable)) {
                    return super.visitVariable(variable, context);
                }

                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                J.ClassDeclaration owner =
                        getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                J.VariableDeclarations declarations =
                        getCursor().firstEnclosingOrThrow(J.VariableDeclarations.class);
                String path = portable(unit.getSourcePath().toString());
                JavaType.Variable declaredField = variable.getVariableType();
                JavaType.FullyQualified ownerType = declaredField == null
                        ? null
                        : M3SemanticAttribution.fullyQualified(declaredField.getOwner());
                String ownerName = ownerType == null
                        ? unresolvedType(path, owner.getSimpleName())
                        : ownerType.getFullyQualifiedName();
                String fieldType = M3SemanticAttribution.typeName(variable.getType());
                String key = ownerName + "#" + variable.getSimpleName() + ":" + fieldType;
                boolean fieldTypeResolved = M3SemanticAttribution.isResolvedType(variable.getType());
                boolean fieldOwnerResolved = ownerType != null
                        && M3SemanticAttribution.isResolvedType(ownerType);
                boolean fieldResolved = fieldOwnerResolved && fieldTypeResolved;
                String status = fieldResolved ? "RESOLVED" : "UNRESOLVED";

                state.putField(
                        path + "\u0000" + key,
                        new M3SemanticInventoryTables.FieldRow(
                                path,
                                ownerName,
                                key,
                                variable.getSimpleName(),
                                fieldType,
                                visibility(declarations.getModifiers()),
                                hasModifier(declarations.getModifiers(), J.Modifier.Type.Static),
                                hasModifier(declarations.getModifiers(), J.Modifier.Type.Final),
                                status,
                                "READ_ONLY_EVIDENCE"));
                state.addEdge(
                        new EdgeKey(
                                path,
                                "TYPE",
                                ownerName,
                                "DECLARES_FIELD",
                                "FIELD",
                                key,
                                status.equals("RESOLVED")
                                        ? "ATTRIBUTED_LST"
                                        : "SYNTACTIC_DECLARATION_ONLY"));
                if (fieldResolved) {
                    state.addEdge(
                            new EdgeKey(
                                    path,
                                    "FIELD",
                                    key,
                                    "FIELD_TYPE",
                                    "TYPE_SIGNATURE",
                                    fieldType,
                                    "OPENREWRITE_VARIABLE_TYPE"));
                }

                if (!fieldResolved) {
                    state.addMissing(
                            new MissingKey(
                                    path,
                                    "FIELD",
                                    ownerName + "#" + variable.getSimpleName(),
                                    !fieldOwnerResolved
                                            ? "MISSING_FIELD_OWNER_ATTRIBUTION"
                                            : "MISSING_FIELD_TYPE_ATTRIBUTION"));
                }
                return super.visitVariable(variable, context);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation invocation, ExecutionContext context) {
                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                // Documentation references share the Java invocation node shape but do not execute.
                boolean documentation = getCursor().firstEnclosing(Javadoc.DocComment.class) != null;
                emitCallableTarget(
                        portable(unit.getSourcePath().toString()),
                        M3SemanticAttribution.resolvedMethod(invocation.getMethodType()),
                        documentation ? "JAVADOC_REFERENCE" : "METHOD_INVOCATION",
                        invocation.getSimpleName(),
                        documentation ? "MISSING_JAVADOC_REFERENCE_TARGET_ATTRIBUTION"
                                : "MISSING_INVOCATION_TARGET_ATTRIBUTION",
                        documentation ? "DOCUMENTATION_METHOD_TARGET" : "STATIC_INVOCATION_TARGET",
                        documentation ? "DOCUMENTATION_TYPE_TARGET" : "INITIALIZER_STATIC_INVOCATION_TARGET");
                return super.visitMethodInvocation(invocation, context);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext context) {
                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                emitCallableTarget(
                        portable(unit.getSourcePath().toString()),
                        M3SemanticAttribution.resolvedMethod(newClass.getConstructorType()),
                        "CONSTRUCTOR_INVOCATION",
                        newClass.printTrimmed(new JavaPrinter<Integer>()),
                        "MISSING_CONSTRUCTOR_TARGET_ATTRIBUTION",
                        "STATIC_CONSTRUCTOR_TARGET",
                        "INITIALIZER_STATIC_CONSTRUCTOR_TARGET");
                return super.visitNewClass(newClass, context);
            }

            @Override
            public J.MemberReference visitMemberReference(
                    J.MemberReference reference, ExecutionContext context) {
                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                boolean documentation = getCursor().firstEnclosing(Javadoc.DocComment.class) != null;
                emitCallableTarget(
                        portable(unit.getSourcePath().toString()),
                        M3SemanticAttribution.resolvedMethod(reference.getMethodType()),
                        documentation ? "JAVADOC_REFERENCE" : "METHOD_REFERENCE",
                        reference.getReference().getSimpleName(),
                        documentation ? "MISSING_JAVADOC_REFERENCE_TARGET_ATTRIBUTION"
                                : "MISSING_METHOD_REFERENCE_TARGET_ATTRIBUTION",
                        documentation ? "DOCUMENTATION_METHOD_TARGET" : "STATIC_METHOD_REFERENCE_TARGET",
                        documentation ? "DOCUMENTATION_TYPE_TARGET" : "INITIALIZER_STATIC_METHOD_REFERENCE_TARGET");
                return super.visitMemberReference(reference, context);
            }

            private void emitCallableTarget(
                    String path,
                    JavaType.Method target,
                    String sourceKind,
                    String sourceSymbol,
                    String missingTargetReason,
                    String methodRelation,
                    String initializerRelation) {
                if (target == null) {
                    state.addMissing(
                            new MissingKey(
                                    path,
                                    sourceKind,
                                    sourceSymbol,
                                    missingTargetReason));
                    return;
                }

                M3SemanticScopeBoundary.Selection<Cursor> scope = M3SemanticScopeBoundary.find(
                        getCursor(), Cursor::getParent, this::scopeBoundary, null);
                Object owner = scope.cursor() == null ? null : scope.cursor().getValue();
                if (owner instanceof J.MethodDeclaration callerDeclaration) {
                    JavaType.Method caller =
                            M3SemanticAttribution.resolvedMethod(
                                    callerDeclaration.getMethodType());
                    if (caller == null) {
                        state.addMissing(
                                new MissingKey(
                                        path,
                                        "INVOCATION_CALLER",
                                        callerDeclaration.getSimpleName(),
                                        "MISSING_CALLER_METHOD_ATTRIBUTION"));
                        return;
                    }
                    state.addEdge(
                            new EdgeKey(
                                    path,
                                    "METHOD",
                                    M3SemanticAttribution.methodKey(caller),
                                    scope.relation(methodRelation),
                                    "METHOD",
                                    M3SemanticAttribution.methodKey(target),
                                    "OPENREWRITE_METHOD_TYPE"));
                    return;
                }

                if (scope.kind() == M3SemanticScopeBoundary.Kind.ANONYMOUS_BODY) {
                    state.addMissing(
                            new MissingKey(
                                    path,
                                    "ANONYMOUS_INITIALIZER_SCOPE",
                                    sourceSymbol + " -> " + M3SemanticAttribution.methodKey(target),
                                    scope.deferredLambda()
                                            ? "ANONYMOUS_LAMBDA_OWNER_NOT_MODELED"
                                            : "ANONYMOUS_INITIALIZER_OWNER_NOT_MODELED"));
                    return;
                }
                JavaType.FullyQualified ownerType = owner instanceof J.ClassDeclaration declaration
                        ? M3SemanticAttribution.fullyQualified(declaration.getType())
                        : null;
                if (ownerType == null) {
                    state.addMissing(
                            new MissingKey(
                                    path,
                                    "INITIALIZER_SCOPE",
                                    sourceSymbol,
                                    "MISSING_ENCLOSING_TYPE_ATTRIBUTION"));
                    return;
                }
                state.addEdge(
                        new EdgeKey(
                                path,
                                "TYPE",
                                ownerType.getFullyQualifiedName(),
                                scope.relation(initializerRelation),
                                "METHOD",
                                M3SemanticAttribution.methodKey(target),
                                "OPENREWRITE_METHOD_TYPE"));
            }

            private boolean isFieldDeclaration(J.VariableDeclarations.NamedVariable variable) {
                if (variable.isField(getCursor())) {
                    return true;
                }
                Cursor declarationScope = variable.getDeclaringScope(getCursor());
                if (!(declarationScope.getValue() instanceof J.Block block)) {
                    return false;
                }
                Object container = declarationScope.getParentTreeCursor().getValue();
                return container instanceof J.NewClass anonymous && anonymous.getBody() == block;
            }

            private M3SemanticScopeBoundary.Kind scopeBoundary(Cursor parent, Cursor child) {
                Object value = parent.getValue();
                if (value instanceof J.MethodDeclaration) {
                    return M3SemanticScopeBoundary.Kind.METHOD;
                }
                if (value instanceof J.ClassDeclaration) {
                    return M3SemanticScopeBoundary.Kind.TYPE;
                }
                if (value instanceof J.NewClass anonymous
                        && anonymous.getBody() != null
                        && child.getValue() == anonymous.getBody()) {
                    return M3SemanticScopeBoundary.Kind.ANONYMOUS_BODY;
                }
                if (value instanceof J.Lambda) {
                    return M3SemanticScopeBoundary.Kind.LAMBDA;
                }
                return M3SemanticScopeBoundary.Kind.NONE;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(State state, ExecutionContext context) {
        synchronized (state) {
            if (state.exported) {
                return List.of();
            }

            state.types.values().stream()
                    .sorted(Comparator.comparing(M3SemanticInventoryTables.TypeRow::sourcePath)
                            .thenComparing(M3SemanticInventoryTables.TypeRow::qualifiedType))
                    .forEach(row -> typeTable.insertRow(context, row));
            state.methods.values().stream()
                    .sorted(Comparator.comparing(M3SemanticInventoryTables.MethodRow::sourcePath)
                            .thenComparing(M3SemanticInventoryTables.MethodRow::methodKey))
                    .forEach(row -> methodTable.insertRow(context, row));
            state.fields.values().stream()
                    .sorted(Comparator.comparing(M3SemanticInventoryTables.FieldRow::sourcePath)
                            .thenComparing(M3SemanticInventoryTables.FieldRow::fieldKey))
                    .forEach(row -> fieldTable.insertRow(context, row));
            state.edges.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> edgeTable.insertRow(
                            context,
                            new M3SemanticInventoryTables.EdgeRow(
                                    entry.getKey().sourcePath(),
                                    entry.getKey().fromKind(),
                                    entry.getKey().fromSymbol(),
                                    entry.getKey().relation(),
                                    entry.getKey().toKind(),
                                    entry.getKey().toSymbol(),
                                    entry.getValue().sum(),
                                    entry.getKey().evidence(),
                                    "READ_ONLY_EVIDENCE")));
            state.missing.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> missingTable.insertRow(
                            context,
                            new M3SemanticInventoryTables.MissingAttributionRow(
                                    entry.getKey().sourcePath(),
                                    entry.getKey().nodeKind(),
                                    entry.getKey().symbol(),
                                    entry.getKey().reason(),
                                    entry.getValue().sum(),
                                    "READ_ONLY_EVIDENCE")));
            state.exported = true;
            return List.of();
        }
    }

    private static String unresolvedType(String path, String simpleName) {
        return "<unresolved-type>:" + path + "#" + simpleName;
    }

    private static String unresolvedMethod(String owner, J.MethodDeclaration method) {
        long arity = method.getParameters().stream()
                .filter(parameter -> !(parameter instanceof J.Empty))
                .count();
        return owner + "#" + method.getSimpleName() + "/arity=" + arity
                + "/syntax=(" + unresolvedParameters(method) + ")";
    }

    private static String unresolvedReturnType(J.MethodDeclaration method) {
        return method.getReturnTypeExpression() == null
                ? method.isConstructor() ? "<constructor>" : "<unresolved>"
                : method.getReturnTypeExpression().printTrimmed(new JavaPrinter<Integer>());
    }

    private static String unresolvedParameters(J.MethodDeclaration method) {
        return method.getParameters().stream()
                .map(parameter -> parameter.printTrimmed(new JavaPrinter<Integer>()))
                .collect(Collectors.joining(","));
    }

    private static String visibility(List<J.Modifier> modifiers) {
        if (hasModifier(modifiers, J.Modifier.Type.Public)) {
            return "PUBLIC";
        }
        if (hasModifier(modifiers, J.Modifier.Type.Protected)) {
            return "PROTECTED";
        }
        if (hasModifier(modifiers, J.Modifier.Type.Private)) {
            return "PRIVATE";
        }
        return "PACKAGE";
    }

    private static boolean hasModifier(
            List<J.Modifier> modifiers, J.Modifier.Type modifierType) {
        return modifiers.stream().anyMatch(modifier -> modifier.getType() == modifierType);
    }

    private static String portable(String value) {
        return value.replace('\\', '/');
    }

    /** One source-set accumulator. It never owns or mutates source trees. */
    public static final class State {
        private final Map<String, M3SemanticInventoryTables.TypeRow> types = new ConcurrentHashMap<>();
        private final Map<String, M3SemanticInventoryTables.MethodRow> methods = new ConcurrentHashMap<>();
        private final Map<String, M3SemanticInventoryTables.FieldRow> fields = new ConcurrentHashMap<>();
        private final Map<EdgeKey, LongAdder> edges = new ConcurrentHashMap<>();
        private final Map<MissingKey, LongAdder> missing = new ConcurrentHashMap<>();
        private boolean exported;

        private void putType(String key, M3SemanticInventoryTables.TypeRow row) {
            putUnique(types, key, row);
        }

        private void putMethod(String key, M3SemanticInventoryTables.MethodRow row) {
            putUnique(methods, key, row);
        }

        private void putField(String key, M3SemanticInventoryTables.FieldRow row) {
            putUnique(fields, key, row);
        }

        private void addEdge(EdgeKey key) {
            edges.computeIfAbsent(key, ignored -> new LongAdder()).increment();
        }

        private void addMissing(MissingKey key) {
            missing.computeIfAbsent(key, ignored -> new LongAdder()).increment();
        }

        private static <T> void putUnique(Map<String, T> rows, String key, T row) {
            T previous = rows.putIfAbsent(key, row);
            if (previous != null && !previous.equals(row)) {
                throw new IllegalStateException("semantic inventory identity conflict: " + key);
            }
        }
    }

    private record EdgeKey(
            String sourcePath,
            String fromKind,
            String fromSymbol,
            String relation,
            String toKind,
            String toSymbol,
            String evidence)
            implements Comparable<EdgeKey> {
        @Override
        public int compareTo(EdgeKey other) {
            return Comparator.comparing(EdgeKey::sourcePath)
                    .thenComparing(EdgeKey::fromKind)
                    .thenComparing(EdgeKey::fromSymbol)
                    .thenComparing(EdgeKey::relation)
                    .thenComparing(EdgeKey::toKind)
                    .thenComparing(EdgeKey::toSymbol)
                    .thenComparing(EdgeKey::evidence)
                    .compare(this, other);
        }
    }

    private record MissingKey(String sourcePath, String nodeKind, String symbol, String reason)
            implements Comparable<MissingKey> {
        @Override
        public int compareTo(MissingKey other) {
            return Comparator.comparing(MissingKey::sourcePath)
                    .thenComparing(MissingKey::nodeKind)
                    .thenComparing(MissingKey::symbol)
                    .thenComparing(MissingKey::reason)
                    .compare(this, other);
        }
    }

}
