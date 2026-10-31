// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/marker/JavaVersion.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.marker;

import org.openrewrite.marker.Marker;
import java.util.UUID;

public final class JavaVersion implements Marker {
    private final UUID id;
    private final String createdBy;
    private final String vmVendor;
    private final String sourceCompatibility;
    private final String targetCompatibility;

    public int getMajorVersion() {
        try {
            return Integer.parseInt(normalize(sourceCompatibility));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @return The major --release version.
     */
    public int getMajorReleaseVersion() {
        try {
            return Integer.parseInt(normalize(targetCompatibility));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String normalize(String version) {
        if (!version.contains(".")) {
            return version;
        }
        if (version.startsWith("1.")) {
            String removePrefix = version.substring(version.indexOf(".") + 1);
            return normalize(removePrefix);
        } else {
            return version.substring(0, version.indexOf("."));
        }
    }

    public JavaVersion(final UUID id, final String createdBy, final String vmVendor, final String sourceCompatibility, final String targetCompatibility) {
        this.id = id;
        this.createdBy = createdBy;
        this.vmVendor = vmVendor;
        this.sourceCompatibility = sourceCompatibility;
        this.targetCompatibility = targetCompatibility;
    }

    public UUID getId() {
        return this.id;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getVmVendor() {
        return this.vmVendor;
    }

    public String getSourceCompatibility() {
        return this.sourceCompatibility;
    }

    public String getTargetCompatibility() {
        return this.targetCompatibility;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "JavaVersion(id=" + this.getId() + ", createdBy=" + this.getCreatedBy() + ", vmVendor=" + this.getVmVendor() + ", sourceCompatibility=" + this.getSourceCompatibility() + ", targetCompatibility=" + this.getTargetCompatibility() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaVersion)) return false;
        final JavaVersion other = (JavaVersion) o;
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
    public JavaVersion withId(final UUID id) {
        return this.id == id ? this : new JavaVersion(id, this.createdBy, this.vmVendor, this.sourceCompatibility, this.targetCompatibility);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaVersion withCreatedBy(final String createdBy) {
        return this.createdBy == createdBy ? this : new JavaVersion(this.id, createdBy, this.vmVendor, this.sourceCompatibility, this.targetCompatibility);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaVersion withVmVendor(final String vmVendor) {
        return this.vmVendor == vmVendor ? this : new JavaVersion(this.id, this.createdBy, vmVendor, this.sourceCompatibility, this.targetCompatibility);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaVersion withSourceCompatibility(final String sourceCompatibility) {
        return this.sourceCompatibility == sourceCompatibility ? this : new JavaVersion(this.id, this.createdBy, this.vmVendor, sourceCompatibility, this.targetCompatibility);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaVersion withTargetCompatibility(final String targetCompatibility) {
        return this.targetCompatibility == targetCompatibility ? this : new JavaVersion(this.id, this.createdBy, this.vmVendor, this.sourceCompatibility, targetCompatibility);
    }
}
