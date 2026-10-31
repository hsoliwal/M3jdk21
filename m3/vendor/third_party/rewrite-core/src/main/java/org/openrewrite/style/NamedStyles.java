// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/style/NamedStyles.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.style;

import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import java.util.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * A collection of styles by name, e.g., IntelliJ IDEA or Google Java Format.
 */
public class NamedStyles implements Marker {
    UUID id;
    String name;
    String displayName;
    @Nullable
    String description;
    Set<String> tags;
    Collection<Style> styles;

    @SuppressWarnings("unchecked")
    @Nullable
    public static <S extends Style> S merge(Class<S> styleClass, Iterable<? extends NamedStyles> namedStyles) {
        S merged = null;
        for (NamedStyles namedStyle : namedStyles) {
            Collection<Style> styles = namedStyle.styles;
            if (styles != null) {
                for (Style style : styles) {
                    if (styleClass.isInstance(style)) {
                        style = style.applyDefaults();
                        if (merged == null) {
                            merged = (S) style;
                        } else {
                            merged = (S) merged.merge(style);
                        }
                    }
                }
            }
        }
        return merged;
    }

    /**
     * Merge many NamedStyles into one NamedStyle.
     *
     * @param styles The styles to be merged. Styles earlier in the list take precedence over styles later in the list.
     * @return A single merged style with the aggregate configuration of all inputs. null if the list is empty.
     */
    @Nullable
    public static NamedStyles merge(List<NamedStyles> styles) {
        if (styles.isEmpty()) {
            return null;
        } else if (styles.size() == 1) {
            return styles.get(0);
        }
        Set<Class<? extends Style>> styleClasses = new HashSet<>();
        for (NamedStyles namedStyles : styles) {
            for (Style style : namedStyles.getStyles()) {
                styleClasses.add(style.getClass());
            }
        }
        List<Style> mergedStyles = new ArrayList<>(styleClasses.size());
        for (Class<? extends Style> styleClass : styleClasses) {
            mergedStyles.add(NamedStyles.merge(styleClass, styles));
        }
        return new NamedStyles(Tree.randomId(), "MergedStyles", "Merged styles", "Merged Styles from " + styles.stream().map(NamedStyles::getName).collect(joining(", ")), styles.stream().map(NamedStyles::getTags).flatMap(Set::stream).collect(toSet()), mergedStyles);
    }

    public Validated<Object> validate() {
        return Validated.none();
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public Collection<Style> getStyles() {
        return this.styles;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    public void setTags(final Set<String> tags) {
        this.tags = tags;
    }

    public void setStyles(final Collection<Style> styles) {
        this.styles = styles;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "NamedStyles(id=" + this.getId() + ", name=" + this.getName() + ", displayName=" + this.getDisplayName() + ", description=" + this.getDescription() + ", tags=" + this.getTags() + ", styles=" + this.getStyles() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof NamedStyles)) return false;
        final NamedStyles other = (NamedStyles) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof NamedStyles;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    public NamedStyles(final UUID id, final String name, final String displayName, @Nullable final String description, final Set<String> tags, final Collection<Style> styles) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.tags = tags;
        this.styles = styles;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NamedStyles withId(final UUID id) {
        return this.id == id ? this : new NamedStyles(id, this.name, this.displayName, this.description, this.tags, this.styles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NamedStyles withName(final String name) {
        return this.name == name ? this : new NamedStyles(this.id, name, this.displayName, this.description, this.tags, this.styles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NamedStyles withDisplayName(final String displayName) {
        return this.displayName == displayName ? this : new NamedStyles(this.id, this.name, displayName, this.description, this.tags, this.styles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NamedStyles withDescription(@Nullable final String description) {
        return this.description == description ? this : new NamedStyles(this.id, this.name, this.displayName, description, this.tags, this.styles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NamedStyles withTags(final Set<String> tags) {
        return this.tags == tags ? this : new NamedStyles(this.id, this.name, this.displayName, this.description, tags, this.styles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NamedStyles withStyles(final Collection<Style> styles) {
        return this.styles == styles ? this : new NamedStyles(this.id, this.name, this.displayName, this.description, this.tags, styles);
    }
}
