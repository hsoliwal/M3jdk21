// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/OutdatedSerializer.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker;

import java.util.UUID;

public final class OutdatedSerializer implements Marker {
    private final UUID id;
    private final String language;
    private final String minimumVersion;
    private final String actualVersion;

    public OutdatedSerializer(final UUID id, final String language, final String minimumVersion, final String actualVersion) {
        this.id = id;
        this.language = language;
        this.minimumVersion = minimumVersion;
        this.actualVersion = actualVersion;
    }

    public UUID getId() {
        return this.id;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getMinimumVersion() {
        return this.minimumVersion;
    }

    public String getActualVersion() {
        return this.actualVersion;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "OutdatedSerializer(id=" + this.getId() + ", language=" + this.getLanguage() + ", minimumVersion=" + this.getMinimumVersion() + ", actualVersion=" + this.getActualVersion() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof OutdatedSerializer)) return false;
        final OutdatedSerializer other = (OutdatedSerializer) o;
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
    public OutdatedSerializer withId(final UUID id) {
        return this.id == id ? this : new OutdatedSerializer(id, this.language, this.minimumVersion, this.actualVersion);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OutdatedSerializer withLanguage(final String language) {
        return this.language == language ? this : new OutdatedSerializer(this.id, language, this.minimumVersion, this.actualVersion);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OutdatedSerializer withMinimumVersion(final String minimumVersion) {
        return this.minimumVersion == minimumVersion ? this : new OutdatedSerializer(this.id, this.language, minimumVersion, this.actualVersion);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OutdatedSerializer withActualVersion(final String actualVersion) {
        return this.actualVersion == actualVersion ? this : new OutdatedSerializer(this.id, this.language, this.minimumVersion, actualVersion);
    }
}
