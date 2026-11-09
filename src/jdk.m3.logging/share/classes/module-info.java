/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

/**
 * Defines the M3JDK additions to the Java Logging API: {@link jdk.m3.logging.M3StructuredFormatter} writes each log
 * record as one tab-separated line with fixed columns, so logs are tables that CPU tools read directly.
 *
 * @moduleGraph
 * @since 21
 */
module jdk.m3.logging {
    requires transitive java.logging;

    exports jdk.m3.logging;
}
