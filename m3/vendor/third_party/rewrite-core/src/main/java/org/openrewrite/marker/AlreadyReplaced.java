// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/AlreadyReplaced.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker;

import org.openrewrite.internal.lang.Nullable;
import java.util.UUID;

/**
 * Ensure that the same replacement is not applied to the same file more than once per recipe run.
 * Used to avoid the situation where replacing "a" with "ab" results in something like "abb".
 */
public final class AlreadyReplaced implements Marker {
    private final UUID id;
    private final String find;
    @Nullable
    private final String replace;

    public AlreadyReplaced(final UUID id, final String find, @Nullable final String replace) {
        this.id = id;
        this.find = find;
        this.replace = replace;
    }

    public UUID getId() {
        return this.id;
    }

    public String getFind() {
        return this.find;
    }

    @Nullable
    public String getReplace() {
        return this.replace;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof AlreadyReplaced)) return false;
        final AlreadyReplaced other = (AlreadyReplaced) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$find = this.getFind();
        final Object other$find = other.getFind();
        if (this$find == null ? other$find != null : !this$find.equals(other$find)) return false;
        final Object this$replace = this.getReplace();
        final Object other$replace = other.getReplace();
        if (this$replace == null ? other$replace != null : !this$replace.equals(other$replace)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $find = this.getFind();
        result = result * PRIME + ($find == null ? 43 : $find.hashCode());
        final Object $replace = this.getReplace();
        result = result * PRIME + ($replace == null ? 43 : $replace.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "AlreadyReplaced(id=" + this.getId() + ", find=" + this.getFind() + ", replace=" + this.getReplace() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AlreadyReplaced withId(final UUID id) {
        return this.id == id ? this : new AlreadyReplaced(id, this.find, this.replace);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AlreadyReplaced withFind(final String find) {
        return this.find == find ? this : new AlreadyReplaced(this.id, find, this.replace);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AlreadyReplaced withReplace(@Nullable final String replace) {
        return this.replace == replace ? this : new AlreadyReplaced(this.id, this.find, replace);
    }
}
