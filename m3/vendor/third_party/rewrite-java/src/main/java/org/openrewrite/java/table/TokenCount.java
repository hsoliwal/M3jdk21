// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/table/TokenCount.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class TokenCount extends DataTable<TokenCount.Row> {
    public TokenCount(Recipe recipe) {
        super(recipe, "Token count", "The number of tokens from a code snippet");
    }


    public static final class Row {
        @Column(displayName = "Name of Class or Method", description = "The name of the class or method.")
        private final String codeSnippet;
        @Column(displayName = "Tokens", description = "The number of tokens estimated in the code snippet.")
        private final int tokens;

        public Row(final String codeSnippet, final int tokens) {
            this.codeSnippet = codeSnippet;
            this.tokens = tokens;
        }

        public String getCodeSnippet() {
            return this.codeSnippet;
        }

        public int getTokens() {
            return this.tokens;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof TokenCount.Row)) return false;
            final TokenCount.Row other = (TokenCount.Row) o;
            if (this.getTokens() != other.getTokens()) return false;
            final Object this$codeSnippet = this.getCodeSnippet();
            final Object other$codeSnippet = other.getCodeSnippet();
            if (this$codeSnippet == null ? other$codeSnippet != null : !this$codeSnippet.equals(other$codeSnippet)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getTokens();
            final Object $codeSnippet = this.getCodeSnippet();
            result = result * PRIME + ($codeSnippet == null ? 43 : $codeSnippet.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "TokenCount.Row(codeSnippet=" + this.getCodeSnippet() + ", tokens=" + this.getTokens() + ")";
        }
    }
}
