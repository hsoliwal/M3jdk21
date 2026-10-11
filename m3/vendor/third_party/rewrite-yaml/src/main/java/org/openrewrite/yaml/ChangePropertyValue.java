// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/ChangePropertyValue.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.yaml;

import org.openrewrite.*;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ChangePropertyValue extends Recipe {
    @Option(displayName = "Property key", description = "The key to look for. Supports glob patterns.", example = "management.metrics.binders.*.enabled")
    private final String propertyKey;
    @Option(displayName = "New value", description = "The new value to be used for key specified by `propertyKey`.")
    private final String newValue;
    @Option(displayName = "Old value", required = false, description = "Only change the property value if it matches the configured `oldValue`.")
    @Nullable
    private final String oldValue;
    @Option(displayName = "Regex", description = "Default `false`. If enabled, `oldValue` will be interpreted as a Regular Expression, " + "to replace only all parts that match the regex. Capturing group can be used in `newValue`.", required = false)
    @Nullable
    private final Boolean regex;
    @Option(displayName = "Use relaxed binding", description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " + "rules. Default is `true`. Set to `false`  to use exact matching.", required = false)
    @Nullable
    private final Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Change YAML property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", propertyKey, newValue);
    }

    @Override
    public String getDescription() {
        return "Change a YAML property. Expects dot notation for nested YAML mappings, similar to how Spring Boot interprets `application.yml` files.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("oldValue", "is required if `regex` is enabled", oldValue, value -> !(Boolean.TRUE.equals(regex) && StringUtils.isNullOrEmpty(value))));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                String prop = getProperty(getCursor());
                if (matchesPropertyKey(prop) && matchesOldValue(e.getValue())) {
                    Yaml.Scalar updatedValue = updateValue(e.getValue());
                    if (updatedValue != null) {
                        e = e.withValue(updatedValue);
                    }
                }
                return e;
            }
        };
    }

    // returns null if value should not change
    @Nullable
    private Yaml.Scalar updateValue(Yaml.Block value) {
        if (!(value instanceof Yaml.Scalar)) {
            return null;
        }
        Yaml.Scalar scalar = (Yaml.Scalar) value;
        Yaml.Scalar newScalar = scalar.withValue(Boolean.TRUE.equals(regex) ? scalar.getValue().replaceAll(Objects.requireNonNull(oldValue), newValue) : newValue);
        return scalar.getValue().equals(newScalar.getValue()) ? null : newScalar;
    }

    private boolean matchesPropertyKey(String prop) {
        return !Boolean.FALSE.equals(relaxedBinding) ? NameCaseConvention.matchesGlobRelaxedBinding(prop, propertyKey) : StringUtils.matchesGlob(prop, propertyKey);
    }

    private boolean matchesOldValue(Yaml.Block value) {
        if (!(value instanceof Yaml.Scalar)) {
            return false;
        }
        Yaml.Scalar scalar = (Yaml.Scalar) value;
        return StringUtils.isNullOrEmpty(oldValue) || (Boolean.TRUE.equals(regex) ? Pattern.compile(oldValue).matcher(scalar.getValue()).find() : scalar.getValue().equals(oldValue));
    }

    private static String getProperty(Cursor cursor) {
        StringBuilder asProperty = new StringBuilder();
        Iterator<Object> path = cursor.getPath();
        int i = 0;
        while (path.hasNext()) {
            Object next = path.next();
            if (next instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) next;
                if (i++ > 0) {
                    asProperty.insert(0, '.');
                }
                asProperty.insert(0, entry.getKey().getValue());
            }
        }
        return asProperty.toString();
    }

    public ChangePropertyValue(final String propertyKey, final String newValue, @Nullable final String oldValue, @Nullable final Boolean regex, @Nullable final Boolean relaxedBinding) {
        this.propertyKey = propertyKey;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.regex = regex;
        this.relaxedBinding = relaxedBinding;
    }

    public String getPropertyKey() {
        return this.propertyKey;
    }

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

    @Nullable
    public Boolean getRelaxedBinding() {
        return this.relaxedBinding;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangePropertyValue(propertyKey=" + this.getPropertyKey() + ", newValue=" + this.getNewValue() + ", oldValue=" + this.getOldValue() + ", regex=" + this.getRegex() + ", relaxedBinding=" + this.getRelaxedBinding() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangePropertyValue)) return false;
        final ChangePropertyValue other = (ChangePropertyValue) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$regex = this.getRegex();
        final Object other$regex = other.getRegex();
        if (this$regex == null ? other$regex != null : !this$regex.equals(other$regex)) return false;
        final Object this$relaxedBinding = this.getRelaxedBinding();
        final Object other$relaxedBinding = other.getRelaxedBinding();
        if (this$relaxedBinding == null ? other$relaxedBinding != null : !this$relaxedBinding.equals(other$relaxedBinding)) return false;
        final Object this$propertyKey = this.getPropertyKey();
        final Object other$propertyKey = other.getPropertyKey();
        if (this$propertyKey == null ? other$propertyKey != null : !this$propertyKey.equals(other$propertyKey)) return false;
        final Object this$newValue = this.getNewValue();
        final Object other$newValue = other.getNewValue();
        if (this$newValue == null ? other$newValue != null : !this$newValue.equals(other$newValue)) return false;
        final Object this$oldValue = this.getOldValue();
        final Object other$oldValue = other.getOldValue();
        if (this$oldValue == null ? other$oldValue != null : !this$oldValue.equals(other$oldValue)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangePropertyValue;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $regex = this.getRegex();
        result = result * PRIME + ($regex == null ? 43 : $regex.hashCode());
        final Object $relaxedBinding = this.getRelaxedBinding();
        result = result * PRIME + ($relaxedBinding == null ? 43 : $relaxedBinding.hashCode());
        final Object $propertyKey = this.getPropertyKey();
        result = result * PRIME + ($propertyKey == null ? 43 : $propertyKey.hashCode());
        final Object $newValue = this.getNewValue();
        result = result * PRIME + ($newValue == null ? 43 : $newValue.hashCode());
        final Object $oldValue = this.getOldValue();
        result = result * PRIME + ($oldValue == null ? 43 : $oldValue.hashCode());
        return result;
    }
}
