/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.m3.rewrite.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Read-only, attributed stream-chain facts and one finite domain naming hint.
 * A hint is a human-readable index: it is neither stable semantic identity nor
 * a warrant for equivalence, extraction, mutation, or evaluation of the chain.
 * Unsupported syntax or attribution remains UNKNOWN, with original AST facts.
 */
public final class NebulaM3StreamSegmentNames {
    private static final String ITEM = "org.eclipse.nebula.widgets.opal.roundedtoolbar.RoundedToolItem";
    private static final String EVENT = "org.eclipse.swt.widgets.Event";
    private static final String RECTANGLE = "org.eclipse.swt.graphics.Rectangle";
    private static final String STREAM = "java.util.stream.Stream";
    private static final String HINT = "findFirstEnabledItemContainingEvent";
    private static final int MAX_SEGMENTS = 32;

    private NebulaM3StreamSegmentNames() {
    }

    public enum Status { MATCH, UNKNOWN }

    /** Retains the exact original node, including receiver, arguments and callee. */
    public record Segment(J.MethodInvocation invocation) {
        public JavaType.Method callee() {
            return invocation.getMethodType();
        }

        public Expression receiver() {
            return invocation.getSelect();
        }

        public List<Expression> arguments() {
            return List.copyOf(invocation.getArguments());
        }
    }

    public static final class Candidate {
        private final Status status;
        private final List<Segment> segments;
        private final JavaType.Variable itemParameter;
        private final J.Identifier eventCapture;

        private Candidate(Status status, List<Segment> segments,
                JavaType.Variable itemParameter, J.Identifier eventCapture) {
            this.status = status;
            this.segments = List.copyOf(segments);
            this.itemParameter = itemParameter;
            this.eventCapture = eventCapture;
        }

        public Status status() {
            return status;
        }

        public List<Segment> segments() {
            return segments;
        }

        public Optional<String> suggestedName() {
            return status == Status.MATCH ? Optional.of(HINT) : Optional.empty();
        }

        public Optional<JavaType.Variable> itemParameter() {
            return Optional.ofNullable(itemParameter);
        }

        public Optional<J.Identifier> eventCapture() {
            return Optional.ofNullable(eventCapture);
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean equivalenceAuthority() {
            return false;
        }
    }

    public static Candidate inspect(J.MethodInvocation terminal) {
        List<Segment> segments = new ArrayList<>();
        J.MethodInvocation current = terminal;
        while (true) {
            segments.add(new Segment(current));
            if (segments.size() == MAX_SEGMENTS) {
                Collections.reverse(segments);
                return unknown(segments);
            }
            if (!(current.getSelect() instanceof J.MethodInvocation predecessor)) {
                break;
            }
            current = predecessor;
        }
        Collections.reverse(segments);
        if (segments.size() != 3) {
            return unknown(segments);
        }
        J.MethodInvocation stream = segments.get(0).invocation();
        J.MethodInvocation filter = segments.get(1).invocation();
        if (!method(stream, "java.util.Collection", "stream", 0)
                || !method(filter, STREAM, "filter", 1)
                || !method(terminal, STREAM, "findFirst", 0)
                || !noArguments(stream) || !noArguments(terminal)
                || !itemContainer(stream.getType(), STREAM)
                || !itemContainer(filter.getType(), STREAM)
                || !itemContainer(terminal.getType(), "java.util.Optional")
                || !(stream.getSelect() instanceof J.Identifier collection)
                || collection.getFieldType() == null
                || !itemCollection(collection.getType())
                || filter.getArguments().size() != 1
                || !(filter.getArguments().get(0) instanceof J.Lambda lambda)
                || lambda.getParameters().isParenthesized()
                || lambda.getParameters().getParameters().size() != 1
                || !(lambda.getParameters().getParameters().get(0) instanceof J.VariableDeclarations parameter)
                || parameter.getTypeExpression() != null
                || parameter.getVariables().size() != 1
                || !(lambda.getBody() instanceof J.Binary predicate)
                || predicate.getOperator() != J.Binary.Type.And
                || !(predicate.getLeft() instanceof J.MethodInvocation contains)
                || !(predicate.getRight() instanceof J.MethodInvocation enabled)) {
            return unknown(segments);
        }
        JavaType.Variable item = parameter.getVariables().get(0).getVariableType();
        if (item == null || !TypeUtils.isOfClassType(item.getType(), ITEM)
                || !method(contains, RECTANGLE, "contains", 2)
                || contains.getType() != JavaType.Primitive.Boolean
                || !contains.getMethodType().getParameterTypes().equals(
                        List.of(JavaType.Primitive.Int, JavaType.Primitive.Int))
                || !(contains.getSelect() instanceof J.MethodInvocation bounds)
                || !method(bounds, ITEM, "getBounds", 0) || !noArguments(bounds)
                || !TypeUtils.isOfClassType(bounds.getType(), RECTANGLE)
                || !sameParameter(bounds.getSelect(), item)
                || !method(enabled, ITEM, "isEnabled", 0) || !noArguments(enabled)
                || enabled.getType() != JavaType.Primitive.Boolean
                || !sameParameter(enabled.getSelect(), item)
                || contains.getArguments().size() != 2) {
            return unknown(segments);
        }
        J.Identifier eventX = coordinate(contains.getArguments().get(0), "x");
        J.Identifier eventY = coordinate(contains.getArguments().get(1), "y");
        if (eventX == null || eventY == null
                || eventX.getFieldType() != eventY.getFieldType()) {
            return unknown(segments);
        }
        return new Candidate(Status.MATCH, segments, item, eventX);
    }

    /** Declines a hint already used by a declared or visible inherited member. */
    public static Optional<String> selectName(Candidate candidate, J.ClassDeclaration scope) {
        JavaType.FullyQualified type = scope.getType();
        if (candidate.status() != Status.MATCH || type == null) {
            return Optional.empty();
        }
        for (org.openrewrite.java.tree.Statement statement : scope.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method && HINT.equals(method.getSimpleName())) {
                return Optional.empty();
            }
            if (statement instanceof J.VariableDeclarations fields) {
                for (J.VariableDeclarations.NamedVariable field : fields.getVariables()) {
                    if (HINT.equals(field.getSimpleName())) {
                        return Optional.empty();
                    }
                }
            }
        }
        Iterator<JavaType.Method> methods = type.getVisibleMethods();
        while (methods.hasNext()) {
            if (HINT.equals(methods.next().getName())) {
                return Optional.empty();
            }
        }
        Iterator<JavaType.Variable> members = type.getVisibleMembers();
        while (members.hasNext()) {
            if (HINT.equals(members.next().getName())) {
                return Optional.empty();
            }
        }
        return candidate.suggestedName();
    }

    private static Candidate unknown(List<Segment> segments) {
        return new Candidate(Status.UNKNOWN, segments, null, null);
    }

    private static boolean method(J.MethodInvocation invocation, String owner, String name, int arity) {
        JavaType.Method method = invocation.getMethodType();
        return method != null && method.getDeclaringType() != null
                && owner.equals(method.getDeclaringType().getFullyQualifiedName())
                && name.equals(method.getName()) && name.equals(invocation.getSimpleName())
                && !method.hasFlags(Flag.Static) && method.getParameterTypes().size() == arity
                && TypeUtils.isWellFormedType(method);
    }

    private static boolean noArguments(J.MethodInvocation invocation) {
        List<Expression> arguments = invocation.getArguments();
        return arguments.isEmpty() || arguments.size() == 1 && arguments.get(0) instanceof J.Empty;
    }

    private static boolean itemContainer(JavaType type, String owner) {
        return type instanceof JavaType.Parameterized parameterized
                && owner.equals(parameterized.getFullyQualifiedName())
                && parameterized.getTypeParameters().size() == 1
                && TypeUtils.isOfClassType(parameterized.getTypeParameters().get(0), ITEM);
    }

    private static boolean itemCollection(JavaType type) {
        return type instanceof JavaType.Parameterized parameterized
                && TypeUtils.isAssignableTo("java.util.Collection", type)
                && parameterized.getTypeParameters().size() == 1
                && TypeUtils.isOfClassType(parameterized.getTypeParameters().get(0), ITEM);
    }

    private static boolean sameParameter(Expression receiver, JavaType.Variable parameter) {
        return receiver instanceof J.Identifier identifier && identifier.getFieldType() == parameter;
    }

    private static J.Identifier coordinate(Expression expression, String name) {
        if (!(expression instanceof J.FieldAccess access)
                || !name.equals(access.getSimpleName())
                || !(access.getTarget() instanceof J.Identifier event)
                || !TypeUtils.isOfClassType(event.getType(), EVENT)
                || event.getFieldType() == null
                || !TypeUtils.isOfClassType(event.getFieldType().getType(), EVENT)
                || !(event.getFieldType().getOwner() instanceof JavaType.Method)
                || access.getName().getFieldType() == null
                || !name.equals(access.getName().getFieldType().getName())
                || !TypeUtils.isOfClassType(access.getName().getFieldType().getOwner(), EVENT)
                || access.getName().getFieldType().getType() != JavaType.Primitive.Int
                || access.getType() != JavaType.Primitive.Int) {
            return null;
        }
        return event;
    }
}
