// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/IsInRepository.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.SearchResult;
import java.util.Optional;
import java.util.Set;

public final class IsInRepository extends Recipe {
    @Override
    public String getDisplayName() {
        return "Is in repository";
    }

    @Override
    public String getDescription() {
        return "A search recipe which marks files that are in a repository with one of the supplied names. " + "Intended for use as a precondition for other recipes being run over many different repositories.";
    }

    @Option(displayName = "Allowed repositories", description = "The names of the repositories that are allowed to be searched. " + "Determines repository name according to git metadata recorded in the `GitProvenance` marker.", example = "rewrite")
    private final Set<String> allowedRepositories;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            @Nullable
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                Optional<GitProvenance> maybeGp = tree.getMarkers().findFirst(GitProvenance.class);
                if (maybeGp.isPresent()) {
                    GitProvenance gp = maybeGp.get();
                    if (allowedRepositories.contains(gp.getRepositoryName())) {
                        return SearchResult.found(tree);
                    }
                }
                return tree;
            }
        };
    }

    public IsInRepository(final Set<String> allowedRepositories) {
        this.allowedRepositories = allowedRepositories;
    }

    public Set<String> getAllowedRepositories() {
        return this.allowedRepositories;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "IsInRepository(allowedRepositories=" + this.getAllowedRepositories() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof IsInRepository)) return false;
        final IsInRepository other = (IsInRepository) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$allowedRepositories = this.getAllowedRepositories();
        final Object other$allowedRepositories = other.getAllowedRepositories();
        if (this$allowedRepositories == null ? other$allowedRepositories != null : !this$allowedRepositories.equals(other$allowedRepositories)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof IsInRepository;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $allowedRepositories = this.getAllowedRepositories();
        result = result * PRIME + ($allowedRepositories == null ? 43 : $allowedRepositories.hashCode());
        return result;
    }
}
