/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
package com.m3.tooling;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

/** Bounded, opt-in lowering at an already explicit M3 view admission boundary. */
public final class ExplicitViewConcatRecipe extends Recipe {
    private static final String OWNER = "com.m3.indexstring.M3String";
    private static final MethodMatcher SOURCE =
            new MethodMatcher(OWNER + " fromString(java.lang.String)");

    @Option(displayName = "Enable explicit view concatenation",
            description = "Permit this experimental allocation-changing transformation. Defaults to false.",
            required = false)
    private final Boolean enabled;

    public ExplicitViewConcatRecipe() { this(null); }

    @JsonCreator
    public ExplicitViewConcatRecipe(@JsonProperty("enabled") Boolean enabled) { this.enabled = enabled; }

    public Boolean getEnabled() { return enabled; }

    @Override public String getDisplayName() { return "Lower explicit M3 view String concatenation"; }

    @Override public String getDescription() {
        return "Opt-in lowering of attributed M3String.fromString(String + String) to the resolved "
                + "fromConcatOperands helper. Keeps qualifier and operand expressions in order; refuses "
                + "unresolved types, constants, comments, implicit static imports and absent helper ABI.";
    }

    @Override public int maxCycles() { return 1; }

    @Override public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation,
                                                                        ExecutionContext context) {
                J.MethodInvocation visited = super.visitMethodInvocation(invocation, context);
                if (!Boolean.TRUE.equals(enabled) || !SOURCE.matches(visited)
                        || visited.getSelect() == null || visited.getArguments().size() != 1) return visited;
                J.ClassDeclaration declaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (declaration != null && TypeUtils.isOfClassType(declaration.getType(), OWNER)) return visited;
                JavaType.Method sourceType = visited.getMethodType();
                if (sourceType == null || !sourceType.hasFlags(Flag.Public, Flag.Static)
                        || !TypeUtils.isOfClassType(sourceType.getReturnType(), OWNER)) return visited;
                Expression argument = visited.getArguments().getFirst();
                if (!(argument instanceof J.Binary concat) || concat.getOperator() != J.Binary.Type.Addition
                        || !TypeUtils.isString(concat.getLeft().getType())
                        || !TypeUtils.isString(concat.getRight().getType())
                        || unsafeLayoutOrConstant(visited)) return visited;
                JavaType.Method helper = destination(sourceType.getDeclaringType());
                if (helper == null) return visited;
                // Retain the original select, so instance-qualified static invocation side effects remain.
                // Retain both operand ASTs, rather than reparsing their printed source or evaluating early.
                return visited.withName(visited.getName().withSimpleName(helper.getName()))
                        .withArguments(List.of(concat.getLeft().withPrefix(concat.getPrefix()), concat.getRight()))
                        .withMethodType(helper);
            }
        };
    }

    private static JavaType.Method destination(JavaType.FullyQualified type) {
        if (!TypeUtils.isOfClassType(type, OWNER)) return null;
        for (JavaType.Method method : type.getMethods()) {
            if (method.getName().equals("fromConcatOperands") && method.hasFlags(Flag.Public, Flag.Static)
                    && TypeUtils.isOfClassType(method.getReturnType(), OWNER)
                    && method.getParameterTypes().size() == 2
                    && method.getParameterTypes().stream().allMatch(TypeUtils::isString)) return method;
        }
        return null;
    }

    private static boolean unsafeLayoutOrConstant(J.MethodInvocation invocation) {
        boolean[] refuse = {false};
        new JavaIsoVisitor<boolean[]>() {
            @Override public Space visitSpace(Space space, Space.Location location, boolean[] found) {
                if (!space.getComments().isEmpty()) found[0] = true;
                return super.visitSpace(space, location, found);
            }
            @Override public J.Literal visitLiteral(J.Literal literal, boolean[] found) {
                if (literal.getValue() instanceof String) found[0] = true;
                return super.visitLiteral(literal, found);
            }
            @Override public J.Identifier visitIdentifier(J.Identifier identifier, boolean[] found) {
                JavaType.Variable variable = identifier.getFieldType();
                if (variable != null && variable.hasFlags(Flag.Final) && TypeUtils.isString(variable.getType())) {
                    found[0] = true;
                }
                return super.visitIdentifier(identifier, found);
            }
        }.visit(invocation, refuse);
        return refuse[0];
    }
}
