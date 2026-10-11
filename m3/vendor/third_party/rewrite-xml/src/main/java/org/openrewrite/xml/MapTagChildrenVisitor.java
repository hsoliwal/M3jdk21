// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/MapTagChildrenVisitor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Applies a transformation to the children of the specified tag.
 */
public final class MapTagChildrenVisitor<T> extends XmlVisitor<T> {
    public final Xml.Tag scope;
    public final UnaryOperator<Content> map;

    @Override
    public Xml visitTag(Xml.Tag tag, T ctx) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
        if (scope.isScope(t)) {
            //noinspection unchecked
            t = t.withContent(ListUtils.map((List<Content>) t.getContent(), map));
        }
        return t;
    }

    /**
     * Transform the children of a tag with the supplied mapping function.
     *
     * @param parent the tag whose direct child elements are to be transformed by 'map'.
     * @param map the function used to transform the direct child elements of 'parent'.
     * @return 'parent' with its children transformed by 'map'
     */
    public static Xml.Tag mapChildren(Xml.Tag parent, UnaryOperator<Content> map) {
        return mapChildren(parent, parent, map);
    }

    /**
     * Transform the children of a tag with the supplied mapping function.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag whose direct child elements are to be transformed by 'map'.
     * @param map the function used to transform the direct child elements of 'parent'.
     * @return 'parentScope' which somewhere contains 'parent' with its children transformed by 'map'
     */
    public static Xml.Tag mapChildren(Xml.Tag parentScope, Xml.Tag parent, UnaryOperator<Content> map) {
        //noinspection ConstantConditions
        return (Xml.Tag) new MapTagChildrenVisitor<Void>(parent, map).visitNonNull(parentScope, null);
    }

    public static Xml.Tag mapTagChildren(Xml.Tag parent, UnaryOperator<Xml.Tag> map) {
        return mapTagChildren(parent, parent, map);
    }

    public static Xml.Tag mapTagChildren(Xml.Tag parentScope, Xml.Tag parent, UnaryOperator<Xml.Tag> map) {
        return mapChildren(parentScope, parent, content -> {
            if (content instanceof Xml.Tag) {
                return map.apply((Xml.Tag) content);
            }
            return content;
        });
    }

    public MapTagChildrenVisitor(final Xml.Tag scope, final UnaryOperator<Content> map) {
        this.scope = scope;
        this.map = map;
    }

    public Xml.Tag getScope() {
        return this.scope;
    }

    public UnaryOperator<Content> getMap() {
        return this.map;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "MapTagChildrenVisitor(scope=" + this.getScope() + ", map=" + this.getMap() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof MapTagChildrenVisitor)) return false;
        final MapTagChildrenVisitor<?> other = (MapTagChildrenVisitor<?>) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$scope = this.getScope();
        final Object other$scope = other.getScope();
        if (this$scope == null ? other$scope != null : !this$scope.equals(other$scope)) return false;
        final Object this$map = this.getMap();
        final Object other$map = other.getMap();
        if (this$map == null ? other$map != null : !this$map.equals(other$map)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof MapTagChildrenVisitor;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $scope = this.getScope();
        result = result * PRIME + ($scope == null ? 43 : $scope.hashCode());
        final Object $map = this.getMap();
        result = result * PRIME + ($map == null ? 43 : $map.hashCode());
        return result;
    }
}
