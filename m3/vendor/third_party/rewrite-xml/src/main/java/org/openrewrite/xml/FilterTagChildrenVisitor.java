// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/FilterTagChildrenVisitor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.xml;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import java.util.function.Predicate;

/**
 * Filter the children to only those matching the supplied predicate.
 */
public final class FilterTagChildrenVisitor<T> extends XmlVisitor<T> {
    public final Xml.Tag scope;
    public final Predicate<Content> childTest;

    @Override
    public Xml visitTag(Xml.Tag tag, T ctx) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
        if (scope.isScope(t)) {
            t = t.withContent(ListUtils.map(t.getContent(), it -> {
                if (childTest.test(it)) {
                    return it;
                }
                return null;
            }));
        }
        return t;
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     *
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterChildren(Xml.Tag parent, Predicate<Content> childTest) {
        return filterChildren(parent, parent, childTest);
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parentScope` which somewhere contains 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterChildren(Xml.Tag parentScope, Xml.Tag parent, Predicate<Content> childTest) {
        //noinspection ConstantConditions
        return (Xml.Tag) new FilterTagChildrenVisitor<Void>(parent, childTest).visitNonNull(parentScope, null);
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     * Non-tag children, such as comments, are untouched.
     *
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterTagChildren(Xml.Tag parent, Predicate<Xml.Tag> childTest) {
        return filterTagChildren(parent, parent, childTest);
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     * Non-tag children, such as comments, are untouched.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parentScope` which somewhere contains 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterTagChildren(Xml.Tag parentScope, Xml.Tag parent, Predicate<Xml.Tag> childTest) {
        return filterChildren(parentScope, parent, child -> {
            if (child instanceof Xml.Tag) {
                return childTest.test((Xml.Tag) child);
            }
            return true;
        });
    }

    public FilterTagChildrenVisitor(final Xml.Tag scope, final Predicate<Content> childTest) {
        this.scope = scope;
        this.childTest = childTest;
    }

    public Xml.Tag getScope() {
        return this.scope;
    }

    public Predicate<Content> getChildTest() {
        return this.childTest;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FilterTagChildrenVisitor(scope=" + this.getScope() + ", childTest=" + this.getChildTest() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FilterTagChildrenVisitor)) return false;
        final FilterTagChildrenVisitor<?> other = (FilterTagChildrenVisitor<?>) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$scope = this.getScope();
        final Object other$scope = other.getScope();
        if (this$scope == null ? other$scope != null : !this$scope.equals(other$scope)) return false;
        final Object this$childTest = this.getChildTest();
        final Object other$childTest = other.getChildTest();
        if (this$childTest == null ? other$childTest != null : !this$childTest.equals(other$childTest)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FilterTagChildrenVisitor;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $scope = this.getScope();
        result = result * PRIME + ($scope == null ? 43 : $scope.hashCode());
        final Object $childTest = this.getChildTest();
        result = result * PRIME + ($childTest == null ? 43 : $childTest.hashCode());
        return result;
    }
}
