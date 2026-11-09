// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/config/RecipeDescriptor.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.config;

import org.openrewrite.Contributor;
import org.openrewrite.Maintainer;
import org.openrewrite.internal.lang.Nullable;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public final class RecipeDescriptor {
    private final String name;
    private final String displayName;
    private final String description;
    private final Set<String> tags;
    @Nullable
    private final Duration estimatedEffortPerOccurrence;
    private final List<OptionDescriptor> options;
    private final List<RecipeDescriptor> recipeList;
    private final List<DataTableDescriptor> dataTables;
    private final List<Maintainer> maintainers;
    private final List<Contributor> contributors;
    private final List<RecipeExample> examples;
    private final URI source;

    public RecipeDescriptor(final String name, final String displayName, final String description, final Set<String> tags, @Nullable final Duration estimatedEffortPerOccurrence, final List<OptionDescriptor> options, final List<RecipeDescriptor> recipeList, final List<DataTableDescriptor> dataTables, final List<Maintainer> maintainers, final List<Contributor> contributors, final List<RecipeExample> examples, final URI source) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.tags = tags;
        this.estimatedEffortPerOccurrence = estimatedEffortPerOccurrence;
        this.options = options;
        this.recipeList = recipeList;
        this.dataTables = dataTables;
        this.maintainers = maintainers;
        this.contributors = contributors;
        this.examples = examples;
        this.source = source;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    @Nullable
    public Duration getEstimatedEffortPerOccurrence() {
        return this.estimatedEffortPerOccurrence;
    }

    public List<OptionDescriptor> getOptions() {
        return this.options;
    }

    public List<RecipeDescriptor> getRecipeList() {
        return this.recipeList;
    }

    public List<DataTableDescriptor> getDataTables() {
        return this.dataTables;
    }

    public List<Maintainer> getMaintainers() {
        return this.maintainers;
    }

    public List<Contributor> getContributors() {
        return this.contributors;
    }

    public List<RecipeExample> getExamples() {
        return this.examples;
    }

    public URI getSource() {
        return this.source;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RecipeDescriptor(name=" + this.getName() + ", displayName=" + this.getDisplayName() + ", description=" + this.getDescription() + ", tags=" + this.getTags() + ", estimatedEffortPerOccurrence=" + this.getEstimatedEffortPerOccurrence() + ", options=" + this.getOptions() + ", recipeList=" + this.getRecipeList() + ", dataTables=" + this.getDataTables() + ", maintainers=" + this.getMaintainers() + ", contributors=" + this.getContributors() + ", examples=" + this.getExamples() + ", source=" + this.getSource() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RecipeDescriptor)) return false;
        final RecipeDescriptor other = (RecipeDescriptor) o;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$options = this.getOptions();
        final Object other$options = other.getOptions();
        if (this$options == null ? other$options != null : !this$options.equals(other$options)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $options = this.getOptions();
        result = result * PRIME + ($options == null ? 43 : $options.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RecipeDescriptor withRecipeList(final List<RecipeDescriptor> recipeList) {
        return this.recipeList == recipeList ? this : new RecipeDescriptor(this.name, this.displayName, this.description, this.tags, this.estimatedEffortPerOccurrence, this.options, recipeList, this.dataTables, this.maintainers, this.contributors, this.examples, this.source);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RecipeDescriptor withDataTables(final List<DataTableDescriptor> dataTables) {
        return this.dataTables == dataTables ? this : new RecipeDescriptor(this.name, this.displayName, this.description, this.tags, this.estimatedEffortPerOccurrence, this.options, this.recipeList, dataTables, this.maintainers, this.contributors, this.examples, this.source);
    }
}
