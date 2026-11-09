// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/tree/JavaCoordinates.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.template.Coordinates;
import java.util.Comparator;

public class JavaCoordinates implements Coordinates {
    private final J tree;
    private final Space.Location spaceLocation;
    private final Mode mode;
    @Nullable
    private final Comparator<? extends J> comparator;

    public boolean isReplacement() {
        return Mode.REPLACEMENT.equals(mode);
    }

    /**
     * Determines whether we are replacing a whole tree element, and not either
     * (1) replacing just a piece of a method, class, or variable declaration signature or
     * (2) inserting a new element
     */
    public boolean isReplaceWholeCursorValue() {
        return isReplacement() && spaceLocation == null;
    }


    public enum Mode {
        AFTER, BEFORE, REPLACEMENT;
    }

    @Nullable
    public <J2 extends J> Comparator<J2> getComparator() {
        //noinspection unchecked
        return (Comparator<J2>) comparator;
    }

    public JavaCoordinates(final J tree, final Space.Location spaceLocation, final Mode mode, @Nullable final Comparator<? extends J> comparator) {
        this.tree = tree;
        this.spaceLocation = spaceLocation;
        this.mode = mode;
        this.comparator = comparator;
    }

    public J getTree() {
        return this.tree;
    }

    public Space.Location getSpaceLocation() {
        return this.spaceLocation;
    }

    public Mode getMode() {
        return this.mode;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaCoordinates)) return false;
        final JavaCoordinates other = (JavaCoordinates) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$tree = this.getTree();
        final Object other$tree = other.getTree();
        if (this$tree == null ? other$tree != null : !this$tree.equals(other$tree)) return false;
        final Object this$spaceLocation = this.getSpaceLocation();
        final Object other$spaceLocation = other.getSpaceLocation();
        if (this$spaceLocation == null ? other$spaceLocation != null : !this$spaceLocation.equals(other$spaceLocation)) return false;
        final Object this$mode = this.getMode();
        final Object other$mode = other.getMode();
        if (this$mode == null ? other$mode != null : !this$mode.equals(other$mode)) return false;
        final Object this$comparator = this.getComparator();
        final Object other$comparator = other.getComparator();
        if (this$comparator == null ? other$comparator != null : !this$comparator.equals(other$comparator)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof JavaCoordinates;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $tree = this.getTree();
        result = result * PRIME + ($tree == null ? 43 : $tree.hashCode());
        final Object $spaceLocation = this.getSpaceLocation();
        result = result * PRIME + ($spaceLocation == null ? 43 : $spaceLocation.hashCode());
        final Object $mode = this.getMode();
        result = result * PRIME + ($mode == null ? 43 : $mode.hashCode());
        final Object $comparator = this.getComparator();
        result = result * PRIME + ($comparator == null ? 43 : $comparator.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "JavaCoordinates(tree=" + this.getTree() + ", spaceLocation=" + this.getSpaceLocation() + ", mode=" + this.getMode() + ", comparator=" + this.getComparator() + ")";
    }
}
