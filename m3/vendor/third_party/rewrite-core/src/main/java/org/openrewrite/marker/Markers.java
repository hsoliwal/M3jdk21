// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/Markers.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public final class Markers {
    public static final Markers EMPTY = new Markers(randomId(), emptyList());
    private final UUID id;
    private final List<Marker> markers;

    public static Markers build(Collection<? extends Marker> markers) {
        if (markers.isEmpty()) {
            return EMPTY;
        }
        List<Marker> markerList;
        if (markers instanceof List) {
            //noinspection unchecked
            markerList = (List<Marker>) markers;
        } else {
            markerList = new ArrayList<>(markers.size());
            markerList.addAll(markers);
        }
        return new Markers(randomId(), markerList);
    }

    /**
     * {@link TreeVisitor} may respond to a marker to determine whether to act on
     * a source file or not.
     *
     * @return A marker collection containing any additional context about the containing {@link Tree} element.
     */
    public Collection<? extends Marker> entries() {
        return markers;
    }

    /**
     * Adds a new marker element to the collection.
     *
     * @param marker The data to add or update.
     * @return A new {@link Markers} with an added marker.
     */
    public Markers add(Marker marker) {
        for (Marker m : markers) {
            if (marker.equals(m)) {
                return this;
            }
        }
        List<Marker> updatedmarker = new ArrayList<>(markers);
        updatedmarker.add(marker);
        return new Markers(id, updatedmarker);
    }

    /**
     * Add a new marker or update some existing marker.
     *
     * @param identity          A new marker to add if it doesn't already exist. Existence is determined by type equality.
     * @param remappingFunction The function that merges an existing marker.
     * @param <M>               The type of marker.
     * @return A new {@link Markers} with an added or updated marker.
     */
    public <M extends Marker> Markers computeByType(M identity, BinaryOperator<M> remappingFunction) {
        AtomicBoolean updated = new AtomicBoolean(false);
        List<Marker> markers = ListUtils.map(this.markers, m -> {
            if (m.getClass().equals(identity.getClass())) {
                updated.set(true);
                //noinspection unchecked
                return remappingFunction.apply((M) m, identity);
            }
            return m;
        });
        return withMarkers(!updated.get() ? ListUtils.concat(markers, identity) : markers);
    }

    public Markers removeByType(Class<? extends Marker> type) {
        return withMarkers(ListUtils.map(this.markers, m -> type.equals(m.getClass()) ? null : m));
    }

    public <M extends Marker> Markers setByType(M m) {
        return computeByType(m, (existing, replacement) -> replacement);
    }

    /**
     * Add a new marker or update some existing marker.
     *
     * @param identity          A new marker to add if it doesn't already exist. Existence is determined by regular equality.
     * @param remappingFunction The function that merges an existing marker with the new marker.
     * @param <M>               The type of marker.
     * @return A new {@link Markers} with an added or updated marker.
     */
    public <M extends Marker> Markers compute(M identity, BinaryOperator<M> remappingFunction) {
        AtomicBoolean foundEqualMarker = new AtomicBoolean(false);
        List<Marker> updatedMarkers = ListUtils.map(markers, m -> {
            if (m.equals(identity)) {
                foundEqualMarker.set(true);
                //noinspection unchecked
                return remappingFunction.apply((M) m, identity);
            }
            return m;
        });
        if (!foundEqualMarker.get()) {
            updatedMarkers = ListUtils.concat(updatedMarkers, identity);
        }
        return withMarkers(updatedMarkers);
    }

    /**
     * Add a new marker or update some existing marker.
     *
     * @param m   A marker, which may or may not already exist already.
     * @param <M> The marker type.
     * @return If a marker already exists that matches by object equality, an unchanged markers reference
     * is returned. Otherwise, the supplied marker is added.
     */
    public <M extends Marker> Markers addIfAbsent(M m) {
        return compute(m, (m1, m2) -> m1);
    }

    public <M extends Marker> List<M> findAll(Class<M> markerType) {
        return markers.stream().filter(markerType::isInstance).map(markerType::cast).collect(toList());
    }

    public <M extends Marker> Optional<M> findFirst(Class<M> markerType) {
        return markers.stream().filter(markerType::isInstance).map(markerType::cast).findFirst();
    }

    public Markers(final UUID id, final List<Marker> markers) {
        this.id = id;
        this.markers = markers;
    }

    public UUID getId() {
        return this.id;
    }

    public List<Marker> getMarkers() {
        return this.markers;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof Markers)) return false;
        final Markers other = (Markers) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$markers = this.getMarkers();
        final Object other$markers = other.getMarkers();
        if (this$markers == null ? other$markers != null : !this$markers.equals(other$markers)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $markers = this.getMarkers();
        result = result * PRIME + ($markers == null ? 43 : $markers.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "Markers(id=" + this.getId() + ", markers=" + this.getMarkers() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Markers withId(final UUID id) {
        return this.id == id ? this : new Markers(id, this.markers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Markers withMarkers(final List<Marker> markers) {
        return this.markers == markers ? this : new Markers(this.id, markers);
    }
}
