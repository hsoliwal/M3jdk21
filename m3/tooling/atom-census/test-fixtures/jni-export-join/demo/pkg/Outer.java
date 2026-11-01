package demo.pkg;
public class Outer {
    static { System.loadLibrary("m3jni_projection"); }
    public static native int count(int[] values);
    public static native int ping();
    public native int overloaded(int n);
    public native int overloaded(byte[] n);
    public static class Inner { public native int do_Thing(int x); }
    void local() { class Local { native int hidden(int n); } }
}
