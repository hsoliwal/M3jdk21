/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Random;

/**
 * Wiring shared by the generated per-atom tests of one package: seeds derived from the atom name (reproducible
 * reruns, no magic seeds), and handles on private members for the white-box checks of private atoms.
 */
final class AtomSupport {

    /** Base of every seed; a check's seed is derived from its atom name. */
    static final long SEED_BASE = 20261031L;
    private static final int SEED_MIX = 31;

    private AtomSupport() {
    }

    static long seedOf(String atom) {
        return SEED_BASE * SEED_MIX + atom.hashCode();
    }

    static Random random(String atom) {
        return new Random(seedOf(atom));
    }

    static MethodHandles.Lookup lookupIn(Class<?> owner) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new AssertionError("the package of " + owner.getName() + " is not open to the test", e);
        }
    }

    static MethodHandle virtual(Class<?> owner, String name, Class<?> returns, Class<?>... params) {
        try {
            return lookupIn(owner).findVirtual(owner, name, MethodType.methodType(returns, params));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(owner.getName() + "." + name, e);
        }
    }

    static MethodHandle statik(Class<?> owner, String name, Class<?> returns, Class<?>... params) {
        try {
            return lookupIn(owner).findStatic(owner, name, MethodType.methodType(returns, params));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(owner.getName() + "." + name, e);
        }
    }

    static VarHandle field(Class<?> owner, String name, Class<?> type) {
        try {
            return lookupIn(owner).findVarHandle(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(owner.getName() + "." + name, e);
        }
    }

    /** Invokes {@code h} with {@code args}; what the atom throws is rethrown unchanged. */
    static Object call(MethodHandle h, Object... args) {
        try {
            return h.invokeWithArguments(args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
