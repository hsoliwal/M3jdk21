package com.synexia.indexstring;
/**
 * Donor-side stub (differential only): a spelling with the members the donor trie core, vocabulary tree and
 * word-break plan call. equals/hashCode are spelling equality, as the donor's MIndexString.equals is.
 */
public final class MIndexString implements CharSequence {
    private final String text;
    public MIndexString(String text) { this.text = text; }
    public Object resolver() { return null; }
    public int tokenCount() { return 1; }
    public long tokenIdAt(int token) { return 0L; }
    public SubMIndexString view(int begin, int end) { return new SubMIndexString(this, begin, end); }
    @Override public int length() { return text.length(); }
    @Override public char charAt(int index) { return text.charAt(index); }
    @Override public CharSequence subSequence(int start, int end) { return text.subSequence(start, end); }
    @Override public String toString() { return text; }
    @Override public boolean equals(Object o) { return o instanceof MIndexString m && m.text.equals(text); }
    @Override public int hashCode() { return text.hashCode(); }
}
