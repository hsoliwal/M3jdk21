/* *****************************************************************************
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

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Logger with {@code {}} placeholders, backed by {@link System.Logger}.
 *
 * <p>Absorbed donor classes that logged through a {@code {}}-placeholder API keep their call sites and their message
 * strings unchanged: the method names and descriptors here are the ones those call sites already use, so a relocated
 * call site differs from the donor only in the owner type. Formatting follows the established {@code {}} convention:
 * each {@code {}} takes the next argument, {@code \{}} is a literal {@code {}}, {@code \\{}} is a backslash followed
 * by a substituted argument, arrays print their elements, and a trailing {@link Throwable} argument is logged as the
 * throwable rather than formatted. Arguments are formatted only when the level is enabled.</p>
 *
 * <p>User decision 2026-11-09 ("Internal {} facade"): the one logging owner for absorbed code; lombok
 * {@code @Slf4j} becomes {@code private static final PlaceholderLogger log = PlaceholderLogger.of(Owner.class)}.</p>
 */
@FunctionalInterface
public interface PlaceholderLogger {

    /** Returns the logger named after {@code owner}'s binary name. */
    static PlaceholderLogger of(Class<?> owner) {
        return of(owner.getName());
    }

    /** Returns the logger named {@code name}. */
    static PlaceholderLogger of(String name) {
        System.Logger logger = System.getLogger(name);
        return () -> logger;
    }

    /** Returns the backing system logger. */
    System.Logger delegate();

    /** Returns the logger's name. */
    default String getName() { return delegate().getName(); }

    /** Returns whether TRACE is enabled. */
    default boolean isTraceEnabled() { return delegate().isLoggable(Level.TRACE); }
    /** Returns whether DEBUG is enabled. */
    default boolean isDebugEnabled() { return delegate().isLoggable(Level.DEBUG); }
    /** Returns whether INFO is enabled. */
    default boolean isInfoEnabled() { return delegate().isLoggable(Level.INFO); }
    /** Returns whether WARNING is enabled. */
    default boolean isWarnEnabled() { return delegate().isLoggable(Level.WARNING); }
    /** Returns whether ERROR is enabled. */
    default boolean isErrorEnabled() { return delegate().isLoggable(Level.ERROR); }

    /** Logs {@code msg} at TRACE. */
    default void trace(String msg) { emit(Level.TRACE, msg, null); }
    /** Logs {@code format} with one argument at TRACE. */
    default void trace(String format, Object arg) { emit(Level.TRACE, format, new Object[] {arg}); }
    /** Logs {@code format} with two arguments at TRACE. */
    default void trace(String format, Object arg1, Object arg2) { emit(Level.TRACE, format, new Object[] {arg1, arg2}); }
    /** Logs {@code format} with arguments at TRACE. */
    default void trace(String format, Object... args) { emit(Level.TRACE, format, args); }
    /** Logs {@code msg} and {@code t} at TRACE. */
    default void trace(String msg, Throwable t) { emit(Level.TRACE, msg, new Object[] {t}); }

    /** Logs {@code msg} at DEBUG. */
    default void debug(String msg) { emit(Level.DEBUG, msg, null); }
    /** Logs {@code format} with one argument at DEBUG. */
    default void debug(String format, Object arg) { emit(Level.DEBUG, format, new Object[] {arg}); }
    /** Logs {@code format} with two arguments at DEBUG. */
    default void debug(String format, Object arg1, Object arg2) { emit(Level.DEBUG, format, new Object[] {arg1, arg2}); }
    /** Logs {@code format} with arguments at DEBUG. */
    default void debug(String format, Object... args) { emit(Level.DEBUG, format, args); }
    /** Logs {@code msg} and {@code t} at DEBUG. */
    default void debug(String msg, Throwable t) { emit(Level.DEBUG, msg, new Object[] {t}); }

    /** Logs {@code msg} at INFO. */
    default void info(String msg) { emit(Level.INFO, msg, null); }
    /** Logs {@code format} with one argument at INFO. */
    default void info(String format, Object arg) { emit(Level.INFO, format, new Object[] {arg}); }
    /** Logs {@code format} with two arguments at INFO. */
    default void info(String format, Object arg1, Object arg2) { emit(Level.INFO, format, new Object[] {arg1, arg2}); }
    /** Logs {@code format} with arguments at INFO. */
    default void info(String format, Object... args) { emit(Level.INFO, format, args); }
    /** Logs {@code msg} and {@code t} at INFO. */
    default void info(String msg, Throwable t) { emit(Level.INFO, msg, new Object[] {t}); }

    /** Logs {@code msg} at WARNING. */
    default void warn(String msg) { emit(Level.WARNING, msg, null); }
    /** Logs {@code format} with one argument at WARNING. */
    default void warn(String format, Object arg) { emit(Level.WARNING, format, new Object[] {arg}); }
    /** Logs {@code format} with two arguments at WARNING. */
    default void warn(String format, Object arg1, Object arg2) { emit(Level.WARNING, format, new Object[] {arg1, arg2}); }
    /** Logs {@code format} with arguments at WARNING. */
    default void warn(String format, Object... args) { emit(Level.WARNING, format, args); }
    /** Logs {@code msg} and {@code t} at WARNING. */
    default void warn(String msg, Throwable t) { emit(Level.WARNING, msg, new Object[] {t}); }

    /** Logs {@code msg} at ERROR. */
    default void error(String msg) { emit(Level.ERROR, msg, null); }
    /** Logs {@code format} with one argument at ERROR. */
    default void error(String format, Object arg) { emit(Level.ERROR, format, new Object[] {arg}); }
    /** Logs {@code format} with two arguments at ERROR. */
    default void error(String format, Object arg1, Object arg2) { emit(Level.ERROR, format, new Object[] {arg1, arg2}); }
    /** Logs {@code format} with arguments at ERROR. */
    default void error(String format, Object... args) { emit(Level.ERROR, format, args); }
    /** Logs {@code msg} and {@code t} at ERROR. */
    default void error(String msg, Throwable t) { emit(Level.ERROR, msg, new Object[] {t}); }

    /**
     * Formats {@code pattern} with {@code args} by the {@code {}} rules above. A trailing {@link Throwable} is not
     * consumed; {@link #throwableOf(Object[])} returns it.
     */
    static String format(String pattern, Object[] args) {
        if (pattern == null) return null;
        int usable = args == null ? 0 : args.length - (throwableOf(args) != null ? 1 : 0);
        StringBuilder out = new StringBuilder(pattern.length() + 50);
        int from = 0;
        int index = 0;
        while (index < usable) {
            int at = pattern.indexOf("{}", from);
            if (at < 0) break;
            if (substitute(out, pattern, from, at, args[index])) index++;
            from = at + 2;
        }
        return out.append(pattern, from, pattern.length()).toString();
    }

    /** Returns the trailing {@link Throwable} of {@code args}, or {@code null}. */
    static Throwable throwableOf(Object[] args) {
        if (args == null || args.length == 0) return null;
        return args[args.length - 1] instanceof Throwable t ? t : null;
    }

    /**
     * Appends {@code pattern[from, at)} and resolves the placeholder at {@code at}. Returns {@code true} when
     * {@code arg} was consumed, {@code false} when the placeholder was escaped ({@code \{}}) and printed literally.
     */
    private static boolean substitute(StringBuilder out, String pattern, int from, int at, Object arg) {
        boolean escaped = at > 0 && pattern.charAt(at - 1) == '\\';
        boolean doubly = escaped && at > 1 && pattern.charAt(at - 2) == '\\';
        if (escaped && !doubly) {
            out.append(pattern, from, at - 1).append("{}");
            return false;
        }
        out.append(pattern, from, doubly ? at - 1 : at);
        appendValue(out, arg, new IdentityHashMap<>());
        return true;
    }

    /** Appends {@code value}; arrays print their elements and a recursive array prints {@code [...]}. */
    private static void appendValue(StringBuilder out, Object value, Map<Object[], Boolean> seen) {
        if (value == null || !value.getClass().isArray()) {
            out.append(safeToString(value));
        } else if (value instanceof Object[] array) {
            if (seen.put(array, Boolean.TRUE) != null) {
                out.append("[...]");
                return;
            }
            out.append('[');
            for (int i = 0; i < array.length; i++) {
                if (i > 0) out.append(", ");
                appendValue(out, array[i], seen);
            }
            out.append(']');
            seen.remove(array);
        } else {
            out.append(primitiveArrayToString(value));
        }
    }

    /** Returns {@code String.valueOf(value)}, or a marker when {@code toString()} throws. */
    private static String safeToString(Object value) {
        try {
            return String.valueOf(value);
        } catch (RuntimeException failed) {
            return "[FAILED toString()]";
        }
    }

    /** Returns the {@link Arrays#toString} form of a primitive array. */
    private static String primitiveArrayToString(Object array) {
        return switch (array) {
            case boolean[] a -> Arrays.toString(a);
            case byte[] a -> Arrays.toString(a);
            case char[] a -> Arrays.toString(a);
            case short[] a -> Arrays.toString(a);
            case int[] a -> Arrays.toString(a);
            case long[] a -> Arrays.toString(a);
            case float[] a -> Arrays.toString(a);
            case double[] a -> Arrays.toString(a);
            default -> String.valueOf(array);
        };
    }

    /** Formats and logs when {@code level} is enabled; a trailing throwable is passed to the system logger. */
    private void emit(Level level, String format, Object[] args) {
        System.Logger logger = delegate();
        if (!logger.isLoggable(level)) return;
        Throwable thrown = throwableOf(args);
        String message = args == null ? format : format(format, args);
        if (thrown != null) {
            logger.log(level, message, thrown);
        } else {
            logger.log(level, message);
        }
    }
}
