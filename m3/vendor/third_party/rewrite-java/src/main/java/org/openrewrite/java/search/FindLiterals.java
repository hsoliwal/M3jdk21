// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindLiterals.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FindLiterals extends Recipe {
    @Option(displayName = "Pattern", description = "A regular expression pattern to match literals against.", example = "file://")
    private final String pattern;

    @Override
    public String getDisplayName() {
        return "Find literals";
    }

    @Override
    public String getDescription() {
        return "Find literals matching a pattern.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("pattern", "Must be a valid regular expression", pattern, p -> {
            try {
                Pattern.compile(p);
                return true;
            } catch (PatternSyntaxException e) {
                return false;
            }
        }));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern compiledPattern = Pattern.compile(pattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (literal.getValueSource() != null) {
                    if (literal.getType() == JavaType.Primitive.String) {
                        if (!literal.getValueSource().isEmpty() && compiledPattern.matcher(literal.getValueSource().substring(1, literal.getValueSource().length() - 1)).matches()) {
                            return SearchResult.found(literal);
                        }
                    }
                    if (compiledPattern.matcher(literal.getValueSource()).matches()) {
                        return SearchResult.found(literal);
                    }
                }
                return literal;
            }
        };
    }

    public FindLiterals(final String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return this.pattern;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindLiterals(pattern=" + this.getPattern() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindLiterals)) return false;
        final FindLiterals other = (FindLiterals) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$pattern = this.getPattern();
        final Object other$pattern = other.getPattern();
        if (this$pattern == null ? other$pattern != null : !this$pattern.equals(other$pattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindLiterals;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $pattern = this.getPattern();
        result = result * PRIME + ($pattern == null ? 43 : $pattern.hashCode());
        return result;
    }
}
