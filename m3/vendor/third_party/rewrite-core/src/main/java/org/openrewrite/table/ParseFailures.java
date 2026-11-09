// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/ParseFailures.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

@JsonIgnoreType
public class ParseFailures extends DataTable<ParseFailures.Row> {
    public ParseFailures(Recipe recipe) {
        super(recipe, "Parser failures", "A list of files that failed to parse along with stack traces of their failures.");
    }


    public static final class Row {
        @Column(displayName = "Parser", description = "The parser implementation that failed.")
        private final String parser;
        @Column(displayName = "Source path", description = "The file that failed to parse.")
        private final String sourcePath;
        @Column(displayName = "Exception type", description = "The class name of the exception that produce the parse failure.")
        @Nullable
        private final String exceptionType;
        @Column(displayName = "Tree type", description = "The type of the tree element that was being parsed " + "when the failure occurred. This can refer either to the intended " + "target OpenRewrite Tree type or a parser or compiler internal tree " + "type that we couldn\'t determine how to map.")
        @Nullable
        private final String treeType;
        @Column(displayName = "Snippet", description = "The code snippet that the failure occurred on. Omitted when the parser fails on the whole file.")
        @Nullable
        private final String snippet;
        @Column(displayName = "Stack trace", description = "The stack trace of the failure.")
        private final String stackTrace;

        public Row(final String parser, final String sourcePath, @Nullable final String exceptionType, @Nullable final String treeType, @Nullable final String snippet, final String stackTrace) {
            this.parser = parser;
            this.sourcePath = sourcePath;
            this.exceptionType = exceptionType;
            this.treeType = treeType;
            this.snippet = snippet;
            this.stackTrace = stackTrace;
        }

        public String getParser() {
            return this.parser;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        @Nullable
        public String getExceptionType() {
            return this.exceptionType;
        }

        @Nullable
        public String getTreeType() {
            return this.treeType;
        }

        @Nullable
        public String getSnippet() {
            return this.snippet;
        }

        public String getStackTrace() {
            return this.stackTrace;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof ParseFailures.Row)) return false;
            final ParseFailures.Row other = (ParseFailures.Row) o;
            final Object this$parser = this.getParser();
            final Object other$parser = other.getParser();
            if (this$parser == null ? other$parser != null : !this$parser.equals(other$parser)) return false;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$exceptionType = this.getExceptionType();
            final Object other$exceptionType = other.getExceptionType();
            if (this$exceptionType == null ? other$exceptionType != null : !this$exceptionType.equals(other$exceptionType)) return false;
            final Object this$treeType = this.getTreeType();
            final Object other$treeType = other.getTreeType();
            if (this$treeType == null ? other$treeType != null : !this$treeType.equals(other$treeType)) return false;
            final Object this$snippet = this.getSnippet();
            final Object other$snippet = other.getSnippet();
            if (this$snippet == null ? other$snippet != null : !this$snippet.equals(other$snippet)) return false;
            final Object this$stackTrace = this.getStackTrace();
            final Object other$stackTrace = other.getStackTrace();
            if (this$stackTrace == null ? other$stackTrace != null : !this$stackTrace.equals(other$stackTrace)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $parser = this.getParser();
            result = result * PRIME + ($parser == null ? 43 : $parser.hashCode());
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $exceptionType = this.getExceptionType();
            result = result * PRIME + ($exceptionType == null ? 43 : $exceptionType.hashCode());
            final Object $treeType = this.getTreeType();
            result = result * PRIME + ($treeType == null ? 43 : $treeType.hashCode());
            final Object $snippet = this.getSnippet();
            result = result * PRIME + ($snippet == null ? 43 : $snippet.hashCode());
            final Object $stackTrace = this.getStackTrace();
            result = result * PRIME + ($stackTrace == null ? 43 : $stackTrace.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "ParseFailures.Row(parser=" + this.getParser() + ", sourcePath=" + this.getSourcePath() + ", exceptionType=" + this.getExceptionType() + ", treeType=" + this.getTreeType() + ", snippet=" + this.getSnippet() + ", stackTrace=" + this.getStackTrace() + ")";
        }
    }
}
