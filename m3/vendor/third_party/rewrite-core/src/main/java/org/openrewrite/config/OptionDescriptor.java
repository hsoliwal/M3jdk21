// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/config/OptionDescriptor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.config;

import org.openrewrite.internal.lang.Nullable;
import java.util.List;

public final class OptionDescriptor {
    private final String name;
    private final String type;
    @Nullable
    private final String displayName;
    @Nullable
    private final String description;
    @Nullable
    private final String example;
    @Nullable
    private final List<String> valid;
    private final boolean required;
    @Nullable
    private final Object value;

    public OptionDescriptor(final String name, final String type, @Nullable final String displayName, @Nullable final String description, @Nullable final String example, @Nullable final List<String> valid, final boolean required, @Nullable final Object value) {
        this.name = name;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.example = example;
        this.valid = valid;
        this.required = required;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    @Nullable
    public String getDisplayName() {
        return this.displayName;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @Nullable
    public String getExample() {
        return this.example;
    }

    @Nullable
    public List<String> getValid() {
        return this.valid;
    }

    public boolean isRequired() {
        return this.required;
    }

    @Nullable
    public Object getValue() {
        return this.value;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "OptionDescriptor(name=" + this.getName() + ", type=" + this.getType() + ", displayName=" + this.getDisplayName() + ", description=" + this.getDescription() + ", example=" + this.getExample() + ", valid=" + this.getValid() + ", required=" + this.isRequired() + ", value=" + this.getValue() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof OptionDescriptor)) return false;
        final OptionDescriptor other = (OptionDescriptor) o;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        return result;
    }
}
