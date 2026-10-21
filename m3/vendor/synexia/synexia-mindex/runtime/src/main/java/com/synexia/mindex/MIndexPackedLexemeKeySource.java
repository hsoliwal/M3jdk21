// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Internal primitive key-lane contract shared by heap and mapped packed lexeme indexes.
 *
 * <p>The matrix/search side indexes depend only on immutable coordinate lanes and caller value
 * ids; they do not own or copy the canonical text payload.</p>
 */
interface MIndexPackedLexemeKeySource {
    MIndexLexemeCodec codec();

    int packedEntryCount();

    long highKeyAtEntry(int entry);

    long lowKeyAtEntry(int entry);

    int valueIdAtEntry(int entry);
}
