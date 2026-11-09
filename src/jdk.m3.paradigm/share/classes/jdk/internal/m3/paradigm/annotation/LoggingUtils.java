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
package jdk.internal.m3.paradigm.annotation;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
/**
 * LoggingUtils - Common logging utilities.
 *
 * <p>
 * Centralized logging helpers for consistent logging patterns across the module
 * with support for structured logging.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Log operation start
 * LoggingUtils.logStart(log, "Processing spec", "spec", specName);
 *
 * // Log operation completion
 * LoggingUtils.logComplete(log, "Processing spec", "spec", specName, "status", "SUCCESS");
 *
 * // Log with duration
 * long startTime = System.currentTimeMillis();
 * // ... do work ...
 * LoggingUtils.logWithDuration(log, "Generation", "Generated code", startTime, "spec", specName);
 * }</pre>
 *
 * @since m3-anop 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/utils/LoggingUtils.java}
 * blob {@code 2dae04ad425bfe1cfa5a30e983dd4f906565ce5f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@SuppressWarnings("regexp")
public final class LoggingUtils {

    private static final PlaceholderLogger log = PlaceholderLogger.of(LoggingUtils.class);

    private LoggingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Log operation start.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message
     * @param key
     *            the context key
     * @param value
     *            the context value
     */
    public static void logStart(Object logger, String message, String key, Object value) {
        logInfo(logger, message + ": { } = { }", key, value);
    }

    /**
     * Log operation completion.
     *
     * @param logger
     *            the logger
     * @param operation
     *            the operation name
     * @param details
     *            detail key-value pairs (alternating key, value)
     */
    public static void logComplete(Object logger, String operation, Object... details) {
        var msg = new StringBuilder(operation).append(" completed");
        if (details.length > 0) {
            msg.append(':');
            for (int i = 0; i < details.length; i += 2) {
                msg.append(' ').append(details[i]).append('=');
                if (i + 1 < details.length) {
                    msg.append(details[i + 1]);
                }
                if (i + 2 < details.length) {
                    msg.append(',');
                }
            }
        }
        logInfo(logger, msg.toString());
    }

    /**
     * Log operation with duration.
     *
     * @param logger
     *            the logger
     * @param operation
     *            the operation name (used as prefix in log message)
     * @param message
     *            the completion message
     * @param startTimeMs
     *            the start time in milliseconds
     * @param details
     *            detail key-value pairs
     */
    public static void logWithDuration(
            Object logger, String operation, String message, long startTimeMs, Object... details) {
        long duration = System.currentTimeMillis() - startTimeMs;
        var msg = new StringBuilder(operation)
                .append(": ")
                .append(message)
                .append(" in ")
                .append(duration)
                .append("ms");
        appendDetails(msg, details);
        logInfo(logger, msg.toString());
    }

    private static void appendDetails(StringBuilder msg, Object... details) {
        if (details.length > 0) {
            msg.append(" -");
            for (int i = 0; i < details.length; i += 2) {
                msg.append(' ').append(details[i]).append('=');
                if (i + 1 < details.length) {
                    msg.append(details[i + 1]);
                }
                if (i + 2 < details.length) {
                    msg.append(',');
                }
            }
        }
    }

    /**
     * Log info message.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message
     */
    public static void logInfo(Object logger, String message) {
        invokeLogMethod(logger, "info", String.class, message, () -> log.info("[INFO] {}", message));
    }

    /**
     * Log info message with arguments.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message template
     * @param args
     *            the arguments
     */
    public static void logInfo(Object logger, String message, Object... args) {
        invokeLogMethod(
                logger,
                "info",
                String.class,
                message,
                args,
                () -> log.info("[INFO] {}", String.format(Locale.ROOT, message, args)));
    }

    /**
     * Log debug message.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message
     */
    public static void logDebug(Object logger, String message) {
        invokeLogMethod(logger, "debug", String.class, message, () -> log.debug("[DEBUG] {}", message));
    }

    /**
     * Log debug message with arguments.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message template
     * @param args
     *            the arguments
     */
    public static void logDebug(Object logger, String message, Object... args) {
        invokeLogMethod(
                logger,
                "debug",
                String.class,
                message,
                args,
                () -> log.debug("[DEBUG] {}", String.format(Locale.ROOT, message, args)));
    }

    /**
     * Log warning message.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message
     */
    public static void logWarn(Object logger, String message) {
        invokeLogMethod(logger, "warn", String.class, message, () -> log.warn("[WARN] {}", message));
    }

    /**
     * Log error message.
     *
     * @param logger
     *            the logger
     * @param message
     *            the message
     * @param error
     *            the error
     */
    public static void logError(Object logger, String message, Exception error) {
        invokeLogMethod(logger, "error", String.class, message, error, () -> log.error("[ERROR] {}", message, error));
    }

    private static void invokeLogMethod(
            Object logger, String methodName, Class<?> paramType, Object param, Runnable fallback) {
        if (logger == null) {
            return;
        }
        try {
            java.lang.reflect.Method method = logger.getClass().getMethod(methodName, paramType);
            method.invoke(logger, param);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fallback.run();
        }
    }

    private static void invokeLogMethod(
            Object logger, String methodName, Class<?> paramType, Object param1, Object param2, Runnable fallback) {
        if (logger == null) {
            return;
        }
        try {
            java.lang.reflect.Method method = logger.getClass().getMethod(methodName, paramType, Object[].class);
            method.invoke(logger, param1, param2);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fallback.run();
        }
    }
}
