// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/marker/JavaProject.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.marker;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import java.util.UUID;

public final class JavaProject implements Marker {
    private final UUID id;
    private final String projectName;
    @Nullable
    private final Publication publication;


    public static class Publication {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public Publication(final String groupId, final String artifactId, final String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return this.groupId;
        }

        public String getArtifactId() {
            return this.artifactId;
        }

        public String getVersion() {
            return this.version;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof JavaProject.Publication)) return false;
            final JavaProject.Publication other = (JavaProject.Publication) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$groupId = this.getGroupId();
            final Object other$groupId = other.getGroupId();
            if (this$groupId == null ? other$groupId != null : !this$groupId.equals(other$groupId)) return false;
            final Object this$artifactId = this.getArtifactId();
            final Object other$artifactId = other.getArtifactId();
            if (this$artifactId == null ? other$artifactId != null : !this$artifactId.equals(other$artifactId)) return false;
            final Object this$version = this.getVersion();
            final Object other$version = other.getVersion();
            if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof JavaProject.Publication;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $groupId = this.getGroupId();
            result = result * PRIME + ($groupId == null ? 43 : $groupId.hashCode());
            final Object $artifactId = this.getArtifactId();
            result = result * PRIME + ($artifactId == null ? 43 : $artifactId.hashCode());
            final Object $version = this.getVersion();
            result = result * PRIME + ($version == null ? 43 : $version.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "JavaProject.Publication(groupId=" + this.getGroupId() + ", artifactId=" + this.getArtifactId() + ", version=" + this.getVersion() + ")";
        }
    }

    public JavaProject(final UUID id, final String projectName, @Nullable final Publication publication) {
        this.id = id;
        this.projectName = projectName;
        this.publication = publication;
    }

    public UUID getId() {
        return this.id;
    }

    public String getProjectName() {
        return this.projectName;
    }

    @Nullable
    public Publication getPublication() {
        return this.publication;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "JavaProject(id=" + this.getId() + ", projectName=" + this.getProjectName() + ", publication=" + this.getPublication() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaProject)) return false;
        final JavaProject other = (JavaProject) o;
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
    public JavaProject withId(final UUID id) {
        return this.id == id ? this : new JavaProject(id, this.projectName, this.publication);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaProject withProjectName(final String projectName) {
        return this.projectName == projectName ? this : new JavaProject(this.id, projectName, this.publication);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaProject withPublication(@Nullable final Publication publication) {
        return this.publication == publication ? this : new JavaProject(this.id, this.projectName, publication);
    }
}
