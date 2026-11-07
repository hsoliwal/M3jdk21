package com.synexia.indexstring;
/** Donor-side stub (differential only): pooled text whose hashCode is the Java String hash, as the donor pool's javaHash is. */
public final class MatIndexString {
    private final int handle; private final String text;
    MatIndexString(int handle, String text) { this.handle = handle; this.text = text; }
    public int handle() { return handle; }
    public String materialize() { return text; }
    public Object toTuple() { return text; }
    public int length() { return text.length(); }
    @Override public int hashCode() { return text.hashCode(); }
}
