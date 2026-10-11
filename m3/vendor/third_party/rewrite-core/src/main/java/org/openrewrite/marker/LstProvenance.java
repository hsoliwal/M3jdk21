// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/LstProvenance.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import java.time.Instant;
import java.util.UUID;

public final class LstProvenance implements Marker {
    private final UUID id;
    private final Type buildToolType;
    private final String buildToolVersion;
    private final String lstSerializerVersion;
    /**
     * Nullable for backwards compatibility with older LSTs.
     */
    @Nullable
    private final Instant timestampUtc;


    public enum Type {
        Gradle, Maven, Bazel, Cli, CobolCli;
    }

    public LstProvenance(final UUID id, final Type buildToolType, final String buildToolVersion, final String lstSerializerVersion, @Nullable final Instant timestampUtc) {
        this.id = id;
        this.buildToolType = buildToolType;
        this.buildToolVersion = buildToolVersion;
        this.lstSerializerVersion = lstSerializerVersion;
        this.timestampUtc = timestampUtc;
    }

    public UUID getId() {
        return this.id;
    }

    public Type getBuildToolType() {
        return this.buildToolType;
    }

    public String getBuildToolVersion() {
        return this.buildToolVersion;
    }

    public String getLstSerializerVersion() {
        return this.lstSerializerVersion;
    }

    /**
     * Nullable for backwards compatibility with older LSTs.
     */
    @Nullable
    public Instant getTimestampUtc() {
        return this.timestampUtc;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof LstProvenance)) return false;
        final LstProvenance other = (LstProvenance) o;
        final Object this$buildToolType = this.getBuildToolType();
        final Object other$buildToolType = other.getBuildToolType();
        if (this$buildToolType == null ? other$buildToolType != null : !this$buildToolType.equals(other$buildToolType)) return false;
        final Object this$buildToolVersion = this.getBuildToolVersion();
        final Object other$buildToolVersion = other.getBuildToolVersion();
        if (this$buildToolVersion == null ? other$buildToolVersion != null : !this$buildToolVersion.equals(other$buildToolVersion)) return false;
        final Object this$lstSerializerVersion = this.getLstSerializerVersion();
        final Object other$lstSerializerVersion = other.getLstSerializerVersion();
        if (this$lstSerializerVersion == null ? other$lstSerializerVersion != null : !this$lstSerializerVersion.equals(other$lstSerializerVersion)) return false;
        final Object this$timestampUtc = this.getTimestampUtc();
        final Object other$timestampUtc = other.getTimestampUtc();
        if (this$timestampUtc == null ? other$timestampUtc != null : !this$timestampUtc.equals(other$timestampUtc)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $buildToolType = this.getBuildToolType();
        result = result * PRIME + ($buildToolType == null ? 43 : $buildToolType.hashCode());
        final Object $buildToolVersion = this.getBuildToolVersion();
        result = result * PRIME + ($buildToolVersion == null ? 43 : $buildToolVersion.hashCode());
        final Object $lstSerializerVersion = this.getLstSerializerVersion();
        result = result * PRIME + ($lstSerializerVersion == null ? 43 : $lstSerializerVersion.hashCode());
        final Object $timestampUtc = this.getTimestampUtc();
        result = result * PRIME + ($timestampUtc == null ? 43 : $timestampUtc.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "LstProvenance(id=" + this.getId() + ", buildToolType=" + this.getBuildToolType() + ", buildToolVersion=" + this.getBuildToolVersion() + ", lstSerializerVersion=" + this.getLstSerializerVersion() + ", timestampUtc=" + this.getTimestampUtc() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public LstProvenance withId(final UUID id) {
        return this.id == id ? this : new LstProvenance(id, this.buildToolType, this.buildToolVersion, this.lstSerializerVersion, this.timestampUtc);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public LstProvenance withBuildToolType(final Type buildToolType) {
        return this.buildToolType == buildToolType ? this : new LstProvenance(this.id, buildToolType, this.buildToolVersion, this.lstSerializerVersion, this.timestampUtc);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public LstProvenance withBuildToolVersion(final String buildToolVersion) {
        return this.buildToolVersion == buildToolVersion ? this : new LstProvenance(this.id, this.buildToolType, buildToolVersion, this.lstSerializerVersion, this.timestampUtc);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public LstProvenance withLstSerializerVersion(final String lstSerializerVersion) {
        return this.lstSerializerVersion == lstSerializerVersion ? this : new LstProvenance(this.id, this.buildToolType, this.buildToolVersion, lstSerializerVersion, this.timestampUtc);
    }

    /**
     * Nullable for backwards compatibility with older LSTs.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public LstProvenance withTimestampUtc(@Nullable final Instant timestampUtc) {
        return this.timestampUtc == timestampUtc ? this : new LstProvenance(this.id, this.buildToolType, this.buildToolVersion, this.lstSerializerVersion, timestampUtc);
    }
}
