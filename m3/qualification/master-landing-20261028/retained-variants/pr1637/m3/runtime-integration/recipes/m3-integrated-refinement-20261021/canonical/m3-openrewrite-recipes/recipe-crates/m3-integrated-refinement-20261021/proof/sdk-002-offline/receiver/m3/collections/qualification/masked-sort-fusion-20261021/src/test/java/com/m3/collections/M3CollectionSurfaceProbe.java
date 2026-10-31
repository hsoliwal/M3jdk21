/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Effective callable descriptors plus explicitly observable endpoint declaration metadata. */
public final class M3CollectionSurfaceProbe {
    private M3CollectionSurfaceProbe() { }

    public static void main(String[] args) throws ReflectiveOperationException {
        for (Class<?> owner : new Class<?>[] {M3FilteredViews.class,
                M3FilteredNavigableSet.class, M3IndexedListSnapshot.class}) {
            TreeSet<String> descriptors = new TreeSet<>();
            for (Method method : owner.getMethods()) {
                String parameters = Arrays.stream(method.getParameterTypes()).map(Class::getName)
                        .collect(Collectors.joining(","));
                descriptors.add("CALL\t" + owner.getName() + "\t" + method.getName()
                        + "(" + parameters + "):" + method.getReturnType().getName()
                        + "\tstatic=" + Modifier.isStatic(method.getModifiers()));
            }
            descriptors.forEach(System.out::println);
        }
        for (String name : new String[] {"removeFirst", "removeLast"}) {
            Method method = M3FilteredNavigableSet.class.getMethod(name);
            System.out.println("ENDPOINT\t" + name + "\tdeclaring=" + method.getDeclaringClass().getName()
                    + "\tdefault=" + method.isDefault());
        }
    }
}
