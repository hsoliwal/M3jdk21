// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/config/DeclarativeRecipe.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import static java.util.Collections.emptyList;
import static org.openrewrite.Validated.invalid;

public class DeclarativeRecipe extends Recipe {
    private final String name;
    @Language("markdown")
    private final String displayName;
    @Language("markdown")
    private final String description;
    private final Set<String> tags;
    @Nullable
    private final Duration estimatedEffortPerOccurrence;
    private final URI source;
    private final boolean causesAnotherCycle;

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle || super.causesAnotherCycle();
    }

    private final List<Maintainer> maintainers;
    private final List<Recipe> uninitializedRecipes = new ArrayList<>();
    private List<Recipe> recipeList = new ArrayList<>();
    private final List<Recipe> uninitializedPreconditions = new ArrayList<>();
    private List<Recipe> preconditions = new ArrayList<>();

    public void addPrecondition(Recipe recipe) {
        uninitializedPreconditions.add(recipe);
    }

    @JsonIgnore
    private Validated<Object> validation = Validated.none();
    @JsonIgnore
    private Validated<Object> initValidation = null;

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ? super.getEstimatedEffortPerOccurrence() : estimatedEffortPerOccurrence;
    }

    public void initialize(Collection<Recipe> availableRecipes, Map<String, List<Contributor>> recipeToContributors) {
        initValidation = Validated.none();
        initialize(uninitializedRecipes, recipeList, availableRecipes, recipeToContributors);
        initialize(uninitializedPreconditions, preconditions, availableRecipes, recipeToContributors);
    }

    private void initialize(List<Recipe> uninitialized, List<Recipe> initialized, Collection<Recipe> availableRecipes, Map<String, List<Contributor>> recipeToContributors) {
        initialized.clear();
        for (int i = 0; i < uninitialized.size(); i++) {
            Recipe recipe = uninitialized.get(i);
            if (recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                Optional<Recipe> next = availableRecipes.stream().filter(r -> recipeFqn.equals(r.getName())).findAny();
                if (next.isPresent()) {
                    Recipe subRecipe = next.get();
                    if (subRecipe instanceof DeclarativeRecipe) {
                        ((DeclarativeRecipe) subRecipe).initialize(availableRecipes, recipeToContributors);
                    }
                    initialized.add(subRecipe);
                } else {
                    initValidation = initValidation.and(invalid(name + ".recipeList" + "[" + i + "] (in " + source + ")", recipeFqn, "recipe \'" + recipeFqn + "\' does not exist.", null));
                }
            } else {
                recipe.setContributors(recipeToContributors.getOrDefault(recipe.getName(), emptyList()));
                if (recipe instanceof DeclarativeRecipe) {
                    ((DeclarativeRecipe) recipe).initialize(availableRecipes, recipeToContributors);
                }
                initialized.add(recipe);
            }
        }
    }


    static final class PreconditionBellwether extends Recipe {
        @Override
        public String getDisplayName() {
            return "Precondition bellwether";
        }

        @Override
        public String getDescription() {
            return "Evaluates a precondition and makes that result available to the preconditions of other recipes. " + "\"bellwether\", noun - One that serves as a leader or as a leading indicator of future trends. ";
        }

        private final TreeVisitor<?, ExecutionContext> precondition;
        private transient boolean preconditionApplicable;

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                    return precondition.isAcceptable(sourceFile, ctx);
                }
                @Override
                @Nullable
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    Tree t = precondition.visit(tree, ctx);
                    preconditionApplicable = t != tree;
                    return tree;
                }
            };
        }

        public TreeVisitor<?, ExecutionContext> getPrecondition() {
            return this.precondition;
        }

        public boolean isPreconditionApplicable() {
            return this.preconditionApplicable;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeclarativeRecipe.PreconditionBellwether(precondition=" + this.getPrecondition() + ", preconditionApplicable=" + this.isPreconditionApplicable() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DeclarativeRecipe.PreconditionBellwether)) return false;
            final DeclarativeRecipe.PreconditionBellwether other = (DeclarativeRecipe.PreconditionBellwether) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$precondition = this.getPrecondition();
            final Object other$precondition = other.getPrecondition();
            if (this$precondition == null ? other$precondition != null : !this$precondition.equals(other$precondition)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof DeclarativeRecipe.PreconditionBellwether;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $precondition = this.getPrecondition();
            result = result * PRIME + ($precondition == null ? 43 : $precondition.hashCode());
            return result;
        }

        public PreconditionBellwether(final TreeVisitor<?, ExecutionContext> precondition) {
            this.precondition = precondition;
        }
    }


    static final class BellwetherDecoratedRecipe extends Recipe {
        private final DeclarativeRecipe.PreconditionBellwether bellwether;
        private final Recipe delegate;

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(bellwether.isPreconditionApplicable(), delegate.getVisitor());
        }

        @Override
        public List<Recipe> getRecipeList() {
            return decorateWithPreconditionBellwether(bellwether, delegate.getRecipeList());
        }

        public BellwetherDecoratedRecipe(final DeclarativeRecipe.PreconditionBellwether bellwether, final Recipe delegate) {
            this.bellwether = bellwether;
            this.delegate = delegate;
        }

        public DeclarativeRecipe.PreconditionBellwether getBellwether() {
            return this.bellwether;
        }

        public Recipe getDelegate() {
            return this.delegate;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeclarativeRecipe.BellwetherDecoratedRecipe(bellwether=" + this.getBellwether() + ", delegate=" + this.getDelegate() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DeclarativeRecipe.BellwetherDecoratedRecipe)) return false;
            final DeclarativeRecipe.BellwetherDecoratedRecipe other = (DeclarativeRecipe.BellwetherDecoratedRecipe) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$bellwether = this.getBellwether();
            final Object other$bellwether = other.getBellwether();
            if (this$bellwether == null ? other$bellwether != null : !this$bellwether.equals(other$bellwether)) return false;
            final Object this$delegate = this.getDelegate();
            final Object other$delegate = other.getDelegate();
            if (this$delegate == null ? other$delegate != null : !this$delegate.equals(other$delegate)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof DeclarativeRecipe.BellwetherDecoratedRecipe;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $bellwether = this.getBellwether();
            result = result * PRIME + ($bellwether == null ? 43 : $bellwether.hashCode());
            final Object $delegate = this.getDelegate();
            result = result * PRIME + ($delegate == null ? 43 : $delegate.hashCode());
            return result;
        }
    }


    static final class BellwetherDecoratedScanningRecipe<T> extends ScanningRecipe<T> {
        private final DeclarativeRecipe.PreconditionBellwether bellwether;
        private final ScanningRecipe<T> delegate;

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public T getInitialValue(ExecutionContext ctx) {
            return delegate.getInitialValue(ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(T acc) {
            return delegate.getScanner(acc);
        }

        @Override
        public Collection<? extends SourceFile> generate(T acc, ExecutionContext ctx) {
            return delegate.generate(acc, ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(T acc) {
            return Preconditions.check(bellwether.isPreconditionApplicable(), delegate.getVisitor(acc));
        }

        @Override
        public List<Recipe> getRecipeList() {
            return decorateWithPreconditionBellwether(bellwether, delegate.getRecipeList());
        }

        public BellwetherDecoratedScanningRecipe(final DeclarativeRecipe.PreconditionBellwether bellwether, final ScanningRecipe<T> delegate) {
            this.bellwether = bellwether;
            this.delegate = delegate;
        }

        public DeclarativeRecipe.PreconditionBellwether getBellwether() {
            return this.bellwether;
        }

        public ScanningRecipe<T> getDelegate() {
            return this.delegate;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeclarativeRecipe.BellwetherDecoratedScanningRecipe(bellwether=" + this.getBellwether() + ", delegate=" + this.getDelegate() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DeclarativeRecipe.BellwetherDecoratedScanningRecipe)) return false;
            final DeclarativeRecipe.BellwetherDecoratedScanningRecipe<?> other = (DeclarativeRecipe.BellwetherDecoratedScanningRecipe<?>) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$bellwether = this.getBellwether();
            final Object other$bellwether = other.getBellwether();
            if (this$bellwether == null ? other$bellwether != null : !this$bellwether.equals(other$bellwether)) return false;
            final Object this$delegate = this.getDelegate();
            final Object other$delegate = other.getDelegate();
            if (this$delegate == null ? other$delegate != null : !this$delegate.equals(other$delegate)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof DeclarativeRecipe.BellwetherDecoratedScanningRecipe;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $bellwether = this.getBellwether();
            result = result * PRIME + ($bellwether == null ? 43 : $bellwether.hashCode());
            final Object $delegate = this.getDelegate();
            result = result * PRIME + ($delegate == null ? 43 : $delegate.hashCode());
            return result;
        }
    }

    @Override
    public final List<Recipe> getRecipeList() {
        if (preconditions.isEmpty()) {
            return recipeList;
        }
        TreeVisitor<?, ExecutionContext> andPreconditions = null;
        for (Recipe precondition : preconditions) {
            if (isScanningRecipe(precondition)) {
                throw new IllegalArgumentException(getName() + " declares the ScanningRecipe " + precondition.getName() + " as a precondition." + "ScanningRecipe cannot be used as Preconditions.");
            }
            if (andPreconditions == null) {
                andPreconditions = precondition.getVisitor();
            } else {
                andPreconditions = Preconditions.and(andPreconditions, precondition.getVisitor());
            }
        }
        PreconditionBellwether bellwether = new PreconditionBellwether(andPreconditions);
        List<Recipe> recipeListWithBellwether = new ArrayList<>(recipeList.size() + 1);
        recipeListWithBellwether.add(bellwether);
        recipeListWithBellwether.addAll(decorateWithPreconditionBellwether(bellwether, recipeList));
        return recipeListWithBellwether;
    }

    private static boolean isScanningRecipe(Recipe recipe) {
        if (recipe instanceof ScanningRecipe) {
            return true;
        }
        for (Recipe r : recipe.getRecipeList()) {
            if (isScanningRecipe(r)) {
                return true;
            }
        }
        return false;
    }

    private static List<Recipe> decorateWithPreconditionBellwether(PreconditionBellwether bellwether, List<Recipe> recipeList) {
        List<Recipe> mappedRecipeList = new ArrayList<>(recipeList.size());
        for (Recipe recipe : recipeList) {
            if (recipe instanceof ScanningRecipe) {
                mappedRecipeList.add(new BellwetherDecoratedScanningRecipe<>(bellwether, (ScanningRecipe<?>) recipe));
            } else {
                mappedRecipeList.add(new BellwetherDecoratedRecipe(bellwether, recipe));
            }
        }
        return mappedRecipeList;
    }

    public void addUninitialized(Recipe recipe) {
        uninitializedRecipes.add(recipe);
    }

    public void addUninitialized(String recipeName) {
        uninitializedRecipes.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
    }

    public void addUninitializedPrecondition(Recipe recipe) {
        uninitializedPreconditions.add(recipe);
    }

    public void addUninitializedPrecondition(String recipeName) {
        uninitializedPreconditions.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
    }

    public void addValidation(Validated<Object> validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated<Object> validate() {
        return Validated.<Object>test("initialization", "initialize(..) must be called on DeclarativeRecipe prior to use.", this, r -> initValidation != null).and(validation).and(initValidation);
    }


    private static final class LazyLoadedRecipe extends Recipe {
        private final String recipeFqn;

        @Override
        public String getDisplayName() {
            return "Lazy loaded recipe";
        }

        @Override
        public String getDescription() {
            return "Recipe that is loaded lazily.";
        }

        public LazyLoadedRecipe(final String recipeFqn) {
            this.recipeFqn = recipeFqn;
        }

        public String getRecipeFqn() {
            return this.recipeFqn;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeclarativeRecipe.LazyLoadedRecipe(recipeFqn=" + this.getRecipeFqn() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DeclarativeRecipe.LazyLoadedRecipe)) return false;
            final DeclarativeRecipe.LazyLoadedRecipe other = (DeclarativeRecipe.LazyLoadedRecipe) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$recipeFqn = this.getRecipeFqn();
            final Object other$recipeFqn = other.getRecipeFqn();
            if (this$recipeFqn == null ? other$recipeFqn != null : !this$recipeFqn.equals(other$recipeFqn)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof DeclarativeRecipe.LazyLoadedRecipe;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $recipeFqn = this.getRecipeFqn();
            result = result * PRIME + ($recipeFqn == null ? 43 : $recipeFqn.hashCode());
            return result;
        }
    }

    @Override
    protected RecipeDescriptor createRecipeDescriptor() {
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe childRecipe : getRecipeList()) {
            recipeList.add(childRecipe.getDescriptor());
        }
        return new RecipeDescriptor(getName(), getDisplayName(), getDescription(), getTags(), getEstimatedEffortPerOccurrence(), emptyList(), recipeList, getDataTableDescriptors(), getMaintainers(), getContributors(), getExamples(), source);
    }


    private static final class NameEmail {
        private final String name;
        private final String email;

        public NameEmail(final String name, final String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return this.name;
        }

        public String getEmail() {
            return this.email;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DeclarativeRecipe.NameEmail)) return false;
            final DeclarativeRecipe.NameEmail other = (DeclarativeRecipe.NameEmail) o;
            final Object this$name = this.getName();
            final Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final Object this$email = this.getEmail();
            final Object other$email = other.getEmail();
            if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final Object $email = this.getEmail();
            result = result * PRIME + ($email == null ? 43 : $email.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeclarativeRecipe.NameEmail(name=" + this.getName() + ", email=" + this.getEmail() + ")";
        }
    }

    @Override
    public List<Contributor> getContributors() {
        if (contributors == null) {
            Map<NameEmail, Integer> contributorToLineCount = new HashMap<>();
            contributors = new ArrayList<>();
            for (Recipe recipe : getRecipeList()) {
                for (Contributor contributor : recipe.getContributors()) {
                    NameEmail nameEmail = new NameEmail(contributor.getName(), contributor.getEmail());
                    contributorToLineCount.put(nameEmail, contributorToLineCount.getOrDefault(nameEmail, 0) + contributor.getLineCount());
                }
            }
            for (Map.Entry<NameEmail, Integer> contributorEntry : contributorToLineCount.entrySet()) {
                contributors.add(new Contributor(contributorEntry.getKey().getName(), contributorEntry.getKey().getEmail(), contributorEntry.getValue()));
            }
            contributors.sort(Comparator.comparing(Contributor::getLineCount).reversed());
        }
        return contributors;
    }

    public DeclarativeRecipe(final String name, @Language("markdown") final String displayName, @Language("markdown") final String description, final Set<String> tags, @Nullable final Duration estimatedEffortPerOccurrence, final URI source, final boolean causesAnotherCycle, final List<Maintainer> maintainers) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.tags = tags;
        this.estimatedEffortPerOccurrence = estimatedEffortPerOccurrence;
        this.source = source;
        this.causesAnotherCycle = causesAnotherCycle;
        this.maintainers = maintainers;
    }

    public String getName() {
        return this.name;
    }

    @Language("markdown")
    public String getDisplayName() {
        return this.displayName;
    }

    @Language("markdown")
    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public List<Maintainer> getMaintainers() {
        return this.maintainers;
    }

    public void setRecipeList(final List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    public void setPreconditions(final List<Recipe> preconditions) {
        this.preconditions = preconditions;
    }
}
