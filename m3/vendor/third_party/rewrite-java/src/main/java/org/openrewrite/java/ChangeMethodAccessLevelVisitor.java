// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeMethodAccessLevelVisitor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;
import java.util.*;
import static java.util.Collections.emptyList;

public final class ChangeMethodAccessLevelVisitor<P> extends JavaIsoVisitor<P> {
    private static final Collection<J.Modifier.Type> EXPLICIT_ACCESS_LEVELS = Arrays.asList(J.Modifier.Type.Public, J.Modifier.Type.Private, J.Modifier.Type.Protected);
    private final MethodMatcher methodMatcher;
    @Nullable
    private final J.Modifier.Type newAccessLevel;

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if (classDecl == null) {
            return m;
        }
        if (methodMatcher.matches(method, classDecl)) {
            J.Modifier.Type currentMethodAccessLevel = m.getModifiers().stream().map(J.Modifier::getType).filter(EXPLICIT_ACCESS_LEVELS::contains).findAny().orElse(null);
            if (currentMethodAccessLevel == newAccessLevel) {
                // No changes required
                return m;
            }
            // Replace former modifier by new modifier if package-private is not involved
            if (EXPLICIT_ACCESS_LEVELS.contains(currentMethodAccessLevel) && EXPLICIT_ACCESS_LEVELS.contains(newAccessLevel)) {
                m = m.withModifiers(ListUtils.map(m.getModifiers(), mod -> mod.getType() == currentMethodAccessLevel ? mod.withType(newAccessLevel) : mod));
            } else 
            // If current access level is package-private (no modifier), add the new modifier
            if (currentMethodAccessLevel == null) {
                J.Modifier mod = new J.Modifier(Tree.randomId(), Space.build(" ", emptyList()), Markers.EMPTY, null, newAccessLevel, Collections.emptyList());
                m = m.withModifiers(ListUtils.concat(mod, m.getModifiers()));
                if (method.getModifiers().isEmpty()) {
                    J.TypeParameters typeParams = m.getPadding().getTypeParameters();
                    if (typeParams == null) {
                        TypeTree returnExpr = m.getReturnTypeExpression();
                        if (returnExpr == null) {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), m.getName().getPrefix()));
                            m = m.withName(m.getName().withPrefix(Space.format(" ")));
                        } else {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), returnExpr.getPrefix()));
                            m = m.withReturnTypeExpression(returnExpr.withPrefix(Space.format(" ")));
                        }
                    } else {
                        m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), typeParams.getPrefix()));
                        m = m.getPadding().withTypeParameters(typeParams.withPrefix(Space.format(" ")));
                    }
                } else {
                    m = m.withModifiers(ListUtils.map(m.getModifiers(), (i, mod2) -> {
                        if (i == 0) {
                            return mod2.withPrefix(method.getModifiers().get(0).getPrefix());
                        } else if (i == 1) {
                            return mod2.withPrefix(Space.format(" "));
                        }
                        return mod2;
                    }));
                }
            } else 
            // If target access level is package-private (no modifier), remove the current access level modifier
            // and copy any associated comments
            if (newAccessLevel == null) {
                final List<Comment> modifierComments = new ArrayList<>();
                List<J.Modifier> modifiers = ListUtils.map(m.getModifiers(), mod -> {
                    if (mod.getType() == currentMethodAccessLevel) {
                        modifierComments.addAll(mod.getComments());
                        return null;
                    }
                    // copy access level modifier comment to next modifier if it exists
                    if (!modifierComments.isEmpty()) {
                        J.Modifier nextModifier = mod.withComments(ListUtils.concatAll(new ArrayList<>(modifierComments), mod.getComments()));
                        modifierComments.clear();
                        return nextModifier;
                    }
                    return mod;
                });
                // if no following modifier exists, add comments to method itself
                if (!modifierComments.isEmpty()) {
                    m = m.withComments(ListUtils.concatAll(m.getComments(), modifierComments));
                }
                m = maybeAutoFormat(m, m.withModifiers(modifiers), p).withBody(m.getBody());
            }
        }
        return m;
    }

    public ChangeMethodAccessLevelVisitor(final MethodMatcher methodMatcher, @Nullable final J.Modifier.Type newAccessLevel) {
        this.methodMatcher = methodMatcher;
        this.newAccessLevel = newAccessLevel;
    }

    public MethodMatcher getMethodMatcher() {
        return this.methodMatcher;
    }

    @Nullable
    public J.Modifier.Type getNewAccessLevel() {
        return this.newAccessLevel;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeMethodAccessLevelVisitor(methodMatcher=" + this.getMethodMatcher() + ", newAccessLevel=" + this.getNewAccessLevel() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeMethodAccessLevelVisitor)) return false;
        final ChangeMethodAccessLevelVisitor<?> other = (ChangeMethodAccessLevelVisitor<?>) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$methodMatcher = this.getMethodMatcher();
        final Object other$methodMatcher = other.getMethodMatcher();
        if (this$methodMatcher == null ? other$methodMatcher != null : !this$methodMatcher.equals(other$methodMatcher)) return false;
        final Object this$newAccessLevel = this.getNewAccessLevel();
        final Object other$newAccessLevel = other.getNewAccessLevel();
        if (this$newAccessLevel == null ? other$newAccessLevel != null : !this$newAccessLevel.equals(other$newAccessLevel)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeMethodAccessLevelVisitor;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $methodMatcher = this.getMethodMatcher();
        result = result * PRIME + ($methodMatcher == null ? 43 : $methodMatcher.hashCode());
        final Object $newAccessLevel = this.getNewAccessLevel();
        result = result * PRIME + ($newAccessLevel == null ? 43 : $newAccessLevel.hashCode());
        return result;
    }
}
