// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/HasJavaVersion.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import static java.util.Objects.requireNonNull;

public final class HasJavaVersion extends Recipe {
    @Option(displayName = "Java version", description = "An exact version number or node-style semver selector used to select the version number.", example = "17.X")
    private final String version;
    @Option(displayName = "Version check against target compatibility", description = "The source and target compatibility versions can be different. This option allows you to " + "check against the target compatibility version instead of the source compatibility version.", example = "17.X", required = false)
    @Nullable
    private final Boolean checkTargetCompatibility;

    @Override
    public String getDisplayName() {
        return "Find files compiled at a specific Java version";
    }

    @Override
    public String getDescription() {
        return "Finds Java source files matching a particular language level. This is useful especially as an " + "applicable test for other recipes.";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree != null) {
                    return tree.getMarkers().findFirst(JavaVersion.class).filter(version -> versionComparator.isValid(null, Integer.toString(Boolean.TRUE.equals(checkTargetCompatibility) ? version.getMajorReleaseVersion() : version.getMajorVersion()))).map(version -> SearchResult.found(tree)).orElse(tree);
                }
                return tree;
            }
        };
    }

    public HasJavaVersion(final String version, @Nullable final Boolean checkTargetCompatibility) {
        this.version = version;
        this.checkTargetCompatibility = checkTargetCompatibility;
    }

    public String getVersion() {
        return this.version;
    }

    @Nullable
    public Boolean getCheckTargetCompatibility() {
        return this.checkTargetCompatibility;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "HasJavaVersion(version=" + this.getVersion() + ", checkTargetCompatibility=" + this.getCheckTargetCompatibility() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof HasJavaVersion)) return false;
        final HasJavaVersion other = (HasJavaVersion) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$checkTargetCompatibility = this.getCheckTargetCompatibility();
        final Object other$checkTargetCompatibility = other.getCheckTargetCompatibility();
        if (this$checkTargetCompatibility == null ? other$checkTargetCompatibility != null : !this$checkTargetCompatibility.equals(other$checkTargetCompatibility)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof HasJavaVersion;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $checkTargetCompatibility = this.getCheckTargetCompatibility();
        result = result * PRIME + ($checkTargetCompatibility == null ? 43 : $checkTargetCompatibility.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        return result;
    }
}
