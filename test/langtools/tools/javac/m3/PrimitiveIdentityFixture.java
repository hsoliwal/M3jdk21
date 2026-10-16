/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */
import static jdk.internal.mindex.M3PrimitiveIdentity.*;

/** Compile with normal javac and the opt-in launcher; behavior must agree and bytecode must differ. */
public class PrimitiveIdentityFixture {
    static int order;
    public static boolean eqBoolean(boolean a, boolean b) { return sameBoolean(a,b); }
    public static boolean eqByte(byte a, byte b) { return sameByte(a,b); }
    public static boolean eqShort(short a, short b) { return sameShort(a,b); }
    public static boolean eqChar(char a, char b) { return sameChar(a,b); }
    public static boolean eqInt(int a, int b) { return sameInt(a,b); }
    public static boolean eqLong(long a, long b) { return sameLong(a,b); }
    public static boolean eqFloat(float a, float b) { return sameFloat(a,b); }
    public static boolean eqDouble(double a, double b) { return sameDouble(a,b); }
    public static boolean keyBoolean(boolean a, boolean b) { return ofBoolean(a)==ofBoolean(b); }
    public static boolean keyByte(byte a, byte b) { return ofByte(a)==ofByte(b); }
    public static boolean keyShort(short a, short b) { return ofShort(a)!=ofShort(b); }
    public static boolean keyChar(char a, char b) { return ofChar(a)==ofChar(b); }
    public static boolean keyInt(int a, int b) { return ofInt(a)==ofInt(b); }
    public static boolean keyLong(long a, long b) { return ofLong(a)==ofLong(b); }
    public static boolean rawFloat(float a, float b) { return ofFloat(a)==ofFloat(b); }
    public static boolean rawDouble(double a, double b) { return ofDouble(a)==ofDouble(b); }
    public static boolean mixed(int a, long b) { return ofInt(a)==ofLong(b); }
    public static boolean boxed(Integer a, Integer b) { return sameInt(a,b); }
    public static boolean widen(byte a, int b) { return sameInt(a,b); }
    private static jdk.internal.mindex.M3PrimitiveIdentity qualifier() { order++; return null; }
    public static boolean qualifiedStatic(int a, int b) { return qualifier().sameInt(a,b); }
    static class Fake { static long ofInt(int x) { order++; return -x; } }
    public static boolean shadow(int a, int b) { return Fake.ofInt(a)==Fake.ofInt(b); }
    private static int left() { order=order*10+1; return 7; }
    private static int right() { order=order*10+2; return 7; }
    private static int explode() { order=order*10+3; throw new IllegalArgumentException(); }
    static void check(boolean x) { if(!x) throw new AssertionError(); }
    public static void main(String[] args) {
        check(eqBoolean(true,true)); check(eqByte((byte)-128,(byte)-128));
        check(eqShort((short)-32768,(short)-32768)); check(eqChar('￿','￿'));
        check(eqInt(Integer.MIN_VALUE,Integer.MIN_VALUE)); check(eqLong(Long.MIN_VALUE,Long.MIN_VALUE));
        check(!eqFloat(Float.NaN,Float.NaN)); check(eqFloat(-0.0f,0.0f));
        check(!eqDouble(Double.NaN,Double.NaN)); check(eqDouble(-0.0d,0.0d));
        check(keyBoolean(false,false)); check(keyByte((byte)1,(byte)1));
        check(keyShort((short)1,(short)2)); check(keyChar('x','x')); check(keyInt(42,42)); check(keyLong(-1,-1));
        check(!rawFloat(0.0f,-0.0f)); check(rawFloat(Float.NaN,Float.NaN));
        check(!rawDouble(0.0d,-0.0d)); check(rawDouble(Double.NaN,Double.NaN));
        check(mixed(42,42L)); check(boxed(1000,1000)); check(widen((byte)5,5));
        try { boxed(null,0); throw new AssertionError(); } catch(NullPointerException expected) { }
        order=0; check(shadow(42,42)); check(order==2);
        order=0; check(qualifiedStatic(42,42)); check(order==1);
        order=0; check(sameInt(left(),right())); check(order==12);
        order=0; check(ofInt(left())==ofInt(right())); check(order==12);
        order=0;
        try { sameInt(explode(),right()); throw new AssertionError(); }
        catch(IllegalArgumentException expected) { check(order==3); }
        Object a=new Object(), b=new Object(); check(a!=b);
        java.util.Random random=new java.util.Random(42);
        for(int i=0;i<20000;i++) {
            int x=random.nextInt(), y=random.nextInt(); long u=random.nextLong(), v=random.nextLong();
            float f=Float.intBitsToFloat(x), g=Float.intBitsToFloat(y);
            double d=Double.longBitsToDouble(u), e=Double.longBitsToDouble(v);
            check(eqInt(x,y)==(x==y)); check(keyInt(x,y)==(x==y));
            check(eqLong(u,v)==(u==v)); check(keyLong(u,v)==(u==v));
            check(eqFloat(f,g)==(f==g)); check(eqDouble(d,e)==(d==e));
            check(rawFloat(f,g)==(Float.floatToRawIntBits(f)==Float.floatToRawIntBits(g)));
            check(rawDouble(d,e)==(Double.doubleToRawLongBits(d)==Double.doubleToRawLongBits(e)));
        }
        System.out.println("PrimitiveIdentityFixture PASS");
    }
}
