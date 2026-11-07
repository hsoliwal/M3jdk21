package com.synexia.indexstring;
/** Donor-side stub (differential only): a range view; toString is the spelling of the range. */
public final class SubMIndexString implements CharSequence {
    private final MIndexString source; private final int begin, end;
    SubMIndexString(MIndexString source, int begin, int end) {
        java.util.Objects.checkFromToIndex(begin, end, source.length());
        this.source = source; this.begin = begin; this.end = end;
    }
    @Override public int length() { return end - begin; }
    @Override public char charAt(int index) { return source.charAt(begin + java.util.Objects.checkIndex(index, length())); }
    @Override public CharSequence subSequence(int s, int e) { return toString().subSequence(s, e); }
    @Override public String toString() { return source.toString().substring(begin, end); }
}
