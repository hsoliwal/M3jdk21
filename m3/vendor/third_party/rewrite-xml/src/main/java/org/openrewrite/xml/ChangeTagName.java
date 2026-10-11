// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/ChangeTagName.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

public final class ChangeTagName extends Recipe {
    @Option(displayName = "Element name", description = "The name of the element whose attribute\'s value is to be changed. Interpreted as an XPath expression.", example = "/settings/servers/server/username")
    private final String elementName;
    @Option(displayName = "New name", description = "The new name for the tag.", example = "user")
    private final String newName;

    @Override
    public String getDisplayName() {
        return "Change XML tag name";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", elementName, newName);
    }

    @Override
    public String getDescription() {
        return "Alters the name of XML tags matching the provided expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(elementName);
            @Override
            public Xml.Tag visitTag(final Xml.Tag tag, final ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    return tag.withName(newName);
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    public ChangeTagName(final String elementName, final String newName) {
        this.elementName = elementName;
        this.newName = newName;
    }

    public String getElementName() {
        return this.elementName;
    }

    public String getNewName() {
        return this.newName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeTagName(elementName=" + this.getElementName() + ", newName=" + this.getNewName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeTagName)) return false;
        final ChangeTagName other = (ChangeTagName) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$elementName = this.getElementName();
        final Object other$elementName = other.getElementName();
        if (this$elementName == null ? other$elementName != null : !this$elementName.equals(other$elementName)) return false;
        final Object this$newName = this.getNewName();
        final Object other$newName = other.getNewName();
        if (this$newName == null ? other$newName != null : !this$newName.equals(other$newName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeTagName;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $elementName = this.getElementName();
        result = result * PRIME + ($elementName == null ? 43 : $elementName.hashCode());
        final Object $newName = this.getNewName();
        result = result * PRIME + ($newName == null ? 43 : $newName.hashCode());
        return result;
    }
}
