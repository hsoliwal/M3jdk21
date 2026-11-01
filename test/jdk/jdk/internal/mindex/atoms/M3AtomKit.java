/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Shared harness of the per-atom jdk.internal.mindex tests (ATOM-TEST KIT v1).
 *
 * <p>Package-private atoms are reached through {@link MethodHandles#privateLookupIn}; the tests
 * therefore declare {@code @modules java.base/jdk.internal.mindex:+open}. Every random input is
 * drawn from a {@link SplittableRandom} seeded by {@link #SEED} and the atom name, so a failure
 * replays exactly.</p>
 */
final class M3AtomKit {
    /** Package of the atoms under test. */
    static final String PACKAGE = "jdk.internal.mindex.";
    /** Base seed of every generated input sequence ("M3ATOMS!" in ASCII). */
    static final long SEED = 0x4D3341544F4D5321L;
    /** Number of generated operations of one differential sequence. */
    static final int OPERATIONS = 2_000;
    /** Number of generated cases of one structural check. */
    static final int CASES = 200;

    private M3AtomKit() {
    }

    /** Throwing body of a check. */
    @FunctionalInterface
    interface Body {
        void run() throws Throwable;
    }

    /** Loads a type of the package by simple (or {@code Outer$Inner}) name. */
    static Class<?> type(String simpleName) {
        try {
            return Class.forName(PACKAGE + simpleName, true, Object.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("atom owner missing: " + simpleName, e);
        }
    }

    /** Full-privilege lookup in the owner (the package is opened to the test). */
    static MethodHandles.Lookup lookup(Class<?> owner) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new AssertionError("package not opened to the test: " + owner.getName(), e);
        }
    }

    static MethodHandle findStatic(Class<?> owner, String name, MethodType type) {
        try {
            return lookup(owner).findStatic(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("static atom missing: " + owner.getSimpleName() + "." + name + type, e);
        }
    }

    static MethodHandle findVirtual(Class<?> owner, String name, MethodType type) {
        try {
            return lookup(owner).findVirtual(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("instance atom missing: " + owner.getSimpleName() + "." + name + type, e);
        }
    }

    static MethodHandle findConstructor(Class<?> owner, MethodType type) {
        try {
            return lookup(owner).findConstructor(owner, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("constructor atom missing: " + owner.getSimpleName() + type, e);
        }
    }

    /** Constructor atom of the named class: the simple name must match, so the handle is the intended atom. */
    static MethodHandle findConstructor(Class<?> owner, String simpleName, MethodType type) {
        assertEquals(simpleName, owner.getSimpleName(), "constructor atom owner");
        return findConstructor(owner, type);
    }

    static MethodHandle findGetter(Class<?> owner, String name, Class<?> type) {
        try {
            return lookup(owner).findGetter(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("field missing: " + owner.getSimpleName() + "." + name, e);
        }
    }

    static MethodHandle findStaticGetter(Class<?> owner, String name, Class<?> type) {
        try {
            return lookup(owner).findStaticGetter(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("static field missing: " + owner.getSimpleName() + "." + name, e);
        }
    }

    /** Deterministic generator of one atom's inputs. */
    static SplittableRandom random(String atom) {
        return new SplittableRandom(SEED ^ atom.hashCode());
    }

    /** Word width used by the boundary probes of {@link #intDomain}. */
    private static final int WORD = Long.SIZE;
    /** Number of word boundaries probed on each side of an interval end. */
    private static final int BOUNDARY_WORDS = 3;

    /**
     * Inputs of an int atom over [lo, hi]: both ends and their neighbours, the values around the
     * first word boundaries, then {@link #OPERATIONS} uniform samples.
     */
    static int[] intDomain(String atom, int lo, int hi) {
        java.util.TreeSet<Integer> probes = new java.util.TreeSet<>();
        long[] anchors = {lo, (long) lo + 1, (long) hi - 1, hi};
        for (long anchor : anchors) {
            if (anchor >= lo && anchor <= hi) {
                probes.add((int) anchor);
            }
        }
        for (int k = 1; k <= BOUNDARY_WORDS; k++) {
            for (long v : new long[] {(long) k * WORD - 1, (long) k * WORD, (long) k * WORD + 1}) {
                if (v >= lo && v <= hi) {
                    probes.add((int) v);
                }
            }
        }
        SplittableRandom random = random(atom);
        int[] out = new int[probes.size() + OPERATIONS];
        int n = 0;
        for (int p : probes) {
            out[n++] = p;
        }
        for (int i = 0; i < OPERATIONS; i++) {
            out[n++] = (int) random.nextLong(lo, (long) hi + 1);
        }
        return out;
    }

    /**
     * Utility-class constructor contract: the class is final, holds no instance state and declares
     * exactly one constructor, private and without parameters. The constructor still links
     * (a private lookup can create an instance) and creating one has no observable effect.
     */
    static void assertUtilityConstructor(Class<?> owner) throws Throwable {
        assertTrue(Modifier.isFinal(owner.getModifiers()), owner + " must be final");
        Constructor<?>[] constructors = owner.getDeclaredConstructors();
        assertEquals(1, constructors.length, owner + " constructors");
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()), owner + " constructor must be private");
        assertEquals(0, constructors[0].getParameterCount(), owner + " constructor parameters");
        assertTrue(Arrays.stream(owner.getDeclaredFields()).allMatch(f -> Modifier.isStatic(f.getModifiers())),
                owner + " must not declare instance state");
        Object instance = findConstructor(owner, MethodType.methodType(void.class)).invoke();
        assertNotNull(instance);
        assertEquals(owner, instance.getClass());
    }

    /**
     * Native-lane contract: the atom is a static native method. On a runtime whose libjava does not
     * carry the M3 symbol the call fails with {@link UnsatisfiedLinkError}; when it is linked, the
     * result must equal the Java reference computed by {@code linkedCheck}.
     */
    static boolean assertNativeLane(Class<?> owner, String name, Body call) throws Throwable {
        Method method = Arrays.stream(owner.getDeclaredMethods())
                .filter(m -> m.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("native atom missing: " + name));
        assertTrue(Modifier.isNative(method.getModifiers()), name + " must be native");
        assertTrue(Modifier.isStatic(method.getModifiers()), name + " must be static");
        try {
            call.run();
            return true;
        } catch (UnsatisfiedLinkError unlinked) {
            assertTrue(unlinked.getMessage() == null || unlinked.getMessage().contains(name),
                    "unexpected link failure " + unlinked);
            return false;
        }
    }

    /** Runs {@code body} and returns the throwable it raised, failing when it completes normally. */
    static <T extends Throwable> T expect(Class<T> type, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            throw new AssertionError("expected " + type.getName() + " but got " + t, t);
        }
        fail("expected " + type.getName());
        return null;
    }

    /** Asserts that {@code body} throws {@code type} with a message containing {@code fragment}. */
    static void expectMessage(Class<? extends Throwable> type, String fragment, Body body) {
        Throwable t = expect(type, body);
        assertNotNull(t.getMessage(), "message of " + t);
        assertTrue(t.getMessage().contains(fragment), "message '" + t.getMessage() + "' lacks '" + fragment + "'");
    }

    /** Asserts the two arrays differ in identity but not in content (defensive copy contract). */
    static void assertCopy(long[] expected, long[] actual) {
        assertFalse(expected == actual, "defensive copy expected");
        assertTrue(Arrays.equals(expected, actual), Arrays.toString(expected) + " != " + Arrays.toString(actual));
    }
}
