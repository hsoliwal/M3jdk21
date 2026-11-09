// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/SearchResult.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.openrewrite.Tree.randomId;

public final class SearchResult implements Marker {
    private final UUID id;
    @Nullable
    private final String description;

    public static <T extends Tree> T found(@Nullable T t) {
        return found(t, null);
    }

    public static <T extends Tree> T found(@Nullable T t, @Nullable String description) {
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        return t.withMarkers(t.getMarkers().computeByType(new SearchResult(randomId(), description), (s1, s2) -> s1 == null ? s2 : s1));
    }

    /**
     * Merge the description of two search results into a single search result with a unified description.
     * <p>
     * If the there already exists a search result with the same description, the existing search result is returned.
     */
    @Incubating(since = "8.0.0")
    public static <T extends Tree> T mergingFound(@Nullable T t, String description) {
        return mergingFound(t, description, ", ");
    }

    /**
     * Merge the description of two search results into a single search result with a unified description.
     * <p>
     * If the there already exists a search result with the same description, the existing search result is returned.
     *
     * @param delimiter The delimiter to use when merging descriptions.
     */
    @Incubating(since = "8.0.0")
    public static <T extends Tree> T mergingFound(@Nullable T t, String description, String delimiter) {
        Objects.requireNonNull(delimiter, "delimiter must not be null");
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        return t.withMarkers(t.getMarkers().computeByType(new SearchResult(randomId(), description), (s1, s2) -> {
            if (s1 == null) {
                return s2;
            }
            if (s2 == null) {
                return s1;
            }
            if (s1.getDescription() == null) {
                return s2;
            }
            if (s2.getDescription() == null) {
                return s1;
            }
            if (s1.getDescription().equals(s2.getDescription()) || s1.getDescription().startsWith(s2.getDescription() + delimiter) || s1.getDescription().contains(delimiter + s2.getDescription() + delimiter) || s1.getDescription().endsWith(s2.getDescription())) {
                return s1;
            }
            return s1.withDescription(s1.getDescription() + delimiter + s2.getDescription());
        }));
    }

    /**
     * @param cursor         The cursor at the point where the marker is being visited.
     * @param commentWrapper A function that wraps arbitrary text in a multi-line comment that is language-specific.
     * @return The printed representation of the marker.
     */
    @Override
    public String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        return commentWrapper.apply(description == null ? "" : "(" + description + ")");
    }

    public SearchResult(final UUID id, @Nullable final String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof SearchResult)) return false;
        final SearchResult other = (SearchResult) o;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    public UUID getId() {
        return this.id;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SearchResult withId(final UUID id) {
        return this.id == id ? this : new SearchResult(id, this.description);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SearchResult withDescription(@Nullable final String description) {
        return this.description == description ? this : new SearchResult(this.id, description);
    }
}
