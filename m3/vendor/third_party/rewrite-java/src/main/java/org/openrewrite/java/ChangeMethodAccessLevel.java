// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeMethodAccessLevel.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.J;

public final class ChangeMethodAccessLevel extends Recipe {
    @Option(displayName = "Method pattern", description = "A method pattern that is used to find matching method declarations/invocations.", example = "org.mockito.Matchers anyVararg()")
    private final String methodPattern;
    @Option(displayName = "New access level", description = "New method access level to apply to the method.", example = "public", valid = {"private", "protected", "package", "public"})
    private final String newAccessLevel;
    @Option(displayName = "Match on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable
    private final Boolean matchOverrides;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", methodPattern, newAccessLevel);
    }

    @Override
    public String getDisplayName() {
        return "Change method access level";
    }

    @Override
    public String getDescription() {
        return "Change the access level (public, protected, private, package private) of a method.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("newAccessLevel", "Must be one of \'private\', \'protected\', \'package\', \'public\'", newAccessLevel, level -> "private".equals(level) || "protected".equals(level) || "package".equals(level) || "public".equals(level)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        J.Modifier.Type type;
        switch (newAccessLevel) {
        case "public": 
            type = J.Modifier.Type.Public;
            break;
        case "protected": 
            type = J.Modifier.Type.Protected;
            break;
        case "private": 
            type = J.Modifier.Type.Private;
            break;
        default: 
            type = null;
        }
        return Preconditions.check(new DeclaresMethod<>(methodPattern, matchOverrides), new ChangeMethodAccessLevelVisitor<>(new MethodMatcher(methodPattern, matchOverrides), type));
    }

    public ChangeMethodAccessLevel(final String methodPattern, final String newAccessLevel, @Nullable final Boolean matchOverrides) {
        this.methodPattern = methodPattern;
        this.newAccessLevel = newAccessLevel;
        this.matchOverrides = matchOverrides;
    }

    public String getMethodPattern() {
        return this.methodPattern;
    }

    public String getNewAccessLevel() {
        return this.newAccessLevel;
    }

    @Nullable
    public Boolean getMatchOverrides() {
        return this.matchOverrides;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeMethodAccessLevel(methodPattern=" + this.getMethodPattern() + ", newAccessLevel=" + this.getNewAccessLevel() + ", matchOverrides=" + this.getMatchOverrides() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeMethodAccessLevel)) return false;
        final ChangeMethodAccessLevel other = (ChangeMethodAccessLevel) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchOverrides = this.getMatchOverrides();
        final Object other$matchOverrides = other.getMatchOverrides();
        if (this$matchOverrides == null ? other$matchOverrides != null : !this$matchOverrides.equals(other$matchOverrides)) return false;
        final Object this$methodPattern = this.getMethodPattern();
        final Object other$methodPattern = other.getMethodPattern();
        if (this$methodPattern == null ? other$methodPattern != null : !this$methodPattern.equals(other$methodPattern)) return false;
        final Object this$newAccessLevel = this.getNewAccessLevel();
        final Object other$newAccessLevel = other.getNewAccessLevel();
        if (this$newAccessLevel == null ? other$newAccessLevel != null : !this$newAccessLevel.equals(other$newAccessLevel)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeMethodAccessLevel;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchOverrides = this.getMatchOverrides();
        result = result * PRIME + ($matchOverrides == null ? 43 : $matchOverrides.hashCode());
        final Object $methodPattern = this.getMethodPattern();
        result = result * PRIME + ($methodPattern == null ? 43 : $methodPattern.hashCode());
        final Object $newAccessLevel = this.getNewAccessLevel();
        result = result * PRIME + ($newAccessLevel == null ? 43 : $newAccessLevel.hashCode());
        return result;
    }
}
