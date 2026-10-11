// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/ChangeTagAttribute.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;
import java.util.regex.Pattern;

public final class ChangeTagAttribute extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change XML attribute";
    }

    @Override
    public String getDescription() {
        return "Alters XML attribute value on a specified element.";
    }

    @Option(displayName = "Element name", description = "The name of the element whose attribute\'s value is to be changed. Interpreted as an XPath expression.", example = "property")
    private final String elementName;
    @Option(displayName = "Attribute name", description = "The name of the attribute whose value is to be changed.", example = "name")
    private final String attributeName;
    @Option(displayName = "New value", description = "The new value to be used for key specified by `attributeName`, Set to null if you want to remove the attribute.", example = "newfoo.bar.attribute.value.string")
    @Nullable
    private final String newValue;
    @Option(displayName = "Old value", example = "foo.bar.attribute.value.string", required = false, description = "Only change the property value if it matches the configured `oldValue`.")
    @Nullable
    private final String oldValue;
    @Option(displayName = "Regex", description = "Default false. If true, `oldValue` will be interpreted as a Regular Expression, and capture group contents will be available in `newValue`.", required = false)
    @Nullable
    private final Boolean regex;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (new XPathMatcher(elementName).matches(getCursor())) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenElementAttribute));
                }
                return t;
            }
            public Xml.Attribute visitChosenElementAttribute(Xml.Attribute attribute) {
                if (!attribute.getKeyAsString().equals(attributeName)) {
                    return attribute;
                }
                String stringValue = attribute.getValueAsString();
                if (oldValue != null) {
                    if (Boolean.TRUE.equals(regex) && !Pattern.matches(oldValue, stringValue)) {
                        return attribute;
                    }
                    if ((regex == null || Boolean.FALSE.equals(regex)) && !stringValue.startsWith(oldValue)) {
                        return attribute;
                    }
                }
                if (newValue == null) {
                    //noinspection DataFlowIssue
                    return null;
                }
                String changedValue = oldValue != null ? (Boolean.TRUE.equals(regex) ? stringValue.replaceAll(oldValue, newValue) : stringValue.replace(oldValue, newValue)) : newValue;
                return attribute.withValue(new Xml.Attribute.Value(attribute.getId(), "", attribute.getMarkers(), attribute.getValue().getQuote(), changedValue));
            }
        };
    }

    public ChangeTagAttribute(final String elementName, final String attributeName, @Nullable final String newValue, @Nullable final String oldValue, @Nullable final Boolean regex) {
        this.elementName = elementName;
        this.attributeName = attributeName;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.regex = regex;
    }

    public String getElementName() {
        return this.elementName;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    @Nullable
    public String getNewValue() {
        return this.newValue;
    }

    @Nullable
    public String getOldValue() {
        return this.oldValue;
    }

    @Nullable
    public Boolean getRegex() {
        return this.regex;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeTagAttribute(elementName=" + this.getElementName() + ", attributeName=" + this.getAttributeName() + ", newValue=" + this.getNewValue() + ", oldValue=" + this.getOldValue() + ", regex=" + this.getRegex() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeTagAttribute)) return false;
        final ChangeTagAttribute other = (ChangeTagAttribute) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$regex = this.getRegex();
        final Object other$regex = other.getRegex();
        if (this$regex == null ? other$regex != null : !this$regex.equals(other$regex)) return false;
        final Object this$elementName = this.getElementName();
        final Object other$elementName = other.getElementName();
        if (this$elementName == null ? other$elementName != null : !this$elementName.equals(other$elementName)) return false;
        final Object this$attributeName = this.getAttributeName();
        final Object other$attributeName = other.getAttributeName();
        if (this$attributeName == null ? other$attributeName != null : !this$attributeName.equals(other$attributeName)) return false;
        final Object this$newValue = this.getNewValue();
        final Object other$newValue = other.getNewValue();
        if (this$newValue == null ? other$newValue != null : !this$newValue.equals(other$newValue)) return false;
        final Object this$oldValue = this.getOldValue();
        final Object other$oldValue = other.getOldValue();
        if (this$oldValue == null ? other$oldValue != null : !this$oldValue.equals(other$oldValue)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeTagAttribute;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $regex = this.getRegex();
        result = result * PRIME + ($regex == null ? 43 : $regex.hashCode());
        final Object $elementName = this.getElementName();
        result = result * PRIME + ($elementName == null ? 43 : $elementName.hashCode());
        final Object $attributeName = this.getAttributeName();
        result = result * PRIME + ($attributeName == null ? 43 : $attributeName.hashCode());
        final Object $newValue = this.getNewValue();
        result = result * PRIME + ($newValue == null ? 43 : $newValue.hashCode());
        final Object $oldValue = this.getOldValue();
        result = result * PRIME + ($oldValue == null ? 43 : $oldValue.hashCode());
        return result;
    }
}
