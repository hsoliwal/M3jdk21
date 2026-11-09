// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/internal/template/TemplateParameter.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.internal.template;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import java.util.UUID;

public final class TemplateParameter implements Marker {
    private final UUID id;
    private final JavaType type;
    @Nullable
    private final String name;

    public TemplateParameter(final UUID id, final JavaType type, @Nullable final String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public UUID getId() {
        return this.id;
    }

    public JavaType getType() {
        return this.type;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TemplateParameter)) return false;
        final TemplateParameter other = (TemplateParameter) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TemplateParameter(id=" + this.getId() + ", type=" + this.getType() + ", name=" + this.getName() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TemplateParameter withId(final UUID id) {
        return this.id == id ? this : new TemplateParameter(id, this.type, this.name);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TemplateParameter withType(final JavaType type) {
        return this.type == type ? this : new TemplateParameter(this.id, type, this.name);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TemplateParameter withName(@Nullable final String name) {
        return this.name == name ? this : new TemplateParameter(this.id, this.type, name);
    }
}
