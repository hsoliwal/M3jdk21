/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

/** Canonical owner node for M3String. Text payload never lives in M3String itself. */
abstract sealed class M3StringOwner permits M3StringAtom, M3StringTuple {
    static final byte ATOM = 1;
    static final byte TUPLE = 2;

    final byte kind;
    final int length;
    final byte coder;
    final int javaHash;
    final long structuralHash64;
    private volatile M3StringFacts facts;

    M3StringOwner(byte kind, int length, byte coder, int javaHash, long structuralHash64) {
        this.kind = kind;
        this.length = length;
        this.coder = coder;
        this.javaHash = javaHash;
        this.structuralHash64 = structuralHash64;
    }

    abstract char charAt(int index);

    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        for (int source = start, target = destinationStart; source < end; source++, target++) {
            destination[target] = charAt(source);
        }
    }

    final M3StringFacts facts() {
        M3StringFacts current = facts;
        if (current != null) return current;
        synchronized (this) {
            current = facts;
            if (current == null) {
                current = computeFacts();
                if (current.utf16Length != length || current.javaHash != javaHash) {
                    throw new InternalError("M3String canonical fact mismatch");
                }
                facts = current;
            }
            return current;
        }
    }

    abstract M3StringFacts computeFacts();
}
