// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/** Immutable node in a frozen hierarchical lexicon registry. */
public record MIndexNamespace(
        int id,
        String path,
        MIndexLexiconKind kind,
        int parentId,
        String languageTag,
        int priority) {

    public MIndexNamespace {
        if (id < 0 || id > MIndexAtomRef.MAX_NAMESPACE_ID) {
            throw new IllegalArgumentException("namespace id out of range: " + id);
        }
        path = Objects.requireNonNull(path, "path");
        kind = Objects.requireNonNull(kind, "kind");
        if (parentId < -1 || parentId > MIndexAtomRef.MAX_NAMESPACE_ID) {
            throw new IllegalArgumentException("parent id out of range: " + parentId);
        }
        languageTag = languageTag == null ? "" : languageTag;
    }

    public boolean hasParent() {
        return parentId >= 0;
    }

    public boolean languageScoped() {
        return !languageTag.isEmpty();
    }
}
