/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.lang.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Array-based member selection and explicitly authorized handle conversion.
 *
 * <p>Handle factories borrow the caller-supplied {@link Lookup}; they never
 * manufacture a lookup or change an {@link AccessibleObject} access flag.
 * Access checking, receiver narrowing, variable arity and checked failures
 * follow the corresponding {@code Lookup} operation. In particular, existing
 * access overrides on reflected objects have exactly the effect documented by
 * that operation; {@code unreflectVarHandle} ignores those overrides.
 *
 * <p>Member selection keeps the supplied encounter order and member identities
 * in a detached, unmodifiable list. It does not traverse class
 * hierarchies, change the unspecified order of reflection discovery, retain
 * a global member cache or allocate a wrapper for each selected member.
 *
 * @since 21
 */
public final class Reflections {
    private Reflections() { }

    /**
     * Tests all required bits and the absence of all forbidden bits.
     * Any overlapping required and forbidden bits make the match impossible.
     * Bits are interpreted literally, including bits outside {@link Modifier}.
     * The member's modifiers are read exactly once.
     *
     * @param member member to inspect
     * @param requiredModifiers every bit that must be present
     * @param forbiddenModifiers every bit that must be absent
     * @return whether both mask conditions hold
     * @throws NullPointerException if {@code member} is null
     */
    public static boolean hasModifiers(Member member, int requiredModifiers,
                                       int forbiddenModifiers) {
        int modifiers = Objects.requireNonNull(member, "member").getModifiers();
        return matches(modifiers, requiredModifiers, forbiddenModifiers);
    }

    /**
     * Selects members from the complete supplied array.
     *
     * @param <M> member type
     * @param members source array
     * @param requiredModifiers every bit that must be present
     * @param forbiddenModifiers every bit that must be absent
     * @return a detached unmodifiable list in encounter order
     * @throws NullPointerException if the array or any member is null
     * @see #filterMembers(Member[], int, int, int, int)
     */
    public static <M extends Member> List<M> filterMembers(
            M[] members, int requiredModifiers, int forbiddenModifiers) {
        Objects.requireNonNull(members, "members");
        return filterMembers(members, 0, members.length,
                requiredModifiers, forbiddenModifiers);
    }

    /**
     * Selects members in the half-open range {@code [fromIndex, toIndex)}.
     * Each visited slot is checked for null and its modifiers read exactly
     * once, in encounter order, even when the masks overlap. Elements outside
     * the range are not examined. An empty range returns an empty list.
     * The source array must not be changed during this operation.
     *
     * @param <M> member type
     * @param members source array
     * @param fromIndex first included slot
     * @param toIndex first excluded slot
     * @param requiredModifiers every bit that must be present
     * @param forbiddenModifiers every bit that must be absent
     * @return a detached unmodifiable list in encounter order
     * @throws NullPointerException if the array or a member in the range is null
     * @throws IndexOutOfBoundsException if the range is outside the array or
     *         its lower bound exceeds its upper bound
     */
    public static <M extends Member> List<M> filterMembers(
            M[] members, int fromIndex, int toIndex,
            int requiredModifiers, int forbiddenModifiers) {
        Objects.requireNonNull(members, "members");
        Objects.checkFromToIndex(fromIndex, toIndex, members.length);
        ArrayList<M> selected = new ArrayList<>();
        for (int index = fromIndex; index < toIndex; index++) {
            M member = Objects.requireNonNull(members[index], "member");
            int modifiers = member.getModifiers();
            if (matches(modifiers, requiredModifiers, forbiddenModifiers)) {
                selected.add(member);
            }
        }
        return List.copyOf(selected);
    }

    private static boolean matches(int modifiers, int required, int forbidden) {
        return (modifiers & required) == required && (modifiers & forbidden) == 0;
    }

    /**
     * Converts a reflected method with the supplied lookup's authority.
     *
     * @param lookup caller-supplied lookup
     * @param method reflected method
     * @return the method handle produced by {@link Lookup#unreflect(Method)}
     * @throws NullPointerException if either argument is null
     * @throws IllegalAccessException if the lookup operation refuses access
     */
    public static MethodHandle unreflect(Lookup lookup, Method method)
            throws IllegalAccessException {
        Objects.requireNonNull(lookup, "lookup");
        return lookup.unreflect(Objects.requireNonNull(method, "method"));
    }

    /**
     * Converts a reflected constructor with the supplied lookup's authority.
     *
     * @param lookup caller-supplied lookup
     * @param constructor reflected constructor
     * @return the handle from {@link Lookup#unreflectConstructor(Constructor)}
     * @throws NullPointerException if either argument is null
     * @throws IllegalAccessException if the lookup operation refuses access
     */
    public static MethodHandle unreflectConstructor(Lookup lookup,
                                                    Constructor<?> constructor)
            throws IllegalAccessException {
        Objects.requireNonNull(lookup, "lookup");
        return lookup.unreflectConstructor(
                Objects.requireNonNull(constructor, "constructor"));
    }

    /**
     * Converts a reflected field to a getter with the supplied authority.
     *
     * @param lookup caller-supplied lookup
     * @param field reflected field
     * @return the handle from {@link Lookup#unreflectGetter(Field)}
     * @throws NullPointerException if either argument is null
     * @throws IllegalAccessException if the lookup operation refuses access
     */
    public static MethodHandle unreflectGetter(Lookup lookup, Field field)
            throws IllegalAccessException {
        Objects.requireNonNull(lookup, "lookup");
        return lookup.unreflectGetter(Objects.requireNonNull(field, "field"));
    }

    /**
     * Converts a reflected field to a setter with the supplied authority.
     * Final-field restrictions follow {@link Lookup#unreflectSetter(Field)}.
     *
     * @param lookup caller-supplied lookup
     * @param field reflected field
     * @return the field setter handle
     * @throws NullPointerException if either argument is null
     * @throws IllegalAccessException if the lookup operation refuses access
     */
    public static MethodHandle unreflectSetter(Lookup lookup, Field field)
            throws IllegalAccessException {
        Objects.requireNonNull(lookup, "lookup");
        return lookup.unreflectSetter(Objects.requireNonNull(field, "field"));
    }

    /**
     * Converts a reflected field to a variable handle with the supplied
     * authority. The field's access override flag is ignored, as specified by
     * {@link Lookup#unreflectVarHandle(Field)}.
     *
     * @param lookup caller-supplied lookup
     * @param field reflected field
     * @return the field variable handle
     * @throws NullPointerException if either argument is null
     * @throws IllegalAccessException if the lookup operation refuses access
     */
    public static VarHandle unreflectVarHandle(Lookup lookup, Field field)
            throws IllegalAccessException {
        Objects.requireNonNull(lookup, "lookup");
        return lookup.unreflectVarHandle(Objects.requireNonNull(field, "field"));
    }
}
