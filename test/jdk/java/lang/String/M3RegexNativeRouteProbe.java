/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/**
 * Test-only, thread-scoped observation of the existing libjava regex JNI entries.
 * Start the VM with -agentpath and point m3.regex.route.probe at that same library.
 * No production classes or methods need a test hook.
 */
public final class M3RegexNativeRouteProbe {
    public static final int REPEAT = 0;
    public static final int COMPOSE = 1;
    public static final int STRUCTURED_COMPOSE = 2;

    static {
        String library = System.getProperty("m3.regex.route.probe");
        if (library == null || library.isEmpty()) {
            throw new IllegalStateException("m3.regex.route.probe must name the agent library");
        }
        System.load(library);
    }

    private M3RegexNativeRouteProbe() { }

    /** Clear and enable only this native thread's counters. */
    public static native void arm();
    /** Disable only this native thread's counters; retain its observed counts. */
    public static native void disarm();
    /** Snapshot [repeat0, compose0, composeStructured0] for this native thread. */
    public static native long[] counts();
    /** Bit 0=repeat0, bit 1=compose0, bit 2=composeStructured0 successfully wrapped. */
    public static native int boundMask();
    /** Nonzero means the agent could not preserve a target binding. */
    public static native int bindingErrors();
}
