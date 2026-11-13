public class ConcatNullProbe {
    record Probe(String v, boolean nul) { @Override public String toString() { return nul ? null : v; } }
    static String two(Probe a, Probe b) { return "<" + a + "|" + b + ">"; }
    static String obj(Object o) { return "x" + o + "y"; }
    static String str(String s, int i) { return "a" + s + "b" + i; }
    static String strOnly(String s, String t) { return "p" + s + "q" + t + "r"; }
    public static void main(String[] args) {
        String n = null;
        System.out.println("two=" + two(new Probe("A", false), new Probe("B", true)));
        System.out.println("obj=" + obj(null));
        System.out.println("str=" + str(n, 7));
        System.out.println("strOnly=" + strOnly(n, "T"));
    }
}
