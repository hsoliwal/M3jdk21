// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/config/RecipeExample.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.config;

import org.openrewrite.internal.lang.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RecipeExample {
    String description = "";
    List<String> parameters = new ArrayList<>();
    List<Source> sources = new ArrayList<>();


    public static class Source {
        String before;
        @Nullable
        String after;
        @Nullable
        String path;
        String language;

        public String getBefore() {
            return this.before;
        }

        @Nullable
        public String getAfter() {
            return this.after;
        }

        @Nullable
        public String getPath() {
            return this.path;
        }

        public String getLanguage() {
            return this.language;
        }

        public void setBefore(final String before) {
            this.before = before;
        }

        public void setAfter(@Nullable final String after) {
            this.after = after;
        }

        public void setPath(@Nullable final String path) {
            this.path = path;
        }

        public void setLanguage(final String language) {
            this.language = language;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof RecipeExample.Source)) return false;
            final RecipeExample.Source other = (RecipeExample.Source) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$before = this.getBefore();
            final Object other$before = other.getBefore();
            if (this$before == null ? other$before != null : !this$before.equals(other$before)) return false;
            final Object this$after = this.getAfter();
            final Object other$after = other.getAfter();
            if (this$after == null ? other$after != null : !this$after.equals(other$after)) return false;
            final Object this$path = this.getPath();
            final Object other$path = other.getPath();
            if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
            final Object this$language = this.getLanguage();
            final Object other$language = other.getLanguage();
            if (this$language == null ? other$language != null : !this$language.equals(other$language)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof RecipeExample.Source;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $before = this.getBefore();
            result = result * PRIME + ($before == null ? 43 : $before.hashCode());
            final Object $after = this.getAfter();
            result = result * PRIME + ($after == null ? 43 : $after.hashCode());
            final Object $path = this.getPath();
            result = result * PRIME + ($path == null ? 43 : $path.hashCode());
            final Object $language = this.getLanguage();
            result = result * PRIME + ($language == null ? 43 : $language.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "RecipeExample.Source(before=" + this.getBefore() + ", after=" + this.getAfter() + ", path=" + this.getPath() + ", language=" + this.getLanguage() + ")";
        }

        public Source(final String before, @Nullable final String after, @Nullable final String path, final String language) {
            this.before = before;
            this.after = after;
            this.path = path;
            this.language = language;
        }
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RecipeExample)) return false;
        final RecipeExample other = (RecipeExample) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final Object this$parameters = this.getParameters();
        final Object other$parameters = other.getParameters();
        if (this$parameters == null ? other$parameters != null : !this$parameters.equals(other$parameters)) return false;
        final Object this$sources = this.getSources();
        final Object other$sources = other.getSources();
        if (this$sources == null ? other$sources != null : !this$sources.equals(other$sources)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RecipeExample;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $parameters = this.getParameters();
        result = result * PRIME + ($parameters == null ? 43 : $parameters.hashCode());
        final Object $sources = this.getSources();
        result = result * PRIME + ($sources == null ? 43 : $sources.hashCode());
        return result;
    }

    public String getDescription() {
        return this.description;
    }

    public List<String> getParameters() {
        return this.parameters;
    }

    public List<Source> getSources() {
        return this.sources;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setParameters(final List<String> parameters) {
        this.parameters = parameters;
    }

    public void setSources(final List<Source> sources) {
        this.sources = sources;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RecipeExample(description=" + this.getDescription() + ", parameters=" + this.getParameters() + ", sources=" + this.getSources() + ")";
    }

    public RecipeExample() {
    }
}
