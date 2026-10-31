// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindDeprecatedUses.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import java.util.Arrays;
import java.util.List;

public final class FindDeprecatedUses extends Recipe {
    @Option(displayName = "Type pattern", description = "A type pattern that is used to find deprecations from certain types.", example = "org.springframework..*", required = false)
    @Nullable
    private final String typePattern;
    @Option(displayName = "Match inherited", description = "When enabled, find types that inherit from a deprecated type.", required = false)
    @Nullable
    private final Boolean matchInherited;
    @Option(displayName = "Ignore deprecated scopes", description = "When a deprecated type is used in a deprecated method or class, ignore it.", required = false)
    @Nullable
    private final Boolean ignoreDeprecatedScopes;

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(new FindDeprecatedMethods((typePattern == null || typePattern.isEmpty() ? null : typePattern + " *(..)"), ignoreDeprecatedScopes), new FindDeprecatedClasses(typePattern, matchInherited, ignoreDeprecatedScopes), new FindDeprecatedFields(typePattern, matchInherited, ignoreDeprecatedScopes));
    }

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated classes, methods, and fields";
    }

    @Override
    public String getInstanceNameSuffix() {
        if (typePattern != null) {
            return "matching `" + typePattern + "`";
        }
        return super.getInstanceNameSuffix();
    }

    @Override
    public String getDescription() {
        return "Find deprecated uses of methods, fields, and types. Optionally ignore those classes that are inside deprecated scopes.";
    }

    public FindDeprecatedUses(@Nullable final String typePattern, @Nullable final Boolean matchInherited, @Nullable final Boolean ignoreDeprecatedScopes) {
        this.typePattern = typePattern;
        this.matchInherited = matchInherited;
        this.ignoreDeprecatedScopes = ignoreDeprecatedScopes;
    }

    @Nullable
    public String getTypePattern() {
        return this.typePattern;
    }

    @Nullable
    public Boolean getMatchInherited() {
        return this.matchInherited;
    }

    @Nullable
    public Boolean getIgnoreDeprecatedScopes() {
        return this.ignoreDeprecatedScopes;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindDeprecatedUses(typePattern=" + this.getTypePattern() + ", matchInherited=" + this.getMatchInherited() + ", ignoreDeprecatedScopes=" + this.getIgnoreDeprecatedScopes() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindDeprecatedUses)) return false;
        final FindDeprecatedUses other = (FindDeprecatedUses) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchInherited = this.getMatchInherited();
        final Object other$matchInherited = other.getMatchInherited();
        if (this$matchInherited == null ? other$matchInherited != null : !this$matchInherited.equals(other$matchInherited)) return false;
        final Object this$ignoreDeprecatedScopes = this.getIgnoreDeprecatedScopes();
        final Object other$ignoreDeprecatedScopes = other.getIgnoreDeprecatedScopes();
        if (this$ignoreDeprecatedScopes == null ? other$ignoreDeprecatedScopes != null : !this$ignoreDeprecatedScopes.equals(other$ignoreDeprecatedScopes)) return false;
        final Object this$typePattern = this.getTypePattern();
        final Object other$typePattern = other.getTypePattern();
        if (this$typePattern == null ? other$typePattern != null : !this$typePattern.equals(other$typePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindDeprecatedUses;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchInherited = this.getMatchInherited();
        result = result * PRIME + ($matchInherited == null ? 43 : $matchInherited.hashCode());
        final Object $ignoreDeprecatedScopes = this.getIgnoreDeprecatedScopes();
        result = result * PRIME + ($ignoreDeprecatedScopes == null ? 43 : $ignoreDeprecatedScopes.hashCode());
        final Object $typePattern = this.getTypePattern();
        result = result * PRIME + ($typePattern == null ? 43 : $typePattern.hashCode());
        return result;
    }
}
