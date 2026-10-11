// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/semver/LatestPatch.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.semver;

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

public final class LatestPatch implements VersionComparator {
    @Nullable
    private final String metadataPattern;

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        Validated<? extends VersionComparator> validated = currentVersion == null ? LatestRelease.buildLatestRelease("latest.release", metadataPattern) : TildeRange.build("~" + Semver.majorVersion(currentVersion) + "." + Semver.minorVersion(currentVersion), metadataPattern);
        if (validated.isValid()) {
            VersionComparator comparator = validated.getValue();
            if (comparator != null) {
                return comparator.isValid(currentVersion, version);
            }
        }
        return false;
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        if (currentVersion == null) {
            return new LatestRelease(null).compare(null, v1, v2);
        }
        //noinspection ConstantConditions
        return TildeRange.build("~" + Semver.majorVersion(currentVersion) + "." + Semver.minorVersion(currentVersion), metadataPattern).<VersionComparator>getValue().compare(currentVersion, v1, v2);
    }

    public static Validated<LatestPatch> build(String toVersion, @Nullable String metadataPattern) {
        return "latest.patch".equalsIgnoreCase(toVersion) ? Validated.valid("latestPatch", new LatestPatch(metadataPattern)) : Validated.invalid("latestPatch", toVersion, "not latest release");
    }

    public LatestPatch(@Nullable final String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    @Nullable
    public String getMetadataPattern() {
        return this.metadataPattern;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof LatestPatch)) return false;
        final LatestPatch other = (LatestPatch) o;
        final Object this$metadataPattern = this.getMetadataPattern();
        final Object other$metadataPattern = other.getMetadataPattern();
        if (this$metadataPattern == null ? other$metadataPattern != null : !this$metadataPattern.equals(other$metadataPattern)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $metadataPattern = this.getMetadataPattern();
        result = result * PRIME + ($metadataPattern == null ? 43 : $metadataPattern.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "LatestPatch(metadataPattern=" + this.getMetadataPattern() + ")";
    }
}
