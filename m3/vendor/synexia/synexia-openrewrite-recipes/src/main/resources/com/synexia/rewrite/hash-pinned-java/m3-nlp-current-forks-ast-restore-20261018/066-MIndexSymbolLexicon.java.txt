// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Optional;

/** Read-only typed lexicon backed by a shared canonical symbol table. */
public interface MIndexSymbolLexicon {
    int dictionaryVersion();
    MIndexNamespace namespace();
    MIndexSymbolTable symbols();
    int size();
    Optional<MIndexAtomRef> lookup(String surface);
    MIndexAtomRef ref(int rank);
    long symbolId(MIndexAtomRef ref);
    long frequency(MIndexAtomRef ref);
    long classes(MIndexAtomRef ref);

    default String materialize(MIndexAtomRef ref) {
        return symbols().materialize(symbolId(ref));
    }

    default void requireLocal(MIndexAtomRef ref) {
        if (ref == null
                || ref.version() != dictionaryVersion()
                || ref.namespaceId() != namespace().id()
                || ref.entryId() >= size()) {
            throw new IllegalArgumentException("Atom ref does not belong to this lexicon: " + ref);
        }
    }
}
