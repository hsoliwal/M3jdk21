// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/RecipesThatMadeChanges.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import java.util.*;

public final class RecipesThatMadeChanges implements Marker {
    private final UUID id;
    private final Collection<List<Recipe>> recipes;

    public static RecipesThatMadeChanges create(List<Recipe> recipeStack) {
        List<List<Recipe>> recipeStackList = new ArrayList<>(1);
        recipeStackList.add(recipeStack);
        return new RecipesThatMadeChanges(Tree.randomId(), recipeStackList);
    }

    public RecipesThatMadeChanges(final UUID id, final Collection<List<Recipe>> recipes) {
        this.id = id;
        this.recipes = recipes;
    }

    public UUID getId() {
        return this.id;
    }

    public Collection<List<Recipe>> getRecipes() {
        return this.recipes;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RecipesThatMadeChanges(id=" + this.getId() + ", recipes=" + this.getRecipes() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RecipesThatMadeChanges)) return false;
        final RecipesThatMadeChanges other = (RecipesThatMadeChanges) o;
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
    public RecipesThatMadeChanges withId(final UUID id) {
        return this.id == id ? this : new RecipesThatMadeChanges(id, this.recipes);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RecipesThatMadeChanges withRecipes(final Collection<List<Recipe>> recipes) {
        return this.recipes == recipes ? this : new RecipesThatMadeChanges(this.id, recipes);
    }
}
