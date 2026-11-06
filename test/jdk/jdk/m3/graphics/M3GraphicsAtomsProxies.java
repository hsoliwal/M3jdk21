/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Test-made implementations of interface atoms with default methods (M3GraphicsAtomsRecipes.PROXIED), one per side:
 * a java.lang.reflect.Proxy of the side's interface whose abstract methods return the logical value of their token for
 * the proxy's round (the same value on both sides) and whose default methods run the side's own code through
 * {@link InvocationHandler#invokeDefault}. The default methods are what the tests compare; equals, hashCode and
 * toString of the proxy itself are identity-based and never compared (M3GraphicsAtomsCompare treats two proxies of
 * the same interface as the same receiver).
 */
final class M3GraphicsAtomsProxies {
    private static final String EQUALS = "equals";
    private static final String HASH_CODE = "hashCode";
    private static final String PROXY = "proxy ";
    private static final String SALT = "proxy#";
    private static final String DOT = ".";
    private static final String VOID = "V";
    private static final int FIRST = 0;

    private M3GraphicsAtomsProxies() {}

    /** The proxy of an interface atom on one side for one round. */
    static Object proxy(String atom, int round, M3GraphicsAtomsOracle.Side side) {
        Class<?> type = M3GraphicsAtomsOracle.atom(atom, side);
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (p, m, args) -> invoke(atom, round, side, p, m, args));
    }

    private static Object invoke(String atom, int round, M3GraphicsAtomsOracle.Side side, Object proxy, Method m,
            Object[] args) throws Throwable {
        if (m.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, m, args);
        }
        if (m.getDeclaringClass() == Object.class) {
            return objectMethod(atom, proxy, m, args);
        }
        Map<String, String> tokens = M3GraphicsAtomsRecipes.PROXIED.get(atom);
        String token = tokens.get(m.getName());
        if (VOID.equals(token)) {
            return null;
        }
        String key = atom + DOT + m.getName();
        M3GraphicsAtomsValues.Context c = new M3GraphicsAtomsValues.Context(atom, key,
                M3GraphicsAtomsValues.seed(SALT + key), List.of());
        return M3GraphicsAtomsValues.materialize(M3GraphicsAtomsValues.arg(token, c, round, FIRST), side);
    }

    private static Object objectMethod(String atom, Object proxy, Method m, Object[] args) {
        return switch (m.getName()) {
            case EQUALS -> proxy == args[FIRST];
            case HASH_CODE -> System.identityHashCode(proxy);
            default -> PROXY + atom;
        };
    }
}
