// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/text/ChangeText.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.text;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import java.util.Collections;
import java.util.Set;
import static java.util.Collections.emptyList;

public final class ChangeText extends Recipe {
    @Option(displayName = "Text after change", description = "The text file will have only this text after the change. The snippet provided here can be multiline.", example = "Some text.")
    private final String toText;

    @Override
    public Set<String> getTags() {
        return Collections.singleton("plain text");
    }

    @Override
    public String getDisplayName() {
        return "Change text";
    }

    @Override
    public String getInstanceNameSuffix() {
        return "to `" + toText + "`";
    }

    @Override
    public String getDescription() {
        return "Completely replaces the contents of the text file with other text. " + "Use together with a `FindSourceFiles` precondition to limit which files are changed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                return text.withSnippets(emptyList()).withText(toText);
            }
        };
    }

    public ChangeText(final String toText) {
        this.toText = toText;
    }

    public String getToText() {
        return this.toText;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeText(toText=" + this.getToText() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeText)) return false;
        final ChangeText other = (ChangeText) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$toText = this.getToText();
        final Object other$toText = other.getToText();
        if (this$toText == null ? other$toText != null : !this$toText.equals(other$toText)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeText;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $toText = this.getToText();
        result = result * PRIME + ($toText == null ? 43 : $toText.hashCode());
        return result;
    }
}
