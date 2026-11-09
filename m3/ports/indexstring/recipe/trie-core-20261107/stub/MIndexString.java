package com.synexia.indexstring;
/** Donor-side stub (differential only): never instantiated; String inputs take the CharSequence overloads. */
public abstract class MIndexString implements CharSequence {
    public abstract Object resolver();
    public abstract int tokenCount();
    public abstract long tokenIdAt(int token);
}
