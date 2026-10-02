// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticIndex;
import com.m3.indexdb.M3IndexDbSemanticKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

/**
 * MODULE-scope, non-mutating inheritance/implementation resolver for the semantic atom graph.
 *
 * <p>The FILE semantic recipe must not inspect neighboring files. This scanner is the explicit
 * scope promotion: it sees all attributed types in one invocation, resolves only relationships
 * whose target type is also present, and emits composition edges for M3IndexDB fan-in.
 */
public final class M3TypeRelationRecipe
        extends ScanningRecipe<M3TypeRelationRecipe.Accumulator> {
    private static final String DEFAULT_REPOSITORY = "hsoliwal/M3jdk21";

    private final String repository;
    private transient M3SemanticEdgeTable edges = new M3SemanticEdgeTable(this);

    public M3TypeRelationRecipe() {
        this(DEFAULT_REPOSITORY);
    }

    public M3TypeRelationRecipe(String repository) {
        String checked = Objects.requireNonNull(repository, "repository").strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("repository");
        }
        this.repository = checked;
    }

    @Override
    public String getDisplayName() {
        return "Resolve M3 semantic type relationships";
    }

    @Override
    public String getDescription() {
        return "Resolves module-visible implements/extends relationships between indexed source "
                + "types and emits M3IndexDB semantic edges without modifying source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "semantic-index",
                "module",
                "implements",
                "extends",
                "patternization",
                "iop",
                "non-mutating");
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator accumulator) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration declaration,
                    ExecutionContext ctx) {
                J.ClassDeclaration value =
                        super.visitClassDeclaration(declaration, ctx);
                JavaType.FullyQualified sourceType = value.getType();
                if (sourceType == null) return value;

                M3IndexDbSemanticKind sourceKind = semanticKind(value.getKind());
                String sourceFqn = sourceType.getFullyQualifiedName();
                accumulator.types.add(
                        new TypeRecord(
                                sourceFqn,
                                sourceKind,
                                nodeId(sourceKind, sourceFqn)));

                TypeTree parentClass = value.getExtends();
                if (parentClass != null) {
                    addReference(
                            accumulator,
                            sourceFqn,
                            sourceKind,
                            parentClass.getType(),
                            "EXTENDS",
                            0);
                }

                List<TypeTree> interfaces = value.getImplements();
                if (interfaces != null) {
                    String role = value.getKind() == J.ClassDeclaration.Kind.Type.Interface
                            ? "EXTENDS"
                            : "IMPLEMENTS";
                    for (int ordinal = 0; ordinal < interfaces.size(); ordinal++) {
                        addReference(
                                accumulator,
                                sourceFqn,
                                sourceKind,
                                interfaces.get(ordinal).getType(),
                                role,
                                ordinal);
                    }
                }
                return value;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(
            Accumulator accumulator,
            ExecutionContext ctx) {
        accumulator.types.sort(Comparator.comparing(TypeRecord::fullyQualifiedName));
        accumulator.references.sort(
                Comparator.comparing(ReferenceRecord::sourceFqn)
                        .thenComparing(ReferenceRecord::role)
                        .thenComparing(ReferenceRecord::targetFqn)
                        .thenComparingInt(ReferenceRecord::ordinal));

        for (ReferenceRecord reference : accumulator.references) {
            TypeRecord target = find(accumulator.types, reference.targetFqn());
            if (target == null) continue;

            String sourceId = nodeId(reference.sourceKind(), reference.sourceFqn());
            edges.insertRow(
                    ctx,
                    new M3SemanticEdgeTable.Row(
                            sourceId,
                            target.nodeId(),
                            reference.role(),
                            reference.ordinal()));
        }
        return List.of();
    }

    public String repository() {
        return repository;
    }

    private void addReference(
            Accumulator accumulator,
            String sourceFqn,
            M3IndexDbSemanticKind sourceKind,
            JavaType targetType,
            String role,
            int ordinal) {
        if (!(targetType instanceof JavaType.FullyQualified target)) return;
        accumulator.references.add(
                new ReferenceRecord(
                        sourceFqn,
                        sourceKind,
                        target.getFullyQualifiedName(),
                        role,
                        ordinal));
    }

    private String nodeId(
            M3IndexDbSemanticKind kind,
            String fullyQualifiedName) {
        return M3IndexDbSemanticIndex.nodeId(
                kind,
                repository + "/type/" + fullyQualifiedName);
    }

    private static M3IndexDbSemanticKind semanticKind(
            J.ClassDeclaration.Kind.Type kind) {
        return kind == J.ClassDeclaration.Kind.Type.Interface
                ? M3IndexDbSemanticKind.INTERFACE
                : M3IndexDbSemanticKind.IMPLEMENTATION;
    }

    private static TypeRecord find(
            List<TypeRecord> types,
            String fullyQualifiedName) {
        int low = 0;
        int high = types.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            TypeRecord candidate = types.get(middle);
            int compared =
                    candidate.fullyQualifiedName().compareTo(fullyQualifiedName);
            if (compared < 0) {
                low = middle + 1;
            } else if (compared > 0) {
                high = middle;
            } else {
                return candidate;
            }
        }
        return null;
    }

    static final class Accumulator {
        private final List<TypeRecord> types = new ArrayList<>();
        private final List<ReferenceRecord> references = new ArrayList<>();
    }

    private record TypeRecord(
            String fullyQualifiedName,
            M3IndexDbSemanticKind kind,
            String nodeId) {}

    private record ReferenceRecord(
            String sourceFqn,
            M3IndexDbSemanticKind sourceKind,
            String targetFqn,
            String role,
            int ordinal) {}
}
