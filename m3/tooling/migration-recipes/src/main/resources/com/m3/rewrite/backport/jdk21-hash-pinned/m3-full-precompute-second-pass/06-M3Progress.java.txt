// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/** Minimal target-owned progress/cancellation contract for bounded precompute work. */
public interface M3Progress {
    M3Progress NONE = new M3Progress() {};

    default void begin(String task, long totalWork) {}
    default void subTask(String task) {}
    default void worked(long work) {}
    default void checkCanceled() {}
    default void done() {}

    static M3Progress none() {
        return NONE;
    }
}
