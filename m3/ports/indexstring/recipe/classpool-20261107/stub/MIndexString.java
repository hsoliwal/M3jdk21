package com.synexia.indexstring;
/** Donor-side stub (differential only): wraps the materialized spelling. */
public final class MIndexString {
    private final String text;
    public MIndexString(Object tuple) { this.text = (String) tuple; }
    public String materialize() { return text; }
    @Override public String toString() { return text; }
}
