/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * A borrowed, synchronous observer of the bytes advanced by one main stream.
 * The supplied slice is read-only and valid only for the duration of the call;
 * an observer that retains bytes must make its own copy. An observer may maintain
 * incremental parser or metadata state, but it does not independently read ahead.
 * Neither composition nor stream wrappers close or flush borrowed observers.
 */
@FunctionalInterface
public interface MFileShadow {
    /** Observes a nonempty slice after the main read or confirmed write. */
    void onBytes(byte[] bytes, int offset, int length) throws IOException;

    /**
     * Receives evidence only after the owned primary closes successfully, and for
     * input only after that wrapper has observed EOF. Early close and failures do
     * not complete the observer. A completion failure propagates without retry.
     * This evidence describes the wrapper's byte domain, not source stability or
     * a whole-file assertion for an arbitrarily positioned or decoded stream.
     */
    default void onComplete(MFileStreams.Completion result) throws IOException { }

    /**
     * Calls this observer followed by {@code next} for each slice and completion.
     * Composition is synchronous and fails fast. Earlier observers may already
     * have changed their state when a later observer fails; there is no rollback.
     */
    default MFileShadow andThen(MFileShadow next) {
        return MFileStreams.combineShadows(this, Objects.requireNonNull(next, "next"));
    }

    /** Adapts a caller-owned output without flushing or closing it. */
    static MFileShadow of(OutputStream borrowedSink) {
        return MFileStreams.outputShadow(Objects.requireNonNull(borrowedSink, "borrowedSink"));
    }
}
