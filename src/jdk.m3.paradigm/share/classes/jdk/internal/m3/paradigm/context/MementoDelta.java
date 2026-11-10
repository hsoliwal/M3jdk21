/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Explicit upserts and tombstones; removing a key never means parent fallback.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MementoDelta.java}
 * blob {@code b017a3b030288b6409ec66e857c8261851bce77c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MementoDelta(Map<String, String> upserts, Set<String> removals) {
    public MementoDelta {
        upserts = MementoCodec.copy(upserts);
        removals = Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNull(removals)));
        if (removals.size() > MementoCodec.MAX_CELLS
                || removals.stream().anyMatch(upserts::containsKey)) {
            throw new IllegalArgumentException("Invalid or overlapping delta keys");
        }
    }

    /** Calculates an exact key/value delta without hash-based equivalence. */
    public static MementoDelta between(Map<String, String> before, Map<String, String> after) {
        Map<String, String> oldState = MementoCodec.copy(before);
        Map<String, String> newState = MementoCodec.copy(after);
        Map<String, String> writes = newState.entrySet().stream()
                .filter(entry -> !Objects.equals(oldState.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<String> deletes = oldState.keySet().stream()
                .filter(key -> !newState.containsKey(key)).collect(Collectors.toSet());
        return new MementoDelta(writes, deletes);
    }

    /** Applies this delta without mutating the original state. */
    public Map<String, String> apply(Map<String, String> before) {
        TreeMap<String, String> next = new TreeMap<>(MementoCodec.copy(before));
        removals.forEach(next::remove);
        next.putAll(upserts);
        MementoCodec.encode(next);
        return MementoCodec.copy(next);
    }
}
