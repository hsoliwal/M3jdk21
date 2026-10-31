// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/config/CategoryDescriptor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.config;

import org.intellij.lang.annotations.Language;
import java.util.Set;

public final class CategoryDescriptor {
    public static final int LOWEST_PRECEDENCE = -1;
    public static final int DEFAULT_PRECEDENCE = 0;
    public static final int HIGHEST_PRECEDENCE = Integer.MAX_VALUE;
    @Language("markdown")
    private final String displayName;
    private final String packageName;
    @Language("markdown")
    private final String description;
    private final Set<String> tags;
    private final boolean root;
    /**
     * Defines the sort order for category descriptors of the same {@link #packageName}. The description, tags, and root values of the highest
     * priority category descriptor for a given package name will be used.
     * <p/>
     * Lower values have higher priority. The default value is {@link #LOWEST_PRECEDENCE}, indicating the lowest priority
     * (effectively deferring to any other specified priority value).
     */
    private final int priority;
    private final boolean synthetic;

    public CategoryDescriptor(@Language("markdown") final String displayName, final String packageName, @Language("markdown") final String description, final Set<String> tags, final boolean root, final int priority, final boolean synthetic) {
        this.displayName = displayName;
        this.packageName = packageName;
        this.description = description;
        this.tags = tags;
        this.root = root;
        this.priority = priority;
        this.synthetic = synthetic;
    }

    @Language("markdown")
    public String getDisplayName() {
        return this.displayName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    @Language("markdown")
    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public boolean isRoot() {
        return this.root;
    }

    /**
     * Defines the sort order for category descriptors of the same {@link #packageName}. The description, tags, and root values of the highest
     * priority category descriptor for a given package name will be used.
     * <p/>
     * Lower values have higher priority. The default value is {@link #LOWEST_PRECEDENCE}, indicating the lowest priority
     * (effectively deferring to any other specified priority value).
     */
    public int getPriority() {
        return this.priority;
    }

    public boolean isSynthetic() {
        return this.synthetic;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CategoryDescriptor)) return false;
        final CategoryDescriptor other = (CategoryDescriptor) o;
        if (this.isRoot() != other.isRoot()) return false;
        if (this.getPriority() != other.getPriority()) return false;
        if (this.isSynthetic() != other.isSynthetic()) return false;
        final Object this$displayName = this.getDisplayName();
        final Object other$displayName = other.getDisplayName();
        if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) return false;
        final Object this$packageName = this.getPackageName();
        final Object other$packageName = other.getPackageName();
        if (this$packageName == null ? other$packageName != null : !this$packageName.equals(other$packageName)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final Object this$tags = this.getTags();
        final Object other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isRoot() ? 79 : 97);
        result = result * PRIME + this.getPriority();
        result = result * PRIME + (this.isSynthetic() ? 79 : 97);
        final Object $displayName = this.getDisplayName();
        result = result * PRIME + ($displayName == null ? 43 : $displayName.hashCode());
        final Object $packageName = this.getPackageName();
        result = result * PRIME + ($packageName == null ? 43 : $packageName.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $tags = this.getTags();
        result = result * PRIME + ($tags == null ? 43 : $tags.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CategoryDescriptor(displayName=" + this.getDisplayName() + ", packageName=" + this.getPackageName() + ", description=" + this.getDescription() + ", tags=" + this.getTags() + ", root=" + this.isRoot() + ", priority=" + this.getPriority() + ", synthetic=" + this.isSynthetic() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withDisplayName(@Language("markdown") final String displayName) {
        return this.displayName == displayName ? this : new CategoryDescriptor(displayName, this.packageName, this.description, this.tags, this.root, this.priority, this.synthetic);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withPackageName(final String packageName) {
        return this.packageName == packageName ? this : new CategoryDescriptor(this.displayName, packageName, this.description, this.tags, this.root, this.priority, this.synthetic);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withDescription(@Language("markdown") final String description) {
        return this.description == description ? this : new CategoryDescriptor(this.displayName, this.packageName, description, this.tags, this.root, this.priority, this.synthetic);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withTags(final Set<String> tags) {
        return this.tags == tags ? this : new CategoryDescriptor(this.displayName, this.packageName, this.description, tags, this.root, this.priority, this.synthetic);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withRoot(final boolean root) {
        return this.root == root ? this : new CategoryDescriptor(this.displayName, this.packageName, this.description, this.tags, root, this.priority, this.synthetic);
    }

    /**
     * Defines the sort order for category descriptors of the same {@link #packageName}. The description, tags, and root values of the highest
     * priority category descriptor for a given package name will be used.
     * <p/>
     * Lower values have higher priority. The default value is {@link #LOWEST_PRECEDENCE}, indicating the lowest priority
     * (effectively deferring to any other specified priority value).
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withPriority(final int priority) {
        return this.priority == priority ? this : new CategoryDescriptor(this.displayName, this.packageName, this.description, this.tags, this.root, priority, this.synthetic);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CategoryDescriptor withSynthetic(final boolean synthetic) {
        return this.synthetic == synthetic ? this : new CategoryDescriptor(this.displayName, this.packageName, this.description, this.tags, this.root, this.priority, synthetic);
    }
}
