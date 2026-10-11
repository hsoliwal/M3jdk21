// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/FindParseFailures.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markup;
import org.openrewrite.table.ParseFailures;
import java.util.Objects;

public final class FindParseFailures extends Recipe {
    @Option(displayName = "Max snippet length", description = "When the failure occurs on a granular tree element, its source code will be included " + "as a column in the data table up to this maximum snippet length.", required = false)
    @Nullable
    private final Integer maxSnippetLength;
    @Option(displayName = "Parser type", description = "Only display failures from parsers with this fully qualified name.", required = false, example = "org.openrewrite.yaml.YamlParser")
    @Nullable
    private final String parserType;
    @Option(displayName = "Stack trace", description = "Only mark stack traces with a message containing this text.", required = false)
    @Nullable
    private final String stackTrace;
    private final transient ParseFailures failures = new ParseFailures(this);

    @Override
    public String getDisplayName() {
        return "Find source files with `ParseExceptionResult` markers";
    }

    @Override
    public String getDescription() {
        return "This recipe explores parse failures after an LST is produced for classifying the types of " + "failures that can occur and prioritizing fixes according to the most common problems.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree postVisit(Tree tree, ExecutionContext ctx) {
                return tree.getMarkers().findFirst(ParseExceptionResult.class).map(exceptionResult -> {
                    if (parserType != null && !Objects.equals(exceptionResult.getParserType(), parserType)) {
                        return tree;
                    }
                    if (stackTrace != null && !exceptionResult.getMessage().contains(stackTrace)) {
                        return tree;
                    }
                    String snippet = tree instanceof SourceFile ? null : tree.printTrimmed(getCursor());
                    if (snippet != null && maxSnippetLength != null && snippet.length() > maxSnippetLength) {
                        snippet = snippet.substring(0, maxSnippetLength);
                    }
                    failures.insertRow(ctx, new ParseFailures.Row(exceptionResult.getParserType(), (tree instanceof SourceFile ? (SourceFile) tree : getCursor().firstEnclosingOrThrow(SourceFile.class)).getSourcePath().toString(), exceptionResult.getExceptionType(), exceptionResult.getTreeType(), snippet, exceptionResult.getMessage()));
                    return Markup.info(tree, exceptionResult.getMessage());
                }).orElse(tree);
            }
        };
    }

    public FindParseFailures(@Nullable final Integer maxSnippetLength, @Nullable final String parserType, @Nullable final String stackTrace) {
        this.maxSnippetLength = maxSnippetLength;
        this.parserType = parserType;
        this.stackTrace = stackTrace;
    }

    @Nullable
    public Integer getMaxSnippetLength() {
        return this.maxSnippetLength;
    }

    @Nullable
    public String getParserType() {
        return this.parserType;
    }

    @Nullable
    public String getStackTrace() {
        return this.stackTrace;
    }

    public ParseFailures getFailures() {
        return this.failures;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindParseFailures(maxSnippetLength=" + this.getMaxSnippetLength() + ", parserType=" + this.getParserType() + ", stackTrace=" + this.getStackTrace() + ", failures=" + this.getFailures() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindParseFailures)) return false;
        final FindParseFailures other = (FindParseFailures) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$maxSnippetLength = this.getMaxSnippetLength();
        final Object other$maxSnippetLength = other.getMaxSnippetLength();
        if (this$maxSnippetLength == null ? other$maxSnippetLength != null : !this$maxSnippetLength.equals(other$maxSnippetLength)) return false;
        final Object this$parserType = this.getParserType();
        final Object other$parserType = other.getParserType();
        if (this$parserType == null ? other$parserType != null : !this$parserType.equals(other$parserType)) return false;
        final Object this$stackTrace = this.getStackTrace();
        final Object other$stackTrace = other.getStackTrace();
        if (this$stackTrace == null ? other$stackTrace != null : !this$stackTrace.equals(other$stackTrace)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindParseFailures;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $maxSnippetLength = this.getMaxSnippetLength();
        result = result * PRIME + ($maxSnippetLength == null ? 43 : $maxSnippetLength.hashCode());
        final Object $parserType = this.getParserType();
        result = result * PRIME + ($parserType == null ? 43 : $parserType.hashCode());
        final Object $stackTrace = this.getStackTrace();
        result = result * PRIME + ($stackTrace == null ? 43 : $stackTrace.hashCode());
        return result;
    }
}
