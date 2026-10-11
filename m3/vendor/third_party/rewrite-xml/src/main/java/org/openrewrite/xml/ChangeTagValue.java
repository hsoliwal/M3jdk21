// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/ChangeTagValue.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

public final class ChangeTagValue extends Recipe {
    @Option(displayName = "Element name", description = "The name of the element whose value is to be changed. Interpreted as an XPath Expression.", example = "/settings/servers/server/username")
    private final String elementName;
    @Option(displayName = "Old value", description = "The old value of the tag.", required = false, example = "user")
    @Nullable
    private final String oldValue;
    @Option(displayName = "New value", description = "The new value for the tag.", example = "user")
    private final String newValue;

    @Override
    public String getDisplayName() {
        return "Change XML tag value";
    }

    @Override
    public String getDescription() {
        return "Alters the value of XML tags matching the provided expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(elementName);
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    // if either no oldValue is supplied OR oldValue equals the value of this tag
                    // then change the value to the newValue supplied
                    if (oldValue == null || oldValue.equals(tag.getValue().orElse(null))) {
                        doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    public ChangeTagValue(final String elementName, @Nullable final String oldValue, final String newValue) {
        this.elementName = elementName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getElementName() {
        return this.elementName;
    }

    @Nullable
    public String getOldValue() {
        return this.oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeTagValue(elementName=" + this.getElementName() + ", oldValue=" + this.getOldValue() + ", newValue=" + this.getNewValue() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeTagValue)) return false;
        final ChangeTagValue other = (ChangeTagValue) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$elementName = this.getElementName();
        final Object other$elementName = other.getElementName();
        if (this$elementName == null ? other$elementName != null : !this$elementName.equals(other$elementName)) return false;
        final Object this$oldValue = this.getOldValue();
        final Object other$oldValue = other.getOldValue();
        if (this$oldValue == null ? other$oldValue != null : !this$oldValue.equals(other$oldValue)) return false;
        final Object this$newValue = this.getNewValue();
        final Object other$newValue = other.getNewValue();
        if (this$newValue == null ? other$newValue != null : !this$newValue.equals(other$newValue)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeTagValue;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $elementName = this.getElementName();
        result = result * PRIME + ($elementName == null ? 43 : $elementName.hashCode());
        final Object $oldValue = this.getOldValue();
        result = result * PRIME + ($oldValue == null ? 43 : $oldValue.hashCode());
        final Object $newValue = this.getNewValue();
        result = result * PRIME + ($newValue == null ? 43 : $newValue.hashCode());
        return result;
    }
}
