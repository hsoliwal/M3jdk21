/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.lang.invoke.*;
import java.util.Arrays;
import java.util.function.*;
/**
 * VarHandleOps — VarHandle factory, typed array handles, generic/int/long/boolean
 * get/set/CAS/add/fence/lazy-init/counter/flag utilities.
 *
 * @see jdk.internal.m3.collections.LazyRef
 * @see jdk.internal.m3.collections.AtomicRef
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/VarHandleOps.java}
 * blob {@code 74a7e97ebac1db5e9ae68d0b3a4da373fe4b8035} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class VarHandleOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(VarHandleOps.class);

    private VarHandleOps() {}

    // --- Factory ---
    public static VarHandle findVarHandle(Class<?> c, String name, Class<?> type) { try { return MethodHandles.lookup().findVarHandle(c, name, type); } catch (ReflectiveOperationException e) { throw new RuntimeException(c.getName() + "." + name, e); } }
    public static VarHandle arrayHandle(Class<?> arr) { return MethodHandles.arrayElementVarHandle(arr); }
    public static VarHandle findStatic(Class<?> c, String name, Class<?> type) { try { return MethodHandles.lookup().findStaticVarHandle(c, name, type); } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); } }
    public static VarHandle findPrivate(MethodHandles.Lookup l, Class<?> c, String name, Class<?> type) { try { return l.findVarHandle(c, name, type); } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); } }
    public static VarHandle findStaticPrivate(MethodHandles.Lookup l, Class<?> c, String name, Class<?> type) { try { return l.findStaticVarHandle(c, name, type); } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); } }

    // --- Typed array handles ---
    public static VarHandle intArrayHandle() { return MethodHandles.arrayElementVarHandle(int[].class); }
    public static VarHandle longArrayHandle() { return MethodHandles.arrayElementVarHandle(long[].class); }
    public static VarHandle objectArrayHandle() { return MethodHandles.arrayElementVarHandle(Object[].class); }
    public static VarHandle booleanArrayHandle() { return MethodHandles.arrayElementVarHandle(boolean[].class); }
    public static VarHandle byteArrayHandle() { return MethodHandles.arrayElementVarHandle(byte[].class); }
    public static VarHandle shortArrayHandle() { return MethodHandles.arrayElementVarHandle(short[].class); }
    public static VarHandle charArrayHandle() { return MethodHandles.arrayElementVarHandle(char[].class); }
    public static VarHandle floatArrayHandle() { return MethodHandles.arrayElementVarHandle(float[].class); }
    public static VarHandle doubleArrayHandle() { return MethodHandles.arrayElementVarHandle(double[].class); }

    // --- Generic get/set ---
    @SuppressWarnings("unchecked") public static <T> T get(VarHandle h, Object o) { return (T) h.get(o); }
    public static <T> void set(VarHandle h, Object o, T v) { h.set(o, v); }
    @SuppressWarnings("unchecked") public static <T> T getVolatile(VarHandle h, Object o) { return (T) h.getVolatile(o); }
    public static <T> void setVolatile(VarHandle h, Object o, T v) { h.setVolatile(o, v); }
    @SuppressWarnings("unchecked") public static <T> T getAcquire(VarHandle h, Object o) { return (T) h.getAcquire(o); }
    public static <T> void setRelease(VarHandle h, Object o, T v) { h.setRelease(o, v); }
    @SuppressWarnings("unchecked") public static <T> T getOpaque(VarHandle h, Object o) { return (T) h.getOpaque(o); }
    public static <T> void setOpaque(VarHandle h, Object o, T v) { h.setOpaque(o, v); }

    // --- CAS ---
    public static <T> boolean cas(VarHandle h, Object o, T exp, T upd) { return h.compareAndSet(o, exp, upd); }
    public static <T> boolean casAcquire(VarHandle h, Object o, T exp, T upd) { return h.weakCompareAndSetAcquire(o, exp, upd); }
    public static <T> boolean casRelease(VarHandle h, Object o, T exp, T upd) { return h.weakCompareAndSetRelease(o, exp, upd); }
    public static <T> boolean casPlain(VarHandle h, Object o, T exp, T upd) { return h.weakCompareAndSetPlain(o, exp, upd); }
    @SuppressWarnings("unchecked") public static <T> T compareAndExchange(VarHandle h, Object o, T exp, T upd) { return (T) h.compareAndExchange(o, exp, upd); }
    @SuppressWarnings("unchecked") public static <T> T compareAndExchangeAcquire(VarHandle h, Object o, T exp, T upd) { return (T) h.compareAndExchangeAcquire(o, exp, upd); }
    @SuppressWarnings("unchecked") public static <T> T compareAndExchangeRelease(VarHandle h, Object o, T exp, T upd) { return (T) h.compareAndExchangeRelease(o, exp, upd); }
    @SuppressWarnings("unchecked") public static <T> T getAndSet(VarHandle h, Object o, T v) { return (T) h.getAndSet(o, v); }
    @SuppressWarnings("unchecked") public static <T> T getAndSetAcquire(VarHandle h, Object o, T v) { return (T) h.getAndSetAcquire(o, v); }
    @SuppressWarnings("unchecked") public static <T> T getAndSetRelease(VarHandle h, Object o, T v) { return (T) h.getAndSetRelease(o, v); }

    // --- Int ops ---
    public static int getInt(VarHandle h, Object o) { return (int) h.get(o); }
    public static void setInt(VarHandle h, Object o, int v) { h.set(o, v); }
    public static int getIntVolatile(VarHandle h, Object o) { return (int) h.getVolatile(o); }
    public static void setIntVolatile(VarHandle h, Object o, int v) { h.setVolatile(o, v); }
    public static int getIntAcquire(VarHandle h, Object o) { return (int) h.getAcquire(o); }
    public static void setIntRelease(VarHandle h, Object o, int v) { h.setRelease(o, v); }
    public static boolean casInt(VarHandle h, Object o, int exp, int upd) { return h.compareAndSet(o, exp, upd); }
    public static int getAndSetInt(VarHandle h, Object o, int v) { return (int) h.getAndSet(o, v); }
    public static int getAndAddInt(VarHandle h, Object o, int d) { return (int) h.getAndAdd(o, d); }
    public static int addAndGetInt(VarHandle h, Object o, int d) { return (int) h.getAndAdd(o, d) + d; }
    public static int incrementAndGetInt(VarHandle h, Object o) { return addAndGetInt(h, o, 1); }
    public static int decrementAndGetInt(VarHandle h, Object o) { return addAndGetInt(h, o, -1); }
    public static int getAndIncrementInt(VarHandle h, Object o) { return getAndAddInt(h, o, 1); }
    public static int getAndDecrementInt(VarHandle h, Object o) { return getAndAddInt(h, o, -1); }
    public static int getAndBitwiseOrInt(VarHandle h, Object o, int m) { return (int) h.getAndBitwiseOr(o, m); }
    public static int getAndBitwiseAndInt(VarHandle h, Object o, int m) { return (int) h.getAndBitwiseAnd(o, m); }
    public static int getAndBitwiseXorInt(VarHandle h, Object o, int m) { return (int) h.getAndBitwiseXor(o, m); }

    // --- Long ops ---
    public static long getLong(VarHandle h, Object o) { return (long) h.get(o); }
    public static void setLong(VarHandle h, Object o, long v) { h.set(o, v); }
    public static long getLongVolatile(VarHandle h, Object o) { return (long) h.getVolatile(o); }
    public static void setLongVolatile(VarHandle h, Object o, long v) { h.setVolatile(o, v); }
    public static long getLongAcquire(VarHandle h, Object o) { return (long) h.getAcquire(o); }
    public static void setLongRelease(VarHandle h, Object o, long v) { h.setRelease(o, v); }
    public static boolean casLong(VarHandle h, Object o, long exp, long upd) { return h.compareAndSet(o, exp, upd); }
    public static long getAndSetLong(VarHandle h, Object o, long v) { return (long) h.getAndSet(o, v); }
    public static long getAndAddLong(VarHandle h, Object o, long d) { return (long) h.getAndAdd(o, d); }
    public static long addAndGetLong(VarHandle h, Object o, long d) { return (long) h.getAndAdd(o, d) + d; }
    public static long incrementAndGetLong(VarHandle h, Object o) { return addAndGetLong(h, o, 1L); }
    public static long decrementAndGetLong(VarHandle h, Object o) { return addAndGetLong(h, o, -1L); }
    public static long getAndIncrementLong(VarHandle h, Object o) { return getAndAddLong(h, o, 1L); }
    public static long getAndDecrementLong(VarHandle h, Object o) { return getAndAddLong(h, o, -1L); }
    public static long getAndBitwiseOrLong(VarHandle h, Object o, long m) { return (long) h.getAndBitwiseOr(o, m); }
    public static long getAndBitwiseAndLong(VarHandle h, Object o, long m) { return (long) h.getAndBitwiseAnd(o, m); }
    public static long getAndBitwiseXorLong(VarHandle h, Object o, long m) { return (long) h.getAndBitwiseXor(o, m); }

    // --- Boolean ops ---
    public static boolean getBoolean(VarHandle h, Object o) { return (boolean) h.get(o); }
    public static void setBoolean(VarHandle h, Object o, boolean v) { h.set(o, v); }
    public static boolean getBooleanVolatile(VarHandle h, Object o) { return (boolean) h.getVolatile(o); }
    public static void setBooleanVolatile(VarHandle h, Object o, boolean v) { h.setVolatile(o, v); }
    public static boolean getBooleanAcquire(VarHandle h, Object o) { return (boolean) h.getAcquire(o); }
    public static void setBooleanRelease(VarHandle h, Object o, boolean v) { h.setRelease(o, v); }
    public static boolean casBoolean(VarHandle h, Object o, boolean exp, boolean upd) { return h.compareAndSet(o, exp, upd); }
    public static boolean getAndSetBoolean(VarHandle h, Object o, boolean v) { return (boolean) h.getAndSet(o, v); }

    // --- Fences ---
    public static void fullFence() { VarHandle.fullFence(); }
    public static void acquireFence() { VarHandle.acquireFence(); }
    public static void releaseFence() { VarHandle.releaseFence(); }
    public static void loadLoadFence() { VarHandle.loadLoadFence(); }
    public static void storeStoreFence() { VarHandle.storeStoreFence(); }

    // --- Atomic update ---
    @SuppressWarnings("unchecked") public static <T> T updateAndGet(VarHandle h, Object o, UnaryOperator<T> f) { T p, n; do { p = (T) h.getAcquire(o); n = f.apply(p); } while (!h.compareAndSet(o, p, n)); return n; }
    @SuppressWarnings("unchecked") public static <T> T getAndUpdate(VarHandle h, Object o, UnaryOperator<T> f) { T p, n; do { p = (T) h.getAcquire(o); n = f.apply(p); } while (!h.compareAndSet(o, p, n)); return p; }
    public static long updateAndGetLong(VarHandle h, Object o, LongUnaryOperator f) { long p, n; do { p = (long) h.getAcquire(o); n = f.applyAsLong(p); } while (!h.compareAndSet(o, p, n)); return n; }
    public static long accumulateAndGetLong(VarHandle h, Object o, long x, LongBinaryOperator f) { long p, n; do { p = (long) h.getAcquire(o); n = f.applyAsLong(p, x); } while (!h.compareAndSet(o, p, n)); return n; }

    // --- Lazy init ---
    @SuppressWarnings("unchecked") public static <T> T lazyInit(VarHandle h, Object o, Supplier<T> f) { T v = (T) h.getAcquire(o); if (v != null) return v; T nv = f.get(); return h.compareAndSet(o, null, nv) ? nv : (T) h.getAcquire(o); }
    @SuppressWarnings("unchecked") public static <T> T lazyInitVolatile(VarHandle h, Object o, Supplier<T> f) { T v = (T) h.getVolatile(o); if (v != null) return v; synchronized (o) { v = (T) h.getVolatile(o); if (v != null) return v; v = f.get(); h.setVolatile(o, v); return v; } }

    // --- Counter/Flag helpers ---
    public static long nextLong(VarHandle h, Object o) { return getAndAddLong(h, o, 1L); }
    public static int nextInt(VarHandle h, Object o) { return getAndAddInt(h, o, 1); }
    public static boolean setFlagIfUnset(VarHandle h, Object o) { return casBoolean(h, o, false, true); }
    public static boolean clearFlagIfSet(VarHandle h, Object o) { return casBoolean(h, o, true, false); }
    public static boolean toggleFlag(VarHandle h, Object o) { boolean p; do { p = getBooleanAcquire(h, o); } while (!h.compareAndSet(o, p, !p)); return !p; }

    // --- CLI ---
    public static void main(String[] args) {
        if (args.length == 0) { log.info("VarHandleOps CLI: list|demo"); System.exit(1); }
        switch (args[0].toLowerCase()) {
            case "list" -> log.info("Ops: findVarHandle, arrayHandle, get/set, CAS, int/long/boolean ops, fences, lazyInit, counter/flag");
            case "demo" -> { int[] a = {0, 0, 0}; var vh = intArrayHandle(); log.info("Init: {}", Arrays.toString(a)); vh.set(a, 0, 10); log.info("set(0,10): {}", Arrays.toString(a)); boolean ok = vh.compareAndSet(a, 1, 0, 20); log.info("CAS(1,0->20)={}: {}", ok, Arrays.toString(a)); int p = (int) vh.getAndAdd(a, 2, 5); log.info("getAndAdd(2,5)={}: {}", p, Arrays.toString(a)); }
            default -> { log.error("Unknown: {}", args[0]); System.exit(1); }
        }
    }
}
