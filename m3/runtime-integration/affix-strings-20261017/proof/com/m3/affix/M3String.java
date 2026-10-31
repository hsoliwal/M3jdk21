package com.m3.affix;
final class M3String implements CharSequence {
    private final String text;
    M3String(String value) { text = value; }
    M3StringFacts facts() {
        long b=0L;
        for(int i=0;i<text.length();i++) b |= M3StringFacts.codeUnitSignal(text.charAt(i));
        return new M3StringFacts(b);
    }
    public int length() { return text.length(); }
    public char charAt(int i) { return text.charAt(i); }
    public CharSequence subSequence(int a,int b) { return text.subSequence(a,b); }
    public String toString() { return text; }
}
