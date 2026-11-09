// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/BuildTool.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker;

import java.util.UUID;

public final class BuildTool implements Marker {
    private final UUID id;
    private final Type type;
    private final String version;


    public enum Type {
        Gradle, Maven, Bazel, ModerneCli;
    }

    public BuildTool(final UUID id, final Type type, final String version) {
        this.id = id;
        this.type = type;
        this.version = version;
    }

    public UUID getId() {
        return this.id;
    }

    public Type getType() {
        return this.type;
    }

    public String getVersion() {
        return this.version;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "BuildTool(id=" + this.getId() + ", type=" + this.getType() + ", version=" + this.getVersion() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof BuildTool)) return false;
        final BuildTool other = (BuildTool) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildTool withId(final UUID id) {
        return this.id == id ? this : new BuildTool(id, this.type, this.version);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildTool withType(final Type type) {
        return this.type == type ? this : new BuildTool(this.id, type, this.version);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildTool withVersion(final String version) {
        return this.version == version ? this : new BuildTool(this.id, this.type, version);
    }
}
